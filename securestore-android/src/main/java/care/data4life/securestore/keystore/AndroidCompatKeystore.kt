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

package care.data4life.securestore.keystore

import android.content.Context
import android.security.KeyPairGeneratorSpec
import care.data4life.sdk.util.Base64
import care.data4life.sdk.util.toBytes
import care.data4life.sdk.util.toChars
import care.data4life.securestore.BuildConfig
import care.data4life.securestore.security.AsymmetricKey
import care.data4life.securestore.security.SecretKey
import care.data4life.securestore.security.SymmetricKey
import care.data4life.securestore.storage.AndroidSharedPreferenceStorage
import java.math.BigInteger
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.PrivateKey
import java.util.Calendar
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.security.auth.x500.X500Principal

interface CompatKeystore {
    fun loadKey(): SymmetricKey

    fun clear()
}

class AndroidCompatKeystore(
    private val context: Context,
    private val keyStore: KeyStore = KeyStore.getInstance(KEYSTORE_PROVIDER),
    private val storage: AndroidSharedPreferenceStorage = AndroidSharedPreferenceStorage(
        context,
        PREFERENCE_NAME
    )
) : CompatKeystore {

    init {
        keyStore.load(null)
    }

    override fun loadKey(): SymmetricKey {
        var symmetricKey = loadSymmetricSecretKey()

        if (symmetricKey == null) {
            symmetricKey = createSymmetricKey()
        }

        return symmetricKey
    }

    override fun clear() {
        keyStore.deleteEntry(ASYMMETRIC_KEY_ALIAS)
        keyStore.deleteEntry(SYMMETRIC_KEY_ALIAS)
        storage.clear()
    }

    // #################################### SYM

    private fun loadSymmetricSecretKey(): SymmetricKey? {
        val asymmetricKey = getAsymmetricKey()

        var symmetricKey = loadSymmetricKey(asymmetricKey)

        if (symmetricKey == null) {
            symmetricKey = createSymmetricKey()
            storeSymmetricKey(symmetricKey, asymmetricKey)
        }

        return symmetricKey
    }

    private fun createSymmetricKey(): SymmetricKey {
        val keyGenerator = KeyGenerator.getInstance(SymmetricKey.KEY_ALGORITHM)

        keyGenerator.init(SymmetricKey.KEY_SIZE)

        return SymmetricKey(keyGenerator.generateKey())
    }

    private fun loadSymmetricKey(asymmetricKey: AsymmetricKey): SymmetricKey? {
        val encryptedSymmetricKey = storage.getData(SYMMETRIC_KEY_ALIAS)

        return if (encryptedSymmetricKey != null) {
            unwrapKey(encryptedSymmetricKey, asymmetricKey)
        } else {
            null
        }
    }

    private fun storeSymmetricKey(symmetricKey: SymmetricKey, asymmetricKey: AsymmetricKey) {
        val encryptedSymmetricKey = wrapKey(symmetricKey, asymmetricKey)

        storage.addData(SYMMETRIC_KEY_ALIAS, encryptedSymmetricKey)
    }

    // #################################### ASYM

    private fun getAsymmetricKey(): AsymmetricKey {
        var keyPair = getAsymmetricKeyPair()

        if (keyPair == null) {
            keyPair = createAsymmetricKey()
        }

        return AsymmetricKey(keyPair)
    }

    private fun getAsymmetricKeyPair(): KeyPair? {
        val privateKey = keyStore.getKey(ASYMMETRIC_KEY_ALIAS, null) as PrivateKey?
        val publicKey = keyStore.getCertificate(ASYMMETRIC_KEY_ALIAS)?.publicKey

        return if (privateKey != null && publicKey != null) {
            KeyPair(publicKey, privateKey)
        } else {
            null
        }
    }

    private fun createAsymmetricKey(): KeyPair {
        val keyGenerator = KeyPairGenerator.getInstance(
            AsymmetricKey.KEY_ALGORITHM,
            KEYSTORE_PROVIDER
        )

        initGeneratorForAsymmetricKeySpec(keyGenerator, ASYMMETRIC_KEY_ALIAS)

        return keyGenerator.generateKeyPair()
    }

    private fun initGeneratorForAsymmetricKeySpec(generator: KeyPairGenerator, alias: String) {
        val startDate = Calendar.getInstance()
        val endDate = Calendar.getInstance()
        endDate.add(Calendar.YEAR, 25)

        @Suppress("DEPRECATION")
        val builder = KeyPairGeneratorSpec.Builder(context)
            .setAlias(alias)
            .setSerialNumber(BigInteger.ONE)
            .setSubject(X500Principal(ENCRYPTION_KEY_SUBJECT))
            .setKeySize(AsymmetricKey.KEY_SIZE)
            .setStartDate(startDate.time)
            .setEndDate(endDate.time)

        generator.initialize(builder.build())
    }

    // #################################### WRAP

    private fun wrapKey(symmetricKey: SymmetricKey, wrapperKey: AsymmetricKey): CharArray {
        val cipher = Cipher.getInstance(AsymmetricKey.KEY_TRANSFORMATION)
        cipher.init(Cipher.WRAP_MODE, wrapperKey.getPublicKey())

        return Base64.encode(cipher.wrap(symmetricKey.getKey())).toChars()
    }

    private fun unwrapKey(symmetricKeyData: CharArray, wrapperKey: AsymmetricKey): SymmetricKey {
        val cipher = Cipher.getInstance(AsymmetricKey.KEY_TRANSFORMATION)
        cipher.init(Cipher.UNWRAP_MODE, wrapperKey.getPrivateKey())

        val decodedKey = Base64.decode(symmetricKeyData.toBytes())

        val key = cipher.unwrap(
            decodedKey,
            SymmetricKey.KEY_ALGORITHM,
            Cipher.SECRET_KEY
        ) as SecretKey

        return SymmetricKey(key)
    }

    companion object {
        internal const val PREFERENCE_NAME = BuildConfig.LIBRARY_PACKAGE_NAME + ".storage.keystore"

        internal const val ENCRYPTION_KEY_SUBJECT =
            "CN=${BuildConfig.LIBRARY_PACKAGE_NAME} asymmetric"

        const val KEYSTORE_PROVIDER = "AndroidKeyStore"

        const val ASYMMETRIC_KEY_ALIAS = "securestore_asymmetric_key"
        const val SYMMETRIC_KEY_ALIAS = "securestore_symmetric_keychain_key"
    }
}
