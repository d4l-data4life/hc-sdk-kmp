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

package care.data4life.sdk.network

import care.data4life.crypto.GCKey
import care.data4life.sdk.data.DataResource
import care.data4life.sdk.fhir.Fhir3Resource
import care.data4life.sdk.fhir.Fhir4Resource
import care.data4life.sdk.lang.CoreRuntimeException
import care.data4life.sdk.network.model.DecryptedR4Record
import care.data4life.sdk.network.model.DecryptedRecord
import care.data4life.sdk.network.model.DecryptedRecordGuard
import care.data4life.sdk.network.model.NetworkModelContract
import care.data4life.sdk.network.model.definitions.DecryptedBaseRecord
import care.data4life.sdk.network.model.definitions.DecryptedFhir3Record
import care.data4life.sdk.network.model.definitions.DecryptedFhir4Record

// TODO: Add factory to make testing easier
internal class DecryptedRecordMapper : NetworkModelContract.DecryptedRecordBuilder {
    private var identifier: String? = null
    private var tags: HashMap<String, String>? = null
    private var annotations: List<String> = listOf()
    private var creationDate: String? = null
    private var updatedDate: String? = null
    private var attachmentKey: GCKey? = null
    private var dataKey: GCKey? = null
    private var modelVersion: Int? = null

    private val guard: NetworkModelContract.LimitGuard = DecryptedRecordGuard

    //mandatory
    override fun setTags(
            tags: HashMap<String, String>?
    ): DecryptedRecordMapper = this.also { it.tags = tags }

    override fun setCreationDate(
            creationDate: String?
    ): DecryptedRecordMapper = this.also { it.creationDate = creationDate }

    override fun setDataKey(
            dataKey: GCKey?
    ): DecryptedRecordMapper = this.also { it.dataKey = dataKey }

    override fun setModelVersion(
            modelVersion: Int?
    ): DecryptedRecordMapper = this.also { it.modelVersion = modelVersion }

    //Optional
    override fun setIdentifier(
            identifier: String?
    ): DecryptedRecordMapper = this.also { it.identifier = identifier }

    override fun setAnnotations(
            annotations: List<String>?
    ): DecryptedRecordMapper = this.also { it.annotations = annotations ?: listOf() }

    override fun setUpdateDate(
            updatedDate: String?
    ): DecryptedRecordMapper = this.also { it.updatedDate = updatedDate }

    override fun setAttachmentKey(
            attachmentKey: GCKey?
    ): DecryptedRecordMapper = this.also { it.attachmentKey = attachmentKey }

    @Throws(CoreRuntimeException.InternalFailure::class)
    private fun validatePayload(
            tags: HashMap<String, String>?,
            creationDate: String?,
            dataKey: GCKey?,
            modelVersion: Int?
    ) {
        if (
                this.tags == null && tags == null ||
                this.creationDate == null && creationDate == null ||
                this.dataKey == null && dataKey == null ||
                this.modelVersion == null && modelVersion == null

        ) {
            throw CoreRuntimeException.InternalFailure()
        }
    }

    @Throws(CoreRuntimeException.InternalFailure::class)
    private fun <T : Fhir3Resource?> buildFhir3Record(
            resource: T?,
            tags: HashMap<String, String>,
            creationDate: String,
            dataKey: GCKey,
            modelVersion: Int
    ): DecryptedFhir3Record<T?> =
            DecryptedRecord(
                    this.identifier,
                    resource,
                    tags,
                    this.annotations,
                    creationDate,
                    this.updatedDate,
                    dataKey,
                    this.attachmentKey,
                    modelVersion
            )

    @Throws(CoreRuntimeException.InternalFailure::class)
    private fun <T : Fhir4Resource> buildFhir4Record(
            resource: T,
            tags: HashMap<String, String>,
            creationDate: String,
            dataKey: GCKey,
            modelVersion: Int
    ): DecryptedFhir4Record<T> =
            DecryptedR4Record(
                    this.identifier,
                    resource,
                    tags,
                    this.annotations,
                    creationDate,
                    this.updatedDate,
                    dataKey,
                    this.attachmentKey,
                    modelVersion
            )

    @Throws(CoreRuntimeException.InternalFailure::class)
    private fun buildCustomRecord(
            resource: DataResource,
            tags: HashMap<String, String>,
            creationDate: String?,
            dataKey: GCKey?,
            modelVersion: Int?
    ): care.data4life.sdk.network.model.definitions.DecryptedCustomDataRecord = care.data4life.sdk.network.model.DecryptedDataRecord(
            identifier,
            resource,
            tags,
            annotations,
            creationDate!!,
            updatedDate,
            dataKey!!,
            modelVersion!!
    )

    @Suppress("UNCHECKED_CAST", "NAME_SHADOWING")
    @Throws(CoreRuntimeException.InternalFailure::class)
    override fun <T : Any?> build(
            resource: T,
            tags: HashMap<String, String>?,
            creationDate: String?,
            dataKey: GCKey?,
            modelVersion: Int?
    ): DecryptedBaseRecord<T> {
        this.validatePayload(
                tags,
                creationDate,
                dataKey,
                modelVersion
        )

        val tags = tags ?: this.tags!!
        val creationDate = creationDate ?: this.creationDate!!
        val dataKey = dataKey ?: this.dataKey!!
        val modelVersion = modelVersion ?: this.modelVersion!!

        guard.checkTagsAndAnnotationsLimits(tags, annotations)

        return when (resource) {
            null -> this.buildFhir3Record(
                    resource,
                    tags,
                    creationDate,
                    dataKey,
                    modelVersion
            )// ToDo: Explode
            is Fhir3Resource -> this.buildFhir3Record(
                    resource,
                    tags,
                    creationDate,
                    dataKey,
                    modelVersion
            )
            is Fhir4Resource -> this.buildFhir4Record(
                    resource,
                    tags,
                    creationDate,
                    dataKey,
                    modelVersion
            )
            is DataResource -> this.buildCustomRecord(
                    resource,
                    tags,
                    creationDate,
                    dataKey,
                    modelVersion
            ).also { this.guard.checkDataLimit(resource.asByteArray()) }
            else -> throw CoreRuntimeException.InternalFailure()

        } as DecryptedBaseRecord<T>
    }

    override fun clear(): DecryptedRecordMapper = this.also {
        it.identifier = null
        it.tags = null
        it.annotations = listOf()
        it.creationDate = null
        it.updatedDate = null
        it.attachmentKey = null
        it.dataKey = null
        it.modelVersion = null
    }
}
