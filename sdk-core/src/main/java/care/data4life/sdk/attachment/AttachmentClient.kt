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

package care.data4life.sdk.attachment

import care.data4life.sdk.network.model.NetworkRecordContract
import care.data4life.sdk.wrapper.HelperContract
import care.data4life.sdk.wrapper.WrapperContract

internal class AttachmentClient(
        private val fhirAttachmentHelper: HelperContract.FhirAttachmentHelper
): AttachmentContract.Client {
    private fun unpackAttachments(
            attachments: HashMap<WrapperContract.Attachment, String?>
    ): HashMap<Any, String?> {
        val rawAttachments = hashMapOf<Any, String?>()

        for(attachment in attachments.keys) {
            rawAttachments[attachment.unwrap()] = attachments[attachment]
        }

        return rawAttachments
    }

    private fun setUploadData(
            record: NetworkRecordContract.DecryptedRecord<WrapperContract.Resource>,
            attachmentData: HashMap<Any, String?>?
    ) {
        val rawResource = record.resource.unwrap()
        val attachments = fhirAttachmentHelper.getAttachment(rawResource)
        if (attachments.isNotEmpty()) {
            fhirAttachmentHelper.updateAttachmentData(
                    rawResource,
                    attachmentData
            )
        }
    }

    override fun removeUploadData(
            record: NetworkRecordContract.DecryptedRecord<WrapperContract.Resource>
    ): NetworkRecordContract.DecryptedRecord<WrapperContract.Resource> {
        return record.also {
            if (record.resource.type != WrapperContract.Resource.TYPE.DATA) {
                setUploadData(
                        record,
                        null
                )
            }
        }
    }

    override fun restoreUploadData(
            record: NetworkRecordContract.DecryptedRecord<WrapperContract.Resource>,
            originalResource: WrapperContract.Resource?,
            attachmentData: HashMap<WrapperContract.Attachment, String?>?
    ): NetworkRecordContract.DecryptedRecord<WrapperContract.Resource> {
        if (
                record.resource.type != WrapperContract.Resource.TYPE.DATA &&
                originalResource != null &&
                originalResource.type != WrapperContract.Resource.TYPE.DATA
        ) {
            record.resource = originalResource

            if (attachmentData != null) {
                @Suppress("UNCHECKED_CAST")
                setUploadData(
                        record,
                        unpackAttachments(attachmentData)
                )
            }
        }

        return record
    }

}
