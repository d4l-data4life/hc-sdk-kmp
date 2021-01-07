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

enum class Environment {

    LOCAL,
    DEVELOPMENT,
    STAGING,
    SANDBOX,
    PRODUCTION;

    fun getApiBaseURL(platform: String): String {
        if (D4L.equals(platform, ignoreCase = true)) {
            return d4lBaseUrl()
        } else if (S4H.equals(platform, ignoreCase = true)) {
            return s4hBaseUrl()
        }
        throw IllegalArgumentException("No supported platform found for value($platform)")
    }

    private fun d4lBaseUrl(): String {
        return when (this) {
            SANDBOX -> "https://api-phdp-sandbox.hpsgc.de"
            DEVELOPMENT -> "https://api-phdp-dev.hpsgc.de"
            STAGING -> "https://api-staging.data4life.care"
            LOCAL -> "https://api.data4life.local"
            PRODUCTION -> "https://api.data4life.care"
        }
    }

    private fun s4hBaseUrl(): String {
        return when (this) {
            SANDBOX -> "https://api-sandbox.smart4health.eu"
            DEVELOPMENT -> "https://api-dev.smart4health.eu"
            STAGING -> "https://api-staging.smart4health.eu"
            LOCAL -> "https://api.smart4health.local"
            PRODUCTION -> "https://api.smart4health.eu"
        }
    }

    fun getCertificatePin(platform: String): String {
        if (D4L.equals(platform, ignoreCase = true)) {
            return d4lCertificatePin()
        } else if (S4H.equals(platform, ignoreCase = true)) {
            return sh4CertificatePin()
        }
        throw IllegalArgumentException("No supported platform found for value($platform)")
    }

    private fun d4lCertificatePin(): String {
        return when (this) {
            SANDBOX, DEVELOPMENT, LOCAL -> "sha256/3f81qEv2rjHvcrwof2egbKo5MjjSHaN/4DOl7R+pH0E="
            STAGING, PRODUCTION -> "sha256/AJvjswWs1n4m1KDmFNnTqBit2RHFvXsrVU3Uhxcoe4Y="
        }
    }

    private fun sh4CertificatePin(): String {
        return when (this) {
            SANDBOX, DEVELOPMENT, LOCAL, STAGING, PRODUCTION -> "sha256/yPBKbgJMVnMeovGKbAtuz65sfy/gpDu0WTiuB8bE5G0="
        }
    }

    companion object {
        private const val D4L = "d4l"
        private const val S4H = "s4h"

        @JvmStatic
        fun fromName(name: String?): Environment {
            return if (!name.isNullOrBlank()) {
                when {
                    LOCAL.name.equals(name, ignoreCase = true) -> LOCAL
                    DEVELOPMENT.name.equals(name, ignoreCase = true) -> DEVELOPMENT
                    STAGING.name.equals(name, ignoreCase = true) -> STAGING
                    SANDBOX.name.equals(name, ignoreCase = true) -> SANDBOX
                    else -> PRODUCTION
                }
            } else {
                PRODUCTION
            }
        }
    }
}
