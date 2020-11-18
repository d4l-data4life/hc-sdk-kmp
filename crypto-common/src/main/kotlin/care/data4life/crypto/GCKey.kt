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

import care.data4life.crypto.security.SecretKeySpec
import care.data4life.sdk.util.Base64

class GCKey(val algorithm: GCAESKeyAlgorithm,
            private var symmetricKey: GCSymmetricKey?,
            val keyVersion: Int) {


    @Json("key")
    private var keyBase64: CharArray = charArrayOf()


    fun getSymmetricKey(): GCSymmetricKey {
        return symmetricKey
                ?: GCSymmetricKey(SecretKeySpec(Base64.decode(String(keyBase64)), algorithm.transformation)).also { symmetricKey = it }
    }

    @ExperimentalStdlibApi
    fun getKeyBase64(): CharArray {
        return if (keyBase64.isEmpty())
            Base64.encodeToString(symmetricKey!!.value.getEncoded()).toCharArray().also { keyBase64 = it };
        else
            keyBase64;
    }


    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is GCKey) return false

        if (algorithm != other.algorithm) return false
        if (symmetricKey != other.symmetricKey) return false
        if (keyVersion != other.keyVersion) return false

        return true
    }

    override fun hashCode(): Int {
        var result = algorithm.hashCode()
        result = 31 * result + (symmetricKey?.hashCode() ?: 0)
        result = 31 * result + (keyVersion)
        return result
    }

}
