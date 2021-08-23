/*
 * Copyright (c) 2021 D4L data4life gGmbH / All rights reserved.
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

import care.data4life.sdk.securestore.SecureStoreContract
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import io.mockk.clearAllMocks
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.spyk
import io.mockk.verify
import org.junit.Before
import org.junit.Test
import java.io.IOException
import kotlin.test.assertEquals

class CryptoSecureStoreTest {
    private var secureStore: SecureStoreContract.SecureStore = mockk()
    private var moshi: Moshi = mockk()
    private var adapter: JsonAdapter<ExchangeKey> = mockk()

    // SUT
    private var cryptoSecureStore: CryptoSecureStore = spyk(CryptoSecureStore(moshi, secureStore))

    @Before
    fun setUp() {
        clearAllMocks()
        every { moshi.adapter(ExchangeKey::class.java) } returns adapter
    }

    @Test
    fun storeSecret_shouldStoreString() {
        // Given
        every { secureStore.addData(DATA_ALIAS, DATA.toCharArray()) } just runs

        // When
        cryptoSecureStore.storeSecret(DATA_ALIAS, DATA.toCharArray())

        // Then
        verify(exactly = 1) { secureStore.addData(DATA_ALIAS, DATA.toCharArray()) }
    }

    @Test
    fun storeSecret_shouldStoreObject() {
        // Given
        val result = Any()
        val adapterObj: JsonAdapter<Any> = mockk()
        every { moshi.adapter<Any>(java.lang.Object::class.java) } returns adapterObj
        every { adapterObj.toJson(any()) } returns OBJECT_JSON
        every { secureStore.addData(OBJECT_ALIAS, OBJECT_JSON.toCharArray()) } just runs

        // When
        cryptoSecureStore.storeSecret(OBJECT_ALIAS, result)

        // Then
        verify(exactly = 1) { secureStore.addData(OBJECT_ALIAS, OBJECT_JSON.toCharArray()) }
    }

    @Throws(Exception::class)
    @Test
    fun secret_shouldReturnString() {
        // Given
        every { secureStore.getData(DATA_ALIAS) } returns DATA.toCharArray()

        // When
        val result = String(cryptoSecureStore.getSecret(DATA_ALIAS))

        // Then
        assertEquals(DATA, result)
    }

    @Throws(Exception::class)
    @Test
    fun secret_shouldReturnObject() {
        // Given
        val exchangeKey = mockk<ExchangeKey>()
        every { secureStore.getData(OBJECT_ALIAS) } returns OBJECT_JSON.toCharArray()
        every { adapter.fromJson(OBJECT_JSON) } returns exchangeKey

        // When
        val result = cryptoSecureStore.getSecret(OBJECT_ALIAS, ExchangeKey::class.java)

        // Then
        assertEquals(exchangeKey, result)
    }

    @Test
    fun clearStorage_shouldBeCalledOnce() {
        // Given
        every { secureStore.clear() } just runs
        // When
        cryptoSecureStore.clear()

        // Then
        verify(exactly = 1) { secureStore.clear() }
    }

    @Test
    fun deleteSecret_shouldSucceed() {
        // given
        every { secureStore.removeData(DATA_ALIAS) } just runs
        // When
        cryptoSecureStore.deleteSecret(DATA_ALIAS)

        // Then
        verify(exactly = 1) { secureStore.removeData(DATA_ALIAS) }
    }

    @Test
    fun storeGCKey() {
        // Given
        val key = mockk<GCKey>()
        every { adapter.toJson(any<ExchangeKey>()) } returns DATA
        every { secureStore.addData(DATA_ALIAS, DATA.toCharArray()) } just runs
        every { key.getKeyBase64() } returns DATA
        every { key.keyVersion } returns 1

        // When
        cryptoSecureStore.storeKey(DATA_ALIAS, key, KeyType.DATA_KEY)

        // Then
        verify { adapter.toJson(any<ExchangeKey>()) }
        verify { key.getKeyBase64() }
        verify { key.keyVersion }
        confirmVerified(key)
        verify { secureStore.addData(DATA_ALIAS, DATA.toCharArray()) }
    }

    @Test
    fun storeGCKeyPair() {
        // Given
        val key = mockk<GCKeyPair>()
        val algorithm = mockk<GCRSAKeyAlgorithm>()
        every { key.algorithm } returns algorithm
        every { key.getPrivateKeyBase64() } returns DATA
        every { key.getPublicKeyBase64() } returns DATA
        every { key.keyVersion } returns 1
        every { adapter.toJson(any<ExchangeKey>()) } returns DATA
        every { secureStore.addData(DATA_ALIAS, DATA.toCharArray()) } just runs

        // When
        cryptoSecureStore.storeKey(DATA_ALIAS, key)

        // Then
        verify { adapter.toJson(any<ExchangeKey>()) }
        verify { key.getPublicKeyBase64() }
        verify { key.getPrivateKeyBase64() }
        verify { key.keyVersion }
        confirmVerified(key)
        verify { secureStore.addData(DATA_ALIAS, DATA.toCharArray()) }
    }

    @Throws(IOException::class)
    @Test
    fun exchangeKey() {
        // Given
        every { secureStore.getData(DATA_ALIAS) } returns DATA.toCharArray()
        val mockedExchangeKey = mockk<ExchangeKey>()
        every { adapter.fromJson(DATA) } returns mockedExchangeKey

        // When
        val exchangeKey = cryptoSecureStore.getExchangeKey(DATA_ALIAS)

        // Then
        assertEquals(mockedExchangeKey, exchangeKey)
    }

    companion object {
        private const val DATA_ALIAS = "data_alias"
        private const val DATA = "data"
        private const val OBJECT_ALIAS = "object_alias"
        private const val OBJECT_JSON = "object_json"
    }
}
