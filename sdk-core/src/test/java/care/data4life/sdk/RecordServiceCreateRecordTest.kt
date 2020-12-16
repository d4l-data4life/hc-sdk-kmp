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


import care.data4life.crypto.GCKey
import care.data4life.sdk.call.DataRecord
import care.data4life.sdk.call.Fhir4Record
import care.data4life.sdk.data.DataResource
import care.data4life.sdk.fhir.Fhir3Resource
import care.data4life.sdk.fhir.Fhir4Resource
import care.data4life.sdk.model.ModelVersion
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
import org.junit.Assert.assertSame
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import kotlin.collections.HashMap


class RecordServiceCreateRecordTest: RecordTestBase() {
    private lateinit var builder: NetworkRecordContract.Builder

    @Before
    fun setUp() {
        init()

        builder = mockk()

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
    fun `Given, createRecord is called with a DataResource, it wraps it and delegates it to the generic createRecord and return its Result`() {
        // Given
        val userId = "id"
        val rawResource = mockk<DataResource>()

        val wrappedResource =  mockk<WrapperContract.Resource>()

        val createdRecord = mockk<DataRecord<DataResource>>()

        every { ResourceFactory.wrap(rawResource) } returns wrappedResource

        @Suppress("UNCHECKED_CAST")
        every {
            recordService.createRecord(userId, wrappedResource, any())
        } returns Single.just(createdRecord as BaseRecord<Any>)

        // When
        val subscriber = recordService.createRecord(
                userId,
                rawResource,
                mockk(relaxed = true)
        ).test().await()

        val record = subscriber
                .assertNoErrors()
                .assertComplete()
                .assertValueCount(1)
                .values()[0]

        // Then
        assertSame(
                record,
                createdRecord
        )

        verify(exactly = 1) { ResourceFactory.wrap(rawResource) }
    }

    @Test
    fun `Given, createRecord is called with a Fhir3Resource, it wraps it and delegates it to the generic createRecord and return its Result`() {
        // Given
        val userId = "id"
        val rawResource = mockk<Fhir3Resource>()

        val wrappedResource =  mockk<WrapperContract.Resource>()

        val createdRecord = mockk<Record<Fhir3Resource>>()

        every { ResourceFactory.wrap(rawResource) } returns wrappedResource

        @Suppress("UNCHECKED_CAST")
        every {
            recordService.createRecord(userId, wrappedResource, any())
        } returns Single.just(createdRecord as BaseRecord<Any>)

        // When
        val subscriber = recordService.createRecord(
                userId,
                rawResource,
                mockk(relaxed = true)
        ).test().await()

        val record = subscriber
                .assertNoErrors()
                .assertComplete()
                .assertValueCount(1)
                .values()[0]

        // Then
        assertSame(
                record,
                createdRecord
        )

        verify(exactly = 1) { ResourceFactory.wrap(rawResource) }
    }

    @Test
    fun `Given, createRecord is called with a Fhir4Resource, it wraps it and delegates it to the generic createRecord and return its Result`() {
        // Given
        val userId = "id"
        val rawResource = mockk<Fhir4Resource>()

        val wrappedResource =  mockk<WrapperContract.Resource>()

        val createdRecord = mockk<Fhir4Record<Fhir4Resource>>()

        every { ResourceFactory.wrap(rawResource) } returns wrappedResource

        @Suppress("UNCHECKED_CAST")
        every {
            recordService.createRecord(userId, wrappedResource, any())
        } returns Single.just(createdRecord as BaseRecord<Any>)

        // When
        val subscriber = recordService.createRecord(
                userId,
                rawResource,
                mockk(relaxed = true)
        ).test().await()

        val record = subscriber
                .assertNoErrors()
                .assertComplete()
                .assertValueCount(1)
                .values()[0]

        // Then
        assertSame(
                record,
                createdRecord
        )

        verify(exactly = 1) { ResourceFactory.wrap(rawResource) }
    }

    @Test
    fun `Given, createRecord is called with a wrapped Resource, it return a BaseRecord`() {
        // Given
        val userId = "id"
        val rawResource = mockk<Fhir4Resource>()

        val wrappedResource =  mockk<WrapperContract.Resource>()

        val createdRecord = mockk<Fhir4Record<Fhir4Resource>>()

        every { ResourceFactory.wrap(rawResource) } returns wrappedResource

        @Suppress("UNCHECKED_CAST")
        every {
            recordService.createRecord(userId, wrappedResource, any())
        } returns Single.just(createdRecord as BaseRecord<Any>)

        // When
        val subscriber = recordService.createRecord(
                userId,
                rawResource,
                mockk(relaxed = true)
        ).test().await()

        val record = subscriber
                .assertNoErrors()
                .assertComplete()
                .assertValueCount(1)
                .values()[0]

        // Then
        assertSame(
                record,
                createdRecord
        )

        verify(exactly = 1) { ResourceFactory.wrap(rawResource) }
    }


    @Test
    fun `Given, createRecord is called with a DataResource and a UserId, it returns a new Record`() {
        // Given
        val wrappedResource =  mockk<WrapperContract.Resource>()
        val annotations = mockk<List<String>>()
        val userId = "id"

        val data = mockk<HashMap<WrapperContract.Attachment, String?>>()
        val decryptedRecord = mockk<NetworkRecordContract.DecryptedRecord>()
        val tags = mockk<HashMap<String, String>>()
        val dataKey = mockk<GCKey>()

        val encryptedRecord = mockk<EncryptedRecord>()

        val createdRecord = mockk<BaseRecord<Any>>()


        every { wrappedResource.type } returns WrapperContract.Resource.TYPE.DATA

        every { attachmentService.checkDataRestrictions(wrappedResource) } returns mockk()
        every { attachmentService.extractUploadData(wrappedResource) } returns data

        every { taggingService.appendDefaultTags( null, null ) } returns tags

        every { cryptoService.generateGCKey() } returns Single.just(dataKey)

        every {
            anyConstructed<DecryptedRecordBuilder>().setAnnotations(annotations)
        } returns builder
        every { builder.build(
                wrappedResource,
                tags,
                any(),//FIXME
                dataKey,
                ModelVersion.CURRENT
        ) } returns decryptedRecord

        every { attachmentClient.uploadData(decryptedRecord, userId) } returns decryptedRecord
        every { attachmentClient.removeUploadData(decryptedRecord) } returns decryptedRecord
        every { recordCryptoService.encryptRecord(decryptedRecord) } returns encryptedRecord
        every { apiService.createRecord(ALIAS, userId, encryptedRecord) } returns Single.just(encryptedRecord)
        every { recordCryptoService.decryptRecord(encryptedRecord, userId) } returns decryptedRecord
        every { attachmentClient.restoreUploadData(decryptedRecord, wrappedResource, data) } returns decryptedRecord
        every { ResourceHelper.assignResourceId(decryptedRecord) } returns decryptedRecord
        every { SdkRecordFactory.getInstance(decryptedRecord) } returns createdRecord

        // When
        val subscriber = recordService.createRecord(
                userId,
                wrappedResource,
                annotations
        ).test().await()

        val record = subscriber
                .assertNoErrors()
                .assertComplete()
                .assertValueCount(1)
                .values()[0]

        // Then
        assertSame(
                record,
                createdRecord
        )
    }

    // ToDo: Remove this when the switch is in the tagger
    @Ignore //This has currently problems with mockking fhir
    @Test
    fun `Given, createRecord is called with non DataResource and a UserId, it returns a new Record`() {
        // Given
        val wrappedResource =  mockk<WrapperContract.Resource>()
        val rawResource = mockk<Fhir3Resource>()
        val annotations = mockk<List<String>>()
        val userId = "id"
        val resourceType = "type"

        val data = mockk<HashMap<WrapperContract.Attachment, String?>>()
        val decryptedRecord = mockk<NetworkRecordContract.DecryptedRecord>()
        val tags = mockk<HashMap<String, String>>()
        val dataKey = mockk<GCKey>()

        val encryptedRecord = mockk<EncryptedRecord>()

        val createdRecord = mockk<BaseRecord<Any>>()


        every { wrappedResource.type } returns WrapperContract.Resource.TYPE.FHIR3
        every { wrappedResource.unwrap() } returns rawResource

        every { rawResource.resourceType } returns resourceType

        every { attachmentService.checkDataRestrictions(wrappedResource) } returns mockk()
        every { attachmentService.extractUploadData(wrappedResource) } returns data

        every { taggingService.appendDefaultTags( resourceType, null ) } returns tags

        every { cryptoService.generateGCKey() } returns Single.just(dataKey)

        every {
            anyConstructed<DecryptedRecordBuilder>().setAnnotations(annotations)
        } returns builder
        every { builder.build(
                wrappedResource,
                tags,
                any(),//FIXME
                dataKey,
                ModelVersion.CURRENT
        ) } returns decryptedRecord

        every { attachmentClient.uploadData(decryptedRecord, userId) } returns decryptedRecord
        every { attachmentClient.removeUploadData(decryptedRecord) } returns decryptedRecord
        every { recordCryptoService.encryptRecord(decryptedRecord) } returns encryptedRecord
        every { apiService.createRecord(ALIAS, userId, encryptedRecord) } returns Single.just(encryptedRecord)
        every { recordCryptoService.decryptRecord(encryptedRecord, userId) } returns decryptedRecord
        every { attachmentClient.restoreUploadData(decryptedRecord, wrappedResource, data) } returns decryptedRecord
        every { ResourceHelper.assignResourceId(decryptedRecord) } returns decryptedRecord
        every { SdkRecordFactory.getInstance(decryptedRecord) } returns createdRecord

        // When
        val subscriber = recordService.createRecord(
                userId,
                wrappedResource,
                annotations
        ).test().await()

        val record = subscriber
                .assertNoErrors()
                .assertComplete()
                .assertValueCount(1)
                .values()[0]

        // Then
        assertSame(
                record,
                createdRecord
        )
    }

    //Todo: Add test for unhappy path
    @Test
    fun `Given, createRecords is called with a list of Fhir3Resources and a UserId, it returns a List of Records`() {
        // Given
        val wrappedResource =  mockk<Fhir3Resource>()
        val resources = mutableListOf(wrappedResource)
        val userId = "id"
        val resourceType = "type"

        val createdRecord = mockk<Record<Fhir3Resource>>()
        every { recordService.createRecord(userId, wrappedResource, listOf()) } returns Single.just(createdRecord)

        // When
        val observer = recordService.createRecords(resources, userId).test().await()

        // Then
        val result = observer
                .assertNoErrors()
                .assertComplete()
                .assertValueCount(1)
                .values()[0]

        assertSame(
                result.successfulOperations[0],
                createdRecord
        )
    }
}
