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
package care.data4life.sdk

import care.data4life.sdk.attachment.AttachmentService
import care.data4life.sdk.attachment.FileService
import care.data4life.sdk.auth.Authorization
import care.data4life.sdk.auth.AuthorizationConfiguration
import care.data4life.sdk.auth.AuthorizationContract
import care.data4life.sdk.auth.AuthorizationException
import care.data4life.sdk.auth.AuthorizationService
import care.data4life.sdk.auth.UserService
import care.data4life.sdk.auth.storage.InMemoryAuthStorage
import care.data4life.sdk.call.CallHandler
import care.data4life.sdk.config.SDKConfig
import care.data4life.sdk.crypto.CryptoSecureStore
import care.data4life.sdk.crypto.CryptoService
import care.data4life.sdk.crypto.GCKeyPair
import care.data4life.sdk.fhir.ResourceCryptoService
import care.data4life.sdk.log.Log
import care.data4life.sdk.log.Logger
import care.data4life.sdk.network.ApiService
import care.data4life.sdk.network.Environment
import care.data4life.sdk.network.NetworkingContract
import care.data4life.sdk.securestore.SecureStore
import care.data4life.sdk.securestore.SecureStoreContract
import care.data4life.sdk.securestore.SecureStoreCryptor
import care.data4life.sdk.securestore.SecureStoreStorage
import care.data4life.sdk.tag.TagCryptoService
import care.data4life.sdk.tag.TaggingService

class Data4LifeClient(
    alias: String,
    private val authorizationService: AuthorizationService,
    private val cryptoService: CryptoService,
    userService: UserService,
    recordService: RecordService,
    callHandler: CallHandler
) : BaseClient(
    alias,
    userService,
    recordService,
    callHandler,
    createAuthClient(
        alias, userService, callHandler
    ),
    createDataClient(
        userService, recordService, callHandler
    ),
    createFhir4Client(
        userService, recordService, callHandler
    ),
    createLegacyDataClient(
        userService, recordService, callHandler
    )
) {
    val authorizationUrl: String
        get() = cryptoService
            .generateGCKeyPair()
            .flatMap { gcKeyPair: GCKeyPair ->
                cryptoService.convertAsymmetricKeyToBase64ExchangeKey(
                    gcKeyPair.publicKey!!
                )
            }
            .map { pubKey: String -> authorizationService.createAuthorizationUrl(alias, pubKey) }
            .blockingGet()

    fun finishLogin(url: String): Boolean {
        val authorized = authorizationService.finishAuthorization(alias, url)
        if (!authorized) {
            throw AuthorizationException.FailedToLogin()
        }
        return userService.finishLogin(authorized).blockingGet()
    }

    companion object {
        private const val DEBUG = true

        @JvmOverloads
        fun init(
            alias: String,
            clientId: String,
            clientSecret: String,
            environment: Environment,
            redirectUrl: String,
            platform: String,
            scopes: Set<String> = Authorization.defaultScopes,
            secureStore: SecureStoreContract.SecureStore = SecureStore(SecureStoreCryptor(), SecureStoreStorage()),
            authorizationStore: AuthorizationContract.Storage = InMemoryAuthStorage()
        ): Data4LifeClient {
            Log.info(String.format("Initializing SDK for alias(%s) with scopes(%s)", alias, scopes))

            val authConfiguration = AuthorizationConfiguration(
                clientId,
                clientSecret,
                environment.getApiBaseURL(platform),
                environment.getApiBaseURL(platform),
                redirectUrl,
                scopes
            )
            val authorizationService = AuthorizationService(
                alias,
                authConfiguration,
                authorizationStore
            )

            val networkConnectivityService = NetworkingContract.NetworkConnectivityService { true }

            val apiService = ApiService(
                authorizationService,
                environment,
                clientId,
                clientSecret,
                platform,
                networkConnectivityService,
                NetworkingContract.Client.JAVA,
                SDKConfig.version,
                null,
                DEBUG
            )

            val cryptoSecureStore = CryptoSecureStore(secureStore = secureStore)
            val cryptoService = CryptoService(alias, cryptoSecureStore)

            val userService = UserService(alias, authorizationService, apiService, cryptoSecureStore, cryptoService)

            val tagEncryptionService = TagCryptoService(cryptoService)
            val taggingService = TaggingService(clientId)

            val resourceCryptoService = ResourceCryptoService(cryptoService)
            val fileService = FileService(alias, apiService, cryptoService)
            val attachmentService = AttachmentService(fileService, JvmImageResizer())

            val errorHandler = D4LErrorHandler()
            val callHandler = CallHandler(errorHandler)

            val partnerId = clientId.split(CLIENT_ID_SPLIT_CHAR).toTypedArray()[PARTNER_ID_INDEX]

            val recordService = RecordService(
                partnerId,
                alias,
                apiService,
                tagEncryptionService,
                taggingService,
                resourceCryptoService,
                attachmentService,
                cryptoService,
                errorHandler
            )

            return Data4LifeClient(alias, authorizationService, cryptoService, userService, recordService, callHandler)
        }

        fun setLogger(logger: Logger) {
            BaseClient.setLogger(logger)
        }
    }
}
