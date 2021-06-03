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

package care.data4life.sdk.network.util.interceptor

import care.data4life.sdk.network.NetworkingInternalContract
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.unmockkConstructor
import io.mockk.verify
import okhttp3.Interceptor
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertSame
import kotlin.test.assertTrue

class LoggingInterceptorTest {
    @Before
    fun setup() {
        mockkConstructor(HttpLoggingInterceptor::class)
    }

    @After
    fun tearDown() {
        unmockkConstructor(HttpLoggingInterceptor::class)
    }

    @Test
    fun `It fulfils InterceptorFactory`() {
        val factory: Any = LoggingInterceptor

        assertTrue(factory is NetworkingInternalContract.InterceptorFactory<*>)
    }

    @Test
    fun `Given getInstance is called with false, it builds an Interceptor while setting NONE debug level`() {
        // Given
        every {
            anyConstructed<HttpLoggingInterceptor>()
                .setLevel(HttpLoggingInterceptor.Level.NONE)
        } returns mockk()

        // When
        val interceptor: Any = LoggingInterceptor.getInstance(false)

        // Then
        assertTrue(interceptor is NetworkingInternalContract.Interceptor)

        verify(exactly = 1) {
            anyConstructed<HttpLoggingInterceptor>()
                .setLevel(HttpLoggingInterceptor.Level.NONE)
        }
    }

    @Test
    fun `Given getInstance is called with true, it builds an Interceptor while setting HEADERS debug level`() {
        // Given
        every {
            anyConstructed<HttpLoggingInterceptor>()
                .setLevel(HttpLoggingInterceptor.Level.HEADERS)
        } returns mockk()

        // When
        val interceptor: Any = LoggingInterceptor.getInstance(true)

        // Then
        assertTrue(interceptor is NetworkingInternalContract.Interceptor)

        verify(exactly = 1) {
            anyConstructed<HttpLoggingInterceptor>()
                .setLevel(HttpLoggingInterceptor.Level.HEADERS)
        }
    }

    @Test
    fun `Given intercept is called on the created instance, the delegates it to the wrapped interceptor and returns its result`() {
        // Given
        val internalInterceptor: HttpLoggingInterceptor = mockk()
        val chain: Interceptor.Chain = mockk()
        val response: Response = mockk()

        every {
            anyConstructed<HttpLoggingInterceptor>()
                .setLevel(any())
        } returns internalInterceptor

        every {
            internalInterceptor.intercept(chain)
        } returns response

        // When
        val result = LoggingInterceptor.getInstance(false).intercept(chain)

        // Then
        assertSame(
            actual = result,
            expected = response
        )

        verify(exactly = 1) { internalInterceptor.intercept(chain) }
    }
}
