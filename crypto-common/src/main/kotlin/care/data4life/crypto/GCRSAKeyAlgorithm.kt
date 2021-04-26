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

import care.data4life.crypto.GCRSAKeyAlgorithm.Hash.SHA256

class GCRSAKeyAlgorithm : Algorithm() {

    val hash: String

    enum class Hash {
        SHA256
    }

    override val padding: String
        get() = Algorithm.Padding.OAEPPadding.name

    init {
        cipher = Algorithm.Cipher.RSA.name
        blockMode = Algorithm.BlockMode.ECB.name
        this.hash = SHA256.name
    }

    override val transformation: String
        get() {
            var padding = padding
            if (padding == Algorithm.Padding.OAEPPadding.name || padding == OAEP) {
                if (hash == SHA256.name || hash == SHA_256) {
                    padding = OAEP_PADDING
                }
            }
            return "$cipher/$blockMode/$padding"
        }

    companion object {
        private const val OAEP = "OAEP"
        private const val OAEP_PADDING = "OAEPWithSHA-256AndMGF1Padding"
        private const val SHA_256 = "SHA-256"
    }
}
