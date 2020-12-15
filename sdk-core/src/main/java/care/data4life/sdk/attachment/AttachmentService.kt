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
import care.data4life.sdk.lang.DataValidationException
import care.data4life.sdk.util.Base64.decode
import care.data4life.sdk.util.Base64.encodeToString
import care.data4life.sdk.util.HashUtil.sha1
import care.data4life.sdk.wrapper.WrapperContract
import io.reactivex.Observable
import io.reactivex.Single

// TODO add internal
class AttachmentService internal constructor(
        private val fileService: FileContract.Service,
        private val thumbnailService: ThumbnailContract.Service
) : AttachmentContract.Service {
    private val fhirDateValidator: AttachmentContract.FhirDateValidator = FhirDateValidator

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

    companion object {
        private const val DOWNSCALED_ATTACHMENT_ID_POS = 1
    }
}
