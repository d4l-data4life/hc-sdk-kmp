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

import care.data4life.sdk.network.NetworkingContract
import care.data4life.sdk.network.NetworkingContract.Companion.BASIC_AUTH_MARKER
import care.data4life.sdk.network.NetworkingContract.Companion.FORMAT_BASIC_AUTH
import care.data4life.sdk.network.NetworkingContract.Companion.HEADER_AUTHORIZATION
import care.data4life.sdk.util.Base64
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import java.io.IOException

internal class BasicAuthorizationInterceptor private constructor(
    private val credentials: String
) : NetworkingContract.Interceptor {
    private fun modifyRequest(request: Request): Request {
        return request.newBuilder()
            .replaceHeader(HEADER_AUTHORIZATION, credentials)
            .build()
    }

    private fun replaceAndProceed(
        chain: Interceptor.Chain,
        request: Request
    ): Response = chain.proceed(modifyRequest(request))

    /**
     * Interceptor that attaches an authorization header to a request.
     * <p>
     * The authorization is basic auth.
     *
     * @param chain OkHttp interceptor chain
     * @return OkHttp response
     * @throws IOException
     */
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        return if (request.header(HEADER_AUTHORIZATION) == BASIC_AUTH_MARKER) {
            replaceAndProceed(chain, request)
        } else {
            chain.proceed(request)
        }
    }

    companion object Factory : NetworkingContract.InterceptorFactory<Pair<String, String>> {
        private fun prepareCredentials(credentials: Pair<String, String>): String {
            val (user, secret) = credentials
            return String.format(
                FORMAT_BASIC_AUTH,
                Base64.encodeToString("$user:$secret")
            )
        }

        override fun getInstance(payload: Pair<String, String>): NetworkingContract.Interceptor {
            return BasicAuthorizationInterceptor(prepareCredentials(payload))
        }
    }
}
