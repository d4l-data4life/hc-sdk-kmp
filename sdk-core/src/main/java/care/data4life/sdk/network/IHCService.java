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

package care.data4life.sdk.network;

import java.util.List;
import java.util.Map;

import care.data4life.sdk.network.model.CommonKeyResponse;
import care.data4life.sdk.network.model.DocumentUploadResponse;
import care.data4life.sdk.network.model.EncryptedRecord;
import care.data4life.sdk.network.model.UserInfo;
import care.data4life.sdk.network.model.VersionInfo;
import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.Single;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Response;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.HEAD;
import retrofit2.http.Header;
import retrofit2.http.Headers;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface IHCService {


    String AUTHORIZATION_WITH_ACCESS_TOKEN = "Authorization: access_token";
    String AUTHORIZATION_WITH_BASIC_AUTH = "Authorization: basic_auth";
    String HEADER_ALIAS = "gc_alias";
    String HEADER_CONTENT_TYPE_OCTET_STREAM = "content-type: application/octet-stream";


    @GET("/users/{userId}/commonkeys/{commonKeyId}")
    @Headers(AUTHORIZATION_WITH_ACCESS_TOKEN)
    Single<CommonKeyResponse> fetchCommonKey(
            @Header(HEADER_ALIAS) String alias,
            @Path("userId") String userId,
            @Path("commonKeyId") String commonKeyId
    );

    @POST("/users/{userId}/tek")
    @Headers(AUTHORIZATION_WITH_ACCESS_TOKEN)
    Completable uploadTagEncryptionKey(
            @Header(HEADER_ALIAS) String alias,
            @Path("userId") String userId,
            @Body Map<String, String> params
    );

    @POST("/users/{userId}/records")
    @Headers(AUTHORIZATION_WITH_ACCESS_TOKEN)
    Single<EncryptedRecord> createRecord(
            @Header(HEADER_ALIAS) String alias,
            @Path("userId") String userId,
            @Body EncryptedRecord encryptedRecord
    );

    @GET("/users/{userId}/records")
    @Headers(AUTHORIZATION_WITH_ACCESS_TOKEN)
    Observable<List<EncryptedRecord>> searchRecords(
            @Header(HEADER_ALIAS) String alias,
            @Path("userId") String userId,
            @Query("start_date") String startDate,
            @Query("end_date") String endDate,
            @Query("limit") Integer pageSize,
            @Query("offset") Integer offset,
            @Query("tags") List<String> tags
    );

    @HEAD("/users/{userId}/records")
    @Headers(AUTHORIZATION_WITH_ACCESS_TOKEN)
    Single<Response<Void>> getRecordsHeader(
            @Header(HEADER_ALIAS) String alias,
            @Path("userId") String userId,
            @Query("tags") List<String> tags
    );

    @GET("/users/{userId}/records/{recordId}")
    @Headers(AUTHORIZATION_WITH_ACCESS_TOKEN)
    Single<EncryptedRecord> fetchRecord(
            @Header(HEADER_ALIAS) String alias,
            @Path("userId") String userId,
            @Path("recordId") String recordId
    );

    @DELETE("/users/{userId}/records/{recordId}")
    @Headers(AUTHORIZATION_WITH_ACCESS_TOKEN)
    Completable deleteRecord(
            @Header(HEADER_ALIAS) String alias,
            @Path("userId") String userId,
            @Path("recordId") String recordId
    );

    @PUT("/users/{userId}/records/{recordId}")
    @Headers(AUTHORIZATION_WITH_ACCESS_TOKEN)
    Single<EncryptedRecord> updateRecord(
            @Header(HEADER_ALIAS) String alias,
            @Path("userId") String userId,
            @Path("recordId") String recordId,
            @Body EncryptedRecord encryptedRecord
    );

    @POST("/users/{userId}/documents")
    @Headers({AUTHORIZATION_WITH_ACCESS_TOKEN, HEADER_CONTENT_TYPE_OCTET_STREAM})
    Single<DocumentUploadResponse> uploadDocument(
            @Header(HEADER_ALIAS) String alias,
            @Path("userId") String userId,
            @Body RequestBody encryptedAttachment
    );

    @GET("/users/{userId}/documents/{documentId}")
    @Headers(AUTHORIZATION_WITH_ACCESS_TOKEN)
    Single<ResponseBody> downloadDocument(
            @Header(HEADER_ALIAS) String alias,
            @Path("userId") String userId,
            @Path("documentId") String documentId
    );

    @DELETE("/users/{userId}/documents/{documentId}")
    @Headers(AUTHORIZATION_WITH_ACCESS_TOKEN)
    Single<Void> deleteDocument(
            @Header(HEADER_ALIAS) String alias,
            @Path("userId") String userId,
            @Path("documentId") String documentId
    );

    @Headers(AUTHORIZATION_WITH_ACCESS_TOKEN)
    @GET("/userinfo")
    Single<UserInfo> fetchUserInfo(
            @Header(HEADER_ALIAS) String alias
    );

    @FormUrlEncoded
    @POST("/oauth/revoke")
    @Headers(AUTHORIZATION_WITH_BASIC_AUTH)
    Completable logout(
            @Header(HEADER_ALIAS) String alias,
            @Field("token") String refresh_token
    );

    @Headers(AUTHORIZATION_WITH_ACCESS_TOKEN)
    @GET("/sdk/v1/android/versions.json")
    Single<VersionInfo> getVersionUpdateInfo(
            @Header(HEADER_ALIAS) String alias
    );

}

