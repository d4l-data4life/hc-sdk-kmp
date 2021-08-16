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

package care.data4life.sdk.network.typeadapter

import care.data4life.sdk.lang.CoreRuntimeException
import care.data4life.sdk.model.ModelContract
import com.squareup.moshi.FromJson
import com.squareup.moshi.ToJson

class RecordStatusAdapter {
    @ToJson
    fun toJson(status: ModelContract.RecordStatus): String = status.id

    @FromJson
    fun fromJson(status: String): ModelContract.RecordStatus {
        return try {
            ModelContract.RecordStatus.valueOf(status)
        } catch (_: Exception) {
            throw CoreRuntimeException.Default(
                "Unknown record status $status."
            )
        }
    }
}
