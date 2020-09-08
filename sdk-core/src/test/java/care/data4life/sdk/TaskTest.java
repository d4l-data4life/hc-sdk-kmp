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

package care.data4life.sdk;

import org.junit.Test;

import io.reactivex.disposables.Disposable;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class TaskTest {

    @Test
    public void newTask_shouldBeIn_activeState() {
        //given
        Task task = new Task(null);

        //then
        assertThat(task.isActive()).isTrue();
        assertThat(task.isCanceled()).isFalse();
    }

    @Test
    public void finish_shouldMakeTask_inactive() {
        //given
        Task task = new Task(null);

        //when
        task.finish();

        //then
        assertThat(task.isActive()).isFalse();
        assertThat(task.isCanceled()).isFalse();
    }

    @Test
    public void cancel_shouldCancel_activeTask() {
        //given
        Disposable operationHandle = mock(Disposable.class);
        Task task = new Task(operationHandle);

        //when
        boolean result = task.cancel();

        //then
        assertThat(result).isTrue();
        assertThat(task.isCanceled()).isTrue();
        assertThat(task.isActive()).isFalse();
        verify(operationHandle).dispose();
    }

    @Test
    public void cancel_shouldFailToCancel_alreadyCanceledTask() {
        //given
        Task task = new Task(null);
        task.cancel();

        //when
        boolean result = task.cancel();

        //then
        assertThat(result).isFalse();
    }
}
