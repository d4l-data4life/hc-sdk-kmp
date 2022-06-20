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

package care.data4life.sdk.network

import care.data4life.sdk.auth.AuthorizationContract
import okhttp3.CertificatePinner
import okhttp3.OkHttpClient
import okhttp3.Response

internal class NetworkingInternalContract {
    interface CertificatePinnerFactory {
        fun getInstance(
            platform: String,
            environment: NetworkingContract.Environment
        ): CertificatePinner
    }

    interface Interceptor : okhttp3.Interceptor {
        override fun intercept(chain: okhttp3.Interceptor.Chain): Response
    }

    interface PartialInterceptor<T : Any> {
        fun intercept(
            payload: T,
            chain: okhttp3.Interceptor.Chain
        ): Response
    }

    interface InterceptorFactory<T : Any> {
        fun getInstance(payload: T): Interceptor
    }

    enum class Data4LifeURI(val uri: String) {
        SANDBOX("https://api-phdp-sandbox.hpsgc.de"),
        DEVELOPMENT("https://api-phdp-dev.hpsgc.de"),
        STAGING("https://api-staging.data4life.care"),
        LOCAL("https://api.data4life.local"),
        PRODUCTION("https://api.data4life.care")
    }

    enum class Smart4HealthURI(val uri: String) {
        SANDBOX("https://api-sandbox.smart4health.eu"),
        DEVELOPMENT("https://api-dev.smart4health.eu"),
        STAGING("https://api-staging.smart4health.eu"),
        LOCAL("https://api.smart4health.local"),
        PRODUCTION("https://api.smart4health.eu")
    }

    interface ClientFactory {
        fun getInstanceLegacy(
            authService: AuthorizationContract.Service,
            environment: NetworkingContract.Environment,
            clientId: String,
            clientSecret: String,
            platform: String,
            connectivityService: NetworkingContract.NetworkConnectivityService,
            clientName: NetworkingContract.Clients,
            clientVersion: String,
            staticAccessToken: ByteArray?,
            debugFlag: Boolean
        ): OkHttpClient
    }

    interface HealthCloudApiFactory {
        fun getInstance(
            client: OkHttpClient,
            platform: String,
            environment: NetworkingContract.Environment
        ): HealthCloudApi
    }
}
