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

import care.data4life.sdk.lang.DataValidationException
import care.data4life.sdk.network.model.DecryptedRecordGuard
import care.data4life.sdk.network.model.definitions.LimitGuard
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LimitGuardTest {
    private fun buildMap(numberOfEntries: Int): HashMap<String, String> {
        var char = 'A'
        val map = hashMapOf<String, String>()
        for (i in 0 until numberOfEntries) {
            map[char.toString()] = char.toString()
            char++
        }

        return map
    }

    private fun buildList(numberOfEntries: Int): List<String> {
        var char = 'A'
        val list = mutableListOf<String>()
        for (i in 0 until numberOfEntries) {
            list.add(char.toString())
            char++
        }

        return list
    }

    @Test
    fun `it is a LimitGuard`() {
        assertTrue(DecryptedRecordGuard is LimitGuard)
    }

    @Test
    fun `Given, checkTagsAndAnnotationsLimits is called with Tags, which exceeds the limit, and empty Annotations, it fails with a TagsAndAnnotationsLimitViolation`() {
        val tags = buildMap(50)

        // When
        try {
            DecryptedRecordGuard.checkTagsAndAnnotationsLimits(tags, listOf())
            assertTrue(false)// FIXME: This is stupid
        } catch (e: Exception) {
            // Then
            assertTrue(e is DataValidationException.TagsAndAnnotationsLimitViolation)
            assertEquals(
                    e.message,
                    "Annotations and Tags are exceeding maximum length"
            )
        }
    }

    @Test
    fun `Given, checkTagsAndAnnotationsLimits is called with Tags, which are in the boundaries, and empty Annotations, it accepts`() {
        // Given
        val tags = buildMap(49)

        // When
        DecryptedRecordGuard.checkTagsAndAnnotationsLimits(tags, listOf())

        // Then
        assertTrue(true)
    }

    @Test
    fun `Given, checkTagsAndAnnotationsLimits is called with empty Tags, and empty Annotations, which exceeds the limit, it fails with a TagsAndAnnotationsLimitViolation`() {
        val annotations = buildList(100)

        // When
        try {
            DecryptedRecordGuard.checkTagsAndAnnotationsLimits(hashMapOf(), annotations)
            assertTrue(false)// FIXME: This is stupid
        } catch (e: Exception) {
            // Then
            assertTrue(e is DataValidationException.TagsAndAnnotationsLimitViolation)
            assertEquals(
                    e.message,
                    "Annotations and Tags are exceeding maximum length"
            )
        }
    }

    @Test
    fun `Given, checkTagsAndAnnotationsLimits is called with empty Tags and Annotations, which are in the boundaries, it accepts`() {
        // Given
        val annotations = buildList(99)

        // When
        DecryptedRecordGuard.checkTagsAndAnnotationsLimits(hashMapOf(), annotations)

        // Then
        assertTrue(true)
    }

    @Test
    fun `Given, checkTagsAndAnnotationsLimits is called with Tags and Annotations, which exceeding the limit together, it fails with a TagsAndAnnotationsLimitViolation`() {
        val tags = buildMap(25)
        val annotations = buildList(50)

        // When
        try {
            DecryptedRecordGuard.checkTagsAndAnnotationsLimits(tags, annotations)
            assertTrue(false)// FIXME: This is stupid
        } catch (e: Exception) {
            // Then
            assertTrue(e is DataValidationException.TagsAndAnnotationsLimitViolation)
            assertEquals(
                    e.message,
                    "Annotations and Tags are exceeding maximum length"
            )
        }
    }

    @Test
    fun `Given, checkTagsAndAnnotationsLimits is called with Tags and Annotations, which are together in the boundaries, it accepts`() {
        // Given
        val tags = buildMap(24)
        val annotations = buildList(49)

        // When
        DecryptedRecordGuard.checkTagsAndAnnotationsLimits(tags, annotations)

        // Then
        assertTrue(true)
    }

    @Test
    fun `Given, checkDataLimit is called with a payload, which exceeds the boundaries, it fails with CustomDataLimitViolation`() {
        // Given
        val data = ByteArray(10485760)

        // When
        try {
            DecryptedRecordGuard.checkDataLimit(data)
            assertTrue(false)// FIXME: This is stupid
        } catch (e: Exception) {
            // Then
            assertTrue(e is DataValidationException.CustomDataLimitViolation)
            assertEquals(
                    e.message,
                    "The given record data exceeds the maximum size"
            )
        }
    }

    @Test
    fun `Given, checkDataLimit is called with a payload, which in the limit, it accepts`() {
        // Given
        val data = ByteArray(10)

        // When
        DecryptedRecordGuard.checkDataLimit(data)

        // Then
        assertTrue(true)
    }
}
