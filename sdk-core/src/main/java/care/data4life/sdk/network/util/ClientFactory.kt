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

package care.data4life.sdk.network.util

import care.data4life.sdk.auth.AuthorizationContract
import care.data4life.sdk.network.NetworkingContract
import care.data4life.sdk.network.NetworkingContract.Companion.PLATFORM_S4H
import care.data4life.sdk.network.NetworkingInternalContract
import care.data4life.sdk.network.util.interceptor.BasicAuthorizationInterceptor
import care.data4life.sdk.network.util.interceptor.LoggingInterceptor
import care.data4life.sdk.network.util.interceptor.OAuthAuthorizationInterceptor
import care.data4life.sdk.network.util.interceptor.RetryInterceptor
import care.data4life.sdk.network.util.interceptor.StaticAuthorizationInterceptor
import care.data4life.sdk.network.util.interceptor.VersionInterceptor
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

object ClientFactory : NetworkingInternalContract.ClientFactory {
    private fun setCertificationPinner(
        builder: OkHttpClient.Builder,
        environment: NetworkingContract.Environment,
        platform: String,
    ): OkHttpClient.Builder {
        return if (platform == PLATFORM_S4H) {
            builder // do nothing for S4H
        } else {
            builder
                .certificatePinner(
                    CertificatePinnerFactory.getInstance(platform, environment)
                )
        }
    }

    private fun addAuthorizationInterceptor(
        builder: OkHttpClient.Builder,
        authService: AuthorizationContract.Service,
        user: String,
        clientSecret: String,
        staticAccessToken: ByteArray?
    ): OkHttpClient.Builder {
        return if (staticAccessToken is ByteArray) {
            builder.addInterceptor(
                StaticAuthorizationInterceptor.getInstance(
                    String(
                        staticAccessToken
                    )
                )
            )
        } else {
            builder.addInterceptor(OAuthAuthorizationInterceptor.getInstance(authService))
                .addInterceptor(
                    BasicAuthorizationInterceptor.getInstance(
                        Pair(user, clientSecret)
                    )
                )
        }
    }

    private fun setInterceptors(
        builder: OkHttpClient.Builder,
        authService: AuthorizationContract.Service,
        clientId: String,
        clientSecret: String,
        connectivityService: NetworkingContract.NetworkConnectivityService,
        agent: NetworkingContract.Clients,
        clientVersion: String,
        staticAccessToken: ByteArray?,
        debugFlag: Boolean
    ): OkHttpClient.Builder {
        return addAuthorizationInterceptor(
            builder,
            authService,
            clientId,
            clientSecret,
            staticAccessToken
        )
            .addInterceptor(RetryInterceptor.getInstance(connectivityService))
            .addInterceptor(LoggingInterceptor.getInstance(debugFlag))
            .addInterceptor(VersionInterceptor.getInstance(Pair(agent, clientVersion)))
    }

    private fun setTimeouts(builder: OkHttpClient.Builder): OkHttpClient.Builder {
        return builder
            .connectTimeout(NetworkingContract.REQUEST_TIMEOUT, TimeUnit.MINUTES)
            .readTimeout(NetworkingContract.REQUEST_TIMEOUT, TimeUnit.MINUTES)
            .writeTimeout(NetworkingContract.REQUEST_TIMEOUT, TimeUnit.MINUTES)
            .callTimeout(NetworkingContract.REQUEST_TIMEOUT, TimeUnit.MINUTES)
    }

    override fun getInstance(
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
    ): OkHttpClient {
        return OkHttpClient.Builder()
            .let { builder -> setCertificationPinner(builder, environment, platform) }
            .let { builder ->
                setInterceptors(
                    builder,
                    authService,
                    clientId,
                    clientSecret,
                    connectivityService,
                    clientName,
                    clientVersion,
                    staticAccessToken,
                    debugFlag
                )
            }
            .let { builder -> setTimeouts(builder) }
            .build()
    }
}
