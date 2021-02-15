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

import care.data4life.sdk.attachment.AttachmentService
import care.data4life.sdk.call.DataRecord
import care.data4life.sdk.call.Fhir4Record
import care.data4life.sdk.data.DataResource
import care.data4life.sdk.fhir.Fhir3Resource
import care.data4life.sdk.fhir.Fhir4Resource
import care.data4life.sdk.fhir.FhirService
import care.data4life.sdk.model.ModelVersion
import care.data4life.sdk.model.Record
import care.data4life.sdk.network.model.EncryptedRecord
import care.data4life.sdk.tag.TagEncryptionService
import care.data4life.sdk.tag.TaggingService
import com.google.common.truth.Truth
import io.mockk.every
import io.mockk.mockk
import io.reactivex.Observable
import io.reactivex.Single
import org.junit.Before
import org.junit.Test
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import care.data4life.fhir.r4.model.DocumentReference as Fhir4DocumentReference
import care.data4life.fhir.stu3.model.DocumentReference as Fhir3DocumentReference


class RecordServiceFetchIntegration : RecordServiceIntegrationBase() {
    private lateinit var encryptedRecord2: EncryptedRecord
    private lateinit var encryptedBody2: String
    private lateinit var stringifiedResource2: String
    private val OFFSET = 23
    private val PAGE_SIZE = 42

    @Before
    fun setUp() {
        apiService = mockk()
        cryptoService = mockk()
        fileService = mockk()
        imageResizer = mockk()
        errorHandler = mockk()

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
        tagEncryptionKey = mockk()
        commonKey = mockk()
        encryptedDataKey = mockk()
        encryptedAttachmentKey = mockk()
    }

    private fun runFhirFlow(
            tags: Map<String, String>,
            annotations: Map<String, String> = mapOf()
    ) {
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

        //decrypt Annotations
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

        // decrypt Resource
        every { cryptoService.hasCommonKey(commonKeyId) } returns true
        every { cryptoService.getCommonKeyById(commonKeyId) } returns commonKey
        every { cryptoService.symDecryptSymmetricKey(commonKey, encryptedAttachmentKey) } returns Single.just(attachmentKey)
        every { cryptoService.symDecryptSymmetricKey(commonKey, encryptedDataKey) } returns Single.just(dataKey)
        every { cryptoService.decodeAndDecryptString(dataKey, encryptedBody) } returns Single.just(stringifiedResource)
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

        //decrypt Annotations
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

        // decrypt Resource
        every { cryptoService.hasCommonKey(commonKeyId) } returns true
        every { cryptoService.getCommonKeyById(commonKeyId) } returns commonKey
        every { cryptoService.symDecryptSymmetricKey(commonKey, encryptedDataKey) } returns Single.just(dataKey)
        every { cryptoService.decrypt(dataKey, encryptedResource) } returns Single.just(resource.value)
    }

    fun runFhirFlowBatch(
            tags: Map<String, String>,
            annotations: Map<String, String> = mapOf(),
            encryptedTags: List<String>
    ) {
        // encrypt tags
        every { cryptoService.fetchTagEncryptionKey() } returns tagEncryptionKey
        every {
            cryptoService.symEncrypt(tagEncryptionKey, eq(
                    tags["resourcetype"]!!.toByteArray()
            ), IV)
        } returns tags["resourcetype"]!!.toByteArray()
        every {
            cryptoService.symEncrypt(tagEncryptionKey, eq(
                    tags["fhirversion"]!!.toByteArray()
            ), IV)
        } returns tags["fhirversion"]!!.toByteArray()

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

        //fetch
        every {
            apiService.fetchRecords(
                    ALIAS,
                    USER_ID,
                    null,
                    null,
                    PAGE_SIZE,
                    OFFSET,
                    encryptedTags
            )
        } returns Observable.fromArray(
                arrayListOf(
                        encryptedRecord,
                        encryptedRecord2
                )
        )

        // decryptTag
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

        // decrypt Resource
        every { cryptoService.hasCommonKey(commonKeyId) } returns true
        every { cryptoService.getCommonKeyById(commonKeyId) } returns commonKey
        every { cryptoService.symDecryptSymmetricKey(commonKey, encryptedAttachmentKey) } returns Single.just(attachmentKey)
        every { cryptoService.symDecryptSymmetricKey(commonKey, encryptedDataKey) } returns Single.just(dataKey)
        every { cryptoService.decodeAndDecryptString(dataKey, encryptedBody) } returns Single.just(stringifiedResource)
        every { cryptoService.decodeAndDecryptString(dataKey, encryptedBody2) } returns Single.just(stringifiedResource2)
    }

    fun runDataFlowBatch(
            value1: ByteArray,
            value2: ByteArray,
            encryptedResource1: ByteArray,
            encryptedResource2: ByteArray,
            tags: Map<String, String>,
            annotations: Map<String, String> = mapOf(),
            encryptedTags: List<String>
    ) {
        // encrypt tags
        every { cryptoService.fetchTagEncryptionKey() } returns tagEncryptionKey
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

        every {
            apiService.fetchRecords(
                    ALIAS,
                    USER_ID,
                    null,
                    null,
                    PAGE_SIZE,
                    OFFSET,
                    encryptedTags
            )
        } returns Observable.fromArray(
                arrayListOf(
                        encryptedRecord,
                        encryptedRecord2
                )
        )

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

        //decrypt Annotations
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
        every { cryptoService.hasCommonKey(commonKeyId) } returns true
        every { cryptoService.getCommonKeyById(commonKeyId) } returns commonKey


        // decrypt Resource
        every { cryptoService.symDecryptSymmetricKey(commonKey, encryptedDataKey) } returns Single.just(dataKey)
        every { cryptoService.decrypt(dataKey, encryptedResource1) } returns Single.just(value1)
        every { cryptoService.decrypt(dataKey, encryptedResource2) } returns Single.just(value2)
    }

    @Test
    fun `Given, fetchFhir3Record is called, with its appropriate payloads, it returns a Record`() {
        // Given
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
        encryptedRecord = EncryptedRecord(
                commonKeyId,
                RECORD_ID,
                listOf("cGFydG5lcj1iNDY=", "Y2xpZW50PWI0NiUyM3Rlc3Q=", "ZmhpcnZlcnNpb249MyUyZTAlMmUx", "cmVzb3VyY2V0eXBlPWRvY3VtZW50cmVmZXJlbmNl"),
                encryptedBody,
                CREATION_DATE,
                encryptedDataKey,
                encryptedAttachmentKey,
                ModelVersion.CURRENT,
                UPDATE_DATE
        )

        stringifiedResource = "{\"content\":[{\"attachment\":{\"hash\":\"jwZ0G6YALQ4N8RGNHzJHIgX6j+I=\",\"id\":\"42\",\"size\":42}}],\"identifier\":[{\"assigner\":{\"reference\":\"partnerId\"},\"value\":\"d4l_f_p_t#42\"}],\"resourceType\":\"DocumentReference\"}"

        runFhirFlow(tags)

        // When
        val result = recordService.fetchFhir3Record<Fhir3Resource>(USER_ID, RECORD_ID).blockingGet()

        // Then
        Truth.assertThat(result).isInstanceOf(Record::class.java)
        Truth.assertThat(result.resource).isInstanceOf(Fhir3Resource::class.java)
    }

    @Test
    fun `Given, fetchFhir3Record is called, with its appropriate payloads, it returns a Record with annotations`() {
        // Given
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
                ModelVersion.CURRENT,
                UPDATE_DATE
        )

        stringifiedResource = "{\"content\":[{\"attachment\":{\"hash\":\"jwZ0G6YALQ4N8RGNHzJHIgX6j+I=\",\"id\":\"42\",\"size\":42}}],\"identifier\":[{\"assigner\":{\"reference\":\"partnerId\"},\"value\":\"d4l_f_p_t#42\"}],\"resourceType\":\"DocumentReference\"}"

        runFhirFlow(tags, annotations)

        // When
        val result = recordService.fetchFhir3Record<Fhir3Resource>(USER_ID, RECORD_ID).blockingGet()

        // Then
        Truth.assertThat(result).isInstanceOf(Record::class.java)
        Truth.assertThat(result.resource).isInstanceOf(Fhir3Resource::class.java)
        Truth.assertThat(result.annotations).isEqualTo(listOf("wow", "it", "works"))
    }

    @Test
    fun `Given, fetchFhir4Record is called, with its appropriate payloads, it returns a Fhir4Record`() {
        // Given
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
        encryptedRecord = EncryptedRecord(
                commonKeyId,
                RECORD_ID,
                listOf("cGFydG5lcj1iNDY=", "Y2xpZW50PWI0NiUyM3Rlc3Q=", "ZmhpcnZlcnNpb249NCUyZTAlMmUx", "cmVzb3VyY2V0eXBlPWRvY3VtZW50cmVmZXJlbmNl"),
                encryptedBody,
                CREATION_DATE,
                encryptedDataKey,
                encryptedAttachmentKey,
                ModelVersion.CURRENT,
                UPDATE_DATE
        )

        stringifiedResource = "{\"content\":[{\"attachment\":{\"hash\":\"jwZ0G6YALQ4N8RGNHzJHIgX6j+I=\",\"id\":\"42\",\"size\":42}}],\"identifier\":[{\"assigner\":{\"reference\":\"partnerId\"},\"value\":\"d4l_f_p_t#42\"}],\"resourceType\":\"DocumentReference\"}"

        runFhirFlow(tags)

        // When
        val result = recordService.fetchFhir4Record<Fhir4Resource>(USER_ID, RECORD_ID).blockingGet()

        // Then
        Truth.assertThat(result).isInstanceOf(Fhir4Record::class.java)
        Truth.assertThat(result.resource).isInstanceOf(Fhir4Resource::class.java)
    }

    @Test
    fun `Given, fetchFhir4Record is called, with its appropriate payloads, it returns a Fhir4Record with Annotations`() {
        // Given
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
                ModelVersion.CURRENT,
                UPDATE_DATE
        )

        stringifiedResource = "{\"content\":[{\"attachment\":{\"hash\":\"jwZ0G6YALQ4N8RGNHzJHIgX6j+I=\",\"id\":\"42\",\"size\":42}}],\"identifier\":[{\"assigner\":{\"reference\":\"partnerId\"},\"value\":\"d4l_f_p_t#42\"}],\"resourceType\":\"DocumentReference\"}"

        runFhirFlow(tags, annotations)

        // When
        val result = recordService.fetchFhir4Record<Fhir4Resource>(USER_ID, RECORD_ID).blockingGet()

        // Then
        Truth.assertThat(result).isInstanceOf(Fhir4Record::class.java)
        Truth.assertThat(result.resource).isInstanceOf(Fhir4Resource::class.java)
        Truth.assertThat(result.annotations).isEqualTo(listOf("wow", "it", "works"))
    }

    @Test
    fun `Given, fetchDataRecord is called, with its appropriate payloads, it returns a DataRecord`() {
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
                listOf(
                        "cGFydG5lcj1iNDY=",
                        "Y2xpZW50PWI0NiUyM3Rlc3Q=",
                        "ZmxhZz1hcHBkYXRh"
                ),
                encryptedBody,
                CREATION_DATE,
                encryptedDataKey,
                null,
                ModelVersion.CURRENT,
                UPDATE_DATE
        )

        runDataFlow(resource, encryptedResource, tags)

        // When
        val result = recordService.fetchDataRecord(USER_ID, RECORD_ID).blockingGet()

        // Then
        Truth.assertThat(result).isInstanceOf(DataRecord::class.java)
        Truth.assertThat(result.resource).isInstanceOf(DataResource::class.java)
        Truth.assertThat(resource.value).isEqualTo(resource.value)
    }

    @Test
    fun `Given, fetchDataRecord is called, with its appropriate payloads, it returns a DataRecord, with Annotations`() {
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
                ModelVersion.CURRENT,
                UPDATE_DATE
        )

        runDataFlow(resource, encryptedResource, tags, annotations)

        // When
        val result = recordService.fetchDataRecord(USER_ID, RECORD_ID).blockingGet()

        // Then
        Truth.assertThat(result).isInstanceOf(DataRecord::class.java)
        Truth.assertThat(result.resource).isInstanceOf(DataResource::class.java)
        Truth.assertThat(resource.value).isEqualTo(resource.value)
    }

    // batch API
    @Test
    fun `Given, fetchFhir3Records is called, with its appropriate payloads, it returns a List of Records`() {
        // Given
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

        val encodedEncyrptedResourceType = "cmVzb3VyY2V0eXBlPWRvY3VtZW50cmVmZXJlbmNl"
        val encodedEncryptedVersion = "ZmhpcnZlcnNpb249MyUyZTAlMmUx"

        encryptedBody = "ZW5jcnlwdGVk"
        encryptedBody2 = "Wlc1amNubHdkR1Zr"

        encryptedRecord = EncryptedRecord(
                commonKeyId,
                RECORD_ID,
                listOf("cGFydG5lcj1iNDY=", "Y2xpZW50PWI0NiUyM3Rlc3Q=", encodedEncryptedVersion, encodedEncyrptedResourceType),
                encryptedBody,
                CREATION_DATE,
                encryptedDataKey,
                encryptedAttachmentKey,
                ModelVersion.CURRENT,
                UPDATE_DATE
        )

        encryptedRecord2 = EncryptedRecord(
                commonKeyId,
                RECORD_ID,
                listOf("cGFydG5lcj1iNDY=", "Y2xpZW50PWI0NiUyM3Rlc3Q=", encodedEncryptedVersion, encodedEncyrptedResourceType),
                encryptedBody2,
                CREATION_DATE,
                encryptedDataKey,
                encryptedAttachmentKey,
                ModelVersion.CURRENT,
                UPDATE_DATE
        )

        stringifiedResource = "{\"content\":[{\"attachment\":{\"hash\":\"jwZ0G6YALQ4N8RGNHzJHIgX6j+I=\",\"id\":\"42\",\"size\":42}}],\"identifier\":[{\"assigner\":{\"reference\":\"partnerId\"},\"value\":\"d4l_f_p_t#42\"}],\"resourceType\":\"DocumentReference\"}"
        stringifiedResource2 = "{\"content\":[{\"attachment\":{\"hash\":\"jwZ0G6YALQ4N8RGNHzJHIgX6j+I=\",\"id\":\"attachmentId#previewId#thumbnailId\",\"size\":42}}],\"resourceType\":\"DocumentReference\"}"

        runFhirFlowBatch(
                tags,
                encryptedTags = listOf(encodedEncryptedVersion, encodedEncyrptedResourceType)
        )

        // When
        val result = recordService.fetchFhir3Records(
                USER_ID,
                Fhir3DocumentReference::class.java,
                listOf(),
                null,
                null,
                42,
                23
        ).blockingGet()

        // Then
        Truth.assertThat(result).hasSize(2)
        Truth.assertThat(result[0].resource).isInstanceOf(Fhir3Resource::class.java)
        Truth.assertThat(result[1].resource).isInstanceOf(Fhir3Resource::class.java)
        Truth.assertThat(result[0].annotations).isEmpty()
        Truth.assertThat(result[1].annotations).isEmpty()
    }

    @Test
    fun `Given, fetchFhir3Records is called, with its appropriate payloads, it returns a List of Records filtered by Annotations`() {
        // Given
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

        val encodedEncyrptedResourceType = "cmVzb3VyY2V0eXBlPWRvY3VtZW50cmVmZXJlbmNl"
        val encodedEncryptedVersion = "ZmhpcnZlcnNpb249MyUyZTAlMmUx"

        val encryptedTags = mutableListOf(
                encodedEncryptedVersion,
                encodedEncyrptedResourceType,
                "Y3VzdG9tPXdvdw==",
                "Y3VzdG9tPWl0",
                "Y3VzdG9tPXdvcmtz"
        )

        encryptedBody = "ZW5jcnlwdGVk"
        encryptedBody2 = "Wlc1amNubHdkR1Zr"

        encryptedRecord = EncryptedRecord(
                commonKeyId,
                RECORD_ID,
                listOf(
                        "cGFydG5lcj1iNDY=",
                        "Y2xpZW50PWI0NiUyM3Rlc3Q=",
                        encodedEncyrptedResourceType,
                        encodedEncryptedVersion,
                        "Y3VzdG9tPXdvdw==",
                        "Y3VzdG9tPWl0",
                        "Y3VzdG9tPXdvcmtz"
                ),
                encryptedBody,
                CREATION_DATE,
                encryptedDataKey,
                encryptedAttachmentKey,
                ModelVersion.CURRENT,
                UPDATE_DATE
        )

        encryptedRecord2 = EncryptedRecord(
                commonKeyId,
                RECORD_ID,
                listOf(
                        "cGFydG5lcj1iNDY=",
                        "Y2xpZW50PWI0NiUyM3Rlc3Q=",
                        "ZmhpcnZlcnNpb249MyUyZTAlMmUx",
                        encodedEncyrptedResourceType,
                        encodedEncryptedVersion
                ),
                encryptedBody2,
                CREATION_DATE,
                encryptedDataKey,
                encryptedAttachmentKey,
                ModelVersion.CURRENT,
                UPDATE_DATE
        )

        stringifiedResource = "{\"content\":[{\"attachment\":{\"hash\":\"jwZ0G6YALQ4N8RGNHzJHIgX6j+I=\",\"id\":\"42\",\"size\":42}}],\"identifier\":[{\"assigner\":{\"reference\":\"partnerId\"},\"value\":\"d4l_f_p_t#42\"}],\"resourceType\":\"DocumentReference\"}"
        stringifiedResource2 = "{\"content\":[{\"attachment\":{\"hash\":\"jwZ0G6YALQ4N8RGNHzJHIgX6j+I=\",\"id\":\"attachmentId#previewId#thumbnailId\",\"size\":42}}],\"resourceType\":\"DocumentReference\"}"

        runFhirFlowBatch(tags, annotations, encryptedTags)

        // When
        val result = recordService.fetchFhir3Records(
                USER_ID,
                Fhir3DocumentReference::class.java,
                listOf("wow", "it", "works"),
                null,
                null,
                PAGE_SIZE,
                OFFSET
        ).blockingGet()

        // Then
        Truth.assertThat(result).hasSize(1)
        Truth.assertThat(result[0].resource).isInstanceOf(Fhir3Resource::class.java)
        Truth.assertThat(result[0].annotations).isEqualTo(listOf("wow", "it", "works"))
    }

    @Test
    fun `Given, fetchFhir4Records is called, with its appropriate payloads, it returns a List of Fhir4Records`() {
        // Given
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

        val encodedEncyrptedResourceType = "cmVzb3VyY2V0eXBlPWRvY3VtZW50cmVmZXJlbmNl"
        val encodedEncryptedVersion = "ZmhpcnZlcnNpb249NCUyZTAlMmUx"

        encryptedBody = "ZW5jcnlwdGVk"
        encryptedBody2 = "Wlc1amNubHdkR1Zr"

        encryptedRecord = EncryptedRecord(
                commonKeyId,
                RECORD_ID,
                listOf(
                        "cGFydG5lcj1iNDY=",
                        "Y2xpZW50PWI0NiUyM3Rlc3Q=",
                        encodedEncryptedVersion,
                        encodedEncyrptedResourceType
                ),
                encryptedBody,
                CREATION_DATE,
                encryptedDataKey,
                encryptedAttachmentKey,
                ModelVersion.CURRENT,
                UPDATE_DATE
        )

        encryptedRecord2 = EncryptedRecord(
                commonKeyId,
                RECORD_ID,
                listOf(
                        "cGFydG5lcj1iNDY=",
                        "Y2xpZW50PWI0NiUyM3Rlc3Q=",
                        encodedEncryptedVersion,
                        encodedEncyrptedResourceType
                ),
                encryptedBody2,
                CREATION_DATE,
                encryptedDataKey,
                encryptedAttachmentKey,
                ModelVersion.CURRENT,
                UPDATE_DATE
        )

        stringifiedResource = "{\"content\":[{\"attachment\":{\"hash\":\"jwZ0G6YALQ4N8RGNHzJHIgX6j+I=\",\"id\":\"42\",\"size\":42}}],\"identifier\":[{\"assigner\":{\"reference\":\"partnerId\"},\"value\":\"d4l_f_p_t#42\"}],\"resourceType\":\"DocumentReference\"}"
        stringifiedResource2 = "{\"content\":[{\"attachment\":{\"hash\":\"jwZ0G6YALQ4N8RGNHzJHIgX6j+I=\",\"id\":\"attachmentId#previewId#thumbnailId\",\"size\":42}}],\"resourceType\":\"DocumentReference\"}"

        runFhirFlowBatch(tags, encryptedTags = listOf(encodedEncryptedVersion, encodedEncyrptedResourceType))

        // When
        val result = recordService.fetchFhir4Records(
                USER_ID,
                Fhir4DocumentReference::class.java,
                listOf(),
                null,
                null,
                42,
                23
        ).blockingGet()

        // Then
        Truth.assertThat(result).hasSize(2)
        Truth.assertThat(result[0].resource).isInstanceOf(Fhir4Resource::class.java)
        Truth.assertThat(result[1].resource).isInstanceOf(Fhir4Resource::class.java)
        Truth.assertThat(result[0].annotations).isEmpty()
        Truth.assertThat(result[1].annotations).isEmpty()
    }

    @Test
    fun `Given, fetchFhir4Records is called, with its appropriate payloads, it returns a List of Fhir4Records filtered by Annotations`() {
        // Given
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

        val encodedEncyrptedResourceType = "cmVzb3VyY2V0eXBlPWRvY3VtZW50cmVmZXJlbmNl"
        val encodedEncryptedVersion = "ZmhpcnZlcnNpb249NCUyZTAlMmUx"

        val encryptedTags = mutableListOf(
                encodedEncryptedVersion,
                encodedEncyrptedResourceType,
                "Y3VzdG9tPXdvdw==",
                "Y3VzdG9tPWl0",
                "Y3VzdG9tPXdvcmtz"
        )

        encryptedBody = "ZW5jcnlwdGVk"
        encryptedBody2 = "Wlc1amNubHdkR1Zr"

        encryptedRecord = EncryptedRecord(
                commonKeyId,
                RECORD_ID,
                listOf(
                        "cGFydG5lcj1iNDY=",
                        "Y2xpZW50PWI0NiUyM3Rlc3Q=",
                        encodedEncryptedVersion,
                        encodedEncyrptedResourceType,
                        "Y3VzdG9tPXdvdw==",
                        "Y3VzdG9tPWl0",
                        "Y3VzdG9tPXdvcmtz"
                ),
                encryptedBody,
                CREATION_DATE,
                encryptedDataKey,
                encryptedAttachmentKey,
                ModelVersion.CURRENT,
                UPDATE_DATE
        )

        encryptedRecord2 = EncryptedRecord(
                commonKeyId,
                RECORD_ID,
                listOf(
                        "cGFydG5lcj1iNDY=",
                        "Y2xpZW50PWI0NiUyM3Rlc3Q=",
                        encodedEncryptedVersion,
                        encodedEncyrptedResourceType
                ),
                encryptedBody2,
                CREATION_DATE,
                encryptedDataKey,
                encryptedAttachmentKey,
                ModelVersion.CURRENT,
                UPDATE_DATE
        )

        stringifiedResource = "{\"content\":[{\"attachment\":{\"hash\":\"jwZ0G6YALQ4N8RGNHzJHIgX6j+I=\",\"id\":\"42\",\"size\":42}}],\"identifier\":[{\"assigner\":{\"reference\":\"partnerId\"},\"value\":\"d4l_f_p_t#42\"}],\"resourceType\":\"DocumentReference\"}"
        stringifiedResource2 = "{\"content\":[{\"attachment\":{\"hash\":\"jwZ0G6YALQ4N8RGNHzJHIgX6j+I=\",\"id\":\"attachmentId#previewId#thumbnailId\",\"size\":42}}],\"resourceType\":\"DocumentReference\"}"

        runFhirFlowBatch(tags, annotations, encryptedTags)

        // When
        val result = recordService.fetchFhir4Records(
                USER_ID,
                Fhir4DocumentReference::class.java,
                listOf("wow", "it", "works"),
                null,
                null,
                PAGE_SIZE,
                OFFSET
        ).blockingGet()

        // Then
        Truth.assertThat(result).hasSize(1)
        Truth.assertThat(result[0].resource).isInstanceOf(Fhir4Resource::class.java)
        Truth.assertThat(result[0].annotations).isEqualTo(listOf("wow", "it", "works"))
    }

    @Test
    fun `Given, fetchFhirDataRecords is called, with its appropriate payloads, it returns a List of DataRecords`() {
        // Given
        val resource1 = DataResource("I am a new test".toByteArray())
        val resource2 = DataResource("I am a second test".toByteArray())

        val tags = mapOf(
                "partner" to "partner=$PARTNER_ID".toLowerCase(),
                "client" to "client=${
                    URLEncoder.encode(CLIENT_ID.toLowerCase(), StandardCharsets.UTF_8.displayName())
                }",
                "flag" to "flag=appdata"
        )

        val encryptedResourceType = "ZmxhZz1hcHBkYXRh"

        val encryptedResource1 = "I am a new test".toByteArray()
        val encryptedResource2 = "I am a second test".toByteArray()

        encryptedBody = "SSBhbSBhIG5ldyB0ZXN0"
        encryptedBody2 = "SSBhbSBhIHNlY29uZCB0ZXN0"

        encryptedRecord = EncryptedRecord(
                commonKeyId,
                RECORD_ID,
                listOf(
                        "cGFydG5lcj1iNDY=", "Y2xpZW50PWI0NiUyM3Rlc3Q=",
                        encryptedResourceType
                ),
                encryptedBody,
                CREATION_DATE,
                encryptedDataKey,
                null,
                ModelVersion.CURRENT,
                UPDATE_DATE
        )

        encryptedRecord2 = EncryptedRecord(
                commonKeyId,
                RECORD_ID,
                listOf(
                        "cGFydG5lcj1iNDY=",
                        "Y2xpZW50PWI0NiUyM3Rlc3Q=",
                        encryptedResourceType
                ),
                encryptedBody2,
                CREATION_DATE,
                encryptedDataKey,
                null,
                ModelVersion.CURRENT,
                UPDATE_DATE
        )

        // encrypt tags
        every { cryptoService.fetchTagEncryptionKey() } returns tagEncryptionKey
        every {
            cryptoService.symEncrypt(tagEncryptionKey, eq(
                    tags["flag"]!!.toByteArray()
            ), IV)
        } returns tags["flag"]!!.toByteArray()

        every {
            apiService.fetchRecords(
                    ALIAS,
                    USER_ID,
                    null,
                    null,
                    PAGE_SIZE,
                    OFFSET,
                    listOf(encryptedResourceType)
            )
        } returns Observable.fromArray(
                arrayListOf(
                        encryptedRecord,
                        encryptedRecord2
                )
        )

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

        // decrypt body
        // fetch common key
        every { cryptoService.hasCommonKey(commonKeyId) } returns true
        every { cryptoService.getCommonKeyById(commonKeyId) } returns commonKey


        // decrypt Resource
        every { cryptoService.symDecryptSymmetricKey(commonKey, encryptedDataKey) } returns Single.just(dataKey)
        every { cryptoService.decrypt(dataKey, encryptedResource1) } returns Single.just(resource1.value)
        every { cryptoService.decrypt(dataKey, encryptedResource2) } returns Single.just(resource2.value)

        // When
        val result = recordService.fetchDataRecords(
                USER_ID,
                listOf(),
                null,
                null,
                PAGE_SIZE,
                OFFSET
        ).blockingGet()

        // Then
        Truth.assertThat(result).hasSize(2)
        Truth.assertThat(result[0].resource).isInstanceOf(DataResource::class.java)
        Truth.assertThat(result[1].resource).isInstanceOf(DataResource::class.java)
        Truth.assertThat(result[0].resource).isEqualTo(resource1)
        Truth.assertThat(result[1].resource).isEqualTo(resource2)
        Truth.assertThat(result[0].annotations).isEmpty()
        Truth.assertThat(result[1].annotations).isEmpty()
    }

    @Test
    fun `Given, fetchFhirDataRecords is called, with its appropriate payloads, it returns a List of DataRecords filtered by Annotations`() {
        // Given
        val resource1 = DataResource("I am a new test".toByteArray())
        val resource2 = DataResource("I am a second test".toByteArray())

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

        val encryptedResourceType = "ZmxhZz1hcHBkYXRh"

        val encryptedResource1 = "I am a new test".toByteArray()
        val encryptedResource2 = "I am a second test".toByteArray()

        encryptedBody = "SSBhbSBhIG5ldyB0ZXN0"
        encryptedBody2 = "SSBhbSBhIHNlY29uZCB0ZXN0"

        encryptedRecord = EncryptedRecord(
                commonKeyId,
                RECORD_ID,
                listOf(
                        "cGFydG5lcj1iNDY=",
                        "Y2xpZW50PWI0NiUyM3Rlc3Q=",
                        encryptedResourceType
                ),
                encryptedBody,
                CREATION_DATE,
                encryptedDataKey,
                null,
                ModelVersion.CURRENT,
                UPDATE_DATE
        )

        encryptedRecord2 = EncryptedRecord(
                commonKeyId,
                RECORD_ID,
                listOf(
                        "cGFydG5lcj1iNDY=", "Y2xpZW50PWI0NiUyM3Rlc3Q=",
                        encryptedResourceType,
                        "Y3VzdG9tPXdvdw==",
                        "Y3VzdG9tPWl0",
                        "Y3VzdG9tPXdvcmtz"
                ),
                encryptedBody2,
                CREATION_DATE,
                encryptedDataKey,
                null,
                ModelVersion.CURRENT,
                UPDATE_DATE
        )

        val encryptedTags = listOf(
                encryptedResourceType,
                "Y3VzdG9tPXdvdw==",
                "Y3VzdG9tPWl0",
                "Y3VzdG9tPXdvcmtz"
        )

        runDataFlowBatch(
                resource1.value,
                resource2.value,
                encryptedResource1,
                encryptedResource2,
                tags,
                annotations,
                encryptedTags
        )

        // When
        val result = recordService.fetchDataRecords(
                USER_ID,
                listOf("wow", "it", "works"),
                null,
                null,
                PAGE_SIZE,
                OFFSET
        ).blockingGet()

        // Then
        Truth.assertThat(result).hasSize(1)
        Truth.assertThat(result[0].resource).isInstanceOf(DataResource::class.java)
        Truth.assertThat(result[0].resource).isEqualTo(resource2)
        Truth.assertThat(result[0].annotations).isEqualTo(listOf("wow", "it", "works"))
    }

    // Compatibility
    @Test
    fun `Given, fetchFhir4Record is called, with its appropriate payloads, but it fetches a Fhir3Record, it fails with`() {
        // Given
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
                ModelVersion.CURRENT,
                UPDATE_DATE
        )

        stringifiedResource = "{\"content\":[{\"attachment\":{\"hash\":\"jwZ0G6YALQ4N8RGNHzJHIgX6j+I=\",\"id\":\"42\",\"size\":42}}],\"identifier\":[{\"assigner\":{\"reference\":\"partnerId\"},\"value\":\"d4l_f_p_t#42\"}],\"resourceType\":\"DocumentReference\"}"

        runFhirFlow(tags, annotations)

        // When
        try {
            recordService.fetchFhir4Record<Fhir4Resource>(USER_ID, RECORD_ID).blockingGet()
        } catch (e: Exception) {
            // Then
            Truth.assertThat(e).isInstanceOf(ClassCastException::class.java)
        }
    }

    // ToDo: Case any Fhir<->Data
    // ToDo: Cases for fetch**Records
}
