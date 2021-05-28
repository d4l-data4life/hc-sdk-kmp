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

package care.data4life.sdk.client

import care.data4life.sdk.auth.AuthContract
import care.data4life.sdk.call.CallContract
import care.data4life.sdk.call.CallHandler
import care.data4life.sdk.call.Callback
import care.data4life.sdk.call.Task
import care.data4life.sdk.record.RecordContract
import care.data4life.sdk.resource.DataResource
import care.data4life.sdk.resource.ResourceContract
import care.data4life.sdk.tag.Annotations
import org.threeten.bp.LocalDate

internal class DataRecordClient(
    userService: AuthContract.UserService,
    private val recordService: RecordContract.Service,
    handler: CallHandler
) : ResourceContract.Client, Client(
    userService,
    handler
) {
    override fun <T : ResourceContract.DataResource> create(
        resource: T,
        annotations: Annotations,
        callback: Callback<CallContract.Record<T>>
    ): Task = executeOperationFlow(
        { userId -> recordService.createRecord(userId, resource, annotations) },
        callback
    )

    override fun <T : ResourceContract.DataResource> update(
        recordId: String,
        resource: T,
        annotations: Annotations,
        callback: Callback<CallContract.Record<T>>
    ): Task = executeOperationFlow(
        { userId -> recordService.updateRecord(userId, recordId, resource, annotations) },
        callback
    )

    override fun <T : ResourceContract.DataResource> fetch(
        recordId: String,
        callback: Callback<CallContract.Record<T>>
    ): Task = executeOperationFlow(
        { userId -> recordService.fetchDataRecord(userId, recordId) },
        callback
    )

    override fun <T : ResourceContract.DataResource> search(
        annotations: Annotations,
        startDate: LocalDate?,
        endDate: LocalDate?,
        pageSize: Int,
        offset: Int,
        callback: Callback<List<CallContract.Record<T>>>
    ): Task = executeOperationFlow(
        { userId ->
            recordService.fetchDataRecords(
                userId,
                annotations,
                startDate,
                endDate,
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
