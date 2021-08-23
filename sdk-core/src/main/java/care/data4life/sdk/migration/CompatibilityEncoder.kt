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

import care.data4life.sdk.migration.MigrationInternalContract.CompatibilityEncoder.Companion.JS_LEGACY_ENCODING_REPLACEMENTS
import care.data4life.sdk.tag.TagEncoding
import care.data4life.sdk.tag.TaggingContract
import care.data4life.sdk.wrapper.UrlEncoding
import care.data4life.sdk.wrapper.WrapperContract

internal object CompatibilityEncoder : MigrationInternalContract.CompatibilityEncoder {
    private val tagEncoding: TaggingContract.Encoding = TagEncoding
    private val urlEncoding: WrapperContract.UrlEncoding = UrlEncoding

    private fun mapJSExceptions(encodedTag: String): String {
        var result = encodedTag

        JS_LEGACY_ENCODING_REPLACEMENTS.entries.forEach { replacement ->
            result = result.replace(replacement.key, replacement.value)
        }

        return result
    }

    override fun encode(tagValue: String): MigrationInternalContract.CompatibilityTag {
        val kmpLegacyEncoding = tagEncoding.normalize(tagValue)

        return MigrationInternalContract.CompatibilityTag(
            validEncoding = tagEncoding.encode(tagValue),
            kmpLegacyEncoding = kmpLegacyEncoding,
            jsLegacyEncoding = mapJSExceptions(
                urlEncoding.encode(kmpLegacyEncoding)
            ),
            iosLegacyEncoding = urlEncoding.encode(kmpLegacyEncoding)
        )
    }

    override fun normalize(tagValue: String): String = tagEncoding.normalize(tagValue)
}
