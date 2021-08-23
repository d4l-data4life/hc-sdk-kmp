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

package care.data4life.sdk.crypto

import care.data4life.sdk.util.Base64
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import javax.crypto.spec.SecretKeySpec

internal class KeyFactory(
    private val base64: Base64
) : CryptoInternalContract.KeyFactory {

    override fun createGCKey(exchangeKey: ExchangeKey): GCKey {
        val algorithm: GCAESKeyAlgorithm = if (exchangeKey.type === KeyType.TAG_KEY) {
            GCAESKeyAlgorithm.createTagAlgorithm()
        } else {
            GCAESKeyAlgorithm.createDataAlgorithm()
        }

        val symmetricKey = GCSymmetricKey(
            SecretKeySpec(
                base64.decode(exchangeKey.symmetricKey!!),
                algorithm.transformation
            )
        )

        return GCKey(algorithm, symmetricKey, exchangeKey.getVersion().symmetricKeySize)
    }

    override fun createGCKeyPair(exchangeKey: ExchangeKey): GCKeyPair {
        val algorithm = GCRSAKeyAlgorithm()
        val privateKeyBase64 = exchangeKey.privateKey
        val publicKeyBase64 = exchangeKey.publicKey
        val pkcs8EncodedKeySpec = PKCS8EncodedKeySpec(base64.decode(privateKeyBase64!!))
        val x509EncodedKeySpec = X509EncodedKeySpec(base64.decode(publicKeyBase64!!))
        val keyFactory = java.security.KeyFactory.getInstance(algorithm.cipher)
        val privateKey = keyFactory.generatePrivate(pkcs8EncodedKeySpec)
        val publicKey = keyFactory.generatePublic(x509EncodedKeySpec)
        val gcPrivateKey = GCAsymmetricKey(privateKey, GCAsymmetricKey.Type.Private)
        val gcPublicKey = GCAsymmetricKey(publicKey, GCAsymmetricKey.Type.Public)

        val keyVersion = exchangeKey.getVersion().value

        return GCKeyPair(algorithm, gcPrivateKey, gcPublicKey, keyVersion)
    }
}
