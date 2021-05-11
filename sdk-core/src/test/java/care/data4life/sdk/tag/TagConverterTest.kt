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

package care.data4life.sdk.tag

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TagConverterTest {
    @Test
    fun `It fulfils TagConverter`() {
        val converter: Any = TagConverter
        assertTrue(converter is TaggingContract.Converter)
    }

    @Test
    fun `Given, convertToTagMap is called with a List of Strings, it converts them into a list and deserializes the key and string`() {
        // Given
        val serializesTags = listOf(
            "potato=bread",
            "tomato=soup"
        )

        val expected = hashMapOf(
            "potato" to "bread",
            "tomato" to "soup"
        )

        // When
        val tags = TagConverter.toTags(serializesTags)

        // Then
        assertEquals(
            expected,
            tags
        )
    }

    @Test
    fun `Given convertToTagMap is called with a empty List, it returns a empty Map`() {
        // Given
        val serializesTags = listOf<String>()

        // When
        val result = TagConverter.toTags(serializesTags)

        // Then
        assertTrue(result.isEmpty())
    }
}
