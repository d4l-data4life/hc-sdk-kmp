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


import java.util.Set;

import care.data4life.auth.Authorization;
import care.data4life.auth.AuthorizationConfiguration;
import care.data4life.auth.AuthorizationContract;
import care.data4life.auth.AuthorizationException;
import care.data4life.auth.AuthorizationService;
import care.data4life.auth.storage.InMemoryAuthStorage;
import care.data4life.sdk.log.Log;
import care.data4life.sdk.network.Environment;
import care.data4life.securestore.SecureStore;
import care.data4life.securestore.SecureStoreContract;
import care.data4life.securestore.SecureStoreCryptor;
import care.data4life.securestore.SecureStoreStorage;

public final class Data4LifeClient extends BaseClient {

    private static final String CLIENT_NAME = "SDK";
    private static final boolean DEBUG = true;


    private AuthorizationService authorizationService;
    private CryptoService cryptoService;


    protected Data4LifeClient(String alias,
                              AuthorizationService authorizationService,
                              CryptoService cryptoService,
                              UserService userService,
                              RecordService recordService,
                              SdkContract.ErrorHandler errorHandler) {
        super(alias, userService, recordService, errorHandler);
        this.authorizationService = authorizationService;
        this.cryptoService = cryptoService;
    }

    public static Data4LifeClient init(String alias,
                                       String clientId,
                                       String clientSecret,
                                       Environment environment,
                                       String redirectUrl,
                                       String platform) {
        return init(alias, clientId, clientSecret, environment, redirectUrl, platform, Authorization.Companion.getDefaultScopes(), new SecureStore(new SecureStoreCryptor(), new SecureStoreStorage()), new InMemoryAuthStorage());
    }

    public static Data4LifeClient init(String alias,
                                       String clientId,
                                       String clientSecret,
                                       Environment environment,
                                       String redirectUrl,
                                       String platform,
                                       Set<String> scopes) {
        return init(alias, clientId, clientSecret, environment, redirectUrl, platform, scopes, new SecureStore(new SecureStoreCryptor(), new SecureStoreStorage()), new InMemoryAuthStorage());
    }

    public static Data4LifeClient init(String alias,
                                       String clientId,
                                       String clientSecret,
                                       Environment environment,
                                       String redirectUrl,
                                       String platform,
                                       Set<String> scopes,
                                       SecureStoreContract.SecureStore secureStore,
                                       AuthorizationContract.Storage authorizationStore) {
        Log.info(String.format("Initializing SDK for alias(%s) with scopes(%s)", alias, scopes));

        AuthorizationConfiguration configuration = new AuthorizationConfiguration(
                clientId,
                clientSecret,
                environment.getApiBaseURL(platform),
                environment.getApiBaseURL(platform),
                redirectUrl,
                scopes
        );

        AuthorizationService authorizationService = new AuthorizationService(
                alias,
                configuration,
                authorizationStore
        );
        OAuthService oAuthService = new OAuthService(authorizationService);

        NetworkConnectivityService networkConnectivityService = () -> true;

        ApiService apiService = new ApiService(oAuthService, environment, clientId, clientSecret, platform, networkConnectivityService, CLIENT_NAME, DEBUG);

        CryptoSecureStore cryptoSecureStore = new CryptoSecureStore(secureStore);
        CryptoService cryptoService = new CryptoService(alias, cryptoSecureStore);
        UserService userService = new UserService(alias, oAuthService, apiService, cryptoSecureStore, cryptoService);

        TagEncryptionService tagEncryptionService = new TagEncryptionService(cryptoService);
        TaggingService taggingService = new TaggingService(clientId);
        FhirService fhirService = new FhirService(cryptoService);
        FileService fileService = new FileService(alias, apiService, cryptoService);
        AttachmentService attachmentService = new AttachmentService(alias, fileService, new JvmImageResizer());
        D4LErrorHandler errorHandler = new D4LErrorHandler();
        String partnerId = clientId.split(CLIENT_ID_SPLIT_CHAR)[PARTNER_ID_INDEX];
        RecordService recordService = new RecordService(partnerId, alias, apiService, tagEncryptionService, taggingService, fhirService, attachmentService, cryptoService, errorHandler);

        return new Data4LifeClient(alias, authorizationService, cryptoService, userService, recordService, errorHandler);
    }


    public String getAuthorizationUrl() {
        return cryptoService
                .generateGCKeyPair()
                .flatMap(gcKeyPair -> cryptoService.convertAsymmetricKeyToBase64ExchangeKey(gcKeyPair.getPublicKey()))
                .map(pubKey -> authorizationService.createAuthorizationUrl(this.alias, pubKey))
                .blockingGet();
    }

    @SuppressWarnings("ConstantConditions")
    public boolean finishLogin(String url) throws Throwable {
        boolean authorized = authorizationService.finishAuthorization(this.alias, url);

        if (!authorized) {
            throw (Throwable) new AuthorizationException.FailedToLogin();
        }

        return userService.finishLogin(authorized).blockingGet();
    }

}
