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

import care.data4life.sdk.auth.AuthContract
import care.data4life.sdk.call.CallHandler
import care.data4life.sdk.call.Callback
import care.data4life.sdk.call.Task
import care.data4life.sdk.record.RecordContract
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.reactivex.Single
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

class FhirRecordClientTest {
    @Test
    fun `Given count is called, with a resourceType, Annotations and a Callback it returns its Task`() {
        // Given
        val userService: AuthContract.UserService = mockk()
        val recordService: RecordContract.Service = mockk()
        val callHandler: CallHandler = mockk()
        val client = Fhir4RecordClient(
            userService,
            recordService,
            callHandler
        )

        val resourceType = Fhir4Resource::class.java
        val annotations: List<String> = mockk()
        val callback: Callback<Int> = mockk()

        val userId = "Potato"
        val expectedAmount = 23
        val amount = Single.just(23)
        val expected: Task = mockk()
        val capturedAmount = slot<Single<Int>>()

        every { userService.finishLogin(true) } returns Single.just(true)
        every { userService.uID } returns Single.just(userId)
        every {
            recordService.countFhir4Records(resourceType, userId, annotations)
        } returns amount
        every {
            callHandler.executeSingle(capture(capturedAmount), callback)
        } answers {
            val actualAmount = capturedAmount.captured.blockingGet()
            assertEquals(
                expected = expectedAmount,
                actual = actualAmount
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
}
