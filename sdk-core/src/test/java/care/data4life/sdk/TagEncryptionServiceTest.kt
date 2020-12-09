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

import care.data4life.crypto.GCKey
import care.data4life.sdk.test.util.TestSchedulerRule
import care.data4life.sdk.util.Base64
import com.google.common.truth.Truth
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mockito

class TagEncryptionServiceTest {
    @Rule
    @JvmField
    var schedulerRule = TestSchedulerRule()
    private lateinit var mockCryptoService: CryptoService
    private lateinit var mockBase64: Base64
    private lateinit var sut: TagEncryptionService

    @Before
    fun setUp() {
        mockCryptoService = Mockito.mock(CryptoService::class.java)
        mockBase64 = Mockito.mock(Base64::class.java)
        sut = TagEncryptionService(mockCryptoService, mockBase64)
    }

    @Test
    @Throws(Exception::class)
    fun encryptTags() {
        // given
        val tag = "key" to "value"
        val tags = hashMapOf(tag)
        val gcKey = Mockito.mock(GCKey::class.java)
        val encryptedTag = "encryptedTag"
        val symEncrypted = ByteArray(23)

        Mockito.doReturn(gcKey)
                .`when`(mockCryptoService)
                .fetchTagEncryptionKey()
        Mockito.doReturn(symEncrypted)
                .`when`(mockCryptoService)
                .symEncrypt(
                        gcKey,
                        "key=value".toByteArray(),
                        IV
                )
        Mockito.doReturn(encryptedTag)
                .`when`(mockBase64)
                .encodeToString(symEncrypted)


        // when
        val encryptedTags: List<String> = sut.encryptTags(tags)

        // then
        Truth.assertThat(encryptedTags).containsExactly(encryptedTag)
        Mockito.verify(mockCryptoService)
                .symEncrypt(
                        gcKey,
                        "key=value".toByteArray(),
                        IV
                )
    }

    @Test(expected = RuntimeException::class)
    @Throws(Exception::class)
    fun encryptTags_shouldThrowException() {
        // given
        val tag = "key" to "value"
        val tags = hashMapOf(tag)
        val gcKey = Mockito.mock(GCKey::class.java)
        val encryptedTag = "encryptedTag"
        val symEncrypted = ByteArray(23)

        Mockito.doReturn(gcKey)
                .`when`(mockCryptoService)
                .fetchTagEncryptionKey()
        Mockito.doReturn(symEncrypted)
                .`when`(mockCryptoService)
                .symDecrypt(
                        gcKey,
                        encryptedTag.toByteArray(),
                        IV
                )
        Mockito.doReturn(null)
                .`when`(mockBase64)
                .encodeToString(symEncrypted)

        // when
        sut.encryptTags(tags)
    }

    @Test
    @Throws(Exception::class)
    fun decryptTags() {
        // given
        val tag = "key=value"
        val gcKey = Mockito.mock(GCKey::class.java)
        val encryptedTag = "encryptedTag"
        val encryptedTags: MutableList<String> = arrayListOf(encryptedTag)

        Mockito.doReturn(gcKey)
                .`when`(mockCryptoService)
                .fetchTagEncryptionKey()
        Mockito.doReturn(encryptedTag.toByteArray())
                .`when`(mockBase64)
                .decode(encryptedTag)
        Mockito.doReturn(tag.toByteArray())
                .`when`(mockCryptoService)
                .symDecrypt(
                        gcKey,
                        encryptedTag.toByteArray(),
                        IV
                )

        // when
        val decryptedTags = sut.decryptTags(encryptedTags)

        // then
        Truth.assertThat(decryptedTags).containsExactly("key", "value")
    }

    @Test
    @Throws(Exception::class)
    fun decryptTags_filtersAnnotationKey() {
        // given
        val tag = "${ANNOTATION_KEY}value"
        val gcKey = Mockito.mock(GCKey::class.java)
        val encryptedTag = "encryptedTag"
        val encryptedTags: MutableList<String> = arrayListOf(encryptedTag)

        Mockito.doReturn(gcKey)
                .`when`(mockCryptoService)
                .fetchTagEncryptionKey()
        Mockito.doReturn(encryptedTag.toByteArray())
                .`when`(mockBase64)
                .decode(encryptedTag)
        Mockito.doReturn(tag.toByteArray())
                .`when`(mockCryptoService)
                .symDecrypt(
                        gcKey,
                        encryptedTag.toByteArray(),
                        IV
                )

        // when
        val decryptedTags = sut.decryptTags(encryptedTags)

        // then
        Truth.assertThat(decryptedTags).containsExactly()
    }

    @Test(expected = RuntimeException::class)
    @Throws(Exception::class)
    fun decryptTags_shouldThrowException() {
        // when
        sut.decryptTags(mutableListOf("ignore me"))
    }

    @Test
    @Throws(Exception::class)
    fun encryptAnnotations() {
        // given
        val annotations = listOf("value")
        val gcKey = Mockito.mock(GCKey::class.java)
        val encryptedTag = "encryptedAnnotation"
        val symEncrypted = ByteArray(23)

        Mockito.doReturn(gcKey)
                .`when`(mockCryptoService)
                .fetchTagEncryptionKey()
        Mockito.doReturn(symEncrypted)
                .`when`(mockCryptoService)
                .symEncrypt(
                        gcKey,
                        "${ANNOTATION_KEY}value".toByteArray(),
                        IV
                )
        Mockito.doReturn(encryptedTag)
                .`when`(mockBase64)
                .encodeToString(symEncrypted)

        // when
        val encryptedAnnotations: List<String> = sut.encryptAnnotations(annotations)

        // then
        Truth.assertThat(encryptedAnnotations).containsExactly(encryptedTag)
        Mockito.verify(mockCryptoService)
                .symEncrypt(
                        gcKey,
                        "${ANNOTATION_KEY}value".toByteArray(),
                        IV
                )
    }

    @Test(expected = RuntimeException::class)
    @Throws(Exception::class)
    fun encryptAnnotations_shouldThrowException() {
        // given
        val annotations = listOf("value")
        val gcKey = Mockito.mock(GCKey::class.java)
        val symEncrypted = ByteArray(23)

        Mockito.doReturn(gcKey)
                .`when`(mockCryptoService)
                .fetchTagEncryptionKey()
        Mockito.doReturn(symEncrypted)
                .`when`(mockCryptoService)
                .symEncrypt(
                        gcKey,
                        "${ANNOTATION_KEY}value".toByteArray(),
                        IV
                )
        Mockito.doReturn(null)
                .`when`(mockBase64)
                .encodeToString(symEncrypted)

        // when
        sut.encryptAnnotations(annotations)
    }

    @Test
    @Throws(Exception::class)
    fun decryptAnnotations() {
        // given
        val expected = "value"
        val tag = "${ANNOTATION_KEY}$expected"
        val gcKey = Mockito.mock(GCKey::class.java)
        val encryptedTag = "encryptedTag"
        val encryptedAnnotations: MutableList<String> = arrayListOf(encryptedTag)

        Mockito.doReturn(gcKey)
                .`when`(mockCryptoService)
                .fetchTagEncryptionKey()
        Mockito.doReturn(encryptedTag.toByteArray())
                .`when`(mockBase64)
                .decode(encryptedTag)
        Mockito.doReturn(tag.toByteArray())
                .`when`(mockCryptoService)
                .symDecrypt(
                        gcKey,
                        encryptedTag.toByteArray(),
                        IV
                )
        // when
        val decryptedAnnotations: List<String> = sut.decryptAnnotations(encryptedAnnotations)

        // then
        Truth.assertThat(decryptedAnnotations).containsExactly(expected)
    }

    @Test
    @Throws(Exception::class)
    fun decryptAnnotations_filtersNonAnnotationKey() {
        // given
        val tag = "key=value"
        val gcKey = Mockito.mock(GCKey::class.java)
        val encryptedTag = "encryptedTag"
        val encryptedAnnotations: MutableList<String> = arrayListOf(encryptedTag)

        Mockito.doReturn(gcKey)
                .`when`(mockCryptoService)
                .fetchTagEncryptionKey()
        Mockito.doReturn(encryptedTag.toByteArray())
                .`when`(mockBase64)
                .decode(encryptedTag)
        Mockito.doReturn(tag.toByteArray())
                .`when`(mockCryptoService)
                .symDecrypt(
                        gcKey,
                        encryptedTag.toByteArray(),
                        IV
                )

        // when
        val decryptedAnnotations = sut.decryptAnnotations(encryptedAnnotations)

        // then
        Truth.assertThat(decryptedAnnotations).containsExactly()
    }

    @Test(expected = RuntimeException::class)
    @Throws(Exception::class)
    fun decryptAnnotations_shouldThrowException() {
        // given
        sut.decryptAnnotations(mutableListOf("ignore me"))
    }

    companion object {
        private const val ANNOTATION_KEY = "custom" + TaggingService.TAG_DELIMITER
        private val IV = ByteArray(16)
    }
}
