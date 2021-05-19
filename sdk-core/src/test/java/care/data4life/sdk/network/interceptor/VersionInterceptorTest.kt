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

package care.data4life.sdk.network.interceptor

import care.data4life.sdk.network.NetworkingContract
import care.data4life.sdk.network.NetworkingContract.Companion.HEADER_SDK_VERSION
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import org.junit.Test
import kotlin.test.assertSame
import kotlin.test.assertTrue

class VersionInterceptorTest {
    @Test
    fun `It fulfils InterceptorFactory`() {
        val factory: Any = VersionInterceptor

        assertTrue(factory is NetworkingContract.InterceptorFactory<*>)
    }

    @Test
    fun `Given getInstance is called with a clientName it returns a Interceptor`() {
        // Given
        val clientName = "name"

        // When
        val interceptor: Any = VersionInterceptor.getInstance(
            Pair(
                NetworkingContract.Clients.ANDROID,
                clientName
            )
        )

        // Then
        assertTrue(interceptor is NetworkingContract.Interceptor)
    }

    @Test
    fun `Given intercept is called on the created instance, it sets the current SDK Version header for Android and returns the Response`() {
        // Given
        val clientName = "test"
        val version = "android-test"

        val response: Response = mockk()

        val chain: Interceptor.Chain = mockk()
        val request: Request = mockk()
        val modifiedRequest: Request = mockk()
        val builder: Request.Builder = mockk()

        every { chain.request() } returns request
        every { request.newBuilder() } returns builder
        every {
            builder.addHeader(
                HEADER_SDK_VERSION,
                version
            )
        } returns builder
        every { builder.build() } returns modifiedRequest
        every { chain.proceed(modifiedRequest) } returns response

        // When
        val interceptor = VersionInterceptor.getInstance(
            Pair(
                NetworkingContract.Clients.ANDROID,
                clientName
            )
        )

        // Then
        val result = interceptor.intercept(chain)

        assertSame(
            actual = result,
            expected = response
        )

        verify(exactly = 1) {
            builder.addHeader(
                HEADER_SDK_VERSION,
                version
            )
        }

        verify(exactly = 1) { chain.proceed(modifiedRequest) }
    }

    @Test
    fun `Given intercept is called on the created instance, it sets the current SDK Version header for Java and returns the Response`() {
        // Given
        val clientName = "test"
        val version = "jvm-test"

        val response: Response = mockk()

        val chain: Interceptor.Chain = mockk()
        val request: Request = mockk()
        val modifiedRequest: Request = mockk()
        val builder: Request.Builder = mockk()

        every { chain.request() } returns request
        every { request.newBuilder() } returns builder
        every {
            builder.addHeader(
                HEADER_SDK_VERSION,
                version
            )
        } returns builder
        every { builder.build() } returns modifiedRequest
        every { chain.proceed(modifiedRequest) } returns response

        // When
        val interceptor = VersionInterceptor.getInstance(
            Pair(
                NetworkingContract.Clients.JAVA,
                clientName
            )
        )

        // Then
        val result = interceptor.intercept(chain)

        assertSame(
            actual = result,
            expected = response
        )

        verify(exactly = 1) {
            builder.addHeader(
                HEADER_SDK_VERSION,
                version
            )
        }
        verify(exactly = 1) { chain.proceed(modifiedRequest) }
    }

    @Test
    fun `Given intercept is called on the created instance, it sets the current SDK Version header for Ingestion and returns the Response`() {
        // Given
        val clientName = "test"
        val version = "ingestion-test"

        val response: Response = mockk()

        val chain: Interceptor.Chain = mockk()
        val request: Request = mockk()
        val modifiedRequest: Request = mockk()
        val builder: Request.Builder = mockk()

        every { chain.request() } returns request
        every { request.newBuilder() } returns builder
        every {
            builder.addHeader(
                HEADER_SDK_VERSION,
                version
            )
        } returns builder
        every { builder.build() } returns modifiedRequest
        every { chain.proceed(modifiedRequest) } returns response

        // When
        val interceptor = VersionInterceptor.getInstance(
            Pair(
                NetworkingContract.Clients.INGESTION,
                clientName
            )
        )

        // Then
        val result = interceptor.intercept(chain)

        assertSame(
            actual = result,
            expected = response
        )

        verify(exactly = 1) {
            builder.addHeader(
                HEADER_SDK_VERSION,
                version
            )
        }
        verify(exactly = 1) { chain.proceed(modifiedRequest) }
    }
}
