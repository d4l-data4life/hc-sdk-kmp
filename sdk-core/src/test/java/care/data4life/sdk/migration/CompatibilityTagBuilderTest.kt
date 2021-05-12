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
import care.data4life.sdk.network.NetworkingContract
import care.data4life.sdk.network.util.SearchTagsPipe
import care.data4life.sdk.tag.TaggingContract
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import io.mockk.verify
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertSame
import kotlin.test.assertTrue

class CompatibilityTagBuilderTest {
    private val tagCryptoService: TaggingContract.CryptoService = mockk()
    private val searchTagsPipe: NetworkingContract.SearchTagsPipeIn = mockk()

    @Before
    fun setUp() {
        clearAllMocks()
        mockkObject(SearchTagsPipe)

        every { SearchTagsPipe.newPipe() } returns searchTagsPipe
    }

    @After
    fun tearDown() {
        unmockkObject(SearchTagsPipe)
    }

    @Test
    fun `It fulfils CompatibilityTagBuilderFactory`() {
        val factory: Any = CompatibilityTagBuilder

        assertTrue(factory is MigrationContract.CompatibilityTagBuilderFactory)
    }

    @Test
    fun `Given newBuilder is called with its proper Arguments, it returns a CompatibilityTagBuilder`() {
        val builder: Any = CompatibilityTagBuilder.newBuilder(tagCryptoService, mockk())

        assertTrue(builder is MigrationContract.CompatibilityTagBuilder)
    }

    @Test
    fun `Given newBuilder is called with its proper Arguments, it initializes a new SearchTagsBuilder`() {
        CompatibilityTagBuilder.newBuilder(tagCryptoService, mockk())

        verify(exactly = 1) { SearchTagsPipe.newPipe() }
    }

    @Test
    fun `Given, add is called with its proper Arguments, it returns a CompatibilityTagBuilder`() {
        // Given
        val tagEncryptionKey: GCKey = mockk()
        val builder = CompatibilityTagBuilder.newBuilder(tagCryptoService, tagEncryptionKey)

        every { tagCryptoService.encryptList(any(), any(), any()) } returns mockk()
        every { searchTagsPipe.addOrTuple(any()) } returns searchTagsPipe

        // When
        val referencedBuilder: Any = builder.add("NotImportant", mockk(relaxed = true))

        // Then
        assertSame(
            actual = referencedBuilder,
            expected = builder
        )
    }

    @Test
    fun `Given, add is called with TagGroupKey and a TagGroupValues, it uses the TagKey and Separator as Prefix and delegates it with the TagGroupValues to the TagCryptoService`() {
        // Given
        val tagEncryptionKey: GCKey = mockk()
        val builder = CompatibilityTagBuilder.newBuilder(tagCryptoService, tagEncryptionKey)
        val encodedTags = mapOf(
            "tag1" to Triple("encodedTag1Value", "encodedAndroidLegacyTag1Value", "encodedJSLegacyTag1Value"),
            "tag2" to Triple("encodedTag2Value", "encodedAndroidLegacyTag2Value", "encodedJSLegacyTag2Value"),
            "tag3" to Triple("encodedTag3Value", "encodedAndroidLegacyTag3Value", "encodedJSLegacyTag3Value")
        )

        every { searchTagsPipe.addOrTuple(any()) } returns searchTagsPipe

        every {
            tagCryptoService.encryptList(
                encodedTags.values.toList()[0].toList(),
                tagEncryptionKey,
                "${encodedTags.keys.toList()[0]}${TaggingContract.DELIMITER}"
            )
        } returns mockk()

        every {
            tagCryptoService.encryptList(
                encodedTags.values.toList()[1].toList(),
                tagEncryptionKey,
                "${encodedTags.keys.toList()[1]}${TaggingContract.DELIMITER}"
            )
        } returns mockk()

        every {
            tagCryptoService.encryptList(
                encodedTags.values.toList()[2].toList(),
                tagEncryptionKey,
                "${encodedTags.keys.toList()[2]}${TaggingContract.DELIMITER}"
            )
        } returns mockk()

        // When
        encodedTags.forEach { tagGroup ->
            builder.add(
                tagGroup.key,
                tagGroup.value
            )
        }

        // Then
        verify(atLeast = 1) {
            tagCryptoService.encryptList(
                encodedTags.values.toList()[0].toList(),
                tagEncryptionKey,
                "${encodedTags.keys.toList()[0]}${TaggingContract.DELIMITER}"
            )
        }

        verify(atLeast = 1) {
            tagCryptoService.encryptList(
                encodedTags.values.toList()[1].toList(),
                tagEncryptionKey,
                "${encodedTags.keys.toList()[1]}${TaggingContract.DELIMITER}"
            )
        }

        verify(atLeast = 1) {
            tagCryptoService.encryptList(
                encodedTags.values.toList()[2].toList(),
                tagEncryptionKey,
                "${encodedTags.keys.toList()[2]}${TaggingContract.DELIMITER}"
            )
        }
    }

    @Test
    fun `Given, add is called with TagGroupKey and a TagGroupValues, it adds the encrypted OrGroup to the SearchTagsPipe`() {
        // Given
        val tagEncryptionKey: GCKey = mockk()
        val builder = CompatibilityTagBuilder.newBuilder(tagCryptoService, tagEncryptionKey)
        val encodedTags = mapOf(
            "tag1" to Triple("encodedTag1Value", "encodedAndroidLegacyTag1Value", "encodedJSLegacyTag1Value"),
            "tag2" to Triple("encodedTag2Value", "encodedAndroidLegacyTag2Value", "encodedJSLegacyTag2Value"),
            "tag3" to Triple("encodedTag3Value", "encodedAndroidLegacyTag3Value", "encodedJSLegacyTag3Value")
        )
        val encryptedGroups = listOf<MutableList<String>>(mockk(), mockk(), mockk())

        every {
            tagCryptoService.encryptList(encodedTags.values.toList()[0].toList(), any(), any())
        } returns encryptedGroups[0]

        every {
            tagCryptoService.encryptList(encodedTags.values.toList()[1].toList(), any(), any())
        } returns encryptedGroups[1]

        every {
            tagCryptoService.encryptList(encodedTags.values.toList()[2].toList(), any(), any())
        } returns encryptedGroups[2]

        every { searchTagsPipe.addOrTuple(encryptedGroups[0]) } returns searchTagsPipe
        every { searchTagsPipe.addOrTuple(encryptedGroups[1]) } returns searchTagsPipe
        every { searchTagsPipe.addOrTuple(encryptedGroups[2]) } returns searchTagsPipe

        // When
        encodedTags.forEach { tagGroup ->
            builder.add(
                tagGroup.key,
                tagGroup.value
            )
        }

        // Then
        verify(atLeast = 1) { searchTagsPipe.addOrTuple(encryptedGroups[0]) }
        verify(atLeast = 1) { searchTagsPipe.addOrTuple(encryptedGroups[1]) }
        verify(atLeast = 1) { searchTagsPipe.addOrTuple(encryptedGroups[2]) }
    }

    @Test
    fun `Given, build is called after an arbitrary amount of calling add, it returns the resulting reading end of the pipe`() {
        // Given
        val tagEncryptionKey: GCKey = mockk()
        val builder = CompatibilityTagBuilder.newBuilder(tagCryptoService, tagEncryptionKey)
        val pipeOut: NetworkingContract.SearchTagsPipeOut = mockk()
        val encodedTags = mapOf(
            "tag1" to Triple("encodedTag1Value", "encodedAndroidLegacyTag1Value", "encodedJSLegacyTag1Value"),
            "tag2" to Triple("encodedTag2Value", "encodedAndroidLegacyTag2Value", "encodedJSLegacyTag2Value"),
            "tag3" to Triple("encodedTag3Value", "encodedAndroidLegacyTag3Value", "encodedJSLegacyTag3Value")
        )

        every {
            tagCryptoService.encryptList(any(), any(), any())
        } returns mockk()
        every { searchTagsPipe.addOrTuple(any()) } returns searchTagsPipe
        every { searchTagsPipe.seal() } returns pipeOut

        // When
        encodedTags.forEach { tagGroup ->
            builder.add(
                tagGroup.key,
                tagGroup.value
            )
        }

        val pipe: Any = builder.build()

        // Then
        assertTrue(pipe is NetworkingContract.SearchTagsPipeOut)

        verify(exactly = 1) { searchTagsPipe.seal() }
    }
}
