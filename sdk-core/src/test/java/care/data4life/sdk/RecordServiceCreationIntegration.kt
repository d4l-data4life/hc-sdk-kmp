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
import care.data4life.crypto.KeyType
import care.data4life.fhir.stu3.model.Attachment
import care.data4life.fhir.stu3.model.DocumentReference
import care.data4life.sdk.attachment.AttachmentService
import care.data4life.sdk.attachment.FileService
import care.data4life.sdk.call.DataRecord
import care.data4life.sdk.call.Fhir4Record
import care.data4life.sdk.data.DataResource
import care.data4life.sdk.fhir.Fhir3Attachment
import care.data4life.sdk.fhir.Fhir4Attachment
import care.data4life.sdk.fhir.Fhir4Resource
import care.data4life.sdk.fhir.FhirService
import care.data4life.sdk.model.Meta
import care.data4life.sdk.model.ModelVersion
import care.data4life.sdk.model.Record
import care.data4life.sdk.network.model.EncryptedKey
import care.data4life.sdk.network.model.EncryptedRecord
import care.data4life.sdk.record.RecordContract
import care.data4life.sdk.tag.TagEncryptionService
import care.data4life.sdk.tag.TaggingService
import care.data4life.sdk.util.Base64
import care.data4life.sdk.util.HashUtil.sha1
import care.data4life.sdk.util.MimeType
import com.google.common.truth.Truth
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.reactivex.Single
import org.junit.Before
import org.junit.Test
import org.threeten.bp.LocalDate
import org.threeten.bp.LocalDateTime
import org.threeten.bp.format.DateTimeFormatter
import org.threeten.bp.format.DateTimeFormatterBuilder
import java.util.*

class RecordServiceCreationIntegration {
    private val commonKeyId = "commonKeyId"

    private lateinit var dataKey: GCKey
    private lateinit var attachmentKey: GCKey
    private lateinit var encryptionKey: GCKey
    private lateinit var commonKey: GCKey
    private lateinit var commonKey2: GCKey
    private lateinit var encryptedDataKey: EncryptedKey
    private lateinit var encryptedAttachmentKey: EncryptedKey

    private lateinit var recordService: RecordContract.Service
    private lateinit var apiService: ApiService
    private lateinit var cryptoService: CryptoService
    private lateinit var fileService: FileService
    private lateinit var imageResizer: ImageResizer
    private lateinit var errorHandler: D4LErrorHandler
    private lateinit var encryptedRecord: EncryptedRecord

    private lateinit var encryptedBody: String
    private lateinit var stringifiedResource: String

    @Before
    fun setUp() {
        apiService = mockk()
        cryptoService = mockk()
        fileService = mockk()
        imageResizer = mockk()
        errorHandler = mockk()

        mockkObject(MimeType)

        recordService = RecordService(
                RecordServiceTestBase.PARTNER_ID,
                RecordServiceTestBase.ALIAS,
                apiService,
                TagEncryptionService(
                      cryptoService
                ),
                TaggingService(CLIENT_ID),
                FhirService(
                        cryptoService
                ),
                AttachmentService(
                        fileService,
                        imageResizer
                ),
                cryptoService,
                errorHandler
        )

        dataKey = mockk()
        attachmentKey = mockk()
        encryptionKey = mockk()
        commonKey = mockk()
        commonKey2 = mockk()
        encryptedDataKey = mockk()
        encryptedAttachmentKey = mockk()
    }

    @Test
    fun `Given, createFhir3Record is called with the appropriate payload without Attachments, it creates a Record for Fhir3`() {
        val resource = buildDocumentReferenceFhir3()
        val tags = mapOf(
                "partner" to "partner=TEST",
                "client" to "client=TEST",
                "fhirversion" to "fhirversion=3.0.1",
                "resourcetype" to "resourcetype=documentreference"
        )

        stringifiedResource = "{\"content\":[{\"attachment\":{\"hash\":\"jwZ0G6YALQ4N8RGNHzJHIgX6j+I=\",\"id\":\"42\",\"size\":42}}],\"identifier\":[{\"assigner\":{\"reference\":\"partnerId\"},\"value\":\"d4l_f_p_t#42\"}],\"resourceType\":\"DocumentReference\"}"
        encryptedBody = "ZW5jcnlwdGVk"

        encryptedRecord = EncryptedRecord(
                commonKeyId,
                RECORD_ID,
                listOf("cGFydG5lcj1URVNU", "Y2xpZW50PVRFU1Q=", "ZmhpcnZlcnNpb249My4wLjE=", "cmVzb3VyY2V0eXBlPWRvY3VtZW50cmVmZXJlbmNl"),
                encryptedBody,
                CREATION_DATE,
                encryptedDataKey,
                encryptedAttachmentKey,
                ModelVersion.CURRENT
        ).also { it.updatedDate = UPDATE_DATE }

        val gcKeys = mutableListOf(
                dataKey,
                attachmentKey,
                dataKey,
                attachmentKey
        )

        // constrain check
        every { MimeType.recognizeMimeType(Base64.decode(ATTACHMENT_PAYLOAD)) } returns MimeType.PDF
        // get tags
        every { cryptoService.generateGCKey() } answers {
            Single.just(gcKeys.removeAt(0))
        }
        every {
            fileService.uploadFile(dataKey, USER_ID, Base64.decode(ATTACHMENT_PAYLOAD))
        } returns Single.just("23")
        every { imageResizer.isResizable(byteArrayOf(117, -85, 90)) } returns false

        // Encrypt Record
        // encrypt tags
        every { cryptoService.fetchTagEncryptionKey() } returns encryptionKey
        every {
            cryptoService.symEncrypt(encryptionKey, eq(
                    Base64.decode(Base64.encode(tags["partner"]!!.toByteArray()))
            ), IV)
        } returns tags["partner"]!!.toByteArray()
        every {
            cryptoService.symEncrypt(encryptionKey, eq(
                    Base64.decode(Base64.encode(tags["client"]!!.toByteArray()))
            ), IV)
        } returns tags["client"]!!.toByteArray()
        every {
            cryptoService.symEncrypt(encryptionKey, eq(
                    Base64.decode(Base64.encode(tags["fhirversion"]!!.toByteArray()))
            ), IV)
        } returns tags["fhirversion"]!!.toByteArray()
        every {
            cryptoService.symEncrypt(encryptionKey, eq(
                    Base64.decode(Base64.encode(tags["resourcetype"]!!.toByteArray()))
            ), IV)
        } returns tags["resourcetype"]!!.toByteArray()

        // encrypt Resource
        every { cryptoService.fetchCurrentCommonKey() } returns commonKey
        every { cryptoService.currentCommonKeyId } returns commonKeyId
        every { cryptoService.encryptSymmetricKey(commonKey, KeyType.DATA_KEY, dataKey) } returns Single.just(encryptedDataKey)
        every { cryptoService.encryptString(dataKey, stringifiedResource) } returns Single.just(encryptedBody)

        //encrypt Attachment
        every {
            cryptoService.encryptSymmetricKey(commonKey, KeyType.ATTACHMENT_KEY, attachmentKey)
        } returns Single.just(encryptedAttachmentKey)
        every {
            fileService.uploadFile(attachmentKey, USER_ID, Base64.decode(ATTACHMENT_PAYLOAD))
        } returns Single.just("42")

        every { apiService.createRecord(ALIAS, USER_ID, any()) } returns Single.just(encryptedRecord)

        // decrypt tags
        every {
            cryptoService.symDecrypt(encryptionKey, eq(
                    Base64.decode(Base64.encode(tags["partner"]!!.toByteArray()))
            ), IV)
        } returns tags["partner"]!!.toByteArray()
        every {
            cryptoService.symDecrypt(encryptionKey, eq(
                    Base64.decode(Base64.encode(tags["client"]!!.toByteArray()))
            ), IV)
        } returns tags["client"]!!.toByteArray()
        every {
            cryptoService.symDecrypt(encryptionKey, eq(
                    Base64.decode(Base64.encode(tags["fhirversion"]!!.toByteArray()))
            ), IV)
        } returns tags["fhirversion"]!!.toByteArray()
        every {
            cryptoService.symDecrypt(encryptionKey, eq(
                    Base64.decode(Base64.encode(tags["resourcetype"]!!.toByteArray()))
            ), IV)
        } returns tags["resourcetype"]!!.toByteArray()

        // decrypt record
        every { cryptoService.hasCommonKey(commonKeyId) } returns true
        every { cryptoService.getCommonKeyById(commonKeyId) } returns commonKey
        every { cryptoService.symDecryptSymmetricKey(commonKey, encryptedDataKey) } returns Single.just(dataKey)
        every { cryptoService.symDecryptSymmetricKey(commonKey, encryptedAttachmentKey) } returns Single.just(attachmentKey)
        every { cryptoService.decryptString(dataKey, encryptedBody) } returns Single.just(stringifiedResource)

        val result = recordService.createRecord(
                USER_ID,
                resource,
                listOf()
        ).blockingGet()

        Truth.assertThat(result).isInstanceOf(Record::class.java)
        Truth.assertThat(result.resource.identifier).isNotEmpty()
        Truth.assertThat(result.meta).isEqualTo(buildMeta(CREATION_DATE, UPDATE_DATE))
        Truth.assertThat(result.annotations).isEmpty()
        Truth.assertThat(result.resource).isEqualTo(resource)
    }

    @Test
    fun `Given, createFhir3Record is called with the appropriate payload with Attachments, it creates a Record for Fhir3`() {
        val resource = buildDocumentReferenceFhir3()
        val tags = mapOf(
                "partner" to "partner=TEST",
                "client" to "client=TEST",
                "fhirversion" to "fhirversion=3.0.1",
                "resourcetype" to "resourcetype=documentreference"
        )
        val annotations = mapOf(
                "wow" to "custom=wow",
                "it" to "custom=it",
                "works" to "custom=works"
        )

        stringifiedResource = "{\"content\":[{\"attachment\":{\"hash\":\"jwZ0G6YALQ4N8RGNHzJHIgX6j+I=\",\"id\":\"42\",\"size\":42}}],\"identifier\":[{\"assigner\":{\"reference\":\"partnerId\"},\"value\":\"d4l_f_p_t#42\"}],\"resourceType\":\"DocumentReference\"}"
        encryptedBody = "ZW5jcnlwdGVk"

        encryptedRecord = EncryptedRecord(
                commonKeyId,
                RECORD_ID,
                listOf(
                        "cGFydG5lcj1URVNU",
                        "Y2xpZW50PVRFU1Q=",
                        "ZmhpcnZlcnNpb249My4wLjE=",
                        "cmVzb3VyY2V0eXBlPWRvY3VtZW50cmVmZXJlbmNl",
                        "Y3VzdG9tPXdvdw==",
                        "Y3VzdG9tPWl0",
                        "Y3VzdG9tPXdvcmtz"
                ),
                encryptedBody,
                CREATION_DATE,
                encryptedDataKey,
                encryptedAttachmentKey,
                ModelVersion.CURRENT
        ).also { it.updatedDate = UPDATE_DATE }

        val gcKeys = mutableListOf(
                dataKey,
                attachmentKey,
                dataKey,
                attachmentKey
        )

        // constrain check
        every { MimeType.recognizeMimeType(Base64.decode(ATTACHMENT_PAYLOAD)) } returns MimeType.PDF
        // get tags
        every { cryptoService.generateGCKey() } answers {
            Single.just(gcKeys.removeAt(0))
        }
        every {
            fileService.uploadFile(dataKey, USER_ID, Base64.decode(ATTACHMENT_PAYLOAD))
        } returns Single.just("23")
        every { imageResizer.isResizable(byteArrayOf(117, -85, 90)) } returns false

        // Encrypt Record
        // encrypt tags
        every { cryptoService.fetchTagEncryptionKey() } returns encryptionKey
        every {
            cryptoService.symEncrypt(encryptionKey, eq(
                    Base64.decode(Base64.encode(tags["partner"]!!.toByteArray()))
            ), IV)
        } returns tags["partner"]!!.toByteArray()
        every {
            cryptoService.symEncrypt(encryptionKey, eq(
                    Base64.decode(Base64.encode(tags["client"]!!.toByteArray()))
            ), IV)
        } returns tags["client"]!!.toByteArray()
        every {
            cryptoService.symEncrypt(encryptionKey, eq(
                    Base64.decode(Base64.encode(tags["fhirversion"]!!.toByteArray()))
            ), IV)
        } returns tags["fhirversion"]!!.toByteArray()
        every {
            cryptoService.symEncrypt(encryptionKey, eq(
                    Base64.decode(Base64.encode(tags["resourcetype"]!!.toByteArray()))
            ), IV)
        } returns tags["resourcetype"]!!.toByteArray()

        // encrypt annotations
        every {
            cryptoService.symEncrypt(encryptionKey, eq(
                    Base64.decode(Base64.encode(annotations["wow"]!!.toByteArray()))
            ), IV)
        } returns annotations["wow"]!!.toByteArray()
        every {
            cryptoService.symEncrypt(encryptionKey, eq(
                    Base64.decode(Base64.encode(annotations["it"]!!.toByteArray()))
            ), IV)
        } returns annotations["it"]!!.toByteArray()
        every {
            cryptoService.symEncrypt(encryptionKey, eq(
                    Base64.decode(Base64.encode(annotations["works"]!!.toByteArray()))
            ), IV)
        } returns annotations["works"]!!.toByteArray()

        // encrypt Resource
        every { cryptoService.fetchCurrentCommonKey() } returns commonKey
        every { cryptoService.currentCommonKeyId } returns commonKeyId
        every { cryptoService.encryptSymmetricKey(commonKey, KeyType.DATA_KEY, dataKey) } returns Single.just(encryptedDataKey)
        every { cryptoService.encryptString(dataKey, stringifiedResource) } returns Single.just(encryptedBody)

        //encrypt Attachment
        every {
            cryptoService.encryptSymmetricKey(commonKey, KeyType.ATTACHMENT_KEY, attachmentKey)
        } returns Single.just(encryptedAttachmentKey)
        every {
            fileService.uploadFile(attachmentKey, USER_ID, Base64.decode(ATTACHMENT_PAYLOAD))
        } returns Single.just("42")

        every { apiService.createRecord(ALIAS, USER_ID, any()) } returns Single.just(encryptedRecord)

        // decrypt tags
        every {
            cryptoService.symDecrypt(encryptionKey, eq(
                    Base64.decode(Base64.encode(tags["partner"]!!.toByteArray()))
            ), IV)
        } returns tags["partner"]!!.toByteArray()
        every {
            cryptoService.symDecrypt(encryptionKey, eq(
                    Base64.decode(Base64.encode(tags["client"]!!.toByteArray()))
            ), IV)
        } returns tags["client"]!!.toByteArray()
        every {
            cryptoService.symDecrypt(encryptionKey, eq(
                    Base64.decode(Base64.encode(tags["fhirversion"]!!.toByteArray()))
            ), IV)
        } returns tags["fhirversion"]!!.toByteArray()
        every {
            cryptoService.symDecrypt(encryptionKey, eq(
                    Base64.decode(Base64.encode(tags["resourcetype"]!!.toByteArray()))
            ), IV)
        } returns tags["resourcetype"]!!.toByteArray()

        //decrypt annotations
        every {
            cryptoService.symDecrypt(encryptionKey, eq(
                    Base64.decode(Base64.encode(annotations["wow"]!!.toByteArray()))
            ), IV)
        } returns annotations["wow"]!!.toByteArray()
        every {
            cryptoService.symDecrypt(encryptionKey, eq(
                    Base64.decode(Base64.encode(annotations["it"]!!.toByteArray()))
            ), IV)
        } returns annotations["it"]!!.toByteArray()
        every {
            cryptoService.symDecrypt(encryptionKey, eq(
                    Base64.decode(Base64.encode(annotations["works"]!!.toByteArray()))
            ), IV)
        } returns annotations["works"]!!.toByteArray()

        // decrypt record
        every { cryptoService.hasCommonKey(commonKeyId) } returns true
        every { cryptoService.getCommonKeyById(commonKeyId) } returns commonKey
        every { cryptoService.symDecryptSymmetricKey(commonKey, encryptedDataKey) } returns Single.just(dataKey)
        every { cryptoService.symDecryptSymmetricKey(commonKey, encryptedAttachmentKey) } returns Single.just(attachmentKey)
        every { cryptoService.decryptString(dataKey, encryptedBody) } returns Single.just(stringifiedResource)

        val result = recordService.createRecord(
                USER_ID,
                resource,
                listOf("wow", "it", "works")
        ).blockingGet()

        Truth.assertThat(result).isInstanceOf(Record::class.java)
        Truth.assertThat(result.resource.identifier).isNotEmpty()
        Truth.assertThat(result.meta).isEqualTo(buildMeta(CREATION_DATE, UPDATE_DATE))
        Truth.assertThat(result.annotations).isEqualTo(listOf("wow", "it", "works"))
        Truth.assertThat(result.resource).isEqualTo(resource)
    }

    @Test
    fun `Given, createFhir4Record is called with the appropriate payload without annotations, it creates a Record for Fhir4`() {
        val resource: Fhir4Resource = buildDocumentReferenceFhir4()
        val tags = mapOf(
                "partner" to "partner=TEST",
                "client" to "client=TEST",
                "fhirversion" to "fhirversion=4.0.1",
                "resourcetype" to "resourcetype=documentreference"
        )

        stringifiedResource = "{\"content\":[{\"attachment\":{\"hash\":\"jwZ0G6YALQ4N8RGNHzJHIgX6j+I=\",\"id\":\"42\",\"size\":42}}],\"identifier\":[{\"assigner\":{\"reference\":\"partnerId\"},\"value\":\"d4l_f_p_t#42\"}],\"resourceType\":\"DocumentReference\"}"
        encryptedBody = "ZW5jcnlwdGVk"

        encryptedRecord = EncryptedRecord(
                commonKeyId,
                RECORD_ID,
                listOf("cGFydG5lcj1URVNU", "Y2xpZW50PVRFU1Q=", "ZmhpcnZlcnNpb249NC4wLjE=", "cmVzb3VyY2V0eXBlPWRvY3VtZW50cmVmZXJlbmNl"),
                encryptedBody,
                CREATION_DATE,
                encryptedDataKey,
                encryptedAttachmentKey,
                ModelVersion.CURRENT
        ).also { it.updatedDate = UPDATE_DATE }

        val gcKeys = mutableListOf(
                dataKey,
                attachmentKey,
                dataKey,
                attachmentKey
        )

        // constrain check
        every { MimeType.recognizeMimeType(Base64.decode(ATTACHMENT_PAYLOAD)) } returns MimeType.PDF
        // get tags
        every { cryptoService.generateGCKey() } answers {
            Single.just(gcKeys.removeAt(0))
        }
        every {
            fileService.uploadFile(dataKey, USER_ID, Base64.decode(ATTACHMENT_PAYLOAD))
        } returns Single.just("23")
        every { imageResizer.isResizable(byteArrayOf(117, -85, 90)) } returns false

        // encrypt Record
        // encrypt tags
        every { cryptoService.fetchTagEncryptionKey() } returns encryptionKey
        every {
            cryptoService.symEncrypt(encryptionKey, eq(
                    Base64.decode(Base64.encode(tags["partner"]!!.toByteArray()))
            ), IV)
        } returns tags["partner"]!!.toByteArray()
        every {
            cryptoService.symEncrypt(encryptionKey, eq(
                    Base64.decode(Base64.encode(tags["client"]!!.toByteArray()))
            ), IV)
        } returns tags["client"]!!.toByteArray()
        every {
            cryptoService.symEncrypt(encryptionKey, eq(
                    Base64.decode(Base64.encode(tags["fhirversion"]!!.toByteArray()))
            ), IV)
        } returns tags["fhirversion"]!!.toByteArray()
        every {
            cryptoService.symEncrypt(encryptionKey, eq(
                    Base64.decode(Base64.encode(tags["resourcetype"]!!.toByteArray()))
            ), IV)
        } returns tags["resourcetype"]!!.toByteArray()

        // encrypt Resource
        every { cryptoService.fetchCurrentCommonKey() } returns commonKey
        every { cryptoService.currentCommonKeyId } returns commonKeyId
        every { cryptoService.encryptSymmetricKey(commonKey, KeyType.DATA_KEY, dataKey) } returns Single.just(encryptedDataKey)
        every { cryptoService.encryptString(dataKey, stringifiedResource) } returns Single.just(encryptedBody)

        //encrypt Attachment
        every {
            cryptoService.encryptSymmetricKey(commonKey, KeyType.ATTACHMENT_KEY, attachmentKey)
        } returns Single.just(encryptedAttachmentKey)
        every {
            fileService.uploadFile(attachmentKey, USER_ID, Base64.decode(ATTACHMENT_PAYLOAD))
        } returns Single.just("42")

        every { apiService.createRecord(ALIAS, USER_ID, any()) } returns Single.just(encryptedRecord)

        // decrypt tags
        every {
            cryptoService.symDecrypt(encryptionKey, eq(
                    Base64.decode(Base64.encode(tags["partner"]!!.toByteArray()))
            ), IV)
        } returns tags["partner"]!!.toByteArray()
        every {
            cryptoService.symDecrypt(encryptionKey, eq(
                    Base64.decode(Base64.encode(tags["client"]!!.toByteArray()))
            ), IV)
        } returns tags["client"]!!.toByteArray()
        every {
            cryptoService.symDecrypt(encryptionKey, eq(
                    Base64.decode(Base64.encode(tags["fhirversion"]!!.toByteArray()))
            ), IV)
        } returns tags["fhirversion"]!!.toByteArray()
        every {
            cryptoService.symDecrypt(encryptionKey, eq(
                    Base64.decode(Base64.encode(tags["resourcetype"]!!.toByteArray()))
            ), IV)
        } returns tags["resourcetype"]!!.toByteArray()

        // decrypt record
        every { cryptoService.hasCommonKey(commonKeyId) } returns true
        every { cryptoService.getCommonKeyById(commonKeyId) } returns commonKey
        every { cryptoService.symDecryptSymmetricKey(commonKey, encryptedDataKey) } returns Single.just(dataKey)
        every { cryptoService.symDecryptSymmetricKey(commonKey, encryptedAttachmentKey) } returns Single.just(attachmentKey)
        every { cryptoService.decryptString(dataKey, encryptedBody) } returns Single.just(stringifiedResource)

        val result = recordService.createRecord(
                USER_ID,
                resource,
                listOf()
        ).blockingGet()

        Truth.assertThat(result).isInstanceOf(Fhir4Record::class.java)
        Truth.assertThat(result.identifier).isEqualTo(RECORD_ID)
        Truth.assertThat(result.meta).isEqualTo(buildMeta(CREATION_DATE, UPDATE_DATE))
        Truth.assertThat(result.annotations).isEmpty()
        Truth.assertThat(result.resource).isEqualTo(resource)
    }

    @Test
    fun `Given, createFhir4Record is called with the appropriate payload with annotations, it creates a Record for Fhir4`() {
        val resource: Fhir4Resource = buildDocumentReferenceFhir4()
        val tags = mapOf(
                "partner" to "partner=TEST",
                "client" to "client=TEST",
                "fhirversion" to "fhirversion=4.0.1",
                "resourcetype" to "resourcetype=documentreference"
        )
        val annotations = mapOf(
                "wow" to "custom=wow",
                "it" to "custom=it",
                "works" to "custom=works"
        )

        stringifiedResource = "{\"content\":[{\"attachment\":{\"hash\":\"jwZ0G6YALQ4N8RGNHzJHIgX6j+I=\",\"id\":\"42\",\"size\":42}}],\"identifier\":[{\"assigner\":{\"reference\":\"partnerId\"},\"value\":\"d4l_f_p_t#42\"}],\"resourceType\":\"DocumentReference\"}"
        encryptedBody = "ZW5jcnlwdGVk"

        encryptedRecord = EncryptedRecord(
                commonKeyId,
                RECORD_ID,
                listOf(
                        "cGFydG5lcj1URVNU",
                        "Y2xpZW50PVRFU1Q=",
                        "ZmhpcnZlcnNpb249NC4wLjE=",
                        "cmVzb3VyY2V0eXBlPWRvY3VtZW50cmVmZXJlbmNl",
                        "Y3VzdG9tPXdvdw==",
                        "Y3VzdG9tPWl0",
                        "Y3VzdG9tPXdvcmtz"
                ),
                encryptedBody,
                CREATION_DATE,
                encryptedDataKey,
                encryptedAttachmentKey,
                ModelVersion.CURRENT
        ).also { it.updatedDate = UPDATE_DATE }

        val gcKeys = mutableListOf(
                dataKey,
                attachmentKey,
                dataKey,
                attachmentKey
        )

        // constrain check
        every { MimeType.recognizeMimeType(Base64.decode(ATTACHMENT_PAYLOAD)) } returns MimeType.PDF
        // get tags
        every { cryptoService.generateGCKey() } answers {
            Single.just(gcKeys.removeAt(0))
        }
        every {
            fileService.uploadFile(dataKey, USER_ID, Base64.decode(ATTACHMENT_PAYLOAD))
        } returns Single.just("23")
        every { imageResizer.isResizable(byteArrayOf(117, -85, 90)) } returns false

        // encrypt Record
        // encrypt tags
        every { cryptoService.fetchTagEncryptionKey() } returns encryptionKey
        every {
            cryptoService.symEncrypt(encryptionKey, eq(
                    Base64.decode(Base64.encode(tags["partner"]!!.toByteArray()))
            ), IV)
        } returns tags["partner"]!!.toByteArray()
        every {
            cryptoService.symEncrypt(encryptionKey, eq(
                    Base64.decode(Base64.encode(tags["client"]!!.toByteArray()))
            ), IV)
        } returns tags["client"]!!.toByteArray()
        every {
            cryptoService.symEncrypt(encryptionKey, eq(
                    Base64.decode(Base64.encode(tags["fhirversion"]!!.toByteArray()))
            ), IV)
        } returns tags["fhirversion"]!!.toByteArray()
        every {
            cryptoService.symEncrypt(encryptionKey, eq(
                    Base64.decode(Base64.encode(tags["resourcetype"]!!.toByteArray()))
            ), IV)
        } returns tags["resourcetype"]!!.toByteArray()

        // encrypt annotations
        every {
            cryptoService.symEncrypt(encryptionKey, eq(
                    Base64.decode(Base64.encode(annotations["wow"]!!.toByteArray()))
            ), IV)
        } returns annotations["wow"]!!.toByteArray()
        every {
            cryptoService.symEncrypt(encryptionKey, eq(
                    Base64.decode(Base64.encode(annotations["it"]!!.toByteArray()))
            ), IV)
        } returns annotations["it"]!!.toByteArray()
        every {
            cryptoService.symEncrypt(encryptionKey, eq(
                    Base64.decode(Base64.encode(annotations["works"]!!.toByteArray()))
            ), IV)
        } returns annotations["works"]!!.toByteArray()

        // encrypt Resource
        every { cryptoService.fetchCurrentCommonKey() } returns commonKey
        every { cryptoService.currentCommonKeyId } returns commonKeyId
        every { cryptoService.encryptSymmetricKey(commonKey, KeyType.DATA_KEY, dataKey) } returns Single.just(encryptedDataKey)
        every { cryptoService.encryptString(dataKey, stringifiedResource) } returns Single.just(encryptedBody)

        //encrypt Attachment
        every {
            cryptoService.encryptSymmetricKey(commonKey, KeyType.ATTACHMENT_KEY, attachmentKey)
        } returns Single.just(encryptedAttachmentKey)
        every {
            fileService.uploadFile(attachmentKey, USER_ID, Base64.decode(ATTACHMENT_PAYLOAD))
        } returns Single.just("42")

        every { apiService.createRecord(ALIAS, USER_ID, any()) } returns Single.just(encryptedRecord)

        // decrypt tags
        every {
            cryptoService.symDecrypt(encryptionKey, eq(
                    Base64.decode(Base64.encode(tags["partner"]!!.toByteArray()))
            ), IV)
        } returns tags["partner"]!!.toByteArray()
        every {
            cryptoService.symDecrypt(encryptionKey, eq(
                    Base64.decode(Base64.encode(tags["client"]!!.toByteArray()))
            ), IV)
        } returns tags["client"]!!.toByteArray()
        every {
            cryptoService.symDecrypt(encryptionKey, eq(
                    Base64.decode(Base64.encode(tags["fhirversion"]!!.toByteArray()))
            ), IV)
        } returns tags["fhirversion"]!!.toByteArray()
        every {
            cryptoService.symDecrypt(encryptionKey, eq(
                    Base64.decode(Base64.encode(tags["resourcetype"]!!.toByteArray()))
            ), IV)
        } returns tags["resourcetype"]!!.toByteArray()

        //decrypt annotations
        every {
            cryptoService.symDecrypt(encryptionKey, eq(
                    Base64.decode(Base64.encode(annotations["wow"]!!.toByteArray()))
            ), IV)
        } returns annotations["wow"]!!.toByteArray()
        every {
            cryptoService.symDecrypt(encryptionKey, eq(
                    Base64.decode(Base64.encode(annotations["it"]!!.toByteArray()))
            ), IV)
        } returns annotations["it"]!!.toByteArray()
        every {
            cryptoService.symDecrypt(encryptionKey, eq(
                    Base64.decode(Base64.encode(annotations["works"]!!.toByteArray()))
            ), IV)
        } returns annotations["works"]!!.toByteArray()

        // decrypt record
        every { cryptoService.hasCommonKey(commonKeyId) } returns true
        every { cryptoService.getCommonKeyById(commonKeyId) } returns commonKey
        every { cryptoService.symDecryptSymmetricKey(commonKey, encryptedDataKey) } returns Single.just(dataKey)
        every { cryptoService.symDecryptSymmetricKey(commonKey, encryptedAttachmentKey) } returns Single.just(attachmentKey)
        every { cryptoService.decryptString(dataKey, encryptedBody) } returns Single.just(stringifiedResource)

        val result = recordService.createRecord(
                USER_ID,
                resource,
                listOf("wow", "it", "works")
        ).blockingGet()

        Truth.assertThat(result).isInstanceOf(Fhir4Record::class.java)
        Truth.assertThat(result.identifier).isEqualTo(RECORD_ID)
        Truth.assertThat(result.meta).isEqualTo(buildMeta(CREATION_DATE, UPDATE_DATE))
        Truth.assertThat(result.annotations).isEqualTo(listOf("wow", "it", "works"))
        Truth.assertThat(result.resource).isEqualTo(resource)
    }

    @Test
    fun `Given, createDataRecord is called with the appropriate payload without Attachments, it creates a Record for DataResource`() {
        val resource = DataResource("I am test".toByteArray())
        val tags = mapOf(
                "partner" to "partner=TEST",
                "client" to "client=TEST",
                "flag" to "flag=appdata"
        )

        val encryptedResource = "The test string encrypted".toByteArray()

        encryptedBody = "VGhlIHRlc3Qgc3RyaW5nIGVuY3J5cHRlZA=="
        encryptedRecord = EncryptedRecord(
                commonKeyId,
                RECORD_ID,
                listOf("cGFydG5lcj1URVNU", "Y2xpZW50PVRFU1Q=", "ZmxhZz1hcHBkYXRh"),
                encryptedBody,
                CREATION_DATE,
                encryptedDataKey,
                null,
                ModelVersion.CURRENT
        ).also { it.updatedDate = UPDATE_DATE }

        val gcKeys = mutableListOf(
                dataKey,
                dataKey
        )

        // encrypt Record
        // get tags
        every { cryptoService.generateGCKey() } answers {
            Single.just(gcKeys.removeAt(0))
        }

        // encrypt tags
        every { cryptoService.fetchTagEncryptionKey() } returns encryptionKey
        every {
            cryptoService.symEncrypt(encryptionKey, eq(
                    Base64.decode(Base64.encode(tags["partner"]!!.toByteArray()))
            ), IV)
        } returns tags["partner"]!!.toByteArray()
        every {
            cryptoService.symEncrypt(encryptionKey, eq(
                    Base64.decode(Base64.encode(tags["client"]!!.toByteArray()))
            ), IV)
        } returns tags["client"]!!.toByteArray()
        every {
            cryptoService.symEncrypt(encryptionKey, eq(
                    Base64.decode(Base64.encode(tags["flag"]!!.toByteArray()))
            ), IV)
        } returns tags["flag"]!!.toByteArray()

        // encrypt Resource
        every { cryptoService.fetchCurrentCommonKey() } returns commonKey
        every { cryptoService.currentCommonKeyId } returns commonKeyId
        every { cryptoService.encryptSymmetricKey(commonKey, KeyType.DATA_KEY, dataKey) } returns Single.just(encryptedDataKey)
        every { cryptoService.encrypt(dataKey, resource.value) } returns Single.just(encryptedResource)

        every { apiService.createRecord(ALIAS, USER_ID, any()) } returns Single.just(encryptedRecord)

        // decrypt tags
        every {
            cryptoService.symDecrypt(encryptionKey, eq(
                    Base64.decode(Base64.encode(tags["partner"]!!.toByteArray()))
            ), IV)
        } returns tags["partner"]!!.toByteArray()
        every {
            cryptoService.symDecrypt(encryptionKey, eq(
                    Base64.decode(Base64.encode(tags["client"]!!.toByteArray()))
            ), IV)
        } returns tags["client"]!!.toByteArray()
        every {
            cryptoService.symDecrypt(encryptionKey, eq(
                    Base64.decode(Base64.encode(tags["flag"]!!.toByteArray()))
            ), IV)
        } returns tags["flag"]!!.toByteArray()

        // decrypt record
        every { cryptoService.hasCommonKey(commonKeyId) } returns true
        every { cryptoService.getCommonKeyById(commonKeyId) } returns commonKey
        every { cryptoService.symDecryptSymmetricKey(commonKey, encryptedDataKey) } returns Single.just(dataKey)
        every { cryptoService.decrypt(dataKey, encryptedResource) } returns Single.just(resource.value)

        val result = recordService.createRecord(
                USER_ID,
                resource,
                listOf()
        ).blockingGet()

        Truth.assertThat(result).isInstanceOf(DataRecord::class.java)
        Truth.assertThat(result.identifier).isEqualTo(RECORD_ID)
        Truth.assertThat(result.meta).isEqualTo(buildMeta(CREATION_DATE, UPDATE_DATE))
        Truth.assertThat(result.annotations).isEmpty()
        Truth.assertThat(result.resource).isEqualTo(resource)
    }

    @Test
    fun `Given, createDataRecord is called with the appropriate payload with Attachments, it creates a Record for DataResource`() {
        val resource = DataResource("I am test".toByteArray())
        val tags = mapOf(
                "partner" to "partner=TEST",
                "client" to "client=TEST",
                "flag" to "flag=appdata"
        )
        val annotations = mapOf(
                "wow" to "custom=wow",
                "it" to "custom=it",
                "works" to "custom=works"
        )

        val encryptedResource = "The test string encrypted".toByteArray()

        encryptedBody = "VGhlIHRlc3Qgc3RyaW5nIGVuY3J5cHRlZA=="
        encryptedRecord = EncryptedRecord(
                commonKeyId,
                RECORD_ID,
                listOf(
                        "cGFydG5lcj1URVNU",
                        "Y2xpZW50PVRFU1Q=",
                        "ZmxhZz1hcHBkYXRh",
                        "Y3VzdG9tPXdvdw==",
                        "Y3VzdG9tPWl0",
                        "Y3VzdG9tPXdvcmtz"
                ),
                encryptedBody,
                CREATION_DATE,
                encryptedDataKey,
                null,
                ModelVersion.CURRENT
        ).also { it.updatedDate = UPDATE_DATE }

        val gcKeys = mutableListOf(
                dataKey,
                dataKey
        )

        // encrypt Record
        // get tags
        every { cryptoService.generateGCKey() } answers {
            Single.just(gcKeys.removeAt(0))
        }

        // encrypt tags
        every { cryptoService.fetchTagEncryptionKey() } returns encryptionKey
        every {
            cryptoService.symEncrypt(encryptionKey, eq(
                    Base64.decode(Base64.encode(tags["partner"]!!.toByteArray()))
            ), IV)
        } returns tags["partner"]!!.toByteArray()
        every {
            cryptoService.symEncrypt(encryptionKey, eq(
                    Base64.decode(Base64.encode(tags["client"]!!.toByteArray()))
            ), IV)
        } returns tags["client"]!!.toByteArray()
        every {
            cryptoService.symEncrypt(encryptionKey, eq(
                    Base64.decode(Base64.encode(tags["flag"]!!.toByteArray()))
            ), IV)
        } returns tags["flag"]!!.toByteArray()

        // encrypt annotations
        every {
            cryptoService.symEncrypt(encryptionKey, eq(
                    Base64.decode(Base64.encode(annotations["wow"]!!.toByteArray()))
            ), IV)
        } returns annotations["wow"]!!.toByteArray()
        every {
            cryptoService.symEncrypt(encryptionKey, eq(
                    Base64.decode(Base64.encode(annotations["it"]!!.toByteArray()))
            ), IV)
        } returns annotations["it"]!!.toByteArray()
        every {
            cryptoService.symEncrypt(encryptionKey, eq(
                    Base64.decode(Base64.encode(annotations["works"]!!.toByteArray()))
            ), IV)
        } returns annotations["works"]!!.toByteArray()

        // encrypt Resource
        every { cryptoService.fetchCurrentCommonKey() } returns commonKey
        every { cryptoService.currentCommonKeyId } returns commonKeyId
        every { cryptoService.encryptSymmetricKey(commonKey, KeyType.DATA_KEY, dataKey) } returns Single.just(encryptedDataKey)
        every { cryptoService.encrypt(dataKey, resource.value) } returns Single.just(encryptedResource)

        every { apiService.createRecord(ALIAS, USER_ID, any()) } returns Single.just(encryptedRecord)

        // decrypt tags
        every {
            cryptoService.symDecrypt(encryptionKey, eq(
                    Base64.decode(Base64.encode(tags["partner"]!!.toByteArray()))
            ), IV)
        } returns tags["partner"]!!.toByteArray()
        every {
            cryptoService.symDecrypt(encryptionKey, eq(
                    Base64.decode(Base64.encode(tags["client"]!!.toByteArray()))
            ), IV)
        } returns tags["client"]!!.toByteArray()
        every {
            cryptoService.symDecrypt(encryptionKey, eq(
                    Base64.decode(Base64.encode(tags["flag"]!!.toByteArray()))
            ), IV)
        } returns tags["flag"]!!.toByteArray()

        //decrypt annotations
        every {
            cryptoService.symDecrypt(encryptionKey, eq(
                    Base64.decode(Base64.encode(annotations["wow"]!!.toByteArray()))
            ), IV)
        } returns annotations["wow"]!!.toByteArray()
        every {
            cryptoService.symDecrypt(encryptionKey, eq(
                    Base64.decode(Base64.encode(annotations["it"]!!.toByteArray()))
            ), IV)
        } returns annotations["it"]!!.toByteArray()
        every {
            cryptoService.symDecrypt(encryptionKey, eq(
                    Base64.decode(Base64.encode(annotations["works"]!!.toByteArray()))
            ), IV)
        } returns annotations["works"]!!.toByteArray()

        // decrypt record
        every { cryptoService.hasCommonKey(commonKeyId) } returns true
        every { cryptoService.getCommonKeyById(commonKeyId) } returns commonKey
        every { cryptoService.symDecryptSymmetricKey(commonKey, encryptedDataKey) } returns Single.just(dataKey)
        every { cryptoService.decrypt(dataKey, encryptedResource) } returns Single.just(resource.value)

        val result = recordService.createRecord(
                USER_ID,
                resource,
                listOf("wow", "it", "works")
        ).blockingGet()

        Truth.assertThat(result).isInstanceOf(DataRecord::class.java)
        Truth.assertThat(result.identifier).isEqualTo(RECORD_ID)
        Truth.assertThat(result.meta).isEqualTo(buildMeta(CREATION_DATE, UPDATE_DATE))
        Truth.assertThat(result.annotations).isEqualTo(listOf("wow", "it", "works"))
        Truth.assertThat(result.resource).isEqualTo(resource)
    }

    companion object {
        private const val CLIENT_ID = "TEST"
        private const val USER_ID = "ME"
        private const val ALIAS = "alias"
        private const val RECORD_ID = "chaos"
        private const val CREATION_DATE = "2020-12-12"
        private const val UPDATE_DATE = "2020-12-13T17:21:08.234123"
        private const val ATTACHMENT_PAYLOAD = "data"
        private val IV = ByteArray(16)
        private const val DATE_FORMAT = "yyyy-MM-dd"
        private const val DATE_TIME_FORMAT = "yyyy-MM-dd'T'HH:mm:ss[.SSS]"
        private val DATE_FORMATTER = DateTimeFormatter.ofPattern(DATE_FORMAT, Locale.US)
        private val DATE_TIME_FORMATTER = DateTimeFormatterBuilder()
                .parseLenient()
                .appendPattern(DATE_TIME_FORMAT)
                .toFormatter(Locale.US)

        private fun buildFhir3Attachment(): Fhir3Attachment {
            val attachment = Fhir3Attachment()
            attachment.id = null
            attachment.data = ATTACHMENT_PAYLOAD
            attachment.size = 42
            attachment.hash = Base64.encodeToString(sha1(Base64.decode(ATTACHMENT_PAYLOAD)))
            return attachment
        }

       private fun buildDocumentReferenceFhir3(): DocumentReference {
            val content = buildDocRefContentFhir3(buildFhir3Attachment())
            val contents: MutableList<DocumentReference.DocumentReferenceContent> = ArrayList()
            contents.add(content)
            return DocumentReference(
                    null,
                    null,
                    null,
                    contents
            )
        }

        private fun buildDocRefContentFhir3(attachment: Attachment): DocumentReference.DocumentReferenceContent {
            return DocumentReference.DocumentReferenceContent(attachment)
        }

        private fun buildDocumentReferenceFhir3(data: ByteArray?): DocumentReference {
            val doc = buildDocumentReferenceFhir3()
            doc.content[0].attachment.data = Base64.encodeToString(data!!)
            return doc
        }

        private fun buildFhir4Attachment(): Fhir4Attachment {
            val attachment = Fhir4Attachment()
            attachment.id = null
            attachment.data = ATTACHMENT_PAYLOAD
            attachment.size = 42
            attachment.hash = Base64.encodeToString(sha1(Base64.decode(ATTACHMENT_PAYLOAD)))
            return attachment
        }

        private fun buildDocRefContentFhir4(attachment: Fhir4Attachment): care.data4life.fhir.r4.model.DocumentReference.DocumentReferenceContent {
            return care.data4life.fhir.r4.model.DocumentReference.DocumentReferenceContent(attachment)
        }

        private fun buildDocumentReferenceFhir4(): care.data4life.fhir.r4.model.DocumentReference {
            val content = buildDocRefContentFhir4(buildFhir4Attachment())
            val contents: MutableList<care.data4life.fhir.r4.model.DocumentReference.DocumentReferenceContent> = ArrayList()
            contents.add(content)

            return care.data4life.fhir.r4.model.DocumentReference(
                    null,
                    contents
            )
        }

        private fun buildMeta(
                customCreationDate: String,
                updatedDate: String
        ): Meta = Meta(
                LocalDate.parse(customCreationDate, DATE_FORMATTER),
                LocalDateTime.parse(updatedDate, DATE_TIME_FORMATTER)
        )
    }
}
