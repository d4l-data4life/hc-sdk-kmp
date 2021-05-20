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

package care.data4life.sdk.network.interceptor

import care.data4life.auth.AuthorizationContract
import care.data4life.sdk.lang.D4LException
import care.data4life.sdk.network.NetworkingContract
import care.data4life.sdk.network.NetworkingContract.Companion.FORMAT_BEARER_TOKEN
import care.data4life.sdk.network.NetworkingContract.Companion.HEADER_AUTHORIZATION
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response

class OAuthFetchTokenAuthorizationInterceptor internal constructor(
    private val authService: AuthorizationContract.Service
) : NetworkingContract.PartialInterceptor<Pair<String, Request>> {
    private fun modifyRequest(request: Request, alias: String): Request {
        val token = authService.getAccessToken(alias)
        return request.newBuilder()
            .replaceHeader(
                HEADER_AUTHORIZATION,
                String.format(FORMAT_BEARER_TOKEN, token)
            ).build()
    }

    private fun determineRequest(request: Request, alias: String): Request {
        return try {
            modifyRequest(request, alias)
        } catch (e: D4LException) {
            request
        }
    }

    override fun intercept(
        payload: Pair<String, Request>,
        chain: Interceptor.Chain
    ): Response {
        val (alias, request) = payload

        return chain.proceed(determineRequest(request, alias))
    }
}
