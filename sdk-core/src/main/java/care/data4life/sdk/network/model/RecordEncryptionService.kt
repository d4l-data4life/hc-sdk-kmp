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
import care.data4life.sdk.crypto.CryptoContract
import care.data4life.sdk.data.DataResource
import care.data4life.sdk.fhir.Fhir3Resource
import care.data4life.sdk.fhir.Fhir4Resource
import care.data4life.sdk.lang.CoreRuntimeException
import care.data4life.sdk.model.ModelContract.ModelVersion.Companion.CURRENT
import care.data4life.sdk.tag.Annotations
import care.data4life.sdk.tag.TaggingContract
import care.data4life.sdk.tag.Tags
import care.data4life.sdk.wrapper.WrapperContract

class RecordEncryptionService(
    private val taggingService: TaggingContract.Service,
    private val guard: NetworkModelContract.LimitGuard,
    private val cryptoService: CryptoContract.Service,
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

    override fun <T : Any> encrypt(
        record: NetworkModelContract.DecryptedBaseRecord<T>,
        userId: String
    ): NetworkModelContract.EncryptedRecord {
        TODO("Not yet implemented")
    }

    override fun <T : Any> decrypt(
        record: NetworkModelContract.EncryptedRecord,
        userId: String
    ): NetworkModelContract.DecryptedBaseRecord<T> {
        TODO("Not yet implemented")
    }
}
