/*
 * Copyright (c) 2021 D4L data4life gGmbH / All rights reserved.
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

package care.data4life.sdk.attachment

import care.data4life.sdk.crypto.CryptoContract
import care.data4life.sdk.crypto.GCKey
import care.data4life.sdk.network.NetworkingContract
import care.data4life.sdk.test.util.GenericTestDataProvider.ALIAS
import care.data4life.sdk.test.util.GenericTestDataProvider.USER_ID
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.reactivex.Single
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame
import kotlin.test.assertTrue
import org.junit.Before
import org.junit.Test

class FileServiceTest {
    private val apiService: NetworkingContract.Service = mockk()
    private val cryptoService: CryptoContract.Service = mockk()
    private lateinit var service: AttachmentContract.FileService

    @Before
    fun setUp() {
        clearAllMocks()

        service = FileService(ALIAS, apiService, cryptoService)
    }

    @Test
    fun `it fulfils FileService`() {
        val service: Any = FileService(ALIAS, mockk(), mockk())

        assertTrue(service is AttachmentContract.FileService)
    }

    @Test
    fun `Given downloadFile is called with Key, UserId and a FileId, it downloads and decrypts the File from the FileStorage`() {
        // Given
        val key: GCKey = mockk()
        val userId = USER_ID
        val fileId = "id"

        val encryptedFile = ByteArray(42)
        val decryptedFile = ByteArray(23)

        every {
            apiService.downloadDocument(ALIAS, userId, fileId)
        } returns Single.just(encryptedFile)
        every { cryptoService.decrypt(key, encryptedFile) } returns Single.just(decryptedFile)

        // When
        val file = service.downloadFile(key, userId, fileId).blockingGet()

        // Then
        assertSame(
            actual = file,
            expected = decryptedFile
        )
    }

    @Test
    fun `Given downloadFile is called with Key, UserId and a FileId, it propagates errors`() {
        // Given
        val key: GCKey = mockk()
        val userId = USER_ID
        val fileId = "id"

        val errorMessage = "Happy error path"

        every {
            apiService.downloadDocument(ALIAS, userId, fileId)
        } returns Single.error(RuntimeException(errorMessage))

        // Then
        val error = assertFailsWith<RuntimeException> {
            // When
            service.downloadFile(key, userId, fileId).blockingGet()
        }

        assertEquals(
            actual = error.message,
            expected = "care.data4life.sdk.lang.FileException\$DownloadFailed: java.lang.RuntimeException: $errorMessage"
        )
    }

    @Test
    fun `Given uploadFile is called with Key, UserId and a File, it encrypts and uploads the File to the FileStorage while returning the FileId`() {
        // Given
        val key: GCKey = mockk()
        val userId = USER_ID
        val file = ByteArray(23)

        val encryptedFile = ByteArray(42)
        val expectedFileId = "id"

        every { cryptoService.encrypt(key, file) } returns Single.just(encryptedFile)
        every {
            apiService.uploadDocument(ALIAS, userId, encryptedFile)
        } returns Single.just(expectedFileId)

        // When
        val fileId = service.uploadFile(key, userId, file).blockingGet()

        // Then
        assertEquals(
            actual = fileId,
            expected = expectedFileId
        )
    }

    @Test
    fun `Given uploadFile is called with Key, UserId and a File, it propagates errors`() {
        // Given
        val key: GCKey = mockk()
        val userId = USER_ID
        val file = ByteArray(23)

        val errorMessage = "Happy error path"

        every {
            cryptoService.encrypt(key, file)
        } returns Single.error(RuntimeException(errorMessage))

        // Then
        val error = assertFailsWith<RuntimeException> {
            // When
            service.uploadFile(key, userId, file).blockingGet()
        }

        assertEquals(
            actual = error.message,
            expected = "care.data4life.sdk.lang.FileException\$UploadFailed: java.lang.RuntimeException: $errorMessage"
        )
    }

    @Test
    fun `Given deleteFile is called with a UserId and FileId, it delegates it to the ApiService and returns its result`() {
        // Given
        val userId = USER_ID
        val fileId = "id"

        val isDelete = true

        every { apiService.deleteDocument(ALIAS, userId, fileId) } returns Single.just(isDelete)

        // When
        val result = service.deleteFile(userId, fileId).blockingGet()

        // Then
        assertEquals(
            actual = result,
            expected = result
        )
    }
}
