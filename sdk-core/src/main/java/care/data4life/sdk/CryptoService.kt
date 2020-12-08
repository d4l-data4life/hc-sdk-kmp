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

import care.data4life.crypto.CryptoProtocol
import care.data4life.crypto.ExchangeKey
import care.data4life.crypto.ExchangeKeyFactory.createKey
import care.data4life.crypto.GCAESKeyAlgorithm
import care.data4life.crypto.GCAsymmetricKey
import care.data4life.crypto.GCKey
import care.data4life.crypto.GCKeyPair
import care.data4life.crypto.GCRSAKeyAlgorithm
import care.data4life.crypto.KeyOptions
import care.data4life.crypto.KeyType
import care.data4life.crypto.KeyVersion
import care.data4life.crypto.convertPrivateKeyPemStringToGCKeyPair
import care.data4life.crypto.error.CryptoException
import care.data4life.sdk.crypto.CommonKeyService
import care.data4life.sdk.crypto.KeyFactory
import care.data4life.sdk.lang.D4LException
import care.data4life.sdk.lang.D4LRuntimeException
import care.data4life.sdk.log.Log
import care.data4life.sdk.network.model.EncryptedKey
import care.data4life.sdk.util.Base64
import com.squareup.moshi.Moshi
import io.reactivex.Single
import java.io.IOException
import java.nio.charset.Charset
import java.security.SecureRandom

internal open class CryptoService : CryptoProtocol {

    private val moshi: Moshi
    private var alias: String
    private var storage: CryptoSecureStore
    private var rng: SecureRandom
    private var base64: Base64
    private var keyFactory: KeyFactory
    private var commonKeyService: CommonKeyService

    // for testing only
    constructor(
            alias: String,
            storage: CryptoSecureStore,
            moshi: Moshi,
            rng: SecureRandom,
            base64: Base64,
            keyFactory: KeyFactory,
            commonKeyService: CommonKeyService
    ) {
        this.alias = alias
        this.storage = storage
        this.moshi = moshi
        this.rng = rng
        this.base64 = base64
        this.keyFactory = keyFactory
        this.commonKeyService = commonKeyService
    }

    constructor(
            alias: String,
            storage: CryptoSecureStore
    ) {
        this.alias = alias
        this.storage = storage
        moshi = Moshi.Builder().build()
        rng = SecureRandom()
        base64 = Base64
        keyFactory = KeyFactory(base64)
        commonKeyService = CommonKeyService(alias, storage, keyFactory)
    }

    fun encrypt(key: GCKey, data: ByteArray): Single<ByteArray> {
        return Single
                .fromCallable { data }
                .map { dataArray ->
                    val iv = ByteArray(IV_SIZE)
                    rng.nextBytes(iv)
                    val ciphertext = symEncrypt(key, dataArray, iv)
                    val result = ByteArray(iv.size + ciphertext.size)
                    System.arraycopy(iv, 0, result, 0, iv.size)
                    System.arraycopy(ciphertext, 0, result, iv.size, ciphertext.size)
                    result
                }
                .onErrorResumeNext { error ->
                    Log.error(error, "Failed to encrypt data")
                    Single.error(CryptoException.EncryptionFailed("Failed to encrypt data") as D4LException)
                }
    }

    fun decrypt(key: GCKey, data: ByteArray): Single<ByteArray> {
        return Single
                .fromCallable { data }
                .map { dataArray ->
                    val iv = dataArray.copyOfRange(0, IV_SIZE)
                    val cipher = dataArray.copyOfRange(IV_SIZE, data.size)
                    symDecrypt(key, cipher, iv)
                }
                .onErrorResumeNext { error ->
                    Log.error(error, "Failed to decrypt data")
                    Single.error(CryptoException.DecryptionFailed("Failed to decrypt data") as D4LException)
                }
    }

    fun encryptString(key: GCKey, data: String): Single<String> {
        return encrypt(key, data.toByteArray())
                .map { dataArray -> base64.encodeToString(dataArray) }
                .onErrorResumeNext { error ->
                    Log.error(error, "Failed to encrypt string")
                    Single.error(CryptoException.EncryptionFailed("Failed to encrypt string") as D4LException)
                }
    }

    fun decryptString(key: GCKey, dataBase64: String): Single<String> {
        return Single
                .fromCallable { base64.decode(dataBase64) }
                .flatMap { decoded: ByteArray -> decrypt(key, decoded) }
                .map { decrypted: ByteArray -> String(decrypted, Charset.forName("UTF-8")) }
                .onErrorResumeNext { error ->
                    Log.error(error, "Failed to decrypt string")
                    Single.error(CryptoException.DecryptionFailed("Failed to decrypt string") as D4LException)
                }
    }

    fun encryptSymmetricKey(key: GCKey, keyType: KeyType, gckey: GCKey): Single<EncryptedKey> {
        return Single.fromCallable { createKey(KEY_VERSION, keyType, gckey.getKeyBase64()) }
                .map { exchangeKey -> moshi.adapter(ExchangeKey::class.java).toJson(exchangeKey) }
                .flatMap { jsonKey -> encryptString(key, jsonKey) }
                .map<EncryptedKey> { encryptedKeyBase64 -> EncryptedKey(encryptedKeyBase64) }
                .onErrorResumeNext { error ->
                    Log.error(error, "Failed to encrypt GcKey")
                    Single.error(CryptoException.KeyEncryptionFailed("Failed to encrypt GcKey") as D4LException)
                }
    }

    fun symDecryptSymmetricKey(key: GCKey, encryptedKey: EncryptedKey): Single<GCKey> {
        return decryptString(key, encryptedKey.encryptedKey)
                .map { keyJson -> moshi.adapter(ExchangeKey::class.java).fromJson(keyJson) }
                .flatMap { exchangeKey -> convertExchangeKeyToGCKey(exchangeKey) }
    }

    fun asymDecryptSymetricKey(keyPair: GCKeyPair, encryptedKey: EncryptedKey): Single<GCKey> {
        return Single.fromCallable { base64.decode(encryptedKey.encryptedKey) }
                .map { decoded -> asymDecrypt(keyPair, decoded) }
                .map { keyJson -> moshi.adapter(ExchangeKey::class.java).fromJson(String(keyJson, Charset.forName("UTF-8"))) }
                .flatMap { exchangeKey -> convertExchangeKeyToGCKey(exchangeKey) }
    }

    private fun convertExchangeKeyToGCKey(exchangeKey: ExchangeKey): Single<GCKey> {
        return Single.fromCallable { exchangeKey }
                .map { exchKey ->
                    if (exchKey.getVersion() !== KeyVersion.VERSION_1) {
                        throw (CryptoException.InvalidKeyVersion(exchKey.getVersion().value) as D4LException)
                    } else if (exchKey.type === KeyType.APP_PUBLIC_KEY || exchKey.type === KeyType.APP_PRIVATE_KEY) {
                        throw (CryptoException.KeyDecryptionFailed("can't decrypt asymmetric to symmetric key") as D4LException)
                    }
                    keyFactory.createGCKey(exchKey)
                }
                .onErrorResumeNext { error ->
                    Log.error(error, "Failed to decrypt exchangeKey")
                    if (error is D4LException) {
                        return@onErrorResumeNext Single.error<GCKey>(error)
                    }
                    Single.error(CryptoException.KeyDecryptionFailed("Failed to decrypt exchange key") as D4LException)
                }
    }

    fun convertAsymmetricKeyToBase64ExchangeKey(gcAsymmetricKey: GCAsymmetricKey): Single<String> {
        return Single.fromCallable { gcAsymmetricKey.value.encoded } //SPKI
                .map(base64::encodeToString)
                .map { encodedKey -> createKey(KEY_VERSION, KeyType.APP_PUBLIC_KEY, encodedKey) }
                .map { exchangeKey: ExchangeKey -> moshi.adapter(ExchangeKey::class.java).toJson(exchangeKey) }
                .map { data: String -> base64.encodeToString(data) }
    }

    fun generateGCKey(): Single<GCKey> {
        return Single
                .fromCallable { GCAESKeyAlgorithm.createDataAlgorithm() }
                .map { algorithm ->
                    val options = KeyOptions(KEY_VERSION.symmetricKeySize)
                    generateSymKey(algorithm, options)
                }
                .onErrorResumeNext { error ->
                    Log.error(error, "Failed to generate encryption key")
                    Single.error(CryptoException.KeyGenerationFailed("Failed to generate encryption key") as D4LException)
                }
    }

    fun generateGCKeyPair(): Single<GCKeyPair> {
        return Single
                .fromCallable { GCRSAKeyAlgorithm() }
                .map { algorithm ->
                    deleteGCKeyPair()
                    val options = KeyOptions(KEY_VERSION.asymmetricKeySize, GC_KEYPAIR)
                    generateAsymKeyPair(algorithm, options)
                }
                .map { gcKeyPair ->
                    saveGCKeyPair(gcKeyPair)
                    gcKeyPair
                }
    }

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
    fun setGCKeyPairFromPemPrivateKey(privateKeyAsPem: String) {
        try {
            // Removing any existing key pair
            deleteGCKeyPair()
            // Create a new key pair based on the passed private key
            val algorithm = GCRSAKeyAlgorithm()
            val gcKeyPair = convertPrivateKeyPemStringToGCKeyPair(privateKeyAsPem, algorithm, KEY_VERSION.asymmetricKeySize)
            // Store new key pair
            saveGCKeyPair(gcKeyPair)
        } catch (e: Exception) {
            Log.error(e, "Error during PEM key parsing")
            throw D4LRuntimeException("Error during PEM key parsing")
        }
    }

    fun saveGCKeyPair(keyPair: GCKeyPair?) {
        storage.storeKey(prefix() + GC_KEYPAIR, keyPair)
    }

    fun deleteGCKeyPair() {
        deleteSecret(GC_KEYPAIR)
    }

    fun fetchGCKeyPair(): Single<GCKeyPair> {
        return Single
                .fromCallable { getExchangeKey(GC_KEYPAIR) }
                .map { exchangeKey -> keyFactory.createGCKeyPair(exchangeKey) }
                .onErrorResumeNext { error ->
                    Log.error(error, "Failed to fetch encryption key")
                    Single.error(CryptoException.KeyFetchingFailed("Failed to fetch encryption key") as D4LException)
                }
    }

    fun storeTagEncryptionKey(tek: GCKey) {
        storeKey(TEK_KEY, tek, KeyType.TAG_KEY)
    }

    @Throws(IOException::class)
    fun fetchTagEncryptionKey(): GCKey {
        return getGCKey(TEK_KEY)
    }

    private fun storeKey(key: String, value: GCKey, keyType: KeyType) {
        storage.storeKey(prefix() + key, value, keyType)
    }

    @Throws(IOException::class)
    private fun getGCKey(key: String): GCKey {
        return keyFactory.createGCKey(getExchangeKey(key))
    }

    @Throws(IOException::class)
    private fun getExchangeKey(key: String): ExchangeKey {
        return storage.getExchangeKey(prefix() + key)
    }

    private fun deleteSecret(key: String) {
        storage.deleteSecret(prefix() + key)
    }

    private fun prefix(): String {
        return alias + "_"
    }

    // Common Key Handling
    @Throws(IOException::class)
    fun fetchCurrentCommonKey(): GCKey? {
        return commonKeyService.fetchCurrentCommonKey()
    }

    @Throws(IOException::class)
    fun getCommonKeyById(commonKeyId: String): GCKey {
        return commonKeyService.fetchCommonKey(commonKeyId)
    }

    val currentCommonKeyId: String?
        get() = commonKeyService.fetchCurrentCommonKeyId()

    fun storeCurrentCommonKeyId(commonKeyId: String) {
        commonKeyService.storeCurrentCommonKeyId(commonKeyId)
    }

    fun storeCommonKey(commonKeyId: String, commonKey: GCKey) {
        commonKeyService.storeCommonKey(commonKeyId, commonKey)
    }

    fun hasCommonKey(commonKeyId: String): Boolean {
        return commonKeyService.hasCommonKey(commonKeyId)
    }

    companion object {
        private const val TEK_KEY = "crypto_tag_encryption_key"
        private const val GC_KEYPAIR = "crypto_gc_keypair"
        private val KEY_VERSION = KeyVersion.VERSION_1
        private const val IV_SIZE = 12
    }
}
