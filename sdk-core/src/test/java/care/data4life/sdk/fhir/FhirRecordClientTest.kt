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
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.reactivex.Single
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

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
    fun `Given count is called, with a resourceType, Annotations and a Callback it returns the corresponding Task`() {
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
    fun `Given download is called, with a recordId and a Callback, it returns the corresponding Task`() {
        // Given
        val callback: Callback<Fhir4Record<Fhir4Resource>> = mockk()

        val recordId = "Potato"
        val userId = "Soup"
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
    fun `Given downloadAttachment is called, with a recordId and a Callback, it returns the corresponding Task`() {
        // Given
        val callback: Callback<Fhir4Attachment> = mockk()

        val recordId = "Potato"
        val attachmentId = "Tomato"
        val userId = "Soup"
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

        val recordId = "Potato"
        val attachmentIds: List<String> = mockk()
        val userId = "Soup"
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
