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
import care.data4life.sdk.model.ModelVersion
import care.data4life.sdk.model.SdkRecordFactory
import care.data4life.sdk.model.definitions.BaseRecord
import care.data4life.sdk.network.DecryptedRecordBuilderImpl
import care.data4life.sdk.network.model.definitions.DecryptedFhir3Record
import care.data4life.sdk.util.MimeType
import com.google.common.truth.Truth
import io.mockk.every
import io.reactivex.Single
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.mockito.ArgumentMatchers
import org.mockito.Mockito
import org.threeten.bp.LocalDate
import java.io.IOException

/*
class RecordServiceCreateRecordTest : RecordServiceTestBase() {
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
    fun `Given, createRecord is called with a DomainResource and a UserId, it returns a new Record`() {
        // Given
        every {
            anyConstructed<DecryptedRecordBuilderImpl>().setAnnotations(listOf())
        } returns mockDecryptedRecordBuilder as DecryptedRecordBuilderImpl

        @Suppress("UNCHECKED_CAST")
        Mockito.`when`(
                mockDecryptedRecordBuilder.build(
                        mockCarePlan,
                        mockTags,
                        DATE_FORMATTER.format(LocalDate.now(UTC_ZONE_ID)),
                        mockDataKey,
                        ModelVersion.CURRENT
                )
        ).thenReturn(mockDecryptedFhir3Record as DecryptedFhir3Record<CarePlan>)
        Mockito.doReturn(mockUploadData).`when`(recordService).extractUploadData(mockCarePlan)
        Mockito.`when`(mockCarePlan.resourceType).thenReturn(CarePlan.resourceType)
        Mockito.`when`(mockTaggingService.appendDefaultTags(
                ArgumentMatchers.eq(CarePlan.resourceType),
                ArgumentMatchers.any<HashMap<String, String>>())
        ).thenReturn(mockTags)
        Mockito.`when`(mockCryptoService.generateGCKey()).thenReturn(Single.just(mockDataKey))
        Mockito.doReturn(mockDecryptedFhir3Record).`when`(recordService)
                ._uploadData(
                        mockDecryptedFhir3Record,
                        USER_ID
                )
        Mockito.doReturn(mockDecryptedFhir3Record).`when`(recordService)
                .removeUploadData(mockDecryptedFhir3Record)

        Mockito.doReturn(mockEncryptedRecord)
                .`when`(recordService).encryptRecord(
                        mockDecryptedFhir3Record
                )
        Mockito.`when`(mockApiService.createRecord(ALIAS, USER_ID, mockEncryptedRecord))
                .thenReturn(Single.just(mockEncryptedRecord))
        Mockito.doReturn(mockDecryptedFhir3Record)
                .`when`(recordService)
                .decryptRecord<DomainResource>(mockEncryptedRecord, USER_ID)
        Mockito.doReturn(mockDecryptedFhir3Record)
                .`when`(recordService).restoreUploadData(
                        mockDecryptedFhir3Record,
                        mockCarePlan,
                        mockUploadData
                )
        @Suppress("UNCHECKED_CAST")
        every { SdkRecordFactory.getInstance(mockDecryptedFhir3Record) } returns mockRecord as BaseRecord<DomainResource>

        val annotations = listOf<String>()

        // When
        val subscriber = recordService.createRecord(USER_ID, mockCarePlan, annotations).test().await()

        // Then
        val record = subscriber
                .assertNoErrors()
                .assertComplete()
                .assertValueCount(1)
                .values()[0]
        Truth.assertThat(record).isSameInstanceAs(mockRecord)

        inOrder.verify(mockTaggingService).appendDefaultTags(
                ArgumentMatchers.anyString(),
                ArgumentMatchers.any<HashMap<String, String>>()
        )
        inOrder.verify(mockCryptoService).generateGCKey()
        inOrder.verify(mockDecryptedRecordBuilder).build(
                mockCarePlan,
                mockTags,
                DATE_FORMATTER.format(LocalDate.now(UTC_ZONE_ID)),
                mockDataKey,
                ModelVersion.CURRENT
        )
        inOrder.verify(recordService)._uploadData(
                mockDecryptedFhir3Record,
                USER_ID
        )
        inOrder.verify(recordService).removeUploadData(mockDecryptedFhir3Record)

        inOrder.verify(recordService).encryptRecord(
                mockDecryptedFhir3Record
        )
        inOrder.verify(mockApiService).createRecord(ALIAS, USER_ID, mockEncryptedRecord)
        inOrder.verify(recordService).decryptRecord<DomainResource>(mockEncryptedRecord, USER_ID)
        inOrder.verify(recordService).restoreUploadData(
                mockDecryptedFhir3Record,
                mockCarePlan,
                mockUploadData
        )
        inOrder.verify(recordService).assignResourceId(mockDecryptedFhir3Record)
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
    fun `Given, createRecord is called a DomainResource, a UserId and no attachment, it returns a new Record`() {
        // Given
        every {
            anyConstructed<DecryptedRecordBuilderImpl>().setAnnotations(listOf())
        } returns mockDecryptedRecordBuilder as DecryptedRecordBuilderImpl

        @Suppress("UNCHECKED_CAST")
        Mockito.`when`(
                mockDecryptedRecordBuilder.build(
                        mockCarePlan,
                        mockTags,
                        DATE_FORMATTER.format(LocalDate.now(UTC_ZONE_ID)),
                        mockDataKey,
                        ModelVersion.CURRENT
                )
        ).thenReturn(mockDecryptedFhir3Record as DecryptedFhir3Record<CarePlan>)
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
        Mockito.doReturn(mockDecryptedFhir3Record)
                .`when`(recordService)
                ._uploadData(
                        mockDecryptedFhir3Record,
                        USER_ID
                )
        Mockito.doReturn(mockDecryptedFhir3Record)
                .`when`(recordService)
                .removeUploadData(mockDecryptedFhir3Record)

        Mockito.doReturn(mockEncryptedRecord).`when`(recordService)
                .encryptRecord(mockDecryptedFhir3Record)
        Mockito.`when`(mockApiService.createRecord(ALIAS, USER_ID, mockEncryptedRecord))
                .thenReturn(Single.just(mockEncryptedRecord))
        Mockito.doReturn(mockDecryptedFhir3Record)
                .`when`(recordService)
                .decryptRecord<DomainResource>(
                        mockEncryptedRecord,
                        USER_ID
                )
        Mockito.doReturn(mockDecryptedFhir3Record)
                .`when`(recordService)
                .restoreUploadData(
                        mockDecryptedFhir3Record,
                        mockCarePlan,
                        mockUploadData
                )

        Mockito.doReturn(mockDecryptedFhir3Record)
                .`when`(recordService)
                .uploadData(
                        mockDecryptedFhir3Record,
                        null,
                        USER_ID
                )
        @Suppress("UNCHECKED_CAST")
        every { SdkRecordFactory.getInstance(mockDecryptedFhir3Record) } returns mockRecord as BaseRecord<DomainResource>

        val annotations = listOf<String>()

        // When
        val subscriber = recordService.createRecord(USER_ID, mockCarePlan, annotations).test().await()

        // Then
        val record = subscriber
                .assertNoErrors()
                .assertComplete()
                .assertValueCount(1)
                .values()[0]
        Truth.assertThat(record).isSameInstanceAs(mockRecord)

        inOrder.verify(mockTaggingService).appendDefaultTags(
                ArgumentMatchers.anyString(),
                ArgumentMatchers.any<HashMap<String, String>>()
        )
        inOrder.verify(mockCryptoService).generateGCKey()
        inOrder.verify(mockDecryptedRecordBuilder).build(
                mockCarePlan,
                mockTags,
                DATE_FORMATTER.format(LocalDate.now(UTC_ZONE_ID)),
                mockDataKey,
                ModelVersion.CURRENT
        )
        inOrder.verify(recordService)._uploadData(
                mockDecryptedFhir3Record,
                USER_ID
        )
        inOrder.verify(recordService).removeUploadData(mockDecryptedFhir3Record)
        inOrder.verify(recordService).encryptRecord(mockDecryptedFhir3Record)
        inOrder.verify(mockApiService).createRecord(ALIAS, USER_ID, mockEncryptedRecord)
        inOrder.verify(recordService).decryptRecord<DomainResource>(mockEncryptedRecord, USER_ID)
        inOrder.verify(recordService).restoreUploadData(
                mockDecryptedFhir3Record,
                mockCarePlan,
                null
        )
        inOrder.verify(recordService).assignResourceId(mockDecryptedFhir3Record)
        inOrder.verifyNoMoreInteractions()
    }

    @Test
    @Throws(
            InterruptedException::class,
            DataRestrictionException.UnsupportedFileType::class,
            DataRestrictionException.MaxDataSizeViolation::class
    )
    fun `Given, createRecord is called with unsupported data, it throws an error`() {
        // Given
        val invalidData = byteArrayOf(0x00)
        val doc = buildDocumentReference(invalidData)
        val annotations = listOf<String>()

        // When
        try {
            recordService.createRecord(USER_ID, doc, annotations).test().await()
            Assert.fail("Exception expected!")
        } catch (ex: D4LException) {

            // Then
            Truth.assertThat(ex.javaClass).isEqualTo(DataRestrictionException.UnsupportedFileType::class.java)
        }
        inOrder.verify(recordService).createRecord(USER_ID, doc, annotations)
        inOrder.verify(recordService).checkDataRestrictions(doc)
        inOrder.verifyNoMoreInteractions()
    }

    @Test
    @Throws(
            InterruptedException::class,
            DataRestrictionException.UnsupportedFileType::class,
            DataRestrictionException.MaxDataSizeViolation::class
    )
    fun `Given, createData is called with data, which exceeds the file size limitation, it throws an error`() {
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
            recordService.createRecord(USER_ID, doc, annotations).test().await()
            Assert.fail("Exception expected!")
        } catch (ex: D4LException) {

            // Then
            Truth.assertThat(ex.javaClass).isEqualTo(DataRestrictionException.MaxDataSizeViolation::class.java)
        }
        inOrder.verify(recordService).createRecord(USER_ID, doc, annotations)
        inOrder.verify(recordService).checkDataRestrictions(doc)
        inOrder.verifyNoMoreInteractions()
    }

    @Test
    @Throws(
            InterruptedException::class,
            DataRestrictionException.UnsupportedFileType::class,
            DataRestrictionException.MaxDataSizeViolation::class
    )
    fun `Given, createRecords is called with multiple DomainResource and a UserId, it returns a multiple Records`() {
        // Given
        val resources = listOf(
                mockCarePlan as DomainResource,
                mockCarePlan as DomainResource
        )
        val annotations = listOf<String>()
        Mockito.doReturn(Single.just(mockRecord)).`when`(recordService).createRecord(USER_ID, mockCarePlan, annotations)

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
        inOrder.verify(recordService, Mockito.times(2)).createRecord(USER_ID, mockCarePlan, annotations)
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
    fun `Given, createRecord is called with a DomainResource, a UserId and Annotations, it returns a new Record`() {
        // Given
        every {
            anyConstructed<DecryptedRecordBuilderImpl>().setAnnotations(ANNOTATIONS)
        } returns mockDecryptedRecordBuilder as DecryptedRecordBuilderImpl

        @Suppress("UNCHECKED_CAST")
        Mockito.`when`(
                mockDecryptedRecordBuilder.build(
                        mockCarePlan,
                        mockTags,
                        DATE_FORMATTER.format(LocalDate.now(UTC_ZONE_ID)),
                        mockDataKey,
                        ModelVersion.CURRENT
                )
        ).thenReturn(mockAnnotatedDecryptedFhirRecord as DecryptedFhir3Record<CarePlan>)
        Mockito.doReturn(mockUploadData).`when`(recordService).extractUploadData(mockCarePlan)
        Mockito.`when`(mockCarePlan.resourceType).thenReturn(CarePlan.resourceType)
        Mockito.`when`(mockTaggingService.appendDefaultTags(
                ArgumentMatchers.eq(CarePlan.resourceType),
                ArgumentMatchers.any<HashMap<String, String>>())
        ).thenReturn(mockTags)
        Mockito.`when`(mockCryptoService.generateGCKey()).thenReturn(Single.just(mockDataKey))
        Mockito.doReturn(mockAnnotatedDecryptedFhirRecord).`when`(recordService)
                ._uploadData(
                        mockAnnotatedDecryptedFhirRecord,
                        USER_ID
                )
        Mockito.doReturn(mockAnnotatedDecryptedFhirRecord).`when`(recordService)
                .removeUploadData(mockAnnotatedDecryptedFhirRecord)

        Mockito.doReturn(mockAnnotatedEncryptedRecord)
                .`when`(recordService).encryptRecord(
                        mockAnnotatedDecryptedFhirRecord
                )
        Mockito.`when`(mockApiService.createRecord(ALIAS, USER_ID, mockAnnotatedEncryptedRecord))
                .thenReturn(Single.just(mockAnnotatedEncryptedRecord))
        Mockito.doReturn(mockAnnotatedDecryptedFhirRecord)
                .`when`(recordService)
                .decryptRecord<DomainResource>(mockAnnotatedEncryptedRecord, USER_ID)
        Mockito.doReturn(mockAnnotatedDecryptedFhirRecord)
                .`when`(recordService).restoreUploadData(
                        mockAnnotatedDecryptedFhirRecord,
                        mockCarePlan,
                        mockUploadData
                )
        @Suppress("UNCHECKED_CAST")
        every { SdkRecordFactory.getInstance(mockAnnotatedDecryptedFhirRecord) } returns mockRecord as BaseRecord<DomainResource>

        // When
        val subscriber = recordService.createRecord(USER_ID, mockCarePlan, ANNOTATIONS).test().await()

        // Then
        val record = subscriber
                .assertNoErrors()
                .assertComplete()
                .assertValueCount(1)
                .values()[0]

        Truth.assertThat(record).isSameInstanceAs(mockRecord)

        inOrder.verify(mockTaggingService).appendDefaultTags(
                ArgumentMatchers.anyString(),
                ArgumentMatchers.any<HashMap<String, String>>()
        )
        inOrder.verify(mockCryptoService).generateGCKey()
        inOrder.verify(mockDecryptedRecordBuilder).build(
                mockCarePlan,
                mockTags,
                DATE_FORMATTER.format(LocalDate.now(UTC_ZONE_ID)),
                mockDataKey,
                ModelVersion.CURRENT
        )
        inOrder.verify(recordService)._uploadData(
                mockAnnotatedDecryptedFhirRecord,
                USER_ID
        )
        inOrder.verify(recordService).removeUploadData(mockAnnotatedDecryptedFhirRecord)

        inOrder.verify(recordService).encryptRecord(
                mockAnnotatedDecryptedFhirRecord
        )
        inOrder.verify(mockApiService).createRecord(ALIAS, USER_ID, mockAnnotatedEncryptedRecord)
        inOrder.verify(recordService).decryptRecord<DomainResource>(mockAnnotatedEncryptedRecord, USER_ID)
        inOrder.verify(recordService).restoreUploadData(
                mockAnnotatedDecryptedFhirRecord,
                mockCarePlan,
                mockUploadData
        )
        inOrder.verify(recordService).assignResourceId(mockAnnotatedDecryptedFhirRecord)
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
    fun `Given, createRecords is called a DomainResource and a UserId, Annotations and without attachment, it returns a new Record`() {
        // Given
        every {
            anyConstructed<DecryptedRecordBuilderImpl>().setAnnotations(ANNOTATIONS)
        } returns mockDecryptedRecordBuilder as DecryptedRecordBuilderImpl

        @Suppress("UNCHECKED_CAST")
        Mockito.`when`(
                mockDecryptedRecordBuilder.build(
                        mockCarePlan,
                        mockTags,
                        DATE_FORMATTER.format(LocalDate.now(UTC_ZONE_ID)),
                        mockDataKey,
                        ModelVersion.CURRENT
                )
        ).thenReturn(mockAnnotatedDecryptedFhirRecord as DecryptedFhir3Record<CarePlan>)
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
        Mockito.doReturn(mockAnnotatedDecryptedFhirRecord)
                .`when`(recordService)
                ._uploadData(
                        mockAnnotatedDecryptedFhirRecord,
                        USER_ID
                )
        Mockito.doReturn(mockAnnotatedDecryptedFhirRecord)
                .`when`(recordService)
                .removeUploadData(mockAnnotatedDecryptedFhirRecord)

        Mockito.doReturn(mockAnnotatedEncryptedRecord).`when`(recordService)
                .encryptRecord(mockAnnotatedDecryptedFhirRecord)
        Mockito.`when`(mockApiService.createRecord(ALIAS, USER_ID, mockAnnotatedEncryptedRecord))
                .thenReturn(Single.just(mockAnnotatedEncryptedRecord))
        Mockito.doReturn(mockAnnotatedDecryptedFhirRecord)
                .`when`(recordService)
                .decryptRecord<DomainResource>(
                        mockAnnotatedEncryptedRecord,
                        USER_ID
                )
        Mockito.doReturn(mockAnnotatedDecryptedFhirRecord)
                .`when`(recordService)
                .restoreUploadData(
                        mockAnnotatedDecryptedFhirRecord,
                        mockCarePlan,
                        mockUploadData
                )

        Mockito.doReturn(mockAnnotatedDecryptedFhirRecord)
                .`when`(recordService)
                ._uploadData(
                        mockAnnotatedDecryptedFhirRecord,
                        USER_ID
                )
        @Suppress("UNCHECKED_CAST")
        every { SdkRecordFactory.getInstance(mockAnnotatedDecryptedFhirRecord) } returns mockRecord as BaseRecord<DomainResource>

        // When
        val subscriber = recordService.createRecord(USER_ID, mockCarePlan, ANNOTATIONS).test().await()

        // Then
        val record = subscriber
                .assertNoErrors()
                .assertComplete()
                .assertValueCount(1)
                .values()[0]

        Truth.assertThat(record).isSameInstanceAs(mockRecord)

        inOrder.verify(mockTaggingService).appendDefaultTags(
                ArgumentMatchers.anyString(),
                ArgumentMatchers.any<HashMap<String, String>>()
        )
        inOrder.verify(mockCryptoService).generateGCKey()
        inOrder.verify(mockDecryptedRecordBuilder).build(
                mockCarePlan,
                mockTags,
                DATE_FORMATTER.format(LocalDate.now(UTC_ZONE_ID)),
                mockDataKey,
                ModelVersion.CURRENT
        )
        inOrder.verify(recordService)._uploadData(
                mockAnnotatedDecryptedFhirRecord,
                USER_ID
        )
        inOrder.verify(recordService).removeUploadData(
                mockAnnotatedDecryptedFhirRecord
        )

        inOrder.verify(recordService).encryptRecord(
                mockAnnotatedDecryptedFhirRecord
        )
        inOrder.verify(mockApiService).createRecord(ALIAS, USER_ID, mockAnnotatedEncryptedRecord)
        inOrder.verify(recordService).decryptRecord<DomainResource>(mockAnnotatedEncryptedRecord, USER_ID)
        inOrder.verify(recordService).restoreUploadData(
                mockAnnotatedDecryptedFhirRecord,
                mockCarePlan,
                null
        )
        inOrder.verify(recordService).assignResourceId(mockAnnotatedDecryptedFhirRecord)
        inOrder.verifyNoMoreInteractions()
    }

    @Test
    @Throws(
            InterruptedException::class,
            DataRestrictionException.UnsupportedFileType::class,
            DataRestrictionException.MaxDataSizeViolation::class
    )
    fun `Given, createRecord is called with a unsupported Data, a UserId and Annotations, it throws an error`() {
        // Given
        val invalidData = byteArrayOf(0x00)
        val doc = buildDocumentReference(invalidData)

        // When
        try {
            recordService.createRecord(USER_ID, doc, ANNOTATIONS).test().await()
            Assert.fail("Exception expected!")
        } catch (ex: D4LException) {

            // Then
            Truth.assertThat(ex.javaClass).isEqualTo(DataRestrictionException.UnsupportedFileType::class.java)
        }
        inOrder.verify(recordService).createRecord(USER_ID, doc, ANNOTATIONS)
        inOrder.verify(recordService).checkDataRestrictions(doc)
        inOrder.verifyNoMoreInteractions()
    }

    @Test
    @Throws(
            InterruptedException::class,
            DataRestrictionException.UnsupportedFileType::class,
            DataRestrictionException.MaxDataSizeViolation::class
    )
    fun `Given, createRecord is called with data, which exceeds the file size limitation, a UserId and Annotations, it throws an error`() {
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
            recordService.createRecord(USER_ID, doc, ANNOTATIONS).test().await()
            Assert.fail("Exception expected!")
        } catch (ex: D4LException) {

            // Then
            Truth.assertThat(ex.javaClass).isEqualTo(DataRestrictionException.MaxDataSizeViolation::class.java)
        }
        inOrder.verify(recordService).createRecord(USER_ID, doc, ANNOTATIONS)
        inOrder.verify(recordService).checkDataRestrictions(doc)
        inOrder.verifyNoMoreInteractions()
    }

    @Test
    @Ignore
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
    fun `Given createRecord is called with a Byte resource, a UserId and Annotations, it returns a new DataRecord`() {
        // Given
        every {
            anyConstructed<DecryptedRecordBuilderImpl>().setAnnotations(ANNOTATIONS)
        } returns mockDecryptedRecordBuilder as DecryptedRecordBuilderImpl

        Mockito.`when`(mockTaggingService.appendDefaultTags(null, null))
                .thenReturn(mockTags)
        Mockito.`when`(mockCryptoService.generateGCKey()).thenReturn(Single.just(mockDataKey))
        @Suppress("UNCHECKED_CAST")
        Mockito.`when`(
                mockDecryptedRecordBuilder.build(
                        mockDataResource.value,
                        mockTags,
                        DATE_FORMATTER.format(LocalDate.now(UTC_ZONE_ID)),
                        mockDataKey,
                        ModelVersion.CURRENT
                )
        ).thenReturn(mockDecryptedDataRecord)
        Mockito.doReturn(mockEncryptedRecord)
                .`when`(recordService)
                .encryptRecord(mockDecryptedDataRecord)
        Mockito.`when`(mockApiService.createRecord(ALIAS, USER_ID, mockEncryptedRecord))
                .thenReturn(Single.just(mockEncryptedRecord))
        Mockito.doReturn(mockDecryptedDataRecord)
                .`when`(recordService)
                .decryptRecord<ByteArray>(mockEncryptedRecord, USER_ID)
        Mockito.doReturn(mockDecryptedDataRecord)
                .`when`(recordService)
                .assignResourceId(mockDecryptedDataRecord)
        @Suppress("UNCHECKED_CAST")
        every { SdkRecordFactory.getInstance(mockDecryptedDataRecord) } returns mockDataRecord

        // When
        val subscriber = recordService.createRecord(
                USER_ID,
                mockDataResource,
                ANNOTATIONS
        ).test().await()

        // Then
        val record = subscriber
                .assertNoErrors()
                .assertComplete()
                .assertValueCount(1)
                .values()[0]
        Truth.assertThat(record).isSameInstanceAs(mockDataRecord)

        inOrder.verify(mockTaggingService).appendDefaultTags(null, null)
        inOrder.verify(mockCryptoService).generateGCKey()
        inOrder.verify(mockDecryptedRecordBuilder).build(
                mockDataResource.value,
                mockTags,
                DATE_FORMATTER.format(LocalDate.now(UTC_ZONE_ID)),
                mockDataKey,
                ModelVersion.CURRENT
        )
        inOrder.verify(recordService)
                .encryptRecord(mockDecryptedDataRecord)//mockDecryptedDataRecord)
        inOrder.verify(mockApiService).createRecord(ALIAS, USER_ID, mockEncryptedRecord)
        inOrder.verify(recordService).decryptRecord<ByteArray>(mockEncryptedRecord, USER_ID)
        inOrder.verify(recordService).assignResourceId(mockDecryptedDataRecord)
        inOrder.verifyNoMoreInteractions()
    }
}

 */
