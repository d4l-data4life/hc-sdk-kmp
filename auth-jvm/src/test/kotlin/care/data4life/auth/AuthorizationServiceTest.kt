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

import care.data4life.auth.test.util.ClientConfigLoader
import care.data4life.auth.test.util.Environment
import care.data4life.auth.test.util.Environment.DEVELOPMENT
import care.data4life.auth.test.util.TestConfigLoader
import care.data4life.sdk.util.Base64
import com.github.scribejava.core.model.OAuth2AccessToken
import com.github.scribejava.core.model.OAuth2Authorization
import com.github.scribejava.core.oauth.OAuth20Service
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifyAll
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AuthorizationServiceTest {

    private lateinit var service: AuthorizationService
    private val store: AuthorizationContract.Storage = mockk()
    private val oAuthService: OAuth20Service = mockk()
    private val base64 = Base64

    @Before
    fun setup() {
        val clientConfig = ClientConfigLoader.load()

        val config = AuthorizationConfiguration(
                clientConfig[DEVELOPMENT].id,
                clientConfig[DEVELOPMENT].secret,
                "https://api-phdp-dev.hpsgc.de",
                "https://api-phdp-dev.hpsgc.de",
                clientConfig[DEVELOPMENT].redirectScheme,
                Authorization.defaultScopes
        )

        service = AuthorizationService(ALIAS, config, store, base64, oAuthService)
    }

    @Test
    fun createAuthorizationUrl() {
        val map = mutableMapOf<String, String>()
        map["public_key"] = PUBLIC_KEY
        map["user_id"] = ALIAS

        every { oAuthService.getAuthorizationUrl(map) } returns AUTHORIZATION_URL

        val authUrl = service.createAuthorizationUrl(ALIAS, PUBLIC_KEY)

        assertEquals(AUTHORIZATION_URL, authUrl)
    }

    @Test
    fun finishAuthorization() {
        val auth = OAuth2Authorization()
        auth.state = EXPECTED_STATE
        auth.code = EXPECTED_CODE
        every { oAuthService.state } returns EXPECTED_STATE
        every { store.readAuthState(AUTH_KEY) } returns AUTH_STATE
        every { oAuthService.extractAuthorization(CALLBACK_URL) } returns auth
        val accessToken = OAuth2AccessToken(ACCESS_TOKEN, IGNORE, 0, REFRESH_TOKEN, IGNORE, IGNORE)
        every { oAuthService.getAccessToken(EXPECTED_CODE) } returns accessToken
        every { store.writeAuthState(any(), any()) } returns Unit

        val isLoggedIn = service.finishAuthorization(ALIAS, CALLBACK_URL)

        verify { store.writeAuthState(TOKEN_KEY, TOKEN_STATE) }
        assertTrue { isLoggedIn }
    }

    @Test
    fun finishAuthorization_withDifferentStates() {
        val auth = OAuth2Authorization()
        auth.state = "differentAuthState"
        auth.code = EXPECTED_CODE
        every { oAuthService.state } returns EXPECTED_STATE
        every { store.readAuthState(AUTH_KEY) } returns AUTH_STATE
        every { oAuthService.extractAuthorization(CALLBACK_URL) } returns auth

        val isLoggedIn = service.finishAuthorization(ALIAS, CALLBACK_URL)

        assertFalse { isLoggedIn }
    }

    @Test(expected = AuthorizationException.FailedToRestoreAuthState::class)
    fun finishAuthorization_shouldThrowException() {
        val auth = OAuth2Authorization()
        auth.state = EXPECTED_STATE
        auth.code = EXPECTED_CODE
        every { oAuthService.state } returns EXPECTED_STATE
        every { store.readAuthState(AUTH_KEY) } returns AUTH_STATE_BROKEN
        every { oAuthService.extractAuthorization(CALLBACK_URL) } returns auth

        val isLoggedIn = service.finishAuthorization(ALIAS, CALLBACK_URL)

        assertFalse { isLoggedIn }
    }

    @Test
    fun getAccessToken() {
        every { store.readAuthState(any()) } returns TOKEN_STATE

        val actual = service.getAccessToken(ALIAS)

        verify { store.readAuthState(TOKEN_KEY) }
        assertEquals(actual, ACCESS_TOKEN)
    }

    @Test(expected = AuthorizationException.FailedToRestoreAccessToken::class)
    fun getAccessToken_whereRestoredAccessTokenIsNull_shouldThrowException() {
        every { store.readAuthState(any()) } returns EMPTY_JSON

        val actual = service.getAccessToken(ALIAS)

        verify { store.readAuthState(TOKEN_KEY) }
        assertEquals(actual, ACCESS_TOKEN)
    }

    @Test(expected = AuthorizationException.FailedToRestoreTokenState::class)
    fun getAccessToken_shouldThrowException() {
        every { store.readAuthState(any()) } returns TOKEN_STATE_BROKEN

        service.getAccessToken(ALIAS)
    }

    @Test
    fun getRefreshToken() {
        every { store.readAuthState(any()) } returns TOKEN_STATE

        val actual = service.getRefreshToken(ALIAS)

        verify { store.readAuthState(TOKEN_KEY) }
        assertEquals(actual, REFRESH_TOKEN)
    }

    @Test(expected = AuthorizationException.FailedToRestoreRefreshToken::class)
    fun getRefreshToken_whereRestoredRefreshTokenIsNull_shouldThrowException() {
        every { store.readAuthState(any()) } returns EMPTY_JSON

        val actual = service.getRefreshToken(ALIAS)

        verify { store.readAuthState(TOKEN_KEY) }
        assertEquals(actual, REFRESH_TOKEN)
    }

    @Test
    fun refreshAccessToken() {
        val accessToken = "accessToken"
        val refreshToken = "refreshToken"
        val stateJson = """{"accessToken":"$accessToken","refreshToken":"$refreshToken"}"""
        val token = mockk<OAuth2AccessToken>()
        every { token.accessToken } returns accessToken
        every { token.refreshToken } returns refreshToken
        every { store.readAuthState(TOKEN_KEY) } returns TOKEN_STATE
        every { store.writeAuthState(any(), any()) } returns Unit
        every { oAuthService.refreshAccessToken(any()) } returns token

        val actual = service.refreshAccessToken(ALIAS)

        verifyAll {
            store.readAuthState(TOKEN_KEY)
            oAuthService.refreshAccessToken(REFRESH_TOKEN)
            store.writeAuthState(TOKEN_KEY, stateJson)
        }

        assertEquals(actual, accessToken)
    }

    @Test
    fun refreshAccessToken_withoutAuthState() {
        every { store.readAuthState(TOKEN_KEY) } returns null

        try {
            service.refreshAccessToken(ALIAS)
        } catch (e: AuthorizationException.FailedToRestoreRefreshToken) {
            assertTrue { true }
        }
    }

    @Test
    fun refreshAccessToken_withoutNewAccessToken() {
        val accessToken = null
        val refreshToken = "refreshToken"
        val tokenStateJson = """{"refreshToken":"$refreshToken"}"""
        val token = mockk<OAuth2AccessToken>()
        every { token.accessToken } returns accessToken
        every { token.refreshToken } returns refreshToken
        every { store.readAuthState(TOKEN_KEY) } returns TOKEN_STATE
        every { store.writeAuthState(any(), any()) } returns Unit
        every { oAuthService.refreshAccessToken(any()) } returns token

        try {
            service.refreshAccessToken(ALIAS)
        } catch (e: AuthorizationException.FailedToRefreshAccessToken) {
            assertTrue { true }
        }

        verifyAll {
            store.readAuthState(TOKEN_KEY)
            oAuthService.refreshAccessToken(REFRESH_TOKEN)
            store.writeAuthState(TOKEN_KEY, tokenStateJson)
        }

    }

    @Test
    fun isAuthorized() {
        every { store.readAuthState(AUTH_KEY) } returns AUTH_STATE
        every { store.readAuthState(TOKEN_KEY) } returns TOKEN_STATE

        val actual = service.isAuthorized(ALIAS)

        assertTrue { actual }
        verify { store.readAuthState(AUTH_KEY) }
        verify { store.readAuthState(TOKEN_KEY) }

    }

    @Test
    fun clear() {
        every { store.clear() } returns Unit
        service.clear()

        verify { store.clear() }
    }

    companion object {
        const val IGNORE = ""
        const val ALIAS = "userAlias"
        const val AUTH_KEY = "auth.$ALIAS"
        const val TOKEN_KEY = "token.$ALIAS"
        const val ACCESS_TOKEN = "expectedAccessToken"
        const val REFRESH_TOKEN = "expectedRefreshToken"
        const val EXPECTED_CODE = "expected_code"
        const val SECRET = "secret123"
        const val EXPECTED_STATE = "eyJhbGlhcyI6InVzZXJBbGlhcyIsInNlY3JldCI6InNlY3JldDEyMyJ9"

        const val EMPTY_JSON = "{}"
        const val AUTH_STATE = """{"alias":"$ALIAS","secret": "$SECRET"}"""
        const val TOKEN_STATE = """{"accessToken":"$ACCESS_TOKEN","refreshToken":"$REFRESH_TOKEN"}"""

        const val TOKEN_STATE_BROKEN = """ {"accessToken"; "something"} """
        const val AUTH_STATE_BROKEN = """{"alias";""}"""

        const val PUBLIC_KEY = "eyJwdWIiOiJNSUlCSWpBTkJna3Foa2lHOXcwQkFRRUZBQU9DQVE4QU1JSUJDZ0tDQVFFQTJ3UFd2aGxYTFUybVFzOEIybUlaS0ZMK0x0cm81THp3VHR1YllzWWs2cTVPWWJ2eHhBVjRPWnZLaWc3enRaa2orM0R0RWlZaDVKTlVpMEpvQXRFQnhqRXVSeGVacDJ2eGJ4MUg3VDNVY29aRW9aNGE0QUQ0bFZoZ3ZKZVN5bXRUeUNPUC9WczlrUndBYnZuU2svR1d0RkVaeWtidUIvRDBIZ3NSdlNHTk9ZQUxpQzF2eXd4S1krak84QXpocmhna0VFa3M2bFZKTlZ1U0tmVTJwcDhwMVB3Q1pCaTVRQXR6a0JpOG92ODZOa1lmTlYrWC9SOStsVFJlSlZjeGNsNGxBQ3gzcU1nd05iT0Y0ZzluYnBrNFVmK1l6eWw5MWxGemFMY1dWRzNyeTRkVjFuRW9JR3Z2MXNkY05IaGZkSVJBNjd3VFQwaDVmV2ZiOUltdG9kaEpYUUlEQVFBQiIsInQiOiJhcHViIiwidiI6MX0="

        const val CALLBACK_URL = "http://localhost:8888/icarus/gccallback?code=$EXPECTED_CODE&state=$SECRET"
        const val AUTHORIZATION_URL = "expected_auth_url"
    }
}
