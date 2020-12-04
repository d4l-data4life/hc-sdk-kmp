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

import care.data4life.sdk.util.Base64
import care.data4life.crypto.KeyType
import care.data4life.fhir.stu3.model.CarePlan
import care.data4life.fhir.stu3.model.DomainResource
import care.data4life.sdk.lang.D4LException
import care.data4life.sdk.lang.DataValidationException
import care.data4life.sdk.model.ModelVersion
import com.google.common.truth.Truth
import io.mockk.every
import io.mockk.mockkObject
import io.mockk.unmockkObject
import io.reactivex.Single
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers
import org.mockito.Mockito
import java.io.IOException

class RecordServiceCryptoTest: RecordServiceTestBase() {
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
    fun `Given a DecryptedRecord, encryptRecord returns a EncryptedRecord`() {
        // Given
        val currentCommonKeyId = "currentCommonKeyId"
        Mockito.`when`(mockTagEncryptionService.encryptTags(mockTags))
                .thenReturn(mockEncryptedTags)
        Mockito.`when`(mockTagEncryptionService.encryptAnnotations(ANNOTATIONS))
                .thenReturn(ANNOTATIONS)
        Mockito.`when`(mockFhirService.encryptResource(mockDataKey, mockCarePlan))
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
        val encryptedRecord = recordService.encryptRecord(mockAnnotatedDecryptedRecord)

        // Then
        Truth.assertThat(encryptedRecord.commonKeyId).isEqualTo(currentCommonKeyId)
        Truth.assertThat(encryptedRecord.encryptedAttachmentsKey).isNull()
        inOrder.verify(mockTagEncryptionService).encryptTags(mockTags)
        inOrder.verify(mockTagEncryptionService).encryptAnnotations(ANNOTATIONS)
        inOrder.verify(mockFhirService).encryptResource(mockDataKey, mockCarePlan)
        inOrder.verify(mockCryptoService).fetchCurrentCommonKey()
        inOrder.verify(mockCryptoService)
                .encryptSymmetricKey(mockCommonKey, KeyType.DATA_KEY, mockDataKey)
        inOrder.verifyNoMoreInteractions()

        Mockito.verify((mockEncryptedTags as MutableList)).addAll(ANNOTATIONS)
    }

    @Test
    @Throws(IOException::class)
    fun `Given a DecryptedRecord, encryptRecord adds a encrypted AttachmentKey, if the DecryptedRecord contains a AttachmentKey`() {
        // Given
        val currentCommonKeyId = "currentCommonKeyId"
        Mockito.`when`(mockAnnotatedDecryptedRecord.attachmentsKey).thenReturn(mockAttachmentKey)
        Mockito.`when`(mockTagEncryptionService.encryptTags(mockTags))
                .thenReturn(mockEncryptedTags)
        Mockito.`when`(mockTagEncryptionService.encryptAnnotations(ANNOTATIONS))
                .thenReturn(mockEncryptedAnnotations)
        Mockito.`when`(mockFhirService.encryptResource(mockDataKey, mockCarePlan))
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
        val encryptedRecord = recordService.encryptRecord(mockAnnotatedDecryptedRecord)

        // Then
        Truth.assertThat(encryptedRecord.commonKeyId).isEqualTo(currentCommonKeyId)
        Truth.assertThat(encryptedRecord.encryptedAttachmentsKey).isEqualTo(mockEncryptedAttachmentKey)

        inOrder.verify(mockTagEncryptionService).encryptTags(mockTags)
        inOrder.verify(mockTagEncryptionService).encryptAnnotations(ANNOTATIONS)
        inOrder.verify(mockFhirService).encryptResource(mockDataKey, mockCarePlan)
        inOrder.verify(mockCryptoService).fetchCurrentCommonKey()
        inOrder.verify(mockCryptoService)
                .encryptSymmetricKey(mockCommonKey, KeyType.DATA_KEY, mockDataKey)
        inOrder.verify(mockCryptoService)
                .encryptSymmetricKey(mockCommonKey, KeyType.ATTACHMENT_KEY, mockAttachmentKey)
        inOrder.verifyNoMoreInteractions()

        Mockito.verify((mockEncryptedTags as MutableList)).addAll(mockEncryptedAnnotations)
    }

    @Test
    @Throws(IOException::class, DataValidationException.ModelVersionNotSupported::class)
    fun `Given a EncryptedRecord and UserId, decryptRecord returns a DecryptedRecord`() {
        // Given
        val commonKeyId = "mockCommonKeyId"
        Mockito.`when`(mockEncryptedRecord.modelVersion).thenReturn(1)
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
                        CarePlan.resourceType,
                        ENCRYPTED_RESOURCE)
        ).thenReturn(mockCarePlan)

        // When
        val decrypted = recordService.decryptRecord<DomainResource>(mockEncryptedRecord, USER_ID)

        // Then
        Truth.assertThat(decrypted.annotations).isEqualTo(ANNOTATIONS)
        Truth.assertThat(decrypted.attachmentsKey).isNull()
        inOrder.verify(mockTagEncryptionService).decryptTags(mockEncryptedTags)
        inOrder.verify(mockTagEncryptionService).decryptAnnotations(mockEncryptedTags)
        inOrder.verify(mockCryptoService).hasCommonKey(commonKeyId)
        inOrder.verify(mockCryptoService).getCommonKeyById(commonKeyId)
        inOrder.verify(mockCryptoService).symDecryptSymmetricKey(mockCommonKey, mockEncryptedDataKey)
        inOrder.verify(mockFhirService).decryptResource<DomainResource>(
                mockDataKey,
                CarePlan.resourceType,
                ENCRYPTED_RESOURCE
        )
        inOrder.verifyNoMoreInteractions()
    }

    @Test
    @Throws(IOException::class)
    fun `Given a EncryptedRecord and UserId, decryptRecord throws an error, if the ModelVersion is not supported`() {
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
    fun `Given a EncryptedRecord and UserId, decryptRecord adds a decrypted AttachmentKey, if the EncryptedRecord contains a encrypted AttachmentKey`() {
        // Given
        val commonKeyId = "mockCommonKeyId"
        Mockito.`when`(mockEncryptedRecord.modelVersion).thenReturn(1)
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
                        CarePlan.resourceType,
                        ENCRYPTED_RESOURCE)
        ).thenReturn(mockCarePlan)

        // When
        val decrypted = recordService.decryptRecord<DomainResource>(mockEncryptedRecord, USER_ID)

        // Then
        Truth.assertThat(decrypted.annotations).isEqualTo(ANNOTATIONS)
        Truth.assertThat(decrypted.attachmentsKey).isEqualTo(mockAttachmentKey)
        inOrder.verify(mockTagEncryptionService).decryptTags(mockEncryptedTags)
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
        inOrder.verifyNoMoreInteractions()
    }

    @Test
    @Throws(IOException::class)
    fun `Given a DecryptedAppDataRecord, encryptAppDataRecord returns a EncryptedRecord`() {
        // Given
        val currentCommonKeyId = "currentCommonKeyId"
        Mockito.`when`(mockTagEncryptionService.encryptTags(mockTags))
                .thenReturn(mockEncryptedTags)
        Mockito.`when`(mockTagEncryptionService.encryptAnnotations(ANNOTATIONS))
                .thenReturn(ANNOTATIONS)
        Mockito.`when`(mockCryptoService.encrypt(mockDataKey, mockAppData))
                .thenReturn(ENCRYPTED_APPDATA)
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
        val encryptedRecord = recordService.encryptAppDataRecord(mockAnnotatedDecryptedAppDataRecord)

        // Then
        Truth.assertThat(encryptedRecord.commonKeyId).isEqualTo(currentCommonKeyId)
        Truth.assertThat(encryptedRecord.encryptedAttachmentsKey).isNull()
        inOrder.verify(mockTagEncryptionService).encryptTags(mockTags)
        inOrder.verify(mockTagEncryptionService).encryptAnnotations(ANNOTATIONS)
        inOrder.verify(mockCryptoService).encrypt(mockDataKey, mockAppData)
        inOrder.verify(mockCryptoService).fetchCurrentCommonKey()
        inOrder.verify(mockCryptoService)
                .encryptSymmetricKey(mockCommonKey, KeyType.DATA_KEY, mockDataKey)
        inOrder.verifyNoMoreInteractions()

        Mockito.verify((mockEncryptedTags as MutableList)).addAll(ANNOTATIONS)
    }

    @Test
    @Throws(IOException::class, DataValidationException.ModelVersionNotSupported::class)
    fun `Given a EncryptedRecord and UserId, decryptAppDataRecord returns a DecryptedRecord`() {
        // Given
        mockkObject(Base64)
        every { Base64.decode(ENCRYPTED_RESOURCE) } returns ENCRYPTED_APPDATA.blockingGet()
        val commonKeyId = "mockCommonKeyId"
        Mockito.`when`(mockEncryptedRecord.modelVersion).thenReturn(1)
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
                mockCryptoService.decrypt(
                        mockDataKey,
                        ENCRYPTED_APPDATA.blockingGet()
                )
        ).thenReturn(Single.just(mockAppData))

        // When
        val decrypted = recordService.decryptAppDataRecord(mockEncryptedRecord, USER_ID)

        // Then
        Truth.assertThat(decrypted.annotations).isEqualTo(ANNOTATIONS)
        inOrder.verify(mockTagEncryptionService).decryptTags(mockEncryptedTags)
        inOrder.verify(mockTagEncryptionService).decryptAnnotations(mockEncryptedTags)
        inOrder.verify(mockCryptoService).hasCommonKey(commonKeyId)
        inOrder.verify(mockCryptoService).getCommonKeyById(commonKeyId)
        inOrder.verify(mockCryptoService).symDecryptSymmetricKey(mockCommonKey, mockEncryptedDataKey)
        inOrder.verify(mockCryptoService).decrypt(
                mockDataKey,
                ENCRYPTED_APPDATA.blockingGet()
        )
        inOrder.verifyNoMoreInteractions()
    }

    @Test
    @Throws(IOException::class)
    fun `Given a EncryptedRecord and UserId, decryptAppDataRecord throws an error, if the ModelVersion is not supported`() {
        // Given
        Mockito.`when`(mockEncryptedRecord.modelVersion).thenReturn(ModelVersion.CURRENT + 1)

        // When
        try {
            recordService.decryptAppDataRecord(mockEncryptedRecord, USER_ID)
            Assert.fail("Exception expected!")
        } catch (e: D4LException) {
            // Then
            Truth.assertThat(e.javaClass).isEqualTo(DataValidationException.ModelVersionNotSupported::class.java)
            Truth.assertThat(e.message).isEqualTo("Please update SDK to latest version!")
        }
    }
}
