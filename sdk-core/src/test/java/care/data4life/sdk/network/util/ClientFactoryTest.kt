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

package care.data4life.sdk.network.util

import care.data4life.auth.AuthorizationContract
import care.data4life.sdk.network.NetworkingContract
import care.data4life.sdk.network.NetworkingContract.Companion.PLATFORM_S4H
import care.data4life.sdk.network.NetworkingContract.Companion.REQUEST_TIMEOUT
import care.data4life.sdk.network.NetworkingInternalContract
import care.data4life.sdk.network.util.interceptor.BasicAuthorizationInterceptor
import care.data4life.sdk.network.util.interceptor.LoggingInterceptor
import care.data4life.sdk.network.util.interceptor.OAuthAuthorizationInterceptor
import care.data4life.sdk.network.util.interceptor.RetryInterceptor
import care.data4life.sdk.network.util.interceptor.StaticAuthorizationInterceptor
import care.data4life.sdk.network.util.interceptor.VersionInterceptor
import care.data4life.sdk.test.util.GenericTestDataProvider.AUTH_TOKEN
import care.data4life.sdk.test.util.GenericTestDataProvider.CLIENT_ID
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import io.mockk.verify
import okhttp3.CertificatePinner
import okhttp3.OkHttpClient
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ClientFactoryTest {
    private val authService: AuthorizationContract.Service = mockk()
    private val environment: NetworkingContract.Environment = mockk()
    private val connectivityService: NetworkingContract.NetworkConnectivityService = mockk()
    private val clientName: NetworkingContract.Clients = mockk()

    @Before
    fun setUp() {
        mockkObject(LoggingInterceptor)
        mockkObject(RetryInterceptor)
        mockkObject(OAuthAuthorizationInterceptor)
        mockkObject(BasicAuthorizationInterceptor)
        mockkObject(StaticAuthorizationInterceptor)
        mockkObject(VersionInterceptor)
        mockkObject(CertificatePinnerFactory)
    }

    @After
    fun tearDown() {
        unmockkObject(LoggingInterceptor)
        unmockkObject(RetryInterceptor)
        unmockkObject(OAuthAuthorizationInterceptor)
        unmockkObject(BasicAuthorizationInterceptor)
        unmockkObject(StaticAuthorizationInterceptor)
        mockkObject(VersionInterceptor)
        unmockkObject(CertificatePinnerFactory)
    }

    @Test
    fun `It fulfils ClientFactory`() {
        val factory: Any = ClientFactory

        assertTrue(factory is NetworkingInternalContract.ClientFactory)
    }

    @Test
    fun `Given, getInstance is called with its appropriate parameter, which does not include a static token it builds a Client, with the OAuthInterceptor`() {
        // Given
        val clientID = CLIENT_ID
        val clientVersion = "1.2.3"
        val secret = "geheim"
        val platform = "form"
        val staticAccessToken = null
        val flag = false

        val loggingInterceptor: NetworkingInternalContract.Interceptor = mockk()
        val retryInterceptor: NetworkingInternalContract.Interceptor = mockk()
        val versionInterceptor: NetworkingInternalContract.Interceptor = mockk()
        val oAuthInterceptor: NetworkingInternalContract.Interceptor = mockk()
        val basicInterceptor: NetworkingInternalContract.Interceptor = mockk()
        val pinner: CertificatePinner = mockk(relaxed = true)

        every { LoggingInterceptor.getInstance(flag) } returns loggingInterceptor
        every { RetryInterceptor.getInstance(connectivityService) } returns retryInterceptor
        every {
            VersionInterceptor.getInstance(
                Pair(
                    clientName,
                    clientVersion
                )
            )
        } returns versionInterceptor
        every { OAuthAuthorizationInterceptor.getInstance(authService) } returns oAuthInterceptor
        every {
            BasicAuthorizationInterceptor.getInstance(
                Pair(
                    clientID,
                    secret
                )
            )
        } returns basicInterceptor

        every { CertificatePinnerFactory.getInstance(platform, environment) } returns pinner

        // When
        val client: Any = ClientFactory.getInstance(
            authService,
            environment,
            clientID,
            secret,
            platform,
            connectivityService,
            clientName,
            clientVersion,
            staticAccessToken,
            flag
        )

        // Then
        assertTrue(client is OkHttpClient)

        val expectedInterceptors = listOf(
            loggingInterceptor,
            retryInterceptor,
            versionInterceptor,
            oAuthInterceptor,
            basicInterceptor
        )

        expectedInterceptors.forEach {
            assertTrue(it in client.interceptors)
        }

        assertEquals(
            actual = client.interceptors.size,
            expected = expectedInterceptors.size
        )

        assertEquals(
            actual = ((client.callTimeoutMillis / 1000) / 60).toLong(),
            expected = REQUEST_TIMEOUT
        )
        assertEquals(
            actual = ((client.callTimeoutMillis / 1000) / 60).toLong(),
            expected = REQUEST_TIMEOUT
        )
        assertEquals(
            actual = ((client.callTimeoutMillis / 1000) / 60).toLong(),
            expected = REQUEST_TIMEOUT
        )

        // Workaround since we cannot mock internal methods
        assertEquals(
            actual = client.certificatePinner.toString()
                .replace("CertificatePinner(child of #", "")
                .split('#')[0],
            expected = pinner.toString()
                .replace("CertificatePinner(#", "")
                .trim(')')
        )

        verify(exactly = 1) { CertificatePinnerFactory.getInstance(platform, environment) }
    }

    @Test
    fun `Given, getInstance is called with its appropriate parameter, which does not include a static token and S4H as target, it builds a Client, with the OAuthInterceptor and without a Certificatepin`() {
        // Given
        val clientID = CLIENT_ID
        val clientVersion = "1.2.3"
        val secret = "geheim"
        val platform = PLATFORM_S4H
        val staticAccessToken = null
        val flag = false

        val loggingInterceptor: NetworkingInternalContract.Interceptor = mockk()
        val retryInterceptor: NetworkingInternalContract.Interceptor = mockk()
        val versionInterceptor: NetworkingInternalContract.Interceptor = mockk()
        val oAuthInterceptor: NetworkingInternalContract.Interceptor = mockk()
        val basicInterceptor: NetworkingInternalContract.Interceptor = mockk()
        val pinner: CertificatePinner = mockk(relaxed = true)

        every { LoggingInterceptor.getInstance(flag) } returns loggingInterceptor
        every { RetryInterceptor.getInstance(connectivityService) } returns retryInterceptor
        every {
            VersionInterceptor.getInstance(
                Pair(
                    clientName,
                    clientVersion
                )
            )
        } returns versionInterceptor
        every { OAuthAuthorizationInterceptor.getInstance(authService) } returns oAuthInterceptor
        every {
            BasicAuthorizationInterceptor.getInstance(
                Pair(
                    clientID,
                    secret
                )
            )
        } returns basicInterceptor

        every { CertificatePinnerFactory.getInstance(platform, environment) } returns pinner

        // When
        val client: Any = ClientFactory.getInstance(
            authService,
            environment,
            clientID,
            secret,
            platform,
            connectivityService,
            clientName,
            clientVersion,
            staticAccessToken,
            flag
        )

        // Then
        assertTrue(client is OkHttpClient)

        val expectedInterceptors = listOf(
            loggingInterceptor,
            retryInterceptor,
            versionInterceptor,
            oAuthInterceptor,
            basicInterceptor
        )

        expectedInterceptors.forEach {
            assertTrue(it in client.interceptors)
        }

        assertEquals(
            actual = client.interceptors.size,
            expected = expectedInterceptors.size
        )

        assertEquals(
            actual = ((client.callTimeoutMillis / 1000) / 60).toLong(),
            expected = REQUEST_TIMEOUT
        )
        assertEquals(
            actual = ((client.callTimeoutMillis / 1000) / 60).toLong(),
            expected = REQUEST_TIMEOUT
        )
        assertEquals(
            actual = ((client.callTimeoutMillis / 1000) / 60).toLong(),
            expected = REQUEST_TIMEOUT
        )

        verify(exactly = 0) { CertificatePinnerFactory.getInstance(platform, environment) }
    }

    @Test
    fun `Given, getInstance is called with its appropriate parameter, which includes a static token it builds a Client, with the StaticAuthInterceptor`() {
        // Given
        val clientID = CLIENT_ID
        val clientVersion = "1.2.3"
        val secret = "geheim"
        val platform = "form"
        val staticAccessToken = AUTH_TOKEN
        val flag = false

        val loggingInterceptor: NetworkingInternalContract.Interceptor = mockk()
        val retryInterceptor: NetworkingInternalContract.Interceptor = mockk()
        val versionInterceptor: NetworkingInternalContract.Interceptor = mockk()
        val staticAuthorizationInterceptor: NetworkingInternalContract.Interceptor = mockk()
        val pinner: CertificatePinner = mockk(relaxed = true)

        every { LoggingInterceptor.getInstance(flag) } returns loggingInterceptor
        every { RetryInterceptor.getInstance(connectivityService) } returns retryInterceptor
        every {
            VersionInterceptor.getInstance(
                Pair(
                    clientName,
                    clientVersion
                )
            )
        } returns versionInterceptor
        every { StaticAuthorizationInterceptor.getInstance(staticAccessToken) } returns staticAuthorizationInterceptor

        every { CertificatePinnerFactory.getInstance(platform, environment) } returns pinner

        // When
        val client: Any = ClientFactory.getInstance(
            authService,
            environment,
            clientID,
            secret,
            platform,
            connectivityService,
            clientName,
            clientVersion,
            staticAccessToken.toByteArray(),
            flag
        )

        // Then
        assertTrue(client is OkHttpClient)

        val expectedInterceptors = listOf(
            loggingInterceptor,
            retryInterceptor,
            versionInterceptor,
            staticAuthorizationInterceptor
        )

        expectedInterceptors.forEach {
            assertTrue(it in client.interceptors)
        }

        assertEquals(
            actual = client.interceptors.size,
            expected = expectedInterceptors.size
        )

        assertEquals(
            actual = ((client.callTimeoutMillis / 1000) / 60).toLong(),
            expected = REQUEST_TIMEOUT
        )
        assertEquals(
            actual = ((client.callTimeoutMillis / 1000) / 60).toLong(),
            expected = REQUEST_TIMEOUT
        )
        assertEquals(
            actual = ((client.callTimeoutMillis / 1000) / 60).toLong(),
            expected = REQUEST_TIMEOUT
        )

        // Workaround since we cannot mock internal methods
        assertEquals(
            actual = client.certificatePinner.toString()
                .replace("CertificatePinner(child of #", "")
                .split('#')[0],
            expected = pinner.toString()
                .replace("CertificatePinner(#", "")
                .trim(')')
        )
    }
}
