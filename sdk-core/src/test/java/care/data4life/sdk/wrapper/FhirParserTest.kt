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
import care.data4life.sdk.lang.CoreRuntimeException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FhirParserTest {

    @Test
    fun `it is a ResourceParser`() {
        assertTrue((SdkFhirParser as Any) is WrapperContract.FhirParser)
    }

    @Test
    fun `Given, toFhir3 is called with a ResourceType and a Source, it returns a Resource`() {
        // Given
        val type = "DocumentReference"
        val source = "{\"resourceType\":\"DomainResource\"}"

        // When
        val resource = SdkFhirParser.toFhir3(type, source)

        // Then
        assertTrue( resource is Fhir3Resource )
    }

    @Test // Test is working, but the Constructor Mock causes flakyness
    fun `Given, toFhir4 is called with a ResourceType and a Source, it returns a Resource`() {
        // Given
        val type = "DocumentReference"
        val source = "{\"resourceType\":\"DomainResource\"}"

        // When
        val resource: Any = SdkFhirParser.toFhir4(type, source)

        assertTrue( resource is Fhir4Resource )
    }

    @Test
    fun `Given, fromResource with a non Fhir Resource, it fails`() {

        try {
            // When
            SdkFhirParser.fromResource("resource")
            assertTrue(false)//Fixme
        } catch (e: Exception) {
            // Then
            assertTrue(e is CoreRuntimeException.InternalFailure)
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
