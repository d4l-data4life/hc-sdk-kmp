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

package care.data4life.sdk

import care.data4life.crypto.GCKey
import care.data4life.crypto.GCKeyPair
import care.data4life.crypto.KeyType
import care.data4life.sdk.attachment.FileService
import care.data4life.sdk.model.Meta
import care.data4life.sdk.model.ModelContract.ModelVersion.Companion.CURRENT
import care.data4life.sdk.network.model.CommonKeyResponse
import care.data4life.sdk.network.model.EncryptedKey
import care.data4life.sdk.network.model.EncryptedRecord
import care.data4life.sdk.tag.Tags
import care.data4life.sdk.test.util.GenericTestDataProvider.DATE_FORMATTER
import care.data4life.sdk.test.util.GenericTestDataProvider.DATE_TIME_FORMATTER
import care.data4life.sdk.test.util.GenericTestDataProvider.IV
import care.data4life.sdk.util.Base64
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.reactivex.Single
import org.threeten.bp.LocalDate
import org.threeten.bp.LocalDateTime
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import javax.xml.bind.DatatypeConverter

typealias CryptoMap = Map<Pair<Triple<GCKey, GCKey, GCKey?>, String>, Pair<EncryptedKey, EncryptedKey?>>
typealias CommonKeyList = List<Pair<String, GCKey>>

class RecordServiceModuleTestFlowHelper(
    private val apiService: ApiService,
    private val cryptoService: CryptoService,
    private val fileService: FileService,
    private val imageResizer: ImageResizer
) {
    private val mdHandle = MessageDigest.getInstance("MD5")

    fun encode(tag: String): String {
        return URLEncoder.encode(tag, StandardCharsets.UTF_8.displayName())
            .replace(".", "%2e")
            .replace("+", "%20")
            .replace("*", "%2a")
            .replace("-", "%2d")
            .replace("_", "%5f")
            .toLowerCase()
    }

    fun prepareTags(
        tags: Tags
    ) {
        tags.forEach { (key, value) ->
            tags[key] = "$key=${encode(value)}"
        }
    }

    fun prepareAnnotations(
        annotations: List<String>
    ): Map<String, String> {
        val mappedAnnotations = mutableMapOf<String, String>()

        annotations.forEach {
            mappedAnnotations[it] = "custom=${encode(it)}"
        }

        return mappedAnnotations
    }

    private fun encrypt(tags: Map<String, String>, cryptoKey: GCKey) {
        tags.keys.forEach { key ->
            every {
                cryptoService.symEncrypt(
                    cryptoKey,
                    eq(tags.getValue(key).toByteArray()),
                    IV
                )
            } returns md5(tags.getValue(key)).toByteArray()
        }
    }

    fun encryptTags(
        tags: Tags,
        tagEncryptionKey: GCKey
    ): Unit = encrypt(tags, tagEncryptionKey)

    fun encryptAnnotation(
        annotations: Map<String, String>,
        tagEncryptionKey: GCKey
    ) {
        if (annotations.isNotEmpty()) {
            encrypt(annotations, tagEncryptionKey)
        }
    }

    private fun encryptTagsAndAnnotations(
        gcKeys: MutableList<GCKey?>,
        tags: Map<String, String>,
        annotations: Map<String, String>,
        tagEncryptionKey: GCKey
    ) {
        every { cryptoService.generateGCKey() } answers {
            Single.just(gcKeys.removeAt(0))
        }

        encryptTags(tags as Tags, tagEncryptionKey)

        encryptAnnotation(annotations, tagEncryptionKey)
    }

    private fun encryptResourcePreamble(
        commonKeyPairs: CommonKeyList,
        gcKeys: MutableList<GCKey?>,
        tags: Map<String, String>,
        annotations: Map<String, String>,
        tagEncryptionKey: GCKey
    ) {
        val ids: MutableList<String> = mutableListOf()
        val keys: MutableList<GCKey> = mutableListOf()

        commonKeyPairs.forEach { (commonKeyId, commonKey) ->
            ids.add(commonKeyId)
            keys.add(commonKey)
        }

        every { cryptoService.fetchCurrentCommonKey() } answers { keys.removeAt(0) }
        every { cryptoService.currentCommonKeyId } answers { ids.removeAt(0) }

        encryptTagsAndAnnotations(gcKeys, tags, annotations, tagEncryptionKey)
    }

    fun encryptFhirResource(
        commonKeyPairs: CommonKeyList,
        cryptoMap: CryptoMap,
        gcKeys: MutableList<GCKey?>,
        tags: Map<String, String>,
        annotations: Map<String, String>,
        tagEncryptionKey: GCKey
    ) {
        encryptResourcePreamble(
            commonKeyPairs,
            gcKeys,
            tags,
            annotations,
            tagEncryptionKey
        )
        encryptFhirResource(cryptoMap)
    }

    fun encryptDataRecord(
        commonKeyPairs: CommonKeyList,
        cryptoMap: CryptoMap,
        gcKeys: MutableList<GCKey?>,
        tags: Map<String, String>,
        annotations: Map<String, String>,
        tagEncryptionKey: GCKey
    ) {
        encryptResourcePreamble(
            commonKeyPairs,
            gcKeys,
            tags,
            annotations,
            tagEncryptionKey
        )

        encryptDataResource(cryptoMap)
    }

    private fun md5(str: String): String {
        mdHandle.update(str.toByteArray())
        return DatatypeConverter
            .printHexBinary(mdHandle.digest())
            .toUpperCase()
            .also { mdHandle.reset() }
    }

    private fun makeEncryptionItem(
        serializedResource: String,
        commonKey: GCKey,
        dataKey: GCKey,
        encryptedDataKey: EncryptedKey,
        attachmentKey: GCKey? = null,
        encryptedAttachmentKey: EncryptedKey? = null
    ) = Pair(
        Pair(Triple(commonKey, dataKey, attachmentKey), serializedResource),
        Pair(encryptedDataKey, encryptedAttachmentKey)
    )

    private fun encryptFhirResource(cryptoMap: CryptoMap) {
        cryptoMap.forEach { (plain, encrypted) ->
            val (commonKey, dataKey, attachmentKey) = plain.first
            val (encryptedDataKey, encryptedAttachmentKey) = encrypted

            every {
                cryptoService.encryptSymmetricKey(commonKey, KeyType.DATA_KEY, dataKey)
            } returns Single.just(encryptedDataKey)
            every {
                cryptoService.encryptAndEncodeString(dataKey, plain.second)
            } returns Single.just(md5(plain.second))

            if (attachmentKey is GCKey) {
                every {
                    cryptoService.encryptSymmetricKey(
                        commonKey,
                        KeyType.ATTACHMENT_KEY,
                        attachmentKey
                    )
                } returns Single.just(encryptedAttachmentKey!!)
            }
        }
    }

    fun encryptDataResource(cryptoMap: CryptoMap) {
        cryptoMap.forEach { (plain, encrypted) ->
            val (commonKey, dataKey, _) = plain.first
            val (encryptedDataKey, _) = encrypted

            every {
                cryptoService.encryptSymmetricKey(commonKey, KeyType.DATA_KEY, dataKey)
            } returns Single.just(encryptedDataKey)
            every {
                cryptoService.encrypt(dataKey, plain.second.toByteArray())
            } returns Single.just(md5(plain.second).toByteArray())
        }
    }

    private fun decrypt(tags: Map<String, String>, cryptoKey: GCKey) {
        tags.keys.forEach { key ->
            every {
                cryptoService.symDecrypt(
                    cryptoKey,
                    eq(md5(tags.getValue(key)).toByteArray()),
                    IV
                )
            } returns tags.getValue(key).toByteArray()
        }
    }

    private fun decryptTags(
        tags: Map<String, String>,
        tagEncryptionKey: GCKey
    ): Unit = decrypt(tags, tagEncryptionKey)

    private fun decryptAnnotation(
        annotations: Map<String, String>,
        tagEncryptionKey: GCKey
    ) {
        if (annotations.isNotEmpty()) {
            decryptTags(annotations, tagEncryptionKey)
        }
    }

    fun decryptTagsAndAnnotations(
        tags: Map<String, String>,
        annotations: Map<String, String>,
        tagEncryptionKey: GCKey
    ) {
        decryptTags(tags, tagEncryptionKey)
        decryptAnnotation(annotations, tagEncryptionKey)
    }

    fun tagEncryptionKeyCallOrder(keys: List<GCKey>) {
        every {
            cryptoService.fetchTagEncryptionKey()
        } returnsMany keys andThen { mockk() }
    }

    fun runWithoutStoredCommonKey(
        alias: String,
        userId: String,
        commonKeyPairs: CommonKeyList
    ) {
        val keyPairs: MutableList<Single<GCKeyPair>> = mutableListOf()

        commonKeyPairs.forEach { (commonKeyId, commonKey) ->
            val commonKeyResponse: CommonKeyResponse = mockk()
            val encryptedCommonKey: EncryptedKey = mockk()

            val keyPair: GCKeyPair = mockk()
            keyPairs.add(Single.just(keyPair))

            // store
            every { cryptoService.hasCommonKey(commonKeyId) } returns false
            every {
                apiService.fetchCommonKey(alias, userId, commonKeyId)
            } returns Single.just(commonKeyResponse)
            every { commonKeyResponse.commonKey } returns encryptedCommonKey

            every {
                cryptoService.asymDecryptSymetricKey(keyPair, encryptedCommonKey)
            } returns Single.just(commonKey)
            every { cryptoService.storeCommonKey(commonKeyId, commonKey) } just Runs
        }

        every { cryptoService.fetchGCKeyPair() } returnsMany keyPairs
    }

    fun runWithStoredCommonKey(commonKeyPairs: CommonKeyList) {
        commonKeyPairs.forEach { (commonKeyId, commonKey) ->
            every { cryptoService.hasCommonKey(commonKeyId) } returns true
            every { cryptoService.getCommonKeyById(commonKeyId) } returns commonKey
        }
    }

    fun uploadAttachment(
        attachmentKey: GCKey,
        userId: String,
        payload: Pair<ByteArray, String>,
        resized: Pair<Pair<ByteArray, String>, Pair<ByteArray, String>?>? = null
    ) {
        every {
            fileService.uploadFile(attachmentKey, userId, payload.first)
        } returns Single.just(payload.second)

        resizing(payload.first, userId, attachmentKey, resized)
    }

    private fun resizing(
        data: ByteArray,
        userId: String,
        attachmentKey: GCKey,
        resizedImages: Pair<Pair<ByteArray, String>, Pair<ByteArray, String>?>?
    ) {
        if (resizedImages == null) {
            every { imageResizer.isResizable(data) } returns false
        } else {
            every { imageResizer.isResizable(data) } returns true

            resizeImage(
                data,
                resizedImages.first.first,
                resizedImages.first.second,
                ImageResizer.DEFAULT_PREVIEW_SIZE_PX,
                userId,
                attachmentKey
            )

            if (resizedImages.second is Pair<*, *>) {
                resizeImage(
                    data,
                    resizedImages.second!!.first,
                    resizedImages.second!!.second,
                    ImageResizer.DEFAULT_THUMBNAIL_SIZE_PX,
                    userId,
                    attachmentKey
                )
            } else {
                resizeImage(
                    data,
                    null,
                    null,
                    ImageResizer.DEFAULT_THUMBNAIL_SIZE_PX,
                    userId,
                    attachmentKey
                )
            }
        }
    }

    private fun resizeImage(
        data: ByteArray,
        resizedImage: ByteArray?,
        imageId: String?,
        targetHeight: Int,
        userId: String,
        attachmentKey: GCKey
    ) {
        every {
            imageResizer.resizeToHeight(
                data,
                targetHeight,
                ImageResizer.DEFAULT_JPEG_QUALITY_PERCENT
            )
        } returns resizedImage

        if (resizedImage is ByteArray) {
            every {
                fileService.uploadFile(attachmentKey, userId, resizedImage)
            } returns Single.just(imageId)
        }
    }

    fun decryptFhirResource(cryptoMap: CryptoMap) {
        cryptoMap.forEach { (plain, encrypted) ->
            val (commonKey, dataKey, attachmentKey) = plain.first
            val (encryptedDataKey, encryptedAttachmentKey) = encrypted

            every {
                cryptoService.symDecryptSymmetricKey(commonKey, encryptedDataKey)
            } returns Single.just(dataKey)
            every {
                cryptoService.decodeAndDecryptString(dataKey, md5(plain.second))
            } returns Single.just(plain.second)

            if (encryptedAttachmentKey is EncryptedKey) {
                every {
                    cryptoService.symDecryptSymmetricKey(commonKey, encryptedAttachmentKey)
                } returns Single.just(attachmentKey!!)
            }
        }
    }

    fun decryptDataResource(cryptoMap: CryptoMap) {
        cryptoMap.forEach { (plain, encrypted) ->
            val (commonKey, dataKey, _) = plain.first
            val (encryptedDataKey, _) = encrypted

            every {
                cryptoService.symDecryptSymmetricKey(commonKey, encryptedDataKey)
            } returns Single.just(dataKey)
            every {
                cryptoService.decrypt(dataKey, md5(plain.second).toByteArray())
            } returns Single.just(plain.second.toByteArray())
        }
    }

    fun prepareFlowPayload(
        serializedResources: List<String>,
        commonKeys: List<Pair<String, GCKey>>,
        dataKeys: List<Pair<GCKey, EncryptedKey>>,
        attachmentKeys: List<Pair<GCKey?, EncryptedKey?>>? = null
    ): Pair<CryptoMap, List<Pair<EncryptedKey, EncryptedKey?>>> {
        val cryptoMap =
            mutableMapOf<Pair<Triple<GCKey, GCKey, GCKey?>, String>, Pair<EncryptedKey, EncryptedKey?>>()
        val encryptedKeys = mutableListOf<Pair<EncryptedKey, EncryptedKey?>>()
        for (idx in 0..serializedResources.lastIndex) {
            val attachmentKey = if (attachmentKeys is List<*>) attachmentKeys[idx].first else null
            val encryptedAttachmentKey: EncryptedKey? = if (attachmentKey is GCKey) {
                attachmentKeys!![idx].second
            } else {
                null
            }

            val item = makeEncryptionItem(
                serializedResources[idx],
                commonKeys[idx].second,
                dataKeys[idx].first,
                dataKeys[idx].second,
                attachmentKey,
                encryptedAttachmentKey
            )

            cryptoMap[item.first] = item.second
            encryptedKeys.add(Pair(dataKeys[idx].second, encryptedAttachmentKey))
        }

        return Pair(cryptoMap, encryptedKeys)
    }

    private fun createEncryptedRecord(
        id: String?,
        commonKeyPair: Pair<String, GCKey>,
        tags: List<String>,
        annotations: List<String>,
        body: String,
        dates: Pair<String?, String?>,
        keys: Pair<EncryptedKey, EncryptedKey?>
    ): EncryptedRecord = EncryptedRecord(
        commonKeyPair.first,
        id,
        tags
            .toMutableList()
            .also { it.addAll(annotations) }
            .map { Base64.encodeToString(md5(it)) },
        body,
        dates.first,
        keys.first,
        keys.second,
        CURRENT,
        dates.second
    )

    fun buildEncryptedRecord(
        id: String?,
        commonKeyPair: Pair<String, GCKey>,
        tags: List<String>,
        annotations: List<String>,
        body: String,
        dates: Pair<String?, String?>,
        keys: Pair<EncryptedKey, EncryptedKey?>
    ): EncryptedRecord = createEncryptedRecord(
        id,
        commonKeyPair,
        tags,
        annotations,
        md5(body),
        dates,
        keys
    )

    fun buildEncryptedRecordWithEncodedBody(
        id: String?,
        commonKeyPair: Pair<String, GCKey>,
        tags: List<String>,
        annotations: List<String>,
        body: String,
        dates: Pair<String?, String?>,
        keys: Pair<EncryptedKey, EncryptedKey?>
    ): EncryptedRecord = createEncryptedRecord(
        id,
        commonKeyPair,
        tags,
        annotations,
        Base64.encodeToString(md5(body)),
        dates,
        keys
    )

    fun buildMeta(
        customCreationDate: String,
        updatedDate: String
    ): Meta = Meta(
        LocalDate.parse(customCreationDate, DATE_FORMATTER),
        LocalDateTime.parse(updatedDate, DATE_TIME_FORMATTER)
    )
}
