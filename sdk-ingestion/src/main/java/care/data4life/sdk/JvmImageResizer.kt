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
package care.data4life.sdk

import care.data4life.sdk.JvmImageResizer.ResizeDimension.Height
import care.data4life.sdk.JvmImageResizer.ResizeDimension.Width
import care.data4life.sdk.attachment.AttachmentContract.ImageResizer
import care.data4life.sdk.lang.ImageResizeException.JpegWriterMissing
import care.data4life.sdk.log.Log
import care.data4life.sdk.util.MimeType.Companion.recognizeMimeType
import care.data4life.sdk.util.MimeType.JPEG
import care.data4life.sdk.util.MimeType.PNG
import java.awt.RenderingHints
import java.awt.Transparency
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import javax.imageio.IIOImage
import javax.imageio.ImageIO
import javax.imageio.ImageWriteParam
import javax.imageio.stream.MemoryCacheImageOutputStream

class JvmImageResizer : ImageResizer {

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
        val dataMimeType = recognizeMimeType(data)
        return dataMimeType == JPEG || dataMimeType == PNG
    }

    private fun resize(
        resizeType: ResizeDimension,
        originalImage: ByteArray,
        targetSizePx: Int,
        targetQuality: Int
    ): ByteArray? {
        val image = decodeImage(originalImage) ?: return null
        val scaledImage: BufferedImage = when (resizeType) {
            Width -> {
                if (image.width <= targetSizePx) return null
                val targetHeightPx = (targetSizePx.toDouble() * image.height / image.width).toInt()
                downscaleImage(image, targetSizePx, targetHeightPx, true)
            }
            Height -> {
                if (image.height <= targetSizePx) return null
                val targetWidthPx = (targetSizePx.toDouble() * image.width / image.height).toInt()
                downscaleImage(image, targetWidthPx, targetSizePx, true)
            }
        }
        return compressImage(scaledImage, targetQuality)
    }

    private fun decodeImage(imageData: ByteArray): BufferedImage? {
        return try {
            ImageIO.read(ByteArrayInputStream(imageData))
        } catch (e: IOException) {
            Log.error(e, e.message)
            null
        }
    }

    private fun downscaleImage(
        originalImage: BufferedImage,
        targetWidth: Int,
        targetHeight: Int,
        higherQuality: Boolean
    ): BufferedImage {
        val type =
            if (originalImage.transparency == Transparency.OPAQUE) BufferedImage.TYPE_INT_RGB else BufferedImage.TYPE_INT_ARGB
        var scaledImage = originalImage
        var w: Int
        var h: Int
        if (higherQuality) {
            // Use multi-step technique: start with original size, then
            // scale down in multiple passes with drawImage()
            // until the target size is reached
            w = originalImage.width
            h = originalImage.height
        } else {
            // Use one-step technique: scale directly from original
            // size to target size with a single drawImage() call
            w = targetWidth
            h = targetHeight
        }
        do {
            if (higherQuality && w > targetWidth) {
                w /= 2
                if (w < targetWidth) {
                    w = targetWidth
                }
            }
            if (higherQuality && h > targetHeight) {
                h /= 2
                if (h < targetHeight) {
                    h = targetHeight
                }
            }
            val tmp = BufferedImage(w, h, type)
            val g2 = tmp.createGraphics()
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR)
            g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
            g2.drawImage(scaledImage, 0, 0, w, h, null)
            g2.dispose()

            scaledImage = tmp
        } while (w != targetWidth || h != targetHeight)
        return scaledImage
    }

    @Throws(JpegWriterMissing::class)
    private fun compressImage(image: BufferedImage, quality: Int): ByteArray? {
        val qualityPercent =
            (if (quality == VALUE_UNKNOWN) ImageResizer.DEFAULT_JPEG_QUALITY_PERCENT else quality) / HUNDRED_PERCENT
        val iterator = ImageIO.getImageWritersByFormatName(JPEG_WRITER)
        if (!iterator.hasNext()) {
            throw JpegWriterMissing()
        }
        val jpgWriter = iterator.next()
        val jpgWriteParam = jpgWriter.defaultWriteParam
        jpgWriteParam.compressionMode = ImageWriteParam.MODE_EXPLICIT
        jpgWriteParam.compressionQuality = qualityPercent
        val compressedImage = ByteArrayOutputStream()
        try {
            jpgWriter.output = MemoryCacheImageOutputStream(compressedImage)
            jpgWriter.write(null, IIOImage(image, null, null), jpgWriteParam)
        } catch (e: IOException) {
            Log.error(e, e.message)
            return null
        } finally {
            jpgWriter.dispose()
        }
        return compressedImage.toByteArray()
    }

    companion object {
        private const val JPEG_WRITER = "jpeg"
        private const val VALUE_UNKNOWN = -1
        private const val HUNDRED_PERCENT = 100f
    }
}
