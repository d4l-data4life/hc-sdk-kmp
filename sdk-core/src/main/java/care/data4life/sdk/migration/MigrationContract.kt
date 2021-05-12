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

package care.data4life.sdk.migration

import care.data4life.sdk.network.model.NetworkModelContract
import care.data4life.sdk.tag.Annotations
import care.data4life.sdk.tag.Tags
import io.reactivex.Observable
import io.reactivex.Single

@Retention(AnnotationRetention.SOURCE)
@MustBeDocumented
annotation class Migration(val message: String)

interface MigrationContract {
    interface CompatibilityService {
        fun searchRecords(
            alias: String,
            userId: String,
            startDate: String?,
            endDate: String?,
            pageSize: Int,
            offSet: Int,
            tags: Tags,
            annotations: Annotations
        ): Observable<List<NetworkModelContract.EncryptedRecord>>

        fun countRecords(
            alias: String,
            userId: String,
            tags: Tags,
            annotations: Annotations
        ): Single<Int>
    }

    interface LegacyTagEncoder {
        companion object {
            val JS_LEGACY_ENCODING_EXCEPTIONS = listOf(
                "%2a",
                "%2d",
                "%2e",
                "%5f",
                "%7e"
            )
        }
    }
}
