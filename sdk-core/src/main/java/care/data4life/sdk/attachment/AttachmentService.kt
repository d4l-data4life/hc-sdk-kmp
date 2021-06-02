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
import care.data4life.sdk.attachment.AttachmentContract.Companion.INVALID_DOWNSCALED_IMAGE
import care.data4life.sdk.attachment.AttachmentContract.ImageResizer.Companion.DEFAULT_PREVIEW_SIZE_PX
import care.data4life.sdk.attachment.AttachmentContract.ImageResizer.Companion.DEFAULT_THUMBNAIL_SIZE_PX
import care.data4life.sdk.lang.DataValidationException
import care.data4life.sdk.util.Base64.decode
import care.data4life.sdk.wrapper.SDKImageResizer
import care.data4life.sdk.wrapper.WrapperContract
import io.reactivex.Observable
import io.reactivex.Single

// TODO add internal
class AttachmentService internal constructor(
    private val fileService: AttachmentContract.FileService,
    resizer: AttachmentContract.ImageResizer
) : AttachmentContract.Service {
    private val imageResizer = SDKImageResizer(resizer)

    override fun upload(
        attachments: List<WrapperContract.Attachment>,
        attachmentsKey: GCKey,
        userId: String
    ): Single<List<Pair<WrapperContract.Attachment, List<String>?>>> {
        return Observable.fromIterable(attachments)
            .filter { it.data != null }
            .map { attachment ->
                val originalData = decode(attachment.data!!)
                attachment.id = fileService.uploadFile(
                    attachmentsKey,
                    userId,
                    originalData
                ).blockingGet()

                val additionalIds = uploadDownscaledImages(
                    attachmentsKey,
                    userId,
                    attachment,
                    originalData
                )
                Pair(attachment, additionalIds)
            }
            .toList()
    }

    override fun delete(attachmentId: String, userId: String): Single<Boolean> {
        return fileService.deleteFile(userId, attachmentId)
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
            .filter { it.id != null }
            .map { attachment ->
                val attachmentId = AttachmentDownloadHelper.deriveAttachmentId(attachment)

                val data = fileService.downloadFile(
                    attachmentsKey,
                    userId,
                    attachmentId
                ).blockingGet()

                AttachmentDownloadHelper.addAttachmentPayload(attachment, data)
            }
            .toList()
    }

    // TODO -> thumbnail service
    private fun uploadDownscaledImages(
        attachmentsKey: GCKey,
        userId: String,
        attachment: WrapperContract.Attachment,
        originalData: ByteArray
    ): List<String>? {
        return if (imageResizer.isResizable(originalData)) {
            val additionalIds = mutableListOf<String>()
            additionalIds.add(
                scaleToPreviewAndUpload(
                    attachmentsKey,
                    userId,
                    attachment,
                    originalData
                ).also { it ?: return null }!!
            )

            additionalIds.add(
                scaleToThumbnailAndUpload(
                    attachmentsKey,
                    userId,
                    attachment,
                    originalData
                ).also { it ?: return null }!!
            )

            additionalIds
        } else {
            emptyList()
        }
    }

    private fun scaleToPreviewAndUpload(
        attachmentsKey: GCKey,
        userId: String,
        attachment: WrapperContract.Attachment,
        originalData: ByteArray
    ): String? {
        return resizeAndUpload(
            attachmentsKey,
            userId,
            attachment,
            originalData,
            DEFAULT_PREVIEW_SIZE_PX
        )
    }

    private fun scaleToThumbnailAndUpload(
        attachmentsKey: GCKey,
        userId: String,
        attachment: WrapperContract.Attachment,
        originalData: ByteArray
    ): String? {
        return resizeAndUpload(
            attachmentsKey,
            userId,
            attachment,
            originalData,
            DEFAULT_THUMBNAIL_SIZE_PX
        )
    }

    // TODO -> thumbnail service
    private fun resizeAndUpload(
        attachmentsKey: GCKey,
        userId: String,
        attachment: WrapperContract.Attachment,
        originalData: ByteArray,
        targetHeight: Int
    ): String? {
        return when (val downscaledImage = imageResizer.resize(originalData, targetHeight)) {
            INVALID_DOWNSCALED_IMAGE -> null
            is ByteArray -> fileService.uploadFile(
                attachmentsKey,
                userId,
                downscaledImage
            ).blockingGet()
            else -> attachment.id // currentSizePx <= targetSizePx && nothing to upload
        }
    }
}
