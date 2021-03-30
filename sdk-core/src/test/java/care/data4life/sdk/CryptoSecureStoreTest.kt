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
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers
import org.mockito.Mockito
import java.io.IOException
import java.lang.reflect.Type

class CryptoSecureStoreTest {
    private var secureStore: SecureStoreContract.SecureStore? = null
    private var moshi: Moshi? = null
    private var adapter: JsonAdapter<*>? = null

    // SUT
    private var cryptoSecureStore: CryptoSecureStore? = null
    @Before
    fun setUp() {
        secureStore = Mockito.mock(SecureStoreContract.SecureStore::class.java)
        moshi = Mockito.mock(Moshi::class.java)
        adapter = Mockito.mock(JsonAdapter::class.java)
        Mockito.`when`(moshi.adapter(ArgumentMatchers.any<Class<Any>>())).thenReturn(adapter)
        cryptoSecureStore = Mockito.spy(CryptoSecureStore(moshi, secureStore))
    }

    @Test
    fun storeSecret_shouldStoreString() {
        // When
        cryptoSecureStore!!.storeSecret(DATA_ALIAS, DATA.toCharArray())

        // Then
        Mockito.verify(secureStore, Mockito.times(1))!!.addData(DATA_ALIAS, DATA.toCharArray())
    }

    @Test
    fun storeSecret_shouldStoreObject() {
        // Given
        val result = Any()
        Mockito.`when`(moshi!!.adapter<Any>(ArgumentMatchers.any(Type::class.java))).thenReturn(adapter)
        Mockito.`when`(adapter!!.toJson(ArgumentMatchers.any<Any>())).thenReturn(OBJECT_JSON)

        // When
        cryptoSecureStore!!.storeSecret(OBJECT_ALIAS, result)

        // Then
        Mockito.verify(secureStore, Mockito.times(1))!!.addData(OBJECT_ALIAS, OBJECT_JSON.toCharArray())
    }
    // Given

    // When
    @get:Throws(Exception::class)
    @get:Test
    val secret_shouldReturnString: Unit
        // Then
        get() {
            // Given
            Mockito.`when`(secureStore!!.getData(DATA_ALIAS)).thenReturn(DATA.toCharArray())

            // When
            val result = String(cryptoSecureStore!!.getSecret(DATA_ALIAS))

            // Then
            Assert.assertEquals(DATA, result)
        }
    // Given

    // When
    @get:Throws(Exception::class)
    @get:Test
    val secret_shouldReturnObject: Unit
        // Then
        get() {
            // Given
            Mockito.`when`(secureStore!!.getData(OBJECT_ALIAS)).thenReturn(OBJECT_JSON.toCharArray())
            Mockito.`when`(adapter!!.fromJson(OBJECT_JSON)).thenReturn(OBJECT)

            // When
            val result = cryptoSecureStore!!.getSecret(OBJECT_ALIAS, Any::class.java)

            // Then
            Assert.assertEquals(OBJECT, result)
        }

    @Test
    fun clearStorage_shoudBeCalledOnce() {
        // When
        cryptoSecureStore!!.clear()

        // Then
        Mockito.verify(secureStore, Mockito.times(1))!!.clear()
    }

    @Test
    fun deleteSecret_shouldSucceed() {
        // When
        cryptoSecureStore!!.deleteSecret(DATA_ALIAS)

        // Then
        Mockito.verify(secureStore, Mockito.times(1))!!.removeData(DATA_ALIAS)
    }

    @Test
    fun storeGCKey() {
        // given
        val key = Mockito.mock(GCKey::class.java)
        Mockito.`when`(moshi!!.adapter<Any>(ArgumentMatchers.any(Type::class.java))).thenReturn(adapter)
        Mockito.`when`(adapter!!.toJson(ArgumentMatchers.any<Any>())).thenReturn(DATA)

        // When
        cryptoSecureStore!!.storeKey(DATA_ALIAS, key, KeyType.DATA_KEY)

        // Then
        Mockito.verify(adapter)!!.toJson(ArgumentMatchers.any<ExchangeKey>(ExchangeKey::class.java))
        Mockito.verify(key).getKeyBase64()
        Mockito.verify(key).keyVersion
        Mockito.verifyNoMoreInteractions(key)
        Mockito.verify(secureStore)!!.addData(ArgumentMatchers.eq(DATA_ALIAS), ArgumentMatchers.eq(DATA.toCharArray()))
    }

    @Test
    fun storeGCKeyPair() {
        // given
        val key = Mockito.mock(GCKeyPair::class.java)
        val algorithm = GCRSAKeyAlgorithm()
        Mockito.`when`(key.algorithm).thenReturn(algorithm)
        Mockito.`when`(key.getPublicKeyBase64()).thenReturn(DATA.toCharArray())
        Mockito.`when`(key.getPrivateKeyBase64()).thenReturn(DATA.toCharArray())
        Mockito.`when`(key.keyVersion).thenReturn(1)
        Mockito.`when`(moshi!!.adapter<Any>(ArgumentMatchers.any(Type::class.java))).thenReturn(adapter)
        Mockito.`when`(adapter!!.toJson(ArgumentMatchers.any<Any>())).thenReturn(DATA)

        // When
        cryptoSecureStore!!.storeKey(DATA_ALIAS, key)

        // Then
        Mockito.verify(adapter)!!.toJson(ArgumentMatchers.any<ExchangeKey>(ExchangeKey::class.java))
        Mockito.verify(key).getPrivateKeyBase64()
        Mockito.verify(key).getPublicKeyBase64()
        Mockito.verify(key).keyVersion
        Mockito.verifyNoMoreInteractions(key)
        Mockito.verify(secureStore)!!.addData(ArgumentMatchers.eq(DATA_ALIAS), ArgumentMatchers.eq(DATA.toCharArray()))
    }
    // Given

    // When
    @get:Throws(IOException::class)
    @get:Test
    val exchangeKey:

    // Then
            Unit
        get() {
            // Given
            Mockito.`when`(secureStore!!.getData(DATA_ALIAS)).thenReturn(DATA.toCharArray())
            Mockito.`when`(moshi!!.adapter<Any>(ArgumentMatchers.any(Type::class.java))).thenReturn(adapter)
            val mockedExchangeKey = Mockito.mock(ExchangeKey::class.java)
            Mockito.`when`(adapter!!.fromJson(DATA)).thenReturn(mockedExchangeKey)

            // When
            val exchangeKey = cryptoSecureStore!!.getExchangeKey(DATA_ALIAS)

            // Then
            Assert.assertEquals(mockedExchangeKey, exchangeKey)
        }

    companion object {
        private const val DATA_ALIAS = "data_alias"
        private const val DATA = "data"
        private const val OBJECT_ALIAS = "object_alias"
        private val OBJECT = Any()
        private const val OBJECT_JSON = "object_json"
    }
}
