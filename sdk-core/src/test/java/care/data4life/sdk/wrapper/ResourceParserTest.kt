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

import care.data4life.fhir.Fhir
import care.data4life.fhir.FhirParser
import care.data4life.sdk.fhir.Fhir3Resource
import care.data4life.sdk.fhir.Fhir4Resource
import care.data4life.sdk.lang.CoreRuntimeException
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.mockkObject
import io.mockk.unmockkConstructor
import io.mockk.unmockkObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Ignore
import org.junit.Test

class ResourceParserTest {
    @Before
    fun setUp() {
        mockkConstructor(Fhir::class)
        mockkObject(FhirElementFactory)
        mockkObject(ResourceFactory)
    }

    @After
    fun tearDown() {
        unmockkConstructor(Fhir::class)
        unmockkObject(FhirElementFactory)
        unmockkObject(ResourceFactory)
    }


    @Test
    fun `it is a ResourceParser`() {
        assertTrue((ResourceParser as Any) is WrapperContract.ResourceParser )
    }

    @Test
    fun `Given, toFhir3 is called with a unknown ResourceType and a Source, it fails`() {
        try {
            // When
            ResourceParser.toFhir3("something", "a test")
            assertTrue(false)//Fixme
        } catch (e: Exception) {
            // Then
            assertTrue(e is CoreRuntimeException.InternalFailure)
        }
    }

    @Test
    fun `Given, toFhir3 is called with a ResourceType and a Source, but it could not be parsed, it fails`() {
        // Given
        val type = "DocumentReference"
        val source = "sampleSource"
        val resourceClass = Fhir3Resource::class.java

        val parser = mockk<FhirParser<Any>>()

        every { FhirElementFactory.getFhir3ClassForType(type) } returns resourceClass
        every { anyConstructed<Fhir>().createStu3Parser() } returns parser
        every { parser.toFhir(resourceClass, source) } returns null

        try {
            // When
            ResourceParser.toFhir3("something", "a test")
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
        val resource = ResourceParser.toFhir3(type, source)

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
            ResourceParser.toFhir4("something", "a test")
            assertTrue(false)//Fixme
        } catch (e: Exception) {
            // Then
            assertTrue(e is CoreRuntimeException.InternalFailure)
        }
    }

    @Ignore
    @Test // Test is working, but the Constructor Mock causes flakyness
    fun `Given, toFhir4 is called with a ResourceType and a Source, but it could not be parsed, it fails`() {
        // Given
        val type = "DocumentReference"
        val source = "sampleSource"
        val resourceClass = Fhir4Resource::class.java

        val parser = mockk<FhirParser<Any>>()

        every { FhirElementFactory.getFhir4ClassForType(type) } returns resourceClass
        every { anyConstructed<Fhir>().createR4Parser() } returns parser
        every { parser.toFhir(resourceClass, source) } returns null

        try {
            // When
            ResourceParser.toFhir4("something", "a test")
            assertTrue(false)//Fixme
        } catch (e: Exception) {
            // Then
            assertTrue(e is CoreRuntimeException.InternalFailure)
        }
    }

    @Ignore
    @Test // Test is working, but the Constructor Mock causes flakyness
    fun `Given, toFhir4 is called with a ResourceType and a Source, it returns a Resource`() {
        // Given
        val type = "DocumentReference"
        val source = "sampleSource"
        val resourceClass = Fhir4Resource::class.java

        val rawResource = mockk<Fhir4Resource>()
        val resource = mockk<WrapperContract.Resource>()

        val parser = mockk<FhirParser<Any>>()

        every { FhirElementFactory.getFhir4ClassForType(type) } returns resourceClass
        every { anyConstructed<Fhir>().createR4Parser() } returns parser
        every { parser.toFhir(resourceClass, source) } returns rawResource
        every { ResourceFactory.wrap(rawResource) } returns resource

        // Then
        assertSame(
                ResourceParser.toFhir4(type, source),
                resource
        )
    }

    @Test
    fun `Given, fromResource with a non Fhir Resource, it fails`() {
        val resource = mockk<WrapperContract.Resource>()

        every { resource.type } returns WrapperContract.Resource.TYPE.DATA

        try {
            // When
            ResourceParser.fromResource(resource)
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
                ResourceParser.fromResource(resource),
                "{\"resourceType\":\"DomainResource\"}"
        )
    }
}
