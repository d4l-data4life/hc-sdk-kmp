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
import care.data4life.sdk.lang.D4LException
import care.data4life.sdk.network.NetworkingContract.Companion.HEADER_AUTHORIZATION
import care.data4life.sdk.network.NetworkingInternalContract
import care.data4life.sdk.test.util.GenericTestDataProvider.ALIAS
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verifyOrder
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import org.junit.Before
import org.junit.Test
import kotlin.test.assertSame
import kotlin.test.assertTrue

class OAuthFetchTokenAuthorizationInterceptorTest {
    private val service: AuthorizationContract.Service = mockk()

    @Before
    fun setUp() {
        clearAllMocks()
    }

    @Test
    fun `It fulfils PartialInterceptor`() {
        val interceptor: Any = OAuthFetchTokenAuthorizationInterceptor(service)

        assertTrue(interceptor is NetworkingInternalContract.PartialInterceptor<*>)
    }

    @Test
    fun `Given intercept was called, it replaces HEADER_AUTHORIZATION with the resolved token and returns the Requests Response`() {
        // Given
        val token = "token"
        val response: Response = mockk()
        val alias = ALIAS

        val chain: Interceptor.Chain = mockk()
        val request: Request = mockk()
        val modifiedRequest: Request = mockk()
        val builder: Request.Builder = mockk()

        every { service.getAccessToken(alias) } returns token

        every { request.newBuilder() } returns builder
        every {
            builder.header(HEADER_AUTHORIZATION, "Bearer $token")
        } returns builder
        every {
            builder.build()
        } returns modifiedRequest

        every { chain.proceed(modifiedRequest) } returns response

        // When
        val actual = OAuthFetchTokenAuthorizationInterceptor(service).intercept(
            Pair(alias, request),
            chain
        )

        // Then
        assertSame(
            actual = actual,
            expected = response
        )
        verifyOrder {
            service.getAccessToken(alias)
            request.newBuilder()
            builder.header(HEADER_AUTHORIZATION, "Bearer $token")
            builder.build()
            chain.proceed(modifiedRequest)
        }
    }

    @Test
    fun `Given intercept was called, forwards the Request, if it cannot resolve the token`() {
        // Given
        val response: Response = mockk()
        val alias = ALIAS

        val chain: Interceptor.Chain = mockk()
        val request: Request = mockk()

        every { service.getAccessToken(alias) } answers { throw D4LException() }

        every { chain.proceed(request) } returns response

        // When
        val actual = OAuthFetchTokenAuthorizationInterceptor(service).intercept(
            Pair(alias, request),
            chain
        )

        // Then
        assertSame(
            actual = actual,
            expected = response
        )

        verifyOrder {
            service.getAccessToken(alias)
            chain.proceed(request)
        }
    }
}
