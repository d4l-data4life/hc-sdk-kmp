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

import care.data4life.sdk.call.Callback
import care.data4life.sdk.call.DataRecord
import care.data4life.sdk.call.Task
import care.data4life.sdk.data.DataResource
import care.data4life.sdk.lang.D4LException
import org.threeten.bp.LocalDate
import care.data4life.sdk.listener.Callback as LegacyCallback
import care.data4life.sdk.listener.ResultListener as LegacyListener

interface SdkContract {

    interface Client {

        val data: DataRecordClient

        val fhir4: Fhir4RecordClient
    }

    interface AuthClient {
        /**
         * Get the currently active User session token if present.
         *
         * @param listener result contains either User session token or Error
         */
        fun getUserSessionToken(listener: LegacyListener<String>)

        /**
         * Checks if user is logged in.
         *
         * @param listener resulting Boolean indicates if the user is logged in or not or Error
         */
        fun isUserLoggedIn(listener: LegacyListener<Boolean>)

        /**
         * Logout the user
         *
         * @param listener either [Callback.onSuccess] is called or [Callback.onError]
         */
        fun logout(listener: LegacyCallback)

    }

    interface Fhir4RecordClient {
    }

    interface DataRecordClient {
        /**
         * Creates an {@link DataRecord} record.
         *
         * @param data           the data that will be created
         * @param annotations    custom annotations added as tags to the record
         * @param callback       either {@link Callback#onSuccess(Object)} or {@link Callback#onError(D4LException)} will be called
         */
        fun create(resource: DataResource, annotations: List<String>, callback: Callback<DataRecord<DataResource>>)

        /**
         * @param recordId       the id of the {@link care.data4life.sdk.model.definitions.DataRecord} that shall be update
         * @param data           the updated data byte array that shall be uploaded
         * @param annotations    custom annotations added as tags to the record
         * @param callback       either {@link Callback#onSuccess(Object)} or {@link Callback#onError(D4LException)} will be called
         */
        fun update(recordId: String, resource: DataResource, annotations: List<String>, callback: Callback<DataRecord<DataResource>>)

        /**
         * Delete an DataRecord
         *
         * @param recordId      the id of the record that shall be deleted
         * @param callback      either {@link Callback#onSuccess()} or {@link Callback#onError(D4LException)} will be called
         */
        fun delete(recordId: String, callback: Callback<Boolean>)

        /**
         * Fetch an DataRecord with given recordId
         *
         * @param recordId          the id of the data record which shall be fetched
         * @param callback          either {@link Callback#onSuccess(Object)} or {@link Callback#onError(D4LException)} will be called
         * @return                  {@link Task} which can be used to cancel ongoing operation or to query operation status
         */
        fun fetch(recordId: String, callback: Callback<DataRecord<DataResource>>): Task

        /**
         * Search DataRecords with filters
         *
         * @param annotations custom annotations added as tags to the record
         * @param startDate   the filtered records have a creation date after the start date
         * @param endDate     the filtered records have a creation date before the endDate
         * @param pageSize    define the size page result
         * @param offset      the offset of the records list
         * @param callback    either {@link Callback#onSuccess(Object)} or {@link Callback#onError(D4LException)} will be called
         * @return            {@link Task} which can be used to cancel ongoing operation or to query operation status
         */
        fun search(annotations: List<String>,
                   startDate: LocalDate?,
                   endDate: LocalDate?,
                   pageSize: Int,
                   offset: Int,
                   callback: Callback<List<DataRecord<DataResource>>>
        ): Task

    }

    /**
     * Legacy Client interface
     *
     * Deprecated with version v1.9.0
     * <p>
     * Will be removed in version v2.0.0
     */
    @Deprecated(message = "Deprecated with version v1.9.0 and will be removed in version v2.0.0")
    interface LegacyDataClient : SdkContractLegacy.DataClient

    interface ErrorHandler {
        fun handleError(error: Throwable): D4LException
    }
}
