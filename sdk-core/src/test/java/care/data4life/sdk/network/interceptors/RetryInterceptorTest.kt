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

package care.data4life.sdk.network.interceptors

import care.data4life.sdk.lang.CoreRuntimeException
import care.data4life.sdk.network.NetworkingContract
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import org.junit.Test
import java.net.SocketTimeoutException
import kotlin.test.assertFailsWith
import kotlin.test.assertSame
import kotlin.test.assertTrue

class RetryInterceptorTest {
    @Test
    fun `It fulfils InterceptorFactory`() {
        val factory: Any = RetryInterceptor

        assertTrue(factory is NetworkingContract.InterceptorFactory<*>)
    }

    @Test
    fun `Given getInstance is called with a NetworkConnectivityService, it creates an Interceptor`() {
        // Given
        val service: NetworkingContract.NetworkConnectivityService = mockk()

        // When
        val interceptor: Any = RetryInterceptor.getInstance(service)

        // Then
        assertTrue(interceptor is NetworkingContract.Interceptor)
    }

    @Test
    fun `Given a RetryInterceptor was created and intercept is called, it returns the Response of the Request`() {
        // Given
        val service: NetworkingContract.NetworkConnectivityService = mockk()
        val chain: Interceptor.Chain = mockk()
        val response: Response = mockk()
        val request: Request = mockk()

        every { chain.request() } returns request
        every { chain.proceed(request) } returns response

        // When
        val result = RetryInterceptor.getInstance(service).intercept(chain)

        // Then
        assertSame(
            actual = result,
            expected = response
        )

        verify(exactly = 1) { chain.proceed(request) }
    }

    @Test
    fun `Given a RetryInterceptor was created and intercept is called, the Request fails, but the device is connected, it tries again and returns the Response`() {
        // Given
        val service: NetworkingContract.NetworkConnectivityService = mockk()
        val chain: Interceptor.Chain = mockk()
        val response: Response = mockk()
        val request: Request = mockk()

        var isFail = false

        every { service.isConnected } returns true
        every { chain.request() } returns request
        every { chain.proceed(request) } answers {
            if (isFail) {
                response
            } else {
                throw SocketTimeoutException().also {
                    isFail = true
                }
            }
        }

        // When
        val result = RetryInterceptor.getInstance(service).intercept(chain)

        // Then
        assertSame(
            actual = result,
            expected = response
        )

        verify(exactly = 2) { chain.proceed(request) }
    }

    @Test
    fun `Given a RetryInterceptor was created and intercept is called, the Request fails, but the device is not connected, it fails`() {
        // Given
        val service: NetworkingContract.NetworkConnectivityService = mockk()
        val chain: Interceptor.Chain = mockk()
        val response: Response = mockk()
        val request: Request = mockk()

        var isFail = false

        every { service.isConnected } returns false
        every { chain.request() } returns request
        every { chain.proceed(request) } answers {
            if (isFail) {
                response
            } else {
                throw SocketTimeoutException().also {
                    isFail = true
                }
            }
        }

        // Then
        assertFailsWith<CoreRuntimeException.InternalFailure> {
            // When
            RetryInterceptor.getInstance(service).intercept(chain)
        }
    }
}
