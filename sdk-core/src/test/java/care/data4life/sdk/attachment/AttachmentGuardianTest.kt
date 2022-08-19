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
import care.data4life.sdk.wrapper.WrapperContract
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import io.mockk.verify
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.Test

class AttachmentGuardianTest {
    @Test
    fun `It fulfils AttachmentGuardian`() {
        val guard: Any = AttachmentGuardian

        assertTrue(guard is AttachmentContract.Guardian)
    }

    @Test
    fun `Given, guardId is called with an Attachment, it fails with a DataValidationException#IdUsageViolation, if the Attachment contains a id`() {
        // Given
        val attachment: WrapperContract.Attachment = mockk()

        every { attachment.id } returns "id"

        // Then
        val error = assertFailsWith<DataValidationException.IdUsageViolation> {
            // When
            AttachmentGuardian.guardId(attachment)
        }

        assertEquals(
            actual = error.message,
            expected = "Attachment.id should be null"
        )
        verify(exactly = 1) { attachment.id }
    }

    @Test
    fun `Given, guardId is called with an Attachment, it accepts, if the AttachmentId is null`() {
        // Given
        val attachment: WrapperContract.Attachment = mockk()

        every { attachment.id } returns null

        // When
        AttachmentGuardian.guardId(attachment)

        // Then
        verify(exactly = 1) { attachment.id }
    }

    @Test
    fun `Given, guardNonId is called with an Attachment, it fails with a DataValidationException#IdUsageViolation, if the AttachmentId is null`() {
        // Given
        val attachment: WrapperContract.Attachment = mockk()

        every { attachment.id } returns null

        // Then
        val error = assertFailsWith<DataValidationException.IdUsageViolation> {
            // When
            AttachmentGuardian.guardNonNullId(attachment)
        }

        assertEquals(
            actual = error.message,
            expected = "Attachment.id expected"
        )
        verify(exactly = 1) { attachment.id }
    }

    @Test
    fun `Given, guardNonId is called with an Attachment, it accepts, if the AttachmentId is not null`() {
        // Given
        val attachment: WrapperContract.Attachment = mockk()

        every { attachment.id } returns "non null"

        // When
        AttachmentGuardian.guardNonNullId(attachment)

        // Then
        verify(exactly = 1) { attachment.id }
    }

    @Test
    fun `Given, guardIdAgainstExistingIds is called with an Attachment and a Set of ReferenceIds, it fails with a DataValidationException#IdUsageViolation, if the Attachment is not reflected by the ReferenceIds`() {
        // Given
        val attachment: WrapperContract.Attachment = mockk()
        val knownAttachmentIds = setOf("1", "2", "3")

        every { attachment.id } returns "id"

        // Then
        val error = assertFailsWith<DataValidationException.IdUsageViolation> {
            // When
            AttachmentGuardian.guardIdAgainstExistingIds(attachment, knownAttachmentIds)
        }

        assertEquals(
            actual = error.message,
            expected = "Valid Attachment.id expected"
        )
        verify(exactly = 1) { attachment.id }
    }

    @Test
    fun `Given, guardIdAgainstExistingIds is called with an Attachment and a Set of ReferenceIds, it accepts, if the Attachment is reflected by the ReferenceIds`() {
        // Given
        val attachment: WrapperContract.Attachment = mockk()
        val knownAttachmentIds = setOf("1", "id", "3")

        every { attachment.id } returns "id"

        // When
        AttachmentGuardian.guardIdAgainstExistingIds(attachment, knownAttachmentIds)

        // Then
        verify(exactly = 1) { attachment.id }
    }

    @Test
    fun `Given, guardHash is called with an Attachment, it fails with a DataValidationException#ExpectedFieldViolation, if the Attachment has no hash`() {
        // Given
        val attachment: WrapperContract.Attachment = mockk()

        every { attachment.hash } returns null

        // Then
        val error = assertFailsWith<DataValidationException.ExpectedFieldViolation> {
            // When
            AttachmentGuardian.guardHash(attachment)
        }

        assertEquals(
            actual = error.message,
            expected = "Attachment.hash expected"
        )
        verify(exactly = 0) { attachment.data }
        verify(exactly = 1) { attachment.hash }
    }

    @Test
    fun `Given, guardHash is called with an Attachment, it fails with a DataValidationException#ExpectedFieldViolation, if Attachment hash no data`() {
        // Given
        val attachment: WrapperContract.Attachment = mockk()

        every { attachment.hash } returns "bla"
        every { attachment.data } returns null

        // Then
        val error = assertFailsWith<DataValidationException.ExpectedFieldViolation> {
            // When
            AttachmentGuardian.guardHash(attachment)
        }

        assertEquals(
            actual = error.message,
            expected = "Attachment.data expected"
        )
        verify(exactly = 1) { attachment.data }
        verify(exactly = 1) { attachment.hash }
    }

    @Test
    fun `Given, guardHash is called with an Attachment, it fails with a DataValidationException#InvalidAttachmentPayloadHash, if the calculated hash does not match the actual AttachmentHash`() {
        // Given
        mockkObject(AttachmentHasher)

        val attachment: WrapperContract.Attachment = mockk()

        every { attachment.data } returns "dGVzdA=="
        every { attachment.hash } returns "123456789"
        every { AttachmentHasher.hash("test".toByteArray()) } returns "NotValid"

        // Then
        val error = assertFailsWith<DataValidationException.InvalidAttachmentPayloadHash> {
            // When
            AttachmentGuardian.guardHash(attachment)
        }

        assertEquals(
            actual = error.message,
            expected = "Attachment.hash is not valid"
        )
        verify(exactly = 1) { attachment.data }
        verify(exactly = 1) { attachment.hash }
        verify(exactly = 1) { AttachmentHasher.hash("test".toByteArray()) }

        unmockkObject(AttachmentHasher)
    }

    @Test
    fun `Given, guardHash is called with an Attachment, it returns true, if AttachmentHash and AttachmentData is present and the calculated hash matches the actual AttachmentHash`() {
        // Given
        mockkObject(AttachmentHasher)

        val attachment: WrapperContract.Attachment = mockk()
        val hash = "pass"

        every { attachment.data } returns "dGVzdA=="
        every { attachment.hash } returns hash
        every { AttachmentHasher.hash("test".toByteArray()) } returns hash

        // When
        val actual = AttachmentGuardian.guardHash(attachment)

        // Then
        assertTrue(actual)
        verify(exactly = 1) { attachment.data }
        verify(exactly = 1) { attachment.hash }
        verify(exactly = 1) { AttachmentHasher.hash("test".toByteArray()) }

        unmockkObject(AttachmentHasher)
    }

    @Test
    fun `Given, guardHash is called with an Attachment and a ReferenceAttachment, it fails with a DataValidationException#ExpectedFieldViolation, if the Attachment has no hash`() {
        // Given
        val attachment: WrapperContract.Attachment = mockk()
        val reference: WrapperContract.Attachment = mockk()

        every { attachment.hash } returns null

        // Then
        val error = assertFailsWith<DataValidationException.ExpectedFieldViolation> {
            // When
            AttachmentGuardian.guardHash(attachment, reference)
        }

        assertEquals(
            actual = error.message,
            expected = "Attachment.hash expected"
        )
        verify(exactly = 1) { attachment.hash }
    }

    @Test
    fun `Given, guardHash is called with an Attachment and a ReferenceAttachment, it returns true, if ReferenceAttachment has no AttachmentHash`() {
        // Given
        val attachment: WrapperContract.Attachment = mockk()
        val reference: WrapperContract.Attachment = mockk()

        every { attachment.hash } returns "bla"
        every { reference.hash } returns null

        // When
        val actual = AttachmentGuardian.guardHash(attachment, reference)

        // Then
        assertTrue(actual)
        verify(exactly = 1) { attachment.hash }
    }

    @Test
    fun `Given, guardHash is called with an Attachment and a ReferenceAttachment, it returns true, if ReferenceAttachmentHash does not match the AttachmentHash`() {
        // Given
        val attachment: WrapperContract.Attachment = mockk()
        val reference: WrapperContract.Attachment = mockk()

        every { attachment.hash } returns "bla"
        every { reference.hash } returns "blup"

        // When
        val actual = AttachmentGuardian.guardHash(attachment, reference)

        // Then
        assertTrue(actual)
        verify(exactly = 1) { attachment.hash }
    }

    @Test
    fun `Given, guardHash is called with an Attachment and a ReferenceAttachment, it returns false, if ReferenceAttachmentHash matches the AttachmentHash`() {
        // Given
        val attachment: WrapperContract.Attachment = mockk()
        val reference: WrapperContract.Attachment = mockk()
        val hash = "hash"

        every { attachment.hash } returns hash
        every { reference.hash } returns hash

        // When
        val actual = AttachmentGuardian.guardHash(attachment, reference)

        // Then
        assertFalse(actual)
        verify(exactly = 1) { attachment.hash }
    }

    @Test
    fun `Given, guardSize is called with an Attachment, it fails, if Attachment has no size`() {
        // Given
        val attachment: WrapperContract.Attachment = mockk()

        every { attachment.size } returns null

        // Then
        val error = assertFailsWith<DataValidationException.ExpectedFieldViolation> {
            // When
            AttachmentGuardian.guardSize(attachment)
        }

        assertEquals(
            actual = error.message,
            expected = "Attachment.size expected"
        )
        verify(exactly = 1) { attachment.size }
    }

    @Test
    fun `Given, guardSize is called with an Attachment, it accepts, if Attachment has a size`() {
        // Given
        val attachment: WrapperContract.Attachment = mockk()

        every { attachment.size } returns 42

        // When
        AttachmentGuardian.guardSize(attachment)

        // Then
        verify(exactly = 1) { attachment.size }
    }
}
