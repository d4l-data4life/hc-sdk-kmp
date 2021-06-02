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
import care.data4life.sdk.wrapper.WrapperContract.Attachment
import io.reactivex.Single

interface AttachmentContract {
    interface Service {
        fun upload(
            attachments: List<Attachment>,
            attachmentsKey: GCKey,
            userId: String
        ): Single<List<Pair<Attachment, List<String>?>>>

        @Throws(DataValidationException.InvalidAttachmentPayloadHash::class)
        fun download(
            attachments: List<Attachment>,
            attachmentsKey: GCKey,
            userId: String
        ): Single<List<Attachment>>

        fun delete(attachmentId: String, userId: String): Single<Boolean>
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

    // TODO: This should be internal of Attachments
    fun interface Hasher {
        fun hash(data: ByteArray): String
    }

    // TODO: This should be internal of Attachments
    interface Guardian {
        @Throws(DataValidationException.IdUsageViolation::class)
        fun guardId(attachment: Attachment)

        @Throws(DataValidationException.IdUsageViolation::class)
        fun guardNonNullId(attachment: Attachment)

        @Throws(DataValidationException.IdUsageViolation::class)
        fun guardIdAgainstExistingIds(attachment: Attachment, referenceIds: Set<String>)

        @Throws(
            DataValidationException.ExpectedFieldViolation::class,
            DataValidationException.InvalidAttachmentPayloadHash::class
        )
        fun guardHash(attachment: Attachment, reference: Attachment? = null): Boolean

        @Throws(DataValidationException.ExpectedFieldViolation::class)
        fun guardSize(attachment: Attachment)
    }

    companion object {
        val INVALID_DOWNSCALED_IMAGE = ByteArray(0)
        internal const val DOWNSCALED_ATTACHMENT_ID_POS = 1
        internal const val POSITION_PREVIEW = 0
        internal const val SPLIT_CHAR = "#"
    }
}
