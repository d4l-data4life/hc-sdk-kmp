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
import care.data4life.fhir.stu3.model.Attachment
import care.data4life.fhir.stu3.model.CarePlan
import care.data4life.fhir.stu3.model.DocumentReference
import care.data4life.fhir.stu3.model.DomainResource
import care.data4life.sdk.lang.D4LException
import care.data4life.sdk.model.Meta
import care.data4life.sdk.model.ModelVersion
import care.data4life.sdk.model.Record
import care.data4life.sdk.network.model.DecryptedRecord
import care.data4life.sdk.network.model.DecryptedAppDataRecord
import care.data4life.sdk.network.model.EncryptedKey
import care.data4life.sdk.network.model.EncryptedRecord
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import org.mockito.ArgumentMatchers
import org.mockito.InOrder
import org.mockito.Mockito
import org.threeten.bp.Clock
import org.threeten.bp.LocalDate
import org.threeten.bp.format.DateTimeFormatter
import java.util.*

abstract class RecordServiceTestBase {
    //SUT
    internal lateinit var recordService: RecordService
    internal lateinit var mockApiService: ApiService
    internal lateinit var mockTagEncryptionService: TagEncryptionService
    internal lateinit var mockTaggingService: TaggingService
    internal lateinit var mockFhirService: FhirService
    internal lateinit var mockAttachmentService: AttachmentService
    internal lateinit var mockCryptoService: CryptoService
    private lateinit var mockErrorHandler: D4LErrorHandler
    internal lateinit var mockCarePlan: CarePlan
    internal lateinit var mockAppData: ByteArray
    internal lateinit var mockDocumentReference: DocumentReference
    internal lateinit var mockTags: HashMap<String, String>
    internal lateinit var mockUploadData: HashMap<Attachment, String>
    internal lateinit var mockEncryptedTags: List<String>
    internal lateinit var mockEncryptedAnnotations: List<String>
    internal lateinit var mockDataKey: GCKey
    internal lateinit var mockCommonKey: GCKey
    internal lateinit var mockAttachmentKey: GCKey
    internal lateinit var mockEncryptedDataKey: EncryptedKey
    internal lateinit var mockEncryptedAttachmentKey: EncryptedKey
    internal lateinit var mockEncryptedRecord: EncryptedRecord
    internal lateinit var mockAnnotatedEncryptedRecord: EncryptedRecord
    internal lateinit var mockDecryptedRecord: DecryptedRecord<DomainResource>
    internal lateinit var mockAnnotatedDecryptedRecord: DecryptedRecord<DomainResource>
    internal lateinit var mockDecryptedAppDataRecord: DecryptedAppDataRecord
    internal lateinit var mockMeta: Meta
    private lateinit var mockD4LException: D4LException
    internal lateinit var mockRecord: Record<CarePlan>
    internal lateinit var inOrder: InOrder
    internal lateinit var decryptedRecordIndicator: DecryptedRecord<DomainResource>
    internal lateinit var annotatedDecryptedRecordIndicator: DecryptedRecord<DomainResource>
    
    @Suppress("UNCHECKED_CAST")
    fun init() {
        mockApiService = Mockito.mock(ApiService::class.java)
        mockTagEncryptionService = Mockito.mock(TagEncryptionService::class.java)
        mockTaggingService = Mockito.mock(TaggingService::class.java)
        mockFhirService = Mockito.mock(FhirService::class.java)
        mockAttachmentService = Mockito.mock(AttachmentService::class.java)
        mockCryptoService = Mockito.mock(CryptoService::class.java)
        mockErrorHandler = Mockito.mock(D4LErrorHandler::class.java)
        recordService = Mockito.spy(
                RecordService(
                        PARTNER_ID,
                        ALIAS,
                        mockApiService,
                        mockTagEncryptionService,
                        mockTaggingService,
                        mockFhirService,
                        mockAttachmentService,
                        mockCryptoService,
                        mockErrorHandler
                )
        )
        mockCarePlan = Mockito.mock(CarePlan::class.java)
        mockAppData = ByteArray(23)
        mockDocumentReference = Mockito.mock(DocumentReference::class.java)
        mockTags = Mockito.mock<HashMap<*, *>>(HashMap::class.java) as HashMap<String, String>
        mockUploadData = Mockito.mock<HashMap<*, *>>(HashMap::class.java) as HashMap<Attachment, String>
        mockEncryptedTags = Mockito.mock<MutableList<*>>(MutableList::class.java) as List<String>
        mockEncryptedAnnotations = Mockito.mock<MutableList<*>>(MutableList::class.java) as List<String>
        mockDataKey = Mockito.mock(GCKey::class.java)
        mockAttachmentKey = Mockito.mock(GCKey::class.java)
        mockCommonKey = Mockito.mock(GCKey::class.java)
        mockEncryptedDataKey = Mockito.mock(EncryptedKey::class.java)
        mockEncryptedAttachmentKey = Mockito.mock(EncryptedKey::class.java)
        mockEncryptedRecord = Mockito.mock(EncryptedRecord::class.java)
        mockAnnotatedEncryptedRecord = Mockito.mock(EncryptedRecord::class.java)
        mockDecryptedRecord = Mockito.mock(DecryptedRecord::class.java) as DecryptedRecord<DomainResource>
        mockAnnotatedDecryptedRecord = Mockito.mock(DecryptedRecord::class.java) as DecryptedRecord<DomainResource>
        mockDecryptedAppDataRecord = Mockito.mock(DecryptedAppDataRecord::class.java)
        mockMeta = Mockito.mock(Meta::class.java)
        mockD4LException = Mockito.mock(D4LException::class.java)
        mockRecord = Mockito.mock<Record<*>>(Record::class.java) as Record<CarePlan>

        Mockito.`when`(mockRecord.fhirResource).thenReturn(mockCarePlan)
        Mockito.`when`(mockRecord.meta).thenReturn(mockMeta)

        Mockito.`when`<HashMap<*, *>?>(mockDecryptedRecord.tags).thenReturn(mockTags)
        Mockito.`when`(mockDecryptedRecord.dataKey).thenReturn(mockDataKey)
        Mockito.`when`(mockDecryptedRecord.resource).thenReturn(mockCarePlan)
        Mockito.`when`(mockDecryptedRecord.modelVersion).thenReturn(ModelVersion.CURRENT)

        Mockito.`when`<HashMap<*, *>?>(mockAnnotatedDecryptedRecord.tags).thenReturn(mockTags)
        Mockito.`when`(mockAnnotatedDecryptedRecord.dataKey).thenReturn(mockDataKey)
        Mockito.`when`(mockAnnotatedDecryptedRecord.resource).thenReturn(mockCarePlan)
        Mockito.`when`(mockAnnotatedDecryptedRecord.modelVersion).thenReturn(ModelVersion.CURRENT)
        Mockito.`when`(mockAnnotatedDecryptedRecord.annotations).thenReturn(ANNOTATIONS)

        Mockito.`when`<HashMap<*, *>?>(mockDecryptedAppDataRecord.tags).thenReturn(mockTags)
        Mockito.`when`(mockDecryptedAppDataRecord.dataKey).thenReturn(mockDataKey)
        Mockito.`when`(mockDecryptedAppDataRecord.appData).thenReturn(mockAppData)
        Mockito.`when`(mockDecryptedAppDataRecord.identifier).thenReturn("id")
        Mockito.`when`(mockDecryptedAppDataRecord.modelVersion).thenReturn(ModelVersion.CURRENT)
        Mockito.`when`(mockDecryptedAppDataRecord.annotations).thenReturn(ANNOTATIONS)


        Mockito.`when`(mockTags[RESOURCE_TYPE]).thenReturn(CarePlan.resourceType)

        Mockito.`when`(mockEncryptedRecord.encryptedTags).thenReturn(mockEncryptedTags)
        Mockito.`when`(mockEncryptedRecord.encryptedDataKey).thenReturn(mockEncryptedDataKey)
        Mockito.`when`(mockEncryptedRecord.encryptedBody).thenReturn(ENCRYPTED_RESOURCE)
        Mockito.`when`(mockEncryptedRecord.modelVersion).thenReturn(ModelVersion.CURRENT)
        Mockito.`when`(mockEncryptedRecord.identifier).thenReturn(RECORD_ID)

        Mockito.`when`(mockAnnotatedEncryptedRecord.encryptedTags).thenReturn(mockEncryptedTags)
        Mockito.`when`(mockAnnotatedEncryptedRecord.encryptedDataKey).thenReturn(mockEncryptedDataKey)
        Mockito.`when`(mockAnnotatedEncryptedRecord.encryptedBody).thenReturn(ENCRYPTED_RESOURCE)
        Mockito.`when`(mockAnnotatedEncryptedRecord.modelVersion).thenReturn(ModelVersion.CURRENT)
        Mockito.`when`(mockAnnotatedEncryptedRecord.identifier).thenReturn(RECORD_ID)

        Mockito.`when`(mockErrorHandler.handleError(ArgumentMatchers.any(Exception::class.java))).thenReturn(mockD4LException)
        inOrder = Mockito.inOrder(
                mockApiService,
                mockTagEncryptionService,
                mockTaggingService,
                mockFhirService,
                mockAttachmentService,
                mockCryptoService,
                mockErrorHandler,
                recordService
        )

        decryptedRecordIndicator = DecryptedRecord(
                null,
                mockCarePlan as DomainResource,
                mockTags,
                listOf(),
                DATE_FORMATTER.format(LOCAL_DATE),
                null,
                mockDataKey,
                null,
                ModelVersion.CURRENT
        )

        annotatedDecryptedRecordIndicator = DecryptedRecord(
                null,
                mockCarePlan as DomainResource,
                mockTags,
                ANNOTATIONS,
                DATE_FORMATTER.format(LOCAL_DATE),
                null,
                mockDataKey,
                null,
                ModelVersion.CURRENT
        )

        mockkStatic(LocalDate::class)
        every { LocalDate.now(any() as Clock) } returns LOCAL_DATE
    }

    fun stop() {
        unmockkAll()
    }

    companion object {
        internal const val PARTNER_ID = "partnerId"
        internal const val USER_ID = "userId"
        internal const val ENCRYPTED_RESOURCE = "encryptedResource"
        internal val ENCRYPTED_APPDATA = ByteArray(42)
        internal const val RESOURCE_TYPE = "resourcetype"
        internal const val RECORD_ID = "recordId"
        internal const val ALIAS = "alias"
        internal const val DATA = "data"
        internal const val DATA_HASH = "dataHash"
        internal const val ATTACHMENT_ID = "attachmentId"
        internal const val THUMBNAIL_ID = "thumbnailId"
        internal const val PREVIEW_ID = "previewId"
        internal const val ASSIGNER = "assigner"
        internal const val ADDITIONAL_ID = RecordService.DOWNSCALED_ATTACHMENT_IDS_FMT +
                RecordService.SPLIT_CHAR +
                ATTACHMENT_ID +
                RecordService.SPLIT_CHAR +
                PREVIEW_ID +
                RecordService.SPLIT_CHAR +
                THUMBNAIL_ID
        internal val LOCAL_DATE = LocalDate.of(2001, 1, 1)
        internal val DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.US)
        internal val ANNOTATIONS = listOf("potato", "tomato", "soup")
    }
}