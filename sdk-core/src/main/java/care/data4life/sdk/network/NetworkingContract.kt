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
import care.data4life.sdk.network.model.CommonKeyResponse
import care.data4life.sdk.network.model.EncryptedRecord
import care.data4life.sdk.network.model.NetworkModelContract
import care.data4life.sdk.network.model.UserInfo
import care.data4life.sdk.network.model.VersionList
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import okhttp3.CertificatePinner
import okhttp3.OkHttpClient
import okhttp3.Response

class NetworkingContract {
    // TODO: Break this down
    interface Service {
        // TODO: move into a key route
        fun fetchCommonKey(alias: String, userId: String, commonKeyId: String): Single<CommonKeyResponse>

        fun uploadTagEncryptionKey(alias: String, userId: String, encryptedKey: String): Completable

        // TODO: move record route
        fun createRecord(
            alias: String,
            userId: String,
            encryptedRecord: NetworkModelContract.EncryptedRecord
        ): Single<EncryptedRecord>

        fun updateRecord(
            alias: String,
            userId: String,
            recordId: String,
            encryptedRecord: NetworkModelContract.EncryptedRecord
        ): Single<EncryptedRecord >

        fun fetchRecord(alias: String, userId: String, recordId: String): Single<EncryptedRecord>

        fun searchRecords(
            alias: String,
            userId: String,
            startDate: String?,
            endDate: String?,
            pageSize: Int,
            offset: Int,
            tags: SearchTags
        ): Observable<List<EncryptedRecord>>

        fun getCount(alias: String, userId: String, tags: SearchTags): Single<Int>

        fun deleteRecord(alias: String, userId: String, recordId: String): Completable

        // TODO: move into a Attachment route
        fun uploadDocument(
            alias: String,
            userId: String,
            encryptedAttachment: ByteArray
        ): Single<String>

        fun downloadDocument(alias: String, userId: String, documentId: String): Single<ByteArray>

        fun deleteDocument(alias: String, userId: String, documentId: String): Single<Boolean>

        // TODO: Move into a user/utils route
        fun fetchUserInfo(alias: String): Single<UserInfo>

        fun logout(alias: String): Completable

        fun fetchVersionInfo(): Single<VersionList>
    }

    internal interface CertificatePinnerFactory {
        fun getInstance(platform: String, env: Environment): CertificatePinner
    }

    internal interface Interceptor : okhttp3.Interceptor {
        override fun intercept(chain: okhttp3.Interceptor.Chain): Response
    }

    internal interface PartialInterceptor<T : Any> {
        fun intercept(
            payload: T,
            chain: okhttp3.Interceptor.Chain
        ): Response
    }

    internal interface InterceptorFactory<T : Any> {
        fun getInstance(payload: T): Interceptor
    }

    enum class Clients(val identifier: String) {
        ANDROID("android"),
        JAVA("jvm"),
        INGESTION("ingestion")
    }

    interface NetworkConnectivityService {
        val isConnected: Boolean
    }

    internal enum class Data4LifeURI(val uri: String) {
        SANDBOX("https://api-phdp-sandbox.hpsgc.de"),
        DEVELOPMENT("https://api-phdp-dev.hpsgc.de"),
        STAGING("https://api-staging.data4life.care"),
        LOCAL("https://api.data4life.local"),
        PRODUCTION("https://api.data4life.care")
    }

    internal enum class Smart4HealthURI(val uri: String) {
        SANDBOX("https://api-sandbox.smart4health.eu"),
        DEVELOPMENT("https://api-dev.smart4health.eu"),
        STAGING("https://api-staging.smart4health.eu"),
        LOCAL("https://api.smart4health.local"),
        PRODUCTION("https://api.smart4health.eu")
    }

    interface Environment {
        fun getApiBaseURL(platform: String): String
        fun getCertificatePin(platform: String): String
    }

    interface EnvironmentFactory {
        fun fromName(name: String?): Environment
    }

    internal interface ClientFactory {
        fun getInstance(
            authService: AuthorizationContract.Service,
            environment: Environment,
            clientId: String,
            clientSecret: String,
            platform: String,
            connectivityService: NetworkConnectivityService,
            clientName: Clients,
            clientVersion: String,
            staticAccessToken: ByteArray?,
            debugFlag: Boolean
        ): OkHttpClient
    }

    internal interface IHCServiceFactory {
        fun getInstance(
            client: OkHttpClient,
            platform: String,
            environment: Environment
        ): IHCService
    }

    interface SearchTagsBuilder {
        fun addOrTuple(tuple: List<String>): SearchTagsBuilder
        fun seal(): SearchTags
    }

    interface SearchTags {
        val tags: String
    }

    interface SearchTagsBuilderFactory {
        fun newBuilder(): SearchTagsBuilder
    }

    companion object {
        const val PLATFORM_D4L = "d4l"
        const val PLATFORM_S4H = "s4h"
        const val DATA4LIFE_CARE = "sha256/AJvjswWs1n4m1KDmFNnTqBit2RHFvXsrVU3Uhxcoe4Y="
        const val HPSGC_DE = "sha256/3f81qEv2rjHvcrwof2egbKo5MjjSHaN/4DOl7R+pH0E="
        const val SMART4HEALTH_EU = "sha256/yPBKbgJMVnMeovGKbAtuz65sfy/gpDu0WTiuB8bE5G0="
        const val REQUEST_TIMEOUT: Long = 2
        const val HEADER_ALIAS = "gc_alias"
        const val HEADER_AUTHORIZATION = "Authorization"
        const val ACCESS_TOKEN_MARKER = "access_token"
        const val BASIC_AUTH_MARKER = "basic_auth"
        const val HEADER_GC_SDK_VERSION = "gc-sdk-version"
        const val FORMAT_CLIENT_VERSION = "%s-%s"
        const val HEADER_TOTAL_COUNT = "x-total-count"
        const val PARAM_TAG_ENCRYPTION_KEY = "tek"
        const val FORMAT_BEARER_TOKEN = "Bearer %s"
        const val FORMAT_BASIC_AUTH = "Basic %s"
        const val MEDIA_TYPE_OCTET_STREAM = "application/octet-stream"
        const val HTTP_401_UNAUTHORIZED = 401
        const val AUTHORIZATION_WITH_ACCESS_TOKEN = "$HEADER_AUTHORIZATION: $ACCESS_TOKEN_MARKER"
        const val AUTHORIZATION_WITH_BASIC_AUTH = "$HEADER_AUTHORIZATION: $BASIC_AUTH_MARKER"
        const val HEADER_CONTENT_TYPE_OCTET_STREAM = "content-type: $MEDIA_TYPE_OCTET_STREAM"
    }
}
