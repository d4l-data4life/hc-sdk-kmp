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

package care.data4life.sdk.network

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class EnvironmentTest {
    @Test
    fun `It fulfils EnvironmentFactory`() {
        val environment: Any = Environment

        assertTrue(environment is NetworkingContract.EnvironmentFactory)
    }

    @Test
    fun `Given fromName is called with an string arbitrary string, it returns a production Environment`() {
        val environment: Any = Environment.fromName("totally random")

        assertTrue(environment is NetworkingContract.Environment)
        assertEquals(
            actual = environment,
            expected = Environment.PRODUCTION
        )
    }

    @Test
    fun `Given fromName is called with null, it returns a production Environment`() {
        val environment: Any = Environment.fromName(null)

        assertTrue(environment is NetworkingContract.Environment)
        assertEquals(
            actual = environment,
            expected = Environment.PRODUCTION
        )
    }

    @Test
    fun `Given fromName is called with production, it returns a production Environment`() {
        val environment: Any = Environment.fromName("production")

        assertTrue(environment is NetworkingContract.Environment)
        assertEquals(
            actual = environment,
            expected = Environment.PRODUCTION
        )
    }

    @Test
    fun `Given fromName is called with local, it returns a local Environment`() {
        val environment: Any = Environment.fromName("local")

        assertTrue(environment is NetworkingContract.Environment)
        assertEquals(
            actual = environment,
            expected = Environment.LOCAL
        )
    }

    @Test
    fun `Given fromName is called with development, it returns a development Environment`() {
        val environment: Any = Environment.fromName("development")

        assertTrue(environment is NetworkingContract.Environment)
        assertEquals(
            actual = environment,
            expected = Environment.DEVELOPMENT
        )
    }

    @Test
    fun `Given fromName is called with sandbox, it returns a sandbox Environment`() {
        val environment: Any = Environment.fromName("sandbox")

        assertTrue(environment is NetworkingContract.Environment)
        assertEquals(
            actual = environment,
            expected = Environment.SANDBOX
        )
    }

    @Test
    fun `Given fromName is called with staging, it returns a staging Environment`() {
        val environment: Any = Environment.fromName("staging")

        assertTrue(environment is NetworkingContract.Environment)
        assertEquals(
            actual = environment,
            expected = Environment.STAGING
        )
    }

    @Test
    fun `Given fromName is called, it ignores the casing of the given string`() {
        assertEquals(
            actual = Environment.fromName("SAndBoX"),
            expected = Environment.SANDBOX
        )
        assertEquals(
            actual = Environment.fromName("stAGiNg"),
            expected = Environment.STAGING
        )
        assertEquals(
            actual = Environment.fromName("DEvELopMEnt"),
            expected = Environment.DEVELOPMENT
        )
        assertEquals(
            actual = Environment.fromName("LOCAL"),
            expected = Environment.LOCAL
        )
    }

    // URI
    @Test
    fun `Given, getApiBaseURL is called with a unknown platform, it fails`() {
        // Given
        val platform = "this never passes"
        val env = Environment.fromName("local")

        // Then
        val error = assertFailsWith<IllegalArgumentException> {
            // When
            env.getApiBaseURL(platform)
        }

        // Then
        assertEquals(
            actual = error.message,
            expected = "No supported platform found for value($platform)"
        )
    }
    // D4L
    @Test
    fun `Given, getApiBaseURL is called with a d4l platform on production, it returns the appropriate URI`() {
        // Given
        val env = Environment.fromName("production")

        // When
        val uri = env.getApiBaseURL("d4l")

        // Then
        assertEquals(
            actual = uri,
            expected = "https://api.data4life.care"
        )
    }

    @Test
    fun `Given, getApiBaseURL is called with a d4l platform on local, it returns the appropriate URI`() {
        // Given
        val env = Environment.fromName("local")

        // When
        val uri = env.getApiBaseURL("d4l")

        // Then
        assertEquals(
            actual = uri,
            expected = "https://api.data4life.local"
        )
    }

    @Test
    fun `Given, getApiBaseURL is called with a d4l platform on staging, it returns the appropriate URI`() {
        // Given
        val env = Environment.fromName("staging")

        // When
        val uri = env.getApiBaseURL("d4l")

        // Then
        assertEquals(
            actual = uri,
            expected = "https://api-staging.data4life.care"
        )
    }

    @Test
    fun `Given, getApiBaseURL is called with a d4l platform on development, it returns the appropriate URI`() {
        // Given
        val env = Environment.fromName("development")

        // When
        val uri = env.getApiBaseURL("d4l")

        // Then
        assertEquals(
            actual = uri,
            expected = "https://api-phdp-dev.hpsgc.de"
        )
    }

    @Test
    fun `Given, getApiBaseURL is called with a d4l platform on sandbox, it returns the appropriate URI`() {
        // Given
        val env = Environment.fromName("sandbox")

        // When
        val uri = env.getApiBaseURL("d4l")

        // Then
        assertEquals(
            actual = uri,
            expected = "https://api-phdp-sandbox.hpsgc.de"
        )
    }

    // SH4
    @Test
    fun `Given, getApiBaseURL is called with a s4h platform on production, it returns the appropriate URI`() {
        // Given
        val env = Environment.fromName("production")

        // When
        val uri = env.getApiBaseURL("s4h")

        // Then
        assertEquals(
            actual = uri,
            expected = "https://api.smart4health.eu"
        )
    }

    @Test
    fun `Given, getApiBaseURL is called with a s4h platform on local, it returns the appropriate URI`() {
        // Given
        val env = Environment.fromName("local")

        // When
        val uri = env.getApiBaseURL("s4h")

        // Then
        assertEquals(
            actual = uri,
            expected = "https://api.smart4health.local"
        )
    }

    @Test
    fun `Given, getApiBaseURL is called with a s4h platform on staging, it returns the appropriate URI`() {
        // Given
        val env = Environment.fromName("staging")

        // When
        val uri = env.getApiBaseURL("s4h")

        // Then
        assertEquals(
            actual = uri,
            expected = "https://api-staging.smart4health.eu"
        )
    }

    @Test
    fun `Given, getApiBaseURL is called with a s4h platform on development, it returns the appropriate URI`() {
        // Given
        val env = Environment.fromName("development")

        // When
        val uri = env.getApiBaseURL("s4h")

        // Then
        assertEquals(
            actual = uri,
            expected = "https://api-dev.smart4health.eu"
        )
    }

    @Test
    fun `Given, getApiBaseURL is called with a s4h platform on sandbox, it returns the appropriate URI`() {
        // Given
        val env = Environment.fromName("sandbox")

        // When
        val uri = env.getApiBaseURL("s4h")

        // Then
        assertEquals(
            actual = uri,
            expected = "https://api-sandbox.smart4health.eu"
        )
    }

    @Test
    fun `Given, getApiBaseURL is called with a s4h platform on sandbox, it ignores the casing of the string`() {
        val env = Environment.fromName("sandbox")

        assertEquals(
            actual = env.getApiBaseURL("S4h"),
            expected = "https://api-sandbox.smart4health.eu"
        )

        assertEquals(
            actual = env.getApiBaseURL("d4L"),
            expected = "https://api-phdp-sandbox.hpsgc.de"
        )
    }

    // CERT
    @Test
    fun `Given, getCertificatePin is called with a unknown platform, it fails`() {
        // Given
        val platform = "this never passes"
        val env = Environment.fromName("local")

        // Then
        val error = assertFailsWith<IllegalArgumentException> {
            // When
            env.getCertificatePin(platform)
        }

        // Then
        assertEquals(
            actual = error.message,
            expected = "No supported platform found for value($platform)"
        )
    }

    // D4L
    @Test
    fun `Given, getCertificatePin is called with a d4l platform on production, it returns the appropriate Certificate`() {
        // Given
        val env = Environment.fromName("production")

        // When
        val cert = env.getCertificatePin("d4l")

        // Then
        assertEquals(
            actual = cert,
            expected = "sha256/AJvjswWs1n4m1KDmFNnTqBit2RHFvXsrVU3Uhxcoe4Y="
        )
    }

    @Test
    fun `Given, getCertificatePin is called with a d4l platform on local, it returns the appropriate Certificate`() {
        // Given
        val env = Environment.fromName("local")

        // When
        val cert = env.getCertificatePin("d4l")

        // Then
        assertEquals(
            actual = cert,
            expected = "sha256/3f81qEv2rjHvcrwof2egbKo5MjjSHaN/4DOl7R+pH0E="
        )
    }

    @Test
    fun `Given, getCertificatePin is called with a d4l platform on staging, it returns the appropriate Certificate`() {
        // Given
        val env = Environment.fromName("staging")

        // When
        val cert = env.getCertificatePin("d4l")

        // Then
        assertEquals(
            actual = cert,
            expected = "sha256/AJvjswWs1n4m1KDmFNnTqBit2RHFvXsrVU3Uhxcoe4Y="
        )
    }

    @Test
    fun `Given, getCertificatePin is called with a d4l platform on development, it returns the appropriate Certificate`() {
        // Given
        val env = Environment.fromName("development")

        // When
        val cert = env.getCertificatePin("d4l")

        // Then
        assertEquals(
            actual = cert,
            expected = "sha256/3f81qEv2rjHvcrwof2egbKo5MjjSHaN/4DOl7R+pH0E="
        )
    }

    @Test
    fun `Given, getCertificatePin is called with a d4l platform on sandbox, it returns the appropriate Certificate`() {
        // Given
        val env = Environment.fromName("sandbox")

        // When
        val cert = env.getCertificatePin("d4l")

        // Then
        assertEquals(
            actual = cert,
            expected = "sha256/3f81qEv2rjHvcrwof2egbKo5MjjSHaN/4DOl7R+pH0E="
        )
    }

    // S4H
    @Test
    fun `Given, getCertificatePin is called with a s4h platform on production, it returns the appropriate Certificate`() {
        // Given
        val env = Environment.fromName("production")

        // When
        val cert = env.getCertificatePin("s4h")

        // Then
        assertEquals(
            actual = cert,
            expected = "sha256/yPBKbgJMVnMeovGKbAtuz65sfy/gpDu0WTiuB8bE5G0="
        )
    }

    @Test
    fun `Given, getCertificatePin is called with a s4h platform on local, it returns the appropriate Certificate`() {
        // Given
        val env = Environment.fromName("local")

        // When
        val cert = env.getCertificatePin("s4h")

        // Then
        assertEquals(
            actual = cert,
            expected = "sha256/yPBKbgJMVnMeovGKbAtuz65sfy/gpDu0WTiuB8bE5G0="
        )
    }

    @Test
    fun `Given, getCertificatePin is called with a s4h platform on staging, it returns the appropriate Certificate`() {
        // Given
        val env = Environment.fromName("staging")

        // When
        val cert = env.getCertificatePin("s4h")

        // Then
        assertEquals(
            actual = cert,
            expected = "sha256/yPBKbgJMVnMeovGKbAtuz65sfy/gpDu0WTiuB8bE5G0="
        )
    }

    @Test
    fun `Given, getCertificatePin is called with a s4h platform on development, it returns the appropriate Certificate`() {
        // Given
        val env = Environment.fromName("development")

        // When
        val cert = env.getCertificatePin("s4h")

        // Then
        assertEquals(
            actual = cert,
            expected = "sha256/yPBKbgJMVnMeovGKbAtuz65sfy/gpDu0WTiuB8bE5G0="
        )
    }

    @Test
    fun `Given, getCertificatePin is called with a s4h platform on sandbox, it returns the appropriate Certificate`() {
        // Given
        val env = Environment.fromName("sandbox")

        // When
        val cert = env.getCertificatePin("s4h")

        // Then
        assertEquals(
            actual = cert,
            expected = "sha256/yPBKbgJMVnMeovGKbAtuz65sfy/gpDu0WTiuB8bE5G0="
        )
    }
}
