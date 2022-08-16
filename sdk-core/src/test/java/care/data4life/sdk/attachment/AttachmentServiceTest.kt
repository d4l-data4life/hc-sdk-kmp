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

import care.data4life.sdk.crypto.GCKey
import care.data4life.sdk.test.util.GenericTestDataProvider.ATTACHMENT_ID
import care.data4life.sdk.test.util.GenericTestDataProvider.USER_ID
import care.data4life.sdk.util.Base64.encodeToString
import care.data4life.sdk.wrapper.SdkImageResizer
import care.data4life.sdk.wrapper.WrapperContract
import io.mockk.Runs
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.mockkObject
import io.mockk.slot
import io.mockk.unmockkObject
import io.mockk.verify
import io.mockk.verifyOrder
import io.reactivex.Single
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
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
        mockkConstructor(SdkImageResizer::class)

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
    fun `Given download is called with a List of Attachments, the AttachmentKey and the UserId, it returns a list of Attachments with data`() {
        // Given
        mockkObject(AttachmentDownloadHelper)

        val userID = USER_ID
        val attachmentId = "id1"
        val derivedId = "id2"
        val attachment: WrapperContract.Attachment = mockk()
        val attachmentKey: GCKey = mockk()
        val downloadedAttachment = ByteArray(23)

        every { attachment.id } returns attachmentId

        every {
            fileService.downloadFile(attachmentKey, userID, derivedId)
        } returns Single.just(downloadedAttachment)

        every { AttachmentDownloadHelper.deriveAttachmentId(attachment) } returns derivedId
        every {
            AttachmentDownloadHelper.addAttachmentPayload(attachment, downloadedAttachment)
        } returns attachment

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
        assertSame(
            actual = downloadedAttachments[0],
            expected = attachment
        )

        verifyOrder {
            AttachmentDownloadHelper.deriveAttachmentId(attachment)
            fileService.downloadFile(attachmentKey, userID, derivedId)
            AttachmentDownloadHelper.addAttachmentPayload(attachment, downloadedAttachment)
        }

        unmockkObject(AttachmentDownloadHelper)
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

        every { anyConstructed<SdkImageResizer>().isResizable(data) } returns false

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
        val userId = USER_ID
        val receivedId = "newId"
        val attachment: WrapperContract.Attachment = mockk()
        val data = "test".toByteArray()
        val encodedData = encodeToString(data)
        val attachmentKey: GCKey = mockk()
        val originalData = slot<ByteArray>()

        every { attachment.id } returns receivedId
        every { attachment.id = receivedId } just Runs

        every { attachment.data } returns encodedData
        every {
            fileService.uploadFile(attachmentKey, userId, data)
        } returns Single.just(receivedId)

        every { anyConstructed<SdkImageResizer>().isResizable(data) } returns true
        every {
            anyConstructed<SdkImageResizer>().resize(capture(originalData), any())
        } answers { originalData.captured }

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

        verify(exactly = 1) { attachment.id = receivedId }
        verify(exactly = 1) { fileService.uploadFile(any(), any(), any()) }
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

        every { attachment.id } returns receivedId
        every { attachment.id = receivedId } just Runs

        every { attachment.data } returns encodedData
        every {
            fileService.uploadFile(attachmentKey, userId, data)
        } returns Single.just(receivedId)

        every { anyConstructed<SdkImageResizer>().isResizable(data) } returns true
        every {
            anyConstructed<SdkImageResizer>().resize(
                data,
                AttachmentContract.ImageResizer.DEFAULT_PREVIEW_SIZE_PX,
            )
        } returns preview

        every {
            fileService.uploadFile(attachmentKey, userId, preview)
        } returns Single.just(previewId)

        every {
            anyConstructed<SdkImageResizer>().resize(
                data,
                AttachmentContract.ImageResizer.DEFAULT_THUMBNAIL_SIZE_PX,
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

    @Test
    fun `Given upload is called with a list of Attachments, which contain data, the AttachmentKey and the UserId, it returns the uploaded attachments and AdditionalIds with a duplicate of the AttachmentId, if the image is too small`() {
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

        every { attachment.id } returns receivedId
        every { attachment.id = receivedId } just Runs

        every { attachment.data } returns encodedData
        every {
            fileService.uploadFile(attachmentKey, userId, data)
        } returns Single.just(receivedId)

        every { anyConstructed<SdkImageResizer>().isResizable(data) } returns true
        every {
            anyConstructed<SdkImageResizer>().resize(
                data,
                AttachmentContract.ImageResizer.DEFAULT_PREVIEW_SIZE_PX,
            )
        } returns preview

        every {
            fileService.uploadFile(attachmentKey, userId, preview)
        } returns Single.just(previewId)

        every {
            anyConstructed<SdkImageResizer>().resize(
                data,
                AttachmentContract.ImageResizer.DEFAULT_THUMBNAIL_SIZE_PX,
            )
        } returns null

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
            expected = receivedId
        )

        assertSame(
            actual = modifiedAttachment,
            expected = attachment
        )

        verify(exactly = 1) { attachment.id = receivedId }
        verify(exactly = 2) { fileService.uploadFile(any(), any(), any()) }
    }
}
