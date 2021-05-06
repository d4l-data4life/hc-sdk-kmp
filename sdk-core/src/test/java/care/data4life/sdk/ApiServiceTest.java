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
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.concurrent.TimeUnit;

import care.data4life.auth.AuthorizationService;
import care.data4life.sdk.network.Environment;
import care.data4life.sdk.network.IHCService;
import care.data4life.sdk.network.model.EncryptedKey;
import care.data4life.sdk.network.model.EncryptedRecord;
import care.data4life.sdk.network.model.UserInfo;
import care.data4life.sdk.test.util.TestSchedulerRule;
import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.observers.TestObserver;
import okhttp3.OkHttpClient;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.moshi.MoshiConverterFactory;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ApiServiceTest {

    private static final String ALIAS = "ALIAS";
    private static final String PLATFORM = "d4l";

    @Rule
    public TestSchedulerRule schedulerRule = new TestSchedulerRule();
    private ApiService apiService;
    private OkHttpClient client;
    private IHCService service;
    private MockWebServer server = new MockWebServer();
    private AuthorizationService authService;
    private NetworkConnectivityService connectivityService;
    private Moshi moshi;

    @Before
    public void setUp() throws Throwable {
        authService = mock(AuthorizationService.class);
        when(authService.refreshAccessToken(ALIAS)).thenReturn("access_token");
        moshi = new Moshi.Builder().build();
        connectivityService = mock(NetworkConnectivityService.class);
        when(connectivityService.isConnected()).thenReturn(true);
        apiService = spy(new ApiService(authService, Environment.DEVELOPMENT, "", "", PLATFORM, connectivityService, "", true));
        client = apiService.getClient();
    }

    @Test
    public void fromName_shouldReturnEnvironment() {
        // when
        Environment outputNull = Environment.fromName(null);
        Environment outputDefault = Environment.fromName("");
        Environment outputDevelopment = Environment.fromName("development");
        Environment outputStaging = Environment.fromName("staging");
        Environment outputProduction = Environment.fromName("production");

        // then
        assertNotNull(outputNull);
        assertEquals(Environment.DEVELOPMENT, outputDevelopment);
        assertEquals(Environment.STAGING, outputStaging);
        assertEquals(Environment.PRODUCTION, outputProduction);
        assertEquals("https://api-phdp-dev.hpsgc.de", outputDevelopment.getApiBaseURL(PLATFORM));
        assertEquals("https://api.data4life.care", outputProduction.getApiBaseURL(PLATFORM));
        assertEquals("https://api.data4life.care", outputNull.getApiBaseURL(PLATFORM));
        assertEquals("https://api.data4life.care", outputDefault.getApiBaseURL(PLATFORM));
    }

    @Test
    @Ignore("This does not run in gradle currently")
    public void fetchToken_shouldReturnToken() {
        // given
        MockWebServer mockWebServer = new MockWebServer();
        UserInfo userInfo = new UserInfo(
                "abc",
                new EncryptedKey("add"),
                null,
                new EncryptedKey("add")
        );
        mockWebServer.enqueue(new MockResponse().setBody(moshi.adapter(UserInfo.class).toJson(userInfo)));
        service = new Retrofit.Builder()
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .addConverterFactory(MoshiConverterFactory.create())
                .baseUrl(mockWebServer.url("/userinfo/"))
                .client(client)
                .build()
                .create(IHCService.class);

        // when
        TestObserver<UserInfo> testSubscriber = service.fetchUserInfo(ALIAS)
                .test();

        // then
        testSubscriber
                .onSuccess(userInfo);
    }


    @Test
    @Ignore("This does not run in gradle currently")
    public void testTokenRefresh_shouldRefreshToken() {
        // given
        server.enqueue(new MockResponse().setBody("something as a body for the timeout")
                .setBodyDelay(100, TimeUnit.MILLISECONDS));
        client = client.newBuilder()
                .connectTimeout(10, TimeUnit.MILLISECONDS)
                .readTimeout(10, TimeUnit.MILLISECONDS)
                .writeTimeout(10, TimeUnit.MILLISECONDS)
                .build();
        service = new Retrofit.Builder()
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .addConverterFactory(MoshiConverterFactory.create())
                .baseUrl(server.url("/"))
                .client(client)
                .build()
                .create(IHCService.class);

        // when
        TestObserver<UserInfo> testSubscriber = service.fetchUserInfo(ALIAS).test();

        // then
        testSubscriber
                .assertError(throwable -> throwable.getMessage().equals("timeout") || throwable.getMessage().equals("Read timed out"));
    }

    @Test
    @Ignore("This does not run in gradle currently")
    public void testNetworkTimeoutShouldRetryRequest() throws Throwable {
        // given
        UserInfo userInfo = new UserInfo(
                "abc",
                new EncryptedKey("add"),
                null,
                new EncryptedKey("add")
        );
        server.enqueue(new MockResponse()
                .setResponseCode(401)
                .setBody(moshi.adapter(UserInfo.class).toJson(userInfo)));

        service = new Retrofit.Builder()
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .addConverterFactory(MoshiConverterFactory.create())
                .baseUrl(server.url("/"))
                .client(client)
                .build()
                .create(IHCService.class);
        // when
        service.fetchUserInfo(ALIAS)
                .test();

        // then
        verify(authService, atLeastOnce()).refreshAccessToken(ALIAS);
    }

    @Test
    public void fetchCommonKey() {
        assertThat(apiService.fetchCommonKey(any(), any(), any()),
                instanceOf(Single.class));
    }

    @Test
    public void uploadTagEncryptionKey_shouldReturnCompletable() {
        assertThat(apiService.uploadTagEncryptionKey(anyString(), anyString(), anyString()),
                instanceOf(Completable.class));
    }

    @Test
    public void createRecord_shouldReturnSingle() {
        assertThat(apiService.createRecord(eq(ALIAS), anyString(), any(EncryptedRecord.class)),
                instanceOf(Single.class));
    }

    @Test
    public void fetchRecords_shouldReturnObservable() {
        assertThat(apiService.fetchRecords(anyString(), anyString(), any(), any(), anyInt(), anyInt(), any()),
                instanceOf(Observable.class));
    }

    @Test
    public void deleteRecord_shouldReturnCompletable() {
        assertThat(apiService.deleteRecord(anyString(), anyString(), anyString()),
                instanceOf(Completable.class));
    }

    @Test
    public void fetchRecord_shouldReturnSingle() {
        assertThat(apiService.fetchRecord(anyString(), anyString(), anyString()),
                instanceOf(Single.class));
    }

    @Test
    public void updateRecord_shouldReturnSingle() {
        assertThat(apiService.updateRecord(anyString(), anyString(), anyString(), any(EncryptedRecord.class)),
                instanceOf(Single.class));
    }

    @Test
    public void getCount_shouldReturnSingle() {
        assertThat(apiService.getCount(anyString(), anyString(), any()), instanceOf(Single.class));
    }

    @Test
    public void fetchUserInfo_shouldReturnSingle() {
        assertThat(apiService.fetchUserInfo(eq(ALIAS)), instanceOf(Single.class));
    }
}
