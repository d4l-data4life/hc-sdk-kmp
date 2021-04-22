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

package care.data4life.crypto

import org.bouncycastle.jce.provider.BouncyCastleProvider
import java.security.InvalidAlgorithmParameterException
import java.security.InvalidKeyException
import java.security.KeyPairGenerator
import java.security.NoSuchAlgorithmException
import java.security.NoSuchProviderException
import java.security.SecureRandom
import java.security.Security
import javax.crypto.BadPaddingException
import javax.crypto.Cipher
import javax.crypto.IllegalBlockSizeException
import javax.crypto.KeyGenerator
import javax.crypto.NoSuchPaddingException
import javax.crypto.spec.IvParameterSpec

actual abstract class CryptoProtocol {

    @Throws(
        BadPaddingException::class,
        IllegalBlockSizeException::class,
        NoSuchPaddingException::class,
        NoSuchAlgorithmException::class,
        InvalidAlgorithmParameterException::class,
        InvalidKeyException::class,
        NoSuchProviderException::class
    )
    actual fun symEncrypt(key: GCKey, data: ByteArray, iv: ByteArray): ByteArray {
        val cipher = createCypher(key.algorithm.transformation)
        val spec = IvParameterSpec(iv)
        cipher.init(Cipher.ENCRYPT_MODE, key.getSymmetricKey().value, spec)
        return cipher.doFinal(data)
    }

    @Throws(
        InvalidAlgorithmParameterException::class,
        InvalidKeyException::class,
        NoSuchPaddingException::class,
        NoSuchAlgorithmException::class,
        BadPaddingException::class,
        IllegalBlockSizeException::class,
        NoSuchProviderException::class
    )
    actual fun symDecrypt(key: GCKey, data: ByteArray, iv: ByteArray): ByteArray {
        val cipher = createCypher(key.algorithm.transformation)
        val spec = IvParameterSpec(iv)
        cipher.init(Cipher.DECRYPT_MODE, key.getSymmetricKey().value, spec)
        return cipher.doFinal(data)
    }

    @Throws(
        NoSuchPaddingException::class,
        NoSuchAlgorithmException::class,
        BadPaddingException::class,
        IllegalBlockSizeException::class,
        InvalidKeyException::class,
        NoSuchProviderException::class
    )
    actual fun asymEncrypt(key: GCKeyPair, data: ByteArray): ByteArray {
        val cipher = createCypher(key.algorithm.transformation)
        cipher.init(Cipher.ENCRYPT_MODE, key.publicKey!!.value)
        return cipher.doFinal(data)
    }

    @Throws(
        NoSuchPaddingException::class,
        NoSuchAlgorithmException::class,
        InvalidKeyException::class,
        BadPaddingException::class,
        IllegalBlockSizeException::class,
        NoSuchProviderException::class
    )
    actual fun asymDecrypt(key: GCKeyPair, data: ByteArray): ByteArray {
        val cipher = createCypher(key.algorithm.transformation)
        cipher.init(Cipher.DECRYPT_MODE, key.privateKey!!.value)
        return cipher.doFinal(data)
    }

    @Throws(NoSuchAlgorithmException::class, NoSuchProviderException::class)
    actual fun generateAsymKeyPair(algorithm: GCRSAKeyAlgorithm, options: KeyOptions): GCKeyPair {
        val keyPairGenerator = KeyPairGenerator.getInstance(algorithm.cipher)
        keyPairGenerator.initialize(options.keySize, SecureRandom())
        val keyPair = keyPairGenerator.generateKeyPair()
        return GCKeyPair(
            algorithm,
            GCAsymmetricKey(keyPair.private, GCAsymmetricKey.Type.Private),
            GCAsymmetricKey(keyPair.public, GCAsymmetricKey.Type.Public),
            options.keySize
        )
    }

    @Throws(NoSuchAlgorithmException::class, NoSuchProviderException::class)
    actual fun generateSymKey(algorithm: GCAESKeyAlgorithm, options: KeyOptions): GCKey {
        val keyGenerator = createKeyGenerator(algorithm.cipher)
        keyGenerator.init(options.keySize)
        val key = keyGenerator.generateKey()
        return GCKey(algorithm, GCSymmetricKey(key), options.keySize)
    }

    @Throws(
        NoSuchPaddingException::class,
        NoSuchAlgorithmException::class,
        NoSuchProviderException::class
    )
    open fun createCypher(transformation: String): Cipher {
        return Cipher.getInstance(transformation, BouncyCastleProvider.PROVIDER_NAME)
    }

    @Throws(NoSuchProviderException::class, NoSuchAlgorithmException::class)
    open fun createKeyGenerator(algorithm: String?): KeyGenerator {
        return KeyGenerator.getInstance(algorithm, BouncyCastleProvider.PROVIDER_NAME)
    }

    companion object {

        init {
            Security.removeProvider(BouncyCastleProvider.PROVIDER_NAME)
            Security.addProvider(BouncyCastleProvider())
        }
    }
}
