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
import care.data4life.sdk.lang.DataValidationException
import care.data4life.sdk.lang.ImageResizeException
import care.data4life.sdk.wrapper.WrapperContract
import io.reactivex.Single

// TODO change to internal
class AttachmentContract {

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
    }

    internal interface CompatibilityValidator {
        fun isHashable(attachment: WrapperContract.Attachment): Boolean
    }

    interface FileService {
        fun downloadFile(key: GCKey, userId: String, fileId: String): Single<ByteArray>
        fun uploadFile(key: GCKey, userId: String, data: ByteArray): Single<String>
        fun deleteFile(userId: String, fileId: String): Single<Boolean>
    }

    // FIXME check internal use against Java and Kotlin Clients
    interface ImageResizer {

        @Throws(ImageResizeException.JpegWriterMissing::class)
        fun resizeToWidth(
            originalImage: ByteArray,
            targetWidth: Int,
            targetQuality: Int
        ): ByteArray?

        @Throws(ImageResizeException.JpegWriterMissing::class)
        fun resizeToHeight(
            originalImage: ByteArray,
            targetHeight: Int,
            targetQuality: Int
        ): ByteArray?

        fun isResizable(data: ByteArray): Boolean

        companion object {
            const val DEFAULT_THUMBNAIL_SIZE_PX = 200
            const val DEFAULT_PREVIEW_SIZE_PX = 1000
            const val DEFAULT_JPEG_QUALITY_PERCENT = 80
        }
    }
}

