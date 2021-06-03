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

import android.content.Intent;

import org.junit.Before;
import org.junit.Test;

import java.util.HashSet;
import java.util.Set;

import care.data4life.auth.AuthorizationService;
import care.data4life.auth.AuthorizationService.AuthorizationListener;
import care.data4life.crypto.GCAsymmetricKey;
import care.data4life.crypto.GCKeyPair;
import care.data4life.sdk.auth.UserService;
import care.data4life.sdk.call.CallHandler;
import care.data4life.sdk.crypto.CryptoService;
import io.reactivex.Single;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

public class Data4LifeClientTest {
    private static final boolean IS_LOGGED_IN = true;
    private static final String ALIAS = "alias";

    private UserService userService;
    private Data4LifeClient instance;
    private AuthorizationService authorizationService;
    private CryptoService cryptoService;
    private RecordService recordService;
    private CallHandler callHandler;

    @Before
    public void setUp() {
        authorizationService = mock(AuthorizationService.class);
        cryptoService = mock(CryptoService.class);
        userService = mock(UserService.class);
        recordService = mock(RecordService.class);
        callHandler = mock(CallHandler.class);

        when(userService.getUserID()).thenReturn(Single.just("uid"));

        instance = spy(new Data4LifeClient(ALIAS, cryptoService, authorizationService, userService, recordService, callHandler));
    }

    @Test
    public void hasUserId() {
        doReturn(Single.just(IS_LOGGED_IN)).when(userService).finishLogin(IS_LOGGED_IN);
        assertThat(instance.getUserId()).isEqualTo("uid");
    }

    @Test
    public void authorizationIntent_withScopes() {
        //Given
        String pubKey = "Pubkey";
        Set<String> scopes = new HashSet<>();
        scopes.add("scope");
        GCKeyPair mockPair = mock(GCKeyPair.class);

        doReturn(mock(GCAsymmetricKey.class)).when(mockPair).getPublicKey();
        doReturn(Single.just(mockPair)).when(cryptoService).generateGCKeyPair();
        doReturn(Single.just(pubKey)).when(cryptoService).convertAsymmetricKeyToBase64ExchangeKey(any(GCAsymmetricKey.class));
        Intent mockIntent = mock(Intent.class);
        when(authorizationService.loginIntent(eq(null), eq(scopes), eq(pubKey), any(AuthorizationListener.class))).thenReturn(mockIntent);

        //When
        Intent loginIntent = instance.getLoginIntent(null, scopes);

        //Then
        assertThat(loginIntent).isEqualTo(mockIntent);
    }
}
