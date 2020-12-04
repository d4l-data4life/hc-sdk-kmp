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
import care.data4life.sdk.lang.DataValidationException
import care.data4life.sdk.util.MimeType
import com.google.common.truth.Truth
import io.reactivex.Single
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers
import org.mockito.Mockito
import java.io.IOException

class RecordServiceUpdateRecordTest: RecordServiceTestBase() {
    @Before
    fun setUp() {
        init()
    }

    @Test
    @Throws(
            InterruptedException::class,
            IOException::class,
            DataValidationException.ModelVersionNotSupported::class,
            DataRestrictionException.UnsupportedFileType::class,
            DataRestrictionException.MaxDataSizeViolation::class
    )
    fun `Given a resource and a UserId, it updates a Record`() {
        // Given
        mockCarePlan.id = RECORD_ID
        Mockito.`when`(mockCarePlan.resourceType).thenReturn(CarePlan.resourceType)
        Mockito.`when`(
                mockApiService.fetchRecord(
                        ALIAS,
                        USER_ID,
                        RECORD_ID
                )
        ).thenReturn(Single.just(mockEncryptedRecord))
        Mockito.doReturn(mockDecryptedRecord)
                .`when`(recordService)
                .decryptRecord<DomainResource>(mockEncryptedRecord, USER_ID)
        Mockito.doReturn(mockEncryptedRecord).`when`(recordService).encryptRecord(mockDecryptedRecord)
        Mockito.`when`(
                mockApiService.updateRecord(
                        ALIAS,
                        USER_ID,
                        RECORD_ID,
                        mockEncryptedRecord
                )
        ).thenReturn(Single.just(mockEncryptedRecord))
        Mockito.doReturn(mockMeta).`when`(recordService).buildMeta(mockDecryptedRecord)

        // When
        val observer = recordService.updateRecord(mockCarePlan, USER_ID).test().await()

        // Then
        val result = observer.assertNoErrors()
                .assertComplete()
                .assertValueCount(1)
                .values()[0]
        Truth.assertThat(result.meta).isEqualTo(mockMeta)
        Truth.assertThat(result.fhirResource).isEqualTo(mockCarePlan)
        inOrder.verify(mockApiService).fetchRecord(ALIAS, USER_ID, RECORD_ID)
        inOrder.verify(recordService).decryptRecord<DomainResource>(mockEncryptedRecord, USER_ID)
        inOrder.verify(recordService).encryptRecord(mockDecryptedRecord)
        inOrder.verify(mockApiService).updateRecord(ALIAS, USER_ID, RECORD_ID, mockEncryptedRecord)
        inOrder.verify(recordService).buildMeta(mockDecryptedRecord)
        inOrder.verifyNoMoreInteractions()

        Mockito.verify(mockDecryptedRecord, Mockito.times(4)).resource
        Mockito.verify(mockDecryptedRecord, Mockito.times(1)).annotations
        Mockito.verify(
                mockDecryptedRecord,
                Mockito.times(0)
        ).annotations = ArgumentMatchers.any()

        // Cleanup
        mockCarePlan.id = null
    }

    @Test
    @Throws(DataRestrictionException.UnsupportedFileType::class, DataRestrictionException.MaxDataSizeViolation::class)
    fun `Given a unsupported data and a UserId, it throws an error on update`() {
        // Given
        val invalidData = byteArrayOf(0x00)
        val doc = RecordServiceTest.buildDocumentReference(invalidData)

        // When
        try {
            recordService.updateRecord(doc, USER_ID).test().await()
            Assert.fail("Exception expected!")
        } catch (ex: Exception) {
            // Then
            Truth.assertThat(ex).isInstanceOf(DataRestrictionException.UnsupportedFileType::class.java)
        }
        inOrder.verify(recordService).updateRecord(doc, USER_ID)
        inOrder.verify(recordService).checkDataRestrictions(doc)
        inOrder.verifyNoMoreInteractions()
    }

    @Test
    @Throws(DataRestrictionException.UnsupportedFileType::class, DataRestrictionException.MaxDataSizeViolation::class)
    fun `Given data, which exceeds the file size limitations, and a UserId, it throws an error on update`() {
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
            recordService.updateRecord(doc, USER_ID).test().await()
            Assert.fail("Exception expected!")
        } catch (ex: Exception) {
            // Then
            Truth.assertThat(ex).isInstanceOf(DataRestrictionException.MaxDataSizeViolation::class.java)
        }
        inOrder.verify(recordService).updateRecord(doc, USER_ID)
        inOrder.verify(recordService).checkDataRestrictions(doc)
        inOrder.verifyNoMoreInteractions()
    }

    @Test
    @Throws(
            InterruptedException::class,
            IOException::class,
            DataValidationException.ModelVersionNotSupported::class,
            DataRestrictionException.UnsupportedFileType::class,
            DataRestrictionException.MaxDataSizeViolation::class
    )
    fun `Given a resource, annotations and a UserId, it updates a Record`() {
        // Given
        mockCarePlan.id = RECORD_ID
        Mockito.`when`(mockCarePlan.resourceType).thenReturn(CarePlan.resourceType)
        Mockito.`when`(
                mockApiService.fetchRecord(
                        ALIAS,
                        USER_ID,
                        RECORD_ID
                )
        ).thenReturn(Single.just(mockEncryptedRecord))
        Mockito.doReturn(mockDecryptedRecord)
                .`when`(recordService)
                .decryptRecord<DomainResource>(mockEncryptedRecord, USER_ID)
        Mockito.doReturn(mockEncryptedRecord).`when`(recordService).encryptRecord(mockDecryptedRecord)
        Mockito.`when`(
                mockApiService.updateRecord(
                        ALIAS,
                        USER_ID,
                        RECORD_ID,
                        mockEncryptedRecord
                )
        ).thenReturn(Single.just(mockEncryptedRecord))
        Mockito.doReturn(mockMeta).`when`(recordService).buildMeta(mockDecryptedRecord)

        // When
        val observer = recordService.updateRecord(mockCarePlan, USER_ID, ANNOTATIONS).test().await()

        // Then
        val result = observer.assertNoErrors()
                .assertComplete()
                .assertValueCount(1)
                .values()[0]
        Truth.assertThat(result.meta).isEqualTo(mockMeta)
        Truth.assertThat(result.fhirResource).isEqualTo(mockCarePlan)

        inOrder.verify(mockApiService).fetchRecord(ALIAS, USER_ID, RECORD_ID)
        inOrder.verify(recordService).decryptRecord<DomainResource>(mockEncryptedRecord, USER_ID)
        inOrder.verify(recordService).encryptRecord(mockDecryptedRecord)
        inOrder.verify(mockApiService).updateRecord(ALIAS, USER_ID, RECORD_ID, mockEncryptedRecord)
        inOrder.verify(recordService).buildMeta(mockDecryptedRecord)
        inOrder.verifyNoMoreInteractions()

        Mockito.verify(mockDecryptedRecord, Mockito.times(4)).resource
        Mockito.verify(mockDecryptedRecord, Mockito.times(1)).annotations
        Mockito.verify(
                mockDecryptedRecord,
                Mockito.times(1)
        ).annotations = ANNOTATIONS

        // Cleanup
        mockCarePlan.id = null
    }

    @Test
    @Throws(DataRestrictionException.UnsupportedFileType::class, DataRestrictionException.MaxDataSizeViolation::class)
    fun `Given data, which exceeds the file size limitations, annotations and a UserId, it throws an error on update`() {
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
            recordService.updateRecord(doc, USER_ID, ANNOTATIONS).test().await()
            Assert.fail("Exception expected!")
        } catch (ex: Exception) {
            // Then
            Truth.assertThat(ex).isInstanceOf(DataRestrictionException.MaxDataSizeViolation::class.java)
        }
        inOrder.verify(recordService).updateRecord(doc, USER_ID, ANNOTATIONS)
        inOrder.verify(recordService).checkDataRestrictions(doc)
        inOrder.verifyNoMoreInteractions()
    }



    @Test
    @Throws(
            InterruptedException::class,
            DataRestrictionException.UnsupportedFileType::class,
            DataRestrictionException.MaxDataSizeViolation::class
    )
    fun `Given multiple resources and a UserId, it updates multiple Records`() {
        // Given
        val resources = listOf(mockCarePlan, mockCarePlan)
        Mockito.doReturn(Single.just(mockRecord))
                .`when`(recordService)
                .updateRecord(mockCarePlan, USER_ID)

        // When
        val observer = recordService.updateRecords(resources, USER_ID).test().await()

        // Then
        val result = observer
                .assertNoErrors()
                .assertComplete()
                .assertValueCount(1)
                .values()[0]
        Truth.assertThat(result.failedUpdates).hasSize(0)
        Truth.assertThat(result.successfulUpdates).hasSize(2)
        Truth.assertThat(result.successfulUpdates).containsExactly(mockRecord, mockRecord)
        inOrder.verify(recordService).updateRecords(resources, USER_ID)
        inOrder.verify(
                recordService,
                Mockito.times(2)
        ).updateRecord(mockCarePlan, USER_ID)
        inOrder.verifyNoMoreInteractions()
    }
}
