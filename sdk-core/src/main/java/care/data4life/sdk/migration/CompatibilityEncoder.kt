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

import care.data4life.sdk.migration.MigrationContract.CompatibilityEncoder.Companion.JS_LEGACY_ENCODING_EXCEPTIONS
import care.data4life.sdk.tag.TagEncoding
import care.data4life.sdk.tag.TaggingContract
import care.data4life.sdk.wrapper.URLEncoding
import care.data4life.sdk.wrapper.WrapperContract

internal object CompatibilityEncoder : MigrationContract.CompatibilityEncoder {
    private val tagEncoding: TaggingContract.Encoding = TagEncoding
    private val urlEncoding: WrapperContract.URLEncoding = URLEncoding

    private fun mapJSExceptions(encodedTag: String): String {
        var result = encodedTag

        JS_LEGACY_ENCODING_EXCEPTIONS.entries.forEach { replacement ->
            result = result.replace(replacement.key, replacement.value)
        }

        return result
    }

    override fun encode(tagValue: String): Triple<String, String, String> {
        val validEncoding = tagEncoding.encode(tagValue)
        val normalizedTag = tagEncoding.normalize(tagValue)
        val jsLegacyEncoding = mapJSExceptions(
            urlEncoding.encode(normalizedTag)
        )

        return Triple(
            validEncoding,
            normalizedTag,
            jsLegacyEncoding
        )
    }

    override fun normalize(tagValue: String): String = tagEncoding.normalize(tagValue)
}
