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
import care.data4life.fhir.stu3.util.FhirDateTimeParser
import care.data4life.sdk.config.DataRestrictionException
import care.data4life.sdk.fhir.Fhir3Attachment
import care.data4life.sdk.helpers.stu3.AttachmentBuilder.buildWith
import care.data4life.sdk.lang.D4LException
import care.data4life.sdk.lang.DataValidationException.InvalidAttachmentPayloadHash
import care.data4life.sdk.wrapper.SdkAttachmentFactory
import care.data4life.sdk.wrapper.WrapperContract
import com.google.common.truth.Truth
import io.reactivex.Single
import org.junit.Assert
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.mockito.Mockito
import java.util.*

class AttachmentServiceTest {
    private val ATTACHMENT_ID = "attachmentId"
    private val THUMBNAIL_ID = "attachmentId#previewId"
    private val RECORD_ID = "recordId"
    private val USER_ID = "userId"
    private val TITLE = "title"
    private val CONTENT_TYPE = "contentType"
    private val creationDate = FhirDateTimeParser.parseDateTime("2013-04-03")
    private val pdf = byteArrayOf(0x25, 0x50, 0x44, 0x46, 0x2d)
    private val largeFile = ByteArray(20000000 + 1)
    private val dataBase64 = "JVBERi0="
    private val DATA_HASH = "dataHash"
    private val attachmentKey = Mockito.mock(GCKey::class.java)

    private lateinit var mockFileService: AttachmentContract.FileService
    private lateinit var mockImageResizer: AttachmentContract.ImageResizer
    private lateinit var attachment: WrapperContract.Attachment

    private lateinit var attachmentService: AttachmentService

    @Before
    @Throws(DataRestrictionException.UnsupportedFileType::class, DataRestrictionException.MaxDataSizeViolation::class)
    fun setUp() {
        attachment = SdkAttachmentFactory.wrap(buildWith(TITLE, creationDate, CONTENT_TYPE, pdf))
        mockFileService = Mockito.mock(FileService::class.java)
        mockImageResizer = Mockito.mock(AttachmentContract.ImageResizer::class.java)
        attachmentService = AttachmentService(mockFileService, mockImageResizer)
    }

    @Test
    @Throws(InterruptedException::class,
            DataRestrictionException.UnsupportedFileType::class,
            DataRestrictionException.MaxDataSizeViolation::class)
    fun uploadAttachments_shouldReturnListOfAttachments() {
        // given
        attachment.id = "id"
        val newAttachment = SdkAttachmentFactory.wrap(buildWith("newAttachment", creationDate, CONTENT_TYPE, pdf))
        newAttachment.id = null
        val attachments = listOf(attachment, newAttachment)
        Mockito.`when`(mockFileService.uploadFile(attachmentKey, USER_ID, pdf)).thenReturn(Single.just(ATTACHMENT_ID))

        // when
        val subscriber = attachmentService
                .upload(attachments, attachmentKey, USER_ID).test().await()

        // then
        val result = subscriber
                .assertNoErrors()
                .assertComplete()
                .assertValueCount(1)
                .values()[0]
        Truth.assertThat(result).hasSize(2)
        val a1 = result[0].first.unwrap<Fhir3Attachment>()
        Truth.assertThat(a1.id).isEqualTo(ATTACHMENT_ID)
        Truth.assertThat(a1.title).isEqualTo(TITLE)
        Truth.assertThat(a1.creation).isEqualTo(creationDate)
        Truth.assertThat(a1.contentType).isEqualTo(CONTENT_TYPE)
        Truth.assertThat(a1.data).isEqualTo(dataBase64)

        val a2 = result[1].first.unwrap<Fhir3Attachment>()
        Truth.assertThat(a2.id).isEqualTo(ATTACHMENT_ID)
        Truth.assertThat(a2.title).isEqualTo("newAttachment")
        Truth.assertThat(a2.creation).isEqualTo(creationDate)
        Truth.assertThat(a2.contentType).isEqualTo(CONTENT_TYPE)
        Truth.assertThat(a2.data).isEqualTo(dataBase64)

        Mockito.verify(mockFileService, Mockito.times(2))!!.uploadFile(attachmentKey, USER_ID, pdf)
        Mockito.verifyNoMoreInteractions(mockFileService)
    }

    @Ignore("Legacy leftover")
    @Test
    @Throws(InterruptedException::class,
            DataRestrictionException.UnsupportedFileType::class,
            DataRestrictionException.MaxDataSizeViolation::class)
    fun uploadAttachment_with_20MB_and_1_shouldFail() {
        // given
        attachment.id = "id"
        System.arraycopy(pdf, 0, largeFile, 0, pdf.size)
        try {
            val newAttachment = SdkAttachmentFactory.wrap(buildWith("newAttachment", creationDate, CONTENT_TYPE, largeFile))
            newAttachment.id = null
            Assert.fail("Should have thrown an exception");
        } catch (e: DataRestrictionException.MaxDataSizeViolation) {
            assert(true)
        }
    }

    @Test
    @Throws(InterruptedException::class, InvalidAttachmentPayloadHash::class)
    fun downloadAttachments_shouldReturnListOfAttachments() {
        // given
        attachment.id = ATTACHMENT_ID
        attachment.data = null
        val attachments = listOf(attachment)
        Mockito.`when`(mockFileService.downloadFile(attachmentKey, USER_ID, ATTACHMENT_ID)).thenReturn(Single.just(pdf))

        // when
        val subscriber = attachmentService.download(attachments, attachmentKey, USER_ID).test().await()

        // then
        val result = subscriber
                .assertNoErrors()
                .assertComplete()
                .assertValueCount(1)
                .values()[0]
        Truth.assertThat(result).hasSize(1)
        val a = result[0].unwrap<Fhir3Attachment>()
        Truth.assertThat(a.id).isEqualTo(ATTACHMENT_ID)
        Truth.assertThat(a.title).isEqualTo(TITLE)
        Truth.assertThat(a.creation).isEqualTo(creationDate)
        Truth.assertThat(a.contentType).isEqualTo(CONTENT_TYPE)
        Truth.assertThat(a.data).isEqualTo(dataBase64)

        Mockito.verify(mockFileService)!!.downloadFile(attachmentKey, USER_ID, ATTACHMENT_ID)
        Mockito.verifyNoMoreInteractions(mockFileService)
    }

    @Ignore("Legacy leftover")
    @Test
    @Throws(InvalidAttachmentPayloadHash::class)
    fun downloadAttachments_shouldThrow_whenInvalidHashAttachment() {
        // Given
        attachment.id = ATTACHMENT_ID
        attachment.hash = DATA_HASH
        val attachments = listOf(attachment)

        // When
        try {
            attachmentService.download(attachments, attachmentKey, USER_ID)
            Assert.fail("Exception expected!");
        } catch (e: D4LException) {

            // Then
            Truth.assertThat(e.javaClass).isEqualTo(InvalidAttachmentPayloadHash::class.java)
            Truth.assertThat(e.message).isEqualTo("Attachment.hash is not valid")
        }
    }

    @Test
    @Throws(InterruptedException::class, InvalidAttachmentPayloadHash::class)
    fun downloadAttachments_shouldTNot_throw_whenInvalidHashPreview() {
        // Given
        attachment.id = THUMBNAIL_ID
        attachment.hash = DATA_HASH
        val attachments = Arrays.asList(attachment)
        Mockito.`when`(
                mockFileService.downloadFile(
                        attachmentKey,
                        USER_ID,
                        THUMBNAIL_ID.split(ThumbnailService.SPLIT_CHAR)[1])
        ).thenReturn(Single.just(pdf))

        // when
        val subscriber = attachmentService.download(attachments, attachmentKey, USER_ID).test().await()

        // then
        val result = subscriber
                .assertNoErrors()
                .assertComplete()
                .assertValueCount(1)
                .values()[0]
        Truth.assertThat(result).hasSize(1)

        val a = result[0].unwrap<Fhir3Attachment>()
        Truth.assertThat(a.id).isEqualTo(THUMBNAIL_ID)
        Truth.assertThat(a.title).isEqualTo(TITLE)
        Truth.assertThat(a.creation).isEqualTo(creationDate)
        Truth.assertThat(a.contentType).isEqualTo(CONTENT_TYPE)
        Truth.assertThat(a.data).isEqualTo(dataBase64)

        Mockito.verify(mockFileService)!!.downloadFile(
                attachmentKey,
                USER_ID,
                THUMBNAIL_ID.split(ThumbnailService.SPLIT_CHAR)[1]
        )
        Mockito.verifyNoMoreInteractions(mockFileService)
    }

    @Test
    @Throws(InterruptedException::class, DataRestrictionException.UnsupportedFileType::class,
            DataRestrictionException.MaxDataSizeViolation::class)
    fun updatingAttachments_shouldReturnListOfAttachmentsInOrder() {
        // given
        attachment.id = null
        val newAttachment = SdkAttachmentFactory.wrap(buildWith("newAttachment", creationDate, CONTENT_TYPE, pdf))
        val attachments = listOf(attachment, newAttachment)
        Mockito.`when`(mockFileService.uploadFile(attachmentKey, USER_ID, pdf)).thenReturn(Single.just(ATTACHMENT_ID))

        // when
        val subscriber = attachmentService.upload(attachments, attachmentKey, USER_ID).test().await()

        // then
        val result = subscriber
                .assertNoErrors()
                .assertComplete()
                .assertValueCount(1)
                .values()[0]

        Truth.assertThat(result).hasSize(2)
        Truth.assertThat(result[0].first).isEqualTo(attachment)
        Truth.assertThat(result[1].first).isEqualTo(newAttachment)
    }

    @Test
    @Throws(InterruptedException::class)
    fun deleteAttachment() {
        // given
        Mockito.`when`(mockFileService.deleteFile(USER_ID, ATTACHMENT_ID)).thenReturn(Single.just(true))

        // when
        val subscriber = attachmentService.delete(ATTACHMENT_ID, USER_ID).test().await()

        // then
        subscriber.assertNoErrors()
                .assertComplete()
                .assertValue { it: Boolean? -> it!! }
    }

    @Test
    @Throws(InterruptedException::class)
    fun deleteAttachment_shouldFail() {
        // given
        Mockito.`when`(mockFileService.deleteFile(USER_ID, ATTACHMENT_ID)).thenReturn(Single.error(Throwable()))

        // when
        val subscriber = attachmentService.delete(ATTACHMENT_ID, USER_ID).test().await()

        // then
        subscriber.assertError { obj: Throwable? -> Objects.nonNull(obj) }
                .assertNotComplete()
    }
}
