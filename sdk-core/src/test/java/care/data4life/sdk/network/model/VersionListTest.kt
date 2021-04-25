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
import io.mockk.mockk
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class VersionListTest {
    private val moshi = Moshi.Builder().build()

    @Test
    fun `It fulfills VersionList`() {
        val versions: Any = VersionList(mockk())
        assertTrue(versions is NetworkModelContract.VersionList)
    }

    @Test
    fun `Given a VersionList is serialized, it transforms into a valid JSON format`() {
        assertEquals(
            actual = moshi.adapter(VersionList::class.java).toJson(VERSION_LIST),
            expected = SERIALIZED_VERSION_LIST
        )
    }

    @Test
    fun `Given a VersionList is deserialized, it transforms into VersionList`() {
        assertEquals(
            actual = moshi.adapter(VersionList::class.java).fromJson(
                SERIALIZED_VERSION_LIST
            ),
            expected = VERSION_LIST
        )
    }

    @Test
    fun `Given isSupported is called with a version, it returns SUPPORTED, if the given version is supported`() {
        // Given
        val version = Version(
            25,
            "1.10.0",
            "supported"
        )
        val versions = VersionList(listOf(version))

        // When
        val status = versions.resolveSupportStatus(version.name)
        // Then
        assertEquals(
            expected = NetworkModelContract.VersionStatus.SUPPORTED,
            actual = status
        )
    }

    @Test
    fun `Given isSupported is called with a version, it ignores any amendments to the version itself`() {
        // Given
        val version = Version(
            25,
            "1.10.0-config.debug",
            "supported"
        )
        val versions = VersionList(listOf(version))

        // When
        val status = versions.resolveSupportStatus(version.name)
        // Then
        assertEquals(
            expected = NetworkModelContract.VersionStatus.SUPPORTED,
            actual = status
        )
    }

    @Test
    fun `Given isSupported is called with a version, it returns SUPPORTED, if version is unknown`() {
        // Given
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
        val versions = VersionList(listOf(version))

        // When
        val status = versions.resolveSupportStatus(currentVersion.name)
        // Then
        assertEquals(
            expected = NetworkModelContract.VersionStatus.SUPPORTED,
            actual = status
        )
    }

    @Test
    fun `Given isSupported is called with a version, it returns UNSPPORTED if version is unsupported`() {
        // Given
        val version = Version(
            25,
            "1.9.0",
            "unsupported"
        )
        val versions = VersionList(listOf(version))

        // When
        val status = versions.resolveSupportStatus(version.name)
        // Then
        assertEquals(
            expected = NetworkModelContract.VersionStatus.UNSUPPORTED,
            actual = status
        )
    }

    @Test
    fun `Given isSupported is called with a version, it returns DEPRECATED if version is deprecated`() {
        // Given
        val version = Version(
            25,
            "1.9.0",
            "deprecated"
        )
        val versions = VersionList(listOf(version))

        // When
        val status = versions.resolveSupportStatus(version.name)
        // Then
        assertEquals(
            expected = NetworkModelContract.VersionStatus.DEPRECATED,
            actual = status
        )
    }

    companion object {
        private val VERSION = Version(
            25,
            "1.9.0",
            "supported"
        )
        private val VERSION_LIST = VersionList(listOf(VERSION))
        private const val SERIALIZED_VERSION_LIST =
            "{\"versions\":[{\"version_code\":25,\"version_name\":\"1.9.0\",\"status\":\"supported\"}]}"
    }

    companion object {
        private val VERSION = Version(
            25,
            "1.9.0",
            "supported"
        )
        private val VERSION_LIST = VersionList(listOf(VERSION))
        private const val SERIALIZED_VERSION_LIST =
            "{\"versions\":[{\"status\":\"supported\",\"version_code\":25,\"version_name\":\"1.9.0\"}]}"
    }

    companion object {
        private val VERSION = Version(
            25,
            "1.9.0",
            "supported"
        )
        private val VERSION_LIST = VersionList(listOf(VERSION))
        private const val SERIALIZED_VERSION_LIST =
            "{\"versions\":[{\"status\":\"supported\",\"version_code\":25,\"version_name\":\"1.9.0\"}]}"
    }
}
