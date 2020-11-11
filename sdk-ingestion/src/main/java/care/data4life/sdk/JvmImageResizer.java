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

package care.data4life.sdk;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Transparency;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Iterator;

import javax.annotation.Nullable;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.MemoryCacheImageOutputStream;

import care.data4life.sdk.lang.ImageResizeException;
import care.data4life.sdk.log.Log;
import care.data4life.sdk.util.MimeType;

public class JvmImageResizer implements ImageResizer {
    private static final String JPEG_WRITER = "jpeg";
    private static final String JPEG_WRITER_MISSING_MSG = "Jpeg writer missing!";

    private static final int OFFSET_ZERO = 0;
    private static final int VALUE_UNKNOWN = -1;
    private static final float HUNDRED_PERCENT = 100f;

    private enum ResizeDimension {
        Width,
        Height
    }


    @Override
    @Nullable
    public byte[] resizeToWidth(byte[] originalImage, int targetWidth, int targetQuality) throws ImageResizeException.JpegWriterMissing {
        return resize(ResizeDimension.Width, originalImage, targetWidth, targetQuality);
    }

    @Override
    @Nullable
    public byte[] resizeToHeight(byte[] originalImage, int targetHeight, int targetQuality) throws ImageResizeException.JpegWriterMissing {
        return resize(ResizeDimension.Height, originalImage, targetHeight, targetQuality);
    }

    public boolean isResizable(byte[] data) {
        MimeType dataMimeType = MimeType.Companion.recognizeMimeType(data);
        return dataMimeType == MimeType.JPEG || dataMimeType == MimeType.PNG;
    }

    @Nullable
    private byte[] resize(ResizeDimension resizeType, byte[] originalImage, int targetSizePx, int targetQuality) throws ImageResizeException.JpegWriterMissing {
        BufferedImage image = decodeImage(originalImage);
        if (image == null) return null;

        BufferedImage scaledImage;
        switch (resizeType) {
            case Width:
                if (image.getWidth() <= targetSizePx) return null;
                int targetHeightPx = (int) ((double) targetSizePx * image.getHeight() / image.getWidth());
                scaledImage = downscaleImage(image, targetSizePx, targetHeightPx, true);
                break;
            case Height:
                if (image.getHeight() <= targetSizePx) return null;
                int targetWidthPx = (int) ((double) targetSizePx * image.getWidth() / image.getHeight());
                scaledImage = downscaleImage(image, targetWidthPx, targetSizePx, true);
                break;
            default:
                throw new IllegalStateException("Unexpected case!");
        }

        return compressImage(scaledImage, targetQuality);
    }

    @Nullable
    private BufferedImage decodeImage(byte[] imageData) {
        try {
            return ImageIO.read(new ByteArrayInputStream(imageData));
        } catch (IOException e) {
            Log.error(e, e.getMessage());
            return null;
        }
    }

    private BufferedImage downscaleImage(
            BufferedImage originalImage,
            int targetWidth,
            int targetHeight,
            boolean higherQuality) {

        int type = (originalImage.getTransparency() == Transparency.OPAQUE) ? BufferedImage.TYPE_INT_RGB : BufferedImage.TYPE_INT_ARGB;
        BufferedImage scaledImage = originalImage;

        int w, h;
        if (higherQuality) {
            // Use multi-step technique: start with original size, then
            // scale down in multiple passes with drawImage()
            // until the target size is reached
            w = originalImage.getWidth();
            h = originalImage.getHeight();
        } else {
            // Use one-step technique: scale directly from original
            // size to target size with a single drawImage() call
            w = targetWidth;
            h = targetHeight;
        }

        do {
            if (higherQuality && w > targetWidth) {
                w /= 2;
                if (w < targetWidth) {
                    w = targetWidth;
                }
            }

            if (higherQuality && h > targetHeight) {
                h /= 2;
                if (h < targetHeight) {
                    h = targetHeight;
                }
            }

            BufferedImage tmp = new BufferedImage(w, h, type);
            Graphics2D g2 = tmp.createGraphics();
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.drawImage(scaledImage, 0, 0, w, h, null);
            g2.dispose();

            scaledImage = tmp;
        } while (w != targetWidth || h != targetHeight);

        return scaledImage;
    }

    @Nullable
    private byte[] compressImage(BufferedImage image, int quality) throws ImageResizeException.JpegWriterMissing {
        float qualityPercent = (quality == VALUE_UNKNOWN ? DEFAULT_JPEG_QUALITY_PERCENT : quality) / HUNDRED_PERCENT;

        Iterator<ImageWriter> iterator = ImageIO.getImageWritersByFormatName(JPEG_WRITER);
        if (!iterator.hasNext()) {
            throw new ImageResizeException.JpegWriterMissing();
        }
        ImageWriter jpgWriter = iterator.next();
        ImageWriteParam jpgWriteParam = jpgWriter.getDefaultWriteParam();
        jpgWriteParam.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
        jpgWriteParam.setCompressionQuality(qualityPercent);

        ByteArrayOutputStream compressedImage = new ByteArrayOutputStream();
        try {
            jpgWriter.setOutput(new MemoryCacheImageOutputStream(compressedImage));
            jpgWriter.write(null, new IIOImage(image, null, null), jpgWriteParam);
        } catch (IOException e) {
            Log.error(e, e.getMessage());
            return null;
        } finally {
            jpgWriter.dispose();
        }

        return compressedImage.toByteArray();
    }
}
