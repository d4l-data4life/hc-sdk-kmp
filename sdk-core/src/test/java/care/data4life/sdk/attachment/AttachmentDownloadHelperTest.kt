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

package care.data4life.sdk.attachment

import care.data4life.sdk.lang.DataValidationException
import care.data4life.sdk.util.Base64
import care.data4life.sdk.wrapper.WrapperContract
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import io.mockk.verify
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame
import kotlin.test.assertTrue

class AttachmentDownloadHelperTest {
    @Test
    fun `It fulfils AttachmentDownloadHelper`() {
        val helper: Any = AttachmentDownloadHelper

        assertTrue(helper is AttachmentInternalContract.DownloadHelper)
    }

    @Test
    fun `Given deriveAttachmentId is called with an Attachment, it reflects the id of the Attachment, if it not contains an SPLIT_CHAR`() {
        // Given
        val expected = "id"
        val attachment: WrapperContract.Attachment = mockk()

        every { attachment.id } returns expected

        // When
        val actual = AttachmentDownloadHelper.deriveAttachmentId(attachment)

        // Then
        assertEquals(
            actual = actual,
            expected = expected
        )
    }

    @Test
    fun `Given deriveAttachmentId is called with an Attachment, it resolve the id of the Attachment, if it contains an SPLIT_CHAR`() {
        // Given
        val expected = "expectedId"
        val attachmentId = "id#$expected"
        val attachment: WrapperContract.Attachment = mockk()

        every { attachment.id } returns attachmentId

        // When
        val actual = AttachmentDownloadHelper.deriveAttachmentId(attachment)

        // Then
        assertEquals(
            actual = actual,
            expected = expected
        )
    }

    @Test
    fun `Given addAttachmentPayload is called with an Attachment and the Data, it fails, if the Attachment is not a Thumbnail and the AttachmentData is hashable and the Hash matches not the Payload`() {
        // Given
        mockkObject(CompatibilityValidator)
        mockkObject(AttachmentHasher)

        val id = "id"
        val oldHash = "oldHash"
        val currentHash = "hash"

        val payload = ByteArray(23)
        val attachment: WrapperContract.Attachment = mockk()

        every { attachment.id } returns id
        every { attachment.hash } returns oldHash

        every { attachment.data = any() } just Runs
        every { attachment.hash = any() } just Runs

        every { CompatibilityValidator.isHashable(attachment) } returns true
        every { AttachmentHasher.hash(payload) } returns currentHash

        // Then
        val error = assertFailsWith<DataValidationException.InvalidAttachmentPayloadHash> {
            // When
            AttachmentDownloadHelper.addAttachmentPayload(attachment, payload)
        }

        assertEquals(
            actual = error.message,
            expected = "Attachment hash is invalid"
        )

        verify(exactly = 0) { attachment.data = any() }
        verify(exactly = 0) { attachment.hash = any() }

        unmockkObject(CompatibilityValidator)
        unmockkObject(AttachmentHasher)
    }

    @Test
    fun `Given addAttachmentPayload is called with an Attachment and the Data, it returns the modified Attachment, if the Attachment is not a Thumbnail and the AttachmentData is hashable and the Hash matches the Payload`() {
        // Given
        mockkObject(CompatibilityValidator)
        mockkObject(AttachmentHasher)

        val id = "id"
        val currentHash = "hash"

        val payload = ByteArray(23)
        val attachment: WrapperContract.Attachment = mockk()

        every { attachment.id } returns id
        every { attachment.hash } returns currentHash

        every { attachment.data = Base64.encodeToString(payload) } just Runs
        every { attachment.hash = currentHash } just Runs

        every { CompatibilityValidator.isHashable(attachment) } returns true
        every { AttachmentHasher.hash(payload) } returns currentHash

        // When
        val modifiedAttachment = AttachmentDownloadHelper.addAttachmentPayload(attachment, payload)

        // Then
        assertSame(
            actual = modifiedAttachment,
            expected = attachment
        )

        verify(exactly = 1) { attachment.data = Base64.encodeToString(payload) }
        verify(exactly = 1) { attachment.hash = currentHash }

        unmockkObject(CompatibilityValidator)
        unmockkObject(AttachmentHasher)
    }

    @Test
    fun `Given addAttachmentPayload is called with an Attachment and the Data, it returns the modified Attachment, if the Attachment is not a Thumbnail and the AttachmentData is not hashable`() {
        // Given
        mockkObject(CompatibilityValidator)
        mockkObject(AttachmentHasher)

        val id = "id"
        val currentHash = "hash"

        val payload = ByteArray(23)
        val attachment: WrapperContract.Attachment = mockk()

        every { attachment.id } returns id
        every { attachment.hash } returns currentHash

        every { attachment.data = Base64.encodeToString(payload) } just Runs
        every { attachment.hash = currentHash } just Runs

        every { CompatibilityValidator.isHashable(attachment) } returns false
        every { AttachmentHasher.hash(payload) } returns currentHash

        // When
        val modifiedAttachment = AttachmentDownloadHelper.addAttachmentPayload(attachment, payload)

        // Then
        assertSame(
            actual = modifiedAttachment,
            expected = attachment
        )

        verify(exactly = 1) { attachment.data = Base64.encodeToString(payload) }
        verify(exactly = 0) { attachment.hash }
        verify(exactly = 1) { attachment.hash = currentHash }

        unmockkObject(CompatibilityValidator)
        unmockkObject(AttachmentHasher)
    }

    @Test
    fun `Given addAttachmentPayload is called with an Attachment and the Data, it returns the modified Attachment, if the Attachment is a Thumbnail`() {
        // Given
        mockkObject(CompatibilityValidator)
        mockkObject(AttachmentHasher)

        val id = "id#preview"
        val currentHash = "hash"

        val payload = ByteArray(23)
        val attachment: WrapperContract.Attachment = mockk()

        every { attachment.id } returns id
        every { attachment.hash } returns currentHash

        every { attachment.data = Base64.encodeToString(payload) } just Runs
        every { attachment.hash = currentHash } just Runs

        every { AttachmentHasher.hash(payload) } returns currentHash

        // When
        val modifiedAttachment = AttachmentDownloadHelper.addAttachmentPayload(attachment, payload)

        // Then
        assertSame(
            actual = modifiedAttachment,
            expected = attachment
        )

        verify(exactly = 0) { CompatibilityValidator.isHashable(attachment) }
        verify(exactly = 1) { attachment.data = Base64.encodeToString(payload) }
        verify(exactly = 0) { attachment.hash }
        verify(exactly = 1) { attachment.hash = currentHash }

        unmockkObject(CompatibilityValidator)
        unmockkObject(AttachmentHasher)
    }
}
