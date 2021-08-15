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
import care.data4life.sdk.attachment.AttachmentContract
import care.data4life.sdk.attachment.AttachmentGuardian
import care.data4life.sdk.attachment.AttachmentHasher
import care.data4life.sdk.attachment.ThumbnailService
import care.data4life.sdk.attachment.ThumbnailService.Companion.SPLIT_CHAR
import care.data4life.sdk.call.DataRecord
import care.data4life.sdk.call.Fhir4Record
import care.data4life.sdk.config.DataRestriction.DATA_SIZE_MAX_BYTES
import care.data4life.sdk.config.DataRestrictionException
import care.data4life.sdk.crypto.CryptoContract
import care.data4life.sdk.data.DataResource
import care.data4life.sdk.fhir.Fhir3Attachment
import care.data4life.sdk.fhir.Fhir3Resource
import care.data4life.sdk.fhir.Fhir4Attachment
import care.data4life.sdk.fhir.Fhir4Resource
import care.data4life.sdk.fhir.FhirContract
import care.data4life.sdk.lang.CoreRuntimeException
import care.data4life.sdk.lang.D4LException
import care.data4life.sdk.lang.DataValidationException
import care.data4life.sdk.migration.MigrationContract
import care.data4life.sdk.migration.RecordCompatibilityService
import care.data4life.sdk.model.CreateResult
import care.data4life.sdk.model.DeleteResult
import care.data4life.sdk.model.DownloadResult
import care.data4life.sdk.model.DownloadType
import care.data4life.sdk.model.FetchResult
import care.data4life.sdk.model.ModelContract.BaseRecord
import care.data4life.sdk.model.ModelInternalContract.RecordFactory
import care.data4life.sdk.model.ModelVersion
import care.data4life.sdk.model.Record
import care.data4life.sdk.model.RecordMapper
import care.data4life.sdk.model.UpdateResult
import care.data4life.sdk.network.NetworkingContract
import care.data4life.sdk.network.model.DecryptedRecordGuard
import care.data4life.sdk.network.model.NetworkModelContract
import care.data4life.sdk.network.model.NetworkModelContract.DecryptedBaseRecord
import care.data4life.sdk.network.model.NetworkModelInternalContract.DecryptedFhir3Record
import care.data4life.sdk.network.model.NetworkModelInternalContract.DecryptedFhir4Record
import care.data4life.sdk.network.model.RecordCryptoService
import care.data4life.sdk.record.RecordContract
import care.data4life.sdk.record.RecordContract.Service.Companion.DOWNSCALED_ATTACHMENT_IDS_FMT
import care.data4life.sdk.record.RecordContract.Service.Companion.DOWNSCALED_ATTACHMENT_IDS_SIZE
import care.data4life.sdk.record.RecordContract.Service.Companion.EMPTY_RECORD_ID
import care.data4life.sdk.record.RecordContract.Service.Companion.FULL_ATTACHMENT_ID_POS
import care.data4life.sdk.record.RecordContract.Service.Companion.PREVIEW_ID_POS
import care.data4life.sdk.record.RecordContract.Service.Companion.THUMBNAIL_ID_POS
import care.data4life.sdk.tag.Annotations
import care.data4life.sdk.tag.TaggingContract
import care.data4life.sdk.util.Base64.decode
import care.data4life.sdk.util.MimeType
import care.data4life.sdk.util.MimeType.Companion.recognizeMimeType
import care.data4life.sdk.wrapper.SdkAttachmentFactory
import care.data4life.sdk.wrapper.SdkDateTimeFormatter
import care.data4life.sdk.wrapper.SdkFhirAttachmentHelper
import care.data4life.sdk.wrapper.SdkIdentifierFactory
import care.data4life.sdk.wrapper.WrapperContract
import care.data4life.sdk.wrapper.WrapperInternalContract
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import org.threeten.bp.LocalDate

// TODO internal
// TODO add Factory
class RecordService internal constructor(
    private val partnerId: String,
    private val alias: String,
    private val apiService: NetworkingContract.Service,
    tagCryptoService: TaggingContract.CryptoService,
    private val taggingService: TaggingContract.Service,
    resourceCryptoService: FhirContract.CryptoService,
    private val attachmentService: AttachmentContract.Service,
    private val cryptoService: CryptoContract.Service,
    private val errorHandler: SdkContract.ErrorHandler,
    private val compatibilityService: MigrationContract.CompatibilityService
) : RecordContract.Service {

    constructor(
        partnerId: String,
        alias: String,
        apiService: NetworkingContract.Service,
        tagCryptoService: TaggingContract.CryptoService,
        taggingService: TaggingContract.Service,
        resourceCryptoService: FhirContract.CryptoService,
        attachmentService: AttachmentContract.Service,
        cryptoService: CryptoContract.Service,
        errorHandler: SdkContract.ErrorHandler
    ) : this(
        partnerId,
        alias,
        apiService,
        tagCryptoService,
        taggingService,
        resourceCryptoService,
        attachmentService,
        cryptoService,
        errorHandler,
        RecordCompatibilityService(cryptoService, tagCryptoService)
    )

    private val recordCryptoService: NetworkModelContract.CryptoService = RecordCryptoService(
        alias,
        apiService,
        taggingService,
        tagCryptoService,
        DecryptedRecordGuard,
        cryptoService,
        resourceCryptoService,
        SdkDateTimeFormatter,
        ModelVersion
    )
    private val recordFactory: RecordFactory = RecordMapper
    private val fhirAttachmentHelper: WrapperInternalContract.FhirAttachmentHelper = SdkFhirAttachmentHelper
    private val attachmentFactory: WrapperInternalContract.AttachmentFactory = SdkAttachmentFactory
    private val identifierFactory: WrapperInternalContract.IdentifierFactory = SdkIdentifierFactory
    private val dateTimeFormatter: WrapperContract.DateTimeFormatter = SdkDateTimeFormatter
    private val attachmentGuardian: AttachmentContract.Guardian = AttachmentGuardian
    private val attachmentHash: AttachmentContract.Hasher = AttachmentHasher

    private fun isFhir3(resource: Any?): Boolean = resource is Fhir3Resource
    private fun isFhir4(resource: Any?): Boolean = resource is Fhir4Resource
    private fun isFhir(resource: Any?): Boolean = isFhir3(resource) || isFhir4(resource)

    private fun <T : Any> createRecord(
        userId: String,
        resource: T,
        annotations: Annotations
    ): Single<BaseRecord<T>> {
        checkDataRestrictions(resource)

        val data = extractUploadData(resource)

        return fromResource(resource, annotations)
            .map { createdRecord -> uploadData(createdRecord, userId) }
            .map { createdRecord -> removeUploadData(createdRecord) }
            .map { createdRecord -> encryptRecord(createdRecord) }
            .flatMap { encryptedRecord -> apiService.createRecord(alias, userId, encryptedRecord) }
            .map { encryptedRecord -> decryptRecord<T>(encryptedRecord, userId) }
            .map { receivedRecord -> restoreUploadData(receivedRecord, resource, data) }
            .map { receivedRecord -> assignResourceId(receivedRecord) }
            .map { receivedRecord -> recordFactory.getInstance(receivedRecord) }
    }

    @Suppress("UNCHECKED_CAST")
    @Throws(
        DataRestrictionException.UnsupportedFileType::class,
        DataRestrictionException.MaxDataSizeViolation::class
    )
    override fun <T : Fhir3Resource> createRecord(
        userId: String,
        resource: T,
        annotations: Annotations
    ): Single<Record<T>> = createRecord(
        userId,
        resource as Any,
        annotations
    ) as Single<Record<T>>

    @Suppress("UNCHECKED_CAST")
    override fun createRecord(
        userId: String,
        resource: DataResource,
        annotations: Annotations
    ): Single<DataRecord<DataResource>> = createRecord(
        userId,
        resource as Any,
        annotations
    ) as Single<DataRecord<DataResource>>

    @Suppress("UNCHECKED_CAST")
    override fun <T : Fhir4Resource> createRecord(
        userId: String,
        resource: T,
        annotations: Annotations
    ): Single<Fhir4Record<T>> = createRecord(
        userId,
        resource as Any,
        annotations
    ) as Single<Fhir4Record<T>>

    fun <T : Fhir3Resource> createRecords(
        resources: List<T>,
        userId: String
    ): Single<CreateResult<T>> {
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
    ): Completable = apiService.deleteRecord(alias, userId, recordId)

    fun deleteRecords(recordIds: List<String>, userId: String): Single<DeleteResult> {
        val failedDeletes: MutableList<Pair<String, D4LException>> = mutableListOf()
        return Observable
            .fromCallable { recordIds }
            .flatMapIterable { it }
            .flatMapSingle { recordId ->
                deleteRecord(userId, recordId)
                    .doOnError { error ->
                        failedDeletes.add(
                            Pair(recordId, errorHandler.handleError(error))
                        )
                    }
                    .toSingleDefault(recordId)
                    .onErrorReturnItem(EMPTY_RECORD_ID)
            }
            .filter { it.isNotEmpty() }
            .toList()
            .map { DeleteResult(it, failedDeletes) }
    }

    // ToDo throw error on false error
    private fun <T : Any> _fetchRecord(
        recordId: String,
        userId: String
    ): Single<BaseRecord<T>> {
        return apiService
            .fetchRecord(alias, userId, recordId)
            .map { encryptedRecord -> decryptRecord<T>(encryptedRecord, userId) }
            .map { decryptedRecord -> assignResourceId(decryptedRecord) }
            .map { decryptedRecord -> recordFactory.getInstance(decryptedRecord) }
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
    ): Single<DataRecord<DataResource>> =
        _fetchRecord<DataResource>(recordId, userId) as Single<DataRecord<DataResource>>

    fun <T : Fhir3Resource> fetchFhir3Records(
        recordIds: List<String>,
        userId: String
    ): Single<FetchResult<T>> {
        val failedFetches: MutableList<Pair<String, D4LException>> = arrayListOf()
        return Observable
            .fromCallable { recordIds }
            .flatMapIterable { it }
            .flatMapSingle { recordId ->
                fetchFhir3Record<T>(
                    userId = userId,
                    recordId = recordId
                ).ignoreErrors {
                    failedFetches.add(Pair(recordId, errorHandler.handleError(it)))
                }
            }
            .toList()
            .map { FetchResult(it, failedFetches) }
    }

    private fun <T : Any> searchRecords(
        userId: String,
        resourceType: Class<T>,
        annotations: Annotations,
        startDate: LocalDate?,
        endDate: LocalDate?,
        pageSize: Int,
        offset: Int
    ): Single<List<BaseRecord<T>>> {
        val startTime = if (startDate != null) dateTimeFormatter.formatDate(startDate) else null
        val endTime = if (endDate != null) dateTimeFormatter.formatDate(endDate) else null

        return Observable
            .fromCallable {
                compatibilityService.resolveSearchTags(
                    taggingService.getTagsFromType(resourceType),
                    annotations
                )
            }
            .flatMap { tags ->
                apiService.searchRecords(
                    alias,
                    userId,
                    startTime,
                    endTime,
                    null,
                    null,
                    false,
                    pageSize,
                    offset,
                    tags
                )
            }
            .flatMapIterable { it }
            .map { encryptedRecord -> decryptRecord<T>(encryptedRecord, userId) }
            .map { decryptedRecord -> assignResourceId(decryptedRecord) }
            .map { decryptedRecord -> recordFactory.getInstance(decryptedRecord) }
            .toList()
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
        annotations: Annotations,
        startDate: LocalDate?,
        endDate: LocalDate?,
        pageSize: Int,
        offset: Int,
        timeSearchParameter: SdkContract.TimeSearchParameter?
    ): Single<List<Record<T>>> = searchRecords(
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
        annotations: Annotations,
        startDate: LocalDate?,
        endDate: LocalDate?,
        pageSize: Int,
        offset: Int,
        timeSearchParameter: SdkContract.TimeSearchParameter?
    ): Single<List<Fhir4Record<T>>> = searchRecords(
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
        annotations: Annotations,
        startDate: LocalDate?,
        endDate: LocalDate?,
        pageSize: Int,
        offset: Int,
        timeSearchParameter: SdkContract.TimeSearchParameter?
    ): Single<List<DataRecord<DataResource>>> = searchRecords(
        userId,
        DataResource::class.java,
        annotations,
        startDate,
        endDate,
        pageSize,
        offset
    ) as Single<List<DataRecord<DataResource>>>

    @Throws(
        DataRestrictionException.UnsupportedFileType::class,
        DataRestrictionException.MaxDataSizeViolation::class
    )
    internal fun <T : Any> updateRecord(
        userId: String,
        recordId: String,
        resource: T,
        annotations: Annotations
    ): Single<BaseRecord<T>> {
        checkDataRestrictions(resource)
        val data = extractUploadData(resource)

        return apiService
            .fetchRecord(alias, userId, recordId)
            .map { fetchedRecord ->
                decryptRecord<T>(
                    fetchedRecord,
                    userId
                )
            } // Fixme: Resource clash
            .map { decryptedRecord -> updateData(decryptedRecord, resource, userId) }
            .map { decryptedRecord ->
                cleanObsoleteAdditionalIdentifiers(resource)

                decryptedRecord.also {
                    it.resource = resource
                    it.annotations = annotations
                }
            }
            .map { decryptedRecord -> removeUploadData(decryptedRecord) }
            .map { decryptedRecord -> encryptRecord(decryptedRecord) }
            .flatMap { encryptedRecord ->
                apiService.updateRecord(
                    alias,
                    userId,
                    recordId,
                    encryptedRecord
                )
            }
            .map { encryptedRecord -> decryptRecord<T>(encryptedRecord, userId) }
            .map { decryptedRecord -> restoreUploadData(decryptedRecord, resource, data) }
            .map { decryptedRecord -> assignResourceId(decryptedRecord) }
            .map { decryptedRecord -> recordFactory.getInstance(decryptedRecord) }
    }

    @Suppress("UNCHECKED_CAST")
    @Throws(
        DataRestrictionException.UnsupportedFileType::class,
        DataRestrictionException.MaxDataSizeViolation::class
    )
    override fun <T : Fhir3Resource> updateRecord(
        userId: String,
        recordId: String,
        resource: T,
        annotations: Annotations
    ): Single<Record<T>> = updateRecord(
        userId,
        recordId,
        resource as Any,
        annotations
    ) as Single<Record<T>>

    @Suppress("UNCHECKED_CAST")
    @Throws(
        DataRestrictionException.UnsupportedFileType::class,
        DataRestrictionException.MaxDataSizeViolation::class
    )
    override fun <T : Fhir4Resource> updateRecord(
        userId: String,
        recordId: String,
        resource: T,
        annotations: Annotations
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
        annotations: Annotations
    ): Single<DataRecord<DataResource>> = updateRecord(
        userId,
        recordId,
        resource as Any,
        annotations
    ) as Single<DataRecord<DataResource>>

    fun <T : Fhir3Resource> updateRecords(
        resources: List<T>,
        userId: String
    ): Single<UpdateResult<T>> {
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

    private fun _countRecords(
        type: Class<out Any>,
        userId: String,
        annotations: Annotations
    ): Single<Int> {
        val searchTags = compatibilityService.resolveSearchTags(
            taggingService.getTagsFromType(type),
            annotations
        )

        return apiService.countRecords(
            alias,
            userId,
            searchTags
        )
    }

    @JvmOverloads
    @Deprecated("Deprecated with version v1.9.0 and will be removed in version v2.0.0")
    fun countRecords(
        type: Class<out Fhir3Resource>?,
        userId: String,
        annotations: Annotations = listOf()
    ): Single<Int> {
        return if (type == null) {
            countAllFhir3Records(userId, annotations)
        } else {
            countFhir3Records(type, userId, annotations)
        }
    }

    override fun countFhir3Records(
        type: Class<out Fhir3Resource>,
        userId: String,
        annotations: Annotations
    ): Single<Int> = _countRecords(type, userId, annotations)

    override fun countFhir4Records(
        type: Class<out Fhir4Resource>,
        userId: String,
        annotations: Annotations
    ): Single<Int> = _countRecords(type, userId, annotations)

    override fun countAllFhir3Records(
        userId: String,
        annotations: Annotations
    ): Single<Int> = countFhir3Records(Fhir3Resource::class.java, userId, annotations)

    override fun countDataRecords(
        type: Class<out DataResource>,
        userId: String,
        annotations: Annotations
    ): Single<Int> = _countRecords(type, userId, annotations)

    override fun <T : Fhir3Resource> downloadFhir3Record(
        recordId: String,
        userId: String
    ): Single<Record<T>> = downloadRecord<T>(
        recordId,
        userId,
        ::isFhir3
    ) as Single<Record<T>>

    override fun <T : Fhir4Resource> downloadFhir4Record(
        recordId: String,
        userId: String
    ): Single<Fhir4Record<T>> = downloadRecord<T>(
        recordId,
        userId,
        ::isFhir4
    ) as Single<Fhir4Record<T>>

    private fun <T : Any> downloadRecord(
        recordId: String,
        userId: String,
        resourceBarrier: (resource: Any) -> Boolean
    ): Single<BaseRecord<T>> = apiService
        .fetchRecord(alias, userId, recordId)
        .map { encryptedRecord -> decryptRecord<T>(encryptedRecord, userId) }
        .map { decryptedRecord -> failOnResourceInconsistency(decryptedRecord, resourceBarrier) }
        .map { decryptedRecord -> downloadData(decryptedRecord, userId) }
        .map { decryptedRecord ->
            decryptedRecord.also {
                checkDataRestrictions(decryptedRecord.resource)
            }
        }
        .map { decryptedRecord -> assignResourceId(decryptedRecord) }
        .map { decryptedRecord -> recordFactory.getInstance(decryptedRecord) }

    fun <T : Fhir3Resource> downloadRecords(
        recordIds: List<String>,
        userId: String
    ): Single<DownloadResult<T>> {
        val failedDownloads: MutableList<Pair<String, D4LException>> = arrayListOf()
        return Observable
            .fromCallable { recordIds }
            .flatMapIterable { it }
            .flatMapSingle { recordId ->
                downloadFhir3Record<T>(recordId, userId)
                    .ignoreErrors {
                        failedDownloads.add(Pair(recordId, errorHandler.handleError(it)))
                    }
            }
            .toList()
            .map { records -> DownloadResult(records as MutableList<Record<T>>?, failedDownloads) }
    }

    override fun downloadFhir3Attachment(
        recordId: String,
        attachmentId: String,
        userId: String,
        type: DownloadType
    ): Single<Fhir3Attachment> = downloadFhir3Attachments(
        recordId,
        listOf(attachmentId),
        userId,
        type
    ).map { it[0] }

    override fun downloadFhir4Attachment(
        recordId: String,
        attachmentId: String,
        userId: String,
        type: DownloadType
    ): Single<Fhir4Attachment> = downloadFhir4Attachments(
        recordId,
        listOf(attachmentId),
        userId,
        type
    ).map { it[0] }

    @Throws(IllegalArgumentException::class)
    override fun downloadFhir3Attachments(
        recordId: String,
        attachmentIds: List<String>,
        userId: String,
        type: DownloadType
    ): Single<List<Fhir3Attachment>> = downloadAttachments<Fhir3Resource, Fhir3Attachment>(
        recordId,
        attachmentIds,
        userId,
        type,
        ::isFhir3
    )

    @Throws(IllegalArgumentException::class)
    override fun downloadFhir4Attachments(
        recordId: String,
        attachmentIds: List<String>,
        userId: String,
        type: DownloadType
    ): Single<List<Fhir4Attachment>> = downloadAttachments<Fhir4Resource, Fhir4Attachment>(
        recordId,
        attachmentIds,
        userId,
        type,
        ::isFhir4
    )

    @Throws(IllegalArgumentException::class)
    private fun <T : Any, R : Any> downloadAttachments(
        recordId: String,
        attachmentIds: List<String>,
        userId: String,
        type: DownloadType,
        resourceBarrier: (resource: Any) -> Boolean
    ): Single<List<R>> = apiService
        .fetchRecord(alias, userId, recordId)
        .map { encryptedRecord -> decryptRecord<T>(encryptedRecord, userId) }
        .map { decryptedRecord -> failOnResourceInconsistency(decryptedRecord, resourceBarrier) }
        .flatMap { decryptedRecord ->
            downloadAttachmentsFromStorage(
                attachmentIds,
                userId,
                type,
                decryptedRecord
            )
        }

    //region utility methods
    @Throws(IllegalArgumentException::class)
    private fun <T : Any> failOnResourceInconsistency(
        record: DecryptedBaseRecord<T>,
        resourceBarrier: (resource: Any) -> Boolean
    ): DecryptedBaseRecord<T> {
        return if (resourceBarrier(record.resource)) {
            record
        } else {
            throw IllegalArgumentException("The given Record does not match the expected resource type.")
        }
    }

    @Deprecated("This is a test concern and should be removed once a proper DI/SL is in place.")
    internal fun <T : Any> fromResource(
        resource: T,
        annotations: Annotations
    ): Single<DecryptedBaseRecord<T>> = Single.just(
        recordCryptoService.fromResource(resource, annotations)
    )

    @Deprecated("This is a test concern and should be removed once a proper DI/SL is in place.")
    internal fun <T : Any> encryptRecord(
        record: DecryptedBaseRecord<T>
    ): NetworkModelContract.EncryptedRecord = recordCryptoService.encrypt(record)

    @Deprecated("This is a test concern and should be removed once a proper DI/SL is in place.")
    internal fun <T : Any> decryptRecord(
        record: NetworkModelContract.EncryptedRecord,
        userId: String
    ): DecryptedBaseRecord<T> = recordCryptoService.decrypt(record, userId)

    @Throws(
        DataValidationException.IdUsageViolation::class,
        DataValidationException.InvalidAttachmentPayloadHash::class
    )
    internal fun <T : Any, R : Any> downloadAttachmentsFromStorage(
        attachmentIds: List<String>,
        userId: String,
        type: DownloadType,
        decryptedRecord: DecryptedBaseRecord<T>
    ): Single<List<R>> {
        if (fhirAttachmentHelper.hasAttachment(decryptedRecord.resource)) {
            val resource = decryptedRecord.resource
            val attachments = fhirAttachmentHelper.getAttachment(resource) ?: emptyList<R?>()
            val validAttachments = mutableListOf<WrapperContract.Attachment>()

            for (rawAttachment in attachments) {
                if (rawAttachment != null) {
                    val attachment = SdkAttachmentFactory.wrap(rawAttachment)
                    if (attachmentIds.contains(attachment.id)) {
                        validAttachments.add(attachment)
                    }
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
                .flattenAsObservable { attachment -> attachment }
                .map {
                    attachment ->
                    if (attachment.id!!.contains(SPLIT_CHAR)) {
                        updateAttachmentMeta(attachment)
                    } else {
                        attachment
                    }
                }
                .map { attachment -> attachment.unwrap<R>() }
                .toList()
        }

        throw IllegalArgumentException("Expected a record of a type that has attachment")
    }

    fun deleteAttachment(
        attachmentId: String,
        userId: String
    ): Single<Boolean> = attachmentService.delete(attachmentId, userId)

    private fun isFhirWithPossibleAttachments(
        resource: Any
    ): Boolean = isFhir(resource) && fhirAttachmentHelper.hasAttachment(resource)

    fun <T : Any> extractUploadData(resource: T): HashMap<Any, String?>? {
        return if (isFhirWithPossibleAttachments(resource)) {
            val attachments = fhirAttachmentHelper.getAttachment(resource)
                ?: return null

            val data = HashMap<Any, String?>(attachments.size)
            for (rawAttachment in attachments) {
                if (rawAttachment != null) {
                    val attachment = attachmentFactory.wrap(rawAttachment)

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
        return if (isFhirWithPossibleAttachments(record.resource)) {
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
        originalResource: T,
        attachmentData: HashMap<Any, String?>? // TODO: Match Any as Attachment against Fhir version
    ): DecryptedBaseRecord<T> {
        if (!isFhir(record.resource) || !isFhir(originalResource)) {
            return record
        }

        // ToDo: This should not be done here
        record.resource = originalResource

        return if (attachmentData == null || !fhirAttachmentHelper.hasAttachment(record.resource)) {
            record
        } else {
            setUploadData(
                record,
                attachmentData
            )
        }
    }

    private fun <T : Any> resolveAttachmentKey(record: DecryptedBaseRecord<T>): GCKey {
        if (record.attachmentsKey !is GCKey) {
            record.attachmentsKey = cryptoService.generateGCKey().blockingGet()
        }

        return record.attachmentsKey!!
    }

    private fun <T : Any> uploadAttachmentsOnDemand(
        record: DecryptedBaseRecord<T>,
        resource: T,
        attachments: List<WrapperContract.Attachment>,
        userId: String
    ) {
        if (attachments.isNotEmpty()) {
            updateFhirResourceIdentifier(
                resource,
                attachmentService.upload(
                    attachments,
                    resolveAttachmentKey(record),
                    userId
                ).blockingGet()
            )
        }
    }

    private fun determineUploadableAttachment(
        rawAttachments: List<Any?>,
        validAttachments: MutableList<WrapperContract.Attachment>
    ) {
        for (rawAttachment in rawAttachments) {
            if (rawAttachment != null) {

                val attachment = attachmentFactory.wrap(rawAttachment)
                attachmentGuardian.guardId(attachment)
                attachmentGuardian.guardSize(attachment)
                attachmentGuardian.guardHash(attachment)

                validAttachments.add(attachment)
            }
        }
    }

    @Throws(
        DataValidationException.IdUsageViolation::class,
        DataValidationException.ExpectedFieldViolation::class,
        DataValidationException.InvalidAttachmentPayloadHash::class
    )
    internal fun <T : Any> uploadData(
        record: DecryptedBaseRecord<T>,
        userId: String
    ): DecryptedBaseRecord<T> {
        if (!isFhir(record.resource)) {
            return record
        }

        val resource = record.resource

        if (!fhirAttachmentHelper.hasAttachment(resource)) return record
        val attachments = fhirAttachmentHelper.getAttachment(resource)
            ?: return record

        val validAttachments: MutableList<WrapperContract.Attachment> = arrayListOf()

        determineUploadableAttachment(attachments, validAttachments)
        uploadAttachmentsOnDemand(record, resource, validAttachments, userId)
        return record
    }

    private fun determineUpdateableAttachment(
        rawNewAttachments: List<Any?>,
        oldAttachments: MutableMap<String, WrapperContract.Attachment>,
        validAttachments: MutableList<WrapperContract.Attachment>
    ) {
        for (rawNewAttachment in rawNewAttachments) {
            if (rawNewAttachment != null) {
                val newAttachment = attachmentFactory.wrap(rawNewAttachment)
                attachmentGuardian.guardSize(newAttachment)

                val oldAttachment = if (newAttachment.id == null) {
                    null
                } else {
                    attachmentGuardian.guardIdAgainstExistingIds(newAttachment, oldAttachments.keys)
                    oldAttachments[newAttachment.id]
                }

                if (attachmentGuardian.guardHash(newAttachment, oldAttachment)) {
                    validAttachments.add(newAttachment)
                }
            }
        }
    }

    @Throws(
        DataValidationException.IdUsageViolation::class,
        DataValidationException.ExpectedFieldViolation::class,
        DataValidationException.InvalidAttachmentPayloadHash::class,
        CoreRuntimeException.UnsupportedOperation::class
    )
    internal fun <T : Any> updateData(
        record: DecryptedBaseRecord<T>,
        newResource: T,
        userId: String
    ): DecryptedBaseRecord<T> {
        if (!isFhir(record.resource)) {
            return record
        }

        if (!isFhir(newResource)) {
            throw CoreRuntimeException.UnsupportedOperation()
        }

        if (!fhirAttachmentHelper.hasAttachment(record.resource)) return record
        val attachments = fhirAttachmentHelper.getAttachment(record.resource) ?: listOf<Any>()

        val validAttachments: MutableList<WrapperContract.Attachment> = mutableListOf()
        val oldAttachments: MutableMap<String, WrapperContract.Attachment> = mutableMapOf()

        for (rawAttachment in attachments) {
            if (rawAttachment != null) {
                val attachment = attachmentFactory.wrap(rawAttachment)

                if (attachment.id != null) {
                    oldAttachments[attachment.id!!] = attachment
                }
            }
        }

        determineUpdateableAttachment(
            fhirAttachmentHelper.getAttachment(newResource) ?: listOf<Any>(),
            oldAttachments,
            validAttachments
        )

        uploadAttachmentsOnDemand(record, newResource, validAttachments, userId)
        return record
    }

    @Throws(
        DataValidationException.IdUsageViolation::class,
        DataValidationException.InvalidAttachmentPayloadHash::class
    )
    internal fun <T : Any> downloadData(
        record: DecryptedBaseRecord<T>,
        userId: String
    ): DecryptedBaseRecord<T> {
        if (!isFhir(record.resource)) {
            return record
        }

        val resource = record.resource
        if (!fhirAttachmentHelper.hasAttachment(resource)) return record
        val rawAttachments = fhirAttachmentHelper.getAttachment(resource)
            ?: return record

        val attachments = mutableListOf<WrapperContract.Attachment>()

        for (rawAttachment in rawAttachments) {
            if (rawAttachment != null) {
                val attachment = attachmentFactory.wrap(rawAttachment)

                attachmentGuardian.guardNonNullId(attachment)

                attachments.add(attachment)
            }
        }

        if (attachments.isNotEmpty()) {
            @Suppress("CheckResult")
            attachmentService.download(
                attachments,
                resolveAttachmentKey(record),
                userId
            ).blockingGet()
        }

        return record
    }

    @Throws(DataValidationException.IdUsageViolation::class)
    fun <T : Any> cleanObsoleteAdditionalIdentifiers(resource: T) {
        if (isFhirWithPossibleAttachments(resource)) {
            val currentAttachments = fhirAttachmentHelper.getAttachment(resource)
            if (currentAttachments !is List<*> || currentAttachments.isEmpty()) {
                return
            }

            val identifiers = fhirAttachmentHelper.getIdentifier(resource) ?: return
            val currentAttachmentIds = mutableListOf<String>()

            currentAttachments.forEach {
                if (it != null) {
                    val attachment = attachmentFactory.wrap(it)
                    if (attachment.id != null) {
                        currentAttachmentIds.add(attachment.id!!)
                    }
                }
            }

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

    internal fun updateFhirResourceIdentifier(
        resource: Any,
        result: List<Pair<WrapperContract.Attachment, List<String?>?>>
    ) {
        val sb = StringBuilder()
        for ((attachment, second) in result) {
            if (second != null) { // Attachment is a of image type
                sb.setLength(0)
                sb.append(DOWNSCALED_ATTACHMENT_IDS_FMT)
                    .append(ThumbnailService.SPLIT_CHAR)
                    .append(attachment.id)

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
        attachments: List<WrapperContract.Attachment>,
        identifiers: List<Any>?,
        type: DownloadType
    ) {
        for (attachment in attachments) {
            val additionalIds = extractAdditionalAttachmentIds(
                identifiers,
                attachment.id
            )
            if (additionalIds is List<*>) {
                when (type) {
                    DownloadType.Full -> { /* do nothing */
                    }
                    DownloadType.Medium -> attachment.id += ThumbnailService.SPLIT_CHAR + additionalIds[PREVIEW_ID_POS]
                    DownloadType.Small -> attachment.id += ThumbnailService.SPLIT_CHAR + additionalIds[THUMBNAIL_ID_POS]
                }
            }
        }
    }

    @Throws(DataValidationException.IdUsageViolation::class)
    internal fun extractAdditionalAttachmentIds(
        additionalIds: List<Any>?,
        attachmentId: String?
    ): List<String>? {
        if (additionalIds == null) return null

        for (id in additionalIds) {
            val parts = splitAdditionalAttachmentId(identifierFactory.wrap(id))
            if (parts != null && parts[FULL_ATTACHMENT_ID_POS] == attachmentId) return parts
        }

        return null // Attachment is not of image type
    }

    // TODO: Move to AttachmentService
    @Throws(
        DataRestrictionException.MaxDataSizeViolation::class,
        DataRestrictionException.UnsupportedFileType::class
    )
    fun <T : Any> checkDataRestrictions(resource: T) {
        if (isFhirWithPossibleAttachments(resource)) {
            val attachments = fhirAttachmentHelper.getAttachment(resource)
                ?: return

            for (rawAttachment in attachments) {
                rawAttachment ?: continue

                val attachment = attachmentFactory.wrap(rawAttachment)
                if (attachment.data is String) {
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
    }

    @Throws(DataValidationException.IdUsageViolation::class)
    internal fun splitAdditionalAttachmentId(identifier: WrapperInternalContract.Identifier): List<String>? {
        if (identifier.value == null || !identifier.value!!.startsWith(DOWNSCALED_ATTACHMENT_IDS_FMT)) {
            return null
        }
        val parts = identifier.value!!.split(ThumbnailService.SPLIT_CHAR.toRegex())

        if (parts.size != DOWNSCALED_ATTACHMENT_IDS_SIZE) {
            throw DataValidationException.IdUsageViolation(identifier.value)
        }
        return parts
    }

    // TODO move to AttachmentService
    fun updateAttachmentMeta(attachment: WrapperContract.Attachment): WrapperContract.Attachment {
        val data = decode(attachment.data!!)
        attachment.size = data.size
        attachment.hash = attachmentHash.hash(data)
        return attachment
    }

    // TODO: make it private
    private fun <T : Any> assignResourceId(
        record: DecryptedBaseRecord<T>
    ): DecryptedBaseRecord<T> {
        return record.also {
            when (record.resource) {
                is Fhir3Resource -> (record as DecryptedFhir3Record<Fhir3Resource>).resource.id =
                    record.identifier
                is Fhir4Resource -> (record as DecryptedFhir4Record<Fhir4Resource>).resource.id =
                    record.identifier
            }
        }
    }
    //endregion

    private fun <T> Single<T>.ignoreErrors(exceptionHandler: (Throwable) -> Unit) =
        retryWhen { errors ->
            errors
                .doOnNext { exceptionHandler(it) }
                .map { 0 }
        }
}
