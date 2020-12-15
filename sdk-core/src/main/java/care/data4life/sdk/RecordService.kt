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
import care.data4life.sdk.attachment.AttachmentContract
import care.data4life.sdk.attachment.ThumbnailService
import care.data4life.sdk.attachment.ThumbnailService.Companion.SPLIT_CHAR
import care.data4life.sdk.call.DataRecord
import care.data4life.sdk.call.Fhir4Record
import care.data4life.sdk.config.DataRestriction.DATA_SIZE_MAX_BYTES
import care.data4life.sdk.config.DataRestrictionException
import care.data4life.sdk.data.DataResource
import care.data4life.sdk.fhir.Fhir3Resource
import care.data4life.sdk.fhir.Fhir4Resource
import care.data4life.sdk.fhir.FhirService
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
import care.data4life.sdk.wrappers.FhirAttachmentHelper
import care.data4life.sdk.wrappers.FhirElementFactory
import care.data4life.sdk.model.SdkRecordFactory
import care.data4life.sdk.model.UpdateResult
import care.data4life.sdk.model.definitions.BaseRecord
import care.data4life.sdk.model.definitions.RecordFactory
import care.data4life.sdk.network.DecryptedRecordBuilder
import care.data4life.sdk.network.model.EncryptedKey
import care.data4life.sdk.network.model.EncryptedRecord
import care.data4life.sdk.network.model.NetworkRecordsContract
import care.data4life.sdk.record.RecordContract
import care.data4life.sdk.tag.TagEncryptionService
import care.data4life.sdk.tag.TaggingService
import care.data4life.sdk.util.Base64.decode
import care.data4life.sdk.util.Base64.encodeToString
import care.data4life.sdk.util.HashUtil.sha1
import care.data4life.sdk.util.MimeType
import care.data4life.sdk.util.MimeType.Companion.recognizeMimeType
import care.data4life.sdk.wrappers.AttachmentFactory
import care.data4life.sdk.wrappers.HelpersContract
import care.data4life.sdk.wrappers.SdkIdentifierFactory
import care.data4life.sdk.wrappers.WrapperFactoriesContract
import care.data4life.sdk.wrappers.WrappersContract
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
import kotlin.collections.HashMap


// TODO internal
class RecordService(
        private val partnerId: String,
        private val alias: String,
        private val apiService: ApiService,
        private val tagEncryptionService: TagEncryptionService,
        private val taggingService: TaggingService,
        private val fhirService: FhirService,
        private val attachmentService: AttachmentContract.Service,
        private val cryptoService: CryptoService,
        private val errorHandler: SdkContract.ErrorHandler
) : RecordContract.Service {
    @Deprecated("")
    internal enum class UploadDownloadOperation {
        UPLOAD, DOWNLOAD, UPDATE
    }

    @Deprecated("")
    internal enum class RemoveRestoreOperation {
        RESTORE
    }

    private val recordFactory: RecordFactory = SdkRecordFactory
    private val fhirElementFactory: WrapperFactoriesContract.FhirElementFactory = FhirElementFactory
    private val fhirAttachmentHelper: HelpersContract.FhirAttachmentHelper = FhirAttachmentHelper
    private val attachmentFactory: WrapperFactoriesContract.AttachmentFactory = AttachmentFactory
    private val identifierFactory: WrapperFactoriesContract.IdentifierFactory = SdkIdentifierFactory

    private fun getTagsOnCreate(resource: Any): HashMap<String, String> {
        return if (resource is ByteArray) {
            taggingService.appendDefaultTags(null, null)
        } else {
            taggingService.appendDefaultTags((resource as Fhir3Resource).resourceType, null)
        }
    }

    private fun <T : Any> createRecord(
            userId: String,
            resource: T,
            annotations: List<String>
    ): Single<BaseRecord<T>> {
        checkDataRestrictions(resource)
        val data = extractUploadData(resource)
        val createdRecord = Single.just(
                DecryptedRecordBuilder()
                        .setAnnotations(annotations)
                        .build(
                                resource,
                                this.getTagsOnCreate(resource),
                                DATE_FORMATTER.format(LocalDate.now(UTC_ZONE_ID)),
                                cryptoService.generateGCKey().blockingGet(),
                                ModelVersion.CURRENT
                        )
        )

        return createdRecord
                .map { _uploadData(it, userId) }
                .map { removeUploadData(it) }
                .map { encryptRecord(it) }
                .flatMap { apiService.createRecord(alias, userId, it) }
                .map { decryptRecord<T>(it, userId) }
                .map { restoreUploadData(it, resource, data) }
                .map { assignResourceId(it) }
                .map { recordFactory.getInstance(it) }
    }

    @Suppress("UNCHECKED_CAST")
    @Throws(DataRestrictionException.UnsupportedFileType::class,
            DataRestrictionException.MaxDataSizeViolation::class)
    override fun <T : Fhir3Resource> createRecord(
            userId: String,
            resource: T,
            annotations: List<String>
    ): Single<Record<T>> = createRecord(
            userId,
            resource as Any,
            annotations
    ) as Single<Record<T>>

    @Suppress("UNCHECKED_CAST")
    override fun createRecord(
            userId: String,
            resource: DataResource,
            annotations: List<String>
    ): Single<DataRecord<DataResource>> = createRecord(
            userId,
            resource as Any,
            annotations
    ) as Single<DataRecord<DataResource>>

    @Suppress("UNCHECKED_CAST")
    override fun <T : Fhir4Resource> createRecord(
            userId: String,
            resource: T,
            annotations: List<String>
    ): Single<Fhir4Record<T>> = createRecord(
            userId,
            resource as Any,
            annotations
    ) as Single<Fhir4Record<T>>

    fun <T : Fhir3Resource> createRecords(resources: List<T>, userId: String): Single<CreateResult<T>> {
        val failedOperations: MutableList<Pair<T, D4LException>> = mutableListOf()
        return Observable
                .fromCallable { resources }
                .flatMapIterable { it }
                .flatMapSingle { resource ->
                    createRecord(userId, resource, listOf()).onErrorReturn { error ->
                        Record<T>(null, null, null).also {
                            failedOperations.add(Pair(resource, errorHandler.handleError(error)))
                            Unit
                        }
                    }
                }
                .filter { it != EMPTY_RECORD }
                .toList()
                .map { CreateResult(it, failedOperations) }
    }

    override fun deleteRecord(
            userId: String,
            recordId: String
    ): Completable = apiService.deleteRecord(alias, recordId, userId)

    fun deleteRecords(recordIds: List<String>, userId: String): Single<DeleteResult> {
        val failedDeletes: MutableList<Pair<String, D4LException>> = mutableListOf()
        return Observable
                .fromCallable { recordIds }
                .flatMapIterable { it }
                .flatMapSingle { recordId ->
                    deleteRecord(recordId, userId)
                            .doOnError { error ->
                                failedDeletes.add(
                                        Pair(recordId, errorHandler.handleError(error)))
                            }
                            .toSingleDefault(recordId)
                            .onErrorReturnItem(EMPTY_RECORD_ID)
                }
                .filter { it.isNotEmpty() }
                .toList()
                .map { DeleteResult(it, failedDeletes) }
    }

    private fun <T : Any> _fetchRecord(
            recordId: String,
            userId: String
    ): Single<BaseRecord<T>> {
        return apiService
                .fetchRecord(alias, userId, recordId)
                .map { decryptRecord<Any>(it, userId) }
                .map { assignResourceId(it) }
                .map { decryptedRecord ->
                    @Suppress("UNCHECKED_CAST")
                    recordFactory.getInstance(decryptedRecord) as BaseRecord<T>
                }
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T : Fhir3Resource> fetchFhir3Record(
            userId: String,
            recordId: String
    ): Single<Record<T>> = _fetchRecord<T>(recordId, userId) as Single<Record<T>>

    @Suppress("UNCHECKED_CAST")
    override fun <T : Fhir4Resource> fetchFhir4Record(
            userId: String,
            recordId: String
    ): Single<Fhir4Record<T>> = _fetchRecord<T>(recordId, userId) as Single<Fhir4Record<T>>

    @Suppress("UNCHECKED_CAST")
    override fun fetchDataRecord(
            userId: String,
            recordId: String
    ): Single<DataRecord<DataResource>> = _fetchRecord<DataResource>(recordId, userId) as Single<DataRecord<DataResource>>


    fun <T : Fhir3Resource> fetchFhir3Records(recordIds: List<String>, userId: String): Single<FetchResult<T>> {
        val failedFetches: MutableList<Pair<String, D4LException>> = arrayListOf()
        return Observable
                .fromCallable { recordIds }
                .flatMapIterable { it }
                .flatMapSingle { recordId ->
                    fetchFhir3Record<T>(recordId, userId)
                            .onErrorReturn { error ->
                                Record<T>(null, null, null).also {
                                    failedFetches.add(Pair(recordId, errorHandler.handleError(error)))
                                    Unit
                                }
                            }
                }
                .filter { it != EMPTY_RECORD }
                .toList()
                .map { FetchResult(it, failedFetches) }
    }

    private fun getTagsOnFetch(resourceType: Class<Any>): HashMap<String, String> {
        return if (resourceType.simpleName == "byte[]") {
            taggingService.appendAppDataTags(hashMapOf())!!
        } else {
            @Suppress("UNCHECKED_CAST")
            taggingService.getTagFromType(
                    fhirElementFactory.getFhirTypeForClass(resourceType as Class<out Fhir3Resource>)
            )
        }
    }

    private fun <T : Any> _fetchRecords(
            userId: String,
            resourceType: Class<T>,
            annotations: List<String>,
            startDate: LocalDate?,
            endDate: LocalDate?,
            pageSize: Int,
            offset: Int
    ): Single<List<BaseRecord<T>>> {
        val startTime = if (startDate != null) DATE_FORMATTER.format(startDate) else null
        val endTime = if (endDate != null) DATE_FORMATTER.format(endDate) else null
        @Suppress("UNCHECKED_CAST")
        return Observable
                .fromCallable {
                    @Suppress("UNCHECKED_CAST")
                    getTagsOnFetch(resourceType as Class<Any>)
                }
                .map { tagEncryptionService.encryptTags(it) as MutableList<String> }
                .map { tags ->
                    tags.also {
                        it.addAll(tagEncryptionService.encryptAnnotations(annotations))
                    }
                }
                .flatMap {
                    apiService.fetchRecords(
                            alias,
                            userId,
                            startTime,
                            endTime,
                            pageSize,
                            offset,
                            it
                    )
                }
                .flatMapIterable { it }
                .map { decryptRecord<Any>(it, userId) }
                .let {
                    if (resourceType.simpleName == "byte[]") {
                        it
                    } else {
                        it
                                .filter { decryptedRecord -> resourceType.isAssignableFrom(decryptedRecord.resource::class.java) }
                                .filter { decryptedRecord -> decryptedRecord.annotations.containsAll(annotations) }

                    }
                }
                .map { assignResourceId(it) }
                .map { recordFactory.getInstance(it) }
                .toList() as Single<List<BaseRecord<T>>>
    }

    fun <T : Fhir3Resource> fetchFhir3Records(
            userId: String,
            resourceType: Class<T>,
            startDate: LocalDate?,
            endDate: LocalDate?,
            pageSize: Int,
            offset: Int
    ): Single<List<Record<T>>> = fetchFhir3Records(
            userId,
            resourceType,
            emptyList(),
            startDate,
            endDate,
            pageSize,
            offset
    )

    @Suppress("UNCHECKED_CAST")
    override fun <T : Fhir3Resource> fetchFhir3Records(
            userId: String,
            resourceType: Class<T>,
            annotations: List<String>,
            startDate: LocalDate?,
            endDate: LocalDate?,
            pageSize: Int,
            offset: Int
    ): Single<List<Record<T>>> = _fetchRecords(
            userId,
            resourceType,
            annotations,
            startDate,
            endDate,
            pageSize,
            offset
    ) as Single<List<Record<T>>>

    @Suppress("UNCHECKED_CAST")
    override fun <T : Fhir4Resource> fetchFhir4Records(
            userId: String,
            resourceType: Class<T>,
            annotations: List<String>,
            startDate: LocalDate?,
            endDate: LocalDate?,
            pageSize: Int,
            offset: Int
    ): Single<List<Fhir4Record<T>>> = _fetchRecords(
            userId,
            resourceType,
            annotations,
            startDate,
            endDate,
            pageSize,
            offset
    ) as Single<List<Fhir4Record<T>>>

    @Suppress("UNCHECKED_CAST")
    override fun fetchDataRecords(
            userId: String,
            annotations: List<String>,
            startDate: LocalDate?,
            endDate: LocalDate?,
            pageSize: Int,
            offset: Int
    ): Single<List<DataRecord<DataResource>>> = _fetchRecords(
            userId,
            ByteArray::class.java,
            annotations,
            startDate,
            endDate,
            pageSize,
            offset
    ) as Single<List<DataRecord<DataResource>>>

    fun <T : Fhir3Resource> downloadRecord(
            recordId: String,
            userId: String
    ): Single<Record<T>> = apiService
            .fetchRecord(alias, userId, recordId)
            .map { encryptedRecord ->
                @Suppress("UNCHECKED_CAST")
                decryptRecord<Fhir3Resource>(encryptedRecord, userId) as NetworkRecordsContract.DecryptedFhir3Record<T>
            }
            .map { downloadData(it, userId) }
            .map { decryptedRecord ->
                decryptedRecord.also {
                    checkDataRestrictions(decryptedRecord.resource)
                }
            }
            .map { assignResourceId(it) }
            .map { decryptedRecord ->
                @Suppress("UNCHECKED_CAST")
                recordFactory.getInstance(decryptedRecord) as Record<T>
            }

    fun <T : Fhir3Resource> downloadRecords(
            recordIds: List<String>,
            userId: String
    ): Single<DownloadResult<T>> {
        val failedDownloads: MutableList<Pair<String, D4LException>> = arrayListOf()
        return Observable
                .fromCallable { recordIds }
                .flatMapIterable { it }
                .flatMapSingle { recordId ->
                    downloadRecord<T>(recordId, userId)
                            .onErrorReturn { error ->
                                failedDownloads.add(Pair(recordId, errorHandler.handleError(error)))
                                Record(null, null, null)
                            }
                }
                .filter { it != EMPTY_RECORD }
                .toList()
                .map { DownloadResult(it, failedDownloads) }
    }

    @Throws(DataRestrictionException.UnsupportedFileType::class,
            DataRestrictionException.MaxDataSizeViolation::class)
    private fun <T : Any> updateRecord(
            userId: String,
            recordId: String,
            resource: T,
            annotations: List<String>
    ): Single<BaseRecord<T>> {
        checkDataRestrictions(resource)
        val data = extractUploadData(resource)

        @Suppress("UNCHECKED_CAST")
        return apiService
                .fetchRecord(alias, userId, recordId)
                .map { decryptRecord<Any>(it, userId) }
                .map { updateData(it, resource, userId) }
                .map { decryptedRecord ->
                    if (decryptedRecord is NetworkRecordsContract.DecryptedFhir3Record) {
                        cleanObsoleteAdditionalIdentifiers(resource as Fhir3Resource)
                    }

                    decryptedRecord.also {
                        (it as NetworkRecordsContract.DecryptedRecord<T>).resource = resource
                        it.annotations = annotations
                    }

                }
                .map { removeUploadData(it) }
                .map { encryptRecord(it) }
                .flatMap { apiService.updateRecord(alias, userId, recordId, it) }
                .map { decryptRecord<Any>(it, userId) }
                .map { restoreUploadData(it, resource, data) }
                .map { assignResourceId(it) }
                .map { recordFactory.getInstance(it) } as Single<BaseRecord<T>>
    }

    @Suppress("UNCHECKED_CAST")
    @Throws(DataRestrictionException.UnsupportedFileType::class,
            DataRestrictionException.MaxDataSizeViolation::class)
    override fun <T : Fhir3Resource> updateRecord(
            userId: String,
            recordId: String,
            resource: T,
            annotations: List<String>
    ): Single<Record<T>> = updateRecord(
            userId,
            recordId,
            resource as Any,
            annotations
    ) as Single<Record<T>>

    @Suppress("UNCHECKED_CAST")
    override fun <T : Fhir4Resource> updateRecord(
            userId: String,
            recordId: String,
            resource: T,
            annotations: List<String>
    ): Single<Fhir4Record<T>> = updateRecord(
            userId,
            recordId,
            resource as Any,
            annotations
    ) as Single<Fhir4Record<T>>

    @Suppress("UNCHECKED_CAST")
    override fun updateRecord(
            userId: String,
            recordId: String,
            resource: DataResource,
            annotations: List<String>
    ): Single<DataRecord<DataResource>> = updateRecord(
            userId,
            recordId,
            resource as Any,
            annotations
    ) as Single<DataRecord<DataResource>>

    fun <T : Fhir3Resource> updateRecords(resources: List<T>, userId: String): Single<UpdateResult<T>> {
        val failedUpdates: MutableList<Pair<T, D4LException>> = arrayListOf()
        return Observable
                .fromCallable { resources }
                .flatMapIterable { it }
                .flatMapSingle { resource ->
                    updateRecord(userId, resource.id!!, resource, listOf())
                            .onErrorReturn { error ->
                                Record<T>(null, null, null).also {
                                    failedUpdates.add(Pair(resource, errorHandler.handleError(error)))
                                    Unit
                                }
                            }
                }
                .filter { it != EMPTY_RECORD }
                .toList()
                .map { UpdateResult(it, failedUpdates) }
    }

    @JvmOverloads
    fun countRecords(
            type: Class<out Fhir3Resource>?,
            userId: String,
            annotations: List<String> = listOf()
    ): Single<Int> = if (type == null) {
        apiService.getCount(alias, userId, null)
    } else {
        Single
                .fromCallable { taggingService.getTagFromType(fhirElementFactory.getFhirTypeForClass(type)) }
                .map { tagEncryptionService.encryptTags(it) as MutableList<String> }
                .map { tags -> tags.also { it.addAll(tagEncryptionService.encryptAnnotations(annotations)) } }
                .flatMap { apiService.getCount(alias, userId, it) }
    }

    //region utility methods
    private fun <T> getEncryptedResourceAndAttachment(
            record: NetworkRecordsContract.DecryptedRecord<T>,
            commonKey: GCKey
    ): Pair<String, EncryptedKey?> {
        return if (record is NetworkRecordsContract.DecryptedDataRecord) {
            Pair(
                    encodeToString(
                            cryptoService.encrypt(record.dataKey!!, record.resource).blockingGet()
                    ),
                    null
            )
        } else {
            Pair(
                    fhirService.encryptResource(record.dataKey!!, record.resource as Fhir3Resource),
                    if (record.attachmentsKey == null) {
                        null
                    } else {
                        cryptoService.encryptSymmetricKey(
                                commonKey,
                                KeyType.ATTACHMENT_KEY,
                                record.attachmentsKey!!
                        ).blockingGet()
                    }
            )
        }
    }

    @Throws(IOException::class)
    internal fun <T> encryptRecord(
            record: NetworkRecordsContract.DecryptedRecord<T>
    ): EncryptedRecord {
        val encryptedTags = tagEncryptionService.encryptTags(record.tags!!).also {
            (it as MutableList<String>).addAll(tagEncryptionService.encryptAnnotations(record.annotations))
        }

        val commonKey = cryptoService.fetchCurrentCommonKey()
        val currentCommonKeyId = cryptoService.currentCommonKeyId
        val encryptedDataKey = cryptoService.encryptSymmetricKey(
                commonKey,
                KeyType.DATA_KEY,
                record.dataKey!!
        ).blockingGet()

        val (encryptedResource, encryptedAttachmentsKey) = getEncryptedResourceAndAttachment(
                record,
                commonKey
        )

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

    private fun getCommonKey(commonKeyId: String, userId: String): GCKey {
        val commonKeyStored = cryptoService.hasCommonKey(commonKeyId)
        return if (commonKeyStored) {
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
    }

    private fun <T : Any> decryptRecord(
            record: EncryptedRecord,
            builder: NetworkRecordsContract.Builder
    ): NetworkRecordsContract.DecryptedRecord<T> {
        @Suppress("UNCHECKED_CAST")
        return when {
            record.encryptedBody == null || record.encryptedBody.isEmpty() -> builder.build(null)
            builder.tags!!.containsKey(TaggingService.TAG_RESOURCE_TYPE) -> builder.build(
                    fhirService.decryptResource<Fhir3Resource>(
                            builder.dataKey!!,
                            builder.tags!![TaggingService.TAG_RESOURCE_TYPE]!!,
                            record.encryptedBody
                    )
            )
            else -> builder.build(decode(record.encryptedBody).let {
                cryptoService.decrypt(builder.dataKey!!, it).blockingGet()
            })
        } as NetworkRecordsContract.DecryptedRecord<T>
    }

    @Suppress("UNCHECKED_CAST")
    @Throws(IOException::class, DataValidationException.ModelVersionNotSupported::class)
    internal fun <T : Any> decryptRecord(
            record: EncryptedRecord,
            userId: String
    ): NetworkRecordsContract.DecryptedRecord<T> {
        if (!ModelVersion.isModelVersionSupported(record.modelVersion)) {
            throw DataValidationException.ModelVersionNotSupported("Please update SDK to latest version!")
        }

        val builder = DecryptedRecordBuilder()
                .setIdentifier(record.identifier)
                .setTags(
                        tagEncryptionService.decryptTags(record.encryptedTags)
                )
                .setAnnotations(
                        tagEncryptionService.decryptAnnotations(record.encryptedTags)
                )
                .setCreationDate(record.customCreationDate)
                .setUpdateDate(record.updatedDate)
                .setModelVersion(record.modelVersion)

        val commonKeyId = record.commonKeyId
        val commonKey: GCKey = getCommonKey(commonKeyId, userId)

        builder.setDataKey(
                cryptoService.symDecryptSymmetricKey(commonKey, record.encryptedDataKey).blockingGet()
        )

        if (record.encryptedAttachmentsKey != null) {
            builder.setAttachmentKey(
                    cryptoService.symDecryptSymmetricKey(
                            commonKey,
                            record.encryptedAttachmentsKey
                    ).blockingGet()
            )
        }

        return decryptRecord(record, builder)
    }

    fun downloadAttachment(
            recordId: String,
            attachmentId: String,
            userId: String,
            type: DownloadType
    ): Single<WrappersContract.Attachment> = downloadAttachments(
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
    ): Single<List<WrappersContract.Attachment>> = apiService
            .fetchRecord(alias, userId, recordId)
            .map { decryptRecord<Fhir3Resource>(it, userId) }
            .flatMap {
                downloadAttachmentsFromStorage(
                        attachmentIds,
                        userId,
                        type,
                        it as NetworkRecordsContract.DecryptedFhir3Record<Fhir3Resource>
                )
            }

    @Throws(DataValidationException.IdUsageViolation::class,
            DataValidationException.InvalidAttachmentPayloadHash::class)
    private fun downloadAttachmentsFromStorage(
            attachmentIds: List<String>,
            userId: String,
            type: DownloadType,
            decryptedRecord: NetworkRecordsContract.DecryptedFhir3Record<Fhir3Resource>
    ): Single<out List<WrappersContract.Attachment>> {
        if (fhirAttachmentHelper.hasAttachment(decryptedRecord.resource)) {
            val resource = decryptedRecord.resource
            val attachments = fhirAttachmentHelper.getAttachment(resource)
            val validAttachments = mutableListOf<WrappersContract.Attachment>()

            for (rawAttachment in attachments) {
                val attachment = attachmentFactory.wrap(rawAttachment)
                if (attachmentIds.contains(attachment?.id)) {
                    validAttachments.add(attachment!!)
                }
            }
            if (validAttachments.size != attachmentIds.size)
                throw DataValidationException.IdUsageViolation("Please provide correct attachment ids!")

            setAttachmentIdForDownloadType(
                    validAttachments,
                    fhirAttachmentHelper.getIdentifier(resource),
                    type
            )

            return attachmentService.download(
                    validAttachments,
                    // FIXME this is forced
                    decryptedRecord.attachmentsKey!!,
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
    ): Single<Boolean> = attachmentService.delete(attachmentId, userId)

    fun <T : Any> extractUploadData(resource: T): HashMap<WrappersContract.Attachment, String?>? {
        return if (resource !is ByteArray) {
            val attachments = fhirAttachmentHelper.getAttachment(resource)
            if (attachments.isEmpty()) return null

            val data = HashMap<WrappersContract.Attachment, String?>(attachments.size)
            for (rawAttachment in attachments) {
                val attachment = attachmentFactory.wrap(rawAttachment)
                if (attachment?.data != null) {
                    data[attachment] = attachment.data
                }
            }

            if (data.isEmpty()) null else data
        } else {
            null
        }
    }

    private fun <T : Any> setUploadData(
            record: NetworkRecordsContract.DecryptedRecord<T>,
            attachmentData: HashMap<Any, String?>?
    ): NetworkRecordsContract.DecryptedRecord<T> = record.also {
        val attachments = fhirAttachmentHelper.getAttachment(record.resource)
        if (attachments.isNotEmpty()) {
            @Suppress("UNCHECKED_CAST")
            fhirAttachmentHelper.updateAttachmentData(record.resource, attachmentData)
        }
    }

    internal fun <T : Any> removeUploadData(
            record: NetworkRecordsContract.DecryptedRecord<T>
    ): NetworkRecordsContract.DecryptedRecord<T> {
        @Suppress("UNCHECKED_CAST")
        return if (record !is NetworkRecordsContract.DecryptedDataRecord) {
            setUploadData(
                    record,
                    null
            )
        } else {
            record
        }
    }

    internal fun <T : Any> restoreUploadData(
            record: NetworkRecordsContract.DecryptedRecord<T>,
            originalResource: T?,
            attachmentData: HashMap<WrappersContract.Attachment, String?>?
    ): NetworkRecordsContract.DecryptedRecord<T> {
        if (record is NetworkRecordsContract.DecryptedDataRecord || originalResource == null || originalResource is ByteArray) {
            return record
        }

        record.resource = originalResource

        @Suppress("UNCHECKED_CAST")
        return if (attachmentData == null) {
            record
        } else {
            setUploadData(
                    record as NetworkRecordsContract.DecryptedFhir3Record<Fhir3Resource>,
                    attachmentData as HashMap<Any, String?>
            ) as NetworkRecordsContract.DecryptedRecord<T>
        }
    }

    @Deprecated("")
    internal fun <T : Fhir3Resource> removeOrRestoreUploadData(
            operation: RemoveRestoreOperation,
            record: NetworkRecordsContract.DecryptedFhir3Record<T>,
            originalResource: T?,
            attachmentData: HashMap<WrappersContract.Attachment, String?>?
    ): NetworkRecordsContract.DecryptedFhir3Record<T> {
        return if (operation == RemoveRestoreOperation.RESTORE) {
            restoreUploadData(record as NetworkRecordsContract.DecryptedRecord<T>, originalResource, attachmentData) as NetworkRecordsContract.DecryptedFhir3Record<T>
        } else {
            removeUploadData(record as NetworkRecordsContract.DecryptedRecord<T>) as NetworkRecordsContract.DecryptedFhir3Record<T>
        }
    }

    @Throws(DataValidationException.IdUsageViolation::class,
            DataValidationException.ExpectedFieldViolation::class,
            DataValidationException.InvalidAttachmentPayloadHash::class)
    internal fun <T : Any> _uploadData(
            record: NetworkRecordsContract.DecryptedRecord<T>,
            userId: String
    ): NetworkRecordsContract.DecryptedRecord<T> {
        if (record !is NetworkRecordsContract.DecryptedFhir3Record) {
            return record
        }

        val resource: Fhir3Resource = record.resource as Fhir3Resource

        if (!fhirAttachmentHelper.hasAttachment(resource)) return record
        val attachments = fhirAttachmentHelper.getAttachment(resource)

        if (record.attachmentsKey == null) {
            record.attachmentsKey = cryptoService.generateGCKey().blockingGet()
        }

        val validAttachments: MutableList<WrappersContract.Attachment> = arrayListOf()
        for (rawAttachment in attachments) {
            val attachment = attachmentFactory.wrap(rawAttachment)

            when {
                attachment == null -> {/* do nothing*/}
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

        if (validAttachments.isNotEmpty()) {
            updateResourceIdentifier(
                    resource,
                    attachmentService.upload(
                            validAttachments,
                            // FIXME this is forced
                            record.attachmentsKey!!,
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
    internal fun <T : Any> updateData(
            record: NetworkRecordsContract.DecryptedRecord<T>,
            newResource: T?,
            userId: String?
    ): NetworkRecordsContract.DecryptedRecord<T> {
        if (record !is NetworkRecordsContract.DecryptedFhir3Record) {
            return record
        }

        if (newResource !is Fhir3Resource) {
            throw CoreRuntimeException.UnsupportedOperation()
        }

        var resource = record.resource as Fhir3Resource
        if (!fhirAttachmentHelper.hasAttachment(resource)) return record
        val attachments = fhirAttachmentHelper.getAttachment(resource)

        val validAttachments: MutableList<WrappersContract.Attachment> = arrayListOf()
        val oldAttachments: HashMap<String?, WrappersContract.Attachment> = hashMapOf()

        for (rawAttachment in attachments) {
            val attachment = attachmentFactory.wrap(rawAttachment)

            if (attachment?.id != null) {
                oldAttachments[attachment.id] = attachment
            }
        }

        resource = newResource
        val newAttachments = fhirAttachmentHelper.getAttachment(newResource)
        for (rawNewAttachment in newAttachments) {
            val newAttachment = attachmentFactory.wrap(rawNewAttachment)

            when {
                newAttachment == null -> {/* do nothing*/}
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
        if (validAttachments.isNotEmpty()) {
            updateResourceIdentifier(
                    resource,
                    attachmentService.upload(
                            validAttachments,
                            // FIXME this is forced
                            record.attachmentsKey!!,
                            // FIXME this is forced
                            userId!!
                    ).blockingGet()
            )
        }
        return record
    }

    @Throws(DataValidationException.IdUsageViolation::class, DataValidationException.InvalidAttachmentPayloadHash::class)
    internal fun <T : Any> downloadData(
            record: NetworkRecordsContract.DecryptedRecord<T>,
            userId: String?
    ): NetworkRecordsContract.DecryptedRecord<T> {
        if (record is NetworkRecordsContract.DecryptedDataRecord) {
            return record
        }

        val resource = record.resource

        if (!fhirAttachmentHelper.hasAttachment(resource)) return record

        val attachments = fhirAttachmentHelper.getAttachment(resource)
        val wrappedAttachments = mutableListOf<WrappersContract.Attachment>()

        for(rawAttachment in attachments) {
            val attachment = attachmentFactory.wrap(rawAttachment)
            attachment?.id ?: throw DataValidationException.IdUsageViolation("Attachment.id expected")

            wrappedAttachments.add(attachment)
        }

        @Suppress("CheckResult")
        attachmentService.download(
                wrappedAttachments,
                // FIXME this is forced
                record.attachmentsKey!!,
                // FIXME this is forced
                userId!!
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
    internal fun <T : Any> uploadData(
            record: NetworkRecordsContract.DecryptedRecord<T>,
            newResource: T?,
            userId: String
    ): NetworkRecordsContract.DecryptedRecord<T> {
        return if (newResource == null) {
            _uploadData(record, userId)
        } else {
            updateData(record, newResource, userId)
        }
    }

    @Deprecated("")
    @Throws(DataValidationException.IdUsageViolation::class,
            DataValidationException.ExpectedFieldViolation::class,
            DataValidationException.InvalidAttachmentPayloadHash::class,
            CoreRuntimeException.UnsupportedOperation::class)
    internal fun <T : Fhir3Resource> uploadOrDownloadData(
            operation: UploadDownloadOperation,
            record: NetworkRecordsContract.DecryptedFhir3Record<T>,
            newResource: T?,
            userId: String
    ): NetworkRecordsContract.DecryptedFhir3Record<T> {
        return when (operation) {
            UploadDownloadOperation.UPDATE -> updateData(record, newResource!!, userId) as NetworkRecordsContract.DecryptedFhir3Record
            UploadDownloadOperation.UPLOAD -> _uploadData(record, userId) as NetworkRecordsContract.DecryptedFhir3Record<T>
            UploadDownloadOperation.DOWNLOAD -> downloadData(record, userId) as NetworkRecordsContract.DecryptedFhir3Record<T>
        }
    }

    private fun updateResourceIdentifier(
            d: Any,
            result: List<Pair<WrappersContract.Attachment, List<String>?>>
    ) {
        val sb = StringBuilder()
        for ((first, second) in result) {
            if (second != null) { //Attachment is a of image type
                sb.setLength(0)
                sb.append(DOWNSCALED_ATTACHMENT_IDS_FMT).append(ThumbnailService.SPLIT_CHAR).append(first.id)
                for (additionalId in second) {
                    sb.append(ThumbnailService.SPLIT_CHAR).append(additionalId)
                }
                fhirAttachmentHelper.appendIdentifier(d, sb.toString(), partnerId)
            }
        }
    }

    // TODO move to AttachmentService -> Thumbnail handling
    @Throws(DataValidationException.IdUsageViolation::class)
    fun setAttachmentIdForDownloadType(
            attachments: List<WrappersContract.Attachment>,
            identifiers: List<Any>?,
            type: DownloadType?
    ) {
        for (attachment in attachments) {
            val additionalIds = extractAdditionalAttachmentIds(identifiers, attachment.id)
            if (additionalIds != null) {
                when (type) {
                    DownloadType.Full -> {
                    }
                    DownloadType.Medium -> attachment.id += ThumbnailService.SPLIT_CHAR + additionalIds[PREVIEW_ID_POS]
                    DownloadType.Small -> attachment.id += ThumbnailService.SPLIT_CHAR + additionalIds[THUMBNAIL_ID_POS]
                    else -> throw CoreRuntimeException.UnsupportedOperation()
                }
            }
        }
    }

    @Throws(DataValidationException.IdUsageViolation::class)
    fun extractAdditionalAttachmentIds(
            additionalIds: List<Any>?,
            attachmentId: String?
    ): Array<String>? {
        if (additionalIds == null) return null
        for (identifier in additionalIds) {
            val parts = splitAdditionalAttachmentId(identifierFactory.wrap(identifier))
            if (parts != null && parts[FULL_ATTACHMENT_ID_POS] == attachmentId) return parts
        }
        return null //Attachment is not of image type
    }

    @Throws(DataValidationException.IdUsageViolation::class)
    fun splitAdditionalAttachmentId(identifier: WrappersContract.Identifier?): Array<String>? {
        if (identifier?.value == null || !identifier.value!!.startsWith(DOWNSCALED_ATTACHMENT_IDS_FMT)) {
            return null
        }
        val parts = identifier.value!!.split(ThumbnailService.SPLIT_CHAR.toRegex()).toTypedArray()
        if (parts.size != DOWNSCALED_ATTACHMENT_IDS_SIZE) {
            throw DataValidationException.IdUsageViolation(identifier.value)
        }
        return parts
    }

    // TODO move to AttachmentService
    fun updateAttachmentMeta(attachment: WrappersContract.Attachment): WrappersContract.Attachment {
        val data = decode(attachment.data!!)
        attachment.size = data.size
        attachment.hash = encodeToString(sha1(data))
        return attachment
    }

    // TODO move to AttachmentService
    fun getValidHash(attachment: WrappersContract.Attachment): String {
        val data = decode(attachment.data!!)
        return encodeToString(sha1(data))
    }

    @Throws(DataValidationException.IdUsageViolation::class)
    fun <T : Any> cleanObsoleteAdditionalIdentifiers(resource: T?) {
        if(resource == null || resource is ByteArray) return

        val currentAttachments = fhirAttachmentHelper.getAttachment(resource)

        if (currentAttachments.isNotEmpty()) {
            val identifiers = fhirAttachmentHelper.getIdentifier(resource)
            val currentAttachmentIds: MutableList<String?> = arrayListOf(currentAttachments.size.toString())

            for(rawAttachment in currentAttachments) {
                attachmentFactory.wrap(rawAttachment).also {
                    if (it != null) {
                        currentAttachmentIds.add(it.id)
                    }
                }
            }

            val updatedIdentifiers: MutableList<Any> = mutableListOf()
            val identifierIterator = identifiers.iterator()

            while (identifierIterator.hasNext()) {
                val next = identifierIterator.next()
                val parts = splitAdditionalAttachmentId(
                        identifierFactory.wrap(next)
                )

                if (parts == null || currentAttachmentIds.contains(parts[FULL_ATTACHMENT_ID_POS])) {
                    updatedIdentifiers.add(next)
                }
            }

            fhirAttachmentHelper.setIdentifier(resource, updatedIdentifiers)
        }
    }

    @Throws(DataRestrictionException.MaxDataSizeViolation::class, DataRestrictionException.UnsupportedFileType::class)
    fun <T : Any> checkDataRestrictions(resource: T?) {
        if (resource == null || resource is ByteArray) {
            return
        }

        val attachments = fhirAttachmentHelper.getAttachment(resource)
        for (rawAttachment in attachments) {
            val attachment = attachmentFactory.wrap(rawAttachment)
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

    internal fun <T : Any> assignResourceId(
            record: NetworkRecordsContract.DecryptedRecord<T>
    ): NetworkRecordsContract.DecryptedRecord<T> {
        return record.also {
            if (record.resource is Fhir3Resource) {
                @Suppress("UNCHECKED_CAST")
                (record as NetworkRecordsContract.DecryptedFhir3Record<Fhir3Resource>).resource.id = record.identifier
            }
        }
    }

    @Deprecated("")
    internal fun buildMeta(
            record: NetworkRecordsContract.DecryptedRecord<*>
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
