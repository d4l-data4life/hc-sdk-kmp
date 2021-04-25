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
import care.data4life.sdk.fhir.Fhir3Resource
import care.data4life.sdk.fhir.Fhir4Resource
import care.data4life.sdk.fhir.FhirContract
import care.data4life.sdk.lang.CoreRuntimeException
import care.data4life.sdk.model.ModelContract.ModelVersion.Companion.CURRENT
import care.data4life.sdk.tag.Annotations
import care.data4life.sdk.tag.TaggingContract
import care.data4life.sdk.tag.Tags
import care.data4life.sdk.wrapper.WrapperContract

class RecordEncryptionService(
    private val taggingService: TaggingContract.Service,
    private val tagEncryptionService: TaggingContract.EncryptionService,
    private val guard: NetworkModelContract.LimitGuard,
    private val cryptoService: CryptoContract.Service,
    private val fhirService: FhirContract.Service,
    private val dateTimeFormatter: WrapperContract.DateTimeFormatter
) : NetworkModelContract.EncryptionService {
    private fun <T : Fhir3Resource> buildFhir3Record(
        resource: T,
        tags: Tags,
        annotations: Annotations,
        creationDate: String,
        dataKey: GCKey,
        modelVersion: Int
    ): NetworkModelContract.DecryptedFhir3Record<T> {
        return DecryptedRecord(
            null,
            resource,
            tags,
            annotations,
            creationDate,
            null,
            dataKey,
            null,
            modelVersion
        )
    }

    private fun <T : Fhir4Resource> buildFhir4Record(
        resource: T,
        tags: Tags,
        annotations: Annotations,
        creationDate: String,
        dataKey: GCKey,
        modelVersion: Int
    ): NetworkModelContract.DecryptedFhir4Record<T> {
        return DecryptedR4Record(
            null,
            resource,
            tags,
            annotations,
            creationDate,
            null,
            dataKey,
            null,
            modelVersion
        )
    }

    private fun buildCustomRecord(
        resource: DataResource,
        tags: Tags,
        annotations: Annotations,
        creationDate: String,
        dataKey: GCKey,
        modelVersion: Int
    ): NetworkModelContract.DecryptedCustomDataRecord {
        guard.checkDataLimit(resource.value)

        return DecryptedDataRecord(
            null,
            resource,
            tags,
            annotations,
            creationDate,
            null,
            dataKey,
            modelVersion
        )
    }

    override fun <T : Any> fromResource(
        resource: T,
        annotations: Annotations
    ): NetworkModelContract.DecryptedBaseRecord<T> {
        val tags = taggingService.appendDefaultTags(resource, null)

        guard.checkTagsAndAnnotationsLimits(tags, annotations)

        val record = when (resource) {
            is Fhir3Resource -> this.buildFhir3Record(
                resource,
                tags,
                annotations,
                dateTimeFormatter.now(),
                cryptoService.generateGCKey().blockingGet(),
                CURRENT
            )
            is Fhir4Resource -> this.buildFhir4Record(
                resource,
                tags,
                annotations,
                dateTimeFormatter.now(),
                cryptoService.generateGCKey().blockingGet(),
                CURRENT
            )
            is DataResource -> this.buildCustomRecord(
                resource,
                tags,
                annotations,
                dateTimeFormatter.now(),
                cryptoService.generateGCKey().blockingGet(),
                CURRENT
            )
            else -> throw CoreRuntimeException.UnsupportedOperation()
        }

        return record as NetworkModelContract.DecryptedBaseRecord<T>
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
            tagEncryptionService.encryptTagsAndAnnotations(
                decryptedRecord.tags,
                decryptedRecord.annotations
            ),
            fhirService._encryptResource(dataKey, decryptedRecord.resource),
            decryptedRecord.customCreationDate,
            encryptedDataKey,
            encryptedAttachmentKey,
            decryptedRecord.modelVersion
        )
    }

    override fun <T : Any> decrypt(
        encryptedRecord: NetworkModelContract.EncryptedRecord,
        userId: String
    ): NetworkModelContract.DecryptedBaseRecord<T> {
        TODO("Not yet implemented")
    }
}
