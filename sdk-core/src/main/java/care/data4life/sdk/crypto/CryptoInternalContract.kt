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

package care.data4life.sdk.crypto

import java.io.IOException

internal class CryptoInternalContract {
    interface CommonKeyService {

        fun fetchCurrentCommonKeyId(): String

        @Throws(IOException::class)
        fun fetchCurrentCommonKey(): GCKey

        fun fetchCommonKey(commonKeyId: String): GCKey

        fun storeCurrentCommonKeyId(commonKeyId: String)

        fun storeCommonKey(commonKeyId: String, commonKey: GCKey)

        fun hasCommonKey(commonKeyId: String): Boolean

        companion object {
            const val DEFAULT_COMMON_KEY_ID = "00000000-0000-0000-0000-000000000000"
        }
    }

    interface KeyFactory {
        fun createGCKey(exchangeKey: ExchangeKey): GCKey
        fun createGCKeyPair(exchangeKey: ExchangeKey): GCKeyPair
    }
}
