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

import com.squareup.moshi.Moshi;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import care.data4life.sdk.util.Base64;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.doReturn;
import static org.mockito.MockitoAnnotations.initMocks;

public class UserIdExtractorTest {

    private static final String CALLBACK_URL = "http://localhost:8888/icarus/gccallback?code=kC_CQ_4NR4m9kCdQJsfC_g&state=eyJhbGlhcyI6ImV4cGVjdGVkVXNlcklkIiwic2VjcmV0Ijoie1wiYWxpYXNcIjpcImFsaWFzXCIsXCJzZWNyZXRcIjpcIjE2MjkyMDU1XCJ9In0%3D";
    private static final String ENCODED_STATE = "eyJhbGlhcyI6ImV4cGVjdGVkVXNlcklkIiwic2VjcmV0Ijoie1wiYWxpYXNcIjpcImFsaWFzXCIsXCJzZWNyZXRcIjpcIjE2MjkyMDU1XCJ9In0%3D";
    private static final String DECODED_URL = "eyJhbGlhcyI6ImV4cGVjdGVkVXNlcklkIiwic2VjcmV0Ijoie1wiYWxpYXNcIjpcImFsaWFzXCIsXCJzZWNyZXRcIjpcIjE2MjkyMDU1XCJ9In0=";
    private static final String INVALID_JSON_CALLBACK_URL = "http://localhost:8888/icarus/gccallback?code=kC_CQ_4NR4m9kCdQJsfC_g&state=eyJzb21ldGhpbmciOyJmYWlsdXJlIn0%3D";
    private static final String EXPECTED_USER_ID = "expectedUserId";
    private static final String EMPTY_STRING = "EMPTY_STRING";
    private static final String STATE_WITH_INVALID_JSON = "eyJzb21ldGhpbmciOyJmYWlsdXJlIn0=";
    private static final String STATE_WITH_INVALID_JSON_URL_ENCODED = "eyJzb21ldGhpbmciOyJmYWlsdXJlIn0%3D";


    @Mock private UrlDecoder mockUrlDecoder;


    private UserIdExtractor extractor;


    @Before
    public void setUp() {
        initMocks(this);
        extractor = new UserIdExtractor(mockUrlDecoder, Base64.INSTANCE, new Moshi.Builder().build());

    }

    @Test
    public void extractFromCallbackUrl() {
        doReturn(DECODED_URL).when(mockUrlDecoder).decode(ENCODED_STATE);

        String actual = extractor.extract(CALLBACK_URL);

        assertEquals(EXPECTED_USER_ID, actual);
    }

    @Test
    public void extractWithoutStatePresentShouldReturnNull() {
        String actual = extractor.extract(EMPTY_STRING);

        assertNull(actual);
    }

    @Test
    public void extractFromInvalidJsonState() {
        doReturn(STATE_WITH_INVALID_JSON).when(mockUrlDecoder).decode(STATE_WITH_INVALID_JSON_URL_ENCODED);

        String actual = extractor.extract(INVALID_JSON_CALLBACK_URL);

        assertNull(actual);
    }


}
