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
import care.data4life.sdk.RecordServiceTestBase.Companion.ALIAS
import care.data4life.sdk.RecordServiceTestBase.Companion.PARTNER_ID
import care.data4life.sdk.RecordServiceTestBase.Companion.USER_ID
import care.data4life.sdk.attachment.AttachmentContract
import care.data4life.sdk.call.DataRecord
import care.data4life.sdk.call.Fhir4Record
import care.data4life.sdk.data.DataResource
import care.data4life.sdk.fhir.Fhir3Resource
import care.data4life.sdk.fhir.Fhir4Resource
import care.data4life.sdk.fhir.FhirContract
import care.data4life.sdk.migration.MigrationContract
import care.data4life.sdk.model.ModelContract.BaseRecord
import care.data4life.sdk.model.ModelVersion
import care.data4life.sdk.model.Record
import care.data4life.sdk.model.RecordMapper
import care.data4life.sdk.network.DecryptedRecordMapper
import care.data4life.sdk.network.model.DecryptedDataRecord
import care.data4life.sdk.network.model.EncryptedRecord
import care.data4life.sdk.network.model.definitions.DecryptedFhir3Record
import care.data4life.sdk.network.model.definitions.DecryptedFhir4Record
import care.data4life.sdk.tag.TaggingContract
import care.data4life.sdk.wrapper.SdkDateTimeFormatter
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.mockkObject
import io.mockk.spyk
import io.mockk.unmockkConstructor
import io.mockk.unmockkObject
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
    private lateinit var apiService: ApiService
    private lateinit var cryptoService: CryptoService
    private lateinit var fhirService: FhirContract.Service
    private lateinit var tagEncryptionService: TaggingContract.EncryptionService
    private lateinit var taggingService: TaggingContract.Service
    private lateinit var attachmentService: AttachmentContract.Service
    private lateinit var errorHandler: SdkContract.ErrorHandler
    private lateinit var tags: HashMap<String, String>
    private val defaultAnnotation: MutableList<String> = mutableListOf()

    private lateinit var compatibilityService: MigrationContract.CompatibilityService
    private lateinit var decryptedRecordMapper: DecryptedRecordMapper
    private lateinit var dataKey: GCKey
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

        compatibilityService = mockk()
        decryptedRecordMapper = mockk()
        dataKey = mockk()
        encryptedRecord = mockk()

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
        mockkObject(SdkDateTimeFormatter)
        mockkConstructor(DecryptedRecordMapper::class)
    }

    @After
    fun tearDown() {
        unmockkObject(RecordMapper)
        unmockkObject(SdkDateTimeFormatter)
        unmockkConstructor(DecryptedRecordMapper::class)
    }

    @Test
    fun `Given, createRecord is called with a Fhir3 resource and a UserId, it returns a new Record`() {
        // Given
        val resource: Fhir3Resource = mockk(relaxed = true)
        val createdRecord: DecryptedFhir3Record<Fhir3Resource> = mockk()
        val receivedRecord: DecryptedFhir3Record<Fhir3Resource> = mockk(relaxed = true)
        val record: Record<Fhir3Resource> = mockk()
        val date = "now"

        every { SdkDateTimeFormatter.now() } returns date
        every { createdRecord.resource } returns resource
        every { receivedRecord.resource } returns resource

        every {
            anyConstructed<DecryptedRecordMapper>().setAnnotations(defaultAnnotation)
        } returns decryptedRecordMapper

        every { taggingService.appendDefaultTags(resource, null) } returns tags
        every { cryptoService.generateGCKey() } returns Single.just(dataKey)

        every {
            decryptedRecordMapper.build(
                resource,
                tags,
                date,
                dataKey,
                ModelVersion.CURRENT
            )
        } returns createdRecord
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
        val subscriber =
            recordService.createRecord(USER_ID, resource, defaultAnnotation).test().await()

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
            taggingService.appendDefaultTags(resource, null)
            SdkDateTimeFormatter.now()
            cryptoService.generateGCKey()
            decryptedRecordMapper.build(
                resource,
                tags,
                date,
                dataKey,
                ModelVersion.CURRENT
            )
            recordService._uploadData(createdRecord, USER_ID)
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
            recordService.assignResourceId(receivedRecord)
            RecordMapper.getInstance(receivedRecord)
        }
    }

    @Test
    fun `Given, createRecord is called with a Fhir4 resource and a UserId, it returns a new Record`() {
        // Given
        val resource: Fhir4Resource = mockk(relaxed = true)
        val createdRecord: DecryptedFhir4Record<Fhir4Resource> = mockk()
        val receivedRecord: DecryptedFhir4Record<Fhir4Resource> = mockk(relaxed = true)
        val record: Fhir4Record<Fhir4Resource> = mockk()
        val date = "now"

        every { SdkDateTimeFormatter.now() } returns date
        every { createdRecord.resource } returns resource
        every { receivedRecord.resource } returns resource

        every {
            anyConstructed<DecryptedRecordMapper>().setAnnotations(defaultAnnotation)
        } returns decryptedRecordMapper

        every { taggingService.appendDefaultTags(resource, null) } returns tags
        every { cryptoService.generateGCKey() } returns Single.just(dataKey)

        every {
            decryptedRecordMapper.build(
                resource,
                tags,
                date,
                dataKey,
                ModelVersion.CURRENT
            )
        } returns createdRecord
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
        val subscriber =
            recordService.createRecord(USER_ID, resource, defaultAnnotation).test().await()

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
            taggingService.appendDefaultTags(resource, null)
            SdkDateTimeFormatter.now()
            cryptoService.generateGCKey()
            decryptedRecordMapper.build(
                resource,
                tags,
                date,
                dataKey,
                ModelVersion.CURRENT
            )
            recordService._uploadData(createdRecord, USER_ID)
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
            recordService.assignResourceId(receivedRecord)
            RecordMapper.getInstance(receivedRecord)
        }
    }

    @Test
    fun `Given, createRecord is called with a DataResource and a UserId, it returns a new Record`() {
        // Given
        val resource: DataResource = mockk()
        val createdRecord: DecryptedDataRecord = mockk()
        val receivedRecord: DecryptedDataRecord = mockk(relaxed = true)
        val record: DataRecord<DataResource> = mockk()
        val date = "now"

        every { SdkDateTimeFormatter.now() } returns date
        every { createdRecord.resource } returns resource
        every { receivedRecord.resource } returns resource

        every {
            anyConstructed<DecryptedRecordMapper>().setAnnotations(defaultAnnotation)
        } returns decryptedRecordMapper

        every { taggingService.appendDefaultTags(resource, null) } returns tags
        every { cryptoService.generateGCKey() } returns Single.just(dataKey)

        every {
            decryptedRecordMapper.build(
                resource,
                tags,
                date,
                dataKey,
                ModelVersion.CURRENT
            )
        } returns createdRecord
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
        val subscriber =
            recordService.createRecord(USER_ID, resource, defaultAnnotation).test().await()

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
            taggingService.appendDefaultTags(resource, null)
            SdkDateTimeFormatter.now()
            cryptoService.generateGCKey()
            decryptedRecordMapper.build(
                resource,
                tags,
                date,
                dataKey,
                ModelVersion.CURRENT
            )
            recordService._uploadData(createdRecord, USER_ID)
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
            recordService.assignResourceId(receivedRecord)
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
        val date = "now"

        every { SdkDateTimeFormatter.now() } returns date
        every { createdRecord.resource } returns resource
        every { receivedRecord.resource } returns resource

        every {
            anyConstructed<DecryptedRecordMapper>().setAnnotations(defaultAnnotation)
        } returns decryptedRecordMapper

        every { recordService.extractUploadData(resource) } returns uploadData
        every { taggingService.appendDefaultTags(resource, null) } returns tags
        every { cryptoService.generateGCKey() } returns Single.just(dataKey)

        every {
            decryptedRecordMapper.build(
                resource,
                tags,
                date,
                dataKey,
                ModelVersion.CURRENT
            )
        } returns createdRecord
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
        val subscriber =
            recordService.createRecord(USER_ID, resource, defaultAnnotation).test().await()

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
            taggingService.appendDefaultTags(resource, null)
            SdkDateTimeFormatter.now()
            cryptoService.generateGCKey()
            decryptedRecordMapper.build(
                resource,
                tags,
                date,
                dataKey,
                ModelVersion.CURRENT
            )
            recordService._uploadData(createdRecord, USER_ID)
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
            recordService.assignResourceId(receivedRecord)
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
        val date = "now"

        every { SdkDateTimeFormatter.now() } returns date
        every { createdRecord.resource } returns resource
        every { receivedRecord.resource } returns resource

        every {
            anyConstructed<DecryptedRecordMapper>().setAnnotations(defaultAnnotation)
        } returns decryptedRecordMapper

        every { recordService.extractUploadData(resource) } returns uploadData
        every { taggingService.appendDefaultTags(resource, null) } returns tags
        every { cryptoService.generateGCKey() } returns Single.just(dataKey)

        every {
            decryptedRecordMapper.build(
                resource,
                tags,
                date,
                dataKey,
                ModelVersion.CURRENT
            )
        } returns createdRecord
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
        val subscriber =
            recordService.createRecord(USER_ID, resource, defaultAnnotation).test().await()

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
            taggingService.appendDefaultTags(resource, null)
            SdkDateTimeFormatter.now()
            cryptoService.generateGCKey()
            decryptedRecordMapper.build(
                resource,
                tags,
                date,
                dataKey,
                ModelVersion.CURRENT
            )
            recordService._uploadData(createdRecord, USER_ID)
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
            recordService.assignResourceId(receivedRecord)
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
        val date = "now"
        val annotations: List<String> = mockk()

        every { SdkDateTimeFormatter.now() } returns date
        every { createdRecord.resource } returns resource
        every { receivedRecord.resource } returns resource

        every {
            anyConstructed<DecryptedRecordMapper>().setAnnotations(annotations)
        } returns decryptedRecordMapper

        every { taggingService.appendDefaultTags(resource, null) } returns tags
        every { cryptoService.generateGCKey() } returns Single.just(dataKey)

        every {
            decryptedRecordMapper.build(
                resource,
                tags,
                date,
                dataKey,
                ModelVersion.CURRENT
            )
        } returns createdRecord
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
            taggingService.appendDefaultTags(resource, null)
            SdkDateTimeFormatter.now()
            cryptoService.generateGCKey()
            decryptedRecordMapper.build(
                resource,
                tags,
                date,
                dataKey,
                ModelVersion.CURRENT
            )
            recordService._uploadData(createdRecord, USER_ID)
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
            recordService.assignResourceId(receivedRecord)
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
        val date = "now"
        val annotations: List<String> = mockk()

        every { SdkDateTimeFormatter.now() } returns date
        every { createdRecord.resource } returns resource
        every { receivedRecord.resource } returns resource

        every {
            anyConstructed<DecryptedRecordMapper>().setAnnotations(annotations)
        } returns decryptedRecordMapper

        every { taggingService.appendDefaultTags(resource, null) } returns tags
        every { cryptoService.generateGCKey() } returns Single.just(dataKey)

        every {
            decryptedRecordMapper.build(
                resource,
                tags,
                date,
                dataKey,
                ModelVersion.CURRENT
            )
        } returns createdRecord
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
            taggingService.appendDefaultTags(resource, null)
            SdkDateTimeFormatter.now()
            cryptoService.generateGCKey()
            decryptedRecordMapper.build(
                resource,
                tags,
                date,
                dataKey,
                ModelVersion.CURRENT
            )
            recordService._uploadData(createdRecord, USER_ID)
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
            recordService.assignResourceId(receivedRecord)
            RecordMapper.getInstance(receivedRecord)
        }
    }

    @Test
    fun `Given, createRecords is called a DataResource and a UserId and Annotations, it returns a new Record`() {
        // Given
        val resource: DataResource = mockk()
        val createdRecord: DecryptedDataRecord = mockk()
        val receivedRecord: DecryptedDataRecord = mockk(relaxed = true)
        val record: DataRecord<DataResource> = mockk()
        val date = "now"
        val annotations: List<String> = mockk()

        every { SdkDateTimeFormatter.now() } returns date
        every { createdRecord.resource } returns resource
        every { receivedRecord.resource } returns resource

        every {
            anyConstructed<DecryptedRecordMapper>().setAnnotations(annotations)
        } returns decryptedRecordMapper

        every { taggingService.appendDefaultTags(resource, null) } returns tags
        every { cryptoService.generateGCKey() } returns Single.just(dataKey)

        every {
            decryptedRecordMapper.build(
                resource,
                tags,
                date,
                dataKey,
                ModelVersion.CURRENT
            )
        } returns createdRecord
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
            taggingService.appendDefaultTags(resource, null)
            SdkDateTimeFormatter.now()
            cryptoService.generateGCKey()
            decryptedRecordMapper.build(
                resource,
                tags,
                date,
                dataKey,
                ModelVersion.CURRENT
            )
            recordService._uploadData(createdRecord, USER_ID)
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
            recordService.assignResourceId(receivedRecord)
            RecordMapper.getInstance(receivedRecord)
        }
    }

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
