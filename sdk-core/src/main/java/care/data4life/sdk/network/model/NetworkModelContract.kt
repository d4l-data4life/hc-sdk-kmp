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
import care.data4life.sdk.lang.CoreRuntimeException
import care.data4life.sdk.lang.DataValidationException
import care.data4life.sdk.network.model.definitions.DecryptedBaseRecord

class NetworkModelContract {
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
        fun <T : Any?> build(
                resource: T,
                tags: HashMap<String, String>? = null,
                creationDate: String? = null,
                dataKey: GCKey? = null,
                modelVersion: Int? = null
        ): DecryptedBaseRecord<T>

        fun clear(): DecryptedRecordBuilder
    }

    internal interface LimitGuard {
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
