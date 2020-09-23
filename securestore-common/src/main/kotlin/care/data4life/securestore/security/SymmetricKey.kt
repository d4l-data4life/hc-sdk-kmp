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

package care.data4life.securestore.security

class SymmetricKey(
        private val key: SecretKey
) {

    fun getKey() = key


    companion object {
        //TODO they should be platform specific

        const val KEY_ALGORITHM = "AES"
        const val KEY_BLOCK_MODE = "GCM"
        const val KEY_PADDING = "NoPadding"
        const val KEY_SIZE = 256
        const val KEY_IV_TAG_LENGTH = 128
        const val KEY_IV_SIZE = 12

        // alternative AES/GCM/NoPadding

        const val KEY_TRANSFORMATION = "$KEY_ALGORITHM/$KEY_BLOCK_MODE/$KEY_PADDING"
    }
}
