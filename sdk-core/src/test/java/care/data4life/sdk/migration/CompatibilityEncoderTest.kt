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

import care.data4life.sdk.tag.TaggingContract
import care.data4life.sdk.wrapper.WrapperContract
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CompatibilityEncoderTest {
    private val tagEncoding: TaggingContract.Encoding = mockk()
    private val urlEncoding: WrapperContract.URLEncoding = mockk()
    private lateinit var compatibilityEncoder: MigrationContract.CompatibilityEncoder

    @Before
    fun setUp() {
        clearAllMocks()
        compatibilityEncoder = CompatibilityEncoder(tagEncoding, urlEncoding)
    }

    @Test
    fun `It fulfils CompatibilityEncoder`() {
        val encoder: Any = CompatibilityEncoder(tagEncoding, urlEncoding)

        assertTrue(encoder is MigrationContract.CompatibilityEncoder)
    }

    @Test
    fun `Given encode is called with a String, it delegates the String to encode of the TagEncoder and returns its result as the actual valid encoding`() {
        // Given
        val tagValue = "I am a value"
        val expected = "Well done"

        every { tagEncoding.encode(tagValue) } returns expected
        every { tagEncoding.normalize(tagValue) } returns "notImportant"
        every { urlEncoding.encode(tagValue) } returns "notImportant"

        // When
        val (encoded, _, _) = compatibilityEncoder.encode(tagValue)

        // Then
        assertEquals(
            expected = expected,
            actual = encoded
        )

        verify(exactly = 1) { tagEncoding.encode(tagValue) }
    }

    @Test
    fun `Given encode is called with a String, it delegates the String to normalize of the TagEncoder and returns its result as Android legacy encoding`() {
        // Given
        val tagValue = "I am a value"
        val expected = "Well done"

        every { tagEncoding.encode(tagValue) } returns "notImportant"
        every { tagEncoding.normalize(tagValue) } returns expected
        every { urlEncoding.encode(tagValue) } returns "notImportant"

        // When
        val (_, encoded, _) = compatibilityEncoder.encode(tagValue)

        // Then
        assertEquals(
            expected = expected,
            actual = encoded
        )

        verify(exactly = 1) { tagEncoding.normalize(tagValue) }
    }

    @Test
    fun `Given encode is called with a String, it delegates the String to encode of the URLEncoder and returns its result as JS legacy encoding`() {
        // Given
        val tagValue = "I am a value"
        val expected = "Well done"

        every { tagEncoding.encode(tagValue) } returns "notImportant"
        every { tagEncoding.normalize(tagValue) } returns "notImportant"
        every { urlEncoding.encode(tagValue) } returns expected

        // When
        val (_, _, encoded) = compatibilityEncoder.encode(tagValue)

        // Then
        assertEquals(
            expected = expected,
            actual = encoded
        )

        verify(exactly = 1) { urlEncoding.encode(tagValue) }
    }

    @Test
    fun `Given encode is called with a String, it maps JS legacy encoding lowercases`() {
        // Given
        val tagValue = "!'()*-_.~"
        val expected = "%21%27%28%29%2a%2d%5f%2e%7e"

        every { tagEncoding.encode(tagValue) } returns "notImportant"
        every { tagEncoding.normalize(tagValue) } returns "notImportant"
        every { urlEncoding.encode(tagValue) } returns expected.toUpperCase()

        // When
        val (_, _, encoded) = compatibilityEncoder.encode(tagValue)

        // Then
        assertEquals(
            expected = expected,
            actual = encoded
        )

        verify(exactly = 1) { urlEncoding.encode(tagValue) }
    }
}
