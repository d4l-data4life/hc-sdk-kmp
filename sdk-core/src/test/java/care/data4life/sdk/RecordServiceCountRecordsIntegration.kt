/*
 * Copyright (c) 2021 D4L data4life gGmbH / All rights reserved.
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
import care.data4life.sdk.fhir.FhirService
import care.data4life.sdk.tag.TagEncryptionService
import care.data4life.sdk.tag.TaggingService
import com.google.common.truth.Truth
import io.mockk.every
import io.mockk.mockk
import io.reactivex.Single
import org.junit.Before
import org.junit.Test
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import care.data4life.fhir.r4.model.DocumentReference as Fhir4Reference
import care.data4life.fhir.stu3.model.DocumentReference as Fhir3Reference

class RecordServiceCountRecordsIntegration : RecordServiceIntegrationBase() {
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
                TagEncryptionService(cryptoService),
                TaggingService(CLIENT_ID),
                FhirService(cryptoService),
                AttachmentService(fileService, imageResizer),
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

    private fun encryptTagsAndAnnotations(
            tags: Map<String, String>,
            annotations: Map<String, String> = mapOf()
    ) {
        // encrypt tags
        every { cryptoService.fetchTagEncryptionKey() } returns tagEncryptionKey
        every {
            cryptoService.symEncrypt(
                    tagEncryptionKey, eq(
                    tags["resourcetype"]!!.toByteArray()
            ), IV
            )
        } returns tags["resourcetype"]!!.toByteArray()
        every {
            cryptoService.symEncrypt(
                    tagEncryptionKey, eq(
                    tags["fhirversion"]!!.toByteArray()
            ), IV
            )
        } returns tags["fhirversion"]!!.toByteArray()
        every {
            cryptoService.symEncrypt(
                    tagEncryptionKey, eq(
                    tags["legecyfhirversion"]!!.toByteArray()
            ), IV
            )
        } returns tags["legecyfhirversion"]!!.toByteArray()

        // encrypt annotations
        if (annotations.isNotEmpty()) {
            every {
                cryptoService.symEncrypt(
                        tagEncryptionKey, eq(
                        annotations["wow"]!!.toByteArray()
                ), IV
                )
            } returns annotations["wow"]!!.toByteArray()
            every {
                cryptoService.symEncrypt(
                        tagEncryptionKey, eq(
                        annotations["it"]!!.toByteArray()
                ), IV
                )
            } returns annotations["it"]!!.toByteArray()
            every {
                cryptoService.symEncrypt(
                        tagEncryptionKey, eq(
                        annotations["works"]!!.toByteArray()
                ), IV
                )
            } returns annotations["works"]!!.toByteArray()
        }
    }

    @Test
    fun `Given, countRecords is called with a type and UserId, it returns the amount of Records`() {
        val encodedEncryptedVersion = "ZmhpcnZlcnNpb249MyUyZTAlMmUx"
        val encryptedVersion = "ZmhpcnZlcnNpb249My4wLjE="
        val encodedEncryptedResourceType = "cmVzb3VyY2V0eXBlPWRvY3VtZW50cmVmZXJlbmNl"

        val tags = mapOf(
                "partner" to "partner=${PARTNER_ID}".toLowerCase(),
                "client" to "client=${
                    URLEncoder.encode(CLIENT_ID.toLowerCase(), StandardCharsets.UTF_8.displayName())
                }",
                "fhirversion" to "fhirversion=${
                    "3.0.1".replace(".", "%2e")
                }",
                "legecyfhirversion" to "fhirversion=3.0.1",
                "resourcetype" to "resourcetype=documentreference"
        )

        encryptTagsAndAnnotations(tags)

        every {
            apiService.getCount(
                    ALIAS,
                    USER_ID,
                    listOf(encodedEncryptedVersion, encodedEncryptedResourceType)
            )
        } returns Single.just(21)

        every {
            apiService.getCount(
                    ALIAS,
                    USER_ID,
                    listOf(encryptedVersion, encodedEncryptedResourceType)
            )
        } returns Single.just(21)

        // When
        val result = (recordService as RecordService).countRecords(
                Fhir3Reference::class.java,
                USER_ID
        ).blockingGet()

        // Then
        Truth.assertThat(result).isEqualTo(42)
    }

    @Test
    fun `Given, countRecords is called without a type, UserId and Annotations, it returns the amount of Records`() {
        val encodedEncryptedVersion = "ZmhpcnZlcnNpb249MyUyZTAlMmUx"
        val encryptedVersion = "ZmhpcnZlcnNpb249My4wLjE="

        val annotations = mapOf(
                "wow" to "custom=wow",
                "it" to "custom=it",
                "works" to "custom=works"
        )

        val tags = mapOf(
                "partner" to "partner=${PARTNER_ID}".toLowerCase(),
                "client" to "client=${
                    URLEncoder.encode(CLIENT_ID.toLowerCase(), StandardCharsets.UTF_8.displayName())
                }",
                "fhirversion" to "fhirversion=${
                    "3.0.1".replace(".", "%2e")
                }",
                "legecyfhirversion" to "fhirversion=3.0.1",
                "resourcetype" to "resourcetype=documentreference"
        )

        encryptTagsAndAnnotations(tags, annotations)

        every {
            apiService.getCount(
                    ALIAS,
                    USER_ID,
                    listOf(
                            encodedEncryptedVersion,
                            "Y3VzdG9tPXdvdw==",
                            "Y3VzdG9tPWl0",
                            "Y3VzdG9tPXdvcmtz"
                    )
            )
        } returns Single.just(21)
        every {
            apiService.getCount(
                    ALIAS,
                    USER_ID,
                    listOf(
                            encryptedVersion,
                            "Y3VzdG9tPXdvdw==",
                            "Y3VzdG9tPWl0",
                            "Y3VzdG9tPXdvcmtz"
                    )
            )
        } returns Single.just(21)

        // When
        val result = (recordService as RecordService).countRecords(
                null,
                USER_ID,
                annotations.keys.toList()
        ).blockingGet()

        // Then
        Truth.assertThat(result).isEqualTo(42)
    }

    @Test
    fun `Given, countFhir3Records is called with a type, UserId and Annotations, it returns the amount of Records`() {
        val encodedEncryptedVersion = "ZmhpcnZlcnNpb249MyUyZTAlMmUx"
        val encryptedVersion = "ZmhpcnZlcnNpb249My4wLjE="
        val encodedEncryptedResourceType = "cmVzb3VyY2V0eXBlPWRvY3VtZW50cmVmZXJlbmNl"

        val annotations = mapOf(
                "wow" to "custom=wow",
                "it" to "custom=it",
                "works" to "custom=works"
        )

        val tags = mapOf(
                "partner" to "partner=${PARTNER_ID}".toLowerCase(),
                "client" to "client=${
                    URLEncoder.encode(CLIENT_ID.toLowerCase(), StandardCharsets.UTF_8.displayName())
                }",
                "fhirversion" to "fhirversion=${
                    "3.0.1".replace(".", "%2e")
                }",
                "legecyfhirversion" to "fhirversion=3.0.1",
                "resourcetype" to "resourcetype=documentreference"
        )

        encryptTagsAndAnnotations(tags, annotations)

        every {
            apiService.getCount(
                    ALIAS,
                    USER_ID,
                    listOf(
                            encodedEncryptedVersion,
                            encodedEncryptedResourceType,
                            "Y3VzdG9tPXdvdw==",
                            "Y3VzdG9tPWl0",
                            "Y3VzdG9tPXdvcmtz"
                    )
            )
        } returns Single.just(21)
        every {
            apiService.getCount(
                    ALIAS,
                    USER_ID,
                    listOf(
                            encryptedVersion,
                            encodedEncryptedResourceType,
                            "Y3VzdG9tPXdvdw==",
                            "Y3VzdG9tPWl0",
                            "Y3VzdG9tPXdvcmtz"
                    )
            )
        } returns Single.just(21)

        // When
        val result = (recordService as RecordService).countFhir3Records(
                Fhir3Reference::class.java,
                USER_ID,
                annotations.keys.toList()
        ).blockingGet()

        // Then
        Truth.assertThat(result).isEqualTo(42)
    }

    @Test
    fun `Given, countFhir4Records is called with a type, UserId and Annotations, it returns the amount of Records`() {
        val encodedEncryptedVersion = "ZmhpcnZlcnNpb249NCUyZTAlMmUx"
        val encryptedVersion = "ZmhpcnZlcnNpb249NC4wLjE="
        val encodedEncryptedResourceType = "cmVzb3VyY2V0eXBlPWRvY3VtZW50cmVmZXJlbmNl"

        val annotations = mapOf(
                "wow" to "custom=wow",
                "it" to "custom=it",
                "works" to "custom=works"
        )

        val tags = mapOf(
                "partner" to "partner=${PARTNER_ID}".toLowerCase(),
                "client" to "client=${
                    URLEncoder.encode(CLIENT_ID.toLowerCase(), StandardCharsets.UTF_8.displayName())
                }",
                "fhirversion" to "fhirversion=${
                    "4.0.1".replace(".", "%2e")
                }",
                "legecyfhirversion" to "fhirversion=4.0.1",
                "resourcetype" to "resourcetype=documentreference"
        )

        encryptTagsAndAnnotations(tags, annotations)

        every {
            apiService.getCount(
                    ALIAS,
                    USER_ID,
                    listOf(
                            encodedEncryptedVersion,
                            encodedEncryptedResourceType,
                            "Y3VzdG9tPXdvdw==",
                            "Y3VzdG9tPWl0",
                            "Y3VzdG9tPXdvcmtz"
                    )
            )
        } returns Single.just(21)
        every {
            apiService.getCount(
                    ALIAS,
                    USER_ID,
                    listOf(
                            encryptedVersion,
                            encodedEncryptedResourceType,
                            "Y3VzdG9tPXdvdw==",
                            "Y3VzdG9tPWl0",
                            "Y3VzdG9tPXdvcmtz"
                    )
            )
        } returns Single.just(21)

        // When
        val result = (recordService as RecordService).countFhir4Records(
                Fhir4Reference::class.java,
                USER_ID,
                annotations.keys.toList()
        ).blockingGet()

        // Then
        Truth.assertThat(result).isEqualTo(42)
    }

    @Test
    fun `Given, countAllFhir3Records is called with a UserId and Annotations, it returns the amount of Records`() {
        val encodedEncryptedVersion = "ZmhpcnZlcnNpb249MyUyZTAlMmUx"
        val encryptedVersion = "ZmhpcnZlcnNpb249My4wLjE="

        val annotations = mapOf(
                "wow" to "custom=wow",
                "it" to "custom=it",
                "works" to "custom=works"
        )

        val tags = mapOf(
                "partner" to "partner=${PARTNER_ID}".toLowerCase(),
                "client" to "client=${
                    URLEncoder.encode(CLIENT_ID.toLowerCase(), StandardCharsets.UTF_8.displayName())
                }",
                "fhirversion" to "fhirversion=${
                    "3.0.1".replace(".", "%2e")
                }",
                "legecyfhirversion" to "fhirversion=3.0.1",
                "resourcetype" to "resourcetype=documentreference"
        )

        encryptTagsAndAnnotations(tags, annotations)

        every {
            apiService.getCount(
                    ALIAS,
                    USER_ID,
                    listOf(
                            encodedEncryptedVersion,
                            "Y3VzdG9tPXdvdw==",
                            "Y3VzdG9tPWl0",
                            "Y3VzdG9tPXdvcmtz"
                    )
            )
        } returns Single.just(21)
        every {
            apiService.getCount(
                    ALIAS,
                    USER_ID,
                    listOf(
                            encryptedVersion,
                            "Y3VzdG9tPXdvdw==",
                            "Y3VzdG9tPWl0",
                            "Y3VzdG9tPXdvcmtz"
                    )
            )
        } returns Single.just(21)

        // When
        val result = (recordService as RecordService).countAllFhir3Records(
                USER_ID,
                annotations.keys.toList()
        ).blockingGet()

        // Then
        Truth.assertThat(result).isEqualTo(42)
    }
}
