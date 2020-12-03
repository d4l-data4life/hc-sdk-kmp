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
import care.data4life.sdk.config.DataRestriction.DATA_SIZE_MAX_BYTES
import care.data4life.sdk.config.DataRestrictionException
import care.data4life.sdk.lang.D4LException
import care.data4life.sdk.lang.DataValidationException
import care.data4life.sdk.model.Meta
import care.data4life.sdk.model.ModelVersion
import care.data4life.sdk.model.Record
import care.data4life.sdk.network.model.DecryptedRecord
import care.data4life.sdk.network.model.EncryptedKey
import care.data4life.sdk.network.model.EncryptedRecord
import care.data4life.sdk.util.MimeType

import org.threeten.bp.Clock
import org.threeten.bp.LocalDate
import org.threeten.bp.format.DateTimeFormatter

import java.io.IOException
import java.util.*

import com.google.common.truth.Truth
import io.mockk.every
import io.mockk.mockkStatic
import io.reactivex.Single
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers
import org.mockito.InOrder
import org.mockito.Mockito

class RecordServiceCreateRecordTest {
    //SUT
    private lateinit var recordService: RecordService
    private lateinit var mockApiService: ApiService
    private lateinit var mockTagEncryptionService: TagEncryptionService
    private lateinit var mockTaggingService: TaggingService
    private lateinit var mockFhirService: FhirService
    private lateinit var mockAttachmentService: AttachmentService
    private lateinit var mockCryptoService: CryptoService
    private lateinit var mockErrorHandler: D4LErrorHandler
    private lateinit var mockCarePlan: CarePlan
    private lateinit var mockDocumentReference: DocumentReference
    private lateinit var mockTags: HashMap<String, String>
    private lateinit var mockUploadData: HashMap<Attachment, String>
    private lateinit var mockEncryptedTags: List<String>
    private lateinit var mockDataKey: GCKey
    private lateinit var mockCommonKey: GCKey
    private lateinit var mockAttachmentKey: GCKey
    private lateinit var mockEncryptedDataKey: EncryptedKey
    private lateinit var mockEncryptedRecord: EncryptedRecord
    private lateinit var mockAnnotattedEncryptedRecord: EncryptedRecord
    private lateinit var mockDecryptedRecord: DecryptedRecord<CarePlan>
    private lateinit var mockAnnotatedDecryptedRecord: DecryptedRecord<CarePlan>
    private lateinit var mockMeta: Meta
    private lateinit var mockD4LException: D4LException
    private lateinit var mockRecord: Record<CarePlan>
    private lateinit var inOrder: InOrder
    private lateinit var decryptedRecordIndicator: DecryptedRecord<DomainResource>
    private lateinit var annotatedDecryptedRecordIndicator: DecryptedRecord<DomainResource>

    @Suppress("UNCHECKED_CAST")
    @Before
    fun setup() {
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
        mockDocumentReference = Mockito.mock(DocumentReference::class.java)
        mockTags = Mockito.mock<HashMap<*, *>>(HashMap::class.java) as HashMap<String, String>
        mockUploadData = Mockito.mock<HashMap<*, *>>(HashMap::class.java) as HashMap<Attachment, String>
        mockEncryptedTags = Mockito.mock<MutableList<*>>(MutableList::class.java) as List<String>
        mockDataKey = Mockito.mock(GCKey::class.java)
        mockAttachmentKey = Mockito.mock(GCKey::class.java)
        mockCommonKey = Mockito.mock(GCKey::class.java)
        mockEncryptedDataKey = Mockito.mock(EncryptedKey::class.java)
        mockEncryptedRecord = Mockito.mock(EncryptedRecord::class.java)
        mockAnnotattedEncryptedRecord = Mockito.mock(EncryptedRecord::class.java)
        mockDecryptedRecord = Mockito.mock(DecryptedRecord::class.java) as DecryptedRecord<CarePlan>
        mockAnnotatedDecryptedRecord = Mockito.mock(DecryptedRecord::class.java) as DecryptedRecord<CarePlan>
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

        Mockito.`when`(mockTags[RESOURCE_TYPE]).thenReturn(CarePlan.resourceType)

        Mockito.`when`(mockEncryptedRecord.encryptedTags).thenReturn(mockEncryptedTags)
        Mockito.`when`(mockEncryptedRecord.encryptedDataKey).thenReturn(mockEncryptedDataKey)
        Mockito.`when`(mockEncryptedRecord.encryptedBody).thenReturn(ENCRYPTED_RESOURCE)
        Mockito.`when`(mockEncryptedRecord.modelVersion).thenReturn(ModelVersion.CURRENT)
        Mockito.`when`(mockEncryptedRecord.identifier).thenReturn(RECORD_ID)

        Mockito.`when`(mockAnnotattedEncryptedRecord.encryptedTags).thenReturn(mockEncryptedTags)
        Mockito.`when`(mockAnnotattedEncryptedRecord.encryptedDataKey).thenReturn(mockEncryptedDataKey)
        Mockito.`when`(mockAnnotattedEncryptedRecord.encryptedBody).thenReturn(ENCRYPTED_RESOURCE)
        Mockito.`when`(mockAnnotattedEncryptedRecord.modelVersion).thenReturn(ModelVersion.CURRENT)
        Mockito.`when`(mockAnnotattedEncryptedRecord.identifier).thenReturn(RECORD_ID)

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
        every { LocalDate.now( any() as Clock ) } returns LOCAL_DATE
    }

    @Test
    @Throws(
            InterruptedException::class,
            IOException::class,
            DataRestrictionException.UnsupportedFileType::class,
            DataRestrictionException.MaxDataSizeViolation::class,
            DataValidationException.ModelVersionNotSupported::class,
            DataValidationException.ExpectedFieldViolation::class,
            DataValidationException.IdUsageViolation::class,
            DataValidationException.InvalidAttachmentPayloadHash::class
    )
    fun `Given createRecord is called, returns a new Record`() {
        // Given
        Mockito.doReturn(mockUploadData).`when`(recordService).extractUploadData(mockCarePlan)
        Mockito.`when`(mockCarePlan.resourceType).thenReturn(CarePlan.resourceType)
        Mockito.`when`(mockTaggingService.appendDefaultTags(
                ArgumentMatchers.eq(CarePlan.resourceType),
                ArgumentMatchers.any<HashMap<String, String>>())
        ).thenReturn(mockTags)
        Mockito.`when`(mockCryptoService.generateGCKey()).thenReturn(Single.just(mockDataKey))
        Mockito.doReturn(mockDecryptedRecord).`when`(recordService)
                .uploadData(
                        ArgumentMatchers.eq(decryptedRecordIndicator),
                        ArgumentMatchers.eq<DomainResource?>(null),
                        ArgumentMatchers.eq(USER_ID)
                )
        Mockito.doReturn(mockDecryptedRecord).`when`(recordService)
                .removeUploadData(ArgumentMatchers.any<DecryptedRecord<DomainResource>>())
        @Suppress("UNCHECKED_CAST")
        Mockito.doReturn(mockEncryptedRecord)
                .`when`(recordService).encryptRecord<DomainResource>(
                        mockDecryptedRecord as DecryptedRecord<DomainResource>
                )
        Mockito.`when`(mockApiService.createRecord(ALIAS, USER_ID, mockEncryptedRecord))
                .thenReturn(Single.just(mockEncryptedRecord))
        Mockito.doReturn(mockDecryptedRecord)
                .`when`(recordService)
                .decryptRecord<DomainResource>(mockEncryptedRecord, USER_ID)
        Mockito.doReturn(mockDecryptedRecord)
                .`when`(recordService).restoreUploadData<CarePlan>(
                        mockDecryptedRecord,
                        mockCarePlan,
                        mockUploadData
                )
        Mockito.doReturn(mockMeta).`when`(recordService).buildMeta(mockDecryptedRecord)

        // When
        val subscriber = recordService.createRecord(mockCarePlan, USER_ID).test().await()

        // Then
        val record = subscriber
                .assertNoErrors()
                .assertComplete()
                .assertValueCount(1)
                .values()[0]
        Truth.assertThat(record.meta).isEqualTo(mockMeta)
        Truth.assertThat(record.fhirResource).isEqualTo(mockCarePlan)
        Truth.assertThat(record.annotations).isEqualTo(listOf<String>())
        inOrder.verify(mockTaggingService).appendDefaultTags(
                ArgumentMatchers.anyString(),
                ArgumentMatchers.any<HashMap<String, String>>()
        )
        inOrder.verify(mockCryptoService).generateGCKey()
        inOrder.verify(recordService).uploadData(
                ArgumentMatchers.eq(decryptedRecordIndicator),
                ArgumentMatchers.eq<DomainResource?>(null),
                ArgumentMatchers.eq(USER_ID)
        )
        inOrder.verify(recordService).removeUploadData(
                ArgumentMatchers.any<DecryptedRecord<DomainResource>>()
        )
        @Suppress("UNCHECKED_CAST")
        inOrder.verify(recordService).encryptRecord<DomainResource>(
                mockDecryptedRecord as DecryptedRecord<DomainResource>
        )
        inOrder.verify(mockApiService).createRecord(ALIAS, USER_ID, mockEncryptedRecord)
        inOrder.verify(recordService).decryptRecord<DomainResource>(mockEncryptedRecord, USER_ID)
        inOrder.verify(recordService).restoreUploadData<CarePlan>(
                mockDecryptedRecord,
                mockCarePlan,
                mockUploadData
        )
        inOrder.verify(recordService).buildMeta(mockDecryptedRecord)
        inOrder.verifyNoMoreInteractions()
    }

    @Test
    @Throws(
            InterruptedException::class,
            IOException::class,
            DataRestrictionException.UnsupportedFileType::class,
            DataRestrictionException.MaxDataSizeViolation::class,
            DataValidationException.ModelVersionNotSupported::class,
            DataValidationException.ExpectedFieldViolation::class,
            DataValidationException.IdUsageViolation::class,
            DataValidationException.InvalidAttachmentPayloadHash::class
    )
    fun `Given createRecord is called without attachment, returns a new Record`() {
        // Given
        Mockito.doReturn(null).`when`(recordService).extractUploadData(mockCarePlan)
        Mockito.`when`(mockCarePlan.resourceType).thenReturn(CarePlan.resourceType)
        Mockito.`when`(
                mockTaggingService.appendDefaultTags(
                        ArgumentMatchers.eq(CarePlan.resourceType),
                        ArgumentMatchers.any<HashMap<String, String>>()
                )
        ).thenReturn(mockTags)
        Mockito.`when`(mockCryptoService.generateGCKey())
                .thenReturn(Single.just(mockDataKey))
        Mockito.doReturn(mockDecryptedRecord)
		        .`when`(recordService)
                .uploadData(
                        ArgumentMatchers.eq(decryptedRecordIndicator),
                        ArgumentMatchers.eq<DomainResource?>(null),
                        ArgumentMatchers.eq(USER_ID)
                )
        Mockito.doReturn(mockDecryptedRecord)
                .`when`(recordService)
                .removeUploadData(ArgumentMatchers.any<DecryptedRecord<DomainResource>>())
        @Suppress("UNCHECKED_CAST")
        Mockito.doReturn(mockEncryptedRecord).`when`(recordService)
                .encryptRecord<DomainResource>(mockDecryptedRecord as DecryptedRecord<DomainResource>)
        Mockito.`when`(mockApiService.createRecord(ALIAS, USER_ID, mockEncryptedRecord))
                .thenReturn(Single.just(mockEncryptedRecord))
        Mockito.doReturn(mockDecryptedRecord)
                .`when`(recordService)
                .decryptRecord<DomainResource>(
                        mockEncryptedRecord,
                        USER_ID
                )
        Mockito.doReturn(mockDecryptedRecord)
                .`when`(recordService)
                .restoreUploadData<CarePlan>(
                        mockDecryptedRecord,
                        mockCarePlan,
                        mockUploadData
                )
        @Suppress("UNCHECKED_CAST")
        Mockito.doReturn(mockDecryptedRecord)
                .`when`(recordService)
                .uploadData<DomainResource>(
                        mockDecryptedRecord as DecryptedRecord<DomainResource>,
                        null,
                        USER_ID
                )
        Mockito.doReturn(mockMeta).`when`(recordService).buildMeta(mockDecryptedRecord)

        // When
        val subscriber = recordService.createRecord(mockCarePlan, USER_ID).test().await()

        // Then
        val record = subscriber
                .assertNoErrors()
                .assertComplete()
                .assertValueCount(1)
                .values()[0]
        Truth.assertThat(record.meta).isEqualTo(mockMeta)
        Truth.assertThat(record.fhirResource).isEqualTo(mockCarePlan)
        Truth.assertThat(record.annotations).isEqualTo(listOf<String>())
        inOrder.verify(mockTaggingService).appendDefaultTags(
                ArgumentMatchers.anyString(),
                ArgumentMatchers.any<HashMap<String, String>>()
        )
        inOrder.verify(mockCryptoService).generateGCKey()
        inOrder.verify(recordService).uploadData(
                ArgumentMatchers.eq(decryptedRecordIndicator),
                ArgumentMatchers.eq<DomainResource?>(null),
                ArgumentMatchers.eq(USER_ID)
        )
        inOrder.verify(recordService).removeUploadData(
                ArgumentMatchers.same(mockDecryptedRecord)
        )
        @Suppress("UNCHECKED_CAST")
        inOrder.verify(recordService).encryptRecord<DomainResource>(
                mockDecryptedRecord as DecryptedRecord<DomainResource>
        )
        inOrder.verify(mockApiService).createRecord(ALIAS, USER_ID, mockEncryptedRecord)
        inOrder.verify(recordService).decryptRecord<DomainResource>(mockEncryptedRecord, USER_ID)
        inOrder.verify(recordService).restoreUploadData(
                mockDecryptedRecord,
                mockCarePlan,
                null
        )
        inOrder.verify(recordService).buildMeta(mockDecryptedRecord)
        inOrder.verifyNoMoreInteractions()
    }

    @Test
    @Throws(
            InterruptedException::class,
            DataRestrictionException.UnsupportedFileType::class,
            DataRestrictionException.MaxDataSizeViolation::class
    )
    fun `Given createRecord is called with unsupported data, throws an error`() {
        // Given
        val invalidData = byteArrayOf(0x00)
        val doc = RecordServiceTest.buildDocumentReference(invalidData)

        // When
        try {
            recordService.createRecord(doc, USER_ID).test().await()
            Assert.fail("Exception expected!")
        } catch (ex: D4LException) {

            // Then
            Truth.assertThat(ex.javaClass).isEqualTo(DataRestrictionException.UnsupportedFileType::class.java)
        }
        inOrder.verify(recordService).createRecord(doc, USER_ID)
        inOrder.verify(recordService).checkDataRestrictions(doc)
        inOrder.verifyNoMoreInteractions()
    }

    @Test
    @Throws(
            InterruptedException::class,
            DataRestrictionException.UnsupportedFileType::class,
            DataRestrictionException.MaxDataSizeViolation::class
    )
    fun `Given createRecord is called with data, which exceeds the file size limitation, throws an error`() {
        // Given
        val invalidSizePdf = arrayOfNulls<Byte>(DATA_SIZE_MAX_BYTES + 1)
        System.arraycopy(
                MimeType.PDF.byteSignature()[0] as Any,
                0,
                invalidSizePdf,
                0,
                MimeType.PDF.byteSignature()[0]!!.size
        )
        val doc = RecordServiceTest.buildDocumentReference(RecordServiceTest.unboxByteArray(invalidSizePdf))

        // When
        try {
            recordService.createRecord(doc, USER_ID).test().await()
            Assert.fail("Exception expected!")
        } catch (ex: D4LException) {

            // Then
            Truth.assertThat(ex.javaClass).isEqualTo(DataRestrictionException.MaxDataSizeViolation::class.java)
        }
        inOrder.verify(recordService).createRecord(doc, USER_ID)
        inOrder.verify(recordService).checkDataRestrictions(doc)
        inOrder.verifyNoMoreInteractions()
    }

    @Test
    @Throws(
            InterruptedException::class,
            DataRestrictionException.UnsupportedFileType::class,
            DataRestrictionException.MaxDataSizeViolation::class
    )
    fun `Given createRecords is called, it returns a multiple Records`() {
        // Given
        val resources: List<DomainResource> = listOf(
                mockCarePlan as DomainResource,
                mockCarePlan as DomainResource
        )
        Mockito.doReturn(Single.just(mockRecord)).`when`(recordService).createRecord(mockCarePlan, USER_ID)

        // When
        val observer = recordService.createRecords(resources, USER_ID).test().await()

        // Then
        val result = observer
                .assertNoErrors()
                .assertComplete()
                .assertValueCount(1)
                .values()[0]
        Truth.assertThat(result.failedOperations).isEmpty()
        Truth.assertThat(result.successfulOperations).hasSize(2)
        Truth.assertThat(result.successfulOperations[0].fhirResource).isEqualTo(mockCarePlan)
        Truth.assertThat(result.successfulOperations[0].meta).isEqualTo(mockMeta)
        Truth.assertThat(result.successfulOperations[1].fhirResource).isEqualTo(mockCarePlan)
        Truth.assertThat(result.successfulOperations[1].meta).isEqualTo(mockMeta)
        inOrder.verify(recordService).createRecords(resources, USER_ID)
        inOrder.verify(recordService, Mockito.times(2)).createRecord(mockCarePlan, USER_ID)
        inOrder.verifyNoMoreInteractions()
    }

    @Test
    @Throws(
            InterruptedException::class,
            IOException::class,
            DataRestrictionException.UnsupportedFileType::class,
            DataRestrictionException.MaxDataSizeViolation::class,
            DataValidationException.ModelVersionNotSupported::class,
            DataValidationException.ExpectedFieldViolation::class,
            DataValidationException.IdUsageViolation::class,
            DataValidationException.InvalidAttachmentPayloadHash::class
    )
    fun `Given createRecord is called with annotations, it creates a new Record`() {
        // Given
        Mockito.doReturn(mockUploadData).`when`(recordService).extractUploadData(mockCarePlan)
        Mockito.`when`(mockCarePlan.resourceType).thenReturn(CarePlan.resourceType)
        Mockito.`when`(mockTaggingService.appendDefaultTags(
                ArgumentMatchers.eq(CarePlan.resourceType),
                ArgumentMatchers.any<HashMap<String, String>>())
        ).thenReturn(mockTags)
        Mockito.`when`(mockCryptoService.generateGCKey()).thenReturn(Single.just(mockDataKey))
        Mockito.doReturn(mockAnnotatedDecryptedRecord).`when`(recordService)
                .uploadData(
                        ArgumentMatchers.eq(annotatedDecryptedRecordIndicator),
                        ArgumentMatchers.eq<DomainResource?>(null),
                        ArgumentMatchers.eq(USER_ID)
                )
        Mockito.doReturn(mockAnnotatedDecryptedRecord).`when`(recordService)
                .removeUploadData(ArgumentMatchers.any<DecryptedRecord<DomainResource>>())
        @Suppress("UNCHECKED_CAST")
        Mockito.doReturn(mockAnnotattedEncryptedRecord)
                .`when`(recordService).encryptRecord<DomainResource>(
                        mockAnnotatedDecryptedRecord as DecryptedRecord<DomainResource>
                )
        Mockito.`when`(mockApiService.createRecord(ALIAS, USER_ID, mockAnnotattedEncryptedRecord))
                .thenReturn(Single.just(mockAnnotattedEncryptedRecord))
        Mockito.doReturn(mockAnnotatedDecryptedRecord)
                .`when`(recordService)
                .decryptRecord<DomainResource>(mockAnnotattedEncryptedRecord, USER_ID)
        Mockito.doReturn(mockAnnotatedDecryptedRecord)
                .`when`(recordService).restoreUploadData<CarePlan>(
                        mockAnnotatedDecryptedRecord,
                        mockCarePlan,
                        mockUploadData
                )
        Mockito.doReturn(mockMeta).`when`(recordService).buildMeta(mockAnnotatedDecryptedRecord)

        // When
        val subscriber = recordService.createRecord(mockCarePlan, USER_ID, ANNOTATIONS).test().await()

        // Then
        val record = subscriber
                .assertNoErrors()
                .assertComplete()
                .assertValueCount(1)
                .values()[0]

        Truth.assertThat(record.meta).isEqualTo(mockMeta)
        Truth.assertThat(record.fhirResource).isEqualTo(mockCarePlan)
        Truth.assertThat(record.annotations).isSameInstanceAs(ANNOTATIONS)

        inOrder.verify(mockTaggingService).appendDefaultTags(
                ArgumentMatchers.anyString(),
                ArgumentMatchers.any<HashMap<String, String>>()
        )
        inOrder.verify(mockCryptoService).generateGCKey()
        inOrder.verify(recordService).uploadData(
                ArgumentMatchers.eq(annotatedDecryptedRecordIndicator),
                ArgumentMatchers.eq<DomainResource?>(null),
                ArgumentMatchers.eq(USER_ID)
        )
        inOrder.verify(recordService).removeUploadData(
                ArgumentMatchers.any<DecryptedRecord<DomainResource>>()
        )
        @Suppress("UNCHECKED_CAST")
        inOrder.verify(recordService).encryptRecord<DomainResource>(
                mockAnnotatedDecryptedRecord as DecryptedRecord<DomainResource>
        )
        inOrder.verify(mockApiService).createRecord(ALIAS, USER_ID, mockAnnotattedEncryptedRecord)
        inOrder.verify(recordService).decryptRecord<DomainResource>(mockAnnotattedEncryptedRecord, USER_ID)
        inOrder.verify(recordService).restoreUploadData<CarePlan>(
                mockAnnotatedDecryptedRecord,
                mockCarePlan,
                mockUploadData
        )
        inOrder.verify(recordService).buildMeta(mockAnnotatedDecryptedRecord)
        inOrder.verifyNoMoreInteractions()
    }

    @Test
    @Throws(
            InterruptedException::class,
            IOException::class,
            DataRestrictionException.UnsupportedFileType::class,
            DataRestrictionException.MaxDataSizeViolation::class,
            DataValidationException.ModelVersionNotSupported::class,
            DataValidationException.ExpectedFieldViolation::class,
            DataValidationException.IdUsageViolation::class,
            DataValidationException.InvalidAttachmentPayloadHash::class
    )
    fun `Given createRecord is called with annotations and without attachment, it creates a new Record`() {
        // Given
        Mockito.doReturn(null).`when`(recordService).extractUploadData(mockCarePlan)
        Mockito.`when`(mockCarePlan.resourceType).thenReturn(CarePlan.resourceType)
        Mockito.`when`(
                mockTaggingService.appendDefaultTags(
                        ArgumentMatchers.eq(CarePlan.resourceType),
                        ArgumentMatchers.any<HashMap<String, String>>()
                )
        ).thenReturn(mockTags)
        Mockito.`when`(mockCryptoService.generateGCKey())
                .thenReturn(Single.just(mockDataKey))
        Mockito.doReturn(mockAnnotatedDecryptedRecord)
                .`when`(recordService)
                .uploadData(
                        ArgumentMatchers.eq(annotatedDecryptedRecordIndicator),
                        ArgumentMatchers.eq<DomainResource?>(null),
                        ArgumentMatchers.eq(USER_ID)
                )
        Mockito.doReturn(mockAnnotatedDecryptedRecord)
                .`when`(recordService)
                .removeUploadData(ArgumentMatchers.same(mockAnnotatedDecryptedRecord))
        @Suppress("UNCHECKED_CAST")
        Mockito.doReturn(mockAnnotattedEncryptedRecord).`when`(recordService)
                .encryptRecord<DomainResource>(mockAnnotatedDecryptedRecord as DecryptedRecord<DomainResource>)
        Mockito.`when`(mockApiService.createRecord(ALIAS, USER_ID, mockAnnotattedEncryptedRecord))
                .thenReturn(Single.just(mockAnnotattedEncryptedRecord))
        Mockito.doReturn(mockAnnotatedDecryptedRecord)
                .`when`(recordService)
                .decryptRecord<DomainResource>(
                        mockAnnotattedEncryptedRecord,
                        USER_ID
                )
        Mockito.doReturn(mockAnnotatedDecryptedRecord)
                .`when`(recordService)
                .restoreUploadData<CarePlan>(
                        mockAnnotatedDecryptedRecord,
                        mockCarePlan,
                        mockUploadData
                )
        @Suppress("UNCHECKED_CAST")
        Mockito.doReturn(mockAnnotatedDecryptedRecord)
                .`when`(recordService)
                .uploadData<DomainResource>(
                        mockAnnotatedDecryptedRecord as DecryptedRecord<DomainResource>,
                        null,
                        USER_ID
                )
        Mockito.doReturn(mockMeta).`when`(recordService).buildMeta(mockAnnotatedDecryptedRecord)

        // When
        val subscriber = recordService.createRecord(mockCarePlan, USER_ID, ANNOTATIONS).test().await()

        // Then
        val record = subscriber
                .assertNoErrors()
                .assertComplete()
                .assertValueCount(1)
                .values()[0]
        Truth.assertThat(record.meta).isEqualTo(mockMeta)
        Truth.assertThat(record.fhirResource).isEqualTo(mockCarePlan)
        Truth.assertThat(record.annotations).isSameInstanceAs(ANNOTATIONS)

        inOrder.verify(mockTaggingService).appendDefaultTags(
                ArgumentMatchers.anyString(),
                ArgumentMatchers.any<HashMap<String, String>>()
        )
        inOrder.verify(mockCryptoService).generateGCKey()
        inOrder.verify(recordService).uploadData(
                ArgumentMatchers.eq(annotatedDecryptedRecordIndicator),
                ArgumentMatchers.eq<DomainResource?>(null),
                ArgumentMatchers.eq(USER_ID)
        )
        inOrder.verify(recordService).removeUploadData(
                ArgumentMatchers.same(mockAnnotatedDecryptedRecord)
        )
        @Suppress("UNCHECKED_CAST")
        inOrder.verify(recordService).encryptRecord<DomainResource>(
                mockAnnotatedDecryptedRecord as DecryptedRecord<DomainResource>
        )
        inOrder.verify(mockApiService).createRecord(ALIAS, USER_ID, mockAnnotattedEncryptedRecord)
        inOrder.verify(recordService).decryptRecord<DomainResource>(mockAnnotattedEncryptedRecord, USER_ID)
        inOrder.verify(recordService).restoreUploadData(
                mockAnnotatedDecryptedRecord,
                mockCarePlan,
                null
        )
        inOrder.verify(recordService).buildMeta(mockAnnotatedDecryptedRecord)
        inOrder.verifyNoMoreInteractions()
    }

    @Test
    @Throws(
            InterruptedException::class,
            DataRestrictionException.UnsupportedFileType::class,
            DataRestrictionException.MaxDataSizeViolation::class
    )
    fun `Given createRecord is called with annotations and unsupported Data, it throws an error`() {
        // Given
        val invalidData = byteArrayOf(0x00)
        val doc = RecordServiceTest.buildDocumentReference(invalidData)

        // When
        try {
            recordService.createRecord(doc, USER_ID, ANNOTATIONS).test().await()
            Assert.fail("Exception expected!")
        } catch (ex: D4LException) {

            // Then
            Truth.assertThat(ex.javaClass).isEqualTo(DataRestrictionException.UnsupportedFileType::class.java)
        }
        inOrder.verify(recordService).createRecord(doc, USER_ID, ANNOTATIONS)
        inOrder.verify(recordService).checkDataRestrictions(doc)
        inOrder.verifyNoMoreInteractions()
    }

    @Test
    @Throws(
            InterruptedException::class,
            DataRestrictionException.UnsupportedFileType::class,
            DataRestrictionException.MaxDataSizeViolation::class
    )
    fun `Given createRecord is called with annotations and data, which exceeds the file size limitation, throws an error`() {
        // Given
        val invalidSizePdf = arrayOfNulls<Byte>(DATA_SIZE_MAX_BYTES + 1)
        System.arraycopy(
                MimeType.PDF.byteSignature()[0] as Any,
                0,
                invalidSizePdf,
                0,
                MimeType.PDF.byteSignature()[0]!!.size
        )
        val doc = RecordServiceTest.buildDocumentReference(RecordServiceTest.unboxByteArray(invalidSizePdf))

        // When
        try {
            recordService.createRecord(doc, USER_ID, ANNOTATIONS).test().await()
            Assert.fail("Exception expected!")
        } catch (ex: D4LException) {

            // Then
            Truth.assertThat(ex.javaClass).isEqualTo(DataRestrictionException.MaxDataSizeViolation::class.java)
        }
        inOrder.verify(recordService).createRecord(doc, USER_ID, ANNOTATIONS)
        inOrder.verify(recordService).checkDataRestrictions(doc)
        inOrder.verifyNoMoreInteractions()
    }

    companion object {
        private const val PARTNER_ID = "partnerId"
        private const val USER_ID = "userId"
        private const val ENCRYPTED_RESOURCE = "encryptedResource"
        private const val RESOURCE_TYPE = "resourcetype"
        private const val RECORD_ID = "recordId"
        private const val ALIAS = "alias"
        private val LOCAL_DATE = LocalDate.of(2001,  1, 1)
        private val DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.US)
        private val ANNOTATIONS = listOf("potato", "tomato", "soup")
    }
}
