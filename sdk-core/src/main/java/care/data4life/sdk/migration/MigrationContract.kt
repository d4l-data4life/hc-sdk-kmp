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

import care.data4life.sdk.network.NetworkingContract
import care.data4life.sdk.tag.Annotations
import care.data4life.sdk.tag.Tags

@Retention(AnnotationRetention.SOURCE)
@MustBeDocumented
annotation class Migration(val message: String)

class MigrationContract {
    interface CompatibilityService {
        fun resolveSearchTags(
            tags: Tags,
            annotation: Annotations
        ): NetworkingContract.SearchTags
    }

    internal interface CompatibilityEncoder {
        fun encode(tagValue: String): Triple<String, String, String>
        fun normalize(tagValue: String): String

        companion object {
            val JS_LEGACY_ENCODING_EXCEPTIONS = mapOf(
                "%2A" to "%2a",
                "%2D" to "%2d",
                "%2E" to "%2e",
                "%5F" to "%5f",
                "%7E" to "%7e"
            )
        }
    }
}
