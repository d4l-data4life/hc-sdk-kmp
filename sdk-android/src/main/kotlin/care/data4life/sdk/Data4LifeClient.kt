/*
 * Copyright (c) 2022 D4L data4life gGmbH - All rights reserved.
 */

package care.data4life.sdk

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import care.data4life.sdk.attachment.AttachmentService
import care.data4life.sdk.attachment.FileService
import care.data4life.sdk.auth.AuthorizationConfiguration
import care.data4life.sdk.auth.AuthorizationService
import care.data4life.sdk.auth.UserService
import care.data4life.sdk.auth.storage.SharedPrefsAuthStorage
import care.data4life.sdk.call.CallHandler
import care.data4life.sdk.config.SDKConfig
import care.data4life.sdk.crypto.CryptoSecureStore
import care.data4life.sdk.crypto.CryptoService
import care.data4life.sdk.crypto.GCKeyPair
import care.data4life.sdk.fhir.ResourceCryptoService
import care.data4life.sdk.lang.CoreRuntimeException
import care.data4life.sdk.listener.Callback
import care.data4life.sdk.network.ApiService
import care.data4life.sdk.network.Environment
import care.data4life.sdk.network.NetworkingContract
import care.data4life.sdk.securestore.SecureStore
import care.data4life.sdk.securestore.SecureStoreCryptor
import care.data4life.sdk.securestore.SecureStoreStorage
import care.data4life.sdk.tag.TagCryptoService
import care.data4life.sdk.tag.TaggingService
import io.reactivex.schedulers.Schedulers

class Data4LifeClient private constructor(
    alias: String,
    private val cryptoService: CryptoService,
    private val authorizationService: AuthorizationService,
    userService: UserService,
    recordService: RecordService,
    private val callHandler: CallHandler
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

    fun getLoginIntent(scopes: Set<String>?): Intent {
        val publicKey = cryptoService
            .generateGCKeyPair()
            .map(GCKeyPair::publicKey)
            .flatMap(cryptoService::convertAsymmetricKeyToBase64ExchangeKey)
            .blockingGet()

        return authorizationService.loginIntent(scopes, publicKey)
    }

    fun finishLogin(authData: Intent, callback: Callback) {
        authorizationService.finishLogin(
            authData,
            object : AuthorizationService.Callback {
                override fun onSuccess() {
                    userService.finishLogin(true)
                        .subscribeOn(Schedulers.io())
                        .subscribe(
                            { isLoggedIn: Boolean? -> callback.onSuccess() },
                            { error -> callback.onError(callHandler.errorHandler.handleError(error)) }
                        )
                }

                override fun onError(error: Throwable) {
                    callback.onError(callHandler.errorHandler.handleError(error))
                }
            }
        )
    }

    companion object {
        private const val DEFAULT_ENVIRONMENT = "production"

        private const val CLIENT_ID = "care.data4life.sdk.CLIENT_ID"
        private const val CLIENT_SECRET = "care.data4life.sdk.CLIENT_SECRET"
        private const val REDIRECT_URL = "care.data4life.sdk.REDIRECT_URL"
        private const val ENVIRONMENT = "care.data4life.sdk.ENVIRONMENT"
        private const val PLATFORM = "care.data4life.sdk.PLATFORM"
        private const val DEBUG = "care.data4life.sdk.DEBUG"

        private const val UNKNOWN = "unknown"

        private lateinit var INSTANCE: Data4LifeClient

        const val D4L_AUTH = 9905

        fun getInstance(): Data4LifeClient {
            return INSTANCE
        }

        fun init(context: Context): Data4LifeClient {
            return init(context, InitializationConfig.DEFAULT_CONFIG)
        }

        fun init(context: Context, initConfig: InitializationConfig): Data4LifeClient {
            INSTANCE = createClient(
                context,
                loadSdkConfig(context),
                initConfig
            )

            return INSTANCE
        }

        private fun createClient(
            context: Context,
            sdkConfig: SdkConfig,
            initConfig: InitializationConfig
        ): Data4LifeClient {
            val authConfiguration = AuthorizationConfiguration(
                sdkConfig.clientId,
                sdkConfig.clientSecret,
                sdkConfig.environment.getApiBaseURL(sdkConfig.platform),
                sdkConfig.environment.getApiBaseURL(sdkConfig.platform),
                sdkConfig.redirectUrl,
                initConfig.scopes
            )

            val secureStore = SecureStore(SecureStoreCryptor(context), SecureStoreStorage(context))
            val authStore = SharedPrefsAuthStorage(secureStore)
            val authService = AuthorizationService(context, authConfiguration, authStore)
            val cryptoStore = CryptoSecureStore(secureStore = secureStore)
            val cryptoService = CryptoService(initConfig.alias, cryptoStore)
            val tagCryptoService = TagCryptoService(cryptoService)
            val connectivityService = NetworkConnectivityServiceAndroid(context)
            val apiService = ApiService(
                authService = authService,
                environment = sdkConfig.environment,
                clientId = sdkConfig.clientId,
                clientSecret = sdkConfig.clientSecret,
                platform = sdkConfig.platform,
                connectivityService = connectivityService,
                agent = NetworkingContract.Clients.ANDROID,
                agentVersion = SDKConfig.version,
                debug = sdkConfig.debug
            )

            val userService = UserService(initConfig.alias, authService, apiService, cryptoStore, cryptoService)
            val taggingService = TaggingService(sdkConfig.clientId)
            val resourceCryptoService = ResourceCryptoService(cryptoService)
            val fileService = FileService(initConfig.alias, apiService, cryptoService)
            val attachmentService = AttachmentService(fileService, AndroidImageResizer())
            val errorHandler = D4LErrorHandler()
            val callHandler = CallHandler(errorHandler)
            val recordService = RecordService(
                sdkConfig.partnerId,
                initConfig.alias,
                apiService,
                tagCryptoService,
                taggingService,
                resourceCryptoService,
                attachmentService,
                cryptoService,
                errorHandler
            )

            return Data4LifeClient(
                initConfig.alias,
                cryptoService,
                authService,
                userService,
                recordService,
                callHandler
            )
        }

        private fun loadSdkConfig(context: Context): SdkConfig {
            val applicationInfo = try {
                context.packageManager.getApplicationInfo(context.packageName, PackageManager.GET_META_DATA)
            } catch (error: PackageManager.NameNotFoundException) {
                throw CoreRuntimeException.ApplicationMetadataInaccessible()
            }

            val platform = applicationInfo.metaData.getString(PLATFORM, UNKNOWN)
            if (platform == UNKNOWN) throw CoreRuntimeException.InvalidManifest()

            val environment = Environment.fromName(applicationInfo.metaData.getString(ENVIRONMENT, DEFAULT_ENVIRONMENT))

            val clientId = applicationInfo.metaData.getString(CLIENT_ID, UNKNOWN)
            if (clientId == UNKNOWN) throw CoreRuntimeException.InvalidManifest()
            else if (!clientId.contains("#")) throw CoreRuntimeException.ClientIdMalformed()

            val partnerId = clientId.split(CLIENT_ID_SPLIT_CHAR).toTypedArray()[PARTNER_ID_INDEX]

            val clientSecret = applicationInfo.metaData.getString(CLIENT_SECRET, UNKNOWN)
            if (clientSecret == UNKNOWN) throw CoreRuntimeException.InvalidManifest()

            val redirectUrl = applicationInfo.metaData.getString(REDIRECT_URL, UNKNOWN)
            if (redirectUrl == UNKNOWN) throw CoreRuntimeException.InvalidManifest()

            val debug = if (0 != applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) {
                applicationInfo.metaData.getBoolean(DEBUG, false)
            } else {
                false
            }

            return SdkConfig(
                platform = platform,
                environment = environment,
                partnerId = partnerId,
                clientId = clientId,
                clientSecret = clientSecret,
                redirectUrl = redirectUrl,
                debug = debug
            )
        }
    }
}
