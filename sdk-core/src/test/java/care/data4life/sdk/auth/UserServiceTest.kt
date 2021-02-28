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

package care.data4life.sdk.auth

import care.data4life.auth.AuthorizationService
import care.data4life.crypto.GCKey
import care.data4life.crypto.GCKeyPair
import care.data4life.crypto.GCSymmetricKey
import care.data4life.sdk.ApiService
import care.data4life.sdk.CryptoSecureStore
import care.data4life.sdk.CryptoService
import care.data4life.sdk.log.Log
import care.data4life.sdk.network.model.EncryptedKey
import care.data4life.sdk.network.model.UserInfo
import care.data4life.sdk.network.model.VersionList
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.verify
import io.reactivex.Completable
import io.reactivex.Single
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers
import org.mockito.Mockito
import org.mockito.Mockito.any
import org.mockito.Mockito.mock

class UserServiceTest {

    companion object {
        private const val AUTH_STATE = "authState"
        private const val USER_ALIAS = "userAlias"
        private const val KEY_USER_ID = "user_id"
        private const val AUTH_TOKEN = "authToken"
    }

    private lateinit var apiService: ApiService
    private lateinit var storage: CryptoSecureStore
    private lateinit var userService: UserService
    private lateinit var cryptoService: CryptoService
    private lateinit var authService: AuthorizationService

    @Before
    fun setUp() {
        authService = mock(AuthorizationService::class.java)
        userService = mock(UserService::class.java)
        storage = mock(CryptoSecureStore::class.java)
        apiService = mock(ApiService::class.java)
        cryptoService = mock(CryptoService::class.java)
        userService =
            Mockito.spy(UserService(USER_ALIAS, authService, apiService, storage, cryptoService))
    }

    @Test
    fun `isLoggedIn_shouldReturnTrue`() {
        //given
        coEvery { authService.isAuthorized(USER_ALIAS) } returns true
        Mockito.`when`(storage.getSecret(AUTH_STATE)).thenAnswer {
            "auth_state"
        }
        val mockGCKey = mock(GCKey::class.java)
        coEvery { mockGCKey.getSymmetricKey() } returns mock(GCSymmetricKey::class.java)
        coEvery { cryptoService.fetchTagEncryptionKey() } returns mockGCKey
        coEvery { cryptoService.fetchCurrentCommonKey() } returns mockGCKey
        // when
        val testSubscriber = userService.isLoggedIn(USER_ALIAS)
            .test()

        // then
        testSubscriber
            .assertNoErrors()
            .assertValue(true)
    }

    @Test
    fun `isLoggedIn_shouldReturnFalse`() {
        //given
        Mockito.`when`(storage.getSecret(AUTH_STATE)).thenAnswer {
            "auth_state"
        }
        val mockGCKey = mock(GCKey::class.java)
        coEvery { mockGCKey.getSymmetricKey() } returns any()
        coEvery { cryptoService.fetchTagEncryptionKey() } returns mockGCKey
        coEvery { cryptoService.fetchCurrentCommonKey() } returns mockGCKey
        // when
        val testSubscriber = userService.isLoggedIn(USER_ALIAS)
            .test()

        // then
        testSubscriber
            .assertNoErrors()
            .assertValue(false)
    }

    @Test
    fun `getSessionToken should Return True`() {
        //given
        coEvery { authService.refreshAccessToken(USER_ALIAS) } returns AUTH_TOKEN
        // when
        val testSubscriber = userService.getSessionToken(USER_ALIAS)
            .test()

        // then
        testSubscriber
            .assertNoErrors()
            .assertValue(AUTH_TOKEN)
    }

    @Test
    fun `getSessionToken should Throws Error`() {
        //given
        coEvery { authService.refreshAccessToken(USER_ALIAS) } returns any()
        // when
        val testSubscriber = userService.getSessionToken(USER_ALIAS)
            .test()

        // then
        testSubscriber
            .assertError(NullPointerException::class.java)
            .assertNotComplete()
    }

    @Test
    fun `Given VersionList is fetched, when the call fails return true`() {
        //given
        val error = RuntimeException("error")
        val response: Single<VersionList> = Single.fromCallable { throw error }
        mockkObject(Log)
        coEvery { apiService.fetchVersionInfo("alias") } returns response
        //when
        val testSubscriber = userService.getVersionInfo("1.9.0")
            ?.test()
            ?.await()
        //then
        testSubscriber?.assertValue(true)
        verify(exactly = 1) { Log.error(error, "Version not supported") }
    }

    @Test
    fun `Given VersionList is fetched, when version supported return true`() {
        val versions: VersionList = mockk()
        val response: Single<VersionList> = Single.just(versions)
        val currentVersion = "1.9.0"
        coEvery { apiService.fetchVersionInfo(currentVersion) } returns response
        coEvery { versions.isSupported(currentVersion) } returns true
        //when
        val testSubscriber = userService.getVersionInfo("1.9.0")
            ?.test()
        //then
        testSubscriber?.assertValue(true)
            ?.assertNoErrors()

    }

    @Test
    fun `logout should Return Success`() {
        //given
        coEvery { apiService.logout(USER_ALIAS) } returns Completable.complete()
        // when
        val testSubscriber = userService.logout()
            .test()
            .await()

        // then
        testSubscriber
            .assertNoErrors()
            .assertComplete()
        Mockito.verify(storage).clear()
    }

    @Test
    fun `logout should Return Exception`() {
        //given
        coEvery { apiService.logout(USER_ALIAS) } returns Completable.error(Exception())
        // when
        val testSubscriber = userService.logout()
            .test()
            .await()

        // then
        testSubscriber
            .assertError(Exception::class.java)
            .assertNotComplete()
        Mockito.verifyZeroInteractions(storage)
    }

    @Test
    fun `finish Login should Return Boolean Success`() {
        //given
        val userInfo = mock(UserInfo::class.java)
        coEvery { userInfo.commonKey } returns mock(EncryptedKey::class.java)
        coEvery { userInfo.commonKeyId } returns "mockedCommonKeyId"
        coEvery { userInfo.tagEncryptionKey } returns mock(EncryptedKey::class.java)
        coEvery { userInfo.uid } returns ""
        coEvery { apiService.fetchUserInfo(USER_ALIAS) } returns Single.just(userInfo)
        coEvery { cryptoService.fetchGCKeyPair() } returns Single.just(mock(GCKeyPair::class.java))
        coEvery {
            cryptoService.asymDecryptSymetricKey(
                ArgumentMatchers.any(),
                ArgumentMatchers.any()
            )
        } returns Single.just(mock(GCKey::class.java))
        coEvery {
            cryptoService.symDecryptSymmetricKey(
                ArgumentMatchers.any(),
                ArgumentMatchers.any()
            )
        } returns Single.just(mock(GCKey::class.java))
        // when
        val testSubscriber = userService.finishLogin(true)
            .test()


        // then
        testSubscriber
            .assertValue { aBoolean: Boolean? -> aBoolean!! }
            .assertComplete()
    }

    @Test
    fun `getUID should Return String`() {
        //given
        val uid = "mock-uid"
        Mockito.`when`(storage.getSecret(USER_ALIAS + "_" + KEY_USER_ID, String::class.java)).thenAnswer {
           uid
        }
        // when
        val testObserver = userService.uID.test()

        // then
        testObserver
            .assertNoErrors()
            .assertValue(uid)
    }
}
