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

package care.data4life.sdk.network.model.definitions

import care.data4life.crypto.GCKey
import care.data4life.fhir.stu3.model.DomainResource
import care.data4life.sdk.lang.CoreRuntimeException

internal interface DecryptedRecordBuilder {
    //mandatory
    fun setTags(tags: HashMap<String, String>?): DecryptedRecordBuilder
    fun setCreationDate(creationDate: String?): DecryptedRecordBuilder
    fun setDataKey(dataKey: GCKey?): DecryptedRecordBuilder
    fun setModelVersion(modelVersion: Int?): DecryptedRecordBuilder

    //Optional
    fun setIdentifier(identifier: String?): DecryptedRecordBuilder
    fun setAnnotations(annotations: List<String>?): DecryptedRecordBuilder
    fun setUpdateDate(updatedDate: String?): DecryptedRecordBuilder
    fun setAttachmentKey(attachmentKey: GCKey?): DecryptedRecordBuilder

    @Throws(CoreRuntimeException.InternalFailure::class)
    fun <T : DomainResource?> build(
            resource: T,
            tags: HashMap<String, String>,
            creationDate: String,
            dataKey: GCKey,
            modelVersion: Int
    ): DecryptedFhirRecord<T>

    @Throws(CoreRuntimeException.InternalFailure::class)
    fun <T : DomainResource?> build(
            resource: T
    ): DecryptedFhirRecord<T>

    @Throws(CoreRuntimeException.InternalFailure::class)
    fun build(
            resource: ByteArray,
            tags: HashMap<String, String>,
            creationDate: String,
            dataKey: GCKey,
            modelVersion: Int
    ): DecryptedDataRecord

    @Throws(CoreRuntimeException.InternalFailure::class)
    fun build(
            resource: ByteArray
    ): DecryptedDataRecord

    fun clear(): DecryptedRecordBuilder
}
