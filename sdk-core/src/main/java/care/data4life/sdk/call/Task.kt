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

import io.reactivex.disposables.Disposable

class Task {

    var operationHandle: Disposable? = null

    var isActive = true
        private set
    var isCanceled = false
        private set


    fun finish() {
        isActive = false
        operationHandle = null
    }

    fun cancel(): Boolean {
        if (!isActive || isCanceled) return false

        operationHandle?.dispose()

        operationHandle = null
        isCanceled = true
        isActive = false

        return true
    }
}
