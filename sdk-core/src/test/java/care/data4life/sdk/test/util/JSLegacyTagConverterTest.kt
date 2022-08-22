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

package care.data4life.sdk.test.util

import kotlin.test.assertEquals
import org.junit.Test

class JSLegacyTagConverterTest {
    @Test
    fun `Given, convertTag is called, with encoded String, which contains no special chars, it reflects the String`() {
        // Given
        val payload = "testString"

        // When
        val actual = JSLegacyTagConverter.convertTag(payload)

        // Then
        assertEquals(
            expected = payload,
            actual = actual
        )
    }

    @Test
    fun `Given, convertTag is called, with encoded String, which contains encoded chars and they are in lowercase and they had been manually implemented in JS, it reflects them`() {
        // Given
        val payload = "%2d%5ftest%2a%2eString%7e"

        // When
        val actual = JSLegacyTagConverter.convertTag(payload)

        // Then
        assertEquals(
            expected = payload,
            actual = actual
        )
    }

    @Test
    fun `Given, convertTag is called, with encoded String, which contains encoded chars and they are in lowercase and they had been not manually implemented in JS, it returns them in uppercase`() {
        // Given
        val expected = "js%2C%20%D0%B2%D0%B5%D1%80%D0%BE%D1%8F%D1%82%D0%BD%D0%BE%2C%20%D0%BD%D0%B5%20%D0%BE%D0%B1%D1%80%D0%B0%D1%89%D0%B0%D0%BB%20%D0%B2%D0%BD%D0%B8%D0%BC%D0%B0%D0%BD%D0%B8%D1%8F"
        val payload = "js%2c%20%d0%b2%d0%b5%d1%80%d0%be%d1%8f%d1%82%d0%bd%d0%be%2c%20%d0%bd%d0%b5%20%d0%be%d0%b1%d1%80%d0%b0%d1%89%d0%b0%d0%bb%20%d0%b2%d0%bd%d0%b8%d0%bc%d0%b0%d0%bd%d0%b8%d1%8f"

        // When
        val actual = JSLegacyTagConverter.convertTag(payload)

        // Then
        assertEquals(
            expected = expected,
            actual = actual
        )
    }

    @Test
    fun `Given, convertTag is called, with encoded String, which contains mixed encodings, it converts them properly`() {
        // Given
        val expected = "%2d%5ftest%2a%2eString%7ejs%2C%20%D0%B2%D0%B5%D1%80%D0%BE%D1%8F%D1%82%D0%BD%D0%BE%2C%2d%5ftest%2a%2eString%7e%20%D0%BD%D0%B5%20%D0%BE%D0%B1%D1%80%D0%B0%D1%89%D0%B0%D0%BB%20%D0%B2%D0%BD%D0%B8%D0%BC%D0%B0%D0%BD%D0%B8%D1%8F%2d%5ftest%2a%2eString%7e"
        val payload = "%2d%5ftest%2a%2eString%7ejs%2c%20%d0%b2%d0%b5%d1%80%d0%be%d1%8f%d1%82%d0%bd%d0%be%2c%2d%5ftest%2a%2eString%7e%20%d0%bd%d0%b5%20%d0%be%d0%b1%d1%80%d0%b0%d1%89%d0%b0%d0%bb%20%d0%b2%d0%bd%d0%b8%d0%bc%d0%b0%d0%bd%d0%b8%d1%8f%2d%5ftest%2a%2eString%7e"

        // When
        val actual = JSLegacyTagConverter.convertTag(payload)

        // Then
        assertEquals(
            expected = expected,
            actual = actual
        )
    }
}
