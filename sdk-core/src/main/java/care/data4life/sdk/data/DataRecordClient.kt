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

package care.data4life.sdk.data

import care.data4life.sdk.SdkContract
import care.data4life.sdk.auth.AuthContract
import care.data4life.sdk.call.CallHandler
import care.data4life.sdk.call.Callback
import care.data4life.sdk.call.DataRecord
import care.data4life.sdk.call.Task
import care.data4life.sdk.record.RecordContract
import care.data4life.sdk.tag.Annotations
import io.reactivex.Single

internal class DataRecordClient(
    private val userService: AuthContract.UserService,
    private val recordService: RecordContract.Service,
    private val handler: CallHandler
) : DataContract.Client {
    private fun <T> executeOperationFlow(
        operation: (userId: String) -> Single<T>,
        callback: Callback<T>
    ): Task {
        val flow = userService.finishLogin(true)
            .flatMap { userService.userID }
            .flatMap { userId -> operation(userId) }
        return handler.executeSingle(flow, callback)
    }

    override fun create(
        resource: DataResource,
        annotations: Annotations,
        callback: Callback<DataRecord<DataResource>>
    ): Task = executeOperationFlow(
        { userId -> recordService.createRecord(userId, resource, annotations) },
        callback
    )

    override fun update(
        recordId: String,
        resource: DataResource,
        annotations: Annotations,
        callback: Callback<DataRecord<DataResource>>
    ): Task = executeOperationFlow(
        { userId -> recordService.updateRecord(userId, recordId, resource, annotations) },
        callback
    )

    override fun fetch(
        recordId: String,
        callback: Callback<DataRecord<DataResource>>
    ): Task = executeOperationFlow(
        { userId -> recordService.fetchDataRecord(userId, recordId) },
        callback
    )

    override fun search(
        annotations: Annotations,
        creationDateRange: SdkContract.CreationDateRange,
        updateDateTimeRange: SdkContract.UpdateDateTimeRange,
        includeDeletedRecords: Boolean,
        pageSize: Int,
        offset: Int,
        callback: Callback<List<DataRecord<DataResource>>>
    ): Task = executeOperationFlow(
        { userId ->
            recordService.searchDataRecords(
                userId,
                annotations,
                creationDateRange,
                updateDateTimeRange,
                includeDeletedRecords,
                pageSize,
                offset
            )
        },
        callback
    )

    override fun count(
        annotations: Annotations,
        callback: Callback<Int>
    ): Task = executeOperationFlow(
        { userId -> recordService.countDataRecords(DataResource::class.java, userId, annotations) },
        callback
    )

    override fun delete(
        recordId: String,
        callback: Callback<Boolean>
    ): Task = executeOperationFlow(
        { userId -> recordService.deleteRecord(userId, recordId).toSingle { true } },
        callback
    )
}
