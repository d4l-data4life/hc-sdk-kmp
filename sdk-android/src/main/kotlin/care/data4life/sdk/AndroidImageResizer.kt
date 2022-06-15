/*
 * Copyright (c) 2022 D4L data4life gGmbH / All rights reserved.
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

package care.data4life.sdk

import android.graphics.Bitmap.CompressFormat
import android.graphics.BitmapFactory
import android.graphics.BitmapFactory.Options
import care.data4life.sdk.AndroidImageResizer.ResizeDimension.Height
import care.data4life.sdk.AndroidImageResizer.ResizeDimension.Width
import care.data4life.sdk.attachment.AttachmentContract.ImageResizer
import care.data4life.sdk.util.MimeType.Companion.recognizeMimeType
import care.data4life.sdk.util.MimeType.JPEG
import care.data4life.sdk.util.MimeType.PNG
import java.io.ByteArrayOutputStream

class AndroidImageResizer : ImageResizer {

    private enum class ResizeDimension {
        Width, Height
    }

    override fun resizeToWidth(originalImage: ByteArray, targetWidth: Int, targetQuality: Int): ByteArray? {
        return resize(Width, originalImage, targetWidth, targetQuality)
    }

    override fun resizeToHeight(originalImage: ByteArray, targetHeight: Int, targetQuality: Int): ByteArray? {
        return resize(Height, originalImage, targetHeight, targetQuality)
    }

    override fun isResizable(data: ByteArray): Boolean {
        val mimeType = recognizeMimeType(data)
        return mimeType == JPEG || mimeType == PNG
    }

    private fun resize(
        resizeType: ResizeDimension,
        originalImage: ByteArray,
        targetSizePx: Int,
        targetQuality: Int
    ): ByteArray? {
        val quality = if (targetQuality == VALUE_UNKNOWN) ImageResizer.DEFAULT_JPEG_QUALITY_PERCENT else targetQuality
        val options = Options()
        options.inJustDecodeBounds = true
        BitmapFactory.decodeByteArray(originalImage, OFFSET_ZERO, originalImage.size, options)
        val originalWidth = options.outWidth
        val originalHeight = options.outHeight
        when (resizeType) {
            Width -> {
                if (originalWidth <= targetSizePx) return null
                options.inSampleSize = calculateInSampleSize(originalHeight, originalWidth, targetSizePx, VALUE_UNKNOWN)
                options.inDensity = options.outWidth
            }
            Height -> {
                if (originalHeight <= targetSizePx) return null
                options.inSampleSize = calculateInSampleSize(originalHeight, originalWidth, VALUE_UNKNOWN, targetSizePx)
                options.inDensity = options.outHeight
            }
        }
        options.inTargetDensity = targetSizePx * options.inSampleSize
        options.inJustDecodeBounds = false
        options.inScaled = true
        val scaledBmp = BitmapFactory.decodeByteArray(originalImage, OFFSET_ZERO, originalImage.size, options)
        val baos = ByteArrayOutputStream()
        return if (scaledBmp.compress(CompressFormat.JPEG, quality, baos)) {
            scaledBmp.recycle()
            baos.toByteArray()
        } else null
    }

    private fun calculateInSampleSize(height: Int, width: Int, reqWidth: Int, reqHeight: Int): Int {
        var inSampleSize = 1
        if (reqWidth != VALUE_UNKNOWN && reqHeight != VALUE_UNKNOWN && width > reqWidth && height > reqHeight) {
            val halfHeight = height / 2
            val halfWidth = width / 2
            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        } else if (reqWidth != VALUE_UNKNOWN && width > reqWidth) {
            val halfWidth = width / 2
            while (halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        } else if (reqHeight != VALUE_UNKNOWN && height > reqHeight) {
            val halfHeight = height / 2
            while (halfHeight / inSampleSize >= reqHeight) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }

    companion object {
        private const val OFFSET_ZERO = 0
        private const val VALUE_UNKNOWN = -1
    }
}
