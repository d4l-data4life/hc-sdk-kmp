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
import care.data4life.sdk.network.NetworkingContract.Companion.HTTP_401_UNAUTHORIZED
import care.data4life.sdk.test.util.GenericTestDataProvider.ALIAS
import care.data4life.sdk.test.util.GenericTestDataProvider.AUTH_TOKEN
import io.mockk.Called
import io.mockk.Runs
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifyOrder
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import org.junit.Before
import org.junit.Test
import kotlin.test.assertSame
import kotlin.test.assertTrue

class OAuthRetryAuthorizationInterceptorTest {
    private val service: AuthorizationContract.Service = mockk()

    @Before
    fun setUp() {
        clearAllMocks()
    }

    @Test
    fun `It fulfils ParialInterceptor`() {
        val interceptor: Any = OAuthRetryTokenAuthorizationInterceptor(service)

        assertTrue(interceptor is NetworkingContract.PartialInterceptor<*>)
    }

    @Test
    fun `Given a interceptor was created and intercept was called, it resolves the Alias and if AUTHORIZATION_WITH_ACCESS_TOKEN is present, while forwarding the Request`() {
        val response: Response = mockk()
        val chain: Interceptor.Chain = mockk()
        val request: Request = mockk()

        every { chain.proceed(request) } returns response
        every { response.code } returns 418

        // When
        val actual = OAuthRetryTokenAuthorizationInterceptor(service).intercept(
            Triple("egal", request, response),
            chain
        )

        // Then
        assertSame(
            actual = actual,
            expected = response
        )

        verify { request wasNot Called }
        verify { chain wasNot Called }
    }

    @Test
    fun `Given a interceptor was created and intercept was called, intercepts the Response, refreshes the Token and retries the Request, if the ResponseCode was 401 and the AccessToken marker is present`() {
        val alias = ALIAS
        val token = AUTH_TOKEN
        val orgResponse: Response = mockk()
        val newResponse: Response = mockk()

        val chain: Interceptor.Chain = mockk()
        val request: Request = mockk()

        val modifiedRequest: Request = mockk()
        val builder: Request.Builder = mockk()

        every { chain.proceed(modifiedRequest) } returns newResponse

        every { orgResponse.code } returns HTTP_401_UNAUTHORIZED
        every { orgResponse.close() } just Runs

        every { service.refreshAccessToken(alias) } returns token

        every { request.newBuilder() } returns builder
        every {
            builder.removeHeader(NetworkingContract.HEADER_AUTHORIZATION)
        } returns builder
        every {
            builder.addHeader(NetworkingContract.HEADER_AUTHORIZATION, "Bearer $token")
        } returns builder
        every {
            builder.build()
        } returns modifiedRequest

        // When
        val actual = OAuthRetryTokenAuthorizationInterceptor(service).intercept(
            Triple(alias, request, orgResponse),
            chain
        )

        // Then
        assertSame(
            actual = actual,
            expected = newResponse
        )

        verifyOrder {
            service.refreshAccessToken(alias)
            builder.removeHeader(NetworkingContract.HEADER_AUTHORIZATION)
            builder.addHeader(NetworkingContract.HEADER_AUTHORIZATION, "Bearer $token")
            chain.proceed(modifiedRequest)
        }
    }

    @Test
    fun `Given a interceptor was created and intercept was called, intercepts the Response, clears the authService and returns the failed Response, if the refreshment of the token fails`() {
        val alias = ALIAS
        val response: Response = mockk()

        val chain: Interceptor.Chain = mockk()
        val request: Request = mockk()

        every { response.code } returns HTTP_401_UNAUTHORIZED

        every { service.refreshAccessToken(alias) } answers { throw D4LException("fail") }
        every { service.clear() } just Runs

        // When
        val actual = OAuthRetryTokenAuthorizationInterceptor(service).intercept(
            Triple(alias, request, response),
            chain
        )

        // Then
        assertSame(
            actual = actual,
            expected = response
        )

        verify { request wasNot Called }
        verify { chain wasNot Called }
    }
}
