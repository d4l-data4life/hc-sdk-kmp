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

import care.data4life.sdk.CryptoSecureStore
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verifyAll
import org.junit.Before
import org.junit.Ignore
import org.junit.Test

@Ignore
class CommonKeyServiceTest {

    private val DEFAULT_COMMON_KEY_ID = "00000000-0000-0000-0000-000000000000"
    private val ROTATED_COMMON_KEY_ID = "11111111-1111-1111-1111-111111111111"


    private val ALIAS = "someAlias"

    private val ALIAS_KOMMON_KEY = ""
    private val ALIAS_COMMON_KEY_ID = DEFAULT_COMMON_KEY_ID
    private val ALIAS_ROTATED_COMMON_KEY_ID = ALIAS + ROTATED_COMMON_KEY_ID


    private val ALIAS_CURRENT_COMMON_KEY_ID = "${ALIAS}_crypto_current_common_key_id"

    private val ALIAS_COMMON_KEY_DEFAULT = "${ALIAS}_crypto_common_key_${DEFAULT_COMMON_KEY_ID}"
    private val ALIAS_COMMON_KEY_ROTATED = "${ALIAS}_crypto_common_key_${ROTATED_COMMON_KEY_ID}"


    private val mockStorage = mockk<CryptoSecureStore>(relaxed = true)
    private val mockKeyFactory = mockk<KeyFactory>()


    // SUT
    private lateinit var commonKeyService: CommonKeyService


    @Before
    fun setup() {
        commonKeyService = spyk(CommonKeyService(ALIAS, mockStorage, mockKeyFactory))
    }


    @Test
    fun `fetchCurrentCommonKeyId() SHOULD return current common key`() {
        // Given
        every { mockStorage.getSecret(ALIAS_CURRENT_COMMON_KEY_ID) } returns DEFAULT_COMMON_KEY_ID.toCharArray()

        // When
        commonKeyService.fetchCurrentCommonKey()

        // Then
        verifyAll {
            mockStorage.getSecret(ALIAS_CURRENT_COMMON_KEY_ID)
        }
    }

    @Test
    fun `fetchCurrentCommonKeyId() SHOULD return default key WHEN current common key not present`() {

    }

    @Test
    fun `fetchCurrentCommonKeyId() SHOULD return current common key WHEN rotated`() {
        //TODO
        // Given
        every { mockStorage.getSecret(ALIAS_CURRENT_COMMON_KEY_ID) } returns ROTATED_COMMON_KEY_ID.toCharArray()

        // When
        commonKeyService.fetchCurrentCommonKey()

        // Then
        verifyAll {
            mockStorage.getSecret(ALIAS_CURRENT_COMMON_KEY_ID)
        }
    }


    @Test
    fun `fetchCurrentCommonKey() SHOULD return current common key`() {
        // Given
        every { mockStorage.getSecret(ALIAS_CURRENT_COMMON_KEY_ID) } returns DEFAULT_COMMON_KEY_ID.toCharArray()

        // When
        commonKeyService.fetchCurrentCommonKey()

        // Then
        verifyAll {
            mockStorage.getSecret(ALIAS_CURRENT_COMMON_KEY_ID)
        }
    }

    @Test
    fun `fetchCurrentCommonKey() SHOULD return default key WHEN current common key not present`() {

    }

    @Test
    fun `fetchCurrentCommonKey() SHOULD return current common key WHEN rotated`() {
        //TODO
        // Given
        every { mockStorage.getSecret(ALIAS_CURRENT_COMMON_KEY_ID) } returns ROTATED_COMMON_KEY_ID.toCharArray()

        // When
        commonKeyService.fetchCurrentCommonKey()

        // Then
        verifyAll {
            mockStorage.getSecret(ALIAS_CURRENT_COMMON_KEY_ID)
        }
    }

}
