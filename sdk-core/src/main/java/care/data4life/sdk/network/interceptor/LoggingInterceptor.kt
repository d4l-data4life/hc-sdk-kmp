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

import care.data4life.sdk.network.NetworkingContract
import okhttp3.Interceptor
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor

class LoggingInterceptor private constructor(
    private val interceptor: HttpLoggingInterceptor
) : NetworkingContract.Interceptor {
    override fun intercept(
        chain: Interceptor.Chain
    ): Response = interceptor.intercept(chain)

    companion object Factory : NetworkingContract.InterceptorFactory<Boolean> {
        private fun determineDebugLevel(flag: Boolean): HttpLoggingInterceptor.Level {
            return if (flag) {
                HttpLoggingInterceptor.Level.HEADERS
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }

        override fun getInstance(payload: Boolean): NetworkingContract.Interceptor {
            val interceptor = HttpLoggingInterceptor()
                .setLevel(determineDebugLevel(payload))

            return LoggingInterceptor(interceptor)
        }
    }
}
