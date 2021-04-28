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

import care.data4life.auth.AuthorizationContract
import care.data4life.sdk.lang.D4LException
import care.data4life.sdk.network.NetworkingContract
import care.data4life.sdk.network.NetworkingContract.Companion.ACCESS_TOKEN_MARKER
import care.data4life.sdk.network.NetworkingContract.Companion.HEADER_ALIAS
import care.data4life.sdk.network.NetworkingContract.Companion.HEADER_AUTHORIZATION
import care.data4life.sdk.test.util.GenericTestDataProvider.ALIAS
import io.mockk.every
import io.mockk.mockk
import io.mockk.verifyOrder
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import org.junit.Test
import kotlin.test.assertSame
import kotlin.test.assertTrue

class OAuthFetchTokenAuthorizationInterceptorTest {
    @Test
    fun `It fulfils InterceptorFactory`() {
        val factory: Any = OAuthFetchTokenAuthorizationInterceptor

        assertTrue(factory is NetworkingContract.InterceptorFactory<*>)
    }

    @Test
    fun `Given getInstance is called with a AuthorizationContractService it returns a Interceptor`() {
        // Given
        val service: AuthorizationContract.Service = mockk()

        // When
        val interceptor: Any = OAuthFetchTokenAuthorizationInterceptor.getInstance(service)

        // Then
        assertTrue(interceptor is NetworkingContract.Interceptor)
    }

    @Test
    fun `Given a interceptor was created and intercept was called, it simply proceeds with the cain, if no AUTHORIZATION_WITH_ACCESS_TOKEN is present`() {
        // Given
        val service: AuthorizationContract.Service = mockk()

        val response: Response = mockk()
        val chain: Interceptor.Chain = mockk()
        val request: Request = mockk()

        every { request.header(HEADER_ALIAS) } returns "woop"
        every { request.header(HEADER_AUTHORIZATION) } returns "you should not care"
        every { chain.request() } returns request
        every { chain.proceed(request) } returns response

        // When
        val actual = OAuthFetchTokenAuthorizationInterceptor.getInstance(service).intercept(chain)

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
    fun `Given intercept was called, fetches and removes the alias from the HEADER, replaces HEADER_AUTHORIZATION with the resolved token and returns the Requests Response`() {
        // Given
        val token = "token"
        val service: AuthorizationContract.Service = mockk()
        val response: Response = mockk()
        val alias = ALIAS

        val chain: Interceptor.Chain = mockk()
        val request: Request = mockk()
        val modifiedRequest: Request = mockk()
        val builder: Request.Builder = mockk()

        every { chain.request() } returns request
        every { service.getAccessToken(alias) } returns token
        every { request.newBuilder() } returns builder
        every { request.header(HEADER_AUTHORIZATION) } returns ACCESS_TOKEN_MARKER
        every { request.header(HEADER_ALIAS) } returns alias
        every { builder.removeHeader(HEADER_ALIAS) } returns builder
        every {
            builder.removeHeader(HEADER_AUTHORIZATION)
        } returns builder
        every {
            builder.addHeader(HEADER_AUTHORIZATION, "Bearer $token")
        } returns builder
        every {
            builder.build()
        } returns modifiedRequest

        every { chain.proceed(modifiedRequest) } returns response

        // When
        val actual = OAuthFetchTokenAuthorizationInterceptor.getInstance(service).intercept(chain)

        // Then
        assertSame(
            actual = actual,
            expected = response
        )
        verifyOrder {
            chain.request()
            request.header(HEADER_ALIAS)
            request.header(HEADER_AUTHORIZATION)
            service.getAccessToken(alias)
            request.newBuilder()
            builder.removeHeader(HEADER_ALIAS)
            builder.removeHeader(HEADER_AUTHORIZATION)
            builder.addHeader(HEADER_AUTHORIZATION, "Bearer $token")
            builder.build()
            chain.proceed(modifiedRequest)
        }
    }

    @Test
    fun `Given intercept was called, forwards the Request, if it cannot resolve the token`() {
        // Given
        val service: AuthorizationContract.Service = mockk()
        val response: Response = mockk()
        val alias = ALIAS

        val chain: Interceptor.Chain = mockk()
        val request: Request = mockk()

        every { chain.request() } returns request
        every { request.header(HEADER_ALIAS) } returns alias
        every { request.header(HEADER_AUTHORIZATION) } returns ACCESS_TOKEN_MARKER
        every { service.getAccessToken(alias) } answers { throw D4LException() }

        every { chain.proceed(request) } returns response

        // When
        val actual = OAuthFetchTokenAuthorizationInterceptor.getInstance(service).intercept(chain)

        // Then
        assertSame(
            actual = actual,
            expected = response
        )

        verifyOrder {
            chain.request()
            request.header(HEADER_ALIAS)
            request.header(HEADER_AUTHORIZATION)
            service.getAccessToken(alias)
            chain.proceed(request)
        }
    }
}
