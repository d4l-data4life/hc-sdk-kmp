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
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
actual data class GCKey(
    val algorithm: GCAESKeyAlgorithm,
    internal var symmetricKey: GCSymmetricKey?,
    val keyVersion: Int
) {

    @field:Json(name = "key")
    internal var keyBase64: String? = null

    fun getSymmetricKey(): GCSymmetricKey {
        return symmetricKey
            ?: GCSymmetricKey(
                SecretKeySpec(
                    Base64.decode(keyBase64!!),
                    algorithm.transformation
                )
            ).also { symmetricKey = it }
    }

    fun getKeyBase64(): String = keyBase64
        ?: Base64.encodeToString(symmetricKey!!.value.encoded).also { keyBase64 = it }
}
