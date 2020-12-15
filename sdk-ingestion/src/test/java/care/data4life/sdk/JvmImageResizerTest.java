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

import org.junit.BeforeClass;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

import care.data4life.sdk.lang.ImageResizeException;
import care.data4life.sdk.test.util.ResourceHelper;

import static care.data4life.sdk.ImageResizer.DEFAULT_JPEG_QUALITY_PERCENT;
import static care.data4life.sdk.ImageResizer.DEFAULT_PREVIEW_SIZE_PX;
import static care.data4life.sdk.ImageResizer.DEFAULT_THUMBNAIL_SIZE_PX;
import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.fail;

public class JvmImageResizerTest {
    private static final String TEST_IMAGE = "consent_document_1920x2487.jpg";
    private static final String FAILED_TO_LOAD_IMG_MSG = "Failed to load test image!";
    private static final String FAILED_TO_DECODE_IMG_MSG = "Failed to decode test image!";
    private static final float TOLERANCE = 0.01f;

    private static byte[] originalImage;
    private static float originalAspectRatio;
    private static int originalWidth;
    private static int originalHeight;

    //SUT
    private ImageResizer imageResizer = new JvmImageResizer();

    @BeforeClass
    public static void init() {
        try {
            originalImage = ResourceHelper.loadBytes(TEST_IMAGE);
        } catch (IOException e) {
            e.printStackTrace();
            fail(FAILED_TO_LOAD_IMG_MSG);
        }


        ImageBounds imageBounds = null;
        try {
            imageBounds = decodeImageBounds(originalImage);
        } catch (IOException e) {
            e.printStackTrace();
            fail(FAILED_TO_DECODE_IMG_MSG);
        }
        originalWidth = imageBounds.width;
        originalHeight = imageBounds.height;
        originalAspectRatio = (float) originalWidth / originalHeight;
    }

    @Test
    public void imageResizer_shouldResizeImageByWidthToTargetSize() throws IOException, ImageResizeException.JpegWriterMissing {
        //when
        byte[] downscaledImage = imageResizer.resizeToWidth(originalImage, DEFAULT_PREVIEW_SIZE_PX, DEFAULT_JPEG_QUALITY_PERCENT);

        //then
        ImageBounds imageBounds = decodeImageBounds(downscaledImage);
        float downscaledAspectRatio = (float) imageBounds.width / imageBounds.height;
        assertThat(imageBounds.width).isEqualTo(DEFAULT_PREVIEW_SIZE_PX);
        assertThat(downscaledAspectRatio).isWithin(TOLERANCE).of(originalAspectRatio);
    }

    @Test
    public void imageResizer_shouldReturnNull_whenTargetSizeIsSmallerOrEqualThanOriginalWidth() throws ImageResizeException.JpegWriterMissing {
        //when
        byte[] downscaledImage = imageResizer.resizeToWidth(originalImage, originalWidth, DEFAULT_JPEG_QUALITY_PERCENT);

        //then
        assertThat(downscaledImage).isNull();
    }

    @Test
    public void imageResizer_shouldResizeImageByHeightToTargetSize() throws IOException, ImageResizeException.JpegWriterMissing {
        //when
        byte[] downscaledImage = imageResizer.resizeToHeight(originalImage, DEFAULT_THUMBNAIL_SIZE_PX, DEFAULT_JPEG_QUALITY_PERCENT);

        //then
        ImageBounds imageBounds = decodeImageBounds(downscaledImage);
        float downscaledAspectRatio = (float) imageBounds.width / imageBounds.height;
        assertThat(imageBounds.height).isEqualTo(DEFAULT_THUMBNAIL_SIZE_PX);
        assertThat(downscaledAspectRatio).isWithin(TOLERANCE).of(originalAspectRatio);
    }

    @Test
    public void imageResizer_shouldReturnNull_whenTargetSizeIsSmallerOrEqualThanOriginalHeight() throws ImageResizeException.JpegWriterMissing {
        //when
        byte[] downscaledImage = imageResizer.resizeToHeight(originalImage, originalHeight, DEFAULT_JPEG_QUALITY_PERCENT);

        //then
        assertThat(downscaledImage).isNull();
    }

    private static ImageBounds decodeImageBounds(byte[] image) throws IOException {
        ImageInputStream is = ImageIO.createImageInputStream(new ByteArrayInputStream(image));
        ImageReader reader = ImageIO.getImageReaders(is).next();
        reader.setInput(is);
        return new ImageBounds(reader.getWidth(reader.getMinIndex()), reader.getHeight(reader.getMinIndex()));
    }

    private static class ImageBounds {
        private int width;
        private int height;

        public ImageBounds(int width, int height) {
            this.width = width;
            this.height = height;
        }
    }
}
