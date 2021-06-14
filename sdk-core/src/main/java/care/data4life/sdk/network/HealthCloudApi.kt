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
package care.data4life.sdk.network

import care.data4life.sdk.network.NetworkingContract.Companion.AUTHORIZATION_WITH_ACCESS_TOKEN
import care.data4life.sdk.network.NetworkingContract.Companion.AUTHORIZATION_WITH_BASIC_AUTH
import care.data4life.sdk.network.NetworkingContract.Companion.HEADER_ALIAS
import care.data4life.sdk.network.NetworkingContract.Companion.HEADER_CONTENT_TYPE_OCTET_STREAM
import care.data4life.sdk.network.model.CommonKeyResponse
import care.data4life.sdk.network.model.DocumentUploadResponse
import care.data4life.sdk.network.model.EncryptedRecord
import care.data4life.sdk.network.model.UserInfo
import care.data4life.sdk.network.model.VersionList
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.HEAD
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface HealthCloudApi {
    // Key
    @GET("/users/{userId}/commonkeys/{commonKeyId}")
    @Headers(AUTHORIZATION_WITH_ACCESS_TOKEN)
    fun fetchCommonKey(
        @Header(HEADER_ALIAS) alias: String,
        @Path("userId") userId: String,
        @Path("commonKeyId") commonKeyId: String
    ): Single<CommonKeyResponse>

    @POST("/users/{userId}/tek")
    @Headers(AUTHORIZATION_WITH_ACCESS_TOKEN)
    fun uploadTagEncryptionKey(
        @Header(HEADER_ALIAS) alias: String,
        @Path("userId") userId: String,
        @Body params: Map<String, String>
    ): Completable

    // Record
    @POST("/users/{userId}/records")
    @Headers(AUTHORIZATION_WITH_ACCESS_TOKEN)
    fun createRecord(
        @Header(HEADER_ALIAS) alias: String,
        @Path("userId") userId: String,
        @Body encryptedRecord: EncryptedRecord
    ): Single<EncryptedRecord>

    @PUT("/users/{userId}/records/{recordId}")
    @Headers(AUTHORIZATION_WITH_ACCESS_TOKEN)
    fun updateRecord(
        @Header(HEADER_ALIAS) alias: String,
        @Path("userId") userId: String,
        @Path("recordId") recordId: String,
        @Body encryptedRecord: EncryptedRecord
    ): Single<EncryptedRecord>

    @GET("/users/{userId}/records/{recordId}")
    @Headers(AUTHORIZATION_WITH_ACCESS_TOKEN)
    fun fetchRecord(
        @Header(HEADER_ALIAS) alias: String,
        @Path("userId") userId: String,
        @Path("recordId") recordId: String
    ): Single<EncryptedRecord>

    // FIXME validate if startDate, endDate, pageSize and offset are nullable
    @GET("/users/{userId}/records")
    @Headers(AUTHORIZATION_WITH_ACCESS_TOKEN)
    fun searchRecords(
        @Header(HEADER_ALIAS) alias: String,
        @Path("userId") userId: String,
        @Query("start_date") startDate: String?,
        @Query("end_date") endDate: String?,
        @Query("limit") pageSize: Int,
        @Query("offset") offset: Int,
        @Query("tags") tags: String
    ): Observable<List<EncryptedRecord>>

    @HEAD("/users/{userId}/records")
    @Headers(AUTHORIZATION_WITH_ACCESS_TOKEN)
    fun getRecordsHeader(
        @Header(HEADER_ALIAS) alias: String,
        @Path("userId") userId: String,
        @Query("tags") tags: String
    ): Single<Response<Void>>

    @DELETE("/users/{userId}/records/{recordId}")
    @Headers(AUTHORIZATION_WITH_ACCESS_TOKEN)
    fun deleteRecord(
        @Header(HEADER_ALIAS) alias: String,
        @Path("userId") userId: String,
        @Path("recordId") recordId: String
    ): Completable

    // Attachments
    @POST("/users/{userId}/documents")
    @Headers(AUTHORIZATION_WITH_ACCESS_TOKEN, HEADER_CONTENT_TYPE_OCTET_STREAM)
    fun uploadDocument(
        @Header(HEADER_ALIAS) alias: String,
        @Path("userId") userId: String,
        @Body encryptedAttachment: RequestBody
    ): Single<DocumentUploadResponse>

    @GET("/users/{userId}/documents/{documentId}")
    @Headers(AUTHORIZATION_WITH_ACCESS_TOKEN)
    fun downloadDocument(
        @Header(HEADER_ALIAS) alias: String,
        @Path("userId") userId: String,
        @Path("documentId") documentId: String
    ): Single<ResponseBody>

    @DELETE("/users/{userId}/documents/{documentId}")
    @Headers(AUTHORIZATION_WITH_ACCESS_TOKEN)
    fun deleteDocument(
        @Header(HEADER_ALIAS) alias: String,
        @Path("userId") userId: String,
        @Path("documentId") documentId: String
    ): Single<Void>

    // Misc
    @Headers(AUTHORIZATION_WITH_ACCESS_TOKEN)
    @GET("/userinfo")
    fun fetchUserInfo(
        @Header(HEADER_ALIAS) alias: String
    ): Single<UserInfo>

    @GET("/sdk/v1/android/versions.json")
    fun fetchVersionInfo(): Single<VersionList>

    @FormUrlEncoded
    @POST("/oauth/revoke")
    @Headers(AUTHORIZATION_WITH_BASIC_AUTH)
    fun logout(
        @Header(HEADER_ALIAS) alias: String,
        @Field("token") refresh_token: String
    ): Completable
}
