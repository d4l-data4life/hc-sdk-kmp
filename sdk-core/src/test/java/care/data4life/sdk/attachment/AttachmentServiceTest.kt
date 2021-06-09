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

package care.data4life.sdk.attachment

import care.data4life.crypto.GCKey
import care.data4life.sdk.lang.ImageResizeException
import care.data4life.sdk.log.Log
import care.data4life.sdk.test.util.GenericTestDataProvider.ATTACHMENT_ID
import care.data4life.sdk.test.util.GenericTestDataProvider.USER_ID
import care.data4life.sdk.util.Base64.encodeToString
import care.data4life.sdk.wrapper.WrapperContract
import io.mockk.Runs
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import io.mockk.verify
import io.reactivex.Single
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class AttachmentServiceTest {
    private val fileService: AttachmentContract.FileService = mockk()
    private val resizer: AttachmentContract.ImageResizer = mockk()
    private lateinit var service: AttachmentContract.Service

    @Before
    fun setUp() {
        clearAllMocks()

        service = AttachmentService(fileService, resizer)
    }

    @Test
    fun `It fulfils AttachmentService`() {
        val service: Any = AttachmentService(mockk(), mockk())

        assertTrue(service is AttachmentContract.Service)
    }

    @Test
    fun `Given delete is called with a AttachmentId and UserId it delegates the call to the FileService and returns its result`() {
        // Given
        val attachmentId = ATTACHMENT_ID
        val userId = USER_ID

        val expected = Single.just(true)

        every { fileService.deleteFile(userId, attachmentId) } returns expected

        // When
        val actual = service.delete(attachmentId, userId)

        // Then
        assertSame(
            actual = actual,
            expected = expected
        )
    }

    @Test
    fun `Given download is called with a List of Attachments, the AttachmentKey and the UserId, filters out Attachments, which have no Id`() {
        // Given
        val attachment1: WrapperContract.Attachment = mockk()
        val attachment2: WrapperContract.Attachment = mockk()
        val attachmentKey: GCKey = mockk()

        every { attachment1.id } returns null
        every { attachment2.id } returns null

        // When
        val downloaded = service.download(
            listOf(attachment1, attachment2),
            attachmentKey,
            USER_ID
        ).blockingGet()

        // Then
        assertEquals(
            actual = downloaded.size,
            expected = 0
        )
    }

    @Test
    fun `Given download is called with a List of Attachments, the AttachmentKey and the UserId, it fails if the Attachment contains not a preview and it is hashable and the new and old hash do not match`() {
        // Given
        mockkObject(CompatibilityValidator)
        mockkObject(AttachmentHasher)

        val userID = USER_ID
        val attachment: WrapperContract.Attachment = mockk()
        val attachmentKey: GCKey = mockk()
        val downloadedAttachment = ByteArray(23)

        every { attachment.id } returns ATTACHMENT_ID
        every { attachment.hash } returns "not a match"

        every {
            fileService.downloadFile(attachmentKey, userID, ATTACHMENT_ID)
        } returns Single.just(downloadedAttachment)
        every { AttachmentHasher.hash(downloadedAttachment) } returns "hash"
        every { CompatibilityValidator.isHashable(attachment) } returns true

        // Then
        val error = assertFailsWith<RuntimeException> {
            // When
            service.download(listOf(attachment), attachmentKey, userID).blockingGet()
        }

        assertEquals(
            actual = error.message,
            expected = "care.data4life.sdk.lang.DataValidationException\$InvalidAttachmentPayloadHash: Attachment hash is invalid"
        )

        unmockkObject(CompatibilityValidator)
        unmockkObject(AttachmentHasher)
    }

    @Test
    fun `Given download  is called with a List of Attachments, the AttachmentKey and the UserId, it returns a list of Attachments with data if the Attachment do not contain a preview and are hashable and the hashes`() {
        // Given
        mockkObject(CompatibilityValidator)
        mockkObject(AttachmentHasher)

        val userID = USER_ID
        val hash = "hash"
        val attachment: WrapperContract.Attachment = mockk()
        val attachmentKey: GCKey = mockk()
        val downloadedAttachment = ByteArray(23)
        val decodedAttachment = encodeToString(downloadedAttachment)

        every { attachment.id } returns ATTACHMENT_ID
        every { attachment.hash } returns hash

        every { attachment.data = decodedAttachment } just Runs
        every { attachment.hash = hash } just Runs

        every {
            fileService.downloadFile(attachmentKey, userID, ATTACHMENT_ID)
        } returns Single.just(downloadedAttachment)
        every { CompatibilityValidator.isHashable(attachment) } returns true
        every { AttachmentHasher.hash(downloadedAttachment) } returns hash

        // When
        val downloadedAttachments = service.download(
            listOf(attachment),
            attachmentKey,
            userID
        ).blockingGet()

        // Then
        assertEquals(
            expected = 1,
            actual = downloadedAttachments.size
        )

        verify(exactly = 1) { attachment.data = decodedAttachment }
        verify(exactly = 1) { attachment.hash = hash }

        unmockkObject(CompatibilityValidator)
        unmockkObject(AttachmentHasher)
    }

    @Test
    fun `Given download  is called with a List of Attachments, the AttachmentKey and the UserId, it returns a list of Attachments with data if the Attachment do not contain a preview and are not hashable`() {
        // Given
        mockkObject(CompatibilityValidator)
        mockkObject(AttachmentHasher)

        val userID = USER_ID
        val hash = "hash"
        val attachment: WrapperContract.Attachment = mockk()
        val attachmentKey: GCKey = mockk()
        val downloadedAttachment = ByteArray(23)
        val decodedAttachment = encodeToString(downloadedAttachment)

        every { attachment.id } returns ATTACHMENT_ID
        every { attachment.hash } returns "not important"

        every { attachment.data = decodedAttachment } just Runs
        every { attachment.hash = hash } just Runs

        every {
            fileService.downloadFile(attachmentKey, userID, ATTACHMENT_ID)
        } returns Single.just(downloadedAttachment)
        every { CompatibilityValidator.isHashable(attachment) } returns false
        every { AttachmentHasher.hash(downloadedAttachment) } returns hash

        // When
        val downloadedAttachments = service.download(
            listOf(attachment),
            attachmentKey,
            userID
        ).blockingGet()

        // Then
        assertEquals(
            expected = 1,
            actual = downloadedAttachments.size
        )

        verify(exactly = 1) { attachment.data = decodedAttachment }
        verify(exactly = 1) { attachment.hash = hash }

        unmockkObject(CompatibilityValidator)
        unmockkObject(AttachmentHasher)
    }

    @Test
    fun `Given download  is called with a List of Attachments, the AttachmentKey and the UserId, it returns a list of Attachments with data if the Attachment contains a preview`() {
        // Given
        mockkObject(CompatibilityValidator)
        mockkObject(AttachmentHasher)

        val userID = USER_ID
        val hash = "hash"
        val attachment: WrapperContract.Attachment = mockk()
        val attachmentKey: GCKey = mockk()
        val downloadedAttachment = ByteArray(23)
        val decodedAttachment = encodeToString(downloadedAttachment)

        every { attachment.id } returns "not important#$ATTACHMENT_ID"
        every { attachment.hash } returns "not important"

        every { attachment.data = decodedAttachment } just Runs
        every { attachment.hash = hash } just Runs

        every {
            fileService.downloadFile(attachmentKey, userID, ATTACHMENT_ID)
        } returns Single.just(downloadedAttachment)
        every { AttachmentHasher.hash(downloadedAttachment) } returns hash

        // When
        val downloadedAttachments = service.download(
            listOf(attachment),
            attachmentKey,
            userID
        ).blockingGet()

        // Then
        assertEquals(
            expected = 1,
            actual = downloadedAttachments.size
        )

        verify(exactly = 1) { attachment.data = decodedAttachment }
        verify(exactly = 1) { attachment.hash = hash }

        unmockkObject(CompatibilityValidator)
        unmockkObject(AttachmentHasher)
    }

    @Test
    fun `Given upload is called with a list of Attachments, which contain data, the AttachmentKey and the UserId, it filters attachments which have actually no data`() {
        // Given
        val attachment1: WrapperContract.Attachment = mockk()
        val attachment2: WrapperContract.Attachment = mockk()
        val attachmentKey: GCKey = mockk()

        every { attachment1.data } returns null
        every { attachment2.data } returns null

        // When
        val uploaded = service.upload(
            listOf(attachment1, attachment2),
            attachmentKey,
            USER_ID
        ).blockingGet()

        // Then
        assertEquals(
            actual = uploaded.size,
            expected = 0
        )

        verify(exactly = 0) { fileService.uploadFile(any(), any(), any()) }
    }

    @Test
    fun `Given upload is called with a list of Attachments, which contain data, the AttachmentKey and the UserId, it returns the uploaded attachments and no AdditionalIds if the Attachment is not resizeable`() {
        // Given
        val userId = USER_ID
        val receivedId = "newId"
        val attachment: WrapperContract.Attachment = mockk()
        val data = "test".toByteArray()
        val encodedData = encodeToString(data)
        val attachmentKey: GCKey = mockk()

        every { attachment.id = receivedId } just Runs

        every { attachment.data } returns encodedData
        every {
            fileService.uploadFile(attachmentKey, userId, data)
        } returns Single.just(receivedId)

        every { resizer.isResizable(data) } returns false

        // When
        val uploaded = service.upload(listOf(attachment), attachmentKey, userId).blockingGet()

        // Then
        assertEquals(
            actual = uploaded.size,
            expected = 1
        )

        val (modifiedAttachment, additionalIds) = uploaded[0]
        assertTrue(additionalIds is List<*>)
        assertEquals(
            actual = additionalIds.size,
            expected = 0
        )
        assertSame(
            actual = modifiedAttachment,
            expected = attachment
        )

        verify(exactly = 1) { attachment.id = receivedId }
        verify(exactly = 1) { fileService.uploadFile(any(), any(), any()) }
    }

    @Test
    fun `Given upload is called with a list of Attachments, which contain data, the AttachmentKey and the UserId, it returns the uploaded attachments and null for AdditionalIds if the Attachment is resizeable, but the resizing fails`() {
        // Given
        mockkObject(Log)

        val userId = USER_ID
        val receivedId = "newId"
        val attachment: WrapperContract.Attachment = mockk()
        val data = "test".toByteArray()
        val encodedData = encodeToString(data)
        val attachmentKey: GCKey = mockk()
        val error = ImageResizeException.JpegWriterMissing()

        every { attachment.id = receivedId } just Runs

        every { attachment.data } returns encodedData
        every {
            fileService.uploadFile(attachmentKey, userId, data)
        } returns Single.just(receivedId)

        every { resizer.isResizable(data) } returns true
        every { resizer.resizeToHeight(any(), any(), any()) } throws error

        // When
        val uploaded = service.upload(listOf(attachment), attachmentKey, userId).blockingGet()

        // Then
        assertEquals(
            actual = uploaded.size,
            expected = 1
        )

        val (modifiedAttachment, additionalIds) = uploaded[0]
        assertNull(additionalIds)
        assertSame(
            actual = modifiedAttachment,
            expected = attachment
        )

        verify(exactly = 1) { Log.error(error, error.message) }
        verify(exactly = 1) { attachment.id = receivedId }
        verify(exactly = 1) { fileService.uploadFile(any(), any(), any()) }

        unmockkObject(Log)
    }

    @Test
    fun `Given upload is called with a list of Attachments, which contain data, the AttachmentKey and the UserId, it returns the uploaded attachments and AdditionalIds if the Attachment is resizeable and the resizing succeeds`() {
        // Given
        val userId = USER_ID
        val receivedId = "newId"
        val attachment: WrapperContract.Attachment = mockk()
        val data = "test".toByteArray()
        val encodedData = encodeToString(data)
        val attachmentKey: GCKey = mockk()

        val preview = ByteArray(23)
        val thumbnail = ByteArray(42)

        val previewId = "prev"
        val thumbnailId = "thumb"

        every { attachment.id = receivedId } just Runs

        every { attachment.data } returns encodedData
        every {
            fileService.uploadFile(attachmentKey, userId, data)
        } returns Single.just(receivedId)

        every { resizer.isResizable(data) } returns true
        every {
            resizer.resizeToHeight(
                data,
                AttachmentContract.ImageResizer.DEFAULT_PREVIEW_SIZE_PX,
                AttachmentContract.ImageResizer.DEFAULT_JPEG_QUALITY_PERCENT
            )
        } returns preview

        every {
            fileService.uploadFile(attachmentKey, userId, preview)
        } returns Single.just(previewId)

        every {
            resizer.resizeToHeight(
                data,
                AttachmentContract.ImageResizer.DEFAULT_THUMBNAIL_SIZE_PX,
                AttachmentContract.ImageResizer.DEFAULT_JPEG_QUALITY_PERCENT
            )
        } returns thumbnail

        every {
            fileService.uploadFile(attachmentKey, userId, thumbnail)
        } returns Single.just(thumbnailId)

        // When
        val uploaded = service.upload(listOf(attachment), attachmentKey, userId).blockingGet()

        // Then
        assertEquals(
            actual = uploaded.size,
            expected = 1
        )

        val (modifiedAttachment, additionalIds) = uploaded[0]
        assertTrue(additionalIds is List<*>)
        assertEquals(
            actual = additionalIds.size,
            expected = 2
        )
        assertEquals(
            actual = additionalIds[0],
            expected = previewId
        )
        assertEquals(
            actual = additionalIds[1],
            expected = thumbnailId
        )

        assertSame(
            actual = modifiedAttachment,
            expected = attachment
        )

        verify(exactly = 1) { attachment.id = receivedId }
        verify(exactly = 3) { fileService.uploadFile(any(), any(), any()) }
    }
}
