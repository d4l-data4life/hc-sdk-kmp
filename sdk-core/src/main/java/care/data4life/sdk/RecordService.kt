/*
 * Copyright (c) 2020 D4L data4life gGmbH / All rights reserved.
 *
 * D4L owns all legal rights, title and interest in and to the Software Development Kit ("SDK"),
 * including any intellectual property rights that subsist in the SDK.
 *
 * The SDK and its documentation may be accessed and used for viewing/review purposes only.
 * Any usage of the SDK for other purposes, including usage for the development of
 * applications/third-party applications shall require the conclusion of a license agreement
 * between you and D4L.
 *
 * If you are interested in licensing the SDK for your own applications/third-party
 * applications and/or if you’d like to contribute to the development of the SDK, please
 * contact D4L by email to help@data4life.care.
 */
package care.data4life.sdk

import care.data4life.crypto.GCKey
import care.data4life.crypto.KeyType
import care.data4life.fhir.stu3.model.Attachment
import care.data4life.fhir.stu3.model.DomainResource
import care.data4life.fhir.stu3.model.FhirElementFactory
import care.data4life.fhir.stu3.model.Identifier
import care.data4life.fhir.stu3.util.FhirAttachmentHelper
import care.data4life.sdk.config.DataRestriction.DATA_SIZE_MAX_BYTES
import care.data4life.sdk.config.DataRestrictionException
import care.data4life.sdk.lang.CoreRuntimeException
import care.data4life.sdk.lang.D4LException
import care.data4life.sdk.lang.DataValidationException
import care.data4life.sdk.model.CreateResult
import care.data4life.sdk.model.DeleteResult
import care.data4life.sdk.model.DownloadResult
import care.data4life.sdk.model.DownloadType
import care.data4life.sdk.model.FetchResult
import care.data4life.sdk.model.Meta
import care.data4life.sdk.model.ModelVersion
import care.data4life.sdk.model.Record
import care.data4life.sdk.model.AppDataRecord
import care.data4life.sdk.model.UpdateResult
import care.data4life.sdk.model.definitions.BaseRecord
import care.data4life.sdk.model.definitions.DataRecord
import care.data4life.sdk.network.model.DecryptedRecord
import care.data4life.sdk.network.model.DecryptedAppDataRecord
import care.data4life.sdk.network.model.EncryptedKey
import care.data4life.sdk.network.model.EncryptedRecord
import care.data4life.sdk.network.model.definitions.DecryptedBaseRecord
import care.data4life.sdk.network.model.definitions.DecryptedDataRecord
import care.data4life.sdk.network.model.definitions.DecryptedFhir3Record
import care.data4life.sdk.util.Base64.decode
import care.data4life.sdk.util.Base64.encodeToString
import care.data4life.sdk.util.HashUtil.sha1
import care.data4life.sdk.util.MimeType
import care.data4life.sdk.util.MimeType.Companion.recognizeMimeType
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import org.threeten.bp.LocalDate
import org.threeten.bp.LocalDateTime
import org.threeten.bp.ZoneId
import org.threeten.bp.format.DateTimeFormatter
import org.threeten.bp.format.DateTimeFormatterBuilder
import java.io.IOException
import java.util.*

internal class RecordService(
        private val partnerId: String,
        private val alias: String,
        private val apiService: ApiService,
        private val tagEncryptionService: TagEncryptionService,
        private val taggingService: TaggingService,
        private val fhirService: FhirService,
        private val attachmentService: AttachmentService,
        private val cryptoService: CryptoService,
        private val errorHandler: SdkContract.ErrorHandler
) {
    internal enum class UploadDownloadOperation {
        UPLOAD, DOWNLOAD, UPDATE
    }

    internal enum class RemoveRestoreOperation {
        REMOVE, RESTORE
    }

    @Throws(DataRestrictionException.UnsupportedFileType::class,
            DataRestrictionException.MaxDataSizeViolation::class)
    @JvmOverloads
    fun <T : DomainResource> createRecord(
            resource: T,
            userId: String,
            annotations: List<String> = listOf()
    ): Single<Record<T>> {
        checkDataRestrictions(resource)
        val data = extractUploadData(resource)
        val createdDate = DATE_FORMATTER.format(LocalDate.now(UTC_ZONE_ID))
        val createRecord: Single<DecryptedFhir3Record<T>> = Single.just(createdDate)
                .map { createdAt ->
                    DecryptedRecord(
                            null,
                            resource,
                            taggingService.appendDefaultTags(resource.resourceType, null),
                            annotations,
                            createdAt,
                            null,
                            cryptoService.generateGCKey().blockingGet(),
                            null,
                            ModelVersion.CURRENT
                    )
                }
        return createRecord
                .map { record -> uploadData(record, null, userId) }
                .map { record -> removeUploadData(record) }
                .map { record -> encryptRecord(record) }
                .flatMap { encryptedRecord -> apiService.createRecord(alias, userId, encryptedRecord) }
                .map { encryptedRecord ->
                    @Suppress("UNCHECKED_CAST")
                    decryptRecord<DomainResource>(encryptedRecord, userId) as DecryptedFhir3Record<T>
                }
                .map { record -> restoreUploadData(record, resource, data) }
                .map { record -> assignResourceId(record) }
                .map { decryptedRecord ->
                    Record(
                            decryptedRecord.resource,
                            buildMeta(decryptedRecord),
                            decryptedRecord.annotations
                    )
                }
    }

    fun createRecord(
            resource: ByteArray,
            userId: String,
            annotations: List<String>
    ): Single<DataRecord> {
        val createdDate = DATE_FORMATTER.format(LocalDate.now(UTC_ZONE_ID))
        val createRecord = Single.just(createdDate)
                .map { createdAt ->
                    DecryptedAppDataRecord(
                            null,
                            resource,
                            taggingService.appendDefaultAnnotatedTags(
                                    null,
                                    null
                            ),
                            annotations,
                            createdAt,
                            null,
                            cryptoService.generateGCKey().blockingGet(),
                            ModelVersion.CURRENT
                    )
                }
        return createRecord
                .map { record -> encryptDataRecord(record) }
                .flatMap { encryptedRecord -> apiService.createRecord(alias, userId, encryptedRecord) }
                .map { encryptedRecord -> decryptRecord<ByteArray>(encryptedRecord, userId) }
                .map { decryptedRecord ->
                    AppDataRecord(
                            decryptedRecord.identifier!!,
                            decryptedRecord.resource,
                            buildMeta(decryptedRecord),
                            annotations
                    )
                }
    }

    fun <T : DomainResource> createRecords(resources: List<T>, userId: String): Single<CreateResult<T>> {
        val failedOperations: MutableList<Pair<T, D4LException>> = mutableListOf()
        return Observable
                .fromCallable { resources }
                .flatMapIterable { resource -> resource }
                .flatMapSingle { resource ->
                    createRecord(resource, userId).onErrorReturn { error ->
                        Record<T>(null, null, null).also {
                            failedOperations.add(Pair(resource, errorHandler.handleError(error)))
                            Unit
                        }
                    }
                }
                .filter { record -> record != EMPTY_RECORD }
                .toList()
                .map { successOperations -> CreateResult(successOperations, failedOperations) }
    }

    fun deleteRecord(
            recordId: String,
            userId: String
    ): Completable = apiService.deleteRecord(alias, recordId, userId)

    fun deleteRecords(recordIds: List<String>, userId: String): Single<DeleteResult> {
        val failedDeletes: MutableList<Pair<String, D4LException>> = mutableListOf()
        return Observable
                .fromCallable { recordIds }
                .flatMapIterable { recordId -> recordId }
                .flatMapSingle { recordId ->
                    deleteRecord(recordId, userId)
                            .doOnError { error ->
                                failedDeletes.add(
                                        Pair(recordId, errorHandler.handleError(error)))
                            }
                            .toSingleDefault(recordId)
                            .onErrorReturnItem(EMPTY_RECORD_ID)
                }
                .filter { recordId -> recordId.isNotEmpty() }
                .toList()
                .map { successfulDeletes -> DeleteResult(successfulDeletes, failedDeletes) }
    }

    private fun <T : Any> _fetchRecord(
            recordId: String,
            userId: String
    ): Single<BaseRecord<T>> {
        return apiService
                .fetchRecord(alias, userId, recordId)
                .map { encryptedRecord -> decryptRecord<Any>(encryptedRecord, userId) }
                .map { decryptedRecord ->
                    if (decryptedRecord.resource !is ByteArray) {
                        @Suppress("UNCHECKED_CAST")
                        assignResourceId(decryptedRecord as DecryptedFhir3Record<DomainResource>)
                    } else {
                        decryptedRecord
                    }
                }
                .map { decryptedRecord ->
                    @Suppress("UNCHECKED_CAST")
                    if (decryptedRecord is DecryptedDataRecord) {
                        AppDataRecord(
                                Objects.requireNonNull(decryptedRecord.identifier)!!,
                                decryptedRecord.resource,
                                buildMeta(decryptedRecord),
                                decryptedRecord.annotations
                        ) as BaseRecord<T>
                    } else {
                        Record(
                                decryptedRecord.resource as DomainResource,
                                buildMeta(decryptedRecord)
                        ) as BaseRecord<T>
                    }

                }

    }

    @Suppress("UNCHECKED_CAST")
    fun <T : DomainResource> fetchRecord(
            recordId: String,
            userId: String
    ): Single<Record<T>> = _fetchRecord<T>(recordId, userId) as Single<Record<T>>

    @Suppress("UNCHECKED_CAST")
    @Deprecated("")
    fun fetchAppDataRecord(
            recordId: String,
            userId: String
    ): Single<DataRecord> = _fetchRecord<ByteArray>(recordId, userId) as Single<DataRecord>

    fun <T : DomainResource> fetchRecords(recordIds: List<String>, userId: String): Single<FetchResult<T>> {
        val failedFetches: MutableList<Pair<String, D4LException>> = arrayListOf()
        return Observable
                .fromCallable { recordIds }
                .flatMapIterable { recordId -> recordId }
                .flatMapSingle { recordId ->
                    fetchRecord<T>(recordId, userId)
                            .onErrorReturn { error ->
                                Record<T>(null, null, null).also {
                                    failedFetches.add(Pair(recordId, errorHandler.handleError(error)))
                                    Unit
                                }
                            }
                }
                .filter { record -> record != EMPTY_RECORD }
                .toList()
                .map { successfulFetches ->
                    @Suppress("UNCHECKED_CAST")
                    FetchResult(
                            successfulFetches as MutableList<Record<T>>,
                            failedFetches
                    )
                }
    }

    fun <T : DomainResource> fetchRecords(
            userId: String,
            resourceType: Class<T>,
            startDate: LocalDate?,
            endDate: LocalDate?,
            pageSize: Int,
            offset: Int
    ): Single<List<Record<T>>> = fetchRecords(
            userId,
            resourceType, emptyList(),
            startDate,
            endDate,
            pageSize,
            offset
    )

    fun <T : DomainResource> fetchRecords(
            userId: String,
            resourceType: Class<T>,
            annotations: List<String>,
            startDate: LocalDate?,
            endDate: LocalDate?,
            pageSize: Int,
            offset: Int
    ): Single<List<Record<T>>> = fetch(
            userId,
            annotations,
            startDate,
            endDate,
            pageSize,
            offset,
            { taggingService.getTagFromType(FhirElementFactory.getFhirTypeForClass(resourceType)) }
    )
            .map { encryptedRecord ->
                @Suppress("UNCHECKED_CAST")
                decryptRecord<DomainResource>(encryptedRecord, userId) as DecryptedFhir3Record<T>
            }
            .filter { decryptedRecord -> resourceType.isAssignableFrom(decryptedRecord.resource::class.java) }
            .filter { decryptedRecord -> decryptedRecord.annotations.containsAll(annotations) }
            .map { record -> assignResourceId(record) }
            .map { decryptedRecord ->
                Record(
                        decryptedRecord.resource,
                        buildMeta(decryptedRecord),
                        decryptedRecord.annotations
                )
            }
            .toList()

    fun fetchRecords(
            userId: String,
            annotations: List<String>,
            startDate: LocalDate?,
            endDate: LocalDate?,
            pageSize: Int,
            offset: Int
    ): Single<List<DataRecord>> = fetch(
            userId,
            annotations,
            startDate,
            endDate,
            pageSize,
            offset,
            { taggingService.appendAppDataTags(HashMap()) }
    )
            .map { encryptedRecord -> decryptRecord<ByteArray>(encryptedRecord, userId) }
            .map { decryptedRecord ->
                AppDataRecord(
                        decryptedRecord.identifier!!,
                        decryptedRecord.resource,
                        buildMeta(decryptedRecord),
                        decryptedRecord.annotations
                ) as DataRecord
            }
            .toList()

    fun downloadAttachment(
            recordId: String,
            attachmentId: String,
            userId: String,
            type: DownloadType
    ): Single<Attachment> = downloadAttachments(
            recordId,
            arrayListOf(attachmentId),
            userId,
            type
    ).map { it[0] }

    fun downloadAttachments(
            recordId: String,
            attachmentIds: List<String>,
            userId: String,
            type: DownloadType
    ): Single<List<Attachment>> = apiService
            .fetchRecord(alias, userId, recordId)
            .map { encryptedRecord -> decryptRecord<DomainResource>(encryptedRecord, userId) }
            .flatMap { decryptedRecord ->
                downloadAttachmentsFromStorage(
                        attachmentIds,
                        userId,
                        type,
                        decryptedRecord as DecryptedFhir3Record<DomainResource>
                )
            }

    @Throws(DataValidationException.IdUsageViolation::class,
            DataValidationException.InvalidAttachmentPayloadHash::class)
    private fun downloadAttachmentsFromStorage(
            attachmentIds: List<String>,
            userId: String,
            type: DownloadType,
            decryptedRecord: DecryptedFhir3Record<DomainResource>
    ): Single<out List<Attachment>> {
        if (FhirAttachmentHelper.hasAttachment(decryptedRecord.resource)) {
            val resource = decryptedRecord.resource
            val attachments = FhirAttachmentHelper.getAttachment(resource)
            val validAttachments = mutableListOf<Attachment>()

            for (attachment in attachments) {
                if (attachmentIds.contains(attachment.id)) {
                    validAttachments.add(attachment)
                }
            }
            if (validAttachments.size != attachmentIds.size)
                throw DataValidationException.IdUsageViolation("Please provide correct attachment ids!")

            setAttachmentIdForDownloadType(
                    validAttachments,
                    FhirAttachmentHelper.getIdentifier(resource),
                    type
            )

            return attachmentService.downloadAttachments(
                    validAttachments,
                    decryptedRecord.attachmentsKey,
                    userId
            )
                    .flattenAsObservable { items -> items }
                    .map { attachment ->
                        attachment.also {
                            if (attachment.id!!.contains(SPLIT_CHAR)) updateAttachmentMeta(attachment)
                        }
                    }
                    .toList()
        }

        throw IllegalArgumentException("Expected a record of a type that has attachment")
    }

    fun deleteAttachment(
            attachmentId: String,
            userId: String
    ): Single<Boolean> = attachmentService.deleteAttachment(attachmentId, userId)

    fun <T : DomainResource> downloadRecord(
            recordId: String,
            userId: String
    ): Single<Record<T>> = apiService
            .fetchRecord(alias, userId, recordId)
            .map { encryptedRecord ->
                @Suppress("UNCHECKED_CAST")
                decryptRecord<DomainResource>(encryptedRecord, userId) as DecryptedFhir3Record<T>
            }
            .map { decryptedRecord -> downloadData(decryptedRecord, userId) }
            .map { decryptedRecord ->
                decryptedRecord.also {
                    checkDataRestrictions(decryptedRecord.resource)
                }
            }
            .map { record -> assignResourceId(record) }
            .map { decryptedRecord ->
                Record(
                        decryptedRecord.resource,
                        buildMeta(decryptedRecord)
                )
            }

    fun <T : DomainResource> downloadRecords(
            recordIds: List<String>,
            userId: String
    ): Single<DownloadResult<T>> {
        val failedDownloads: MutableList<Pair<String, D4LException>> = arrayListOf()
        return Observable
                .fromCallable { recordIds }
                .flatMapIterable { recordId -> recordId }
                .flatMapSingle { recordId ->
                    downloadRecord<T>(recordId, userId)
                            .onErrorReturn { error ->
                                failedDownloads.add(Pair(recordId, errorHandler.handleError(error)))
                                Record<T>(null, null, null)
                            }
                }
                .filter { record -> record != EMPTY_RECORD }
                .toList()
                .map { successfulDownloads -> DownloadResult(successfulDownloads, failedDownloads) }
    }

    @Throws(DataRestrictionException.UnsupportedFileType::class,
            DataRestrictionException.MaxDataSizeViolation::class)
    @JvmOverloads
    fun <T : DomainResource> updateRecord(
            resource: T,
            userId: String,
            annotations: List<String>? = null
    ): Single<Record<T>> {
        checkDataRestrictions(resource)
        val data = extractUploadData(resource)
        val recordId = resource.id
        return apiService
                .fetchRecord(alias, userId, recordId)
                .map { encryptedRecord -> decryptRecord<DomainResource>(encryptedRecord, userId) }
                .map { decryptedRecord ->
                    @Suppress("UNCHECKED_CAST")
                    uploadData(
                            decryptedRecord as DecryptedFhir3Record<T>,
                            resource,
                            userId
                    )
                }
                .map { decryptedRecord ->
                    decryptedRecord.also {
                        cleanObsoleteAdditionalIdentifiers(resource)
                        decryptedRecord.resource = resource
                        if (annotations != null) {
                            decryptedRecord.annotations = annotations
                        }
                    }
                }
                .map { record -> removeUploadData(record) }
                .map { record -> encryptRecord(record) }
                .flatMap { encryptedRecord -> apiService.updateRecord(alias, userId, recordId, encryptedRecord) }
                .map { encryptedRecord ->
                    @Suppress("UNCHECKED_CAST")
                    decryptRecord<DomainResource>(encryptedRecord, userId) as DecryptedFhir3Record<T>
                }
                .map { decryptedRecord -> restoreUploadData(decryptedRecord, resource, data) }
                .map { record -> assignResourceId(record) }
                .map { decryptedRecord ->
                    Record(
                            decryptedRecord.resource,
                            buildMeta(decryptedRecord),
                            decryptedRecord.annotations
                    )
                }
    }

    fun updateRecord(
            resource: ByteArray,
            userId: String,
            recordId: String,
            annotations: List<String>? = listOf()
    ): Single<DataRecord> = apiService
            .fetchRecord(alias, userId, recordId)
            .map { encryptedRecord -> decryptRecord<ByteArray>(encryptedRecord, userId) as DecryptedDataRecord }
            .map { decryptedRecord -> decryptedRecord.copyWithResourceAnnotations(resource, annotations) }
            .map { record -> encryptDataRecord(record) }
            .flatMap { encryptedRecord ->
                apiService.updateRecord(
                        alias,
                        userId,
                        recordId,
                        encryptedRecord
                )
            }
            .map { encryptedRecord -> decryptRecord<ByteArray>(encryptedRecord, userId) }
            .map { decryptedRecord ->
                AppDataRecord(
                        decryptedRecord.identifier!!,
                        decryptedRecord.resource,
                        buildMeta(decryptedRecord),
                        decryptedRecord.annotations
                )
            }

    fun <T : DomainResource> updateRecords(resources: List<T>, userId: String): Single<UpdateResult<T>> {
        val failedUpdates: MutableList<Pair<T, D4LException>> = arrayListOf()
        return Observable
                .fromCallable { resources }
                .flatMapIterable { resource -> resource }
                .flatMapSingle { resource ->
                    updateRecord(resource, userId)
                            .onErrorReturn { error ->
                                Record<T>(null, null, null).also {
                                    failedUpdates.add(Pair(resource, errorHandler.handleError(error)))
                                    Unit
                                }
                            }
                }
                .filter { record -> record != EMPTY_RECORD }
                .toList()
                .map { successfulUpdates -> UpdateResult(successfulUpdates, failedUpdates) }
    }

    @JvmOverloads
    fun countRecords(
            type: Class<out DomainResource>?,
            userId: String,
            annotations: List<String> = listOf()
    ): Single<Int> = if (type == null) {
        apiService.getCount(alias, userId, null)
    } else {
        Single
                .fromCallable { taggingService.getTagFromType(FhirElementFactory.getFhirTypeForClass(type)) }
                .map { tags -> tagEncryptionService.encryptTags(tags) as MutableList<String> }
                .map { tags -> tags.also { it.addAll(tagEncryptionService.encryptAnnotations(annotations)) } }
                .flatMap { encryptedTags -> apiService.getCount(alias, userId, encryptedTags) }
    }

    //region utility methods
    private fun fetch(
            userId: String,
            annotations: List<String>,
            startDate: LocalDate?,
            endDate: LocalDate?,
            pageSize: Int,
            offset: Int,
            getTags: () -> HashMap<String, String>?
    ): Observable<EncryptedRecord> {
        val startTime = if (startDate != null) DATE_FORMATTER.format(startDate) else null
        val endTime = if (endDate != null) DATE_FORMATTER.format(endDate) else null
        return Observable
                .fromCallable { getTags() }
                .map { tags -> tagEncryptionService.encryptTags(tags) as MutableList<String> }
                .map { tags ->
                    tags.also {
                        it.addAll(tagEncryptionService.encryptAnnotations(annotations))
                    }
                }
                .flatMap { encryptedTags ->
                    apiService.fetchRecords(
                            alias,
                            userId,
                            startTime,
                            endTime,
                            pageSize,
                            offset,
                            encryptedTags
                    )
                }
                .flatMapIterable { encryptedRecords -> encryptedRecords }
    }

    @Throws(IOException::class)
    private fun <T> encrypt(
            record: DecryptedBaseRecord<T>,
            getEncryptedResource: () -> String,
            getEncryptedAttachment: (commonKey: GCKey) -> EncryptedKey?
    ): EncryptedRecord {
        val encryptedTags = tagEncryptionService.encryptTags(record.tags!!).also {
            (it as MutableList<String>).addAll(tagEncryptionService.encryptAnnotations(record.annotations))
        }
        val encryptedResource = getEncryptedResource()
        val commonKey = cryptoService.fetchCurrentCommonKey()
        val currentCommonKeyId = cryptoService.currentCommonKeyId
        val encryptedDataKey = cryptoService.encryptSymmetricKey(
                commonKey,
                KeyType.DATA_KEY,
                record.dataKey!!
        ).blockingGet()
        val encryptedAttachmentsKey = getEncryptedAttachment(commonKey)
        return EncryptedRecord(
                currentCommonKeyId,
                record.identifier,
                encryptedTags,
                encryptedResource,
                record.customCreationDate,
                encryptedDataKey,
                encryptedAttachmentsKey,
                record.modelVersion
        )
    }

    @Throws(IOException::class, DataValidationException.ModelVersionNotSupported::class)
    private fun <T> decrypt(
            record: EncryptedRecord,
            userId: String?,
            decryptSource: (
                    tags: HashMap<String, String>,
                    annotations: List<String>,
                    dataKey: GCKey,
                    commonKey: GCKey
            ) -> T
    ): T {
        if (!ModelVersion.isModelVersionSupported(record.modelVersion)) {
            throw DataValidationException.ModelVersionNotSupported("Please update SDK to latest version!")
        }
        val tags = tagEncryptionService.decryptTags(record.encryptedTags)
        val annotations = tagEncryptionService.decryptAnnotations(record.encryptedTags)
        val commonKeyId = record.commonKeyId
        val commonKeyStored = cryptoService.hasCommonKey(commonKeyId)
        val commonKey: GCKey =
                if (commonKeyStored) {
                    cryptoService.getCommonKeyById(commonKeyId)
                } else {
                    val commonKeyResponse = apiService.fetchCommonKey(
                            alias,
                            userId,
                            commonKeyId
                    ).blockingGet()

                    cryptoService.asymDecryptSymetricKey(
                            cryptoService.fetchGCKeyPair().blockingGet(),
                            commonKeyResponse.commonKey
                    ).blockingGet().also {
                        cryptoService.storeCommonKey(commonKeyId, it)
                    }
                }
        val dataKey = cryptoService.symDecryptSymmetricKey(commonKey, record.encryptedDataKey).blockingGet()
        return decryptSource(
                tags,
                annotations,
                dataKey,
                commonKey
        )
    }

    @Throws(IOException::class)
    fun <T : DomainResource> encryptRecord(
            record: DecryptedFhir3Record<T>
    ): EncryptedRecord = encrypt(
            record,
            { fhirService.encryptResource(record.dataKey!!, record.resource) },
            { commonKey: GCKey ->
                if (record.attachmentsKey == null) {
                    null
                } else {
                    cryptoService.encryptSymmetricKey(
                            commonKey,
                            KeyType.ATTACHMENT_KEY,
                            record.attachmentsKey!!
                    ).blockingGet()
                }
            }
    )

    @Throws(IOException::class)
    internal fun encryptDataRecord(
            record: DecryptedDataRecord? //FIXME: this a test concern, which should be removed soon as possible
    ): EncryptedRecord = encrypt(
            record!!,
            { encodeToString(cryptoService.encrypt(record.dataKey!!, record.resource).blockingGet()) },
            { null }
    )

    @Suppress("UNCHECKED_CAST")
    @Throws(IOException::class, DataValidationException.ModelVersionNotSupported::class)
    fun <T : Any> decryptRecord(
            record: EncryptedRecord,
            userId: String
    ): DecryptedBaseRecord<T> = decrypt(
            record,
            userId
    ) { tags: HashMap<String, String>, annotations: List<String>, dataKey: GCKey, commonKey: GCKey ->
        if (tags.containsKey(TaggingService.TAG_RESOURCE_TYPE)) {
            decryptFhirRecord<DomainResource>(
                    record,
                    tags,
                    annotations,
                    dataKey,
                    commonKey
            ) as DecryptedBaseRecord<T>
        } else {
            decryptDataRecord(
                    record,
                    tags,
                    annotations,
                    dataKey
            ) as DecryptedBaseRecord<T>
        }
    }

    @Throws(IOException::class, DataValidationException.ModelVersionNotSupported::class)
    private fun <T : DomainResource> decryptFhirRecord(
            record: EncryptedRecord,
            tags: HashMap<String, String>,
            annotations: List<String>,
            dataKey: GCKey,
            commonKey: GCKey
    ): DecryptedFhir3Record<T?> {
        val attachmentsKey =
                if (record.encryptedAttachmentsKey == null) {
                    null
                } else {
                    cryptoService.symDecryptSymmetricKey(
                            commonKey,
                            record.encryptedAttachmentsKey
                    ).blockingGet()
                }

        val resource =
                if (record.encryptedBody == null || record.encryptedBody.isEmpty()) {
                    null
                } else {
                    fhirService.decryptResource<T>(
                            dataKey,
                            tags[TaggingService.TAG_RESOURCE_TYPE]!!,
                            record.encryptedBody
                    )
                }

        return DecryptedRecord(
                record.identifier,
                resource,
                tags,
                annotations,
                record.customCreationDate,
                record.updatedDate,
                dataKey,
                attachmentsKey,
                record.modelVersion
        )
    }

    @Throws(IOException::class, DataValidationException.ModelVersionNotSupported::class)
    private fun decryptDataRecord(
            record: EncryptedRecord,
            tags: HashMap<String, String>,
            annotations: List<String>,
            dataKey: GCKey
    ): DecryptedDataRecord {
        val resource = if (record.encryptedBody == null || record.encryptedBody.isEmpty()) {
            ByteArray(0)// FIXME: This is a potential Bug, we should throw an error here, like NoValidRecord
        } else {
            decode(record.encryptedBody).let {
                cryptoService.decrypt(dataKey, it).blockingGet()
            }
        }

        return DecryptedAppDataRecord(
                record.identifier,
                resource,
                tags,
                annotations,
                record.customCreationDate,
                record.updatedDate,
                dataKey,
                record.modelVersion
        )
    }

    fun <T : DomainResource> extractUploadData(resource: T): HashMap<Attachment, String?>? {
        val attachments = FhirAttachmentHelper.getAttachment(resource)
        if (attachments == null || attachments.isEmpty()) return null

        val data = HashMap<Attachment, String?>(attachments.size)
        for (attachment in attachments) {
            if (attachment?.data != null) {
                data[attachment] = attachment.data
            }
        }
        return if (data.isEmpty()) null else data
    }

    fun <T : DomainResource> removeUploadData(
            record: DecryptedFhir3Record<T>
    ): DecryptedFhir3Record<T> = removeOrRestoreUploadData(
            RemoveRestoreOperation.REMOVE,
            record,
            null,
            null
    )

    fun <T : DomainResource> restoreUploadData(
            record: DecryptedFhir3Record<T>,
            originalResource: T,
            attachmentData: HashMap<Attachment, String?>?
    ): DecryptedFhir3Record<T> = removeOrRestoreUploadData(
            RemoveRestoreOperation.RESTORE,
            record,
            originalResource,
            attachmentData
    )

    fun <T : DomainResource> removeOrRestoreUploadData(
            operation: RemoveRestoreOperation,
            record: DecryptedFhir3Record<T>,
            originalResource: T?,
            attachmentData: HashMap<Attachment, String?>?
    ): DecryptedFhir3Record<T> {
        if (operation == RemoveRestoreOperation.RESTORE) {
            if (originalResource != null) record.resource = originalResource
            if (attachmentData == null) return record
        }
        val attachments = FhirAttachmentHelper.getAttachment(record.resource)
        return record.also {
            if (attachments != null && attachments.isNotEmpty()) {
                FhirAttachmentHelper.updateAttachmentData(record.resource, attachmentData)
            }
        }
    }

    @Throws(DataValidationException.IdUsageViolation::class,
            DataValidationException.ExpectedFieldViolation::class,
            DataValidationException.InvalidAttachmentPayloadHash::class)
    fun <T : DomainResource> _uploadData(
            record: DecryptedFhir3Record<T>,
            userId: String
    ): DecryptedFhir3Record<T> {
        val resource = record.resource
        if (!FhirAttachmentHelper.hasAttachment(resource)) return record
        val attachments = if (FhirAttachmentHelper.getAttachment(resource) == null) {
            arrayListOf()
        } else {
            FhirAttachmentHelper.getAttachment(resource)
        }

        attachments ?: return record

        if (record.attachmentsKey == null) {
            record.attachmentsKey = cryptoService.generateGCKey().blockingGet()
        }
        val validAttachments: MutableList<Attachment> = arrayListOf()
        for (attachment in attachments) {
            if (attachment != null) {
                when {
                    attachment.id != null ->
                        throw DataValidationException.IdUsageViolation("Attachment.id should be null")

                    attachment.hash == null || attachment.size == null ->
                        throw DataValidationException.ExpectedFieldViolation(
                                "Attachment.hash and Attachment.size expected"
                        )
                    getValidHash(attachment) != attachment.hash ->
                        throw DataValidationException.InvalidAttachmentPayloadHash(
                                "Attachment.hash is not valid"
                        )
                    else -> validAttachments.add(attachment)
                }
            }
        }
        if (validAttachments.isNotEmpty()) {
            updateDomainResourceIdentifier(
                    resource,
                    attachmentService.uploadAttachments(
                            validAttachments,
                            record.attachmentsKey,
                            userId
                    ).blockingGet()
            )
        }
        return record
    }

    @Throws(DataValidationException.IdUsageViolation::class,
            DataValidationException.ExpectedFieldViolation::class,
            DataValidationException.InvalidAttachmentPayloadHash::class,
            CoreRuntimeException.UnsupportedOperation::class)
    fun <T : DomainResource> updateData(
            record: DecryptedFhir3Record<T>,
            newResource: T?,
            userId: String?
    ): DecryptedFhir3Record<T> {
        newResource ?: throw CoreRuntimeException.UnsupportedOperation()

        var resource = record.resource
        if (!FhirAttachmentHelper.hasAttachment(resource)) return record
        val attachments =
                if (FhirAttachmentHelper.getAttachment(resource) == null) {
                    arrayListOf()
                } else {
                    FhirAttachmentHelper.getAttachment(resource)
                }

        val validAttachments: MutableList<Attachment> = arrayListOf()
        val oldAttachments: HashMap<String?, Attachment> = hashMapOf()

        for (attachment in attachments) {
            if (attachment?.id != null) {
                oldAttachments[attachment.id] = attachment
            }
        }
        resource = newResource
        val newAttachments = FhirAttachmentHelper.getAttachment(newResource)
        for (newAttachment in newAttachments) {
            if (newAttachment != null) {
                when {
                    newAttachment.hash == null || newAttachment.size == null ->
                        throw DataValidationException.ExpectedFieldViolation(
                                "Attachment.hash and Attachment.size expected"
                        )
                    getValidHash(newAttachment) != newAttachment.hash ->
                        throw DataValidationException.InvalidAttachmentPayloadHash(
                                "Attachment.hash is not valid"
                        )
                    newAttachment.id == null -> validAttachments.add(newAttachment)
                    else -> {
                        val oldAttachment = oldAttachments[newAttachment.id]
                                ?: throw DataValidationException.IdUsageViolation(
                                        "Valid Attachment.id expected"
                                )
                        if (oldAttachment.hash == null || newAttachment.hash != oldAttachment.hash) {
                            validAttachments.add(newAttachment)
                        }
                    }
                }
            }
        }
        if (validAttachments.isNotEmpty()) {
            updateDomainResourceIdentifier(
                    resource,
                    attachmentService.uploadAttachments(
                            validAttachments,
                            record.attachmentsKey,
                            userId
                    ).blockingGet()
            )
        }
        return record
    }

    @Throws(DataValidationException.IdUsageViolation::class, DataValidationException.InvalidAttachmentPayloadHash::class)
    fun <T : DomainResource> downloadData(
            record: DecryptedFhir3Record<T>,
            userId: String?
    ): DecryptedFhir3Record<T> {
        val resource = record.resource
        if (!FhirAttachmentHelper.hasAttachment(resource)) return record
        val attachments = if (FhirAttachmentHelper.getAttachment(resource) == null) {
            arrayListOf()
        } else {
            FhirAttachmentHelper.getAttachment(resource)
        }

        attachments ?: return record

        attachments.forEach {
            it.id ?: throw DataValidationException.IdUsageViolation("Attachment.id expected")
        }

        @Suppress("CheckResult")
        attachmentService.downloadAttachments(
                attachments,
                record.attachmentsKey,
                userId
        ).blockingGet()
        return record
    }

    // FIXME
    // This method should not allowed to exist any longer in this shape. _uploadData should take over
    // as soon as possible so we can get rid of uploadOrDownloadData. This also means uploadData should
    // not be responsible for the actual upload and a update.
    @Throws(DataValidationException.IdUsageViolation::class,
            DataValidationException.ExpectedFieldViolation::class,
            DataValidationException.InvalidAttachmentPayloadHash::class)
    fun <T : DomainResource> uploadData(
            record: DecryptedFhir3Record<T>,
            newResource: T?,
            userId: String
    ): DecryptedFhir3Record<T> = if (newResource == null) _uploadData(record, userId) else updateData(record, newResource, userId)

    @Deprecated("")
    @Throws(DataValidationException.IdUsageViolation::class,
            DataValidationException.ExpectedFieldViolation::class,
            DataValidationException.InvalidAttachmentPayloadHash::class,
            CoreRuntimeException.UnsupportedOperation::class)
    fun <T : DomainResource> uploadOrDownloadData(
            operation: UploadDownloadOperation,
            record: DecryptedFhir3Record<T>,
            newResource: T?,
            userId: String
    ): DecryptedFhir3Record<T> {
        return when (operation) {
            UploadDownloadOperation.UPDATE -> updateData(record, newResource!!, userId)
            UploadDownloadOperation.UPLOAD -> _uploadData(record, userId)
            UploadDownloadOperation.DOWNLOAD -> downloadData(record, userId)
        }
    }

    private fun updateDomainResourceIdentifier(
            d: DomainResource,
            result: List<Pair<Attachment, List<String>?>>
    ) {
        val sb = StringBuilder()
        for ((first, second) in result) {
            if (second != null) { //Attachment is a of image type
                sb.setLength(0)
                sb.append(DOWNSCALED_ATTACHMENT_IDS_FMT).append(SPLIT_CHAR).append(first.id)
                for (additionalId in second) {
                    sb.append(SPLIT_CHAR).append(additionalId)
                }
                FhirAttachmentHelper.appendIdentifier(d, sb.toString(), partnerId)
            }
        }
    }

    @Throws(DataValidationException.IdUsageViolation::class)
    fun setAttachmentIdForDownloadType(
            attachments: List<Attachment>,
            identifiers: List<Identifier>?,
            type: DownloadType?
    ) {
        for (attachment in attachments) {
            val additionalIds = extractAdditionalAttachmentIds(identifiers, attachment.id)
            if (additionalIds != null) {
                when (type) {
                    DownloadType.Full -> {
                    }
                    DownloadType.Medium -> attachment.id += SPLIT_CHAR + additionalIds[PREVIEW_ID_POS]
                    DownloadType.Small -> attachment.id += SPLIT_CHAR + additionalIds[THUMBNAIL_ID_POS]
                    else -> throw CoreRuntimeException.UnsupportedOperation()
                }
            }
        }
    }

    @Throws(DataValidationException.IdUsageViolation::class)
    fun extractAdditionalAttachmentIds(
            additionalIds: List<Identifier>?,
            attachmentId: String?
    ): Array<String>? {
        if (additionalIds == null) return null
        for (i in additionalIds) {
            val parts = splitAdditionalAttachmentId(i)
            if (parts != null && parts[FULL_ATTACHMENT_ID_POS] == attachmentId) return parts
        }
        return null //Attachment is not of image type
    }

    @Throws(DataValidationException.IdUsageViolation::class)
    fun splitAdditionalAttachmentId(identifier: Identifier): Array<String>? {
        if (identifier.value == null || !identifier.value!!.startsWith(DOWNSCALED_ATTACHMENT_IDS_FMT)) {
            return null
        }
        val parts = identifier.value!!.split(SPLIT_CHAR.toRegex()).toTypedArray()
        if (parts.size != DOWNSCALED_ATTACHMENT_IDS_SIZE) {
            throw DataValidationException.IdUsageViolation(identifier.value)
        }
        return parts
    }

    fun updateAttachmentMeta(attachment: Attachment): Attachment {
        val data = decode(attachment.data!!)
        attachment.size = data.size
        attachment.hash = encodeToString(sha1(data))
        return attachment
    }

    fun getValidHash(attachment: Attachment): String {
        val data = decode(attachment.data!!)
        return encodeToString(sha1(data))
    }

    @Throws(DataValidationException.IdUsageViolation::class)
    fun <T : DomainResource?> cleanObsoleteAdditionalIdentifiers(resource: T) {
        if (FhirAttachmentHelper.hasAttachment(resource) && FhirAttachmentHelper.getAttachment(resource).isNotEmpty()) {
            val identifiers = FhirAttachmentHelper.getIdentifier(resource)
            val currentAttachments = FhirAttachmentHelper.getAttachment(resource)
            val currentAttachmentIds: MutableList<String> = arrayListOf(currentAttachments.size.toString())

            currentAttachments.forEach { currentAttachmentIds.add(it.id!!) }

            if (identifiers == null) return
            val updatedIdentifiers: MutableList<Identifier> = mutableListOf()
            val identifierIterator = identifiers.iterator()

            while (identifierIterator.hasNext()) {
                val next = identifierIterator.next()
                val parts = splitAdditionalAttachmentId(next)
                if (parts == null || currentAttachmentIds.contains(parts[FULL_ATTACHMENT_ID_POS])) {
                    updatedIdentifiers.add(next)
                } else {
                    identifierIterator.remove()
                }
            }
            FhirAttachmentHelper.setIdentifier(resource, updatedIdentifiers)
        }
    }

    @Throws(DataRestrictionException.MaxDataSizeViolation::class, DataRestrictionException.UnsupportedFileType::class)
    fun <T : DomainResource> checkDataRestrictions(resource: T?) {
        if (!FhirAttachmentHelper.hasAttachment(resource)) return
        val attachments = FhirAttachmentHelper.getAttachment(resource)
        for (attachment in attachments) {
            attachment?.data ?: return

            val data = decode(attachment.data!!)
            if (recognizeMimeType(data) == MimeType.UNKNOWN) {
                throw DataRestrictionException.UnsupportedFileType()
            }
            if (data.size > DATA_SIZE_MAX_BYTES) {
                throw DataRestrictionException.MaxDataSizeViolation()
            }
        }
    }

    fun <T : DomainResource> assignResourceId(
            record: DecryptedFhir3Record<T>
    ): DecryptedFhir3Record<T> = record.also { record.resource.id = record.identifier }

    fun buildMeta(
            record: DecryptedBaseRecord<*>
    ): Meta = Meta(
            LocalDate.parse(record.customCreationDate, DATE_FORMATTER),
            LocalDateTime.parse(record.updatedDate, DATE_TIME_FORMATTER)
    )
    //endregion

    companion object {
        private val EMPTY_RECORD = Record(null, null, null)
        private const val DATE_FORMAT = "yyyy-MM-dd"
        private const val DATE_TIME_FORMAT = "yyyy-MM-dd'T'HH:mm:ss[.SSS]"
        private const val EMPTY_RECORD_ID = ""
        const val DOWNSCALED_ATTACHMENT_IDS_FMT = "d4l_f_p_t" //d4l -> namespace, f-> full, p -> preview, t -> thumbnail
        const val SPLIT_CHAR = "#"
        const val DOWNSCALED_ATTACHMENT_IDS_SIZE = 4
        const val FULL_ATTACHMENT_ID_POS = 1
        const val PREVIEW_ID_POS = 2
        const val THUMBNAIL_ID_POS = 3
        private val DATE_FORMATTER = DateTimeFormatter.ofPattern(DATE_FORMAT, Locale.US)
        private val DATE_TIME_FORMATTER = DateTimeFormatterBuilder()
                .parseLenient()
                .appendPattern(DATE_TIME_FORMAT)
                .toFormatter(Locale.US)
        private val UTC_ZONE_ID = ZoneId.of("UTC")
    }
}
