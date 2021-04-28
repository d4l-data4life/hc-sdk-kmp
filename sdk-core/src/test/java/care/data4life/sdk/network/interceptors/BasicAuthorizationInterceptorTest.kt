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

import care.data4life.sdk.network.NetworkingContract
import care.data4life.sdk.network.NetworkingContract.Companion.BASIC_AUTH_MARKER
import care.data4life.sdk.network.NetworkingContract.Companion.HEADER_AUTHORIZATION
import care.data4life.sdk.util.Base64
import io.mockk.every
import io.mockk.mockk
import io.mockk.verifyOrder
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import org.junit.Test
import kotlin.test.assertSame
import kotlin.test.assertTrue

class BasicAuthorizationInterceptorTest {
    @Test
    fun `It fulfils InterceptorFactory`() {
        val factory: Any = VersionInterceptor

        assertTrue(factory is NetworkingContract.InterceptorFactory<*>)
    }

    @Test
    fun `Given getInstance is called with cretentials it returns a Interceptor`() {
        // Given
        val credentials = Pair("a", "b")

        // When
        val interceptor: Any = BasicAuthorizationInterceptor.getInstance(credentials)

        // Then
        assertTrue(interceptor is NetworkingContract.Interceptor)
    }

    @Test
    fun `Given intercept is called, it simply forwards the given chain, if no HEADER_AUTHORIZATION is present, proceeds with the Request and returns the Response`() {
        // Given
        val response: Response = mockk()

        val chain: Interceptor.Chain = mockk()
        val request: Request = mockk()

        every { request.header(HEADER_AUTHORIZATION) } returns null
        every { chain.request() } returns request
        every { chain.proceed(request) } returns response

        // When
        val actual = BasicAuthorizationInterceptor.getInstance(Pair("a", "b")).intercept(chain)

        // Then
        assertSame(
            actual = actual,
            expected = response
        )

        verifyOrder {
            chain.request()
            request.header(HEADER_AUTHORIZATION)
            chain.proceed(request)
        }
    }

    @Test
    fun `Given intercept is called, it simply forwards the given chain, AUTHORIZATION_WITH_BASIC_AUTH is present, proceeds with the Request and returns the Response`() {
        // Given
        val response: Response = mockk()

        val chain: Interceptor.Chain = mockk()
        val request: Request = mockk()

        every { request.header(HEADER_AUTHORIZATION) } returns "you should not care"
        every { chain.request() } returns request
        every { chain.proceed(request) } returns response

        // When
        val actual = BasicAuthorizationInterceptor.getInstance(Pair("a", "b")).intercept(chain)

        // Then
        assertSame(
            actual = actual,
            expected = response
        )

        verifyOrder {
            chain.request()
            request.header(HEADER_AUTHORIZATION)
            chain.proceed(request)
        }
    }

    @Test
    fun `Given intercept is called, it replaces HEADER_AUTHORIZATION, if AUTHORIZATION_WITH_BASIC_AUTH is present, proceeds with the Request and returns the Response`() {
        // Given
        val clientId = "me"
        val secret = "secret"
        val credential = Base64.encodeToString("$clientId:$secret")

        val response: Response = mockk()

        val chain: Interceptor.Chain = mockk()
        val request: Request = mockk()
        val modifiedRequest: Request = mockk()
        val builder: Request.Builder = mockk()

        every { chain.request() } returns request
        every { request.header(HEADER_AUTHORIZATION) } returns BASIC_AUTH_MARKER
        every { request.newBuilder() } returns builder
        every {
            builder.removeHeader(HEADER_AUTHORIZATION)
        } returns builder
        every {
            builder.addHeader(HEADER_AUTHORIZATION, "Basic $credential")
        } returns builder
        every {
            builder.build()
        } returns modifiedRequest

        every { chain.proceed(modifiedRequest) } returns response

        // When
        val actual = BasicAuthorizationInterceptor.getInstance(Pair(clientId, secret)).intercept(chain)

        // Then
        assertSame(
            actual = actual,
            expected = response
        )

        verifyOrder {
            chain.request()
            request.header(HEADER_AUTHORIZATION)
            request.newBuilder()
            builder.removeHeader(HEADER_AUTHORIZATION)
            builder.addHeader(HEADER_AUTHORIZATION, "Basic $credential")
            builder.build()
            chain.proceed(modifiedRequest)
        }
    }
}
