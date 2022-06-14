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
package care.data4life.sdk

import care.data4life.fhir.util.Preconditions
import care.data4life.sdk.auth.Authorization.Companion.defaultScopes
import care.data4life.sdk.InitializationConfig
import care.data4life.sdk.auth.Authorization

class InitializationConfig private constructor(
    val alias: String,
    val scopes: Set<String>
) {

    class Builder {
        private var alias: String = DEFAULT_ALIAS
        private var scopes: Set<String> = DEFAULT_SCOPES

        fun setAlias(alias: String): Builder {
            this.alias = alias
            return this
        }

        fun setScopes(scopes: Set<String>): Builder {
            this.scopes = scopes
            return this
        }

        fun build(): InitializationConfig {
            Preconditions.checkArgument(alias.isNotEmpty(), "alias is required")
            Preconditions.checkArgument(scopes.isNotEmpty(), "scopes are required")
            return InitializationConfig(alias, scopes)
        }
    }

    companion object {
        private const val DEFAULT_ALIAS = "data4life_android"
        private val DEFAULT_SCOPES = defaultScopes

        @JvmField
        var DEFAULT_CONFIG = Builder().build()
    }
}
