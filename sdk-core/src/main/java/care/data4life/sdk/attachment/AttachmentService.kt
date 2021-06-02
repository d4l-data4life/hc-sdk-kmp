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
import care.data4life.sdk.attachment.AttachmentContract.ImageResizer.Companion.DEFAULT_JPEG_QUALITY_PERCENT
import care.data4life.sdk.attachment.AttachmentContract.ImageResizer.Companion.DEFAULT_PREVIEW_SIZE_PX
import care.data4life.sdk.attachment.AttachmentContract.ImageResizer.Companion.DEFAULT_THUMBNAIL_SIZE_PX
import care.data4life.sdk.attachment.ThumbnailService.Companion.SPLIT_CHAR
import care.data4life.sdk.lang.DataValidationException
import care.data4life.sdk.lang.ImageResizeException
import care.data4life.sdk.log.Log
import care.data4life.sdk.util.Base64.decode
import care.data4life.sdk.util.Base64.encodeToString
import care.data4life.sdk.wrapper.WrapperContract
import io.reactivex.Observable
import io.reactivex.Single

// TODO add internal
class AttachmentService internal constructor(
    private val fileService: AttachmentContract.FileService,
    // TODO move imageResizer to thumbnail service
    private val imageResizer: AttachmentContract.ImageResizer
) : AttachmentContract.Service {
    private val hasher: AttachmentContract.Hasher = AttachmentHasher

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
                var attachmentId = attachment.id!!
                var isPreview = false
                if (attachmentId.contains(SPLIT_CHAR)) {
                    attachmentId = attachmentId.split(SPLIT_CHAR)[DOWNSCALED_ATTACHMENT_ID_POS]
                    isPreview = true
                }

                val data = fileService.downloadFile(
                    attachmentsKey,
                    userId,
                    attachmentId
                ).blockingGet()
                val newHash = hasher.hash(data)

                if (!isPreview &&
                    CompatibilityValidator.isHashable(attachment) &&
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

    override fun delete(attachmentId: String, userId: String): Single<Boolean> {
        return fileService.deleteFile(userId, attachmentId)
    }

    // TODO -> thumbnail service
    private fun uploadDownscaledImages(
        attachmentsKey: GCKey,
        userId: String,
        attachment: WrapperContract.Attachment,
        originalData: ByteArray
    ): List<String>? {
        var additionalIds = mutableListOf<String>()
        if (imageResizer.isResizable(originalData)) {
            additionalIds = ArrayList()
            var downscaledId: String?
            for (position in 0..1) { // TODO: Remove the loop
                downscaledId = resizeAndUpload(
                    attachmentsKey,
                    userId,
                    attachment,
                    originalData,
                    if (position == POSITION_PREVIEW) DEFAULT_PREVIEW_SIZE_PX else DEFAULT_THUMBNAIL_SIZE_PX
                )

                if (downscaledId == null) return null

                additionalIds.add(downscaledId)
            }
        }
        return additionalIds
    }

    // TODO -> thumbnail service
    private fun resizeAndUpload(
        attachmentsKey: GCKey,
        userId: String,
        attachment: WrapperContract.Attachment,
        originalData: ByteArray,
        targetHeight: Int
    ): String? {
        val downscaledImage = try {
            imageResizer.resizeToHeight(
                originalData,
                targetHeight,
                DEFAULT_JPEG_QUALITY_PERCENT
            )
        } catch (exception: ImageResizeException.JpegWriterMissing) {
            Log.error(exception, exception.message)
            return null
        }

        return if (downscaledImage == null) { // currentSizePx <= targetSizePx
            attachment.id // nothing to upload
        } else fileService.uploadFile(attachmentsKey, userId, downscaledImage).blockingGet()
    }

    companion object {
        private const val DOWNSCALED_ATTACHMENT_ID_POS = 1
        private const val POSITION_PREVIEW = 0
    }
}
