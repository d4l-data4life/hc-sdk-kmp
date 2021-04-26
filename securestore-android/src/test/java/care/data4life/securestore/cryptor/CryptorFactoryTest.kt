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

package care.data4life.securestore.cryptor

import android.content.Context
import android.os.Build
import care.data4life.securestore.test.setFinalStaticField
import com.google.crypto.tink.aead.AeadFactory
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import org.junit.After
import org.junit.Test
import java.security.KeyStore

class CryptorFactoryTest {

    private val mockContext = mockk<Context>(relaxed = true)

    private val originalSdkVersion = Build.VERSION.SDK_INT

    @Test
    fun `create() should return AndroidCompatCryptor when SDK version less than 23`() {
        // Given
        mockkStatic(KeyStore::class)
        every { KeyStore.getInstance(any()) } returns mockk(relaxed = true)
        setFinalStaticField(Build.VERSION::class.java.getField("SDK_INT"), 21)

        // When
        val result = CryptorFactory.create(mockContext)

        // Then
        kotlin.test.assertEquals(AndroidCompatCryptor::class.java, result::class.java)
    }

    @Test
    fun `create() should return TinkAndroidCryptor when SDK version min 23`() {
        // Given
        mockkObject(TinkAndroidCryptor)
        every { TinkAndroidCryptor.initKeysetManager(mockContext) } returns mockk(relaxed = true)
        mockkStatic(AeadFactory::class)
        every { AeadFactory.getPrimitive(any()) } returns mockk()
        setFinalStaticField(Build.VERSION::class.java.getField("SDK_INT"), 23)

        // When
        val result = CryptorFactory.create(mockContext)

        // Then
        kotlin.test.assertEquals(TinkAndroidCryptor::class.java, result::class.java)
    }

    @After
    fun cleanUp() {
        setFinalStaticField(Build.VERSION::class.java.getField("SDK_INT"), originalSdkVersion)
    }
}
