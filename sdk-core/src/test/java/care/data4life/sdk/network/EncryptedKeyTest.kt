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

package care.data4life.sdk.network

import care.data4life.sdk.network.model.EncryptedKey
import care.data4life.sdk.network.model.NetworkModelContract
import care.data4life.sdk.util.Base64
import io.mockk.every
import io.mockk.mockkObject
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EncryptedKeyTest {

    @Test
    fun `it is a EncryptedKeyMaker`() {
        assertTrue((EncryptedKey as Any) is NetworkModelContract.EncryptedKeyMaker)
    }

    @Test
    fun `Given create is called, it creates a new EncryptedKey`() {
        val key = EncryptedKey.create("test".toByteArray())
        assertTrue((key as Any) is NetworkModelContract.EncryptedKey)
    }

    @Test
    fun `Given create is called, it encodes the given key`() {
        // Given
        val expected = "potato"
        val givenValue = "test"

        mockkObject(Base64)
        every { Base64.encodeToString(givenValue.toByteArray()) } returns expected

        // When
        val key = EncryptedKey.create("test".toByteArray())

        // Then
        assertEquals(
                expected,
                key.base64Key
        )

        verify(exactly = 1) { Base64.encodeToString(givenValue.toByteArray()) }
    }

    @Test
    fun `Given decode is called, it decodes the given key`() {
        // Given
        val expected = "test".toByteArray()
        val storedValue = "potato"

        mockkObject(Base64)
        every { Base64.decode(storedValue) } returns expected

        // When
        val key = EncryptedKey(storedValue)

        // Then
        assertEquals(
                expected,
                key.decode()
        )
    }
}
