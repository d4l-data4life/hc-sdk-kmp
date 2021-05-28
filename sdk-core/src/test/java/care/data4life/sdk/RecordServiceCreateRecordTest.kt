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
import care.data4life.sdk.migration.MigrationContract
import care.data4life.sdk.model.ModelContract.BaseRecord
import care.data4life.sdk.model.Record
import care.data4life.sdk.model.RecordMapper
import care.data4life.sdk.network.NetworkingContract
import care.data4life.sdk.network.model.DecryptedDataRecord
import care.data4life.sdk.network.model.EncryptedRecord
import care.data4life.sdk.network.model.NetworkModelContract.DecryptedFhir3Record
import care.data4life.sdk.network.model.NetworkModelContract.DecryptedFhir4Record
import care.data4life.sdk.resource.ResourceContract.DataResource
import care.data4life.sdk.resource.Fhir3Resource
import care.data4life.sdk.resource.Fhir4Resource
import care.data4life.sdk.resource.ResourceContract
import care.data4life.sdk.tag.Annotations
import care.data4life.sdk.tag.TaggingContract
import care.data4life.sdk.test.util.GenericTestDataProvider.ALIAS
import care.data4life.sdk.test.util.GenericTestDataProvider.PARTNER_ID
import care.data4life.sdk.test.util.GenericTestDataProvider.USER_ID
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.spyk
import io.mockk.unmockkObject
import io.mockk.verify
import io.mockk.verifyOrder
import io.reactivex.Single
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue

class RecordServiceCreateRecordTest {
    private lateinit var recordService: RecordService
    private val apiService: NetworkingContract.Service = mockk()
    private val cryptoService: CryptoContract.Service = mockk()
    private val resourceCryptoService: ResourceContract.CryptoService = mockk()
    private val tagCryptoService: TaggingContract.CryptoService = mockk()
    private val taggingService: TaggingContract.Service = mockk()
    private val attachmentService: AttachmentContract.Service = mockk()
    private val errorHandler: SdkContract.ErrorHandler = mockk()

    private val defaultAnnotation: List<String> = emptyList()

    private val compatibilityService: MigrationContract.CompatibilityService = mockk()
    private val encryptedRecord: EncryptedRecord = mockk()

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

    // FHIR3
    @Test
    fun `Given, createRecord is called with a Fhir3 resource and a UserId, it returns a new Record`() {
        // Given
        val resource: Fhir3Resource = mockk(relaxed = true)
        val createdRecord: DecryptedFhir3Record<Fhir3Resource> = mockk()
        val receivedRecord: DecryptedFhir3Record<Fhir3Resource> = mockk(relaxed = true)
        val record: Record<Fhir3Resource> = mockk()
        val identifier = "id"

        every { createdRecord.resource } returns resource
        every { receivedRecord.resource } returns resource
        every { receivedRecord.identifier } returns identifier

        every {
            recordService.fromResource(resource, defaultAnnotation)
        } returns Single.just(createdRecord)

        every { recordService.encryptRecord(createdRecord) } returns encryptedRecord
        every {
            apiService.createRecord(
                ALIAS,
                USER_ID,
                encryptedRecord
            )
        } returns Single.just(encryptedRecord)
        every {
            recordService.decryptRecord<Fhir3Resource>(
                encryptedRecord,
                USER_ID
            )
        } returns receivedRecord

        @Suppress("UNCHECKED_CAST")
        every { RecordMapper.getInstance(receivedRecord) } returns record as BaseRecord<Fhir3Resource>

        // When
        val subscriber = recordService.createRecord(
            USER_ID,
            resource,
            defaultAnnotation
        ).test().await()

        // Then
        val result = subscriber
            .assertNoErrors()
            .assertComplete()
            .assertValueCount(1)
            .values()[0]

        assertSame(
            actual = result,
            expected = record
        )
        verifyOrder {
            recordService.createRecord(USER_ID, resource, defaultAnnotation)
            recordService.checkDataRestrictions(resource)
            recordService.extractUploadData(resource)
            recordService.fromResource(resource, defaultAnnotation)
            recordService.uploadData(createdRecord, USER_ID)
            recordService.removeUploadData(createdRecord)
            recordService.encryptRecord(createdRecord)
            apiService.createRecord(
                ALIAS,
                USER_ID,
                encryptedRecord
            )
            recordService.decryptRecord<Fhir3Resource>(encryptedRecord, USER_ID)
            recordService.restoreUploadData(
                receivedRecord,
                resource,
                null
            )
            resource.id = receivedRecord.identifier
            RecordMapper.getInstance(receivedRecord)
        }
    }

    @Test
    fun `Given, createRecord is called with a Fhir3 resource, which contains an attachment, and a UserId, it returns a new Record`() {
        // Given
        val resource: Fhir3Resource = mockk(relaxed = true)
        val createdRecord: DecryptedFhir3Record<Fhir3Resource> = mockk()
        val receivedRecord: DecryptedFhir3Record<Fhir3Resource> = mockk(relaxed = true)
        val record: Record<Fhir3Resource> = mockk()
        val uploadData: HashMap<Any, String?> = mockk()
        val identifier = "id"

        every { createdRecord.resource } returns resource
        every { receivedRecord.resource } returns resource
        every { record.identifier } returns identifier

        every {
            recordService.fromResource(resource, defaultAnnotation)
        } returns Single.just(createdRecord)

        every { recordService.extractUploadData(resource) } returns uploadData
        every { recordService.encryptRecord(createdRecord) } returns encryptedRecord
        every {
            apiService.createRecord(
                ALIAS,
                USER_ID,
                encryptedRecord
            )
        } returns Single.just(encryptedRecord)
        every {
            recordService.decryptRecord<Fhir3Resource>(
                encryptedRecord,
                USER_ID
            )
        } returns receivedRecord

        @Suppress("UNCHECKED_CAST")
        every { RecordMapper.getInstance(receivedRecord) } returns record as BaseRecord<Fhir3Resource>

        // When
        val subscriber = recordService.createRecord(
            USER_ID,
            resource,
            defaultAnnotation
        ).test().await()

        // Then
        val result = subscriber
            .assertNoErrors()
            .assertComplete()
            .assertValueCount(1)
            .values()[0]

        assertSame(
            actual = result,
            expected = record
        )
        verifyOrder {
            recordService.createRecord(USER_ID, resource, defaultAnnotation)
            recordService.checkDataRestrictions(resource)
            recordService.extractUploadData(resource)
            recordService.fromResource(resource, defaultAnnotation)
            recordService.uploadData(createdRecord, USER_ID)
            recordService.removeUploadData(createdRecord)
            recordService.encryptRecord(createdRecord)
            apiService.createRecord(
                ALIAS,
                USER_ID,
                encryptedRecord
            )
            recordService.decryptRecord<Fhir3Resource>(encryptedRecord, USER_ID)
            recordService.restoreUploadData(
                receivedRecord,
                resource,
                uploadData
            )
            resource.id = receivedRecord.identifier
            RecordMapper.getInstance(receivedRecord)
        }
    }

    @Test
    fun `Given, createRecords is called a Fhir3 resource and a UserId and Annotations, it returns a new Record`() {
        // Given
        val resource: Fhir3Resource = mockk(relaxed = true)
        val createdRecord: DecryptedFhir3Record<Fhir3Resource> = mockk()
        val receivedRecord: DecryptedFhir3Record<Fhir3Resource> = mockk(relaxed = true)
        val record: Record<Fhir3Resource> = mockk()
        val identifier = "id"
        val annotations: Annotations = mockk()

        every { createdRecord.resource } returns resource
        every { receivedRecord.resource } returns resource
        every { receivedRecord.identifier } returns identifier

        every {
            recordService.fromResource(resource, annotations)
        } returns Single.just(createdRecord)

        every { recordService.encryptRecord(createdRecord) } returns encryptedRecord
        every {
            apiService.createRecord(
                ALIAS,
                USER_ID,
                encryptedRecord
            )
        } returns Single.just(encryptedRecord)
        every {
            recordService.decryptRecord<Fhir3Resource>(
                encryptedRecord,
                USER_ID
            )
        } returns receivedRecord

        @Suppress("UNCHECKED_CAST")
        every { RecordMapper.getInstance(receivedRecord) } returns record as BaseRecord<Fhir3Resource>

        // When
        val subscriber = recordService.createRecord(USER_ID, resource, annotations).test().await()

        // Then
        val result = subscriber
            .assertNoErrors()
            .assertComplete()
            .assertValueCount(1)
            .values()[0]

        assertSame(
            actual = result,
            expected = record
        )
        verifyOrder {
            recordService.createRecord(USER_ID, resource, annotations)
            recordService.checkDataRestrictions(resource)
            recordService.extractUploadData(resource)
            recordService.fromResource(resource, annotations)
            recordService.uploadData(createdRecord, USER_ID)
            recordService.removeUploadData(createdRecord)
            recordService.encryptRecord(createdRecord)
            apiService.createRecord(
                ALIAS,
                USER_ID,
                encryptedRecord
            )
            recordService.decryptRecord<Fhir3Resource>(encryptedRecord, USER_ID)
            recordService.restoreUploadData(
                receivedRecord,
                resource,
                null
            )
            resource.id = receivedRecord.identifier
            RecordMapper.getInstance(receivedRecord)
        }
    }

    // FHIR4
    @Test
    fun `Given, createRecord is called with a Fhir4 resource and a UserId, it returns a new Record`() {
        // Given
        val resource: Fhir4Resource = mockk(relaxed = true)
        val createdRecord: DecryptedFhir4Record<Fhir4Resource> = mockk()
        val receivedRecord: DecryptedFhir4Record<Fhir4Resource> = mockk(relaxed = true)
        val record: Fhir4Record<Fhir4Resource> = mockk()
        val identifier = "id"

        every { createdRecord.resource } returns resource
        every { receivedRecord.resource } returns resource
        every { receivedRecord.identifier } returns identifier

        every {
            recordService.fromResource(resource, defaultAnnotation)
        } returns Single.just(createdRecord)

        every { recordService.encryptRecord(createdRecord) } returns encryptedRecord
        every {
            apiService.createRecord(
                ALIAS,
                USER_ID,
                encryptedRecord
            )
        } returns Single.just(encryptedRecord)
        every {
            recordService.decryptRecord<Fhir4Resource>(
                encryptedRecord,
                USER_ID
            )
        } returns receivedRecord

        @Suppress("UNCHECKED_CAST")
        every { RecordMapper.getInstance(receivedRecord) } returns record as BaseRecord<Fhir4Resource>

        // When
        val subscriber = recordService.createRecord(
            USER_ID,
            resource,
            defaultAnnotation
        ).test().await()

        // Then
        val result = subscriber
            .assertNoErrors()
            .assertComplete()
            .assertValueCount(1)
            .values()[0]

        assertSame(
            actual = result,
            expected = record
        )
        verifyOrder {
            recordService.createRecord(USER_ID, resource, defaultAnnotation)
            recordService.checkDataRestrictions(resource)
            recordService.extractUploadData(resource)
            recordService.fromResource(resource, defaultAnnotation)
            recordService.uploadData(createdRecord, USER_ID)
            recordService.removeUploadData(createdRecord)
            recordService.encryptRecord(createdRecord)
            apiService.createRecord(
                ALIAS,
                USER_ID,
                encryptedRecord
            )
            recordService.decryptRecord<Fhir4Resource>(encryptedRecord, USER_ID)
            recordService.restoreUploadData(
                receivedRecord,
                resource,
                null
            )
            resource.id = receivedRecord.identifier
            RecordMapper.getInstance(receivedRecord)
        }
    }

    @Test
    fun `Given, createRecord is called with a Fhir4 resource, which contains an attachment, and a UserId, it returns a new Record`() {
        // Given
        val resource: Fhir4Resource = mockk(relaxed = true)
        val createdRecord: DecryptedFhir4Record<Fhir4Resource> = mockk()
        val receivedRecord: DecryptedFhir4Record<Fhir4Resource> = mockk(relaxed = true)
        val record: Fhir4Record<Fhir4Resource> = mockk()
        val uploadData: HashMap<Any, String?> = mockk()
        val identifier = "id"

        every { createdRecord.resource } returns resource
        every { receivedRecord.resource } returns resource
        every { record.identifier } returns identifier

        every {
            recordService.fromResource(resource, defaultAnnotation)
        } returns Single.just(createdRecord)

        every { recordService.extractUploadData(resource) } returns uploadData
        every { recordService.encryptRecord(createdRecord) } returns encryptedRecord
        every {
            apiService.createRecord(
                ALIAS,
                USER_ID,
                encryptedRecord
            )
        } returns Single.just(encryptedRecord)
        every {
            recordService.decryptRecord<Fhir4Resource>(
                encryptedRecord,
                USER_ID
            )
        } returns receivedRecord

        @Suppress("UNCHECKED_CAST")
        every { RecordMapper.getInstance(receivedRecord) } returns record as BaseRecord<Fhir4Resource>

        // When
        val subscriber = recordService.createRecord(
            USER_ID,
            resource,
            defaultAnnotation
        ).test().await()

        // Then
        val result = subscriber
            .assertNoErrors()
            .assertComplete()
            .assertValueCount(1)
            .values()[0]

        assertSame(
            actual = result,
            expected = record
        )
        verifyOrder {
            recordService.createRecord(USER_ID, resource, defaultAnnotation)
            recordService.checkDataRestrictions(resource)
            recordService.extractUploadData(resource)
            recordService.fromResource(resource, defaultAnnotation)
            recordService.uploadData(createdRecord, USER_ID)
            recordService.removeUploadData(createdRecord)
            recordService.encryptRecord(createdRecord)
            apiService.createRecord(
                ALIAS,
                USER_ID,
                encryptedRecord
            )
            recordService.decryptRecord<Fhir4Resource>(encryptedRecord, USER_ID)
            recordService.restoreUploadData(
                receivedRecord,
                resource,
                uploadData
            )
            resource.id = receivedRecord.identifier
            RecordMapper.getInstance(receivedRecord)
        }
    }

    @Test
    fun `Given, createRecords is called a Fhir4 resource and a UserId and Annotations, it returns a new Record`() {
        // Given
        val resource: Fhir4Resource = mockk(relaxed = true)
        val createdRecord: DecryptedFhir4Record<Fhir4Resource> = mockk()
        val receivedRecord: DecryptedFhir4Record<Fhir4Resource> = mockk(relaxed = true)
        val record: Fhir4Record<Fhir4Resource> = mockk()
        val identifier = "id"
        val annotations: Annotations = mockk()

        every { createdRecord.resource } returns resource
        every { receivedRecord.resource } returns resource
        every { receivedRecord.identifier } returns identifier

        every {
            recordService.fromResource(resource, annotations)
        } returns Single.just(createdRecord)

        every { recordService.encryptRecord(createdRecord) } returns encryptedRecord
        every {
            apiService.createRecord(
                ALIAS,
                USER_ID,
                encryptedRecord
            )
        } returns Single.just(encryptedRecord)
        every {
            recordService.decryptRecord<Fhir4Resource>(
                encryptedRecord,
                USER_ID
            )
        } returns receivedRecord

        @Suppress("UNCHECKED_CAST")
        every { RecordMapper.getInstance(receivedRecord) } returns record as BaseRecord<Fhir4Resource>

        // When
        val subscriber = recordService.createRecord(USER_ID, resource, annotations).test().await()

        // Then
        val result = subscriber
            .assertNoErrors()
            .assertComplete()
            .assertValueCount(1)
            .values()[0]

        assertSame(
            actual = result,
            expected = record
        )
        verifyOrder {
            recordService.createRecord(USER_ID, resource, annotations)
            recordService.checkDataRestrictions(resource)
            recordService.extractUploadData(resource)
            recordService.fromResource(resource, annotations)
            recordService.uploadData(createdRecord, USER_ID)
            recordService.removeUploadData(createdRecord)
            recordService.encryptRecord(createdRecord)
            apiService.createRecord(
                ALIAS,
                USER_ID,
                encryptedRecord
            )
            recordService.decryptRecord<Fhir4Resource>(encryptedRecord, USER_ID)
            recordService.restoreUploadData(
                receivedRecord,
                resource,
                null
            )
            resource.id = receivedRecord.identifier
            RecordMapper.getInstance(receivedRecord)
        }
    }

    // Arbitrary Data
    @Test
    fun `Given, createRecord is called with a DataResource and a UserId, it returns a new Record`() {
        // Given
        val resource: DataResource = mockk()
        val createdRecord: DecryptedDataRecord = mockk()
        val receivedRecord: DecryptedDataRecord = mockk(relaxed = true)
        val record: DataRecord<DataResource> = mockk()

        every { createdRecord.resource } returns resource
        every { receivedRecord.resource } returns resource

        every {
            recordService.fromResource(resource, defaultAnnotation)
        } returns Single.just(createdRecord)

        every { recordService.encryptRecord(createdRecord) } returns encryptedRecord
        every {
            apiService.createRecord(
                ALIAS,
                USER_ID,
                encryptedRecord
            )
        } returns Single.just(encryptedRecord)
        every {
            recordService.decryptRecord<DataResource>(
                encryptedRecord,
                USER_ID
            )
        } returns receivedRecord

        @Suppress("UNCHECKED_CAST")
        every { RecordMapper.getInstance(receivedRecord) } returns record as BaseRecord<DataResource>

        // When
        val subscriber = recordService.createRecord(
            USER_ID,
            resource,
            defaultAnnotation
        ).test().await()

        // Then
        val result = subscriber
            .assertNoErrors()
            .assertComplete()
            .assertValueCount(1)
            .values()[0]

        assertSame(
            actual = result,
            expected = record
        )
        verifyOrder {
            recordService.createRecord(USER_ID, resource, defaultAnnotation)
            recordService.checkDataRestrictions(resource)
            recordService.extractUploadData(resource)
            recordService.fromResource(resource, defaultAnnotation)
            recordService.uploadData(createdRecord, USER_ID)
            recordService.removeUploadData(createdRecord)
            recordService.encryptRecord(createdRecord)
            apiService.createRecord(
                ALIAS,
                USER_ID,
                encryptedRecord
            )
            recordService.decryptRecord<Fhir3Resource>(encryptedRecord, USER_ID)
            recordService.restoreUploadData(
                receivedRecord,
                resource,
                null
            )
            RecordMapper.getInstance(receivedRecord)
        }

        verify(exactly = 0) { receivedRecord.identifier = any() }
    }

    @Test
    fun `Given, createRecords is called a DataResource and a UserId and Annotations, it returns a new Record`() {
        // Given
        val resource: DataResource = mockk()
        val createdRecord: DecryptedDataRecord = mockk()
        val receivedRecord: DecryptedDataRecord = mockk(relaxed = true)
        val record: DataRecord<DataResource> = mockk()
        val annotations: Annotations = mockk()

        every { createdRecord.resource } returns resource
        every { receivedRecord.resource } returns resource

        every {
            recordService.fromResource(resource, annotations)
        } returns Single.just(createdRecord)

        every { recordService.encryptRecord(createdRecord) } returns encryptedRecord
        every {
            apiService.createRecord(
                ALIAS,
                USER_ID,
                encryptedRecord
            )
        } returns Single.just(encryptedRecord)
        every {
            recordService.decryptRecord<DataResource>(
                encryptedRecord,
                USER_ID
            )
        } returns receivedRecord

        @Suppress("UNCHECKED_CAST")
        every { RecordMapper.getInstance(receivedRecord) } returns record as BaseRecord<DataResource>

        // When
        val subscriber = recordService.createRecord(USER_ID, resource, annotations).test().await()

        // Then
        val result = subscriber
            .assertNoErrors()
            .assertComplete()
            .assertValueCount(1)
            .values()[0]

        assertSame(
            actual = result,
            expected = record
        )
        verifyOrder {
            recordService.createRecord(USER_ID, resource, annotations)
            recordService.checkDataRestrictions(resource)
            recordService.extractUploadData(resource)
            recordService.fromResource(resource, annotations)
            recordService.uploadData(createdRecord, USER_ID)
            recordService.removeUploadData(createdRecord)
            recordService.encryptRecord(createdRecord)
            apiService.createRecord(
                ALIAS,
                USER_ID,
                encryptedRecord
            )
            recordService.decryptRecord<Fhir3Resource>(encryptedRecord, USER_ID)
            recordService.restoreUploadData(
                receivedRecord,
                resource,
                null
            )
            RecordMapper.getInstance(receivedRecord)
        }

        verify(exactly = 0) { receivedRecord.identifier = any() }
    }

    // Batch API
    // FHIR3
    @Test
    fun `Given, createRecords is called with multiple Fhir3 resources and a UserId, it returns a multiple Records`() {
        val resources = listOf<Fhir3Resource>(
            mockk(),
            mockk()
        )

        val expected = listOf<Record<Fhir3Resource>>(
            mockk(),
            mockk()
        )

        val returnValues = listOf(
            Single.just(expected[0]),
            Single.just(expected[1])
        )

        every {
            recordService.createRecord(
                USER_ID,
                or(resources[0], resources[1]),
                defaultAnnotation
            )
        } returnsMany returnValues

        // When
        val observer = recordService.createRecords(resources, USER_ID).test().await()

        // Then
        val result = observer
            .assertNoErrors()
            .assertComplete()
            .assertValueCount(1)
            .values()[0]

        assertTrue(result.failedOperations.isEmpty())
        assertEquals(
            actual = result.successfulOperations.size,
            expected = 2
        )
        assertSame(
            actual = result.successfulOperations[0],
            expected = expected[0]
        )
        assertSame(
            actual = result.successfulOperations[1],
            expected = expected[1]
        )

        verifyOrder {
            recordService.createRecords(resources, USER_ID)
            recordService.createRecord(
                USER_ID,
                or(resources[0], resources[1]),
                defaultAnnotation
            )
            recordService.createRecord(
                USER_ID,
                or(resources[0], resources[1]),
                defaultAnnotation
            )
        }
    }
}
