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

package care.data4life.auth

import io.mockk.every
import io.mockk.mockk
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals

class AuthorizationApiTest {

    private lateinit var sut: AuthorizationApi


    @Before
    fun setUp() {
    }

    @Test
    fun authorizationBaseUrl() {
        val configuration = mockk<AuthorizationConfiguration>()
        every { configuration.authorizationEndpoint } returns AUTHORIZATION_ENDPOINT
        sut = AuthorizationApi(configuration)

        val actual = sut.getAuthorizationUrl(IGNORE, IGNORE, IGNORE, IGNORE, IGNORE, emptyMap())

        assertEquals(EXPECTED_AUTH_URL, actual)
    }

    @Test
    fun accessTokenEndpoint() {
        val configuration = mockk<AuthorizationConfiguration>()
        every { configuration.tokenEndpoint } returns TOKEN_ENDPOINT
        sut = AuthorizationApi(configuration)

        val actual = sut.accessTokenEndpoint

        assertEquals(EXPECTED_TOKEN_URL, actual)
    }


    companion object {
        private const val IGNORE = ""

        private const val EXPECTED_AUTH_URL = "expected_authorization_endpoint/oauth/authorize?response_type=&client_id=&redirect_uri=&scope=&state="
        private const val EXPECTED_TOKEN_URL = "expected_token_endpoint/oauth/token"

        private const val AUTHORIZATION_ENDPOINT = "expected_authorization_endpoint"
        private const val TOKEN_ENDPOINT = "expected_token_endpoint"

    }

}
