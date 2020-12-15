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
package care.data4life.sdk.attachment

import care.data4life.crypto.GCKey
import care.data4life.sdk.ApiService
import care.data4life.sdk.CryptoService
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkClass
import io.reactivex.Single
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers
import org.mockito.Mockito
import java.io.IOException
import java.util.*

class FileServiceTest {
    private lateinit var fileService: FileService
    private lateinit var apiService: ApiService
    private lateinit var cryptoService: CryptoService
    @Before
    @Throws(Exception::class)
    fun setUp() {
        apiService = Mockito.mock(ApiService::class.java)
        cryptoService = mockkClass(CryptoService::class)
        fileService = Mockito.spy(FileService(ALIAS, apiService, cryptoService))
    }

    @Test
    fun downloadFile_shouldReturnSingleWithByteArrayOrError() {
        // given
        Mockito.`when`(
                apiService.downloadDocument(
                        ArgumentMatchers.anyString(),
                        ArgumentMatchers.anyString(),
                        ArgumentMatchers.anyString())
        ).thenReturn(Single.just(RESULT))
        every { cryptoService.encrypt(any(), any()) } returns Single.just(RESULT)
        Mockito.`when`(
                apiService.downloadDocument(
                        ArgumentMatchers.anyString(),
                        ArgumentMatchers.anyString(),
                        ArgumentMatchers.anyString())
        ).thenReturn(Single.error(IOException()))

        // when
        val testSubscriber = fileService.downloadFile(Mockito.mock(GCKey::class.java), USER_ID, FILE_ID)
                .test()

        // then
        testSubscriber.assertError(Throwable::class.java)
    }

    @Test
    fun uploadFiles_shouldReturnTrueOrFalse() {
        // given
        every { cryptoService.encrypt(any(), any()) } returns Single.just(RESULT)
        Mockito.`when`(apiService.uploadDocument(
                ArgumentMatchers.anyString(),
                ArgumentMatchers.anyString(),
                ArgumentMatchers.any(ByteArray::class.java))
        ).thenReturn(Single.just(FILE_ID))

        // when
        val testSubscriber = fileService.uploadFile(Mockito.mock(GCKey::class.java), USER_ID, ByteArray(1))
                .test()
        Mockito.`when`(
                apiService.uploadDocument(
                        ArgumentMatchers.anyString(),
                        ArgumentMatchers.anyString(),
                        ArgumentMatchers.any())
        ).thenReturn(Single.error(Throwable()))
        val testSubscriber2 = fileService.uploadFile(Mockito.mock(GCKey::class.java), USER_ID, ByteArray(1))
                .test()

        // then
        Mockito.verify(apiService, Mockito.times(2))!!.uploadDocument(ALIAS, USER_ID, RESULT)
        testSubscriber
                .assertNoErrors()
                .assertComplete()
        testSubscriber2.assertError(Throwable::class.java)
    }

    @Test
    fun deleteFile() {
        Mockito.`when`(apiService.deleteDocument(ALIAS, USER_ID, FILE_ID)).thenReturn(Single.just(true))

        // when
        val subscriber = fileService.deleteFile(USER_ID, FILE_ID).test()

        // then
        subscriber.assertComplete()
                .assertNoErrors()
                .assertValue { it: Boolean? -> it!! }
    }

    @Test
    fun deleteFile_shouldFail() {
        Mockito.`when`(apiService.deleteDocument(ALIAS, USER_ID, FILE_ID)).thenReturn(Single.error(Throwable()))

        // when
        val subscriber = fileService.deleteFile(USER_ID, FILE_ID).test()

        // then
        subscriber.assertNotComplete()
                .assertError { obj: Throwable? -> Objects.nonNull(obj) }
    }

    companion object {
        private const val ALIAS = "alias"
        private const val FILE_ID = "fileId"
        private const val USER_ID = "userId"
        private val RESULT = ByteArray(1)
    }
}
