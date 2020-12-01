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

import care.data4life.sdk.lang.TaskException;
import care.data4life.sdk.listener.Callback;
import care.data4life.sdk.listener.ResultListener;
import io.reactivex.Completable;
import io.reactivex.Single;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public class CallHandler {

    protected SdkContract.ErrorHandler errorHandler;

    CallHandler(
            SdkContract.ErrorHandler errorHandler
    ) {
        this.errorHandler = errorHandler;
    }

    public <T> Task executeSingle(Single<T> operation, ResultListener<T> listener) {
        final Task task = new Task(null);

        Disposable operationHandle =
                operation
                        .doOnDispose(() -> {
                            if (task.isCanceled())
                                listener.onError(errorHandler.handleError(new TaskException.CancelException()));
                        })
                        .doFinally(task::finish)
                        .subscribeOn(Schedulers.io())
                        .subscribe(
                                listener::onSuccess,
                                error -> {
                                    if (!task.isActive()) return;
                                    listener.onError(errorHandler.handleError(error));
                                }
                        );

        task.setOperationHandle(operationHandle);
        return task;
    }

    public Task executeCompletable(Completable operation, Callback listener) {
        final Task task = new Task(null);

        Disposable operationHandle =
                operation
                        .doOnDispose(() -> {
                            if (task.isCanceled())
                                listener.onError(errorHandler.handleError(new TaskException.CancelException()));
                        })
                        .doFinally(task::finish)
                        .subscribeOn(Schedulers.io())
                        .subscribe(
                                listener::onSuccess,
                                error -> {
                                    if (!task.isActive()) return;
                                    listener.onError(errorHandler.handleError(error));
                                }
                        );

        task.setOperationHandle(operationHandle);
        return task;
    }
}
