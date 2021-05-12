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

package care.data4life.sdk.test.util

import care.data4life.sdk.migration.MigrationContract.CompatibilityEncoder.Companion.JS_LEGACY_ENCODING_EXCEPTIONS
import java.util.Locale
import java.util.regex.Matcher
import java.util.regex.Pattern

object JSLegacyTagConverter {
    private val JS_LEGACY_ENCODING = Pattern.compile("(%[0-9][a-z])|(%[a-z][0-9])|(%[a-z][a-z])")

    private fun filterEncodings(rawEncodings: List<String>): Set<String> {
        return rawEncodings
            .filter { encoding -> encoding !in JS_LEGACY_ENCODING_EXCEPTIONS.values }
            .toSet()
    }

    private fun convertEncodings(rawEncodings: List<String>, tag: String): String {
        val cleanedEncodings = filterEncodings(rawEncodings)
        var alignedTag = tag

        cleanedEncodings.forEach { encoding ->
            alignedTag = alignedTag.replace(encoding, encoding.toUpperCase(Locale.US))
        }

        return alignedTag
    }

    private fun makeJSLegacyEncoding(matcher: Matcher, tag: String): String {
        val encodings: MutableList<String> = mutableListOf()
        do {
            encodings.add(matcher.group())
        } while (matcher.find())

        return convertEncodings(encodings, tag)
    }

    fun convertTag(tag: String): String {
        val matches = JS_LEGACY_ENCODING.matcher(tag)

        return if (matches.find()) makeJSLegacyEncoding(matches, tag) else tag
    }
}
