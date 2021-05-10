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
import care.data4life.crypto.GCAsymmetricKey
import care.data4life.crypto.GCKey
import care.data4life.crypto.GCKeyPair
import care.data4life.crypto.KeyType
import care.data4life.crypto.KeyVersion
import care.data4life.sdk.lang.D4LException
import care.data4life.sdk.lang.D4LRuntimeException
import care.data4life.sdk.network.model.NetworkModelContract
import io.reactivex.Single
import java.io.IOException
import java.security.InvalidAlgorithmParameterException
import java.security.InvalidKeyException
import java.security.NoSuchAlgorithmException
import java.security.NoSuchProviderException
import javax.crypto.BadPaddingException
import javax.crypto.IllegalBlockSizeException
import javax.crypto.NoSuchPaddingException

class CryptoContract {
    interface SecureStore {
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

    interface Service {
        val currentCommonKeyId: String

        fun encrypt(key: GCKey, data: ByteArray): Single<ByteArray>
        fun decrypt(key: GCKey, data: ByteArray): Single<ByteArray>
        fun encryptAndEncodeString(key: GCKey, data: String): Single<String>
        fun decodeAndDecryptString(key: GCKey, dataBase64: String): Single<String>
        fun encryptSymmetricKey(
            key: GCKey,
            keyType: KeyType,
            gckey: GCKey
        ): Single<NetworkModelContract.EncryptedKey>

        fun symDecryptSymmetricKey(
            key: GCKey,
            encryptedKey: NetworkModelContract.EncryptedKey
        ): Single<GCKey>

        fun asymDecryptSymetricKey(
            keyPair: GCKeyPair,
            encryptedKey: NetworkModelContract.EncryptedKey
        ): Single<GCKey>

        fun convertAsymmetricKeyToBase64ExchangeKey(gcAsymmetricKey: GCAsymmetricKey): Single<String>
        fun generateGCKey(): Single<GCKey>
        fun generateGCKeyPair(): Single<GCKeyPair>

        /**
         * Set the stored asymmetric key pair to have the private key equal to the passed key and
         * a random, non-matching public key.
         *
         *
         * This is meant for usage in scenarios where keys are generated and managed externally.
         *
         * @param privateKeyAsPem Private key encoded in PEM format
         * @throws D4LRuntimeException Thrown if any step of the parsing fails
         */
        @Throws(D4LRuntimeException::class)
        fun setGCKeyPairFromPemPrivateKey(privateKeyAsPem: String)
        fun fetchGCKeyPair(): Single<GCKeyPair>
        fun storeTagEncryptionKey(tek: GCKey)

        @Throws(IOException::class)
        fun fetchTagEncryptionKey(): GCKey

        // Common Key Handling
        @Throws(IOException::class)
        fun fetchCurrentCommonKey(): GCKey

        @Throws(IOException::class)
        fun getCommonKeyById(commonKeyId: String): GCKey
        fun storeCurrentCommonKeyId(commonKeyId: String)
        fun storeCommonKey(commonKeyId: String, commonKey: GCKey)
        fun hasCommonKey(commonKeyId: String): Boolean

        @Throws(
            BadPaddingException::class,
            IllegalBlockSizeException::class,
            NoSuchPaddingException::class,
            NoSuchAlgorithmException::class,
            InvalidAlgorithmParameterException::class,
            InvalidKeyException::class,
            NoSuchProviderException::class
        )
        fun symEncrypt(key: GCKey, data: ByteArray, iv: ByteArray): ByteArray

        @Throws(
            InvalidAlgorithmParameterException::class,
            InvalidKeyException::class,
            NoSuchPaddingException::class,
            NoSuchAlgorithmException::class,
            BadPaddingException::class,
            IllegalBlockSizeException::class,
            NoSuchProviderException::class
        )
        fun symDecrypt(key: GCKey, data: ByteArray, iv: ByteArray): ByteArray

        companion object {
            const val TEK_KEY = "crypto_tag_encryption_key"
            const val GC_KEYPAIR = "crypto_gc_keypair"
            val KEY_VERSION = KeyVersion.VERSION_1
            const val IV_SIZE = 12
        }
    }

    internal interface CommonKeyService {

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

    internal interface KeyFactory {
        fun createGCKey(exchangeKey: ExchangeKey): GCKey
        fun createGCKeyPair(exchangeKey: ExchangeKey): GCKeyPair
    }
}
