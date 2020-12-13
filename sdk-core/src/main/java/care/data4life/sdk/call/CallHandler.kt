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
import care.data4life.sdk.lang.TaskException
import care.data4life.sdk.listener.Callback as LegacyCallback
import care.data4life.sdk.listener.ResultListener as LegacyListener
import io.reactivex.Completable
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers

class CallHandler(
        var errorHandler: SdkContract.ErrorHandler
) {
    fun <T> executeSingle(operation: Single<T>, callback: Callback<T>): Task {
        val task = Task()
        val operationHandle = operation
                .doOnDispose { if (task.isCanceled) callback.onError(errorHandler.handleError(TaskException.CancelException())) }
                .doFinally { task.finish() }
                .subscribeOn(Schedulers.io())
                .subscribe({ t: T -> callback.onSuccess(t) }
                ) { error ->
                    if (!task.isActive) return@subscribe
                    callback.onError(errorHandler.handleError(error))
                }
        task.operationHandle = operationHandle
        return task
    }

    fun <T> executeSingle(operation: Single<T>, listener: LegacyListener<T>): Task {
        val task = Task()
        val operationHandle = operation
                .doOnDispose { if (task.isCanceled) listener.onError(errorHandler.handleError(TaskException.CancelException())) }
                .doFinally { task.finish() }
                .subscribeOn(Schedulers.io())
                .subscribe({ t: T -> listener.onSuccess(t) }
                ) { error ->
                    if (!task.isActive) return@subscribe
                    listener.onError(errorHandler.handleError(error))
                }
        task.operationHandle = operationHandle
        return task
    }

    fun executeCompletable(operation: Completable, listener: LegacyCallback): Task {
        val task = Task()
        val operationHandle = operation
                .doOnDispose { if (task.isCanceled) listener.onError(errorHandler.handleError(TaskException.CancelException())) }
                .doFinally { task.finish() }
                .subscribeOn(Schedulers.io())
                .subscribe({ listener.onSuccess() }
                ) { error ->
                    if (!task.isActive) return@subscribe
                    listener.onError(errorHandler.handleError(error))
                }
        task.operationHandle = operationHandle
        return task
    }
}
