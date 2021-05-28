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

package care.data4life.sdk.wrapper

import care.data4life.sdk.fhir.Fhir3Resource
import care.data4life.sdk.fhir.Fhir4Resource
import care.data4life.sdk.fhir.FhirContract
import care.data4life.sdk.lang.CoreRuntimeException
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class FhirParserTest {
    @Test
    fun `It fulfils ResourceParser`() {
        val parser: Any = SdkFhirParser
        assertTrue(parser is WrapperContract.FhirParser)
    }

    @Test
    fun `Given, toFhir is called with a ResourceType, a unknown version and a Source, it fails`() {
        assertFailsWith<CoreRuntimeException.UnsupportedOperation> {
            SdkFhirParser.toFhir("type", "unknown", "resource")
        }
    }

    @Test
    fun `Given, toFhir is called with a ResourceType, a Fhir3 version and a Source, it returns a Fhir3Resource`() {
        // Given
        val type = "DocumentReference"
        val source = "{\"resourceType\":\"DocumentReference\"}"

        // When
        val resource: Any = SdkFhirParser.toFhir(
            type,
            FhirContract.FhirVersion.FHIR_3.version,
            source
        )

        // Then
        assertTrue(resource is Fhir3Resource)
    }

    @Test
    fun `Given, toFhir is called with a ResourceType, a Fhir4 version and a Source, it returns a Fhir4Resource`() {
        // Given
        val type = "DocumentReference"
        val source = "{\"resourceType\":\"DomainResource\"}"

        // When
        val resource: Any = SdkFhirParser.toFhir(
            type,
            FhirContract.FhirVersion.FHIR_4.version,
            source
        )

        // Then
        assertTrue(resource is Fhir4Resource)
    }

    @Test
    fun `Given, fromResource with a non Fhir Resource, it fails`() {
        assertFailsWith<CoreRuntimeException.InternalFailure> {
            SdkFhirParser.fromResource("resource")
        }
    }

    @Test
    fun `Given, fromResource with a Fhir3 Resource, it serializes it`() {
        val resource = Fhir3Resource()

        assertEquals(
            SdkFhirParser.fromResource(resource),
            "{\"resourceType\":\"DomainResource\"}"
        )
    }

    @Test
    fun `Given, fromResource with a Fhir4 Resource, it serializes it`() {
        val resource = Fhir4Resource()

        assertEquals(
            SdkFhirParser.fromResource(resource),
            "{\"resourceType\":\"DomainResource\"}"
        )
    }
}
