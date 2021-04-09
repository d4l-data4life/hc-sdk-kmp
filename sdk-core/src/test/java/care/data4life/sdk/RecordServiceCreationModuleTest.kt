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
import care.data4life.sdk.attachment.AttachmentService
import care.data4life.sdk.attachment.FileService
import care.data4life.sdk.call.DataRecord
import care.data4life.sdk.call.Fhir4Record
import care.data4life.sdk.config.DataRestrictionException
import care.data4life.sdk.data.DataResource
import care.data4life.sdk.fhir.Fhir3Identifier
import care.data4life.sdk.fhir.Fhir4Identifier
import care.data4life.sdk.fhir.FhirService
import care.data4life.sdk.model.Record
import care.data4life.sdk.network.model.EncryptedKey
import care.data4life.sdk.network.model.EncryptedRecord
import care.data4life.sdk.record.RecordContract
import care.data4life.sdk.tag.TagEncryptionService
import care.data4life.sdk.tag.TaggingService
import care.data4life.sdk.tag.Tags
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
import care.data4life.sdk.test.util.TestResourceHelper.loadTemplate
import care.data4life.sdk.test.util.TestResourceHelper.loadTemplateWithAttachments
import care.data4life.sdk.util.Base64
import care.data4life.sdk.wrapper.SdkDateTimeFormatter
import care.data4life.sdk.wrapper.SdkFhirParser
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.reactivex.Single
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import care.data4life.fhir.r4.model.DocumentReference as Fhir4DocumentReference
import care.data4life.fhir.r4.model.Reference as Fhir4Reference
import care.data4life.fhir.stu3.model.DocumentReference as Fhir3DocumentReference
import care.data4life.fhir.stu3.model.Reference as Fhir3Reference

class RecordServiceCreationModuleTest {
    private val dataKey: GCKey = mockk()
    private val attachmentKey: GCKey = mockk()
    private val tagEncryptionKey: GCKey = mockk()
    private val commonKey: GCKey = mockk()
    private val encryptedDataKey: EncryptedKey = mockk()
    private val encryptedAttachmentKey: EncryptedKey = mockk()

    private lateinit var recordService: RecordContract.Service
    private lateinit var flowHelper: RecordServiceModuleTestFlowHelper
    private val apiService: ApiService = mockk()
    private val cryptoService: CryptoService = mockk()
    private val fileService: FileService = mockk()
    private val imageResizer: ImageResizer = mockk()
    private val errorHandler: D4LErrorHandler = mockk()

    @Before
    fun setUp() {
        clearAllMocks()
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
            cryptoService,
            fileService,
            imageResizer
        )
    }

    private fun prepareEncryptedFhirRecord(
        resource: String,
        tags: Map<String, String>,
        annotations: Map<String, String>,
        commonKeyList: CommonKeyList,
        encryptedKeys: Pair<EncryptedKey, EncryptedKey?>
    ): EncryptedRecord = flowHelper.buildEncryptedRecord(
        null,
        commonKeyList[0],
        tags.values.toList(),
        annotations.values.toList(),
        resource,
        Pair(SdkDateTimeFormatter.now(), null),
        encryptedKeys
    )

    private fun prepareEncryptedDataRecord(
        resource: String,
        tags: Map<String, String>,
        annotations: Map<String, String>,
        commonKeyList: CommonKeyList,
        encryptedKeys: Pair<EncryptedKey, EncryptedKey?>
    ): EncryptedRecord = flowHelper.buildEncryptedRecordWithEncodedBody(
        null,
        commonKeyList[0],
        tags.values.toList(),
        annotations.values.toList(),
        resource,
        Pair(SdkDateTimeFormatter.now(), null),
        encryptedKeys
    )

    private fun createRecievedEncryptedRecord(
        encryptedRecord: EncryptedRecord,
        recordId: String,
        creationDate: String = CREATION_DATE,
        updatedDate: String = UPDATE_DATE
    ): EncryptedRecord = encryptedRecord.copy(
        identifier = recordId,
        customCreationDate = creationDate,
        updatedDate = updatedDate
    )

    private fun runFlow(
        encryptedUploadRecord: EncryptedRecord,
        encryptedReceivedRecord: EncryptedRecord,
        userId: String,
        alias: String,
        tags: Map<String, String>,
        annotations: Map<String, String>,
        gcKeys: MutableList<GCKey?>,
        tagEncryptionKey: GCKey,
        commonKeyList: CommonKeyList,
        cryptoMap: CryptoMap
    ) {
        flowHelper.tagEncryptionKeyCallOrder(listOf(tagEncryptionKey, tagEncryptionKey))

        flowHelper.encryptFhirResource(
            commonKeyList,
            cryptoMap,
            gcKeys,
            tags,
            annotations,
            tagEncryptionKey
        )

        every {
            apiService.createRecord(alias, userId, eq(encryptedUploadRecord))
        } returns Single.just(encryptedReceivedRecord)

        flowHelper.decryptTagsAndAnnotations(tags, annotations, tagEncryptionKey)
    }

    private fun runFhirFlow(
        serializedResource: String,
        recordId: String = RECORD_ID,
        userId: String = USER_ID,
        alias: String = ALIAS,
        tags: Map<String, String>,
        annotations: Map<String, String> = emptyMap(),
        gcKeys: MutableList<GCKey?>,
        commonKey: Pair<String, GCKey> = COMMON_KEY_ID to this.commonKey,
        dataKey: Pair<GCKey, EncryptedKey> = this.dataKey to encryptedDataKey,
        attachmentKey: Pair<GCKey, EncryptedKey>? = this.attachmentKey to encryptedAttachmentKey,
        tagEncryptionKey: GCKey = this.tagEncryptionKey
    ) {
        val commonKeyList = listOf(commonKey)

        val (cryptoMap, encryptedKeys) = flowHelper.prepareFlowPayload(
            listOf(serializedResource),
            commonKeyList,
            listOf(dataKey),
            if (attachmentKey is Pair) listOf(attachmentKey) else null
        )

        val encryptedUploadRecord = prepareEncryptedFhirRecord(
            serializedResource,
            tags,
            annotations,
            commonKeyList,
            encryptedKeys[0]
        )

        val encryptedReceivedRecord = createRecievedEncryptedRecord(encryptedUploadRecord, recordId)

        runFlow(
            encryptedUploadRecord,
            encryptedReceivedRecord,
            userId,
            alias,
            tags,
            annotations,
            gcKeys,
            tagEncryptionKey,
            commonKeyList,
            cryptoMap
        )

        flowHelper.encryptFhirResource(
            commonKeyList,
            cryptoMap,
            gcKeys,
            tags,
            annotations,
            tagEncryptionKey
        )

        flowHelper.runWithoutStoredCommonKey(alias, userId, commonKeyList)
        flowHelper.decryptFhirResource(cryptoMap)
    }

    private fun runFhirFlowWithAttachment(
        serializedResource: String,
        data: ByteArray,
        recordId: String = RECORD_ID,
        userId: String = USER_ID,
        attachmentId: String = ATTACHMENT_ID,
        alias: String = ALIAS,
        tags: Map<String, String>,
        annotations: Map<String, String> = mapOf(),
        gcKeys: MutableList<GCKey?>,
        commonKey: Pair<String, GCKey> = COMMON_KEY_ID to this.commonKey,
        dataKey: Pair<GCKey, EncryptedKey> = this.dataKey to encryptedDataKey,
        attachmentKey: Pair<GCKey, EncryptedKey> = this.attachmentKey to encryptedAttachmentKey,
        tagEncryptionKey: GCKey = this.tagEncryptionKey,
        resizedImages: Pair<Pair<ByteArray, String>, Pair<ByteArray, String>?>? = null
    ) {
        runFhirFlow(
            serializedResource,
            recordId,
            userId,
            alias,
            tags,
            annotations,
            gcKeys,
            commonKey,
            dataKey,
            attachmentKey,
            tagEncryptionKey
        )

        flowHelper.uploadAttachment(
            attachmentKey = attachmentKey.first,
            payload = Pair(data, attachmentId),
            userId = userId,
            resized = resizedImages
        )
    }

    private fun runArbitraryDataFlow(
        serializedResource: String,
        recordId: String = RECORD_ID,
        userId: String = USER_ID,
        alias: String = ALIAS,
        tags: Map<String, String>,
        annotations: Map<String, String> = emptyMap(),
        gcKeys: MutableList<GCKey?>,
        commonKey: Pair<String, GCKey> = COMMON_KEY_ID to this.commonKey,
        dataKey: Pair<GCKey, EncryptedKey> = this.dataKey to encryptedDataKey,
        tagEncryptionKey: GCKey = this.tagEncryptionKey
    ) {
        val commonKeyList = listOf(commonKey)

        val (cryptoMap, encryptedKeys) = flowHelper.prepareFlowPayload(
            listOf(serializedResource),
            commonKeyList,
            listOf(dataKey),
            null
        )

        val encryptedUploadRecord = prepareEncryptedDataRecord(
            serializedResource,
            tags,
            annotations,
            commonKeyList,
            encryptedKeys[0]
        )

        val encryptedReceivedRecord = createRecievedEncryptedRecord(encryptedUploadRecord, recordId)

        runFlow(
            encryptedUploadRecord,
            encryptedReceivedRecord,
            userId,
            alias,
            tags,
            annotations,
            gcKeys,
            tagEncryptionKey,
            commonKeyList,
            cryptoMap
        )

        flowHelper.encryptDataRecord(
            commonKeyList,
            cryptoMap,
            gcKeys,
            tags,
            annotations,
            tagEncryptionKey
        )

        flowHelper.runWithStoredCommonKey(commonKeyList)
        flowHelper.decryptDataResource(cryptoMap)
    }

    // FHIR3
    @Test
    fun `Given, createFhir3Record is called with the appropriate payload without Annotations or Attachments, it creates a Record for Fhir3`() {
        // Given
        val resourceType = "DocumentReference"
        val tags = mapOf(
            "partner" to PARTNER_ID,
            "client" to CLIENT_ID,
            "fhirversion" to "3.0.1",
            "resourcetype" to resourceType
        ).also { flowHelper.prepareTags(it as Tags) }

        val serializedResource = loadTemplate(
            "common",
            "documentReference-without-attachment-template",
            RECORD_ID,
            PARTNER_ID
        )

        val resource = SdkFhirParser.toFhir3(
            resourceType,
            serializedResource
        ) as Fhir3DocumentReference


        val gcKeys: MutableList<GCKey?> = mutableListOf(
            dataKey,
            null,
            dataKey
        )

        runFhirFlow(
            serializedResource = SdkFhirParser.fromResource(resource)!!,
            tags = tags,
            gcKeys = gcKeys,
            attachmentKey = null
        )

        // When
        val result = recordService.createRecord(
            USER_ID,
            resource,
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
            expected = resource,
            actual = result.resource
        )
    }

    @Test
    fun `Given, createFhir3Record is called with the appropriate payload with Annotations and without Attachments, it creates a Record for Fhir3`() {
        // Given
        val resourceType = "DocumentReference"
        val tags = mapOf(
            "partner" to PARTNER_ID,
            "client" to CLIENT_ID,
            "fhirversion" to "3.0.1",
            "resourcetype" to resourceType
        ).also { flowHelper.prepareTags(it as Tags) }

        val annotations = listOf(
            "wow",
            "it",
            "works",
            "and",
            "like_a_duracell_häsi"
        )

        val serializedResource = loadTemplate(
            "common",
            "documentReference-without-attachment-template",
            RECORD_ID,
            PARTNER_ID
        )

        val resource = SdkFhirParser.toFhir3(
            resourceType,
            serializedResource
        ) as Fhir3DocumentReference


        val gcKeys: MutableList<GCKey?> = mutableListOf(
            dataKey,
            null,
            dataKey
        )

        runFhirFlow(
            serializedResource = SdkFhirParser.fromResource(resource)!!,
            tags = tags,
            gcKeys = gcKeys,
            annotations = flowHelper.prepareAnnotations(annotations),
            attachmentKey = null
        )

        // When
        val result = recordService.createRecord(
            USER_ID,
            resource,
            annotations = annotations
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
            expected = resource,
            actual = result.resource
        )
    }

    @Test
    fun `Given, createFhir3Record is called with the appropriate payload with Annotations and Attachments, it creates a Record for Fhir3`() {
        // Given
        val resourceType = "DocumentReference"
        val tags = mapOf(
            "partner" to PARTNER_ID,
            "client" to CLIENT_ID,
            "fhirversion" to "3.0.1",
            "resourcetype" to resourceType
        ).also { flowHelper.prepareTags(it as Tags) }

        val annotations = listOf(
            "wow",
            "it",
            "works",
            "and",
            "like_a_duracell_häsi"
        )

        val rawAttachment = TestResourceHelper.getByteResource("attachments", "sample.pdf")
        val attachment = Base64.encodeToString(rawAttachment)

        val serializedResource = loadTemplateWithAttachments(
            "common",
            "documentReference-with-attachment-template",
            RECORD_ID,
            PARTNER_ID,
            "Sample PDF",
            "application/pdf",
            attachment
        )

        val resource = SdkFhirParser.toFhir3(
            resourceType,
            serializedResource
        ) as Fhir3DocumentReference

        val internalResource = SdkFhirParser.toFhir3(
            resourceType,
            serializedResource
        ) as Fhir3DocumentReference

        internalResource.identifier!!.add(
            Fhir3Identifier().also {
                it.assigner = Fhir3Reference().also { ref -> ref.reference = PARTNER_ID }
                it.value = "d4l_f_p_t#$ATTACHMENT_ID"
            }
        )

        internalResource.content[0].attachment.id = ATTACHMENT_ID
        internalResource.content[0].attachment.data = null


        val gcKeys: MutableList<GCKey?> = mutableListOf(
            dataKey,
            attachmentKey,
            dataKey
        )

        runFhirFlowWithAttachment(
            serializedResource = SdkFhirParser.fromResource(internalResource)!!,
            data = rawAttachment,
            tags = tags,
            gcKeys = gcKeys,
            annotations = flowHelper.prepareAnnotations(annotations),
            attachmentId = ATTACHMENT_ID
        )

        // When
        val result = recordService.createRecord(
            USER_ID,
            resource,
            annotations = annotations
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
            expected = resource,
            actual = result.resource
        )
        assertEquals(
            actual = resource.content.size,
            expected = 1
        )
        assertEquals(
            actual = resource.content[0].attachment.data,
            expected = attachment
        )
        assertEquals(
            actual = resource.content[0].attachment.id,
            expected = ATTACHMENT_ID
        )
        assertEquals(
            actual = resource.identifier?.get(1)?.value,
            expected = "d4l_f_p_t#$ATTACHMENT_ID"
        )
    }

    @Test
    fun `Given, createFhir3Record is called with the appropriate payload with Annotations and Attachments, it creates a Record for Fhir3, while resizing the attachment`() {
        // Given
        val resourceType = "DocumentReference"
        val tags = mapOf(
            "partner" to PARTNER_ID,
            "client" to CLIENT_ID,
            "fhirversion" to "3.0.1",
            "resourcetype" to resourceType
        ).also { flowHelper.prepareTags(it as Tags) }

        val annotations = listOf(
            "wow",
            "it",
            "works",
            "and",
            "like_a_duracell_häsi"
        )

        val rawAttachment = TestResourceHelper.getByteResource("attachments", "sample.png")
        val attachment = Base64.encodeToString(rawAttachment)

        val serializedResource = loadTemplateWithAttachments(
            "common",
            "documentReference-with-attachment-template",
            RECORD_ID,
            PARTNER_ID,
            "Sample PNG",
            "image/png",
            attachment
        )

        val resource = SdkFhirParser.toFhir3(
            resourceType,
            serializedResource
        ) as Fhir3DocumentReference

        val internalResource = SdkFhirParser.toFhir3(
            resourceType,
            serializedResource
        ) as Fhir3DocumentReference

        internalResource.identifier!!.add(
            Fhir3Identifier().also {
                it.assigner = Fhir3Reference().also { ref -> ref.reference = PARTNER_ID }
                it.value = "d4l_f_p_t#$ATTACHMENT_ID#$PREVIEW_ID#$THUMBNAIL_ID"
            }
        )

        internalResource.content[0].attachment.id = ATTACHMENT_ID
        internalResource.content[0].attachment.data = null


        val gcKeys: MutableList<GCKey?> = mutableListOf(
            dataKey,
            attachmentKey,
            dataKey
        )

        val preview = Pair(ByteArray(2), PREVIEW_ID)
        val thumbnail = Pair(ByteArray(1), THUMBNAIL_ID)

        runFhirFlowWithAttachment(
            serializedResource = SdkFhirParser.fromResource(internalResource)!!,
            data = rawAttachment,
            tags = tags,
            gcKeys = gcKeys,
            annotations = flowHelper.prepareAnnotations(annotations),
            attachmentId = ATTACHMENT_ID,
            resizedImages = Pair(preview, thumbnail)
        )

        // When
        val result = recordService.createRecord(
            USER_ID,
            resource,
            annotations = annotations
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
            expected = resource,
            actual = result.resource
        )
        assertEquals(
            actual = resource.content.size,
            expected = 1
        )
        assertEquals(
            actual = resource.content[0].attachment.data,
            expected = attachment
        )
        assertEquals(
            actual = resource.content[0].attachment.id,
            expected = ATTACHMENT_ID
        )
        assertEquals(
            actual = resource.identifier?.get(1)?.value,
            expected = "d4l_f_p_t#$ATTACHMENT_ID#$PREVIEW_ID#$THUMBNAIL_ID"
        )
    }

    @Test
    fun `Given, createFhir3Record is called with the appropriate payload with Annotations and Attachments, it fails due to a ill Attachment`() {
        // Given
        val resourceType = "DocumentReference"
        val tags = mapOf(
            "partner" to PARTNER_ID,
            "client" to CLIENT_ID,
            "fhirversion" to "3.0.1",
            "resourcetype" to resourceType
        ).also { flowHelper.prepareTags(it as Tags) }

        val annotations = listOf(
            "wow",
            "it",
            "works",
            "and",
            "like_a_duracell_häsi"
        )

        val rawAttachment = PDF_OVERSIZED
        val attachment = PDF_OVERSIZED_ENCODED

        val serializedResource = loadTemplateWithAttachments(
            "common",
            "documentReference-with-attachment-template",
            RECORD_ID,
            PARTNER_ID,
            "Sample PDF",
            "application/pdf",
            attachment
        )

        val resource = SdkFhirParser.toFhir3(
            resourceType,
            serializedResource
        ) as Fhir3DocumentReference


        val gcKeys: MutableList<GCKey?> = mutableListOf(
            dataKey,
            attachmentKey,
            dataKey
        )

        runFhirFlowWithAttachment(
            serializedResource = SdkFhirParser.fromResource(resource)!!,
            data = rawAttachment,
            tags = tags,
            gcKeys = gcKeys,
            annotations = flowHelper.prepareAnnotations(annotations),
            attachmentId = ATTACHMENT_ID
        )

        // Then
        assertFailsWith<DataRestrictionException.MaxDataSizeViolation> {
            // When
            recordService.createRecord(
                USER_ID,
                resource,
                annotations = annotations
            ).blockingGet()
        }
    }

    // FHIR4
    @Test
    fun `Given, createFhir4Record is called with the appropriate payload without Annotations or Attachments, it creates a Record for Fhir4`() {
        // Given
        val resourceType = "DocumentReference"
        val tags = mapOf(
            "partner" to PARTNER_ID,
            "client" to CLIENT_ID,
            "fhirversion" to "4.0.1",
            "resourcetype" to resourceType
        ).also { flowHelper.prepareTags(it as Tags) }

        val serializedResource = loadTemplate(
            "common",
            "documentReference-without-attachment-template",
            RECORD_ID,
            PARTNER_ID
        )

        val resource = SdkFhirParser.toFhir4(
            resourceType,
            serializedResource
        ) as Fhir4DocumentReference


        val gcKeys: MutableList<GCKey?> = mutableListOf(
            dataKey,
            null,
            dataKey
        )

        runFhirFlow(
            serializedResource = SdkFhirParser.fromResource(resource)!!,
            tags = tags,
            gcKeys = gcKeys,
            attachmentKey = null
        )

        // When
        val result = recordService.createRecord(
            USER_ID,
            resource,
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
            expected = resource,
            actual = result.resource
        )
    }

    @Test
    fun `Given, createFhir4Record is called with the appropriate payload with Annotations and without Attachments, it creates a Record for Fhir4`() {
        // Given
        val resourceType = "DocumentReference"
        val tags = mapOf(
            "partner" to PARTNER_ID,
            "client" to CLIENT_ID,
            "fhirversion" to "4.0.1",
            "resourcetype" to resourceType
        ).also { flowHelper.prepareTags(it as Tags) }

        val annotations = listOf(
            "wow",
            "it",
            "works",
            "and",
            "like_a_duracell_häsi"
        )

        val serializedResource = loadTemplate(
            "common",
            "documentReference-without-attachment-template",
            RECORD_ID,
            PARTNER_ID
        )

        val resource = SdkFhirParser.toFhir4(
            resourceType,
            serializedResource
        ) as Fhir4DocumentReference


        val gcKeys: MutableList<GCKey?> = mutableListOf(
            dataKey,
            null,
            dataKey
        )

        runFhirFlow(
            serializedResource = SdkFhirParser.fromResource(resource)!!,
            tags = tags,
            gcKeys = gcKeys,
            annotations = flowHelper.prepareAnnotations(annotations),
            attachmentKey = null
        )

        // When
        val result = recordService.createRecord(
            USER_ID,
            resource,
            annotations = annotations
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
            expected = resource,
            actual = result.resource
        )
    }

    @Test
    fun `Given, createFhir4Record is called with the appropriate payload with Annotations and Attachments, it creates a Record for Fhir4`() {
        // Given
        val resourceType = "DocumentReference"
        val tags = mapOf(
            "partner" to PARTNER_ID,
            "client" to CLIENT_ID,
            "fhirversion" to "4.0.1",
            "resourcetype" to resourceType
        ).also { flowHelper.prepareTags(it as Tags) }

        val annotations = listOf(
            "wow",
            "it",
            "works",
            "and",
            "like_a_duracell_häsi"
        )

        val rawAttachment = TestResourceHelper.getByteResource("attachments", "sample.pdf")
        val attachment = Base64.encodeToString(rawAttachment)

        val serializedResource = loadTemplateWithAttachments(
            "common",
            "documentReference-with-attachment-template",
            RECORD_ID,
            PARTNER_ID,
            "Sample PDF",
            "application/pdf",
            attachment
        )

        val resource = SdkFhirParser.toFhir4(
            resourceType,
            serializedResource
        ) as Fhir4DocumentReference

        val internalResource = SdkFhirParser.toFhir4(
            resourceType,
            serializedResource
        ) as Fhir4DocumentReference

        internalResource.identifier!!.add(
            Fhir4Identifier().also {
                it.assigner = Fhir4Reference()
                    .also { ref -> ref.reference = PARTNER_ID }
                it.value = "d4l_f_p_t#$ATTACHMENT_ID"
            }
        )

        internalResource.content[0].attachment.id = ATTACHMENT_ID
        internalResource.content[0].attachment.data = null


        val gcKeys: MutableList<GCKey?> = mutableListOf(
            dataKey,
            attachmentKey,
            dataKey
        )

        runFhirFlowWithAttachment(
            serializedResource = SdkFhirParser.fromResource(internalResource)!!,
            data = rawAttachment,
            tags = tags,
            gcKeys = gcKeys,
            annotations = flowHelper.prepareAnnotations(annotations),
            attachmentId = ATTACHMENT_ID
        )

        // When
        val result = recordService.createRecord(
            USER_ID,
            resource,
            annotations = annotations
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
            expected = resource,
            actual = result.resource
        )
        assertEquals(
            actual = resource.content.size,
            expected = 1
        )
        assertEquals(
            actual = resource.content[0].attachment.data,
            expected = attachment
        )
        assertEquals(
            actual = resource.content[0].attachment.id,
            expected = ATTACHMENT_ID
        )
        assertEquals(
            actual = resource.identifier?.get(1)?.value,
            expected = "d4l_f_p_t#$ATTACHMENT_ID"
        )
    }

    @Test
    fun `Given, createFhir4Record is called with the appropriate payload with Annotations and Attachments, it creates a Record for Fhir4, while resizing the attachment`() {
        // Given
        val resourceType = "DocumentReference"
        val tags = mapOf(
            "partner" to PARTNER_ID,
            "client" to CLIENT_ID,
            "fhirversion" to "4.0.1",
            "resourcetype" to resourceType
        ).also { flowHelper.prepareTags(it as Tags) }

        val annotations = listOf(
            "wow",
            "it",
            "works",
            "and",
            "like_a_duracell_häsi"
        )

        val rawAttachment = TestResourceHelper.getByteResource("attachments", "sample.png")
        val attachment = Base64.encodeToString(rawAttachment)

        val serializedResource = loadTemplateWithAttachments(
            "common",
            "documentReference-with-attachment-template",
            RECORD_ID,
            PARTNER_ID,
            "Sample PNG",
            "image/png",
            attachment
        )

        val resource = SdkFhirParser.toFhir4(
            resourceType,
            serializedResource
        ) as Fhir4DocumentReference

        val internalResource = SdkFhirParser.toFhir4(
            resourceType,
            serializedResource
        ) as Fhir4DocumentReference

        internalResource.identifier!!.add(
            Fhir4Identifier().also {
                it.assigner = Fhir4Reference()
                    .also { ref -> ref.reference = PARTNER_ID }
                it.value = "d4l_f_p_t#$ATTACHMENT_ID#$PREVIEW_ID#$THUMBNAIL_ID"
            }
        )

        internalResource.content[0].attachment.id = ATTACHMENT_ID
        internalResource.content[0].attachment.data = null


        val gcKeys: MutableList<GCKey?> = mutableListOf(
            dataKey,
            attachmentKey,
            dataKey
        )

        val preview = Pair(ByteArray(2), PREVIEW_ID)
        val thumbnail = Pair(ByteArray(1), THUMBNAIL_ID)

        runFhirFlowWithAttachment(
            serializedResource = SdkFhirParser.fromResource(internalResource)!!,
            data = rawAttachment,
            tags = tags,
            gcKeys = gcKeys,
            annotations = flowHelper.prepareAnnotations(annotations),
            attachmentId = ATTACHMENT_ID,
            resizedImages = Pair(preview, thumbnail)
        )

        // When
        val result = recordService.createRecord(
            USER_ID,
            resource,
            annotations = annotations
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
            expected = resource,
            actual = result.resource
        )
        assertEquals(
            actual = resource.content.size,
            expected = 1
        )
        assertEquals(
            actual = resource.content[0].attachment.data,
            expected = attachment
        )
        assertEquals(
            actual = resource.content[0].attachment.id,
            expected = ATTACHMENT_ID
        )
        assertEquals(
            actual = resource.identifier?.get(1)?.value,
            expected = "d4l_f_p_t#$ATTACHMENT_ID#$PREVIEW_ID#$THUMBNAIL_ID"
        )
    }

    @Test
    fun `Given, createFhir4Record is called with the appropriate payload with Annotations and Attachments, it fails due to a ill Attachment`() {
        // Given
        val resourceType = "DocumentReference"
        val tags = mapOf(
            "partner" to PARTNER_ID,
            "client" to CLIENT_ID,
            "fhirversion" to "4.0.1",
            "resourcetype" to resourceType
        ).also { flowHelper.prepareTags(it as Tags) }

        val annotations = listOf(
            "wow",
            "it",
            "works",
            "and",
            "like_a_duracell_häsi"
        )

        val rawAttachment = PDF_OVERSIZED
        val attachment = PDF_OVERSIZED_ENCODED

        val serializedResource = loadTemplateWithAttachments(
            "common",
            "documentReference-with-attachment-template",
            RECORD_ID,
            PARTNER_ID,
            "Sample PNG",
            "image/png",
            attachment
        )

        val resource = SdkFhirParser.toFhir4(
            resourceType,
            serializedResource
        ) as Fhir4DocumentReference


        val gcKeys: MutableList<GCKey?> = mutableListOf(
            dataKey,
            attachmentKey,
            dataKey
        )

        runFhirFlowWithAttachment(
            serializedResource = SdkFhirParser.fromResource(resource)!!,
            data = rawAttachment,
            tags = tags,
            gcKeys = gcKeys,
            annotations = flowHelper.prepareAnnotations(annotations),
            attachmentId = ATTACHMENT_ID
        )

        // Then
        assertFailsWith<DataRestrictionException.MaxDataSizeViolation> {
            // When
            recordService.createRecord(
                USER_ID,
                resource,
                annotations = annotations
            ).blockingGet()
        }
    }

    // Arbitrary Data
    @Test
    fun `Given, createDataRecord is called with the appropriate payload without Annotations, it creates a Record for Arbitrary Data`() {
        // Given
        val payload = "Me and my poney its name is Johny."
        val resource = DataResource(payload.toByteArray())

        val tags = mapOf(
            "flag" to ARBITRARY_DATA_KEY,
            "partner" to PARTNER_ID,
            "client" to CLIENT_ID
        ).also { flowHelper.prepareTags(it as Tags) }

        val gcKeys: MutableList<GCKey?> = mutableListOf(
            dataKey,
            null,
            dataKey
        )

        runArbitraryDataFlow(
            serializedResource = payload,
            tags = tags,
            gcKeys = gcKeys
        )

        // When
        val result = recordService.createRecord(
            USER_ID,
            resource,
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
            expected = resource,
            actual = result.resource
        )
    }

    @Test
    fun `Given, createDataRecord is called with the appropriate payload with Annotations, it creates a Record for Arbitrary Data`() {
        // Given
        val payload = "Me and my poney its name is Johny."
        val resource = DataResource(payload.toByteArray())

        val tags = mapOf(
            "flag" to ARBITRARY_DATA_KEY,
            "partner" to PARTNER_ID,
            "client" to CLIENT_ID
        ).also { flowHelper.prepareTags(it as Tags) }

        val annotations = listOf(
            "wow",
            "it",
            "works",
            "and",
            "like_a_duracell_häsi"
        )

        val gcKeys: MutableList<GCKey?> = mutableListOf(
            dataKey,
            null,
            dataKey
        )

        runArbitraryDataFlow(
            serializedResource = payload,
            tags = tags,
            annotations = flowHelper.prepareAnnotations(annotations),
            gcKeys = gcKeys
        )

        // When
        val result = recordService.createRecord(
            USER_ID,
            resource,
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
            expected = resource,
            actual = result.resource
        )
    }
}
