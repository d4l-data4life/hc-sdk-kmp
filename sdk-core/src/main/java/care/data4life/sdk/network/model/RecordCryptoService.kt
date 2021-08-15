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

package care.data4life.sdk.network.model

import care.data4life.crypto.GCKey
import care.data4life.crypto.KeyType
import care.data4life.sdk.crypto.CryptoContract
import care.data4life.sdk.data.DataResource
import care.data4life.sdk.date.DateHelperContract
import care.data4life.sdk.fhir.Fhir3Resource
import care.data4life.sdk.fhir.Fhir4Resource
import care.data4life.sdk.fhir.FhirContract
import care.data4life.sdk.lang.CoreRuntimeException
import care.data4life.sdk.lang.DataValidationException
import care.data4life.sdk.model.ModelContract
import care.data4life.sdk.model.ModelContract.ModelVersion.Companion.CURRENT
import care.data4life.sdk.network.NetworkingContract
import care.data4life.sdk.tag.Annotations
import care.data4life.sdk.tag.TaggingContract
import care.data4life.sdk.tag.Tags

class RecordCryptoService(
    private val alias: String,
    private val apiService: NetworkingContract.Service,
    private val taggingService: TaggingContract.Service,
    private val tagCryptoService: TaggingContract.CryptoService,
    private val guard: NetworkModelContract.LimitGuard,
    private val cryptoService: CryptoContract.Service,
    private val resourceCryptoService: FhirContract.CryptoService,
    private val dateTimeFormatter: DateHelperContract.DateTimeFormatter,
    private val modelVersion: ModelContract.ModelVersion
) : NetworkModelContract.CryptoService {
    private fun <T : Fhir3Resource> buildFhir3Record(
        identifier: String?,
        resource: T,
        tags: Tags,
        annotations: Annotations,
        creationDate: String?,
        updateDate: String?,
        dataKey: GCKey,
        attachmentKey: GCKey?,
        modelVersion: Int
    ): NetworkModelInternalContract.DecryptedFhir3Record<T> {
        return DecryptedRecord(
            identifier,
            resource,
            tags,
            annotations,
            creationDate,
            updateDate,
            dataKey,
            attachmentKey,
            modelVersion
        )
    }

    private fun <T : Fhir4Resource> buildFhir4Record(
        identifier: String?,
        resource: T,
        tags: Tags,
        annotations: Annotations,
        creationDate: String?,
        updateDate: String?,
        dataKey: GCKey,
        attachmentKey: GCKey?,
        modelVersion: Int
    ): NetworkModelInternalContract.DecryptedFhir4Record<T> {
        return DecryptedR4Record(
            identifier,
            resource,
            tags,
            annotations,
            creationDate,
            updateDate,
            dataKey,
            attachmentKey,
            modelVersion
        )
    }

    private fun buildCustomRecord(
        identifier: String?,
        resource: DataResource,
        tags: Tags,
        annotations: Annotations,
        creationDate: String?,
        updateDate: String?,
        dataKey: GCKey,
        modelVersion: Int
    ): NetworkModelInternalContract.DecryptedCustomDataRecord {
        guard.checkDataLimit(resource.value)

        return DecryptedDataRecord(
            identifier,
            resource,
            tags,
            annotations,
            creationDate,
            updateDate,
            dataKey,
            modelVersion
        )
    }

    private fun <T : Any> buildRecord(
        identifier: String? = null,
        resource: T,
        tags: Tags,
        annotations: Annotations,
        creationDate: String?,
        updateDate: String? = null,
        dataKey: GCKey,
        attachmentKey: GCKey? = null,
        modelVersion: Int = CURRENT
    ): NetworkModelContract.DecryptedBaseRecord<T> {
        val record = when (resource) {
            is Fhir3Resource -> this.buildFhir3Record(
                identifier,
                resource,
                tags,
                annotations,
                creationDate,
                updateDate,
                dataKey,
                attachmentKey,
                modelVersion
            )
            is Fhir4Resource -> this.buildFhir4Record(
                identifier,
                resource,
                tags,
                annotations,
                creationDate,
                updateDate,
                dataKey,
                attachmentKey,
                modelVersion
            )
            is DataResource -> this.buildCustomRecord(
                identifier,
                resource,
                tags,
                annotations,
                creationDate,
                updateDate,
                dataKey,
                modelVersion
            )
            else -> throw CoreRuntimeException.UnsupportedOperation()
        }

        return record as NetworkModelContract.DecryptedBaseRecord<T>
    }

    override fun <T : Any> fromResource(
        resource: T,
        annotations: Annotations
    ): NetworkModelContract.DecryptedBaseRecord<T> {
        val tags = taggingService.appendDefaultTags(resource, null)

        guard.checkTagsAndAnnotationsLimits(tags, annotations)

        return buildRecord(
            resource = resource,
            tags = tags,
            annotations = annotations,
            creationDate = dateTimeFormatter.now(),
            dataKey = cryptoService.generateGCKey().blockingGet()
        )
    }

    private fun fetchCommonKey(): Pair<GCKey, String> {
        return Pair(
            cryptoService.fetchCurrentCommonKey(),
            cryptoService.currentCommonKeyId
        )
    }

    private fun encryptKey(
        commonKey: GCKey,
        mode: KeyType,
        key: GCKey
    ): EncryptedKey {
        return cryptoService.encryptSymmetricKey(
            commonKey,
            mode,
            key
        ).blockingGet() as EncryptedKey
    }

    private fun encryptKeys(
        commonKey: GCKey,
        dataKey: GCKey,
        attachmentKey: GCKey?
    ): Pair<EncryptedKey, EncryptedKey?> {
        val encryptedDataKey = encryptKey(commonKey, KeyType.DATA_KEY, dataKey)

        return if (attachmentKey is GCKey) {
            Pair(
                encryptedDataKey,
                encryptKey(commonKey, KeyType.ATTACHMENT_KEY, attachmentKey)
            )
        } else {
            Pair(encryptedDataKey, null)
        }
    }

    override fun <T : Any> encrypt(
        decryptedRecord: NetworkModelContract.DecryptedBaseRecord<T>
    ): NetworkModelContract.EncryptedRecord {
        val dataKey = decryptedRecord.dataKey
        val (commonKey, commonKeyId) = fetchCommonKey()
        val (encryptedDataKey, encryptedAttachmentKey) = encryptKeys(
            commonKey,
            dataKey,
            decryptedRecord.attachmentsKey
        )

        return EncryptedRecord(
            commonKeyId,
            decryptedRecord.identifier,
            tagCryptoService.encryptTagsAndAnnotations(
                decryptedRecord.tags,
                decryptedRecord.annotations
            ),
            resourceCryptoService.encryptResource(dataKey, decryptedRecord.resource),
            decryptedRecord.customCreationDate,
            encryptedDataKey,
            encryptedAttachmentKey,
            decryptedRecord.modelVersion
        )
    }

    private fun validateRecord(encryptedResource: String, version: Int) {
        if (encryptedResource.isEmpty()) {
            throw CoreRuntimeException.InternalFailure()
        }

        if (!modelVersion.isModelVersionSupported(version)) {
            throw DataValidationException.ModelVersionNotSupported("Please update SDK to latest version!")
        }
    }

    private fun getCommonKey(commonKeyId: String, userId: String): GCKey {
        val commonKeyStored = cryptoService.hasCommonKey(commonKeyId)
        return if (commonKeyStored) {
            cryptoService.getCommonKeyById(commonKeyId)
        } else {
            // TODO: This should be in a different Service
            val commonKeyResponse = apiService.fetchCommonKey(
                alias,
                userId,
                commonKeyId
            ).blockingGet()

            cryptoService.asymDecryptSymetricKey(
                cryptoService.fetchGCKeyPair().blockingGet(),
                commonKeyResponse.commonKey
            ).blockingGet().also {
                cryptoService.storeCommonKey(commonKeyId, it)
            }
        }
    }

    private fun decryptKey(
        commonKey: GCKey,
        key: NetworkModelContract.EncryptedKey
    ): GCKey {
        return cryptoService.symDecryptSymmetricKey(
            commonKey,
            key
        ).blockingGet()
    }

    private fun decryptKeys(
        commonKey: GCKey,
        encryptedDataKey: NetworkModelContract.EncryptedKey,
        encryptedAttachmentKey: NetworkModelContract.EncryptedKey?
    ): Pair<GCKey, GCKey?> {
        val dataKey = decryptKey(commonKey, encryptedDataKey)

        return if (encryptedAttachmentKey is NetworkModelContract.EncryptedKey) {
            Pair(dataKey, decryptKey(commonKey, encryptedAttachmentKey))
        } else {
            Pair(dataKey, null)
        }
    }

    override fun <T : Any> decrypt(
        encryptedRecord: NetworkModelContract.EncryptedRecord,
        userId: String
    ): NetworkModelContract.DecryptedBaseRecord<T> {
        validateRecord(encryptedRecord.encryptedBody, encryptedRecord.modelVersion)

        val (tags, annotations) = tagCryptoService.decryptTagsAndAnnotations(
            encryptedRecord.encryptedTags
        )
        val commonKey = getCommonKey(encryptedRecord.commonKeyId, userId)
        val (dataKey, attachmentKey) = decryptKeys(
            commonKey,
            encryptedRecord.encryptedDataKey,
            encryptedRecord.encryptedAttachmentsKey
        )

        return buildRecord(
            encryptedRecord.identifier,
            resourceCryptoService.decryptResource(dataKey, tags, encryptedRecord.encryptedBody),
            tags,
            annotations,
            encryptedRecord.customCreationDate,
            encryptedRecord.updatedDate,
            dataKey,
            attachmentKey,
            encryptedRecord.modelVersion
        )
    }
}
