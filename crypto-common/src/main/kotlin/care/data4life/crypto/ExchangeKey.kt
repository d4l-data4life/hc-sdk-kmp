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

@JsonClass(generateAdapter = true)
data class ExchangeKey constructor(
    @field:Json(name = "t") val type: KeyType,
    @field:Json(name = "priv") val privateKey: String?,
    @field:Json(name = "pub") val publicKey: String?,
    @field:Json(name = "sym") val symmetricKey: String?,
    @field:Json(name = "v") internal val version: Int?
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
}
