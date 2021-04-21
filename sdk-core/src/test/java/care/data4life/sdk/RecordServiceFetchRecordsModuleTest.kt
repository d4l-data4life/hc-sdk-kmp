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
import care.data4life.sdk.attachment.AttachmentContract
import care.data4life.sdk.attachment.AttachmentService
import care.data4life.sdk.call.DataRecord
import care.data4life.sdk.call.Fhir4Record
import care.data4life.sdk.crypto.CryptoContract
import care.data4life.sdk.fhir.FhirService
import care.data4life.sdk.model.Record
import care.data4life.sdk.network.model.EncryptedKey
import care.data4life.sdk.network.model.EncryptedRecord
import care.data4life.sdk.record.RecordContract
import care.data4life.sdk.tag.TagEncryptionService
import care.data4life.sdk.tag.TaggingService
import care.data4life.sdk.test.fake.CryptoServiceFake
import care.data4life.sdk.test.fake.CryptoServiceIteration
import care.data4life.sdk.test.util.GenericTestDataProvider.ALIAS
import care.data4life.sdk.test.util.GenericTestDataProvider.ARBITRARY_DATA_KEY
import care.data4life.sdk.test.util.GenericTestDataProvider.CLIENT_ID
import care.data4life.sdk.test.util.GenericTestDataProvider.COMMON_KEY_ID
import care.data4life.sdk.test.util.GenericTestDataProvider.CREATION_DATE
import care.data4life.sdk.test.util.GenericTestDataProvider.OFFSET
import care.data4life.sdk.test.util.GenericTestDataProvider.PAGE_SIZE
import care.data4life.sdk.test.util.GenericTestDataProvider.PARTNER_ID
import care.data4life.sdk.test.util.GenericTestDataProvider.RECORD_ID
import care.data4life.sdk.test.util.GenericTestDataProvider.RECORD_ID_COMPATIBILITY
import care.data4life.sdk.test.util.GenericTestDataProvider.UPDATE_DATE
import care.data4life.sdk.test.util.GenericTestDataProvider.USER_ID
import care.data4life.sdk.test.util.TestResourceHelper
import care.data4life.sdk.wrapper.SdkFhirParser
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.reactivex.Observable
import io.reactivex.Single
import org.junit.Before
import org.junit.Test
import org.threeten.bp.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import care.data4life.fhir.r4.model.DocumentReference as Fhir4DocumentReference
import care.data4life.fhir.stu3.model.DocumentReference as Fhir3DocumentReference

class RecordServiceFetchRecordsModuleTest {
    private val dataKey: GCKey = mockk()
    private val attachmentKey: GCKey = mockk()
    private val tagEncryptionKey: GCKey = mockk()
    private val commonKey: GCKey = mockk()
    private val encryptedDataKey: EncryptedKey = mockk()
    private val encryptedAttachmentKey: EncryptedKey = mockk()

    private lateinit var recordService: RecordContract.Service
    private lateinit var flowHelper: RecordServiceModuleTestFlowHelper
    private val apiService: ApiService = mockk()
    private lateinit var cryptoService: CryptoContract.Service
    private val fileService: AttachmentContract.FileService = mockk()
    private val imageResizer: AttachmentContract.ImageResizer = mockk()
    private val errorHandler: D4LErrorHandler = mockk()

    @Before
    fun setUp() {
        clearAllMocks()

        cryptoService = CryptoServiceFake()

        recordService = RecordService(
            PARTNER_ID,
            ALIAS,
            apiService,
            TagEncryptionService(cryptoService),
            TaggingService(CLIENT_ID),
            FhirService(cryptoService),
            AttachmentService(
                fileService,
                imageResizer
            ),
            cryptoService,
            errorHandler
        )

        flowHelper = RecordServiceModuleTestFlowHelper(
            apiService,
            fileService,
            imageResizer
        )
    }

    private fun runFetchFlow(
        serializedResource: String,
        encryptedRecord: EncryptedRecord,
        tags: List<String>,
        annotations: List<String>,
        useStoredCommonKey: Boolean,
        commonKey: Pair<String, GCKey>,
        dataKey: Pair<GCKey, EncryptedKey>,
        attachmentKey: Pair<GCKey, EncryptedKey>?,
        tagEncryptionKey: GCKey,
        recordId: String,
        userId: String,
        alias: String
    ) {
        val encryptedCommonKey = flowHelper.prepareStoredOrUnstoredCommonKeyRun(
            alias,
            userId,
            commonKey.first,
            useStoredCommonKey
        )

        val receivedIteration = CryptoServiceIteration(
            gcKeyOrder = emptyList(),
            commonKey = commonKey.second,
            commonKeyId = commonKey.first,
            commonKeyIsStored = useStoredCommonKey,
            commonKeyFetchCalls = 1,
            encryptedCommonKey = encryptedCommonKey,
            dataKey = dataKey.first,
            encryptedDataKey = dataKey.second,
            attachmentKey = attachmentKey?.first,
            encryptedAttachmentKey = attachmentKey?.second,
            tagEncryptionKey = tagEncryptionKey,
            tagEncryptionKeyCalls = 1,
            resources = listOf(serializedResource),
            tags = tags,
            annotations = annotations,
            hashFunction = { value -> flowHelper.md5(value) }
        )

        every {
            apiService.fetchRecord(alias, userId, recordId)
        } answers {
            (cryptoService as CryptoServiceFake).iteration = receivedIteration
            Single.just(
                encryptedRecord
            )
        }
    }

    private fun runFhirFetchFlow(
        serializedResource: String,
        tags: Map<String, String>,
        annotations: List<String> = emptyList(),
        useStoredCommonKey: Boolean = true,
        commonKey: Pair<String, GCKey> = COMMON_KEY_ID to this.commonKey,
        dataKey: Pair<GCKey, EncryptedKey> = this.dataKey to encryptedDataKey,
        attachmentKey: Pair<GCKey, EncryptedKey>? = this.attachmentKey to encryptedAttachmentKey,
        tagEncryptionKey: GCKey = this.tagEncryptionKey,
        recordId: String = RECORD_ID,
        userId: String = USER_ID,
        alias: String = ALIAS,
        creationDate: String = CREATION_DATE,
        updateDate: String = UPDATE_DATE
    ) {
        val encodedTags = flowHelper.prepareTags(tags)
        val encodedAnnotations = flowHelper.prepareAnnotations(annotations)

        val encryptedRecord = flowHelper.prepareEncryptedFhirRecord(
            recordId,
            serializedResource,
            encodedTags,
            encodedAnnotations,
            commonKey.first,
            dataKey.second,
            attachmentKey?.second,
            creationDate,
            updateDate
        )

        runFetchFlow(
            serializedResource,
            encryptedRecord,
            encodedTags,
            encodedAnnotations,
            useStoredCommonKey,
            commonKey,
            dataKey,
            attachmentKey,
            tagEncryptionKey,
            recordId,
            userId,
            alias
        )
    }

    private fun runDataFetchFlow(
        serializedResource: String,
        tags: Map<String, String>,
        annotations: List<String> = emptyList(),
        useStoredCommonKey: Boolean = true,
        commonKey: Pair<String, GCKey> = COMMON_KEY_ID to this.commonKey,
        dataKey: Pair<GCKey, EncryptedKey> = this.dataKey to encryptedDataKey,
        attachmentKey: Pair<GCKey, EncryptedKey>? = this.attachmentKey to encryptedAttachmentKey,
        tagEncryptionKey: GCKey = this.tagEncryptionKey,
        recordId: String = RECORD_ID,
        userId: String = USER_ID,
        alias: String = ALIAS,
        creationDate: String = CREATION_DATE,
        updateDate: String = UPDATE_DATE
    ) {
        val encodedTags = flowHelper.prepareTags(tags)
        val encodedAnnotations = flowHelper.prepareAnnotations(annotations)

        val encryptedRecord = flowHelper.prepareEncryptedDataRecord(
            recordId,
            serializedResource,
            encodedTags,
            encodedAnnotations,
            commonKey.first,
            dataKey.second,
            attachmentKey?.second,
            creationDate,
            updateDate
        )

        runFetchFlow(
            serializedResource,
            encryptedRecord,
            encodedTags,
            encodedAnnotations,
            useStoredCommonKey,
            commonKey,
            dataKey,
            attachmentKey,
            tagEncryptionKey,
            recordId,
            userId,
            alias
        )
    }

    private fun runBatchFlow(
        serializedResources: Pair<String, String>,
        encryptedRecord: EncryptedRecord,
        encryptedLegacyRecord: EncryptedRecord,
        searchTags: Map<String, String>,
        encodedTags: List<String>,
        encodedAnnotations: List<String>,
        legacyTags: List<String>,
        legacyAnnotations: List<String>,
        tagEncryptionKeyCalls: Int,
        useStoredCommonKey: Boolean,
        commonKey: Pair<String, GCKey>,
        dataKey: Pair<GCKey, EncryptedKey>,
        attachmentKey: Pair<GCKey, EncryptedKey>?,
        tagEncryptionKey: GCKey,
        userId: String = USER_ID,
        alias: String = ALIAS,
        startDate: String?,
        endDate: String?,
        pageSize: Int,
        offset: Int
    ) {
        val (encodedSearchTags, legacySearchTags) = flowHelper.prepareCompatibilityTags(searchTags)

        val encryptedTagsAndAnnotations = flowHelper.hashAndEncodeTagsAndAnnotations(
            flowHelper.mergeTags(encodedSearchTags, encodedAnnotations)
        )

        val encryptedLegacyTagsAndAnnotations = flowHelper.hashAndEncodeTagsAndAnnotations(
            flowHelper.mergeTags(legacySearchTags, legacyAnnotations)
        )

        val encryptedCommonKey = flowHelper.prepareStoredOrUnstoredCommonKeyRun(
            alias,
            userId,
            commonKey.first,
            useStoredCommonKey
        )
        val receivedIteration = CryptoServiceIteration(
            gcKeyOrder = emptyList(),
            commonKey = commonKey.second,
            commonKeyId = commonKey.first,
            commonKeyIsStored = useStoredCommonKey,
            commonKeyFetchCalls = 1,
            encryptedCommonKey = encryptedCommonKey,
            dataKey = dataKey.first,
            encryptedDataKey = dataKey.second,
            attachmentKey = attachmentKey?.first,
            encryptedAttachmentKey = attachmentKey?.second,
            tagEncryptionKey = tagEncryptionKey,
            tagEncryptionKeyCalls = tagEncryptionKeyCalls,
            resources = serializedResources.toList(),
            tags = flowHelper.mergeTags(encodedTags, legacyTags),
            annotations = flowHelper.mergeTags(encodedAnnotations, legacyAnnotations),
            hashFunction = { value -> flowHelper.md5(value) }
        )

        val search = slot<String>()

        (cryptoService as CryptoServiceFake).iteration = receivedIteration

        every {
            apiService.fetchRecords(
                ALIAS,
                USER_ID,
                startDate,
                endDate,
                pageSize,
                offset,
                capture(search)
            )
        } answers {
            val record = when (search.captured) {
                encryptedTagsAndAnnotations.joinToString(",") -> encryptedRecord
                encryptedLegacyTagsAndAnnotations.joinToString(",") -> encryptedLegacyRecord
                else -> throw RuntimeException(
                    "Unexpected tags and annotations:\n${search.captured}"
                )
            }

            Observable.fromCallable { listOf(record) }
        }
    }

    private fun runFhirBatchFlow(
        serializedResources: Pair<String, String>,
        tags: Map<String, String>,
        searchTags: Map<String, String>,
        annotations: List<String> = emptyList(),
        tagEncryptionKeyCalls: Int = 3,
        useStoredCommonKey: Boolean = true,
        commonKey: Pair<String, GCKey> = COMMON_KEY_ID to this.commonKey,
        dataKey: Pair<GCKey, EncryptedKey> = this.dataKey to encryptedDataKey,
        attachmentKey: Pair<GCKey, EncryptedKey>? = this.attachmentKey to encryptedAttachmentKey,
        tagEncryptionKey: GCKey = this.tagEncryptionKey,
        recordIds: Pair<String, String> = RECORD_ID to RECORD_ID_COMPATIBILITY,
        userId: String = USER_ID,
        alias: String = ALIAS,
        creationDate: String = CREATION_DATE,
        updateDate: String = UPDATE_DATE,
        startDate: String? = null,
        endDate: String? = null,
        pageSize: Int = PAGE_SIZE,
        offset: Int = OFFSET
    ) {
        val (encodedTags, legacyTags) = flowHelper.prepareCompatibilityTags(tags)
        val (encodedAnnotations, legacyAnnotations) = flowHelper.prepareCompatibilityAnnotations(
            annotations
        )

        val encryptedRecord = flowHelper.prepareEncryptedFhirRecord(
            recordIds.first,
            serializedResources.first,
            encodedTags,
            encodedAnnotations,
            commonKey.first,
            dataKey.second,
            attachmentKey?.second,
            creationDate,
            updateDate
        )

        val encryptedLegacyRecord = flowHelper.prepareEncryptedFhirRecord(
            recordIds.second,
            serializedResources.second,
            legacyTags,
            legacyAnnotations,
            commonKey.first,
            dataKey.second,
            attachmentKey?.second,
            creationDate,
            updateDate
        )

        runBatchFlow(
            serializedResources,
            encryptedRecord,
            encryptedLegacyRecord,
            searchTags,
            encodedTags,
            encodedAnnotations,
            legacyTags,
            legacyAnnotations,
            tagEncryptionKeyCalls,
            useStoredCommonKey,
            commonKey,
            dataKey,
            attachmentKey,
            tagEncryptionKey,
            userId,
            alias,
            startDate,
            endDate,
            pageSize,
            offset
        )
    }

    private fun runDataBatchFlow(
        serializedResources: Pair<String, String>,
        tags: Map<String, String>,
        searchTags: Map<String, String>,
        annotations: List<String> = emptyList(),
        tagEncryptionKeyCalls: Int = 3,
        useStoredCommonKey: Boolean = true,
        commonKey: Pair<String, GCKey> = COMMON_KEY_ID to this.commonKey,
        dataKey: Pair<GCKey, EncryptedKey> = this.dataKey to encryptedDataKey,
        attachmentKey: Pair<GCKey, EncryptedKey>? = this.attachmentKey to encryptedAttachmentKey,
        tagEncryptionKey: GCKey = this.tagEncryptionKey,
        recordIds: Pair<String, String> = RECORD_ID to RECORD_ID_COMPATIBILITY,
        userId: String = USER_ID,
        alias: String = ALIAS,
        creationDate: String = CREATION_DATE,
        updateDate: String = UPDATE_DATE,
        startDate: String? = null,
        endDate: String? = null,
        pageSize: Int = PAGE_SIZE,
        offset: Int = OFFSET
    ) {
        val (encodedTags, legacyTags) = flowHelper.prepareCompatibilityTags(tags)
        val (encodedAnnotations, legacyAnnotations) = flowHelper.prepareCompatibilityAnnotations(
            annotations
        )

        val encryptedRecord = flowHelper.prepareEncryptedDataRecord(
            recordIds.first,
            serializedResources.first,
            encodedTags,
            encodedAnnotations,
            commonKey.first,
            dataKey.second,
            attachmentKey?.second,
            creationDate,
            updateDate
        )

        val encryptedLegacyRecord = flowHelper.prepareEncryptedDataRecord(
            recordIds.second,
            serializedResources.second,
            legacyTags,
            legacyAnnotations,
            commonKey.first,
            dataKey.second,
            attachmentKey?.second,
            creationDate,
            updateDate
        )

        runBatchFlow(
            serializedResources,
            encryptedRecord,
            encryptedLegacyRecord,
            searchTags,
            encodedTags,
            encodedAnnotations,
            legacyTags,
            legacyAnnotations,
            tagEncryptionKeyCalls,
            useStoredCommonKey,
            commonKey,
            dataKey,
            attachmentKey,
            tagEncryptionKey,
            userId,
            alias,
            startDate,
            endDate,
            pageSize,
            offset
        )
    }

    // Fetch
    // FHIR3
    @Test
    fun `Given, fetchFhir3Record is called, with a UserId and RecordId, it returns a Record`() {
        // Given
        val resourceType = "DocumentReference"
        val tags = mapOf(
            "partner" to PARTNER_ID,
            "client" to CLIENT_ID,
            "fhirversion" to "3.0.1",
            "resourcetype" to resourceType
        )

        val template = TestResourceHelper.loadTemplate(
            "common",
            "documentReference-without-attachment-template",
            RECORD_ID,
            PARTNER_ID
        )

        val resource = SdkFhirParser.toFhir3(
            resourceType,
            template
        ) as Fhir3DocumentReference

        runFhirFetchFlow(
            serializedResource = SdkFhirParser.fromResource(resource)!!,
            tags = tags
        )

        // When
        val result = recordService.fetchFhir3Record<Fhir3DocumentReference>(
            USER_ID,
            RECORD_ID
        ).blockingGet()

        // Then
        assertTrue(result is Record)
        assertEquals(
            expected = flowHelper.buildMeta(CREATION_DATE, UPDATE_DATE),
            actual = result.meta
        )
        assertTrue(result.annotations!!.isEmpty())
        assertEquals(
            expected = SdkFhirParser.fromResource(resource),
            actual = SdkFhirParser.fromResource(result.resource)
        )
    }

    @Test
    fun `Given, fetchFhir3Record is called, with a UserId and RecordId, it returns a Record with Annotations`() {
        // Given
        val resourceType = "DocumentReference"
        val tags = mapOf(
            "partner" to PARTNER_ID,
            "client" to CLIENT_ID,
            "fhirversion" to "3.0.1",
            "resourcetype" to resourceType
        )

        val annotations = listOf(
            "wow",
            "it",
            "works",
            "and",
            "like_a_duracell_häsi"
        )

        val template = TestResourceHelper.loadTemplate(
            "common",
            "documentReference-without-attachment-template",
            RECORD_ID,
            PARTNER_ID
        )

        val resource = SdkFhirParser.toFhir3(
            resourceType,
            template
        ) as Fhir3DocumentReference

        runFhirFetchFlow(
            serializedResource = SdkFhirParser.fromResource(resource)!!,
            tags = tags,
            annotations = annotations,
            useStoredCommonKey = false
        )

        // When
        val result = recordService.fetchFhir3Record<Fhir3DocumentReference>(
            USER_ID,
            RECORD_ID
        ).blockingGet()

        // Then
        assertTrue(result is Record)
        assertEquals(
            expected = flowHelper.buildMeta(CREATION_DATE, UPDATE_DATE),
            actual = result.meta
        )
        assertEquals(
            actual = result.annotations,
            expected = annotations
        )
        assertEquals(
            expected = SdkFhirParser.fromResource(resource),
            actual = SdkFhirParser.fromResource(result.resource)
        )
    }

    // FHIR4
    @Test
    fun `Given, fetchFhir4Record is called, with a UserId and RecordId, it returns a Record`() {
        // Given
        val resourceType = "DocumentReference"
        val tags = mapOf(
            "partner" to PARTNER_ID,
            "client" to CLIENT_ID,
            "fhirversion" to "4.0.1",
            "resourcetype" to resourceType
        )

        val template = TestResourceHelper.loadTemplate(
            "common",
            "documentReference-without-attachment-template",
            RECORD_ID,
            PARTNER_ID
        )

        val resource = SdkFhirParser.toFhir4(
            resourceType,
            template
        ) as Fhir4DocumentReference

        runFhirFetchFlow(
            serializedResource = SdkFhirParser.fromResource(resource)!!,
            tags = tags
        )

        // When
        val result = recordService.fetchFhir4Record<Fhir4DocumentReference>(
            USER_ID,
            RECORD_ID
        ).blockingGet()

        // Then
        assertTrue(result is Fhir4Record)
        assertEquals(
            expected = flowHelper.buildMeta(CREATION_DATE, UPDATE_DATE),
            actual = result.meta
        )
        assertTrue(result.annotations.isEmpty())
        assertEquals(
            expected = SdkFhirParser.fromResource(resource),
            actual = SdkFhirParser.fromResource(result.resource)
        )
    }

    @Test
    fun `Given, fetchFhir4Record is called, with a UserId and RecordId, it returns a Record with Annotations`() {
        // Given
        val resourceType = "DocumentReference"
        val tags = mapOf(
            "partner" to PARTNER_ID,
            "client" to CLIENT_ID,
            "fhirversion" to "4.0.1",
            "resourcetype" to resourceType
        )

        val annotations = listOf(
            "wow",
            "it",
            "works",
            "and",
            "like_a_duracell_häsi"
        )

        val template = TestResourceHelper.loadTemplate(
            "common",
            "documentReference-without-attachment-template",
            RECORD_ID,
            PARTNER_ID
        )

        val resource = SdkFhirParser.toFhir4(
            resourceType,
            template
        ) as Fhir4DocumentReference

        runFhirFetchFlow(
            serializedResource = SdkFhirParser.fromResource(resource)!!,
            tags = tags,
            annotations = annotations,
            useStoredCommonKey = false
        )

        // When
        val result = recordService.fetchFhir4Record<Fhir4DocumentReference>(
            USER_ID,
            RECORD_ID
        ).blockingGet()

        // Then
        assertTrue(result is Fhir4Record)
        assertEquals(
            expected = flowHelper.buildMeta(CREATION_DATE, UPDATE_DATE),
            actual = result.meta
        )
        assertEquals(
            actual = result.annotations,
            expected = annotations
        )
        assertEquals(
            expected = SdkFhirParser.fromResource(resource),
            actual = SdkFhirParser.fromResource(result.resource)
        )
    }

    // Arbitrary Data
    @Test
    fun `Given, fetchDataRecord is called, with a UserId and RecordId, it returns a Record`() {
        // Given
        val resource = "The never ending story."
        val tags = mapOf(
            "flag" to ARBITRARY_DATA_KEY,
            "partner" to PARTNER_ID,
            "client" to CLIENT_ID
        )

        runDataFetchFlow(
            serializedResource = resource,
            tags = tags
        )

        // When
        val result = recordService.fetchDataRecord(
            USER_ID,
            RECORD_ID
        ).blockingGet()

        // Then
        assertTrue(result is DataRecord)
        assertEquals(
            expected = flowHelper.buildMeta(CREATION_DATE, UPDATE_DATE),
            actual = result.meta
        )
        assertTrue(result.annotations.isEmpty())
        assertEquals(
            expected = resource,
            actual = String(result.resource.value)
        )
    }

    @Test
    fun `Given, fetchDataRecord is called, with a UserId and RecordId, it returns a Record with Annotations`() {
        // Given
        val resource = "The never ending story."
        val tags = mapOf(
            "flag" to ARBITRARY_DATA_KEY,
            "partner" to PARTNER_ID,
            "client" to CLIENT_ID
        )

        val annotations = listOf(
            "wow",
            "it",
            "works",
            "and",
            "like_a_duracell_häsi"
        )

        runDataFetchFlow(
            serializedResource = resource,
            tags = tags,
            annotations = annotations,
            useStoredCommonKey = false
        )

        // When
        val result = recordService.fetchDataRecord(
            USER_ID,
            RECORD_ID
        ).blockingGet()

        // Then
        assertTrue(result is DataRecord)
        assertEquals(
            expected = flowHelper.buildMeta(CREATION_DATE, UPDATE_DATE),
            actual = result.meta
        )
        assertEquals(
            actual = result.annotations,
            expected = annotations
        )
        assertEquals(
            expected = resource,
            actual = String(result.resource.value)
        )
    }

    // search
    // FHIR 3
    @Test
    fun `Given, fetchFhir3Records is called, with its appropriate payloads, it returns a List of Records`() {
        // Given
        val resourceType = "DocumentReference"
        val tags = mapOf(
            "partner" to PARTNER_ID,
            "client" to CLIENT_ID,
            "fhirversion" to "3.0.1",
            "resourcetype" to resourceType
        )

        val searchTags = mapOf(
            "fhirversion" to "3.0.1",
            "resourcetype" to resourceType
        )

        val template = TestResourceHelper.loadTemplate(
            "common",
            "documentReference-without-attachment-template",
            RECORD_ID,
            PARTNER_ID
        )

        val template2 = TestResourceHelper.loadTemplate(
            "common",
            "documentReference-without-attachment-template",
            RECORD_ID_COMPATIBILITY,
            PARTNER_ID
        )

        val resource = SdkFhirParser.toFhir3(
            resourceType,
            template
        ) as Fhir3DocumentReference

        val legacyResource = SdkFhirParser.toFhir3(
            resourceType,
            template2
        ) as Fhir3DocumentReference

        legacyResource.description = "legacyRecord"

        runFhirBatchFlow(
            serializedResources = SdkFhirParser.fromResource(resource)!! to SdkFhirParser.fromResource(
                legacyResource
            )!!,
            tags = tags,
            searchTags = searchTags,
            useStoredCommonKey = false
        )

        // When
        val result = recordService.fetchFhir3Records(
            USER_ID,
            Fhir3DocumentReference::class.java,
            emptyList(),
            null,
            null,
            PAGE_SIZE,
            OFFSET
        ).blockingGet()

        // Then
        assertEquals(
            expected = 2,
            actual = result.size
        )
        assertEquals(
            actual = SdkFhirParser.fromResource(result[0].resource),
            expected = SdkFhirParser.fromResource(resource)
        )
        assertEquals(
            actual = SdkFhirParser.fromResource(result[1].resource),
            expected = SdkFhirParser.fromResource(legacyResource)
        )
        assertTrue(result[0].annotations.isNullOrEmpty())
        assertTrue(result[1].annotations.isNullOrEmpty())
    }

    @Test
    fun `Given, fetchFhir3Records is called, with its appropriate payloads, it returns a List of Records filtered by Annotations`() {
        // Given
        val resourceType = "DocumentReference"
        val tags = mapOf(
            "partner" to PARTNER_ID,
            "client" to CLIENT_ID,
            "fhirversion" to "3.0.1",
            "resourcetype" to resourceType
        )

        val searchTags = mapOf(
            "fhirversion" to "3.0.1",
            "resourcetype" to resourceType
        )

        val annotations = listOf(
            "wow",
            "it",
            "works",
            "and",
            "like_a_duracell_häsi"
        )

        val template = TestResourceHelper.loadTemplate(
            "common",
            "documentReference-without-attachment-template",
            RECORD_ID,
            PARTNER_ID
        )

        val template2 = TestResourceHelper.loadTemplate(
            "common",
            "documentReference-without-attachment-template",
            RECORD_ID_COMPATIBILITY,
            PARTNER_ID
        )

        val resource = SdkFhirParser.toFhir3(
            resourceType,
            template
        ) as Fhir3DocumentReference

        val legacyResource = SdkFhirParser.toFhir3(
            resourceType,
            template2
        ) as Fhir3DocumentReference

        legacyResource.description = "legacyRecord"

        runFhirBatchFlow(
            serializedResources = SdkFhirParser.fromResource(resource)!! to SdkFhirParser.fromResource(
                legacyResource
            )!!,
            tags = tags,
            annotations = annotations,
            searchTags = searchTags
        )

        // When
        val result = recordService.fetchFhir3Records(
            USER_ID,
            Fhir3DocumentReference::class.java,
            annotations,
            null,
            null,
            PAGE_SIZE,
            OFFSET
        ).blockingGet()

        // Then
        assertEquals(
            expected = 2,
            actual = result.size
        )
        assertEquals(
            actual = SdkFhirParser.fromResource(result[0].resource),
            expected = SdkFhirParser.fromResource(resource)
        )
        assertEquals(
            actual = SdkFhirParser.fromResource(result[1].resource),
            expected = SdkFhirParser.fromResource(legacyResource)
        )
        assertEquals(
            actual = result[0].annotations,
            expected = annotations
        )
        assertEquals(
            actual = result[1].annotations,
            expected = annotations
        )
    }

    @Test
    fun `Given, fetchFhir3Records is called, with its appropriate payloads, it returns a List of Records filtered by the provided date parameter and Annotations`() {
        // Given
        val resourceType = "DocumentReference"
        val tags = mapOf(
            "partner" to PARTNER_ID,
            "client" to CLIENT_ID,
            "fhirversion" to "3.0.1",
            "resourcetype" to resourceType
        )

        val searchTags = mapOf(
            "fhirversion" to "3.0.1",
            "resourcetype" to resourceType
        )

        val annotations = listOf(
            "wow",
            "it",
            "works",
            "and",
            "like_a_duracell_häsi"
        )

        val startDate = LocalDate.of(2020, 1, 1)
        val endDate = LocalDate.of(2020, 12, 1)

        val template = TestResourceHelper.loadTemplate(
            "common",
            "documentReference-without-attachment-template",
            RECORD_ID,
            PARTNER_ID
        )

        val template2 = TestResourceHelper.loadTemplate(
            "common",
            "documentReference-without-attachment-template",
            RECORD_ID_COMPATIBILITY,
            PARTNER_ID
        )

        val resource = SdkFhirParser.toFhir3(
            resourceType,
            template
        ) as Fhir3DocumentReference

        val legacyResource = SdkFhirParser.toFhir3(
            resourceType,
            template2
        ) as Fhir3DocumentReference

        legacyResource.description = "legacyRecord"

        runFhirBatchFlow(
            serializedResources = SdkFhirParser.fromResource(resource)!! to SdkFhirParser.fromResource(
                legacyResource
            )!!,
            tags = tags,
            annotations = annotations,
            searchTags = searchTags,
            startDate = startDate.toString(),
            endDate = endDate.toString()
        )

        // When
        val result = recordService.fetchFhir3Records(
            USER_ID,
            Fhir3DocumentReference::class.java,
            annotations,
            startDate,
            endDate,
            PAGE_SIZE,
            OFFSET
        ).blockingGet()

        // Then
        assertEquals(
            expected = 2,
            actual = result.size
        )
        assertEquals(
            actual = SdkFhirParser.fromResource(result[0].resource),
            expected = SdkFhirParser.fromResource(resource)
        )
        assertEquals(
            actual = SdkFhirParser.fromResource(result[1].resource),
            expected = SdkFhirParser.fromResource(legacyResource)
        )
        assertEquals(
            actual = result[0].annotations,
            expected = annotations
        )
        assertEquals(
            actual = result[1].annotations,
            expected = annotations
        )
    }

    // FHIR 4
    @Test
    fun `Given, fetchFhir4Records is called, with its appropriate payloads, it returns a List of Records`() {
        // Given
        val resourceType = "DocumentReference"
        val tags = mapOf(
            "partner" to PARTNER_ID,
            "client" to CLIENT_ID,
            "fhirversion" to "4.0.1",
            "resourcetype" to resourceType
        )

        val searchTags = mapOf(
            "fhirversion" to "4.0.1",
            "resourcetype" to resourceType
        )

        val template = TestResourceHelper.loadTemplate(
            "common",
            "documentReference-without-attachment-template",
            RECORD_ID,
            PARTNER_ID
        )

        val template2 = TestResourceHelper.loadTemplate(
            "common",
            "documentReference-without-attachment-template",
            RECORD_ID_COMPATIBILITY,
            PARTNER_ID
        )

        val resource = SdkFhirParser.toFhir4(
            resourceType,
            template
        ) as Fhir4DocumentReference

        val legacyResource = SdkFhirParser.toFhir4(
            resourceType,
            template2
        ) as Fhir4DocumentReference

        legacyResource.description = "legacyRecord"

        runFhirBatchFlow(
            serializedResources = SdkFhirParser.fromResource(resource)!! to SdkFhirParser.fromResource(
                legacyResource
            )!!,
            tags = tags,
            searchTags = searchTags,
            useStoredCommonKey = false
        )

        // When
        val result = recordService.fetchFhir4Records(
            USER_ID,
            Fhir4DocumentReference::class.java,
            emptyList(),
            null,
            null,
            PAGE_SIZE,
            OFFSET
        ).blockingGet()

        // Then
        assertEquals(
            expected = 2,
            actual = result.size
        )
        assertEquals(
            actual = SdkFhirParser.fromResource(result[0].resource),
            expected = SdkFhirParser.fromResource(resource)
        )
        assertEquals(
            actual = SdkFhirParser.fromResource(result[1].resource),
            expected = SdkFhirParser.fromResource(legacyResource)
        )
        assertTrue(result[0].annotations.isNullOrEmpty())
        assertTrue(result[1].annotations.isNullOrEmpty())
    }

    @Test
    fun `Given, fetchFhir4Records is called, with its appropriate payloads, it returns a List of Records filtered by Annotations`() {
        // Given
        val resourceType = "DocumentReference"
        val tags = mapOf(
            "partner" to PARTNER_ID,
            "client" to CLIENT_ID,
            "fhirversion" to "4.0.1",
            "resourcetype" to resourceType
        )

        val searchTags = mapOf(
            "fhirversion" to "4.0.1",
            "resourcetype" to resourceType
        )

        val annotations = listOf(
            "wow",
            "it",
            "works",
            "and",
            "like_a_duracell_häsi"
        )

        val template = TestResourceHelper.loadTemplate(
            "common",
            "documentReference-without-attachment-template",
            RECORD_ID,
            PARTNER_ID
        )

        val template2 = TestResourceHelper.loadTemplate(
            "common",
            "documentReference-without-attachment-template",
            RECORD_ID_COMPATIBILITY,
            PARTNER_ID
        )

        val resource = SdkFhirParser.toFhir4(
            resourceType,
            template
        ) as Fhir4DocumentReference

        val legacyResource = SdkFhirParser.toFhir4(
            resourceType,
            template2
        ) as Fhir4DocumentReference

        legacyResource.description = "legacyRecord"

        runFhirBatchFlow(
            serializedResources = SdkFhirParser.fromResource(resource)!! to SdkFhirParser.fromResource(
                legacyResource
            )!!,
            tags = tags,
            annotations = annotations,
            searchTags = searchTags
        )

        // When
        val result = recordService.fetchFhir4Records(
            USER_ID,
            Fhir4DocumentReference::class.java,
            annotations,
            null,
            null,
            PAGE_SIZE,
            OFFSET
        ).blockingGet()

        // Then
        assertEquals(
            expected = 2,
            actual = result.size
        )
        assertEquals(
            actual = SdkFhirParser.fromResource(result[0].resource),
            expected = SdkFhirParser.fromResource(resource)
        )
        assertEquals(
            actual = SdkFhirParser.fromResource(result[1].resource),
            expected = SdkFhirParser.fromResource(legacyResource)
        )
        assertEquals(
            actual = result[0].annotations,
            expected = annotations
        )
        assertEquals(
            actual = result[1].annotations,
            expected = annotations
        )
    }

    @Test
    fun `Given, fetchFhir4Records is called, with its appropriate payloads, it returns a List of Records filtered by the provided date parameter and Annotations`() {
        // Given
        val resourceType = "DocumentReference"
        val tags = mapOf(
            "partner" to PARTNER_ID,
            "client" to CLIENT_ID,
            "fhirversion" to "4.0.1",
            "resourcetype" to resourceType
        )

        val searchTags = mapOf(
            "fhirversion" to "4.0.1",
            "resourcetype" to resourceType
        )

        val annotations = listOf(
            "wow",
            "it",
            "works",
            "and",
            "like_a_duracell_häsi"
        )

        val startDate = LocalDate.of(2020, 1, 1)
        val endDate = LocalDate.of(2020, 12, 1)

        val template = TestResourceHelper.loadTemplate(
            "common",
            "documentReference-without-attachment-template",
            RECORD_ID,
            PARTNER_ID
        )

        val template2 = TestResourceHelper.loadTemplate(
            "common",
            "documentReference-without-attachment-template",
            RECORD_ID_COMPATIBILITY,
            PARTNER_ID
        )

        val resource = SdkFhirParser.toFhir4(
            resourceType,
            template
        ) as Fhir4DocumentReference

        val legacyResource = SdkFhirParser.toFhir4(
            resourceType,
            template2
        ) as Fhir4DocumentReference

        legacyResource.description = "legacyRecord"

        runFhirBatchFlow(
            serializedResources = SdkFhirParser.fromResource(resource)!! to SdkFhirParser.fromResource(
                legacyResource
            )!!,
            tags = tags,
            annotations = annotations,
            searchTags = searchTags,
            startDate = startDate.toString(),
            endDate = endDate.toString()
        )

        // When
        val result = recordService.fetchFhir4Records(
            USER_ID,
            Fhir4DocumentReference::class.java,
            annotations,
            startDate,
            endDate,
            PAGE_SIZE,
            OFFSET
        ).blockingGet()

        // Then
        assertEquals(
            expected = 2,
            actual = result.size
        )
        assertEquals(
            actual = SdkFhirParser.fromResource(result[0].resource),
            expected = SdkFhirParser.fromResource(resource)
        )
        assertEquals(
            actual = SdkFhirParser.fromResource(result[1].resource),
            expected = SdkFhirParser.fromResource(legacyResource)
        )
        assertEquals(
            actual = result[0].annotations,
            expected = annotations
        )
        assertEquals(
            actual = result[1].annotations,
            expected = annotations
        )
    }

    // Arbitrary Data
    @Test
    fun `Given, fetchDataRecords is called, with its appropriate payloads, it returns a List of Records`() {
        // Given
        val resource = "The never ending story."
        val tags = mapOf(
            "flag" to ARBITRARY_DATA_KEY,
            "partner" to PARTNER_ID,
            "client" to CLIENT_ID
        )

        val searchTags = mapOf(
            "flag" to ARBITRARY_DATA_KEY
        )

        runDataBatchFlow(
            serializedResources = resource to resource,
            tags = tags,
            searchTags = searchTags,
            useStoredCommonKey = false
        )

        // When
        val result = recordService.fetchDataRecords(
            USER_ID,
            emptyList(),
            null,
            null,
            PAGE_SIZE,
            OFFSET
        ).blockingGet()

        // Then
        assertEquals(
            expected = 1,
            actual = result.size
        )
        assertEquals(
            actual = String(result[0].resource.value),
            expected = resource
        )
        assertTrue(result[0].annotations.isNullOrEmpty())
    }

    @Test
    fun `Given, fetchDataRecords is called, with its appropriate payloads, it returns a List of Records filtered by Annotations`() {
        // Given
        val resource = "The never ending story."
        val legacyResource = "Completely new design, ey."

        val tags = mapOf(
            "flag" to ARBITRARY_DATA_KEY,
            "partner" to PARTNER_ID,
            "client" to CLIENT_ID
        )

        val searchTags = mapOf(
            "flag" to ARBITRARY_DATA_KEY
        )

        val annotations = listOf(
            "wow",
            "it",
            "works",
            "and",
            "like_a_duracell_häsi"
        )

        runDataBatchFlow(
            serializedResources = resource to legacyResource,
            tags = tags,
            annotations = annotations,
            searchTags = searchTags
        )

        // When
        val result = recordService.fetchDataRecords(
            USER_ID,
            annotations,
            null,
            null,
            PAGE_SIZE,
            OFFSET
        ).blockingGet()

        // Then
        assertEquals(
            expected = 2,
            actual = result.size
        )
        assertEquals(
            actual = String(result[0].resource.value),
            expected = resource
        )
        assertEquals(
            actual = String(result[1].resource.value),
            expected = legacyResource
        )
        assertEquals(
            actual = result[0].annotations,
            expected = annotations
        )
        assertEquals(
            actual = result[1].annotations,
            expected = annotations
        )
    }

    @Test
    fun `Given, fetchDataRecords is called, with its appropriate payloads, it returns a List of Records filtered by the provided date parameter and Annotations`() {
        // Given
        val resource = "The never ending story."
        val legacyResource = "Completely new design, ey."

        val tags = mapOf(
            "flag" to ARBITRARY_DATA_KEY,
            "partner" to PARTNER_ID,
            "client" to CLIENT_ID
        )

        val searchTags = mapOf(
            "flag" to ARBITRARY_DATA_KEY
        )

        val annotations = listOf(
            "wow",
            "it",
            "works",
            "and",
            "like_a_duracell_häsi"
        )

        val startDate = LocalDate.of(2020, 1, 1)
        val endDate = LocalDate.of(2020, 12, 1)

        runDataBatchFlow(
            serializedResources = resource to legacyResource,
            tags = tags,
            annotations = annotations,
            searchTags = searchTags,
            startDate = startDate.toString(),
            endDate = endDate.toString()
        )

        // When
        val result = recordService.fetchDataRecords(
            USER_ID,
            annotations,
            startDate,
            endDate,
            PAGE_SIZE,
            OFFSET
        ).blockingGet()

        // Then
        assertEquals(
            expected = 2,
            actual = result.size
        )
        assertEquals(
            actual = String(result[0].resource.value),
            expected = resource
        )
        assertEquals(
            actual = String(result[1].resource.value),
            expected = legacyResource
        )
        assertEquals(
            actual = result[0].annotations,
            expected = annotations
        )
        assertEquals(
            actual = result[1].annotations,
            expected = annotations
        )
    }
}
