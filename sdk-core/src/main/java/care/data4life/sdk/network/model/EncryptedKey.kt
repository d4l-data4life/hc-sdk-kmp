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
package care.data4life.sdk.network.model

import care.data4life.sdk.util.Base64

data class EncryptedKey internal constructor(
    override val base64Key: String
): NetworkModelContract.EncryptedKey {
    override fun decode(): ByteArray = Base64.decode(base64Key)

    companion object Factory: NetworkModelContract.EncryptedKeyMaker {
        @JvmStatic
        override fun create(key: ByteArray): NetworkModelContract.EncryptedKey {
            return EncryptedKey(
                Base64.encodeToString(key)
            )
        }
    }
}
