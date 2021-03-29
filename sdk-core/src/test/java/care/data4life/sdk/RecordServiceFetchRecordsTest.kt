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

import care.data4life.sdk.RecordServiceTestBase.Companion.ALIAS
import care.data4life.sdk.RecordServiceTestBase.Companion.PARTNER_ID
import care.data4life.sdk.RecordServiceTestBase.Companion.RECORD_ID
import care.data4life.sdk.RecordServiceTestBase.Companion.USER_ID
import care.data4life.sdk.attachment.AttachmentContract
import care.data4life.sdk.call.DataRecord
import care.data4life.sdk.call.Fhir4Record
import care.data4life.sdk.data.DataResource
import care.data4life.sdk.fhir.Fhir3Resource
import care.data4life.sdk.fhir.Fhir4Resource
import care.data4life.sdk.fhir.FhirContract
import care.data4life.sdk.lang.D4LException
import care.data4life.sdk.migration.MigrationContract
import care.data4life.sdk.model.ModelContract.BaseRecord
import care.data4life.sdk.model.Record
import care.data4life.sdk.model.RecordMapper
import care.data4life.sdk.network.model.DecryptedDataRecord
import care.data4life.sdk.network.model.EncryptedRecord
import care.data4life.sdk.network.model.definitions.DecryptedBaseRecord
import care.data4life.sdk.network.model.definitions.DecryptedFhir3Record
import care.data4life.sdk.network.model.definitions.DecryptedFhir4Record
import care.data4life.sdk.tag.TaggingContract
import care.data4life.sdk.wrapper.SdkDateTimeFormatter
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.spyk
import io.mockk.unmockkObject
import io.mockk.verify
import io.mockk.verifyOrder
import io.reactivex.Observable
import io.reactivex.Single
import org.junit.After
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.threeten.bp.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertSame
import care.data4life.fhir.r4.model.CarePlan as Fhir4CarePlan
import care.data4life.fhir.stu3.model.CarePlan as Fhir3CarePlan

class RecordServiceFetchRecordsTest {
    private lateinit var recordService: RecordService
    private lateinit var apiService: ApiService
    private lateinit var cryptoService: CryptoService
    private lateinit var fhirService: FhirContract.Service
    private lateinit var tagEncryptionService: TaggingContract.EncryptionService
    private lateinit var taggingService: TaggingContract.Service
    private lateinit var attachmentService: AttachmentContract.Service
    private lateinit var errorHandler: SdkContract.ErrorHandler
    private lateinit var tags: HashMap<String, String>
    private lateinit var encryptedTags: MutableList<String>
    private val defaultAnnotation: MutableList<String> = mutableListOf()
    private lateinit var encryptedAnnotations: MutableList<String>


    private lateinit var compatibilityService: MigrationContract.CompatibilityService
    private lateinit var encryptedRecord: EncryptedRecord

    @Before
    fun setUp() {
        apiService = mockk()
        cryptoService = mockk()
        fhirService = mockk()
        tagEncryptionService = mockk()
        taggingService = mockk()
        attachmentService = mockk()
        errorHandler = mockk()
        tags = mockk()
        encryptedTags = mockk()
        encryptedAnnotations = mockk()
        encryptedRecord = mockk(relaxed = true)
        compatibilityService = mockk()

        recordService = spyk(
                RecordService(
                        PARTNER_ID,
                        ALIAS,
                        apiService,
                        tagEncryptionService,
                        taggingService,
                        fhirService,
                        attachmentService,
                        cryptoService,
                        errorHandler,
                        compatibilityService
                )
        )

        mockkObject(RecordMapper)
    }

    @After
    fun tearDown() {
        unmockkObject(RecordMapper)
    }

    @Test
    fun `Given, fetchFhir3Record is called with a RecordId and UserId for an Fhir3Record, it returns a Record`() {
        // Given
        val resource: Fhir3Resource = mockk()
        val id = "id"
        val expected: Record<Fhir3CarePlan> = mockk()
        val decrypted: DecryptedFhir3Record<Fhir3Resource> = mockk()

        every { decrypted.resource } returns resource
        every { decrypted.identifier } returns id

        every {
            apiService.fetchRecord(ALIAS, USER_ID, RECORD_ID)
        } returns Single.just(encryptedRecord)
        every {
            recordService.decryptRecord<Fhir3Resource>(
                    encryptedRecord,
                    USER_ID
            )
        } returns decrypted
        every { RecordMapper.getInstance(decrypted) } returns expected as BaseRecord<Fhir3Resource>

        // When
        val observer =
                recordService.fetchFhir3Record<Fhir3CarePlan>(USER_ID, RECORD_ID).test().await()

        // Then
        val record = observer
                .assertNoErrors()
                .assertComplete()
                .assertValueCount(1)
                .values()[0]

        assertSame<Record<Fhir3CarePlan>>(
                expected = expected as Record<Fhir3CarePlan>,
                actual = record
        )
        verifyOrder {
            apiService.fetchRecord(ALIAS, USER_ID, RECORD_ID)
            recordService.decryptRecord<Fhir3CarePlan>(encryptedRecord, USER_ID)
            resource.id = id
            RecordMapper.getInstance(decrypted)
        }
    }

    @Test
    fun `Given, fetchFhir4Record is called with a RecordId and UserId for an Fhir4Record, it returns a Record`() {
        // Given
        val resource: Fhir4Resource = mockk()
        val id = "id"
        val expected: Fhir4Record<Fhir4CarePlan> = mockk()
        val decrypted: DecryptedFhir4Record<Fhir4Resource> = mockk()

        every { decrypted.resource } returns resource
        every { decrypted.identifier } returns id

        every {
            apiService.fetchRecord(ALIAS, USER_ID, RECORD_ID)
        } returns Single.just(encryptedRecord)
        every {
            recordService.decryptRecord<Fhir4Resource>(
                    encryptedRecord,
                    USER_ID
            )
        } returns decrypted
        every { RecordMapper.getInstance(decrypted) } returns expected as BaseRecord<Fhir4Resource>

        // When
        val observer =
                recordService.fetchFhir4Record<Fhir4CarePlan>(USER_ID, RECORD_ID).test().await()

        // Then
        val record = observer
                .assertNoErrors()
                .assertComplete()
                .assertValueCount(1)
                .values()[0]

        assertSame<Fhir4Record<Fhir4CarePlan>>(
                expected = expected as Fhir4Record<Fhir4CarePlan>,
                actual = record
        )
        verifyOrder {
            apiService.fetchRecord(ALIAS, USER_ID, RECORD_ID)
            recordService.decryptRecord<Fhir4Resource>(encryptedRecord, USER_ID)
            resource.id = id
            RecordMapper.getInstance(decrypted)
        }
    }

    @Test
    fun `Given, fetchDataRecord is called with a RecordId and UserId for an DataRecord, it returns a Record`() {
        // Given
        val resource: DataResource = mockk()
        val expected: DataRecord<DataResource> = mockk()
        val decrypted: DecryptedDataRecord = mockk()

        every { decrypted.resource } returns resource

        every { apiService.fetchRecord(ALIAS, USER_ID, RECORD_ID) } returns Single.just(
                encryptedRecord
        )
        every {
            recordService.decryptRecord<DataResource>(encryptedRecord, USER_ID)
        } returns decrypted as DecryptedBaseRecord<DataResource>
        every { RecordMapper.getInstance(decrypted) } returns expected as BaseRecord<DataResource>

        // When
        val observer = recordService.fetchDataRecord(USER_ID, RECORD_ID).test().await()

        // Then
        val record = observer
                .assertNoErrors()
                .assertComplete()
                .assertValueCount(1)
                .values()[0]

        assertSame<DataRecord<DataResource>>(
                expected = expected,
                actual = record
        )
        verifyOrder {
            apiService.fetchRecord(ALIAS, USER_ID, RECORD_ID)
            recordService.decryptRecord<DataResource>(encryptedRecord, USER_ID)
            RecordMapper.getInstance(decrypted)
        }
    }

    @Test
    fun `Given, fetchFhir3Records is called with multiple RecordIds and a UserId, it returns a List of Records`() {
        //Given
        val ids = listOf("1", "2", "3")
        val record1: Record<Fhir3CarePlan> = mockk()
        val record2: Record<Fhir3CarePlan> = mockk()
        val record3: Record<Fhir3CarePlan> = mockk()
        // When
        every {
            recordService.fetchFhir3Record<Fhir3CarePlan>(USER_ID, match { id -> ids[0] == id })
        } returns Single.just(record1)
        every {
            recordService.fetchFhir3Record<Fhir3CarePlan>(USER_ID, match { id -> ids[1] == id })
        } returns Single.just(record2)
        every {
            recordService.fetchFhir3Record<Fhir3CarePlan>(USER_ID, match { id -> ids[2] == id })
        } returns Single.just(record3)

        val observer = recordService.fetchFhir3Records<Fhir3CarePlan>(ids, USER_ID).test().await()

        // Then
        val result = observer
                .assertNoErrors()
                .assertComplete()
                .assertValueCount(1)
                .values()[0]

        assertEquals<List<Record<Fhir3CarePlan>>>(
                expected = listOf(record1, record2, record3),
                actual = result.successfulFetches
        )
    }

    @Test
    @Ignore(value = "Timeout")
    fun `Given, fetchFhir3Records is called with multiple RecordIds and a UserId, it ignores errors`() {
        //Given
        val ids = listOf("1", "2", "3")
        val record1: Record<Fhir3CarePlan> = mockk()
        val record3: Record<Fhir3CarePlan> = mockk()
        val decrypted: DecryptedFhir3Record<Fhir3CarePlan> = mockk()
        val thrownError = RuntimeException("error")
        val expectedError: D4LException = mockk()

        // When
        every {
            recordService.fetchFhir3Record<Fhir3CarePlan>(USER_ID, match { id -> ids[0] == id })
        } returns Single.just(record1)

        every {
            apiService.fetchRecord(
                    ALIAS,
                    USER_ID,
                    ids[1]
            )
        } returns Single.just(encryptedRecord)
        every {
            recordService.decryptRecord<Fhir3Resource>(encryptedRecord, USER_ID)
        } returns decrypted as DecryptedBaseRecord<Fhir3Resource>
        every { RecordMapper.getInstance(decrypted) } throws thrownError
        every { errorHandler.handleError(thrownError) } returns expectedError

        every {
            recordService.fetchFhir3Record<Fhir3CarePlan>(USER_ID, match { id -> ids[2] == id })
        } returns Single.just(record3)

        val observer = recordService.fetchFhir3Records<Fhir3CarePlan>(ids, USER_ID).test().await()

        // Then
        val result = observer
                .assertComplete()
                .assertValueCount(1)
                .values()[0]

        assertEquals<List<Record<Fhir3CarePlan>>>(
                expected = listOf(record1, record3),
                actual = result.successfulFetches
        )
    }

    @Test
    fun `Given, fetchFhir3Records called with a UserId, a ResourceType, a nulled StartDate, a nulled EndDate, the PageSize and Offset, it returns List of Records`() {
        // Given
        mockkObject(SdkDateTimeFormatter)

        val resource1: Fhir3CarePlan = mockk()
        val id1 = "id1"
        val resource2: Fhir3CarePlan = mockk()
        val id2 = "id2"
        val encryptedRecord1: EncryptedRecord = mockk()
        val encryptedRecord2: EncryptedRecord = mockk()
        val decryptedRecord1: DecryptedFhir3Record<Fhir3CarePlan> = mockk(relaxed = true)
        val decryptedRecord2: DecryptedFhir3Record<Fhir3CarePlan> = mockk(relaxed = true)
        val record1: Record<Fhir3CarePlan> = mockk()
        val record2: Record<Fhir3CarePlan> = mockk()
        val offset = 42
        val pageSize = 23
        val encryptedRecords = listOf(encryptedRecord1, encryptedRecord2)

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

        every { taggingService.getTagsFromType(Fhir3CarePlan::class.java as Class<Any>) } returns tags
        every {
            compatibilityService.searchRecords(
                    ALIAS,
                    USER_ID,
                    null,
                    null,
                    pageSize,
                    offset,
                    tags,
                    defaultAnnotation
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
        val observer = recordService.fetchFhir3Records(
                USER_ID,
                Fhir3CarePlan::class.java,
                null,
                null,
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
            recordService.fetchFhir3Records(
                    USER_ID,
                    Fhir3CarePlan::class.java,
                    emptyList(),
                    null,
                    null,
                    pageSize,
                    offset
            )
            taggingService.getTagsFromType(Fhir3CarePlan::class.java as Class<Any>)
            compatibilityService.searchRecords(
                    ALIAS,
                    USER_ID,
                    null,
                    null,
                    pageSize,
                    offset,
                    tags,
                    defaultAnnotation
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
        verify(exactly = 0) { SdkDateTimeFormatter.formatDate(any()) }

        unmockkObject(SdkDateTimeFormatter)
    }

    @Test
    fun `Given, fetchFhir3Records called with a UserId, a ResourceType, a StartDate, a EndDate, the PageSize and Offset, it returns List of Records`() {
        // Given
        mockkObject(SdkDateTimeFormatter)

        val resource1: Fhir3CarePlan = mockk()
        val id1 = "id1"
        val resource2: Fhir3CarePlan = mockk()
        val id2 = "id2"
        val encryptedRecord1: EncryptedRecord = mockk()
        val encryptedRecord2: EncryptedRecord = mockk()
        val decryptedRecord1: DecryptedFhir3Record<Fhir3CarePlan> = mockk(relaxed = true)
        val decryptedRecord2: DecryptedFhir3Record<Fhir3CarePlan> = mockk(relaxed = true)
        val record1: Record<Fhir3CarePlan> = mockk()
        val record2: Record<Fhir3CarePlan> = mockk()
        val startDate: LocalDate = mockk()
        val start = "start"
        val endDate: LocalDate = mockk()
        val end = "end"
        val offset = 42
        val pageSize = 23
        val encryptedRecords = listOf(encryptedRecord1, encryptedRecord2)

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

        every { SdkDateTimeFormatter.formatDate(startDate) } returns start
        every { SdkDateTimeFormatter.formatDate(endDate) } returns end
        every { taggingService.getTagsFromType(Fhir3CarePlan::class.java as Class<Any>) } returns tags
        every {
            compatibilityService.searchRecords(
                    ALIAS,
                    USER_ID,
                    start,
                    end,
                    pageSize,
                    offset,
                    tags,
                    defaultAnnotation
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
        val observer = recordService.fetchFhir3Records(
                USER_ID,
                Fhir3CarePlan::class.java,
                startDate,
                endDate,
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
            recordService.fetchFhir3Records(
                    USER_ID,
                    Fhir3CarePlan::class.java,
                    emptyList(),
                    startDate,
                    endDate,
                    pageSize,
                    offset
            )
            SdkDateTimeFormatter.formatDate(startDate)
            SdkDateTimeFormatter.formatDate(endDate)
            taggingService.getTagsFromType(Fhir3CarePlan::class.java as Class<Any>)
            compatibilityService.searchRecords(
                    ALIAS,
                    USER_ID,
                    start,
                    end,
                    pageSize,
                    offset,
                    tags,
                    defaultAnnotation
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

        unmockkObject(SdkDateTimeFormatter)
    }

    @Test
    fun `Given, fetchFhir4Records called with a UserId, a ResourceType, a nulled StartDate, a nulled EndDate, the PageSize and Offset, it returns List of Fhir4Records`() {
        // Given
        mockkObject(SdkDateTimeFormatter)

        val resource1: Fhir4CarePlan = mockk()
        val id1 = "id1"
        val resource2: Fhir4CarePlan = mockk()
        val id2 = "id2"
        val encryptedRecord1: EncryptedRecord = mockk()
        val encryptedRecord2: EncryptedRecord = mockk()
        val decryptedRecord1: DecryptedFhir4Record<Fhir4CarePlan> = mockk(relaxed = true)
        val decryptedRecord2: DecryptedFhir4Record<Fhir4CarePlan> = mockk(relaxed = true)
        val record1: Fhir4Record<Fhir4CarePlan> = mockk()
        val record2: Fhir4Record<Fhir4CarePlan> = mockk()
        val offset = 42
        val pageSize = 23
        val encryptedRecords = listOf(encryptedRecord1, encryptedRecord2)


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

        every { taggingService.getTagsFromType(Fhir4CarePlan::class.java as Class<Any>) } returns tags
        every {
            compatibilityService.searchRecords(
                    ALIAS,
                    USER_ID,
                    null,
                    null,
                    pageSize,
                    offset,
                    tags,
                    defaultAnnotation
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
        val observer = recordService.fetchFhir4Records(
                USER_ID,
                Fhir4CarePlan::class.java,
                emptyList(),
                null,
                null,
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
            taggingService.getTagsFromType(Fhir4CarePlan::class.java as Class<Any>)
            compatibilityService.searchRecords(
                    ALIAS,
                    USER_ID,
                    null,
                    null,
                    pageSize,
                    offset,
                    tags,
                    defaultAnnotation
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
        verify(exactly = 0) { SdkDateTimeFormatter.formatDate(any()) }

        unmockkObject(SdkDateTimeFormatter)
    }

    @Test
    fun `Given, fetchFhir4Records called with a UserId, a ResourceType, a StartDate, a EndDate, the PageSize and Offset, it returns List of Fhir4Records`() {
        // Given
        mockkObject(SdkDateTimeFormatter)

        val resource1: Fhir4CarePlan = mockk()
        val id1 = "id1"
        val resource2: Fhir4CarePlan = mockk()
        val id2 = "id2"
        val encryptedRecord1: EncryptedRecord = mockk()
        val encryptedRecord2: EncryptedRecord = mockk()
        val decryptedRecord1: DecryptedFhir4Record<Fhir4CarePlan> = mockk(relaxed = true)
        val decryptedRecord2: DecryptedFhir4Record<Fhir4CarePlan> = mockk(relaxed = true)
        val record1: Fhir4Record<Fhir4CarePlan> = mockk()
        val record2: Fhir4Record<Fhir4CarePlan> = mockk()
        val startDate: LocalDate = mockk()
        val start = "start"
        val endDate: LocalDate = mockk()
        val end = "end"
        val offset = 42
        val pageSize = 23
        val encryptedRecords = listOf(encryptedRecord1, encryptedRecord2)

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

        every { SdkDateTimeFormatter.formatDate(startDate) } returns start
        every { SdkDateTimeFormatter.formatDate(endDate) } returns end
        every { taggingService.getTagsFromType(Fhir4CarePlan::class.java as Class<Any>) } returns tags
        every {
            compatibilityService.searchRecords(
                    ALIAS,
                    USER_ID,
                    start,
                    end,
                    pageSize,
                    offset,
                    tags,
                    defaultAnnotation
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
        val observer = recordService.fetchFhir4Records(
                USER_ID,
                Fhir4CarePlan::class.java,
                defaultAnnotation,
                startDate,
                endDate,
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
            recordService.fetchFhir4Records(
                    USER_ID,
                    Fhir4CarePlan::class.java,
                    emptyList(),
                    startDate,
                    endDate,
                    pageSize,
                    offset
            )
            SdkDateTimeFormatter.formatDate(startDate)
            SdkDateTimeFormatter.formatDate(endDate)
            taggingService.getTagsFromType(Fhir4CarePlan::class.java as Class<Any>)
            compatibilityService.searchRecords(
                    ALIAS,
                    USER_ID,
                    start,
                    end,
                    pageSize,
                    offset,
                    tags,
                    defaultAnnotation
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

        unmockkObject(SdkDateTimeFormatter)
    }
}
