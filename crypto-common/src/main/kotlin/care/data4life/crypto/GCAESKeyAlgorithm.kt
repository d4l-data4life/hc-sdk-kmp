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

class GCAESKeyAlgorithm private constructor(cipher: Cipher, private val _padding: Padding, blockMode: BlockMode)
    : Algorithm() {

    init {
        this.cipher = cipher.name
        this.blockMode = blockMode.name
    }

    override val padding: String
        get() = _padding.name

    var iv: ByteArray? = null

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is GCAESKeyAlgorithm) return false
        if (!super.equals(other)) return false

        @Suppress("ReplaceArrayEqualityOpWithArraysEquals")
        if (iv != other.iv) return false

        if (iv != null && other.iv != null && !iv!!.contentEquals(other.iv!!)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + (iv?.let { items ->
            var hash = 0
            items.forEach { hash += it.hashCode() }
            return@let hash
        } ?: 0)
        return result
    }

    companion object {
        fun createDataAlgorithm() = GCAESKeyAlgorithm(Cipher.AES, Padding.NoPadding, BlockMode.GCM)
        fun createTagAlgorithm() = GCAESKeyAlgorithm(Cipher.AES, Padding.PKCS7Padding, BlockMode.CBC)
    }

}
