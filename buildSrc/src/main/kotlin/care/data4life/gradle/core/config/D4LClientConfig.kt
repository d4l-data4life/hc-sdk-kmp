/*
 * Copyright (c) 2022 D4L data4life gGmbH / All rights reserved.
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

package care.data4life.gradle.core.config

import care.data4life.gradle.core.config.Environment.PRODUCTION

data class D4LClientConfig(
    val platform: String,
    val configs: Map<Environment, ClientConfig>
) {
    operator fun get(environment: Environment): ClientConfig {
        return configs.getValue(environment)
    }

    fun toConfigMap(environment: Environment, debug: Boolean? = null): Map<String, String> {
        return mutableMapOf(
            "platform" to platform,
            "environment" to environment.toString(),
            "clientId" to get(environment).id,
            "clientSecret" to get(environment).secret,
            "redirectScheme" to get(environment).redirectScheme
        ).also {
            if (environment == PRODUCTION && debug == null) {
                it["debug"] = "false"
            } else if (debug != null) {
                it["debug"] = debug.toString()
            }
        }
    }
}

data class ClientConfig(
    val id: String,
    val secret: String,
    val redirectScheme: String
)

enum class Environment {
    LOCAL,
    DEVELOPMENT,
    STAGING,
    SANDBOX,
    PRODUCTION
}
