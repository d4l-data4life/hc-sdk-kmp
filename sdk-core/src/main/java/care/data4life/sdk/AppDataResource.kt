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

/**
 * AppDataRecord is used to store arbitrary data, analogous to Record
 * @see care.data4life.sdk.model.Record
 */
data class AppDataRecord(
        val appDataResource: ByteArray,
        val id: String,
        val meta: Meta,
        val annotations: List<String>
)

/**
 * DecryptedAppDataRecord is an internal decrypted form of AppDataRecord, analogous to DecryptedRecord
 * @see care.data4life.sdk.network.model.DecryptedRecord
 */
data class DecryptedAppDataRecord(
        var id: String?,
        val appData: ByteArray,
        val tags: HashMap<String, String>,
        val annotations: List<String>,
        val customCreationDate: String,
        val updatedDate: String?,
        val dataKey: GCKey,
        val modelVersion: Int
) {
    fun copyWithResourceAnnotaions(appData: ByteArray, annotations: List<String>?) =
            copy(appData = appData, annotations = annotations ?: this.annotations)
}
