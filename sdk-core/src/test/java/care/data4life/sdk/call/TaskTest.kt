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

import com.google.common.truth.Truth
import io.reactivex.disposables.Disposable
import org.junit.Test
import org.mockito.Mockito

class TaskTest {

    @Test
    fun newTask_shouldBeIn_activeState() {
        // given
        val task = Task()

        // then
        Truth.assertThat(task.isActive).isTrue()
        Truth.assertThat(task.isCanceled).isFalse()
    }

    @Test
    fun finish_shouldMakeTask_inactive() {
        // given
        val task = Task()

        // when
        task.finish()

        // then
        Truth.assertThat(task.isActive).isFalse()
        Truth.assertThat(task.isCanceled).isFalse()
    }

    @Test
    fun cancel_shouldCancel_activeTask() {
        // given
        val operationHandle = Mockito.mock(Disposable::class.java)
        val task = Task()
        task.operationHandle = operationHandle

        // when
        val result = task.cancel()

        // then
        Truth.assertThat(result).isTrue()
        Truth.assertThat(task.isCanceled).isTrue()
        Truth.assertThat(task.isActive).isFalse()
        Mockito.verify(operationHandle).dispose()
    }

    @Test
    fun cancel_shouldFailToCancel_alreadyCanceledTask() {
        // given
        val task = Task()
        task.cancel()

        // when
        val result = task.cancel()

        // then
        Truth.assertThat(result).isFalse()
    }
}
