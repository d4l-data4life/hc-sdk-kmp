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

import care.data4life.crypto.KeyType
import care.data4life.fhir.stu3.model.DomainResource
import care.data4life.sdk.data.DataResource
import care.data4life.sdk.lang.D4LException
import care.data4life.sdk.lang.DataValidationException
import care.data4life.sdk.model.ModelVersion
import care.data4life.sdk.network.model.DecryptedDataRecord
import care.data4life.sdk.network.model.DecryptedRecord
import care.data4life.sdk.util.Base64
import com.google.common.truth.Truth
import io.mockk.every
import io.mockk.mockkObject
import io.reactivex.Single
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers
import org.mockito.Mockito
import java.io.IOException

class RecordServiceCryptoTest : RecordServiceTestBase() {
    @Before
    fun setUp() {
        init()
    }

    @After
    fun tearDown() {
        stop()
    }

    @Test
    @Throws(IOException::class)
    fun `Given, encryptRecord is called with a DecryptedRecord, it returns a EncryptedRecord`() {
        // Given
        val currentCommonKeyId = "currentCommonKeyId"

        Mockito.`when`(mockTagEncryptionService.encryptAndEncodeTags(mockTags))
                .thenReturn(mockEncryptedTags)
        Mockito.`when`(mockTagEncryptionService.encryptAndEncodeAnnotations(ANNOTATIONS))
                .thenReturn(ANNOTATIONS)
        Mockito.`when`(mockFhirService._encryptResource(mockDataKey, mockCarePlan))
                .thenReturn(ENCRYPTED_RESOURCE)
        Mockito.`when`(mockCryptoService.fetchCurrentCommonKey()).thenReturn(mockCommonKey)
        Mockito.`when`(mockCryptoService.currentCommonKeyId).thenReturn(currentCommonKeyId)
        Mockito.`when`(
                mockCryptoService.encryptSymmetricKey(
                        mockCommonKey,
                        KeyType.DATA_KEY,
                        mockDataKey
                )
        ).thenReturn(Single.just(mockEncryptedDataKey))

        // When
        val encryptedRecord = recordService.encryptRecord(mockAnnotatedDecryptedFhirRecord)

        // Then
        Truth.assertThat(encryptedRecord.commonKeyId).isEqualTo(currentCommonKeyId)
        Truth.assertThat(encryptedRecord.encryptedAttachmentsKey).isNull()

        inOrder.verify(mockTagEncryptionService).encryptAndEncodeTags(mockTags)
        inOrder.verify(mockTagEncryptionService).encryptAndEncodeAnnotations(ANNOTATIONS)
        inOrder.verify(mockCryptoService).fetchCurrentCommonKey()
        inOrder.verify(mockCryptoService)
                .encryptSymmetricKey(mockCommonKey, KeyType.DATA_KEY, mockDataKey)
        inOrder.verify(mockFhirService)._encryptResource(mockDataKey, mockCarePlan)
        inOrder.verifyNoMoreInteractions()

        Mockito.verify((mockEncryptedTags as MutableList)).addAll(ANNOTATIONS)
    }

    @Test
    @Throws(IOException::class)
    fun `Given, encryptRecord is called with a DecryptedRecord, it adds a encrypted AttachmentKey, if the DecryptedRecord contains a AttachmentKey`() {
        // Given
        val currentCommonKeyId = "currentCommonKeyId"
        Mockito.`when`(mockAnnotatedDecryptedFhirRecord.attachmentsKey).thenReturn(mockAttachmentKey)
        Mockito.`when`(mockTagEncryptionService.encryptAndEncodeTags(mockTags))
                .thenReturn(mockEncryptedTags)
        Mockito.`when`(mockTagEncryptionService.encryptAndEncodeAnnotations(ANNOTATIONS))
                .thenReturn(mockEncryptedAnnotations)
        Mockito.`when`(mockFhirService._encryptResource(mockDataKey, mockCarePlan))
                .thenReturn(ENCRYPTED_RESOURCE)
        Mockito.`when`(mockCryptoService.fetchCurrentCommonKey()).thenReturn(mockCommonKey)
        Mockito.`when`(mockCryptoService.currentCommonKeyId).thenReturn(currentCommonKeyId)
        Mockito.`when`(
                mockCryptoService.encryptSymmetricKey(
                        mockCommonKey,
                        KeyType.DATA_KEY,
                        mockDataKey
                )
        ).thenReturn(Single.just(mockEncryptedDataKey))
        Mockito.`when`(
                mockCryptoService.encryptSymmetricKey(
                        mockCommonKey,
                        KeyType.ATTACHMENT_KEY,
                        mockAttachmentKey
                )
        ).thenReturn(Single.just(mockEncryptedAttachmentKey))

        // When
        val encryptedRecord = recordService.encryptRecord(mockAnnotatedDecryptedFhirRecord)

        // Then
        Truth.assertThat(encryptedRecord.commonKeyId).isEqualTo(currentCommonKeyId)
        Truth.assertThat(encryptedRecord.encryptedAttachmentsKey).isEqualTo(mockEncryptedAttachmentKey)

        inOrder.verify(mockTagEncryptionService).encryptAndEncodeTags(mockTags)
        inOrder.verify(mockTagEncryptionService).encryptAndEncodeAnnotations(ANNOTATIONS)
        inOrder.verify(mockCryptoService).fetchCurrentCommonKey()
        inOrder.verify(mockCryptoService)
                .encryptSymmetricKey(mockCommonKey, KeyType.DATA_KEY, mockDataKey)
        inOrder.verify(mockFhirService)._encryptResource(mockDataKey, mockCarePlan)
        inOrder.verify(mockCryptoService)
                .encryptSymmetricKey(mockCommonKey, KeyType.ATTACHMENT_KEY, mockAttachmentKey)
        inOrder.verifyNoMoreInteractions()

        Mockito.verify((mockEncryptedTags as MutableList)).addAll(mockEncryptedAnnotations)
    }

    @Test
    @Throws(IOException::class, DataValidationException.ModelVersionNotSupported::class)
    fun `Given, decryptRecord is called with a EncryptedRecord and UserId, it returns a DecryptedRecord`() {
        // Given
        val commonKeyId = "mockCommonKeyId"
        val modelVersion = 1

        Mockito.`when`(mockTags.containsKey("resourcetype")).thenReturn(true)
        Mockito.`when`(mockEncryptedRecord.modelVersion).thenReturn(modelVersion)
        Mockito.`when`(mockEncryptedRecord.commonKeyId).thenReturn(commonKeyId)
        Mockito.`when`(mockTagEncryptionService.decryptTags(mockEncryptedTags))
                .thenReturn(mockTags)
        Mockito.`when`(mockTagEncryptionService.decryptAnnotations(mockEncryptedTags))
                .thenReturn(ANNOTATIONS)
        Mockito.`when`(mockCryptoService.hasCommonKey(ArgumentMatchers.anyString()))
                .thenReturn(true)
        Mockito.`when`(mockCryptoService.getCommonKeyById(ArgumentMatchers.anyString()))
                .thenReturn(mockCommonKey)
        Mockito.`when`(mockCryptoService.symDecryptSymmetricKey(mockCommonKey, mockEncryptedDataKey))
                .thenReturn(Single.just(mockDataKey))
        Mockito.`when`<Any>(
                mockFhirService.decryptResource(
                        mockDataKey,
                        mockTags,
                        ENCRYPTED_RESOURCE)
        ).thenReturn(mockCarePlan)

        // When
        val decrypted = recordService.decryptRecord<DomainResource>(mockEncryptedRecord, USER_ID)

        // Then
        Truth.assertThat(decrypted).isEqualTo(
                DecryptedRecord(
                        RECORD_ID,
                        mockCarePlan,
                        mockTags,
                        ANNOTATIONS,
                        CREATION_DATE,
                        null,
                        mockDataKey,
                        null,
                        modelVersion
                )
        )
        inOrder.verify(mockTagEncryptionService).decryptTags(mockEncryptedTags)
        inOrder.verify(mockTagEncryptionService).decryptAnnotations(mockEncryptedTags)
        inOrder.verify(mockCryptoService).hasCommonKey(commonKeyId)
        inOrder.verify(mockCryptoService).getCommonKeyById(commonKeyId)
        inOrder.verify(mockCryptoService).symDecryptSymmetricKey(mockCommonKey, mockEncryptedDataKey)
        inOrder.verify(mockFhirService).decryptResource<DomainResource>(
                mockDataKey,
                mockTags,
                ENCRYPTED_RESOURCE
        )
        inOrder.verifyNoMoreInteractions()
    }

    @Test
    @Throws(IOException::class)
    fun `Given, decryptRecord is called with a EncryptedRecord and UserId, it throws an error, if the ModelVersion is not supported`() {
        // Given
        Mockito.`when`(mockEncryptedRecord.modelVersion).thenReturn(ModelVersion.CURRENT + 1)

        // When
        try {
            recordService.decryptRecord<DomainResource>(mockEncryptedRecord, USER_ID)
            Assert.fail("Exception expected!")
        } catch (e: D4LException) {
            // Then
            Truth.assertThat(e.javaClass).isEqualTo(DataValidationException.ModelVersionNotSupported::class.java)
            Truth.assertThat(e.message).isEqualTo("Please update SDK to latest version!")
        }
    }

    @Test
    @Throws(IOException::class, DataValidationException.ModelVersionNotSupported::class)
    fun `Given, decryptRecord is called with a EncryptedRecord and UserId, it adds a decrypted AttachmentKey, if the EncryptedRecord contains a encrypted AttachmentKey`() {
        // Given
        val commonKeyId = "mockCommonKeyId"
        val modelVersion = 1

        Mockito.`when`(mockTags.containsKey("resourcetype")).thenReturn(true)
        Mockito.`when`(mockEncryptedRecord.modelVersion).thenReturn(modelVersion)
        Mockito.`when`(mockEncryptedRecord.commonKeyId).thenReturn(commonKeyId)
        Mockito.`when`(mockEncryptedRecord.encryptedAttachmentsKey).thenReturn(mockEncryptedAttachmentKey)
        Mockito.`when`(mockTagEncryptionService.decryptTags(mockEncryptedTags))
                .thenReturn(mockTags)
        Mockito.`when`(mockTagEncryptionService.decryptAnnotations(mockEncryptedTags))
                .thenReturn(ANNOTATIONS)
        Mockito.`when`(mockCryptoService.hasCommonKey(ArgumentMatchers.anyString()))
                .thenReturn(true)
        Mockito.`when`(mockCryptoService.getCommonKeyById(ArgumentMatchers.anyString()))
                .thenReturn(mockCommonKey)
        Mockito.`when`(mockCryptoService.symDecryptSymmetricKey(mockCommonKey, mockEncryptedDataKey))
                .thenReturn(Single.just(mockDataKey))
        Mockito.`when`(mockCryptoService.symDecryptSymmetricKey(mockCommonKey, mockEncryptedAttachmentKey))
                .thenReturn(Single.just(mockAttachmentKey))
        Mockito.`when`<Any>(
                mockFhirService.decryptResource(
                        mockDataKey,
                        mockTags,
                        ENCRYPTED_RESOURCE)
        ).thenReturn(mockCarePlan)

        // When
        val decrypted = recordService.decryptRecord<DomainResource>(mockEncryptedRecord, USER_ID)

        // Then
        Truth.assertThat(decrypted).isEqualTo(
                DecryptedRecord(
                        RECORD_ID,
                        mockCarePlan,
                        mockTags,
                        ANNOTATIONS,
                        CREATION_DATE,
                        null,
                        mockDataKey,
                        mockAttachmentKey,
                        modelVersion
                )
        )

        inOrder.verify(mockTagEncryptionService).decryptTags(mockEncryptedTags)
        inOrder.verify(mockTagEncryptionService).decryptAnnotations(mockEncryptedTags)
        inOrder.verify(mockCryptoService).hasCommonKey(commonKeyId)
        inOrder.verify(mockCryptoService).getCommonKeyById(commonKeyId)
        inOrder.verify(mockCryptoService).symDecryptSymmetricKey(mockCommonKey, mockEncryptedDataKey)
        inOrder.verify(mockCryptoService).symDecryptSymmetricKey(mockCommonKey, mockEncryptedAttachmentKey)
        inOrder.verify(mockFhirService).decryptResource<DomainResource>(
                mockDataKey,
                mockTags,
                ENCRYPTED_RESOURCE
        )
        inOrder.verifyNoMoreInteractions()
    }

    @Test
    @Throws(IOException::class, DataValidationException.ModelVersionNotSupported::class)
    fun `Given, decryptRecord is called with a EncryptedRecord and UserId, it adds a UpdateDate, if the EncryptedRecord contains a UpdateDate`() {
        // Given
        val commonKeyId = "mockCommonKeyId"
        val modelVersion = 1
        val updateDate = "2000-01-01"

        Mockito.`when`(mockTags.containsKey("resourcetype")).thenReturn(true)
        Mockito.`when`(mockEncryptedRecord.modelVersion).thenReturn(modelVersion)
        Mockito.`when`(mockEncryptedRecord.commonKeyId).thenReturn(commonKeyId)
        Mockito.`when`(mockEncryptedRecord.encryptedAttachmentsKey).thenReturn(mockEncryptedAttachmentKey)
        Mockito.`when`(mockEncryptedRecord.updatedDate).thenReturn(updateDate)
        Mockito.`when`(mockTagEncryptionService.decryptTags(mockEncryptedTags))
                .thenReturn(mockTags)
        Mockito.`when`(mockTagEncryptionService.decryptAnnotations(mockEncryptedTags))
                .thenReturn(ANNOTATIONS)
        Mockito.`when`(mockCryptoService.hasCommonKey(ArgumentMatchers.anyString()))
                .thenReturn(true)
        Mockito.`when`(mockCryptoService.getCommonKeyById(ArgumentMatchers.anyString()))
                .thenReturn(mockCommonKey)
        Mockito.`when`(mockCryptoService.symDecryptSymmetricKey(mockCommonKey, mockEncryptedDataKey))
                .thenReturn(Single.just(mockDataKey))
        Mockito.`when`(mockCryptoService.symDecryptSymmetricKey(mockCommonKey, mockEncryptedAttachmentKey))
                .thenReturn(Single.just(mockAttachmentKey))
        Mockito.`when`<Any>(
                mockFhirService.decryptResource(
                        mockDataKey,
                        mockTags,
                        ENCRYPTED_RESOURCE)
        ).thenReturn(mockCarePlan)

        // When
        val decrypted = recordService.decryptRecord<DomainResource>(mockEncryptedRecord, USER_ID)

        // Then
        Truth.assertThat(decrypted).isEqualTo(
                DecryptedRecord(
                        RECORD_ID,
                        mockCarePlan,
                        mockTags,
                        ANNOTATIONS,
                        CREATION_DATE,
                        updateDate,
                        mockDataKey,
                        mockAttachmentKey,
                        modelVersion
                )
        )

        /*inOrder.verify(mockTagEncryptionService).decryptTags(mockEncryptedTags)
        inOrder.verify(mockTagEncryptionService).decryptAnnotations(mockEncryptedTags)
        inOrder.verify(mockCryptoService).hasCommonKey(commonKeyId)
        inOrder.verify(mockCryptoService).getCommonKeyById(commonKeyId)
        inOrder.verify(mockCryptoService).symDecryptSymmetricKey(mockCommonKey, mockEncryptedDataKey)
        inOrder.verify(mockCryptoService).symDecryptSymmetricKey(mockCommonKey, mockEncryptedAttachmentKey)
        inOrder.verify(mockFhirService).decryptResource<DomainResource>(
                mockDataKey,
                CarePlan.resourceType,
                ENCRYPTED_RESOURCE
        )
        inOrder.verifyNoMoreInteractions()*/
    }

    @Test
    @Throws(IOException::class)
    fun `Given, encryptDataRecord is called with a DecryptedAppDataRecord, it returns a EncryptedRecord`() {
        // Given
        val currentCommonKeyId = "currentCommonKeyId"
        Mockito.`when`(mockTagEncryptionService.encryptAndEncodeTags(mockTags))
                .thenReturn(mockEncryptedTags)
        Mockito.`when`(mockTagEncryptionService.encryptAndEncodeAnnotations(ANNOTATIONS))
                .thenReturn(ANNOTATIONS)
        //Mockito.`when`(mockCryptoService.encrypt(mockDataKey, mockDataResource.value))
        //        .thenReturn(Single.just(ENCRYPTED_APPDATA))
        Mockito.`when`(mockCryptoService.fetchCurrentCommonKey()).thenReturn(mockCommonKey)
        Mockito.`when`(mockCryptoService.currentCommonKeyId).thenReturn(currentCommonKeyId)
        Mockito.`when`(
                mockCryptoService.encryptSymmetricKey(
                        mockCommonKey,
                        KeyType.DATA_KEY,
                        mockDataKey
                )
        ).thenReturn(Single.just(mockEncryptedDataKey))

        // When
        val encryptedRecord = recordService.encryptRecord(mockDecryptedDataRecord)

        // Then
        Truth.assertThat(encryptedRecord.commonKeyId).isEqualTo(currentCommonKeyId)
        Truth.assertThat(encryptedRecord.encryptedAttachmentsKey).isNull()

        /*
        inOrder.verify(mockTagEncryptionService).encryptTags(mockTags)
        inOrder.verify(mockTagEncryptionService).encryptAnnotations(ANNOTATIONS)
        inOrder.verify(mockCryptoService).fetchCurrentCommonKey()
        inOrder.verify(mockCryptoService)
                .encryptSymmetricKey(mockCommonKey, KeyType.DATA_KEY, mockDataKey)
        inOrder.verify(mockCryptoService).encrypt(mockDataKey, mockDataResource.value)
        inOrder.verifyNoMoreInteractions()

        Mockito.verify((mockEncryptedTags as MutableList)).addAll(ANNOTATIONS)*/
    }

    @Test
    @Throws(IOException::class, DataValidationException.ModelVersionNotSupported::class)
    fun `Given, decryptRecord is called with a EncryptedRecord and UserId, which encrypts a DataResource, it returns a DecryptedRecord`() {
        // Given
        val commonKeyId = "mockCommonKeyId"
        val modelVersion = 1

        Mockito.`when`(mockEncryptedRecord.modelVersion).thenReturn(modelVersion)
        Mockito.`when`(mockEncryptedRecord.commonKeyId).thenReturn(commonKeyId)
        Mockito.`when`(mockTagEncryptionService.decryptTags(mockEncryptedTags))
                .thenReturn(mockTags)
        Mockito.`when`(mockTagEncryptionService.decryptAnnotations(mockEncryptedTags))
                .thenReturn(ANNOTATIONS)
        Mockito.`when`(mockCryptoService.hasCommonKey(ArgumentMatchers.anyString()))
                .thenReturn(true)
        Mockito.`when`(mockCryptoService.getCommonKeyById(ArgumentMatchers.anyString()))
                .thenReturn(mockCommonKey)
        Mockito.`when`(mockCryptoService.symDecryptSymmetricKey(mockCommonKey, mockEncryptedDataKey))
                .thenReturn(Single.just(mockDataKey))
        Mockito.`when`(mockFhirService.decryptResource<DataResource>(
                mockDataKey,
                mockTags,
                ENCRYPTED_RESOURCE
        )).thenReturn(mockDataResource)

        // When
        val decrypted = recordService.decryptRecord<ByteArray>(mockEncryptedRecord, USER_ID)

        // Then
        Truth.assertThat(decrypted).isEqualTo(
                DecryptedDataRecord(
                        RECORD_ID,
                        mockDataResource,
                        mockTags,
                        ANNOTATIONS,
                        CREATION_DATE,
                        null,
                        mockDataKey,
                        modelVersion
                )
        )

        /*inOrder.verify(mockTagEncryptionService).decryptTags(mockEncryptedTags)
        inOrder.verify(mockTagEncryptionService).decryptAnnotations(mockEncryptedTags)
        inOrder.verify(mockCryptoService).hasCommonKey(commonKeyId)
        inOrder.verify(mockCryptoService).getCommonKeyById(commonKeyId)
        inOrder.verify(mockCryptoService).symDecryptSymmetricKey(mockCommonKey, mockEncryptedDataKey)
        inOrder.verify(mockFhirService.decryptResource<DataResource>(
                mockDataKey,
                mockTags,
                ENCRYPTED_RESOURCE
        ))
        inOrder.verifyNoMoreInteractions()*/
    }

    @Test
    @Throws(IOException::class, DataValidationException.ModelVersionNotSupported::class)
    fun `Given, decryptRecord for DataResource is called with a EncryptedRecord and UserId, it adds a UpdateDate, if the EncryptedRecord contains a UpdateDate`() {
        // Given
        val commonKeyId = "mockCommonKeyId"
        val modelVersion = 1
        val updateDate = "2000-01-01"

        mockkObject(Base64)
        every { Base64.decode(ENCRYPTED_RESOURCE) } returns ENCRYPTED_APPDATA

        Mockito.`when`(mockEncryptedRecord.modelVersion).thenReturn(modelVersion)
        Mockito.`when`(mockEncryptedRecord.commonKeyId).thenReturn(commonKeyId)
        Mockito.`when`(mockEncryptedRecord.updatedDate).thenReturn(updateDate)
        Mockito.`when`(mockTagEncryptionService.decryptTags(mockEncryptedTags))
                .thenReturn(mockTags)
        Mockito.`when`(mockTagEncryptionService.decryptAnnotations(mockEncryptedTags))
                .thenReturn(ANNOTATIONS)
        Mockito.`when`(mockCryptoService.hasCommonKey(ArgumentMatchers.anyString()))
                .thenReturn(true)
        Mockito.`when`(mockCryptoService.getCommonKeyById(ArgumentMatchers.anyString()))
                .thenReturn(mockCommonKey)
        Mockito.`when`(mockCryptoService.symDecryptSymmetricKey(mockCommonKey, mockEncryptedDataKey))
                .thenReturn(Single.just(mockDataKey))
        Mockito.`when`(mockFhirService.decryptResource<DataResource>(
                mockDataKey,
                mockTags,
                ENCRYPTED_RESOURCE
        )).thenReturn(mockDataResource)

        // When
        val decrypted = recordService.decryptRecord<ByteArray>(mockEncryptedRecord, USER_ID)

        // Then
        Truth.assertThat(decrypted).isEqualTo(
                DecryptedDataRecord(
                        RECORD_ID,
                        mockDataResource,
                        mockTags,
                        ANNOTATIONS,
                        CREATION_DATE,
                        updateDate,
                        mockDataKey,
                        modelVersion
                )
        )

        /*inOrder.verify(mockTagEncryptionService).decryptTags(mockEncryptedTags)
        inOrder.verify(mockTagEncryptionService).decryptAnnotations(mockEncryptedTags)
        inOrder.verify(mockCryptoService).hasCommonKey(commonKeyId)
        inOrder.verify(mockCryptoService).getCommonKeyById(commonKeyId)
        inOrder.verify(mockCryptoService).symDecryptSymmetricKey(mockCommonKey, mockEncryptedDataKey)
        inOrder.verify(mockFhirService.decryptResource<DataResource>(
                mockDataKey,
                mockTags,
                ENCRYPTED_RESOURCE
        ))
        inOrder.verifyNoMoreInteractions()*/
    }

    @Test
    @Throws(IOException::class)
    fun `Given, decryptRecord for ByteArray is called with a EncryptedRecord and UserId, it throws an error, if the ModelVersion is not supported`() {
        // Given
        Mockito.`when`(mockEncryptedRecord.modelVersion).thenReturn(ModelVersion.CURRENT + 1)

        // When
        try {
            recordService.decryptRecord<ByteArray>(mockEncryptedRecord, USER_ID)
            Assert.fail("Exception expected!")
        } catch (e: D4LException) {
            // Then
            Truth.assertThat(e.javaClass).isEqualTo(DataValidationException.ModelVersionNotSupported::class.java)
            Truth.assertThat(e.message).isEqualTo("Please update SDK to latest version!")
        }
    }
}
