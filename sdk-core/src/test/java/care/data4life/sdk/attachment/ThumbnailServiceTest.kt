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
import care.data4life.sdk.attachment.ThumbnailService.Companion.DOWNSCALED_ATTACHMENT_IDS_FMT
import care.data4life.sdk.attachment.ThumbnailService.Companion.DOWNSCALED_ATTACHMENT_IDS_SIZE
import care.data4life.sdk.attachment.ThumbnailService.Companion.FULL_ATTACHMENT_ID_POS
import care.data4life.sdk.attachment.ThumbnailService.Companion.PREVIEW_ID_POS
import care.data4life.sdk.attachment.ThumbnailService.Companion.SPLIT_CHAR
import care.data4life.sdk.attachment.ThumbnailService.Companion.THUMBNAIL_ID_POS
import care.data4life.sdk.fhir.Fhir3Resource
import care.data4life.sdk.lang.DataValidationException
import care.data4life.sdk.lang.ImageResizeException
import care.data4life.sdk.log.Log
import care.data4life.sdk.model.DownloadType
import care.data4life.sdk.wrapper.FhirAttachmentHelper
import care.data4life.sdk.wrapper.IdentifierFactory
import care.data4life.sdk.wrapper.WrapperContract
import com.google.common.truth.Truth
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.spyk
import io.mockk.unmockkObject
import io.mockk.verify
import io.reactivex.Single
import org.junit.After
import org.junit.Assert
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ThumbnailServiceTest {
    private lateinit var resizer: ImageResizer
    private lateinit var fileService: FileContract.Service


    private lateinit var thumbnailService: ThumbnailContract.Service
    @Before
    fun setUp() {
        resizer = mockk(relaxed = true)
        fileService = mockk(relaxed = true)

        thumbnailService = ThumbnailService("", resizer, fileService)

        mockkObject(FhirAttachmentHelper)
        mockkObject(IdentifierFactory)
    }
    
    @After
    fun tearDown() {
        unmockkObject(FhirAttachmentHelper)
        unmockkObject(IdentifierFactory)
    }

    @Test
    fun `it is a ThumbnailService`() {
        assertTrue((ThumbnailService("", resizer, fileService) as Any) is ThumbnailContract.Service)
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
        val result = ThumbnailService("", resizer, fileService)
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
        val result = thumbnailService
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
        val result = thumbnailService
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
        val result = thumbnailService
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
        val result = thumbnailService
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
        val result = thumbnailService
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
        val result = thumbnailService
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

        every {  FhirAttachmentHelper.appendIdentifier(
                any(),
                any(),
                any()
        ) } returns mockk()

        // When
        thumbnailService.updateResourceIdentifier(
                resource,
                listOf<Pair<WrapperContract.Attachment, List<String>?>>(attachment to null)
        )

        verify(exactly = 0) { FhirAttachmentHelper.appendIdentifier(
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
        val unwrappedResource = mockk<Fhir3Resource>()
        val partnerId = "di"

        every { resource.unwrap() } returns unwrappedResource
        every { attachment.id } returns "something"

        every { FhirAttachmentHelper.appendIdentifier(
                unwrappedResource,
                "d4l_f_p_t#something#abc",
                partnerId
        ) } returns mockk()

        // When
        ThumbnailService(partnerId, resizer, fileService).updateResourceIdentifier(
                resource,
                listOf<Pair<WrapperContract.Attachment, List<String>?>>(attachment to listOf("abc"))
        )

        verify(exactly = 1) { FhirAttachmentHelper.appendIdentifier(
                unwrappedResource,
                "d4l_f_p_t#something#abc",
                partnerId
        ) }
    }

    //TODO: Unhappy path
    @Test
    @Throws(DataValidationException.IdUsageViolation::class)
    fun `Given, setAttachmentIdForDownloadType is called with with Attachments, Identifiers and Full as DownloadType, it does nothing`() {
        //given
        val attachment = mockk<WrapperContract.Attachment>(relaxed = true)
        val attachmentId = "id"
        val rawId = "raw"

        val attachments = listOf(attachment)
        val identifiers = listOf(rawId)

        val additionalIds = listOf(DOWNSCALED_ATTACHMENT_IDS_FMT, "attachmentId", "previewId", "thumbnailId" )

        val spyService = spyk((thumbnailService as ThumbnailService))

        every { attachment.id } returns attachmentId
        every { spyService.extractAdditionalAttachmentIds(identifiers, attachmentId) } returns additionalIds

        //when downloadType is Full
        spyService.setAttachmentIdForDownloadType(attachments, identifiers, DownloadType.Full)
        //then
        verify(exactly = 0) { attachment.id = any() }
    }

    @Test
    @Throws(DataValidationException.IdUsageViolation::class)
    fun `Given, setAttachmentIdForDownloadType is called with with Attachments, Identifiers and Medium as DownloadType, it sets amends PREVIEW_ID to the AttachmentId`() {
        //given
        val attachment = mockk<WrapperContract.Attachment>(relaxed = true)
        val attachmentId = "id"
        val rawId = "raw"

        val attachments = listOf(attachment)
        val identifiers = listOf(rawId)

        val additionalIds = listOf(DOWNSCALED_ATTACHMENT_IDS_FMT, "attachmentId", "previewId", "thumbnailId" )

        val spyService = spyk((thumbnailService as ThumbnailService))

        every { attachment.id } returns attachmentId
        every { spyService.extractAdditionalAttachmentIds(identifiers, attachmentId) } returns additionalIds

        //when downloadType is Medium
        spyService.setAttachmentIdForDownloadType(attachments, identifiers, DownloadType.Medium)
        //then
        verify(exactly = 1) { attachment.id = attachmentId + SPLIT_CHAR + additionalIds[PREVIEW_ID_POS] }
    }

    @Test
    @Throws(DataValidationException.IdUsageViolation::class)
    fun `Given, setAttachmentIdForDownloadType is called with with Attachments, Identifiers and Small as DownloadType, it sets amends THUMBNAIL_ID to the AttachmentId`() {
        //given
        val attachment = mockk<WrapperContract.Attachment>(relaxed = true)
        val attachmentId = "id"
        val rawId = "raw"

        val attachments = listOf(attachment)
        val identifiers = listOf(rawId)

        val additionalIds = listOf(DOWNSCALED_ATTACHMENT_IDS_FMT, "attachmentId", "previewId", "thumbnailId" )

        val spyService = spyk((thumbnailService as ThumbnailService))

        every { attachment.id } returns attachmentId
        every { spyService.extractAdditionalAttachmentIds(identifiers, attachmentId) } returns additionalIds

        //when downloadType is Small
        spyService.setAttachmentIdForDownloadType(attachments, identifiers, DownloadType.Small)
        //then
        verify(exactly = 1) { attachment.id = attachmentId + SPLIT_CHAR + additionalIds[THUMBNAIL_ID_POS] }
    }

    @Test
    @Throws(DataValidationException.IdUsageViolation::class)
    fun `Given, extractAdditionalAttachmentIds is called with AdditionalIdentifier and a AttachmentId, it returns null, if the AdditionalIdentifier are not additional AttachmentIds`() {
        //given
        val rawId = "I make trouble"
        val wrappedId = mockk<WrapperContract.Identifier>()

        val spyService = spyk((thumbnailService as ThumbnailService))

        every { IdentifierFactory.wrap(rawId) } returns wrappedId
        every { spyService.splitAdditionalAttachmentId(wrappedId) } returns null

        //when
        val additionalIds = spyService.extractAdditionalAttachmentIds(listOf(rawId), "attachmentId")

        //then
        Truth.assertThat(additionalIds).isNull()

        verify(exactly = 1) { IdentifierFactory.wrap(rawId) }
        verify(exactly = 1) { spyService.splitAdditionalAttachmentId(wrappedId) }
    }

    @Test
    @Throws(DataValidationException.IdUsageViolation::class)
    fun `Given, extractAdditionalAttachmentIds is called with AdditionalIdentifier and a AttachmentId, it returns null, if the AdditionalIdentifier are null`() {
        //when
        val additionalIds = (thumbnailService as ThumbnailService).extractAdditionalAttachmentIds(null, "attachmentId")

        //then
        Truth.assertThat(additionalIds).isNull()

        verify(exactly = 0) { IdentifierFactory.wrap(null) }
    }

    @Test
    @Throws(DataValidationException.IdUsageViolation::class)
    fun `Given, extractAdditionalAttachmentIds is called with AdditionalIdentifier and a AttachmentId, it returns the extracted additional Ids`() {
        //given
        val rawId = ADDITIONAL_ID
        val wrappedId = mockk<WrapperContract.Identifier>()
        val additionalId = listOf(DOWNSCALED_ATTACHMENT_IDS_FMT, "attachmentId", "previewId", "thumbnailId" )

        val spyService = spyk((thumbnailService as ThumbnailService))

        every { IdentifierFactory.wrap(rawId) } returns wrappedId
        every { spyService.splitAdditionalAttachmentId(wrappedId) } returns additionalId

        //when
        val additionalIds = spyService.extractAdditionalAttachmentIds(listOf(rawId), "attachmentId")

        //then
        Truth.assertThat(additionalIds).isEqualTo(additionalId)

        verify(exactly = 1) { IdentifierFactory.wrap(rawId) }
        verify(exactly = 1) { spyService.splitAdditionalAttachmentId(wrappedId) }
    }

    @Test
    fun `Given, splitAdditionalAttachmentId is called with a Identifier, which is malformed, it fails with a IdUsageViolation`() {
        //given
        val malformedAdditionalId = ADDITIONAL_ID + SPLIT_CHAR + "unexpectedId"

        val wrappedId = mockk<WrapperContract.Identifier>()

        every { wrappedId.value } returns malformedAdditionalId

        //when
        try {
            (thumbnailService as ThumbnailService).splitAdditionalAttachmentId(wrappedId)
            Assert.fail("Exception expected!")
        } catch (ex: DataValidationException.IdUsageViolation) {

            //then
            Truth.assertThat(ex.message).isEqualTo(malformedAdditionalId)
        }
    }

    @Test
    @Throws(DataValidationException.IdUsageViolation::class)
    fun `Given, splitAdditionalAttachmentId is called with a Identifier, it returns null, if the AdditionalIdentifier is not a additional AttachmentId`() {
        //given
        val wrappedId = mockk<WrapperContract.Identifier>()

        every { wrappedId.value } returns "otherId"

        //when
        val additionalIds = (thumbnailService as ThumbnailService).splitAdditionalAttachmentId(wrappedId)

        //then
        Truth.assertThat(additionalIds).isNull()
    }

    @Test
    @Throws(DataValidationException.IdUsageViolation::class)
    fun `Given, splitAdditionalAttachmentId is called with a Identifier, which is null, it returns null`() {
        //given
        val wrappedId = mockk<WrapperContract.Identifier>()

        every { wrappedId.value } returns null
        //when
        val additionalIds = (thumbnailService as ThumbnailService).splitAdditionalAttachmentId(wrappedId)
        //then
        Truth.assertThat(additionalIds).isNull()
    }

    @Test
    @Throws(DataValidationException.IdUsageViolation::class)
    fun `Given, splitAdditionalAttachmentId is called with a Identifier, return the splitted identifiers`() {
        //given
        val wrappedId = mockk<WrapperContract.Identifier>()

        every { wrappedId.value } returns ADDITIONAL_ID
        //when
        val additionalIds = (thumbnailService as ThumbnailService).splitAdditionalAttachmentId(wrappedId)

        //then
        val d4lNamespacePos = 0
        Truth.assertThat(additionalIds).hasSize(DOWNSCALED_ATTACHMENT_IDS_SIZE)
        Truth.assertThat(additionalIds!![d4lNamespacePos]).isEqualTo(DOWNSCALED_ATTACHMENT_IDS_FMT)
        Truth.assertThat(additionalIds[FULL_ATTACHMENT_ID_POS]).isEqualTo("attachmentId")
        Truth.assertThat(additionalIds[PREVIEW_ID_POS]).isEqualTo("previewId")
        Truth.assertThat(additionalIds[THUMBNAIL_ID_POS]).isEqualTo("thumbnailId")
    }

    companion object {
        private const val ADDITIONAL_ID = DOWNSCALED_ATTACHMENT_IDS_FMT +
                SPLIT_CHAR +
                "attachmentId" +
                SPLIT_CHAR +
                "previewId" +
                SPLIT_CHAR +
                "thumbnailId"
    }

    /*
@Test
    @Throws(DataValidationException.IdUsageViolation::class)
    fun setAttachmentIdForDownloadType_shouldSetAttachmentId() {
        //given
        val attachment = AttachmentBuilder.buildAttachment(ATTACHMENT_ID)
        val additionalId = FhirAttachmentHelper.buildIdentifier(ADDITIONAL_ID, ASSIGNER)
        val attachments = listOf(attachment)
        val identifiers = listOf(additionalId)

        //when downloadType is Full
        recordService.setAttachmentIdForDownloadType(attachments, identifiers, DownloadType.Full)
        //then
        Truth.assertThat(attachment.id).isEqualTo(ATTACHMENT_ID)

        //given
        attachment.id = ATTACHMENT_ID
        //when downloadType is Medium
        recordService.setAttachmentIdForDownloadType(attachments, identifiers, DownloadType.Medium)
        //then
        Truth.assertThat(attachment.id).isEqualTo(ATTACHMENT_ID + SPLIT_CHAR + PREVIEW_ID)

        //given
        attachment.id = ATTACHMENT_ID
        //when downloadType is Small
        recordService.setAttachmentIdForDownloadType(attachments, identifiers, DownloadType.Small)
        //then
        Truth.assertThat(attachment.id).isEqualTo(ATTACHMENT_ID + SPLIT_CHAR + THUMBNAIL_ID)
    }
     */
}
