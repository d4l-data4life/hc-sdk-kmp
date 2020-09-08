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

package care.data4life.auth

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import net.openid.appauth.AuthorizationServiceConfiguration
import net.openid.appauth.TokenResponse
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
class AuthorizationServiceTest {

    private lateinit var mockContext: Context
    private lateinit var authService: AuthorizationService
    private lateinit var serviceConfig: AuthorizationServiceConfiguration
    private lateinit var storage: AuthorizationContract.Storage
    private lateinit var appAuthService: AppAuthService


    // SUT
    private lateinit var sut: AuthorizationService


    @Before
    fun setUp() {
        mockContext = ApplicationProvider.getApplicationContext()

        appAuthService = mockk()
        authService = mockk()
        storage = mockk()
        serviceConfig = AuthorizationServiceConfiguration(Uri.parse("https://test.com"), Uri.parse("https://test.com"))

        val configuration = AuthorizationConfiguration(
                "clientId",
                "clientSecret",
                BASE_URL,
                BASE_URL,
                "redirectUrl",
                Authorization.defaultScopes
        )

        sut = AuthorizationService(
                appAuthService,
                configuration,
                storage
        )
    }

    @Test
    fun finishLogin_shouldFinishLoginProcess() {
        // Given
        val response = TokenResponse.jsonDeserialize(INTENT_PAYLOAD)
        every { storage.writeAuthState(any(), any()) } returns Unit

        every { appAuthService.performTokenRequest(any(), any(), any()) } answers {
            thirdArg<net.openid.appauth.AuthorizationService.TokenResponseCallback>()
                    .onTokenRequestCompleted(response, null)
        }

        val dataIntent = Intent()
        dataIntent.putExtra(APP_AUTH_INTENT_KEY, INTENT_PAYLOAD)

        // When
        sut.finishLogin(dataIntent, object : AuthorizationService.Callback {
            override fun onSuccess() {
                assertTrue { true }
            }

            override fun onError(error: Throwable) {
                assertTrue(false, "Failed to finish login")
            }
        })
        verify { storage.writeAuthState(eq(AUTH_PACKAGE), eq(AUTH_STORE_STRING)) }
    }

    @Test
    fun finishLogin_shouldThrowException() {

        val dataIntent = Intent()
        sut.finishLogin(dataIntent, object : AuthorizationService.Callback {
            override fun onSuccess() {
                assertFalse { true }
            }

            override fun onError(error: Throwable) {
                assertFalse { false }
            }
        })
    }

    @Test
    fun getAccessToken_shouldReturnString() {
        every { storage.readAuthState(AUTH_PACKAGE) } returns AUTH_STORE_STRING

        val actual = sut.getAccessToken(USER_ALIAS)

        assertEquals(EXPECTED_ACCESS_TOKEN, actual, "Failed to restore AuthState")
    }

    @Test(expected = AuthorizationException.FailedToRestoreAccessToken::class)
    fun getAccessToken_shouldThrowException() {
        every { storage.readAuthState(AUTH_PACKAGE) } returns AUTH_STORE_MISSING_TOKEN

        sut.getAccessToken(USER_ALIAS)
    }

    @Test(expected = AuthorizationException.FailedToRestoreAuthState::class)
    fun getAccessToken_shouldThrowException_whenParsingBrokenJson() {
        every { storage.readAuthState(AUTH_PACKAGE) } returns AUTH_STORE_BROKEN_JSON

        sut.getAccessToken(USER_ALIAS)
    }

    @Test
    fun refreshAccessToken_shouldReturnString() {
        every { storage.readAuthState(KEY_AUTH_STATE) } returns AUTH_STORE_STRING
        val response = TokenResponse.jsonDeserialize(INTENT_PAYLOAD)
        every { storage.writeAuthState(any(), any()) } returns Unit

        every { appAuthService.performSynchronousTokenRequest(any(), any()) } answers {
            net.openid.appauth.AuthorizationService.SynchronousTokenRequestData(response, null)
        }

        val actual = sut.refreshAccessToken(USER_ALIAS)

        assertEquals(EXPECTED_ACCESS_TOKEN, actual)
    }

    @Test
    fun getRefreshToken() {
        every { storage.readAuthState(KEY_AUTH_STATE) } returns AUTH_STORE_STRING

        val actual = sut.getRefreshToken(USER_ALIAS)

        assertEquals(EXPECTED_REFRESH_TOKEN, actual)
    }

    @Test(expected = AuthorizationException.FailedToRestoreRefreshToken::class)
    fun getRefreshToken_shouldThrowExceptionWithBrokenJson() {
        every { storage.readAuthState(KEY_AUTH_STATE) } returns AUTH_STORE_MISSING_TOKEN

        sut.getRefreshToken(USER_ALIAS)
    }

    @Test
    fun clearAuthData_shouldClearAllUserData() {
        every { storage.clear() } returns Unit

        sut.clear()

        verify { storage.clear() }
    }

    @Test
    fun isAuthorized() {
        every { storage.readAuthState(KEY_AUTH_STATE) } returns AUTH_STORE_STRING

        val actual = sut.isAuthorized(USER_ALIAS)

        assertTrue { actual }
    }

    @Test
    fun loginIntent() {
        val mockedIntent = mockk<Intent>()
        every { appAuthService.getAuthorizationRequestIntent(any()) } returns mockedIntent
        val listener = mockk<AuthorizationService.AuthorizationListener>()

        val actual = sut.loginIntent(mockContext, emptySet(), IGNORE, listener)

        assertEquals(Intent::class.java, actual.javaClass)
        assertEquals(LoginActivity.authorizationListener, listener)
    }


    companion object {
        private const val USER_ALIAS = "alias"
        private const val KEY_AUTH_STATE = "care.data4life.auth.auth_state"
        private const val BASE_URL = "https://test.com"
        private const val IGNORE = "ignore"

        private const val INTENT_PAYLOAD = """{"request":{"configuration":{"authorizationEndpoint":"http:\/\/192.168.10.142:8080\/authorize","tokenEndpoint":"http:\/\/192.168.10.142:8080\/token"},"clientId":"android","responseType":"code","redirectUri":"data4life:\/\/","state":"pvh0eYcyno3PycYD40V7Hw","codeVerifier":"jQ0-3_485kOHjCdX_PC8rChb99AvmYxcC4YuMc9YaVSIj3x76u8jZkmsOdcT8qR58I0a87k8seOhhBHflF6y8g","codeVerifierChallenge":"QpwewXdsKSNJipXyhDEtbJj3lAytSaxxUAqcSxGBezQ","codeVerifierChallengeMethod":"S256","additionalParameters":{},"grantType":"refresh_token","refreshToken":"mockRefreshToken"},"state":"pvh0eYcyno3PycYD40V7Hw","code":"dehvgDJnTlOrdfVj3ohvMQ","additional_parameters":{},"access_token":"mockAccessToken","refresh_token":"mockRefreshToken"}"""

        private const val EXPECTED_ACCESS_TOKEN = "mockAccessToken"
        private const val EXPECTED_REFRESH_TOKEN = "mockRefreshToken"
        private const val APP_AUTH_INTENT_KEY = "net.openid.appauth.AuthorizationResponse"
        private const val AUTH_PACKAGE = "care.data4life.auth.auth_state"
        private const val AUTH_STORE_STRING = """{"lastAuthorizationResponse":{"access_token":"mockAccessToken","request":{"redirectUri":"data4life:\/\/","codeVerifierChallengeMethod":"S256","responseType":"code","clientId":"android","configuration":{"tokenEndpoint":"http:\/\/192.168.10.142:8080\/token","authorizationEndpoint":"http:\/\/192.168.10.142:8080\/authorize"},"codeVerifier":"jQ0-3_485kOHjCdX_PC8rChb99AvmYxcC4YuMc9YaVSIj3x76u8jZkmsOdcT8qR58I0a87k8seOhhBHflF6y8g","codeVerifierChallenge":"QpwewXdsKSNJipXyhDEtbJj3lAytSaxxUAqcSxGBezQ","additionalParameters":{},"state":"pvh0eYcyno3PycYD40V7Hw"},"code":"dehvgDJnTlOrdfVj3ohvMQ","additional_parameters":{},"state":"pvh0eYcyno3PycYD40V7Hw"},"mLastTokenResponse":{"access_token":"mockAccessToken","request":{"redirectUri":"data4life:\/\/","clientId":"android","configuration":{"tokenEndpoint":"http:\/\/192.168.10.142:8080\/token","authorizationEndpoint":"http:\/\/192.168.10.142:8080\/authorize"},"additionalParameters":{},"grantType":"refresh_token","refreshToken":"mockRefreshToken"},"refresh_token":"mockRefreshToken","additionalParameters":{}},"refreshToken":"mockRefreshToken"}"""
        private const val AUTH_STORE_MISSING_TOKEN = """{"lastAuthorizationResponse":{"request":{"redirectUri":"data4life:\/\/","codeVerifierChallengeMethod":"S256","responseType":"code","clientId":"android","configuration":{"tokenEndpoint":"http:\/\/192.168.10.142:8080\/token","authorizationEndpoint":"http:\/\/192.168.10.142:8080\/authorize"},"codeVerifier":"jQ0-3_485kOHjCdX_PC8rChb99AvmYxcC4YuMc9YaVSIj3x76u8jZkmsOdcT8qR58I0a87k8seOhhBHflF6y8g","codeVerifierChallenge":"QpwewXdsKSNJipXyhDEtbJj3lAytSaxxUAqcSxGBezQ","additionalParameters":{},"state":"pvh0eYcyno3PycYD40V7Hw"},"code":"dehvgDJnTlOrdfVj3ohvMQ","additional_parameters":{},"state":"pvh0eYcyno3PycYD40V7Hw"},"mLastTokenResponse":{"request":{"redirectUri":"data4life:\/\/","clientId":"android","configuration":{"tokenEndpoint":"http:\/\/192.168.10.142:8080\/token","authorizationEndpoint":"http:\/\/192.168.10.142:8080\/authorize"},"additionalParameters":{},"grantType":"refresh_token","refreshToken":"mockRefreshToken"},"additionalParameters":{}}}"""
        private const val AUTH_STORE_BROKEN_JSON = """{"lastAuthorizationResponse";"invalid""""
    }
}
