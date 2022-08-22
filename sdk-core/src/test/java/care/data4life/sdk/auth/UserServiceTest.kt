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

import care.data4life.sdk.crypto.CryptoContract
import care.data4life.sdk.crypto.GCKey
import care.data4life.sdk.crypto.GCKeyPair
import care.data4life.sdk.log.Log
import care.data4life.sdk.network.NetworkingContract
import care.data4life.sdk.network.model.EncryptedKey
import care.data4life.sdk.network.model.UserInfo
import care.data4life.sdk.test.util.GenericTestDataProvider.ALIAS
import care.data4life.sdk.test.util.GenericTestDataProvider.COMMON_KEY_ID
import care.data4life.sdk.test.util.GenericTestDataProvider.USER_ID
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import io.mockk.verify
import io.reactivex.Completable
import io.reactivex.Single
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue
import org.junit.Before
import org.junit.Test

class UserServiceTest {
    private lateinit var service: AuthContract.UserService
    private val authService: AuthorizationContract.Service = mockk()
    private val apiService: NetworkingContract.Service = mockk()
    private val secureStore: CryptoContract.SecureStore = mockk()
    private val cryptoService: CryptoContract.Service = mockk()

    @Before
    fun setUp() {
        service = UserService(
            ALIAS,
            authService,
            apiService,
            secureStore,
            cryptoService
        )
    }

    @Test
    fun `it fulfils UserService`() {
        val service: Any = UserService(ALIAS, mockk(), mockk(), mockk(), mockk())

        assertTrue(service is AuthContract.UserService)
    }

    @Test
    fun `it resolves the current userId if userId is referenced`() {
        // Given
        val userId = USER_ID

        every {
            secureStore.getSecret(
                "${ALIAS}_user_id",
                String::class.java
            )
        } returns USER_ID

        // When
        val currentId = service.userID.blockingGet()

        // Then
        assertEquals(
            actual = currentId,
            expected = userId
        )
    }

    @Test
    fun `Given isLoggedIn is called with an Alias it returns if the user is logged in`() {
        // Given
        val isLoggedIn = true
        val alias = ALIAS

        every { authService.isAuthorized(alias) } returns isLoggedIn

        // When
        val loggedInState = service.isLoggedIn(alias).blockingGet()

        // Then
        assertEquals(
            actual = loggedInState,
            expected = isLoggedIn
        )
    }

    @Test
    fun `Given isLoggedIn is called with an Alias, it returns false if the authService call fails`() {
        // Given
        val alias = ALIAS

        every { authService.isAuthorized(alias) } throws RuntimeException("Does not matter")

        // When
        val loggedInState = service.isLoggedIn(alias).blockingGet()

        // Then
        assertFalse(loggedInState)
    }

    @Test
    fun `Given logout is called, it returns a Completable while clearing the SecureStore`() {
        // Given
        every { apiService.logout(ALIAS) } returns Completable.fromAction { /* noop */ }
        every { secureStore.clear() } just Runs

        // When
        val loggedInState = service.logout().blockingGet()

        // Then
        assertNull(loggedInState)
        verify(exactly = 1) { secureStore.clear() }
    }

    @Test
    fun `Given logout is called, it returns and logs errors while not clearing the SecureStore if the logout fails`() {
        // Given
        mockkObject(Log)
        val error = RuntimeException("Does not matter")

        every { apiService.logout(ALIAS) } returns Completable.fromAction { throw error }
        every { secureStore.clear() } just Runs
        every { Log.error(error, "Failed to logout") } just Runs

        // When
        val loggedInState = service.logout().blockingGet()

        // Then
        assertSame(
            actual = loggedInState,
            expected = error
        )
        verify(exactly = 0) { secureStore.clear() }
        verify(exactly = 1) { Log.error(error, "Failed to logout") }

        unmockkObject(Log)
    }

    @Test
    fun `Given refreshSessionToken with an Alias, it delegates to call to the AuthService and returns its result`() {
        // Given
        val alias = ALIAS
        val expectedToken = "token"

        every { authService.refreshAccessToken(alias) } returns expectedToken

        // When
        val token = service.refreshSessionToken(alias).blockingGet()

        // Then
        assertEquals(
            actual = token,
            expected = expectedToken
        )
    }

    @Test
    fun `Given finishLogin is called with a Boolean, it returns true`() {
        // Given
        val userInfo: UserInfo = mockk()
        val encryptedCommonKey: EncryptedKey = mockk()
        val encryptedTagEncryptionKey: EncryptedKey = mockk()
        val commonKeyId = COMMON_KEY_ID
        val userId = USER_ID

        val keyPair: GCKeyPair = mockk()
        val commonKey: GCKey = mockk()
        val tagEncryptionKey: GCKey = mockk()

        every { userInfo.userId } returns userId
        every { userInfo.commonKeyId } returns commonKeyId
        every { userInfo.encryptedCommonKey } returns encryptedCommonKey
        every { userInfo.encryptedTagEncryptionKey } returns encryptedTagEncryptionKey

        every { apiService.fetchUserInfo(ALIAS) } returns Single.just(userInfo)
        every { cryptoService.fetchGCKeyPair() } returns Single.just(keyPair)

        every { secureStore.storeSecret("${ALIAS}_user_id", userId) } just Runs

        every {
            cryptoService.asymDecryptSymetricKey(
                keyPair,
                encryptedCommonKey
            )
        } returns Single.just(commonKey)
        every { cryptoService.storeCommonKey(commonKeyId, commonKey) } just Runs
        every { cryptoService.storeCurrentCommonKeyId(commonKeyId) } just Runs

        every {
            cryptoService.symDecryptSymmetricKey(
                commonKey,
                encryptedTagEncryptionKey
            )
        } returns Single.just(tagEncryptionKey)
        every { cryptoService.storeTagEncryptionKey(tagEncryptionKey) } just Runs

        // When
        val result = service.finishLogin(true).blockingGet()

        // Then
        assertTrue(result)

        verify(exactly = 1) { apiService.fetchUserInfo(ALIAS) }
        verify(exactly = 1) { cryptoService.fetchGCKeyPair() }

        verify(exactly = 1) { secureStore.storeSecret("${ALIAS}_user_id", userId) }

        verify(exactly = 1) {
            cryptoService.asymDecryptSymetricKey(
                keyPair,
                encryptedCommonKey
            )
        }
        verify(exactly = 1) {
            cryptoService.storeCommonKey(commonKeyId, commonKey)
        }
        verify(exactly = 1) {
            cryptoService.storeCurrentCommonKeyId(commonKeyId)
        }

        verify(exactly = 1) {
            cryptoService.symDecryptSymmetricKey(
                commonKey,
                encryptedTagEncryptionKey
            )
        }
        verify(exactly = 1) { cryptoService.storeTagEncryptionKey(tagEncryptionKey) }
    }

    @Test
    fun `Given finishLogin is called with a Boolean and any operation fails, it returns true while logging the Error`() {
        // Given
        mockkObject(Log)

        val error = RuntimeException("not important")

        val userInfo: UserInfo = mockk()
        val encryptedCommonKey: EncryptedKey = mockk()
        val encryptedTagEncryptionKey: EncryptedKey = mockk()
        val commonKeyId = COMMON_KEY_ID
        val userId = USER_ID

        val keyPair: GCKeyPair = mockk()
        val commonKey: GCKey = mockk()
        val tagEncryptionKey: GCKey = mockk()

        every { userInfo.userId } returns userId
        every { userInfo.commonKeyId } returns commonKeyId
        every { userInfo.encryptedCommonKey } returns encryptedCommonKey
        every { userInfo.encryptedTagEncryptionKey } returns encryptedTagEncryptionKey

        every { apiService.fetchUserInfo(ALIAS) } returns Single.just(userInfo)
        every { cryptoService.fetchGCKeyPair() } returns Single.just(keyPair)

        every { secureStore.storeSecret("${ALIAS}_user_id", userId) } just Runs

        every {
            cryptoService.asymDecryptSymetricKey(
                keyPair,
                encryptedCommonKey
            )
        } returns Single.error(error)
        every { cryptoService.storeCommonKey(commonKeyId, commonKey) } just Runs
        every { cryptoService.storeCurrentCommonKeyId(commonKeyId) } just Runs

        every {
            cryptoService.symDecryptSymmetricKey(
                commonKey,
                encryptedTagEncryptionKey
            )
        } returns Single.just(tagEncryptionKey)
        every { cryptoService.storeTagEncryptionKey(tagEncryptionKey) } just Runs

        every { Log.error(error, "Failed to finish login") } just Runs

        // Then
        val result = assertFailsWith<RuntimeException> {
            // When
            service.finishLogin(true).blockingGet()
        }

        assertSame(
            actual = result,
            expected = error
        )

        verify(exactly = 1) { apiService.fetchUserInfo(ALIAS) }
        verify(exactly = 1) { cryptoService.fetchGCKeyPair() }

        verify(exactly = 1) { secureStore.storeSecret("${ALIAS}_user_id", userId) }

        verify(exactly = 1) {
            cryptoService.asymDecryptSymetricKey(
                keyPair,
                encryptedCommonKey
            )
        }
        verify(exactly = 0) {
            cryptoService.storeCommonKey(commonKeyId, commonKey)
        }
        verify(exactly = 0) {
            cryptoService.storeCurrentCommonKeyId(commonKeyId)
        }

        verify(exactly = 0) {
            cryptoService.symDecryptSymmetricKey(
                commonKey,
                encryptedTagEncryptionKey
            )
        }
        verify(exactly = 0) { cryptoService.storeTagEncryptionKey(tagEncryptionKey) }
        verify(exactly = 1) { Log.error(error, "Failed to finish login") }

        mockkObject(Log)
    }
}
