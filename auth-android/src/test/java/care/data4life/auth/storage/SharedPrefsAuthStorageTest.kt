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

package care.data4life.auth.storage

import care.data4life.securestore.SecureStoreContract
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals

class SharedPrefsAuthStorageTest {

    lateinit var mockSecureStore: SecureStoreContract.SecureStore

    lateinit var sut: SharedPrefsAuthStorage

    @Before
    fun setUp() {
        mockSecureStore = mockk()
        sut = SharedPrefsAuthStorage(mockSecureStore)
    }

    @Test
    fun readAuthState() {
        // given
        val data = "authState"
        every { mockSecureStore.getData(any()) } returns data.toCharArray()

        // when
        val authState = sut.readAuthState(ALIAS)

        // then
        verify { mockSecureStore.getData(KEY) }
        assertEquals(data, authState)
    }

    @Test
    fun writeAuthState() {
        // given
        every { mockSecureStore.addData(any(), any()) } returns Unit
        val authState = "authState"

        // when
        sut.writeAuthState(ALIAS, authState)

        // then
        verify { mockSecureStore.addData(KEY, authState.toCharArray()) }
    }

    @Test
    fun containsAuthState() {
        // given
        val expected = true
        every { mockSecureStore.containsData(KEY) } returns expected

        // when
        val actual = sut.containsAuthState(ALIAS)

        // then
        assertEquals(expected, actual)
    }

    @Test
    fun removeAuthState() {
        // given
        every { mockSecureStore.removeData(KEY) } returns Unit

        // when
        sut.removeAuthState(ALIAS)

        // then
        verify { mockSecureStore.removeData(KEY) }
    }

    @Test
    fun clear() {
        // given
        every { mockSecureStore.clear() } returns Unit

        // when
        sut.clear()

        // then
        verify { mockSecureStore.clear() }
    }

    companion object {
        const val ALIAS = "testAlias"
        const val KEY = "store.auth.state.$ALIAS"
    }
}
