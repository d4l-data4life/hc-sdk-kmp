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
package care.data4life.sdk.auth

import care.data4life.auth.AuthorizationService
import care.data4life.crypto.GCKey
import care.data4life.crypto.GCKeyPair
import care.data4life.crypto.GCSymmetricKey
import care.data4life.sdk.ApiService
import care.data4life.sdk.CryptoSecureStore
import care.data4life.sdk.CryptoService
import care.data4life.sdk.lang.D4LException
import care.data4life.sdk.network.model.EncryptedKey
import care.data4life.sdk.network.model.UserInfo
import care.data4life.sdk.network.model.Version
import care.data4life.sdk.network.model.VersionList
import care.data4life.sdk.test.util.TestSchedulerRule
import io.reactivex.Completable
import io.reactivex.Single
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.ArgumentMatchers
import org.mockito.Mockito
import org.mockito.invocation.InvocationOnMock

class _UserServiceTest {
    @Rule
    var schedulerRule = TestSchedulerRule()
    private lateinit var apiService: ApiService
    private lateinit var storage: CryptoSecureStore
    private var userService: UserService? = null
    private lateinit var cryptoService: CryptoService
    private lateinit var authService: AuthorizationService



//    @Test
//    fun 'isLoggedIn_shouldReturnTrue'(){
//
//    // then
//            Unit
//        get() {
//            // given
//            Mockito.`when`(authService!!.isAuthorized(USER_ALIAS)).thenReturn(true)
//            Mockito.doAnswer { invocation: InvocationOnMock? -> "auth_state" }.`when`(storage)!!.getSecret(AUTH_STATE)
//            val mockGCKey = Mockito.mock(GCKey::class.java)
//            Mockito.`when`(mockGCKey.getSymmetricKey()).thenReturn(Mockito.mock(GCSymmetricKey::class.java))
//            Mockito.`when`(cryptoService!!.fetchTagEncryptionKey()).thenReturn(mockGCKey)
//            Mockito.`when`(cryptoService!!.fetchCurrentCommonKey()).thenReturn(mockGCKey)
//
//            // when
//            val testSubscriber = userService!!.isLoggedIn(USER_ALIAS)
//                    .test()
//
//            // then
//            testSubscriber
//                    .assertNoErrors()
//                    .assertValue(true)
//        }
//    // given
//
//    // when
//    @get:Throws(Exception::class)
//    @get:Test
//    val isLoggedIn_shouldReturnFalse:
//
//    // then
//            Unit
//        get() {
//            // given
//            Mockito.doAnswer { invocation: InvocationOnMock? -> "auth_state" }.`when`(storage)!!.getSecret(AUTH_STATE)
//            val mockGCKey = Mockito.mock(GCKey::class.java)
//            Mockito.`when`(mockGCKey.getSymmetricKey()).thenReturn(null)
//            Mockito.`when`(cryptoService!!.fetchTagEncryptionKey()).thenReturn(mockGCKey)
//            Mockito.`when`(cryptoService!!.fetchCurrentCommonKey()).thenReturn(mockGCKey)
//
//            // when
//            val testSubscriber = userService!!.isLoggedIn(USER_ALIAS)
//                    .test()
//
//            // then
//            testSubscriber
//                    .assertNoErrors()
//                    .assertValue(false)
//        }
//    // given
//
//    // when
//    @get:Throws(Exception::class)
//    @get:Test
//    val sessionToken_shouldReturnTrue:
//
//    // then
//            Unit
//        get() {
//            // given
//            Mockito.`when`(authService!!.refreshAccessToken(USER_ALIAS)).thenReturn(AUTH_TOKEN)
//
//            // when
//            val testSubscriber = userService!!.getSessionToken(USER_ALIAS)
//                    .test()
//
//            // then
//            testSubscriber
//                    .assertNoErrors()
//                    .assertValue(AUTH_TOKEN)
//        }
//    // given
//
//    // when
//    @get:Throws(Exception::class)
//    @get:Test
//    val sessionToken_shouldThrowError: Unit
//        get() {
//            // given
//            Mockito.`when`(authService!!.refreshAccessToken(USER_ALIAS)).thenReturn(null)
//
//            // when
//            val testSubscriber = userService!!.getSessionToken(USER_ALIAS)
//                    .test()
//                    .await()
//
//            // then
//            testSubscriber
//                    .assertError(NullPointerException::class.java)
//                    .assertNotComplete()
//        }
//
//    // given
//    @get:Throws(Exception::class)
//    @get:Test
//    val versionInfo_shouldReturnSuccess: Unit
//        get() {
//            // given
//            val VERSION_NAME = "1.9.0"
//            val versionInfo = VersionList(listOf(Version(VERSION_NAME)))
//            Mockito.`when`(apiService.fetchVersionInfo(VERSION_NAME)).thenReturn(Single.just(versionInfo))
//
//
//            // when
//            val testSubscriber = userService?.getVersionInfo(VERSION_NAME)
//                    ?.test()
//                    ?.await()
//
//            // then
//            testSubscriber
//                    ?.assertNoErrors()
//                    ?.assertComplete()
//        }
//
//    //InterruptedException
//
//    @get:Throws(Exception::class)
//    @get:Test
//    val logout_shouldReturnSuccess(): Unit
//    get() {
//        // given
//        Mockito.`when`(apiService!!.logout(USER_ALIAS)).thenReturn(Completable.complete())
//
//        // when
//        val testSubscriber = userService!!.logout()
//                .test()
//                .await()
//
//        // then
//        testSubscriber
//                .assertNoErrors()
//                .assertComplete()
//        Mockito.verify(storage)!!.clear()
//    }
//
//    //InterruptedException
//    @Test
//    @kotlin.Throws(Exception::class)
//    fun logout_shouldReturnException() {
//        // given
//        Mockito.`when`(apiService!!.logout(USER_ALIAS)).thenReturn(Completable.error(Exception()))
//
//        // when
//        val testSubscriber = userService!!.logout()
//                .test()
//                .await()
//
//        // then
//        testSubscriber
//                .assertError(Exception::class.java)
//                .assertNotComplete()
//        Mockito.verifyZeroInteractions(storage)
//    }
//
//    @Test
//    fun finishLogin_shouldReturnBooleanSuccess() {
//        // given
//        val userInfo = Mockito.mock(UserInfo::class.java)
//        Mockito.`when`(userInfo.commonKey).thenReturn(Mockito.mock(EncryptedKey::class.java))
//        Mockito.`when`(userInfo.commonKeyId).thenReturn("mockedCommonKeyId")
//        Mockito.`when`(userInfo.tagEncryptionKey).thenReturn(Mockito.mock(EncryptedKey::class.java))
//        Mockito.`when`(userInfo.uid).thenReturn("")
//        Mockito.`when`(apiService!!.fetchUserInfo(USER_ALIAS)).thenReturn(Single.just(userInfo))
//        Mockito.`when`(cryptoService!!.fetchGCKeyPair()).thenReturn(Single.just(Mockito.mock(GCKeyPair::class.java)))
//        Mockito.`when`(cryptoService!!.asymDecryptSymetricKey(ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Single.just(Mockito.mock(GCKey::class.java)))
//        Mockito.`when`(cryptoService!!.symDecryptSymmetricKey(ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Single.just(Mockito.mock(GCKey::class.java)))
//
//        // when
//        val testObserver = userService!!.finishLogin(true).test()
//
//        // then
//        testObserver
//                .assertValue { aBoolean: Boolean? -> aBoolean!! }
//                .assertComplete()
//    }
//
//    // given
//    @get:Throws(D4LException::class)
//    @get:Test
//    val uID_shouldReturnString: Unit
//
//    // when
//
//        // then
//        get() {
//            // given
//            val uid = "mock-uid"
//            Mockito.`when`(storage!!.getSecret(USER_ALIAS + "_" + KEY_USER_ID, String::class.java)).thenReturn(uid)
//
//            // when
//            val testObserver = userService!!.uID.test()
//
//            // then
//            testObserver
//                    .assertNoErrors()
//                    .assertValue(uid)
//        }
//
//    companion object {
//        private const val AUTH_STATE = "authState"
//        private const val USER_ALIAS = "userAlias"
//        private const val KEY_USER_ID = "user_id"
//        private const val AUTH_TOKEN = "authToken"
//    }
}
