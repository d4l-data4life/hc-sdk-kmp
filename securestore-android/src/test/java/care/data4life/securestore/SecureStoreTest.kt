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

package care.data4life.securestore

import io.mockk.every
import io.mockk.mockk
import io.mockk.verifyAll
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SecureStoreTest {

    val data = "data".toCharArray()
    val encryptedData = "encryptedData".toCharArray()
    val dataAlias = "data_alias"

    val mockCryptor = mockk<SecureStoreCryptor>()
    val mockStorage = mockk<SecureStoreStorage>(relaxed = true)

    // SUT
    lateinit var secureStore: SecureStore

    @Before
    fun setUp() {
        secureStore = SecureStore(mockCryptor, mockStorage)
    }

    @Test
    fun `addData() should add encrypted data to storage`() {
        // Given
        every { mockCryptor.encrypt(data) } returns encryptedData

        // When
        secureStore.addData(dataAlias, data)

        // Then
        verifyAll {
            mockCryptor.encrypt(data)
            mockStorage.addData(dataAlias, encryptedData)
        }
    }

    @Test
    fun `removeData() should remove data from storage`() {
        // Given

        // When
        secureStore.removeData(dataAlias)

        // Then
        verifyAll {
            mockStorage.removeData(dataAlias)
        }
    }

    @Test
    fun `containsData() should return true`() {
        // Given
        every { mockStorage.containsData(dataAlias) } returns true

        // When
        val result = secureStore.containsData(dataAlias)

        // Then
        verifyAll {
            mockStorage.containsData(dataAlias)
        }

        assertTrue(result)
    }

    @Test
    fun `getData() should return data when data present`() {
        // Given
        every { mockStorage.getData(dataAlias) } returns encryptedData
        every { mockCryptor.decrypt(encryptedData) } returns data

        // When
        val result = secureStore.getData(dataAlias)

        // Then
        verifyAll {
            mockCryptor.decrypt(encryptedData)
            mockStorage.getData(dataAlias)
        }

        assertEquals(data, result)
    }

    @Test
    fun `getData() should return Null when no data present`() {
        // Given
        every { mockStorage.getData(dataAlias) } returns null

        // When
        val result = secureStore.getData(dataAlias)

        // Then
        verifyAll {
            mockStorage.getData(dataAlias)
        }

        assertEquals(null, result)
    }

    @Test
    fun `clear() should call clear on KeyStore and Storage`() {
        every { mockCryptor.clear() } returns Unit

        // When
        secureStore.clear()

        // Then
        verifyAll {
            mockCryptor.clear()
            mockStorage.clear()
        }
    }
}
