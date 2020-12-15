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

package care.data4life.sdk.attachment

import care.data4life.sdk.fhir.Fhir3Resource
import care.data4life.sdk.network.model.NetworkRecordContract
import care.data4life.sdk.wrapper.FhirAttachmentHelper
import care.data4life.sdk.wrapper.WrapperContract
import com.google.common.truth.Truth
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AttachmentClientRemoveAndRestoreTest {
    private lateinit var fhirAttachmentHelper: FhirAttachmentHelper

    private lateinit var attachmentClient: AttachmentContract.Client

    @Before
    fun setUp() {
        fhirAttachmentHelper = mockk()

        attachmentClient = AttachmentClient(
                fhirAttachmentHelper,
                mockk(),
                mockk(),
                mockk(),
                mockk()
        )
    }

    @Test
    fun `it is a AttachmentClient`() {
        val client: Any = AttachmentClient(
                fhirAttachmentHelper,
                mockk(),
                mockk(),
                mockk(),
                mockk()
        )

        assertTrue(client is AttachmentContract.Client)
    }

    @Test
    fun `Given, removeUploadData is called with a DecryptedRecord, which does contains a DataResource, it reflects the given record`() {
        // Given
        val resource = mockk<WrapperContract.Resource>()
        val decryptedRecord = mockk<NetworkRecordContract.DecryptedRecord<WrapperContract.Resource>>()

        every { resource.type } returns WrapperContract.Resource.TYPE.DATA
        every { decryptedRecord.resource } returns resource
        every { fhirAttachmentHelper.getAttachment(any()) } returns mockk()
        every { fhirAttachmentHelper.updateAttachmentData(any(), null) } returns Unit

        // When
        val record = attachmentClient.removeUploadData(decryptedRecord)

        // Then
        Truth.assertThat(record).isSameInstanceAs(decryptedRecord)

        verify(exactly = 0) { fhirAttachmentHelper.getAttachment(any()) }
        verify(exactly = 0) { fhirAttachmentHelper.updateAttachmentData(any(), null) }
    }


    @Test
    fun `Given, removeUploadData is called with a DecryptedRecord which contain no DataResource, it removes the existing Attachments`() {
        // Given
        val resource = mockk<WrapperContract.Resource>()
        val wrappedResource = mockk<Fhir3Resource>()
        val decryptedRecord = mockk<NetworkRecordContract.DecryptedRecord<WrapperContract.Resource>>()
        val attachments = mutableListOf<WrapperContract.Attachment>(
                mockk(),
                mockk()
        )

        every { resource.unwrap() } returns wrappedResource
        every { resource.type } returns WrapperContract.Resource.TYPE.FHIR3
        every { decryptedRecord.resource } returns resource
        @Suppress("UNCHECKED_CAST")
        every { fhirAttachmentHelper.getAttachment(wrappedResource) } returns (attachments as MutableList<Any>)
        every { fhirAttachmentHelper.updateAttachmentData(wrappedResource, null) } returns Unit


        // When
        val record = attachmentClient.removeUploadData(decryptedRecord)

        // Then
        Truth.assertThat(record).isSameInstanceAs(decryptedRecord)

        verify(exactly = 1) { fhirAttachmentHelper.getAttachment(wrappedResource) }
        verify(exactly = 1) { fhirAttachmentHelper.updateAttachmentData(wrappedResource, null) }
    }

    @Test
    fun `Given, removeUploadData is called with a DecryptedRecord, which does not contain a DataResource, it does nothing, if no Attachments exists`() {
        // Given
        val resource = mockk<WrapperContract.Resource>()
        val wrappedResource = mockk<Fhir3Resource>()
        val decryptedRecord = mockk<NetworkRecordContract.DecryptedRecord<WrapperContract.Resource>>()

        every { resource.unwrap() } returns wrappedResource
        every { resource.type } returns WrapperContract.Resource.TYPE.FHIR3
        every { decryptedRecord.resource } returns resource
        @Suppress("UNCHECKED_CAST")
        every { fhirAttachmentHelper.getAttachment(wrappedResource) } returns mutableListOf()
        every { fhirAttachmentHelper.updateAttachmentData(wrappedResource, null) } returns Unit

        // When
        val record = attachmentClient.removeUploadData(decryptedRecord)

        // Then
        Truth.assertThat(record).isSameInstanceAs(decryptedRecord)

        verify(exactly = 1) { fhirAttachmentHelper.getAttachment(wrappedResource) }
        verify(exactly = 0) { fhirAttachmentHelper.updateAttachmentData(wrappedResource, null) }
    }

    @Test
    fun `Given, restoreUploadData is called with DecryptedRecord, which contains a DataResource, a non DataResource and Attachment, it just reflects the given Record`() {
        val decryptedRecord = mockk<NetworkRecordContract.DecryptedRecord<WrapperContract.Resource>>()

        every { decryptedRecord.resource.type } returns WrapperContract.Resource.TYPE.DATA
        every { fhirAttachmentHelper.getAttachment(any()) } returns mockk()
        every { fhirAttachmentHelper.updateAttachmentData(any(), any()) } returns Unit

        // When
        @Suppress("UNCHECKED_CAST")
        val record = attachmentClient.restoreUploadData(
                decryptedRecord,
                mockk(),
                mockk()
        )

        // Then
        Truth.assertThat(record).isSameInstanceAs(decryptedRecord)

        verify(exactly = 0) { decryptedRecord.resource = any() }
        verify(exactly = 0) { fhirAttachmentHelper.getAttachment(any()) }
        verify(exactly = 0) { fhirAttachmentHelper.updateAttachmentData(any(), any()) }
    }

    @Test
    fun `Given, restoreUploadData is called with a DecryptedRecord, which contains a non DataResource, a DataResource and Attachments, it just reflects the given Record`() {
        val resource = mockk<WrapperContract.Resource>()
        val orgResource = mockk<WrapperContract.Resource>()

        val decryptedRecord = mockk<NetworkRecordContract.DecryptedRecord<WrapperContract.Resource>>()

        every { resource.type } returns WrapperContract.Resource.TYPE.FHIR3
        every { orgResource.type } returns WrapperContract.Resource.TYPE.DATA

        every { decryptedRecord.resource } returns resource
        every { fhirAttachmentHelper.getAttachment(any()) } returns mockk()
        every { fhirAttachmentHelper.updateAttachmentData(any(), any()) } returns Unit

        // When
        @Suppress("UNCHECKED_CAST")
        val record = attachmentClient.restoreUploadData(
                decryptedRecord,
                orgResource,
                mockk()
        )

        // Then
        Truth.assertThat(record).isSameInstanceAs(decryptedRecord)


        verify(exactly = 0) { decryptedRecord.resource = any() }
        verify(exactly = 0) { fhirAttachmentHelper.getAttachment(any()) }
        verify(exactly = 0) { fhirAttachmentHelper.updateAttachmentData(any(), any()) }
    }

    @Test
    fun `Given, restoreUploadData is called with a DecryptedRecord, which contains a non DataResource, null as a Resource and Attachment, it just reflects the given Record`() {
        val resource = mockk<WrapperContract.Resource>()

        val decryptedRecord = mockk<NetworkRecordContract.DecryptedRecord<WrapperContract.Resource>>()

        every { resource.type } returns WrapperContract.Resource.TYPE.FHIR3

        every { decryptedRecord.resource } returns resource
        every { fhirAttachmentHelper.getAttachment(any()) } returns mockk()
        every { fhirAttachmentHelper.updateAttachmentData(any(), any()) } returns Unit

        // When
        @Suppress("UNCHECKED_CAST")
        val record = attachmentClient.restoreUploadData(
                decryptedRecord,
                null,
                mockk()
        )

        // Then
        Truth.assertThat(record).isSameInstanceAs(decryptedRecord)


        verify(exactly = 0) { decryptedRecord.resource = any() }
        verify(exactly = 0) { fhirAttachmentHelper.getAttachment(any()) }
        verify(exactly = 0) { fhirAttachmentHelper.updateAttachmentData(any(), any()) }
    }

    @Test
    fun `Given, restoreUploadData is called with a DecryptedRecord, a non DataResource and Attachment, it sets the given Resource to the DecryptedRecord`() {
        val resource = mockk<WrapperContract.Resource>()
        val orgResource = mockk<WrapperContract.Resource>()

        val key1 = mockk<WrapperContract.Attachment>()
        val rawKey1 = mockk<Any>()
        val key2 = mockk<WrapperContract.Attachment>()
        val rawKey2 = mockk<Any>()

        val attachments = hashMapOf<WrapperContract.Attachment, String?>(
                key1 to "21",
                key2 to "42"
        )

        val decryptedRecord = mockk<NetworkRecordContract.DecryptedRecord<WrapperContract.Resource>>(relaxed = true)

        every { resource.type } returns WrapperContract.Resource.TYPE.FHIR3
        every { resource.unwrap() } returns mockk<Fhir3Resource>()
        every { orgResource.type } returns WrapperContract.Resource.TYPE.FHIR3

        every {  key1.unwrap() } returns rawKey1
        every {  key2.unwrap() } returns rawKey2

        every { decryptedRecord.resource } returns resource
        every { fhirAttachmentHelper.getAttachment(any()) } returns mutableListOf()
        every { fhirAttachmentHelper.updateAttachmentData(any(), any()) } returns Unit

        // When
        @Suppress("UNCHECKED_CAST")
        val record = attachmentClient.restoreUploadData(
                decryptedRecord,
                orgResource,
                attachments
        )

        // Then
        Truth.assertThat(record).isSameInstanceAs(decryptedRecord)


        verify(exactly = 1) { decryptedRecord.resource = orgResource }
    }

    @Test
    fun `Given, restoreUploadData is called with a DecryptedRecord, a non DataResource and Attachment, it removes the existing Attachments`() {
        val wrappedResource = mockk<Fhir3Resource>()
        val resource = mockk<WrapperContract.Resource>()
        val orgResource = mockk<WrapperContract.Resource>()

        val key1 = mockk<WrapperContract.Attachment>()
        val rawKey1 = mockk<Any>()
        val key2 = mockk<WrapperContract.Attachment>()
        val rawKey2 = mockk<Any>()

        val serviceAttachments = mutableListOf<Any>("any")

        val attachments = hashMapOf<WrapperContract.Attachment, String?>(
                key1 to "21",
                key2 to "42"
        )

        val rawAttachment = hashMapOf<Any, String?>(
                rawKey1 to "21",
                rawKey2 to "42"
        )

        val decryptedRecord = mockk<NetworkRecordContract.DecryptedRecord<WrapperContract.Resource>>(relaxed = true)

        every { resource.type } returns WrapperContract.Resource.TYPE.FHIR3
        every { resource.unwrap() } returns wrappedResource
        every { decryptedRecord.resource } returns resource
        every { orgResource.type } returns WrapperContract.Resource.TYPE.FHIR3

        every {  key1.unwrap() } returns rawKey1
        every {  key2.unwrap() } returns rawKey2

        every { decryptedRecord.resource } returns resource
        every { fhirAttachmentHelper.getAttachment(wrappedResource) } returns serviceAttachments
        every { fhirAttachmentHelper.updateAttachmentData(wrappedResource, rawAttachment) } returns Unit

        // When
        @Suppress("UNCHECKED_CAST")
        val record = attachmentClient.restoreUploadData(
                decryptedRecord,
                orgResource,
                attachments
        )

        // Then
        Truth.assertThat(record).isSameInstanceAs(decryptedRecord)

        verify(exactly = 1) { decryptedRecord.resource = orgResource }
        verify(exactly = 1) { fhirAttachmentHelper.getAttachment(wrappedResource) }
        verify(exactly = 1) { fhirAttachmentHelper.updateAttachmentData(wrappedResource, rawAttachment) }
    }


    @Test
    fun `Given, restoreUploadData is called with a DecryptedRecord, a non DataResource and Attachment, it does nothing, if no Attachments exists`() {
        val wrappedResource = mockk<Fhir3Resource>()
        val resource = mockk<WrapperContract.Resource>()
        val orgResource = mockk<WrapperContract.Resource>()

        val key1 = mockk<WrapperContract.Attachment>()
        val rawKey1 = mockk<Any>()
        val key2 = mockk<WrapperContract.Attachment>()
        val rawKey2 = mockk<Any>()

        val attachments = hashMapOf<WrapperContract.Attachment, String?>(
                key1 to "21",
                key2 to "42"
        )

        val rawAttachment = hashMapOf<Any, String?>(
                rawKey1 to "21",
                rawKey2 to "42"
        )

        val decryptedRecord = mockk<NetworkRecordContract.DecryptedRecord<WrapperContract.Resource>>(relaxed = true)

        every { resource.type } returns WrapperContract.Resource.TYPE.FHIR3
        every { resource.unwrap() } returns wrappedResource
        every { decryptedRecord.resource } returns resource
        every { orgResource.type } returns WrapperContract.Resource.TYPE.FHIR3

        every {  key1.unwrap() } returns rawKey1
        every {  key2.unwrap() } returns rawKey2

        every { decryptedRecord.resource } returns resource
        every { fhirAttachmentHelper.getAttachment(wrappedResource) } returns mutableListOf()
        every { fhirAttachmentHelper.updateAttachmentData(wrappedResource, rawAttachment) } returns Unit

        // When
        @Suppress("UNCHECKED_CAST")
        val record = attachmentClient.restoreUploadData(
                decryptedRecord,
                orgResource,
                attachments
        )

        // Then
        Truth.assertThat(record).isSameInstanceAs(decryptedRecord)

        verify(exactly = 1) { decryptedRecord.resource = orgResource }
        verify(exactly = 1) { fhirAttachmentHelper.getAttachment(wrappedResource) }
        verify(exactly = 0) { fhirAttachmentHelper.updateAttachmentData(wrappedResource, rawAttachment) }
    }



    @Test
    fun `Given, restoreUploadData is called with a DecryptedRecord, a non DataResource and null as Attachment, it reflects the record without invoking more actions`() {
        val wrappedResource = mockk<Fhir3Resource>()
        val resource = mockk<WrapperContract.Resource>()
        val orgResource = mockk<WrapperContract.Resource>()

        val key1 = mockk<WrapperContract.Attachment>()
        val rawKey1 = mockk<Any>()
        val key2 = mockk<WrapperContract.Attachment>()
        val rawKey2 = mockk<Any>()

        val rawAttachment = hashMapOf<Any, String?>(
                rawKey1 to "21",
                rawKey2 to "42"
        )

        val decryptedRecord = mockk<NetworkRecordContract.DecryptedRecord<WrapperContract.Resource>>(relaxed = true)

        every { resource.type } returns WrapperContract.Resource.TYPE.FHIR3
        every { resource.unwrap() } returns wrappedResource
        every { decryptedRecord.resource } returns resource
        every { orgResource.type } returns WrapperContract.Resource.TYPE.FHIR3

        every {  key1.unwrap() } returns rawKey1
        every {  key2.unwrap() } returns rawKey2

        every { decryptedRecord.resource } returns resource
        every { fhirAttachmentHelper.getAttachment(wrappedResource) } returns mutableListOf()
        every { fhirAttachmentHelper.updateAttachmentData(wrappedResource, rawAttachment) } returns Unit

        // When
        @Suppress("UNCHECKED_CAST")
        val record = attachmentClient.restoreUploadData(
                decryptedRecord,
                orgResource,
                null
        )

        // Then
        Truth.assertThat(record).isSameInstanceAs(decryptedRecord)

        verify(exactly = 1) { decryptedRecord.resource = orgResource }
        verify(exactly = 0) { fhirAttachmentHelper.getAttachment(wrappedResource) }
        verify(exactly = 0) { fhirAttachmentHelper.updateAttachmentData(wrappedResource, rawAttachment) }
    }
}
