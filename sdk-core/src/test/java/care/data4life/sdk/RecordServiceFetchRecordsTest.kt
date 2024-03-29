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

import care.data4life.fhir.r4.model.CarePlan as Fhir4CarePlan
import care.data4life.fhir.stu3.model.CarePlan as Fhir3CarePlan
import care.data4life.sdk.attachment.AttachmentContract
import care.data4life.sdk.call.DataRecord
import care.data4life.sdk.call.Fhir4Record
import care.data4life.sdk.crypto.CryptoContract
import care.data4life.sdk.data.DataResource
import care.data4life.sdk.fhir.Fhir3Resource
import care.data4life.sdk.fhir.Fhir4Resource
import care.data4life.sdk.fhir.FhirContract
import care.data4life.sdk.lang.D4LException
import care.data4life.sdk.migration.MigrationContract
import care.data4life.sdk.model.ModelContract.BaseRecord
import care.data4life.sdk.model.Record
import care.data4life.sdk.model.RecordMapper
import care.data4life.sdk.network.NetworkingContract
import care.data4life.sdk.network.model.DecryptedDataRecord
import care.data4life.sdk.network.model.EncryptedRecord
import care.data4life.sdk.network.model.NetworkModelContract.DecryptedBaseRecord
import care.data4life.sdk.network.model.NetworkModelInternalContract.DecryptedFhir3Record
import care.data4life.sdk.network.model.NetworkModelInternalContract.DecryptedFhir4Record
import care.data4life.sdk.tag.TaggingContract
import care.data4life.sdk.test.util.GenericTestDataProvider.ALIAS
import care.data4life.sdk.test.util.GenericTestDataProvider.PARTNER_ID
import care.data4life.sdk.test.util.GenericTestDataProvider.RECORD_ID
import care.data4life.sdk.test.util.GenericTestDataProvider.USER_ID
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.spyk
import io.mockk.unmockkObject
import io.mockk.verifyOrder
import io.reactivex.Single
import kotlin.test.assertEquals
import kotlin.test.assertSame
import org.junit.After
import org.junit.Before
import org.junit.Ignore
import org.junit.Test

class RecordServiceFetchRecordsTest {
    private lateinit var recordService: RecordService
    private val apiService: NetworkingContract.Service = mockk()
    private val cryptoService: CryptoContract.Service = mockk()
    private val resourceCryptoService: FhirContract.CryptoService = mockk()
    private val tagCryptoService: TaggingContract.CryptoService = mockk()
    private val taggingService: TaggingContract.Service = mockk()
    private val attachmentService: AttachmentContract.Service = mockk()
    private val errorHandler: SdkContract.ErrorHandler = mockk()

    private val compatibilityService: MigrationContract.CompatibilityService = mockk()
    private val encryptedRecord: EncryptedRecord = mockk(relaxed = true)

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

    // Batch
    @Test
    fun `Given, fetchFhir3Records is called with multiple RecordIds and a UserId, it returns a List of Records`() {
        // Given
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
        // Given
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
}
