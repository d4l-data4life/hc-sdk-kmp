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

import care.data4life.sdk.model.ModelContract
import care.data4life.sdk.network.model.NetworkModelContract.Companion.DEFAULT_COMMON_KEY_ID
import care.data4life.sdk.tag.EncryptedTagsAndAnnotations
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)

data class EncryptedRecord(
    @field:Json(name = "common_key_id")
    internal var _commonKeyId: String?,
    @field:Json(name = "record_id")
    override val identifier: String?,
    @field:Json(name = "encrypted_tags")
    override val encryptedTags: EncryptedTagsAndAnnotations,
    @field:Json(name = "encrypted_body")
    override val encryptedBody: String,
    @field:Json(name = "status")
    override val status: ModelContract.RecordStatus,
    @field:Json(name = "date")
    override val customCreationDate: String?,
    @field:Json(name = "encrypted_key")
    override val encryptedDataKey: EncryptedKey,
    @field:Json(name = "attachment_key")
    override val encryptedAttachmentsKey: EncryptedKey?,
    @field:Json(name = "model_version")
    override val modelVersion: Int,
    @field:Json(name = "createdAt")
    override val updatedDate: String? = null,
    @field:Json(name = "version")
    override val version: Int = 0
) : NetworkModelContract.EncryptedRecord {

    init {
        _commonKeyId = _commonKeyId ?: DEFAULT_COMMON_KEY_ID
    }

    override val commonKeyId: String
        get() = this._commonKeyId!!
}
