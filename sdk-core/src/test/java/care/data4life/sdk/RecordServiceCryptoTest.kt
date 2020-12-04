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
import care.data4life.fhir.stu3.model.CarePlan
import care.data4life.fhir.stu3.model.DomainResource
import care.data4life.sdk.lang.D4LException
import care.data4life.sdk.lang.DataValidationException
import care.data4life.sdk.model.ModelVersion
import com.google.common.truth.Truth
import io.reactivex.Single
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

    @Test
    @Throws(IOException::class)
    fun encryptRecord_shouldReturnEncryptedRecord() {
        // Given
        val currentCommonKeyId = "currentCommonKeyId"
        Mockito.`when`(mockTagEncryptionService.encryptTags(mockTags))
                .thenReturn(mockEncryptedTags)
        Mockito.`when`(mockFhirService.encryptResource(mockDataKey, mockCarePlan))
                .thenReturn(ENCRYPTED_RESOURCE)
        Mockito.`when`(mockCryptoService.fetchCurrentCommonKey()).thenReturn(mockCommonKey)
        Mockito.`when`(mockCryptoService.currentCommonKeyId).thenReturn(currentCommonKeyId)
        Mockito.`when`(
                mockCryptoService.encryptSymmetricKey(
                        mockCommonKey,
                        KeyType.DATA_KEY,
                        mockDataKey)
        ).thenReturn(Single.just(mockEncryptedDataKey))
        //TODO add attachmentKey crypto mock

        // When
        val encryptedRecord = recordService.encryptRecord(mockDecryptedRecord)

        // Then
        Truth.assertThat(encryptedRecord.commonKeyId).isEqualTo(currentCommonKeyId)
        inOrder.verify(mockTagEncryptionService).encryptTags(mockTags)
        inOrder.verify(mockFhirService).encryptResource(mockDataKey, mockCarePlan)
        inOrder.verify(mockCryptoService).fetchCurrentCommonKey()
        inOrder.verify(mockCryptoService)
                .encryptSymmetricKey(mockCommonKey, KeyType.DATA_KEY, mockDataKey)
        inOrder.verifyNoMoreInteractions()
    }

    @Test
    @Throws(IOException::class, DataValidationException.ModelVersionNotSupported::class)
    fun decryptRecord_shouldReturnDecryptedRecord() {
        // Given
        val commonKeyId = "mockCommonKeyId"
        Mockito.`when`(mockEncryptedRecord.modelVersion).thenReturn(1)
        Mockito.`when`(mockEncryptedRecord.commonKeyId).thenReturn(commonKeyId)
        Mockito.`when`(mockTagEncryptionService.decryptTags(mockEncryptedTags))
                .thenReturn(mockTags)
        Mockito.`when`(mockCryptoService.hasCommonKey(ArgumentMatchers.anyString()))
                .thenReturn(true)
        Mockito.`when`(mockCryptoService.getCommonKeyById(ArgumentMatchers.anyString()))
                .thenReturn(mockCommonKey)
        Mockito.`when`(mockCryptoService.symDecryptSymmetricKey(mockCommonKey, mockEncryptedDataKey))
                .thenReturn(Single.just(mockDataKey))
        //TODO add attachmentKey decrypt mock
        Mockito.`when`<Any>(
                mockFhirService.decryptResource(
                        mockDataKey,
                        CarePlan.resourceType,
                        ENCRYPTED_RESOURCE)
        ).thenReturn(mockCarePlan)

        // When
        recordService.decryptRecord<DomainResource>(mockEncryptedRecord, USER_ID)

        // Then
        inOrder.verify(mockTagEncryptionService).decryptTags(mockEncryptedTags)
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
    fun decryptRecord_shouldThrow_forUnsupportedModelVersion() {
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
}
