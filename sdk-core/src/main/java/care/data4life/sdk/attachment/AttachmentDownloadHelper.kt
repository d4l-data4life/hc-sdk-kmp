/*
 * Copyright (c) 2021 D4L data4life gGmbH / All rights reserved.
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

package care.data4life.sdk.attachment

import care.data4life.sdk.attachment.AttachmentContract.Companion.DOWNSCALED_ATTACHMENT_ID_POS
import care.data4life.sdk.attachment.AttachmentContract.Companion.SPLIT_CHAR
import care.data4life.sdk.lang.DataValidationException
import care.data4life.sdk.util.Base64
import care.data4life.sdk.wrapper.WrapperContract

object AttachmentDownloadHelper : AttachmentInternalContract.DownloadHelper {
    override fun deriveAttachmentId(attachment: WrapperContract.Attachment): String {
        val attachmentId = attachment.id!!

        return if (attachmentId.contains(SPLIT_CHAR)) {
            attachmentId.split(SPLIT_CHAR)[DOWNSCALED_ATTACHMENT_ID_POS]
        } else {
            attachmentId
        }
    }

    override fun addAttachmentPayload(
        attachment: WrapperContract.Attachment,
        data: ByteArray
    ): WrapperContract.Attachment {
        val newHash = AttachmentHasher.hash(data)

        return if (!attachment.id!!.contains(SPLIT_CHAR) &&
            CompatibilityValidator.isHashable(attachment) &&
            attachment.hash != newHash
        ) {
            throw DataValidationException.InvalidAttachmentPayloadHash("Attachment hash is invalid")
        } else {
            attachment.also {
                it.data = Base64.encodeToString(data)
                it.hash = newHash
            }
        }
    }
}
