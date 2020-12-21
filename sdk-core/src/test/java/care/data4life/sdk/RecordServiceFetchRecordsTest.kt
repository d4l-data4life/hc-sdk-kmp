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

import care.data4life.fhir.stu3.model.CarePlan
import care.data4life.fhir.stu3.model.DomainResource
import care.data4life.sdk.call.DataRecord
import care.data4life.sdk.call.Fhir4Record
import care.data4life.sdk.data.DataResource
import care.data4life.sdk.fhir.Fhir3Resource
import care.data4life.sdk.fhir.Fhir4Resource
import care.data4life.sdk.lang.DataValidationException
import care.data4life.sdk.model.Record
import care.data4life.sdk.model.RecordMapper
import care.data4life.sdk.model.definitions.BaseRecord
import com.google.common.truth.Truth
import io.mockk.every
import io.mockk.mockk
import io.reactivex.Observable
import io.reactivex.Single
import org.junit.After
import org.junit.Assert.assertSame
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers
import org.mockito.Mockito
import org.threeten.bp.LocalDate
import java.io.IOException

class RecordServiceFetchRecordsTest : RecordServiceTestBase() {
    @Before
    fun setUp() {
        init()
    }

    @After
    fun tearDown() {
        stop()
    }

    @Test
    @Throws(
            InterruptedException::class,
            IOException::class,
            DataValidationException.ModelVersionNotSupported::class
    )
    fun `Given, fetchRecord is called with a RecordId and UserId, it returns a Record`() {
        // Given
        Mockito.`when`(mockApiService.fetchRecord(ALIAS, USER_ID, RECORD_ID))
                .thenReturn(Single.just(mockEncryptedRecord))
        Mockito.doReturn(mockDecryptedFhir3Record)
                .`when`(recordService)
                .decryptRecord<DomainResource>(mockEncryptedRecord, USER_ID)
        @Suppress("UNCHECKED_CAST")
        every { RecordMapper.getInstance(mockDecryptedFhir3Record) } returns mockRecord as BaseRecord<DomainResource>

        // When
        val observer = recordService.fetchFhir3Record<CarePlan>(USER_ID, RECORD_ID).test().await()

        // Then
        val record = observer
                .assertNoErrors()
                .assertComplete()
                .assertValueCount(1)
                .values()[0]

        Truth.assertThat(record).isSameInstanceAs(mockRecord)

        inOrder.verify(mockApiService).fetchRecord(ALIAS, USER_ID, RECORD_ID)
        inOrder.verify(recordService).decryptRecord<DomainResource>(mockEncryptedRecord, USER_ID)
        inOrder.verify(recordService).assignResourceId(mockDecryptedFhir3Record)
        inOrder.verifyNoMoreInteractions()
    }

    @Test
    @Throws(InterruptedException::class)
    fun `Given, fetchRecords is called with multiple RecordIds and a UserId, it returns FetchedRecords`() {
        // Given
        Mockito.doReturn(Single.just(mockRecord))
                .`when`(recordService)
                .fetchFhir3Record<DomainResource>(RECORD_ID, USER_ID)
        val ids = listOf(RECORD_ID, RECORD_ID)

        // When
        val observer = recordService.fetchFhir3Records<CarePlan>(ids, USER_ID).test().await()

        // Then
        val result = observer
                .assertNoErrors()
                .assertComplete()
                .assertValueCount(1)
                .values()[0]
        Truth.assertThat(result.successfulFetches).hasSize(2)
        Truth.assertThat(result.failedFetches).hasSize(0)
        inOrder.verify(recordService).fetchFhir3Records<DomainResource>(ids, USER_ID)
        inOrder.verify(recordService, Mockito.times(2)).fetchFhir3Record<DomainResource>(RECORD_ID, USER_ID)
        inOrder.verifyNoMoreInteractions()
    }

    @Test
    @Throws(InterruptedException::class, IOException::class, DataValidationException.ModelVersionNotSupported::class)
    fun `Given, fetchRecords called with a UserId, a ResourceType, a StartDate, a EndDate, the PageSize and Offset, it returns FetchedRecords`() {
        // Given
        val encryptedRecords = listOf(mockEncryptedRecord, mockEncryptedRecord)
        Mockito.`when`(mockTaggingService.getTagFromType(CarePlan::class.java as Class<Any>)).thenReturn(mockTags)
        Mockito.`when`(mockTagEncryptionService.encryptTags(mockTags)).thenReturn(mockEncryptedTags)
        Mockito.`when`(
                mockTagEncryptionService.encryptAnnotations(listOf())
        ).thenReturn(mockEncryptedAnnotations)
        Mockito.`when`(
                mockApiService.fetchRecords(
                        ArgumentMatchers.eq(ALIAS),
                        ArgumentMatchers.eq(USER_ID),
                        ArgumentMatchers.isNull(),
                        ArgumentMatchers.isNull(),
                        ArgumentMatchers.eq(10),
                        ArgumentMatchers.eq(0),
                        ArgumentMatchers.eq(mockEncryptedTags)
                )
        ).thenReturn(Observable.just(encryptedRecords))
        Mockito.doReturn(mockDecryptedFhir3Record)
                .`when`(recordService)
                .decryptRecord<DomainResource>(mockEncryptedRecord, USER_ID)
        @Suppress("UNCHECKED_CAST")
        every { RecordMapper.getInstance(mockDecryptedFhir3Record) } returns mockRecord as BaseRecord<DomainResource>

        // When
        val observer = recordService.fetchFhir3Records(
                USER_ID,
                CarePlan::class.java,
                null,
                null,
                10,
                0
        ).test().await()

        // Then
        val fetched = observer
                .assertNoErrors()
                .assertComplete()
                .assertValueCount(1)
                .values()[0]
        Truth.assertThat(fetched).hasSize(2)
        Truth.assertThat(fetched[0].meta).isEqualTo(mockMeta)
        Truth.assertThat(fetched[0].fhirResource).isEqualTo(mockCarePlan)
        Truth.assertThat(fetched[1].meta).isEqualTo(mockMeta)
        Truth.assertThat(fetched[1].fhirResource).isEqualTo(mockCarePlan)
        inOrder.verify(mockTaggingService).getTagFromType(CarePlan::class.java as Class<Any>)
        inOrder.verify(mockTagEncryptionService).encryptTags(mockTags)
        inOrder.verify(mockTagEncryptionService).encryptAnnotations(listOf())
        inOrder.verify(mockApiService).fetchRecords(
                ArgumentMatchers.eq(ALIAS),
                ArgumentMatchers.eq(USER_ID),
                ArgumentMatchers.isNull(),
                ArgumentMatchers.isNull(),
                ArgumentMatchers.eq(10),
                ArgumentMatchers.eq(0),
                ArgumentMatchers.eq(mockEncryptedTags)
        )
        inOrder.verify(recordService).decryptRecord<DomainResource>(mockEncryptedRecord, USER_ID)
        inOrder.verify(recordService).assignResourceId(mockDecryptedFhir3Record)
        inOrder.verify(recordService).decryptRecord<DomainResource>(mockEncryptedRecord, USER_ID)
        inOrder.verify(recordService).assignResourceId(mockDecryptedFhir3Record)
        inOrder.verifyNoMoreInteractions()
    }

    @Test
    @Throws(InterruptedException::class, IOException::class, DataValidationException.ModelVersionNotSupported::class)
    fun `Given, fetchRecords called with a UserId, a ResourceType, Annotations, a StartDate, a EndDate, the PageSize and Offset, it returns FetchedRecords`() {
        // Given
        val encryptedRecords = listOf(mockEncryptedRecord, mockEncryptedRecord)
        Mockito.`when`(mockTaggingService.getTagFromType(CarePlan::class.java as Class<Any>)).thenReturn(mockTags)
        Mockito.`when`(mockTagEncryptionService.encryptTags(mockTags)).thenReturn(mockEncryptedTags)
        Mockito.`when`(
                mockTagEncryptionService.encryptAnnotations(ANNOTATIONS)
        ).thenReturn(mockEncryptedAnnotations)
        Mockito.`when`(
                mockApiService.fetchRecords(
                        ArgumentMatchers.eq(ALIAS),
                        ArgumentMatchers.eq(USER_ID),
                        ArgumentMatchers.isNull(),
                        ArgumentMatchers.isNull(),
                        ArgumentMatchers.eq(10),
                        ArgumentMatchers.eq(0),
                        ArgumentMatchers.eq(mockEncryptedTags)
                )
        ).thenReturn(Observable.just(encryptedRecords))
        Mockito.doReturn(mockAnnotatedDecryptedFhirRecord)
                .`when`(recordService)
                .decryptRecord<DomainResource>(mockEncryptedRecord, USER_ID)
        @Suppress("UNCHECKED_CAST")
        every { RecordMapper.getInstance(mockAnnotatedDecryptedFhirRecord) } returns mockRecord as BaseRecord<DomainResource>

        // When
        val observer = recordService.fetchFhir3Records(
                USER_ID,
                CarePlan::class.java,
                ANNOTATIONS,
                null,
                null,
                10,
                0
        ).test().await()

        // Then
        val fetched = observer
                .assertNoErrors()
                .assertComplete()
                .assertValueCount(1)
                .values()[0]
        Truth.assertThat(fetched).hasSize(2)
        Truth.assertThat(fetched[0].meta).isEqualTo(mockMeta)
        Truth.assertThat(fetched[0].fhirResource).isEqualTo(mockCarePlan)
        Truth.assertThat(fetched[1].meta).isEqualTo(mockMeta)
        Truth.assertThat(fetched[1].fhirResource).isEqualTo(mockCarePlan)
        inOrder.verify(mockTaggingService).getTagFromType(CarePlan::class.java as Class<Any>)
        inOrder.verify(mockTagEncryptionService).encryptTags(mockTags)
        inOrder.verify(mockTagEncryptionService).encryptAnnotations(ANNOTATIONS)
        inOrder.verify(mockApiService).fetchRecords(
                ArgumentMatchers.eq(ALIAS),
                ArgumentMatchers.eq(USER_ID),
                ArgumentMatchers.isNull(),
                ArgumentMatchers.isNull(),
                ArgumentMatchers.eq(10),
                ArgumentMatchers.eq(0),
                ArgumentMatchers.eq(mockEncryptedTags)
        )
        inOrder.verify(recordService).decryptRecord<DomainResource>(mockEncryptedRecord, USER_ID)
        inOrder.verify(recordService).assignResourceId(mockAnnotatedDecryptedFhirRecord)
        inOrder.verify(recordService).decryptRecord<DomainResource>(mockEncryptedRecord, USER_ID)
        inOrder.verify(recordService).assignResourceId(mockAnnotatedDecryptedFhirRecord)
        inOrder.verifyNoMoreInteractions()
    }

    @Test
    fun `Given, fetchDataRecord is called with UserId and a RecordId for a DataRecord, it to the generic fetchRecord and returns its Result`() {
        // Given
        val userId = "id"
        val recordId = "123"

        val createdRecord = mockk<DataRecord<DataResource>>()

        @Suppress("UNCHECKED_CAST")
        every {
            recordServiceK._fetchRecord<Any>(recordId, userId)
        } returns Single.just(createdRecord as BaseRecord<Any>)

        // When
        val subscriber = recordServiceK.fetchDataRecord(
                userId,
                recordId
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

    @Test
    fun `Given, fetchFhir3Record is called with UserId and a RecordId for a Fhir3Resources, it to the generic fetchRecord and return its Result`() {
        // Given
        val userId = "id"
        val recordId = "123"

        val createdRecord = mockk<Record<Fhir3Resource>>()

        @Suppress("UNCHECKED_CAST")
        every {
            recordServiceK._fetchRecord<Any>(recordId, userId)
        } returns Single.just(createdRecord as BaseRecord<Any>)

        // When
        val subscriber = recordServiceK.fetchFhir3Record<Fhir3Resource>(
                userId,
                recordId
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

    @Test
    fun `Given, fetchFhir4Record is called with UserId and a RecordId, it to the generic fetchRecord and return its Result`() {
        // Given
        val userId = "id"
        val recordId = "123"

        val createdRecord = mockk<Fhir4Record<Fhir4Resource>>()

        @Suppress("UNCHECKED_CAST")
        every {
            recordServiceK._fetchRecord<Any>(recordId, userId)
        } returns Single.just(createdRecord as BaseRecord<Any>)

        // When
        val subscriber = recordServiceK.fetchFhir4Record<Fhir4Resource>(
                userId,
                recordId
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

    @Test
    fun `Given, fetchDataRecords is called with a UserId, Annotations, a StartDate, a EndDate, the PageSize and Offset, it delegates to the generic fetchRecord and returns its Result`() {
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
            recordServiceK._fetchRecords<Any>(
                    userId,
                    null,
                    annotations,
                    startDate,
                    endDate,
                    pageSize,
                    offset
            )
        } returns Single.just(fetchedRecords as List<BaseRecord<Any>>)

        // When
        val observer = recordServiceK.fetchDataRecords(
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
        assertSame(
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
        val startDate = mockk<LocalDate>(relaxed = true)
        val endDate = mockk<LocalDate>(relaxed = true)
        val pageSize = 123
        val offset = 2

        val fetchedRecords = listOf(mockk<Record<Fhir3Resource>>())

        @Suppress("UNCHECKED_CAST")
        every {
            recordServiceK._fetchRecords(
                    userId,
                    klass,
                    annotations,
                    startDate,
                    endDate,
                    pageSize,
                    offset
            )
        } returns Single.just(fetchedRecords)

        // When
        val observer = recordServiceK.fetchFhir3Records(
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
        assertSame(
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
        val startDate = mockk<LocalDate>(relaxed = true)
        val endDate = mockk<LocalDate>(relaxed = true)
        val pageSize = 123
        val offset = 2

        val fetchedRecords = listOf(mockk<Fhir4Record<Fhir4Resource>>())

        @Suppress("UNCHECKED_CAST")
        every {
            recordServiceK._fetchRecords(
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
        val observer = recordServiceK.fetchFhir4Records(
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
        assertSame(
                record,
                fetchedRecords
        )
    }
}
