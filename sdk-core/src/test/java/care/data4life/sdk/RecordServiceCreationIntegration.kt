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
import care.data4life.sdk.attachment.AttachmentService
import care.data4life.sdk.call.DataRecord
import care.data4life.sdk.call.Fhir4Record
import care.data4life.sdk.data.DataResource
import care.data4life.sdk.fhir.Fhir4Resource
import care.data4life.sdk.fhir.FhirService
import care.data4life.sdk.model.ModelVersion
import care.data4life.sdk.model.Record
import care.data4life.sdk.network.model.EncryptedRecord
import care.data4life.sdk.tag.TagEncryptionService
import care.data4life.sdk.tag.TaggingService
import care.data4life.sdk.util.Base64
import care.data4life.sdk.util.MimeType
import com.google.common.truth.Truth
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import io.reactivex.Single
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

class RecordServiceCreationIntegration : RecordServiceIntegrationBase() {
    @Before
    fun setUp() {
        apiService = mockk()
        cryptoService = mockk()
        fileService = mockk()
        imageResizer = mockk()
        errorHandler = mockk()

        mockkObject(MimeType)

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

        dataKey = mockk()
        attachmentKey = mockk()
        tagEncryptionKey = mockk()
        commonKey = mockk()
        encryptedDataKey = mockk()
        encryptedAttachmentKey = mockk()
    }

    @After
    fun tearDown() {
        unmockkObject(MimeType)
    }

    private fun runFhirFlow(
            tags: Map<String, String>,
            annotations: Map<String, String> = mapOf(),
            gcKeys: MutableList<GCKey>
    ) {
        // constrain check
        every { MimeType.recognizeMimeType(Base64.decode(ATTACHMENT_PAYLOAD)) } returns MimeType.PDF
        // get tags
        every { cryptoService.generateGCKey() } answers {
            Single.just(gcKeys.removeAt(0))
        }
        every {
            fileService.uploadFile(attachmentKey, USER_ID, Base64.decode(ATTACHMENT_PAYLOAD))
        } returns Single.just(ATTACHMENT_ID)
        every { imageResizer.isResizable(byteArrayOf(117, -85, 90)) } returns false

        // Encrypt Record
        // encrypt tags
        every { cryptoService.fetchTagEncryptionKey() } returns tagEncryptionKey
        every {
            cryptoService.symEncrypt(tagEncryptionKey, eq(
                    tags["partner"]!!.toByteArray()
            ), IV)
        } returns tags["partner"]!!.toByteArray()
        every {
            cryptoService.symEncrypt(tagEncryptionKey, eq(
                    tags["client"]!!.toByteArray()
            ), IV)
        } returns tags["client"]!!.toByteArray()
        every {
            cryptoService.symEncrypt(tagEncryptionKey, eq(
                    tags["fhirversion"]!!.toByteArray()
            ), IV)
        } returns tags["fhirversion"]!!.toByteArray()
        every {
            cryptoService.symEncrypt(tagEncryptionKey, eq(
                    tags["resourcetype"]!!.toByteArray()
            ), IV)
        } returns tags["resourcetype"]!!.toByteArray()

        // encrypt annotations
        if (annotations.isNotEmpty()) {
            every {
                cryptoService.symEncrypt(tagEncryptionKey, eq(
                        annotations["wow"]!!.toByteArray()
                ), IV)
            } returns annotations["wow"]!!.toByteArray()
            every {
                cryptoService.symEncrypt(tagEncryptionKey, eq(
                        annotations["it"]!!.toByteArray()
                ), IV)
            } returns annotations["it"]!!.toByteArray()
            every {
                cryptoService.symEncrypt(tagEncryptionKey, eq(
                        annotations["works"]!!.toByteArray()
                ), IV)
            } returns annotations["works"]!!.toByteArray()
        }

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
        } returns Single.just(RECORD_ID)

        every { apiService.createRecord(ALIAS, USER_ID, any()) } returns Single.just(encryptedRecord)

        // decrypt tags
        every {
            cryptoService.symDecrypt(tagEncryptionKey, eq(
                    tags["partner"]!!.toByteArray()
            ), IV)
        } returns tags["partner"]!!.toByteArray()
        every {
            cryptoService.symDecrypt(tagEncryptionKey, eq(
                    tags["client"]!!.toByteArray()
            ), IV)
        } returns tags["client"]!!.toByteArray()
        every {
            cryptoService.symDecrypt(tagEncryptionKey, eq(
                    tags["fhirversion"]!!.toByteArray()
            ), IV)
        } returns tags["fhirversion"]!!.toByteArray()
        every {
            cryptoService.symDecrypt(tagEncryptionKey, eq(
                    tags["resourcetype"]!!.toByteArray()
            ), IV)
        } returns tags["resourcetype"]!!.toByteArray()

        //decrypt annotations
        if (annotations.isNotEmpty()) {
            every {
                cryptoService.symDecrypt(tagEncryptionKey, eq(
                        annotations["wow"]!!.toByteArray()
                ), IV)
            } returns annotations["wow"]!!.toByteArray()
            every {
                cryptoService.symDecrypt(tagEncryptionKey, eq(
                        annotations["it"]!!.toByteArray()
                ), IV)
            } returns annotations["it"]!!.toByteArray()
            every {
                cryptoService.symDecrypt(tagEncryptionKey, eq(
                        annotations["works"]!!.toByteArray()
                ), IV)
            } returns annotations["works"]!!.toByteArray()
        }

        // decrypt record
        every { cryptoService.hasCommonKey(commonKeyId) } returns true
        every { cryptoService.getCommonKeyById(commonKeyId) } returns commonKey
        every { cryptoService.symDecryptSymmetricKey(commonKey, encryptedAttachmentKey) } returns Single.just(attachmentKey)
        every { cryptoService.symDecryptSymmetricKey(commonKey, encryptedDataKey) } returns Single.just(dataKey)
        every {
            cryptoService.decryptString(dataKey, encryptedBody)
        } returns Single.just(stringifiedResource)
    }

    private fun runDatalow(
            resource: DataResource,
            encryptedResource: ByteArray,
            tags: Map<String, String>,
            annotations: Map<String, String> = mapOf(),
            gcKeys: MutableList<GCKey>
    ) {
        // encrypt Record
        // get tags
        every { cryptoService.generateGCKey() } answers {
            Single.just(gcKeys.removeAt(0))
        }

        // encrypt tags
        every { cryptoService.fetchTagEncryptionKey() } returns tagEncryptionKey
        every {
            cryptoService.symEncrypt(tagEncryptionKey, eq(
                    tags["partner"]!!.toByteArray()
            ), IV)
        } returns tags["partner"]!!.toByteArray()
        every {
            cryptoService.symEncrypt(tagEncryptionKey, eq(
                    tags["client"]!!.toByteArray()
            ), IV)
        } returns tags["client"]!!.toByteArray()
        every {
            cryptoService.symEncrypt(tagEncryptionKey, eq(
                    tags["flag"]!!.toByteArray()
            ), IV)
        } returns tags["flag"]!!.toByteArray()

        // encrypt annotations
        if (annotations.isNotEmpty()) {
            every {
                cryptoService.symEncrypt(tagEncryptionKey, eq(
                        annotations["wow"]!!.toByteArray()
                ), IV)
            } returns annotations["wow"]!!.toByteArray()
            every {
                cryptoService.symEncrypt(tagEncryptionKey, eq(
                        annotations["it"]!!.toByteArray()
                ), IV)
            } returns annotations["it"]!!.toByteArray()
            every {
                cryptoService.symEncrypt(tagEncryptionKey, eq(
                        annotations["works"]!!.toByteArray()
                ), IV)
            } returns annotations["works"]!!.toByteArray()
        }

        // encrypt Resource
        every { cryptoService.fetchCurrentCommonKey() } returns commonKey
        every { cryptoService.currentCommonKeyId } returns commonKeyId
        every { cryptoService.encryptSymmetricKey(commonKey, KeyType.DATA_KEY, dataKey) } returns Single.just(encryptedDataKey)
        every { cryptoService.encrypt(dataKey, resource.value) } returns Single.just(encryptedResource)

        every { apiService.createRecord(ALIAS, USER_ID, any()) } returns Single.just(encryptedRecord)

        // decrypt tags
        every {
            cryptoService.symDecrypt(tagEncryptionKey, eq(
                    tags["partner"]!!.toByteArray()
            ), IV)
        } returns tags["partner"]!!.toByteArray()
        every {
            cryptoService.symDecrypt(tagEncryptionKey, eq(
                    tags["client"]!!.toByteArray()
            ), IV)
        } returns tags["client"]!!.toByteArray()
        every {
            cryptoService.symDecrypt(tagEncryptionKey, eq(
                    tags["flag"]!!.toByteArray()
            ), IV)
        } returns tags["flag"]!!.toByteArray()

        //decrypt annotations
        if (annotations.isNotEmpty()) {
            every {
                cryptoService.symDecrypt(tagEncryptionKey, eq(
                        annotations["wow"]!!.toByteArray()
                ), IV)
            } returns annotations["wow"]!!.toByteArray()
            every {
                cryptoService.symDecrypt(tagEncryptionKey, eq(
                        annotations["it"]!!.toByteArray()
                ), IV)
            } returns annotations["it"]!!.toByteArray()
            every {
                cryptoService.symDecrypt(tagEncryptionKey, eq(
                        annotations["works"]!!.toByteArray()
                ), IV)
            } returns annotations["works"]!!.toByteArray()
        }

        // decrypt record
        every { cryptoService.hasCommonKey(commonKeyId) } returns true
        every { cryptoService.getCommonKeyById(commonKeyId) } returns commonKey
        every { cryptoService.symDecryptSymmetricKey(commonKey, encryptedDataKey) } returns Single.just(dataKey)
        every { cryptoService.decrypt(dataKey, encryptedResource) } returns Single.just(resource.value)
    }

    @Test
    fun `Given, createFhir3Record is called with the appropriate payload without Annotations, it creates a Record for Fhir3`() {
        // Given
        val resource = buildDocumentReferenceFhir3()

        val tags = mapOf(
                "partner" to "partner=$PARTNER_ID".toLowerCase(),
                "client" to "client=${
                    URLEncoder.encode(CLIENT_ID.toLowerCase(), StandardCharsets.UTF_8.displayName())
                }",
                "fhirversion" to "fhirversion=${
                    "3.0.1".replace(".", "%2e")
                }",
                "resourcetype" to "resourcetype=documentreference"
        )

        stringifiedResource = "{\"content\":[{\"attachment\":{\"hash\":\"jwZ0G6YALQ4N8RGNHzJHIgX6j+I=\",\"id\":\"42\",\"size\":42}}],\"identifier\":[{\"assigner\":{\"reference\":\"$PARTNER_ID\"},\"value\":\"d4l_f_p_t#42\"}],\"resourceType\":\"DocumentReference\"}"
        encryptedBody = "ZW5jcnlwdGVk"

        encryptedRecord = EncryptedRecord(
                commonKeyId,
                RECORD_ID,
                listOf("cGFydG5lcj1iNDY=", "Y2xpZW50PWI0NiUyM3Rlc3Q=", "ZmhpcnZlcnNpb249MyUyZTAlMmUx", "cmVzb3VyY2V0eXBlPWRvY3VtZW50cmVmZXJlbmNl"),
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

        runFhirFlow(tags, gcKeys = gcKeys)

        // When
        val result = recordService.createRecord(
                USER_ID,
                resource,
                listOf()
        ).blockingGet()

        // Then
        Truth.assertThat(result).isInstanceOf(Record::class.java)
        Truth.assertThat(result.resource.identifier).isNotEmpty()
        Truth.assertThat(result.meta).isEqualTo(buildMeta(CREATION_DATE, UPDATE_DATE))
        Truth.assertThat(result.annotations).isEmpty()
        Truth.assertThat(result.resource).isEqualTo(resource)
    }

    @Test
    fun `Given, createFhir3Record is called with the appropriate payload with Annotations, it creates a Record for Fhir3`() {
        // Given
        val resource = buildDocumentReferenceFhir3()

        val tags = mapOf(
                "partner" to "partner=$PARTNER_ID".toLowerCase(),
                "client" to "client=${
                    URLEncoder.encode(CLIENT_ID.toLowerCase(), StandardCharsets.UTF_8.displayName())
                }",
                "fhirversion" to "fhirversion=${
                    "3.0.1".replace(".", "%2e")
                }",
                "resourcetype" to "resourcetype=documentreference"
        )
        val annotations = mapOf(
                "wow" to "custom=wow",
                "it" to "custom=it",
                "works" to "custom=works"
        )

        stringifiedResource = "{\"content\":[{\"attachment\":{\"hash\":\"jwZ0G6YALQ4N8RGNHzJHIgX6j+I=\",\"id\":\"42\",\"size\":42}}],\"identifier\":[{\"assigner\":{\"reference\":\"$PARTNER_ID\"},\"value\":\"d4l_f_p_t#42\"}],\"resourceType\":\"DocumentReference\"}"
        encryptedBody = "ZW5jcnlwdGVk"

        encryptedRecord = EncryptedRecord(
                commonKeyId,
                RECORD_ID,
                listOf(
                        "cGFydG5lcj1iNDY=",
                        "Y2xpZW50PWI0NiUyM3Rlc3Q=",
                        "ZmhpcnZlcnNpb249MyUyZTAlMmUx",
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

        runFhirFlow(tags, annotations, gcKeys)

        // When
        val result = recordService.createRecord(
                USER_ID,
                resource,
                listOf("wow", "it", "works")
        ).blockingGet()

        // Then
        Truth.assertThat(result).isInstanceOf(Record::class.java)
        Truth.assertThat(result.resource.identifier).isNotEmpty()
        Truth.assertThat(result.meta).isEqualTo(buildMeta(CREATION_DATE, UPDATE_DATE))
        Truth.assertThat(result.annotations).isEqualTo(listOf("wow", "it", "works"))
        Truth.assertThat(result.resource).isEqualTo(resource)
    }

    @Test
    fun `Given, createFhir3Record is called with the appropriate payload without Attachments, it creates a Record for Fhir3`() {
        // Given
        val resource = buildDocumentReferenceFhir3()
        resource.content = null

        val tags = mapOf(
                "partner" to "partner=$PARTNER_ID".toLowerCase(),
                "client" to "client=${
                    URLEncoder.encode(CLIENT_ID.toLowerCase(), StandardCharsets.UTF_8.displayName())
                }",
                "fhirversion" to "fhirversion=${
                    "3.0.1".replace(".", "%2e")
                }",
                "resourcetype" to "resourcetype=documentreference"
        )
        val annotations = mapOf(
                "wow" to "custom=wow",
                "it" to "custom=it",
                "works" to "custom=works"
        )

        stringifiedResource = "{\"resourceType\":\"DocumentReference\"}"
        encryptedBody = "ZW5jcnlwdGVk"

        encryptedRecord = EncryptedRecord(
                commonKeyId,
                RECORD_ID,
                listOf(
                        "cGFydG5lcj1iNDY=",
                        "Y2xpZW50PWI0NiUyM3Rlc3Q=",
                        "ZmhpcnZlcnNpb249MyUyZTAlMmUx",
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

        runFhirFlow(tags, annotations, gcKeys)

        // When
        val result = recordService.createRecord(
                USER_ID,
                resource,
                listOf("wow", "it", "works")
        ).blockingGet()

        // Then
        Truth.assertThat(result).isInstanceOf(Record::class.java)
        Truth.assertThat(result.resource).isNotNull()
        Truth.assertThat(result.meta).isEqualTo(buildMeta(CREATION_DATE, UPDATE_DATE))
        Truth.assertThat(result.annotations).isEqualTo(listOf("wow", "it", "works"))
        Truth.assertThat(result.resource).isEqualTo(resource)
    }

    @Test
    fun `Given, createFhir4Record is called with the appropriate payload without annotations, it creates a Record for Fhir4`() {
        // Given
        val resource: Fhir4Resource = buildDocumentReferenceFhir4()

        val tags = mapOf(
                "partner" to "partner=$PARTNER_ID".toLowerCase(),
                "client" to "client=${
                    URLEncoder.encode(CLIENT_ID.toLowerCase(), StandardCharsets.UTF_8.displayName())
                }",
                "fhirversion" to "fhirversion=${
                    "4.0.1".replace(".", "%2e")
                }",
                "resourcetype" to "resourcetype=documentreference"
        )

        stringifiedResource = "{\"content\":[{\"attachment\":{\"hash\":\"jwZ0G6YALQ4N8RGNHzJHIgX6j+I=\",\"id\":\"42\",\"size\":42}}],\"identifier\":[{\"assigner\":{\"reference\":\"$PARTNER_ID\"},\"value\":\"d4l_f_p_t#42\"}],\"resourceType\":\"DocumentReference\"}"
        encryptedBody = "ZW5jcnlwdGVk"

        encryptedRecord = EncryptedRecord(
                commonKeyId,
                RECORD_ID,
                listOf("cGFydG5lcj1iNDY=", "Y2xpZW50PWI0NiUyM3Rlc3Q=", "ZmhpcnZlcnNpb249NCUyZTAlMmUx", "cmVzb3VyY2V0eXBlPWRvY3VtZW50cmVmZXJlbmNl"),
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

        runFhirFlow(tags, gcKeys = gcKeys)

        // When
        val result = recordService.createRecord(
                USER_ID,
                resource,
                listOf()
        ).blockingGet()

        // Then
        Truth.assertThat(result).isInstanceOf(Fhir4Record::class.java)
        Truth.assertThat(result.identifier).isEqualTo(RECORD_ID)
        Truth.assertThat(result.meta).isEqualTo(buildMeta(CREATION_DATE, UPDATE_DATE))
        Truth.assertThat(result.annotations).isEmpty()
        Truth.assertThat(result.resource).isEqualTo(resource)
    }

    @Test
    fun `Given, createFhir4Record is called with the appropriate payload with annotations, it creates a Record for Fhir4`() {
        // Given
        val resource: Fhir4Resource = buildDocumentReferenceFhir4()

        val tags = mapOf(
                "partner" to "partner=$PARTNER_ID".toLowerCase(),
                "client" to "client=${
                    URLEncoder.encode(CLIENT_ID.toLowerCase(), StandardCharsets.UTF_8.displayName())
                }",
                "fhirversion" to "fhirversion=${
                    "4.0.1".replace(".", "%2e")
                }",
                "resourcetype" to "resourcetype=documentreference"
        )
        val annotations = mapOf(
                "wow" to "custom=wow",
                "it" to "custom=it",
                "works" to "custom=works"
        )

        stringifiedResource = "{\"content\":[{\"attachment\":{\"hash\":\"jwZ0G6YALQ4N8RGNHzJHIgX6j+I=\",\"id\":\"42\",\"size\":42}}],\"identifier\":[{\"assigner\":{\"reference\":\"$PARTNER_ID\"},\"value\":\"d4l_f_p_t#42\"}],\"resourceType\":\"DocumentReference\"}"
        encryptedBody = "ZW5jcnlwdGVk"

        encryptedRecord = EncryptedRecord(
                commonKeyId,
                RECORD_ID,
                listOf(
                        "cGFydG5lcj1iNDY=",
                        "Y2xpZW50PWI0NiUyM3Rlc3Q=",
                        "ZmhpcnZlcnNpb249NCUyZTAlMmUx",
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

        runFhirFlow(tags, annotations, gcKeys)

        // When
        val result = recordService.createRecord(
                USER_ID,
                resource,
                listOf("wow", "it", "works")
        ).blockingGet()

        // Then
        Truth.assertThat(result).isInstanceOf(Fhir4Record::class.java)
        Truth.assertThat(result.identifier).isEqualTo(RECORD_ID)
        Truth.assertThat(result.meta).isEqualTo(buildMeta(CREATION_DATE, UPDATE_DATE))
        Truth.assertThat(result.annotations).isEqualTo(listOf("wow", "it", "works"))
        Truth.assertThat(result.resource).isEqualTo(resource)
    }

    @Test
    fun `Given, createFhir4Record is called with the appropriate payload without Attachments, it creates a Fhir4Record`() {
        // Given
        val resource = buildDocumentReferenceFhir4()
        resource.content = null

        val tags = mapOf(
                "partner" to "partner=$PARTNER_ID".toLowerCase(),
                "client" to "client=${
                    URLEncoder.encode(CLIENT_ID.toLowerCase(), StandardCharsets.UTF_8.displayName())
                }",
                "fhirversion" to "fhirversion=${
                    "4.0.1".replace(".", "%2e")
                }",
                "resourcetype" to "resourcetype=documentreference"
        )
        val annotations = mapOf(
                "wow" to "custom=wow",
                "it" to "custom=it",
                "works" to "custom=works"
        )

        stringifiedResource = "{\"resourceType\":\"DocumentReference\"}"
        encryptedBody = "ZW5jcnlwdGVk"

        encryptedRecord = EncryptedRecord(
                commonKeyId,
                RECORD_ID,
                listOf(
                        "cGFydG5lcj1iNDY=",
                        "Y2xpZW50PWI0NiUyM3Rlc3Q=",
                        "ZmhpcnZlcnNpb249NCUyZTAlMmUx",
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

        runFhirFlow(tags, annotations, gcKeys)

        // When
        val result = recordService.createRecord(
                USER_ID,
                resource,
                listOf("wow", "it", "works")
        ).blockingGet()

        // Then
        Truth.assertThat(result).isInstanceOf(Fhir4Record::class.java)
        Truth.assertThat(result.resource).isNotNull()
        Truth.assertThat(result.meta).isEqualTo(buildMeta(CREATION_DATE, UPDATE_DATE))
        Truth.assertThat(result.annotations).isEqualTo(listOf("wow", "it", "works"))
        Truth.assertThat(result.resource).isEqualTo(resource)
    }

    @Test
    fun `Given, createDataRecord is called with the appropriate payload without Annotations, it creates a Record for DataResource`() {
        // Given
        val resource = DataResource("I am test".toByteArray())

        val tags = mapOf(
                "partner" to "partner=$PARTNER_ID".toLowerCase(),
                "client" to "client=${
                    URLEncoder.encode(CLIENT_ID.toLowerCase(), StandardCharsets.UTF_8.displayName())
                }",
                "flag" to "flag=appdata"
        )

        val encryptedResource = "The test string encrypted".toByteArray()

        encryptedBody = "VGhlIHRlc3Qgc3RyaW5nIGVuY3J5cHRlZA=="
        encryptedRecord = EncryptedRecord(
                commonKeyId,
                RECORD_ID,
                listOf("cGFydG5lcj1iNDY=", "Y2xpZW50PWI0NiUyM3Rlc3Q=", "ZmxhZz1hcHBkYXRh"),
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

        runDatalow(
                resource,
                encryptedResource,
                tags,
                gcKeys = gcKeys
        )

        // When
        val result = recordService.createRecord(
                USER_ID,
                resource,
                listOf()
        ).blockingGet()

        // Then
        Truth.assertThat(result).isInstanceOf(DataRecord::class.java)
        Truth.assertThat(result.identifier).isEqualTo(RECORD_ID)
        Truth.assertThat(result.meta).isEqualTo(buildMeta(CREATION_DATE, UPDATE_DATE))
        Truth.assertThat(result.annotations).isEmpty()
        Truth.assertThat(result.resource).isEqualTo(resource)
    }

    @Test
    fun `Given, createDataRecord is called with the appropriate payload with Attachments, it creates a Record for DataResource`() {
        // Given
        val resource = DataResource("I am test".toByteArray())

        val tags = mapOf(
                "partner" to "partner=$PARTNER_ID".toLowerCase(),
                "client" to "client=${
                    URLEncoder.encode(CLIENT_ID.toLowerCase(), StandardCharsets.UTF_8.displayName())
                }",
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
                        "cGFydG5lcj1iNDY=",
                        "Y2xpZW50PWI0NiUyM3Rlc3Q=",
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

        runDatalow(
                resource,
                encryptedResource,
                tags,
                annotations,
                gcKeys
        )

        // When
        val result = recordService.createRecord(
                USER_ID,
                resource,
                listOf("wow", "it", "works")
        ).blockingGet()

        // Then
        Truth.assertThat(result).isInstanceOf(DataRecord::class.java)
        Truth.assertThat(result.identifier).isEqualTo(RECORD_ID)
        Truth.assertThat(result.meta).isEqualTo(buildMeta(CREATION_DATE, UPDATE_DATE))
        Truth.assertThat(result.annotations).isEqualTo(listOf("wow", "it", "works"))
        Truth.assertThat(result.resource).isEqualTo(resource)
    }
}
