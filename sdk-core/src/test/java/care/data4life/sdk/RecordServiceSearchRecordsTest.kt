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

import care.data4life.sdk.attachment.AttachmentContract
import care.data4life.sdk.call.DataRecord
import care.data4life.sdk.call.Fhir4Record
import care.data4life.sdk.crypto.CryptoContract
import care.data4life.sdk.data.DataResource
import care.data4life.sdk.date.DateResolver
import care.data4life.sdk.fhir.FhirContract
import care.data4life.sdk.migration.MigrationContract
import care.data4life.sdk.model.Record
import care.data4life.sdk.model.RecordMapper
import care.data4life.sdk.network.NetworkingContract
import care.data4life.sdk.network.model.DecryptedDataRecord
import care.data4life.sdk.network.model.EncryptedRecord
import care.data4life.sdk.network.model.NetworkModelInternalContract.DecryptedFhir3Record
import care.data4life.sdk.network.model.NetworkModelInternalContract.DecryptedFhir4Record
import care.data4life.sdk.tag.TaggingContract
import care.data4life.sdk.tag.Tags
import care.data4life.sdk.test.util.GenericTestDataProvider.ALIAS
import care.data4life.sdk.test.util.GenericTestDataProvider.PARTNER_ID
import care.data4life.sdk.test.util.GenericTestDataProvider.USER_ID
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.spyk
import io.mockk.unmockkObject
import io.mockk.verifyOrder
import io.reactivex.Observable
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame
import care.data4life.fhir.r4.model.CarePlan as Fhir4CarePlan
import care.data4life.fhir.stu3.model.CarePlan as Fhir3CarePlan

class RecordServiceSearchRecordsTest {
    private lateinit var recordService: RecordService
    private val apiService: NetworkingContract.Service = mockk()
    private val cryptoService: CryptoContract.Service = mockk()
    private val resourceCryptoService: FhirContract.CryptoService = mockk()
    private val tagCryptoService: TaggingContract.CryptoService = mockk()
    private val taggingService: TaggingContract.Service = mockk()
    private val attachmentService: AttachmentContract.Service = mockk()
    private val errorHandler: SdkContract.ErrorHandler = mockk()

    private val tags: Tags = mockk()
    private val defaultAnnotation: List<String> = emptyList()

    private val compatibilityService: MigrationContract.CompatibilityService = mockk()

    @Before
    fun setUp() {
        clearAllMocks()

        recordService = spyk(
            RecordService(
                PARTNER_ID,
                ALIAS,
                apiService,
                tagCryptoService,
                taggingService,
                resourceCryptoService,
                attachmentService,
                cryptoService,
                errorHandler,
                compatibilityService
            )
        )

        mockkObject(RecordMapper)
        mockkObject(DateResolver)
    }

    @After
    fun tearDown() {
        unmockkObject(RecordMapper)
        unmockkObject(DateResolver)
    }

    @Test
    fun `Given, searchFhir3Records called with a UserId, a ResourceType, a nulled StartDate, a nulled EndDate, the PageSize and Offset, it returns List of Records`() {
        // Given

        val resource1: Fhir3CarePlan = mockk()
        val id1 = "id1"
        val resource2: Fhir3CarePlan = mockk()
        val id2 = "id2"

        val includeDeletedRecords = true
        val offset = 42
        val pageSize = 23

        val encryptedRecord1: EncryptedRecord = mockk()
        val encryptedRecord2: EncryptedRecord = mockk()
        val decryptedRecord1: DecryptedFhir3Record<Fhir3CarePlan> = mockk(relaxed = true)
        val decryptedRecord2: DecryptedFhir3Record<Fhir3CarePlan> = mockk(relaxed = true)
        val record1: Record<Fhir3CarePlan> = mockk()
        val record2: Record<Fhir3CarePlan> = mockk()

        val encryptedRecords = listOf(encryptedRecord1, encryptedRecord2)
        val searchTags: NetworkingContract.SearchTags = mockk()

        every {
            hint(Fhir3CarePlan::class)
            decryptedRecord1.resource
        } returns resource1
        every { decryptedRecord1.identifier } returns id1
        every {
            hint(Fhir3CarePlan::class)
            decryptedRecord2.resource
        } returns resource2
        every { decryptedRecord2.identifier } returns id2
        every { decryptedRecord1.annotations } returns defaultAnnotation
        every { decryptedRecord2.annotations } returns defaultAnnotation

        every { DateResolver.resolveCreationDate(null) } returns Pair(null, null)
        every { DateResolver.resolveUpdateDate(null) } returns Pair(null, null)

        every { taggingService.getTagsFromType(Fhir3CarePlan::class.java as Class<Any>) } returns tags
        every { compatibilityService.resolveSearchTags(tags, defaultAnnotation) } returns searchTags

        every {
            apiService.searchRecords(
                ALIAS,
                USER_ID,
                null,
                null,
                null,
                null,
                includeDeletedRecords,
                pageSize,
                offset,
                searchTags
            )
        } returns Observable.fromArray(encryptedRecords)
        every {
            recordService.decryptRecord<Fhir3CarePlan>(encryptedRecord1, USER_ID)
        } returns decryptedRecord1
        every {
            recordService.decryptRecord<Fhir3CarePlan>(encryptedRecord2, USER_ID)
        } returns decryptedRecord2
        every { RecordMapper.getInstance(decryptedRecord1) } returns record1
        every { RecordMapper.getInstance(decryptedRecord2) } returns record2

        // When
        val observer = recordService.searchFhir3Records(
            USER_ID,
            Fhir3CarePlan::class.java,
            emptyList(),
            null,
            null,
            includeDeletedRecords,
            pageSize,
            offset
        ).test().await()

        // Then
        val fetched = observer
            .assertNoErrors()
            .assertComplete()
            .assertValueCount(1)
            .values()[0]

        assertEquals(
            expected = 2,
            actual = fetched.size
        )
        assertSame(
            expected = record1,
            actual = fetched[0]
        )
        assertSame(
            expected = record2,
            actual = fetched[1]
        )

        verifyOrder {
            recordService.searchFhir3Records(
                USER_ID,
                Fhir3CarePlan::class.java,
                emptyList(),
                null,
                null,
                includeDeletedRecords,
                pageSize,
                offset
            )
            DateResolver.resolveCreationDate(null)
            DateResolver.resolveUpdateDate(null)
            taggingService.getTagsFromType(Fhir3CarePlan::class.java as Class<Any>)
            compatibilityService.resolveSearchTags(tags, defaultAnnotation)
            apiService.searchRecords(
                ALIAS,
                USER_ID,
                null,
                null,
                null,
                null,
                includeDeletedRecords,
                pageSize,
                offset,
                searchTags
            )
            recordService.decryptRecord<Fhir3CarePlan>(
                encryptedRecord1,
                USER_ID
            )
            resource1.id = id1
            RecordMapper.getInstance(decryptedRecord1)
            recordService.decryptRecord<Fhir3CarePlan>(
                encryptedRecord2,
                USER_ID
            )
            resource2.id = id2
            RecordMapper.getInstance(decryptedRecord2)
        }
    }

    @Test
    fun `Given, searchFhir3Records called with a UserId, a ResourceType, a CreationDateRange, UpdateDateTimeRange, the PageSize and Offset, it returns List of Records`() {
        // Given

        val resource1: Fhir3CarePlan = mockk()
        val id1 = "id1"
        val resource2: Fhir3CarePlan = mockk()
        val id2 = "id2"

        val creationDate = SdkContract.CreationDateRange(null, null)
        val creation = Pair("creationStart", "creationEnd")
        val updateDate = SdkContract.UpdateDateTimeRange(null, null)
        val update = Pair("updateStart", "updateEnd")
        val offset = 42
        val pageSize = 23
        val includeDeletedRecords = true

        val encryptedRecord1: EncryptedRecord = mockk()
        val encryptedRecord2: EncryptedRecord = mockk()
        val decryptedRecord1: DecryptedFhir3Record<Fhir3CarePlan> = mockk(relaxed = true)
        val decryptedRecord2: DecryptedFhir3Record<Fhir3CarePlan> = mockk(relaxed = true)
        val record1: Record<Fhir3CarePlan> = mockk()
        val record2: Record<Fhir3CarePlan> = mockk()

        val encryptedRecords = listOf(encryptedRecord1, encryptedRecord2)
        val searchTags: NetworkingContract.SearchTags = mockk()

        every {
            hint(Fhir3CarePlan::class)
            decryptedRecord1.resource
        } returns resource1
        every { decryptedRecord1.identifier } returns id1
        every {
            hint(Fhir3CarePlan::class)
            decryptedRecord2.resource
        } returns resource2
        every { decryptedRecord2.identifier } returns id2

        every { decryptedRecord1.annotations } returns defaultAnnotation
        every { decryptedRecord2.annotations } returns defaultAnnotation

        every { DateResolver.resolveCreationDate(creationDate) } returns creation
        every { DateResolver.resolveUpdateDate(updateDate) } returns update

        every { taggingService.getTagsFromType(Fhir3CarePlan::class.java as Class<Any>) } returns tags
        every { compatibilityService.resolveSearchTags(tags, defaultAnnotation) } returns searchTags

        every {
            apiService.searchRecords(
                ALIAS,
                USER_ID,
                creation.first,
                creation.second,
                update.first,
                update.second,
                includeDeletedRecords,
                pageSize,
                offset,
                searchTags
            )
        } returns Observable.fromArray(encryptedRecords)

        every {
            recordService.decryptRecord<Fhir3CarePlan>(encryptedRecord1, USER_ID)
        } returns decryptedRecord1
        every {
            recordService.decryptRecord<Fhir3CarePlan>(encryptedRecord2, USER_ID)
        } returns decryptedRecord2
        every { RecordMapper.getInstance(decryptedRecord1) } returns record1
        every { RecordMapper.getInstance(decryptedRecord2) } returns record2

        // When
        val observer = recordService.searchFhir3Records(
            USER_ID,
            Fhir3CarePlan::class.java,
            emptyList(),
            creationDate,
            updateDate,
            includeDeletedRecords,
            pageSize,
            offset
        ).test().await()

        // Then
        val fetched = observer
            .assertNoErrors()
            .assertComplete()
            .assertValueCount(1)
            .values()[0]

        assertEquals(
            expected = 2,
            actual = fetched.size
        )
        assertSame(
            expected = record1,
            actual = fetched[0]
        )
        assertSame(
            expected = record2,
            actual = fetched[1]
        )

        verifyOrder {
            recordService.searchFhir3Records(
                USER_ID,
                Fhir3CarePlan::class.java,
                emptyList(),
                creationDate,
                updateDate,
                includeDeletedRecords,
                pageSize,
                offset
            )
            DateResolver.resolveCreationDate(creationDate)
            DateResolver.resolveUpdateDate(updateDate)
            taggingService.getTagsFromType(Fhir3CarePlan::class.java as Class<Any>)
            compatibilityService.resolveSearchTags(tags, defaultAnnotation)
            apiService.searchRecords(
                ALIAS,
                USER_ID,
                creation.first,
                creation.second,
                update.first,
                update.second,
                includeDeletedRecords,
                pageSize,
                offset,
                searchTags
            )
            recordService.decryptRecord<Fhir3CarePlan>(
                encryptedRecord1,
                USER_ID
            )
            resource1.id = id1
            RecordMapper.getInstance(decryptedRecord1)
            recordService.decryptRecord<Fhir3CarePlan>(
                encryptedRecord2,
                USER_ID
            )
            resource2.id = id2
            RecordMapper.getInstance(decryptedRecord2)
        }
    }

    @Test
    fun `Given, searchFhir4Records called with a UserId, a ResourceType, a nulled StartDate, a nulled EndDate, the PageSize and Offset, it returns List of Fhir4Records`() {
        // Given

        val resource1: Fhir4CarePlan = mockk()
        val id1 = "id1"
        val resource2: Fhir4CarePlan = mockk()
        val id2 = "id2"

        val offset = 42
        val pageSize = 23
        val includeDeletedRecords = true

        val encryptedRecord1: EncryptedRecord = mockk()
        val encryptedRecord2: EncryptedRecord = mockk()
        val decryptedRecord1: DecryptedFhir4Record<Fhir4CarePlan> = mockk(relaxed = true)
        val decryptedRecord2: DecryptedFhir4Record<Fhir4CarePlan> = mockk(relaxed = true)
        val record1: Fhir4Record<Fhir4CarePlan> = mockk()
        val record2: Fhir4Record<Fhir4CarePlan> = mockk()

        val encryptedRecords = listOf(encryptedRecord1, encryptedRecord2)
        val searchTags: NetworkingContract.SearchTags = mockk()

        every {
            hint(Fhir4CarePlan::class)
            decryptedRecord1.resource
        } returns resource1
        every { decryptedRecord1.identifier } returns id1
        every {
            hint(Fhir4CarePlan::class)
            decryptedRecord2.resource
        } returns resource2
        every { decryptedRecord2.identifier } returns id2

        every { DateResolver.resolveCreationDate(null) } returns Pair(null, null)
        every { DateResolver.resolveUpdateDate(null) } returns Pair(null, null)

        every { taggingService.getTagsFromType(Fhir4CarePlan::class.java as Class<Any>) } returns tags
        every { compatibilityService.resolveSearchTags(tags, defaultAnnotation) } returns searchTags

        every {
            apiService.searchRecords(
                ALIAS,
                USER_ID,
                null,
                null,
                null,
                null,
                includeDeletedRecords,
                pageSize,
                offset,
                searchTags
            )
        } returns Observable.fromArray(encryptedRecords)
        every { decryptedRecord1.annotations } returns defaultAnnotation
        every { decryptedRecord2.annotations } returns defaultAnnotation
        every {
            recordService.decryptRecord<Fhir4CarePlan>(encryptedRecord1, USER_ID)
        } returns decryptedRecord1
        every {
            recordService.decryptRecord<Fhir4CarePlan>(encryptedRecord2, USER_ID)
        } returns decryptedRecord2
        every { RecordMapper.getInstance(decryptedRecord1) } returns record1
        every { RecordMapper.getInstance(decryptedRecord2) } returns record2

        // When
        val observer = recordService.searchFhir4Records(
            USER_ID,
            Fhir4CarePlan::class.java,
            emptyList(),
            null,
            null,
            includeDeletedRecords,
            pageSize,
            offset
        ).test().await()

        // Then
        val fetched = observer
            .assertNoErrors()
            .assertComplete()
            .assertValueCount(1)
            .values()[0]

        assertEquals(
            expected = 2,
            actual = fetched.size
        )
        assertSame(
            expected = record1,
            actual = fetched[0]
        )
        assertSame(
            expected = record2,
            actual = fetched[1]
        )

        verifyOrder {
            DateResolver.resolveCreationDate(null)
            DateResolver.resolveUpdateDate(null)
            taggingService.getTagsFromType(Fhir4CarePlan::class.java as Class<Any>)
            compatibilityService.resolveSearchTags(tags, defaultAnnotation)
            apiService.searchRecords(
                ALIAS,
                USER_ID,
                null,
                null,
                null,
                null,
                includeDeletedRecords,
                pageSize,
                offset,
                searchTags
            )
            recordService.decryptRecord<Fhir4CarePlan>(
                encryptedRecord1,
                USER_ID
            )
            resource1.id = id1
            RecordMapper.getInstance(decryptedRecord1)
            recordService.decryptRecord<Fhir4CarePlan>(
                encryptedRecord2,
                USER_ID
            )
            resource2.id = id2
            RecordMapper.getInstance(decryptedRecord2)
        }
    }

    @Test
    fun `Given, searchFhir4Records called with a UserId, a ResourceType, a StartDate, a EndDate, the PageSize and Offset, it returns List of Fhir4Records`() {
        // Given

        val resource1: Fhir4CarePlan = mockk()
        val id1 = "id1"
        val resource2: Fhir4CarePlan = mockk()
        val id2 = "id2"

        val creationDate = SdkContract.CreationDateRange(null, null)
        val creation = Pair("creationStart", "creationEnd")
        val updateDate = SdkContract.UpdateDateTimeRange(null, null)
        val update = Pair("updateStart", "updateEnd")
        val offset = 42
        val pageSize = 23
        val includeDeletedRecords = true

        val encryptedRecord1: EncryptedRecord = mockk()
        val encryptedRecord2: EncryptedRecord = mockk()
        val decryptedRecord1: DecryptedFhir4Record<Fhir4CarePlan> = mockk(relaxed = true)
        val decryptedRecord2: DecryptedFhir4Record<Fhir4CarePlan> = mockk(relaxed = true)
        val record1: Fhir4Record<Fhir4CarePlan> = mockk()
        val record2: Fhir4Record<Fhir4CarePlan> = mockk()

        val encryptedRecords = listOf(encryptedRecord1, encryptedRecord2)
        val searchTags: NetworkingContract.SearchTags = mockk()

        every {
            hint(Fhir4CarePlan::class)
            decryptedRecord1.resource
        } returns resource1
        every { decryptedRecord1.identifier } returns id1
        every {
            hint(Fhir4CarePlan::class)
            decryptedRecord2.resource
        } returns resource2
        every { decryptedRecord2.identifier } returns id2

        every { decryptedRecord1.annotations } returns defaultAnnotation
        every { decryptedRecord2.annotations } returns defaultAnnotation

        every { DateResolver.resolveCreationDate(creationDate) } returns creation
        every { DateResolver.resolveUpdateDate(updateDate) } returns update

        every { taggingService.getTagsFromType(Fhir4CarePlan::class.java as Class<Any>) } returns tags
        every { compatibilityService.resolveSearchTags(tags, defaultAnnotation) } returns searchTags

        every {
            apiService.searchRecords(
                ALIAS,
                USER_ID,
                creation.first,
                creation.second,
                update.first,
                update.second,
                includeDeletedRecords,
                pageSize,
                offset,
                searchTags
            )
        } returns Observable.fromArray(encryptedRecords)

        every {
            recordService.decryptRecord<Fhir4CarePlan>(encryptedRecord1, USER_ID)
        } returns decryptedRecord1
        every {
            recordService.decryptRecord<Fhir4CarePlan>(encryptedRecord2, USER_ID)
        } returns decryptedRecord2
        every { RecordMapper.getInstance(decryptedRecord1) } returns record1
        every { RecordMapper.getInstance(decryptedRecord2) } returns record2

        // When
        val observer = recordService.searchFhir4Records(
            USER_ID,
            Fhir4CarePlan::class.java,
            emptyList(),
            creationDate,
            updateDate,
            includeDeletedRecords,
            pageSize,
            offset
        ).test().await()

        // Then
        val fetched = observer
            .assertNoErrors()
            .assertComplete()
            .assertValueCount(1)
            .values()[0]

        assertEquals(
            expected = 2,
            actual = fetched.size
        )
        assertSame(
            expected = record1,
            actual = fetched[0]
        )
        assertSame(
            expected = record2,
            actual = fetched[1]
        )

        verifyOrder {
            recordService.searchFhir4Records(
                USER_ID,
                Fhir4CarePlan::class.java,
                emptyList(),
                creationDate,
                updateDate,
                includeDeletedRecords,
                pageSize,
                offset
            )
            DateResolver.resolveCreationDate(creationDate)
            DateResolver.resolveUpdateDate(updateDate)
            taggingService.getTagsFromType(Fhir4CarePlan::class.java as Class<Any>)
            compatibilityService.resolveSearchTags(tags, defaultAnnotation)
            apiService.searchRecords(
                ALIAS,
                USER_ID,
                creation.first,
                creation.second,
                update.first,
                update.second,
                includeDeletedRecords,
                pageSize,
                offset,
                searchTags
            )
            recordService.decryptRecord<Fhir4CarePlan>(
                encryptedRecord1,
                USER_ID
            )
            resource1.id = id1
            RecordMapper.getInstance(decryptedRecord1)
            recordService.decryptRecord<Fhir4CarePlan>(
                encryptedRecord2,
                USER_ID
            )
            resource2.id = id2
            RecordMapper.getInstance(decryptedRecord2)
        }
    }

    @Test
    fun `Given, searchDataRecords called with a UserId, a ResourceType, a nulled StartDate, a nulled EndDate, the PageSize and Offset, it returns List of Fhir4Records`() {
        // Given

        val resource1: DataResource = mockk()
        val id1 = "id1"
        val resource2: DataResource = mockk()
        val id2 = "id2"

        val offset = 42
        val pageSize = 23
        val includeDeletedRecords = true

        val encryptedRecord1: EncryptedRecord = mockk()
        val encryptedRecord2: EncryptedRecord = mockk()
        val decryptedRecord1: DecryptedDataRecord = mockk(relaxed = true)
        val decryptedRecord2: DecryptedDataRecord = mockk(relaxed = true)
        val record1: DataRecord<DataResource> = mockk()
        val record2: DataRecord<DataResource> = mockk()

        val encryptedRecords = listOf(encryptedRecord1, encryptedRecord2)
        val searchTags: NetworkingContract.SearchTags = mockk()

        every {
            decryptedRecord1.resource
        } returns resource1
        every { decryptedRecord1.identifier } returns id1
        every {
            decryptedRecord2.resource
        } returns resource2
        every { decryptedRecord2.identifier } returns id2

        every { DateResolver.resolveCreationDate(null) } returns Pair(null, null)
        every { DateResolver.resolveUpdateDate(null) } returns Pair(null, null)

        every { taggingService.getTagsFromType(DataResource::class.java) } returns tags
        every { compatibilityService.resolveSearchTags(tags, defaultAnnotation) } returns searchTags

        every {
            apiService.searchRecords(
                ALIAS,
                USER_ID,
                null,
                null,
                null,
                null,
                includeDeletedRecords,
                pageSize,
                offset,
                searchTags
            )
        } returns Observable.fromArray(encryptedRecords)
        every { decryptedRecord1.annotations } returns defaultAnnotation
        every { decryptedRecord2.annotations } returns defaultAnnotation
        every {
            recordService.decryptRecord<DataResource>(encryptedRecord1, USER_ID)
        } returns decryptedRecord1
        every {
            recordService.decryptRecord<DataResource>(encryptedRecord2, USER_ID)
        } returns decryptedRecord2
        every { RecordMapper.getInstance(decryptedRecord1) } returns record1
        every { RecordMapper.getInstance(decryptedRecord2) } returns record2

        // When
        val observer = recordService.searchDataRecords(
            USER_ID,
            emptyList(),
            null,
            null,
            includeDeletedRecords,
            pageSize,
            offset
        ).test().await()

        // Then
        val fetched = observer
            .assertNoErrors()
            .assertComplete()
            .assertValueCount(1)
            .values()[0]

        assertEquals(
            expected = 2,
            actual = fetched.size
        )
        assertSame(
            expected = record1,
            actual = fetched[0]
        )
        assertSame(
            expected = record2,
            actual = fetched[1]
        )

        verifyOrder {
            DateResolver.resolveCreationDate(null)
            DateResolver.resolveUpdateDate(null)
            taggingService.getTagsFromType(DataResource::class.java)
            compatibilityService.resolveSearchTags(tags, defaultAnnotation)
            apiService.searchRecords(
                ALIAS,
                USER_ID,
                null,
                null,
                null,
                null,
                includeDeletedRecords,
                pageSize,
                offset,
                searchTags
            )
            recordService.decryptRecord<DataResource>(
                encryptedRecord1,
                USER_ID
            )
            RecordMapper.getInstance(decryptedRecord1)
            recordService.decryptRecord<DataResource>(
                encryptedRecord2,
                USER_ID
            )
            RecordMapper.getInstance(decryptedRecord2)
        }
    }

    @Test
    fun `Given, searchDataRecords called with a UserId, a ResourceType, a StartDate, a EndDate, the PageSize and Offset, it returns List of Fhir4Records`() {
        // Given

        val resource1: DataResource = mockk()
        val id1 = "id1"
        val resource2: DataResource = mockk()
        val id2 = "id2"

        val creationDate = SdkContract.CreationDateRange(null, null)
        val creation = Pair("creationStart", "creationEnd")
        val updateDate = SdkContract.UpdateDateTimeRange(null, null)
        val update = Pair("updateStart", "updateEnd")
        val offset = 42
        val pageSize = 23
        val includeDeletedRecords = true

        val encryptedRecord1: EncryptedRecord = mockk()
        val encryptedRecord2: EncryptedRecord = mockk()
        val decryptedRecord1: DecryptedDataRecord = mockk(relaxed = true)
        val decryptedRecord2: DecryptedDataRecord = mockk(relaxed = true)
        val record1: DataRecord<DataResource> = mockk()
        val record2: DataRecord<DataResource> = mockk()

        val encryptedRecords = listOf(encryptedRecord1, encryptedRecord2)
        val searchTags: NetworkingContract.SearchTags = mockk()

        every {
            decryptedRecord1.resource
        } returns resource1
        every { decryptedRecord1.identifier } returns id1
        every {
            decryptedRecord2.resource
        } returns resource2
        every { decryptedRecord2.identifier } returns id2

        every { decryptedRecord1.annotations } returns defaultAnnotation
        every { decryptedRecord2.annotations } returns defaultAnnotation

        every { DateResolver.resolveCreationDate(creationDate) } returns creation
        every { DateResolver.resolveUpdateDate(updateDate) } returns update

        every { taggingService.getTagsFromType(DataResource::class.java) } returns tags
        every { compatibilityService.resolveSearchTags(tags, defaultAnnotation) } returns searchTags

        every {
            apiService.searchRecords(
                ALIAS,
                USER_ID,
                creation.first,
                creation.second,
                update.first,
                update.second,
                includeDeletedRecords,
                pageSize,
                offset,
                searchTags
            )
        } returns Observable.fromArray(encryptedRecords)

        every {
            recordService.decryptRecord<DataResource>(encryptedRecord1, USER_ID)
        } returns decryptedRecord1
        every {
            recordService.decryptRecord<DataResource>(encryptedRecord2, USER_ID)
        } returns decryptedRecord2
        every { RecordMapper.getInstance(decryptedRecord1) } returns record1
        every { RecordMapper.getInstance(decryptedRecord2) } returns record2

        // When
        val observer = recordService.searchDataRecords(
            USER_ID,
            emptyList(),
            creationDate,
            updateDate,
            includeDeletedRecords,
            pageSize,
            offset
        ).test().await()

        // Then
        val fetched = observer
            .assertNoErrors()
            .assertComplete()
            .assertValueCount(1)
            .values()[0]

        assertEquals(
            expected = 2,
            actual = fetched.size
        )
        assertSame(
            expected = record1,
            actual = fetched[0]
        )
        assertSame(
            expected = record2,
            actual = fetched[1]
        )

        verifyOrder {
            recordService.searchDataRecords(
                USER_ID,
                emptyList(),
                creationDate,
                updateDate,
                includeDeletedRecords,
                pageSize,
                offset
            )
            DateResolver.resolveCreationDate(creationDate)
            DateResolver.resolveUpdateDate(updateDate)
            taggingService.getTagsFromType(DataResource::class.java)
            compatibilityService.resolveSearchTags(tags, defaultAnnotation)
            apiService.searchRecords(
                ALIAS,
                USER_ID,
                creation.first,
                creation.second,
                update.first,
                update.second,
                includeDeletedRecords,
                pageSize,
                offset,
                searchTags
            )
            recordService.decryptRecord<DataResource>(
                encryptedRecord1,
                USER_ID
            )
            RecordMapper.getInstance(decryptedRecord1)
            recordService.decryptRecord<DataResource>(
                encryptedRecord2,
                USER_ID
            )
            RecordMapper.getInstance(decryptedRecord2)
        }
    }
}
