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

package care.data4life.sdk.wrapper

import care.data4life.sdk.attachment.AttachmentContract
import care.data4life.sdk.lang.ImageResizeException
import care.data4life.sdk.log.Log

class SDKImageResizer(
    private val resizer: AttachmentContract.ImageResizer
) : WrapperContract.SDKImageResizer {
    override fun isResizable(data: ByteArray): Boolean = resizer.isResizable(data)

    override fun resize(
        data: ByteArray,
        targetHeight: Int
    ): ByteArray? {
        return try {
            resizer.resizeToHeight(
                data,
                targetHeight,
                AttachmentContract.ImageResizer.DEFAULT_JPEG_QUALITY_PERCENT
            )
        } catch (exception: ImageResizeException.JpegWriterMissing) {
            Log.error(exception, exception.message)
            data
        }
    }
}
