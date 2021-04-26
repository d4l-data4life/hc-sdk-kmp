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

package care.data4life.auth.test.util

import com.squareup.moshi.Moshi
import java.io.BufferedReader
import java.io.FileNotFoundException

data class D4LClientConfig(
    val configs: Map<Environment, ClientConfig>
) {
    operator fun get(environment: Environment): ClientConfig {
        return configs.getValue(environment)
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
    PRODUCTION;

    override fun toString(): String {
        return super.toString().toLowerCase()
    }
}

object ClientConfigLoader {
    private const val FILE_NAME = "client_config.json"

    fun load(): D4LClientConfig {
        try {
            val input = this.javaClass.classLoader.getResourceAsStream(FILE_NAME)
            val json = input.bufferedReader().use(BufferedReader::readText)
            return Moshi.Builder().build().adapter(D4LClientConfig::class.java).fromJson(json)!!
        } catch (exception: FileNotFoundException) {
            throw IllegalStateException(
                "Please run '/gradlew provideTestConfig' before running the tests",
                exception
            )
        } catch (exception: IllegalStateException) {
            throw IllegalStateException(
                "Please run '/gradlew provideTestConfig' before running the tests",
                exception
            )
        }
    }
}
