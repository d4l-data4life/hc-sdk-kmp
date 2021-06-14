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

import care.data4life.crypto.ExchangeKey
import care.data4life.crypto.KeyType
import care.data4life.sdk.util.Base64
import org.junit.Before
import org.junit.Test

class KeyFactoryTest {

    // SUT
    private lateinit var keyFactory: KeyFactory

    @Before
    fun test() {
        keyFactory = KeyFactory(Base64)
    }

    @Test
    fun `createGCKey()`() {
        // Given
        val exchangeKey = ExchangeKey(
            KeyType.COMMON_KEY,
            null,
            null,
            "LL85V5L0n5024C6XLGe+/mmg9F/VkUbRQycnN28S5L4=",
            1
        )

        // When
        val gcKey = keyFactory.createGCKey(exchangeKey)

        // Then
        // no error happened
    }

    @Test
    fun `createGCKeyPair()`() {
        // Given
        val exchangeKey = ExchangeKey(
            KeyType.APP_PRIVATE_KEY,
            "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",
            null,
            1
        )

        // When
        val gcKeyPair = keyFactory.createGCKeyPair(exchangeKey)

        // Then
        // no error happened
    }
}
