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

package care.data4life.sdk.test.util;


import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;

public class FileHelper {
    private static final String FILE_ENCODING = "UTF-8";

    private enum RETURN_TYPE {STRING, BYTE}

    public static String loadString(String fileName) throws IOException {
        return loadFile(fileName, RETURN_TYPE.STRING);
    }

    public static byte[] loadBytes(String fileName) throws IOException {
        return loadFile(fileName, RETURN_TYPE.BYTE);
    }

    private static <T> T loadFile(String filename, RETURN_TYPE returnType) throws IOException {
        InputStream inputStream = FileHelper.class.getClassLoader().getResourceAsStream(filename);
        ByteArrayOutputStream result = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int length;
        while ((length = inputStream.read(buffer)) != -1) {
            result.write(buffer, 0, length);
        }

        switch (returnType) {
            case BYTE:
                return (T) result.toByteArray();
            case STRING:
                return (T) result.toString(FILE_ENCODING);
            default:
                throw new RuntimeException("Unexpected case!");
        }
    }

    public static List<String> loadFileList(String path) throws URISyntaxException {
        URL dirUrl = FileHelper.class.getClassLoader().getResource(path);
        if (dirUrl != null && dirUrl.getProtocol().equals("file")) {
            return Arrays.asList(new File(dirUrl.toURI()).list());
        }

        throw new UnsupportedOperationException("Cannot list files for URL " + dirUrl);
    }
}
