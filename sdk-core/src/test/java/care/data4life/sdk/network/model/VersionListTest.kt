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
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.*

class VersionListTest {
    @Test
    fun `Given a VersionList it implements interface `() {
        //Given
        val versions: Any = VersionList(listOf(Version(
                25,
                "1.9.0",
                "supported"
        )))
        assertTrue(versions is NetworkModelContract.VersionList)
    }

    @Test
    fun `VersionList is serializable `() {
        //Given
        val version = Version(
                25,
                "1.9.0",
                "supported"
        )
        val versions = VersionList(listOf<Version>(version))
        val moshi = Moshi.Builder().build()

        //When
        val actual = moshi.adapter<VersionList>(VersionList::class.java).toJson(versions)
        //Then
        assertEquals(
                "{\"versions\":[{\"status\":\"supported\",\"version_code\":25,\"version_name\":\"1.9.0\"}]}",
                actual
        )
    }


    @Test
    fun `isSupported returns true when version is supported `() {
        //Given
        val version = Version(
                25,
                "1.9.0",
                "supported"
        )
        val versions = VersionList(listOf<Version>(version))

        //When
        val isSupported = versions.isSupported(version.name)
        //Then
        assertTrue(isSupported)
    }

    @Test
    fun `isSupported returns true when version is not in versionList `() {
        //Given
        val version = Version(
                25,
                "1.9.0",
                "supported"
        )
        val currentVersion = Version(
                25,
                "1.9.3",
                "supported"
        )
        val versions = VersionList(listOf<Version>(version))

        //When
        val isSupported = versions.isSupported(currentVersion.name)
        //Then
        assertTrue ( isSupported )
    }

    @Test
    fun `isSupported returns false when version is unsupported `() {
        //Given
        val version = Version(
                25,
                "1.9.0",
                "unsupported"
        )
        val versions = VersionList(listOf<Version>(version))

        //When
        val isSupported = versions.isSupported(version.name)
        //Then
        assertFalse(isSupported)
    }
}
