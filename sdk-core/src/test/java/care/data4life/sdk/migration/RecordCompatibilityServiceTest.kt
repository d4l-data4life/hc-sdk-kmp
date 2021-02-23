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

import care.data4life.sdk.ApiService
import care.data4life.sdk.network.model.EncryptedRecord
import care.data4life.sdk.tag.TaggingContract
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
    private lateinit var taggingEncryptionService: TaggingContract.EncryptionService
    private lateinit var service: MigrationContract.CompatibilityService

    @Before
    fun setUp() {
        apiService = mockk()
        taggingEncryptionService = mockk()
        service = RecordCompatibilityService(apiService, taggingEncryptionService)
    }

    @Test
    fun `it fulfills the CompatibilityService`() {
        val service: Any = RecordCompatibilityService(mockk(), mockk())
        assertTrue(service is MigrationContract.CompatibilityService)
    }

    @Test
    fun `Given countRecords is called, with a Alias, UserId and Tags, it calls the ApiService twice with the encodedAndEncrypted and the encrypted Tags`() {
        // Given
        val alias = "alias"
        val userId = "id"
        val tags: HashMap<String, String> = mockk()
        val annotations: List<String> = mockk()
        val encodedAndEncryptedTags: MutableList<String> = mockk(relaxed = true)
        val encryptedTags: MutableList<String> = mockk(relaxed = true)
        val encryptedAndEncodedAnnotations: MutableList<String> = mockk()
        val encryptedAnnotations: MutableList<String> = mockk()
        val expected = 42

        every { taggingEncryptionService.encryptAndEncodeTags(tags) } returns encodedAndEncryptedTags
        every { taggingEncryptionService.encryptAndEncodeAnnotations(annotations) } returns encryptedAndEncodedAnnotations
        every { taggingEncryptionService.encryptTags(tags) } returns encryptedTags
        every { taggingEncryptionService.encryptAnnotations(annotations) } returns encryptedAnnotations
        every { encodedAndEncryptedTags.addAll(encryptedAndEncodedAnnotations) } returns true
        every { encryptedTags.addAll(encryptedAnnotations) } returns true
        every { apiService.getCount(alias, userId, encryptedTags) } returns Single.just(21)
        every {
            apiService.getCount(
                    alias,
                    userId,
                    encodedAndEncryptedTags
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

        verify(exactly = 1) { taggingEncryptionService.encryptAndEncodeTags(tags) }
        verify(exactly = 1) { taggingEncryptionService.encryptAndEncodeAnnotations(annotations) }
        verify(exactly = 1) { taggingEncryptionService.encryptTags(tags) }
        verify(exactly = 1) { taggingEncryptionService.encryptAnnotations(annotations) }
        verify(exactly = 1) { encodedAndEncryptedTags.addAll(encryptedAndEncodedAnnotations) }
        verify(exactly = 1) { encryptedTags.addAll(encryptedAnnotations) }
        verify(exactly = 2) {
            apiService.getCount(
                    alias,
                    userId,
                    or(encodedAndEncryptedTags, encryptedTags)
            )
        }
    }

    @Test
    fun `Given searchRecords is called with a UserId, a ResourceType, a StartDate, a EndDate, the PageSize, Offset, Tags and Annotations,  it calls the ApiService twice with the encodedAndEncrypted and the encrypted Tags`() {
        val alias = "alias"
        val userId = "id"
        val startTime = "start"
        val endTime = "end"
        val pageSize = 23
        val offset = 42
        val tags: HashMap<String, String> = mockk()
        val annotations: List<String> = mockk()
        val encodedAndEncryptedTags: MutableList<String> = mutableListOf("a", "v")
        val encryptedTags: MutableList<String> = mutableListOf("a", "k")
        val encryptedAndEncodedAnnotations: MutableList<String> = mutableListOf("d")
        val encryptedAnnotations: MutableList<String> = mutableListOf("d")
        val encryptedRecord1: EncryptedRecord = mockk()
        val encryptedRecord2: EncryptedRecord = mockk()

        every { taggingEncryptionService.encryptAndEncodeTags(tags) } returns encodedAndEncryptedTags
        every { taggingEncryptionService.encryptAndEncodeAnnotations(annotations) } returns encryptedAndEncodedAnnotations
        every { taggingEncryptionService.encryptTags(tags) } returns encryptedTags
        every { taggingEncryptionService.encryptAnnotations(annotations) } returns encryptedAnnotations
        every {
            apiService.fetchRecords(
                    alias,
                    userId,
                    startTime,
                    endTime,
                    pageSize,
                    offset,
                    encodedAndEncryptedTags
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

        verify(exactly = 1) { taggingEncryptionService.encryptAndEncodeTags(tags) }
        verify(exactly = 1) { taggingEncryptionService.encryptAndEncodeAnnotations(annotations) }
        verify(exactly = 1) { taggingEncryptionService.encryptTags(tags) }
        verify(exactly = 1) { taggingEncryptionService.encryptAnnotations(annotations) }
        verify(exactly = 2) {
            apiService.fetchRecords(
                    alias,
                    userId,
                    startTime,
                    endTime,
                    pageSize,
                    offset,
                    or(encodedAndEncryptedTags, encryptedTags)
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
        val tags: HashMap<String, String> = mockk()
        val annotations: List<String> = mockk()
        val encodedAndEncryptedTags: MutableList<String> = mutableListOf("a", "b", "c")
        val encryptedTags: MutableList<String> = mutableListOf("c", "b", "a")
        val encryptedAndEncodedAnnotations: MutableList<String> = mutableListOf()
        val encryptedAnnotations: MutableList<String> = mutableListOf()
        val encryptedRecord1: EncryptedRecord = mockk()
        val encryptedRecord2: EncryptedRecord = mockk()

        every { taggingEncryptionService.encryptAndEncodeTags(tags) } returns encodedAndEncryptedTags
        every { taggingEncryptionService.encryptAndEncodeAnnotations(annotations) } returns encryptedAndEncodedAnnotations
        every { taggingEncryptionService.encryptTags(tags) } returns encryptedTags
        every { taggingEncryptionService.encryptAnnotations(annotations) } returns encryptedAnnotations
        every {
            apiService.fetchRecords(
                    alias,
                    userId,
                    startTime,
                    endTime,
                    pageSize,
                    offset,
                    encodedAndEncryptedTags
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

        verify(exactly = 1) { taggingEncryptionService.encryptAndEncodeTags(tags) }
        verify(exactly = 1) { taggingEncryptionService.encryptAndEncodeAnnotations(annotations) }
        verify(exactly = 1) { taggingEncryptionService.encryptTags(tags) }
        verify(exactly = 1) { taggingEncryptionService.encryptAnnotations(annotations) }
        verify(exactly = 1) {
            apiService.fetchRecords(
                    alias,
                    userId,
                    startTime,
                    endTime,
                    pageSize,
                    offset,
                    encodedAndEncryptedTags
            )
        }
    }
}
