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
import care.data4life.sdk.lang.D4LException
import care.data4life.sdk.lang.TaskException
import care.data4life.sdk.log.Log
import io.reactivex.Completable
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import care.data4life.sdk.listener.Callback as LegacyCallback
import care.data4life.sdk.listener.ResultListener as LegacyListener

class CallHandler(
    var errorHandler: SdkContract.ErrorHandler
) {

    fun <T> executeSingle(operation: Single<T>, callback: Callback<T>): Task {
        return Task().also { task ->
            task.operationHandle = wireTask(
                task,
                operation,
                { callback.onSuccess(it) },
                { callback.onError(it) }
            )
        }
    }

    fun <T> executeSingle(operation: Single<T>, listener: LegacyListener<T>): Task {
        return Task().also { task ->
            task.operationHandle = wireTask(
                task,
                operation,
                { listener.onSuccess(it) },
                { listener.onError(it) }
            )
        }
    }

    fun executeCompletable(operation: Completable, listener: LegacyCallback): Task {
        return Task().also { task ->
            task.operationHandle = wireTask(
                task,
                operation.toSingleDefault("Ignore"),
                { listener.onSuccess() },
                { listener.onError(it) }
            )
        }
    }

    private fun <T> wireTask(
        task: Task,
        operation: Single<T>,
        onSuccess: (T) -> Unit,
        onError: (D4LException) -> Unit
    ) =
        operation
            .doOnDispose { if (task.isCanceled) onError(prepareError(TaskException.CancelException())) }
            .doFinally { task.finish() }
            .subscribeOn(Schedulers.io())
            .subscribe({ t: T -> onSuccess(t) }
            ) { error ->
                if (!task.isActive) return@subscribe
                onError(prepareError(error))
            }

    private fun prepareError(error: Throwable): D4LException {
        val cleanedException = errorHandler.handleError(error)
        Log.error(cleanedException, cleanedException.message)
        return cleanedException
    }
}
