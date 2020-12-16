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


import care.data4life.sdk.network.model.NetworkRecordContract
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class ResourceHelperTest {

    @Test
    fun `it is a ResourceHelper`() {
        assertTrue((ResourceHelper as Any) is HelperContract.ResourceHelper)
    }

    @Test
    fun `Given, assignResourceId is called with a DecryptedRecord, which contains a DataRecord, it reflects the Record`() {
        // Given
        val record = mockk<NetworkRecordContract.DecryptedRecord>()
        val recordId = "Id"
        val wrappedResource = mockk<WrapperContract.Resource>(relaxed = true)

        every { record.identifier } returns recordId
        every { record.resource } returns wrappedResource
        every { wrappedResource.type } returns WrapperContract.Resource.TYPE.DATA

        // When
        val result = ResourceHelper.assignResourceId(record)

        // Then
        assertSame(
                record,
                result
        )

        verify(exactly = 0) { wrappedResource.identifier = any() }
    }
    
    @Test
    fun `Given, assignResourceId is called with a DecryptedRecord, which contains a Fhir3Resource, it reassigns its ResourceId`() {
        // Given
        val record = mockk<NetworkRecordContract.DecryptedRecord>()
        val recordId = "Id"
        val wrappedResource = mockk<WrapperContract.Resource>(relaxed = true)

        every { record.identifier } returns recordId
        every { record.resource } returns wrappedResource
        every { wrappedResource.type } returns WrapperContract.Resource.TYPE.FHIR3

        // When
        val result = ResourceHelper.assignResourceId(record)

        // Then
        assertSame(
                record,
                result
        )

        verify(exactly = 1) { wrappedResource.identifier = any() }
    }

    @Test
    fun `Given, assignResourceId is called with a DecryptedRecord, which contains a Fhir4Resource, it reassigns its ResourceId`() {
        // Given
        val record = mockk<NetworkRecordContract.DecryptedRecord>()
        val recordId = "Id"
        val wrappedResource = mockk<WrapperContract.Resource>(relaxed = true)

        every { record.identifier } returns recordId
        every { record.resource } returns wrappedResource
        every { wrappedResource.type } returns WrapperContract.Resource.TYPE.FHIR4

        // When
        val result = ResourceHelper.assignResourceId(record)

        // Then
        assertSame(
                record,
                result
        )

        verify(exactly = 1) { wrappedResource.identifier = any() }
    }
}
