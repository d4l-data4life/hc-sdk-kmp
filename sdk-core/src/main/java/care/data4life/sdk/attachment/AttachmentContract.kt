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

import care.data4life.crypto.GCKey
import care.data4life.sdk.config.DataRestrictionException
import care.data4life.sdk.lang.DataValidationException
import care.data4life.sdk.model.DownloadType
import care.data4life.sdk.network.model.NetworkRecordContract
import care.data4life.sdk.wrapper.WrapperContract
import io.reactivex.Single

// TODO change to internal
interface AttachmentContract {

    interface Service {
        fun upload(
                attachments: List<WrapperContract.Attachment>,
                attachmentsKey: GCKey,
                userId: String
        ): Single<List<Pair<WrapperContract.Attachment, List<String>>>>


        @Throws(DataValidationException.InvalidAttachmentPayloadHash::class)
        fun download(
                attachments: List<WrapperContract.Attachment>,
                attachmentsKey: GCKey,
                userId: String
        ): Single<List<WrapperContract.Attachment>>


        fun delete(attachmentId: String, userId: String): Single<Boolean>

        fun updateAttachmentMeta(attachment: WrapperContract.Attachment): WrapperContract.Attachment

        fun getValidHash(attachment: WrapperContract.Attachment): String?

        @Throws(DataValidationException.IdUsageViolation::class,
                DataValidationException.InvalidAttachmentPayloadHash::class)
        fun downloadAttachmentsFromStorage(
                attachmentIds: List<String>,
                userId: String,
                type: DownloadType,
                decryptedRecord: NetworkRecordContract.DecryptedRecord
        ): Single<out List<WrapperContract.Attachment>>

        @Throws(DataRestrictionException.MaxDataSizeViolation::class, DataRestrictionException.UnsupportedFileType::class)
        fun checkDataRestrictions(resource: WrapperContract.Resource?)

        fun extractUploadData(
                resource: WrapperContract.Resource
        ): HashMap<WrapperContract.Attachment, String?>?
    }

    interface FhirDateValidator {
        fun isInvalidateDate(attachment: WrapperContract.Attachment): Boolean
    }

    interface Client {
        fun removeUploadData(
                record: NetworkRecordContract.DecryptedRecord
        ): NetworkRecordContract.DecryptedRecord

        fun restoreUploadData(
                record: NetworkRecordContract.DecryptedRecord,
                originalResource: WrapperContract.Resource?,
                attachmentData: HashMap<WrapperContract.Attachment, String?>?
        ): NetworkRecordContract.DecryptedRecord

        fun downloadData(
                record: NetworkRecordContract.DecryptedRecord,
                userId: String?
        ): NetworkRecordContract.DecryptedRecord

        fun updateData(
                record: NetworkRecordContract.DecryptedRecord,
                newResource: WrapperContract.Resource?,
                userId: String?
        ): NetworkRecordContract.DecryptedRecord

        fun uploadData(
                record: NetworkRecordContract.DecryptedRecord,
                userId: String
        ): NetworkRecordContract.DecryptedRecord
    }
}