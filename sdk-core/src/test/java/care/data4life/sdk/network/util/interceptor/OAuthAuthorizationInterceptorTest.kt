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

import care.data4life.auth.AuthorizationContract
import care.data4life.sdk.lang.CoreRuntimeException
import care.data4life.sdk.network.NetworkingContract.Companion.ACCESS_TOKEN_MARKER
import care.data4life.sdk.network.NetworkingContract.Companion.HEADER_ALIAS
import care.data4life.sdk.network.NetworkingContract.Companion.HEADER_AUTHORIZATION
import care.data4life.sdk.network.NetworkingInternalContract
import care.data4life.sdk.test.util.GenericTestDataProvider.ALIAS
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.unmockkConstructor
import io.mockk.verifyOrder
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import org.junit.Before
import org.junit.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertSame
import kotlin.test.assertTrue

class OAuthAuthorizationInterceptorTest {
    private val service: AuthorizationContract.Service = mockk()

    @Before
    fun setUp() {
        clearAllMocks()
    }

    @Test
    fun `It fulfils InterceptorFactory`() {
        val factory: Any = OAuthAuthorizationInterceptor

        assertTrue(factory is NetworkingInternalContract.InterceptorFactory<*>)
    }

    @Test
    fun `Given getInstance is called with a AuthorizationContractService it returns a Interceptor`() {
        // When
        val interceptor: Any = OAuthAuthorizationInterceptor.getInstance(service)

        // Then
        assertTrue(interceptor is NetworkingInternalContract.Interceptor)
    }

    @Test
    fun `Given a interceptor was created and intercept was called, it simply proceeds with the cain, if no AUTHORIZATION_WITH_ACCESS_TOKEN is present`() {
        // Given
        val response: Response = mockk()
        val chain: Interceptor.Chain = mockk()
        val request: Request = mockk()

        every { request.header(HEADER_AUTHORIZATION) } returns "you should not care"
        every { chain.request() } returns request
        every { chain.proceed(request) } returns response

        // When
        val actual = OAuthAuthorizationInterceptor.getInstance(service).intercept(chain)

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
    fun `Given a interceptor was created and intercept was called, it fails, if AUTHORIZATION_WITH_ACCESS_TOKEN is present, but no alias could be determined`() {
        // Given
        val response: Response = mockk()
        val chain: Interceptor.Chain = mockk()
        val request: Request = mockk()

        every { request.header(HEADER_AUTHORIZATION) } returns ACCESS_TOKEN_MARKER
        every { request.header(HEADER_ALIAS) } returns null
        every { chain.request() } returns request
        every { chain.proceed(request) } returns response

        // Then
        assertFailsWith<CoreRuntimeException.InternalFailure> {
            // When
            OAuthAuthorizationInterceptor.getInstance(service).intercept(chain)
        }

        verifyOrder {
            chain.request()
            request.header(HEADER_AUTHORIZATION)
            request.header(HEADER_ALIAS)
        }
    }

    @Test
    fun `Given a interceptor was created and intercept was called, it removes the HEADER_ALIAS, delegates Chain and Alias to its PartialInterceptors and returns the result`() {
        // Given
        val alias = ALIAS
        val chain: Interceptor.Chain = mockk()
        val request: Request = mockk()

        val cleanedRequest: Request = mockk()
        val builder: Request.Builder = mockk()

        val response: Response = mockk()
        val modifiedResponse: Response = mockk()

        mockkConstructor(OAuthFetchTokenAuthorizationInterceptor::class)
        mockkConstructor(OAuthRetryTokenAuthorizationInterceptor::class)

        every { request.header(HEADER_AUTHORIZATION) } returns ACCESS_TOKEN_MARKER
        every { request.header(HEADER_ALIAS) } returns alias

        every { request.newBuilder() } returns builder
        every {
            builder.removeHeader(HEADER_ALIAS)
        } returns builder
        every {
            builder.build()
        } returns cleanedRequest

        every { chain.request() } returns request
        every { chain.proceed(request) } returns response

        every {
            anyConstructed<OAuthFetchTokenAuthorizationInterceptor>()
                .intercept(Pair(alias, cleanedRequest), chain)
        } returns response

        every {
            anyConstructed<OAuthRetryTokenAuthorizationInterceptor>()
                .intercept(Triple(alias, cleanedRequest, response), chain)
        } returns modifiedResponse

        // When
        val actual = OAuthAuthorizationInterceptor.getInstance(service).intercept(chain)

        // Then
        assertSame(
            actual = actual,
            expected = modifiedResponse
        )

        verifyOrder {
            chain.request()
            request.header(HEADER_AUTHORIZATION)
            request.header(HEADER_ALIAS)
            builder.removeHeader(HEADER_ALIAS)
            anyConstructed<OAuthFetchTokenAuthorizationInterceptor>()
                .intercept(Pair(alias, cleanedRequest), chain)
            anyConstructed<OAuthRetryTokenAuthorizationInterceptor>()
                .intercept(Triple(alias, cleanedRequest, response), chain)
        }

        unmockkConstructor(OAuthFetchTokenAuthorizationInterceptor::class)
        unmockkConstructor(OAuthRetryTokenAuthorizationInterceptor::class)
    }
}
