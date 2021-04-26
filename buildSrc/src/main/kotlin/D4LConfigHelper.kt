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

import com.google.gson.GsonBuilder
import java.io.File

object D4LConfigHelper {

    private const val FILE_NAME_CLIENT_CONFIG_ANDROID = "d4l-client-config-android.json"
    private const val FILE_NAME_TEST_CONFIG_ANDROID = "d4l-test-config-android.json"

    private const val ENV_CLIENT_CONFIG_ANDROID = "D4L_CLIENT_CONFIG_ANDROID"
    private const val ENV_TEST_CONFIG_ANDROID = "D4L_TEST_CONFIG_ANDROID"

    private fun gson() = GsonBuilder().setPrettyPrinting().create()

    private fun loadConfig(path: String, fileName: String, envVarName: String): String {
        return try {
            File(path, fileName).readText()
        } catch (e: Exception) {
            try {
                System.getenv(envVarName)
            } catch (e: Exception) {
                null
            }
        } ?: throw IllegalStateException(
            "Config file not found here: $path/$fileName nor environment variable $envVarName was set"
        )
    }

    fun loadClientConfigAndroid(path: String): D4LClientConfig {
        val configJson =
            loadConfig(path, FILE_NAME_CLIENT_CONFIG_ANDROID, ENV_CLIENT_CONFIG_ANDROID)
        return gson().fromJson(configJson, D4LClientConfig::class.java)
    }

    fun toJson(d4lClientConfig: D4LClientConfig): String {
        return gson().toJson(d4lClientConfig)
    }

    fun loadTestConfigAndroid(path: String): D4LTestConfig {
        val configJson = loadConfig(path, FILE_NAME_TEST_CONFIG_ANDROID, ENV_TEST_CONFIG_ANDROID)
        return gson().fromJson(configJson, D4LTestConfig::class.java)
    }

    fun toJson(d4lTestConfig: D4LTestConfig): String {
        return gson().toJson(d4lTestConfig)
    }
}
