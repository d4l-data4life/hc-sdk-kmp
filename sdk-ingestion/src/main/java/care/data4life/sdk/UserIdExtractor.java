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

import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;

import java.io.IOException;

import care.data4life.auth.AuthState;
import care.data4life.sdk.util.Base64;

/**
 * With the {@link UserIdExtractor#extract(String)} method the SDK provides the
 * functionality for extracting the user id of a given OAuth callback url.
 */
public class UserIdExtractor {


    private static final String STATE = "state";
    private static final String EQUALS = "=";
    private static final String AMPERSAND = "&";
    private static final char QUESTION_MARK = '?';


    private UrlDecoder urlDecoder;
    private Base64 base64;
    private JsonAdapter<AuthState> adapter;


    public UserIdExtractor() {
        urlDecoder = UrlDecoder.INSTANCE;
        base64 = Base64.INSTANCE;
        adapter = new Moshi.Builder().build().adapter(AuthState.class);
    }

    protected UserIdExtractor(UrlDecoder urlDecoder, Base64 base64, Moshi moshi) {
        this.urlDecoder = urlDecoder;
        this.base64 = base64;
        this.adapter = moshi.adapter(AuthState.class);
    }


    /**
     * Extract the user id from the given OAuth callback url
     *
     * @param callbackUrl the callback url from the OAuth flow with the state and code query params
     * @return the user id or null
     */
    public String extract(String callbackUrl) {
        String stateString = extractStateString(callbackUrl);

        if (stateString.isEmpty()) {
            return null;
        }

        String decodedUrl = urlDecoder.decode(stateString);
        String dataString = base64.decodeToString(decodedUrl);
        AuthState state;
        try {
            state = adapter.fromJson(dataString);
            return state.getAlias();
        } catch (IOException e) {
            // ignore
        }

        return null;
    }

    private String extractStateString(String url) {
        String query = url.substring(url.indexOf(QUESTION_MARK) + 1);
        String[] params = query.split(AMPERSAND);
        for (String keyValue : params) {
            if (keyValue.contains(STATE)) {
                return keyValue.split(EQUALS)[1];
            }
        }
        return "";
    }

}
