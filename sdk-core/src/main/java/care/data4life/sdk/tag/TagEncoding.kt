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
package care.data4life.sdk.tag

import care.data4life.sdk.lang.D4LException
import care.data4life.sdk.lang.DataValidationException
import care.data4life.sdk.tag.TaggingContract.Companion.LOCALE
import care.data4life.sdk.wrapper.URLEncoding
import java.util.Locale

object TagEncoding : TaggingContract.Encoding {
    @Throws(D4LException::class)
    private fun validateTag(tag: String) {
        if (tag.isBlank()) {
            throw DataValidationException.AnnotationViolation(
                "Annotation is empty."
            )
        }
    }

    @Throws(D4LException::class)
    override fun encode(
        tag: String
    ): String = URLEncoding.encode(normalize(tag)).toLowerCase(LOCALE)

    @Throws(D4LException::class)
    override fun normalize(tag: String): String {
        validateTag(tag)
        return tag.toLowerCase(LOCALE).trim()
    }

    override fun decode(
        encodedTag: String
    ): String = URLEncoding.decode(encodedTag)
}
