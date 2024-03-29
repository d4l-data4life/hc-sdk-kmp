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
import care.data4life.sdk.network.NetworkingContract.Companion.FORMAT_CLIENT_VERSION
import care.data4life.sdk.network.NetworkingInternalContract
import okhttp3.Interceptor
import okhttp3.Response

internal class VersionInterceptor private constructor(
    private val version: String
) : NetworkingInternalContract.Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
            .newBuilder()
            .addHeader(
                NetworkingContract.HEADER_SDK_VERSION,
                version
            )
            .build()
        return chain.proceed(request)
    }

    companion object Factory :
        NetworkingInternalContract.InterceptorFactory<Pair<NetworkingContract.Client, String>> {
        private fun format(
            platform: NetworkingContract.Client,
            version: String
        ): String {
            return String.format(
                FORMAT_CLIENT_VERSION,
                platform.identifier,
                version
            )
        }

        override fun getInstance(payload: Pair<NetworkingContract.Client, String>): NetworkingInternalContract.Interceptor {
            val (platform, version) = payload

            return VersionInterceptor(
                format(platform, version)
            )
        }
    }
}
