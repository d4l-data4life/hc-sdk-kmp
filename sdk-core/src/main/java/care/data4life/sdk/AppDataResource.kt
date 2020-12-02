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

package care.data4life.sdk

import care.data4life.crypto.GCKey
import care.data4life.sdk.model.Meta
import care.data4life.sdk.model.RecordBase
import care.data4life.sdk.network.model.DecryptedRecordBase
import java.util.*

/**
 * AppDataRecord is used to store arbitrary data, analogous to Record
 * @see care.data4life.sdk.model.Record
 */
data class AppDataRecord(
        val appDataResource: ByteArray,
        val id: String,
        override val meta: Meta,
        val annotations: List<String>
): RecordBase {
    override fun equals(other: Any?): Boolean {
        return when {
            other !is AppDataRecord             -> false
            id != other.id ||
            meta != other.meta ||
            annotations != other.annotations    -> false
            else                                -> appDataResource.contentEquals(other.appDataResource)
        }
    }

    override fun hashCode(): Int = Objects.hash(id, meta, annotations, appDataResource.contentToString())
}

/**
 * DecryptedAppDataRecord is an internal decrypted form of AppDataRecord, analogous to DecryptedRecord
 * @see care.data4life.sdk.network.model.DecryptedRecord
 */
internal data class DecryptedAppDataRecord(
        override var identifier: String?,
        var appData: ByteArray,
        override var tags: HashMap<String, String>?,
        var annotations: List<String>,
        override var customCreationDate: String?,
        override var updatedDate: String?,
        override var dataKey: GCKey?,
        override var modelVersion: Int
): DecryptedRecordBase {

    override fun equals(other: Any?): Boolean {
        return when {
            other !is DecryptedAppDataRecord        -> false
            identifier != other.identifier ||
            !appData.contentEquals(other.appData) ||
            tags != other.tags ||
            annotations != other.annotations ||
            customCreationDate != other.customCreationDate ||
            updatedDate != other.updatedDate ||
            dataKey != other.dataKey ||
            modelVersion != other.modelVersion      -> false
            else                                    -> true
        }
    }

    override fun hashCode(): Int = Objects.hash(
            identifier,
            appData.contentToString(),
            tags,
            annotations,
            customCreationDate,
            updatedDate,
            dataKey,
            modelVersion
    )

    fun copyWithResourceAnnotaions(
            appData: ByteArray,
            annotations: List<String>? = null
    ) = copy(appData = appData, annotations = annotations ?: this.annotations)
}
