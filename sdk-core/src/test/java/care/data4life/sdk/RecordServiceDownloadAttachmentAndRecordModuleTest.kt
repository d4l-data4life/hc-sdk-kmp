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
import care.data4life.sdk.attachment.FileService
import care.data4life.sdk.crypto.CryptoContract
import care.data4life.sdk.fhir.Fhir3Identifier
import care.data4life.sdk.fhir.FhirService
import care.data4life.sdk.model.DownloadType
import care.data4life.sdk.model.Record
import care.data4life.sdk.network.model.EncryptedKey
import care.data4life.sdk.record.RecordContract
import care.data4life.sdk.tag.TagEncryptionService
import care.data4life.sdk.tag.TaggingService
import care.data4life.sdk.test.fake.CryptoServiceFake
import care.data4life.sdk.test.fake.CryptoServiceIteration
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
import care.data4life.sdk.util.HashUtil
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
import care.data4life.fhir.stu3.model.DocumentReference as Fhir3DocumentReference
import care.data4life.fhir.stu3.model.Reference as Fhir3Reference

class RecordServiceDownloadAttachmentAndRecordModuleTest {
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

    private fun runAttachmentDownloadFlow(
        serializedResource: String,
        rawAttachment: ByteArray,
        tags: Map<String, String>,
        annotations: List<String> = emptyList(),
        attachmentId: String = ATTACHMENT_ID,
        useStoredCommonKey: Boolean = true,
        commonKey: Pair<String, GCKey> = COMMON_KEY_ID to this.commonKey,
        dataKey: Pair<GCKey, EncryptedKey> = this.dataKey to encryptedDataKey,
        attachmentKey: Pair<GCKey, EncryptedKey> = this.attachmentKey to encryptedAttachmentKey,
        tagEncryptionKey: GCKey = this.tagEncryptionKey,
        recordId: String = RECORD_ID,
        userId: String = USER_ID,
        alias: String = ALIAS,
        creationDate: String = CREATION_DATE,
        updateDate: String = UPDATE_DATE
    ) {
        val encodedTags = flowHelper.prepareTags(tags)
        val encodedAnnotations = flowHelper.prepareAnnotations(annotations)
        val encryptedCommonKey = flowHelper.prepareStoredOrUnstoredCommonKeyRun(
            alias,
            userId,
            commonKey.first,
            useStoredCommonKey
        )

        val encryptedRecord = flowHelper.prepareEncryptedFhirRecord(
            recordId,
            serializedResource,
            encodedTags,
            encodedAnnotations,
            commonKey.first,
            dataKey.second,
            attachmentKey.second,
            creationDate,
            updateDate
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
            attachmentKey = attachmentKey.first,
            encryptedAttachmentKey = attachmentKey.second,
            tagEncryptionKey = tagEncryptionKey,
            tagEncryptionKeyCalls = 1,
            resources = listOf(serializedResource, String(rawAttachment)),
            tags = encodedTags,
            annotations = encodedAnnotations,
            hashFunction = { value -> flowHelper.md5(value) }
        )

        var answeredRecord = false

        // Get resource kicks off the flow
        every {
            apiService.fetchRecord(ALIAS, USER_ID, RECORD_ID)
        } answers {
            if (!answeredRecord) {
                answeredRecord = true
                (cryptoService as CryptoServiceFake).iteration = receivedIteration
                Single.just(encryptedRecord)
            } else {
                throw RuntimeException("Unexpected call to ApiService#fetchRecord.")
            }
        }

        // Get attachment
        flowHelper.downloadAttachment(userId, alias, Pair(rawAttachment, attachmentId))
    }

    // Attachments
    // FHIR 3
    @Test
    fun `Given, downloadAttachment is called with its appropriate parameter, it returns a Fhir3Attachment`() {
        // Given
        val resourceType = "DocumentReference"
        val tags = mapOf(
            "partner" to PARTNER_ID,
            "client" to CLIENT_ID,
            "fhirversion" to "3.0.1",
            "resourcetype" to resourceType
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

        internalResource.identifier!!.add(
            Fhir3Identifier().also {
                it.assigner = Fhir3Reference().also { ref -> ref.reference = PARTNER_ID }
                it.value = "d4l_f_p_t#${ATTACHMENT_ID}#${PREVIEW_ID}#${THUMBNAIL_ID}"
            }
        )

        internalResource.content[0].attachment.id = ATTACHMENT_ID
        internalResource.content[0].attachment.data = null
        internalResource.content[0].attachment.hash = Base64.encodeToString(HashUtil.sha1(String(rawAttachment).toByteArray()))

        runAttachmentDownloadFlow(
            serializedResource = SdkFhirParser.fromResource(internalResource)!!,
            rawAttachment = rawAttachment,
            tags = tags,
            attachmentId = ATTACHMENT_ID
        )

        // When
        val result = recordService.downloadAttachment(
            RECORD_ID,
            ATTACHMENT_ID,
            USER_ID,
            DownloadType.Full
        ).blockingGet()

        // Then
        assertEquals(
            actual = Base64.decodeToString(result.data!!),
            expected = String(rawAttachment)
        )
    }

    // FHIR 4
    @Test
    fun `Given, downloadAttachment is called with its appropriate parameter, it fails, if the referenced Resource is Fhir4`() {
        // Given
        val resourceType = "DocumentReference"
        val tags = mapOf(
            "partner" to PARTNER_ID,
            "client" to CLIENT_ID,
            "fhirversion" to "4.0.1",
            "resourcetype" to resourceType
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

        internalResource.identifier!!.add(
            Fhir3Identifier().also {
                it.assigner = Fhir3Reference().also { ref -> ref.reference = PARTNER_ID }
                it.value = "d4l_f_p_t#${ATTACHMENT_ID}#${PREVIEW_ID}#${THUMBNAIL_ID}"
            }
        )

        internalResource.content[0].attachment.id = ATTACHMENT_ID
        internalResource.content[0].attachment.data = null
        internalResource.content[0].attachment.hash = Base64.encodeToString(HashUtil.sha1(String(rawAttachment).toByteArray()))

        runAttachmentDownloadFlow(
            serializedResource = SdkFhirParser.fromResource(internalResource)!!,
            rawAttachment = rawAttachment,
            tags = tags,
            attachmentId = ATTACHMENT_ID
        )

        // Then
        assertFailsWith<ClassCastException> {
            // When
            recordService.downloadAttachment(
                RECORD_ID,
                ATTACHMENT_ID,
                USER_ID,
                DownloadType.Full
            ).blockingGet()
        }
    }

    // Records
    // FHIR 3
    @Test
    fun `Given, downloadRecord is called with its appropriate parameter, it returns a Fhir3Record`() {
        // Given
        val resourceType = "DocumentReference"
        val tags = mapOf(
            "partner" to PARTNER_ID,
            "client" to CLIENT_ID,
            "fhirversion" to "3.0.1",
            "resourcetype" to resourceType
        )

        val rawAttachment = TestResourceHelper.getByteResource("attachments", "sample.pdf")
        val attachment = Base64.encodeToString(String(rawAttachment))

        val template = TestResourceHelper.loadTemplateWithAttachments(
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
            template
        ) as Fhir3DocumentReference

        val internalResource = SdkFhirParser.toFhir3(
            resourceType,
            template
        ) as Fhir3DocumentReference

        resource.identifier!!.add(
            Fhir3Identifier().also {
                it.assigner = Fhir3Reference().also { ref -> ref.reference = PARTNER_ID }
                it.value = "d4l_f_p_t#${ATTACHMENT_ID}#${PREVIEW_ID}#${THUMBNAIL_ID}"
            }
        )

        resource.content[0].attachment.id = ATTACHMENT_ID
        resource.content[0].attachment.hash = Base64.encodeToString(HashUtil.sha1(String(rawAttachment).toByteArray()))

        internalResource.identifier!!.add(
            Fhir3Identifier().also {
                it.assigner = Fhir3Reference().also { ref -> ref.reference = PARTNER_ID }
                it.value = "d4l_f_p_t#${ATTACHMENT_ID}#${PREVIEW_ID}#${THUMBNAIL_ID}"
            }
        )

        internalResource.content[0].attachment.id = ATTACHMENT_ID
        internalResource.content[0].attachment.data = null
        internalResource.content[0].attachment.hash = Base64.encodeToString(HashUtil.sha1(String(rawAttachment).toByteArray()))

        runAttachmentDownloadFlow(
            serializedResource = SdkFhirParser.fromResource(internalResource)!!,
            rawAttachment = rawAttachment,
            tags = tags,
            attachmentId = ATTACHMENT_ID
        )

        // When
        val result = recordService.downloadRecord<Fhir3DocumentReference>(
            RECORD_ID,
            USER_ID
        ).blockingGet()

        // Then
        assertTrue(result is Record)
        assertTrue(result.resource.identifier!!.isNotEmpty())
        assertEquals(
            expected = flowHelper.buildMeta(CREATION_DATE, UPDATE_DATE),
            actual = result.meta
        )

        assertEquals(
            expected = SdkFhirParser.fromResource(resource),
            actual = SdkFhirParser.fromResource(result.resource)
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
            actual = result.resource.identifier?.get(1)?.value,
            expected = "d4l_f_p_t#${ATTACHMENT_ID}#${PREVIEW_ID}#${THUMBNAIL_ID}"
        )
    }
}
