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

class ExchangeKey constructor(
    @field:Json("t") val type: KeyType,
    @field:Json("priv") val privateKey: String?,
    @field:Json("pub") val publicKey: String?,
    @field:Json("sym") val symmetricKey: String?,
    @field:Json("v") private val version: Int?
) {

    constructor(
        type: KeyType,
        privateKey: String?,
        publicKey: String?,
        symmetricKey: String?,
        version: KeyVersion
    ) : this(type, privateKey, publicKey, symmetricKey, version.value)

    fun getVersion(): KeyVersion = when (version) {
        0 -> KeyVersion.VERSION_0
        1 -> KeyVersion.VERSION_1
        else -> KeyVersion.VERSION_1
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ExchangeKey) return false

        if (type != other.type) return false
        if (privateKey != other.privateKey) return false
        if (publicKey != other.publicKey) return false
        if (symmetricKey != other.symmetricKey) return false
        if (version != other.version) return false

        return true
    }

    override fun hashCode(): Int {
        var result = type.hashCode()
        result = 31 * result + (privateKey?.hashCode() ?: 0)
        result = 31 * result + (publicKey?.hashCode() ?: 0)
        result = 31 * result + (symmetricKey?.hashCode() ?: 0)
        result = 31 * result + (version ?: 0)
        return result
    }
}
