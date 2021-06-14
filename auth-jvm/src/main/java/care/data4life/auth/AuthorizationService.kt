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

import care.data4life.auth.storage.InMemoryAuthStorage
import care.data4life.sdk.util.Base64
import com.github.scribejava.core.builder.ServiceBuilder
import com.github.scribejava.core.builder.api.DefaultApi20
import com.github.scribejava.core.extractors.TokenExtractor
import com.github.scribejava.core.model.OAuth2AccessToken
import com.github.scribejava.core.oauth.OAuth20Service
import com.squareup.moshi.Moshi
import java.io.IOException
import java.net.URLDecoder
import java.security.SecureRandom

actual class AuthorizationService @JvmOverloads constructor(
    alias: String,
    val configuration: AuthorizationConfiguration,
    private val storage: AuthorizationContract.Storage = InMemoryAuthStorage(),
    val base64: Base64 = Base64,
    private val service: OAuth20Service? = null
) : AuthorizationContract.Service {

    private val userId = alias

    private val moshi = Moshi.Builder().build()

    private var oAuthService = createService(alias)

    private fun createService(alias: String) = service ?: ServiceBuilder(configuration.clientId)
        .apiSecret(configuration.clientSecret)
        .state(getState(alias))
        .scope(configuration.scopes.joinToString(separator = " ") { it })
        .callback(configuration.callbackUrl)
        .build(AuthorizationApi(configuration))

    @Throws(AuthorizationException.FailedToRestoreAccessToken::class)
    actual override fun getAccessToken(alias: String): String {
        val state = readTokenState(alias)

        state?.accessToken?.let {
            return it
        }

        throw AuthorizationException.FailedToRestoreAccessToken()
    }

    @Throws(AuthorizationException.FailedToRestoreRefreshToken::class)
    actual override fun getRefreshToken(alias: String): String {
        val tokenState = readTokenState(alias)

        tokenState?.refreshToken?.let {
            return it
        }

        throw AuthorizationException.FailedToRestoreRefreshToken()
    }

    @Throws(AuthorizationException.FailedToRefreshAccessToken::class)
    actual override fun refreshAccessToken(alias: String): String {
        val state = readTokenState(alias)

        val token = state?.refreshToken?.let { oAuthService.refreshAccessToken(it) }
            ?: throw AuthorizationException.FailedToRestoreRefreshToken()

        val tokenState = TokenState(token.accessToken, token.refreshToken)
        writeTokenState(alias, tokenState)
        return tokenState.accessToken ?: throw AuthorizationException.FailedToRefreshAccessToken()
    }

    fun createAuthorizationUrl(alias: String, publicKey: String): String {
        val params = mutableMapOf<String, String>()
        params[PUBLIC_KEY] = publicKey
        params[USER_ID] = userId

        oAuthService = createService(alias)

        return oAuthService.getAuthorizationUrl(params)
    }

    private fun getState(alias: String): String {
        val state = if (storage.containsAuthState(authKey(alias))) {
            storage.readAuthState(authKey(alias))!!
        } else {
            val state = generateState()
            writeAuthState(alias, state)
            state
        }
        return base64.encodeToString(state)
    }

    private fun generateState(): String {
        val secret = SecureRandom().nextInt(99999999).toString()
        val state = AuthState(userId, secret)
        return moshi.adapter(AuthState::class.java).toJson(state)
    }

    fun finishAuthorization(alias: String, callbackUrl: String): Boolean {
        val auth = oAuthService.extractAuthorization(callbackUrl)
        val state = URLDecoder.decode(auth.state, "UTF-8")

        val authState = readAuthState(alias)
        val stateString = base64.encodeToString(
            moshi.adapter(AuthState::class.java)
                .toJson(authState)
        )

        if (oAuthService.state == state && state == stateString) {
            val authToken = oAuthService.getAccessToken(auth.code)

            val tokenState = TokenState(authToken.accessToken, authToken.refreshToken)
            writeTokenState(alias, tokenState)
            return true
        }

        return false
    }

    private fun readTokenState(alias: String): TokenState? {
        val tokenStateJson = storage.readAuthState(tokenKey(alias))

        try {
            return tokenStateJson?.let { moshi.adapter(TokenState::class.java).fromJson(it) }
        } catch (e: IOException) {
            throw AuthorizationException.FailedToRestoreTokenState()
        }
    }

    private fun writeTokenState(alias: String, authState: TokenState) {
        val json = moshi.adapter(TokenState::class.java).toJson(authState)
        storage.writeAuthState(tokenKey(alias), json)
    }

    private fun readAuthState(alias: String): AuthState? {
        val authStateJson = storage.readAuthState(authKey(alias))

        try {
            return authStateJson?.let { moshi.adapter(AuthState::class.java).fromJson(it) }
        } catch (e: IOException) {
            throw AuthorizationException.FailedToRestoreAuthState()
        }
    }

    private fun writeAuthState(alias: String, authState: String) {
        storage.writeAuthState(authKey(alias), authState)
    }

    actual override fun isAuthorized(alias: String) =
        storage.readAuthState(authKey(alias)) != null &&
            storage.readAuthState(tokenKey(alias)) != null

    private fun authKey(alias: String): String = authPrefix + alias

    private fun tokenKey(alias: String): String = tokenPrefix + alias

    actual override fun clear() {
        storage.clear()
    }

    companion object {
        private const val tokenPrefix = "token."
        private const val authPrefix = "auth."
        private const val PUBLIC_KEY = "public_key"
        private const val USER_ID = "user_id"

        private const val ERROR_FAILED_TO_RESTORE_ACCESS_TOKEN = "Failed to restore access token"
        private const val ERROR_FAILED_TO_RESTORE_REFRESH_TOKEN = "Failed to restore refresh token"
        private const val ERROR_FAILED_TO_REFRESH_ACCESS_TOKEN = "Failed to refresh access token"
        private const val ERROR_FAILED_TO_LOAD_TOKEN_STATE = "Failed to load token state"
        private const val ERROR_FAILED_TO_LOAD_AUTH_STATE = "Failed to load auth state"
    }
}

class AuthorizationApi(private val configuration: AuthorizationConfiguration) : DefaultApi20() {

    override fun getAccessTokenExtractor(): TokenExtractor<OAuth2AccessToken> {
        return super.getAccessTokenExtractor()
    }

    override fun getAuthorizationBaseUrl(): String {
        return configuration.authorizationEndpoint + Authorization.OAUTH_PATH_AUTHORIZE
    }

    override fun getAccessTokenEndpoint(): String {
        return configuration.tokenEndpoint + Authorization.OAUTH_PATH_TOKEN
    }
}
