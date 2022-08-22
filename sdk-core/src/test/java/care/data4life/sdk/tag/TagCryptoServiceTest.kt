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

import care.data4life.sdk.crypto.CryptoContract
import care.data4life.sdk.crypto.GCKey
import care.data4life.sdk.lang.D4LException
import care.data4life.sdk.tag.TaggingContract.Companion.ANNOTATION_KEY
import care.data4life.sdk.tag.TaggingContract.Companion.DELIMITER
import care.data4life.sdk.test.util.GenericTestDataProvider.IV
import care.data4life.sdk.util.Base64
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import org.junit.Before
import org.junit.Test

class TagCryptoServiceTest {
    private val cryptoService: CryptoContract.Service = mockk()
    private val base64: Base64 = mockk()
    private val tagEncoding: TaggingContract.Encoding = mockk()
    private val tagConverter: TaggingContract.Converter = mockk()
    private lateinit var tagEncryptionService: TagCryptoService

    @Before
    fun setUp() {
        clearAllMocks()
        tagEncryptionService = TagCryptoService(cryptoService, base64, tagEncoding, tagConverter)
    }

    @Test
    fun `It fulfils the EncryptionService`() {
        val helper: Any = tagEncryptionService
        assertTrue(helper is TaggingContract.CryptoService)
    }

    @Test
    fun `Given encryptTagsAndAnnotations is called, it encrypts the given Tags`() {
        // given
        val tag = "key" to "value"
        val tags = hashMapOf(tag)
        val gcKey: GCKey = mockk()
        val encryptedTag = "encryptedTag"
        val symEncrypted = ByteArray(23)

        every { tagEncoding.encode(tag.second) } returns tag.second
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
        val actual = tagEncryptionService.encryptTagsAndAnnotations(tags, listOf())

        // then
        assertEquals(
            actual = actual,
            expected = listOf(encryptedTag)
        )

        verify(exactly = 1) { cryptoService.fetchTagEncryptionKey() }
        verify(exactly = 1) { tagEncoding.encode(tag.second) }
        verify(exactly = 1) { base64.encodeToString(symEncrypted) }
        verify(exactly = 1) {
            cryptoService.symEncrypt(
                gcKey,
                "key${DELIMITER}value".toByteArray(),
                IV
            )
        }
    }

    @Test
    fun `Given encryptTagsAndAnnotations is called, it encrypts the given Annotations`() {
        // Given
        val annotations = listOf("value")
        val gcKey: GCKey = mockk()
        val encryptedAnnotation = "encryptedAnnotation"
        val symEncrypted = ByteArray(23)

        every { tagEncoding.encode(annotations[0]) } returns annotations[0]
        every { cryptoService.fetchTagEncryptionKey() } returns gcKey
        every {
            cryptoService.symEncrypt(
                gcKey,
                "$ANNOTATION_KEY${DELIMITER}value".toByteArray(),
                IV
            )
        } returns symEncrypted
        every { base64.encodeToString(symEncrypted) } returns encryptedAnnotation

        // When
        val actual = tagEncryptionService.encryptTagsAndAnnotations(hashMapOf(), annotations)

        // Then
        assertEquals(
            actual = actual,
            expected = listOf(encryptedAnnotation)
        )

        verify(exactly = 1) { cryptoService.fetchTagEncryptionKey() }
        verify(exactly = 1) { tagEncoding.encode(annotations[0]) }
        verify(exactly = 1) { base64.encodeToString(symEncrypted) }
        verify {
            cryptoService.symEncrypt(
                gcKey,
                "${ANNOTATION_KEY}${DELIMITER}value".toByteArray(),
                IV
            )
        }
    }

    @Test
    fun `Given encryptTagsAndAnnotations is called, it encrypts the given Tags and Annotations it merges them`() {
        // Given
        val tag = "key" to "value"
        val tags = hashMapOf(tag)
        val annotations = listOf("value")
        val gcKey: GCKey = mockk()
        val encryptedTag = "encryptedTag"
        val encryptedAnnotation = "encryptedAnnotation"
        val symEncrypted = listOf(ByteArray(23), ByteArray(42))

        every { tagEncoding.encode(tag.second) } returns tag.second
        every { tagEncoding.encode(annotations[0]) } returns annotations[0]
        every { cryptoService.fetchTagEncryptionKey() } returns gcKey
        every {
            cryptoService.symEncrypt(
                gcKey,
                "key${DELIMITER}value".toByteArray(),
                IV
            )
        } returns symEncrypted[0]
        every {
            cryptoService.symEncrypt(
                gcKey,
                "$ANNOTATION_KEY${DELIMITER}value".toByteArray(),
                IV
            )
        } returns symEncrypted[1]
        every { base64.encodeToString(symEncrypted[0]) } returns encryptedTag
        every { base64.encodeToString(symEncrypted[1]) } returns encryptedAnnotation

        // When
        val actual = tagEncryptionService.encryptTagsAndAnnotations(tags, annotations)

        // Then
        assertEquals(
            actual = actual,
            expected = listOf(encryptedTag, encryptedAnnotation)
        )
        verify(exactly = 1) { cryptoService.fetchTagEncryptionKey() }
        verify(exactly = 2) { tagEncoding.encode(or(tag.second, annotations[0])) }
        verify(exactly = 2) {
            base64.encodeToString(or(symEncrypted[0], symEncrypted[1]))
        }
        verify(exactly = 2) {
            cryptoService.symEncrypt(
                gcKey,
                or(
                    "key${DELIMITER}value".toByteArray(),
                    "${ANNOTATION_KEY}${DELIMITER}value".toByteArray()
                ),
                IV
            )
        }
    }

    @Test
    fun `Given encryptTagsAndAnnotations is called with malicious Tags, it throws fails with a EncryptionFailed Exception`() {
        // Given
        val tag = "key" to "value"
        val tags = hashMapOf(tag)
        val gcKey: GCKey = mockk()
        val encryptedTag = "encryptedTag"

        every { tagEncoding.encode(tag.second) } returns tag.second
        every { cryptoService.fetchTagEncryptionKey() } returns gcKey
        every {
            cryptoService.symEncrypt(
                gcKey,
                encryptedTag.toByteArray(),
                IV
            )
        } throws RuntimeException("Error")

        // When
        val exception = assertFailsWith<D4LException> {
            tagEncryptionService.encryptTagsAndAnnotations(tags, listOf())
        }

        // Then
        assertEquals(
            expected = "Failed to encrypt tag",
            actual = exception.message
        )
    }

    @Test
    fun `Given encryptTagsAndAnnotations is called with malicious Annotations, it throws fails with a EncryptionFailed Exception`() {
        // Given
        val annotations = listOf("value")
        val gcKey: GCKey = mockk()

        every { tagEncoding.encode(annotations[0]) } returns annotations[0]
        every { cryptoService.fetchTagEncryptionKey() } returns gcKey
        every {
            cryptoService.symEncrypt(
                gcKey,
                "$ANNOTATION_KEY${DELIMITER}value".toByteArray(),
                IV
            )
        } throws RuntimeException("Error")

        // When
        val exception = assertFailsWith<D4LException> {
            tagEncryptionService.encryptTagsAndAnnotations(hashMapOf(), annotations)
        }

        // Then
        assertEquals(
            expected = "Failed to encrypt tag",
            actual = exception.message
        )
    }

    @Test
    fun `Given decryptTagsAndAnnotations is called with encrypted Tags, it decrypts and decodes the given Tags and return the result`() {
        // given
        val tag = "key${DELIMITER}value"
        val gcKey: GCKey = mockk()
        val encryptedTag = "encryptedTag"
        val encryptedTagsAndAnnotations: MutableList<String> = arrayListOf(encryptedTag)

        every { tagEncoding.decode(tag) } returns tag
        every { tagConverter.toTags(listOf(tag)) } returns hashMapOf("key" to "value")
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
        val (decryptedTags, _) = tagEncryptionService.decryptTagsAndAnnotations(
            encryptedTagsAndAnnotations
        )

        // then
        assertEquals(
            actual = decryptedTags,
            expected = hashMapOf("key" to "value")
        )

        verify(exactly = 2) { tagEncoding.decode(tag) }
        verify(exactly = 2) { base64.decode(encryptedTag) }
        verify(exactly = 2) {
            cryptoService.symDecrypt(
                gcKey,
                encryptedTag.toByteArray(),
                IV
            )
        }
        verify(exactly = 1) { tagConverter.toTags(listOf(tag)) }
        verify(exactly = 1) { cryptoService.fetchTagEncryptionKey() }
    }

    @Test
    fun `Given decryptTagsAndAnnotations is called with encrypted Tags, it filters AnnotationKey while decrypting Tags`() {
        // given
        val tag = "$ANNOTATION_KEY${DELIMITER}value"
        val gcKey: GCKey = mockk()
        val encryptedTag = "encryptedTag"
        val encryptedTags: MutableList<String> = arrayListOf(encryptedTag)

        every { tagConverter.toTags(listOf()) } returns hashMapOf()
        every { cryptoService.fetchTagEncryptionKey() } returns gcKey
        every { base64.decode(encryptedTag) } returns encryptedTag.toByteArray()
        every {
            cryptoService.symDecrypt(
                gcKey,
                encryptedTag.toByteArray(),
                IV
            )
        } returns tag.toByteArray()
        every { tagEncoding.decode(tag) } returns tag

        // when
        val (decryptedTags, _) = tagEncryptionService.decryptTagsAndAnnotations(encryptedTags)

        // then
        assertEquals(
            actual = decryptedTags,
            expected = hashMapOf()
        )

        verify(exactly = 2) { tagEncoding.decode(tag) }
        verify(exactly = 2) { base64.decode(encryptedTag) }
        verify(exactly = 2) {
            cryptoService.symDecrypt(
                gcKey,
                encryptedTag.toByteArray(),
                IV
            )
        }
        verify(exactly = 1) { tagConverter.toTags(listOf()) }
        verify(exactly = 1) { cryptoService.fetchTagEncryptionKey() }
    }

    @Test
    fun `Given decryptTagsAndAnnotations is called with encrypted Tags, it decrypts and decodes the given encrypted Annotations and return the result`() {
        // Given
        val expected = "value"
        val annotation = "$ANNOTATION_KEY$DELIMITER$expected"
        val gcKey: GCKey = mockk()
        val encryptedAnnotation = "encryptedAnnotation"
        val encryptedAnnotations: MutableList<String> = arrayListOf(encryptedAnnotation)

        every { cryptoService.fetchTagEncryptionKey() } returns gcKey
        every { tagConverter.toTags(listOf()) } returns hashMapOf()
        every { base64.decode(encryptedAnnotation) } returns encryptedAnnotation.toByteArray()
        every {
            cryptoService.symDecrypt(
                gcKey,
                encryptedAnnotation.toByteArray(),
                IV
            )
        } returns annotation.toByteArray()
        every { tagEncoding.decode(annotation) } returns annotation

        // When
        val (_, decryptedAnnotations) = tagEncryptionService.decryptTagsAndAnnotations(
            encryptedAnnotations
        )

        // Then
        assertEquals(
            actual = decryptedAnnotations,
            expected = listOf(expected)
        )

        verify(exactly = 2) { tagEncoding.decode(annotation) }
        verify(exactly = 2) { base64.decode(encryptedAnnotation) }
        verify(exactly = 2) {
            cryptoService.symDecrypt(
                gcKey,
                encryptedAnnotation.toByteArray(),
                IV
            )
        }
        verify(exactly = 1) { tagConverter.toTags(listOf()) }
        verify(exactly = 1) { cryptoService.fetchTagEncryptionKey() }
    }

    @Test
    fun `Given decryptTagsAndAnnotations is called with encrypted Tags, it filters Keys, which are not the AnnotationKey, while decrypting`() {
        // given
        val tag = "key=value"
        val gcKey: GCKey = mockk()
        val encryptedTag = "encryptedTag"
        val encryptedAnnotations: MutableList<String> = arrayListOf(encryptedTag)

        every { cryptoService.fetchTagEncryptionKey() } returns gcKey
        every { tagConverter.toTags(listOf(tag)) } returns hashMapOf("key" to "value")
        every { base64.decode(encryptedTag) } returns encryptedTag.toByteArray()
        every {
            cryptoService.symDecrypt(
                gcKey,
                encryptedTag.toByteArray(),
                IV
            )
        } returns tag.toByteArray()
        every { tagEncoding.decode(tag) } returns tag

        // when
        val (_, decryptedAnnotations) = tagEncryptionService.decryptTagsAndAnnotations(
            encryptedAnnotations
        )

        // then
        assertEquals(
            actual = decryptedAnnotations,
            expected = listOf()
        )

        verify(exactly = 2) { tagEncoding.decode(tag) }
        verify(exactly = 2) { base64.decode(encryptedTag) }
        verify(exactly = 2) {
            cryptoService.symDecrypt(
                gcKey,
                encryptedTag.toByteArray(),
                IV
            )
        }
        verify(exactly = 1) { tagConverter.toTags(listOf(tag)) }
        verify(exactly = 1) { cryptoService.fetchTagEncryptionKey() }
    }

    @Test
    fun `Given decryptTagsAndAnnotations is called with encrypted Tags, it returns Tags and Annotations`() {
        // given
        val tag = "key=value"
        val gcKey: GCKey = mockk()
        val encryptedTag = "encryptedTag"
        val annotation = "$ANNOTATION_KEY${DELIMITER}test"
        val encryptedAnnotation = "encryptedAnnotation"
        val encryptedTagsAnnotations = mutableListOf(
            encryptedTag,
            encryptedAnnotation
        )

        every { cryptoService.fetchTagEncryptionKey() } returns gcKey
        every { tagConverter.toTags(listOf(tag)) } returns hashMapOf("key" to "value")
        every { base64.decode(encryptedTag) } returns encryptedTag.toByteArray()
        every {
            cryptoService.symDecrypt(
                gcKey,
                encryptedTag.toByteArray(),
                IV
            )
        } returns tag.toByteArray()
        every { tagEncoding.decode(tag) } returns tag

        every { base64.decode(encryptedAnnotation) } returns encryptedAnnotation.toByteArray()
        every {
            cryptoService.symDecrypt(
                gcKey,
                encryptedAnnotation.toByteArray(),
                IV
            )
        } returns annotation.toByteArray()
        every { tagEncoding.decode(annotation) } returns annotation

        // when
        val (decryptedTags, decryptedAnnotations) = tagEncryptionService.decryptTagsAndAnnotations(
            encryptedTagsAnnotations
        )

        // then
        assertEquals(
            actual = decryptedTags,
            expected = hashMapOf("key" to "value")
        )
        assertEquals(
            actual = decryptedAnnotations,
            expected = listOf("test")
        )

        verify(exactly = 2) { tagEncoding.decode(tag) }
        verify(exactly = 2) { base64.decode(encryptedTag) }
        verify(exactly = 2) {
            cryptoService.symDecrypt(
                gcKey,
                encryptedTag.toByteArray(),
                IV
            )
        }
        verify(exactly = 1) { tagConverter.toTags(listOf(tag)) }
        verify(exactly = 1) { cryptoService.fetchTagEncryptionKey() }
    }

    @Test
    fun `Given decryptTagsAndAnnotations is called with malicious Tags, it throws fails with a DecryptionFailed Exception`() {
        // Given
        val gcKey: GCKey = mockk()
        every { cryptoService.fetchTagEncryptionKey() } returns gcKey

        // When
        val exception = assertFailsWith<D4LException> {
            tagEncryptionService.decryptTagsAndAnnotations(mutableListOf("ignore me"))
        }

        // Then
        assertEquals(
            expected = "Failed to decrypt tag",
            actual = exception.message
        )
    }
}
