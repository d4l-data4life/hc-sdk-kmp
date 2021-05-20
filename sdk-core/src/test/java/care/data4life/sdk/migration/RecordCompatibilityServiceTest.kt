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
import care.data4life.sdk.tag.TaggingContract
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Before
import org.junit.Test
import kotlin.test.assertSame
import kotlin.test.assertTrue

class RecordCompatibilityServiceTest {
    private val cryptoService: CryptoContract.Service = mockk()
    private val tagCryptoService: TaggingContract.CryptoService = mockk()
    private val compatibilityEncoder: MigrationContract.CompatibilityEncoder = mockk()
    private val searchTagsBuilderFactory: NetworkingContract.SearchTagsBuilderFactory = mockk()
    private val searchTagsPipe: NetworkingContract.SearchTagsBuilder = mockk()
    private lateinit var service: MigrationContract.CompatibilityService

    @Before
    fun setUp() {
        clearAllMocks()

        service = RecordCompatibilityService(
            cryptoService,
            tagCryptoService,
            compatibilityEncoder,
            searchTagsBuilderFactory
        )
    }

    @Test
    fun `It fulfills the CompatibilityService`() {
        val service: Any = RecordCompatibilityService(cryptoService, tagCryptoService)
        assertTrue(service is MigrationContract.CompatibilityService)
    }

    @Test
    fun `Given, resolveSearchTags is called with proper Arguments, setsUp a new SearchTagsPipeIn`() {
        // Given
        val tagEncryptionKey: GCKey = mockk()

        every { cryptoService.fetchTagEncryptionKey() } returns tagEncryptionKey
        every { searchTagsBuilderFactory.newBuilder() } returns mockk(relaxed = true)

        // When
        service.resolveSearchTags(emptyMap(), emptyList())

        // Then
        verify(exactly = 1) { cryptoService.fetchTagEncryptionKey() }
        verify(exactly = 1) { searchTagsBuilderFactory.newBuilder() }
    }

    @Test
    fun `Given, resolveSearchTags is called with proper Arguments, it seals the SearchTags and returns them`() {
        // Given
        val sealedSearchTags: NetworkingContract.SearchTags = mockk()

        every { cryptoService.fetchTagEncryptionKey() } returns mockk()

        every { searchTagsBuilderFactory.newBuilder() } returns searchTagsPipe
        every { searchTagsPipe.seal() } returns sealedSearchTags

        // When
        val tags = service.resolveSearchTags(emptyMap(), emptyList())

        // Then
        assertSame(
            expected = sealedSearchTags,
            actual = tags
        )
        verify(exactly = 1) { searchTagsPipe.seal() }
    }

    // Tags
    @Test
    fun `Given, resolveSearchTags is called with Tags, delegates the plain Tags to the CompatibilityEncoder`() {
        // Given
        val tags = mapOf(
            "tag1" to "tag1Value",
            "tag2" to "tag2Value",
            "tag3" to "tag3Value"
        )

        every { compatibilityEncoder.encode(tags["tag1"]!!) } returns mockk(relaxed = true)
        every { compatibilityEncoder.encode(tags["tag2"]!!) } returns mockk(relaxed = true)
        every { compatibilityEncoder.encode(tags["tag3"]!!) } returns mockk(relaxed = true)

        every { cryptoService.fetchTagEncryptionKey() } returns mockk()
        every { tagCryptoService.encryptList(any(), any(), any()) } returns mockk()

        every { searchTagsBuilderFactory.newBuilder() } returns mockk(relaxed = true)

        // When
        service.resolveSearchTags(tags, emptyList())

        // Then
        verify(atMost = 1) { compatibilityEncoder.encode(tags["tag1"]!!) }
        verify(atMost = 1) { compatibilityEncoder.encode(tags["tag2"]!!) }
        verify(atMost = 1) { compatibilityEncoder.encode(tags["tag3"]!!) }
    }

    @Test
    fun `Given, resolveSearchTags is called with Tags, delegates the TagGroupKey and the TagGroupValue to the TagCryptoService`() {
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

        every { compatibilityEncoder.encode(tags["tag1"]!!) } returns encodedTags[0]
        every { compatibilityEncoder.encode(tags["tag2"]!!) } returns encodedTags[1]
        every { compatibilityEncoder.encode(tags["tag3"]!!) } returns encodedTags[2]

        every { cryptoService.fetchTagEncryptionKey() } returns tagEncryptionKey

        every { searchTagsBuilderFactory.newBuilder() } returns mockk(relaxed = true)

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
        service.resolveSearchTags(tags, emptyList())

        // Then
        verify(atMost = 1) {
            tagCryptoService.encryptList(
                encodedTags[0].toList(),
                tagEncryptionKey,
                "${tags.keys.toList()[0]}${TaggingContract.DELIMITER}"
            )
        }

        verify(atMost = 1) {
            tagCryptoService.encryptList(
                encodedTags[1].toList(),
                tagEncryptionKey,
                "${tags.keys.toList()[1]}${TaggingContract.DELIMITER}"
            )
        }

        verify(atMost = 1) {
            tagCryptoService.encryptList(
                encodedTags[2].toList(),
                tagEncryptionKey,
                "${tags.keys.toList()[2]}${TaggingContract.DELIMITER}"
            )
        }
    }

    @Test
    fun `Given, resolveSearchTags is called with Tags, it adds the encrypted OrGroup to the SearchTagsPipe`() {
        // Given
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
        val encryptedGroups = listOf<MutableList<String>>(mockk(), mockk(), mockk())

        every { cryptoService.fetchTagEncryptionKey() } returns mockk()

        every { compatibilityEncoder.encode(tags["tag1"]!!) } returns encodedTags[0]
        every { compatibilityEncoder.encode(tags["tag2"]!!) } returns encodedTags[1]
        every { compatibilityEncoder.encode(tags["tag3"]!!) } returns encodedTags[2]

        every {
            tagCryptoService.encryptList(encodedTags[0].toList(), any(), any())
        } returns encryptedGroups[0]

        every {
            tagCryptoService.encryptList(encodedTags[1].toList(), any(), any())
        } returns encryptedGroups[1]

        every {
            tagCryptoService.encryptList(encodedTags[2].toList(), any(), any())
        } returns encryptedGroups[2]

        every { searchTagsBuilderFactory.newBuilder() } returns searchTagsPipe
        every { searchTagsPipe.addOrTuple(encryptedGroups[0]) } returns searchTagsPipe
        every { searchTagsPipe.addOrTuple(encryptedGroups[1]) } returns searchTagsPipe
        every { searchTagsPipe.addOrTuple(encryptedGroups[2]) } returns searchTagsPipe
        every { searchTagsPipe.seal() } returns mockk()

        // When
        service.resolveSearchTags(tags, emptyList())

        // Then
        verify(atLeast = 1) { searchTagsPipe.addOrTuple(encryptedGroups[0]) }
        verify(atLeast = 1) { searchTagsPipe.addOrTuple(encryptedGroups[1]) }
        verify(atLeast = 1) { searchTagsPipe.addOrTuple(encryptedGroups[2]) }
    }

    // Annotations
    @Test
    fun `Given, resolveSearchTags is called with Annotations, delegates the plain Tags to the CompatibilityEncoder`() {
        // Given
        val annotations = listOf(
            "annotation1Value",
            "annotation2Value",
            "annotation3Value"
        )

        every { compatibilityEncoder.encode(annotations[0]) } returns mockk(relaxed = true)
        every { compatibilityEncoder.encode(annotations[1]) } returns mockk(relaxed = true)
        every { compatibilityEncoder.encode(annotations[2]) } returns mockk(relaxed = true)

        every { compatibilityEncoder.normalize(annotations[0]) } returns "a"
        every { compatibilityEncoder.normalize(annotations[1]) } returns "b"
        every { compatibilityEncoder.normalize(annotations[2]) } returns "c"

        every { cryptoService.fetchTagEncryptionKey() } returns mockk()
        every { tagCryptoService.encryptList(any(), any(), any()) } returns mockk()

        every { searchTagsBuilderFactory.newBuilder() } returns mockk(relaxed = true)

        // When
        service.resolveSearchTags(emptyMap(), annotations)

        // Then
        verify(atMost = 1) { compatibilityEncoder.encode(annotations[0]) }
        verify(atMost = 1) { compatibilityEncoder.encode(annotations[1]) }
        verify(atMost = 1) { compatibilityEncoder.encode(annotations[2]) }
    }

    @Test
    fun `Given, resolveSearchTags is called with Annotations, delegates the TagGroupKey and the TagGroupValue to the TagCryptoService`() {
        // Given
        val tagEncryptionKey: GCKey = mockk()
        val annotations = listOf(
            "annotation1Value",
            "annotation2Value",
            "annotation3Value"
        )
        val encodedTags = listOf(
            Triple("encodedTag1Value", "encodedAndroidLegacyTag1Value", "encodedJSLegacyTag1Value"),
            Triple("encodedTag2Value", "encodedAndroidLegacyTag2Value", "encodedJSLegacyTag2Value"),
            Triple("encodedTag3Value", "encodedAndroidLegacyTag3Value", "encodedJSLegacyTag3Value")
        )

        every { compatibilityEncoder.encode(annotations[0]) } returns encodedTags[0]
        every { compatibilityEncoder.encode(annotations[1]) } returns encodedTags[1]
        every { compatibilityEncoder.encode(annotations[2]) } returns encodedTags[2]

        every { cryptoService.fetchTagEncryptionKey() } returns tagEncryptionKey

        every { searchTagsBuilderFactory.newBuilder() } returns mockk(relaxed = true)

        every {
            tagCryptoService.encryptList(
                encodedTags[0].copy(second = annotations[0]).toList(),
                tagEncryptionKey,
                "${TaggingContract.ANNOTATION_KEY}${TaggingContract.DELIMITER}"
            )
        } returns mockk()

        every {
            tagCryptoService.encryptList(
                encodedTags[1].copy(second = annotations[1]).toList(),
                tagEncryptionKey,
                "${TaggingContract.ANNOTATION_KEY}${TaggingContract.DELIMITER}"
            )
        } returns mockk()

        every {
            tagCryptoService.encryptList(
                encodedTags[2].copy(second = annotations[2]).toList(),
                tagEncryptionKey,
                "${TaggingContract.ANNOTATION_KEY}${TaggingContract.DELIMITER}"
            )
        } returns mockk()

        // When
        service.resolveSearchTags(emptyMap(), annotations)

        // Then
        verify(atMost = 1) {
            tagCryptoService.encryptList(
                encodedTags[0].copy(second = annotations[0]).toList(),
                tagEncryptionKey,
                "${TaggingContract.ANNOTATION_KEY}${TaggingContract.DELIMITER}"
            )
        }

        verify(atMost = 1) {
            tagCryptoService.encryptList(
                encodedTags[1].copy(second = annotations[1]).toList(),
                tagEncryptionKey,
                "${TaggingContract.ANNOTATION_KEY}${TaggingContract.DELIMITER}"
            )
        }

        verify(atMost = 1) {
            tagCryptoService.encryptList(
                encodedTags[2].copy(second = annotations[2]).toList(),
                tagEncryptionKey,
                "${TaggingContract.ANNOTATION_KEY}${TaggingContract.DELIMITER}"
            )
        }
    }

    @Test
    fun `Given, resolveSearchTags is called with Annotations, it adds the encrypted OrGroup to the SearchTagsPipe`() {
        // Given
        val annotations = listOf(
            "annotation1Value",
            "annotation2Value",
            "annotation3Value"
        )
        val encodedTags = listOf(
            Triple("encodedTag1Value", "encodedAndroidLegacyTag1Value", "encodedJSLegacyTag1Value"),
            Triple("encodedTag2Value", "encodedAndroidLegacyTag2Value", "encodedJSLegacyTag2Value"),
            Triple("encodedTag3Value", "encodedAndroidLegacyTag3Value", "encodedJSLegacyTag3Value")
        )
        val encryptedGroups = listOf<MutableList<String>>(mockk(), mockk(), mockk())

        every { cryptoService.fetchTagEncryptionKey() } returns mockk()

        every { compatibilityEncoder.encode(annotations[0]) } returns encodedTags[0]
        every { compatibilityEncoder.encode(annotations[1]) } returns encodedTags[1]
        every { compatibilityEncoder.encode(annotations[2]) } returns encodedTags[2]

        every {
            tagCryptoService.encryptList(
                encodedTags[0].copy(second = annotations[0]).toList(),
                any(),
                any()
            )
        } returns encryptedGroups[0]

        every {
            tagCryptoService.encryptList(
                encodedTags[1].copy(second = annotations[1]).toList(),
                any(),
                any()
            )
        } returns encryptedGroups[1]

        every {
            tagCryptoService.encryptList(
                encodedTags[2].copy(second = annotations[2]).toList(),
                any(),
                any()
            )
        } returns encryptedGroups[2]

        every { searchTagsBuilderFactory.newBuilder() } returns searchTagsPipe
        every { searchTagsPipe.addOrTuple(encryptedGroups[0]) } returns searchTagsPipe
        every { searchTagsPipe.addOrTuple(encryptedGroups[1]) } returns searchTagsPipe
        every { searchTagsPipe.addOrTuple(encryptedGroups[2]) } returns searchTagsPipe
        every { searchTagsPipe.seal() } returns mockk()

        // When
        service.resolveSearchTags(emptyMap(), annotations)

        // Then
        verify(atLeast = 1) { searchTagsPipe.addOrTuple(encryptedGroups[0]) }
        verify(atLeast = 1) { searchTagsPipe.addOrTuple(encryptedGroups[1]) }
        verify(atLeast = 1) { searchTagsPipe.addOrTuple(encryptedGroups[2]) }
    }
}
