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
import care.data4life.fhir.stu3.model.DomainResource
import care.data4life.sdk.lang.CoreRuntimeException
import care.data4life.sdk.network.model.DecryptedAppDataRecord
import care.data4life.sdk.network.model.DecryptedRecord
import care.data4life.sdk.network.model.DecryptedRecordGuard
import care.data4life.sdk.network.model.definitions.DecryptedBaseRecord
import care.data4life.sdk.network.model.definitions.DecryptedDataRecord
import care.data4life.sdk.network.model.definitions.DecryptedFhir3Record
import care.data4life.sdk.network.model.definitions.DecryptedRecordBuilder
import care.data4life.sdk.network.model.definitions.LimitGuard

internal class DecryptedRecordBuilderImpl : DecryptedRecordBuilder {
    private var identifier: String? = null
    private var _tags: HashMap<String, String>? = null
    private var annotations: List<String> = listOf()
    private var creationDate: String? = null
    private var updatedDate: String? = null
    private var attachmentKey: GCKey? = null
    private var _dataKey: GCKey? = null
    private var modelVersion: Int? = null

    private val guard: LimitGuard = DecryptedRecordGuard

    override val tags: HashMap<String, String>?
        get() = this._tags

    override val dataKey: GCKey?
        get() = this._dataKey

    //mandatory
    override fun setTags(
            tags: HashMap<String, String>?
    ): DecryptedRecordBuilder = this.also { it._tags = tags }

    override fun setCreationDate(
            creationDate: String?
    ): DecryptedRecordBuilder = this.also { it.creationDate = creationDate }

    override fun setDataKey(
            dataKey: GCKey?
    ): DecryptedRecordBuilder = this.also { it._dataKey = dataKey }

    override fun setModelVersion(
            modelVersion: Int?
    ): DecryptedRecordBuilder = this.also { it.modelVersion = modelVersion }

    //Optional
    override fun setIdentifier(
            identifier: String?
    ): DecryptedRecordBuilder = this.also { it.identifier = identifier }

    override fun setAnnotations(
            annotations: List<String>?
    ): DecryptedRecordBuilder = this.also { it.annotations = annotations ?: listOf() }

    override fun setUpdateDate(
            updatedDate: String?
    ): DecryptedRecordBuilder = this.also { it.updatedDate = updatedDate }

    override fun setAttachmentKey(
            attachmentKey: GCKey?
    ): DecryptedRecordBuilder = this.also { it.attachmentKey = attachmentKey }

    @Throws(CoreRuntimeException.InternalFailure::class)
    private fun validatePayload(
            tags: HashMap<String, String>?,
            creationDate: String?,
            dataKey: GCKey?,
            modelVersion: Int?
    ) {
        if (
                this._tags == null && tags == null ||
                this.creationDate == null && creationDate == null ||
                this._dataKey == null && dataKey == null ||
                this.modelVersion == null && modelVersion == null

        ) {
            throw CoreRuntimeException.InternalFailure()
        }
    }

    @Throws(CoreRuntimeException.InternalFailure::class)
    private fun <T : DomainResource?> buildFhirRecord(
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
    private fun buildCustomRecord(
            resource: ByteArray,
            tags: HashMap<String, String>,
            creationDate: String?,
            dataKey: GCKey?,
            modelVersion: Int?
    ): DecryptedDataRecord = DecryptedAppDataRecord(
            this.identifier,
            resource,
            tags,
            this.annotations,
            creationDate!!,
            this.updatedDate,
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

        val tags = tags ?: this._tags!!
        val creationDate = creationDate ?: this.creationDate!!
        val dataKey = dataKey ?: this.dataKey!!
        val modelVersion = modelVersion ?: this.modelVersion!!

        guard.checkTagsAndAnnotationsLimits(tags, annotations)

        return when (resource) {
            null -> this.buildFhirRecord(
                    resource,
                    tags,
                    creationDate,
                    dataKey,
                    modelVersion
            )
            is DomainResource -> this.buildFhirRecord(
                    resource,
                    tags,
                    creationDate,
                    dataKey,
                    modelVersion
            )
            is ByteArray -> this.buildCustomRecord(
                    resource,
                    tags,
                    creationDate,
                    dataKey,
                    modelVersion
            ).also { this.guard.checkDataLimit(resource) }
            else -> throw CoreRuntimeException.InternalFailure()

        } as DecryptedBaseRecord<T>
    }

    override fun clear(): DecryptedRecordBuilder = this.also {
        it.identifier = null
        it._tags = null
        it.annotations = listOf()
        it.creationDate = null
        it.updatedDate = null
        it.attachmentKey = null
        it._dataKey = null
        it.modelVersion = null
    }
}
