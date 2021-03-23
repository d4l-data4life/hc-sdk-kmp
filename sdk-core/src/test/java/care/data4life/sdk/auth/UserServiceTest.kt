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
import io.mockk.Called
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.spyk
import io.mockk.verify
import io.reactivex.Completable
import io.reactivex.Single
import org.junit.Before
import org.junit.Ignore
import org.junit.Test


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
        authService = mockk()
        userService = mockk()
        storage = mockk()
        apiService = mockk()
        cryptoService = mockk()
        userService = spyk(UserService(USER_ALIAS, authService, apiService, storage, cryptoService))
    }

    @Test
    fun `isLoggedIn_shouldReturnTrue`() {
        //given
        every { authService.isAuthorized(USER_ALIAS) } returns true
        every { storage.getSecret(AUTH_STATE) } answers {
            "auth_state".toCharArray()
        }
        val mockGCKey = mockk<GCKey>()
        every { mockGCKey.getSymmetricKey() } returns mockk<GCSymmetricKey>()
        every { cryptoService.fetchTagEncryptionKey() } returns mockGCKey
        every { cryptoService.fetchCurrentCommonKey() } returns mockGCKey
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
        every { storage.getSecret(AUTH_STATE) } answers {
            "auth_state".toCharArray()
        }
        val mockGCKey = mockk<GCKey>()
        every { mockGCKey.getSymmetricKey() } returns mockk<GCSymmetricKey>()
        every { cryptoService.fetchTagEncryptionKey() } returns mockGCKey
        every { cryptoService.fetchCurrentCommonKey() } returns mockGCKey
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
        every { authService.refreshAccessToken(USER_ALIAS) } returns AUTH_TOKEN
        // when
        val testSubscriber = userService.getSessionToken(USER_ALIAS)
                .test()

        // then
        testSubscriber
                .assertNoErrors()
                .assertValue(AUTH_TOKEN)
    }

    @Ignore
    @Test
    fun `getSessionToken should Throws Error`() {
        //given
        every { authService.refreshAccessToken(USER_ALIAS) } returns mockk<String>()
        // when
        val testSubscriber = userService.getSessionToken(USER_ALIAS)
                .test()
                .await()

        // then
        testSubscriber
                .assertNoValues()
    }

    @Test
    fun `Given VersionList is fetched, when the call fails return true`() {
        //given
        val error = RuntimeException("error")
        val response: Single<VersionList> = Single.fromCallable { throw error }
        mockkObject(Log)
        every { apiService.fetchVersionInfo() } returns response
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
        every { apiService.fetchVersionInfo() } returns response
        every { versions.isSupported(currentVersion) } returns true
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
        every { apiService.logout(USER_ALIAS) } returns Completable.complete()
        every { storage.clear() } just Runs
        // when
        val testSubscriber = userService.logout()
                .test()
                .await()

        // then
        testSubscriber
                .assertNoErrors()
                .assertComplete()
        verify(exactly = 1) { storage.clear() }
    }

    @Test
    fun `logout should Return Exception`() {
        //given
        every { apiService.logout(USER_ALIAS) } returns Completable.error(Exception())
        // when
        val testSubscriber = userService.logout()
                .test()
                .await()

        // then
        testSubscriber
                .assertError(Exception::class.java)
                .assertNotComplete()
        verify { storage wasNot Called }
    }

    @Test
    fun `finish Login should Return Boolean Success`() {
        //given
        val userInfo = mockk<UserInfo>()
        every { userInfo.commonKey } returns mockk<EncryptedKey>()
        every { userInfo.commonKeyId } returns "mockedCommonKeyId"
        every { userInfo.tagEncryptionKey } returns mockk<EncryptedKey>()
        every { userInfo.uid } returns "mockedUid"
        every { apiService.fetchUserInfo(USER_ALIAS) } returns Single.just(userInfo)
        every { storage.storeSecret("userAlias_user_id", "mockedUid") } just Runs
        every { cryptoService.storeCommonKey("mockedCommonKeyId", any()) } just Runs
        every { cryptoService.storeCurrentCommonKeyId("mockedCommonKeyId") } just Runs
        every { cryptoService.storeTagEncryptionKey(any()) } just Runs
        every { cryptoService.fetchGCKeyPair() } returns Single.just(mockk<GCKeyPair>())
        every {
            cryptoService.asymDecryptSymetricKey(any(), any())
        } returns Single.just(mockk<GCKey>())
        every {
            cryptoService.symDecryptSymmetricKey(any(), any())
        } returns Single.just(mockk<GCKey>())
        // when
        val testSubscriber = userService.finishLogin(true)
                .test()


        // then
        testSubscriber
                .assertValue { it }
                .assertComplete()
    }

    @Test
    fun `getUID should Return String`() {
        //given
        val uid = "mock-uid"
        every { storage.getSecret(USER_ALIAS + "_" + KEY_USER_ID, String::class.java) } answers {
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
