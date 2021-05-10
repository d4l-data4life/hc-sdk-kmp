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

import care.data4life.sdk.lang.DataValidationException
import care.data4life.sdk.tag.Annotations
import care.data4life.sdk.tag.Tags
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class LimitGuardTest {
    private fun buildMap(numberOfEntries: Int): Tags {
        var char = 'A'
        val map = hashMapOf<String, String>()
        for (i in 0 until numberOfEntries) {
            map[char.toString()] = char.toString()
            char++
        }

        return map
    }

    private fun buildList(numberOfEntries: Int): Annotations {
        var char = 'A'
        val list = mutableListOf<String>()
        for (i in 0 until numberOfEntries) {
            list.add(char.toString())
            char++
        }

        return list
    }

    @Test
    fun `It fulfils LimitGuard`() {
        val guard: Any = DecryptedRecordGuard

        assertTrue(guard is NetworkModelContract.LimitGuard)
    }

    @Test
    fun `Given, checkTagsAndAnnotationsLimits is called with Tags, which exceeds the limit, and empty Annotations, it fails with a TagsAndAnnotationsLimitViolation`() {
        val tags = buildMap(1000)

        // Then
        val error = assertFailsWith<DataValidationException.TagsAndAnnotationsLimitViolation> {
            // When
            DecryptedRecordGuard.checkTagsAndAnnotationsLimits(tags, listOf())
        }

        assertEquals(
            error.message,
            "Annotations and Tags are exceeding maximum length"
        )
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
        val annotations = buildList(1000)

        // Then
        val error = assertFailsWith<DataValidationException.TagsAndAnnotationsLimitViolation> {
            // When
            DecryptedRecordGuard.checkTagsAndAnnotationsLimits(mapOf(), annotations)
        }

        assertEquals(
            error.message,
            "Annotations and Tags are exceeding maximum length"
        )
    }

    @Test
    fun `Given, checkTagsAndAnnotationsLimits is called with empty Tags and Annotations, which are in the boundaries, it accepts`() {
        // Given
        val annotations = buildList(999)

        // When
        DecryptedRecordGuard.checkTagsAndAnnotationsLimits(mapOf(), annotations)

        // Then
        assertTrue(true)
    }

    @Test
    fun `Given, checkTagsAndAnnotationsLimits is called with Tags and Annotations, which exceeding the limit together, it fails with a TagsAndAnnotationsLimitViolation`() {
        val tags = buildMap(25)
        val annotations = buildList(1000)

        // Then
        val error = assertFailsWith<DataValidationException.TagsAndAnnotationsLimitViolation> {
            // When
            DecryptedRecordGuard.checkTagsAndAnnotationsLimits(tags, annotations)
        }

        assertEquals(
            error.message,
            "Annotations and Tags are exceeding maximum length"
        )
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

        // Then
        val error = assertFailsWith<DataValidationException.CustomDataLimitViolation> {
            // When
            DecryptedRecordGuard.checkDataLimit(data)
        }

        assertEquals(
            error.message,
            "The given record data exceeds the maximum size"
        )
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
