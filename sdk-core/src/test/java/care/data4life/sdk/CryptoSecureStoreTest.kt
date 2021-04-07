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
package care.data4life.sdk

import care.data4life.crypto.ExchangeKey
import care.data4life.crypto.GCKey
import care.data4life.crypto.GCKeyPair
import care.data4life.crypto.GCRSAKeyAlgorithm
import care.data4life.crypto.KeyType
import care.data4life.securestore.SecureStoreContract
import com.google.common.base.CharMatcher.any
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import junit.framework.Assert.assertEquals
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatcher
import org.mockito.Mockito
import java.io.IOException
import java.lang.reflect.Type

class CryptoSecureStoreTest {
    private lateinit var secureStore: SecureStoreContract.SecureStore
    private lateinit var moshi: Moshi
    private lateinit var adapter: JsonAdapter<ExchangeKey>

    // SUT
    private lateinit var cryptoSecureStore: CryptoSecureStore

    @Before
    fun setUp() {
        secureStore = mockk()
        moshi = mockk()
        adapter = mockk()
        every { moshi.adapter(ExchangeKey::class.java) } returns adapter
        cryptoSecureStore = Mockito.spy(CryptoSecureStore(moshi, secureStore))
    }

    @Test
    fun storeSecret_shouldStoreString() {
        // given
        every { secureStore.addData(DATA_ALIAS, DATA.toCharArray()) } just runs

        // When
        cryptoSecureStore.storeSecret(DATA_ALIAS, DATA.toCharArray())

        // Then
        verify(exactly = 1) { secureStore.addData(DATA_ALIAS, DATA.toCharArray()) }
    }

    @Test
    fun storeSecret_shouldStoreObject() {
        // Given
        val result = Object()
        val adapterObj : JsonAdapter<Object> = mockk()
        every { moshi.adapter(Object::class.java) } returns adapterObj
        every { adapterObj.toJson(any<Object>()) } returns OBJECT_JSON

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
        every { secureStore.getData(OBJECT_ALIAS) } returns OBJECT_JSON.toCharArray()
        every { adapter.fromJson(OBJECT_JSON) } returns OBJECT

        // When
        val result = cryptoSecureStore.getSecret(OBJECT_ALIAS, ExchangeKey::class.java)

        // Then
        assertEquals(OBJECT, result)
    }

    @Test
    fun clearStorage_shoudBeCalledOnce() {
        // given
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
        // given
        val key = mockk<GCKey>()
        every { adapter.toJson(any<ExchangeKey>()) } returns DATA
        every { secureStore.addData(DATA_ALIAS, DATA.toCharArray()) } just runs
        every { key.getKeyBase64() } returns DATA.toCharArray()
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
        // given
        val key = mockk<GCKeyPair>()
        val algorithm = GCRSAKeyAlgorithm()
        every { key.algorithm } returns algorithm
        every { key.getPrivateKeyBase64() } returns DATA.toCharArray()
        every { key.getPublicKeyBase64() } returns DATA.toCharArray()
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
        private val OBJECT = ExchangeKey(KeyType.COMMON_KEY, charArrayOf(), charArrayOf(), "keyBase64".toCharArray(), 1)
        private const val OBJECT_JSON = "object_json"
    }
}
