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
import care.data4life.sdk.network.NetworkingContract.Companion.HEADER_ALIAS
import care.data4life.sdk.network.NetworkingContract.Companion.HEADER_AUTHORIZATION
import care.data4life.sdk.network.NetworkingInternalContract
import java.io.IOException
import okhttp3.Interceptor
import okhttp3.Response

internal class StaticAuthorizationInterceptor private constructor(
    token: String
) : NetworkingInternalContract.Interceptor {
    private val authHeader = String.format(NetworkingContract.FORMAT_BEARER_TOKEN, token)

    /**
     * Interceptor that attaches an OAuth access token to a request.
     * <p>
     * This interceptor is used for the case where the SDK client does not
     * handle the OAuth flow itself and merely gets an access token injected.
     * Accordingly this interceptor does not attempt to refresh the access token
     * if the request should fail.
     *
     * @param chain OkHttp interceptor chain
     * @return OkHttp response
     * @throws IOException
     */
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
            .newBuilder()
            .removeHeader(HEADER_ALIAS)
            .replaceHeader(HEADER_AUTHORIZATION, authHeader)
            .build()
        return chain.proceed(request)
    }

    companion object Factory : NetworkingInternalContract.InterceptorFactory<String> {
        override fun getInstance(payload: String): NetworkingInternalContract.Interceptor {
            return StaticAuthorizationInterceptor(payload)
        }
    }
}
