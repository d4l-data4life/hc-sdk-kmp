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
import care.data4life.sdk.model.Record
import care.data4life.sdk.model.RecordMapper
import care.data4life.sdk.network.NetworkingContract
import care.data4life.sdk.network.model.DecryptedDataRecord
import care.data4life.sdk.network.model.DecryptedR4Record
import care.data4life.sdk.network.model.DecryptedRecord
import care.data4life.sdk.network.model.EncryptedRecord
import care.data4life.sdk.resource.Fhir3Resource
import care.data4life.sdk.resource.Fhir4Resource
import care.data4life.sdk.resource.ResourceContract
import care.data4life.sdk.resource.ResourceContract.DataResource
import care.data4life.sdk.tag.Annotations
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
import io.mockk.verify
import io.mockk.verifyOrder
import io.reactivex.Single
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

class RecordServiceUpdateRecordTest {
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
    fun `Given, updateRecord is called with a Fhir3 resource and a UserId, it returns a updated Record`() {
        // Given
        val resource: Fhir3Resource = mockk(relaxed = true)
        val fetchedRecord: EncryptedRecord = mockk()
        val decryptedFetchedRecord: DecryptedRecord<Fhir3Resource> = mockk(relaxed = true)
        val encryptedRecord: EncryptedRecord = mockk()
        val receivedRecord: EncryptedRecord = mockk()
        val receivedDecryptedRecord: DecryptedRecord<Fhir3Resource> = mockk(relaxed = true)
        val record: Record<Fhir3Resource> = mockk()
        val identifier = "id"

        every { receivedDecryptedRecord.identifier } returns identifier

        every {
            apiService.fetchRecord(
                ALIAS,
                USER_ID,
                RECORD_ID
            )
        } returns Single.just(fetchedRecord)
        every {
            recordService.decryptRecord<Fhir3Resource>(fetchedRecord, USER_ID)
        } returns decryptedFetchedRecord
        every {
            recordService.encryptRecord(decryptedFetchedRecord)
        } returns encryptedRecord
        every {
            apiService.updateRecord(
                ALIAS,
                USER_ID,
                RECORD_ID,
                encryptedRecord
            )
        } returns Single.just(receivedRecord)
        every {
            recordService.decryptRecord<Fhir3Resource>(receivedRecord, USER_ID)
        } returns receivedDecryptedRecord
        every { RecordMapper.getInstance(receivedDecryptedRecord) } returns record

        // When
        val observer = recordService.updateRecord(
            USER_ID,
            RECORD_ID,
            resource,
            defaultAnnotation
        ).test().await()

        // Then
        val result = observer.assertNoErrors()
            .assertComplete()
            .assertValueCount(1)
            .values()[0]

        assertSame(
            actual = result,
            expected = record
        )

        verifyOrder {
            recordService.updateRecord(USER_ID, RECORD_ID, resource, defaultAnnotation)
            recordService.updateRecord(USER_ID, RECORD_ID, resource as Any, defaultAnnotation)
            recordService.checkDataRestrictions(resource)
            recordService.extractUploadData(resource)
            apiService.fetchRecord(
                ALIAS,
                USER_ID,
                RECORD_ID
            )
            recordService.decryptRecord<Fhir3Resource>(fetchedRecord, USER_ID)
            recordService.updateData(decryptedFetchedRecord, resource, USER_ID)
            recordService.cleanObsoleteAdditionalIdentifiers(resource)
            decryptedFetchedRecord.resource = resource
            recordService.removeUploadData(decryptedFetchedRecord)
            recordService.encryptRecord(decryptedFetchedRecord)
            apiService.updateRecord(
                ALIAS,
                USER_ID,
                RECORD_ID,
                encryptedRecord
            )
            recordService.decryptRecord<Fhir3Resource>(receivedRecord, USER_ID)
            recordService.restoreUploadData(receivedDecryptedRecord, resource, null)
            resource.id = receivedDecryptedRecord.identifier
            RecordMapper.getInstance(receivedDecryptedRecord)
        }
    }

    @Test
    fun `Given, updateRecord is called with a Fhir4 resource and a UserId, it returns a updated Record`() {
        // Given
        val resource: Fhir4Resource = mockk(relaxed = true)
        val fetchedRecord: EncryptedRecord = mockk()
        val decryptedFetchedRecord: DecryptedR4Record<Fhir4Resource> = mockk(relaxed = true)
        val encryptedRecord: EncryptedRecord = mockk()
        val receivedRecord: EncryptedRecord = mockk()
        val receivedDecryptedRecord: DecryptedR4Record<Fhir4Resource> = mockk(relaxed = true)
        val record: Fhir4Record<Fhir4Resource> = mockk()
        val identifier = "id"

        every { receivedDecryptedRecord.identifier } returns identifier

        every {
            apiService.fetchRecord(
                ALIAS,
                USER_ID,
                RECORD_ID
            )
        } returns Single.just(fetchedRecord)
        every {
            recordService.decryptRecord<Fhir4Resource>(fetchedRecord, USER_ID)
        } returns decryptedFetchedRecord
        every {
            recordService.encryptRecord(decryptedFetchedRecord)
        } returns encryptedRecord
        every {
            apiService.updateRecord(
                ALIAS,
                USER_ID,
                RECORD_ID,
                encryptedRecord
            )
        } returns Single.just(receivedRecord)
        every {
            recordService.decryptRecord<Fhir4Resource>(receivedRecord, USER_ID)
        } returns receivedDecryptedRecord
        every { RecordMapper.getInstance(receivedDecryptedRecord) } returns record

        // When
        val observer = recordService.updateRecord(
            USER_ID,
            RECORD_ID,
            resource,
            defaultAnnotation
        ).test().await()

        // Then
        val result = observer.assertNoErrors()
            .assertComplete()
            .assertValueCount(1)
            .values()[0]

        assertSame(
            actual = result,
            expected = record
        )

        verifyOrder {
            recordService.updateRecord(USER_ID, RECORD_ID, resource, defaultAnnotation)
            recordService.updateRecord(USER_ID, RECORD_ID, resource as Any, defaultAnnotation)
            recordService.checkDataRestrictions(resource)
            recordService.extractUploadData(resource)
            apiService.fetchRecord(
                ALIAS,
                USER_ID,
                RECORD_ID
            )
            recordService.decryptRecord<Fhir4Resource>(fetchedRecord, USER_ID)
            recordService.updateData(decryptedFetchedRecord, resource, USER_ID)
            recordService.cleanObsoleteAdditionalIdentifiers(resource)
            decryptedFetchedRecord.resource = resource
            recordService.removeUploadData(decryptedFetchedRecord)
            recordService.encryptRecord(decryptedFetchedRecord)
            apiService.updateRecord(
                ALIAS,
                USER_ID,
                RECORD_ID,
                encryptedRecord
            )
            recordService.decryptRecord<Fhir4Resource>(receivedRecord, USER_ID)
            recordService.restoreUploadData(receivedDecryptedRecord, resource, null)
            resource.id = receivedDecryptedRecord.identifier
            RecordMapper.getInstance(receivedDecryptedRecord)
        }
    }

    @Test
    fun `Given, updateRecord is called with a DataResource and a UserId, it returns a updated Record`() {
        // Given
        val resource: DataResource = mockk(relaxed = true)
        val fetchedRecord: EncryptedRecord = mockk()
        val decryptedFetchedRecord: DecryptedDataRecord = mockk(relaxed = true)
        val encryptedRecord: EncryptedRecord = mockk()
        val receivedRecord: EncryptedRecord = mockk()
        val receivedDecryptedRecord: DecryptedDataRecord = mockk(relaxed = true)
        val record: DataRecord<DataResource> = mockk()

        every {
            apiService.fetchRecord(
                ALIAS,
                USER_ID,
                RECORD_ID
            )
        } returns Single.just(fetchedRecord)
        every {
            recordService.decryptRecord<DataResource>(fetchedRecord, USER_ID)
        } returns decryptedFetchedRecord
        every {
            recordService.encryptRecord(decryptedFetchedRecord)
        } returns encryptedRecord
        every {
            apiService.updateRecord(
                ALIAS,
                USER_ID,
                RECORD_ID,
                encryptedRecord
            )
        } returns Single.just(receivedRecord)
        every {
            recordService.decryptRecord<DataResource>(receivedRecord, USER_ID)
        } returns receivedDecryptedRecord
        every { RecordMapper.getInstance(receivedDecryptedRecord) } returns record

        // When
        val observer = recordService.updateRecord<DataResource>(
            USER_ID,
            RECORD_ID,
            resource,
            defaultAnnotation
        ).test().await()

        // Then
        val result = observer.assertNoErrors()
            .assertComplete()
            .assertValueCount(1)
            .values()[0]

        assertSame(
            actual = result,
            expected = record
        )

        verifyOrder {
            recordService.updateRecord<DataResource>(
                USER_ID,
                RECORD_ID,
                resource,
                defaultAnnotation
            )
            recordService.updateRecord(USER_ID, RECORD_ID, resource as Any, defaultAnnotation)
            recordService.checkDataRestrictions(resource)
            recordService.extractUploadData(resource)
            apiService.fetchRecord(
                ALIAS,
                USER_ID,
                RECORD_ID
            )
            recordService.decryptRecord<DataResource>(fetchedRecord, USER_ID)
            recordService.updateData(decryptedFetchedRecord, resource, USER_ID)
            recordService.cleanObsoleteAdditionalIdentifiers(resource)
            decryptedFetchedRecord.resource = resource
            recordService.removeUploadData(decryptedFetchedRecord)
            recordService.encryptRecord(decryptedFetchedRecord)
            apiService.updateRecord(
                ALIAS,
                USER_ID,
                RECORD_ID,
                encryptedRecord
            )
            recordService.decryptRecord<DataResource>(receivedRecord, USER_ID)
            recordService.restoreUploadData(receivedDecryptedRecord, resource, null)
            RecordMapper.getInstance(receivedDecryptedRecord)
        }

        verify(exactly = 0) { receivedDecryptedRecord.identifier = any() }
    }

    @Test
    fun `Given, updateRecord is called with a Fhir3 resource, a UserId and annotations, it returns a updated Record`() {
        // Given
        val resource: Fhir3Resource = mockk(relaxed = true)
        val fetchedRecord: EncryptedRecord = mockk()
        val decryptedFetchedRecord: DecryptedRecord<Fhir3Resource> = mockk(relaxed = true)
        val encryptedRecord: EncryptedRecord = mockk()
        val receivedRecord: EncryptedRecord = mockk()
        val receivedDecryptedRecord: DecryptedRecord<Fhir3Resource> = mockk(relaxed = true)
        val record: Record<Fhir3Resource> = mockk()
        val annotations: Annotations = mockk()
        val identifier = "id"

        every { receivedDecryptedRecord.identifier } returns identifier

        every {
            apiService.fetchRecord(
                ALIAS,
                USER_ID,
                RECORD_ID
            )
        } returns Single.just(fetchedRecord)
        every {
            recordService.decryptRecord<Fhir3Resource>(fetchedRecord, USER_ID)
        } returns decryptedFetchedRecord
        every {
            recordService.encryptRecord(decryptedFetchedRecord)
        } returns encryptedRecord
        every {
            apiService.updateRecord(
                ALIAS,
                USER_ID,
                RECORD_ID,
                encryptedRecord
            )
        } returns Single.just(receivedRecord)
        every {
            recordService.decryptRecord<Fhir3Resource>(receivedRecord, USER_ID)
        } returns receivedDecryptedRecord
        every { RecordMapper.getInstance(receivedDecryptedRecord) } returns record

        // When
        val observer = recordService.updateRecord(
            USER_ID,
            RECORD_ID,
            resource,
            annotations
        ).test().await()

        // Then
        val result = observer.assertNoErrors()
            .assertComplete()
            .assertValueCount(1)
            .values()[0]

        assertSame(
            actual = result,
            expected = record
        )

        verifyOrder {
            recordService.updateRecord(USER_ID, RECORD_ID, resource, annotations)
            recordService.updateRecord(USER_ID, RECORD_ID, resource as Any, annotations)
            recordService.checkDataRestrictions(resource)
            recordService.extractUploadData(resource)
            apiService.fetchRecord(
                ALIAS,
                USER_ID,
                RECORD_ID
            )
            recordService.decryptRecord<Fhir3Resource>(fetchedRecord, USER_ID)
            recordService.updateData(decryptedFetchedRecord, resource, USER_ID)
            recordService.cleanObsoleteAdditionalIdentifiers(resource)
            decryptedFetchedRecord.resource = resource
            decryptedFetchedRecord.annotations = annotations
            recordService.removeUploadData(decryptedFetchedRecord)
            recordService.encryptRecord(decryptedFetchedRecord)
            apiService.updateRecord(
                ALIAS,
                USER_ID,
                RECORD_ID,
                encryptedRecord
            )
            recordService.decryptRecord<Fhir3Resource>(receivedRecord, USER_ID)
            recordService.restoreUploadData(receivedDecryptedRecord, resource, null)
            resource.id = receivedDecryptedRecord.identifier
            RecordMapper.getInstance(receivedDecryptedRecord)
        }
    }

    @Test
    fun `Given, updateRecord is called with a Fhir4 resource, a UserId and annotations, it returns a updated Record`() {
        // Given
        val resource: Fhir4Resource = mockk(relaxed = true)
        val fetchedRecord: EncryptedRecord = mockk()
        val decryptedFetchedRecord: DecryptedR4Record<Fhir4Resource> = mockk(relaxed = true)
        val encryptedRecord: EncryptedRecord = mockk()
        val receivedRecord: EncryptedRecord = mockk()
        val receivedDecryptedRecord: DecryptedR4Record<Fhir4Resource> = mockk(relaxed = true)
        val record: Fhir4Record<Fhir4Resource> = mockk()
        val annotations: Annotations = mockk()
        val identifier = "id"

        every { receivedDecryptedRecord.identifier } returns identifier

        every {
            apiService.fetchRecord(
                ALIAS,
                USER_ID,
                RECORD_ID
            )
        } returns Single.just(fetchedRecord)
        every {
            recordService.decryptRecord<Fhir4Resource>(fetchedRecord, USER_ID)
        } returns decryptedFetchedRecord
        every {
            recordService.encryptRecord(decryptedFetchedRecord)
        } returns encryptedRecord
        every {
            apiService.updateRecord(
                ALIAS,
                USER_ID,
                RECORD_ID,
                encryptedRecord
            )
        } returns Single.just(receivedRecord)
        every {
            recordService.decryptRecord<Fhir4Resource>(receivedRecord, USER_ID)
        } returns receivedDecryptedRecord
        every { RecordMapper.getInstance(receivedDecryptedRecord) } returns record

        // When
        val observer = recordService.updateRecord(
            USER_ID,
            RECORD_ID,
            resource,
            annotations
        ).test().await()

        // Then
        val result = observer.assertNoErrors()
            .assertComplete()
            .assertValueCount(1)
            .values()[0]

        assertSame(
            actual = result,
            expected = record
        )

        verifyOrder {
            recordService.updateRecord(USER_ID, RECORD_ID, resource, annotations)
            recordService.updateRecord(USER_ID, RECORD_ID, resource as Any, annotations)
            recordService.checkDataRestrictions(resource)
            recordService.extractUploadData(resource)
            apiService.fetchRecord(
                ALIAS,
                USER_ID,
                RECORD_ID
            )
            recordService.decryptRecord<Fhir4Resource>(fetchedRecord, USER_ID)
            recordService.updateData(decryptedFetchedRecord, resource, USER_ID)
            recordService.cleanObsoleteAdditionalIdentifiers(resource)
            decryptedFetchedRecord.resource = resource
            decryptedFetchedRecord.annotations = annotations
            recordService.removeUploadData(decryptedFetchedRecord)
            recordService.encryptRecord(decryptedFetchedRecord)
            apiService.updateRecord(
                ALIAS,
                USER_ID,
                RECORD_ID,
                encryptedRecord
            )
            recordService.decryptRecord<Fhir4Resource>(receivedRecord, USER_ID)
            recordService.restoreUploadData(receivedDecryptedRecord, resource, null)
            resource.id = receivedDecryptedRecord.identifier
            RecordMapper.getInstance(receivedDecryptedRecord)
        }
    }

    @Test
    fun `Given, updateRecord is called with a DataResource, a UserId and annotations, it returns a updated Record`() {
        // Given
        val resource: DataResource = mockk(relaxed = true)
        val fetchedRecord: EncryptedRecord = mockk()
        val decryptedFetchedRecord: DecryptedDataRecord = mockk(relaxed = true)
        val encryptedRecord: EncryptedRecord = mockk()
        val receivedRecord: EncryptedRecord = mockk()
        val receivedDecryptedRecord: DecryptedDataRecord = mockk(relaxed = true)
        val record: DataRecord<DataResource> = mockk()
        val annotations: Annotations = mockk()

        every {
            apiService.fetchRecord(
                ALIAS,
                USER_ID,
                RECORD_ID
            )
        } returns Single.just(fetchedRecord)
        every {
            recordService.decryptRecord<DataResource>(fetchedRecord, USER_ID)
        } returns decryptedFetchedRecord
        every {
            recordService.encryptRecord(decryptedFetchedRecord)
        } returns encryptedRecord
        every {
            apiService.updateRecord(
                ALIAS,
                USER_ID,
                RECORD_ID,
                encryptedRecord
            )
        } returns Single.just(receivedRecord)
        every {
            recordService.decryptRecord<DataResource>(receivedRecord, USER_ID)
        } returns receivedDecryptedRecord
        every { RecordMapper.getInstance(receivedDecryptedRecord) } returns record

        // When
        val observer = recordService.updateRecord<DataResource>(
            USER_ID,
            RECORD_ID,
            resource,
            annotations
        ).test().await()

        // Then
        val result = observer.assertNoErrors()
            .assertComplete()
            .assertValueCount(1)
            .values()[0]

        assertSame(
            actual = result,
            expected = record
        )

        verifyOrder {
            recordService.updateRecord<DataResource>(USER_ID, RECORD_ID, resource, annotations)
            recordService.updateRecord(USER_ID, RECORD_ID, resource as Any, annotations)
            recordService.checkDataRestrictions(resource)
            recordService.extractUploadData(resource)
            apiService.fetchRecord(
                ALIAS,
                USER_ID,
                RECORD_ID
            )
            recordService.decryptRecord<DataResource>(fetchedRecord, USER_ID)
            recordService.updateData(decryptedFetchedRecord, resource, USER_ID)
            recordService.cleanObsoleteAdditionalIdentifiers(resource)
            decryptedFetchedRecord.resource = resource
            decryptedFetchedRecord.annotations = annotations
            recordService.removeUploadData(decryptedFetchedRecord)
            recordService.encryptRecord(decryptedFetchedRecord)
            apiService.updateRecord(
                ALIAS,
                USER_ID,
                RECORD_ID,
                encryptedRecord
            )
            recordService.decryptRecord<DataResource>(receivedRecord, USER_ID)
            recordService.restoreUploadData(receivedDecryptedRecord, resource, null)
            RecordMapper.getInstance(receivedDecryptedRecord)
        }

        verify(exactly = 0) { receivedDecryptedRecord.identifier = any() }
    }

    @Test
    fun `Given, updateRecord is called with a Fhir3 resource, which contains an attachment, and a UserId, it returns a updated Record`() {
        // Given
        val resource: Fhir3Resource = mockk(relaxed = true)
        val fetchedRecord: EncryptedRecord = mockk()
        val decryptedFetchedRecord: DecryptedRecord<Fhir3Resource> = mockk(relaxed = true)
        val encryptedRecord: EncryptedRecord = mockk()
        val receivedRecord: EncryptedRecord = mockk()
        val receivedDecryptedRecord: DecryptedRecord<Fhir3Resource> = mockk(relaxed = true)
        val record: Record<Fhir3Resource> = mockk()
        val attachment: HashMap<Any, String?> = mockk()
        val identifier = "id"

        every { receivedDecryptedRecord.identifier } returns identifier

        every {
            apiService.fetchRecord(
                ALIAS,
                USER_ID,
                RECORD_ID
            )
        } returns Single.just(fetchedRecord)
        every { recordService.extractUploadData(resource) } returns attachment
        every {
            recordService.decryptRecord<Fhir3Resource>(fetchedRecord, USER_ID)
        } returns decryptedFetchedRecord
        every {
            recordService.encryptRecord(decryptedFetchedRecord)
        } returns encryptedRecord
        every {
            apiService.updateRecord(
                ALIAS,
                USER_ID,
                RECORD_ID,
                encryptedRecord
            )
        } returns Single.just(receivedRecord)
        every {
            recordService.decryptRecord<Fhir3Resource>(receivedRecord, USER_ID)
        } returns receivedDecryptedRecord
        every { RecordMapper.getInstance(receivedDecryptedRecord) } returns record

        // When
        val observer = recordService.updateRecord(
            USER_ID,
            RECORD_ID,
            resource,
            defaultAnnotation
        ).test().await()

        // Then
        val result = observer.assertNoErrors()
            .assertComplete()
            .assertValueCount(1)
            .values()[0]

        assertSame(
            actual = result,
            expected = record
        )

        verifyOrder {
            recordService.updateRecord(USER_ID, RECORD_ID, resource, defaultAnnotation)
            recordService.updateRecord(USER_ID, RECORD_ID, resource as Any, defaultAnnotation)
            recordService.checkDataRestrictions(resource)
            recordService.extractUploadData(resource)
            apiService.fetchRecord(
                ALIAS,
                USER_ID,
                RECORD_ID
            )
            recordService.decryptRecord<Fhir3Resource>(fetchedRecord, USER_ID)
            recordService.updateData(decryptedFetchedRecord, resource, USER_ID)
            recordService.cleanObsoleteAdditionalIdentifiers(resource)
            decryptedFetchedRecord.resource = resource
            recordService.removeUploadData(decryptedFetchedRecord)
            recordService.encryptRecord(decryptedFetchedRecord)
            apiService.updateRecord(
                ALIAS,
                USER_ID,
                RECORD_ID,
                encryptedRecord
            )
            recordService.decryptRecord<Fhir3Resource>(receivedRecord, USER_ID)
            recordService.restoreUploadData(receivedDecryptedRecord, resource, attachment)
            resource.id = receivedDecryptedRecord.identifier
            RecordMapper.getInstance(receivedDecryptedRecord)
        }
    }

    @Test
    fun `Given, updateRecord is called with a Fhir4 resource, which contains an attachment, and a UserId, it returns a updated Record`() {
        // Given
        val resource: Fhir4Resource = mockk(relaxed = true)
        val fetchedRecord: EncryptedRecord = mockk()
        val decryptedFetchedRecord: DecryptedR4Record<Fhir4Resource> = mockk(relaxed = true)
        val encryptedRecord: EncryptedRecord = mockk()
        val receivedRecord: EncryptedRecord = mockk()
        val receivedDecryptedRecord: DecryptedR4Record<Fhir4Resource> = mockk(relaxed = true)
        val record: Fhir4Record<Fhir4Resource> = mockk()
        val attachment: HashMap<Any, String?> = mockk()
        val identifier = "id"

        every { receivedDecryptedRecord.identifier } returns identifier

        every {
            apiService.fetchRecord(
                ALIAS,
                USER_ID,
                RECORD_ID
            )
        } returns Single.just(fetchedRecord)
        every { recordService.extractUploadData(resource) } returns attachment
        every {
            recordService.decryptRecord<Fhir4Resource>(fetchedRecord, USER_ID)
        } returns decryptedFetchedRecord
        every {
            recordService.encryptRecord(decryptedFetchedRecord)
        } returns encryptedRecord
        every {
            apiService.updateRecord(
                ALIAS,
                USER_ID,
                RECORD_ID,
                encryptedRecord
            )
        } returns Single.just(receivedRecord)
        every {
            recordService.decryptRecord<Fhir4Resource>(receivedRecord, USER_ID)
        } returns receivedDecryptedRecord
        every { RecordMapper.getInstance(receivedDecryptedRecord) } returns record

        // When
        val observer = recordService.updateRecord(
            USER_ID,
            RECORD_ID,
            resource,
            defaultAnnotation
        ).test().await()

        // Then
        val result = observer.assertNoErrors()
            .assertComplete()
            .assertValueCount(1)
            .values()[0]

        assertSame(
            actual = result,
            expected = record
        )

        verifyOrder {
            recordService.updateRecord(USER_ID, RECORD_ID, resource, defaultAnnotation)
            recordService.updateRecord(USER_ID, RECORD_ID, resource as Any, defaultAnnotation)
            recordService.checkDataRestrictions(resource)
            recordService.extractUploadData(resource)
            apiService.fetchRecord(
                ALIAS,
                USER_ID,
                RECORD_ID
            )
            recordService.decryptRecord<Fhir4Resource>(fetchedRecord, USER_ID)
            recordService.updateData(decryptedFetchedRecord, resource, USER_ID)
            recordService.cleanObsoleteAdditionalIdentifiers(resource)
            decryptedFetchedRecord.resource = resource
            recordService.removeUploadData(decryptedFetchedRecord)
            recordService.encryptRecord(decryptedFetchedRecord)
            apiService.updateRecord(
                ALIAS,
                USER_ID,
                RECORD_ID,
                encryptedRecord
            )
            recordService.decryptRecord<Fhir4Resource>(receivedRecord, USER_ID)
            recordService.restoreUploadData(receivedDecryptedRecord, resource, attachment)
            resource.id = receivedDecryptedRecord.identifier
            RecordMapper.getInstance(receivedDecryptedRecord)
        }
    }

    @Test
    fun `Given, updateRecords is called with multiple resources and a UserId, returns multiple updated Records`() {
        // Given
        val ids = listOf("1", "2")
        val resources = listOf<Fhir3Resource>(
            mockk(),
            mockk()
        )

        val expected = listOf<Record<Fhir3Resource>>(
            mockk(),
            mockk()
        )

        resources[0].id = ids[0]
        resources[1].id = ids[1]

        every {
            recordService.updateRecord(
                USER_ID,
                or(ids[0], ids[1]),
                or(resources[0], resources[1]),
                defaultAnnotation
            )
        } returnsMany listOf(Single.just(expected[0]), Single.just(expected[1]))

        // When
        val observer = recordService.updateRecords(resources, USER_ID).test().await()

        // Then
        val result = observer
            .assertNoErrors()
            .assertComplete()
            .assertValueCount(1)
            .values()[0]

        assertEquals(
            actual = result.failedUpdates.size,
            expected = 0
        )
        assertEquals(
            actual = result.successfulUpdates.size,
            expected = 2
        )
        assertSame(
            actual = result.successfulUpdates[0],
            expected = expected[0]
        )
        assertSame(
            actual = result.successfulUpdates[1],
            expected = expected[1]
        )

        verifyOrder {
            recordService.updateRecord(
                USER_ID,
                or(ids[0], ids[1]),
                or(resources[0], resources[1]),
                defaultAnnotation
            )
            recordService.updateRecord(
                USER_ID,
                or(ids[0], ids[1]),
                or(resources[0], resources[1]),
                defaultAnnotation
            )
        }
    }
}
