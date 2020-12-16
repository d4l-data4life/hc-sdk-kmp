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
import care.data4life.sdk.attachment.ThumbnailService.Companion.SPLIT_CHAR
import care.data4life.sdk.config.DataRestriction
import care.data4life.sdk.config.DataRestrictionException
import care.data4life.sdk.lang.DataValidationException
import care.data4life.sdk.model.DownloadType
import care.data4life.sdk.network.model.NetworkRecordContract
import care.data4life.sdk.util.Base64.decode
import care.data4life.sdk.util.Base64.encodeToString
import care.data4life.sdk.util.HashUtil.sha1
import care.data4life.sdk.util.MimeType
import care.data4life.sdk.wrapper.AttachmentFactory
import care.data4life.sdk.wrapper.FhirAttachmentHelper
import care.data4life.sdk.wrapper.HelperContract
import care.data4life.sdk.wrapper.WrapperContract
import care.data4life.sdk.wrapper.WrapperFactoryContract
import io.reactivex.Observable
import io.reactivex.Single

// TODO add internal
class AttachmentService internal constructor(
        private val fileService: FileContract.Service,
        private val thumbnailService: ThumbnailContract.Service
) : AttachmentContract.Service {
    private val fhirDateValidator: AttachmentContract.FhirDateValidator = FhirDateValidator
    private val fhirAttachmentHelper: HelperContract.FhirAttachmentHelper = FhirAttachmentHelper
    private val attachmentFactory: WrapperFactoryContract.AttachmentFactory = AttachmentFactory

    override fun upload(
            attachments: List<WrapperContract.Attachment>,
            attachmentsKey: GCKey,
            userId: String
    ): Single<List<Pair<WrapperContract.Attachment, List<String>>>> {
        return Observable.fromIterable(attachments)
                .filter { it.data != null }
                .map { attachment ->
                    val originalData = decode(attachment.data!!)
                    attachment.id = fileService.uploadFile(attachmentsKey, userId, originalData).blockingGet()
                    val additionalIds = thumbnailService.uploadDownscaledImages(
                            attachmentsKey,
                            userId,
                            attachment,
                            originalData
                    )
                    Pair(attachment, additionalIds)
                }
                .filter { (first) -> first.id != null } //FIXME: Should this be removed?
                .toList()
    }

    @Throws(DataValidationException.InvalidAttachmentPayloadHash::class)
    override fun download(
            attachments: List<WrapperContract.Attachment>,
            attachmentsKey: GCKey,
            userId: String
    ): Single<List<WrapperContract.Attachment>> {
        return Observable
                .fromCallable { attachments }
                .flatMapIterable { it }
                .filter{ it.id != null }
                .map { attachment ->
                    var attachmentId = attachment.id!!
                    var isPreview = false
                    if (attachmentId.contains(SPLIT_CHAR)) {
                        attachmentId = attachmentId.split(SPLIT_CHAR)[DOWNSCALED_ATTACHMENT_ID_POS]
                        isPreview = true
                    }

                    val data = fileService.downloadFile(attachmentsKey, userId, attachmentId).blockingGet()
                    val newHash = encodeToString(sha1(data))

                    if (!isPreview &&
                            fhirDateValidator.isInvalidateDate(attachment) &&
                            attachment.hash != newHash
                    ) {
                        throw DataValidationException.InvalidAttachmentPayloadHash()
                    } else {
                        attachment.also {
                            it.data = encodeToString(data)
                            it.hash = newHash
                        }
                    }
                }
                .toList()
    }

    override fun delete(
            attachmentId: String,
            userId: String
    ): Single<Boolean> = fileService.deleteFile(userId, attachmentId)

    override fun updateAttachmentMeta(
            attachment: WrapperContract.Attachment
    ): WrapperContract.Attachment {
        if(attachment.data != null) {
            val data = decode(attachment.data!!)
            attachment.size = data.size
            attachment.hash = encodeToString(sha1(data))
        }

        return attachment
    }

    override fun getValidHash(attachment: WrapperContract.Attachment): String {
        val data = decode(attachment.data!!)
        return encodeToString(sha1(data))
    }

    @Throws(DataValidationException.IdUsageViolation::class,
            DataValidationException.InvalidAttachmentPayloadHash::class)
    override fun downloadAttachmentsFromStorage(
            attachmentIds: List<String>,
            userId: String,
            type: DownloadType,
            decryptedRecord: NetworkRecordContract.DecryptedRecord
    ): Single<out List<WrapperContract.Attachment>> {
        val resource = decryptedRecord.resource.unwrap()

        if (fhirAttachmentHelper.hasAttachment(resource)) {
            val attachments = fhirAttachmentHelper.getAttachment(resource)
            val validAttachments = mutableListOf<WrapperContract.Attachment>()

            for (rawAttachment in attachments) {
                val attachment = attachmentFactory.wrap(rawAttachment)
                if (attachmentIds.contains(attachment?.id)) {
                    validAttachments.add(attachment!!)
                }
            }
            if (validAttachments.size != attachmentIds.size)
                throw DataValidationException.IdUsageViolation("Please provide correct attachment ids!")

            thumbnailService.setAttachmentIdForDownloadType(
                    validAttachments,
                    fhirAttachmentHelper.getIdentifier(resource),
                    type
            )

            return download(
                    validAttachments,
                    // FIXME this is forced
                    decryptedRecord.attachmentsKey!!,
                    userId
            )
                    .flattenAsObservable { it }
                    .map { attachment ->
                        attachment.also {
                            if (attachment.id!!.contains(ThumbnailService.SPLIT_CHAR)) updateAttachmentMeta(attachment)
                        }
                    }
                    .toList()
        }

        throw IllegalArgumentException("Expected a record of a type that has attachment")
    }

    @Throws(DataRestrictionException.MaxDataSizeViolation::class, DataRestrictionException.UnsupportedFileType::class)
    override fun checkDataRestrictions(resource: WrapperContract.Resource?) {
        if (resource == null || resource.type == WrapperContract.Resource.TYPE.DATA) {
            return
        }

        val attachments = fhirAttachmentHelper.getAttachment(resource.unwrap())
        for (rawAttachment in attachments) {
            val attachment = attachmentFactory.wrap(rawAttachment)
            attachment?.data ?: return

            val data = decode(attachment.data!!)
            if (MimeType.recognizeMimeType(data) == MimeType.UNKNOWN) {
                throw DataRestrictionException.UnsupportedFileType()
            }
            if (data.size > DataRestriction.DATA_SIZE_MAX_BYTES) {
                throw DataRestrictionException.MaxDataSizeViolation()
            }
        }
    }

    override fun extractUploadData(
            resource: WrapperContract.Resource
    ): HashMap<WrapperContract.Attachment, String?>? {
        return if (resource.type != WrapperContract.Resource.TYPE.DATA) {
            val attachments = fhirAttachmentHelper.getAttachment(resource.unwrap())
            if (attachments.isEmpty()) return null

            val data = HashMap<WrapperContract.Attachment, String?>(attachments.size)
            for (rawAttachment in attachments) {
                val attachment = attachmentFactory.wrap(rawAttachment)
                if (attachment?.data != null) {
                    data[attachment] = attachment.data
                }
            }

            if (data.isEmpty()) null else data
        } else {
            null
        }
    }

    companion object {
        private const val DOWNSCALED_ATTACHMENT_ID_POS = 1
    }
}
