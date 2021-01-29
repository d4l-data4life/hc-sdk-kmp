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
package care.data4life.sdk.tag

import care.data4life.crypto.GCKey
import care.data4life.sdk.CryptoService
import care.data4life.sdk.lang.D4LException
import care.data4life.sdk.tag.TaggingContract.Companion.DELIMITER
import care.data4life.sdk.util.Base64
import com.google.common.truth.Truth
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test

class TagEncryptionServiceTest {
    private lateinit var cryptoService: CryptoService
    private lateinit var base64: Base64
    private lateinit var tagHelper: TaggingContract.Helper
    private lateinit var subjectUnderTest: TagEncryptionService


    @Before
    fun setUp() {
        cryptoService = mockk()
        base64 = mockk()
        tagHelper = mockk()
        subjectUnderTest = TagEncryptionService(cryptoService, base64, tagHelper)
    }

    @Test
    fun encryptTags() {
        // given
        val tag = "key" to "value"
        val tags = hashMapOf(tag)
        val gcKey: GCKey = mockk()
        val encryptedTag = "encryptedTag"
        val symEncrypted = ByteArray(23)

        every { tagHelper.encode(tag.second) } returns tag.second
        every { cryptoService.fetchTagEncryptionKey() } returns gcKey
        every {
            cryptoService.symEncrypt(
                    gcKey,
                    "key${DELIMITER}value".toByteArray(),
                    IV
            )
        } returns symEncrypted
        every { base64.encodeToString(symEncrypted) } returns encryptedTag

        // when
        val encryptedTags: List<String> = subjectUnderTest.encryptTags(tags)

        // then
        Truth.assertThat(encryptedTags).containsExactly(encryptedTag)
        verify {
            cryptoService.symEncrypt(
                    gcKey,
                    "key${DELIMITER}value".toByteArray(),
                    IV
            )
        }
    }

    @Test
    fun `Given encryptTags is called with malicious Tags, it throws fails with a EncryptionFailed Exception`() {
        // given
        val tag = "key" to "value"
        val tags = hashMapOf(tag)
        val gcKey: GCKey = mockk()
        val encryptedTag = "encryptedTag"

        every { tagHelper.encode(tag.second) } returns tag.second
        every { cryptoService.fetchTagEncryptionKey() } returns gcKey
        every {
            cryptoService.symEncrypt(
                    gcKey,
                    encryptedTag.toByteArray(),
                    IV
            )
        } throws RuntimeException("Error")

        // when
        try {
            subjectUnderTest.encryptTags(tags)
            fail("encryptTags should fail")
        } catch (e: Exception) {
            // Then
            assertTrue(e is D4LException)
            assertEquals(
                    "Failed to encrypt tag",
                    e.message
            )
        }
    }

    @Test
    fun decryptTags() {
        // given
        val tag = "key${DELIMITER}value"
        val gcKey: GCKey = mockk()
        val encryptedTag = "encryptedTag"
        val encryptedTags: MutableList<String> = arrayListOf(encryptedTag)

        every { tagHelper.decode(tag) } returns tag
        every { tagHelper.convertToTagMap(listOf(tag)) } returns hashMapOf("key" to "value")
        every { cryptoService.fetchTagEncryptionKey() } returns gcKey
        every { base64.decode(encryptedTag) } returns encryptedTag.toByteArray()
        every {
            cryptoService.symDecrypt(
                    gcKey,
                    encryptedTag.toByteArray(),
                    IV
            )
        } returns tag.toByteArray()

        // when
        val decryptedTags = subjectUnderTest.decryptTags(encryptedTags)

        // then
        Truth.assertThat(decryptedTags).containsExactly("key", "value")
    }

    @Test
    fun decryptTags_filtersAnnotationKey() {
        // given
        val tag = "$ANNOTATION_KEY${DELIMITER}value"
        val gcKey: GCKey = mockk()
        val encryptedTag = "encryptedTag"
        val encryptedTags: MutableList<String> = arrayListOf(encryptedTag)

        every { tagHelper.convertToTagMap(listOf()) } returns hashMapOf()
        every { cryptoService.fetchTagEncryptionKey() } returns gcKey
        every { base64.decode(encryptedTag) } returns encryptedTag.toByteArray()
        every {
            cryptoService.symDecrypt(
                    gcKey,
                    encryptedTag.toByteArray(),
                    IV
            )
        } returns tag.toByteArray()
        every { tagHelper.decode(tag) } returns tag

        // when
        val decryptedTags = subjectUnderTest.decryptTags(encryptedTags)

        // then
        Truth.assertThat(decryptedTags).containsExactly()
    }

    @Test
    fun `Given decryptTags is called with malicious Tags, it throws fails with a DecryptionFailed Exception`() {
        // Given
        val gcKey: GCKey = mockk()
        every { cryptoService.fetchTagEncryptionKey() } returns gcKey

        // When
        try {
            // Then
            subjectUnderTest.decryptTags(mutableListOf("ignore me"))
            fail("decryptTags should fail")
        } catch (e: Exception) {
            assertTrue(e is D4LException)
            assertEquals(
                    "Failed to decrypt tag",
                    e.message
            )
        }
    }

    @Test
    fun encryptAnnotations() {
        // given
        val annotations = listOf("value")
        val gcKey: GCKey = mockk()
        val encryptedAnnotation = "encryptedAnnotation"
        val symEncrypted = ByteArray(23)

        every { tagHelper.encode(annotations[0]) } returns annotations[0]
        every { cryptoService.fetchTagEncryptionKey() } returns gcKey
        every {
            cryptoService.symEncrypt(
                    gcKey,
                    "$ANNOTATION_KEY${DELIMITER}value".toByteArray(),
                    IV
            )
        } returns symEncrypted
        every { base64.encodeToString(symEncrypted) } returns encryptedAnnotation

        // when
        val encryptedAnnotations: List<String> = subjectUnderTest.encryptAnnotations(annotations)

        // then
        Truth.assertThat(encryptedAnnotations).containsExactly(encryptedAnnotation)
        verify {
            cryptoService.symEncrypt(
                    gcKey,
                    "${ANNOTATION_KEY}${DELIMITER}value".toByteArray(),
                    IV
            )
        }
    }

    @Test
    fun `Given encryptAnnotations is called with malicious Annotations, it throws fails with a EncryptionFailed Exception`() {
        // given
        val annotations = listOf("value")
        val gcKey: GCKey = mockk()

        every { tagHelper.encode(annotations[0]) } returns annotations[0]
        every { cryptoService.fetchTagEncryptionKey() } returns gcKey
        every {
            cryptoService.symEncrypt(
                    gcKey,
                    "$ANNOTATION_KEY${DELIMITER}value".toByteArray(),
                    IV
            )
        } throws RuntimeException("Error")

        // when
        try {
            subjectUnderTest.encryptAnnotations(annotations)
            fail("encryptAnnotations should fail")
        } catch (e: Exception) {
            // Then
            assertTrue(e is D4LException)
            assertEquals(
                    "Failed to encrypt tag",
                    e.message
            )
        }
    }

    @Test
    fun decryptAnnotations() {
        // given
        val expected = "value"
        val annotation = "$ANNOTATION_KEY$DELIMITER$expected"
        val gcKey: GCKey = mockk()
        val encryptedAnnotation = "encryptedAnnotation"
        val encryptedAnnotations: MutableList<String> = arrayListOf(encryptedAnnotation)

        every { cryptoService.fetchTagEncryptionKey() } returns gcKey
        every { base64.decode(encryptedAnnotation) } returns encryptedAnnotation.toByteArray()
        every {
            cryptoService.symDecrypt(
                    gcKey,
                    encryptedAnnotation.toByteArray(),
                    IV
            )
        } returns annotation.toByteArray()
        every { tagHelper.decode(annotation) } returns annotation

        // when
        val decryptedAnnotations: List<String> = subjectUnderTest.decryptAnnotations(encryptedAnnotations)

        // then
        Truth.assertThat(decryptedAnnotations).containsExactly(expected)
    }

    @Test
    fun decryptAnnotations_filtersNonAnnotationKey() {
        // given
        val tag = "key=value"
        val gcKey: GCKey = mockk()
        val encryptedTag = "encryptedTag"
        val encryptedAnnotations: MutableList<String> = arrayListOf(encryptedTag)

        every { cryptoService.fetchTagEncryptionKey() } returns gcKey
        every { base64.decode(encryptedTag) } returns encryptedTag.toByteArray()
        every {
            cryptoService.symDecrypt(
                    gcKey,
                    encryptedTag.toByteArray(),
                    IV
            )
        } returns tag.toByteArray()
        every { tagHelper.decode(tag) } returns tag

        // when
        val decryptedAnnotations = subjectUnderTest.decryptAnnotations(encryptedAnnotations)

        // then
        Truth.assertThat(decryptedAnnotations).containsExactly()
    }

    @Test
    fun `Given decryptAnnotations is called with malicious Tags, it throws fails with a DecryptionFailed Exception`() {
        // Given
        val gcKey: GCKey = mockk()
        every { cryptoService.fetchTagEncryptionKey() } returns gcKey

        try {
            // When
            subjectUnderTest.decryptAnnotations(mutableListOf("ignore me"))
            fail("decryptAnnotations should fail")
        } catch (e: Exception) {
            // Then
            assertTrue(e is D4LException)
            assertEquals(
                    "Failed to decrypt tag",
                    e.message
            )
        }
    }

    companion object {
        private const val ANNOTATION_KEY = "custom"
        private val IV = ByteArray(16)
    }
}
