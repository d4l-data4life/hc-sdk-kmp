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
import care.data4life.sdk.ImageResizer
import care.data4life.sdk.fhir.Fhir3Attachment
import care.data4life.sdk.fhir.Fhir3Resource
import care.data4life.sdk.lang.ImageResizeException
import care.data4life.sdk.log.Log
import care.data4life.sdk.wrapper.HelperContract
import care.data4life.sdk.wrapper.WrapperContract
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import io.mockk.verify
import io.reactivex.Single
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ThumbnailServiceTest {
    private lateinit var resizer: ImageResizer
    private lateinit var fileService: FileContract.Service

    @Before
    fun setUp() {
        resizer = mockk(relaxed = true)
        fileService = mockk(relaxed = true)
    }

    @Test
    fun `it is a ThumbnailService`() {
        assertTrue((ThumbnailService("", resizer,fileService, mockk())as Any) is ThumbnailContract.Service)
    }

    @Test
    fun `Given, uploadDownscaledImages a AttachmentKey, a UserId, a Attachment and the OriginalData, which is not resizeable, it returns a empty List`() {
        //Given
        val userId = "id"
        val orgData = ByteArray(42)
        val attachment = mockk<WrapperContract.Attachment>()
        val attachmentKey = mockk<GCKey>()

        every { resizer.isResizable(orgData) }  returns false

        // When
        val result = ThumbnailService("", resizer,fileService, mockk())
                .uploadDownscaledImages(attachmentKey, userId, attachment, orgData)

        // Then
        assertTrue(result.isEmpty())

        verify(exactly = 1) { resizer.isResizable(orgData) }
    }

    @Test
    fun `Given, uploadDownscaledImages a AttachmentKey, a UserId, a Attachment and the OriginalData, it logs an error, if the resizing fails`() {
        //Given
        val userId = "id"
        val orgData = ByteArray(42)
        val attachment = mockk<WrapperContract.Attachment>()
        val attachmentKey = mockk<GCKey>()
        val exception = ImageResizeException.JpegWriterMissing()

        mockkObject(Log)

        every { resizer.isResizable(orgData) }  returns true
        every { resizer.resizeToHeight(
                any(), any(), any()
        ) } throws exception

        every { Log.Companion.error(exception, exception.message) } returns Unit

        // When
        val result = ThumbnailService("", resizer,fileService, mockk())
                .uploadDownscaledImages(attachmentKey, userId, attachment, orgData)

        // Then
        assertTrue(result.isEmpty())

        verify(exactly = 1) { resizer.isResizable(orgData) }
        verify { Log.error(exception, exception.message) }

        unmockkObject(Log)
    }

    @Test
    fun `Given, uploadDownscaledImages a AttachmentKey, a UserId, a Attachment and the OriginalData, it does not upload its preview, if the original image is equal or smaller then its preview`() {
        //Given
        val userId = "id"
        val orgData = ByteArray(42)
        val attachment = mockk<WrapperContract.Attachment>()
        val attachmentKey = mockk<GCKey>()
        val attachmentId = "me"

        every { attachment.id } returns attachmentId
        every { resizer.isResizable(orgData) }  returns true
        every { resizer.resizeToHeight(
                orgData, ImageResizer.DEFAULT_PREVIEW_SIZE_PX, ImageResizer.DEFAULT_JPEG_QUALITY_PERCENT
        ) } returns null
        every { resizer.resizeToHeight(
                any(), ImageResizer.DEFAULT_THUMBNAIL_SIZE_PX, ImageResizer.DEFAULT_JPEG_QUALITY_PERCENT
        ) } returns null

        every { fileService.uploadFile(attachmentKey, userId, any()) } returns mockk()

        // When
        val result = ThumbnailService("", resizer,fileService, mockk())
                .uploadDownscaledImages(attachmentKey, userId, attachment, orgData)

        // Then
        assertEquals(
                attachmentId,
                result[0]
        )

        verify(exactly = 1) { resizer.isResizable(orgData) }
        verify(exactly = 1) { resizer.resizeToHeight(
                orgData, ImageResizer.DEFAULT_PREVIEW_SIZE_PX, ImageResizer.DEFAULT_JPEG_QUALITY_PERCENT
        ) }
        verify(exactly = 0) { fileService.uploadFile(attachmentKey, userId, any()) }
    }

    @Test
    fun `Given, uploadDownscaledImages a AttachmentKey, a UserId, a Attachment and the OriginalData, it uploads its preview, if the original image is bigger then its preview`() {
        //Given
        val userId = "id"
        val orgData = ByteArray(42)
        val attachment = mockk<WrapperContract.Attachment>()
        val attachmentKey = mockk<GCKey>()
        val downscaled = ByteArray(1)
        val upload = mockk<Single<String>>()
        val newId = "new"
        val attachmentId = "me"

        every { attachment.id } returns attachmentId
        every { resizer.isResizable(orgData) }  returns true
        every { resizer.resizeToHeight(
                orgData, ImageResizer.DEFAULT_PREVIEW_SIZE_PX, ImageResizer.DEFAULT_JPEG_QUALITY_PERCENT
        ) } returns downscaled
        every { resizer.resizeToHeight(
                any(), ImageResizer.DEFAULT_THUMBNAIL_SIZE_PX, ImageResizer.DEFAULT_JPEG_QUALITY_PERCENT
        ) } returns null
        every { fileService.uploadFile(attachmentKey, userId, any()) } returns upload
        every { upload.blockingGet() } returns newId

        // When
        val result = ThumbnailService("", resizer,fileService, mockk())
                .uploadDownscaledImages(attachmentKey, userId, attachment, orgData)

        // Then
        assertEquals(
                newId,
                result[0]
        )

        verify(exactly = 1) { resizer.isResizable(orgData) }
        verify(exactly = 1) { resizer.resizeToHeight(
                orgData, ImageResizer.DEFAULT_PREVIEW_SIZE_PX, ImageResizer.DEFAULT_JPEG_QUALITY_PERCENT
        ) }
        verify(exactly = 1) { fileService.uploadFile(attachmentKey, userId, downscaled) }
    }

    @Test
    fun `Given, uploadDownscaledImages a AttachmentKey, a UserId, a Attachment and the OriginalData, it does not upload its thumbnail, if the original image is equal or smaller then its thumbnail`() {
        //Given
        val userId = "id"
        val orgData = ByteArray(42)
        val attachment = mockk<WrapperContract.Attachment>()
        val attachmentKey = mockk<GCKey>()
        val attachmentId = "me"

        every { attachment.id } returns attachmentId
        every { resizer.isResizable(orgData) }  returns true
        every { resizer.resizeToHeight(
                orgData, ImageResizer.DEFAULT_THUMBNAIL_SIZE_PX, ImageResizer.DEFAULT_JPEG_QUALITY_PERCENT
        ) } returns null
        every { resizer.resizeToHeight(
                any(), ImageResizer.DEFAULT_PREVIEW_SIZE_PX, ImageResizer.DEFAULT_JPEG_QUALITY_PERCENT
        ) } returns null

        every { fileService.uploadFile(attachmentKey, userId, any()) } returns mockk()

        // When
        val result = ThumbnailService("", resizer,fileService, mockk())
                .uploadDownscaledImages(attachmentKey, userId, attachment, orgData)

        // Then
        assertEquals(
                attachmentId,
                result[1]
        )

        verify(exactly = 1) { resizer.isResizable(orgData) }
        verify(exactly = 1) { resizer.resizeToHeight(
                orgData, ImageResizer.DEFAULT_THUMBNAIL_SIZE_PX, ImageResizer.DEFAULT_JPEG_QUALITY_PERCENT
        ) }
        verify(exactly = 0) { fileService.uploadFile(attachmentKey, userId, any()) }
    }

    @Test
    fun `Given, uploadDownscaledImages a AttachmentKey, a UserId, a Attachment and the OriginalData, it uploads its thumbnail, if the original image is bigger then its thumbnail`() {
        //Given
        val userId = "id"
        val orgData = ByteArray(42)
        val attachment = mockk<WrapperContract.Attachment>()
        val attachmentKey = mockk<GCKey>()
        val downscaled = ByteArray(1)
        val upload = mockk<Single<String>>()
        val newId = "new"
        val attachmentId = "me"

        every { attachment.id } returns attachmentId
        every { resizer.isResizable(orgData) }  returns true
        every { resizer.resizeToHeight(
                orgData, ImageResizer.DEFAULT_THUMBNAIL_SIZE_PX, ImageResizer.DEFAULT_JPEG_QUALITY_PERCENT
        ) } returns downscaled
        every { resizer.resizeToHeight(
                any(), ImageResizer.DEFAULT_PREVIEW_SIZE_PX, ImageResizer.DEFAULT_JPEG_QUALITY_PERCENT
        ) } returns null
        every { fileService.uploadFile(attachmentKey, userId, any()) } returns upload
        every { upload.blockingGet() } returns newId

        // When
        val result = ThumbnailService("", resizer,fileService, mockk())
                .uploadDownscaledImages(attachmentKey, userId, attachment, orgData)

        // Then
        assertEquals(
                newId,
                result[1]
        )

        verify(exactly = 1) { resizer.isResizable(orgData) }
        verify(exactly = 1) { resizer.resizeToHeight(
                orgData, ImageResizer.DEFAULT_THUMBNAIL_SIZE_PX, ImageResizer.DEFAULT_JPEG_QUALITY_PERCENT
        ) }
        verify(exactly = 1) { fileService.uploadFile(attachmentKey, userId, downscaled) }

    }

    @Test
    fun `Given, uploadDownscaledImages a AttachmentKey, a UserId, a Attachment and the OriginalData, it uploads its thumbnail and its preview`() {
        //Given
        val userId = "id"
        val orgData = ByteArray(42)
        val attachment = mockk<WrapperContract.Attachment>()
        val attachmentKey = mockk<GCKey>()
        val downscaledPreview = ByteArray(1)
        val downscaledThumbnail = ByteArray(2)
        val uploadPreview = mockk<Single<String>>()
        val uploadThumnail = mockk<Single<String>>()
        val newIdPreview = "new"
        val newIdThumbnail = "new2"
        val attachmentId = "me"

        every { attachment.id } returns attachmentId
        every { resizer.isResizable(orgData) }  returns true
        every { resizer.resizeToHeight(
                orgData, ImageResizer.DEFAULT_PREVIEW_SIZE_PX, ImageResizer.DEFAULT_JPEG_QUALITY_PERCENT
        ) } returns downscaledPreview
        every { resizer.resizeToHeight(
                any(), ImageResizer.DEFAULT_THUMBNAIL_SIZE_PX, ImageResizer.DEFAULT_JPEG_QUALITY_PERCENT
        ) } returns downscaledThumbnail

        every { fileService.uploadFile(attachmentKey, userId, downscaledPreview) } returns uploadPreview
        every { uploadPreview.blockingGet() } returns newIdPreview

        every { fileService.uploadFile(attachmentKey, userId, downscaledThumbnail) } returns uploadThumnail
        every { uploadThumnail.blockingGet() } returns newIdThumbnail

        // When
        val result = ThumbnailService("", resizer,fileService, mockk())
                .uploadDownscaledImages(attachmentKey, userId, attachment, orgData)

        // Then
        assertEquals(
                listOf(newIdPreview, newIdThumbnail),
                result
        )

        verify(exactly = 1) { resizer.isResizable(orgData) }
        verify(exactly = 1) { resizer.resizeToHeight(
                orgData, ImageResizer.DEFAULT_PREVIEW_SIZE_PX, ImageResizer.DEFAULT_JPEG_QUALITY_PERCENT
        ) }
        verify(exactly = 1) { resizer.resizeToHeight(
                any(), ImageResizer.DEFAULT_THUMBNAIL_SIZE_PX, ImageResizer.DEFAULT_JPEG_QUALITY_PERCENT
        ) }
        verify(exactly = 1) { fileService.uploadFile(attachmentKey, userId, downscaledPreview) }
        verify(exactly = 1) { fileService.uploadFile(attachmentKey, userId, downscaledThumbnail) }
    }

    @Test
    fun `Given, updateResourceIdentifier is called, with a Resource and a Map of Attachments to a null for a Lists of String, it does noting`() {
        // Given
        val resource = mockk<WrapperContract.Resource>()
        val attachment = mockk<WrapperContract.Attachment>()
        val fhirHelper = mockk<HelperContract.FhirAttachmentHelper>()

        // When
        ThumbnailService("", resizer,fileService, fhirHelper).updateResourceIdentifier(
                resource,
                listOf<Pair<WrapperContract.Attachment, List<String>?>>(attachment to null)
        )

        verify(exactly = 0) { fhirHelper.appendIdentifier(
                any(),
                any(),
                any()
        ) }
    }

    @Test
    fun `Given, updateResourceIdentifier is called, with a Resource and a Map of Attachments to a Lists of String, it appends the Identifier`() {
        // Given
        val resource = mockk<WrapperContract.Resource>()
        val attachment = mockk<WrapperContract.Attachment>()
        val fhirHelper = mockk<HelperContract.FhirAttachmentHelper>(relaxed = true)
        val unwrappedResource = mockk<Fhir3Resource>()
        val partnerId = "di"

        every { resource.unwrap() } returns unwrappedResource
        every { attachment.id } returns "something"

        // When
        ThumbnailService(partnerId, resizer,fileService, fhirHelper).updateResourceIdentifier(
                resource,
                listOf<Pair<WrapperContract.Attachment, List<String>?>>(attachment to listOf("abc"))
        )

        verify(exactly = 1) { fhirHelper.appendIdentifier(
                unwrappedResource,
                "d4l_f_p_t#something#abcg",
                partnerId
        ) }
    }
}
