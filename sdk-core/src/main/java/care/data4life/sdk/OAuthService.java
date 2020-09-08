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

import care.data4life.auth.AuthorizationService;
import care.data4life.sdk.lang.D4LException;

final class OAuthService {

    private AuthorizationService authorizationService;


    OAuthService(AuthorizationService authorizationService) {
        this.authorizationService = authorizationService;
    }


    String getAccessToken(String alias) throws D4LException {
        return authorizationService.getAccessToken(alias);
    }

    String getRefreshToken(String alias) throws D4LException {
        return authorizationService.getRefreshToken(alias);
    }

    String refreshAccessToken(String alias) throws D4LException {
        return authorizationService.refreshAccessToken(alias);
    }

    void clearAuthData() {
        authorizationService.clear();
    }

    boolean isAuthorized(String alias) {
        return authorizationService.isAuthorized(alias);
    }
}
