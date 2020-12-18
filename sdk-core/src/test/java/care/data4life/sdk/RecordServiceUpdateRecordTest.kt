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
import care.data4life.sdk.data.DataResource
import care.data4life.sdk.lang.DataValidationException
import care.data4life.sdk.model.SdkRecordFactory
import care.data4life.sdk.model.definitions.BaseRecord
import care.data4life.sdk.util.MimeType
import com.google.common.truth.Truth
import io.mockk.every
import io.mockk.spyk
import io.reactivex.Single
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.mockito.ArgumentMatchers
import org.mockito.Mockito
import java.io.IOException

class RecordServiceUpdateRecordTest : RecordServiceTestBase() {

    @Before
    fun setUp() {
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
            DataValidationException.ModelVersionNotSupported::class,
            DataRestrictionException.UnsupportedFileType::class,
            DataRestrictionException.MaxDataSizeViolation::class
    )
    fun `Given, updateRecord is called with a resource and a UserId, it returns a updated Record`() {
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
        Mockito.doReturn(mockDecryptedFhir3Record)
                .`when`(recordService)
                .decryptRecord<DomainResource>(mockEncryptedRecord, USER_ID)
        Mockito.doReturn(mockEncryptedRecord).`when`(recordService).encryptRecord(mockDecryptedFhir3Record)
        Mockito.`when`(
                mockApiService.updateRecord(
                        ALIAS,
                        USER_ID,
                        RECORD_ID,
                        mockEncryptedRecord
                )
        ).thenReturn(Single.just(mockEncryptedRecord))
        Mockito.doReturn(mockDecryptedDataRecord)
                .`when`(recordService)
                .assignResourceId(mockDecryptedDataRecord)
        @Suppress("UNCHECKED_CAST")
        every { SdkRecordFactory.getInstance(mockDecryptedFhir3Record) } returns mockRecord as BaseRecord<DomainResource>
        val annotations = listOf<String>()

        // When
        val observer = recordService.updateRecord(USER_ID, RECORD_ID, mockCarePlan, annotations).test().await()

        // Then
        val result = observer.assertNoErrors()
                .assertComplete()
                .assertValueCount(1)
                .values()[0]

        Truth.assertThat(result).isSameInstanceAs(mockRecord)

        inOrder.verify(mockApiService).fetchRecord(ALIAS, USER_ID, RECORD_ID)
        inOrder.verify(recordService).decryptRecord<DomainResource>(mockEncryptedRecord, USER_ID)
        inOrder.verify(recordService).encryptRecord(mockDecryptedFhir3Record)
        inOrder.verify(mockApiService).updateRecord(ALIAS, USER_ID, RECORD_ID, mockEncryptedRecord)
        inOrder.verify(recordService).decryptRecord<DomainResource>(mockEncryptedRecord, USER_ID)
        inOrder.verify(recordService).restoreUploadData(
                mockDecryptedFhir3Record,
                mockCarePlan,
                null
        )
        inOrder.verify(recordService).assignResourceId(mockDecryptedFhir3Record)
        inOrder.verifyNoMoreInteractions()

        Mockito.verify(mockDecryptedFhir3Record, Mockito.times(7)).resource
        Mockito.verify(
                mockDecryptedFhir3Record,
                Mockito.times(2)
        ).resource = mockCarePlan
        Mockito.verify(
                mockDecryptedFhir3Record,
                Mockito.times(1)
        ).annotations = annotations
    }

    @Test
    @Throws(DataRestrictionException.UnsupportedFileType::class, DataRestrictionException.MaxDataSizeViolation::class)
    fun `Given, updateRecord is called with a unsupported data and a UserId, it throws an error on update`() {
        // Given
        val invalidData = byteArrayOf(0x00)
        val doc = buildDocumentReference(invalidData)
        val annotations = listOf<String>()

        // When
        try {
            recordService.updateRecord(USER_ID, RECORD_ID, doc, annotations).test().await()
            Assert.fail("Exception expected!")
        } catch (ex: Exception) {
            // Then
            Truth.assertThat(ex).isInstanceOf(DataRestrictionException.UnsupportedFileType::class.java)
        }

        inOrder.verify(recordService).updateRecord(USER_ID, RECORD_ID, doc, annotations)
        inOrder.verify(recordService).checkDataRestrictions(doc)
        inOrder.verifyNoMoreInteractions()
    }

    @Test
    @Throws(DataRestrictionException.UnsupportedFileType::class, DataRestrictionException.MaxDataSizeViolation::class)
    fun `Given, updateRecord is called with data, which exceeds the file size limitations and a UserId, it throws an error on update`() {
        // Given
        val invalidSizePdf = arrayOfNulls<Byte>(DATA_SIZE_MAX_BYTES + 1)
        System.arraycopy(
                MimeType.PDF.byteSignature()[0] as Any,
                0,
                invalidSizePdf,
                0,
                MimeType.PDF.byteSignature()[0]!!.size
        )
        val doc = buildDocumentReference(unboxByteArray(invalidSizePdf))
        val annotations = listOf<String>()

        // When
        try {
            recordService.updateRecord(USER_ID, RECORD_ID, doc, annotations).test().await()
            Assert.fail("Exception expected!")
        } catch (ex: Exception) {
            // Then
            Truth.assertThat(ex).isInstanceOf(DataRestrictionException.MaxDataSizeViolation::class.java)
        }
        inOrder.verify(recordService).updateRecord(USER_ID, RECORD_ID, doc, annotations)
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
    fun `Given, updateRecord is called with a resource, Annotations and a UserId, it returns updated a Record`() {
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
        Mockito.doReturn(mockDecryptedFhir3Record)
                .`when`(recordService)
                .decryptRecord<DomainResource>(mockEncryptedRecord, USER_ID)
        Mockito.doReturn(mockEncryptedRecord).`when`(recordService).encryptRecord(mockDecryptedFhir3Record)
        Mockito.`when`(
                mockApiService.updateRecord(
                        ALIAS,
                        USER_ID,
                        RECORD_ID,
                        mockEncryptedRecord
                )
        ).thenReturn(Single.just(mockEncryptedRecord))
        @Suppress("UNCHECKED_CAST")
        every { SdkRecordFactory.getInstance(mockDecryptedFhir3Record) } returns mockRecord as BaseRecord<DomainResource>

        // When
        val observer = recordService.updateRecord(USER_ID, RECORD_ID, mockCarePlan, ANNOTATIONS).test().await()

        // Then
        val result = observer.assertNoErrors()
                .assertComplete()
                .assertValueCount(1)
                .values()[0]

        Truth.assertThat(result).isSameInstanceAs(mockRecord)

        inOrder.verify(mockApiService).fetchRecord(ALIAS, USER_ID, RECORD_ID)
        inOrder.verify(recordService).decryptRecord<DomainResource>(mockEncryptedRecord, USER_ID)
        inOrder.verify(recordService).encryptRecord(mockDecryptedFhir3Record)
        inOrder.verify(mockApiService).updateRecord(ALIAS, USER_ID, RECORD_ID, mockEncryptedRecord)
        inOrder.verify(recordService).decryptRecord<DomainResource>(mockEncryptedRecord, USER_ID)
        inOrder.verify(recordService).restoreUploadData(
                mockDecryptedFhir3Record,
                mockCarePlan,
                null
        )
        inOrder.verify(recordService).assignResourceId(mockDecryptedFhir3Record)
        inOrder.verifyNoMoreInteractions()

        Mockito.verify(mockDecryptedFhir3Record, Mockito.times(7)).resource
        Mockito.verify(
                mockDecryptedFhir3Record,
                Mockito.times(2)
        ).resource = mockCarePlan
        Mockito.verify(
                mockDecryptedFhir3Record,
                Mockito.times(1)
        ).annotations = ANNOTATIONS
    }

    @Test
    @Throws(DataRestrictionException.UnsupportedFileType::class, DataRestrictionException.MaxDataSizeViolation::class)
    fun `Given, updateRecord called with data, which exceeds the file size limitations, Annotations and a UserId, it throws an error on update`() {
        // Given
        val invalidSizePdf = arrayOfNulls<Byte>(DATA_SIZE_MAX_BYTES + 1)
        System.arraycopy(
                MimeType.PDF.byteSignature()[0] as Any,
                0,
                invalidSizePdf,
                0,
                MimeType.PDF.byteSignature()[0]!!.size
        )
        val doc = buildDocumentReference(unboxByteArray(invalidSizePdf))

        // When
        try {
            recordService.updateRecord(USER_ID, RECORD_ID, doc, ANNOTATIONS).test().await()
            Assert.fail("Exception expected!")
        } catch (ex: Exception) {
            // Then
            Truth.assertThat(ex).isInstanceOf(DataRestrictionException.MaxDataSizeViolation::class.java)
        }
        inOrder.verify(recordService).updateRecord(USER_ID, RECORD_ID, doc, ANNOTATIONS)
        inOrder.verify(recordService).checkDataRestrictions(doc)
        inOrder.verifyNoMoreInteractions()
    }

    @Test
    @Throws(
            InterruptedException::class,
            DataRestrictionException.UnsupportedFileType::class,
            DataRestrictionException.MaxDataSizeViolation::class
    )
    fun `Given, updateRecords is called with multiple resources, Annotations and a UserId, returns multiple updated Records`() {
        // Given
        mockCarePlan.id = RECORD_ID
        val resources = listOf(mockCarePlan, mockCarePlan)
        val annotations = listOf<String>()

        Mockito.doReturn(Single.just(mockRecord))
                .`when`(recordService)
                .updateRecord(USER_ID, RECORD_ID,  mockCarePlan, annotations)

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
        ).updateRecord(USER_ID, RECORD_ID, mockCarePlan, annotations)
        inOrder.verifyNoMoreInteractions()
    }

    @Test
    @Throws(
            InterruptedException::class,
            DataRestrictionException.UnsupportedFileType::class,
            DataRestrictionException.MaxDataSizeViolation::class
    )
    fun `Given, updateRecord is called with a byte resource, Annotations and a UserId, it returns a updated AppDataRecord`() {
        // Given
        Mockito.`when`(
                mockApiService.fetchRecord(
                        ALIAS,
                        USER_ID,
                        RECORD_ID
                )
        ).thenReturn(Single.just(mockEncryptedRecord))
        Mockito.doReturn(mockDecryptedDataRecord)
                .`when`(recordService)
                .decryptRecord<DataResource>(mockEncryptedRecord, USER_ID)
        Mockito.doReturn(mockEncryptedRecord).`when`(recordService).encryptRecord(mockDecryptedDataRecord)
        Mockito.`when`(
                mockApiService.updateRecord(
                        ALIAS,
                        USER_ID,
                        RECORD_ID,
                        mockEncryptedRecord
                )
        ).thenReturn(Single.just(mockEncryptedRecord))
        Mockito.doReturn(mockDecryptedDataRecord)
                .`when`(recordService)
                .assignResourceId(mockDecryptedDataRecord)
        @Suppress("UNCHECKED_CAST")
        every { SdkRecordFactory.getInstance(mockDecryptedDataRecord) } returns mockDataRecord

        // When
        val observer = recordService.updateRecord(
                USER_ID,
                RECORD_ID,
                mockDataResource,
                ANNOTATIONS
        ).test().await()

        // Then
        val result = observer.assertNoErrors()
                .assertComplete()
                .assertValueCount(1)
                .values()[0]

        Truth.assertThat(result).isSameInstanceAs(mockDataRecord)

        inOrder.verify(mockApiService).fetchRecord(ALIAS, USER_ID, RECORD_ID)
        inOrder.verify(recordService).decryptRecord<DataResource>(mockEncryptedRecord, USER_ID)
        inOrder.verify(recordService).encryptRecord(mockDecryptedDataRecord)
        inOrder.verify(mockApiService).updateRecord(ALIAS, USER_ID, RECORD_ID, mockEncryptedRecord)
        inOrder.verify(recordService).decryptRecord<DataResource>(mockEncryptedRecord, USER_ID)
        inOrder.verify(recordService).assignResourceId(mockDecryptedDataRecord)
        inOrder.verifyNoMoreInteractions()

        Mockito.verify(
                mockDecryptedDataRecord,
                Mockito.times(1)
        ).resource = mockDataResource
        Mockito.verify(
                mockDecryptedDataRecord,
                Mockito.times(1)
        ).annotations = ANNOTATIONS
    }

    @Test
    @Throws(
            InterruptedException::class,
            DataRestrictionException.UnsupportedFileType::class,
            DataRestrictionException.MaxDataSizeViolation::class
    )
    fun `Given, updateRecord is called with a byte resource, empty list Annotations and a UserId, it returns a updated AppDataRecord`() {
        // Given
        Mockito.`when`(
                mockApiService.fetchRecord(
                        ALIAS,
                        USER_ID,
                        RECORD_ID
                )
        ).thenReturn(Single.just(mockEncryptedRecord))
        Mockito.doReturn(mockDecryptedDataRecord)
                .`when`(recordService)
                .decryptRecord<DataResource>(mockEncryptedRecord, USER_ID)
        Mockito.doReturn(mockEncryptedRecord).`when`(recordService).encryptRecord(mockDecryptedDataRecord)
        Mockito.`when`(
                mockApiService.updateRecord(
                        ALIAS,
                        USER_ID,
                        RECORD_ID,
                        mockEncryptedRecord
                )
        ).thenReturn(Single.just(mockEncryptedRecord))
        Mockito.doReturn(mockDecryptedDataRecord)
                .`when`(recordService)
                .assignResourceId(mockDecryptedDataRecord)
        @Suppress("UNCHECKED_CAST")
        every { SdkRecordFactory.getInstance(mockDecryptedDataRecord) } returns mockDataRecord

        // When
        val observer = recordService.updateRecord(
                USER_ID,
                RECORD_ID,
                mockDataResource,
                listOf()
        ).test().await()

        // Then
        val result = observer.assertNoErrors()
                .assertComplete()
                .assertValueCount(1)
                .values()[0]

        Truth.assertThat(result).isSameInstanceAs(mockDataRecord)

        inOrder.verify(mockApiService).fetchRecord(ALIAS, USER_ID, RECORD_ID)
        inOrder.verify(recordService).decryptRecord<DataResource>(mockEncryptedRecord, USER_ID)
        inOrder.verify(recordService).encryptRecord(mockDecryptedDataRecord)
        inOrder.verify(mockApiService).updateRecord(ALIAS, USER_ID, RECORD_ID, mockEncryptedRecord)
        inOrder.verify(recordService).decryptRecord<DataResource>(mockEncryptedRecord, USER_ID)
        inOrder.verify(recordService).assignResourceId(mockDecryptedDataRecord)
        inOrder.verifyNoMoreInteractions()

        Mockito.verify(
                mockDecryptedDataRecord,
                Mockito.times(1)
        ).resource = mockDataResource
        Mockito.verify(
                mockDecryptedDataRecord,
                Mockito.times(1)
        ).annotations = ArgumentMatchers.anyList()
    }
}
