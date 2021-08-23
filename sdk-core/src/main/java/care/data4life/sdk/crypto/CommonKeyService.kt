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

package care.data4life.sdk.crypto

import care.data4life.sdk.crypto.CryptoInternalContract.CommonKeyService.Companion.DEFAULT_COMMON_KEY_ID
import care.data4life.sdk.lang.D4LException
import java.io.IOException

internal class CommonKeyService(
    private val alias: String,
    private val storage: CryptoContract.SecureStore,
    private val keyFactory: KeyFactory
) : CryptoInternalContract.CommonKeyService {

    override fun fetchCurrentCommonKeyId(): String {
        var commonKeyId: String? = null
        try {
            commonKeyId = String(storage.getSecret(aliasCurrentCommonKeyId))
        } catch (e: D4LException) {
            // ignore
        }

        return commonKeyId ?: DEFAULT_COMMON_KEY_ID
    }

    @Throws(IOException::class)
    override fun fetchCurrentCommonKey(): GCKey {
        var commonKeyId = DEFAULT_COMMON_KEY_ID
        try {
            commonKeyId = String(storage.getSecret(aliasCurrentCommonKeyId))
        } catch (e: D4LException) {
            // ignore
        }

        return fetchCommonKey(commonKeyId)
    }

    override fun fetchCommonKey(commonKeyId: String): GCKey {
        val exchangeCommonKey = storage.getExchangeKey(aliasCommonKey(commonKeyId))

        return keyFactory.createGCKey(exchangeCommonKey)
    }

    override fun storeCurrentCommonKeyId(commonKeyId: String) {
        storage.storeSecret(aliasCurrentCommonKeyId, commonKeyId.toCharArray())
    }

    override fun storeCommonKey(commonKeyId: String, commonKey: GCKey) {
        storage.storeKey(aliasCommonKey(commonKeyId), commonKey, KeyType.COMMON_KEY)
    }

    override fun hasCommonKey(commonKeyId: String): Boolean {
        return storage.contains(aliasCommonKey(commonKeyId))
    }

    private fun aliasCommonKey(commonKeyId: String) = "${alias}_crypto_common_key_$commonKeyId"

    private val aliasCurrentCommonKeyId = "${alias}_crypto_current_common_key_id"
}
