/*
 * Copyright (c) 2021 D4L data4life gGmbH / All rights reserved.
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
package care.data4life.sdk.network

import care.data4life.auth.AuthorizationContract
import care.data4life.sdk.NetworkConnectivityService
import care.data4life.sdk.lang.D4LException
import care.data4life.sdk.lang.D4LRuntimeException
import care.data4life.sdk.network.interceptors.VersionInterceptor
import care.data4life.sdk.network.model.CommonKeyResponse
import care.data4life.sdk.network.model.DocumentUploadResponse
import care.data4life.sdk.network.model.EncryptedRecord
import care.data4life.sdk.network.model.NetworkModelContract
import care.data4life.sdk.network.model.UserInfo
import care.data4life.sdk.network.model.VersionList
import care.data4life.sdk.network.typeadapter.EncryptedKeyTypeAdapter
import care.data4life.sdk.util.Base64.encodeToString
import com.squareup.moshi.Moshi
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import okhttp3.CertificatePinner
import okhttp3.HttpUrl
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.moshi.MoshiConverterFactory
import java.io.IOException
import java.net.SocketTimeoutException
import java.util.HashMap
import java.util.concurrent.TimeUnit

// TODO Kotlin and internal
class ApiService constructor(
    private val authService: AuthorizationContract.Service,
    private val environment: NetworkingContract.Environment,
    private val clientID: String,
    private val clientSecret: String,
    private val platform: String?,
    private val connectivityService: NetworkConnectivityService,
    private val clientName: NetworkingContract.Clients,
    private val clientVersion: String,
    staticAccessToken: ByteArray?,
    private val debug: Boolean
) : NetworkingContract.Service {
    private var service: IHCService? = null
    var client: OkHttpClient? = null
        private set
    private val staticAccessToken: String? = staticAccessToken?.let { String(it) }

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
     * @param clientName          Client version
     * @param debug               Debug flag
     */
    constructor(
        authService: AuthorizationContract.Service,
        environment: NetworkingContract.Environment,
        clientID: String,
        clientSecret: String,
        platform: String?,
        connectivityService: NetworkConnectivityService,
        clientName: NetworkingContract.Clients,
        clientVersion: String,
        debug: Boolean
    ) : this(
        authService,
        environment,
        clientID,
        clientSecret,
        platform,
        connectivityService,
        clientName,
        clientVersion,
        null,
        debug
    )

    private fun configureService() {
        val certificatePinner: CertificatePinner = CertificatePinner.Builder()
            .add(
                extractHostname(environment.getApiBaseURL(platform!!)),
                environment.getCertificatePin(
                    platform
                )
            )
            .build()
        val loggingInterceptor = HttpLoggingInterceptor()
        loggingInterceptor.setLevel(if (debug) HttpLoggingInterceptor.Level.HEADERS else HttpLoggingInterceptor.Level.NONE)

        // Pick authentication interceptor based on whether a static access token is used or not
        val authorizationInterceptor = if (staticAccessToken != null) ::staticTokenIntercept else ::intercept
        val retryInterceptor = { chain: Interceptor.Chain ->
            val request = chain.request()
            var response: Response? = null
            try {
                response = chain.proceed(request)
            } catch (exception: SocketTimeoutException) {
                if (connectivityService.isConnected) {
                    response = chain.proceed(request)
                }
            }
            response!!
        }
        client = OkHttpClient.Builder()
            .certificatePinner(certificatePinner)
            .addInterceptor(
                VersionInterceptor.getInstance(
                    Pair(clientName, clientVersion)
                )
            )
            .addInterceptor(authorizationInterceptor)
            .addInterceptor(loggingInterceptor)
            .addInterceptor(retryInterceptor)
            .connectTimeout(NetworkingContract.REQUEST_TIMEOUT, TimeUnit.MINUTES)
            .readTimeout(NetworkingContract.REQUEST_TIMEOUT, TimeUnit.MINUTES)
            .writeTimeout(NetworkingContract.REQUEST_TIMEOUT, TimeUnit.MINUTES)
            .callTimeout(NetworkingContract.REQUEST_TIMEOUT, TimeUnit.MINUTES)
            .build()
        createService(environment.getApiBaseURL(platform))
    }

    private fun extractHostname(apiBaseURL: String): String {
        return apiBaseURL.replaceFirst("https://".toRegex(), "")
    }

    private fun createService(baseUrl: String): IHCService {
        val moshi = Moshi.Builder()
            .add(EncryptedKeyTypeAdapter())
            .build()
        return Retrofit.Builder()
            .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .baseUrl(baseUrl)
            .client(client)
            .build()
            .create(IHCService::class.java).also { service = it }
    }

    override fun fetchCommonKey(
        alias: String,
        userId: String,
        commonKeyId: String
    ): Single<CommonKeyResponse> {
        return service!!.fetchCommonKey(alias, userId, commonKeyId)
    }

    // TODO: Is this method even in use?
    override fun uploadTagEncryptionKey(
        alias: String,
        userId: String,
        encryptedKey: String
    ): Completable {
        val params: MutableMap<String, String> = HashMap()
        params[NetworkingContract.PARAM_TEK] = encryptedKey
        return service!!.uploadTagEncryptionKey(alias, userId, params)
    }

    override fun createRecord(
        alias: String,
        userId: String,
        encryptedRecord: NetworkModelContract.EncryptedRecord
    ): Single<EncryptedRecord> {
        return service!!.createRecord(alias, userId, (encryptedRecord as EncryptedRecord))
    }

    override fun updateRecord(
        alias: String,
        userId: String,
        recordId: String,
        encryptedRecord: NetworkModelContract.EncryptedRecord
    ): Single<EncryptedRecord> {
        return service!!.updateRecord(alias, userId, recordId, (encryptedRecord as EncryptedRecord))
    }

    override fun fetchRecord(
        alias: String,
        userId: String,
        recordId: String
    ): Single<EncryptedRecord> {
        return service!!.fetchRecord(alias, userId, recordId)
    }

    override fun searchRecords(
        alias: String,
        userId: String,
        startDate: String?,
        endDate: String?,
        pageSize: Int,
        offset: Int,
        tags: NetworkingContract.SearchTags
    ): Observable<List<EncryptedRecord>> {
        return service!!.searchRecords(
            alias,
            userId,
            startDate,
            endDate,
            pageSize,
            offset,
            tags.tags
        )
    }

    override fun countRecord(
        alias: String,
        userId: String,
        tags: NetworkingContract.SearchTags
    ): Single<Int> {
        return service!!
            .getRecordsHeader(alias, userId, tags.tags)
            .map { response ->
                response.headers()[NetworkingContract.HEADER_TOTAL_COUNT]!!
                    .toInt()
            }
    }

    override fun deleteRecord(alias: String, userId: String, recordId: String): Completable {
        return service!!.deleteRecord(alias, userId, recordId)
    }

    override fun uploadDocument(
        alias: String,
        userId: String,
        encryptedAttachment: ByteArray
    ): Single<String> {
        return service!!.uploadDocument(
            alias, userId,
            RequestBody.create(
                NetworkingContract.MEDIA_TYPE_OCTET_STREAM.toMediaType(),
                encryptedAttachment
            )
        ).map(DocumentUploadResponse::documentId)
    }

    override fun downloadDocument(
        alias: String,
        userId: String,
        documentId: String
    ): Single<ByteArray> {
        return service!!.downloadDocument(alias, userId, documentId)
            .map { response -> response.bytes() }
    }

    override fun deleteDocument(
        alias: String,
        userId: String,
        documentId: String
    ): Single<Boolean> {
        // network request doesn't has a response except the HTTP 204
        // on success the method will always return `true`
        return service!!.deleteDocument(alias, userId, documentId).map { true }
    }

    override fun fetchUserInfo(alias: String): Single<UserInfo> {
        return service!!
            .fetchUserInfo(alias)
            .subscribeOn(Schedulers.io())
    }

    override fun fetchVersionInfo(): Single<VersionList> {
        return service!!
            .fetchVersionInfo()
            .subscribeOn(Schedulers.io())
    }

    /**
     * Carry out needed logout actions.
     *
     *
     * When using refresh token, this will revoke the OAuth access.
     * When using a static access token, nothing will be done.
     *
     * @param alias Alias
     * @return Completable
     */
    override fun logout(alias: String): Completable {
        if (staticAccessToken != null) {
            throw D4LRuntimeException("Cannot log out when using a static access token!")
        }
        return Single
            .fromCallable { authService.getRefreshToken(alias) }
            .flatMapCompletable { token: String? -> service!!.logout(alias, token!!) }
    }

    /**
     * Interceptor that attaches an authorization header to a request.
     *
     *
     * The authorization can be basic auth or OAuth. In the OAuth case, the
     * interceptor will try the request snd if it comes back with a status code
     * 401 (unauthorized), it will update the OAuth access token using the
     * refresh token.
     *
     * @param chain OkHttp interceptor chain
     * @return OkHttp response
     * @throws IOException
     */
    @Throws(IOException::class)
    private fun intercept(chain: Interceptor.Chain): Response {
        var request: Request = chain.request()
        val alias = request.header(NetworkingContract.HEADER_ALIAS)
        val authHeader = request.headers[NetworkingContract.HEADER_AUTHORIZATION]
        if (authHeader != null && authHeader == NetworkingContract.HEADER_BASIC_AUTH) {
            val auth = encodeToString("$clientID:$clientSecret")
            request = request.newBuilder()
                .removeHeader(NetworkingContract.HEADER_AUTHORIZATION)
                .addHeader(
                    NetworkingContract.HEADER_AUTHORIZATION,
                    String.format(NetworkingContract.FORMAT_BASIC_AUTH, auth)
                ).build()
        } else if (authHeader != null && authHeader == NetworkingContract.HEADER_ACCESS_TOKEN) {
            var tokenKey: String
            tokenKey = try {
                authService.getAccessToken(alias!!)
            } catch (e: D4LException) {
                return chain.proceed(request)
            }
            request = request.newBuilder()
                .removeHeader(NetworkingContract.HEADER_AUTHORIZATION)
                .removeHeader(NetworkingContract.HEADER_ALIAS)
                .addHeader(
                    NetworkingContract.HEADER_AUTHORIZATION,
                    String.format(NetworkingContract.FORMAT_BEARER_TOKEN, tokenKey)
                ).build()
            val response: Response = chain.proceed(request)
            if (response.code == NetworkingContract.HTTP_401_UNAUTHORIZED) {
                tokenKey = try {
                    authService.refreshAccessToken(alias)
                } catch (e: D4LException) {
                    authService.clear()
                    return response
                }
                request = request.newBuilder()
                    .removeHeader(NetworkingContract.HEADER_AUTHORIZATION)
                    .addHeader(
                        NetworkingContract.HEADER_AUTHORIZATION,
                        String.format(NetworkingContract.FORMAT_BEARER_TOKEN, tokenKey)
                    ).build()
                response.close()
                return chain.proceed(request)
            }
            return response
        }
        return chain.proceed(request)
    }

    /**
     * Interceptor that attaches an OAuth access token to a request.
     *
     *
     * This interceptor is used for the case where the SDK client does not
     * handle the OAuth flow itself and merely gets an access token injected.
     * Accordingly this interceptor does not attempt to refresh the access token
     * if the request should fail.
     *
     * @param chain OkHttp interceptor chain
     * @return OkHttp response
     * @throws IOException
     */
    @Throws(IOException::class)
    private fun staticTokenIntercept(chain: Interceptor.Chain): Response {
        var request: Request = chain.request()
        val alias = request.header(NetworkingContract.HEADER_ALIAS)
        val authHeader = request.headers[NetworkingContract.HEADER_AUTHORIZATION]
        request = request.newBuilder()
            .removeHeader(NetworkingContract.HEADER_AUTHORIZATION)
            .removeHeader(NetworkingContract.HEADER_ALIAS)
            .addHeader(
                NetworkingContract.HEADER_AUTHORIZATION,
                String.format(NetworkingContract.FORMAT_BEARER_TOKEN, staticAccessToken)
            ).build()
        return chain.proceed(request)
    }

    /**
     * Full constructor.
     *
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
     * @param clientVersion         Client version
     * @param staticAccessToken   Prefetched OAuth token - if not null, it will be used directly (no token renewal).
     * @param debug               Debug flag
     */
    init {
        configureService()
    }

    // TODO: This is a test concern and will be removed soon
    fun resetService(client: OkHttpClient, baseUrl: HttpUrl) {
        this.client = client
        this.service = createService(baseUrl.toString())
    }
}
