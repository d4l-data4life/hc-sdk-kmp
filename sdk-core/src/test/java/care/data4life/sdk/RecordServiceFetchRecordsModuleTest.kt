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

import care.data4life.fhir.r4.model.DocumentReference as Fhir4DocumentReference
import care.data4life.fhir.stu3.model.DocumentReference as Fhir3DocumentReference
import care.data4life.sdk.attachment.AttachmentContract
import care.data4life.sdk.attachment.AttachmentService
import care.data4life.sdk.call.DataRecord
import care.data4life.sdk.call.Fhir4Record
import care.data4life.sdk.crypto.CryptoContract
import care.data4life.sdk.crypto.GCKey
import care.data4life.sdk.fhir.Fhir3Resource
import care.data4life.sdk.fhir.Fhir4Resource
import care.data4life.sdk.fhir.FhirContract
import care.data4life.sdk.fhir.ResourceCryptoService
import care.data4life.sdk.model.Record
import care.data4life.sdk.network.NetworkingContract
import care.data4life.sdk.network.model.EncryptedKey
import care.data4life.sdk.network.model.EncryptedRecord
import care.data4life.sdk.record.RecordContract
import care.data4life.sdk.tag.Annotations
import care.data4life.sdk.tag.TagCryptoService
import care.data4life.sdk.tag.TaggingService
import care.data4life.sdk.tag.Tags
import care.data4life.sdk.test.fake.CryptoServiceFake
import care.data4life.sdk.test.fake.CryptoServiceIteration
import care.data4life.sdk.test.util.GenericTestDataProvider.ALIAS
import care.data4life.sdk.test.util.GenericTestDataProvider.ARBITRARY_DATA_KEY
import care.data4life.sdk.test.util.GenericTestDataProvider.CLIENT_ID
import care.data4life.sdk.test.util.GenericTestDataProvider.COMMON_KEY_ID
import care.data4life.sdk.test.util.GenericTestDataProvider.CREATION_DATE
import care.data4life.sdk.test.util.GenericTestDataProvider.PARTNER_ID
import care.data4life.sdk.test.util.GenericTestDataProvider.RECORD_ID
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
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.Before
import org.junit.Test

class RecordServiceFetchRecordsModuleTest {
    private val dataKey: GCKey = mockk()
    private val attachmentKey: GCKey = mockk()
    private val tagEncryptionKey: GCKey = mockk()
    private val commonKey: GCKey = mockk()
    private val encryptedDataKey: EncryptedKey = mockk()
    private val encryptedAttachmentKey: EncryptedKey = mockk()

    private lateinit var recordService: RecordContract.Service
    private lateinit var flowHelper: RecordServiceModuleTestFlowHelper
    private val apiService: NetworkingContract.Service = mockk()
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
            TagCryptoService(cryptoService),
            TaggingService(CLIENT_ID),
            ResourceCryptoService(cryptoService),
            AttachmentService(
                fileService,
                imageResizer
            ),
            cryptoService,
            errorHandler
        )

        flowHelper = RecordServiceModuleTestFlowHelper(
            apiService,
            imageResizer
        )
    }

    private fun runFetchFlow(
        serializedResource: String,
        encryptedRecord: EncryptedRecord,
        tags: List<String>,
        annotations: Annotations,
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
        tags: Tags,
        annotations: Annotations = emptyList(),
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

        val encryptedRecord = flowHelper.prepareEncryptedRecord(
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
        tags: Tags,
        annotations: Annotations = emptyList(),
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

        val encryptedRecord = flowHelper.prepareEncryptedRecord(
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
        serializedResources: Triple<String, String, String>,
        encryptedRecords: List<EncryptedRecord>,
        searchTags: String,
        encodedTags: List<String>,
        encodedAnnotations: List<String>,
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
            tags = encodedTags,
            annotations = encodedAnnotations,
            hashFunction = { value -> flowHelper.md5(value) }
        )

        val search = slot<NetworkingContract.SearchTags>()

        (cryptoService as CryptoServiceFake).iteration = receivedIteration

        every {
            apiService.searchRecords(
                ALIAS,
                USER_ID,
                startDate,
                endDate,
                null,
                null,
                false,
                pageSize,
                offset,
                capture(search)
            )
        } answers {
            val actual = flowHelper.decryptSerializedTags(
                search.captured.tagGroups,
                cryptoService,
                tagEncryptionKey
            )

            if (searchTags == actual) {
                Observable.fromCallable { encryptedRecords }
            } else {
                throw RuntimeException(
                    "Unexpected tags and annotations - \nexpected: $searchTags\ngot: $actual"
                )
            }
        }
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

        val resource = SdkFhirParser.toFhir<Fhir3Resource>(
            resourceType,
            FhirContract.FhirVersion.FHIR_3.version,
            template
        ) as Fhir3DocumentReference

        runFhirFetchFlow(
            serializedResource = SdkFhirParser.fromResource(resource),
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

        val resource = SdkFhirParser.toFhir<Fhir3Resource>(
            resourceType,
            FhirContract.FhirVersion.FHIR_3.version,
            template
        ) as Fhir3DocumentReference

        runFhirFetchFlow(
            serializedResource = SdkFhirParser.fromResource(resource),
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

        val resource = SdkFhirParser.toFhir<Fhir4Resource>(
            resourceType,
            FhirContract.FhirVersion.FHIR_4.version,
            template
        ) as Fhir4DocumentReference

        runFhirFetchFlow(
            serializedResource = SdkFhirParser.fromResource(resource),
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

        val resource = SdkFhirParser.toFhir<Fhir4Resource>(
            resourceType,
            FhirContract.FhirVersion.FHIR_4.version,
            template
        ) as Fhir4DocumentReference

        runFhirFetchFlow(
            serializedResource = SdkFhirParser.fromResource(resource),
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
}
