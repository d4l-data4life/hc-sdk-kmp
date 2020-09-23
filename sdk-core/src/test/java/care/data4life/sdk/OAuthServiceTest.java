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

import org.junit.Before;
import org.junit.Test;

import care.data4life.auth.AuthorizationException;
import care.data4life.auth.AuthorizationService;
import care.data4life.sdk.lang.D4LException;

import static junit.framework.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


public class OAuthServiceTest {

    private static final String ACCESS_TOKEN = "access_token";
    private static final String REFRESH_TOKEN = "refresh_token";
    private static final String ALIAS = "alias";

    private AuthorizationService mockAuthorizationService;


    // SUT
    private OAuthService oAuthService;

    @Before
    public void setUp() {
        mockAuthorizationService = mock(AuthorizationService.class);

        oAuthService = spy(new OAuthService(mockAuthorizationService));
    }

    @Test
    public void getAccessToken_shouldReturnString() throws Throwable {
        // given
        when(mockAuthorizationService.getAccessToken(ALIAS)).thenReturn(ACCESS_TOKEN);

        // when
        String accessToken = oAuthService.getAccessToken(ALIAS);

        // then
        assertEquals(ACCESS_TOKEN, accessToken);
    }

    @Test
    public void getAccessToken_shouldThrowException() {
        // given
        AuthorizationException.FailedToRestoreAccessToken exception = new AuthorizationException.FailedToRestoreAccessToken();
        when(mockAuthorizationService.getAccessToken(any())).then(invocation -> {
            throw (Throwable) exception;
        });

        // when
        try {
            oAuthService.getAccessToken(ALIAS);
        } catch (D4LException e) {
            // then
            assertEquals(AuthorizationException.FailedToRestoreAccessToken.class, e.getClass());
            assertEquals("Failed to restore access token", e.getMessage());
        }
    }

    @Test
    public void refreshAccessToken_shouldReturnString() throws Exception {
        // given
        when(mockAuthorizationService.getRefreshToken(ALIAS)).thenReturn(REFRESH_TOKEN);

        // when
        String accessToken = oAuthService.getRefreshToken(ALIAS);

        // then
        assertEquals(REFRESH_TOKEN, accessToken);
    }

    @Test
    public void clearAuthData_shouldClearAllUserData() {
        // when
        oAuthService.clearAuthData();

        // then
        verify(oAuthService, atLeastOnce()).clearAuthData();
    }
}
