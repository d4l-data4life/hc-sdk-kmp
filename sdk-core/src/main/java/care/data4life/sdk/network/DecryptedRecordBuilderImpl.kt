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
import care.data4life.sdk.network.model.definitions.DecryptedDataRecord
import care.data4life.sdk.network.model.definitions.DecryptedFhirRecord
import care.data4life.sdk.network.model.definitions.DecryptedRecordBuilder

internal class DecryptedRecordBuilderImpl : DecryptedRecordBuilder {
    private var identifier: String = ""
    private var annotations: List<String> = listOf()
    private var updatedDate: String? = null
    private var attachmentKey: GCKey? = null

    //mandatory
    override fun setTags(tags: HashMap<String, String>?): DecryptedRecordBuilder {
        TODO("Not yet implemented")
    }

    override fun setCreationDate(creationDate: String?): DecryptedRecordBuilder {
        TODO("Not yet implemented")
    }

    override fun setDataKey(dataKey: GCKey?): DecryptedRecordBuilder {
        TODO("Not yet implemented")
    }

    override fun setModelVersion(modelVersion: Int?): DecryptedRecordBuilder {
        TODO("Not yet implemented")
    }

    //Optional
    override fun setIdentifier(
            identifier: String?
    ): DecryptedRecordBuilder = this.also { it.identifier = identifier ?: "" }

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
    override fun <T : DomainResource?> build(
            resource: T,
            tags: HashMap<String, String>,
            creationDate: String,
            dataKey: GCKey,
            modelVersion: Int
    ): DecryptedFhirRecord<T> = DecryptedRecord(
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
    override fun <T : DomainResource?> build(resource: T): DecryptedFhirRecord<T> {
        TODO("Not yet implemented")
    }

    @Throws(CoreRuntimeException.InternalFailure::class)
    override fun build(
            resource: ByteArray,
            tags: HashMap<String, String>,
            creationDate: String,
            dataKey: GCKey,
            modelVersion: Int
    ): DecryptedDataRecord = DecryptedAppDataRecord(
            this.identifier,
            resource,
            tags,
            this.annotations,
            creationDate,
            this.updatedDate,
            dataKey,
            modelVersion
    )

    @Throws(CoreRuntimeException.InternalFailure::class)
    override fun build(resource: ByteArray): DecryptedDataRecord {
        TODO("Not yet implemented")
    }

    override fun clear(): DecryptedRecordBuilder = this.also {
        it.identifier = ""
        it.annotations = listOf()
        it.updatedDate = null
        it.attachmentKey = null
    }

}
