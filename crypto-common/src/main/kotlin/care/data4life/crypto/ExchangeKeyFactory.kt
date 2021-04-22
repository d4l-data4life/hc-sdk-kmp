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

import care.data4life.crypto.error.CryptoException

object ExchangeKeyFactory {

    fun createKey(keyVersion: KeyVersion, type: KeyType, encryptedKeyBase64: String): ExchangeKey {
        if (keyVersion !== KeyVersion.VERSION_1) {
            throw CryptoException.InvalidKeyVersion(keyVersion.value)
        }

        return when (type) {
            KeyType.APP_PUBLIC_KEY -> createPublicAppKey(encryptedKeyBase64)
            KeyType.COMMON_KEY -> createCommonKey(encryptedKeyBase64)
            KeyType.DATA_KEY -> createDataKey(encryptedKeyBase64)
            KeyType.ATTACHMENT_KEY -> createAttachmentKey(encryptedKeyBase64)
            KeyType.TAG_KEY -> createTagKey(encryptedKeyBase64)

            else -> throw CryptoException.InvalidKeyType(type.name)
        }
    }

    private fun createPublicAppKey(encryptedAppPublicKeyBase64: String): ExchangeKey {
        return ExchangeKey(
            KeyType.APP_PUBLIC_KEY,
            null,
            encryptedAppPublicKeyBase64, null,
            KeyVersion.VERSION_1
        )
    }

    private fun createCommonKey(encryptedCommonKeyBase64: String): ExchangeKey {
        return ExchangeKey(
            KeyType.COMMON_KEY, null, null,
            encryptedCommonKeyBase64,
            KeyVersion.VERSION_1
        )
    }

    private fun createDataKey(encryptedDataKeyBase64: String): ExchangeKey {
        return ExchangeKey(
            KeyType.DATA_KEY, null, null,
            encryptedDataKeyBase64,
            KeyVersion.VERSION_1
        )
    }

    private fun createAttachmentKey(encryptedAttachmentKeyBase64: String): ExchangeKey {
        return ExchangeKey(
            KeyType.ATTACHMENT_KEY, null, null,
            encryptedAttachmentKeyBase64,
            KeyVersion.VERSION_1
        )
    }

    private fun createTagKey(encryptedTagKeyBase64: String): ExchangeKey {
        return ExchangeKey(
            KeyType.TAG_KEY, null, null,
            encryptedTagKeyBase64,
            KeyVersion.VERSION_1
        )
    }
}
