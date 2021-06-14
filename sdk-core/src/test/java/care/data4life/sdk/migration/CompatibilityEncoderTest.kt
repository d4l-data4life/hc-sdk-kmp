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

package care.data4life.sdk.migration

import care.data4life.sdk.tag.TagEncoding
import care.data4life.sdk.wrapper.UrlEncoding
import io.mockk.every
import io.mockk.mockkObject
import io.mockk.unmockkObject
import io.mockk.verify
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CompatibilityEncoderTest {
    private lateinit var compatibilityEncoder: MigrationInternalContract.CompatibilityEncoder

    @Before
    fun setUp() {
        mockkObject(TagEncoding)
        mockkObject(UrlEncoding)

        compatibilityEncoder = CompatibilityEncoder
    }

    @After
    fun tearDown() {
        unmockkObject(TagEncoding)
        unmockkObject(UrlEncoding)
    }

    @Test
    fun `It fulfils CompatibilityEncoder`() {
        val encoder: Any = CompatibilityEncoder

        assertTrue(encoder is MigrationInternalContract.CompatibilityEncoder)
    }

    @Test
    fun `Given encode is called with a String, it delegates the String to encode of the TagEncoder and returns its result as the actual valid encoding`() {
        // Given
        val tagValue = "I am a value"
        val expected = "Well done"

        every { TagEncoding.encode(tagValue) } returns expected
        every { TagEncoding.normalize(tagValue) } returns "notImportant"
        every { UrlEncoding.encode(tagValue) } returns "notImportant"

        // When
        val (encoded, _, _) = compatibilityEncoder.encode(tagValue)

        // Then
        assertEquals(
            expected = expected,
            actual = encoded
        )

        verify(exactly = 1) { TagEncoding.encode(tagValue) }
    }

    @Test
    fun `Given encode is called with a String, it delegates the String to normalize of the TagEncoder and returns its result as Android legacy encoding`() {
        // Given
        val tagValue = "I am a value"
        val expected = "Well done"

        every { TagEncoding.encode(tagValue) } returns "notImportant"
        every { TagEncoding.normalize(tagValue) } returns expected
        every { UrlEncoding.encode(tagValue) } returns "notImportant"

        // When
        val (_, encoded, _) = compatibilityEncoder.encode(tagValue)

        // Then
        assertEquals(
            expected = expected,
            actual = encoded
        )

        verify(exactly = 1) { TagEncoding.normalize(tagValue) }
    }

    @Test
    fun `Given encode is called with a String, it delegates the String to encode of the URLEncoder and returns its result as JS legacy encoding`() {
        // Given
        val tagValue = "I am a value"
        val expected = "Well done"
        val normalized = "Soon"

        every { TagEncoding.encode(tagValue) } returns "notImportant"
        every { TagEncoding.normalize(tagValue) } returns normalized
        every { UrlEncoding.encode(normalized) } returns expected

        // When
        val (_, _, encoded) = compatibilityEncoder.encode(tagValue)

        // Then
        assertEquals(
            expected = expected,
            actual = encoded
        )

        verify(exactly = 1) { TagEncoding.normalize(tagValue) }
        verify(exactly = 1) { UrlEncoding.encode(normalized) }
    }

    @Test
    fun `Given encode is called with a String, it maps JS legacy encoding lowercases`() {
        // Given
        val tagValue = "!'()*-_.~"
        val expected = "%21%27%28%29%2a%2d%5f%2e%7e"

        every { TagEncoding.encode(tagValue) } returns "notImportant"
        every { TagEncoding.normalize(tagValue) } returns tagValue
        every { UrlEncoding.encode(tagValue) } returns expected.toUpperCase()

        // When
        val (_, _, encoded) = compatibilityEncoder.encode(tagValue)

        // Then
        assertEquals(
            expected = expected,
            actual = encoded
        )

        verify(exactly = 1) { TagEncoding.normalize(tagValue) }
        verify(exactly = 1) { UrlEncoding.encode(tagValue) }
    }

    @Test
    fun `Given normalize is called with a String, it passes it through to the TagEncoder and returns its result`() {
        // Given
        val tagValue = "AAA"
        val expected = "BBB"

        every { TagEncoding.normalize(tagValue) } returns expected

        // When
        val normalized = compatibilityEncoder.normalize(tagValue)

        // Then
        assertEquals(
            expected = expected,
            actual = normalized
        )

        verify(exactly = 1) { TagEncoding.normalize(tagValue) }
    }
}
