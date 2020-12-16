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
import care.data4life.sdk.attachment.ThumbnailService.Companion.SPLIT_CHAR
import care.data4life.sdk.config.DataRestriction.DATA_SIZE_MAX_BYTES
import care.data4life.sdk.config.DataRestrictionException
import care.data4life.sdk.lang.CoreRuntimeException
import care.data4life.sdk.lang.D4LException
import care.data4life.sdk.lang.DataValidationException
import care.data4life.sdk.model.DownloadType
import care.data4life.sdk.network.model.NetworkRecordContract
import care.data4life.sdk.util.Base64
import care.data4life.sdk.util.HashUtil
import care.data4life.sdk.util.MimeType
import care.data4life.sdk.wrapper.AttachmentFactory
import care.data4life.sdk.wrapper.FhirAttachmentHelper
import care.data4life.sdk.wrapper.WrapperContract
import com.google.common.truth.Truth
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.spyk
import io.mockk.unmockkObject
import io.mockk.verify
import io.reactivex.Single
import org.junit.After
import org.junit.Assert
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.mockito.Mockito

class AttachmentServiceTest {
    private val attachmentKey = Mockito.mock(GCKey::class.java)
    private lateinit var fileService: FileContract.Service
    private lateinit var thumbnailService: ThumbnailContract.Service
    private lateinit var attachment: WrapperContract.Attachment

    private lateinit var attachmentService: AttachmentService

    @Before
    fun setUp() {
        attachment = mockk(relaxed = true)
        fileService = mockk()
        thumbnailService = mockk()
        attachmentService = AttachmentService(fileService, thumbnailService)

        mockkObject(Base64)
        mockkObject(FhirDateValidator)
        mockkObject(HashUtil)
        mockkObject(FhirAttachmentHelper)
        mockkObject(AttachmentFactory)
        mockkObject(MimeType)
    }

    @After
    fun tearDown() {
        unmockkObject(Base64)
        unmockkObject(FhirDateValidator)
        unmockkObject(HashUtil)
        unmockkObject(FhirAttachmentHelper)
        unmockkObject(AttachmentFactory)
        unmockkObject(MimeType)
    }

    @Test
    fun `Given, upload is called with Attachments, which has no Data, a AttachmentKey and a UserId, it returns a empty List`() {
        // Given
        every { attachment.data } returns null

        // When
        val subscriber = attachmentService.upload(listOf(attachment), attachmentKey, USER_ID).test().await()

        // Then
        val result = subscriber
                .assertNoErrors()
                .assertComplete()
                .assertValueCount(1)
                .values()[0]

        assertTrue(result.isEmpty())

        verify(exactly = 0) { fileService.uploadFile(
                attachmentKey,
                USER_ID,
                any()
        ) }
    }

    @Test
    fun `Given, upload is called with Attachments, a AttachmentKey and a UserId, it fails, if the upload fails`() {
        // Given
        val encodedData = "aaaaaa"
        val decodedData = ByteArray(23)
        val error = CoreRuntimeException.InternalFailure()
        val uploadedId = Single.error<String>(error)

        every { attachment.data } returns encodedData
        every { Base64.decode(encodedData) } returns decodedData
        every { fileService.uploadFile(
                attachmentKey,
                USER_ID,
                decodedData
        ) } returns uploadedId

        // When
        val subscriber = attachmentService.upload(listOf(attachment), attachmentKey, USER_ID)
                .test()
                .await()

        // Then
        subscriber
                .assertError(error)
                .assertNotComplete()

        verify(exactly = 1) { Base64.decode(encodedData) }
        verify(exactly = 1) { fileService.uploadFile(
                attachmentKey,
                USER_ID,
                any()
        ) }
    }

    @Test
    fun `Given, upload is called with Attachments, a AttachmentKey and a UserId, it returns a List with the uploaded ids`() {
        // Given
        val encodedData = "aaaaaa"
        val decodedData = ByteArray(23)
        val uploadedId = mockk<Single<String>>()
        val newId = "new"
        val additionalIds = listOf("a", "b")

        every { attachment.data } returns encodedData
        every { Base64.decode(encodedData) } returns decodedData
        every { fileService.uploadFile(
                attachmentKey,
                USER_ID,
                decodedData
        ) } returns uploadedId
        every { uploadedId.blockingGet() } returns newId
        every { thumbnailService.uploadDownscaledImages(
                attachmentKey,
                USER_ID,
                attachment,
                decodedData
        ) } returns additionalIds

        // When
        val subscriber = attachmentService.upload(listOf(attachment), attachmentKey, USER_ID).test().await()

        // Then
        val result = subscriber
                .assertNoErrors()
                .assertComplete()
                .assertValueCount(1)
                .values()[0]

        assertEquals(
                listOf(Pair(attachment, additionalIds)),
                result
        )

        verify(exactly = 1) { Base64.decode(encodedData) }
        verify(exactly = 1) { fileService.uploadFile(
                attachmentKey,
                USER_ID,
                decodedData
        ) }
        verify(exactly = 1) { uploadedId.blockingGet() }
        verify(exactly = 1) { thumbnailService.uploadDownscaledImages(
                attachmentKey,
                USER_ID,
                attachment,
                decodedData
        ) }
    }

    @Test
    fun `Given, delete is called with AttachmentId and a UserId, it delegates them to the fileService and returns its result`() {
        // Given
        val fileServiceResult = mockk<Single<Boolean>>()
        val attachmentId = "123"

        every { fileService.deleteFile(
                USER_ID,
                attachmentId
        ) } returns fileServiceResult

        // Then
        assertSame(
                fileServiceResult,
                attachmentService.delete(attachmentId, USER_ID)
        )

        verify(exactly = 1) { fileService.deleteFile(
                USER_ID,
                attachmentId
        ) }
    }

    @Test
    fun `Given, download is called with Attachments, a AttachmentKey and a UserID, it ignores Attachments, which have no valid ID`() {
        // Given
        val attachment = mockk<WrapperContract.Attachment>()

        every { attachment.id } returns null

        // When
        val subscriber = attachmentService.download(listOf(attachment), attachmentKey, USER_ID).test().await()

        // Then
        val result = subscriber
                .assertNoErrors()
                .assertComplete()
                .assertValueCount(1)
                .values()[0]

        assertTrue(result.isEmpty())
    }

    @Test
    fun `Given, download is called with Attachments, a AttachmentKey and a UserID, it fails, if the attachmentId does not contain a $SPLIT_CHAR, the Date is invalid and the newHash does not match the oldHash`() {
        // Given
        val attachment = mockk<WrapperContract.Attachment>()
        val id = "tralla"
        val decodedData = ByteArray(23)
        val oldHash = "12345"
        val newHash = "54321"


        every { attachment.id } returns id
        every { attachment.hash } returns oldHash
        every { FhirDateValidator.isInvalidateDate(attachment) } returns true
        every { fileService.downloadFile(
                attachmentKey,
                USER_ID,
                id
        ) } returns Single.just(decodedData)
        every { HashUtil.sha1(decodedData) } returns decodedData
        every { Base64.encodeToString(decodedData) } returns newHash

        // When
        val subscriber = attachmentService.download(listOf(attachment), attachmentKey, USER_ID).test().await()

        // Then
        subscriber
                .assertError(DataValidationException.InvalidAttachmentPayloadHash::class.java)
                .assertNotComplete()


        verify(exactly = 1) { FhirDateValidator.isInvalidateDate(attachment) }
        verify(exactly = 1) { fileService.downloadFile(
                attachmentKey,
                USER_ID,
                id
        ) }
        verify(exactly = 1) { HashUtil.sha1(decodedData) }
        verify(exactly = 1) { Base64.encodeToString(decodedData) }
    }

    @Test
    fun `Given, download is called with Attachments, a AttachmentKey and a UserID, it fails, it returns a list with the downloaded Attachments, if the id contains a SPLIT_CHAR`() {
        // Given
        val attachment = mockk<WrapperContract.Attachment>()
        val id = "tral${SPLIT_CHAR}la"
        val decodedData = ByteArray(23)
        val decodedDataSha = ByteArray(42)
        val encodedData = "aaaaaa"
        val oldHash = "12345"
        val newHash = "54321"


        every { attachment.id } returns id
        every { attachment.hash } returns oldHash
        every { FhirDateValidator.isInvalidateDate(attachment) } returns true
        every { fileService.downloadFile(
                attachmentKey,
                USER_ID,
                "la"//CharSeq after SPLIT_CHAR
        ) } returns Single.just(decodedData)
        every { HashUtil.sha1(decodedData) } returns decodedDataSha
        every { Base64.encodeToString(decodedData) } returns encodedData
        every { Base64.encodeToString(decodedDataSha) } returns newHash
        every { attachment.data = encodedData } returns Unit
        every { attachment.hash = newHash } returns Unit

        // When
        val subscriber = attachmentService.download(listOf(attachment), attachmentKey, USER_ID).test().await()

        // Then
        val result = subscriber
                .assertNoErrors()
                .assertComplete()
                .assertValueCount(1)
                .values()

        assertEquals(
                listOf(listOf(attachment)),
                result
        )


        verify(exactly = 0) { FhirDateValidator.isInvalidateDate(attachment) }
        verify(exactly = 1) { fileService.downloadFile(
                attachmentKey,
                USER_ID,
                "la"//CharSeq after SPLIT_CHAR
        ) }
        verify(exactly = 1) { HashUtil.sha1(decodedData) }
        verify(exactly = 1) { Base64.encodeToString(decodedDataSha) }
        verify(exactly = 1) { Base64.encodeToString(decodedData) }
        verify(exactly = 1) { attachment.data = encodedData }
        verify(exactly = 1) { attachment.hash = newHash }
    }

    @Test
    fun `Given, download is called with Attachments, a AttachmentKey and a UserID, it fails, it returns a list with the downloaded Attachments, if the date is valid`() {
        // Given
        val attachment = mockk<WrapperContract.Attachment>()
        val id = "tralla"
        val decodedData = ByteArray(23)
        val decodedDataSha = ByteArray(42)
        val encodedData = "aaaaaa"
        val oldHash = "12345"
        val newHash = "54321"


        every { attachment.id } returns id
        every { attachment.hash } returns oldHash
        every { FhirDateValidator.isInvalidateDate(attachment) } returns false
        every { fileService.downloadFile(
                attachmentKey,
                USER_ID,
                id//CharSeq after SPLIT_CHAR
        ) } returns Single.just(decodedData)
        every { HashUtil.sha1(decodedData) } returns decodedDataSha
        every { Base64.encodeToString(decodedData) } returns encodedData
        every { Base64.encodeToString(decodedDataSha) } returns newHash
        every { attachment.data = encodedData } returns Unit
        every { attachment.hash = newHash } returns Unit

        // When
        val subscriber = attachmentService.download(listOf(attachment), attachmentKey, USER_ID).test().await()

        // Then
        val result = subscriber
                .assertNoErrors()
                .assertComplete()
                .assertValueCount(1)
                .values()

        assertEquals(
                listOf(listOf(attachment)),
                result
        )


        verify(exactly = 1) { FhirDateValidator.isInvalidateDate(attachment) }
        verify(exactly = 1) { fileService.downloadFile(
                attachmentKey,
                USER_ID,
                id//CharSeq after SPLIT_CHAR
        ) }
        verify(exactly = 1) { HashUtil.sha1(decodedData) }
        verify(exactly = 1) { Base64.encodeToString(decodedDataSha) }
        verify(exactly = 1) { Base64.encodeToString(decodedData) }
        verify(exactly = 1) { attachment.data = encodedData }
        verify(exactly = 1) { attachment.hash = newHash }
    }

    @Test
    fun `Given, download is called with Attachments, a AttachmentKey and a UserID, it fails, it returns a list with the downloaded Attachments, if the newHash matches the oldHash`() {
        // Given
        val attachment = mockk<WrapperContract.Attachment>()
        val id = "tralla"
        val decodedData = ByteArray(23)
        val decodedDataSha = ByteArray(42)
        val encodedData = "aaaaaa"
        val oldHash = "54321"
        val newHash = "54321"


        every { attachment.id } returns id
        every { attachment.hash } returns oldHash
        every { FhirDateValidator.isInvalidateDate(attachment) } returns true
        every { fileService.downloadFile(
                attachmentKey,
                USER_ID,
                id//CharSeq after SPLIT_CHAR
        ) } returns Single.just(decodedData)
        every { HashUtil.sha1(decodedData) } returns decodedDataSha
        every { Base64.encodeToString(decodedData) } returns encodedData
        every { Base64.encodeToString(decodedDataSha) } returns newHash
        every { attachment.data = encodedData } returns Unit
        every { attachment.hash = newHash } returns Unit

        // When
        val subscriber = attachmentService.download(listOf(attachment), attachmentKey, USER_ID).test().await()

        // Then
        val result = subscriber
                .assertNoErrors()
                .assertComplete()
                .assertValueCount(1)
                .values()

        assertEquals(
                listOf(listOf(attachment)),
                result
        )


        verify(exactly = 1) { FhirDateValidator.isInvalidateDate(attachment) }
        verify(exactly = 1) { fileService.downloadFile(
                attachmentKey,
                USER_ID,
                id//CharSeq after SPLIT_CHAR
        ) }
        verify(exactly = 1) { HashUtil.sha1(decodedData) }
        verify(exactly = 1) { Base64.encodeToString(decodedDataSha) }
        verify(exactly = 1) { Base64.encodeToString(decodedData) }
        verify(exactly = 1) { attachment.data = encodedData }
        verify(exactly = 1) { attachment.hash = newHash }
    }

    @Test
    fun `Given, updateAttachmentMeta is called with Attachment, it does not update the meta data, if the Attachment has no data`() {
        // Given
        val attachment = mockk<WrapperContract.Attachment>()

        every { attachment.data } returns null

        // When
        attachmentService.updateAttachmentMeta(attachment)

        // Then
        verify(exactly = 0) { attachment.size = any() }
        verify(exactly = 0) { attachment.hash = any() }
        verify(exactly = 1) { attachment.data }
    }

    @Test
    fun `Given, updateAttachmentMeta is called with Attachment, it updates the meta data of the Attachment, if the Attachment has data`() {
        // Given
        val attachment = mockk<WrapperContract.Attachment>(relaxed = true)
        val dataSize = 23
        val decodedData = ByteArray(dataSize)
        val decodedShaData = ByteArray(42)
        val encodedData = "abcd"
        val data = "efg"

        every { attachment.data } returns data
        every { Base64.decode(data) } returns decodedData
        every { HashUtil.sha1(decodedData) } returns decodedShaData
        every { Base64.encodeToString(decodedShaData) } returns encodedData

        // When
        attachmentService.updateAttachmentMeta(attachment)

        // Then
        verify(exactly = 1) { attachment.size = dataSize }
        verify(exactly = 1) { attachment.hash = encodedData }
        verify(exactly = 2) { attachment.data }
    }

    @Test
    fun `Given, updateAttachmentMeta is called with Attachment, it returns the updated Attachment,`() {
        // Given
        val attachment = mockk<WrapperContract.Attachment>(relaxed = true)
        val dataSize = 23
        val decodedData = ByteArray(dataSize)
        val decodedShaData = ByteArray(42)
        val encodedData = "abcd"
        val data = "efg"

        every { attachment.data } returns data
        every { Base64.decode(data) } returns decodedData
        every { HashUtil.sha1(decodedData) } returns decodedShaData
        every { Base64.encodeToString(decodedShaData) } returns encodedData

        // Then
        assertSame(
                attachment,
                attachmentService.updateAttachmentMeta(attachment)
        )
    }

    @Test
    fun `Given, downloadAttachmentsFromStorage is called with AttachmentIds, a UserId, a DownloadType and a DecryptedRecord, it fails, if the there are no Attachments for the provided Resource`() {
        // Given
        val attachments = mockk<List<String>>()
        val decryptedRecord = mockk<NetworkRecordContract.DecryptedRecord>(relaxed = true)
        val resource = mockk<WrapperContract.Resource>()
        val rawResource = mockk<Any>()

        every { decryptedRecord.resource } returns resource
        every { resource.unwrap() } returns rawResource

        every { FhirAttachmentHelper.hasAttachment(rawResource) } returns false

        // When
        try {
            attachmentService.downloadAttachmentsFromStorage(
                    attachments,
                    USER_ID,
                    DownloadType.Full,
                    decryptedRecord

            )
            Assert.fail("Exception expected!")
        } catch (e: IllegalArgumentException) {

            // Then
            Truth.assertThat(e.javaClass).isEqualTo(IllegalArgumentException::class.java)
            Truth.assertThat(e.message).isEqualTo("Expected a record of a type that has attachment")
        }
    }

    @Test
    fun `Given, downloadAttachmentsFromStorage is called with AttachmentIds, a UserId, a DownloadType and a DecryptedRecord, it fails, if the amount of the given attachments does not match the amount of the validated Attachments`() {
        // Given
        val attachments = listOf(
                "yes"
        )
        val decryptedRecord = mockk<NetworkRecordContract.DecryptedRecord>(relaxed = true)
        val resource = mockk<WrapperContract.Resource>()
        val rawResource = mockk<Any>()

        val serviceAttachments = mutableListOf<Any>(
                "attachment"
        )

        val wrappedServiceAttachment = mockk<WrapperContract.Attachment>()

        every { decryptedRecord.resource } returns resource
        every { resource.unwrap() } returns rawResource

        every { wrappedServiceAttachment.id } returns "no"

        every { FhirAttachmentHelper.hasAttachment(rawResource) } returns true
        every { FhirAttachmentHelper.getAttachment(rawResource) } returns serviceAttachments

        every { AttachmentFactory.wrap(serviceAttachments[0]) } returns wrappedServiceAttachment

        // When
        try {
            attachmentService.downloadAttachmentsFromStorage(
                    attachments,
                    USER_ID,
                    DownloadType.Full,
                    decryptedRecord

            )
            Assert.fail("Exception expected!")
        } catch (e: D4LException) {

            // Then
            Truth.assertThat(e.javaClass).isEqualTo(DataValidationException.IdUsageViolation::class.java)
            Truth.assertThat(e.message).isEqualTo("Please provide correct attachment ids!")
        }
    }

    @Test
    fun `Given, downloadAttachmentsFromStorage is called with AttachmentIds, a UserId, a DownloadType and a DecryptedRecord, it downloads the requested Attachments`() {
        // Given
        val attachments = listOf(
                "yes"
        )
        val decryptedRecord = mockk<NetworkRecordContract.DecryptedRecord>(relaxed = true)
        val resource = mockk<WrapperContract.Resource>()
        val rawResource = mockk<Any>()
        val attachmentKey = mockk<GCKey>()
        val type = DownloadType.Full


        val serviceAttachments = mutableListOf<Any>(
                "attachment"
        )

        val ids = listOf("id")

        val wrappedServiceAttachment = mockk<WrapperContract.Attachment>()
        val validatedAttachments = listOf(wrappedServiceAttachment)

        val downloadedAttachment = mockk<WrapperContract.Attachment>()

        val spyedService = spyk(attachmentService)

        every { decryptedRecord.resource } returns resource
        every { decryptedRecord.attachmentsKey } returns attachmentKey
        every { resource.unwrap() } returns rawResource

        every { wrappedServiceAttachment.id } returns "yes"

        every { downloadedAttachment.id } returns "no split char"

        every { FhirAttachmentHelper.hasAttachment(rawResource) } returns true
        every { FhirAttachmentHelper.getAttachment(rawResource) } returns serviceAttachments
        every { FhirAttachmentHelper.getIdentifier(rawResource) } returns ids

        every { AttachmentFactory.wrap(serviceAttachments[0]) } returns wrappedServiceAttachment

        every { thumbnailService.setAttachmentIdForDownloadType(
                validatedAttachments,
                ids,
                type
        ) } returns Unit

        every { spyedService.download(
                validatedAttachments,
                attachmentKey,
                USER_ID
        ) } returns Single.just(listOf(downloadedAttachment))

        every { spyedService.updateAttachmentMeta(any()) } returns mockk()

        // When
        val subscriber = spyedService.downloadAttachmentsFromStorage(
                attachments,
                USER_ID,
                type,
                decryptedRecord

        ).test().await()
        val result = subscriber
                .assertNoErrors()
                .assertComplete()
                .assertValueCount(1)
                .values()[0]

        // Them

        Truth.assertThat(result).isEqualTo(listOf(downloadedAttachment))


        verify(exactly = 1) {
            spyedService.download(
                    any(),
                    attachmentKey,
                    USER_ID
            )
        }

        verify(exactly = 1 ) { thumbnailService.setAttachmentIdForDownloadType(
                validatedAttachments,
                ids,
                type
        ) }

        verify(exactly = 0) { spyedService.updateAttachmentMeta(any()) }
    }

    @Test
    fun `Given, downloadAttachmentsFromStorage is called with AttachmentIds, a UserId, a DownloadType and a DecryptedRecord, it downloads the requested Attachments and updates their meta`() {
        // Given
        val attachments = listOf(
                "yes"
        )
        val decryptedRecord = mockk<NetworkRecordContract.DecryptedRecord>(relaxed = true)
        val resource = mockk<WrapperContract.Resource>()
        val rawResource = mockk<Any>()
        val attachmentKey = mockk<GCKey>()
        val type = DownloadType.Full


        val serviceAttachments = mutableListOf<Any>(
                "attachment"
        )

        val ids = listOf("id")

        val wrappedServiceAttachment = mockk<WrapperContract.Attachment>()
        val validatedAttachments = listOf(wrappedServiceAttachment)

        val downloadedAttachment = mockk<WrapperContract.Attachment>()

        val spyedService = spyk(attachmentService)

        every { decryptedRecord.resource } returns resource
        every { decryptedRecord.attachmentsKey } returns attachmentKey
        every { resource.unwrap() } returns rawResource

        every { wrappedServiceAttachment.id } returns "yes"

        every { downloadedAttachment.id } returns "with ${SPLIT_CHAR} char"

        every { FhirAttachmentHelper.hasAttachment(rawResource) } returns true
        every { FhirAttachmentHelper.getAttachment(rawResource) } returns serviceAttachments
        every { FhirAttachmentHelper.getIdentifier(rawResource) } returns ids

        every { AttachmentFactory.wrap(serviceAttachments[0]) } returns wrappedServiceAttachment

        every { thumbnailService.setAttachmentIdForDownloadType(
                validatedAttachments,
                ids,
                type
        ) } returns Unit

        every { spyedService.download(
                validatedAttachments,
                attachmentKey,
                USER_ID
        ) } returns Single.just(listOf(downloadedAttachment))

        every { spyedService.updateAttachmentMeta(downloadedAttachment) } returns mockk()

        // When
        val subscriber = spyedService.downloadAttachmentsFromStorage(
                attachments,
                USER_ID,
                type,
                decryptedRecord

        ).test().await()
        val result = subscriber
                .assertNoErrors()
                .assertComplete()
                .assertValueCount(1)
                .values()[0]

        // Them

        Truth.assertThat(result).isEqualTo(listOf(downloadedAttachment))


        verify(exactly = 1) {
            spyedService.download(
                    any(),
                    attachmentKey,
                    USER_ID
            )
        }

        verify(exactly = 1 ) { thumbnailService.setAttachmentIdForDownloadType(
                validatedAttachments,
                ids,
                type
        ) }

        verify(exactly = 1) { spyedService.updateAttachmentMeta(downloadedAttachment) }
    }

    @Ignore("Out of memory during gradlew check")
    @Test
    @Throws(DataRestrictionException.UnsupportedFileType::class, DataRestrictionException.MaxDataSizeViolation::class)
    fun `Given, checkForUnsupportedData is called with a FhirResource, it fails with a MaxDataSizeViolation, if a Attachment exceeds the maximum FileSizeLimit`() {
        // Given
        val resource = mockk<WrapperContract.Resource>()
        val rawResource = mockk<Any>()

        val attachments =  mutableListOf<Any>("attachment")
        val wrappedAttachment = mockk<WrapperContract.Attachment>()

        val data = "bla"
        val decodedData = ByteArray(DATA_SIZE_MAX_BYTES+100000)

        every { resource.type } returns WrapperContract.Resource.TYPE.FHIR3
        every { resource.unwrap() } returns rawResource

        every { wrappedAttachment.data } returns data

        every { FhirAttachmentHelper.getAttachment(rawResource) } returns attachments
        every { AttachmentFactory.wrap("attachment") } returns wrappedAttachment

        every { Base64.decode(data) } returns decodedData
        every { MimeType.recognizeMimeType(decodedData) } returns MimeType.DCM


        // When
        try {
            attachmentService.checkDataRestrictions(resource)
            Assert.fail("Exception expected!")
        } catch (e: D4LException) {

            // Then
            Truth.assertThat(e.javaClass).isEqualTo(DataRestrictionException.MaxDataSizeViolation::class.java)
        }
    }

    @Test
    @Throws(DataRestrictionException.UnsupportedFileType::class, DataRestrictionException.MaxDataSizeViolation::class)
    fun `Given, checkForUnsupportedData is called with a FhirResource, it fails with a MaxDataSizeViolation, if the MimeType of a Attachment is unknown`() {
        // Given
        val resource = mockk<WrapperContract.Resource>()
        val rawResource = mockk<Any>()

        val attachments =  mutableListOf<Any>("attachment")
        val wrappedAttachment = mockk<WrapperContract.Attachment>()

        val data = "bla"
        val decodedData = ByteArray(12)

        every { resource.type } returns WrapperContract.Resource.TYPE.FHIR3
        every { resource.unwrap() } returns rawResource

        every { wrappedAttachment.data } returns data

        every { FhirAttachmentHelper.getAttachment(rawResource) } returns attachments
        every { AttachmentFactory.wrap("attachment") } returns wrappedAttachment

        every { Base64.decode(data) } returns decodedData
        every { MimeType.recognizeMimeType(decodedData) } returns MimeType.UNKNOWN


        // When
        try {
            attachmentService.checkDataRestrictions(resource)
            Assert.fail("Exception expected!")
        } catch (e: D4LException) {

            // Then
            Truth.assertThat(e.javaClass).isEqualTo(DataRestrictionException.UnsupportedFileType::class.java)
        }
    }

    @Test
    @Throws(DataRestrictionException.UnsupportedFileType::class, DataRestrictionException.MaxDataSizeViolation::class)
    fun `Given, checkForUnsupportedData is called a DataResource, it accepts`() {
        // Given
        val resource = mockk<WrapperContract.Resource>()

        every { resource.type } returns WrapperContract.Resource.TYPE.DATA

        attachmentService.checkDataRestrictions(null)

        assertTrue(true)
    }

    @Test
    @Throws(DataRestrictionException.UnsupportedFileType::class, DataRestrictionException.MaxDataSizeViolation::class)
    fun `Given, checkForUnsupportedData is called with null, it accepts`() {

        attachmentService.checkDataRestrictions(null)

        assertTrue(true)
    }

    @Test
    @Throws(DataRestrictionException.UnsupportedFileType::class, DataRestrictionException.MaxDataSizeViolation::class)
    fun `Given, checkForUnsupportedData is called with a FhirResource, it accepts, if all Attachments are in the boundaries`() {
        // Given
        val resource = mockk<WrapperContract.Resource>()
        val rawResource = mockk<Any>()

        val attachments =  mutableListOf<Any>("attachment")
        val wrappedAttachment = mockk<WrapperContract.Attachment>()

        val data = "bla"
        val decodedData = ByteArray(12)

        every { resource.type } returns WrapperContract.Resource.TYPE.FHIR3
        every { resource.unwrap() } returns rawResource

        every { wrappedAttachment.data } returns data

        every { FhirAttachmentHelper.getAttachment(rawResource) } returns attachments
        every { AttachmentFactory.wrap("attachment") } returns wrappedAttachment

        every { Base64.decode(data) } returns decodedData
        every { MimeType.recognizeMimeType(decodedData) } returns MimeType.DCM


        // When
        attachmentService.checkDataRestrictions(resource)
        assertTrue(true)
    }

    @Test
    fun `Given, extractUploadData is called with a non FhirResource, it returns null`() {
        // Given
        val resource = mockk<WrapperContract.Resource>()

        every { resource.type } returns WrapperContract.Resource.TYPE.DATA

        // When
        val data = attachmentService.extractUploadData(resource)
        // Then
        Truth.assertThat(data).isNull()
    }

    @Test
    fun `Given, extractUploadData is called with a FhirResource, it returns null, if the resource has no Attachments`() {
        // Given
        val resource = mockk<WrapperContract.Resource>()
        val rawResource = mockk<Any>()

        every { resource.type } returns WrapperContract.Resource.TYPE.FHIR3
        every { resource.unwrap() } returns rawResource

        every { FhirAttachmentHelper.getAttachment(rawResource) } returns mutableListOf()

        // When
        val data = attachmentService.extractUploadData(resource)
        // Then
        Truth.assertThat(data).isNull()
    }

    @Test
    fun `Given, extractUploadData is called with a FhirResource, it returns null, if the resource has only empty Attachments`() {
        // Given
        val resource = mockk<WrapperContract.Resource>()
        val rawResource = mockk<Any>()

        val attachments =  mutableListOf<Any>("attachment")
        val wrappedAttachment = mockk<WrapperContract.Attachment>()

        every { resource.type } returns WrapperContract.Resource.TYPE.FHIR3
        every { resource.unwrap() } returns rawResource

        every { wrappedAttachment.data } returns null

        every { FhirAttachmentHelper.getAttachment(rawResource) } returns attachments
        every { AttachmentFactory.wrap("attachment") } returns wrappedAttachment

        // When
        val data = attachmentService.extractUploadData(resource)
        // Then
        Truth.assertThat(data).isNull()
    }

    @Test
    fun `Given, extractUploadData is called with a FhirResource, it returns the data of the valid Attachments`() {
        // Given
        val resource = mockk<WrapperContract.Resource>()
        val rawResource = mockk<Any>()

        val attachments =  mutableListOf<Any>("attachment")
        val wrappedAttachment = mockk<WrapperContract.Attachment>()
        val attachmentData = "bla"

        every { resource.type } returns WrapperContract.Resource.TYPE.FHIR3
        every { resource.unwrap() } returns rawResource

        every { wrappedAttachment.data } returns attachmentData

        every { FhirAttachmentHelper.getAttachment(rawResource) } returns attachments
        every { AttachmentFactory.wrap("attachment") } returns wrappedAttachment

        // When
        val data = attachmentService.extractUploadData(resource)
        // Then
        Truth.assertThat(data).isEqualTo(hashMapOf(wrappedAttachment to attachmentData))
    }

    companion object {
        private const val USER_ID = "userId"
    }
}
