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

package care.data4life.sdk.network.model

import com.squareup.moshi.Moshi
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class VersionTest {

    @Test
    fun `Given a Version it  the Version interface`() {
        // Given
        val version: Any = Version(
            25,
            "1.9.0",
            "supported"
        )
        assertTrue(version is NetworkModelContract.Version)
    }

    @Test
    fun `Version is serializable, it builds the valid json format`() {
        // Given
        val version = Version(
            25,
            "1.9.0",
            "supported"
        )
        val moshi = Moshi.Builder().build()

        // When
        val actual = moshi.adapter<Version>(Version::class.java).toJson(version)
        assertEquals(
            "{\"status\":\"supported\",\"version_code\":25,\"version_name\":\"1.9.0\"}",
            actual
        )
    }

    @Test
    fun `Given a Version is deserialized it transforms into Version`() {
        // Given
        val moshi = Moshi.Builder()
            .build()
        val versionJson =
            "{\"status\":\"supported\",\"version_code\":25,\"version_name\":\"1.9.0\"}"

        // When
        val version = moshi.adapter<Version>(Version::class.java).fromJson(versionJson)

        // Then
        assertEquals(
            version,
            Version(
                25,
                "1.9.0",
                "supported"
            )
        )
    }
}
