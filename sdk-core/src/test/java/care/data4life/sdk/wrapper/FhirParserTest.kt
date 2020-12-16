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
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Ignore
import org.junit.Test

class FhirParserTest {

    @Test
    fun `it is a ResourceParser`() {
        assertTrue((FhirParser as Any) is WrapperContract.FhirParser)
    }

    @Test
    fun `Given, toFhir3 is called with a unknown ResourceType and a Source, it fails`() {
        try {
            // When
            FhirParser.toFhir3("something", "a test")
            assertTrue(false)//Fixme
        } catch (e: Exception) {
            // Then
            assertTrue(e is CoreRuntimeException.InternalFailure)
        }
    }

    @Ignore
    @Test
    fun `Given, toFhir3 is called with a ResourceType and a Source, but it could not be parsed, it fails`() {
       try {
            // When
            FhirParser.toFhir3("DocumentReference", "a test")
            assertTrue(false)//Fixme
        } catch (e: Exception) {
            // Then
            assertTrue(e is CoreRuntimeException.InternalFailure)
        }
    }

    @Test
    fun `Given, toFhir3 is called with a ResourceType and a Source, it returns a Resource`() {
        // Given
        val type = "DocumentReference"
        val source = "{\"resourceType\":\"DomainResource\"}"

        // When
        val resource = FhirParser.toFhir3(type, source)

        // Then
        assertSame(
                resource.type,
                WrapperContract.Resource.TYPE.FHIR3
        )
    }

    @Test
    fun `Given, toFhir4 is called with a unknown ResourceType and a Source, it fails`() {
        try {
            // When
            FhirParser.toFhir4("something", "a test")
            assertTrue(false)//Fixme
        } catch (e: Exception) {
            // Then
            assertTrue(e is CoreRuntimeException.InternalFailure)
        }
    }

    @Ignore
    @Test // Test is working, but the Constructor Mock causes flakyness
    fun `Given, toFhir4 is called with a ResourceType and a Source, but it could not be parsed, it fails`() {
        try {
            // When
            FhirParser.toFhir4("DocumentReference", "a test")
            assertTrue(false)//Fixme
        } catch (e: Exception) {
            // Then
            assertTrue(e is CoreRuntimeException.InternalFailure)
        }
    }

    @Test // Test is working, but the Constructor Mock causes flakyness
    fun `Given, toFhir4 is called with a ResourceType and a Source, it returns a Resource`() {
        // Given
        val type = "DocumentReference"
        val source = "{\"resourceType\":\"DomainResource\"}"

        // When
        val resource = FhirParser.toFhir4(type, source)

        assertSame(
                resource.type,
                WrapperContract.Resource.TYPE.FHIR4
        )
    }

    @Test
    fun `Given, fromResource with a non Fhir Resource, it fails`() {
        val resource = mockk<WrapperContract.Resource>()

        every { resource.type } returns WrapperContract.Resource.TYPE.DATA

        try {
            // When
            FhirParser.fromResource(resource)
            assertTrue(false)//Fixme
        } catch (e: Exception) {
            // Then
            assertTrue(e is CoreRuntimeException.InternalFailure)
        }
    }

    @Test
    fun `Given, fromResource with a Fhir3 Resource, it serializes it`() {
        val resource = ResourceFactory.wrap(Fhir3Resource())!!

        assertEquals(
                FhirParser.fromResource(resource),
                "{\"resourceType\":\"DomainResource\"}"
        )
    }

    @Test
    fun `Given, fromResource with a Fhir4 Resource, it serializes it`() {
        val resource = ResourceFactory.wrap(Fhir4Resource())!!

        assertEquals(
                FhirParser.fromResource(resource),
                "{\"resourceType\":\"DomainResource\"}"
        )
    }
}
