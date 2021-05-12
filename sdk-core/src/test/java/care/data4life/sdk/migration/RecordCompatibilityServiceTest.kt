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
import care.data4life.sdk.crypto.CryptoContract
import care.data4life.sdk.network.NetworkingContract
import care.data4life.sdk.network.model.EncryptedRecord
import care.data4life.sdk.tag.Annotations
import care.data4life.sdk.tag.TaggingContract
import care.data4life.sdk.tag.TaggingContract.Companion.ANNOTATION_KEY
import care.data4life.sdk.tag.TaggingContract.Companion.DELIMITER
import care.data4life.sdk.tag.Tags
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import io.reactivex.Observable
import io.reactivex.Single
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RecordCompatibilityServiceTest {
    private val apiService: NetworkingContract.Service = mockk()
    private val cryptoService: CryptoContract.Service = mockk()
    private val tagCryptoService: TaggingContract.CryptoService = mockk()
    private val tagEncoding: TaggingContract.Encoding = mockk()
    private val compatibilityEncoder: MigrationContract.CompatibilityEncoder = mockk()
    private lateinit var service: MigrationContract.CompatibilityService

    @Before
    fun setUp() {
        clearAllMocks()

        service = RecordCompatibilityService(
            apiService,
            cryptoService,
            tagEncoding,
            tagCryptoService,
            compatibilityEncoder
        )
    }

    @Test
    fun `It fulfills the CompatibilityService`() {
        val service: Any = RecordCompatibilityService(
            apiService,
            cryptoService,
            tagEncoding,
            tagCryptoService,
            compatibilityEncoder
        )
        assertTrue(service is MigrationContract.CompatibilityService)
    }

    /*@Test
    fun `Given, countRecords is called with proper Arguments, delegates the plain Tags to the CompatibilityEncoder`() {
        // Given
        val tags = mapOf(
            "tag1" to "tag1Value",
            "tag2" to "tag2Value",
            "tag3" to "tag3Value"
        )

        every { compatibilityEncoder.encode("tag1Value") } returns mockk()
        every { compatibilityEncoder.encode("tag2Value") } returns mockk()
        every { compatibilityEncoder.encode("tag3Value") } returns mockk()
        every { cryptoService.fetchTagEncryptionKey() } returns mockk()
        every { tagCryptoService.encryptList(any(), any(), any()) } returns mockk()

        // When
        service.countRecords("NoVIP", "NoVIP", tags, emptyList())

        // Then
        verify(atLeast = 1) { compatibilityEncoder.encode("tag1Value") }
        verify(atLeast = 1) { compatibilityEncoder.encode("tag2Value") }
        verify(atLeast = 1) { compatibilityEncoder.encode("tag3Value") }
    }

    @Test
    fun `Given, countRecords is called with proper Arguments, delegates uses the TagKey and Separator as Prefix and delegates it with the encoded TagValues to the TagCryptoService`() {
        // Given
        val tagEncryptionKey: GCKey = mockk()
        val tags = mapOf(
            "tag1" to "tag1Value",
            "tag2" to "tag2Value",
            "tag3" to "tag3Value"
        )
        val encodedTags = listOf(
            Triple("encodedTag1Value", "encodedAndroidLegacyTag1Value", "encodedJSLegacyTag1Value"),
            Triple("encodedTag2Value", "encodedAndroidLegacyTag2Value", "encodedJSLegacyTag2Value"),
            Triple("encodedTag3Value", "encodedAndroidLegacyTag3Value", "encodedJSLegacyTag3Value")
        )

        every { compatibilityEncoder.encode("tag1Value") } returns encodedTags[0]
        every { compatibilityEncoder.encode("tag2Value") } returns encodedTags[1]
        every { compatibilityEncoder.encode("tag3Value") } returns encodedTags[2]

        every { cryptoService.fetchTagEncryptionKey() } returns tagEncryptionKey

        every {
            tagCryptoService.encryptList(
                encodedTags[0].toList(),
                tagEncryptionKey,
                "${tags.keys.toList()[0]}${TaggingContract.DELIMITER}"
            )
        } returns mockk()

        every {
            tagCryptoService.encryptList(
                encodedTags[1].toList(),
                tagEncryptionKey,
                "${tags.keys.toList()[1]}${TaggingContract.DELIMITER}"
            )
        } returns mockk()

        every {
            tagCryptoService.encryptList(
                encodedTags[2].toList(),
                tagEncryptionKey,
                "${tags.keys.toList()[2]}${TaggingContract.DELIMITER}"
            )
        } returns mockk()

        // When
        service.countRecords("NoVIP", "NoVIP", tags, emptyList())

        // Then
        verify(exactly = 1) { cryptoService.fetchTagEncryptionKey() }

        verify(atLeast = 1) {
            tagCryptoService.encryptList(
                encodedTags[0].toList(),
                tagEncryptionKey,
                "${tags.keys.toList()[0]}${TaggingContract.DELIMITER}"
            )
        }

        verify(atLeast = 1) {
            tagCryptoService.encryptList(
                encodedTags[1].toList(),
                tagEncryptionKey,
                "${tags.keys.toList()[1]}${TaggingContract.DELIMITER}"
            )
        }

        verify(atLeast = 1) {
            tagCryptoService.encryptList(
                encodedTags[2].toList(),
                tagEncryptionKey,
                "${tags.keys.toList()[2]}${TaggingContract.DELIMITER}"
            )
        }
    }*/

    private fun encryptTagsAndAnnotationsFlow(
        tags: Tags,
        annotations: Annotations,
        encodedAndEncryptedTagsAndAnnotations: MutableList<String>,
        encryptedTags: MutableList<String>,
        encryptedAnnotations: MutableList<String>,
        encryptionKey: GCKey
    ) {
        every { cryptoService.fetchTagEncryptionKey() } returns encryptionKey
        every {
            tagCryptoService.encryptTagsAndAnnotations(tags, annotations, encryptionKey)
        } returns encodedAndEncryptedTagsAndAnnotations

        every {
            tagCryptoService.encryptList(
                annotations,
                encryptionKey,
                ANNOTATION_KEY + DELIMITER
            )
        } returns encryptedAnnotations

        every { tagEncoding.normalize(tags["key"]!!) } returns tags["key"]!!
        every {
            tagCryptoService.encryptList(
                eq(listOf("key=value")),
                encryptionKey
            )
        } returns encryptedTags
    }

    private fun verifyEncryptTagsAndAnnotationsFlow(
        tags: Tags,
        annotations: Annotations,
        encryptionKey: GCKey
    ) {
        verify(exactly = 1) { cryptoService.fetchTagEncryptionKey() }
        verify(exactly = 1) {
            tagCryptoService.encryptTagsAndAnnotations(tags, annotations, encryptionKey)
        }
        verify(exactly = 1) { tagEncoding.normalize(tags["key"]!!) }
        verify(exactly = 2) {
            tagCryptoService.encryptList(
                or(eq(listOf("key=value")), annotations),
                or(encryptionKey, encryptionKey),
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
        val annotations: Annotations = mockk()
        val encryptedTags = mutableListOf("a", "k")
        val encryptedAnnotations = mutableListOf("d")
        val expected = 42
        val encryptionKey: GCKey = mockk()
        val encodedAndEncryptedTagsAndAnnotations = mutableListOf("a", "v")
            .also {
                it.addAll(listOf("d"))
            }
        val indicator = slot<String>()

        encryptTagsAndAnnotationsFlow(
            tags,
            annotations,
            encodedAndEncryptedTagsAndAnnotations,
            encryptedTags,
            encryptedAnnotations,
            encryptionKey
        )

        every {
            apiService.getCount(
                alias,
                userId,
                capture(indicator)
            )
        } answers {
            when (indicator.captured) {
                encodedAndEncryptedTagsAndAnnotations.joinToString(",") -> Single.just(21)
                encryptedTags.joinToString(",") -> Single.just(21)
                else -> throw RuntimeException("Unknown tags ${indicator.captured}")
            }
        }

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
            encryptionKey
        )
        verify(exactly = 2) {
            apiService.getCount(
                alias,
                userId,
                or(
                    encodedAndEncryptedTagsAndAnnotations.joinToString(","),
                    encryptedTags.joinToString(",")
                )
            )
        }
    }

    @Test
    fun `Given searchRecords is called with a UserId, a ResourceType, a StartDate, a EndDate, the PageSize, Offset, Tags and Annotations, it calls the ApiService twice with the encodedAndEncrypted and the encrypted Tags`() {
        // Given
        val alias = "alias"
        val userId = "id"
        val startTime = "start"
        val endTime = "end"
        val pageSize = 23
        val offset = 42
        val tags = hashMapOf("key" to "value")
        val annotations: Annotations = mockk()
        val encodedAndEncryptedTagsAndAnnotations: MutableList<String> = mutableListOf("a", "v")
            .also {
                it.addAll(listOf("d"))
            }
        val encryptedTags: MutableList<String> = mutableListOf("a", "k")
        val encryptedAnnotations: MutableList<String> = mutableListOf("d")
        val encryptionKey: GCKey = mockk()
        val encryptedRecord1: EncryptedRecord = mockk()
        val encryptedRecord2: EncryptedRecord = mockk()
        val indicator = slot<String>()

        encryptTagsAndAnnotationsFlow(
            tags,
            annotations,
            encodedAndEncryptedTagsAndAnnotations,
            encryptedTags,
            encryptedAnnotations,
            encryptionKey
        )
        every {
            apiService.searchRecords(
                alias,
                userId,
                startTime,
                endTime,
                pageSize,
                offset,
                capture(indicator)
            )
        } answers {
            when (indicator.captured) {
                encodedAndEncryptedTagsAndAnnotations.joinToString(",") -> Observable.fromArray(
                    listOf(encryptedRecord1)
                )
                encryptedTags.joinToString(",") -> Observable.fromArray(listOf(encryptedRecord2))
                else -> throw RuntimeException("Unknown tags ${indicator.captured}")
            }
        }

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
            encryptionKey
        )
        verify(exactly = 2) {
            apiService.searchRecords(
                alias,
                userId,
                startTime,
                endTime,
                pageSize,
                offset,
                or(
                    encodedAndEncryptedTagsAndAnnotations.joinToString(","),
                    encryptedTags.joinToString(",")
                )
            )
        }
    }

    @Test
    fun `Given searchRecords is called with its appropriate parameter, it calls the ApiService only if the legacy encrypted tags equal the current version of encrypted tags`() {
        // Given
        val alias = "alias"
        val userId = "id"
        val startTime = "start"
        val endTime = "end"
        val pageSize = 23
        val offset = 42
        val tags = hashMapOf("key" to "value")
        val annotations: Annotations = mockk()
        val encodedAndEncryptedTagsAndAnnotations: MutableList<String> =
            mutableListOf("a", "b", "c")
        val encryptedTags: MutableList<String> = mutableListOf("c", "b", "a")
        val encryptionKey: GCKey = mockk()
        val encryptedAnnotations: MutableList<String> = mutableListOf()
        val encryptedRecord1: EncryptedRecord = mockk()
        val encryptedRecord2: EncryptedRecord = mockk()
        val indicator = slot<String>()

        encryptTagsAndAnnotationsFlow(
            tags,
            annotations,
            encodedAndEncryptedTagsAndAnnotations,
            encryptedTags,
            encryptedAnnotations,
            encryptionKey
        )
        every {
            apiService.searchRecords(
                alias,
                userId,
                startTime,
                endTime,
                pageSize,
                offset,
                capture(indicator)
            )
        } answers {
            when (indicator.captured) {
                encodedAndEncryptedTagsAndAnnotations.joinToString(",") -> Observable.fromArray(
                    listOf(encryptedRecord1)
                )
                encryptedTags.joinToString(",") -> Observable.fromArray(listOf(encryptedRecord2))
                else -> throw RuntimeException("Unknown tags ${indicator.captured}")
            }
        }

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
            encryptionKey
        )
        verify(exactly = 1) {
            apiService.searchRecords(
                alias,
                userId,
                startTime,
                endTime,
                pageSize,
                offset,
                encodedAndEncryptedTagsAndAnnotations.joinToString(",")
            )
        }
    }
}
