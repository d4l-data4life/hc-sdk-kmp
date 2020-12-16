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

import care.data4life.sdk.data.DataResource
import care.data4life.sdk.fhir.Fhir3Resource
import care.data4life.sdk.fhir.Fhir4Resource
import care.data4life.sdk.lang.CoreRuntimeException
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ResourceFactoryTest {
    @Test
    fun `it is a ResourceFactory`() {
        assertTrue((ResourceFactory as Any) is WrapperFactoryContract.ResourceFactory)
    }

    @Test
    fun `Given, wrap is called with a unknown Resource, it fails with CoreRuntimeExceptionInternalFailure`() {
        try {
            // When
            ResourceFactory.wrap("fail me!")
            assertTrue(false)//Fixme
        } catch (e: Exception) {
            // Then
            assertTrue(e is CoreRuntimeException.InternalFailure)
        }
    }

    @Test
    fun `Given, wrap is called with null as Resource, it returns null`() {
        assertNull(ResourceFactory.wrap(null))
    }

    @Test
    fun `Given, wrap is called with a DataResource, it returns a Resource, which has the TYPE DATA`() {
        // Given
        val dataResource = mockk<DataResource>()

        // When
        val resource: Any = ResourceFactory.wrap(dataResource)!!

        // Then
        assertTrue(resource is WrapperContract.Resource)
        assertEquals(
                WrapperContract.Resource.TYPE.DATA,
                (resource as WrapperContract.Resource).type
        )
    }

    @Test
    fun `Given, wrap is called with a Fhir3, it returns a Resource, which has the TYPE FHIR3`() {
        // Given
        val dataResource = mockk<Fhir3Resource>()

        // When
        val resource: Any = ResourceFactory.wrap(dataResource)!!

        // Then
        assertTrue(resource is WrapperContract.Resource)
        assertEquals(
                WrapperContract.Resource.TYPE.FHIR3,
                (resource as WrapperContract.Resource).type
        )
    }

    @Test
    fun `Given, wrap is called with a Fhir4, it returns a Resource, which has the TYPE FHIR4`() {
        // Given
        val dataResource = mockk<Fhir4Resource>()

        // When
        val resource: Any = ResourceFactory.wrap(dataResource)!!

        // Then
        assertTrue(resource is WrapperContract.Resource)
        assertEquals(
                WrapperContract.Resource.TYPE.FHIR4,
                (resource as WrapperContract.Resource).type
        )
    }
}
