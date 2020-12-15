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
import care.data4life.sdk.ImageResizer
import care.data4life.sdk.lang.ImageResizeException
import care.data4life.sdk.log.Log
import care.data4life.sdk.wrapper.WrapperContract


class ThumbnailService internal constructor(
        private val imageResizer: ImageResizer,
        private val fileService: FileContract.Service
): ThumbnailContract.Service {

    override fun uploadDownscaledImages(
            attachmentsKey: GCKey,
            userId: String,
            attachment: WrapperContract.Attachment,
            originalData: ByteArray
    ): List<String> {
        return if(this.imageResizer.isResizable(originalData)) {
            val ids = mutableListOf<String>()

            this.resizeAndUpload(
                    attachmentsKey,
                    userId,
                    attachment,
                    originalData,
                    ImageResizer.DEFAULT_PREVIEW_SIZE_PX
            ).also {
                if(it.isNotEmpty()) ids.add(it)
            }

            this.resizeAndUpload(
                    attachmentsKey,
                    userId,
                    attachment,
                    originalData,
                    ImageResizer.DEFAULT_THUMBNAIL_SIZE_PX
            ).also {
                if(it.isNotEmpty()) ids.add(it)
            }

            ids
        } else {
            listOf()
        }
    }

    private fun resizeAndUpload(
            attachmentsKey: GCKey,
            userId: String,
            attachment: WrapperContract.Attachment,
            originalData: ByteArray,
            targetHeight: Int
    ): String {
        val downscaledImage = try {
            imageResizer.resizeToHeight(originalData, targetHeight, ImageResizer.DEFAULT_JPEG_QUALITY_PERCENT)
        } catch (exception: ImageResizeException.JpegWriterMissing) {
            Log.error(exception, exception.message)
            return ""
        }

        return if(downscaledImage == null) {
            attachment.id ?: ""  //Nothing to upload
        } else {
            fileService.uploadFile(attachmentsKey, userId, downscaledImage).blockingGet()
        }
    }

    companion object {
        const val SPLIT_CHAR = "#"
    }
}
