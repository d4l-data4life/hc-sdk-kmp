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

package care.data4life.sdk.migration

import care.data4life.crypto.GCKey
import care.data4life.sdk.ApiService
import care.data4life.sdk.CryptoService
import care.data4life.sdk.network.model.EncryptedRecord
import care.data4life.sdk.tag.Annotations
import care.data4life.sdk.tag.TaggingContract
import care.data4life.sdk.tag.TaggingContract.Companion.ANNOTATION_KEY
import care.data4life.sdk.tag.TaggingContract.Companion.DELIMITER
import care.data4life.sdk.tag.Tags
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.reactivex.Observable
import io.reactivex.Single
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RecordCompatibilityServiceTest {
    private lateinit var apiService: ApiService
    private lateinit var tagEncryptionService: TaggingContract.EncryptionService
    private lateinit var cryptoService: CryptoService
    private lateinit var tagEncryptionHelper: TaggingContract.Helper
    private lateinit var service: MigrationContract.CompatibilityService

    @Before
    fun setUp() {
        apiService = mockk()
        cryptoService = mockk()
        tagEncryptionService = mockk()
        tagEncryptionHelper = mockk()
        service = RecordCompatibilityService(
                apiService,
                tagEncryptionService,
                cryptoService,
                tagEncryptionHelper
        )
    }

    @Test
    fun `it fulfills the CompatibilityService`() {
        val service: Any = RecordCompatibilityService(mockk(), mockk(), mockk(), mockk())
        assertTrue(service is MigrationContract.CompatibilityService)
    }

    private fun encryptTagsAndAnnotationsFlow(
            tags: Tags,
            annotations: Annotations,
            encodedAndEncryptedTagsAndAnnotations: MutableList<String>,
            encryptedTags: MutableList<String>,
            encryptedAnnotations: MutableList<String>,
            encryptionKeys: List<GCKey>) {
        every { cryptoService.fetchTagEncryptionKey() } returnsMany encryptionKeys
        every {
            tagEncryptionService.encryptTagsAndAnnotations(tags, annotations)
        } returns encodedAndEncryptedTagsAndAnnotations

        every {
            tagEncryptionService.encryptList(
                    annotations,
                    encryptionKeys[1],
                    ANNOTATION_KEY + DELIMITER
            )
        } returns encryptedAnnotations

        every { tagEncryptionHelper.normalize(tags["key"]!!) } returns tags["key"]!!
        every {
            tagEncryptionService.encryptList(
                    eq(listOf("key=value")),
                    encryptionKeys[0]
            )
        } returns encryptedTags
    }

    private fun verifyEncryptTagsAndAnnotationsFlow(
            tags: Tags,
            annotations: Annotations,
            encryptionKeys: List<GCKey>
    ) {
        verify(exactly = 2) { cryptoService.fetchTagEncryptionKey() }
        verify(exactly = 1) { tagEncryptionService.encryptTagsAndAnnotations(tags, annotations) }
        verify(exactly = 1) { tagEncryptionHelper.normalize(tags["key"]!!) }
        verify(exactly = 2) {
            tagEncryptionService.encryptList(
                    or(eq(listOf("key=value")), annotations),
                    or(encryptionKeys[0], encryptionKeys[1]),
                    or("", ANNOTATION_KEY + DELIMITER)
            )
        }
    }

    @Test
    fun `Given countRecords is called, with a Alias, UserId and Tags, it calls the ApiService twice with the encodedAndEncrypted and the encrypted Tags`() {
        // Given
        val alias = "alias"
        val userId = "id"
        val tags = hashMapOf("key" to "value")
        val annotations: List<String> = mockk()
        val encryptedTags = mutableListOf("a", "k")
        val encryptedAnnotations = mutableListOf("d")
        val expected = 42
        val encryptionKeys = listOf<GCKey>(mockk(), mockk())
        val encodedAndEncryptedTagsAndAnnotations = mutableListOf("a", "v")
                .also {
                    it.addAll(listOf("d"))
                }

        encryptTagsAndAnnotationsFlow(
                tags,
                annotations,
                encodedAndEncryptedTagsAndAnnotations,
                encryptedTags,
                encryptedAnnotations,
                encryptionKeys
        )
        
        every { apiService.getCount(alias, userId, encryptedTags) } returns Single.just(21)
        every {
            apiService.getCount(
                    alias,
                    userId,
                    encodedAndEncryptedTagsAndAnnotations
            )
        } returns Single.just(21)

        // When
        val observer = service.countRecords(alias, userId, tags, annotations).test().await()

        // Then
        val result = observer
                .assertComplete()
                .assertValueCount(1)
                .values()[0]

        assertEquals(
                expected = expected,
                actual = result
        )

        verifyEncryptTagsAndAnnotationsFlow(
                tags,
                annotations,
                encryptionKeys
        )
        verify(exactly = 2) {
            apiService.getCount(
                    alias,
                    userId,
                    or(encodedAndEncryptedTagsAndAnnotations, encryptedTags)
            )
        }
    }

    @Test
    fun `Given searchRecords is called with a UserId, a ResourceType, a StartDate, a EndDate, the PageSize, Offset, Tags and Annotations, it calls the ApiService twice with the encodedAndEncrypted and the encrypted Tags`() {
        val alias = "alias"
        val userId = "id"
        val startTime = "start"
        val endTime = "end"
        val pageSize = 23
        val offset = 42
        val tags = hashMapOf("key" to "value")
        val annotations: List<String> = mockk()
        val encodedAndEncryptedTagsAndAnnotations: MutableList<String> = mutableListOf("a", "v")
                .also {
                    it.addAll(listOf("d"))
                }
        val encryptedTags: MutableList<String> = mutableListOf("a", "k")
        val encryptedAnnotations: MutableList<String> = mutableListOf("d")
        val encryptionKeys = listOf<GCKey>(mockk(), mockk())
        val encryptedRecord1: EncryptedRecord = mockk()
        val encryptedRecord2: EncryptedRecord = mockk()

        encryptTagsAndAnnotationsFlow(
                tags,
                annotations,
                encodedAndEncryptedTagsAndAnnotations,
                encryptedTags,
                encryptedAnnotations,
                encryptionKeys
        )
        every {
            apiService.fetchRecords(
                    alias,
                    userId,
                    startTime,
                    endTime,
                    pageSize,
                    offset,
                    encodedAndEncryptedTagsAndAnnotations
            )
        } returns Observable.fromArray(listOf(encryptedRecord1))
        every {
            apiService.fetchRecords(
                    alias,
                    userId,
                    startTime,
                    endTime,
                    pageSize,
                    offset,
                    encryptedTags
            )
        } returns Observable.fromArray(listOf(encryptedRecord2))


        // When
        val observer = service.searchRecords(
                alias,
                userId,
                startTime,
                endTime,
                pageSize,
                offset,
                tags,
                annotations
        ).test().await()

        // Then
        val result = observer
                .assertComplete()
                .assertValueCount(1)
                .values()[0]

        assertEquals(
                expected = listOf(encryptedRecord1, encryptedRecord2),
                actual = result
        )

        verifyEncryptTagsAndAnnotationsFlow(
                tags,
                annotations,
                encryptionKeys
        )
        verify(exactly = 2) {
            apiService.fetchRecords(
                    alias,
                    userId,
                    startTime,
                    endTime,
                    pageSize,
                    offset,
                    or(encodedAndEncryptedTagsAndAnnotations, encryptedTags)
            )
        }
    }

    @Test
    fun `Given searchRecords is called with its appropriate parameter, it calls the ApiService only if the legacy encrypted tags equal the current version of encrypted tags`() {
        val alias = "alias"
        val userId = "id"
        val startTime = "start"
        val endTime = "end"
        val pageSize = 23
        val offset = 42
        val tags = hashMapOf("key" to "value")
        val annotations: List<String> = mockk()
        val encodedAndEncryptedTagsAndAnnotations: MutableList<String> = mutableListOf("a", "b", "c")
        val encryptedTags: MutableList<String> = mutableListOf("c", "b", "a")
        val encryptionKeys = listOf<GCKey>(mockk(), mockk())
        val encryptedAnnotations: MutableList<String> = mutableListOf()
        val encryptedRecord1: EncryptedRecord = mockk()
        val encryptedRecord2: EncryptedRecord = mockk()

        encryptTagsAndAnnotationsFlow(
                tags,
                annotations,
                encodedAndEncryptedTagsAndAnnotations,
                encryptedTags,
                encryptedAnnotations,
                encryptionKeys
        )
        every {
            apiService.fetchRecords(
                    alias,
                    userId,
                    startTime,
                    endTime,
                    pageSize,
                    offset,
                    encodedAndEncryptedTagsAndAnnotations
            )
        } returns Observable.fromArray(listOf(encryptedRecord1))
        every {
            apiService.fetchRecords(
                    alias,
                    userId,
                    startTime,
                    endTime,
                    pageSize,
                    offset,
                    encryptedTags
            )
        } returns Observable.fromArray(listOf(encryptedRecord2))


        // When
        val observer = service.searchRecords(
                alias,
                userId,
                startTime,
                endTime,
                pageSize,
                offset,
                tags,
                annotations
        ).test().await()

        // Then
        val result = observer
                .assertComplete()
                .assertValueCount(1)
                .values()[0]

        assertEquals(
                expected = listOf(encryptedRecord1),
                actual = result
        )

        verifyEncryptTagsAndAnnotationsFlow(
                tags,
                annotations,
                encryptionKeys
        )
        verify(exactly = 1) {
            apiService.fetchRecords(
                    alias,
                    userId,
                    startTime,
                    endTime,
                    pageSize,
                    offset,
                    encodedAndEncryptedTagsAndAnnotations
            )
        }
    }
}
