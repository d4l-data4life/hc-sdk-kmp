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

import com.squareup.moshi.Json

data class EncryptedRecord(
        @field:Json(name = "common_key_id")
        private var _commonKeyId: String?,
        @field:Json(name = "record_id")
        val identifier: String?,
        @field:Json(name = "encrypted_tags")
        val encryptedTags: List<String>,
        @field:Json(name = "encrypted_body")
        val encryptedBody: String?,
        @field:Json(name = "date")
        val customCreationDate: String?,
        @field:Json(name = "encrypted_key")
        val encryptedDataKey: EncryptedKey,
        @field:Json(name = "attachment_key")
        val encryptedAttachmentsKey: EncryptedKey?,
        @field:Json(name = "model_version")
        val modelVersion: Int,
        @field:Json(name = "createdAt")
        val updatedDate: String? = null
) {
    init {
        _commonKeyId = _commonKeyId ?: DEFAULT_COMMON_KEY_ID
    }

    val commonKeyId: String
        get() = this._commonKeyId!!

    val version = 0

    companion object {
        internal const val DEFAULT_COMMON_KEY_ID = "00000000-0000-0000-0000-000000000000"
    }
}
