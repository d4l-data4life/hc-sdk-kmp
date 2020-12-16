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
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.mockkObject
import io.mockk.unmockkConstructor
import io.mockk.unmockkObject
import io.mockk.verify
import io.reactivex.Observable
import io.reactivex.Single
import org.junit.After
import org.junit.Assert
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.threeten.bp.LocalDate

class RecordServiceFetchRecordsTest: RecordTestBase() {
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
    fun `Given, fetchDataRecord is called with UserId and a RecordId for a DataRecord, it to the generic fetchRecord and returns its Result`() {
        // Given
        val userId = "id"
        val recordId = "123"

        val createdRecord = mockk<DataRecord<DataResource>>()

        @Suppress("UNCHECKED_CAST")
        every {
            recordService._fetchRecord(recordId, userId)
        } returns Single.just(createdRecord as BaseRecord<Any>)

        // When
        val subscriber = recordService.fetchDataRecord(
                userId,
                recordId
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

    @Test
    fun `Given, fetchFhir3Record is called with UserId and a RecordId for a Fhir3Resources, it to the generic fetchRecord and return its Result`() {
        // Given
        val userId = "id"
        val recordId = "123"

        val createdRecord = mockk<Record<Fhir3Resource>>()

        @Suppress("UNCHECKED_CAST")
        every {
            recordService._fetchRecord(recordId, userId)
        } returns Single.just(createdRecord as BaseRecord<Any>)

        // When
        val subscriber = recordService.fetchFhir3Record<Fhir3Resource>(
                userId,
                recordId
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

    @Test
    fun `Given, fetchFhir4Record is called with UserId and a RecordId, it to the generic fetchRecord and return its Result`() {
        // Given
        val userId = "id"
        val recordId = "123"

        val createdRecord = mockk<Fhir4Record<Fhir4Resource>>()

        @Suppress("UNCHECKED_CAST")
        every {
            recordService._fetchRecord(recordId, userId)
        } returns Single.just(createdRecord as BaseRecord<Any>)

        // When
        val subscriber = recordService.fetchFhir4Record<Fhir4Resource>(
                userId,
                recordId
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

    @Test
    fun `Given, generic fetchRecords is called with a UserId and a RecordId, it retruns a new Record`() {
        // Given
        val userId = "id"
        val recordId = "123"

        val encryptedRecord = mockk<EncryptedRecord>()
        val decryptedRecord = mockk<NetworkRecordContract.DecryptedRecord>()

        val createdRecord = mockk<BaseRecord<Any>>()

        every { apiService.fetchRecord(ALIAS, userId, recordId) } returns Single.just(encryptedRecord)
        every { recordCryptoService.decryptRecord(encryptedRecord, userId) }  returns decryptedRecord
        every { ResourceHelper.assignResourceId(decryptedRecord) } returns decryptedRecord
        every { SdkRecordFactory.getInstance(decryptedRecord) } returns createdRecord

        // When
        val subscriber = recordService._fetchRecord(recordId, userId).test().await()

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

    // ToDo: unhappy path
    @Test
    fun `Given, fetchFhir3Records is called with RecordIds and a UserId, it returns the fetched Results`() {
        // Given
        val userId = "id"
        val recordIds = listOf("abc")

        val createdRecord = mockk<Record<Fhir3Resource>>()
        every {
            recordService.fetchFhir3Record<Fhir3Resource>(userId, "abc")
        } returns Single.just(createdRecord)

        // When
        val observer = recordService.fetchFhir3Records<Fhir3Resource>(recordIds, userId).test().await()

        // Then
        val result = observer
                .assertNoErrors()
                .assertComplete()
                .assertValueCount(1)
                .values()[0]

        Assert.assertSame(
                result.successfulFetches[0],
                createdRecord
        )
    }

    @Test
    fun `Given, fetchDataRecords is called with a UserId, Annotations, a StartDate, a EndDate, the PageSize and Offset, it to the generic fetchRecord and returns its Result`() {
        // Given
        val userId = "abc"
        val annotations = mockk<List<String>>()
        val startDate = mockk<LocalDate>()
        val endDate = mockk<LocalDate>()
        val pageSize = 123
        val offset = 2

        val fetchedRecords = listOf(mockk<DataRecord<DataResource>>())

        @Suppress("UNCHECKED_CAST")
        every {
            recordService._fetchRecords(
                    userId,
                    ByteArray::class.java as Class<Any>,
                    annotations,
                    startDate,
                    endDate,
                    pageSize,
                    offset
            )
        } returns Single.just(fetchedRecords as List<BaseRecord<Any>>)

        // When
        val observer = recordService.fetchDataRecords(
                userId,
                annotations,
                startDate,
                endDate,
                pageSize,
                offset
        ).test().await()

        val record = observer
                .assertNoErrors()
                .assertComplete()
                .assertValueCount(1)
                .values()[0]

        // Then
        Assert.assertSame(
                record,
                fetchedRecords
        )
    }

    @Test
    fun `Given, fetchFhir3Records is called with a UserId, a ResourceType, Annotations, a StartDate, a EndDate, the PageSize and Offset, it to the generic fetchRecord and returns its Result`() {
        // Given
        val userId = "abc"
        val klass = Fhir3Resource::class.java
        val annotations = mockk<List<String>>()
        val startDate = mockk<LocalDate>()
        val endDate = mockk<LocalDate>()
        val pageSize = 123
        val offset = 2

        val fetchedRecords = listOf(mockk<Record<Fhir3Resource>>())

        @Suppress("UNCHECKED_CAST")
        every {
            recordService._fetchRecords(
                    userId,
                    klass as Class<Any>,
                    annotations,
                    startDate,
                    endDate,
                    pageSize,
                    offset
            )
        } returns Single.just(fetchedRecords as List<BaseRecord<Any>>)

        // When
        val observer = recordService.fetchFhir3Records(
                userId,
                klass,
                annotations,
                startDate,
                endDate,
                pageSize,
                offset
        ).test().await()

        val record = observer
                .assertNoErrors()
                .assertComplete()
                .assertValueCount(1)
                .values()[0]

        // Then
        Assert.assertSame(
                record,
                fetchedRecords
        )
    }

    @Test
    fun `Given, fetchFhir4Records is called with a UserId, a ResourceType, Annotations, a StartDate, a EndDate, the PageSize and Offset, it to the generic fetchRecord and returns its Result`() {
        // Given
        val userId = "abc"
        val klass = Fhir4Resource::class.java
        val annotations = mockk<List<String>>()
        val startDate = mockk<LocalDate>()
        val endDate = mockk<LocalDate>()
        val pageSize = 123
        val offset = 2

        val fetchedRecords = listOf(mockk<Fhir4Record<Fhir4Resource>>())

        @Suppress("UNCHECKED_CAST")
        every {
            recordService._fetchRecords(
                    userId,
                    klass as Class<Any>,
                    annotations,
                    startDate,
                    endDate,
                    pageSize,
                    offset
            )
        } returns Single.just(fetchedRecords as List<BaseRecord<Any>>)

        // When
        val observer = recordService.fetchFhir4Records(
                userId,
                klass,
                annotations,
                startDate,
                endDate,
                pageSize,
                offset
        ).test().await()

        val record = observer
                .assertNoErrors()
                .assertComplete()
                .assertValueCount(1)
                .values()[0]

        // Then
        Assert.assertSame(
                record,
                fetchedRecords
        )
    }

    // ToDo: Alternative branches: Fhir*, Date
    @Test
    fun `Given, generic fetchRecords is called with a UserId, a ResourceType, Annotations, a StartDate, a EndDate, the PageSize and Offset, it to the generic fetchRecord and returns its Result`() {
        // Given
        val userId = "abc"
        val klass = ByteArray::class.java
        val annotations = mockk<List<String>>()
        val pageSize = 123
        val offset = 2

        val tags = hashMapOf<String, String>()

        val encryptedRecords = mutableListOf(mockk<EncryptedRecord>())
        val decryptedRecord = mockk<NetworkRecordContract.DecryptedRecord>()
        val createdRecord = mockk<BaseRecord<Any>>()


        every { taggingService.appendAppDataTags(hashMapOf()) } returns tags
        every { tagEncryptionService.encryptTags(tags) as MutableList<String> } returns mockk(relaxed = true) //FIXME: This should be a list
        every { tagEncryptionService.encryptAnnotations(any()) } returns annotations
        every { apiService.fetchRecords(
                ALIAS,
                userId,
                null,
                null,
                pageSize,
                offset,
                any()
        ) } returns Observable.fromArray(encryptedRecords)

        every { recordCryptoService.decryptRecord(encryptedRecords[0], userId) } returns decryptedRecord
        every { ResourceHelper.assignResourceId(decryptedRecord) } returns decryptedRecord
        every { SdkRecordFactory.getInstance(decryptedRecord) } returns createdRecord

        // When
        @Suppress("UNCHECKED_CAST")
        val observer = recordService._fetchRecords(
                userId,
                klass as Class<Any>,
                annotations,
                null,
                null,
                pageSize,
                offset
        ).test().await()

        val record = observer
                .assertNoErrors()
                .assertComplete()
                .assertValueCount(1)
                .values()[0]

        // Then
        assertEquals(
                record,
                listOf(createdRecord)
        )

        verify(exactly = 1) { SdkRecordFactory.getInstance(decryptedRecord) }
    }
}
