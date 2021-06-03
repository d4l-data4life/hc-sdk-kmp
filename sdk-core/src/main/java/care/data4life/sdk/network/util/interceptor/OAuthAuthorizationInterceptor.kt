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

package care.data4life.sdk.network.util.interceptor

import care.data4life.auth.AuthorizationContract
import care.data4life.sdk.lang.CoreRuntimeException
import care.data4life.sdk.network.NetworkingContract.Companion.ACCESS_TOKEN_MARKER
import care.data4life.sdk.network.NetworkingContract.Companion.HEADER_ALIAS
import care.data4life.sdk.network.NetworkingContract.Companion.HEADER_AUTHORIZATION
import care.data4life.sdk.network.NetworkingInternalContract
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import java.io.IOException

internal class OAuthAuthorizationInterceptor private constructor(
    private val fetchInterceptor: NetworkingInternalContract.PartialInterceptor<Pair<String, Request>>,
    private val retryInterceptor: NetworkingInternalContract.PartialInterceptor<Triple<String, Request, Response>>,
) : NetworkingInternalContract.Interceptor {

    private fun purgeAlias(request: Request): Request {
        return request.newBuilder()
            .removeHeader(HEADER_ALIAS)
            .build()
    }

    private fun delegateInterception(
        alias: String?,
        request: Request,
        chain: Interceptor.Chain
    ): Response {
        return if (alias is String) {
            val cleanedRequest = purgeAlias(request)
            val response = fetchInterceptor.intercept(Pair(alias, cleanedRequest), chain)
            retryInterceptor.intercept(Triple(alias, cleanedRequest, response), chain)
        } else {
            throw CoreRuntimeException.InternalFailure()
        }
    }

    /**
     * Interceptor that attaches an authorization header to a request.
     *
     *
     * The authorization can be basic auth or OAuth. In the OAuth case, the
     * interceptor will try the request snd if it comes back with a status code
     * 401 (unauthorized), it will update the OAuth access token using the
     * refresh token.
     *
     * @param chain OkHttp interceptor chain
     * @return OkHttp response
     * @throws IOException
     */
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()

        return if (request.header(HEADER_AUTHORIZATION) == ACCESS_TOKEN_MARKER) {
            delegateInterception(request.header(HEADER_ALIAS), request, chain)
        } else {
            chain.proceed(request)
        }
    }

    companion object Factory :
        NetworkingInternalContract.InterceptorFactory<AuthorizationContract.Service> {
        override fun getInstance(payload: AuthorizationContract.Service): NetworkingInternalContract.Interceptor {
            return OAuthAuthorizationInterceptor(
                OAuthFetchTokenAuthorizationInterceptor(payload),
                OAuthRetryTokenAuthorizationInterceptor(payload)
            )
        }
    }
}
