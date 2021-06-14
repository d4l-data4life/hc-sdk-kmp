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

import care.data4life.sdk.network.model.CommonKeyResponse
import care.data4life.sdk.network.model.EncryptedRecord
import care.data4life.sdk.network.model.NetworkModelContract
import care.data4life.sdk.network.model.UserInfo
import care.data4life.sdk.network.model.VersionList
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single

interface NetworkingContract {
    // TODO: Break this down
    // TODO: Decouple ReturnValues
    interface Service {
        // TODO: move into a key route
        fun fetchCommonKey(
            alias: String,
            userId: String,
            commonKeyId: String
        ): Single<CommonKeyResponse>

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
        ): Single<EncryptedRecord>

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

        fun countRecords(alias: String, userId: String, tags: SearchTags): Single<Int>

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

    enum class Clients(val identifier: String) {
        ANDROID("android"),
        JAVA("jvm"),
        INGESTION("ingestion")
    }

    fun interface NetworkConnectivityService {
        fun isConnected(): Boolean
    }

    interface Environment {
        fun getApiBaseURL(platform: String): String
        fun getCertificatePin(platform: String): String
    }

    interface EnvironmentFactory {
        fun fromName(name: String?): Environment
    }

    interface SearchTagsBuilder {
        fun addOrTuple(tuple: List<String>): SearchTagsBuilder
        fun seal(): SearchTags
    }

    interface SearchTags {
        val tagGroups: String
    }

    interface SearchTagsBuilderFactory {
        fun newBuilder(): SearchTagsBuilder
    }

    companion object {
        const val PLATFORM_D4L = "d4l"
        const val PLATFORM_S4H = "s4h"
        const val DATA4LIFE_CARE = "sha256/AJvjswWs1n4m1KDmFNnTqBit2RHFvXsrVU3Uhxcoe4Y="
        const val HPSGC_DE = "sha256/3f81qEv2rjHvcrwof2egbKo5MjjSHaN/4DOl7R+pH0E="
        const val REQUEST_TIMEOUT: Long = 2
        const val HEADER_ALIAS = "gc_alias"
        const val HEADER_AUTHORIZATION = "Authorization"
        const val ACCESS_TOKEN_MARKER = "access_token"
        const val BASIC_AUTH_MARKER = "basic_auth"
        const val HEADER_SDK_VERSION = "d4l-sdk-version"
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
