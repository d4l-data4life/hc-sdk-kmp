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


import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import care.data4life.auth.AuthorizationConfiguration;
import care.data4life.auth.AuthorizationContract;
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

    // Fixed dummy parameters for ingestion client
    private static final String ALIAS = "broker";
    private static final String DUMMY_CLIENT_SECRET = "secret";
    private static final String DUMMY_REDIRECT_URL = "dummy";
    /* Since this client get an OAuth access token directly and never makes
    * any requests to the OAuth endpoint, we let the set of scopes be empty. */
    private static final Set<String> EMPTY_SCOPES = new HashSet<>();

    /**
     * Hidden constructor for the ingestion client.
     *
     * @param alias         Alias
     * @param userService   User service
     * @param recordService Record service
     * @param errorHandler  Error handler
     */
    protected Data4LifeClient(String alias,
                              UserService userService,
                              RecordService recordService,
                              SdkContract.ErrorHandler errorHandler) {
        super(alias, userService, recordService, errorHandler);
    }

    /**
     * Factory method for creating an ingestion SDK client instance.
     *
     * Unlike a normal SDK client, the ingestion SDK client does not handle OAuth authorization.
     * Instead, the OAuth flow must be handled by the system using the client and a valid OAuth
     * access token must be passed in. Likewise, the service must be passed the private key that
     * is to be used for the exchange of the symmetric PHDP common key.
     *
     * @param accessToken   Valid OAuth access token
     * @param capPrivateKey Private key (used for common key exchange) in PEM format (not base 64 encoded)
     * @param environment   PHPD environment to be used by SDK client
     * @param platform      Platform to be used (S4H/D4L)
     * @return SDK client instance
     */
    public static Data4LifeClient init(byte[] accessToken,
                                       byte[] capPrivateKey,
                                       Environment environment,
                                       String platform) {
        Log.info("Initializing ingestion SDK client SDK");

        // Need the client ID for tags etc.
        final AccessTokenDecoder accessTokenDecoder = new AccessTokenDecoder();
        String clientId = accessTokenDecoder.extractClientId(accessToken);

        AuthorizationConfiguration configuration = new AuthorizationConfiguration(
                clientId,
                DUMMY_CLIENT_SECRET,
                environment.getApiBaseURL(platform),
                environment.getApiBaseURL(platform),
                DUMMY_REDIRECT_URL,
                EMPTY_SCOPES
        );

        SecureStoreContract.SecureStore secureStore = new SecureStore(new SecureStoreCryptor(), new SecureStoreStorage());
        AuthorizationContract.Storage authorizationStore = new InMemoryAuthStorage();

        AuthorizationService authorizationService = new AuthorizationService(
                ALIAS,
                configuration,
                authorizationStore
        );
        OAuthService oAuthService = new OAuthService(authorizationService);

        NetworkConnectivityService networkConnectivityService = () -> true;

        // Create ApiService that uses a static token
        ApiService apiService = new ApiService(oAuthService, environment, clientId, DUMMY_CLIENT_SECRET, platform, networkConnectivityService, CLIENT_NAME, accessToken, DEBUG);

        CryptoSecureStore cryptoSecureStore = new CryptoSecureStore(secureStore);
        CryptoService cryptoService = new CryptoService(ALIAS, cryptoSecureStore);

        // Inject private key
        cryptoService.setGCKeyPairFromPemPrivateKey(new String(capPrivateKey, StandardCharsets.UTF_8));

        UserService userService = new UserService(ALIAS, oAuthService, apiService, cryptoSecureStore, cryptoService);

        TagEncryptionService tagEncryptionService = new TagEncryptionService(cryptoService);
        TaggingService taggingService = new TaggingService(clientId);
        FhirService fhirService = new FhirService(cryptoService);
        FileService fileService = new FileService(ALIAS, apiService, cryptoService);
        AttachmentService attachmentService = new AttachmentService(ALIAS, fileService, new JvmImageResizer());
        D4LErrorHandler errorHandler = new D4LErrorHandler();
        String partnerId = clientId.split(CLIENT_ID_SPLIT_CHAR)[PARTNER_ID_INDEX];
        RecordService recordService = new RecordService(partnerId, ALIAS, apiService, tagEncryptionService, taggingService, fhirService, attachmentService, cryptoService, errorHandler);

        return new Data4LifeClient(ALIAS, userService, recordService, errorHandler);
    }

    /**
     * Retrieve and store the keys necessary for encrypting data for upload to PHDP.
     * <p>
     * This is the equivalent of the method finishLogin() the standard client service. However,
     * since this use service is meant for clients that do not handle login themselves, the name
     * has been changed to better reflect the functionality.
     *
     * @return True if fetching and storing succeeded
     */
    public boolean fetchKeys() {
        return userService.finishLogin(true).blockingGet();
    }

}
