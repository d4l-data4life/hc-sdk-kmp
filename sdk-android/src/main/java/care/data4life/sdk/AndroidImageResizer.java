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

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import java.io.ByteArrayOutputStream;

import javax.annotation.Nullable;

import care.data4life.sdk.attachment.AttachmentContract.ImageResizer;
import care.data4life.sdk.lang.CoreRuntimeException;
import care.data4life.sdk.util.MimeType;

public class AndroidImageResizer implements ImageResizer {
    private static final int OFFSET_ZERO = 0;
    private static final int VALUE_UNKNOWN = -1;

    private enum ResizeDimension {
        Width,
        Height
    }

    @Override
    @Nullable
    public byte[] resizeToWidth(byte[] originalImage, int targetWidth, int targetQuality) {
        return resize(ResizeDimension.Width, originalImage, targetWidth, targetQuality);
    }

    @Override
    @Nullable
    public byte[] resizeToHeight(byte[] originalImage, int targetHeight, int targetQuality) {
        return resize(ResizeDimension.Height, originalImage, targetHeight, targetQuality);
    }

    @Override
    public boolean isResizable(byte[] data) {
        //TODO set jvmstatic to helpers
        MimeType mimeType = MimeType.Companion.recognizeMimeType(data);
        return mimeType == MimeType.JPEG || mimeType == MimeType.PNG;
    }

    @Nullable
    private byte[] resize(ResizeDimension resizeType, byte[] originalImage, int targetSizePx, int targetQuality) {
        targetQuality = (targetQuality == VALUE_UNKNOWN ? DEFAULT_JPEG_QUALITY_PERCENT : targetQuality);

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeByteArray(originalImage, OFFSET_ZERO, originalImage.length, options);

        int originalWidth = options.outWidth;
        int originalHeight = options.outHeight;
        switch (resizeType) {
            case Width:
                if (originalWidth <= targetSizePx) return null;
                options.inSampleSize = calculateInSampleSize(originalHeight, originalWidth, targetSizePx, VALUE_UNKNOWN);
                options.inDensity = options.outWidth;
                break;
            case Height:
                if (originalHeight <= targetSizePx) return null;
                options.inSampleSize = calculateInSampleSize(originalHeight, originalWidth, VALUE_UNKNOWN, targetSizePx);
                options.inDensity = options.outHeight;
                break;
            default:
                throw new CoreRuntimeException.UnsupportedOperation();
        }
        options.inTargetDensity = targetSizePx * options.inSampleSize;
        options.inJustDecodeBounds = false;
        options.inScaled = true;

        Bitmap scaledBmp = BitmapFactory.decodeByteArray(originalImage, OFFSET_ZERO, originalImage.length, options);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        if (scaledBmp.compress(Bitmap.CompressFormat.JPEG, targetQuality, baos)) {
            scaledBmp.recycle();
            return baos.toByteArray();
        } else return null;
    }

    private int calculateInSampleSize(final int height, final int width, int reqWidth, int reqHeight) {
        int inSampleSize = 1;

        if (reqWidth != VALUE_UNKNOWN && reqHeight != VALUE_UNKNOWN && width > reqWidth && height > reqHeight) {
            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        } else if (reqWidth != VALUE_UNKNOWN && width > reqWidth) {
            final int halfWidth = width / 2;

            while ((halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        } else if (reqHeight != VALUE_UNKNOWN && height > reqHeight) {
            final int halfHeight = height / 2;

            while ((halfHeight / inSampleSize) >= reqHeight) {
                inSampleSize *= 2;
            }
        }

        return inSampleSize;
    }
}

