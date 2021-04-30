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

package care.data4life.sdk.network.interceptors

import care.data4life.auth.AuthorizationContract
import care.data4life.sdk.lang.D4LException
import care.data4life.sdk.network.NetworkingContract
import care.data4life.sdk.network.NetworkingContract.Companion.FORMAT_BEARER_TOKEN
import care.data4life.sdk.network.NetworkingContract.Companion.HEADER_AUTHORIZATION
import care.data4life.sdk.network.NetworkingContract.Companion.HTTP_401_UNAUTHORIZED
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response

class OAuthRetryTokenAuthorizationInterceptor internal constructor(
    private val authService: AuthorizationContract.Service
) : NetworkingContract.PartialInterceptor<Triple<String, Request, Response>> {
    private fun requestAgain(
        chain: Interceptor.Chain,
        request: Request,
        failedResponse: Response
    ): Response = failedResponse.close().let { chain.proceed(request) }

    private fun retry(
        alias: String,
        chain: Interceptor.Chain,
        failedRequest: Request,
        failedResponse: Response
    ): Response {
        val token = try {
            authService.refreshAccessToken(alias)
        } catch (e: D4LException) {
            authService.clear()
            return failedResponse
        }

        val request = failedRequest.newBuilder()
            .replaceHeader(
                HEADER_AUTHORIZATION,
                String.format(FORMAT_BEARER_TOKEN, token)
            )
            .build()

        return requestAgain(chain, request, failedResponse)
    }

    override fun intercept(
        payload: Triple<String, Request, Response>,
        chain: Interceptor.Chain
    ): Response {
        val (alias, request, response) = payload

        return if (response.code == HTTP_401_UNAUTHORIZED) {
            retry(alias, chain, request, response)
        } else {
            response
        }
    }
}
