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

package care.data4life.sdk.auth;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import care.data4life.auth.AuthorizationService;
import care.data4life.crypto.GCKey;
import care.data4life.crypto.GCKeyPair;
import care.data4life.crypto.GCSymmetricKey;
import care.data4life.sdk.ApiService;
import care.data4life.sdk.CryptoSecureStore;
import care.data4life.sdk.CryptoService;
import care.data4life.sdk.lang.D4LException;
import care.data4life.sdk.network.model.EncryptedKey;
import care.data4life.sdk.network.model.UserInfo;
import care.data4life.sdk.test.util.TestSchedulerRule;
import io.reactivex.Completable;
import io.reactivex.Single;
import io.reactivex.observers.TestObserver;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

public class UserServiceTest {

    private static final String AUTH_STATE = "authState";
    private static final String USER_ALIAS = "userAlias";
    private static final String KEY_USER_ID = "user_id";
    private static final String AUTH_TOKEN = "authToken";

    @Rule
    public TestSchedulerRule schedulerRule = new TestSchedulerRule();

    private ApiService apiService;
    private CryptoSecureStore storage;
    private UserService userService;
    private CryptoService cryptoService;
    private AuthorizationService authService;

    @Before
    public void setUp() {
        authService = mock(AuthorizationService.class);
        userService = mock(UserService.class);
        storage = mock(CryptoSecureStore.class);
        apiService = mock(ApiService.class);
        cryptoService = mock(CryptoService.class);
        userService = spy(new UserService(USER_ALIAS, authService, apiService, storage, cryptoService));
    }


    @Test
    public void isLoggedIn_shouldReturnTrue() throws Exception {
        // given
        when(authService.isAuthorized(USER_ALIAS)).thenReturn(true);
        doAnswer(invocation -> "auth_state").when(storage).getSecret(AUTH_STATE);
        GCKey mockGCKey = mock(GCKey.class);
        when(mockGCKey.getSymmetricKey()).thenReturn(mock(GCSymmetricKey.class));
        when(cryptoService.fetchTagEncryptionKey()).thenReturn(mockGCKey);
        when(cryptoService.fetchCurrentCommonKey()).thenReturn(mockGCKey);

        // when
        TestObserver<Boolean> testSubscriber = userService.isLoggedIn(USER_ALIAS)
                .test();

        // then
        testSubscriber
                .assertNoErrors()
                .assertValue(true);
    }

    @Test
    public void isLoggedIn_shouldReturnFalse() throws Exception {
        // given
        doAnswer(invocation -> "auth_state").when(storage).getSecret(AUTH_STATE);
        GCKey mockGCKey = mock(GCKey.class);
        when(mockGCKey.getSymmetricKey()).thenReturn(null);
        when(cryptoService.fetchTagEncryptionKey()).thenReturn(mockGCKey);
        when(cryptoService.fetchCurrentCommonKey()).thenReturn(mockGCKey);

        // when
        TestObserver<Boolean> testSubscriber = userService.isLoggedIn(USER_ALIAS)
                .test();

        // then
        testSubscriber
                .assertNoErrors()
                .assertValue(false);
    }

    @Test
    public void getSessionToken_shouldReturnTrue() throws Exception {
        // given
        when(authService.refreshAccessToken(USER_ALIAS)).thenReturn(AUTH_TOKEN);

        // when
        TestObserver<String> testSubscriber = userService.getSessionToken(USER_ALIAS)
                .test();

        // then
        testSubscriber
                .assertNoErrors()
                .assertValue(AUTH_TOKEN);
    }

    @Test
    public void getSessionToken_shouldThrowError() throws Exception {
        // given
        when(authService.refreshAccessToken(USER_ALIAS)).thenReturn(null);

        // when
        TestObserver<String> testSubscriber = userService.getSessionToken(USER_ALIAS)
                .test()
                .await();

        // then
        testSubscriber
                .assertError(NullPointerException.class)
                .assertNotComplete();
    }

    @Test
    public void logout_shouldReturnSuccess() throws InterruptedException {
        // given
        when(apiService.logout(USER_ALIAS)).thenReturn(Completable.complete());

        // when
        TestObserver<Void> testSubscriber = userService.logout()
                .test()
                .await();

        // then
        testSubscriber
                .assertNoErrors()
                .assertComplete();

        verify(storage).clear();
    }

    @Test
    public void logout_shouldReturnException() throws InterruptedException {
        // given
        when(apiService.logout(USER_ALIAS)).thenReturn(Completable.error(new Exception()));

        // when
        TestObserver<Void> testSubscriber = userService.logout()
                .test()
                .await();

        // then
        testSubscriber
                .assertError(Exception.class)
                .assertNotComplete();

        verifyZeroInteractions(storage);
    }

    @Test
    public void finishLogin_shouldReturnBooleanSuccess() {
        // given
        UserInfo userInfo = mock(UserInfo.class);
        when(userInfo.getCommonKey()).thenReturn(mock(EncryptedKey.class));
        when(userInfo.getCommonKeyId()).thenReturn("mockedCommonKeyId");
        when(userInfo.getTagEncryptionKey()).thenReturn(mock(EncryptedKey.class));
        when(userInfo.getUid()).thenReturn("");

        when(apiService.fetchUserInfo(USER_ALIAS)).thenReturn(Single.just(userInfo));

        when(cryptoService.fetchGCKeyPair()).thenReturn(Single.just(mock(GCKeyPair.class)));
        when(cryptoService.asymDecryptSymetricKey(any(), any())).thenReturn(Single.just(mock(GCKey.class)));
        when(cryptoService.symDecryptSymmetricKey(any(), any())).thenReturn(Single.just(mock(GCKey.class)));

        // when
        TestObserver<Boolean> testObserver = userService.finishLogin(true).test();

        // then
        testObserver
                .assertValue(aBoolean -> aBoolean)
                .assertComplete();
    }

    @Test
    public void getUID_shouldReturnString() throws D4LException {
        // given
        String uid = "mock-uid";
        when(storage.getSecret(USER_ALIAS + "_" + KEY_USER_ID, String.class)).thenReturn(uid);

        // when
        TestObserver<String> testObserver = userService.getUID().test();

        // then
        testObserver
                .assertNoErrors()
                .assertValue(uid);
    }
}
