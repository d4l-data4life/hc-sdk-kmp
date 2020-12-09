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

package care.data4life.sdk.model

import care.data4life.sdk.model.definitions.DataRecord
import java.util.*

/**
 * AppDataRecord is used to store arbitrary data, analogous to Record
 * @see care.data4life.sdk.model.Record
 */
data class AppDataRecord(
        override val identifier: String,
        override val resource: ByteArray,
        override val meta: Meta,
        override val annotations: List<String>
) : DataRecord {
    override fun equals(other: Any?): Boolean {
        return when {
            other !is AppDataRecord -> false
            identifier != other.identifier ||
                    meta != other.meta ||
                    annotations != other.annotations -> false
            else -> resource.contentEquals(other.resource)
        }
    }

    override fun hashCode(): Int = Objects.hash(
            identifier,
            meta,
            annotations,
            resource.contentToString()
    )
}
