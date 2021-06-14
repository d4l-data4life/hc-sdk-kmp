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

import care.data4life.sdk.lang.D4LException
import care.data4life.sdk.lang.DataValidationException
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class TagEncodingTest {
    @Test
    fun `It fulfils the Encoding`() {
        val encoding: Any = TagEncoding
        assertTrue(encoding is TaggingContract.Encoding)
    }

    @Test
    fun `Given, encode is called with a String, it fails, if is its blank`() {
        // Given
        val tag = " "

        // When
        val exception = assertFailsWith<D4LException> { TagEncoding.encode(tag) }

        assertTrue(exception is DataValidationException.AnnotationViolation)
        assertEquals(
            expected = "Annotation is empty.",
            actual = exception.message
        )
    }

    @Test
    fun `Given, encode is called with a String, it applies no encoding on alphanumeric chars`() {
        // Given
        val tag = "tag"

        // When
        val encoded = TagEncoding.encode(tag)

        // Then
        assertEquals(
            expected = tag,
            actual = encoded
        )
    }

    @Test
    fun `Given, encode is called with a String, it trims it`() {
        // Given
        val expected = "tag"

        // When
        val encoded = TagEncoding.encode("  $expected   ")

        // Then
        assertEquals(
            expected = expected,
            actual = encoded
        )
    }

    @Test
    fun `Given, encode is called with a String, it brings it into lowercase`() {
        // Given
        val expected = "TAG"

        // When
        val encoded = TagEncoding.encode(expected)

        // Then
        assertEquals(
            expected = "tag",
            actual = encoded
        )
    }

    @Test
    fun `Given, encode is called with a String, it encodes it`() {
        // Given
        val tag = "你好，世界"

        // When
        val encoded = TagEncoding.encode(tag)

        // Then
        assertEquals(
            expected = "%e4%bd%a0%e5%a5%bd%ef%bc%8c%e4%b8%96%e7%95%8c",
            actual = encoded
        )
    }

    @Test
    fun `Given, encode is called with a String, which contains special chars, it encodes it`() {
        // Given
        val tag = "! '()*-_.~"

        // When
        val encoded = TagEncoding.encode(tag)

        // Then
        assertEquals(
            expected = "%21%20%27%28%29%2a%2d%5f%2e%7e",
            actual = encoded
        )
    }

    @Test
    fun `Given, encode is called with a String, which contains mixed chars, it encodes it`() {
        // Given
        val tag = "你好! world."

        // When
        val encoded = TagEncoding.encode(tag)

        // Then
        assertEquals(
            expected = "%e4%bd%a0%e5%a5%bd%21%20world%2e",
            actual = encoded
        )
    }

    @Test
    fun `Given, decode is called with a encoded String, it decodes it`() {
        // Given
        val encodedTag = "%e4%bd%a0%e5%a5%bd%ef%bc%8c%e4%b8%96%e7%95%8c"

        // When
        val decoded = TagEncoding.decode(encodedTag)

        // Then
        assertEquals(
            expected = "你好，世界",
            actual = decoded
        )
    }

    @Test
    fun `Given, decode is called with a encoded String, which contains special chars, it decodes it`() {
        // Given
        val encodedTag = "%21%27%28%29%2a%2d%5f%2e%7e%20"

        // When
        val decoded = TagEncoding.decode(encodedTag)

        // Then
        assertEquals(
            expected = "!'()*-_.~ ",
            actual = decoded
        )
    }

    @Test
    fun `Given, decode is called with a encoded String, which contains mixed chars, it decodes it`() {
        // Given
        val encodedTag = "%e4%bd%a0%e5%a5%bd%21%20world%2e"

        // When
        val decoded = TagEncoding.decode(encodedTag)

        // Then
        assertEquals(
            expected = "你好! world.",
            actual = decoded
        )
    }

    @Test
    fun `Given, normalize is called with a String, it fails, if is its blank`() {
        // Given
        val tag = " "

        // When
        val exception = assertFailsWith<D4LException> { TagEncoding.normalize(tag) }

        // Then
        assertTrue(exception is DataValidationException.AnnotationViolation)
        assertEquals(
            expected = exception.message,
            actual = "Annotation is empty."
        )
    }

    @Test
    fun `Given, normalize is called with a String, it applies no encoding on alphanumeric chars`() {
        // Given
        val tag = "tag"

        // When
        val normalized = TagEncoding.normalize(tag)

        // Then
        assertEquals(
            expected = tag,
            actual = normalized
        )
    }

    @Test
    fun `Given, normalize is called with a String, it trims it`() {
        // Given
        val expected = "tag"

        // When
        val normalized = TagEncoding.normalize("  $expected   ")

        // Then
        assertEquals(
            expected = expected,
            actual = normalized
        )
    }

    @Test
    fun `Given, normalize is called with a String, it brings it into lowercase`() {
        // Given
        val expected = "TAG"

        // When
        val normalized = TagEncoding.normalize(expected)

        // Then
        assertEquals(
            expected = "tag",
            actual = normalized
        )
    }
}
