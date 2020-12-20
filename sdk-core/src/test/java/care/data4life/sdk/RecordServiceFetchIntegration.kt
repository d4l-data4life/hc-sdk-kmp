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
import care.data4life.sdk.util.MimeType
import com.google.common.truth.Truth
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.reactivex.Single
import org.junit.Before
import org.junit.Test


class RecordServiceFetchIntegration: RecordServiceIntegrationBase() {
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
        tagEncryptionKey = mockk()
        commonKey = mockk()
        encryptedDataKey = mockk()
        encryptedAttachmentKey = mockk()
    }

    private fun runFhirFlow(
            tags: Map<String, String>
    ) {
        every { apiService.fetchRecord(ALIAS, USER_ID, RECORD_ID) } returns  Single.just(encryptedRecord)

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

        // decrypt Resource
        every { cryptoService.hasCommonKey(commonKeyId) } returns true
        every { cryptoService.getCommonKeyById(commonKeyId) } returns commonKey
        every { cryptoService.symDecryptSymmetricKey(commonKey, encryptedAttachmentKey) } returns Single.just(attachmentKey)
        every { cryptoService.symDecryptSymmetricKey(commonKey, encryptedDataKey) } returns Single.just(dataKey)
        every { cryptoService.decryptString(dataKey, encryptedBody) } returns Single.just(stringifiedResource)
    }

    @Test
    fun `Given, fetchFhir3Record is called, with its appropriate payloads, it returns a Record`() {
        // Given
        val tags = mapOf(
                "partner" to "partner=TEST",
                "client" to "client=TEST",
                "fhirversion" to "fhirversion=3.0.1",
                "resourcetype" to "resourcetype=documentreference"
        )

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

        stringifiedResource = "{\"content\":[{\"attachment\":{\"hash\":\"jwZ0G6YALQ4N8RGNHzJHIgX6j+I=\",\"id\":\"42\",\"size\":42}}],\"identifier\":[{\"assigner\":{\"reference\":\"partnerId\"},\"value\":\"d4l_f_p_t#42\"}],\"resourceType\":\"DocumentReference\"}"

        runFhirFlow(tags)

        // When
        val result = recordService.fetchFhir3Record<Fhir3Resource>(USER_ID, RECORD_ID).blockingGet()

        // Then
        Truth.assertThat(result).isInstanceOf(Record::class.java)
        Truth.assertThat(result.resource).isInstanceOf(Fhir3Resource::class.java)
    }

    @Test
    fun `Given, fetchFhir4Record is called, with its appropriate payloads, it returns a Fhir4Record`() {
        // Given
        val tags = mapOf(
                "partner" to "partner=TEST",
                "client" to "client=TEST",
                "fhirversion" to "fhirversion=4.0.1",
                "resourcetype" to "resourcetype=documentreference"
        )

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

        stringifiedResource = "{\"content\":[{\"attachment\":{\"hash\":\"jwZ0G6YALQ4N8RGNHzJHIgX6j+I=\",\"id\":\"42\",\"size\":42}}],\"identifier\":[{\"assigner\":{\"reference\":\"partnerId\"},\"value\":\"d4l_f_p_t#42\"}],\"resourceType\":\"DocumentReference\"}"

        runFhirFlow(tags)

        // When
        val result = recordService.fetchFhir4Record<Fhir4Resource>(USER_ID, RECORD_ID).blockingGet()

        // Then
        Truth.assertThat(result).isInstanceOf(Fhir4Record::class.java)
        Truth.assertThat(result.resource).isInstanceOf(Fhir4Resource::class.java)
    }

    @Test
    fun `Given, fetchDataRecord is called, with its appropriate payloads, it returns a DataRecord`() {
        // Given
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
                listOf(
                        "cGFydG5lcj1URVNU",
                        "Y2xpZW50PVRFU1Q=",
                        "ZmxhZz1hcHBkYXRh"
                ),
                encryptedBody,
                CREATION_DATE,
                encryptedDataKey,
                null,
                ModelVersion.CURRENT
        ).also { it.updatedDate = UPDATE_DATE }

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

        // decrypt Resource
        every { cryptoService.hasCommonKey(commonKeyId) } returns true
        every { cryptoService.getCommonKeyById(commonKeyId) } returns commonKey
        every { cryptoService.symDecryptSymmetricKey(commonKey, encryptedDataKey) } returns Single.just(dataKey)
        every { cryptoService.decrypt(dataKey, encryptedResource) } returns Single.just(resource.value)

        // When
        val result = recordService.fetchDataRecord(USER_ID, RECORD_ID).blockingGet()

        // Then
        Truth.assertThat(result).isInstanceOf(DataRecord::class.java)
        Truth.assertThat(result.resource).isInstanceOf(DataResource::class.java)
        Truth.assertThat(resource.value).isEqualTo(resource.value)
    }
}
