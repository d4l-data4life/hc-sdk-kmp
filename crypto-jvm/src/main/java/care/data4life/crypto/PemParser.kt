/*
 * BSD 3-Clause License
 *
 * Copyright (c) 2020, D4L data4life gGmbH
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *  Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 *  Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 *
 *  Neither the name of the copyright holder nor the names of its
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package care.data4life.crypto

import org.bouncycastle.util.io.pem.PemReader
import java.io.StringReader
import java.security.KeyFactory
import java.security.PublicKey
import java.security.interfaces.RSAPrivateCrtKey
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.RSAPublicKeySpec

/**
 * Convert a string encoding a private key in PEM format to a GCKeyPair.
 *
 * @param privateKeyAsPem   Private key encoded in PEM format
 * @param algorithm         Encryption algorithm to use
 * @param keySize           Encryption key size
 * @return GCKeyPair
 * @throws Exception        Covers the variety of possible errors during parsing
 */
@Throws(Exception::class)
fun convertPrivateKeyPemStringToGCKeyPair(
    privateKeyAsPem: String,
    algorithm: GCRSAKeyAlgorithm,
    keySize: Int
): GCKeyPair {
    val pemReader = PemReader(StringReader(privateKeyAsPem))
    val pemObject = pemReader.readPemObject()
    pemReader.close()
    val pemContent = pemObject.content
    val keyFactory = KeyFactory.getInstance(algorithm.cipher)
    val encodedKeySpec = PKCS8EncodedKeySpec(pemContent)
    val privateKey = keyFactory.generatePrivate(encodedKeySpec)
    val gcPrivateKey = GCAsymmetricKey(privateKey, GCAsymmetricKey.Type.Private)

    val rsaPrivCertKey = privateKey as RSAPrivateCrtKey
    val publicKeySpec = RSAPublicKeySpec(rsaPrivCertKey.modulus, rsaPrivCertKey.publicExponent)
    val publicKey: PublicKey = keyFactory.generatePublic(publicKeySpec)
    val gcPublicKey = GCAsymmetricKey(publicKey, GCAsymmetricKey.Type.Public)

    return GCKeyPair(algorithm, gcPrivateKey, gcPublicKey, keySize)
}
