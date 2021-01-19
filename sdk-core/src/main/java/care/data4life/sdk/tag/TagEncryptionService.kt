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
import care.data4life.sdk.util.Base64
import io.reactivex.Observable
import io.reactivex.Single
import java.io.IOException
import java.nio.charset.StandardCharsets

// TODO internal
class TagEncryptionService @JvmOverloads constructor(
        private val cryptoService: CryptoService,
        private val base64: Base64 = Base64
) : TaggingContract.EncryptionService {
    @Throws(IOException::class)
    private fun encryptList(list: List<String>, prefix: String = ""): List<String> {
        val tek = cryptoService.fetchTagEncryptionKey()
        return Observable
                .fromIterable(list)
                .map { tag -> encryptTag(tek, prefix + tag).blockingGet() }
                .toList()
                .blockingGet()
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
                .filter { decryptedItem -> condition(decryptedItem) }
                .toList()
                .map { decryptedList -> transform(decryptedList) }
                .blockingGet()
    }

    @Throws(IOException::class)
    fun encryptTags(
            tags: HashMap<String, String>
    ): List<String> = encryptList(TagEncryptionHelper.convertToTagList(tags))

    @Throws(IOException::class)
    fun decryptTags(
            encryptedTags: List<String>
    ): HashMap<String, String> = decryptList(
            encryptedTags,
            { d -> !d.startsWith(ANNOTATION_KEY) && d.contains(TaggingService.TAG_DELIMITER) },
            { tagList: List<String> -> TagEncryptionHelper.convertToTagMap(tagList) }
    )


    @Throws(IOException::class)
    fun encryptAnnotations(
            annotations: List<String>
    ): List<String> = encryptList(annotations, ANNOTATION_KEY)

    @Throws(IOException::class)
    fun decryptAnnotations(
            encryptedAnnotations: List<String>
    ): List<String> = decryptList(
            encryptedAnnotations,
            { d -> d.startsWith(ANNOTATION_KEY) },
            { list -> removeAnnotationKey(list) }
    )

    @Throws(D4LException::class)
    fun encryptTag(key: GCKey, tag: String): Single<String> {
        return Single
                .fromCallable { tag.toByteArray() }
                .map { d -> cryptoService.symEncrypt(key, d, IV) }
                .map { data -> base64.encodeToString(data) }
                .onErrorResumeNext {
                    Single.error(
                            CryptoException.EncryptionFailed("Failed to encrypt tag") as D4LException
                    )
                }
    }

    @Throws(D4LException::class)
    fun decryptTag(key: GCKey, base64tag: String): Single<String> {
        return Single
                .fromCallable { base64.decode(base64tag) }
                .map { d -> cryptoService.symDecrypt(key, d, IV) }
                .map { decrypted -> String(decrypted, StandardCharsets.UTF_8) }
                .onErrorResumeNext {
                    Single.error(
                            CryptoException.DecryptionFailed("Failed to decrypt tag") as D4LException
                    )
                }
    }

    companion object {
        private val IV = ByteArray(16)
        private const val ANNOTATION_KEY = "custom" + TaggingService.TAG_DELIMITER

        @JvmStatic
        private fun removeAnnotationKey(
                list: MutableList<String>
        ): List<String> = list.also {
            for (idx in it.indices) {
                it[idx] = it[idx].replaceFirst(ANNOTATION_KEY.toRegex(), "")
            }
        }
    }
}
