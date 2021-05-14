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

import care.data4life.sdk.lang.DataValidationException
import care.data4life.sdk.util.Base64
import care.data4life.sdk.wrapper.WrapperContract

object AttachmentGuardian : AttachmentContract.Guardian {
    private fun hash(data: String): String = AttachmentHasher.hash(Base64.decode(data))

    override fun guardId(attachment: WrapperContract.Attachment) {
        if (attachment.id != null) {
            throw DataValidationException.IdUsageViolation("Attachment.id should be null")
        }
    }

    override fun guardIdAgainstExistingIds(
        attachment: WrapperContract.Attachment,
        referenceIds: Set<String>
    ) {
        if (attachment.id !in referenceIds) {
            throw DataValidationException.IdUsageViolation("Valid Attachment.id expected")
        }
    }

    private fun guardHashWithoutReference(
        attachment: WrapperContract.Attachment,
        hash: String
    ): Boolean {
        val data = attachment.data
            ?: throw DataValidationException.ExpectedFieldViolation("Attachment.data expected")

        if (hash(data) != hash) {
            throw DataValidationException.InvalidAttachmentPayloadHash("Attachment.hash is not valid")
        }

        return true
    }

    private fun guardHashWithReference(
        attachment: WrapperContract.Attachment,
        hash: String
    ): Boolean = attachment.hash != hash

    override fun guardHash(
        attachment: WrapperContract.Attachment,
        reference: WrapperContract.Attachment?
    ): Boolean {
        val hash = attachment.hash
            ?: throw DataValidationException.ExpectedFieldViolation("Attachment.hash expected")

        return if (reference is WrapperContract.Attachment) {
            guardHashWithReference(reference, hash)
        } else {
            guardHashWithoutReference(attachment, hash)
        }
    }

    override fun guardSize(attachment: WrapperContract.Attachment) {
        if (attachment.size == null) {
            throw DataValidationException.ExpectedFieldViolation("Attachment.size expected")
        }
    }
}
