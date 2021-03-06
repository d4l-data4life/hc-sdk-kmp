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

import care.data4life.sdk.auth.UserService
import care.data4life.sdk.call.CallHandler
import care.data4life.sdk.call.Callback
import care.data4life.sdk.call.DataRecord
import care.data4life.sdk.call.Task
import care.data4life.sdk.record.RecordContract
import org.threeten.bp.LocalDate


internal class DataRecordClient(
        private val userService: UserService,
        private val recordService: RecordContract.Service,
        private val handler: CallHandler
) : DataContract.Client {

    override fun create(resource: DataResource, annotations: List<String>, callback: Callback<DataRecord<DataResource>>): Task {
        val operation = userService.finishLogin(true)
                .flatMap { userService.uID }
                .flatMap { uid -> recordService.createRecord(uid, resource, annotations) }
        return handler.executeSingle(operation, callback)
    }

    override fun update(recordId: String, resource: DataResource, annotations: List<String>, callback: Callback<DataRecord<DataResource>>): Task {
        val operation = userService.finishLogin(true)
                .flatMap { userService.uID }
                .flatMap { uid -> recordService.updateRecord(uid, recordId, resource, annotations) }
        return handler.executeSingle(operation, callback)
    }

    override fun delete(recordId: String, callback: Callback<Boolean>): Task {
        val operation = userService.finishLogin(true)
                .flatMap { userService.uID }
                .flatMap { uid -> recordService.deleteRecord(uid, recordId).toSingle { true } }
        return handler.executeSingle(operation, callback)
    }

    override fun fetch(recordId: String, callback: Callback<DataRecord<DataResource>>): Task {
        val operation = userService.finishLogin(true)
                .flatMap { userService.uID }
                .flatMap { uid -> recordService.fetchDataRecord(uid, recordId) }
        return handler.executeSingle(operation, callback)
    }

    override fun search(annotations: List<String>, startDate: LocalDate?, endDate: LocalDate?, pageSize: Int, offset: Int, callback: Callback<List<DataRecord<DataResource>>>): Task {
        val operation = userService.finishLogin(true)
                .flatMap { userService.uID }
                .flatMap { uid ->
                    recordService.fetchDataRecords(
                            uid,
                            annotations,
                            startDate,
                            endDate,
                            pageSize,
                            offset
                    )
                }
        return handler.executeSingle(operation, callback)
    }

}
