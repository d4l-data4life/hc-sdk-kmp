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

import care.data4life.crypto.GCKey
import care.data4life.sdk.attachment.AttachmentContract
import care.data4life.sdk.attachment.AttachmentService
import care.data4life.sdk.crypto.CryptoContract
import care.data4life.sdk.fhir.ResourceCryptoService
import care.data4life.sdk.network.NetworkingContract
import care.data4life.sdk.network.util.SearchTagsBuilder
import care.data4life.sdk.record.RecordContract
import care.data4life.sdk.tag.Annotations
import care.data4life.sdk.tag.TagCryptoService
import care.data4life.sdk.tag.TaggingService
import care.data4life.sdk.tag.Tags
import care.data4life.sdk.test.fake.CryptoServiceFake
import care.data4life.sdk.test.fake.CryptoServiceIteration
import care.data4life.sdk.test.util.GenericTestDataProvider.ALIAS
import care.data4life.sdk.test.util.GenericTestDataProvider.CLIENT_ID
import care.data4life.sdk.test.util.GenericTestDataProvider.PARTNER_ID
import care.data4life.sdk.test.util.GenericTestDataProvider.USER_ID
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.reactivex.Single
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import care.data4life.fhir.r4.model.DocumentReference as Fhir4Reference
import care.data4life.fhir.stu3.model.DocumentReference as Fhir3Reference

class RecordServiceCountRecordsModuleTest {
    private val tagEncryptionKey: GCKey = mockk()

    private lateinit var recordService: RecordContract.Service
    private lateinit var flowHelper: RecordServiceModuleTestFlowHelper
    private val apiService: NetworkingContract.Service = mockk()
    private lateinit var cryptoService: CryptoContract.Service
    private val fileService: AttachmentContract.FileService = mockk()
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
                fileService,
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

    private fun runFlow(
        tags: Tags,
        annotations: Annotations = emptyList(),
        amount: Int,
        alias: String = ALIAS,
        userId: String = USER_ID,
        tagEncryptionKey: GCKey = this.tagEncryptionKey
    ) {
        val (encodedTags, legacyKMPTags, legacyJSTags) = flowHelper.prepareCompatibilityTags(tags)
        val (encodedAnnotations, legacyKMPAnnotations, legacyJSAnnotations) = flowHelper.prepareCompatibilityAnnotations(
            annotations
        )

        val searchTags = SearchTagsBuilder.newBuilder()
            .let { flowHelper.buildExpectedTagGroups(it, encodedTags, legacyKMPTags, legacyJSTags) }
            .let { flowHelper.buildExpectedTagGroups(it, encodedAnnotations, legacyKMPAnnotations, legacyJSAnnotations) }
            .seal()
            .tags

        val receivedIteration = CryptoServiceIteration(
            gcKeyOrder = emptyList(),
            commonKey = mockk(),
            commonKeyId = "",
            commonKeyIsStored = false,
            commonKeyFetchCalls = 0,
            encryptedCommonKey = mockk(),
            dataKey = mockk(),
            encryptedDataKey = mockk(),
            attachmentKey = null,
            encryptedAttachmentKey = null,
            tagEncryptionKey = tagEncryptionKey,
            tagEncryptionKeyCalls = 1,
            resources = emptyList(),
            tags = flowHelper.mergeTags(encodedTags, legacyKMPTags, legacyJSTags),
            annotations = flowHelper.mergeTags(encodedAnnotations, legacyKMPAnnotations, legacyJSAnnotations),
            hashFunction = { value -> flowHelper.md5(value) }
        )

        (cryptoService as CryptoServiceFake).iteration = receivedIteration

        val search = slot<NetworkingContract.SearchTags>()

        every {
            apiService.countRecords(
                alias,
                userId,
                capture(search)
            )
        } answers {
            val actual = flowHelper.decryptSerializedTags(
                search.captured.tags,
                cryptoService,
                tagEncryptionKey
            )

            if (searchTags == actual) {
                Single.just(amount)
            } else {
                throw RuntimeException(
                    "Unexpected tags and annotations - \nexpected: $searchTags\ngot: $actual"
                )
            }
        }
    }

    // FHIR 3
    @Test
    fun `Given, countRecords is called with a type and UserId, it returns the amount of Fhir3Records`() {
        // Given
        val resourceType = "DocumentReference"
        val tags = mapOf(
            "fhirversion" to "3.0.1",
            "resourcetype" to resourceType
        )

        runFlow(
            tags = tags,
            amount = 42
        )

        // When
        val result = (recordService as RecordService).countRecords(
            Fhir3Reference::class.java,
            USER_ID,
            emptyList()
        ).blockingGet()

        // Then
        assertEquals(
            expected = 42,
            actual = result
        )
    }

    @Test
    fun `Given, countRecords is called with a type, UserId and Annotations, it returns the amount of Fhir3Records`() {
        // Given
        val resourceType = "DocumentReference"
        val tags = mapOf(
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

        runFlow(
            tags = tags,
            annotations = annotations,
            amount = 23
        )

        // When
        val result = (recordService as RecordService).countRecords(
            Fhir3Reference::class.java,
            USER_ID,
            annotations
        ).blockingGet()

        // Then
        assertEquals(
            expected = 23,
            actual = result
        )
    }

    @Test
    fun `Given, countFhir3Records is called with a type and UserId, it returns the amount of Fhir3Records`() {
        // Given
        val resourceType = "DocumentReference"
        val tags = mapOf(
            "fhirversion" to "3.0.1",
            "resourcetype" to resourceType
        )

        runFlow(
            tags = tags,
            amount = 42
        )

        // When
        val result = recordService.countFhir3Records(
            Fhir3Reference::class.java,
            USER_ID,
            emptyList()
        ).blockingGet()

        // Then
        assertEquals(
            expected = 42,
            actual = result
        )
    }

    @Test
    fun `Given, countFhir3Records is called with a type, UserId and Annotations, it returns the amount of Fhir3Records`() {
        // Given
        val resourceType = "DocumentReference"
        val tags = mapOf(
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

        runFlow(
            tags = tags,
            annotations = annotations,
            amount = 23
        )

        // When
        val result = recordService.countFhir3Records(
            Fhir3Reference::class.java,
            USER_ID,
            annotations
        ).blockingGet()

        // Then
        assertEquals(
            expected = 23,
            actual = result
        )
    }

    @Test
    fun `Given, countAllFhir3Records is called with a UserId and Annotations, it returns the amount of Records`() {
        // Given
        val tags = mapOf(
            "fhirversion" to "3.0.1"
        )

        val annotations = listOf(
            "wow",
            "it",
            "works",
            "and",
            "like_a_duracell_häsi"
        )

        runFlow(
            tags = tags,
            annotations = annotations,
            amount = 23
        )

        // When
        val result = recordService.countAllFhir3Records(
            USER_ID,
            annotations
        ).blockingGet()

        // Then
        assertEquals(
            expected = 23,
            actual = result
        )
    }

    // FHIR 4
    @Test
    fun `Given, countFhir4Records is called with a type and UserId, it returns the amount of Fhir4Records`() {
        // Given
        val resourceType = "DocumentReference"
        val tags = mapOf(
            "fhirversion" to "4.0.1",
            "resourcetype" to resourceType
        )

        runFlow(
            tags = tags,
            amount = 46
        )

        // When
        val result = recordService.countFhir4Records(
            Fhir4Reference::class.java,
            USER_ID,
            emptyList()
        ).blockingGet()

        // Then
        assertEquals(
            expected = 46,
            actual = result
        )
    }

    @Test
    fun `Given, countFhir4Records is called with a type, UserId and Annotations, it returns the amount of Fhir4Records`() {
        // Given
        val resourceType = "DocumentReference"
        val tags = mapOf(
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

        runFlow(
            tags = tags,
            annotations = annotations,
            amount = 23
        )

        // When
        val result = recordService.countFhir4Records(
            Fhir4Reference::class.java,
            USER_ID,
            annotations
        ).blockingGet()

        // Then
        assertEquals(
            expected = 23,
            actual = result
        )
    }
}
