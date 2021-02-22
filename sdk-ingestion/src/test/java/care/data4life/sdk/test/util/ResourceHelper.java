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

public final class ResourceHelper {
    private static final int DEFAULT_BUFFER_SIZE_BYTES = 1024;
    private static final int OFFSET_ZERO = 0;
    private static final int EOF = -1;

    private ResourceHelper() {
        //empty
    }

    public static byte[] loadBytes(String filename) throws IOException {
        InputStream is = ResourceHelper.class.getResourceAsStream(File.separator + filename);
        ByteArrayOutputStream os = new ByteArrayOutputStream();

        byte[] buffer = new byte[DEFAULT_BUFFER_SIZE_BYTES];
        int len;

        while ((len = is.read(buffer)) != EOF) {
            os.write(buffer, OFFSET_ZERO, len);
        }

        return os.toByteArray();
    }
}
