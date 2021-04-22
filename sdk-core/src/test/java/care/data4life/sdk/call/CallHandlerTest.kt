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
package care.data4life.sdk.call

import care.data4life.sdk.SdkContract
import io.mockk.mockk
import io.reactivex.Completable
import io.reactivex.Single
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import care.data4life.sdk.listener.Callback as LegacyCallback
import care.data4life.sdk.listener.ResultListener as LegacyListener

class CallHandlerTest {

    private lateinit var errorHandler: SdkContract.ErrorHandler

    // SUT
    private lateinit var callHandler: CallHandler

    @Before
    fun setup() {
        errorHandler = mockk()
        callHandler = CallHandler(errorHandler)
    }

    @Test
    fun `Given, executeSingle is called with a Callback, it returns a task`() {
        // Given
        val operation: Single<Any> = mockk(relaxed = true)
        val callback: Callback<Any> = mockk()

        // When
        val result: Any = callHandler.executeSingle(operation, callback)

        // Then
        assertTrue(result is Task)
    }

    @Test
    fun `Given, executeSingle is called with LegacyListener, it returns a task`() {
        // Given
        val operation: Single<Any> = mockk(relaxed = true)
        val listener: LegacyListener<Any> = mockk()

        // When
        val result: Any = callHandler.executeSingle(operation, listener)

        // Then
        assertTrue(result is Task)
    }

    @Test
    fun `Given, executeCompletable is called with LegacyCallback, it returns a task`() {
        // Given
        val operation: Completable = mockk(relaxed = true)
        val callback: LegacyCallback = mockk()

        // When
        val result: Any = callHandler.executeCompletable(operation, callback)

        // Then
        assertTrue(result is Task)
    }
}
