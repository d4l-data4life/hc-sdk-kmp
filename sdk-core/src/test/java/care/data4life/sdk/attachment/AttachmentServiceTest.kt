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
import care.data4life.sdk.lang.CoreRuntimeException
import care.data4life.sdk.lang.DataValidationException
import care.data4life.sdk.util.Base64
import care.data4life.sdk.util.HashUtil
import care.data4life.sdk.wrapper.WrapperContract
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import io.mockk.verify
import io.reactivex.Single
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito

class AttachmentServiceTest {
    private val attachmentKey = Mockito.mock(GCKey::class.java)
    private lateinit var fileService: FileContract.Service
    private lateinit var thumbnailService: ThumbnailContract.Service
    private lateinit var attachment: WrapperContract.Attachment
    private lateinit var attachmentService: AttachmentService  //SUT

    @Before
    fun setUp() {
        attachment = mockk(relaxed = true)
        fileService = mockk()
        thumbnailService = mockk()
        attachmentService = AttachmentService(fileService, thumbnailService)

        mockkObject(Base64)
        mockkObject(FhirDateValidator)
        mockkObject(HashUtil)
    }

    @After
    fun tearDown() {
        unmockkObject(Base64)
        unmockkObject(FhirDateValidator)
        unmockkObject(HashUtil)
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

    companion object {
        private const val USER_ID = "userId"
    }
}
