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

package care.data4life.sdk.e2e.util

import androidx.test.platform.app.InstrumentationRegistry
import com.squareup.moshi.Moshi
import java.io.BufferedReader
import java.io.FileNotFoundException

data class TestConfig(
        val user: User,
        val twillio: TwillioConfig
)

data class User(
        val email: String,
        val password: String,
        val phoneCountryCode: String,
        val phoneLocalNumber: String
) {
    val phoneNumber: String
        get() = phoneCountryCode + phoneLocalNumber
}

data class TwillioConfig(
        val accountSid: String,
        val authSid: String,
        val authToken: String
)

object TestConfigLoader {
    private const val FILE_NAME = "test_config.json"

    fun load(): TestConfig {
        try {
            val input = InstrumentationRegistry.getInstrumentation().context.assets.open(FILE_NAME)
            val json = input.bufferedReader().use(BufferedReader::readText)
            return Moshi.Builder().build().adapter(TestConfig::class.java).fromJson(json)!!
        } catch (e: FileNotFoundException) {
            throw IllegalStateException("Please run '/gradlew provideTestConfig' before running the tests", e)
        }
    }
}
