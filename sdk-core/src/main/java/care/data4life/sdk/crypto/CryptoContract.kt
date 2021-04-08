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

import care.data4life.crypto.ExchangeKey
import care.data4life.crypto.GCKey
import care.data4life.crypto.GCKeyPair
import care.data4life.crypto.KeyType
import care.data4life.sdk.lang.D4LException
import java.io.IOException

class CryptoContract {
    interface CryptoSecureStore {
        fun clear()
        fun storeSecret(alias: String, secret: CharArray)

        @Throws(D4LException::class)
        fun getSecret(alias: String): CharArray
        fun <T : Any> storeSecret(alias: String, crate: T)

        @Throws(D4LException::class)
        fun <T> getSecret(alias: String, type: Class<T>): T?
        fun deleteSecret(alias: String)
        fun storeKey(alias: String, key: GCKey, keyType: KeyType)
        fun storeKey(alias: String, key: GCKeyPair)

        @Throws(IOException::class)
        fun getExchangeKey(alias: String): ExchangeKey

        operator fun contains(alias: String): Boolean
    }

    interface KeyFactory {
        fun createGCKey(exchangeKey: ExchangeKey): GCKey
        fun createGCKeyPair(exchangeKey: ExchangeKey): GCKeyPair
    }
}

