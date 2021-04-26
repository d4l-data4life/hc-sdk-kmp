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
import care.data4life.sdk.tag.TaggingContract.Companion.DELIMITER
import okhttp3.internal.toHexString
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.Locale

object TagEncryptionHelper : TaggingContract.Helper {
    override fun convertToTagMap(tagList: List<String>): HashMap<String, String> {
        val tags = HashMap<String, String>()
        for (entry in tagList) {
            val split = entry.split(DELIMITER)
            if (split.size == 2) {
                val key = split[0]
                val value = split[1]
                if (key.isNotBlank() && value.isNotBlank()) {
                    tags[key] = value
                }
            }
        }
        return tags
    }

    private fun normalizeEncodedChar(char: Char): Char = char.toLowerCase()

    private fun replaceSpecial(char: Char): String {
        val specialChars = listOf('*', '-', '_', '.')
        return when (char) {
            '+' -> "%20"
            in specialChars -> "%${char.toInt().toHexString()}"
            else -> char.toString()
        }
    }

    @Throws(D4LException::class)
    private fun validateTag(tag: String) {
        if (tag.isBlank()) {
            throw DataValidationException.AnnotationViolation(
                "Annotation is empty."
            )
        }
    }

    @Throws(D4LException::class)
    override fun encode(tag: String): String {
        return URLEncoder.encode(
            normalize(tag),
            StandardCharsets.UTF_8.displayName()
        ).map { char ->
            replaceSpecial(
                normalizeEncodedChar(char)
            )
        }.joinToString("")
    }

    @Throws(D4LException::class)
    override fun normalize(tag: String): String {
        validateTag(tag)
        return tag.toLowerCase(Locale.US).trim()
    }

    override fun decode(
        encodedTag: String
    ): String = URLDecoder.decode(encodedTag, StandardCharsets.UTF_8.displayName())
}
