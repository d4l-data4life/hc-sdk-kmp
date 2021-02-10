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
import care.data4life.sdk.fhir.Fhir3Attachment
import care.data4life.sdk.fhir.Fhir3Identifier
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
import care.data4life.sdk.model.RecordMapper
import care.data4life.sdk.model.UpdateResult
import care.data4life.sdk.model.definitions.BaseRecord
import care.data4life.sdk.model.definitions.RecordFactory
import care.data4life.sdk.network.DecryptedRecordMapper
import care.data4life.sdk.network.model.EncryptedKey
import care.data4life.sdk.network.model.NetworkModelContract
import care.data4life.sdk.network.model.EncryptedRecord
import care.data4life.sdk.network.model.definitions.DecryptedBaseRecord
import care.data4life.sdk.network.model.definitions.DecryptedFhir3Record
import care.data4life.sdk.network.model.definitions.DecryptedFhir4Record
import care.data4life.sdk.record.RecordContract
import care.data4life.sdk.tag.TagEncryptionService
import care.data4life.sdk.tag.TaggingService
import care.data4life.sdk.util.Base64.decode
import care.data4life.sdk.util.Base64.encodeToString
import care.data4life.sdk.util.HashUtil.sha1
import care.data4life.sdk.util.MimeType
import care.data4life.sdk.util.MimeType.Companion.recognizeMimeType
import care.data4life.sdk.wrapper.HelperContract
import care.data4life.sdk.wrapper.SdkAttachmentFactory
import care.data4life.sdk.wrapper.SdkFhirAttachmentHelper
import care.data4life.sdk.wrapper.SdkIdentifierFactory
import care.data4life.sdk.wrapper.WrapperContract
import care.data4life.sdk.wrapper.WrapperFactoryContract
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import org.threeten.bp.LocalDate
import org.threeten.bp.LocalDateTime
import org.threeten.bp.ZoneId
import org.threeten.bp.format.DateTimeFormatter
import org.threeten.bp.format.DateTimeFormatterBuilder
import java.io.IOException
import java.util.Locale
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
        REMOVE, RESTORE
    }

    private val recordFactory: RecordFactory = RecordMapper
    private val fhirAttachmentHelper: HelperContract.FhirAttachmentHelper = SdkFhirAttachmentHelper
    private val attchachmentFactory: WrapperFactoryContract.AttachmentFactory = SdkAttachmentFactory
    private val identifierFactory: WrapperFactoryContract.IdentifierFactory = SdkIdentifierFactory

    private fun isFhir(resource: Any?): Boolean = resource is Fhir3Resource || resource is Fhir4Resource

    private fun <T : Any> createRecord(
            userId: String,
            resource: T,
            annotations: List<String>
    ): Single<BaseRecord<T>> {
        checkDataRestrictions(resource)

        val data = extractUploadData(resource)
        val createdRecord = Single.just(
                DecryptedRecordMapper()
                        .setAnnotations(annotations)
                        .build(
                                resource,
                                taggingService.appendDefaultTags(resource, null),
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
    @JvmOverloads
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
                    createRecord(userId, resource, listOf())
                            .ignoreErrors {
                                failedOperations.add(Pair(resource, errorHandler.handleError(it)))
                            }
                }
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

    // ToDo throw error on false error
    internal fun <T : Any> _fetchRecord(
            recordId: String,
            userId: String
    ): Single<BaseRecord<T>> {
        return apiService
                .fetchRecord(alias, userId, recordId)
                .map { decryptRecord<T>(it, userId) }
                .map { assignResourceId(it) }
                .map { recordFactory.getInstance(it) }
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
                            .ignoreErrors {
                                failedFetches.add(Pair(recordId, errorHandler.handleError(it)))
                            }
                }
                .toList()
                .map { FetchResult(it, failedFetches) }
    }

    private fun encryptTagsAndAnnotations(
            painTags: HashMap<String, String>,
            plainAnnotations: List<String>
    ): List<String> {
        return tagEncryptionService.encryptTags(painTags)
                .also { encryptedTags ->
                    encryptedTags.addAll(
                            tagEncryptionService.encryptAnnotations(plainAnnotations)
                    )
                }
    }

    internal fun <T : Any> _fetchRecords(
            userId: String,
            resourceType: Class<T>?,
            annotations: List<String>,
            startDate: LocalDate?,
            endDate: LocalDate?,
            pageSize: Int,
            offset: Int
    ): Single<List<BaseRecord<T>>> {
        val startTime = if (startDate != null) DATE_FORMATTER.format(startDate) else null
        val endTime = if (endDate != null) DATE_FORMATTER.format(endDate) else null

        return Observable
                .fromCallable { taggingService.getTagFromType(resourceType as Class<Any>?) }
                .map { plainTags -> encryptTagsAndAnnotations(plainTags, annotations) }
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
                .map { decryptRecord<T>(it, userId) }
                .let {
                    if (resourceType == null) {
                        it
                    } else {
                        it.filter { decryptedRecord -> resourceType.isAssignableFrom(decryptedRecord.resource::class.java) }
                    }.filter { decryptedRecord -> decryptedRecord.annotations.containsAll(annotations) }
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
    ): Single<List<DataRecord<DataResource>>> = _fetchRecords<DataResource>(
            userId,
            null,
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
                decryptRecord<T>(encryptedRecord, userId) as DecryptedFhir3Record<T>
            }
            .map { downloadData(it, userId) }
            .map { decryptedRecord ->
                decryptedRecord.also {
                    checkDataRestrictions(decryptedRecord.resource)
                }
            }
            .map { assignResourceId(it) }
            .map { decryptedRecord ->
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
                            .ignoreErrors {
                                failedDownloads.add(Pair(recordId, errorHandler.handleError(it)))
                            }
                }
                .toList()
                .map { DownloadResult(it, failedDownloads) }
    }

    @Throws(DataRestrictionException.UnsupportedFileType::class,
            DataRestrictionException.MaxDataSizeViolation::class)
    internal fun <T : Any> updateRecord(
            userId: String,
            recordId: String,
            resource: T,
            annotations: List<String>
    ): Single<BaseRecord<T>> {
        checkDataRestrictions(resource)
        val data = extractUploadData(resource)

        return apiService
                .fetchRecord(alias, userId, recordId)
                .map { decryptRecord<T>(it, userId) } //Fixme: Resource clash
                .map { updateData(it, resource, userId) }
                .map { decryptedRecord ->
                    cleanObsoleteAdditionalIdentifiers(resource)

                    decryptedRecord.also {
                        it.resource = resource
                        if (annotations != null) {
                            it.annotations = annotations
                        }
                    }
                }
                .map { removeUploadData(it) }
                .map { encryptRecord(it) }
                .flatMap { apiService.updateRecord(alias, userId, recordId, it) }
                .map { decryptRecord<T>(it, userId) }
                .map { restoreUploadData(it, resource, data) }
                .map { assignResourceId(it) }
                .map { recordFactory.getInstance(it) }
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
    @Throws(DataRestrictionException.UnsupportedFileType::class,
            DataRestrictionException.MaxDataSizeViolation::class)
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
                            .ignoreErrors {
                                failedUpdates.add(Pair(resource, errorHandler.handleError(it)))
                            }
                }
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
                .fromCallable { taggingService.getTagFromType(type as Class<Any>?) }
                .map { plainTags -> encryptTagsAndAnnotations(plainTags, annotations) }
                .flatMap { apiService.getCount(alias, userId, it) }
    }

    //region utility methods
    @Throws(IOException::class)
    internal fun <T : Any> encryptRecord(record: DecryptedBaseRecord<T>): NetworkModelContract.EncryptedRecord {
        val encryptedTags = encryptTagsAndAnnotations(record.tags!!, record.annotations)

        val commonKey = cryptoService.fetchCurrentCommonKey()
        val currentCommonKeyId = cryptoService.currentCommonKeyId
        val encryptedDataKey = cryptoService.encryptSymmetricKey(
                commonKey,
                KeyType.DATA_KEY,
                record.dataKey!!
        ).blockingGet()

        val encryptedResource = fhirService._encryptResource(record.dataKey!!, record.resource)

        val encryptedAttachmentsKey = if (record.attachmentsKey == null) {
            null
        } else {
            cryptoService.encryptSymmetricKey(
                    commonKey,
                    KeyType.ATTACHMENT_KEY,
                    record.attachmentsKey!!
            ).blockingGet()
        }

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

    @Throws(IOException::class, DataValidationException.ModelVersionNotSupported::class)
    internal fun <T : Any> decryptRecord(
            record: NetworkModelContract.EncryptedRecord,
            userId: String
    ): DecryptedBaseRecord<T> {
        if (!ModelVersion.isModelVersionSupported(record.modelVersion)) {
            throw DataValidationException.ModelVersionNotSupported("Please update SDK to latest version!")
        }

        val tags = tagEncryptionService.decryptTags(record.encryptedTags)

        val builder = DecryptedRecordMapper()
                .setIdentifier(record.identifier)
                .setTags(tags)
                .setAnnotations(
                        tagEncryptionService.decryptAnnotations(record.encryptedTags)
                )
                .setCreationDate(record.customCreationDate)
                .setUpdateDate(record.updatedDate)
                .setModelVersion(record.modelVersion)

        val commonKeyId = record.commonKeyId
        val commonKey: GCKey = getCommonKey(commonKeyId, userId)
        val dataKey = cryptoService.symDecryptSymmetricKey(commonKey, record.encryptedDataKey).blockingGet()

        builder.setDataKey(dataKey)

        val attachmentKey = record.encryptedAttachmentsKey
        if (attachmentKey is EncryptedKey) {
            builder.setAttachmentKey(
                cryptoService.symDecryptSymmetricKey(
                    commonKey,
                    attachmentKey
                ).blockingGet()
            )
        }

        val body = record.encryptedBody
        return builder.build(
            if (body is String && body.isNotEmpty()) {
                fhirService.decryptResource<T>(
                    dataKey,
                    tags,
                    body
                )
            } else {
                null// Fixme: This is a potential bug
            }
        ) as DecryptedBaseRecord<T>
    }

    fun downloadAttachment(
            recordId: String,
            attachmentId: String,
            userId: String,
            type: DownloadType
    ): Single<Fhir3Attachment> = downloadAttachments(
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
    ): Single<List<Fhir3Attachment>> = apiService
            .fetchRecord(alias, userId, recordId)
            .map { decryptRecord<Fhir3Resource>(it, userId) }
            .flatMap {
                downloadAttachmentsFromStorage(
                        attachmentIds,
                        userId,
                        type,
                        it as DecryptedFhir3Record<Fhir3Resource>
                )
            }

    @Throws(DataValidationException.IdUsageViolation::class,
            DataValidationException.InvalidAttachmentPayloadHash::class)
    internal fun downloadAttachmentsFromStorage(
            attachmentIds: List<String>,
            userId: String,
            type: DownloadType,
            decryptedRecord: DecryptedFhir3Record<Fhir3Resource>
    ): Single<out List<Fhir3Attachment>> {
        if (fhirAttachmentHelper.hasAttachment(decryptedRecord.resource)) {
            val resource = decryptedRecord.resource
            val attachments = fhirAttachmentHelper.getAttachment(resource) as List<Fhir3Attachment?>
            val validAttachments = mutableListOf<WrapperContract.Attachment>()
            val validRawAttachments = mutableListOf<Fhir3Attachment>()

            for (attachment in attachments) {
                if (attachmentIds.contains(attachment?.id)) {
                    validRawAttachments.add(attachment!!)
                    validAttachments.add(SdkAttachmentFactory.wrap(attachment))
                }
            }

            if (validAttachments.size != attachmentIds.size)
                throw DataValidationException.IdUsageViolation("Please provide correct attachment ids!")

            setAttachmentIdForDownloadType(
                    validRawAttachments,
                    fhirAttachmentHelper.getIdentifier(resource) as List<Fhir3Identifier>?,
                    type
            )

            return attachmentService.download(
                    validAttachments,
                    // FIXME this is forced
                    decryptedRecord.attachmentsKey!!,
                    userId
            )
                    .flattenAsObservable { it }
                    .map { attachment ->
                        attachment.unwrap<Fhir3Attachment>().also {
                            if (it.id!!.contains(SPLIT_CHAR)) updateAttachmentMeta(it)
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

    fun <T : Any> extractUploadData(resource: T): HashMap<Any, String?>? {
        return if (isFhir(resource)) {
            val attachments = fhirAttachmentHelper.getAttachment(resource) as List<Any?>?
            if (attachments == null || attachments.isEmpty()) return null

            val data = HashMap<Any, String?>(attachments.size)
            for (rawAttachment in attachments) {
                if (rawAttachment != null) {
                    val attachment = attchachmentFactory.wrap(rawAttachment)

                    if (attachment.data != null) {
                        data[rawAttachment] = attachment.data
                    }
                }
            }

            if (data.isEmpty()) null else data
        } else {
            null
        }
    }

    private fun <T : Any> setUploadData(
            record: DecryptedBaseRecord<T>,
            attachmentData: HashMap<Any, String?>?
    ): DecryptedBaseRecord<T> = record.also {
        val attachments = fhirAttachmentHelper.getAttachment(record.resource)
        if (attachments != null && attachments.isNotEmpty()) {
            fhirAttachmentHelper.updateAttachmentData(record.resource, attachmentData)
        }
    }

    internal fun <T : Any> removeUploadData(
            record: DecryptedBaseRecord<T>
    ): DecryptedBaseRecord<T> {
        return if (isFhir(record.resource)) {
            setUploadData(
                    record,
                    null
            )
        } else {
            record
        }
    }

    internal fun <T : Any> restoreUploadData(
            record: DecryptedBaseRecord<T>,
            originalResource: T?,
            attachmentData: HashMap<Any, String?>?
    ): DecryptedBaseRecord<T> {
        if (!isFhir(record.resource) || originalResource == null || originalResource is DataResource) {
            return record
        }

        record.resource = originalResource

        return if (attachmentData == null) {
            record
        } else {
            setUploadData(
                    record,
                    attachmentData
            )
        }
    }

    @Deprecated("")
    internal fun <T : Fhir3Resource> removeOrRestoreUploadData(
            operation: RemoveRestoreOperation,
            record: DecryptedFhir3Record<T>,
            originalResource: T?,
            attachmentData: HashMap<Fhir3Attachment, String?>?
    ): DecryptedFhir3Record<T> {
        return if (operation == RemoveRestoreOperation.RESTORE) {
            restoreUploadData(
                    record as DecryptedBaseRecord<T>,
                    originalResource,
                    attachmentData as HashMap<Any, String?>
            ) as DecryptedFhir3Record<T>
        } else {
            removeUploadData(record as DecryptedBaseRecord<T>) as DecryptedFhir3Record<T>
        }
    }

    @Throws(DataValidationException.IdUsageViolation::class,
            DataValidationException.ExpectedFieldViolation::class,
            DataValidationException.InvalidAttachmentPayloadHash::class)
    internal fun <T : Any> _uploadData(
            record: DecryptedBaseRecord<T>,
            userId: String
    ): DecryptedBaseRecord<T> {
        if (!isFhir(record.resource)) {
            return record
        }

        val resource = record.resource

        if (!fhirAttachmentHelper.hasAttachment(resource)) return record
        val attachments = fhirAttachmentHelper.getAttachment(resource) as List<Any?>?
                ?: return record

        if (record.attachmentsKey == null) {
            record.attachmentsKey = cryptoService.generateGCKey().blockingGet()
        }

        val validAttachments: MutableList<WrapperContract.Attachment> = arrayListOf()
        for (rawAttachment in attachments) {
            if (rawAttachment != null) {

                val attachment = attchachmentFactory.wrap(rawAttachment)

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
            updateFhirResourceIdentifier(
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
            record: DecryptedBaseRecord<T>,
            newResource: T?,
            userId: String?
    ): DecryptedBaseRecord<T> {
        if (!isFhir(record.resource)) {
            return record
        }

        if (newResource == null || !isFhir(newResource)) {
            throw CoreRuntimeException.UnsupportedOperation()
        }

        var resource: T? = record.resource
        if (!fhirAttachmentHelper.hasAttachment(resource!!)) return record
        val attachments = fhirAttachmentHelper.getAttachment(resource) ?: listOf<Any>()

        val validAttachments: MutableList<WrapperContract.Attachment> = arrayListOf()
        val oldAttachments: HashMap<String?, WrapperContract.Attachment> = hashMapOf()

        for (rawAttachment in attachments) {
            if (rawAttachment != null) {
                val attachment = attchachmentFactory.wrap(rawAttachment)

                if (attachment.id != null) {
                    oldAttachments[attachment.id] = attachment
                }
            }
        }

        resource = newResource
        val newAttachments = fhirAttachmentHelper.getAttachment(newResource) ?: listOf<Any>()
        for (rawNewAttachment in newAttachments) {
            if (rawNewAttachment != null) {
                val newAttachment = attchachmentFactory.wrap(rawNewAttachment)

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
            updateFhirResourceIdentifier(
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
            record: DecryptedBaseRecord<T>,
            userId: String?
    ): DecryptedBaseRecord<T> {
        if (!isFhir(record.resource)) {
            return record
        }

        val resource = record.resource
        if (!fhirAttachmentHelper.hasAttachment(resource)) return record
        val rawAttachments = fhirAttachmentHelper.getAttachment(resource) as List<Fhir3Attachment?>?
        val attachments = mutableListOf<WrapperContract.Attachment>()

        rawAttachments ?: return record

        rawAttachments.forEach {
            it?.id ?: throw DataValidationException.IdUsageViolation("Attachment.id expected")

            attachments.add(attchachmentFactory.wrap(it))
        }

        @Suppress("CheckResult")
        attachmentService.download(
                attachments,
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
    @Deprecated("")
    @Throws(DataValidationException.IdUsageViolation::class,
            DataValidationException.ExpectedFieldViolation::class,
            DataValidationException.InvalidAttachmentPayloadHash::class)
    internal fun <T : Fhir3Resource> uploadData(
            record: DecryptedFhir3Record<T>,
            newResource: T?,
            userId: String
    ): DecryptedFhir3Record<T> {
        return if (newResource == null) {
            _uploadData(record, userId) as DecryptedFhir3Record<T>
        } else {
            updateData(record, newResource, userId) as DecryptedFhir3Record<T>
        }
    }

    @Deprecated("")
    @Throws(DataValidationException.IdUsageViolation::class,
            DataValidationException.ExpectedFieldViolation::class,
            DataValidationException.InvalidAttachmentPayloadHash::class,
            CoreRuntimeException.UnsupportedOperation::class)
    internal fun <T : Fhir3Resource> uploadOrDownloadData(
            operation: UploadDownloadOperation,
            record: DecryptedFhir3Record<T>,
            newResource: T?,
            userId: String
    ): DecryptedFhir3Record<T> {
        return when (operation) {
            UploadDownloadOperation.UPDATE -> updateData(record, newResource!!, userId) as DecryptedFhir3Record
            UploadDownloadOperation.UPLOAD -> _uploadData(record, userId) as DecryptedFhir3Record<T>
            UploadDownloadOperation.DOWNLOAD -> downloadData(record, userId) as DecryptedFhir3Record<T>
        }
    }

    /*
     *   TODO: This function makes a false assumption. It claims, that the output of the attachmentService.upload
     *    matches List<String?>?, but it is actually List<String?>. This means the claim of the former author that second (the list)
     *    is a indicator for a type is wrong.
     */
    internal fun updateFhirResourceIdentifier(
            resource: Any,
            result: List<Pair<WrapperContract.Attachment, List<String?>?>>
    ) {
        val sb = StringBuilder()
        for ((attachment, second) in result) {
            if (second != null) { //Attachment is a of image type
                sb.setLength(0)
                sb.append(DOWNSCALED_ATTACHMENT_IDS_FMT).append(ThumbnailService.SPLIT_CHAR).append(attachment.id)
                for (additionalId in second) {
                    sb.append(ThumbnailService.SPLIT_CHAR).append(additionalId)
                }
                fhirAttachmentHelper.appendIdentifier(resource, sb.toString(), partnerId)
            }
        }
    }

    // TODO move to AttachmentService -> Thumbnail handling
    @Throws(DataValidationException.IdUsageViolation::class)
    fun setAttachmentIdForDownloadType(
            attachments: List<Any>,
            identifiers: List<Any>?,
            type: DownloadType?
    ) {
        for (rawAttachment in attachments) {
            val attachment = attchachmentFactory.wrap(rawAttachment)
            val additionalIds = extractAdditionalAttachmentIds(
                    identifiers,
                    attachment.id
            )
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
    internal fun extractAdditionalAttachmentIds(
            additionalIds: List<Any>?,
            attachmentId: String?
    ): Array<String>? {
        if (additionalIds == null) return null
        for (i in additionalIds) {
            val parts = splitAdditionalAttachmentId(identifierFactory.wrap(i))
            if (parts != null && parts[FULL_ATTACHMENT_ID_POS] == attachmentId) return parts
        }
        return null //Attachment is not of image type
    }

    @Throws(DataValidationException.IdUsageViolation::class)
    internal fun splitAdditionalAttachmentId(identifier: WrapperContract.Identifier): Array<String>? {
        if (identifier.value == null || !identifier.value!!.startsWith(DOWNSCALED_ATTACHMENT_IDS_FMT)) {
            return null
        }
        val parts = identifier.value!!.split(ThumbnailService.SPLIT_CHAR.toRegex()).toTypedArray()

        if (parts.size != DOWNSCALED_ATTACHMENT_IDS_SIZE) {
            throw DataValidationException.IdUsageViolation(identifier.value)
        }
        return parts
    }

    // TODO move to AttachmentService
    fun updateAttachmentMeta(attachment: Fhir3Attachment): Fhir3Attachment {
        val data = decode(attachment.data!!)
        attachment.size = data.size
        attachment.hash = encodeToString(sha1(data))
        return attachment
    }

    // TODO move to AttachmentService
    internal fun getValidHash(attachment: WrapperContract.Attachment): String {
        val data = decode(attachment.data!!)
        return encodeToString(sha1(data))
    }

    @Throws(DataValidationException.IdUsageViolation::class)
    fun <T : Any> cleanObsoleteAdditionalIdentifiers(resource: T?) {
        if (
                isFhir(resource) &&
                fhirAttachmentHelper.hasAttachment(resource!!) &&
                fhirAttachmentHelper.getAttachment(resource)?.isNotEmpty() == true
        ) {
            val identifiers = fhirAttachmentHelper.getIdentifier(resource)
            val currentAttachments = fhirAttachmentHelper.getAttachment(resource)!!
            val currentAttachmentIds: MutableList<String> = arrayListOf(currentAttachments.size.toString())

            currentAttachments.forEach {
                if (it != null) {
                    val attachment = attchachmentFactory.wrap(it)
                    if (attachment.id != null) {
                        currentAttachmentIds.add(attachment.id!!)
                    }
                }
            }

            if (identifiers == null) return
            val updatedIdentifiers: MutableList<Any> = mutableListOf()
            val identifierIterator = identifiers.iterator()

            while (identifierIterator.hasNext()) {
                val next = identifierFactory.wrap(identifierIterator.next())
                val parts = splitAdditionalAttachmentId(next)
                if (parts == null || currentAttachmentIds.contains(parts[FULL_ATTACHMENT_ID_POS])) {
                    updatedIdentifiers.add(next.unwrap())
                }
            }
            fhirAttachmentHelper.setIdentifier(resource, updatedIdentifiers)
        }
    }

    @Throws(DataRestrictionException.MaxDataSizeViolation::class, DataRestrictionException.UnsupportedFileType::class)
    fun <T : Any> checkDataRestrictions(resource: T?) {
        if (isFhir(resource) && fhirAttachmentHelper.hasAttachment(resource!!)) {
            val attachments = fhirAttachmentHelper.getAttachment(resource) as List<Any?>? ?: return

            for (rawAttachment in attachments) {
                rawAttachment ?: return

                val attachment = attchachmentFactory.wrap(rawAttachment)

                val data = decode(attachment.data!!)
                if (recognizeMimeType(data) == MimeType.UNKNOWN) {
                    throw DataRestrictionException.UnsupportedFileType()
                }
                if (data.size > DATA_SIZE_MAX_BYTES) {
                    throw DataRestrictionException.MaxDataSizeViolation()
                }
            }
        }
    }

    internal fun <T : Any> assignResourceId(
            record: DecryptedBaseRecord<T>
    ): DecryptedBaseRecord<T> {
        return record.also {
            when (record.resource) {
                is Fhir3Resource -> (record as DecryptedFhir3Record<Fhir3Resource>).resource.id = record.identifier
                is Fhir4Resource -> (record as DecryptedFhir4Record<Fhir4Resource>).resource.id = record.identifier
            }
        }
    }

    @Deprecated("")
    internal fun buildMeta(
            record: DecryptedBaseRecord<*>
    ): Meta = Meta(
            LocalDate.parse(record.customCreationDate, DATE_FORMATTER),
            LocalDateTime.parse(record.updatedDate, DATE_TIME_FORMATTER)
    )
    //endregion

    private fun <T> Single<T>.ignoreErrors(exceptionHandler: (Throwable) -> Unit) = retryWhen { errors ->
        errors
                .doOnNext { exceptionHandler(it) }
                .map { 0 }
    }

    companion object {
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
