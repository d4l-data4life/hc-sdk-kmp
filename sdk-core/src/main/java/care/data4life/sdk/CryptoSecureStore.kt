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
package care.data4life.sdk

import care.data4life.crypto.ExchangeKey
import care.data4life.crypto.GCKey
import care.data4life.crypto.GCKeyPair
import care.data4life.crypto.KeyType
import care.data4life.crypto.error.CryptoException
import care.data4life.sdk.crypto.CryptoContract
import care.data4life.sdk.lang.D4LException
import care.data4life.securestore.SecureStoreContract
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import java.io.IOException
import java.lang.reflect.Type

class CryptoSecureStore @JvmOverloads constructor(
    private val moshi: Moshi = Moshi.Builder().build(),
    private val secureStore: SecureStoreContract.SecureStore
) : CryptoContract.SecureStore {
    override fun clear(): Unit = secureStore.clear()

    override fun storeSecret(
        alias: String,
        secret: CharArray
    ): Unit = secureStore.addData(alias, secret)

    @Throws(D4LException::class)
    override fun getSecret(alias: String): CharArray {
        return secureStore.getData(alias)
            ?: throw CryptoException.DecryptionFailed("Failed to decrypt data")
    }

    override fun <T : Any> storeSecret(alias: String, crate: T) {
        val type: Type = Types.getRawType(crate::class.java)
        val jsonData = moshi.adapter<Any>(type).toJson(crate)
        storeSecret(alias, jsonData.toCharArray())
    }

    @Throws(D4LException::class)
    override fun <T> getSecret(alias: String, type: Class<T>): T? {
        val data: CharArray = secureStore.getData(alias)
            ?: throw CryptoException.DecryptionFailed("Failed to decrypt data")

        return try {
            moshi.adapter(type).fromJson(String(data))
        } catch (e: IOException) {
            throw CryptoException.DecryptionFailed("Failed to decrypt data")
        }
    }

    override fun deleteSecret(alias: String): Unit = secureStore.removeData(alias)

    override fun storeKey(alias: String, key: GCKey, keyType: KeyType) {
        val exchangeKey = ExchangeKey(
            keyType,
            charArrayOf(),
            charArrayOf(),
            key.getKeyBase64(),
            key.keyVersion
        )
        val json = moshi.adapter(ExchangeKey::class.java).toJson(exchangeKey)
        secureStore.addData(alias, json.toCharArray())
    }

    override fun storeKey(alias: String, key: GCKeyPair) {
        val exchangeKey = ExchangeKey(
            KeyType.APP_PRIVATE_KEY,
            key.getPrivateKeyBase64(),
            key.getPublicKeyBase64(),
            charArrayOf(),
            key.keyVersion
        )
        val json = moshi.adapter(ExchangeKey::class.java).toJson(exchangeKey)
        secureStore.addData(alias, json.toCharArray())
    }

    @Throws(IOException::class)
    override fun getExchangeKey(alias: String): ExchangeKey {
        val data = secureStore.getData(alias)
            ?: throw (CryptoException.DecryptionFailed("Failed to decrypt data"))

        return moshi.adapter(ExchangeKey::class.java).fromJson(String(data))
            ?: throw (CryptoException.DecryptionFailed("Failed to decrypt data"))
    }

    override operator fun contains(alias: String): Boolean = secureStore.containsData(alias)
}
