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

import care.data4life.crypto.Algorithm.Padding.NoPadding
import care.data4life.crypto.Algorithm.Padding.OAEPPadding
import care.data4life.crypto.Algorithm.Padding.PKCS7Padding
import care.data4life.sdk.util.Serializable

open class Algorithm : Serializable {

    var cipher: String? = null
        internal set
    protected open var blockMode: String = NONE
    protected open val padding: String
        get() = when (padding.toUpperCase()) {
            OAEP -> OAEPPadding.name
            PKCS7 -> PKCS7Padding.name
            else -> NoPadding.name
        }
    open val transformation: String
        get() = "$cipher/$blockMode/$padding"


    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Algorithm) return false

        if (cipher != other.cipher) return false
        if (blockMode != other.blockMode) return false

        return true
    }

    override fun hashCode(): Int {
        var result = cipher?.hashCode() ?: 0
        result = 31 * result + blockMode.hashCode()
        return result
    }


    enum class Cipher {
        AES, RSA
    }

    enum class BlockMode {
        CBC, ECB, GCM
    }

    enum class Padding {
        NoPadding, PKCS7Padding, OAEPPadding
    }

    companion object {
        private const val NONE = "None"
        private const val OAEP = "OAEP"
        private const val PKCS7 = "PKCS7"
    }

}

