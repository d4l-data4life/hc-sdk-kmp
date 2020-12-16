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

package care.data4life.sdk


import care.data4life.sdk.call.DataRecord
import care.data4life.sdk.call.Fhir4Record
import care.data4life.sdk.data.DataResource
import care.data4life.sdk.fhir.Fhir3Resource
import care.data4life.sdk.fhir.Fhir4Resource
import care.data4life.sdk.model.Record
import care.data4life.sdk.model.SdkRecordFactory
import care.data4life.sdk.model.definitions.BaseRecord
import care.data4life.sdk.network.DecryptedRecordBuilder
import care.data4life.sdk.network.model.EncryptedRecord
import care.data4life.sdk.network.model.NetworkRecordContract
import care.data4life.sdk.wrapper.ResourceFactory
import care.data4life.sdk.wrapper.ResourceHelper
import care.data4life.sdk.wrapper.WrapperContract
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.mockkObject
import io.mockk.unmockkConstructor
import io.mockk.unmockkObject
import io.mockk.verify
import io.reactivex.Single
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test

class RecordServiceUpdateRecordTest: RecordTestBase() {

    @Before
    fun setUp() {
        init()

        mockkObject(ResourceFactory)
        mockkObject(ResourceHelper)
        mockkObject(SdkRecordFactory)
        mockkConstructor(DecryptedRecordBuilder::class)
    }

    @After
    fun tearDown() {
        unmockkObject(ResourceFactory)
        unmockkObject(ResourceHelper)
        unmockkObject(SdkRecordFactory)
        unmockkConstructor(DecryptedRecordBuilder::class)
    }

    @Test
    fun `Given, updateRecord is called with a UserId, recordId, a DataResource and Annotations, it wraps it and delegates it to the generic createRecord and return its Result`() {
        // Given
        val userId = "id"
        val recordId = "absd"
        val rawResource = mockk<DataResource>()
        val annotations = mockk<List<String>>()

        val wrappedResource =  mockk<WrapperContract.Resource>()

        val createdRecord = mockk<DataRecord<DataResource>>()

        every { ResourceFactory.wrap(rawResource) } returns wrappedResource

        @Suppress("UNCHECKED_CAST")
        every {
            recordService.updateRecord(userId, recordId, wrappedResource, annotations)
        } returns Single.just(createdRecord as BaseRecord<Any>)

        // When
        val subscriber = recordService.updateRecord(
                userId,
                recordId,
                rawResource,
                annotations
        ).test().await()

        val record = subscriber
                .assertNoErrors()
                .assertComplete()
                .assertValueCount(1)
                .values()[0]

        // Then
        Assert.assertSame(
                record,
                createdRecord
        )

        verify(exactly = 1) { ResourceFactory.wrap(rawResource) }
    }

    @Test
    fun `Given, updateRecord is called with a UserId, recordId, a Fhir3Resource and Annotations, it wraps it and delegates it to the generic createRecord and return its Result`() {
        // Given
        val userId = "id"
        val recordId = "absd"
        val rawResource = mockk<Fhir3Resource>()
        val annotations = mockk<List<String>>()

        val wrappedResource =  mockk<WrapperContract.Resource>()

        val createdRecord = mockk<Record<Fhir3Resource>>()

        every { ResourceFactory.wrap(rawResource) } returns wrappedResource

        @Suppress("UNCHECKED_CAST")
        every {
            recordService.updateRecord(userId, recordId, wrappedResource, annotations)
        } returns Single.just(createdRecord as BaseRecord<Any>)

        // When
        val subscriber = recordService.updateRecord(
                userId,
                recordId,
                rawResource,
                annotations
        ).test().await()

        val record = subscriber
                .assertNoErrors()
                .assertComplete()
                .assertValueCount(1)
                .values()[0]

        // Then
        Assert.assertSame(
                record,
                createdRecord
        )

        verify(exactly = 1) { ResourceFactory.wrap(rawResource) }
    }

    @Test
    fun `Given, updateRecord is called with a UserId, recordId, a Fhir4Resource and Annotations, it wraps it and delegates it to the generic createRecord and return its Result`() {
        // Given
        val userId = "id"
        val recordId = "absd"
        val rawResource = mockk<Fhir4Resource>()
        val annotations = mockk<List<String>>()

        val wrappedResource =  mockk<WrapperContract.Resource>()

        val createdRecord = mockk<Fhir4Record<Fhir4Resource>>()

        every { ResourceFactory.wrap(rawResource) } returns wrappedResource

        @Suppress("UNCHECKED_CAST")
        every {
            recordService.updateRecord(userId, recordId, wrappedResource, annotations)
        } returns Single.just(createdRecord as BaseRecord<Any>)

        // When
        val subscriber = recordService.updateRecord(
                userId,
                recordId,
                rawResource,
                annotations
        ).test().await()

        val record = subscriber
                .assertNoErrors()
                .assertComplete()
                .assertValueCount(1)
                .values()[0]

        // Then
        Assert.assertSame(
                record,
                createdRecord
        )

        verify(exactly = 1) { ResourceFactory.wrap(rawResource) }
    }


    @Test
    fun `Given, generic updateRecord is called with a UserId, recordId, a WrappedResource and Annotations, it wraps it and delegates it to the generic createRecord and return its Result`() {
        // Given
        val recordId = "absd"
        val wrappedResource =  mockk<WrapperContract.Resource>()
        val annotations = mockk<List<String>>()
        val userId = "id"

        val data = mockk<HashMap<WrapperContract.Attachment, String?>>()
        val decryptedRecord = mockk<NetworkRecordContract.DecryptedRecord>()

        val encryptedRecord = mockk<EncryptedRecord>()

        val createdRecord = mockk<BaseRecord<Any>>()

        every { decryptedRecord.resource } returns wrappedResource
        every { wrappedResource.type } returns WrapperContract.Resource.TYPE.FHIR3

        every { attachmentService.checkDataRestrictions(wrappedResource) } returns mockk()
        every { attachmentService.extractUploadData(wrappedResource) } returns data

        every { apiService.fetchRecord(ALIAS, userId, recordId) } returns Single.just(encryptedRecord)
        every { recordCryptoService.decryptRecord(encryptedRecord, userId) } returns decryptedRecord
        every { attachmentClient.updateData(decryptedRecord, wrappedResource, userId) } returns decryptedRecord
        every { thumbnailService.cleanObsoleteAdditionalIdentifiers(wrappedResource) } returns mockk()
        every { decryptedRecord.resource = wrappedResource } returns mockk()
        every { decryptedRecord.annotations = annotations } returns mockk()
        every { attachmentClient.removeUploadData(decryptedRecord) } returns decryptedRecord
        every { recordCryptoService.encryptRecord(decryptedRecord) } returns encryptedRecord
        every { apiService.updateRecord(ALIAS, userId, recordId, encryptedRecord) } returns Single.just(encryptedRecord)
        every { recordCryptoService.decryptRecord(encryptedRecord, userId) } returns decryptedRecord
        every { attachmentClient.restoreUploadData(decryptedRecord, wrappedResource, data) } returns decryptedRecord
        every { ResourceHelper.assignResourceId(decryptedRecord) } returns decryptedRecord
        every { SdkRecordFactory.getInstance(decryptedRecord) } returns createdRecord

        // When
        val subscriber = recordService.updateRecord(
                userId,
                recordId,
                wrappedResource,
                annotations
        ).test().await()

        val record = subscriber
                .assertNoErrors()
                .assertComplete()
                .assertValueCount(1)
                .values()[0]

        // Then
        Assert.assertSame(
                record,
                createdRecord
        )
    }

    // ToDo UnhappyPath
    @Test
    fun `Given, updateRecords is called, with Resources and a userId, resolves the updates`() {
        // Given
        val resource = Fhir3Resource()
        val resources = mutableListOf(resource)
        val userId = "id"
        val recordId = "bal"

        val createdRecord = mockk<Record<Fhir3Resource>>()
        resource.id = recordId

        every {
            recordService.updateRecord(userId, recordId, resource, listOf())
        } returns Single.just(createdRecord)

        // When
        val observer = recordService.updateRecords(resources, userId).test().await()

        // Then
        val result = observer
                .assertNoErrors()
                .assertComplete()
                .assertValueCount(1)
                .values()[0]

        Assert.assertSame(
                result.successfulUpdates[0],
                createdRecord
        )
    }
}
