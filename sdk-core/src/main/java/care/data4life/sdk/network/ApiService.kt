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
import care.data4life.sdk.lang.D4LRuntimeException
import care.data4life.sdk.network.NetworkingContract.Companion.MEDIA_TYPE_OCTET_STREAM
import care.data4life.sdk.network.model.CommonKeyResponse
import care.data4life.sdk.network.model.DocumentUploadResponse
import care.data4life.sdk.network.model.EncryptedRecord
import care.data4life.sdk.network.model.NetworkModelContract
import care.data4life.sdk.network.model.UserInfo
import care.data4life.sdk.network.model.VersionList
import care.data4life.sdk.network.util.ClientFactory
import care.data4life.sdk.network.util.IHCServiceFactory
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.HashMap

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
 * @param clientId            Client ID
 * @param clientSecret        Client secret
 * @param platform            Usage platform (D4L, S4H)
 * @param connectivityService Connectivity service
 * @param agent               agent name
 * @param agentVersion        agent version
 * @param staticAccessToken   optional Prefetched OAuth token - if not null, it will be used directly (no token renewal).
 * @param debug               Debug flag
 */
class ApiService @JvmOverloads constructor(
    private val authService: AuthorizationContract.Service,
    environment: NetworkingContract.Environment,
    clientId: String,
    clientSecret: String,
    platform: String,
    connectivityService: NetworkingContract.NetworkConnectivityService,
    agent: NetworkingContract.Clients,
    agentVersion: String,
    private val staticAccessToken: ByteArray? = null,
    debug: Boolean
) : NetworkingContract.Service {
    private val service = IHCServiceFactory.getInstance(
        ClientFactory.getInstance(
            authService,
            environment,
            clientId,
            clientSecret,
            platform,
            connectivityService,
            agent,
            agentVersion,
            staticAccessToken,
            debug
        ),
        platform,
        environment
    )

    override fun fetchCommonKey(
        alias: String,
        userId: String,
        commonKeyId: String
    ): Single<CommonKeyResponse> = service.fetchCommonKey(alias, userId, commonKeyId)

    // TODO: Is this method even in use?
    override fun uploadTagEncryptionKey(
        alias: String,
        userId: String,
        encryptedKey: String
    ): Completable {
        val params: MutableMap<String, String> = HashMap()
        params[NetworkingContract.PARAM_TAG_ENCRYPTION_KEY] = encryptedKey
        return service.uploadTagEncryptionKey(alias, userId, params)
    }

    override fun createRecord(
        alias: String,
        userId: String,
        encryptedRecord: NetworkModelContract.EncryptedRecord
    ): Single<EncryptedRecord> {
        return service.createRecord(alias, userId, (encryptedRecord as EncryptedRecord))
    }

    override fun updateRecord(
        alias: String,
        userId: String,
        recordId: String,
        encryptedRecord: NetworkModelContract.EncryptedRecord
    ): Single<EncryptedRecord> {
        return service.updateRecord(alias, userId, recordId, (encryptedRecord as EncryptedRecord))
    }

    override fun fetchRecord(
        alias: String,
        userId: String,
        recordId: String
    ): Single<EncryptedRecord> = service.fetchRecord(alias, userId, recordId)

    override fun searchRecords(
        alias: String,
        userId: String,
        startDate: String?,
        endDate: String?,
        pageSize: Int,
        offset: Int,
        tags: NetworkingContract.SearchTags
    ): Observable<List<EncryptedRecord>> {
        return service.searchRecords(
            alias,
            userId,
            startDate,
            endDate,
            pageSize,
            offset,
            tags.tags
        )
    }

    override fun getCount(alias: String, userId: String, tags: NetworkingContract.SearchTags): Single<Int> {
        return service
            .getRecordsHeader(alias, userId, tags.tags)
            .map { response ->
                response.headers()[NetworkingContract.HEADER_TOTAL_COUNT]!!
                    .toInt()
            }
    }

    override fun deleteRecord(
        alias: String,
        userId: String,
        recordId: String
    ): Completable = service.deleteRecord(alias, userId, recordId)

    override fun uploadDocument(
        alias: String,
        userId: String,
        encryptedAttachment: ByteArray
    ): Single<String> {
        return service.uploadDocument(
            alias,
            userId,
            encryptedAttachment.toRequestBody(MEDIA_TYPE_OCTET_STREAM.toMediaType()),
        ).map(DocumentUploadResponse::documentId)
    }

    override fun downloadDocument(
        alias: String,
        userId: String,
        documentId: String
    ): Single<ByteArray> {
        return service.downloadDocument(alias, userId, documentId)
            .map { response -> response.bytes() }
    }

    override fun deleteDocument(
        alias: String,
        userId: String,
        documentId: String
    ): Single<Boolean> {
        // network request doesn't has a response except the HTTP 204
        // on success the method will always return `true`
        return service.deleteDocument(alias, userId, documentId).map { true }
    }

    override fun fetchUserInfo(alias: String): Single<UserInfo> {
        return service
            .fetchUserInfo(alias)
            .subscribeOn(Schedulers.io())
    }

    override fun fetchVersionInfo(): Single<VersionList> {
        return service
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
        return if (staticAccessToken is ByteArray) {
            throw D4LRuntimeException("Cannot log out when using a static access token!")
        } else {
            Single
                .fromCallable { authService.getRefreshToken(alias) }
                .flatMapCompletable { token -> service.logout(alias, token) }
        }
    }
}
