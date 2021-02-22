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
import care.data4life.sdk.CryptoService
import care.data4life.sdk.lang.D4LException
import care.data4life.sdk.tag.TaggingContract.Companion.DELIMITER
import care.data4life.sdk.util.Base64
import java.io.IOException
import java.nio.charset.StandardCharsets
import kotlin.collections.HashMap

// TODO internal
class TagEncryptionService @JvmOverloads constructor(
        private val cryptoService: CryptoService,
        private val base64: Base64 = Base64,
        private val tagHelper: TaggingContract.Helper = TagEncryptionHelper
) : TaggingContract.EncryptionService {
    @Throws(IOException::class)
    override fun encryptTags(tags: HashMap<String, String>): MutableList<String> {
        return tags
                .map { entry -> entry.key + DELIMITER + tagHelper.encode(entry.value) }
                .let { encryptList(it) }
    }

    @Throws(IOException::class)
    override fun decryptTags(
            encryptedTags: List<String>
    ): HashMap<String, String> = decryptList(
            encryptedTags,
            { decrypted -> !decrypted.startsWith(ANNOTATION_KEY) && decrypted.contains(DELIMITER) },
            { tagList: List<String> -> tagHelper.convertToTagMap(tagList) }
    )

    @Throws(IOException::class)
    override fun encryptAnnotations(annotations: List<String>): MutableList<String> {
        return annotations
                .map { annotation -> tagHelper.encode(annotation) }
                .let { validAnnotations ->
                    encryptList(
                            validAnnotations,
                            ANNOTATION_KEY + DELIMITER
                    )
                }
    }

    @Throws(IOException::class)
    override fun decryptAnnotations(
            encryptedAnnotations: List<String>
    ): List<String> = decryptList(
            encryptedAnnotations,
            { decrypted -> decrypted.startsWith(ANNOTATION_KEY) && decrypted.contains(DELIMITER) },
            { list -> removeAnnotationKey(list) }
    )

    @Throws(IOException::class)
    private fun encryptList(plainList: List<String>, prefix: String = ""): MutableList<String> {
        return plainList.map { tag ->
            encryptItem(
                    cryptoService.fetchTagEncryptionKey(),
                    prefix + tag
            )
        }.toMutableList()
    }

    @Throws(D4LException::class)
    private fun encryptItem(key: GCKey, tag: String): String {
        return try {
            cryptoService.symEncrypt(key, tag.toByteArray(), IV)
                    .let { data -> base64.encodeToString(data) }
        } catch (e: Exception) {
            throw CryptoException.EncryptionFailed("Failed to encrypt tag")
        }
    }

    @Throws(IOException::class)
    private fun <T> decryptList(
            encryptedList: List<String>,
            condition: (decryptedItem: String) -> Boolean,
            transform: (decryptedList: MutableList<String>) -> T
    ): T {
        val tek = cryptoService.fetchTagEncryptionKey()
        return encryptedList
                .map { encryptedTag -> decryptItem(tek, encryptedTag) }
                .map { encodedTag -> tagHelper.decode(encodedTag) }
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
        private val IV = ByteArray(16)
        private const val ANNOTATION_KEY = "custom"

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
