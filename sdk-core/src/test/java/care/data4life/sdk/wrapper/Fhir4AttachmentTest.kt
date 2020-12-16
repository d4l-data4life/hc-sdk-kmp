/*
 * Copyright (c) 2020 D4L data4life gGmbH / All rights reserved.
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

import care.data4life.sdk.fhir.Fhir4Attachment
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class Fhir4AttachmentTest {
    @Test
    fun `it is a Attachment`() {
        val wrapper: Any = SdkFhir4Attachment(Fhir4Attachment())
        assertTrue(wrapper is WrapperContract.Attachment)
    }

    @Test
    fun `Given a wrapped Fhir4Attachment, it allows read access to id`() {
        // Given
        val id = "potato"
        val fhir4Attachment = Fhir4Attachment()
        fhir4Attachment.id  = id

        // When
        val result = SdkFhir4Attachment(fhir4Attachment).id

        // Then
        assertEquals(
                id,
                result
        )
    }

    @Test
    fun `Given a wrapped Fhir4Attachment, it allows write access to id`() {
        // Given
        val id = "potato"
        val fhir4Attachment = Fhir4Attachment()

        // When
        SdkFhir4Attachment(fhir4Attachment).id = id

        // Then
        assertEquals(
                id,
                fhir4Attachment.id
        )
    }

    @Test
    fun `Given a wrapped Fhir4Attachment, it allows read access to data`() {
        // Given
        val data = "soup"
        val fhir4Attachment = Fhir4Attachment()
        fhir4Attachment.data  = data

        // When
        val result = SdkFhir4Attachment(fhir4Attachment).data

        // Then
        assertEquals(
                data,
                result
        )
    }

    @Test
    fun `Given a wrapped Fhir4Attachment, it allows write access to data`() {
        // Given
        val data = "soup"
        val fhir4Attachment = Fhir4Attachment()

        // When
        SdkFhir4Attachment(fhir4Attachment).data = data

        // Then
        assertEquals(
                data,
                fhir4Attachment.data
        )
    }

    @Test
    fun `Given a wrapped Fhir4Attachment, it allows read access to hash`() {
        // Given
        val hash = "1234"
        val fhir4Attachment = Fhir4Attachment()
        fhir4Attachment.hash  = hash

        // When
        val result = SdkFhir4Attachment(fhir4Attachment).hash

        // Then
        assertEquals(
                hash,
                result
        )
    }

    @Test
    fun `Given a wrapped Fhir4Attachment, it allows write access to hash`() {
        // Given
        val hash = "1234"
        val fhir4Attachment = Fhir4Attachment()

        // When
        SdkFhir4Attachment(fhir4Attachment).hash = hash

        // Then
        assertEquals(
                hash,
                fhir4Attachment.hash
        )
    }

    @Test
    fun `Given a wrapped Fhir4Attachment, it allows read access to size`() {
        // Given
        val size = 42
        val fhir4Attachment = Fhir4Attachment()
        fhir4Attachment.size  = size

        // When
        val result = SdkFhir4Attachment(fhir4Attachment).size

        // Then
        assertEquals(
                size,
                result
        )
    }

    @Test
    fun `Given a wrapped Fhir4Attachment, it allows write access to size`() {
        // Given
        val size = 23
        val fhir4Attachment = Fhir4Attachment()

        // When
        SdkFhir4Attachment(fhir4Attachment).size = size

        // Then
        assertEquals(
                size,
                fhir4Attachment.size
        )
    }

    @Test
    fun `Given, unwrap is called, it returns the wrapped Attachment`() {
        val fhir4Attachment = Fhir4Attachment()

        // Then
        assertSame(
                fhir4Attachment,
                SdkFhir4Attachment(fhir4Attachment).unwrap()
        )
    }
}
