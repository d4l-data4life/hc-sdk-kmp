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
import care.data4life.sdk.lang.DataValidationException
import care.data4life.sdk.migration.MigrationContract
import care.data4life.sdk.model.Record
import care.data4life.sdk.model.RecordMapper
import care.data4life.sdk.model.ModelContract.BaseRecord
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
import io.reactivex.Observable
import io.reactivex.Single
import org.junit.After
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.threeten.bp.LocalDate
import java.io.IOException
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

    // mark
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
    }

    @After
    fun tearDown() {
        unmockkObject(RecordMapper)
    }

    @Test
    @Throws(
        InterruptedException::class,
        IOException::class,
        DataValidationException.ModelVersionNotSupported::class
    )
    fun `Given, fetchFhir3Record is called with a RecordId and UserId for an Fhir3Record, it returns a Record`() {
        // Given
        val expected: Record<Fhir3CarePlan> = mockk()
        val decrypted: DecryptedFhir3Record<Fhir3Resource> = mockk()
        mockkObject(RecordMapper)

        every { apiService.fetchRecord(ALIAS, USER_ID, RECORD_ID) } returns Single.just(
            encryptedRecord
        )
        every {
            recordService.decryptRecord<Fhir3Resource>(
                encryptedRecord,
                USER_ID
            )
        } returns decrypted
        every { recordService.assignResourceId(decrypted) } returns decrypted
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
        verify(exactly = 1) { apiService.fetchRecord(ALIAS, USER_ID, RECORD_ID) }
        verify(exactly = 1) { recordService.decryptRecord<Fhir3CarePlan>(encryptedRecord, USER_ID) }
        verify(exactly = 1) { recordService.assignResourceId(decrypted) }
        verify(exactly = 1) { RecordMapper.getInstance(decrypted) }
    }

    @Test
    @Throws(
        InterruptedException::class,
        IOException::class,
        DataValidationException.ModelVersionNotSupported::class
    )
    fun `Given, fetchFhir4Record is called with a RecordId and UserId for an Fhir4Record, it returns a Record`() {
        // Given
        val expected: Fhir4Record<Fhir4CarePlan> = mockk()
        val decrypted: DecryptedFhir4Record<Fhir4Resource> = mockk()
        mockkObject(RecordMapper)

        every { apiService.fetchRecord(ALIAS, USER_ID, RECORD_ID) } returns Single.just(
            encryptedRecord
        )
        every {
            recordService.decryptRecord<Fhir4Resource>(
                encryptedRecord,
                USER_ID
            )
        } returns decrypted
        every { recordService.assignResourceId(decrypted) } returns decrypted
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
        verify(exactly = 1) { apiService.fetchRecord(ALIAS, USER_ID, RECORD_ID) }
        verify(exactly = 1) { recordService.decryptRecord<Fhir4Resource>(encryptedRecord, USER_ID) }
        verify(exactly = 1) { recordService.assignResourceId(decrypted) }
        verify(exactly = 1) { RecordMapper.getInstance(decrypted) }
    }

    @Test
    @Throws(
        InterruptedException::class,
        IOException::class,
        DataValidationException.ModelVersionNotSupported::class
    )
    fun `Given, fetchDataRecord is called with a RecordId and UserId for an DataRecord, it returns a Record`() {
        // Given
        val expected: DataRecord<DataResource> = mockk()
        val decrypted: DecryptedDataRecord = mockk()
        mockkObject(RecordMapper)

        every { apiService.fetchRecord(ALIAS, USER_ID, RECORD_ID) } returns Single.just(
            encryptedRecord
        )
        every {
            recordService.decryptRecord<DataResource>(encryptedRecord, USER_ID)
        } returns decrypted as DecryptedBaseRecord<DataResource>
        every { recordService.assignResourceId(decrypted) } returns decrypted
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
        verify(exactly = 1) { apiService.fetchRecord(ALIAS, USER_ID, RECORD_ID) }
        verify(exactly = 1) { recordService.decryptRecord<DataResource>(encryptedRecord, USER_ID) }
        verify(exactly = 1) { recordService.assignResourceId(decrypted) }
        verify(exactly = 1) { RecordMapper.getInstance(decrypted) }
    }

    @Test
    @Throws(InterruptedException::class)
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
    @Throws(InterruptedException::class)
    fun `Given, fetchFhir3Records is called with multiple RecordIds and a UserId, it ignores errors`() {
        //Given
        val ids = listOf("1", "2", "3")
        val record1: Record<Fhir3CarePlan> = mockk()
        val record3: Record<Fhir3CarePlan> = mockk()
        val decrypted: DecryptedFhir3Record<Fhir3CarePlan> = mockk()
        val thrownError = RuntimeException("error")
        val expectedError: D4LException = mockk()

        mockkObject(RecordMapper)

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
        every { recordService.assignResourceId(decrypted) } returns decrypted
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
    @Throws(
        InterruptedException::class,
        IOException::class,
        DataValidationException.ModelVersionNotSupported::class
    )
    fun `Given, fetchFhir3Records called with a UserId, a ResourceType, a nulled StartDate, a nulled EndDate, the PageSize and Offset, it returns List of Records`() {
        // Given
        val resource1: Fhir3CarePlan = mockk()
        val resource2: Fhir3CarePlan = mockk()
        val encryptedRecord1: EncryptedRecord = mockk()
        val encryptedRecord2: EncryptedRecord = mockk()
        val decryptedRecord1: DecryptedFhir3Record<Fhir3CarePlan> = mockk(relaxed = true)
        val decryptedRecord2: DecryptedFhir3Record<Fhir3CarePlan> = mockk(relaxed = true)
        val record1: Record<Fhir3CarePlan> = mockk()
        val record2: Record<Fhir3CarePlan> = mockk()
        val offset = 42
        val pageSize = 23
        val encryptedRecords = listOf(encryptedRecord1, encryptedRecord2)
        mockkObject(RecordMapper)
        mockkObject(SdkDateTimeFormatter)

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
            hint(Fhir3CarePlan::class)
            decryptedRecord1.resource
        } returns resource1
        every { decryptedRecord1.annotations } returns defaultAnnotation
        every {
            hint(Fhir3CarePlan::class)
            decryptedRecord2.resource
        } returns resource2
        every { decryptedRecord2.annotations } returns defaultAnnotation
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
        verify(exactly = 1) { taggingService.getTagsFromType(Fhir3CarePlan::class.java as Class<Any>) }
        verify(exactly = 0) { SdkDateTimeFormatter.formatDate(any()) }
        verify(exactly = 1) {
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
        }
        verify(exactly = 1) {
            recordService.decryptRecord<Fhir3CarePlan>(
                encryptedRecord1,
                USER_ID
            )
        }
        verify(exactly = 1) {
            recordService.decryptRecord<Fhir3CarePlan>(
                encryptedRecord2,
                USER_ID
            )
        }
        verify(exactly = 1) { RecordMapper.getInstance(decryptedRecord1) }
        verify(exactly = 1) { RecordMapper.getInstance(decryptedRecord2) }

        unmockkObject(RecordMapper)
        unmockkObject(SdkDateTimeFormatter)
    }

    @Test
    @Throws(
        InterruptedException::class,
        IOException::class,
        DataValidationException.ModelVersionNotSupported::class
    )
    fun `Given, fetchFhir3Records called with a UserId, a ResourceType, a StartDate, a EndDate, the PageSize and Offset, it returns List of Records`() {
        // Given
        val resource1: Fhir3CarePlan = mockk()
        val resource2: Fhir3CarePlan = mockk()
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
        mockkObject(RecordMapper)
        mockkObject(SdkDateTimeFormatter)

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
            hint(Fhir3CarePlan::class)
            decryptedRecord1.resource
        } returns resource1
        every { decryptedRecord1.annotations } returns defaultAnnotation
        every {
            hint(Fhir3CarePlan::class)
            decryptedRecord2.resource
        } returns resource2
        every { decryptedRecord2.annotations } returns defaultAnnotation
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
        verify(exactly = 1) { taggingService.getTagsFromType(Fhir3CarePlan::class.java as Class<Any>) }
        verify(exactly = 2) { SdkDateTimeFormatter.formatDate(or(startDate, endDate)) }
        verify(exactly = 1) {
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
        }
        verify(exactly = 1) {
            recordService.decryptRecord<Fhir3CarePlan>(
                encryptedRecord1,
                USER_ID
            )
        }
        verify(exactly = 1) {
            recordService.decryptRecord<Fhir3CarePlan>(
                encryptedRecord2,
                USER_ID
            )
        }
        verify(exactly = 1) { RecordMapper.getInstance(decryptedRecord1) }
        verify(exactly = 1) { RecordMapper.getInstance(decryptedRecord2) }

        unmockkObject(RecordMapper)
        unmockkObject(SdkDateTimeFormatter)
    }

    @Test
    @Throws(
        InterruptedException::class,
        IOException::class,
        DataValidationException.ModelVersionNotSupported::class
    )
    fun `Given, fetchFhir4Records called with a UserId, a ResourceType, a nulled StartDate, a nulled EndDate, the PageSize and Offset, it returns List of Fhir4Records`() {
        // Given
        val resource1: Fhir4CarePlan = mockk()
        val resource2: Fhir4CarePlan = mockk()
        val encryptedRecord1: EncryptedRecord = mockk()
        val encryptedRecord2: EncryptedRecord = mockk()
        val decryptedRecord1: DecryptedFhir4Record<Fhir4CarePlan> = mockk(relaxed = true)
        val decryptedRecord2: DecryptedFhir4Record<Fhir4CarePlan> = mockk(relaxed = true)
        val record1: Fhir4Record<Fhir4CarePlan> = mockk()
        val record2: Fhir4Record<Fhir4CarePlan> = mockk()
        val offset = 42
        val pageSize = 23
        val encryptedRecords = listOf(encryptedRecord1, encryptedRecord2)
        mockkObject(RecordMapper)
        mockkObject(SdkDateTimeFormatter)

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
        every {
            hint(Fhir4CarePlan::class)
            decryptedRecord1.resource
        } returns resource1
        every { decryptedRecord1.annotations } returns defaultAnnotation
        every {
            hint(Fhir4CarePlan::class)
            decryptedRecord2.resource
        } returns resource2
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
            listOf(),
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
        verify(exactly = 1) { taggingService.getTagsFromType(Fhir4CarePlan::class.java as Class<Any>) }
        verify(exactly = 0) { SdkDateTimeFormatter.formatDate(any()) }
        verify(exactly = 1) {
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
        }
        verify(exactly = 1) {
            recordService.decryptRecord<Fhir4CarePlan>(
                encryptedRecord1,
                USER_ID
            )
        }
        verify(exactly = 1) {
            recordService.decryptRecord<Fhir4CarePlan>(
                encryptedRecord2,
                USER_ID
            )
        }
        verify(exactly = 1) { RecordMapper.getInstance(decryptedRecord1) }
        verify(exactly = 1) { RecordMapper.getInstance(decryptedRecord2) }

        unmockkObject(RecordMapper)
        unmockkObject(SdkDateTimeFormatter)
    }

    @Test
    @Throws(
        InterruptedException::class,
        IOException::class,
        DataValidationException.ModelVersionNotSupported::class
    )
    fun `Given, fetchFhir4Records called with a UserId, a ResourceType, a StartDate, a EndDate, the PageSize and Offset, it returns List of Fhir4Records`() {
        // Given
        val resource1: Fhir4CarePlan = mockk()
        val resource2: Fhir4CarePlan = mockk()
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
        mockkObject(RecordMapper)
        mockkObject(SdkDateTimeFormatter)

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
            hint(Fhir4CarePlan::class)
            decryptedRecord1.resource
        } returns resource1
        every { decryptedRecord1.annotations } returns defaultAnnotation
        every {
            hint(Fhir4CarePlan::class)
            decryptedRecord2.resource
        } returns resource2
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
            listOf(),
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
        verify(exactly = 1) { taggingService.getTagsFromType(Fhir4CarePlan::class.java as Class<Any>) }
        verify(exactly = 2) { SdkDateTimeFormatter.formatDate(or(startDate, endDate)) }
        verify(exactly = 1) {
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
        }
        verify(exactly = 1) {
            recordService.decryptRecord<Fhir4CarePlan>(
                encryptedRecord1,
                USER_ID
            )
        }
        verify(exactly = 1) {
            recordService.decryptRecord<Fhir4CarePlan>(
                encryptedRecord2,
                USER_ID
            )
        }
        verify(exactly = 1) { RecordMapper.getInstance(decryptedRecord1) }
        verify(exactly = 1) { RecordMapper.getInstance(decryptedRecord2) }

        unmockkObject(RecordMapper)
        unmockkObject(SdkDateTimeFormatter)
    }
}
