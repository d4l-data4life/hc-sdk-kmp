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
import care.data4life.sdk.crypto.CryptoContract
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
import care.data4life.sdk.test.fake.CryptoServiceFake
import care.data4life.sdk.test.fake.CryptoServiceIteration
import care.data4life.sdk.test.util.GenericTestDataProvider
import care.data4life.sdk.test.util.GenericTestDataProvider.ALIAS
import care.data4life.sdk.test.util.GenericTestDataProvider.ATTACHMENT_ID
import care.data4life.sdk.test.util.GenericTestDataProvider.CLIENT_ID
import care.data4life.sdk.test.util.GenericTestDataProvider.COMMON_KEY_ID
import care.data4life.sdk.test.util.GenericTestDataProvider.CREATION_DATE
import care.data4life.sdk.test.util.GenericTestDataProvider.PARTNER_ID
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
import io.reactivex.Single
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import care.data4life.fhir.r4.model.DocumentReference as Fhir4DocumentReference
import care.data4life.fhir.r4.model.Reference as Fhir4Reference
import care.data4life.fhir.stu3.model.DocumentReference as Fhir3DocumentReference
import care.data4life.fhir.stu3.model.Reference as Fhir3Reference

class RecordServiceUpdateModuleTest {
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
    private val fileService: FileService = mockk()
    private val imageResizer: ImageResizer = mockk()
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

    private fun prepareEncryptedFhirRecord(
            recordId: String,
            resource: String,
            tags: List<String>,
            annotations: List<String>,
            commonKeyId: String,
            encryptedDataKey: EncryptedKey,
            encryptedAttachmentsKey: EncryptedKey?,
            creationDate: String,
            updateDate: String
    ): EncryptedRecord = flowHelper.buildEncryptedRecord(
            recordId,
            commonKeyId,
            tags,
            annotations,
            resource,
            Pair(creationDate, updateDate),
            Pair(encryptedDataKey, encryptedAttachmentsKey)
    )

    private fun prepareEncryptedDataRecord(
            recordId: String,
            resource: String,
            tags: List<String>,
            annotations: List<String>,
            commonKeyId: String,
            encryptedDataKey: EncryptedKey,
            encryptedAttachmentsKey: EncryptedKey?,
            creationDate: String,
            updateDate: String
    ): EncryptedRecord = flowHelper.buildEncryptedRecordWithEncodedBody(
            recordId,
            commonKeyId,
            tags,
            annotations,
            resource,
            Pair(creationDate, updateDate),
            Pair(encryptedDataKey, encryptedAttachmentsKey)
    )

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

        every {
            apiService.updateRecord(alias, userId, recordId, encryptedReceivedRecord.copy(updatedDate = null))
        } answers {
            Single.just(encryptedReceivedRecord).also {
                (cryptoService as CryptoServiceFake).iteration = receivedIteration
            }
        }
    }

    private fun runFlow(
            encryptedUploadRecord: EncryptedRecord,
            encryptedReceivedRecord: EncryptedRecord,
            serializedResourceOld: String,
            serializedResourceNew: String,
            tags: List<String>,
            annotations: List<String>,
            useStoredCommonKey: Boolean,
            commonKey: Pair<String, GCKey>,
            dataKey: Pair<GCKey, EncryptedKey>,
            attachmentKey: Pair<GCKey, EncryptedKey>?,
            tagEncryptionKey: GCKey,
            userId: String,
            alias: String,
            recordId: String
    ) {
        val encryptedCommonKey = flowHelper.prepareStoredOrUnstoredCommonKeyRun(
                alias,
                userId,
                commonKey.first,
                useStoredCommonKey
        )

        val keyOrder = if (attachmentKey is Pair<*, *>) {
            listOf(dataKey.first, attachmentKey.first)
        } else {
            listOf(dataKey.first)
        }

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
                resources = listOf(serializedResourceOld, serializedResourceNew),
                tags = tags,
                annotations = annotations,
                hashFunction = { value -> flowHelper.md5(value) }
        )

        val receivedIteration = CryptoServiceIteration(
                gcKeyOrder = listOf(),
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

    private fun runFhirFlow(
            serializedResourceOld: String,
            serializedResourceNew: String,
            tags: List<String>,
            annotations: List<String> = emptyList(),
            useStoredCommonKey: Boolean = true,
            commonKey: Pair<String, GCKey> = COMMON_KEY_ID to this.commonKey,
            dataKey: Pair<GCKey, EncryptedKey> = this.dataKey to encryptedDataKey,
            tagEncryptionKey: GCKey = this.tagEncryptionKey,
            recordId: String = RECORD_ID,
            userId: String = USER_ID,
            alias: String = ALIAS,
            creationDate: String = CREATION_DATE,
            updateDates: Pair<String, String>,
            oldTags: List<String>? = null
    ) {
        val encryptedUploadRecord = prepareEncryptedFhirRecord(
                recordId,
                serializedResourceOld,
                oldTags ?: tags,
                annotations,
                commonKey.first,
                dataKey.second,
                null,
                creationDate,
                updateDates.first
        )

        val encryptedReceivedRecord = prepareEncryptedFhirRecord(
                recordId,
                serializedResourceNew,
                tags,
                annotations,
                commonKey.first,
                dataKey.second,
                null,
                creationDate,
                updateDates.second
        )

        val allTags = if (oldTags is List) {
            mutableListOf<String>().also {
                it.addAll(tags)
                it.addAll(oldTags)
            }
        } else {
            tags
        }

        runFlow(
                encryptedUploadRecord,
                encryptedReceivedRecord,
                serializedResourceOld,
                serializedResourceNew,
                allTags,
                annotations,
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
            tags: List<String>,
            annotations: List<String> = emptyList(),
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
        val encryptedUploadRecord = prepareEncryptedFhirRecord(
                recordId,
                serializedResourceOld,
                tags,
                annotations,
                commonKey.first,
                dataKey.second,
                attachmentKey.second,
                creationDate,
                updateDates.first
        )

        val encryptedReceivedRecord = prepareEncryptedFhirRecord(
                recordId,
                serializedResourceNew,
                tags,
                annotations,
                commonKey.first,
                dataKey.second,
                attachmentKey.second,
                creationDate,
                updateDates.second
        )

        runFlow(
                encryptedUploadRecord,
                encryptedReceivedRecord,
                serializedResourceOld,
                serializedResourceNew,
                tags,
                annotations,
                useStoredCommonKey,
                commonKey,
                dataKey,
                attachmentKey,
                tagEncryptionKey,
                userId,
                alias,
                recordId
        )

        flowHelper.uploadAttachment(
                attachmentKey = attachmentKey.first,
                payload = Pair(attachmentData, attachmentId),
                userId = userId,
                resized = resizedImages
        )
    }

    private fun runDataFlow(
            serializedResourceOld: String,
            serializedResourceNew: String,
            tags: List<String>,
            annotations: List<String> = emptyList(),
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
        val encryptedUploadRecord = prepareEncryptedDataRecord(
                recordId,
                serializedResourceOld,
                tags,
                annotations,
                commonKey.first,
                dataKey.second,
                null,
                creationDate,
                updateDates.first
        )

        val encryptedReceivedRecord = prepareEncryptedDataRecord(
                recordId,
                serializedResourceNew,
                tags,
                annotations,
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
                tags,
                annotations,
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
        ).also { flowHelper.prepareTags(it as Tags) }

        val template = TestResourceHelper.loadTemplate(
                "common",
                "documentReference-without-attachment-template",
                RECORD_ID,
                PARTNER_ID
        )

        val now = SdkDateTimeFormatter.now()

        val resourceNew = SdkFhirParser.toFhir3(
                resourceType,
                template
        ) as Fhir3DocumentReference

        val resourceOld = SdkFhirParser.toFhir3(
                resourceType,
                template
        ) as Fhir3DocumentReference

        resourceOld.description = "A outdated mock"

        runFhirFlow(
                serializedResourceOld = SdkFhirParser.fromResource(resourceOld)!!,
                serializedResourceNew = SdkFhirParser.fromResource(resourceNew)!!,
                tags = tags.values.toList(),
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
        ).also { flowHelper.prepareTags(it as Tags) }

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

        val resourceNew = SdkFhirParser.toFhir3(
                resourceType,
                template
        ) as Fhir3DocumentReference

        val resourceOld = SdkFhirParser.toFhir3(
                resourceType,
                template
        ) as Fhir3DocumentReference

        resourceOld.description = "A outdated mock"

        runFhirFlow(
                serializedResourceOld = SdkFhirParser.fromResource(resourceOld)!!,
                serializedResourceNew = SdkFhirParser.fromResource(resourceNew)!!,
                tags = tags.values.toList(),
                annotations = flowHelper.prepareAnnotations(annotations),
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

        val template = TestResourceHelper.loadTemplateWithAttachments(
                "common",
                "documentReference-with-attachment-template",
                RECORD_ID,
                PARTNER_ID,
                "Sample PDF",
                "application/pdf",
                attachment,
                "d4l_f_p_t#${ATTACHMENT_ID}#${PREVIEW_ID}#${THUMBNAIL_ID}"
        )

        val internalResource = SdkFhirParser.toFhir3(
                resourceType,
                template
        ) as Fhir3DocumentReference

        val resourceNew = SdkFhirParser.toFhir3(
                resourceType,
                template
        ) as Fhir3DocumentReference

        val resourceOld = SdkFhirParser.toFhir3(
                resourceType,
                template
        ) as Fhir3DocumentReference

        resourceOld.description = "A outdated mock"
        resourceOld.content[0].attachment.data = null

        resourceNew.identifier = mutableListOf(
                Fhir3Identifier().also {
                    it.value = "d4l_f_p_t#${ATTACHMENT_ID}#${PREVIEW_ID}#${THUMBNAIL_ID}"
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
        internalResource.content[0].attachment.id = "${ATTACHMENT_ID}#${PREVIEW_ID}#${THUMBNAIL_ID}"
        internalResource.content[0].attachment.data = null

        runFhirFlowWithAttachment(
                serializedResourceOld = SdkFhirParser.fromResource(resourceOld)!!,
                serializedResourceNew = SdkFhirParser.fromResource(internalResource)!!,
                tags = tags.values.toList(),
                annotations = flowHelper.prepareAnnotations(annotations),
                updateDates = Pair(SdkDateTimeFormatter.now(), UPDATE_DATE),
                attachmentId = "${ATTACHMENT_ID}#${PREVIEW_ID}#${THUMBNAIL_ID}",
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
                expected = result.resource,
                actual = result.resource
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
                expected = "${ATTACHMENT_ID}#${PREVIEW_ID}#${THUMBNAIL_ID}"
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

        val template = TestResourceHelper.loadTemplateWithAttachments(
                "common",
                "documentReference-with-attachment-template",
                RECORD_ID,
                PARTNER_ID,
                "Sample PDF",
                "application/pdf",
                attachment
        )

        val internalResource = SdkFhirParser.toFhir3(
                resourceType,
                template
        ) as Fhir3DocumentReference

        val resourceNew = SdkFhirParser.toFhir3(
                resourceType,
                template
        ) as Fhir3DocumentReference

        val resourceOld = SdkFhirParser.toFhir3(
                resourceType,
                template
        ) as Fhir3DocumentReference

        resourceOld.description = "A outdated mock"
        resourceOld.content[0].attachment.data = null

        resourceNew.identifier = null

        internalResource.content[0].attachment.id = ATTACHMENT_ID
        internalResource.content[0].attachment.data = null
        internalResource.identifier = mutableListOf(
                Fhir3Identifier().also {
                    it.value = "d4l_f_p_t#${ATTACHMENT_ID}#${PREVIEW_ID}#${THUMBNAIL_ID}"
                    it.assigner = Fhir3Reference().also { ref -> ref.reference = PARTNER_ID }
                }
        )

        val preview = Pair(ByteArray(2), PREVIEW_ID)
        val thumbnail = Pair(ByteArray(1), THUMBNAIL_ID)

        runFhirFlowWithAttachment(
                serializedResourceOld = SdkFhirParser.fromResource(resourceOld)!!,
                serializedResourceNew = SdkFhirParser.fromResource(internalResource)!!,
                tags = tags.values.toList(),
                annotations = flowHelper.prepareAnnotations(annotations),
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
                expected = result.resource,
                actual = result.resource
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
        ).also { flowHelper.prepareTags(it as Tags) }

        val annotations = listOf(
                "wow",
                "it",
                "works",
                "and",
                "like_a_duracell_häsi"
        )

        val rawAttachment = GenericTestDataProvider.PDF_OVERSIZED
        val attachment = GenericTestDataProvider.PDF_OVERSIZED_ENCODED

        val template = TestResourceHelper.loadTemplateWithAttachments(
                "common",
                "documentReference-with-attachment-template",
                RECORD_ID,
                PARTNER_ID,
                "Sample PDF",
                "application/pdf",
                attachment
        )

        val internalResource = SdkFhirParser.toFhir3(
                resourceType,
                template
        ) as Fhir3DocumentReference

        val resourceNew = SdkFhirParser.toFhir3(
                resourceType,
                template
        ) as Fhir3DocumentReference

        val resourceOld = SdkFhirParser.toFhir3(
                resourceType,
                template
        ) as Fhir3DocumentReference

        runFhirFlowWithAttachment(
                serializedResourceOld = SdkFhirParser.fromResource(resourceOld)!!,
                serializedResourceNew = SdkFhirParser.fromResource(internalResource)!!,
                tags = tags.values.toList(),
                annotations = flowHelper.prepareAnnotations(annotations),
                updateDates = Pair(SdkDateTimeFormatter.now(), UPDATE_DATE),
                attachmentId = ATTACHMENT_ID,
                attachmentData = rawAttachment
        )

        // Then
        assertFailsWith<DataRestrictionException.MaxDataSizeViolation> {
            // When
            recordService.updateRecord(
                    USER_ID,
                    RECORD_ID,
                    resourceNew,
                    annotations
            ).blockingGet()
        }
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
        ).also { flowHelper.prepareTags(it as Tags) }

        val template = TestResourceHelper.loadTemplate(
                "common",
                "documentReference-without-attachment-template",
                RECORD_ID,
                PARTNER_ID
        )

        val now = SdkDateTimeFormatter.now()

        val resourceNew = SdkFhirParser.toFhir4(
                resourceType,
                template
        ) as Fhir4DocumentReference

        val resourceOld = SdkFhirParser.toFhir4(
                resourceType,
                template
        ) as Fhir4DocumentReference

        resourceOld.description = "A outdated mock"

        runFhirFlow(
                serializedResourceOld = SdkFhirParser.fromResource(resourceOld)!!,
                serializedResourceNew = SdkFhirParser.fromResource(resourceNew)!!,
                tags = tags.values.toList(),
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
        ).also { flowHelper.prepareTags(it as Tags) }

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

        val resourceNew = SdkFhirParser.toFhir4(
                resourceType,
                template
        ) as Fhir4DocumentReference

        val resourceOld = SdkFhirParser.toFhir4(
                resourceType,
                template
        ) as Fhir4DocumentReference

        resourceOld.description = "A outdated mock"

        runFhirFlow(
                serializedResourceOld = SdkFhirParser.fromResource(resourceOld)!!,
                serializedResourceNew = SdkFhirParser.fromResource(resourceNew)!!,
                tags = tags.values.toList(),
                annotations = flowHelper.prepareAnnotations(annotations),
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

        val template = TestResourceHelper.loadTemplateWithAttachments(
                "common",
                "documentReference-with-attachment-template",
                RECORD_ID,
                PARTNER_ID,
                "Sample PDF",
                "application/pdf",
                attachment,
                "d4l_f_p_t#${ATTACHMENT_ID}#${PREVIEW_ID}#${THUMBNAIL_ID}"
        )

        val internalResource = SdkFhirParser.toFhir4(
                resourceType,
                template
        ) as Fhir4DocumentReference

        val resourceNew = SdkFhirParser.toFhir4(
                resourceType,
                template
        ) as Fhir4DocumentReference

        val resourceOld = SdkFhirParser.toFhir4(
                resourceType,
                template
        ) as Fhir4DocumentReference

        resourceOld.description = "A outdated mock"
        resourceOld.content[0].attachment.data = null

        resourceNew.identifier = mutableListOf(
                Fhir4Identifier().also {
                    it.value = "d4l_f_p_t#${ATTACHMENT_ID}#${PREVIEW_ID}#${THUMBNAIL_ID}"
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
        internalResource.content[0].attachment.id = "${ATTACHMENT_ID}#${PREVIEW_ID}#${THUMBNAIL_ID}"
        internalResource.content[0].attachment.data = null

        runFhirFlowWithAttachment(
                serializedResourceOld = SdkFhirParser.fromResource(resourceOld)!!,
                serializedResourceNew = SdkFhirParser.fromResource(internalResource)!!,
                tags = tags.values.toList(),
                annotations = flowHelper.prepareAnnotations(annotations),
                updateDates = Pair(SdkDateTimeFormatter.now(), UPDATE_DATE),
                attachmentId = "${ATTACHMENT_ID}#${PREVIEW_ID}#${THUMBNAIL_ID}",
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
                expected = result.resource,
                actual = result.resource
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
                expected = "${ATTACHMENT_ID}#${PREVIEW_ID}#${THUMBNAIL_ID}"
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

        val template = TestResourceHelper.loadTemplateWithAttachments(
                "common",
                "documentReference-with-attachment-template",
                RECORD_ID,
                PARTNER_ID,
                "Sample PDF",
                "application/pdf",
                attachment
        )

        val internalResource = SdkFhirParser.toFhir4(
                resourceType,
                template
        ) as Fhir4DocumentReference

        val resourceNew = SdkFhirParser.toFhir4(
                resourceType,
                template
        ) as Fhir4DocumentReference

        val resourceOld = SdkFhirParser.toFhir4(
                resourceType,
                template
        ) as Fhir4DocumentReference

        resourceOld.description = "A outdated mock"
        resourceOld.content[0].attachment.data = null

        resourceNew.identifier = null

        internalResource.content[0].attachment.id = ATTACHMENT_ID
        internalResource.content[0].attachment.data = null
        internalResource.identifier = mutableListOf(
                Fhir4Identifier().also {
                    it.value = "d4l_f_p_t#${ATTACHMENT_ID}#${PREVIEW_ID}#${THUMBNAIL_ID}"
                    it.assigner = Fhir4Reference().also { ref -> ref.reference = PARTNER_ID }
                }
        )

        val preview = Pair(ByteArray(2), PREVIEW_ID)
        val thumbnail = Pair(ByteArray(1), THUMBNAIL_ID)

        runFhirFlowWithAttachment(
                serializedResourceOld = SdkFhirParser.fromResource(resourceOld)!!,
                serializedResourceNew = SdkFhirParser.fromResource(internalResource)!!,
                tags = tags.values.toList(),
                annotations = flowHelper.prepareAnnotations(annotations),
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
                expected = result.resource,
                actual = result.resource
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
        ).also { flowHelper.prepareTags(it as Tags) }

        val annotations = listOf(
                "wow",
                "it",
                "works",
                "and",
                "like_a_duracell_häsi"
        )

        val rawAttachment = GenericTestDataProvider.PDF_OVERSIZED
        val attachment = GenericTestDataProvider.PDF_OVERSIZED_ENCODED

        val template = TestResourceHelper.loadTemplateWithAttachments(
                "common",
                "documentReference-with-attachment-template",
                RECORD_ID,
                PARTNER_ID,
                "Sample PDF",
                "application/pdf",
                attachment
        )

        val internalResource = SdkFhirParser.toFhir4(
                resourceType,
                template
        ) as Fhir4DocumentReference

        val resourceNew = SdkFhirParser.toFhir4(
                resourceType,
                template
        ) as Fhir4DocumentReference

        val resourceOld = SdkFhirParser.toFhir4(
                resourceType,
                template
        ) as Fhir4DocumentReference

        runFhirFlowWithAttachment(
                serializedResourceOld = SdkFhirParser.fromResource(resourceOld)!!,
                serializedResourceNew = SdkFhirParser.fromResource(internalResource)!!,
                tags = tags.values.toList(),
                annotations = flowHelper.prepareAnnotations(annotations),
                updateDates = Pair(SdkDateTimeFormatter.now(), UPDATE_DATE),
                attachmentId = ATTACHMENT_ID,
                attachmentData = rawAttachment
        )

        // Then
        assertFailsWith<DataRestrictionException.MaxDataSizeViolation> {
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
                "flag" to GenericTestDataProvider.ARBITRARY_DATA_KEY,
                "partner" to PARTNER_ID,
                "client" to CLIENT_ID
        ).also { flowHelper.prepareTags(it as Tags) }

        runDataFlow(
                serializedResourceOld = old,
                serializedResourceNew = new,
                tags = tags.values.toList(),
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
                "flag" to GenericTestDataProvider.ARBITRARY_DATA_KEY,
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

        runDataFlow(
                serializedResourceOld = old,
                serializedResourceNew = new,
                tags = tags.values.toList(),
                updateDates = Pair(SdkDateTimeFormatter.now(), UPDATE_DATE),
                annotations = flowHelper.prepareAnnotations(annotations)
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
        ).also { flowHelper.prepareTags(it as Tags) }

        val oldTags = mapOf(
                "partner" to PARTNER_ID,
                "client" to CLIENT_ID,
                "fhirversion" to "3.0.1",
                "resourcetype" to resourceType
        ).also { flowHelper.prepareTags(it as Tags) }

        val template = TestResourceHelper.loadTemplate(
                "common",
                "documentReference-without-attachment-template",
                RECORD_ID,
                PARTNER_ID
        )

        val resourceNew = SdkFhirParser.toFhir4(
                resourceType,
                template
        ) as Fhir4DocumentReference

        val resourceOld = SdkFhirParser.toFhir3(
                resourceType,
                template
        ) as Fhir3DocumentReference

        resourceOld.description = "A outdated mock"

        runFhirFlow(
                serializedResourceOld = SdkFhirParser.fromResource(resourceOld)!!,
                serializedResourceNew = SdkFhirParser.fromResource(resourceNew)!!,
                tags = tags.values.toList(),
                updateDates = Pair(SdkDateTimeFormatter.now(), UPDATE_DATE),
                oldTags = oldTags.values.toList()
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
}
