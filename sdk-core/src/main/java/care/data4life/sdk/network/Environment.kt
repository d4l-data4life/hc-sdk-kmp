/*
 * Copyright (c) 2020 D4L data4life gGmbH / All rights reserved.
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

import care.data4life.sdk.network.NetworkingContract.Companion.D4L_SDL_CERT
import care.data4life.sdk.network.NetworkingContract.Companion.D4L_SP_CERT
import care.data4life.sdk.network.NetworkingContract.Companion.PLATFORM_D4L
import care.data4life.sdk.network.NetworkingContract.Companion.PLATFORM_S4H
import care.data4life.sdk.network.NetworkingContract.Companion.S4H_CERT

enum class Environment : NetworkingContract.Environment {
    LOCAL,
    DEVELOPMENT,
    STAGING,
    SANDBOX,
    PRODUCTION;

    override fun getApiBaseURL(platform: String): String {
        return when {
            PLATFORM_D4L.equals(platform, ignoreCase = true) -> d4lBaseUrl()
            PLATFORM_S4H.equals(platform, ignoreCase = true) -> s4hBaseUrl()
            else -> throw IllegalArgumentException("No supported platform found for value($platform)")
        }
    }

    private fun d4lBaseUrl(): String {
        return NetworkingContract.Data4LifeURI.valueOf(this.name).uri
    }

    private fun s4hBaseUrl(): String {
        return NetworkingContract.Smart4HealthURI.valueOf(this.name).uri
    }

    override fun getCertificatePin(platform: String): String {
        return when {
            PLATFORM_D4L.equals(platform, ignoreCase = true) -> d4lCertificatePin()
            PLATFORM_S4H.equals(platform, ignoreCase = true) -> sh4CertificatePin()
            else -> throw IllegalArgumentException("No supported platform found for value($platform)")
        }
    }

    private fun d4lCertificatePin(): String {
        return when (this) {
            SANDBOX, DEVELOPMENT, LOCAL -> D4L_SDL_CERT
            STAGING, PRODUCTION -> D4L_SP_CERT
        }
    }

    private fun sh4CertificatePin(): String {
        return when (this) {
            SANDBOX, DEVELOPMENT, LOCAL, STAGING, PRODUCTION -> S4H_CERT
        }
    }

    companion object Factory : NetworkingContract.EnvironmentFactory {
        private fun determineEnvironment(name: String): Environment {
            return when {
                LOCAL.name.equals(name, ignoreCase = true) -> LOCAL
                DEVELOPMENT.name.equals(name, ignoreCase = true) -> DEVELOPMENT
                STAGING.name.equals(name, ignoreCase = true) -> STAGING
                SANDBOX.name.equals(name, ignoreCase = true) -> SANDBOX
                else -> PRODUCTION
            }
        }

        @JvmStatic
        override fun fromName(name: String?): Environment {
            return if (!name.isNullOrBlank()) {
                determineEnvironment(name)
            } else {
                PRODUCTION
            }
        }
    }
}
