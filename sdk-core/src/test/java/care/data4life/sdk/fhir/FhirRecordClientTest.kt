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

package care.data4life.sdk.fhir

import care.data4life.sdk.SdkContract
import care.data4life.sdk.auth.AuthContract
import care.data4life.sdk.call.CallHandler
import care.data4life.sdk.call.Callback
import care.data4life.sdk.call.Fhir4Record
import care.data4life.sdk.call.Task
import care.data4life.sdk.model.DownloadType
import care.data4life.sdk.record.RecordContract
import care.data4life.sdk.tag.Annotations
import care.data4life.sdk.test.util.GenericTestDataProvider.ATTACHMENT_ID
import care.data4life.sdk.test.util.GenericTestDataProvider.RECORD_ID
import care.data4life.sdk.test.util.GenericTestDataProvider.USER_ID
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.reactivex.Completable
import io.reactivex.Single
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue

class FhirRecordClientTest {
    private val recordService: RecordContract.Service = mockk()
    private val userService: AuthContract.UserService = mockk()
    private val callHandler: CallHandler = mockk()
    private lateinit var client: SdkContract.Fhir4RecordClient

    @Before
    fun setUp() {
        client = Fhir4RecordClient(
            userService,
            recordService,
            callHandler
        )
    }

    @Test
    fun `it fulfils Fhir4RecordClient`() {
        val client: Any = Fhir4RecordClient(mockk(), mockk(), mockk())

        assertTrue(client is SdkContract.Fhir4RecordClient)
    }

    @Test
    fun `Given create is called, with a Resource, Annotations and a Callback it returns the corresponding Task`() {
        // Given
        val resource: Fhir4Resource = mockk()
        val annotations: Annotations = mockk()
        val callback: Callback<Fhir4Record<Fhir4Resource>> = mockk()

        val userId = USER_ID
        val expectedRecord: Fhir4Record<Fhir4Resource> = mockk()
        val record: Single<Fhir4Record<Fhir4Resource>> = Single.just(expectedRecord)
        val expected: Task = mockk()
        val observer = slot<Single<Fhir4Record<Fhir4Resource>>>()

        every { userService.finishLogin(true) } returns Single.just(true)
        every { userService.userID } returns Single.just(userId)
        every {
            recordService.createRecord(userId, resource, annotations)
        } returns record
        every {
            callHandler.executeSingle(capture(observer), callback)
        } answers {
            assertEquals(
                expected = expectedRecord,
                actual = observer.captured.blockingGet()
            )
            expected
        }

        // When
        val actual = client.create(resource, annotations, callback)

        // Then
        assertSame(
            expected = expected,
            actual = actual
        )
    }

    @Test
    fun `Given update is called, with a RecordId, a Resource, Annotations and a Callback it returns the corresponding Task`() {
        // Given
        val resource: Fhir4Resource = mockk()
        val annotations: Annotations = mockk()
        val callback: Callback<Fhir4Record<Fhir4Resource>> = mockk()
        val recordId = RECORD_ID

        val userId = USER_ID
        val expectedRecord: Fhir4Record<Fhir4Resource> = mockk()
        val record: Single<Fhir4Record<Fhir4Resource>> = Single.just(expectedRecord)
        val expected: Task = mockk()
        val observer = slot<Single<Fhir4Record<Fhir4Resource>>>()

        every { userService.finishLogin(true) } returns Single.just(true)
        every { userService.userID } returns Single.just(userId)
        every {
            recordService.updateRecord(userId, recordId, resource, annotations)
        } returns record
        every {
            callHandler.executeSingle(capture(observer), callback)
        } answers {
            assertEquals(
                expected = expectedRecord,
                actual = observer.captured.blockingGet()
            )
            expected
        }

        // When
        val actual = client.update(recordId, resource, annotations, callback)

        // Then
        assertSame(
            expected = expected,
            actual = actual
        )
    }

    @Test
    fun `Given fetch is called, with a RecordId and a Callback it returns the corresponding Task`() {
        // Given
        val callback: Callback<Fhir4Record<Fhir4Resource>> = mockk()
        val recordId = RECORD_ID

        val userId = USER_ID
        val expectedRecord: Fhir4Record<Fhir4Resource> = mockk()
        val record: Single<Fhir4Record<Fhir4Resource>> = Single.just(expectedRecord)
        val expected: Task = mockk()
        val observer = slot<Single<Fhir4Record<Fhir4Resource>>>()

        every { userService.finishLogin(true) } returns Single.just(true)
        every { userService.userID } returns Single.just(userId)
        every {
            recordService.fetchFhir4Record<Fhir4Resource>(userId, recordId)
        } returns record
        every {
            callHandler.executeSingle(capture(observer), callback)
        } answers {
            assertEquals(
                expected = expectedRecord,
                actual = observer.captured.blockingGet()
            )
            expected
        }

        // When
        val actual = client.fetch(recordId, callback)

        // Then
        assertSame(
            expected = expected,
            actual = actual
        )
    }

    @Test
    fun `Given search is called, with a ResourceType, Annotations, a CreationDateRange, a UpdateDateTimeRange, Pagesize, Offset and a Callback it returns the corresponding Task`() {
        // Given
        val callback: Callback<List<Fhir4Record<Fhir4Resource>>> = mockk()
        val resourceType = Fhir4Resource::class.java
        val annotations: Annotations = mockk()
        val creationDate = SdkContract.CreationDateRange(null, null)
        val updateDateTime = SdkContract.UpdateDateTimeRange(null, null)
        val includeDeletedRecords = true
        val pageSize = 23
        val offset = 42

        val userId = USER_ID
        val expectedRecords: List<Fhir4Record<Fhir4Resource>> = mockk()
        val records: Single<List<Fhir4Record<Fhir4Resource>>> = Single.just(expectedRecords)
        val expected: Task = mockk()
        val observer = slot<Single<List<Fhir4Record<Fhir4Resource>>>>()

        every { userService.finishLogin(true) } returns Single.just(true)
        every { userService.userID } returns Single.just(userId)
        every {
            recordService.searchFhir4Records(
                userId,
                resourceType,
                annotations,
                creationDate,
                updateDateTime,
                includeDeletedRecords,
                pageSize,
                offset
            )
        } returns records
        every {
            callHandler.executeSingle(capture(observer), callback)
        } answers {
            assertEquals(
                expected = expectedRecords,
                actual = observer.captured.blockingGet()
            )
            expected
        }

        // When
        val actual = client.search(
            resourceType,
            annotations,
            creationDate,
            updateDateTime,
            includeDeletedRecords,
            pageSize,
            offset,
            callback
        )

        // Then
        assertSame(
            expected = expected,
            actual = actual
        )
    }

    @Test
    fun `Given download is called, with a RecordId and a Callback, it returns the corresponding Task`() {
        // Given
        val callback: Callback<Fhir4Record<Fhir4Resource>> = mockk()

        val recordId = RECORD_ID
        val userId = USER_ID
        val record: Fhir4Record<Fhir4Resource> = mockk()
        val result = Single.just(record)
        val expected: Task = mockk()
        val observer = slot<Single<Fhir4Record<Fhir4Resource>>>()

        every { userService.finishLogin(true) } returns Single.just(true)
        every { userService.userID } returns Single.just(userId)
        every {
            recordService.downloadFhir4Record<Fhir4Resource>(recordId, userId)
        } returns result
        every {
            callHandler.executeSingle(capture(observer), callback)
        } answers {
            assertEquals(
                expected = record,
                actual = observer.captured.blockingGet()
            )
            expected
        }

        // When
        val actual = client.download(recordId, callback)

        // Then
        assertSame(
            expected = expected,
            actual = actual
        )
    }

    @Test
    fun `Given count is called, with a ResourceType, Annotations and a Callback it returns the corresponding Task`() {
        // Given
        val resourceType = Fhir4Resource::class.java
        val annotations: Annotations = mockk()
        val callback: Callback<Int> = mockk()

        val userId = "Potato"
        val expectedAmount = 23
        val amount = Single.just(23)
        val expected: Task = mockk()
        val observer = slot<Single<Int>>()

        every { userService.finishLogin(true) } returns Single.just(true)
        every { userService.userID } returns Single.just(userId)
        every {
            recordService.countFhir4Records(resourceType, userId, annotations)
        } returns amount
        every {
            callHandler.executeSingle(capture(observer), callback)
        } answers {
            assertEquals(
                expected = expectedAmount,
                actual = observer.captured.blockingGet()
            )
            expected
        }

        // When
        val actual = client.count(resourceType, annotations, callback)

        // Then
        assertSame(
            expected = expected,
            actual = actual
        )
    }

    @Test
    fun `Given delete is called, with a RecordId and a Callback, it returns the corresponding Task`() {
        // Given
        val callback: Callback<Boolean> = mockk()

        val recordId = RECORD_ID
        val userId = USER_ID
        val result = Completable.fromCallable { true }
        val expected: Task = mockk()
        val observer = slot<Single<Boolean>>()

        every { userService.finishLogin(true) } returns Single.just(true)
        every { userService.userID } returns Single.just(userId)
        every {
            recordService.deleteRecord(userId, recordId)
        } returns result
        every {
            callHandler.executeSingle(capture(observer), callback)
        } answers {
            assertTrue(observer.captured.blockingGet())
            expected
        }

        // When
        val actual = client.delete(recordId, callback)

        // Then
        assertSame(
            expected = expected,
            actual = actual
        )
    }

    @Test
    fun `Given downloadAttachment is called, with a recordId and a Callback, it returns the corresponding Task`() {
        // Given
        val callback: Callback<Fhir4Attachment> = mockk()

        val recordId = RECORD_ID
        val userId = USER_ID
        val attachmentId = ATTACHMENT_ID
        val downloadType = DownloadType.Full
        val attachment: Fhir4Attachment = mockk()
        val result = Single.just(attachment)
        val expected: Task = mockk()
        val observer = slot<Single<Fhir4Attachment>>()

        every { userService.finishLogin(true) } returns Single.just(true)
        every { userService.userID } returns Single.just(userId)
        every {
            recordService.downloadFhir4Attachment(recordId, attachmentId, userId, downloadType)
        } returns result
        every {
            callHandler.executeSingle(capture(observer), callback)
        } answers {
            assertEquals(
                expected = attachment,
                actual = observer.captured.blockingGet()
            )
            expected
        }

        // When
        val actual = client.downloadAttachment(recordId, attachmentId, downloadType, callback)

        // Then
        assertSame(
            expected = expected,
            actual = actual
        )
    }

    @Test
    fun `Given downloadAttachments is called, with a recordId and a Callback, it returns the corresponding Task`() {
        // Given
        val callback: Callback<List<Fhir4Attachment>> = mockk()

        val recordId = RECORD_ID
        val userId = USER_ID
        val attachmentIds: List<String> = mockk()
        val downloadType = DownloadType.Full
        val attachments: List<Fhir4Attachment> = mockk()
        val result = Single.just(attachments)
        val expected: Task = mockk()
        val observer = slot<Single<List<Fhir4Attachment>>>()

        every { userService.finishLogin(true) } returns Single.just(true)
        every { userService.userID } returns Single.just(userId)
        every {
            recordService.downloadFhir4Attachments(recordId, attachmentIds, userId, downloadType)
        } returns result
        every {
            callHandler.executeSingle(capture(observer), callback)
        } answers {
            assertEquals(
                expected = attachments,
                actual = observer.captured.blockingGet()
            )
            expected
        }

        // When
        val actual = client.downloadAttachments(recordId, attachmentIds, downloadType, callback)

        // Then
        assertSame(
            expected = expected,
            actual = actual
        )
    }
}
