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

import care.data4life.fhir.stu3.model.CarePlan
import care.data4life.fhir.stu3.model.DomainResource
import care.data4life.sdk.config.DataRestriction.DATA_SIZE_MAX_BYTES
import care.data4life.sdk.config.DataRestrictionException
import care.data4life.sdk.lang.D4LException
import care.data4life.sdk.lang.DataValidationException
import care.data4life.sdk.network.model.DecryptedRecord
import care.data4life.sdk.util.MimeType

import java.io.IOException
import java.util.*

import com.google.common.truth.Truth
import io.reactivex.Single
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers
import org.mockito.Mockito

class RecordServiceCreateRecordTest: RecordServiceTestBase() {
    @Before
    fun setup() {
        init()
    }

    @After
    fun tearDown() {
        stop()
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
        
        Mockito.doReturn(mockEncryptedRecord)
                .`when`(recordService).encryptRecord<DomainResource>(
                        mockDecryptedRecord
                )
        Mockito.`when`(mockApiService.createRecord(ALIAS, USER_ID, mockEncryptedRecord))
                .thenReturn(Single.just(mockEncryptedRecord))
        Mockito.doReturn(mockDecryptedRecord)
                .`when`(recordService)
                .decryptRecord<DomainResource>(mockEncryptedRecord, USER_ID)
        Mockito.doReturn(mockDecryptedRecord)
                .`when`(recordService).restoreUploadData<DomainResource>(
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
        
        inOrder.verify(recordService).encryptRecord<DomainResource>(
                mockDecryptedRecord
        )
        inOrder.verify(mockApiService).createRecord(ALIAS, USER_ID, mockEncryptedRecord)
        inOrder.verify(recordService).decryptRecord<DomainResource>(mockEncryptedRecord, USER_ID)
        inOrder.verify(recordService).restoreUploadData<DomainResource>(
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
        
        Mockito.doReturn(mockEncryptedRecord).`when`(recordService)
                .encryptRecord<DomainResource>(mockDecryptedRecord)
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
                .restoreUploadData<DomainResource>(
                        mockDecryptedRecord,
                        mockCarePlan,
                        mockUploadData
                )
        
        Mockito.doReturn(mockDecryptedRecord)
                .`when`(recordService)
                .uploadData<DomainResource>(
                        mockDecryptedRecord,
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
        
        inOrder.verify(recordService).encryptRecord<DomainResource>(
                mockDecryptedRecord
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
        
        Mockito.doReturn(mockAnnotatedEncryptedRecord)
                .`when`(recordService).encryptRecord<DomainResource>(
                        mockAnnotatedDecryptedRecord
                )
        Mockito.`when`(mockApiService.createRecord(ALIAS, USER_ID, mockAnnotatedEncryptedRecord))
                .thenReturn(Single.just(mockAnnotatedEncryptedRecord))
        Mockito.doReturn(mockAnnotatedDecryptedRecord)
                .`when`(recordService)
                .decryptRecord<DomainResource>(mockAnnotatedEncryptedRecord, USER_ID)
        Mockito.doReturn(mockAnnotatedDecryptedRecord)
                .`when`(recordService).restoreUploadData<DomainResource>(
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
        
        inOrder.verify(recordService).encryptRecord<DomainResource>(
                mockAnnotatedDecryptedRecord
        )
        inOrder.verify(mockApiService).createRecord(ALIAS, USER_ID, mockAnnotatedEncryptedRecord)
        inOrder.verify(recordService).decryptRecord<DomainResource>(mockAnnotatedEncryptedRecord, USER_ID)
        inOrder.verify(recordService).restoreUploadData<DomainResource>(
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
        
        Mockito.doReturn(mockAnnotatedEncryptedRecord).`when`(recordService)
                .encryptRecord<DomainResource>(mockAnnotatedDecryptedRecord)
        Mockito.`when`(mockApiService.createRecord(ALIAS, USER_ID, mockAnnotatedEncryptedRecord))
                .thenReturn(Single.just(mockAnnotatedEncryptedRecord))
        Mockito.doReturn(mockAnnotatedDecryptedRecord)
                .`when`(recordService)
                .decryptRecord<DomainResource>(
                        mockAnnotatedEncryptedRecord,
                        USER_ID
                )
        Mockito.doReturn(mockAnnotatedDecryptedRecord)
                .`when`(recordService)
                .restoreUploadData<DomainResource>(
                        mockAnnotatedDecryptedRecord,
                        mockCarePlan,
                        mockUploadData
                )
        
        Mockito.doReturn(mockAnnotatedDecryptedRecord)
                .`when`(recordService)
                .uploadData<DomainResource>(
                        mockAnnotatedDecryptedRecord,
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
        
        inOrder.verify(recordService).encryptRecord<DomainResource>(
                mockAnnotatedDecryptedRecord
        )
        inOrder.verify(mockApiService).createRecord(ALIAS, USER_ID, mockAnnotatedEncryptedRecord)
        inOrder.verify(recordService).decryptRecord<DomainResource>(mockAnnotatedEncryptedRecord, USER_ID)
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
}
