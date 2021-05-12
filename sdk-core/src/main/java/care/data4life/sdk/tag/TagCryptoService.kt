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
import care.data4life.crypto.error.CryptoException
import care.data4life.sdk.crypto.CryptoContract
import care.data4life.sdk.lang.D4LException
import care.data4life.sdk.tag.TaggingContract.Companion.ANNOTATION_KEY
import care.data4life.sdk.tag.TaggingContract.Companion.DELIMITER
import care.data4life.sdk.tag.TaggingContract.CryptoService.Companion.IV
import care.data4life.sdk.util.Base64
import java.io.IOException
import java.nio.charset.StandardCharsets

// TODO internal
class TagCryptoService @JvmOverloads constructor(
    private val cryptoService: CryptoContract.Service,
    private val base64: Base64 = Base64,
    private val tagEncoding: TaggingContract.Encoding = TagEncoding,
    private val tagConverter: TaggingContract.Converter = TagConverter
) : TaggingContract.CryptoService {
    @Throws(D4LException::class)
    override fun encryptTagsAndAnnotations(
        tags: Tags,
        annotations: Annotations
    ): EncryptedTagsAndAnnotations {
        val encryptionKey = cryptoService.fetchTagEncryptionKey()

        return encryptAndEncodeTags(tags, encryptionKey)
            .also { encryptedTags ->
                encryptedTags.addAll(encryptAndEncodeAnnotations(annotations, encryptionKey))
            }
    }

    @Throws(IOException::class)
    private fun encryptAndEncodeTags(
        tags: Tags,
        encryptionKey: GCKey
    ): MutableList<String> {
        return tags
            .map { entry -> entry.key + DELIMITER + tagEncoding.encode(entry.value) }
            .let { encodedTags -> encryptList(encodedTags, encryptionKey) }
    }

    @Throws(IOException::class)
    private fun encryptAndEncodeAnnotations(
        annotations: Annotations,
        encryptionKey: GCKey
    ): MutableList<String> {
        return annotations
            .map { annotation -> tagEncoding.encode(annotation) }
            .let { validAnnotations ->
                encryptList(
                    validAnnotations,
                    encryptionKey,
                    ANNOTATION_KEY + DELIMITER
                )
            }
    }

    @Throws(D4LException::class)
    override fun decryptTagsAndAnnotations(
        encryptedTagsAndAnnotations: EncryptedTagsAndAnnotations
    ): Pair<Tags, Annotations> {
        val encryptionKey = cryptoService.fetchTagEncryptionKey()

        val tags = decryptTags(encryptedTagsAndAnnotations, encryptionKey)
        val annotations = decryptAnnotations(encryptedTagsAndAnnotations, encryptionKey)

        return tags to annotations
    }

    @Throws(IOException::class)
    private fun decryptTags(
        encryptedTags: List<String>,
        encryptionKey: GCKey
    ): Tags = decryptList(
        encryptedTags,
        encryptionKey,
        { decrypted -> !decrypted.startsWith(ANNOTATION_KEY) && decrypted.contains(DELIMITER) },
        { tagList: List<String> -> tagConverter.toTags(tagList) }
    )

    @Throws(IOException::class)
    private fun decryptAnnotations(
        encryptedAnnotations: List<String>,
        encryptionKey: GCKey
    ): Annotations = decryptList(
        encryptedAnnotations,
        encryptionKey,
        { decrypted -> decrypted.startsWith(ANNOTATION_KEY) && decrypted.contains(DELIMITER) },
        { list -> removeAnnotationKey(list) }
    )

    @Throws(IOException::class)
    override fun encryptList(
        plainList: List<String>,
        encryptionKey: GCKey,
        prefix: String
    ): MutableList<String> {
        return plainList.map { tag ->
            encryptItem(
                encryptionKey,
                prefix + tag
            )
        }.toMutableList()
    }

    @Throws(D4LException::class)
    private fun encryptItem(key: GCKey, tag: String): String {
        return cryptoService.symEncrypt(key, tag.toByteArray(), IV)
                .let { data -> base64.encodeToString(data) }//try {
        /*} catch (e: Exception) {
            throw CryptoException.EncryptionFailed("Failed to encrypt tag")
        }*/
    }

    @Throws(IOException::class)
    private fun <T> decryptList(
        encryptedList: List<String>,
        encryptionKey: GCKey,
        condition: (decryptedItem: String) -> Boolean,
        transform: (decryptedList: MutableList<String>) -> T
    ): T {
        return encryptedList
            .map { encryptedTag -> decryptItem(encryptionKey, encryptedTag) }
            .map { encodedTag -> tagEncoding.decode(encodedTag) }
            .filter { decryptedItem -> condition(decryptedItem) }
            .let { decryptedList -> transform(decryptedList.toMutableList()) }
    }

    @Throws(D4LException::class)
    private fun decryptItem(key: GCKey, base64tag: String): String {
        return try {
            base64.decode(base64tag)
                .let { encrypted -> cryptoService.symDecrypt(key, encrypted, IV) }
                .let { decrypted -> String(decrypted, StandardCharsets.UTF_8) }
        } catch (e: Exception) {
            throw CryptoException.DecryptionFailed("Failed to decrypt tag")
        }
    }

    companion object {
        @JvmStatic
        private fun removeAnnotationKey(
            list: MutableList<String>
        ): List<String> = list.also {
            for (idx in it.indices) {
                it[idx] = it[idx].replaceFirst(ANNOTATION_KEY + DELIMITER, "")
            }
        }
    }
}
