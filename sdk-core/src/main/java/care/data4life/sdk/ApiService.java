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

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import care.data4life.auth.AuthorizationService;
import care.data4life.sdk.lang.D4LException;
import care.data4life.sdk.lang.D4LRuntimeException;
import care.data4life.sdk.network.Environment;
import care.data4life.sdk.network.IHCService;
import care.data4life.sdk.network.model.CommonKeyResponse;
import care.data4life.sdk.network.model.DocumentUploadResponse;
import care.data4life.sdk.network.model.EncryptedRecord;
import care.data4life.sdk.network.model.UserInfo;
import care.data4life.sdk.network.typeadapter.EncryptedKeyTypeAdapter;
import care.data4life.sdk.util.Base64;
import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;
import okhttp3.CertificatePinner;
import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.moshi.MoshiConverterFactory;

// TODO Kotlin and internal
public final class ApiService {

    private static final String HEADER_ALIAS = "gc_alias";
    private static final String HEADER_ACCESS_TOKEN = "access_token";
    private static final String HEADER_AUTHORIZATION = "Authorization";
    private static final String HEADER_BASIC_AUTH = "basic_auth";
    private static final String HEADER_GC_SDK_VERSION = "GC-SDK-Version";
    private static final String HEADER_TOTAL_COUNT = "x-total-count";
    private static final String PARAM_FILE_NUMBER = "file_number";
    private static final String PARAM_TEK = "tek";
    private static final String FORMAT_BEARER_TOKEN = "Bearer %s";
    private static final String FORMAT_BASIC_AUTH = "Basic %s";
    private static final String FORMAT_ANDROID_CLIENT_NAME = "Android %s";
    private static final String MEDIA_TYPE_OCTET_STREAM = "application/octet-stream";
    private static final int HTTP_401_UNAUTHORIZED = 401;

    private IHCService service;


    private final String clientID;
    private final String clientSecret;
    private final AuthorizationService authService;
    private final Environment environment;
    private String platform;
    private final NetworkConnectivityService connectivityService;
    private OkHttpClient client;
    private final String clientName;
    private final boolean debug;
    private final String staticAccessToken;

    /**
     * Full constructor.
     *
     * If the staticToken parameter is set to null, the SDK will handle the full OAuth flow
     * and fetch  access and retrieval tokens, renewing the former as needed for later
     * requests.
     * If the a non-null staticToken is passed, the SDK will use this value as an access token.
     * In this case, it will not dynamical fetch or renew tokens.
     *
     * @param authService         AuthorizationService
     * @param environment         Deployment environment
     * @param clientID            Client ID
     * @param clientSecret        Client secret
     * @param platform            Usage platform (D4L, S4H)
     * @param connectivityService Connectivity service
     * @param clientName          Client name
     * @param staticAccessToken   Prefetched OAuth token - if not null, it will be used directly (no token renewal).
     * @param debug               Debug flag
     */
    ApiService(AuthorizationService authService,
               Environment environment,
               String clientID,
               String clientSecret,
               String platform,
               NetworkConnectivityService connectivityService,
               String clientName,
               byte[] staticAccessToken,
               boolean debug) {
        this.authService = authService;
        this.environment = environment;
        this.clientID = clientID;
        this.clientSecret = clientSecret;
        this.platform = platform;
        this.connectivityService = connectivityService;
        this.clientName = clientName;
        this.debug = debug;
        this.staticAccessToken = staticAccessToken == null ? null : new String(staticAccessToken);
        configureService();
    }

    /**
     * Convenience constructor for instances that handle the OAuth flow themselves.
     *
     * @param authService         AuthorizationService
     * @param environment         Deployment environment
     * @param clientID            Client ID
     * @param clientSecret        Client secret
     * @param platform            Usage platform (D4L, S4H)
     * @param connectivityService Connectivity service
     * @param clientName          Client name
     * @param debug               Debug flag
     */
    ApiService(AuthorizationService authService,
               Environment environment,
               String clientID,
               String clientSecret,
               String platform,
               NetworkConnectivityService connectivityService,
               String clientName,
               boolean debug) {
        this(authService,
                environment,
                clientID,
                clientSecret,
                platform,
                connectivityService,
                clientName,
                null,
                debug);
    }

    private void configureService() {
        CertificatePinner certificatePinner = new CertificatePinner.Builder()
                .add(extractHostname(environment.getApiBaseURL(platform)), environment.getCertificatePin(platform))
                .build();

        HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
        loggingInterceptor.setLevel(debug ? HttpLoggingInterceptor.Level.HEADERS : HttpLoggingInterceptor.Level.NONE);

        Interceptor versionInterceptor = chain -> {
            Request request = chain.request();
            request = request.newBuilder()
                    .addHeader(HEADER_GC_SDK_VERSION, String.format(FORMAT_ANDROID_CLIENT_NAME, clientName))
                    .build();
            return chain.proceed(request);
        };

        // Pick authentication interceptor based on whether a static access token is used or not
        Interceptor authorizationInterceptor = this.staticAccessToken != null ? this::staticTokenIntercept : this::intercept;

        Interceptor retryInterceptor = chain -> {
            Request request = chain.request();
            Response response = null;
            try {
                response = chain.proceed(request);
            } catch (SocketTimeoutException exception) {
                if (connectivityService.isConnected()) {
                    response = chain.proceed(request);
                }
            }
            return response;
        };

        client = new OkHttpClient.Builder()
                .certificatePinner(certificatePinner)
                .addInterceptor(versionInterceptor)
                .addInterceptor(authorizationInterceptor)
                .addInterceptor(loggingInterceptor)
                .addInterceptor(retryInterceptor)
                .connectTimeout(2, TimeUnit.MINUTES)
                .readTimeout(2, TimeUnit.MINUTES)
                .writeTimeout(2, TimeUnit.MINUTES)
                .callTimeout(2, TimeUnit.MINUTES)
                .build();

        createService(environment.getApiBaseURL(platform));
    }

    private String extractHostname(String apiBaseURL) {
        return apiBaseURL.replaceFirst("https://", "");
    }

    OkHttpClient getClient() {
        return client;
    }

    private IHCService createService(String baseUrl) {

        Moshi moshi = new Moshi.Builder()
                .add(new EncryptedKeyTypeAdapter())
                .build();

        return service = new Retrofit.Builder()
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .addConverterFactory(MoshiConverterFactory.create(moshi))
                .baseUrl(baseUrl)
                .client(client)
                .build()
                .create(IHCService.class);

    }

    public Single<CommonKeyResponse> fetchCommonKey(String alias, String userId, String commonKeyId) {
        return service.fetchCommonKey(alias, userId, commonKeyId);
    }

    Completable uploadTagEncryptionKey(String alias, String userId, String encryptedKey) {
        Map<String, String> params = new HashMap<>();
        params.put(PARAM_TEK, encryptedKey);
        return service.uploadTagEncryptionKey(alias, userId, params);
    }

    Single<EncryptedRecord> createRecord(String alias, String userId, EncryptedRecord encryptedRecord) {
        return service.createRecord(alias, userId, encryptedRecord);
    }

    Observable<List<EncryptedRecord>> fetchRecords(String alias,
                                                   String userId,
                                                   String startDate,
                                                   String endDate,
                                                   Integer pageSize,
                                                   Integer offset,
                                                   List<String> tags) {
        return service.searchRecords(alias, userId, startDate, endDate, pageSize, offset, tags);
    }

    Completable deleteRecord(String alias, String recordId, String userId) {
        return service.deleteRecord(alias, userId, recordId);
    }

    Single<EncryptedRecord> fetchRecord(String alias, String userId, String recordId) {
        return service.fetchRecord(alias, userId, recordId);
    }

    Single<EncryptedRecord> updateRecord(String alias,
                                         String userId,
                                         String recordId,
                                         EncryptedRecord encryptedRecord) {
        return service.updateRecord(alias, userId, recordId, encryptedRecord);
    }

    // TODO remove public
    public Single<String> uploadDocument(String alias,
                                  String userId,
                                  byte[] encryptedAttachment) {
        return service.uploadDocument(
                alias, userId,
                RequestBody.create(MediaType.parse(MEDIA_TYPE_OCTET_STREAM), encryptedAttachment)
        ).map(DocumentUploadResponse::getDocumentId);
    }

    // TODO remove public
    public Single<byte[]> downloadDocument(String alias,
                                    String userId,
                                    String documentId) {
        return service.downloadDocument(alias, userId, documentId)
                .map(ResponseBody::bytes);
    }

    // TODO remove public
    public Single<Boolean> deleteDocument(String alias,
                                   String userId,
                                   String documentId) {
        // network request doesn't has a response except the HTTP 204
        // on success the method will always return `true`
        return service.deleteDocument(alias, userId, documentId).map(it -> true);
    }

    Single<Integer> getCount(String alias, String userId, List<String> tags) {
        return service
                .getRecordsHeader(alias, userId, tags)
                .map(response -> Integer.parseInt(response.headers().get(HEADER_TOTAL_COUNT)));
    }

    public Single<UserInfo> fetchUserInfo(String alias) {
        return service
                .fetchUserInfo(alias)
                .subscribeOn(Schedulers.io());
    }

    /**
     * Carry out needed logout actions.
     * <p>
     * When using refresh token, this will revoke the OAuth access.
     * When using a static access token, nothing will be done.
     *
     * @param alias Alias
     * @return Completable
     */
    public Completable logout(String alias) {
        if (this.staticAccessToken != null) {
            throw new D4LRuntimeException("Cannot log out when using a static access token!");
        }
        return Single
                .fromCallable(() -> authService.getRefreshToken(alias))
                .flatMapCompletable(token -> service.logout(alias, token));
    }

    /**
     * Interceptor that attaches an authorization header to a request.
     * <p>
     * The authorization can be basic auth or OAuth. In the OAuth case, the
     * interceptor will try the request snd if it comes back with a status code
     * 401 (unauthorized), it will update the OAuth access token using the
     * refresh token.
     *
     * @param chain OkHttp interceptor chain
     * @return OkHttp response
     * @throws IOException
     */
    private Response intercept(Interceptor.Chain chain) throws IOException {
        Request request = chain.request();
        String alias = request.header(HEADER_ALIAS);
        String authHeader = request.headers().get(HEADER_AUTHORIZATION);
        if (authHeader != null && authHeader.equals(HEADER_BASIC_AUTH)) {
            String auth = Base64.INSTANCE.encodeToString(clientID + ":" + clientSecret);
            request = request.newBuilder()
                    .removeHeader(HEADER_AUTHORIZATION)
                    .addHeader(HEADER_AUTHORIZATION, String.format(FORMAT_BASIC_AUTH, auth)).build();
        } else if (authHeader != null && authHeader.equals(HEADER_ACCESS_TOKEN)) {
            String tokenKey;
            try {
                tokenKey = authService.getAccessToken(alias);
            } catch (D4LException e) {
                return chain.proceed(request);
            }
            request = request.newBuilder()
                    .removeHeader(HEADER_AUTHORIZATION)
                    .removeHeader(HEADER_ALIAS)
                    .addHeader(HEADER_AUTHORIZATION, String.format(FORMAT_BEARER_TOKEN, tokenKey)).build();
            Response response = chain.proceed(request);
            if (response.code() == HTTP_401_UNAUTHORIZED) {
                try {
                    tokenKey = authService.refreshAccessToken(alias);
                } catch (D4LException e) {
                    authService.clear();
                    return response;
                }
                request = request.newBuilder()
                        .removeHeader(HEADER_AUTHORIZATION)
                        .addHeader(HEADER_AUTHORIZATION, String.format(FORMAT_BEARER_TOKEN, tokenKey)).build();
                response.close();
                return chain.proceed(request);
            }
            return response;
        }
        return chain.proceed(request);
    }

    /**
     * Interceptor that attaches an OAuth access token to a request.
     * <p>
     * This interceptor is used for the case where the SDK client does not
     * handle the OAuth flow itself and merely gets an access token injected.
     * Accordingly this interceptor does not attempt to refresh the access token
     * if the request should fail.
     *
     * @param chain OkHttp interceptor chain
     * @return OkHttp response
     * @throws IOException
     */
    private Response staticTokenIntercept(Interceptor.Chain chain) throws IOException {
        Request request = chain.request();
        String alias = request.header(HEADER_ALIAS);
        String authHeader = request.headers().get(HEADER_AUTHORIZATION);
        request = request.newBuilder()
                .removeHeader(HEADER_AUTHORIZATION)
                .removeHeader(HEADER_ALIAS)
                .addHeader(HEADER_AUTHORIZATION, String.format(FORMAT_BEARER_TOKEN, this.staticAccessToken)).build();
        return chain.proceed(request);
    }
}


