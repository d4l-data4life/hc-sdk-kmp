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

import care.data4life.sdk.util.Base64
import care.data4life.sdk.util.Serializable
import org.bouncycastle.asn1.pkcs.RSAPublicKey
import java.security.KeyFactory
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.RSAPublicKeySpec

actual class GCKeyPair actual constructor(
        val algorithm: GCRSAKeyAlgorithm,
        privateKey: GCAsymmetricKey,
        publicKey: GCAsymmetricKey,
        val keyVersion: Int
) : Serializable {


    private var privateKeyBase64: String? = null

    @Transient
    var privateKey: GCAsymmetricKey? = null
        get() {
            if (field == null) {
                try {
                    val keyFactory = KeyFactory.getInstance(algorithm.cipher)
                    val encodedKeySpec = PKCS8EncodedKeySpec(Base64.decode(privateKeyBase64!!))
                    val privKey = keyFactory.generatePrivate(encodedKeySpec)
                    this.privateKey = GCAsymmetricKey(privKey, GCAsymmetricKey.Type.Private)
                } catch (e: Exception) {
                    e.printStackTrace()
                }

            }
            return field
        }
    private var publicKeyBase64: String? = null

    // TODO move to test
    @Transient
    var publicKey: GCAsymmetricKey? = null
        get() {
            if (field == null) {
                try {
                    val keyFactory = KeyFactory.getInstance(algorithm.cipher)
                    val pkcs1PublicKey = RSAPublicKey.getInstance(Base64.decode(publicKeyBase64!!))
                    val modulus = pkcs1PublicKey.modulus
                    val publicExponent = pkcs1PublicKey.publicExponent
                    val pubKeySpec = RSAPublicKeySpec(modulus, publicExponent)
                    val pubKey = keyFactory.generatePublic(pubKeySpec)
                    this.publicKey = GCAsymmetricKey(pubKey, GCAsymmetricKey.Type.Public)
                } catch (e: Exception) {
                    e.printStackTrace()
                }

            }
            return field
        }

    init {
        this.privateKey = privateKey
        this.publicKey = publicKey
    }

    actual fun getPublicKeyBase64(): String = publicKeyBase64
            ?: Base64.encodeToString(publicKey!!.value.encoded).also { publicKeyBase64 = it }

    actual fun getPrivateKeyBase64(): String = privateKeyBase64
            ?: Base64.encodeToString(privateKey!!.value.encoded).also { privateKeyBase64 = it }

}
