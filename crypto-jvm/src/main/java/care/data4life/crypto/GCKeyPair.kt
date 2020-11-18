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


    private var privateKeyBase64: CharArray = charArrayOf()
    private var publicKeyBase64: CharArray = charArrayOf()


    @Transient
    var privateKey: GCAsymmetricKey? = null
        get() {
            if (field == null) {
                try {
                    val keyFactory = KeyFactory.getInstance(algorithm.cipher)
                    val encodedKeySpec = PKCS8EncodedKeySpec(Base64.decode(privateKeyBase64.toString()))
                    val privKey = keyFactory.generatePrivate(encodedKeySpec)
                    this.privateKey = GCAsymmetricKey(privKey, GCAsymmetricKey.Type.Private)
                } catch (e: Exception) {
                    e.printStackTrace()
                }

            }
            return field
        }
    @Transient
    var publicKey: GCAsymmetricKey? = null
        get() {
            if (field == null) {
                try {
                    val keyFactory = KeyFactory.getInstance(algorithm.cipher)
                    val pkcs1PublicKey = RSAPublicKey.getInstance(Base64.decode(publicKeyBase64.toString()))
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

    actual fun getPublicKeyBase64(): CharArray{
        return if(publicKeyBase64.isEmpty())
            Base64.encodeToString(String(publicKey!!.value.encoded)).toCharArray().also { publicKeyBase64 = it }
        else
            publicKeyBase64
    }



    actual fun getPrivateKeyBase64(): CharArray{
        return if(privateKeyBase64.isEmpty())
            Base64.encodeToString(String(privateKey!!.value.encoded)).toCharArray().also { privateKeyBase64 = it }
        else
            privateKeyBase64
    }

    init {
        this.privateKey = privateKey
        this.publicKey = publicKey
    }


}
