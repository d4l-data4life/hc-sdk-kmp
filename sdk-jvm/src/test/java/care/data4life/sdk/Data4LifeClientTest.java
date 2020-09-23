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
import org.junit.Rule;
import org.junit.Test;

import care.data4life.auth.AuthorizationService;
import care.data4life.crypto.GCAsymmetricKey;
import care.data4life.crypto.GCKeyPair;
import care.data4life.sdk.test.util.TestSchedulerRule;
import io.reactivex.Single;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class Data4LifeClientTest {

    private static final boolean IS_LOGGED_IN = true;
    private static final boolean IS_LOGGED_OUT = false;
    private static final String ALIAS = "alias";


    @Rule
    public TestSchedulerRule schedulerRule = new TestSchedulerRule();
    private UserService userService;
    private AuthorizationService authorizationService;
    private CryptoService cryptoService;
    private RecordService recordService;
    private Data4LifeClient instance;
    private SdkContract.ErrorHandler errorHandler;

    @Before
    public void setUp() {
        authorizationService = mock(AuthorizationService.class);
        cryptoService = mock(CryptoService.class);
        userService = mock(UserService.class);
        recordService = mock(RecordService.class);
        errorHandler = mock(SdkContract.ErrorHandler.class);


        instance = new Data4LifeClient(
                ALIAS,
                authorizationService,
                cryptoService,
                userService,
                recordService,
                errorHandler);
    }

    @Test
    public void authorizationUrl() {
        String pubKey = "Pubkey";
        String authorizationUrl = "authorizationUrl";
        GCKeyPair mockPair = mock(GCKeyPair.class);

        doReturn(mock(GCAsymmetricKey.class)).when(mockPair).getPublicKey();
        doReturn(Single.just(mockPair)).when(cryptoService).generateGCKeyPair();
        doReturn(Single.just(pubKey)).when(cryptoService).convertAsymmetricKeyToBase64ExchangeKey(any(GCAsymmetricKey.class));
        doReturn(authorizationUrl).when(authorizationService).createAuthorizationUrl(ALIAS, pubKey);


        String actualUrl = instance.getAuthorizationUrl();


        assertEquals(actualUrl, authorizationUrl);
    }

    @Test
    public void finishLogin() throws Throwable {
        String callbackUrl = "callbackUrl";
        doReturn(IS_LOGGED_IN).when(authorizationService).finishAuthorization(ALIAS, callbackUrl);
        doReturn(Single.just(IS_LOGGED_IN)).when(userService).finishLogin(IS_LOGGED_IN);

        boolean actual = instance.finishLogin(callbackUrl);

        assertTrue(actual);

        verify(authorizationService).finishAuthorization(eq(ALIAS), eq(callbackUrl));
        verify(userService).finishLogin(eq(IS_LOGGED_IN));
    }

    @Test(expected = Throwable.class)
    public void finishLoginShouldFail_whenNotLoggedIn() throws Throwable {
        String callbackUrl = "callbackUrl";
        doReturn(IS_LOGGED_OUT).when(authorizationService).finishAuthorization(ALIAS, callbackUrl);

        boolean result = instance.finishLogin(callbackUrl);

        verify(authorizationService).finishAuthorization(eq(ALIAS), eq(callbackUrl));
        assertEquals(result, IS_LOGGED_OUT);
    }
}
