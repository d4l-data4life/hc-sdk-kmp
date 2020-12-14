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

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;

import java.util.Set;

import care.data4life.auth.AuthorizationConfiguration;
import care.data4life.auth.AuthorizationService;
import care.data4life.auth.AuthorizationService.AuthorizationListener;
import care.data4life.auth.storage.SharedPrefsAuthStorage;
import care.data4life.crypto.GCKeyPair;
import care.data4life.sdk.attachment.AttachmentService;
import care.data4life.sdk.attachment.FileService;
import care.data4life.sdk.auth.UserService;
import care.data4life.sdk.call.CallHandler;
import care.data4life.sdk.fhir.FhirService;
import care.data4life.sdk.lang.CoreRuntimeException;
import care.data4life.sdk.lang.D4LException;
import care.data4life.sdk.listener.Callback;
import care.data4life.sdk.network.Environment;
import care.data4life.sdk.tag.TagEncryptionService;
import care.data4life.sdk.tag.TaggingService;
import care.data4life.securestore.SecureStore;
import care.data4life.securestore.SecureStoreCryptor;
import care.data4life.securestore.SecureStoreStorage;
import io.reactivex.schedulers.Schedulers;

public final class Data4LifeClient extends BaseClient {
    private static final String DEFAULT_ENVIRONMENT = "production";

    private static final String CLIENT_ID = "care.data4life.sdk.CLIENT_ID";
    private static final String CLIENT_SECRET = "care.data4life.sdk.CLIENT_SECRET";
    private static final String REDIRECT_URL = "care.data4life.sdk.REDIRECT_URL";
    private static final String ENVIRONMENT = "care.data4life.sdk.ENVIRONMENT";
    private static final String PLATFORM = "care.data4life.sdk.PLATFORM";
    private static final String DEBUG = "care.data4life.sdk.DEBUG";

    public static final int D4L_AUTH = 9905;
    @Deprecated
    public static final int GC_AUTH = D4L_AUTH;

    private static Data4LifeClient instance;
    private CryptoService cryptoService;
    private AuthorizationService authorizationService;

    Data4LifeClient(
            String alias,
            CryptoService cryptoService,
            AuthorizationService authorizationService,
            UserService userService,
            RecordService recordService,
            CallHandler callHandler) {

        super(
                alias,
                userService,
                recordService,
                callHandler,
                Data4LifeClient.Companion.createAuthClient(
                        alias, userService, callHandler
                ),
                Data4LifeClient.Companion.createDataClient(
                        userService, recordService, callHandler
                ),
                Data4LifeClient.Companion.createFhir4Client(
                        userService, recordService, callHandler
                ),
                Data4LifeClient.Companion.createLegacyDataClient(
                        alias, userService, recordService, callHandler
                )
        );
        this.cryptoService = cryptoService;
        this.authorizationService = authorizationService;
    }

    public static Data4LifeClient init(Context context) {
        return init(context, InitializationConfig.DEFAULT_CONFIG);
    }

    public static Data4LifeClient init(Context context, InitializationConfig config) {
        String clientId;
        String clientSecret;
        String redirectUrl;
        String platform;
        ApplicationInfo applicationInfo;

        try {
            applicationInfo = context.getPackageManager().getApplicationInfo(context.getPackageName(), PackageManager.GET_META_DATA);
        } catch (PackageManager.NameNotFoundException e) {
            throw new CoreRuntimeException.ApplicationMetadataInaccessible();
        }

        clientId = String.valueOf(applicationInfo.metaData.get(CLIENT_ID));
        clientSecret = String.valueOf(applicationInfo.metaData.get(CLIENT_SECRET));
        redirectUrl = applicationInfo.metaData.getString(REDIRECT_URL);
        platform = applicationInfo.metaData.getString(PLATFORM);

        if (clientId.equalsIgnoreCase("null") || clientSecret.equalsIgnoreCase("null") || redirectUrl.equalsIgnoreCase("null")) {
            throw new CoreRuntimeException.InvalidManifest();
        } else if (!clientId.contains("#"))
            throw new CoreRuntimeException.ClientIdMalformed();

        return instance = buildClient(context, clientId, clientSecret, redirectUrl, platform, config);
    }

    private static Data4LifeClient buildClient(
            Context context,
            String clientId,
            String clientSecret,
            String redirectUrl,
            String platform,
            InitializationConfig initConfig) {

        Environment environment;
        boolean debug;
        ApplicationInfo applicationInfo;
        try {
            applicationInfo = context.getPackageManager().getApplicationInfo(context.getPackageName(), PackageManager.GET_META_DATA);
        } catch (PackageManager.NameNotFoundException e) {
            throw new CoreRuntimeException.ApplicationMetadataInaccessible();
        }

        Object environmentValue = applicationInfo.metaData.get(ENVIRONMENT);
        environment = Environment.fromName(String.valueOf(environmentValue != null ? environmentValue : DEFAULT_ENVIRONMENT));

        if (0 != (applicationInfo.flags & ApplicationInfo.FLAG_DEBUGGABLE)) {
            debug = applicationInfo.metaData.getBoolean(DEBUG, false);
        } else {
            debug = false;
        }

        AuthorizationConfiguration authConfiguration = new AuthorizationConfiguration(
                clientId,
                clientSecret,
                environment.getApiBaseURL(platform),
                environment.getApiBaseURL(platform),
                redirectUrl,
                initConfig.getScopes()
        );

        SecureStore secureStore = new SecureStore(new SecureStoreCryptor(context), new SecureStoreStorage(context));

        AuthorizationService authorizationService = new AuthorizationService(
                context,
                authConfiguration,
                new SharedPrefsAuthStorage(secureStore)
        );

        CryptoSecureStore store = new CryptoSecureStore(secureStore);

        NetworkConnectivityService connectivityService = new NetworkConnectivityServiceAndroid(context);

        ApiService apiService = new ApiService(authorizationService, environment, clientId, clientSecret, platform, connectivityService, BuildConfig.VERSION_NAME, debug);
        CryptoService cryptoService = new CryptoService(initConfig.getAlias(), store);
        TagEncryptionService tagEncryptionService = new TagEncryptionService(cryptoService);
        //noinspection KotlinInternalInJava
        UserService userService = new UserService(initConfig.getAlias(), authorizationService, apiService, store, cryptoService);
        TaggingService taggingService = new TaggingService(clientId);
        FhirService fhirService = new FhirService(cryptoService);
        FileService fileService = new FileService(initConfig.getAlias(), apiService, cryptoService);
        AttachmentService attachmentService = new AttachmentService(fileService, new AndroidImageResizer());
        SdkContract.ErrorHandler errorHandler = new D4LErrorHandler();
        CallHandler callHandler = new CallHandler(errorHandler);
        String partnerId = clientId.split(CLIENT_ID_SPLIT_CHAR)[PARTNER_ID_INDEX];
        RecordService recordService = new RecordService(partnerId, initConfig.getAlias(), apiService, tagEncryptionService, taggingService, fhirService, attachmentService, cryptoService, errorHandler);

        return new Data4LifeClient(initConfig.getAlias(), cryptoService, authorizationService, userService, recordService, callHandler);
    }

    public static Data4LifeClient getInstance() {
        return instance;
    }

    public Intent getLoginIntent(Context context, Set<String> scopes) {
        String publicKey = cryptoService
                .generateGCKeyPair()
                .map(GCKeyPair::getPublicKey)
                .flatMap(cryptoService::convertAsymmetricKeyToBase64ExchangeKey)
                .blockingGet();

        return authorizationService.loginIntent(context, scopes, publicKey, authListener);
    }

    private AuthorizationListener authListener = new AuthorizationListener() {
        @Override
        public void onSuccess(Intent authData, AuthorizationService.Callback loginFinishedCbk) { //callback is called from the main thread
            finishLogin(authData, new Callback() {
                @Override
                public void onSuccess() {
                    loginFinishedCbk.onSuccess();
                }

                @Override
                public void onError(D4LException exception) {
                    loginFinishedCbk.onError(exception);
                }
            });
        }

        @Override
        public void onError(Throwable error, AuthorizationService.Callback callback) {
            callback.onError(error);
        }
    };

    private void finishLogin(Intent authData, Callback listener) {
        authorizationService.finishLogin(authData, new AuthorizationService.Callback() { //callback is called from the main thread
            @SuppressLint("CheckResult")
            @Override
            public void onSuccess() {
                getUserService()
                        .finishLogin(true)
                        .subscribeOn(Schedulers.io())
                        .subscribe(
                                isLoggedIn -> listener.onSuccess(),
                                error -> listener.onError(getHandler().getErrorHandler().handleError(error))
                        );
            }

            @Override
            public void onError(Throwable error) {
                listener.onError(getHandler().getErrorHandler().handleError(error));
            }
        });
    }
}
