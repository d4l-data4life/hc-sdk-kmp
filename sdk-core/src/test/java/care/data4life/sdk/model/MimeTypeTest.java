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

package care.data4life.sdk.model;

import org.junit.Test;
import java.io.IOException;
import java.util.Arrays;
import care.data4life.sdk.test.util.TestResourceHelper;
import care.data4life.sdk.util.MimeType;
import static com.google.common.truth.Truth.assertThat;

public class MimeTypeTest {

    private static final byte[] VALID_DCM_SIGNATURE = {0x44, 0x49, 0x43, 0x4D};
    private static final byte INVALID_LAST_DCM_BYTE = 0x33; //valid one would be 0x4D

    private static final byte[] DCM_SIGNATURE_WITH_OFFSET;
    private static final byte[] TRIMMED_DCM_SIG_WITH_OFFSET;
    private static final byte[] INVALID_DCM_SIG_WITH_OFFSET;
    private static final String IMAGE_TIFF = "image/tiff";
    private static final String IMAGE_PNG = "image/png";
    private static final String IMAGE_JPG = "image/jpg";
    private static final String APPLICATION_PDF = "application/pdf";
    private static final String APPLICATION_DCM = "application/dicom";

    static {
        int firstDCMsignatureIndx = 0;
        int numberOfMissingBytes = 1;
        int minimalDCMlength = MimeType.DCM.offset() + MimeType.DCM.byteSignature()[firstDCMsignatureIndx].length;
        int lastByteDCMindx = minimalDCMlength - 1;

        DCM_SIGNATURE_WITH_OFFSET = new byte[minimalDCMlength];
        System.arraycopy(VALID_DCM_SIGNATURE, 0, DCM_SIGNATURE_WITH_OFFSET, MimeType.DCM.offset(), VALID_DCM_SIGNATURE.length);

        TRIMMED_DCM_SIG_WITH_OFFSET = Arrays.copyOfRange(DCM_SIGNATURE_WITH_OFFSET, 0, DCM_SIGNATURE_WITH_OFFSET.length - numberOfMissingBytes);

        INVALID_DCM_SIG_WITH_OFFSET = DCM_SIGNATURE_WITH_OFFSET.clone();
        INVALID_DCM_SIG_WITH_OFFSET[lastByteDCMindx] = INVALID_LAST_DCM_BYTE;
    }

    @Test
    public void recognizeMimeType_shouldRecognizeJPEGfile() throws IOException {
        //given
        byte[] jpeg = TestResourceHelper.INSTANCE.getByteResource("attachments", "sample.jpg");

        //when
        MimeType mimeType = MimeType.Companion.recognizeMimeType(jpeg);

        //then
        assertThat(mimeType).isEqualTo(MimeType.JPEG);
    }

    @Test
    public void recognizeMimeType_shouldReturnJPEG_forJPEGsignature() {
        //given

        byte[] jpeg = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0x00, 0x10, 0x4A, 0x46, 0x49, 0x46, 0x00, 0x01};

        //when
        MimeType mimeType = MimeType.Companion.recognizeMimeType(jpeg);

        //then
        assertThat(mimeType).isEqualTo(MimeType.JPEG);
        assertThat(mimeType.getContentType()).isEqualTo(IMAGE_JPG);
    }

    @Test
    public void recognizeMimeType_shouldRecognizePNGfile() throws IOException {
        //given
        byte[] png = TestResourceHelper.INSTANCE.getByteResource("attachments", "sample.png");

        //when
        MimeType mimeType = MimeType.Companion.recognizeMimeType(png);

        //then
        assertThat(mimeType).isEqualTo(MimeType.PNG);
        assertThat(mimeType.getContentType()).isEqualTo(IMAGE_PNG);
    }

    @Test
    public void recognizeMimeType_shouldReturnPNG_forPNGsignature() {
        //given
        byte[] png = {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};

        //when
        MimeType mimeType = MimeType.Companion.recognizeMimeType(png);

        //then
        assertThat(mimeType).isEqualTo(MimeType.PNG);
    }

    @Test
    public void recognizeMimeType_shouldRecognizeTIFFfile() throws IOException {
        //given
        byte[] tiff = TestResourceHelper.INSTANCE.getByteResource("attachments", "sample.tiff");

        //when
        MimeType mimeType = MimeType.Companion.recognizeMimeType(tiff);

        //then
        assertThat(mimeType).isEqualTo(MimeType.TIFF);
        assertThat(mimeType.getContentType()).isEqualTo(IMAGE_TIFF);
    }

    @Test
    public void recognizeMimeType_shouldReturnTIFF_forTIFFsignature() {
        //given
        byte[] tiff = {0x4D, 0x4D, 0x00, 0x2A};

        //when
        MimeType mimeType = MimeType.Companion.recognizeMimeType(tiff);

        //then
        assertThat(mimeType).isEqualTo(MimeType.TIFF);
        assertThat(mimeType.getContentType()).isEqualTo(IMAGE_TIFF);
    }

    @Test
    public void recognizeMimeType_shouldRecognizePDF() throws IOException {
        //given
        byte[] pdf = TestResourceHelper.INSTANCE.getByteResource("attachments", "sample.pdf");

        //when
        MimeType mimeType = MimeType.Companion.recognizeMimeType(pdf);

        //then
        assertThat(mimeType).isEqualTo(MimeType.PDF);
        assertThat(mimeType.getContentType()).isEqualTo(APPLICATION_PDF);
    }

    @Test
    public void recognizeMimeType_shouldReturnPDF_forPDFsignature() {
        //given
        byte[] pdf = {0x25, 0x50, 0x44, 0x46, 0x2d};

        //when
        MimeType mimeType = MimeType.Companion.recognizeMimeType(pdf);

        //then
        assertThat(mimeType).isEqualTo(MimeType.PDF);
        assertThat(mimeType.getContentType()).isEqualTo(APPLICATION_PDF);
    }

    @Test
    public void recognizeMimeType_shouldRecognizeDCMfile() throws IOException {
        //given
        byte[] dcm = TestResourceHelper.INSTANCE.getByteResource("attachments", "sample.dcm");

        //when
        MimeType mimeType = MimeType.Companion.recognizeMimeType(dcm);

        //then
        assertThat(mimeType).isEqualTo(MimeType.DCM);
        assertThat(mimeType.getContentType()).isEqualTo(APPLICATION_DCM);
    }

    @Test
    public void recognizeMimeType_shouldReturnDCM_forDCMsignature() throws IOException {
        //when
        MimeType mimeType = MimeType.Companion.recognizeMimeType(DCM_SIGNATURE_WITH_OFFSET);

        //then
        assertThat(mimeType).isEqualTo(MimeType.DCM);
        assertThat(mimeType.getContentType()).isEqualTo(APPLICATION_DCM);
    }

    @Test
    public void recognizeMimeType_shouldReturnUNKNOWN_forTrimmedDCMsignature() {
        //when
        MimeType mimeType = MimeType.Companion.recognizeMimeType(TRIMMED_DCM_SIG_WITH_OFFSET);

        //then
        assertThat(mimeType).isEqualTo(MimeType.UNKNOWN);
        assertThat(mimeType.getContentType()).isEqualTo("");
    }

    @Test
    public void recognizeMimeType_shouldReturnUNKNOWN_forInvalidDCMsignature() {
        //when
        MimeType mimeType = MimeType.Companion.recognizeMimeType(INVALID_DCM_SIG_WITH_OFFSET);

        //then
        assertThat(mimeType).isEqualTo(MimeType.UNKNOWN);
        assertThat(mimeType.getContentType()).isEqualTo("");
    }
}
