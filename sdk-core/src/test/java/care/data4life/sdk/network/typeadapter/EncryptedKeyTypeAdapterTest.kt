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

package care.data4life.sdk.network.typeadapter

import care.data4life.sdk.network.model.EncryptedKey
import kotlin.test.assertEquals
import kotlin.test.assertNull
import org.junit.Test

class EncryptedKeyTypeAdapterTest {
    @Test
    fun `Given toJson is called with a EncryptedKey, it returns the value of the base64Key`() {
        // Given
        val expected = "something"

        val key = EncryptedKey(expected)

        // When
        val actual = EncryptedKeyTypeAdapter().toJson(key)

        // Then
        assertEquals(
            actual = actual,
            expected = expected
        )
    }

    @Test
    fun `Given fromJson is called with null, it returns null`() {
        // When
        val actual = EncryptedKeyTypeAdapter().fromJson(null)

        // Then
        assertNull(actual)
    }

    @Test
    fun `Given fromJson is called with an empty String, it returns null`() {
        // When
        val actual = EncryptedKeyTypeAdapter().fromJson("")

        // Then
        assertNull(actual)
    }

    @Test
    fun `Given fromJson is called with an non empty String, it returns an EncryptedKey`() {
        // Given
        val expected = "something"

        // When
        val actual = EncryptedKeyTypeAdapter().fromJson(expected)

        // Then
        // Then
        assertEquals(
            actual = actual,
            expected = EncryptedKey(expected)
        )
    }
}
