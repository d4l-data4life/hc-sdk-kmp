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
import care.data4life.sdk.tag.TaggingContract.Companion.TAG_DELIMITER
import care.data4life.sdk.util.Base64
import io.reactivex.Observable
import io.reactivex.Single
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.util.*
import kotlin.collections.HashMap

// TODO internal
class TagEncryptionService @JvmOverloads constructor(
        private val cryptoService: CryptoService,
        private val base64: Base64 = Base64,
        private val tagHelper: TaggingContract.Helper = TagEncryptionHelper
) : TaggingContract.EncryptionService {
    @Throws(IOException::class)
    private fun encryptList(list: List<String>, prefix: String = ""): List<String> {
        val tek = cryptoService.fetchTagEncryptionKey()
        return Observable
                .fromIterable(list)
                .map { tag -> encryptTag(tek, prefix+tag).blockingGet() }
                .toList()
                .blockingGet()
    }

    @Throws(D4LException::class)
    private fun encryptTag(key: GCKey, tag: String): Single<String> {
        return Single
                .fromCallable { tag.toByteArray() }
                .map { plain -> cryptoService.symEncrypt(key, plain, IV) }
                .map { data -> base64.encodeToString(data) }
                .onErrorResumeNext {
                    Single.error(
                            CryptoException.EncryptionFailed("Failed to encrypt tag") as D4LException
                    )
                }
    }

    @Throws(IOException::class)
    private fun <T> decryptList(
            encryptedList: List<String>,
            condition: (decryptedItem: String) -> Boolean,
            transform: (decryptedList: MutableList<String>) -> T
    ): T {
        val tek = cryptoService.fetchTagEncryptionKey()
        return Observable
                .fromIterable(encryptedList)
                .map { encryptedTag -> decryptTag(tek, encryptedTag).blockingGet() }
                .map { encodedTag -> tagHelper.decode(encodedTag) }
                .filter { decryptedItem -> condition(decryptedItem) }
                .toList()
                .map { decryptedList -> transform(decryptedList) }
                .blockingGet()
    }

    @Throws(D4LException::class)
    private fun decryptTag(key: GCKey, base64tag: String): Single<String> {
        return Single
                .fromCallable { base64.decode(base64tag) }
                .map { encrypted -> cryptoService.symDecrypt(key, encrypted, IV) }
                .map { decrypted -> String(decrypted, StandardCharsets.UTF_8) }
                .onErrorResumeNext {
                    Single.error(
                            CryptoException.DecryptionFailed("Failed to decrypt tag") as D4LException
                    )
                }
    }

    @Throws(IOException::class)
    override fun encryptTags(tags: HashMap<String, String>): List<String> {
        return tags
                .map { entry -> entry.key +
                        TAG_DELIMITER +
                        tagHelper.prepare(entry.value.toLowerCase(Locale.US))
                }
                .let { encryptList(it) }
    }

    @Throws(IOException::class)
    override fun decryptTags(
            encryptedTags: List<String>
    ): HashMap<String, String> = decryptList(
            encryptedTags,
            { decrypted -> !decrypted.startsWith(ANNOTATION_KEY) && decrypted.contains(TAG_DELIMITER) },
            { tagList: List<String> -> tagHelper.convertToTagMap(tagList) }
    )

    @Throws(IOException::class)
    override fun encryptAnnotations(
            annotations: List<String>
    ): List<String> {
        return annotations
                .map { annotation -> tagHelper.prepare(annotation) }
                .let { validAnnotations -> encryptList(
                        validAnnotations,
                        ANNOTATION_KEY + TAG_DELIMITER
                ) }
    }

    @Throws(IOException::class)
    override fun decryptAnnotations(
            encryptedAnnotations: List<String>
    ): List<String> = decryptList(
            encryptedAnnotations,
            { decrypted -> decrypted.startsWith(ANNOTATION_KEY) && decrypted.contains(TAG_DELIMITER) },
            { list -> stripAnnotationKey(list) }
    )

    companion object {
        private val IV = ByteArray(16)
        private const val ANNOTATION_KEY = "custom"

        @JvmStatic
        private fun stripAnnotationKey(
                list: MutableList<String>
        ): List<String> = list.also {
            for (idx in it.indices) {
                it[idx] = it[idx].replaceFirst(ANNOTATION_KEY + TAG_DELIMITER, "")
            }
        }
    }
}
