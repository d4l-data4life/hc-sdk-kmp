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


import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.*

class TagEncryptionHelperTest {
    @Test
    fun `it full fills the TaggingContract#Helper`() {
        assertTrue((TagEncryptionHelper as Any) is TaggingContract.Helper)
    }

    @Test
    fun `Given convertToTagList is called with a Map of Strings to String, it converts them into a list and serializes the key and string`() {
        // Given
        val tags = hashMapOf(
                "potato" to "bread",
                "tomato" to "soup"
        )

        val expected = listOf(
                "potato=bread",
                "tomato=soup"
        )

        // When
        val serializesTags = TagEncryptionHelper.convertToTagList(tags)

        assertEquals(
                expected,
                serializesTags
        )
    }

    @Test
    fun `Given convertToTagList is called with a empty Map, it returns a empty list`() {
        // Given
        val tags = HashMap<String, String>()

        // When
        val result = TagEncryptionHelper.convertToTagList(tags)

        // Then
        assertTrue(result.isEmpty())
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
        val tags = TagEncryptionHelper.convertToTagMap(serializesTags)

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
        val result = TagEncryptionHelper.convertToTagMap(serializesTags)

        // Then
        assertTrue(result.isEmpty())
    }
}
