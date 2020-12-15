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

package care.data4life.sdk.network.model

import care.data4life.crypto.GCKey
import care.data4life.sdk.fhir.Fhir3Resource
import care.data4life.sdk.fhir.Fhir4Resource
import care.data4life.sdk.lang.CoreRuntimeException
import care.data4life.sdk.lang.DataValidationException


//ToDo Internal
interface NetworkRecordContract {
     interface DecryptedRecord<T> {
        var identifier: String?
        var resource: T
        var tags: HashMap<String, String>?
        var annotations: List<String>
        var customCreationDate: String?
        var updatedDate: String? //FIXME: This should never be null
        var dataKey: GCKey?
        var attachmentsKey: GCKey?
        var modelVersion: Int
    }

    // FIXME remove nullable type
    interface DecryptedFhir3Record<T : Fhir3Resource?> : DecryptedRecord<T>
    interface DecryptedFhir4Record<T : Fhir4Resource> : DecryptedRecord<T>
    interface DecryptedDataRecord : DecryptedRecord<ByteArray> {
        override var attachmentsKey: GCKey?
            get() = null
            set(_) {}
    }

    interface Builder {
        val tags: HashMap<String, String>?
        val dataKey: GCKey?

        //mandatory
        fun setTags(tags: HashMap<String, String>?): Builder
        fun setCreationDate(creationDate: String?): Builder
        fun setDataKey(dataKey: GCKey?): Builder
        fun setModelVersion(modelVersion: Int?): Builder

        //Optional
        fun setIdentifier(identifier: String?): Builder
        fun setAnnotations(annotations: List<String>?): Builder
        fun setUpdateDate(updatedDate: String?): Builder
        fun setAttachmentKey(attachmentKey: GCKey?): Builder

        @Throws(CoreRuntimeException.InternalFailure::class)
        fun <T : Any?> build(
                resource: T,
                tags: HashMap<String, String>? = null,
                creationDate: String? = null,
                dataKey: GCKey? = null,
                modelVersion: Int? = null
        ): DecryptedRecord<T>

        fun clear(): Builder
    }

    interface LimitGuard {
        @Throws(DataValidationException.TagsAndAnnotationsLimitViolation::class)
        fun checkTagsAndAnnotationsLimits(tags: HashMap<String, String>, annotations: List<String>)

        @Throws(DataValidationException.CustomDataLimitViolation::class)
        fun checkDataLimit(data: ByteArray)

        companion object {
            const val MAX_LENGTH_TAGS_AND_ANNOTATIONS = 1000
            const val MAX_SIZE_CUSTOM_DATA = 10485760 // = 10 MiB in Bytes
        }
    }
}
