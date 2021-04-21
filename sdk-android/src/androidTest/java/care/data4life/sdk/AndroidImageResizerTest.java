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

import android.content.Context;
import android.graphics.BitmapFactory;

import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;

import androidx.test.InstrumentationRegistry;
import care.data4life.sdk.lang.ImageResizeException;
import care.data4life.sdk.test.util.AssetsHelper;
import care.data4life.sdk.attachment.AttachmentContract.ImageResizer;
import static care.data4life.sdk.attachment.AttachmentContract.ImageResizer.DEFAULT_JPEG_QUALITY_PERCENT;
import static care.data4life.sdk.attachment.AttachmentContract.ImageResizer.DEFAULT_PREVIEW_SIZE_PX;
import static care.data4life.sdk.attachment.AttachmentContract.ImageResizer.DEFAULT_THUMBNAIL_SIZE_PX;
import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.fail;

public class AndroidImageResizerTest {
    private static final String TEST_IMAGE = "consent_document_1920x2487.jpg";
    private static final String FAILED_TO_LOAD_IMG_MSG = "Failed to load image!";
    private static final float TOLERANCE = 0.01f;
    private static Context ctx;
    private static byte[] originalImage;
    private static float originalAspectRatio;
    private static int originalWidth;
    private static int originalHeight;

    //SUT
    private ImageResizer imageResizer = new AndroidImageResizer();

    @BeforeClass
    public static void init() {
        ctx = InstrumentationRegistry.getContext();
        try {
            originalImage = AssetsHelper.loadBytes(ctx, TEST_IMAGE);
        } catch (IOException e) {
            e.printStackTrace();
            fail(FAILED_TO_LOAD_IMG_MSG);
        }
        BitmapFactory.Options imageOptions = decodeImageBounds(originalImage);
        originalWidth = imageOptions.outWidth;
        originalHeight = imageOptions.outHeight;
        originalAspectRatio = (float) originalWidth / originalHeight;
    }

    @Test
    public void imageResizer_shouldResizeImageByWidthToTargetSize() throws ImageResizeException.JpegWriterMissing {
        //when
        byte[] downscaledImage = imageResizer.resizeToWidth(originalImage, DEFAULT_PREVIEW_SIZE_PX, DEFAULT_JPEG_QUALITY_PERCENT);

        //then
        BitmapFactory.Options imageOptions = decodeImageBounds(downscaledImage);
        float downscaledAspectRatio = (float) imageOptions.outWidth / imageOptions.outHeight;
        assertThat(imageOptions.outWidth).isEqualTo(DEFAULT_PREVIEW_SIZE_PX);
        assertThat(downscaledAspectRatio).isWithin(TOLERANCE).of(originalAspectRatio);
    }

    @Test
    public void imageResizer_shouldReturnNull_whenTargetSizeIsSmallerOrEqualToOriginalWidth() throws ImageResizeException.JpegWriterMissing {
        //when
        byte[] downscaledImage = imageResizer.resizeToWidth(originalImage, originalWidth, DEFAULT_JPEG_QUALITY_PERCENT);

        //then
        assertThat(downscaledImage).isNull();
    }

    @Test
    public void imageResizer_shouldResizeImageByHeightToTargetSize() throws ImageResizeException.JpegWriterMissing {
        //when
        byte[] downscaledImage = imageResizer.resizeToHeight(originalImage, DEFAULT_THUMBNAIL_SIZE_PX, DEFAULT_JPEG_QUALITY_PERCENT);

        //then
        BitmapFactory.Options imageOptions = decodeImageBounds(downscaledImage);
        float downscaledAspectRatio = (float) imageOptions.outWidth / imageOptions.outHeight;
        assertThat(imageOptions.outHeight).isEqualTo(DEFAULT_THUMBNAIL_SIZE_PX);
        assertThat(downscaledAspectRatio).isWithin(TOLERANCE).of(originalAspectRatio);
    }

    @Test
    public void imageResizer_shouldReturnNull_whenTargetSizeIsSmallerOrEqualThanOriginalHeight() throws ImageResizeException.JpegWriterMissing {
        //when
        byte[] downscaledImage = imageResizer.resizeToHeight(originalImage, originalHeight, DEFAULT_JPEG_QUALITY_PERCENT);

        //then
        assertThat(downscaledImage).isNull();
    }

    private static BitmapFactory.Options decodeImageBounds(byte[] image) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeByteArray(image, 0, image.length, options);
        return options;
    }
}
