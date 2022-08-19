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
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.Test

class VersionTest {
    private val moshi = Moshi.Builder().build()

    @Test
    fun `It fulfils Version`() {
        val version: Any = Version(
            25,
            "1.9.0",
            "supported"
        )

        assertTrue(version is NetworkModelContract.Version)
    }

    @Test
    fun `Version is serializable, it transforms into a valid JSON`() {
        assertEquals(
            expected = SERIALIZED_VERSION,
            actual = moshi.adapter(Version::class.java).toJson(VERSION)
        )
    }

    @Test
    fun `Given a Version is deserialized it transforms into Version`() {
        assertEquals(
            expected = VERSION,
            actual = moshi.adapter(Version::class.java).fromJson(SERIALIZED_VERSION)
        )
    }

    companion object {
        private val VERSION = Version(
            25,
            "1.9.0",
            "supported"
        )
        private const val SERIALIZED_VERSION = "{\"version_code\":25,\"version_name\":\"1.9.0\",\"status\":\"supported\"}"
    }
}
