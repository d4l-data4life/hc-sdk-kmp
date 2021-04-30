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

import care.data4life.sdk.lang.CoreRuntimeException
import care.data4life.sdk.network.NetworkingContract
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import java.net.SocketTimeoutException

class RetryInterceptor private constructor(
    private val connection: NetworkingContract.NetworkConnectivityService
) : NetworkingContract.Interceptor {
    private fun retry(request: Request, chain: Interceptor.Chain): Response {
        return if (connection.isConnected) {
            chain.proceed(request)
        } else {
            throw CoreRuntimeException.InternalFailure()
        }
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        return try {
            chain.proceed(request)
        } catch (e: SocketTimeoutException) {
            retry(request, chain)
        }
    }

    companion object Factory : NetworkingContract.InterceptorFactory<NetworkingContract.NetworkConnectivityService> {
        override fun getInstance(
            payload: NetworkingContract.NetworkConnectivityService
        ): NetworkingContract.Interceptor {
            return RetryInterceptor(payload)
        }
    }
}
