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
package care.data4life.sdk

import care.data4life.crypto.GCKey
import care.data4life.sdk.attachment.AttachmentContract
import care.data4life.sdk.attachment.ThumbnailService.Companion.SPLIT_CHAR
import care.data4life.sdk.call.Fhir4Record
import care.data4life.sdk.crypto.CryptoContract
import care.data4life.sdk.fhir.Fhir3Attachment
import care.data4life.sdk.fhir.Fhir3Identifier
import care.data4life.sdk.fhir.Fhir3Resource
import care.data4life.sdk.fhir.Fhir4Attachment
import care.data4life.sdk.fhir.Fhir4Identifier
import care.data4life.sdk.fhir.Fhir4Resource
import care.data4life.sdk.fhir.FhirContract
import care.data4life.sdk.lang.DataValidationException
import care.data4life.sdk.model.DownloadType
import care.data4life.sdk.model.Record
import care.data4life.sdk.model.RecordMapper
import care.data4life.sdk.network.NetworkingContract
import care.data4life.sdk.network.model.EncryptedRecord
import care.data4life.sdk.network.model.NetworkModelContract.DecryptedBaseRecord
import care.data4life.sdk.network.model.NetworkModelContract.DecryptedFhir3Record
import care.data4life.sdk.network.model.NetworkModelContract.DecryptedFhir4Record
import care.data4life.sdk.record.RecordContract
import care.data4life.sdk.tag.TaggingContract
import care.data4life.sdk.test.util.GenericTestDataProvider.ALIAS
import care.data4life.sdk.test.util.GenericTestDataProvider.ATTACHMENT_ID
import care.data4life.sdk.test.util.GenericTestDataProvider.PARTNER_ID
import care.data4life.sdk.test.util.GenericTestDataProvider.RECORD_ID
import care.data4life.sdk.test.util.GenericTestDataProvider.USER_ID
import care.data4life.sdk.wrapper.SdkAttachmentFactory
import care.data4life.sdk.wrapper.SdkFhirAttachmentHelper
import care.data4life.sdk.wrapper.WrapperContract
import io.mockk.Called
import io.mockk.Runs
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.spyk
import io.mockk.unmockkObject
import io.mockk.verify
import io.mockk.verifyOrder
import io.reactivex.Completable
import io.reactivex.Single
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame
import kotlin.test.assertTrue

class RecordServiceTest {
    private lateinit var recordService: RecordService
    private val apiService: NetworkingContract.Service = mockk()
    private val cryptoService: CryptoContract.Service = mockk()
    private val fhirService: FhirContract.Service = mockk()
    private val tagEncryptionService: TaggingContract.EncryptionService = mockk()
    private val taggingService: TaggingContract.Service = mockk()
    private val attachmentService: AttachmentContract.Service = mockk()
    private val errorHandler: SdkContract.ErrorHandler = mockk()

    @Before
    fun setUp() {
        clearAllMocks()

        recordService = spyk(
            RecordService(
                PARTNER_ID,
                ALIAS,
                apiService,
                tagEncryptionService,
                taggingService,
                fhirService,
                attachmentService,
                cryptoService,
                errorHandler,
                mockk()
            )
        )
    }

    @Test
    fun `It fulfils the RecordContract#Service interface`() {
        val service: Any = recordService
        assertTrue(service is RecordContract.Service)
    }

    @Test
    fun `Given, getValidHash is called with a WrappedAttachment, which contains data, it returns a hash`() {
        // Given
        val attachment: WrapperContract.Attachment = mockk()

        every { attachment.data } returns "dGVzdA==" // == test

        // When
        val hash = recordService.getValidHash(attachment)

        //
        assertEquals(
            actual = hash,
            expected = "qUqP5cyxm6YcTAhz05Hph5gvu9M="
        )
    }

    @Test
    fun `Given, updateAttachmentMeta is called, with a Fhir3Attachment, it updates its meta information`() {
        // Given
        val attachment: WrapperContract.Attachment = mockk()

        every { attachment.data } returns "dGVzdA==" // == test
        every { attachment.size = "test".toByteArray().size } just Runs
        every { attachment.hash = "qUqP5cyxm6YcTAhz05Hph5gvu9M=" } just Runs

        // When
        recordService.updateAttachmentMeta(attachment)

        // Then
        verify(exactly = 1) { attachment.size = "test".toByteArray().size }
        verify(exactly = 1) { attachment.hash = "qUqP5cyxm6YcTAhz05Hph5gvu9M=" }
    }

    @Test
    fun `Given, updateFhirResourceIdentifier is called, with a Fhir3Resource, and a list of pairs of Attachments to String, which is empty, it appends an new Identifier`() {
        mockkObject(SdkFhirAttachmentHelper)
        // Given
        val attachment = mockk<WrapperContract.Attachment>()
        val resource = mockk<Fhir3Resource>(relaxed = true)

        every { attachment.id } returns "something"

        every {
            SdkFhirAttachmentHelper.appendIdentifier(
                resource,
                "d4l_f_p_t#something",
                PARTNER_ID
            )
        } returns mockk()

        // When
        recordService.updateFhirResourceIdentifier(
            resource,
            listOf<Pair<WrapperContract.Attachment, List<String>?>>(attachment to listOf())
        )

        verify(exactly = 1) {
            SdkFhirAttachmentHelper.appendIdentifier(
                resource,
                "d4l_f_p_t#something",
                PARTNER_ID
            )
        }

        unmockkObject(SdkFhirAttachmentHelper)
    }

    @Test
    fun `Given, updateFhirResourceIdentifier is called, with a Fhir3Resource, and a list of pairs of Attachments to String, which is not empty, it amends the Strings and appends an new Identifier`() {
        mockkObject(SdkFhirAttachmentHelper)
        // Given
        val attachment = mockk<WrapperContract.Attachment>()
        val resource = mockk<Fhir3Resource>(relaxed = true)
        val amendments = listOf(
            "tomato",
            "soup"
        )

        every { attachment.id } returns "something"

        every {
            SdkFhirAttachmentHelper.appendIdentifier(
                resource,
                "d4l_f_p_t#something#tomato#soup",
                PARTNER_ID
            )
        } returns mockk()

        // When
        recordService.updateFhirResourceIdentifier(
            resource,
            listOf<Pair<WrapperContract.Attachment, List<String>?>>(attachment to amendments)
        )

        verify(exactly = 1) {
            SdkFhirAttachmentHelper.appendIdentifier(
                resource,
                "d4l_f_p_t#something#tomato#soup",
                PARTNER_ID
            )
        }

        unmockkObject(SdkFhirAttachmentHelper)
    }

    @Test
    fun `Given, updateFhirResourceIdentifier is called, with a Fhir3Resource, and a list of pairs of Attachments to String, which is null, it ignores it`() {
        mockkObject(SdkFhirAttachmentHelper)
        // Given
        val attachment = mockk<WrapperContract.Attachment>()
        val resource = mockk<Fhir3Resource>(relaxed = true)
        val amendments = null

        every { attachment.id } returns "something"

        // When
        recordService.updateFhirResourceIdentifier(
            resource,
            listOf<Pair<WrapperContract.Attachment, List<String>?>>(attachment to amendments)
        )

        verify(exactly = 0) {
            SdkFhirAttachmentHelper.appendIdentifier(
                resource,
                any(),
                PARTNER_ID
            )
        }

        unmockkObject(SdkFhirAttachmentHelper)
    }

    @Test
    fun `Given, updateFhirResourceIdentifier is called, with a Fhir4Resource, and a list of pairs of Attachments to String, which is empty, it appends an new Identifier`() {
        mockkObject(SdkFhirAttachmentHelper)
        // Given
        val attachment = mockk<WrapperContract.Attachment>()
        val resource = mockk<Fhir4Resource>(relaxed = true)

        every { attachment.id } returns "something"

        every {
            SdkFhirAttachmentHelper.appendIdentifier(
                resource,
                "d4l_f_p_t#something",
                PARTNER_ID
            )
        } returns mockk()

        // When
        recordService.updateFhirResourceIdentifier(
            resource,
            listOf<Pair<WrapperContract.Attachment, List<String>?>>(attachment to listOf())
        )

        verify(exactly = 1) {
            SdkFhirAttachmentHelper.appendIdentifier(
                resource,
                "d4l_f_p_t#something",
                PARTNER_ID
            )
        }

        unmockkObject(SdkFhirAttachmentHelper)
    }

    @Test
    fun `Given, updateFhirResourceIdentifier is called, with a Fhir4Resource, and a list of pairs of Attachments to String, which is not empty, it amends the Strings and appends an new Identifier`() {
        mockkObject(SdkFhirAttachmentHelper)
        // Given
        val attachment = mockk<WrapperContract.Attachment>()
        val resource = mockk<Fhir4Resource>(relaxed = true)
        val amendments = listOf(
            "tomato",
            "soup"
        )

        every { attachment.id } returns "something"

        every {
            SdkFhirAttachmentHelper.appendIdentifier(
                resource,
                "d4l_f_p_t#something#tomato#soup",
                PARTNER_ID
            )
        } returns mockk()

        // When
        recordService.updateFhirResourceIdentifier(
            resource,
            listOf<Pair<WrapperContract.Attachment, List<String>?>>(attachment to amendments)
        )

        verify(exactly = 1) {
            SdkFhirAttachmentHelper.appendIdentifier(
                resource,
                "d4l_f_p_t#something#tomato#soup",
                PARTNER_ID
            )
        }

        unmockkObject(SdkFhirAttachmentHelper)
    }

    @Test
    fun `Given, updateFhirResourceIdentifier is called, with a Fhir4Resource, and a list of pairs of Attachments to String, which is null, it ignores it`() {
        mockkObject(SdkFhirAttachmentHelper)
        // Given
        val attachment = mockk<WrapperContract.Attachment>()
        val resource = mockk<Fhir4Resource>(relaxed = true)
        val amendments = null

        every { attachment.id } returns "something"

        // When
        recordService.updateFhirResourceIdentifier(
            resource,
            listOf<Pair<WrapperContract.Attachment, List<String>?>>(attachment to amendments)
        )

        verify(exactly = 0) {
            SdkFhirAttachmentHelper.appendIdentifier(
                resource,
                any(),
                PARTNER_ID
            )
        }

        unmockkObject(SdkFhirAttachmentHelper)
    }

    @Test
    fun ` Given, deleteRecord is called with a UserId and a RecordId, it delegates it to apiService and returns the result`() {
        // Given
        val expected: Completable = mockk()

        every { apiService.deleteRecord(ALIAS, RECORD_ID, USER_ID) } returns expected

        // When
        val actual = recordService.deleteRecord(userId = USER_ID, recordId = RECORD_ID)

        // Then
        assertSame(
            actual = actual,
            expected = expected
        )

        verify(exactly = 1) { apiService.deleteRecord(ALIAS, RECORD_ID, USER_ID) }
    }

    @Test
    fun ` Given, deleteRecords is called with a UserId and a list of RecordIds, it delegates it to apiService and returns the result`() {
        // Given
        val expected: Completable = Completable.complete()
        val ids = listOf(
            "1",
            "2"
        )

        every { apiService.deleteRecord(ALIAS, or(ids[0], ids[1]), USER_ID) } returns expected

        // When
        val subscriber = recordService.deleteRecords(userId = USER_ID, recordIds = ids)
            .test()
            .await()

        // Then
        val actual = subscriber
            .assertNoErrors()
            .assertComplete()
            .assertValueCount(1)
            .values()[0]

        assertEquals(
            actual = actual.successfulDeletes,
            expected = ids
        )

        assertEquals(
            actual = actual.failedDeletes,
            expected = listOf()
        )

        verify(exactly = 2) { apiService.deleteRecord(ALIAS, or(ids[0], ids[1]), USER_ID) }
    }

    @Test
    fun `Given, deleteAttachment is called, with an AttachmentId and a UserId, it delegates it to the AttachmentService and returns its result`() {
        // Given
        val expected: Single<Boolean> = mockk()

        every { attachmentService.delete(ATTACHMENT_ID, USER_ID) } returns expected
        // When
        val actual = recordService.deleteAttachment(ATTACHMENT_ID, USER_ID)

        // Then
        assertSame(
            actual = actual,
            expected = expected
        )
    }

    // FHIR3 - downloadAttachmentsFromStorage
    @Test
    fun `Given, downloadAttachmentsFromStorage is called, with a list of AttachmentsIds, a DecryptedRecord, which contains a Fhir3Resource, it fails, if it not capable of having Attachments`() {
        mockkObject(SdkFhirAttachmentHelper)
        // Given
        val attachmentIds: List<String> = mockk()
        val resource: Fhir3Resource = mockk()
        val decryptedRecord: DecryptedFhir3Record<Fhir3Resource> = mockk()

        every { decryptedRecord.resource } returns resource

        every { SdkFhirAttachmentHelper.hasAttachment(resource) } returns false
        every { SdkFhirAttachmentHelper.getAttachment(any()) } returns mockk()

        // Then
        val error = assertFailsWith<IllegalArgumentException> {
            // When
            recordService.downloadAttachmentsFromStorage<Fhir3Resource, Fhir3Attachment>(
                attachmentIds,
                USER_ID,
                DownloadType.Full,
                decryptedRecord
            )
        }

        assertEquals(
            actual = error.message,
            expected = "Expected a record of a type that has attachment"
        )

        verify { SdkFhirAttachmentHelper.getAttachment(resource)!!.wasNot(Called) }
        unmockkObject(SdkFhirAttachmentHelper)
    }

    @Test
    fun `Given, downloadAttachmentsFromStorage is called, with a list of AttachmentsIds, a DecryptedRecord, which contains a Fhir3Resource, it fails, if there are no actual Attachments`() {
        mockkObject(SdkFhirAttachmentHelper)
        // Given
        val attachmentIds: List<String> = listOf(
            "abc",
            "bcd"
        )
        val resource: Fhir3Resource = mockk()
        val decryptedRecord: DecryptedFhir3Record<Fhir3Resource> = mockk()

        every { decryptedRecord.resource } returns resource

        every { SdkFhirAttachmentHelper.hasAttachment(resource) } returns true
        every { SdkFhirAttachmentHelper.getAttachment(resource) } returns null

        // Then
        val error = assertFailsWith<DataValidationException.IdUsageViolation> {
            // When
            recordService.downloadAttachmentsFromStorage<Fhir3Resource, Fhir3Attachment>(
                attachmentIds,
                USER_ID,
                DownloadType.Full,
                decryptedRecord
            )
        }

        assertEquals(
            actual = error.message,
            expected = "Please provide correct attachment ids!"
        )

        unmockkObject(SdkFhirAttachmentHelper)
    }

    @Test
    fun `Given, downloadAttachmentsFromStorage is called, with a list of AttachmentsIds, a DecryptedRecord, which contains a Fhir3Resource, it fails, if the AttachmentIDs does not match the attachments of the record`() {
        mockkObject(SdkFhirAttachmentHelper)
        // Given
        val attachmentIds: List<String> = listOf(
            "abc",
            "bcd"
        )
        val attachments: MutableList<Fhir3Attachment> = mutableListOf(
            mockk(relaxed = true)
        )
        val resource: Fhir3Resource = mockk()
        val decryptedRecord: DecryptedFhir3Record<Fhir3Resource> = mockk()

        every { decryptedRecord.resource } returns resource

        every { SdkFhirAttachmentHelper.hasAttachment(resource) } returns true
        every { SdkFhirAttachmentHelper.getAttachment(resource) } returns attachments as MutableList<Any?>

        // Then
        val error = assertFailsWith<DataValidationException.IdUsageViolation> {
            // When
            recordService.downloadAttachmentsFromStorage<Fhir3Resource, Fhir3Attachment>(
                attachmentIds,
                USER_ID,
                DownloadType.Full,
                decryptedRecord
            )
        }

        assertEquals(
            actual = error.message,
            expected = "Please provide correct attachment ids!"
        )

        unmockkObject(SdkFhirAttachmentHelper)
    }

    @Test
    fun `Given, downloadAttachmentsFromStorage is called, with a list of AttachmentsIds, a DecryptedRecord, which contains a Fhir3Resource, it fails, while filter the Attachments by their id`() {
        mockkObject(SdkFhirAttachmentHelper)
        // Given
        val attachmentIds: List<String> = listOf(
            "abc",
            "bcd"
        )
        val attachments: MutableList<Fhir3Attachment> = mutableListOf(
            mockk(),
            mockk()
        )
        val resource: Fhir3Resource = mockk()
        val decryptedRecord: DecryptedFhir3Record<Fhir3Resource> = mockk()
        val wrappedAttachments: List<WrapperContract.Attachment> = listOf(
            mockk(),
            mockk()
        )

        every { wrappedAttachments[0].id } returns attachmentIds[0]
        every { wrappedAttachments[1].id } returns attachmentIds[1]

        every { decryptedRecord.resource } returns resource

        every { SdkFhirAttachmentHelper.hasAttachment(resource) } returns true
        every { SdkFhirAttachmentHelper.getAttachment(resource) } returns attachments as MutableList<Any?>

        // Then
        val error = assertFailsWith<DataValidationException.IdUsageViolation> {
            // When
            recordService.downloadAttachmentsFromStorage<Fhir3Resource, Fhir3Attachment>(
                attachmentIds,
                USER_ID,
                DownloadType.Full,
                decryptedRecord
            )
        }

        assertEquals(
            actual = error.message,
            expected = "Please provide correct attachment ids!"
        )

        unmockkObject(SdkFhirAttachmentHelper)
    }

    @Test
    fun `Given, downloadAttachmentsFromStorage is called, with a list of AttachmentsIds, a DecryptedRecord, which contains a Fhir3Resource, it downloads the requested Attachments`() {
        mockkObject(SdkFhirAttachmentHelper)
        mockkObject(SdkAttachmentFactory)
        // Given
        val attachmentKey: GCKey = mockk()
        val attachmentIds: List<String> = listOf(
            "abc",
            "bcd"
        )
        val attachments: MutableList<Fhir3Attachment> = mutableListOf(
            mockk(),
            mockk()
        )
        val resource: Fhir3Resource = mockk()
        val decryptedRecord: DecryptedFhir3Record<Fhir3Resource> = mockk()
        val identifiers: List<Fhir3Identifier> = mockk()
        val wrappedAttachments: List<WrapperContract.Attachment> = listOf(
            mockk(),
            mockk()
        )
        val downloadedWrappedAttachments: List<WrapperContract.Attachment> = listOf(
            mockk(),
            mockk()
        )
        val downloadedAttachments: List<Fhir3Attachment> = listOf(
            spyk(),
            spyk()
        )

        every { wrappedAttachments[0].id } returns attachmentIds[0]
        every { wrappedAttachments[1].id } returns attachmentIds[1]

        every { decryptedRecord.resource } returns resource
        every { decryptedRecord.attachmentsKey } returns attachmentKey

        every { downloadedWrappedAttachments[0].unwrap<Fhir3Attachment>() } returns downloadedAttachments[0]
        every { downloadedWrappedAttachments[1].unwrap<Fhir3Attachment>() } returns downloadedAttachments[1]

        every { downloadedWrappedAttachments[0].id } returns "abc"
        every { downloadedWrappedAttachments[1].id } returns "cdf"

        every { SdkFhirAttachmentHelper.hasAttachment(resource) } returns true
        every { SdkFhirAttachmentHelper.getAttachment(resource) } returns attachments as MutableList<Any?>
        every { SdkFhirAttachmentHelper.getIdentifier(resource) } returns identifiers
        every { SdkAttachmentFactory.wrap(or(attachments[0], attachments[1])) } returnsMany wrappedAttachments
        every {
            recordService.setAttachmentIdForDownloadType(
                wrappedAttachments,
                identifiers,
                DownloadType.Full
            )
        } just Runs

        every {
            attachmentService.download(
                wrappedAttachments,
                attachmentKey,
                USER_ID
            )
        } returns Single.just(downloadedWrappedAttachments)

        // When
        val subscriber = recordService.downloadAttachmentsFromStorage<Fhir3Resource, Fhir3Attachment>(
            attachmentIds,
            USER_ID,
            DownloadType.Full,
            decryptedRecord
        ).test().await()

        // Then
        val result = subscriber
            .assertNoErrors()
            .assertComplete()
            .values()[0]

        assertEquals(
            actual = result.size,
            expected = 2
        )
        assertEquals(
            actual = result,
            expected = downloadedAttachments
        )

        verifyOrder {
            recordService.setAttachmentIdForDownloadType(
                wrappedAttachments,
                identifiers,
                DownloadType.Full
            )
            attachmentService.download(
                wrappedAttachments,
                attachmentKey,
                USER_ID
            )
        }
        verify { recordService.updateAttachmentMeta(any()) wasNot Called }

        unmockkObject(SdkFhirAttachmentHelper)
        unmockkObject(SdkAttachmentFactory)
    }

    @Test
    fun `Given, downloadAttachmentsFromStorage is called, with a list of AttachmentsIds, a DecryptedRecord, which contains a Fhir3Resource, it downloads the requested Attachments, while updating their metas`() {
        mockkObject(SdkFhirAttachmentHelper)
        mockkObject(SdkAttachmentFactory)
        // Given
        val attachmentKey: GCKey = mockk()
        val attachmentIds: List<String> = listOf(
            "abc",
            "bcd"
        )
        val attachments: MutableList<Fhir3Attachment> = mutableListOf(
            mockk(),
            mockk()
        )
        val resource: Fhir3Resource = mockk()
        val decryptedRecord: DecryptedFhir3Record<Fhir3Resource> = mockk()
        val identifiers: List<Fhir3Identifier> = mockk()
        val wrappedAttachments: List<WrapperContract.Attachment> = listOf(
            mockk(),
            mockk()
        )
        val downloadedWrappedAttachments: List<WrapperContract.Attachment> = listOf(
            mockk(),
            mockk()
        )
        val downloadedAttachments: List<Fhir3Attachment> = listOf(
            spyk(),
            spyk()
        )
        val downloadedAttachmentsIds = listOf(
            "tomato${SPLIT_CHAR}soup",
            "potato${SPLIT_CHAR}soup"
        )

        every { wrappedAttachments[0].id } returns attachmentIds[0]
        every { wrappedAttachments[1].id } returns attachmentIds[1]

        every { decryptedRecord.resource } returns resource
        every { decryptedRecord.attachmentsKey } returns attachmentKey

        every { downloadedWrappedAttachments[0].unwrap<Fhir3Attachment>() } returns downloadedAttachments[0]
        every { downloadedWrappedAttachments[1].unwrap<Fhir3Attachment>() } returns downloadedAttachments[1]

        every { downloadedWrappedAttachments[0].id } returns downloadedAttachmentsIds[0]
        every { downloadedWrappedAttachments[1].id } returns downloadedAttachmentsIds[1]

        every { SdkFhirAttachmentHelper.hasAttachment(resource) } returns true
        every { SdkFhirAttachmentHelper.getAttachment(resource) } returns attachments as MutableList<Any?>
        every { SdkFhirAttachmentHelper.getIdentifier(resource) } returns identifiers
        every { SdkAttachmentFactory.wrap(or(attachments[0], attachments[1])) } returnsMany wrappedAttachments
        every {
            recordService.setAttachmentIdForDownloadType(
                wrappedAttachments,
                identifiers,
                DownloadType.Full
            )
        } just Runs

        every {
            attachmentService.download(
                wrappedAttachments,
                attachmentKey,
                USER_ID
            )
        } returns Single.just(downloadedWrappedAttachments)

        every {
            recordService.updateAttachmentMeta(downloadedWrappedAttachments[0])
        } returns downloadedWrappedAttachments[0]

        every {
            recordService.updateAttachmentMeta(downloadedWrappedAttachments[1])
        } returns downloadedWrappedAttachments[1]

        // When
        val subscriber = recordService.downloadAttachmentsFromStorage<Fhir3Resource, Fhir3Attachment>(
            attachmentIds,
            USER_ID,
            DownloadType.Full,
            decryptedRecord
        ).test().await()

        // Then
        val result = subscriber
            .assertNoErrors()
            .assertComplete()
            .values()[0]

        assertEquals(
            actual = result.size,
            expected = 2
        )
        assertEquals(
            actual = result,
            expected = downloadedAttachments
        )

        verifyOrder {
            recordService.setAttachmentIdForDownloadType(
                wrappedAttachments,
                identifiers,
                DownloadType.Full
            )
            attachmentService.download(
                wrappedAttachments,
                attachmentKey,
                USER_ID
            )
            recordService.updateAttachmentMeta(downloadedWrappedAttachments[0])
            recordService.updateAttachmentMeta(downloadedWrappedAttachments[1])
        }

        unmockkObject(SdkFhirAttachmentHelper)
        unmockkObject(SdkAttachmentFactory)
    }

    // FHIR4 - downloadAttachmentsFromStorage
    @Test
    fun `Given, downloadAttachmentsFromStorage is called, with a list of AttachmentsIds, a DecryptedRecord, which contains a Fhir4Resource, it fails, if it not capable of having Attachments`() {
        mockkObject(SdkFhirAttachmentHelper)
        // Given
        val attachmentIds: List<String> = mockk()
        val resource: Fhir4Resource = mockk()
        val decryptedRecord: DecryptedFhir4Record<Fhir4Resource> = mockk()

        every { decryptedRecord.resource } returns resource

        every { SdkFhirAttachmentHelper.hasAttachment(resource) } returns false
        every { SdkFhirAttachmentHelper.getAttachment(any()) } returns mockk()

        // Then
        val error = assertFailsWith<IllegalArgumentException> {
            // When
            recordService.downloadAttachmentsFromStorage<Fhir4Resource, Fhir4Attachment>(
                attachmentIds,
                USER_ID,
                DownloadType.Full,
                decryptedRecord
            )
        }

        assertEquals(
            actual = error.message,
            expected = "Expected a record of a type that has attachment"
        )

        verify { SdkFhirAttachmentHelper.getAttachment(resource)!!.wasNot(Called) }
        unmockkObject(SdkFhirAttachmentHelper)
    }

    @Test
    fun `Given, downloadAttachmentsFromStorage is called, with a list of AttachmentsIds, a DecryptedRecord, which contains a Fhir4Resource, it fails, if there are no actual Attachments`() {
        mockkObject(SdkFhirAttachmentHelper)
        // Given
        val attachmentIds: List<String> = listOf(
            "abc",
            "bcd"
        )
        val resource: Fhir4Resource = mockk()
        val decryptedRecord: DecryptedFhir4Record<Fhir4Resource> = mockk()

        every { decryptedRecord.resource } returns resource

        every { SdkFhirAttachmentHelper.hasAttachment(resource) } returns true
        every { SdkFhirAttachmentHelper.getAttachment(resource) } returns null

        // Then
        val error = assertFailsWith<DataValidationException.IdUsageViolation> {
            // When
            recordService.downloadAttachmentsFromStorage<Fhir4Resource, Fhir4Attachment>(
                attachmentIds,
                USER_ID,
                DownloadType.Full,
                decryptedRecord
            )
        }

        assertEquals(
            actual = error.message,
            expected = "Please provide correct attachment ids!"
        )

        unmockkObject(SdkFhirAttachmentHelper)
    }

    @Test
    fun `Given, downloadAttachmentsFromStorage is called, with a list of AttachmentsIds, a DecryptedRecord, which contains a Fhir4Resource, it fails, if the AttachmentIDs does not match the attachments of the record`() {
        mockkObject(SdkFhirAttachmentHelper)
        // Given
        val attachmentIds: List<String> = listOf(
            "abc",
            "bcd"
        )
        val attachments: MutableList<Fhir4Attachment> = mutableListOf(
            mockk(relaxed = true)
        )
        val resource: Fhir4Resource = mockk()
        val decryptedRecord: DecryptedFhir4Record<Fhir4Resource> = mockk()

        every { decryptedRecord.resource } returns resource

        every { SdkFhirAttachmentHelper.hasAttachment(resource) } returns true
        every { SdkFhirAttachmentHelper.getAttachment(resource) } returns attachments as MutableList<Any?>

        // Then
        val error = assertFailsWith<DataValidationException.IdUsageViolation> {
            // When
            recordService.downloadAttachmentsFromStorage<Fhir4Resource, Fhir4Attachment>(
                attachmentIds,
                USER_ID,
                DownloadType.Full,
                decryptedRecord
            )
        }

        assertEquals(
            actual = error.message,
            expected = "Please provide correct attachment ids!"
        )

        unmockkObject(SdkFhirAttachmentHelper)
    }

    @Test
    fun `Given, downloadAttachmentsFromStorage is called, with a list of AttachmentsIds, a DecryptedRecord, which contains a Fhir4Resource, it fails, while filter the Attachments by their id`() {
        mockkObject(SdkFhirAttachmentHelper)
        // Given
        val attachmentIds: List<String> = listOf(
            "abc",
            "bcd"
        )
        val attachments: MutableList<Fhir4Attachment> = mutableListOf(
            mockk(),
            mockk()
        )
        val resource: Fhir4Resource = mockk()
        val decryptedRecord: DecryptedFhir4Record<Fhir4Resource> = mockk()
        val wrappedAttachments: List<WrapperContract.Attachment> = listOf(
            mockk(),
            mockk()
        )

        every { wrappedAttachments[0].id } returns attachmentIds[0]
        every { wrappedAttachments[1].id } returns attachmentIds[1]

        every { decryptedRecord.resource } returns resource

        every { SdkFhirAttachmentHelper.hasAttachment(resource) } returns true
        every { SdkFhirAttachmentHelper.getAttachment(resource) } returns attachments as MutableList<Any?>

        // Then
        val error = assertFailsWith<DataValidationException.IdUsageViolation> {
            // When
            recordService.downloadAttachmentsFromStorage<Fhir4Resource, Fhir4Attachment>(
                attachmentIds,
                USER_ID,
                DownloadType.Full,
                decryptedRecord
            )
        }

        assertEquals(
            actual = error.message,
            expected = "Please provide correct attachment ids!"
        )

        unmockkObject(SdkFhirAttachmentHelper)
    }

    @Test
    fun `Given, downloadAttachmentsFromStorage is called, with a list of AttachmentsIds, a DecryptedRecord, which contains a Fhir4Resource, it downloads the requested Attachments`() {
        mockkObject(SdkFhirAttachmentHelper)
        mockkObject(SdkAttachmentFactory)
        // Given
        val attachmentKey: GCKey = mockk()
        val attachmentIds: List<String> = listOf(
            "abc",
            "bcd"
        )
        val attachments: MutableList<Fhir4Attachment> = mutableListOf(
            mockk(),
            mockk()
        )
        val resource: Fhir4Resource = mockk()
        val decryptedRecord: DecryptedFhir4Record<Fhir4Resource> = mockk()
        val identifiers: List<Fhir4Identifier> = mockk()
        val wrappedAttachments: List<WrapperContract.Attachment> = listOf(
            mockk(),
            mockk()
        )
        val downloadedWrappedAttachments: List<WrapperContract.Attachment> = listOf(
            mockk(),
            mockk()
        )
        val downloadedAttachments: List<Fhir4Attachment> = listOf(
            spyk(),
            spyk()
        )

        every { wrappedAttachments[0].id } returns attachmentIds[0]
        every { wrappedAttachments[1].id } returns attachmentIds[1]

        every { decryptedRecord.resource } returns resource
        every { decryptedRecord.attachmentsKey } returns attachmentKey

        every { downloadedWrappedAttachments[0].unwrap<Fhir4Attachment>() } returns downloadedAttachments[0]
        every { downloadedWrappedAttachments[1].unwrap<Fhir4Attachment>() } returns downloadedAttachments[1]

        every { downloadedWrappedAttachments[0].id } returns "abc"
        every { downloadedWrappedAttachments[1].id } returns "cdf"

        every { SdkFhirAttachmentHelper.hasAttachment(resource) } returns true
        every { SdkFhirAttachmentHelper.getAttachment(resource) } returns attachments as MutableList<Any?>
        every { SdkFhirAttachmentHelper.getIdentifier(resource) } returns identifiers
        every { SdkAttachmentFactory.wrap(or(attachments[0], attachments[1])) } returnsMany wrappedAttachments
        every {
            recordService.setAttachmentIdForDownloadType(
                wrappedAttachments,
                identifiers,
                DownloadType.Full
            )
        } just Runs

        every {
            attachmentService.download(
                wrappedAttachments,
                attachmentKey,
                USER_ID
            )
        } returns Single.just(downloadedWrappedAttachments)

        // When
        val subscriber = recordService.downloadAttachmentsFromStorage<Fhir4Resource, Fhir4Attachment>(
            attachmentIds,
            USER_ID,
            DownloadType.Full,
            decryptedRecord
        ).test().await()

        // Then
        val result = subscriber
            .assertNoErrors()
            .assertComplete()
            .values()[0]

        assertEquals(
            actual = result.size,
            expected = 2
        )
        assertEquals(
            actual = result,
            expected = downloadedAttachments
        )

        verifyOrder {
            recordService.setAttachmentIdForDownloadType(
                wrappedAttachments,
                identifiers,
                DownloadType.Full
            )
            attachmentService.download(
                wrappedAttachments,
                attachmentKey,
                USER_ID
            )
        }
        verify { recordService.updateAttachmentMeta(any()) wasNot Called }

        unmockkObject(SdkFhirAttachmentHelper)
        unmockkObject(SdkAttachmentFactory)
    }

    @Test
    fun `Given, downloadAttachmentsFromStorage is called, with a list of AttachmentsIds, a DecryptedRecord, which contains a Fhir4Resource, it downloads the requested Attachments, while updating their metas`() {
        mockkObject(SdkFhirAttachmentHelper)
        mockkObject(SdkAttachmentFactory)
        // Given
        val attachmentKey: GCKey = mockk()
        val attachmentIds: List<String> = listOf(
            "abc",
            "bcd"
        )
        val attachments: MutableList<Fhir4Attachment> = mutableListOf(
            mockk(),
            mockk()
        )
        val resource: Fhir4Resource = mockk()
        val decryptedRecord: DecryptedFhir4Record<Fhir4Resource> = mockk()
        val identifiers: List<Fhir4Identifier> = mockk()
        val wrappedAttachments: List<WrapperContract.Attachment> = listOf(
            mockk(),
            mockk()
        )
        val downloadedWrappedAttachments: List<WrapperContract.Attachment> = listOf(
            mockk(),
            mockk()
        )
        val downloadedAttachments: List<Fhir4Attachment> = listOf(
            spyk(),
            spyk()
        )
        val downloadedAttachmentsIds = listOf(
            "tomato${SPLIT_CHAR}soup",
            "potato${SPLIT_CHAR}soup"
        )

        every { wrappedAttachments[0].id } returns attachmentIds[0]
        every { wrappedAttachments[1].id } returns attachmentIds[1]

        every { decryptedRecord.resource } returns resource
        every { decryptedRecord.attachmentsKey } returns attachmentKey

        every { downloadedWrappedAttachments[0].unwrap<Fhir4Attachment>() } returns downloadedAttachments[0]
        every { downloadedWrappedAttachments[1].unwrap<Fhir4Attachment>() } returns downloadedAttachments[1]

        every { downloadedWrappedAttachments[0].id } returns downloadedAttachmentsIds[0]
        every { downloadedWrappedAttachments[1].id } returns downloadedAttachmentsIds[1]

        every { SdkFhirAttachmentHelper.hasAttachment(resource) } returns true
        every { SdkFhirAttachmentHelper.getAttachment(resource) } returns attachments as MutableList<Any?>
        every { SdkFhirAttachmentHelper.getIdentifier(resource) } returns identifiers
        every { SdkAttachmentFactory.wrap(or(attachments[0], attachments[1])) } returnsMany wrappedAttachments
        every {
            recordService.setAttachmentIdForDownloadType(
                wrappedAttachments,
                identifiers,
                DownloadType.Full
            )
        } just Runs

        every {
            attachmentService.download(
                wrappedAttachments,
                attachmentKey,
                USER_ID
            )
        } returns Single.just(downloadedWrappedAttachments)

        every {
            recordService.updateAttachmentMeta(downloadedWrappedAttachments[0])
        } returns downloadedWrappedAttachments[0]

        every {
            recordService.updateAttachmentMeta(downloadedWrappedAttachments[1])
        } returns downloadedWrappedAttachments[1]

        // When
        val subscriber = recordService.downloadAttachmentsFromStorage<Fhir4Resource, Fhir4Attachment>(
            attachmentIds,
            USER_ID,
            DownloadType.Full,
            decryptedRecord
        ).test().await()

        // Then
        val result = subscriber
            .assertNoErrors()
            .assertComplete()
            .values()[0]

        assertEquals(
            actual = result.size,
            expected = 2
        )
        assertEquals(
            actual = result,
            expected = downloadedAttachments
        )

        verifyOrder {
            recordService.setAttachmentIdForDownloadType(
                wrappedAttachments,
                identifiers,
                DownloadType.Full
            )
            attachmentService.download(
                wrappedAttachments,
                attachmentKey,
                USER_ID
            )
            recordService.updateAttachmentMeta(downloadedWrappedAttachments[0])
            recordService.updateAttachmentMeta(downloadedWrappedAttachments[1])
        }

        unmockkObject(SdkFhirAttachmentHelper)
        unmockkObject(SdkAttachmentFactory)
    }

    // FHIR3 Download Attachments
    @Test
    fun `Given, downloadFhir3Attachment is called, with it  a RecordId, AttachmentIds, UserId and a DownloadType, it delegates the call to downloadFhir3Attachments and returns its result`() {
        // Given
        val attachmentId = "abc"
        val expected: Fhir3Attachment = mockk()

        every {
            recordService.downloadFhir3Attachments(
                RECORD_ID,
                listOf(attachmentId),
                USER_ID,
                DownloadType.Full
            )
        } returns Single.just(listOf(expected))

        // When
        val subscriber = recordService.downloadFhir3Attachment(
            RECORD_ID,
            attachmentId,
            USER_ID,
            DownloadType.Full
        ).test().await()

        // Then
        val result = subscriber
            .assertNoErrors()
            .assertComplete()
            .values()[0]

        assertSame(
            actual = result,
            expected = expected
        )

        verifyOrder {
            recordService.downloadFhir3Attachments(
                RECORD_ID,
                listOf(attachmentId),
                USER_ID,
                DownloadType.Full
            )
        }
    }

    @Test
    fun `Given, downloadFhir3Attachments is called with a RecordId, AttachmentIds, UserId and a DownloadType, it fails if the fetched Record is not Fhir3`() {
        // Given
        val userId = "asd"
        val recordId = "ads"
        val attachmentId = "lllll"
        val attachmentIds = listOf(attachmentId)
        val type = DownloadType.Medium

        val encryptedRecord = mockk<EncryptedRecord>()
        val decryptedRecord = mockk<DecryptedFhir4Record<Fhir4Resource>>()
        val resource: Fhir4Resource = mockk()

        every { decryptedRecord.resource } returns resource

        every { apiService.fetchRecord(ALIAS, userId, recordId) } returns Single.just(encryptedRecord)
        every { recordService.decryptRecord<Any>(encryptedRecord, userId) } returns decryptedRecord as DecryptedBaseRecord<Any>

        // When
        val subscriber = recordService.downloadFhir3Attachments(
            recordId,
            attachmentIds,
            userId,
            type
        ).test().await()

        // Then
        subscriber
            .assertError(IllegalArgumentException::class.java)
            .assertErrorMessage("The given Record does not match the expected resource type.")
            .assertNotComplete()
    }

    @Test
    fun `Given, downloadFhir3Attachments is called with a RecordId, AttachmentIds, UserId and a DownloadType, returns encountered Attachments`() {
        // Given
        val userId = "asd"
        val recordId = "ads"
        val attachmentId = "lllll"
        val attachmentIds = listOf(attachmentId)
        val type = DownloadType.Medium

        val response1 = mockk<Fhir3Attachment>()
        val response2 = mockk<Fhir3Attachment>()
        val response = listOf(response1, response2)

        val encryptedRecord = mockk<EncryptedRecord>()
        val decryptedRecord = mockk<DecryptedFhir3Record<Fhir3Resource>>()
        val resource: Fhir3Resource = mockk()

        every { decryptedRecord.resource } returns resource

        every { apiService.fetchRecord(ALIAS, userId, recordId) } returns Single.just(encryptedRecord)
        every { recordService.decryptRecord<Any>(encryptedRecord, userId) } returns decryptedRecord as DecryptedBaseRecord<Any>
        every {
            recordService.downloadAttachmentsFromStorage<Fhir3Resource, Fhir3Attachment>(
                attachmentIds,
                userId,
                type,
                decryptedRecord as DecryptedBaseRecord<Fhir3Resource>
            )
        } returns Single.just(response)

        // When
        val subscriber = recordService.downloadFhir3Attachments(
            recordId,
            attachmentIds,
            userId,
            type
        ).test().await()

        // Then
        val result = subscriber
            .assertNoErrors()
            .assertComplete()
            .assertValueCount(1)
            .values()[0]

        assertSame(
            response,
            result
        )

        verifyOrder {
            apiService.fetchRecord(ALIAS, userId, recordId)
            recordService.decryptRecord<Any>(encryptedRecord, userId)
            recordService.downloadAttachmentsFromStorage<Fhir3Resource, Fhir3Attachment>(
                attachmentIds,
                userId,
                type,
                decryptedRecord as DecryptedBaseRecord<Fhir3Resource>
            )
        }
    }

    // FHIR4 Download Attachments
    @Test
    fun `Given, downloadFhir4Attachment is called, with it  a RecordId, AttachmentIds, UserId and a DownloadType, it delegates the call to downloadFhir4Attachments and returns its result`() {
        // Given
        val attachmentId = "abc"
        val expected: Fhir4Attachment = mockk()

        every {
            recordService.downloadFhir4Attachments(
                RECORD_ID,
                listOf(attachmentId),
                USER_ID,
                DownloadType.Full
            )
        } returns Single.just(listOf(expected))

        // When
        val subscriber = recordService.downloadFhir4Attachment(
            RECORD_ID,
            attachmentId,
            USER_ID,
            DownloadType.Full
        ).test().await()

        // Then
        val result = subscriber
            .assertNoErrors()
            .assertComplete()
            .values()[0]

        assertSame(
            actual = result,
            expected = expected
        )

        verifyOrder {
            recordService.downloadFhir4Attachments(
                RECORD_ID,
                listOf(attachmentId),
                USER_ID,
                DownloadType.Full
            )
        }
    }

    @Test
    fun `Given, downloadFhir4Attachments is called with a RecordId, AttachmentIds, UserId and a DownloadType, it fails if the fetched Record is not Fhir4`() {
        // Given
        val userId = "asd"
        val recordId = "ads"
        val attachmentId = "lllll"
        val attachmentIds = listOf(attachmentId)
        val type = DownloadType.Medium

        val encryptedRecord = mockk<EncryptedRecord>()
        val decryptedRecord = mockk<DecryptedFhir3Record<Fhir3Resource>>()
        val resource: Fhir3Resource = mockk()

        every { decryptedRecord.resource } returns resource

        every { apiService.fetchRecord(ALIAS, userId, recordId) } returns Single.just(encryptedRecord)
        every { recordService.decryptRecord<Any>(encryptedRecord, userId) } returns decryptedRecord as DecryptedBaseRecord<Any>

        // When
        val subscriber = recordService.downloadFhir4Attachments(
            recordId,
            attachmentIds,
            userId,
            type
        ).test().await()

        // Then
        subscriber
            .assertError(IllegalArgumentException::class.java)
            .assertErrorMessage("The given Record does not match the expected resource type.")
            .assertNotComplete()
    }

    @Test
    fun `Given, downloadFhir4Attachments is called with a RecordId, AttachmentIds, UserId and a DownloadType, returns encountered Attachments`() {
        // Given
        val userId = "asd"
        val recordId = "ads"
        val attachmentId = "lllll"
        val attachmentIds = listOf(attachmentId)
        val type = DownloadType.Medium

        val response1 = mockk<Fhir4Attachment>()
        val response2 = mockk<Fhir4Attachment>()
        val response = listOf(response1, response2)

        val encryptedRecord = mockk<EncryptedRecord>()
        val decryptedRecord = mockk<DecryptedFhir4Record<Fhir4Resource>>()
        val resource: Fhir4Resource = mockk()

        every { decryptedRecord.resource } returns resource

        every { apiService.fetchRecord(ALIAS, userId, recordId) } returns Single.just(encryptedRecord)
        every { recordService.decryptRecord<Any>(encryptedRecord, userId) } returns decryptedRecord as DecryptedBaseRecord<Any>
        every {
            recordService.downloadAttachmentsFromStorage<Fhir4Resource, Fhir4Attachment>(
                attachmentIds,
                userId,
                type,
                decryptedRecord as DecryptedBaseRecord<Fhir4Resource>
            )
        } returns Single.just(response)

        // When
        val subscriber = recordService.downloadFhir4Attachments(
            recordId,
            attachmentIds,
            userId,
            type
        ).test().await()

        // Then
        val result = subscriber
            .assertNoErrors()
            .assertComplete()
            .assertValueCount(1)
            .values()[0]

        assertSame(
            response,
            result
        )

        verifyOrder {
            apiService.fetchRecord(ALIAS, userId, recordId)
            recordService.decryptRecord<Any>(encryptedRecord, userId)
            recordService.downloadAttachmentsFromStorage<Fhir4Resource, Fhir4Attachment>(
                attachmentIds,
                userId,
                type,
                decryptedRecord as DecryptedBaseRecord<Fhir4Resource>
            )
        }
    }

    // FHIR3 Download Record
    @Test
    fun `Given, downloadFhir3Record is called, with a RecordId and a UserId, it fails if the given RecordId resolves to a non Fhir3 Record`() {
        mockkObject(RecordMapper)

        // Given
        val resource: Fhir4Resource = mockk()
        val encryptedRecord: EncryptedRecord = mockk()
        val decryptedRecord: DecryptedFhir4Record<Fhir4Resource> = mockk()
        val identifier = "id"

        every { decryptedRecord.resource } returns resource
        every { decryptedRecord.identifier } returns identifier

        every { apiService.fetchRecord(ALIAS, USER_ID, RECORD_ID) } returns Single.just(encryptedRecord)
        every {
            recordService.decryptRecord<Fhir4Resource>(encryptedRecord, USER_ID)
        } returns decryptedRecord

        // When
        val subscriber = recordService.downloadFhir3Record<Fhir3Resource>(RECORD_ID, USER_ID).test().await()

        // Then
        subscriber
            .assertError(IllegalArgumentException::class.java)
            .assertErrorMessage("The given Record does not match the expected resource type.")
            .assertNotComplete()

        unmockkObject(RecordMapper)
    }

    @Test
    fun `Given, downloadFhir3Record is called, with a RecordId and a UserId, it downloads the associated record`() {
        mockkObject(RecordMapper)

        // Given
        val resource: Fhir3Resource = mockk()
        val encryptedRecord: EncryptedRecord = mockk()
        val decryptedRecord: DecryptedFhir3Record<Fhir3Resource> = mockk()
        val createdRecord: Record<Fhir3Resource> = mockk()
        val identifier = "id"

        every { decryptedRecord.resource } returns resource
        every { decryptedRecord.identifier } returns identifier

        every { apiService.fetchRecord(ALIAS, USER_ID, RECORD_ID) } returns Single.just(encryptedRecord)
        every {
            recordService.decryptRecord<Fhir3Resource>(encryptedRecord, USER_ID)
        } returns decryptedRecord
        every { recordService.downloadData(decryptedRecord, USER_ID) } returns decryptedRecord
        every { recordService.checkDataRestrictions(resource) } just Runs
        every { RecordMapper.getInstance(decryptedRecord) } returns createdRecord

        // When
        val subscriber = recordService.downloadFhir3Record<Fhir3Resource>(RECORD_ID, USER_ID).test().await()

        // Then
        val record = subscriber
            .assertNoErrors()
            .assertComplete()
            .assertValueCount(1)
            .values()[0]

        assertSame(
            actual = record,
            expected = createdRecord
        )

        verifyOrder {
            apiService.fetchRecord(ALIAS, USER_ID, RECORD_ID)
            recordService.decryptRecord<Fhir3Resource>(encryptedRecord, USER_ID)
            recordService.downloadData(decryptedRecord, USER_ID)
            recordService.checkDataRestrictions(resource)
            resource.id = identifier
            RecordMapper.getInstance(decryptedRecord)
        }

        unmockkObject(RecordMapper)
    }

    @Test
    fun `Given, downloadRecords is called with RecordIds and UserIds, it streams them into downloadRecord and returns the results`() {
        // Given
        val recordIds = listOf(
            "1",
            "2"
        )

        val records: List<Record<Fhir3Resource>> = listOf(
            mockk(),
            mockk()
        )

        every {
            recordService.downloadFhir3Record<Fhir3Resource>(or(recordIds[0], recordIds[1]), USER_ID)
        } returnsMany listOf(Single.just(records[0]), Single.just(records[1]))

        // When
        val subscriber = recordService.downloadRecords<Fhir3Resource>(recordIds, USER_ID).test().await()

        // Then
        val actual = subscriber
            .assertNoErrors()
            .assertComplete()
            .assertValueCount(1)
            .values()[0]

        assertEquals(
            actual = actual.successfulDownloads,
            expected = records
        )

        assertEquals(
            actual = actual.failedDownloads,
            expected = listOf()
        )
    }

    // FHIR3 Download Record
    @Test
    fun `Given, downloadFhir4Record is called, with a RecordId and a UserId, it fails if the given RecordId resolves to a non Fhir4 Record`() {
        mockkObject(RecordMapper)

        // Given
        val resource: Fhir3Resource = mockk()
        val encryptedRecord: EncryptedRecord = mockk()
        val decryptedRecord: DecryptedFhir3Record<Fhir3Resource> = mockk()
        val identifier = "id"

        every { decryptedRecord.resource } returns resource
        every { decryptedRecord.identifier } returns identifier

        every { apiService.fetchRecord(ALIAS, USER_ID, RECORD_ID) } returns Single.just(encryptedRecord)
        every {
            recordService.decryptRecord<Fhir3Resource>(encryptedRecord, USER_ID)
        } returns decryptedRecord

        // When
        val subscriber = recordService.downloadFhir4Record<Fhir4Resource>(RECORD_ID, USER_ID).test().await()

        // Then
        subscriber
            .assertError(IllegalArgumentException::class.java)
            .assertErrorMessage("The given Record does not match the expected resource type.")
            .assertNotComplete()

        unmockkObject(RecordMapper)
    }

    @Test
    fun `Given, downloadFhir4Record is called, with a RecordId and a UserId, it downloads the associated record`() {
        mockkObject(RecordMapper)

        // Given
        val resource: Fhir4Resource = mockk()
        val encryptedRecord: EncryptedRecord = mockk()
        val decryptedRecord: DecryptedFhir4Record<Fhir4Resource> = mockk()
        val createdRecord: Fhir4Record<Fhir4Resource> = mockk()
        val identifier = "id"

        every { decryptedRecord.resource } returns resource
        every { decryptedRecord.identifier } returns identifier

        every { apiService.fetchRecord(ALIAS, USER_ID, RECORD_ID) } returns Single.just(encryptedRecord)
        every {
            recordService.decryptRecord<Fhir4Resource>(encryptedRecord, USER_ID)
        } returns decryptedRecord
        every { recordService.downloadData(decryptedRecord, USER_ID) } returns decryptedRecord
        every { recordService.checkDataRestrictions(resource) } just Runs
        every { RecordMapper.getInstance(decryptedRecord) } returns createdRecord

        // When
        val subscriber = recordService.downloadFhir4Record<Fhir4Resource>(RECORD_ID, USER_ID).test().await()

        // Then
        val record = subscriber
            .assertNoErrors()
            .assertComplete()
            .assertValueCount(1)
            .values()[0]

        assertSame(
            actual = record,
            expected = createdRecord
        )

        verifyOrder {
            apiService.fetchRecord(ALIAS, USER_ID, RECORD_ID)
            recordService.decryptRecord<Fhir4Resource>(encryptedRecord, USER_ID)
            recordService.downloadData(decryptedRecord, USER_ID)
            recordService.checkDataRestrictions(resource)
            resource.id = identifier
            RecordMapper.getInstance(decryptedRecord)
        }

        unmockkObject(RecordMapper)
    }
}
