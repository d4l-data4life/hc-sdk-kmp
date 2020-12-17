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

import care.data4life.fhir.stu3.model.Attachment as Fhir3Attachment
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class Fhir3AttachmentTest {
    @Test
    fun `it is a Attachment`() {
        val wrapper: Any = SdkFhir3Attachment(Fhir3Attachment())
        assertTrue(wrapper is WrapperContract.Attachment)
    }

    @Test
    fun `Given a wrapped Fhir3Attachment, it allows read access to id`() {
        // Given
        val id = "potato"
        val fhir3Attachment = Fhir3Attachment()
        fhir3Attachment.id  = id

        // When
        val result = SdkFhir3Attachment(fhir3Attachment).id

        // Then
        assertEquals(
                id,
                result
        )
    }

    @Test
    fun `Given a wrapped Fhir3Attachment, it allows write access to id`() {
        // Given
        val id = "potato"
        val fhir3Attachment = Fhir3Attachment()

        // When
        SdkFhir3Attachment(fhir3Attachment).id = id

        // Then
        assertEquals(
                id,
                fhir3Attachment.id
        )
    }

    @Test
    fun `Given a wrapped Fhir3Attachment, it allows read access to data`() {
        // Given
        val data = "soup"
        val fhir3Attachment = Fhir3Attachment()
        fhir3Attachment.data  = data

        // When
        val result = SdkFhir3Attachment(fhir3Attachment).data

        // Then
        assertEquals(
                data,
                result
        )
    }

    @Test
    fun `Given a wrapped Fhir3Attachment, it allows write access to data`() {
        // Given
        val data = "soup"
        val fhir3Attachment = Fhir3Attachment()

        // When
        SdkFhir3Attachment(fhir3Attachment).data = data

        // Then
        assertEquals(
                data,
                fhir3Attachment.data
        )
    }

    @Test
    fun `Given a wrapped Fhir3Attachment, it allows read access to hash`() {
        // Given
        val hash = "1234"
        val fhir3Attachment = Fhir3Attachment()
        fhir3Attachment.hash  = hash

        // When
        val result = SdkFhir3Attachment(fhir3Attachment).hash

        // Then
        assertEquals(
                hash,
                result
        )
    }

    @Test
    fun `Given a wrapped Fhir3Attachment, it allows write access to hash`() {
        // Given
        val hash = "1234"
        val fhir3Attachment = Fhir3Attachment()

        // When
        SdkFhir3Attachment(fhir3Attachment).hash = hash

        // Then
        assertEquals(
                hash,
                fhir3Attachment.hash
        )
    }

    @Test
    fun `Given a wrapped Fhir3Attachment, it allows read access to size`() {
        // Given
        val size = 42
        val fhir3Attachment = Fhir3Attachment()
        fhir3Attachment.size  = size

        // When
        val result = SdkFhir3Attachment(fhir3Attachment).size

        // Then
        assertEquals(
                size,
                result
        )
    }

    @Test
    fun `Given a wrapped Fhir3Attachment, it allows write access to size`() {
        // Given
        val size = 23
        val fhir3Attachment = Fhir3Attachment()

        // When
        SdkFhir3Attachment(fhir3Attachment).size = size

        // Then
        assertEquals(
                size,
                fhir3Attachment.size
        )
    }

    @Test
    fun `Given, unwrap is called, it returns the wrapped Fhir3Attachment`() {
        // Given
        val fhir3Attachment = Fhir3Attachment()

        // When
        assertSame(
                fhir3Attachment,
                SdkFhir3Attachment(fhir3Attachment).unwrap()
        )
    }

    @Test
    fun `Given, on a compare, it return true, if both wrapped resources are identical`() {
        // Given
        val fhir3Attachment = Fhir3Attachment()

        // When
        assertEquals(
                SdkFhir3Attachment(fhir3Attachment),
                SdkFhir3Attachment(fhir3Attachment)
        )
    }

    @Test
    fun `Given, hashCode() is called it returns the hash of the wrapped resource`() {
        // Given
        val fhir3Attachment = Fhir3Attachment()

        // When
        assertEquals(
                fhir3Attachment.hashCode(),
                SdkFhir3Attachment(fhir3Attachment).hashCode()
        )
    }
}
