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

package care.data4life.sdk.wrapper

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class UrlEncodingTest {
    @Test
    fun `It fulfils URLEncoding`() {
        val encoding: Any = UrlEncoding

        assertTrue(encoding is WrapperContract.UrlEncoding)
    }

    @Test
    fun `Given encode is called with a String, it ignores ASCII characters`() {
        // Given
        val str = "ABCabc"

        // When
        val encoded = UrlEncoding.encode(str)

        // Then
        assertEquals(
            expected = str,
            actual = encoded
        )
    }

    @Test
    fun `Given, encode is called with a String, it encodes it`() {
        // Given
        val str = "你好，世界"

        // When
        val encoded = UrlEncoding.encode(str)

        // Then
        assertEquals(
            expected = "%E4%BD%A0%E5%A5%BD%EF%BC%8C%E4%B8%96%E7%95%8C",
            actual = encoded
        )
    }

    @Test
    fun `Given, encode is called with a String, which contains special chars, it encodes it`() {
        // Given
        val str = "! '()*-_.~"

        // When
        val encoded = UrlEncoding.encode(str)

        // Then
        assertEquals(
            expected = "%21%20%27%28%29%2A%2D%5F%2E%7E",
            actual = encoded
        )
    }

    @Test
    fun `Given, encode is called with a String, which contains mixed chars, it encodes it`() {
        // Given
        val str = "你好! world."

        // When
        val encoded = UrlEncoding.encode(str)

        // Then
        assertEquals(
            expected = "%E4%BD%A0%E5%A5%BD%21%20world%2E",
            actual = encoded
        )
    }

    @Test
    fun `Given, decode is called with a encoded String in lowercase, it decodes it`() {
        // Given
        val encodedTag = "%e4%bd%a0%e5%a5%bd%ef%bc%8c%e4%b8%96%e7%95%8c"

        // When
        val decoded = UrlEncoding.decode(encodedTag)

        // Then
        assertEquals(
            expected = "你好，世界",
            actual = decoded
        )
    }

    @Test
    fun `Given, decode is called with a encoded String in uppercase, it decodes it`() {
        // Given
        val encodedTag = "%E4%BD%A0%E5%A5%BD%21%20world%2E"

        // When
        val decoded = UrlEncoding.decode(encodedTag)

        // Then
        assertEquals(
            expected = "你好! world.",
            actual = decoded
        )
    }
}
