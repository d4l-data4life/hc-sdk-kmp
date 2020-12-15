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
import care.data4life.sdk.network.model.definitions.DecryptedDataRecord
import java.util.Objects.hash

/**
 * DecryptedAppDataRecord is an internal decrypted form of AppDataRecord, analogous to DecryptedRecord
 * @see care.data4life.sdk.network.model.DecryptedRecord
 */
internal data class DecryptedAppDataRecord(
        override var identifier: String?,
        override var resource: ByteArray,
        override var tags: HashMap<String, String>?,
        override var annotations: List<String>,
        override var customCreationDate: String?,
        override var updatedDate: String?,
        override var dataKey: GCKey?,
        override var modelVersion: Int
) : DecryptedDataRecord {
    override fun equals(other: Any?): Boolean {
        return when {
            other !is DecryptedAppDataRecord -> false
            identifier != other.identifier ||
                    !resource.contentEquals(other.resource) ||
                    tags != other.tags ||
                    annotations != other.annotations ||
                    customCreationDate != other.customCreationDate ||
                    updatedDate != other.updatedDate ||
                    dataKey != other.dataKey ||
                    modelVersion != other.modelVersion -> false
            else -> true
        }
    }

    override fun hashCode(): Int = hash(
            identifier,
            resource.contentToString(),
            tags,
            annotations,
            customCreationDate,
            updatedDate,
            dataKey,
            modelVersion
    )

    override fun copyWithResourceAnnotations(
            data: ByteArray,
            annotations: List<String>?
    ) = copy(resource = data, annotations = annotations ?: this.annotations)
}
