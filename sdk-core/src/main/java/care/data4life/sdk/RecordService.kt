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


import care.data4life.sdk.attachment.AttachmentContract
import care.data4life.sdk.attachment.ThumbnailContract
import care.data4life.sdk.call.DataRecord
import care.data4life.sdk.call.Fhir4Record
import care.data4life.sdk.config.DataRestrictionException
import care.data4life.sdk.data.DataResource
import care.data4life.sdk.fhir.Fhir3Resource
import care.data4life.sdk.fhir.Fhir4Resource
import care.data4life.sdk.lang.D4LException
import care.data4life.sdk.model.CreateResult
import care.data4life.sdk.model.DeleteResult
import care.data4life.sdk.model.DownloadResult
import care.data4life.sdk.model.DownloadType
import care.data4life.sdk.model.FetchResult
import care.data4life.sdk.model.ModelVersion
import care.data4life.sdk.model.Record
import care.data4life.sdk.wrapper.FhirElementFactory
import care.data4life.sdk.model.SdkRecordFactory
import care.data4life.sdk.model.UpdateResult
import care.data4life.sdk.model.definitions.BaseRecord
import care.data4life.sdk.model.definitions.RecordFactory
import care.data4life.sdk.network.DecryptedRecordBuilder
import care.data4life.sdk.record.RecordContract
import care.data4life.sdk.record.RecordEncryptionContract
import care.data4life.sdk.tag.TagEncryptionService
import care.data4life.sdk.tag.TaggingService
import care.data4life.sdk.wrapper.HelperContract
import care.data4life.sdk.wrapper.ResourceFactory
import care.data4life.sdk.wrapper.ResourceHelper
import care.data4life.sdk.wrapper.WrapperFactoryContract
import care.data4life.sdk.wrapper.WrapperContract
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import org.threeten.bp.LocalDate
import org.threeten.bp.ZoneId
import org.threeten.bp.format.DateTimeFormatter
import java.util.*
import kotlin.collections.HashMap


// TODO internal
class RecordService(
        private val alias: String,
        private val apiService: ApiService,
        private val tagEncryptionService: TagEncryptionService,
        private val taggingService: TaggingService,
        private val attachmentService: AttachmentContract.Service,
        private val attachmentClient: AttachmentContract.Client,
        private val thumbnailService: ThumbnailContract.Service,
        private val cryptoService: CryptoService,
        private val recordCryptoService: RecordEncryptionContract.Service,
        private val errorHandler: SdkContract.ErrorHandler
) : RecordContract.Service {

    private val recordFactory: RecordFactory = SdkRecordFactory
    private val fhirElementFactory: WrapperFactoryContract.FhirElementFactory = FhirElementFactory
    private val resourceWrapperFactory: WrapperFactoryContract.ResourceFactory = ResourceFactory
    private val resourceHelper: HelperContract.ResourceHelper = ResourceHelper

    //ToDo: This should not be here -> the taggingService should take of it
    private fun getTagsOnCreate(resource: WrapperContract.Resource): HashMap<String, String> {
        return if (resource.type == WrapperContract.Resource.TYPE.DATA) {
            taggingService.appendDefaultTags(null, null)
        } else {
            taggingService.appendDefaultTags((resource.unwrap() as Fhir3Resource).resourceType, null)
        }
    }

    fun createRecord(
            userId: String,
            resource: WrapperContract.Resource,
            annotations: List<String>
    ): Single<BaseRecord<Any>> {
        attachmentService.checkDataRestrictions(resource)
        val data = attachmentService.extractUploadData(resource)
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
                .map { attachmentClient.uploadData(it, userId) }
                .map { attachmentClient.removeUploadData(it) }
                .map { recordCryptoService.encryptRecord(it) }
                .flatMap { apiService.createRecord(alias, userId, it) }
                .map { recordCryptoService.decryptRecord(it, userId) }
                .map { attachmentClient.restoreUploadData(it, resource, data) }
                .map { resourceHelper.assignResourceId(it) }
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
            resourceWrapperFactory.wrap(resource)!!,
            annotations
    ) as Single<Record<T>>

    @Suppress("UNCHECKED_CAST")
    override fun createRecord(
            userId: String,
            resource: DataResource,
            annotations: List<String>
    ): Single<DataRecord<DataResource>> = createRecord(
            userId,
            resourceWrapperFactory.wrap(resource)!!,
            annotations
    ) as Single<DataRecord<DataResource>>

    @Suppress("UNCHECKED_CAST")
    override fun <T : Fhir4Resource> createRecord(
            userId: String,
            resource: T,
            annotations: List<String>
    ): Single<Fhir4Record<T>> = createRecord(
            userId,
            resourceWrapperFactory.wrap(resource)!!,
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

    fun _fetchRecord(
            recordId: String,
            userId: String
    ): Single<BaseRecord<Any>> {
        return apiService
                .fetchRecord(alias, userId, recordId)
                .map { recordCryptoService.decryptRecord(it, userId) }
                .map { resourceHelper.assignResourceId(it) }
                .map { recordFactory.getInstance(it) }
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T : Fhir3Resource> fetchFhir3Record(
            userId: String,
            recordId: String
    ): Single<Record<T>> = _fetchRecord(recordId, userId) as Single<Record<T>>

    @Suppress("UNCHECKED_CAST")
    override fun <T : Fhir4Resource> fetchFhir4Record(
            userId: String,
            recordId: String
    ): Single<Fhir4Record<T>> = _fetchRecord(recordId, userId) as Single<Fhir4Record<T>>

    @Suppress("UNCHECKED_CAST")
    override fun fetchDataRecord(
            userId: String,
            recordId: String
    ): Single<DataRecord<DataResource>> = _fetchRecord(recordId, userId) as Single<DataRecord<DataResource>>


    fun <T : Fhir3Resource> fetchFhir3Records(recordIds: List<String>, userId: String): Single<FetchResult<T>> {
        val failedFetches: MutableList<Pair<String, D4LException>> = arrayListOf()
        return Observable
                .fromCallable { recordIds }
                .flatMapIterable { it }
                .flatMapSingle { recordId ->
                    fetchFhir3Record<T>(userId, recordId)
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

    // ToDo: this should go somewhere else
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

    fun _fetchRecords(
            userId: String,
            resourceType: Class<Any>,
            annotations: List<String>,
            startDate: LocalDate?,
            endDate: LocalDate?,
            pageSize: Int,
            offset: Int
    ): Single<List<BaseRecord<Any>>> {
        val startTime = if (startDate != null) DATE_FORMATTER.format(startDate) else null
        val endTime = if (endDate != null) DATE_FORMATTER.format(endDate) else null

        return Observable
                .fromCallable { getTagsOnFetch(resourceType) }
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
                .map { recordCryptoService.decryptRecord(it, userId) }
                .let {
                    if (resourceType.simpleName == "byte[]") {
                        it
                    } else {
                        it
                                .filter { decryptedRecord -> resourceType.isAssignableFrom(decryptedRecord.resource::class.java) }
                                .filter { decryptedRecord -> decryptedRecord.annotations.containsAll(annotations) }

                    }
                }
                .map { resourceHelper.assignResourceId(it) }
                .map { recordFactory.getInstance(it) }
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
            annotations: List<String>,
            startDate: LocalDate?,
            endDate: LocalDate?,
            pageSize: Int,
            offset: Int
    ): Single<List<Record<T>>> = _fetchRecords(
            userId,
            resourceType as Class<Any>,
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
            resourceType as Class<Any>,
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
            ByteArray::class.java as Class<Any>,
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
            .map { recordCryptoService.decryptRecord(it, userId) }
            .map { attachmentClient.downloadData(it, userId) }
            .map { decryptedRecord ->
                decryptedRecord.also {
                    attachmentService.checkDataRestrictions(decryptedRecord.resource)
                }
            }
            .map { resourceHelper.assignResourceId(it) }
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
    fun updateRecord(
            userId: String,
            recordId: String,
            resource: WrapperContract.Resource,
            annotations: List<String>
    ): Single<BaseRecord<Any>> {
        attachmentService.checkDataRestrictions(resource)
        val data = attachmentService.extractUploadData(resource)

        return apiService
                .fetchRecord(alias, userId, recordId)
                .map { recordCryptoService.decryptRecord(it, userId) }
                .map { attachmentClient.updateData(it, resource, userId) }
                .map { decryptedRecord ->
                    if (decryptedRecord.resource.type != WrapperContract.Resource.TYPE.DATA) {
                        thumbnailService.cleanObsoleteAdditionalIdentifiers(resource)
                    }

                    decryptedRecord.also {
                        it.resource = resource
                        it.annotations = annotations
                    }

                }
                .map { attachmentClient.removeUploadData(it) }
                .map { recordCryptoService.encryptRecord(it) }
                .flatMap { apiService.updateRecord(alias, userId, recordId, it) }
                .map { recordCryptoService.decryptRecord(it, userId) }
                .map { attachmentClient.restoreUploadData(it, resource, data) }
                .map { resourceHelper.assignResourceId(it) }
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
            resourceWrapperFactory.wrap(resource)!!,
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
            resourceWrapperFactory.wrap(resource)!!,
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
            resourceWrapperFactory.wrap(resource)!!,
            annotations
    ) as Single<DataRecord<DataResource>>

    fun <T : Fhir3Resource> updateRecords(resources: List<T>, userId: String): Single<UpdateResult<T>> {
        val failedUpdates: MutableList<Pair<T, D4LException>> = arrayListOf()
        return Observable
                .fromCallable { resources }
                .flatMapIterable { it }
                .flatMapSingle { resource ->
                    //Fixme: forced id
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

    fun downloadAttachment(
            recordId: String,
            attachmentId: String,
            userId: String,
            type: DownloadType
    ): Single<WrapperContract.Attachment> = downloadAttachments(
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
    ): Single<List<WrapperContract.Attachment>> = apiService
            .fetchRecord(alias, userId, recordId)
            .map { recordCryptoService.decryptRecord(it, userId) }
            .flatMap {
                attachmentService.downloadAttachmentsFromStorage(
                        attachmentIds,
                        userId,
                        type,
                        it
                )
            }

    fun deleteAttachment(
            attachmentId: String,
            userId: String
    ): Single<Boolean> = attachmentService.delete(attachmentId, userId)

    companion object {
        private val EMPTY_RECORD = Record(null, null, null)
        private const val DATE_FORMAT = "yyyy-MM-dd"
        private const val EMPTY_RECORD_ID = ""
        private val DATE_FORMATTER = DateTimeFormatter.ofPattern(DATE_FORMAT, Locale.US)
        private val UTC_ZONE_ID = ZoneId.of("UTC")
    }
}
