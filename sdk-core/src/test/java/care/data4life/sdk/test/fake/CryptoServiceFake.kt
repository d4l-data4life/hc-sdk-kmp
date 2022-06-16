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

package care.data4life.sdk.test.fake

import care.data4life.sdk.crypto.CryptoContract
import care.data4life.sdk.crypto.GCAsymmetricKey
import care.data4life.sdk.crypto.GCKey
import care.data4life.sdk.crypto.GCKeyPair
import care.data4life.sdk.crypto.KeyType
import care.data4life.sdk.network.model.EncryptedKey
import care.data4life.sdk.network.model.NetworkModelContract
import care.data4life.sdk.tag.Annotations
import care.data4life.sdk.test.util.GenericTestDataProvider.IV
import io.mockk.mockk
import io.reactivex.Single

data class CryptoServiceIteration(
    val gcKeyOrder: List<GCKey>,
    val commonKey: GCKey,
    val commonKeyId: String,
    val commonKeyIsStored: Boolean,
    val commonKeyFetchCalls: Int,
    val encryptedCommonKey: EncryptedKey?,
    val dataKey: GCKey,
    val encryptedDataKey: EncryptedKey,
    val attachmentKey: GCKey?,
    val encryptedAttachmentKey: EncryptedKey?,
    val tagEncryptionKey: GCKey,
    val tagEncryptionKeyCalls: Int,
    val resources: List<String>,
    val tags: List<String>,
    val annotations: Annotations,
    val hashFunction: (payload: String) -> String
)

class CryptoServiceFake : CryptoContract.Service {
    private lateinit var callOrder: MutableList<GCKey>
    private var commonKeyIdPointer: String? = null
    private lateinit var tagsAndAnnotations: Map<String, String>
    private lateinit var hashedTagsAndAnnotations: Map<String, String>
    private lateinit var hashedResources: List<String>
    private lateinit var currentKeyPair: GCKeyPair
    private var remainingTagEncryptionKeyCalls: Int = 0
    private var remainingCommonKeyFetchCalls: Int = 0

    private lateinit var currentIteration: CryptoServiceIteration

    var iteration: CryptoServiceIteration
        get() = currentIteration
        set(newValue) {
            setNewIteration(newValue)
        }

    override val currentCommonKeyId: String
        get() {
            return if (commonKeyIdPointer is String) {
                commonKeyIdPointer!!
            } else {
                throw RuntimeException("You need to init the commonKey first.")
            }
        }

    private fun isResourceKey(
        key: GCKey
    ): Boolean = key == currentIteration.dataKey || key == currentIteration.attachmentKey

    private fun indexOfResource(resource: String): Int {
        return currentIteration.resources.indexOf(resource)
    }

    private fun findResource(key: GCKey, resource: String): Int {
        return if (isResourceKey(key)) {
            indexOfResource(resource)
        } else {
            -1
        }
    }

    override fun encrypt(key: GCKey, data: ByteArray): Single<ByteArray> {
        val idx = findResource(key, String(data))
        return if (idx > -1) {
            Single.just(hashedResources[idx].toByteArray())
        } else {
            throw RuntimeException(
                "Unable to fake resource encryption: \nKey: $key \nData: ${String(data)}"
            )
        }
    }

    private fun indexOfEncryptedResource(hashedResource: String): Int {
        return hashedResources.indexOf(hashedResource)
    }

    private fun findHashedResource(key: GCKey, hashedResource: String): Int {
        return if (isResourceKey(key)) {
            indexOfEncryptedResource(hashedResource)
        } else {
            -1
        }
    }

    override fun decrypt(key: GCKey, data: ByteArray): Single<ByteArray> {
        val idx = findHashedResource(key, String(data))
        return if (idx > -1) {
            Single.just(currentIteration.resources[idx].toByteArray())
        } else {
            throw RuntimeException(
                "Unable to fake resource decryption: \nKey: $key \nData: ${String(data)}"
            )
        }
    }

    override fun encryptAndEncodeByteArray(
        key: GCKey,
        data: ByteArray
    ): Single<String> = encryptAndEncodeString(key, String(data))

    override fun encryptAndEncodeString(key: GCKey, data: String): Single<String> {
        val idx = findResource(key, data)
        return if (idx > -1) {
            Single.just(hashedResources[idx])
        } else {
            throw RuntimeException(
                "Unable to fake resource encryptAndEncodeString: \nKey: $key \nData: $data"
            )
        }
    }

    override fun decodeAndDecryptByteArray(
        key: GCKey,
        dataBase64: String
    ): Single<ByteArray> {
        return decodeAndDecryptString(key, dataBase64)
            .map { string -> string.toByteArray() }
    }

    override fun decodeAndDecryptString(key: GCKey, dataBase64: String): Single<String> {
        val idx = findHashedResource(key, dataBase64)
        return if (idx > -1) {
            Single.just(currentIteration.resources[idx])
        } else {
            throw RuntimeException(
                "Unable to fake resource decodeAndDecryptString: \nKey: $key \nData: $dataBase64"
            )
        }
    }

    private fun isCommonKey(key: GCKey): Boolean = key == currentIteration.commonKey
    private fun isDataKey(key: GCKey): Boolean = key == currentIteration.dataKey
    private fun isAttachmentKey(key: GCKey): Boolean = key == currentIteration.attachmentKey

    private fun matchEncryptSymKey(
        keyType: KeyType,
        gcKey: GCKey
    ): NetworkModelContract.EncryptedKey {
        return when {
            keyType == KeyType.DATA_KEY && isDataKey(gcKey) -> currentIteration.encryptedDataKey
            keyType == KeyType.ATTACHMENT_KEY && isAttachmentKey(gcKey) -> currentIteration.encryptedAttachmentKey!!
            else -> throw RuntimeException(
                "Unexpected payload for encryptSymmetricKey: $keyType (KeyType) and $gcKey (Key)."
            )
        }
    }

    override fun encryptSymmetricKey(
        key: GCKey,
        keyType: KeyType,
        gcKey: GCKey
    ): Single<NetworkModelContract.EncryptedKey> {
        return if (isCommonKey(key)) {
            Single.just(matchEncryptSymKey(keyType, gcKey))
        } else {
            throw RuntimeException("Expected commonKey as parameter in encryptSymmetricKey and got $key.")
        }
    }

    private fun matchDecryptSymKey(key: NetworkModelContract.EncryptedKey): GCKey {
        return when (key) {
            currentIteration.encryptedDataKey -> currentIteration.dataKey
            currentIteration.encryptedAttachmentKey -> currentIteration.attachmentKey!!
            else -> throw RuntimeException(
                "Unexpected payload for symDecryptSymmetricKey: $key (Key)."
            )
        }
    }

    override fun symDecryptSymmetricKey(
        key: GCKey,
        encryptedKey: NetworkModelContract.EncryptedKey
    ): Single<GCKey> {
        return if (isCommonKey(key)) {
            Single.just(matchDecryptSymKey(encryptedKey))
        } else {
            throw RuntimeException("Expected commonKey as parameter in symDecryptSymmetricKey and got $key.")
        }
    }

    private fun matchAdditionalTagParameter(key: GCKey, iv: ByteArray): Boolean {
        return key == currentIteration.tagEncryptionKey &&
            iv.contentEquals(IV)
    }

    private fun isEncryptableTag(
        tag: String,
        key: GCKey,
        iv: ByteArray
    ): Boolean = tagsAndAnnotations.containsKey(tag) && matchAdditionalTagParameter(key, iv)

    private fun getTagValue(tag: ByteArray, key: GCKey, iv: ByteArray): String? {
        val tagAsString = String(tag)

        return if (isEncryptableTag(tagAsString, key, iv)) {
            tagsAndAnnotations.getValue(tagAsString)
        } else {
            null
        }
    }

    override fun symEncrypt(
        key: GCKey,
        data: ByteArray,
        iv: ByteArray
    ): ByteArray {
        val tagValue = getTagValue(data, key, iv)

        return if (tagValue is String) {
            tagValue.toByteArray()
        } else {
            throw RuntimeException(
                "Unexpected payload for symEncrypt(probably tag/annotation encryption):" +
                    "\nKey: $key\nData: ${String(data)}\nIV: $iv"
            )
        }
    }

    private fun isDecryptableTag(
        tag: String,
        key: GCKey,
        iv: ByteArray
    ): Boolean = hashedTagsAndAnnotations.containsKey(tag) && matchAdditionalTagParameter(key, iv)

    private fun getEncryptedTagValue(tag: ByteArray, key: GCKey, iv: ByteArray): String? {
        val tagAsString = String(tag)

        return if (isDecryptableTag(tagAsString, key, iv)) {
            hashedTagsAndAnnotations.getValue(tagAsString)
        } else {
            null
        }
    }

    override fun symDecrypt(key: GCKey, data: ByteArray, iv: ByteArray): ByteArray {
        val tagValue = getEncryptedTagValue(data, key, iv)

        return if (tagValue is String) {
            tagValue.toByteArray()
        } else {
            throw RuntimeException(
                "Unexpected payload for symDecrypt(probably tag/annotation encryption):" +
                    "\nKey: $key\nData: $data\nIV: $iv"
            )
        }
    }

    override fun generateGCKey(): Single<GCKey> = Single.just(callOrder.removeAt(0))

    override fun hasCommonKey(commonKeyId: String): Boolean {
        return if (currentIteration.commonKeyId == commonKeyId) {
            currentIteration.commonKeyIsStored
        } else {
            throw RuntimeException(
                "Unexpected commonKeyId $commonKeyId"
            )
        }
    }

    override fun fetchGCKeyPair(): Single<GCKeyPair> {
        val keyPair: GCKeyPair = mockk()
        currentKeyPair = keyPair
        return Single.just(keyPair)
    }

    override fun asymDecryptSymetricKey(
        keyPair: GCKeyPair,
        encryptedKey: NetworkModelContract.EncryptedKey
    ): Single<GCKey> {
        if (keyPair != currentKeyPair) {
            throw RuntimeException("Something is not in order, you need to fetch the a GCKeyPair first.")
        }

        if (currentIteration.encryptedCommonKey !is NetworkModelContract.EncryptedKey) {
            throw RuntimeException("You did not provided a encrypted CommonKey.")
        }

        if (encryptedKey != currentIteration.encryptedCommonKey) {
            throw RuntimeException("You provided no matching encrypted CommonKey.")
        }

        return Single.just(currentIteration.commonKey)
    }

    override fun storeCommonKey(commonKeyId: String, commonKey: GCKey) {
        if (commonKeyId != currentIteration.commonKeyId) {
            throw RuntimeException("The given commonKeyId, which was meant to be stored, is unexpected.")
        }

        if (commonKey != currentIteration.commonKey) {
            throw RuntimeException("The given commonKey, which was meant to be stored, is unexpected.")
        }

        commonKeyIdPointer = currentIteration.commonKeyId
    }

    override fun fetchTagEncryptionKey(): GCKey {
        return if (remainingTagEncryptionKeyCalls >= 1) {
            currentIteration.tagEncryptionKey.also { remainingTagEncryptionKeyCalls -= 1 }
        } else {
            throw RuntimeException("The fetchTagEncryptionKey exceeds its given limit.")
        }
    }

    override fun fetchCurrentCommonKey(): GCKey {
        return if (remainingCommonKeyFetchCalls >= 1) {
            currentIteration.commonKey.also {
                remainingCommonKeyFetchCalls -= 1
                commonKeyIdPointer = currentIteration.commonKeyId
            }
        } else {
            throw RuntimeException("The fetchCurrentCommonKey exceeds its given limit.")
        }
    }

    override fun getCommonKeyById(commonKeyId: String): GCKey {
        return if (commonKeyId != currentIteration.commonKeyId) {
            throw RuntimeException(
                "The given commonKeyId ($commonKeyId) was not expected in getCommonKeyById."
            )
        } else {
            currentIteration.commonKey
        }
    }

    // Fake util
    private fun hashResources(iteration: CryptoServiceIteration) {
        val hashedResources = mutableListOf<String>()
        iteration.resources.forEach { hashedResources.add(iteration.hashFunction(it)) }
        this.hashedResources = hashedResources
    }

    private fun hashAndSetTagOrAnnotation(
        tag: String,
        tagsAndAnnotations: MutableMap<String, String>,
        hashedTagsAndAnnotations: MutableMap<String, String>
    ) {
        val hashed = currentIteration.hashFunction(tag)
        tagsAndAnnotations[tag] = hashed
        hashedTagsAndAnnotations[hashed] = tag
    }

    private fun prepareTagsAndAnnotations(tags: List<String>, annotations: Annotations) {
        val tagsAndAnnotations = mutableMapOf<String, String>()
        val hashedTagsAndAnnotations = mutableMapOf<String, String>()

        tags.forEach {
            hashAndSetTagOrAnnotation(it, tagsAndAnnotations, hashedTagsAndAnnotations)
        }

        annotations.forEach {
            hashAndSetTagOrAnnotation(it, tagsAndAnnotations, hashedTagsAndAnnotations)
        }

        this.tagsAndAnnotations = tagsAndAnnotations
        this.hashedTagsAndAnnotations = hashedTagsAndAnnotations
    }

    private fun setCommonKey(iteration: CryptoServiceIteration) {
        commonKeyIdPointer = if (iteration.commonKeyIsStored) {
            iteration.commonKeyId
        } else {
            null
        }
    }

    private fun setCallLimits(iteration: CryptoServiceIteration) {
        remainingTagEncryptionKeyCalls = iteration.tagEncryptionKeyCalls
        remainingCommonKeyFetchCalls = iteration.commonKeyFetchCalls
    }

    private fun setNewIteration(iteration: CryptoServiceIteration) {
        currentIteration = iteration

        callOrder = iteration.gcKeyOrder.toMutableList()

        setCallLimits(iteration)
        setCommonKey(iteration)
        hashResources(iteration)
        prepareTagsAndAnnotations(iteration.tags, iteration.annotations)
    }

    // Not used yet
    override fun generateGCKeyPair(): Single<GCKeyPair> {
        TODO("Not yet implemented")
    }

    override fun convertAsymmetricKeyToBase64ExchangeKey(
        gcAsymmetricKey: GCAsymmetricKey
    ): Single<String> {
        TODO("Not yet implemented")
    }

    override fun setGCKeyPairFromPemPrivateKey(privateKeyAsPem: String) {
        TODO("Not yet implemented")
    }

    override fun storeTagEncryptionKey(tek: GCKey) {
        TODO("Not yet implemented")
    }

    override fun storeCurrentCommonKeyId(commonKeyId: String) {
        TODO("Not yet implemented")
    }
}
