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
import care.data4life.crypto.GCKeyPair
import care.data4life.crypto.KeyType
import care.data4life.sdk.attachment.AttachmentService
import care.data4life.sdk.call.DataRecord
import care.data4life.sdk.call.Fhir4Record
import care.data4life.sdk.data.DataResource
import care.data4life.sdk.fhir.FhirService
import care.data4life.sdk.model.ModelContract.ModelVersion.Companion.CURRENT
import care.data4life.sdk.model.Record
import care.data4life.sdk.network.model.CommonKeyResponse
import care.data4life.sdk.network.model.EncryptedKey
import care.data4life.sdk.network.model.EncryptedRecord
import care.data4life.sdk.tag.TagEncryptionService
import care.data4life.sdk.tag.TaggingService
import care.data4life.sdk.test.util.GenericTestDataProvider
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

class RecordServiceUpdateIntegration : RecordServiceIntegrationBase() {
    private lateinit var commonKeyResponse: CommonKeyResponse
    private lateinit var keyPair: GCKeyPair
    private lateinit var encryptedCommonKey: EncryptedKey
    private lateinit var updatedResourceString: String
    private lateinit var updatedEncryptedBody: String
    private lateinit var updatedEncryptedRecord: EncryptedRecord

    @Before
    fun setUp() {
        apiService = mockk()
        cryptoService = mockk()
        fileService = mockk()
        imageResizer = mockk()
        errorHandler = mockk()

        mockkObject(MimeType)

        recordService = RecordService(
                GenericTestDataProvider.PARTNER_ID,
                GenericTestDataProvider.ALIAS,
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
        tagEncryptionKey = mockk()
        commonKey = mockk()
        encryptedDataKey = mockk()
        encryptedAttachmentKey = mockk()
        commonKeyResponse = mockk()
        keyPair = mockk()
        encryptedCommonKey = mockk()
    }

    @After
    fun tearDown() {
        unmockkObject(MimeType)
    }

    private fun runFhirFlow(
            tags: Map<String, String>,
            annotations: Map<String, String> = mapOf(),
            dataKeyRound1: GCKey,
            dataKeyRound2: GCKey,
            gcKeys: MutableList<GCKey>
    ) {
        val dataKeys = mutableListOf(
                dataKeyRound1,
                dataKeyRound2
        )

        // constrain check
        every { MimeType.recognizeMimeType(Base64.decode(ATTACHMENT_PAYLOAD)) } returns MimeType.PDF
        // get tags
        every { cryptoService.generateGCKey() } answers {
            Single.just(gcKeys.removeAt(0))
        }

        every { apiService.fetchRecord(ALIAS, USER_ID, RECORD_ID) } returns Single.just(encryptedRecord)

        // decrypt Record
        // decrypt tags
        every { cryptoService.fetchTagEncryptionKey() } returns tagEncryptionKey
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

        // decrypt body
        every { cryptoService.hasCommonKey(commonKeyId) } returns false
        every { apiService.fetchCommonKey(ALIAS, USER_ID, commonKeyId) } returns Single.just(commonKeyResponse)
        every { commonKeyResponse.commonKey } returns encryptedCommonKey
        every { cryptoService.fetchGCKeyPair() } returns Single.just(keyPair)
        every { cryptoService.asymDecryptSymetricKey(keyPair, encryptedCommonKey) } returns Single.just(commonKey)
        every { cryptoService.storeCommonKey(commonKeyId, commonKey) } returns Unit

        // decrypt Resource
        every { cryptoService.symDecryptSymmetricKey(commonKey, encryptedDataKey) } answers {
            Single.just(dataKeys.removeAt(0))
        }
        every { cryptoService.symDecryptSymmetricKey(commonKey, encryptedAttachmentKey) } returns Single.just(attachmentKey)
        every { cryptoService.decodeAndDecryptString(dataKeyRound1, eq(encryptedBody)) } returns Single.just(
                stringifiedResource
        )

        // upload
        every {
            fileService.uploadFile(attachmentKey, USER_ID, Base64.decode(ATTACHMENT_PAYLOAD))
        } returns Single.just(
                "${ATTACHMENT_ID}#${PREVIEW_ID}#${THUMBNAIL_ID}"
        )
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
        every { cryptoService.encryptSymmetricKey(commonKey, KeyType.DATA_KEY, dataKeyRound1) } returns Single.just(encryptedDataKey)
        every { cryptoService.encryptAndEncodeString(dataKeyRound1, eq(updatedResourceString)) } returns Single.just(updatedEncryptedBody)

        //encrypt Attachment
        every {
            cryptoService.encryptSymmetricKey(commonKey, KeyType.ATTACHMENT_KEY, attachmentKey)
        } returns Single.just(encryptedAttachmentKey)

        every {
            apiService.updateRecord(ALIAS, USER_ID, RECORD_ID, any())
        } returns Single.just(updatedEncryptedRecord)

        // decrypt Record
        every { cryptoService.decodeAndDecryptString(dataKeyRound2, eq(updatedEncryptedBody)) } returns Single.just(updatedResourceString)
    }

    private fun runDataFlow(
            resource: DataResource,
            encryptedResource: ByteArray,
            tags: Map<String, String>,
            annotations: Map<String, String> = mapOf()
    ) {
        every { apiService.fetchRecord(ALIAS, USER_ID, RECORD_ID) } returns Single.just(encryptedRecord)

        // decrypt tags
        every { cryptoService.fetchTagEncryptionKey() } returns tagEncryptionKey
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

        // decrypt body
        // fetch common key
        every { cryptoService.hasCommonKey(commonKeyId) } returns false
        every { apiService.fetchCommonKey(ALIAS, USER_ID, commonKeyId) } returns Single.just(commonKeyResponse)
        every { commonKeyResponse.commonKey } returns encryptedCommonKey
        every { cryptoService.fetchGCKeyPair() } returns Single.just(keyPair)
        every { cryptoService.asymDecryptSymetricKey(keyPair, encryptedCommonKey) } returns Single.just(commonKey)
        every { cryptoService.storeCommonKey(commonKeyId, commonKey) } returns Unit

        // decrypt Resource
        every { cryptoService.symDecryptSymmetricKey(commonKey, encryptedDataKey) } returns Single.just(dataKey)
        every { cryptoService.decrypt(dataKey, encryptedResource) } returns Single.just(resource.value)

        //Encrypt Record
        //encrypt Tags
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

        every {
            apiService.updateRecord(ALIAS, USER_ID, RECORD_ID, any())
        } returns Single.just(updatedEncryptedRecord)

        // decrypt Record
        every { cryptoService.decrypt(dataKey, Base64.decode(updatedEncryptedBody)) } returns Single.just(resource.value)
    }

    @Test
    fun `Given, updateFhir3Record is called with the appropriate payload without Annotations, it return a updated Record`() {
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

        encryptedBody = "ZW5jcnlwdGVk"
        updatedEncryptedBody = "Wlc1amNubHdkR1Zr"

        encryptedRecord = EncryptedRecord(
                commonKeyId,
                RECORD_ID,
                listOf("cGFydG5lcj1iNDY=", "Y2xpZW50PWI0NiUyM3Rlc3Q=", "ZmhpcnZlcnNpb249MyUyZTAlMmUx", "cmVzb3VyY2V0eXBlPWRvY3VtZW50cmVmZXJlbmNl"),
                encryptedBody,
                CREATION_DATE,
                encryptedDataKey,
                encryptedAttachmentKey,
                CURRENT,
                UPDATE_DATE
        )

        updatedEncryptedRecord = EncryptedRecord(
                commonKeyId,
                RECORD_ID,
                listOf("cGFydG5lcj1iNDY=", "Y2xpZW50PWI0NiUyM3Rlc3Q=", "ZmhpcnZlcnNpb249MyUyZTAlMmUx", "cmVzb3VyY2V0eXBlPWRvY3VtZW50cmVmZXJlbmNl"),
                updatedEncryptedBody,
                CREATION_DATE,
                encryptedDataKey,
                encryptedAttachmentKey,
                CURRENT,
                UPDATE_DATE
        )

        stringifiedResource = "{\"content\":[{\"attachment\":{\"hash\":\"jwZ0G6YALQ4N8RGNHzJHIgX6j+I=\",\"id\":\"42\",\"size\":42}}],\"identifier\":[{\"assigner\":{\"reference\":\"partnerId\"},\"value\":\"d4l_f_p_t#42\"}],\"resourceType\":\"DocumentReference\"}"
        updatedResourceString = "{\"content\":[{\"attachment\":{\"hash\":\"jwZ0G6YALQ4N8RGNHzJHIgX6j+I=\",\"id\":\"attachmentId#previewId#thumbnailId\",\"size\":42}}],\"resourceType\":\"DocumentReference\"}"

        val gcKeys = mutableListOf(
                attachmentKey,
                dataKey
        )

        runFhirFlow(
                tags,
                gcKeys = gcKeys,
                dataKeyRound1 = mockk(),
                dataKeyRound2 = mockk()
        )

        // When
        val result = recordService.updateRecord(
                USER_ID,
                RECORD_ID,
                resource,
                listOf()
        ).blockingGet()

        // Then
        Truth.assertThat(result).isInstanceOf(Record::class.java)
        Truth.assertThat(result.resource.identifier).isNull()
        Truth.assertThat(result.meta).isEqualTo(buildMeta(CREATION_DATE, UPDATE_DATE))
        Truth.assertThat(result.annotations).isEmpty()
        Truth.assertThat(result.resource).isEqualTo(resource)
    }

    @Test
    fun `Given, updateFhir3Record is called with the appropriate payload with Annotations, it return a updated Record`() {
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

        encryptedBody = "ZW5jcnlwdGVk"
        updatedEncryptedBody = "Wlc1amNubHdkR1Zr"

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
                CURRENT,
                UPDATE_DATE
        )

        updatedEncryptedRecord = EncryptedRecord(
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
                updatedEncryptedBody,
                CREATION_DATE,
                encryptedDataKey,
                encryptedAttachmentKey,
                CURRENT,
                UPDATE_DATE
        )

        stringifiedResource = "{\"content\":[{\"attachment\":{\"hash\":\"jwZ0G6YALQ4N8RGNHzJHIgX6j+I=\",\"id\":\"42\",\"size\":42}}],\"identifier\":[{\"assigner\":{\"reference\":\"partnerId\"},\"value\":\"d4l_f_p_t#42\"}],\"resourceType\":\"DocumentReference\"}"
        updatedResourceString = "{\"content\":[{\"attachment\":{\"hash\":\"jwZ0G6YALQ4N8RGNHzJHIgX6j+I=\",\"id\":\"attachmentId#previewId#thumbnailId\",\"size\":42}}],\"resourceType\":\"DocumentReference\"}"

        val gcKeys = mutableListOf(
                attachmentKey,
                dataKey
        )

        runFhirFlow(
                tags,
                annotations,
                mockk(),
                mockk(),
                gcKeys
        )

        // When
        val result = recordService.updateRecord(
                USER_ID,
                RECORD_ID,
                resource,
                listOf("wow", "it", "works")
        ).blockingGet()

        // Then
        Truth.assertThat(result).isInstanceOf(Record::class.java)
        Truth.assertThat(result.resource.identifier).isNull()
        Truth.assertThat(result.meta).isEqualTo(buildMeta(CREATION_DATE, UPDATE_DATE))
        Truth.assertThat(result.annotations).isEqualTo(listOf("wow", "it", "works"))
        Truth.assertThat(result.resource).isEqualTo(resource)
    }

    @Test
    fun `Given, updateFhir3Record is called with the appropriate payload without Annotations and Attachments with exiting Identifier, it returns a updated Record`() {
        // Given
        val resource = buildDocumentReferenceFhir3("42")

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

        encryptedBody = "ZW5jcnlwdGVk"
        updatedEncryptedBody = "eyJjb250ZW50IjpbeyJhdHRhY2htZW50Ijp7Imhhc2giOiJqd1owRzZZQUxRNE44UkdOSHpKSElnWDZqK0k9IiwiaWQiOiI0MiIsInNpemUiOjQyfX1dLCJyZXNvdXJjZVR5cGUiOiJEb2N1bWVudFJlZmVyZW5jZSJ9"

        encryptedRecord = EncryptedRecord(
                commonKeyId,
                RECORD_ID,
                listOf("cGFydG5lcj1iNDY=", "Y2xpZW50PWI0NiUyM3Rlc3Q=", "ZmhpcnZlcnNpb249MyUyZTAlMmUx", "cmVzb3VyY2V0eXBlPWRvY3VtZW50cmVmZXJlbmNl"),
                encryptedBody,
                CREATION_DATE,
                encryptedDataKey,
                encryptedAttachmentKey,
                CURRENT,
                UPDATE_DATE
        )

        updatedEncryptedRecord = EncryptedRecord(
                commonKeyId,
                RECORD_ID,
                listOf("cGFydG5lcj1iNDY=", "Y2xpZW50PWI0NiUyM3Rlc3Q=", "ZmhpcnZlcnNpb249MyUyZTAlMmUx", "cmVzb3VyY2V0eXBlPWRvY3VtZW50cmVmZXJlbmNl"),
                updatedEncryptedBody,
                CREATION_DATE,
                encryptedDataKey,
                encryptedAttachmentKey,
                CURRENT,
                UPDATE_DATE
        )

        stringifiedResource = "{\"content\":[{\"attachment\":{\"hash\":\"jwZ0G6YALQ4N8RGNHzJHIgX6j+I=\",\"id\":\"42\",\"size\":42}}],\"identifier\":[{\"assigner\":{\"reference\":\"partnerId\"},\"value\":\"d4l_f_p_t#42\"}],\"resourceType\":\"DocumentReference\"}"
        updatedResourceString = "{\"content\":[{\"attachment\":{\"hash\":\"jwZ0G6YALQ4N8RGNHzJHIgX6j+I=\",\"id\":\"42\",\"size\":42}}],\"resourceType\":\"DocumentReference\"}"

        val gcKeys = mutableListOf(
                attachmentKey,
                dataKey
        )

        runFhirFlow(
                tags,
                gcKeys = gcKeys,
                dataKeyRound1 = mockk(),
                dataKeyRound2 = mockk()
        )

        // When
        val result = recordService.updateRecord(
                USER_ID,
                RECORD_ID,
                resource,
                listOf()
        ).blockingGet()

        // Then
        Truth.assertThat(result).isInstanceOf(Record::class.java)
        Truth.assertThat(result.resource.identifier).isNull()
        Truth.assertThat(result.meta).isEqualTo(buildMeta(CREATION_DATE, UPDATE_DATE))
        Truth.assertThat(result.annotations).isEmpty()
        Truth.assertThat(result.resource).isEqualTo(resource)
    }

    // ToDo with clean obsolete Id

    @Test
    fun `Given, updateFhir4Record is called with the appropriate payload without Annotations, it return a updated Fhir4Record`() {
        // Given
        val resource = buildDocumentReferenceFhir4()

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

        encryptedBody = "ZW5jcnlwdGVk"
        updatedEncryptedBody = "Wlc1amNubHdkR1Zr"

        encryptedRecord = EncryptedRecord(
                commonKeyId,
                RECORD_ID,
                listOf("cGFydG5lcj1iNDY=", "Y2xpZW50PWI0NiUyM3Rlc3Q=", "ZmhpcnZlcnNpb249NCUyZTAlMmUx", "cmVzb3VyY2V0eXBlPWRvY3VtZW50cmVmZXJlbmNl"),
                encryptedBody,
                CREATION_DATE,
                encryptedDataKey,
                encryptedAttachmentKey,
                CURRENT,
                UPDATE_DATE
        )

        updatedEncryptedRecord = EncryptedRecord(
                commonKeyId,
                RECORD_ID,
                listOf("cGFydG5lcj1iNDY=", "Y2xpZW50PWI0NiUyM3Rlc3Q=", "ZmhpcnZlcnNpb249NCUyZTAlMmUx", "cmVzb3VyY2V0eXBlPWRvY3VtZW50cmVmZXJlbmNl"),
                updatedEncryptedBody,
                CREATION_DATE,
                encryptedDataKey,
                encryptedAttachmentKey,
                CURRENT,
                UPDATE_DATE
        )

        stringifiedResource = "{\"content\":[{\"attachment\":{\"hash\":\"jwZ0G6YALQ4N8RGNHzJHIgX6j+I=\",\"id\":\"42\",\"size\":42}}],\"identifier\":[{\"assigner\":{\"reference\":\"partnerId\"},\"value\":\"d4l_f_p_t#42\"}],\"resourceType\":\"DocumentReference\"}"
        updatedResourceString = "{\"content\":[{\"attachment\":{\"hash\":\"jwZ0G6YALQ4N8RGNHzJHIgX6j+I=\",\"id\":\"attachmentId#previewId#thumbnailId\",\"size\":42}}],\"resourceType\":\"DocumentReference\"}"

        val gcKeys = mutableListOf(
                attachmentKey,
                dataKey
        )

        runFhirFlow(
                tags,
                gcKeys = gcKeys,
                dataKeyRound1 = mockk(),
                dataKeyRound2 = mockk()
        )

        // When
        val result = recordService.updateRecord(
                USER_ID,
                RECORD_ID,
                resource,
                listOf()
        ).blockingGet()

        // Then
        Truth.assertThat(result).isInstanceOf(Fhir4Record::class.java)
        Truth.assertThat(result.resource.identifier).isNull()
        Truth.assertThat(result.meta).isEqualTo(buildMeta(CREATION_DATE, UPDATE_DATE))
        Truth.assertThat(result.annotations).isEmpty()
        Truth.assertThat(result.resource).isEqualTo(resource)
    }

    @Test
    fun `Given, updateFhir4Record is called with the appropriate payload with Annotations, it return a updated Fhir4Record`() {
        // Given
        val resource = buildDocumentReferenceFhir4()

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

        encryptedBody = "ZW5jcnlwdGVk"
        updatedEncryptedBody = "Wlc1amNubHdkR1Zr"

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
                CURRENT,
                UPDATE_DATE
        )

        updatedEncryptedRecord = EncryptedRecord(
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
                updatedEncryptedBody,
                CREATION_DATE,
                encryptedDataKey,
                encryptedAttachmentKey,
                CURRENT,
                UPDATE_DATE
        )

        stringifiedResource = "{\"content\":[{\"attachment\":{\"hash\":\"jwZ0G6YALQ4N8RGNHzJHIgX6j+I=\",\"id\":\"42\",\"size\":42}}],\"identifier\":[{\"assigner\":{\"reference\":\"partnerId\"},\"value\":\"d4l_f_p_t#42\"}],\"resourceType\":\"DocumentReference\"}"
        updatedResourceString = "{\"content\":[{\"attachment\":{\"hash\":\"jwZ0G6YALQ4N8RGNHzJHIgX6j+I=\",\"id\":\"attachmentId#previewId#thumbnailId\",\"size\":42}}],\"resourceType\":\"DocumentReference\"}"

        val gcKeys = mutableListOf(
                attachmentKey,
                dataKey
        )

        runFhirFlow(
                tags,
                annotations,
                mockk(),
                mockk(),
                gcKeys
        )

        // When
        val result = recordService.updateRecord(
                USER_ID,
                RECORD_ID,
                resource,
                listOf("wow", "it", "works")
        ).blockingGet()

        // Then
        Truth.assertThat(result).isInstanceOf(Fhir4Record::class.java)
        Truth.assertThat(result.resource.identifier).isNull()
        Truth.assertThat(result.meta).isEqualTo(buildMeta(CREATION_DATE, UPDATE_DATE))
        Truth.assertThat(result.annotations).isEqualTo(listOf("wow", "it", "works"))
        Truth.assertThat(result.resource).isEqualTo(resource)
    }

    @Test
    fun `Given, updateFhir4Record is called with the appropriate payload without Annotations and Attachments with exiting Identifier, it returns a updated Fhir4Record`() {
        // Given
        val resource = buildDocumentReferenceFhir4("42")

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

        encryptedBody = "ZW5jcnlwdGVk"
        updatedEncryptedBody = "eyJjb250ZW50IjpbeyJhdHRhY2htZW50Ijp7Imhhc2giOiJqd1owRzZZQUxRNE44UkdOSHpKSElnWDZqK0k9IiwiaWQiOiI0MiIsInNpemUiOjQyfX1dLCJyZXNvdXJjZVR5cGUiOiJEb2N1bWVudFJlZmVyZW5jZSJ9"

        encryptedRecord = EncryptedRecord(
                commonKeyId,
                RECORD_ID,
                listOf("cGFydG5lcj1iNDY=", "Y2xpZW50PWI0NiUyM3Rlc3Q=", "ZmhpcnZlcnNpb249NCUyZTAlMmUx", "cmVzb3VyY2V0eXBlPWRvY3VtZW50cmVmZXJlbmNl"),
                encryptedBody,
                CREATION_DATE,
                encryptedDataKey,
                encryptedAttachmentKey,
                CURRENT,
                UPDATE_DATE
        )

        updatedEncryptedRecord = EncryptedRecord(
                commonKeyId,
                RECORD_ID,
                listOf("cGFydG5lcj1iNDY=", "Y2xpZW50PWI0NiUyM3Rlc3Q=", "ZmhpcnZlcnNpb249NCUyZTAlMmUx", "cmVzb3VyY2V0eXBlPWRvY3VtZW50cmVmZXJlbmNl"),
                updatedEncryptedBody,
                CREATION_DATE,
                encryptedDataKey,
                encryptedAttachmentKey,
                CURRENT,
                UPDATE_DATE
        )

        stringifiedResource = "{\"content\":[{\"attachment\":{\"hash\":\"jwZ0G6YALQ4N8RGNHzJHIgX6j+I=\",\"id\":\"42\",\"size\":42}}],\"identifier\":[{\"assigner\":{\"reference\":\"partnerId\"},\"value\":\"d4l_f_p_t#42\"}],\"resourceType\":\"DocumentReference\"}"
        updatedResourceString = "{\"content\":[{\"attachment\":{\"hash\":\"jwZ0G6YALQ4N8RGNHzJHIgX6j+I=\",\"id\":\"42\",\"size\":42}}],\"resourceType\":\"DocumentReference\"}"

        val gcKeys = mutableListOf(
                attachmentKey,
                dataKey
        )

        runFhirFlow(
                tags,
                gcKeys = gcKeys,
                dataKeyRound1 = mockk(),
                dataKeyRound2 = mockk()
        )

        // When
        val result = recordService.updateRecord(
                USER_ID,
                RECORD_ID,
                resource,
                listOf()
        ).blockingGet()

        // Then
        Truth.assertThat(result).isInstanceOf(Fhir4Record::class.java)
        Truth.assertThat(result.resource.identifier).isNull()
        Truth.assertThat(result.meta).isEqualTo(buildMeta(CREATION_DATE, UPDATE_DATE))
        Truth.assertThat(result.annotations).isEmpty()
        Truth.assertThat(result.resource).isEqualTo(resource)
    }

    // ToDo with clean obsolete Id

    @Test
    fun `Given, updateDataRecord is called with the appropriate payload without Annotations, it returns a updated DataRecord`() {
        // Given
        val resource = DataResource("I am a new test".toByteArray())

        val tags = mapOf(
                "partner" to "partner=$PARTNER_ID".toLowerCase(),
                "client" to "client=${
                    URLEncoder.encode(CLIENT_ID.toLowerCase(), StandardCharsets.UTF_8.displayName())
                }",
                "flag" to "flag=appdata"
        )

        val encryptedResource = "The test string encrypted".toByteArray()

        encryptedBody = "VGhlIHRlc3Qgc3RyaW5nIGVuY3J5cHRlZA=="
        updatedEncryptedBody = "SSBhbSBhIG5ldyB0ZXN0"

        encryptedRecord = EncryptedRecord(
                commonKeyId,
                RECORD_ID,
                listOf(
                        "cGFydG5lcj1iNDY=", "Y2xpZW50PWI0NiUyM3Rlc3Q=",
                        "ZmxhZz1hcHBkYXRh"
                ),
                encryptedBody,
                CREATION_DATE,
                encryptedDataKey,
                null,
                CURRENT,
                UPDATE_DATE
        )
        updatedEncryptedRecord = EncryptedRecord(
                commonKeyId,
                RECORD_ID,
                listOf(
                        "cGFydG5lcj1iNDY=", "Y2xpZW50PWI0NiUyM3Rlc3Q=",
                        "ZmxhZz1hcHBkYXRh"
                ),
                updatedEncryptedBody,
                CREATION_DATE,
                encryptedDataKey,
                null,
                CURRENT,
                UPDATE_DATE
        )

        runDataFlow(
                resource,
                encryptedResource,
                tags
        )

        // When
        val result = recordService.updateRecord(
                USER_ID,
                RECORD_ID,
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
    fun `Given, updateDataRecord is called with the appropriate payload with Annotations, it returns a updated DataRecord`() {
        // Given
        val resource = DataResource("I am a new test".toByteArray())

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
        updatedEncryptedBody = "SSBhbSBhIG5ldyB0ZXN0"

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
                CURRENT,
                UPDATE_DATE
        )

        updatedEncryptedRecord = EncryptedRecord(
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
                updatedEncryptedBody,
                CREATION_DATE,
                encryptedDataKey,
                null,
                CURRENT,
                UPDATE_DATE
        )

        runDataFlow(
                resource,
                encryptedResource,
                tags,
                annotations
        )

        // When
        val result = recordService.updateRecord(
                USER_ID,
                RECORD_ID,
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

    // Fixme: This is a potential crash without error log
    @Test
    fun `Given, updateRecord is called with a Fhir4Resource, but it is actually a Fhir3Resource, it fails`() {
        // Given
        val resource = buildDocumentReferenceFhir3()

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

        encryptedBody = "ZW5jcnlwdGVk"
        updatedEncryptedBody = "Wlc1amNubHdkR1Zr"

        encryptedRecord = EncryptedRecord(
                commonKeyId,
                RECORD_ID,
                listOf("cGFydG5lcj1iNDY=", "Y2xpZW50PWI0NiUyM3Rlc3Q=", "ZmhpcnZlcnNpb249NCUyZTAlMmUx", "cmVzb3VyY2V0eXBlPWRvY3VtZW50cmVmZXJlbmNl"),
                encryptedBody,
                CREATION_DATE,
                encryptedDataKey,
                encryptedAttachmentKey,
                CURRENT,
                UPDATE_DATE
        )

        updatedEncryptedRecord = EncryptedRecord(
                commonKeyId,
                RECORD_ID,
                listOf("cGFydG5lcj1iNDY=", "Y2xpZW50PWI0NiUyM3Rlc3Q=", "ZmhpcnZlcnNpb249NCUyZTAlMmUx", "cmVzb3VyY2V0eXBlPWRvY3VtZW50cmVmZXJlbmNl"),
                updatedEncryptedBody,
                CREATION_DATE,
                encryptedDataKey,
                encryptedAttachmentKey,
                CURRENT,
                UPDATE_DATE
        )

        stringifiedResource = "{\"content\":[{\"attachment\":{\"hash\":\"jwZ0G6YALQ4N8RGNHzJHIgX6j+I=\",\"id\":\"42\",\"size\":42}}],\"identifier\":[{\"assigner\":{\"reference\":\"partnerId\"},\"value\":\"d4l_f_p_t#42\"}],\"resourceType\":\"DocumentReference\"}"
        updatedResourceString = "{\"content\":[{\"attachment\":{\"hash\":\"jwZ0G6YALQ4N8RGNHzJHIgX6j+I=\",\"id\":\"attachmentId#previewId#thumbnailId\",\"size\":42}}],\"resourceType\":\"DocumentReference\"}"

        val gcKeys = mutableListOf(
                attachmentKey,
                dataKey
        )

        runFhirFlow(
                tags,
                gcKeys = gcKeys,
                dataKeyRound1 = mockk(),
                dataKeyRound2 = mockk()
        )

        try {
            // When
            recordService.updateRecord(
                    USER_ID,
                    RECORD_ID,
                    resource,
                    listOf()
            ).blockingGet()
        } catch (e: Exception) {
            // Then
            Truth.assertThat(e).isInstanceOf(ClassCastException::class.java)
        }
    }

    // TODO Test with attachments
}
