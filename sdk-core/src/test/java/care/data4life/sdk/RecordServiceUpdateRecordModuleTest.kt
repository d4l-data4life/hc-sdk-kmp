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
import care.data4life.sdk.attachment.AttachmentService
import care.data4life.sdk.attachment.FileService
import care.data4life.sdk.call.DataRecord
import care.data4life.sdk.call.Fhir4Record
import care.data4life.sdk.crypto.CryptoContract
import care.data4life.sdk.crypto.GCKey
import care.data4life.sdk.data.DataResource
import care.data4life.sdk.fhir.Fhir3Identifier
import care.data4life.sdk.fhir.Fhir3Resource
import care.data4life.sdk.fhir.Fhir4Identifier
import care.data4life.sdk.fhir.Fhir4Resource
import care.data4life.sdk.fhir.FhirContract
import care.data4life.sdk.fhir.ResourceCryptoService
import care.data4life.sdk.lang.DataValidationException
import care.data4life.sdk.model.Record
import care.data4life.sdk.network.NetworkingContract
import care.data4life.sdk.network.model.EncryptedKey
import care.data4life.sdk.network.model.EncryptedRecord
import care.data4life.sdk.network.model.NetworkModelContract
import care.data4life.sdk.record.RecordContract
import care.data4life.sdk.tag.Annotations
import care.data4life.sdk.tag.TagCryptoService
import care.data4life.sdk.tag.TaggingService
import care.data4life.sdk.tag.Tags
import care.data4life.sdk.test.fake.CryptoServiceFake
import care.data4life.sdk.test.fake.CryptoServiceIteration
import care.data4life.sdk.test.util.GenericTestDataProvider.ALIAS
import care.data4life.sdk.test.util.GenericTestDataProvider.ARBITRARY_DATA_KEY
import care.data4life.sdk.test.util.GenericTestDataProvider.ATTACHMENT_ID
import care.data4life.sdk.test.util.GenericTestDataProvider.CLIENT_ID
import care.data4life.sdk.test.util.GenericTestDataProvider.COMMON_KEY_ID
import care.data4life.sdk.test.util.GenericTestDataProvider.CREATION_DATE
import care.data4life.sdk.test.util.GenericTestDataProvider.PARTNER_ID
import care.data4life.sdk.test.util.GenericTestDataProvider.PDF_OVERSIZED
import care.data4life.sdk.test.util.GenericTestDataProvider.PDF_OVERSIZED_ENCODED
import care.data4life.sdk.test.util.GenericTestDataProvider.PREVIEW_ID
import care.data4life.sdk.test.util.GenericTestDataProvider.RECORD_ID
import care.data4life.sdk.test.util.GenericTestDataProvider.THUMBNAIL_ID
import care.data4life.sdk.test.util.GenericTestDataProvider.UPDATE_DATE
import care.data4life.sdk.test.util.GenericTestDataProvider.USER_ID
import care.data4life.sdk.test.util.TestResourceHelper
import care.data4life.sdk.util.Base64
import care.data4life.sdk.wrapper.SdkDateTimeFormatter
import care.data4life.sdk.wrapper.SdkFhirParser
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.reactivex.Single
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue
import care.data4life.fhir.r4.model.DocumentReference as Fhir4DocumentReference
import care.data4life.fhir.r4.model.Reference as Fhir4Reference
import care.data4life.fhir.stu3.model.DocumentReference as Fhir3DocumentReference
import care.data4life.fhir.stu3.model.Reference as Fhir3Reference

class RecordServiceUpdateRecordModuleTest {
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
                FileService(ALIAS, apiService, cryptoService),
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

    private fun prepareFlow(
        recordId: String,
        alias: String,
        userId: String,
        encryptedUpdateRecord: EncryptedRecord,
        updateIteration: CryptoServiceIteration,
        encryptedReceivedRecord: EncryptedRecord,
        receivedIteration: CryptoServiceIteration
    ) {
        every {
            apiService.fetchRecord(alias, userId, recordId)
        } answers {
            Single.just(encryptedUpdateRecord).also {
                (cryptoService as CryptoServiceFake).iteration = updateIteration
            }
        }

        val actualRecord = slot<NetworkModelContract.EncryptedRecord>()
        val expectedRecord = encryptedReceivedRecord.copy(updatedDate = null)

        every {
            apiService.updateRecord(
                alias,
                userId,
                recordId,
                capture(actualRecord)

            )
        } answers {
            if (flowHelper.compareEncryptedRecords(actualRecord.captured, expectedRecord)) {
                Single.just(encryptedReceivedRecord).also {
                    (cryptoService as CryptoServiceFake).iteration = receivedIteration
                }
            } else {
                throw RuntimeException("Unexpected encrypted record\n${actualRecord.captured}")
            }
        }
    }

    private fun runFlow(
        encryptedUploadRecord: EncryptedRecord,
        encryptedReceivedRecord: EncryptedRecord,
        serializedResourceOld: String,
        serializedResourceNew: String,
        tags: List<String>,
        annotations: Annotations,
        useStoredCommonKey: Boolean,
        commonKey: Pair<String, GCKey>,
        dataKey: Pair<GCKey, EncryptedKey>,
        attachmentKey: Pair<GCKey, EncryptedKey>?,
        tagEncryptionKey: GCKey,
        userId: String,
        alias: String,
        recordId: String,
        attachments: List<String>? = null
    ) {
        val encryptedCommonKey = flowHelper.prepareStoredOrUnstoredCommonKeyRun(
            alias,
            userId,
            commonKey.first,
            useStoredCommonKey
        )

        val keyOrder = flowHelper.makeKeyOrder(dataKey, attachmentKey)

        val resources = flowHelper.packResources(
            listOf(serializedResourceOld, serializedResourceNew),
            attachments
        )

        val uploadIteration = CryptoServiceIteration(
            gcKeyOrder = keyOrder,
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
            tagEncryptionKeyCalls = 2,
            resources = resources,
            tags = tags,
            annotations = annotations,
            hashFunction = { value -> flowHelper.md5(value) }
        )

        val receivedIteration = CryptoServiceIteration(
            gcKeyOrder = emptyList(),
            commonKey = commonKey.second,
            commonKeyId = commonKey.first,
            commonKeyIsStored = true,
            commonKeyFetchCalls = 0,
            encryptedCommonKey = null,
            dataKey = dataKey.first,
            encryptedDataKey = dataKey.second,
            attachmentKey = attachmentKey?.first,
            encryptedAttachmentKey = attachmentKey?.second,
            tagEncryptionKey = tagEncryptionKey,
            tagEncryptionKeyCalls = 1,
            resources = listOf(serializedResourceNew),
            tags = tags,
            annotations = annotations,
            hashFunction = { value -> flowHelper.md5(value) }
        )

        prepareFlow(
            recordId,
            alias,
            userId,
            encryptedUploadRecord,
            uploadIteration,
            encryptedReceivedRecord,
            receivedIteration
        )
    }

    private fun mergeTags(
        tags: Tags,
        oldTags: Map<String, String>?
    ): Pair<List<String>, List<String>> {
        val allTags = mutableListOf<String>()
        val encodedTags: List<String>
        if (oldTags is Map<*, *>) {
            encodedTags = flowHelper.prepareTags(oldTags)
            allTags.addAll(
                flowHelper.prepareTags(tags)
            )
        } else {
            encodedTags = flowHelper.prepareTags(tags)
        }

        allTags.addAll(encodedTags)

        return Pair(encodedTags, allTags)
    }

    private fun runFhirFlow(
        serializedResourceOld: String,
        serializedResourceNew: String,
        tags: Tags,
        annotations: Annotations = emptyList(),
        useStoredCommonKey: Boolean = true,
        commonKey: Pair<String, GCKey> = COMMON_KEY_ID to this.commonKey,
        dataKey: Pair<GCKey, EncryptedKey> = this.dataKey to encryptedDataKey,
        tagEncryptionKey: GCKey = this.tagEncryptionKey,
        recordId: String = RECORD_ID,
        userId: String = USER_ID,
        alias: String = ALIAS,
        creationDate: String = CREATION_DATE,
        updateDates: Pair<String, String>,
        oldTags: Map<String, String>? = null
    ) {
        val (encodedTags, allTags) = mergeTags(tags, oldTags)
        val encodedAnnotations = flowHelper.prepareAnnotations(annotations)

        val encryptedUploadRecord = flowHelper.prepareEncryptedRecord(
            recordId,
            serializedResourceOld,
            encodedTags,
            encodedAnnotations,
            commonKey.first,
            dataKey.second,
            null,
            creationDate,
            updateDates.first
        )

        val encryptedReceivedRecord = flowHelper.prepareEncryptedRecord(
            recordId,
            serializedResourceNew,
            encodedTags,
            encodedAnnotations,
            commonKey.first,
            dataKey.second,
            null,
            creationDate,
            updateDates.second
        )

        runFlow(
            encryptedUploadRecord,
            encryptedReceivedRecord,
            serializedResourceOld,
            serializedResourceNew,
            allTags,
            encodedAnnotations,
            useStoredCommonKey,
            commonKey,
            dataKey,
            null,
            tagEncryptionKey,
            userId,
            alias,
            recordId
        )
    }

    private fun runFhirFlowWithAttachment(
        serializedResourceOld: String,
        serializedResourceNew: String,
        attachmentData: ByteArray,
        tags: Tags,
        annotations: Annotations = emptyList(),
        useStoredCommonKey: Boolean = true,
        commonKey: Pair<String, GCKey> = COMMON_KEY_ID to this.commonKey,
        dataKey: Pair<GCKey, EncryptedKey> = this.dataKey to encryptedDataKey,
        attachmentKey: Pair<GCKey, EncryptedKey> = this.attachmentKey to encryptedAttachmentKey,
        tagEncryptionKey: GCKey = this.tagEncryptionKey,
        recordId: String = RECORD_ID,
        userId: String = USER_ID,
        alias: String = ALIAS,
        creationDate: String = CREATION_DATE,
        updateDates: Pair<String, String>,
        attachmentId: String = ATTACHMENT_ID,
        resizedImages: Pair<Pair<ByteArray, String>, Pair<ByteArray, String>?>? = null
    ) {
        val encodedTags = flowHelper.prepareTags(tags)
        val encodedAnnotations = flowHelper.prepareAnnotations(annotations)

        val encryptedUploadRecord = flowHelper.prepareEncryptedRecord(
            recordId,
            serializedResourceOld,
            encodedTags,
            encodedAnnotations,
            commonKey.first,
            dataKey.second,
            attachmentKey.second,
            creationDate,
            updateDates.first
        )

        val encryptedReceivedRecord = flowHelper.prepareEncryptedRecord(
            recordId,
            serializedResourceNew,
            encodedTags,
            encodedAnnotations,
            commonKey.first,
            dataKey.second,
            attachmentKey.second,
            creationDate,
            updateDates.second
        )

        val mappedAttachments = flowHelper.mapAttachments(attachmentData, resizedImages)

        runFlow(
            encryptedUploadRecord,
            encryptedReceivedRecord,
            serializedResourceOld,
            serializedResourceNew,
            encodedTags,
            encodedAnnotations,
            useStoredCommonKey,
            commonKey,
            dataKey,
            attachmentKey,
            tagEncryptionKey,
            userId,
            alias,
            recordId,
            mappedAttachments
        )

        flowHelper.uploadAttachment(
            alias = alias,
            payload = Pair(attachmentData, attachmentId),
            userId = userId,
            resized = resizedImages
        )
    }

    private fun runDataFlow(
        serializedResourceOld: String,
        serializedResourceNew: String,
        tags: Tags,
        annotations: Annotations = emptyList(),
        useStoredCommonKey: Boolean = true,
        commonKey: Pair<String, GCKey> = COMMON_KEY_ID to this.commonKey,
        dataKey: Pair<GCKey, EncryptedKey> = this.dataKey to encryptedDataKey,
        tagEncryptionKey: GCKey = this.tagEncryptionKey,
        recordId: String = RECORD_ID,
        userId: String = USER_ID,
        alias: String = ALIAS,
        creationDate: String = CREATION_DATE,
        updateDates: Pair<String, String>
    ) {
        val encodedTags = flowHelper.prepareTags(tags)
        val encodedAnnotations = flowHelper.prepareAnnotations(annotations)

        val encryptedUploadRecord = flowHelper.prepareEncryptedRecord(
            recordId,
            serializedResourceOld,
            encodedTags,
            encodedAnnotations,
            commonKey.first,
            dataKey.second,
            null,
            creationDate,
            updateDates.first
        )

        val encryptedReceivedRecord = flowHelper.prepareEncryptedRecord(
            recordId,
            serializedResourceNew,
            encodedTags,
            encodedAnnotations,
            commonKey.first,
            dataKey.second,
            null,
            creationDate,
            updateDates.second
        )

        runFlow(
            encryptedUploadRecord,
            encryptedReceivedRecord,
            serializedResourceOld,
            serializedResourceNew,
            encodedTags,
            encodedAnnotations,
            useStoredCommonKey,
            commonKey,
            dataKey,
            null,
            tagEncryptionKey,
            userId,
            alias,
            recordId
        )
    }

    // FHIR3
    @Test
    fun `Given, updateFhir3Record is called with the appropriate payload without Annotations or Attachments, it return a updated Record`() {
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

        val now = SdkDateTimeFormatter.now()

        val resourceNew = SdkFhirParser.toFhir<Fhir3Resource>(
            resourceType,
            FhirContract.FhirVersion.FHIR_3.version,
            template
        ) as Fhir3DocumentReference

        val resourceOld = SdkFhirParser.toFhir<Fhir3Resource>(
            resourceType,
            FhirContract.FhirVersion.FHIR_3.version,
            template
        ) as Fhir3DocumentReference

        resourceOld.description = "A outdated mock"

        runFhirFlow(
            serializedResourceOld = SdkFhirParser.fromResource(resourceOld),
            serializedResourceNew = SdkFhirParser.fromResource(resourceNew),
            tags = tags,
            updateDates = Pair(now, UPDATE_DATE)
        )

        // When
        val result = recordService.updateRecord(
            USER_ID,
            RECORD_ID,
            resourceNew,
            emptyList()
        ).blockingGet()

        // Then
        assertTrue(result is Record)
        assertTrue(result.resource.identifier!!.isNotEmpty())
        assertEquals(
            expected = flowHelper.buildMeta(CREATION_DATE, UPDATE_DATE),
            actual = result.meta
        )
        assertTrue(result.annotations!!.isEmpty())
        assertEquals(
            expected = resourceNew,
            actual = result.resource
        )
    }

    @Test
    fun `Given, updateFhir3Record is called with the appropriate payload with Annotations and without Attachments, it return a updated Record`() {
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

        val resourceNew = SdkFhirParser.toFhir<Fhir3Resource>(
            resourceType,
            FhirContract.FhirVersion.FHIR_3.version,
            template
        ) as Fhir3DocumentReference

        val resourceOld = SdkFhirParser.toFhir<Fhir3Resource>(
            resourceType,
            FhirContract.FhirVersion.FHIR_3.version,
            template
        ) as Fhir3DocumentReference

        resourceOld.description = "A outdated mock"

        runFhirFlow(
            serializedResourceOld = SdkFhirParser.fromResource(resourceOld),
            serializedResourceNew = SdkFhirParser.fromResource(resourceNew),
            tags = tags,
            annotations = annotations,
            updateDates = Pair(SdkDateTimeFormatter.now(), UPDATE_DATE)
        )

        // When
        val result = recordService.updateRecord(
            USER_ID,
            RECORD_ID,
            resourceNew,
            annotations
        ).blockingGet()

        // Then
        assertTrue(result is Record)
        assertTrue(result.resource.identifier!!.isNotEmpty())
        assertEquals(
            expected = flowHelper.buildMeta(CREATION_DATE, UPDATE_DATE),
            actual = result.meta
        )
        assertEquals(
            actual = result.annotations,
            expected = annotations
        )
        assertEquals(
            expected = resourceNew,
            actual = result.resource
        )
    }

    @Test
    fun `Given, updateFhir3Record is called with the appropriate payload with Annotations and Attachments, it return a updated Record`() {
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

        val rawAttachment = TestResourceHelper.getByteResource("attachments", "sample.pdf")
        val attachment = Base64.encodeToString(rawAttachment)

        val template = TestResourceHelper.loadTemplateWithAttachments(
            "common",
            "documentReference-with-attachment-template",
            RECORD_ID,
            PARTNER_ID,
            "Sample PDF",
            "application/pdf",
            attachment,
            "d4l_f_p_t#$ATTACHMENT_ID#$PREVIEW_ID#$THUMBNAIL_ID"
        )

        val internalResource = SdkFhirParser.toFhir<Fhir3Resource>(
            resourceType,
            FhirContract.FhirVersion.FHIR_3.version,
            template
        ) as Fhir3DocumentReference

        val resourceNew = SdkFhirParser.toFhir<Fhir3Resource>(
            resourceType,
            FhirContract.FhirVersion.FHIR_3.version,
            template
        ) as Fhir3DocumentReference

        val resourceOld = SdkFhirParser.toFhir<Fhir3Resource>(
            resourceType,
            FhirContract.FhirVersion.FHIR_3.version,
            template
        ) as Fhir3DocumentReference

        resourceOld.description = "A outdated mock"
        resourceOld.content[0].attachment.data = null

        resourceNew.identifier = mutableListOf(
            Fhir3Identifier().also {
                it.value = "d4l_f_p_t#$ATTACHMENT_ID#$PREVIEW_ID#$THUMBNAIL_ID"
                it.assigner = Fhir3Reference().also { ref -> ref.reference = PARTNER_ID }
            },
            Fhir3Identifier().also {
                it.value = ATTACHMENT_ID
                it.assigner = Fhir3Reference().also { ref -> ref.reference = PARTNER_ID }
            },
            Fhir3Identifier().also { it.value = PREVIEW_ID },
            Fhir3Identifier().also { it.value = THUMBNAIL_ID },
            Fhir3Identifier().also { it.value = "AdditionalId" }
        )

        internalResource.identifier = mutableListOf(
            Fhir3Identifier().also {
                it.value = ATTACHMENT_ID
                it.assigner = Fhir3Reference().also { ref -> ref.reference = PARTNER_ID }
            },
            Fhir3Identifier().also { it.value = PREVIEW_ID },
            Fhir3Identifier().also { it.value = THUMBNAIL_ID },
            Fhir3Identifier().also { it.value = "AdditionalId" }
        )
        internalResource.content[0].attachment.id = "$ATTACHMENT_ID#$PREVIEW_ID#$THUMBNAIL_ID"
        internalResource.content[0].attachment.data = null

        runFhirFlowWithAttachment(
            serializedResourceOld = SdkFhirParser.fromResource(resourceOld),
            serializedResourceNew = SdkFhirParser.fromResource(internalResource),
            tags = tags,
            annotations = annotations,
            updateDates = Pair(SdkDateTimeFormatter.now(), UPDATE_DATE),
            attachmentId = "$ATTACHMENT_ID#$PREVIEW_ID#$THUMBNAIL_ID",
            attachmentData = rawAttachment
        )

        // When
        val result = recordService.updateRecord(
            USER_ID,
            RECORD_ID,
            resourceNew,
            annotations
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
            expected = SdkFhirParser.fromResource(result.resource),
            actual = SdkFhirParser.fromResource(resourceNew)
        )
        assertEquals(
            actual = result.resource.content.size,
            expected = 1
        )
        assertEquals(
            actual = result.resource.content[0].attachment.data,
            expected = attachment
        )
        assertEquals(
            actual = result.resource.content[0].attachment.id,
            expected = "$ATTACHMENT_ID#$PREVIEW_ID#$THUMBNAIL_ID"
        )
        assertEquals(
            actual = result.resource.identifier!!.size,
            expected = 4
        )
    }

    @Test
    fun `Given, updateFhir3Record is called with the appropriate payload with Annotations and Attachments, it return a updated Record, while resizing the attachment`() {
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

        val rawAttachment = TestResourceHelper.getByteResource("attachments", "sample.pdf")
        val attachment = Base64.encodeToString(rawAttachment)

        val template = TestResourceHelper.loadTemplateWithAttachments(
            "common",
            "documentReference-with-attachment-template",
            RECORD_ID,
            PARTNER_ID,
            "Sample PDF",
            "application/pdf",
            attachment
        )

        val internalResource = SdkFhirParser.toFhir<Fhir3Resource>(
            resourceType,
            FhirContract.FhirVersion.FHIR_3.version,
            template
        ) as Fhir3DocumentReference

        val resourceNew = SdkFhirParser.toFhir<Fhir3Resource>(
            resourceType,
            FhirContract.FhirVersion.FHIR_3.version,
            template
        ) as Fhir3DocumentReference

        val resourceOld = SdkFhirParser.toFhir<Fhir3Resource>(
            resourceType,
            FhirContract.FhirVersion.FHIR_3.version,
            template
        ) as Fhir3DocumentReference

        resourceOld.description = "A outdated mock"
        resourceOld.content[0].attachment.data = null

        resourceNew.identifier = null

        internalResource.content[0].attachment.id = ATTACHMENT_ID
        internalResource.content[0].attachment.data = null
        internalResource.identifier = mutableListOf(
            Fhir3Identifier().also {
                it.value = "d4l_f_p_t#$ATTACHMENT_ID#$PREVIEW_ID#$THUMBNAIL_ID"
                it.assigner = Fhir3Reference().also { ref -> ref.reference = PARTNER_ID }
            }
        )

        val preview = Pair(ByteArray(2), PREVIEW_ID)
        val thumbnail = Pair(ByteArray(1), THUMBNAIL_ID)

        runFhirFlowWithAttachment(
            serializedResourceOld = SdkFhirParser.fromResource(resourceOld),
            serializedResourceNew = SdkFhirParser.fromResource(internalResource),
            tags = tags,
            annotations = annotations,
            updateDates = Pair(SdkDateTimeFormatter.now(), UPDATE_DATE),
            attachmentId = ATTACHMENT_ID,
            attachmentData = rawAttachment,
            resizedImages = Pair(preview, thumbnail)
        )

        // When
        val result = recordService.updateRecord(
            USER_ID,
            RECORD_ID,
            resourceNew,
            annotations
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
            expected = SdkFhirParser.fromResource(result.resource),
            actual = SdkFhirParser.fromResource(resourceNew)
        )
        assertEquals(
            actual = result.resource.content.size,
            expected = 1
        )
        assertEquals(
            actual = result.resource.content[0].attachment.data,
            expected = attachment
        )
        assertEquals(
            actual = result.resource.content[0].attachment.id,
            expected = ATTACHMENT_ID
        )
        assertEquals(
            actual = result.resource.identifier!!.size,
            expected = 1
        )
    }

    @Test
    @Ignore("Gradle runs out of heap memory")
    fun `Given, updateFhir3Record is called with the appropriate payload with Annotations and Attachments, it fails due to a ill Attachment`() {
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

        val rawAttachment = PDF_OVERSIZED
        val attachment = PDF_OVERSIZED_ENCODED

        val template = TestResourceHelper.loadTemplateWithAttachments(
            "common",
            "documentReference-with-attachment-template",
            RECORD_ID,
            PARTNER_ID,
            "Sample PDF",
            "application/pdf",
            attachment
        )

        val internalResource = SdkFhirParser.toFhir<Fhir3Resource>(
            resourceType,
            FhirContract.FhirVersion.FHIR_3.version,
            template
        ) as Fhir3DocumentReference

        val resourceNew = SdkFhirParser.toFhir<Fhir3Resource>(
            resourceType,
            FhirContract.FhirVersion.FHIR_3.version,
            template
        ) as Fhir3DocumentReference

        val resourceOld = SdkFhirParser.toFhir<Fhir3Resource>(
            resourceType,
            FhirContract.FhirVersion.FHIR_3.version,
            template
        ) as Fhir3DocumentReference

        runFhirFlowWithAttachment(
            serializedResourceOld = SdkFhirParser.fromResource(resourceOld),
            serializedResourceNew = SdkFhirParser.fromResource(internalResource),
            tags = tags,
            annotations = annotations,
            updateDates = Pair(SdkDateTimeFormatter.now(), UPDATE_DATE),
            attachmentId = ATTACHMENT_ID,
            attachmentData = rawAttachment
        )

        // Then
        assertFailsWith<DataValidationException.MaxDataSizeViolation> {
            // When
            recordService.updateRecord(
                USER_ID,
                RECORD_ID,
                resourceNew,
                annotations
            ).blockingGet()
        }
    }

    // see: https://gesundheitscloud.atlassian.net/browse/SDK-599
    @Test
    fun `Given, updateFhir3Record is called with the appropriate payload with Annotations and Attachments, it return a updated Record, if the Record does not contain new Attachments`() {
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
            "documentReference-sdk-599-template",
            RECORD_ID,
            PARTNER_ID
        )

        val internalResource = SdkFhirParser.toFhir<Fhir3Resource>(
            resourceType,
            FhirContract.FhirVersion.FHIR_3.version,
            template
        ) as Fhir3DocumentReference

        val resourceNew = SdkFhirParser.toFhir<Fhir3Resource>(
            resourceType,
            FhirContract.FhirVersion.FHIR_3.version,
            template
        ) as Fhir3DocumentReference

        val resourceOld = SdkFhirParser.toFhir<Fhir3Resource>(
            resourceType,
            FhirContract.FhirVersion.FHIR_3.version,
            template
        ) as Fhir3DocumentReference

        resourceOld.description = "A outdated mock"

        runFhirFlow(
            serializedResourceOld = SdkFhirParser.fromResource(resourceOld),
            serializedResourceNew = SdkFhirParser.fromResource(internalResource),
            tags = tags,
            annotations = annotations,
            updateDates = Pair(SdkDateTimeFormatter.now(), UPDATE_DATE)
        )

        // When
        val result = recordService.updateRecord(
            USER_ID,
            RECORD_ID,
            resourceNew,
            annotations
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
            expected = SdkFhirParser.fromResource(result.resource),
            actual = SdkFhirParser.fromResource(resourceNew)
        )
        assertEquals(
            actual = result.resource.content.size,
            expected = 2
        )
        assertNull(result.resource.content[0].attachment.data)
        assertEquals(
            actual = result.resource.content[0].attachment.id,
            expected = resourceOld.content[0].attachment.id
        )

        assertNull(result.resource.content[1].attachment.data)
        assertEquals(
            actual = result.resource.content[1].attachment.id,
            expected = resourceOld.content[1].attachment.id
        )

        assertNull(result.resource.identifier)
    }

    // FHIR4
    @Test
    fun `Given, updateFhir4Record is called with the appropriate payload without Annotations or Attachments, it return a updated Record`() {
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

        val now = SdkDateTimeFormatter.now()

        val resourceNew = SdkFhirParser.toFhir<Fhir4Resource>(
            resourceType,
            FhirContract.FhirVersion.FHIR_4.version,
            template
        ) as Fhir4DocumentReference

        val resourceOld = SdkFhirParser.toFhir<Fhir4Resource>(
            resourceType,
            FhirContract.FhirVersion.FHIR_4.version,
            template
        ) as Fhir4DocumentReference

        resourceOld.description = "A outdated mock"

        runFhirFlow(
            serializedResourceOld = SdkFhirParser.fromResource(resourceOld),
            serializedResourceNew = SdkFhirParser.fromResource(resourceNew),
            tags = tags,
            updateDates = Pair(now, UPDATE_DATE)
        )

        // When
        val result = recordService.updateRecord(
            USER_ID,
            RECORD_ID,
            resourceNew,
            emptyList()
        ).blockingGet()

        // Then
        assertTrue(result is Fhir4Record)
        assertTrue(result.resource.identifier!!.isNotEmpty())
        assertEquals(
            expected = flowHelper.buildMeta(CREATION_DATE, UPDATE_DATE),
            actual = result.meta
        )
        assertTrue(result.annotations.isEmpty())
        assertEquals(
            expected = resourceNew,
            actual = result.resource
        )
    }

    @Test
    fun `Given, updateFhir4Record is called with the appropriate payload with Annotations and without Attachments, it return a updated Record`() {
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

        val resourceNew = SdkFhirParser.toFhir<Fhir4Resource>(
            resourceType,
            FhirContract.FhirVersion.FHIR_4.version,
            template
        ) as Fhir4DocumentReference

        val resourceOld = SdkFhirParser.toFhir<Fhir4Resource>(
            resourceType,
            FhirContract.FhirVersion.FHIR_4.version,
            template
        ) as Fhir4DocumentReference

        resourceOld.description = "A outdated mock"

        runFhirFlow(
            serializedResourceOld = SdkFhirParser.fromResource(resourceOld),
            serializedResourceNew = SdkFhirParser.fromResource(resourceNew),
            tags = tags,
            annotations = annotations,
            updateDates = Pair(SdkDateTimeFormatter.now(), UPDATE_DATE)
        )

        // When
        val result = recordService.updateRecord(
            USER_ID,
            RECORD_ID,
            resourceNew,
            annotations
        ).blockingGet()

        // Then
        assertTrue(result is Fhir4Record)
        assertTrue(result.resource.identifier!!.isNotEmpty())
        assertEquals(
            expected = flowHelper.buildMeta(CREATION_DATE, UPDATE_DATE),
            actual = result.meta
        )
        assertEquals(
            actual = result.annotations,
            expected = annotations
        )
        assertEquals(
            expected = resourceNew,
            actual = result.resource
        )
    }

    @Test
    fun `Given, updateFhir4Record is called with the appropriate payload with Annotations and Attachments, it return a updated Record`() {
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

        val rawAttachment = TestResourceHelper.getByteResource("attachments", "sample.pdf")
        val attachment = Base64.encodeToString(rawAttachment)

        val template = TestResourceHelper.loadTemplateWithAttachments(
            "common",
            "documentReference-with-attachment-template",
            RECORD_ID,
            PARTNER_ID,
            "Sample PDF",
            "application/pdf",
            attachment,
            "d4l_f_p_t#$ATTACHMENT_ID#$PREVIEW_ID#$THUMBNAIL_ID"
        )

        val internalResource = SdkFhirParser.toFhir<Fhir4Resource>(
            resourceType,
            FhirContract.FhirVersion.FHIR_4.version,
            template
        ) as Fhir4DocumentReference

        val resourceNew = SdkFhirParser.toFhir<Fhir4Resource>(
            resourceType,
            FhirContract.FhirVersion.FHIR_4.version,
            template
        ) as Fhir4DocumentReference

        val resourceOld = SdkFhirParser.toFhir<Fhir4Resource>(
            resourceType,
            FhirContract.FhirVersion.FHIR_4.version,
            template
        ) as Fhir4DocumentReference

        resourceOld.description = "A outdated mock"
        resourceOld.content[0].attachment.data = null

        resourceNew.identifier = mutableListOf(
            Fhir4Identifier().also {
                it.value = "d4l_f_p_t#$ATTACHMENT_ID#$PREVIEW_ID#$THUMBNAIL_ID"
                it.assigner = Fhir4Reference().also { ref -> ref.reference = PARTNER_ID }
            },
            Fhir4Identifier().also {
                it.value = ATTACHMENT_ID
                it.assigner = Fhir4Reference().also { ref -> ref.reference = PARTNER_ID }
            },
            Fhir4Identifier().also { it.value = PREVIEW_ID },
            Fhir4Identifier().also { it.value = THUMBNAIL_ID },
            Fhir4Identifier().also { it.value = "AdditionalId" }
        )

        internalResource.identifier = mutableListOf(
            Fhir4Identifier().also {
                it.value = ATTACHMENT_ID
                it.assigner = Fhir4Reference().also { ref -> ref.reference = PARTNER_ID }
            },
            Fhir4Identifier().also { it.value = PREVIEW_ID },
            Fhir4Identifier().also { it.value = THUMBNAIL_ID },
            Fhir4Identifier().also { it.value = "AdditionalId" }
        )
        internalResource.content[0].attachment.id = "$ATTACHMENT_ID#$PREVIEW_ID#$THUMBNAIL_ID"
        internalResource.content[0].attachment.data = null

        runFhirFlowWithAttachment(
            serializedResourceOld = SdkFhirParser.fromResource(resourceOld),
            serializedResourceNew = SdkFhirParser.fromResource(internalResource),
            tags = tags,
            annotations = annotations,
            updateDates = Pair(SdkDateTimeFormatter.now(), UPDATE_DATE),
            attachmentId = "$ATTACHMENT_ID#$PREVIEW_ID#$THUMBNAIL_ID",
            attachmentData = rawAttachment
        )

        // When
        val result = recordService.updateRecord(
            USER_ID,
            RECORD_ID,
            resourceNew,
            annotations
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
            expected = SdkFhirParser.fromResource(result.resource),
            actual = SdkFhirParser.fromResource(resourceNew)
        )
        assertEquals(
            actual = result.resource.content.size,
            expected = 1
        )
        assertEquals(
            actual = result.resource.content[0].attachment.data,
            expected = attachment
        )
        assertEquals(
            actual = result.resource.content[0].attachment.id,
            expected = "$ATTACHMENT_ID#$PREVIEW_ID#$THUMBNAIL_ID"
        )
        assertEquals(
            actual = result.resource.identifier!!.size,
            expected = 4
        )
    }

    @Test
    fun `Given, updateFhir4Record is called with the appropriate payload with Annotations and Attachments, it return a updated Record, while resizing the attachment`() {
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

        val rawAttachment = TestResourceHelper.getByteResource("attachments", "sample.pdf")
        val attachment = Base64.encodeToString(rawAttachment)

        val template = TestResourceHelper.loadTemplateWithAttachments(
            "common",
            "documentReference-with-attachment-template",
            RECORD_ID,
            PARTNER_ID,
            "Sample PDF",
            "application/pdf",
            attachment
        )

        val internalResource = SdkFhirParser.toFhir<Fhir4Resource>(
            resourceType,
            FhirContract.FhirVersion.FHIR_4.version,
            template
        ) as Fhir4DocumentReference

        val resourceNew = SdkFhirParser.toFhir<Fhir4Resource>(
            resourceType,
            FhirContract.FhirVersion.FHIR_4.version,
            template
        ) as Fhir4DocumentReference

        val resourceOld = SdkFhirParser.toFhir<Fhir4Resource>(
            resourceType,
            FhirContract.FhirVersion.FHIR_4.version,
            template
        ) as Fhir4DocumentReference

        resourceOld.description = "A outdated mock"
        resourceOld.content[0].attachment.data = null

        resourceNew.identifier = null

        internalResource.content[0].attachment.id = ATTACHMENT_ID
        internalResource.content[0].attachment.data = null
        internalResource.identifier = mutableListOf(
            Fhir4Identifier().also {
                it.value = "d4l_f_p_t#$ATTACHMENT_ID#$PREVIEW_ID#$THUMBNAIL_ID"
                it.assigner = Fhir4Reference().also { ref -> ref.reference = PARTNER_ID }
            }
        )

        val preview = Pair(ByteArray(2), PREVIEW_ID)
        val thumbnail = Pair(ByteArray(1), THUMBNAIL_ID)

        runFhirFlowWithAttachment(
            serializedResourceOld = SdkFhirParser.fromResource(resourceOld),
            serializedResourceNew = SdkFhirParser.fromResource(internalResource),
            tags = tags,
            annotations = annotations,
            updateDates = Pair(SdkDateTimeFormatter.now(), UPDATE_DATE),
            attachmentId = ATTACHMENT_ID,
            attachmentData = rawAttachment,
            resizedImages = Pair(preview, thumbnail)
        )

        // When
        val result = recordService.updateRecord(
            USER_ID,
            RECORD_ID,
            resourceNew,
            annotations
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
            expected = SdkFhirParser.fromResource(result.resource),
            actual = SdkFhirParser.fromResource(resourceNew)
        )
        assertEquals(
            actual = result.resource.content.size,
            expected = 1
        )
        assertEquals(
            actual = result.resource.content[0].attachment.data,
            expected = attachment
        )
        assertEquals(
            actual = result.resource.content[0].attachment.id,
            expected = ATTACHMENT_ID
        )
        assertEquals(
            actual = result.resource.identifier!!.size,
            expected = 1
        )
    }

    @Test
    @Ignore("Gradle runs out of heap memory")
    fun `Given, updateFhir4Record is called with the appropriate payload with Annotations and Attachments, it fails due to a ill Attachment`() {
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

        val rawAttachment = PDF_OVERSIZED
        val attachment = PDF_OVERSIZED_ENCODED

        val template = TestResourceHelper.loadTemplateWithAttachments(
            "common",
            "documentReference-with-attachment-template",
            RECORD_ID,
            PARTNER_ID,
            "Sample PDF",
            "application/pdf",
            attachment
        )

        val internalResource = SdkFhirParser.toFhir<Fhir4Resource>(
            resourceType,
            FhirContract.FhirVersion.FHIR_4.version,
            template
        ) as Fhir4DocumentReference

        val resourceNew = SdkFhirParser.toFhir<Fhir4Resource>(
            resourceType,
            FhirContract.FhirVersion.FHIR_4.version,
            template
        ) as Fhir4DocumentReference

        val resourceOld = SdkFhirParser.toFhir<Fhir4Resource>(
            resourceType,
            FhirContract.FhirVersion.FHIR_4.version,
            template
        ) as Fhir4DocumentReference

        runFhirFlowWithAttachment(
            serializedResourceOld = SdkFhirParser.fromResource(resourceOld),
            serializedResourceNew = SdkFhirParser.fromResource(internalResource),
            tags = tags,
            annotations = annotations,
            updateDates = Pair(SdkDateTimeFormatter.now(), UPDATE_DATE),
            attachmentId = ATTACHMENT_ID,
            attachmentData = rawAttachment
        )

        // Then
        assertFailsWith<DataValidationException.MaxDataSizeViolation> {
            // When
            recordService.updateRecord(
                USER_ID,
                RECORD_ID,
                resourceNew,
                annotations
            ).blockingGet()
        }
    }

    // Arbitrary Data
    @Test
    fun `Given, updateDataRecord is called with the appropriate payload without Annotations , it return a updated Record for Arbitrary Data`() {
        // Given
        val old = "Me and my poney its name is Johny."
        val new = "Completely new design, ey."
        val resourceNew = DataResource(new.toByteArray())

        val tags = mapOf(
            "flag" to ARBITRARY_DATA_KEY,
            "partner" to PARTNER_ID,
            "client" to CLIENT_ID
        )

        runDataFlow(
            serializedResourceOld = old,
            serializedResourceNew = new,
            tags = tags,
            updateDates = Pair(SdkDateTimeFormatter.now(), UPDATE_DATE)
        )

        // When
        val result = recordService.updateRecord(
            USER_ID,
            RECORD_ID,
            resourceNew,
            emptyList()
        ).blockingGet()

        // Then
        assertTrue(result is DataRecord)
        assertEquals(
            expected = flowHelper.buildMeta(CREATION_DATE, UPDATE_DATE),
            actual = result.meta
        )
        assertTrue(result.annotations.isEmpty())
        assertEquals(
            expected = resourceNew,
            actual = result.resource
        )
    }

    @Test
    fun `Given, updateDataRecord is called with the appropriate payload with Annotations and without Attachments, it return a updated Record for Arbitrary Data`() {
        // Given
        val old = "Me and my poney its name is Johny."
        val new = "Completely new design, ey."
        val resourceNew = DataResource(new.toByteArray())

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

        runDataFlow(
            serializedResourceOld = old,
            serializedResourceNew = new,
            tags = tags,
            updateDates = Pair(SdkDateTimeFormatter.now(), UPDATE_DATE),
            annotations = annotations
        )

        // When
        val result = recordService.updateRecord(
            USER_ID,
            RECORD_ID,
            resourceNew,
            annotations
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
            expected = resourceNew,
            actual = result.resource
        )
    }

    // Compatibility
    // Fixme: This is a potential crash without error log
    @Test
    fun `Given, updateRecord is called with a Fhir4Resource, but it is actually a Fhir3Resource, it fails`() {
        val resourceType = "DocumentReference"
        val tags = mapOf(
            "partner" to PARTNER_ID,
            "client" to CLIENT_ID,
            "fhirversion" to "4.0.1",
            "resourcetype" to resourceType
        )

        val oldTags = mapOf(
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

        val resourceNew = SdkFhirParser.toFhir<Fhir4Resource>(
            resourceType,
            FhirContract.FhirVersion.FHIR_4.version,
            template
        ) as Fhir4DocumentReference

        val resourceOld = SdkFhirParser.toFhir<Fhir3Resource>(
            resourceType,
            FhirContract.FhirVersion.FHIR_3.version,
            template
        ) as Fhir3DocumentReference

        resourceOld.description = "A outdated mock"

        runFhirFlow(
            serializedResourceOld = SdkFhirParser.fromResource(resourceOld),
            serializedResourceNew = SdkFhirParser.fromResource(resourceNew),
            tags = tags,
            updateDates = Pair(SdkDateTimeFormatter.now(), UPDATE_DATE),
            oldTags = oldTags
        )

        // Then
        assertFailsWith<ClassCastException> {
            // When
            recordService.updateRecord(
                USER_ID,
                RECORD_ID,
                resourceNew,
                listOf()
            ).blockingGet()
        }
    }

    // see: https://gesundheitscloud.atlassian.net/browse/SDK-599
    @Test
    fun `Given, updateFhir4Record is called with the appropriate payload with Annotations and Attachments, it return a updated Record, if the Record does not contain new Attachments`() {
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
            "documentReference-sdk-599-template",
            RECORD_ID,
            PARTNER_ID
        )

        val internalResource = SdkFhirParser.toFhir<Fhir4Resource>(
            resourceType,
            FhirContract.FhirVersion.FHIR_4.version,
            template
        ) as Fhir4DocumentReference

        val resourceNew = SdkFhirParser.toFhir<Fhir4Resource>(
            resourceType,
            FhirContract.FhirVersion.FHIR_4.version,
            template
        ) as Fhir4DocumentReference

        val resourceOld = SdkFhirParser.toFhir<Fhir4Resource>(
            resourceType,
            FhirContract.FhirVersion.FHIR_4.version,
            template
        ) as Fhir4DocumentReference

        resourceOld.description = "A outdated mock"

        runFhirFlow(
            serializedResourceOld = SdkFhirParser.fromResource(resourceOld),
            serializedResourceNew = SdkFhirParser.fromResource(internalResource),
            tags = tags,
            annotations = annotations,
            updateDates = Pair(SdkDateTimeFormatter.now(), UPDATE_DATE)
        )

        // When
        val result = recordService.updateRecord(
            USER_ID,
            RECORD_ID,
            resourceNew,
            annotations
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
            expected = SdkFhirParser.fromResource(result.resource),
            actual = SdkFhirParser.fromResource(resourceNew)
        )
        assertEquals(
            actual = result.resource.content.size,
            expected = 2
        )
        assertNull(result.resource.content[0].attachment.data)
        assertEquals(
            actual = result.resource.content[0].attachment.id,
            expected = resourceOld.content[0].attachment.id
        )

        assertNull(result.resource.content[1].attachment.data)
        assertEquals(
            actual = result.resource.content[1].attachment.id,
            expected = resourceOld.content[1].attachment.id
        )

        assertNull(result.resource.identifier)
    }
}
