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

import care.data4life.sdk.network.NetworkingContract
import care.data4life.sdk.network.NetworkingContract.Companion.HEADER_ALIAS
import care.data4life.sdk.network.NetworkingContract.Companion.HEADER_AUTHORIZATION
import io.mockk.every
import io.mockk.mockk
import io.mockk.verifyOrder
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import org.junit.Test
import kotlin.test.assertSame
import kotlin.test.assertTrue

class StaticAuthorizationInterceptorTest {
    @Test
    fun `It fulfils Interceptor`() {
        val interceptor: Any = StaticAuthorizationInterceptor("test")

        assertTrue(interceptor is NetworkingContract.Interceptor)
    }

    @Test
    fun `Given intercept is called with a chain, it removes HEADER_ALIAS and replaces HEADER_AUTHORIZATION and returns the Response of the modified Request`() {
        // Given
        val token = "token"

        val response: Response = mockk()

        val chain: Interceptor.Chain = mockk()
        val request: Request = mockk()
        val modifiedRequest: Request = mockk()
        val builder: Request.Builder = mockk()

        every { chain.request() } returns request
        every { request.newBuilder() } returns builder
        every { builder.removeHeader(HEADER_ALIAS) } returns builder
        every {
            builder.header(HEADER_AUTHORIZATION, "Bearer $token")
        } returns builder
        every {
            builder.build()
        } returns modifiedRequest

        every { chain.proceed(modifiedRequest) } returns response

        // When
        val actual = StaticAuthorizationInterceptor(token).intercept(chain)

        // Then
        assertSame(
            actual = actual,
            expected = response
        )

        verifyOrder {
            builder.removeHeader(HEADER_ALIAS)
            builder.header(NetworkingContract.HEADER_AUTHORIZATION, "Bearer $token")
            builder.build()
        }
    }
}
