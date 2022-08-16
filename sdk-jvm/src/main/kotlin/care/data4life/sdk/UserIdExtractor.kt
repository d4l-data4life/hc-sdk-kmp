/*
 * Copyright (c) 2022 D4L data4life gGmbH / All rights reserved.
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

package care.data4life.sdk

import care.data4life.sdk.auth.AuthState
import care.data4life.sdk.util.Base64
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi.Builder
import java.io.IOException

/**
 * With the [UserIdExtractor.extract] method the SDK provides the
 * functionality for extracting the user id of a given OAuth callback url.
 */
class UserIdExtractor @JvmOverloads constructor(
    private val urlDecoder: UrlDecoder = UrlDecoder,
    private val base64: Base64 = Base64,
    private val adapter: JsonAdapter<AuthState> = Builder().build().adapter(AuthState::class.java)
) {
    /**
     * Extract the user id from the given OAuth callback url
     *
     * @param callbackUrl the callback url from the OAuth flow with the state and code query params
     * @return the user id or null
     */
    fun extract(callbackUrl: String): String? {
        val stateString = extractStateString(callbackUrl)
        if (stateString.isEmpty()) {
            return null
        }
        val decodedUrl = urlDecoder.decode(stateString)
        val dataString = base64.decodeToString(decodedUrl)
        val state: AuthState?
        return try {
            state = adapter.fromJson(dataString)
            state!!.alias
        } catch (e: IOException) {
            null
        }
    }

    private fun extractStateString(url: String): String {
        val query = url.substring(url.indexOf(QUESTION_MARK) + 1)
        val params = query.split(AMPERSAND).toTypedArray()
        for (keyValue in params) {
            if (keyValue.contains(STATE)) {
                return keyValue.split(EQUALS).toTypedArray()[1]
            }
        }
        return ""
    }

    companion object {
        private const val STATE = "state"
        private const val EQUALS = "="
        private const val AMPERSAND = "&"
        private const val QUESTION_MARK = '?'
    }
}
