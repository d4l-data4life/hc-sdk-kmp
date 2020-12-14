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

package care.data4life.sdk.fhir

import care.data4life.sdk.SdkContract
import care.data4life.sdk.auth.UserService
import care.data4life.sdk.call.CallHandler
import care.data4life.sdk.call.Callback
import care.data4life.sdk.call.Fhir4Record
import care.data4life.sdk.call.Task
import care.data4life.sdk.record.RecordContract
import org.threeten.bp.LocalDate

internal class Fhir4RecordClient(
        private val userService: UserService,
        private val recordService: RecordContract.Service,
        private val handler: CallHandler
) : SdkContract.Fhir4RecordClient {

    override fun <T : Fhir4Resource> create(resource: T, annotations: List<String>, callback: Callback<Fhir4Record<T>>) {
        val operation = userService.finishLogin(true)
                .flatMap { userService.uID }
                .flatMap { uid -> recordService.createRecord(uid, resource, annotations) }
        handler.executeSingle(operation, callback)
    }

    override fun <T : Fhir4Resource> update(recordId: String, resource: T, annotations: List<String>, callback: Callback<Fhir4Record<T>>) {
        val operation = userService.finishLogin(true)
                .flatMap { userService.uID }
                .flatMap { uid -> recordService.updateRecord(uid, recordId, resource, annotations) }
        handler.executeSingle(operation, callback)
    }

    override fun delete(recordId: String, callback: Callback<Boolean>) {
        val operation = userService.finishLogin(true)
                .flatMap { userService.uID }
                .flatMap { uid -> recordService.deleteRecord(uid, recordId).toSingle { true } }
        handler.executeSingle(operation, callback)
    }

    override fun <T : Fhir4Resource> fetch(recordId: String, callback: Callback<Fhir4Record<T>>): Task {
        val operation = userService.finishLogin(true)
                .flatMap { userService.uID }
                .flatMap { uid -> recordService.fetchFhir4Record<T>(uid, recordId) }
        return handler.executeSingle(operation, callback)
    }

    override fun <T : Fhir4Resource> search(resourceType: Class<T>, annotations: List<String>, startDate: LocalDate?, endDate: LocalDate?, pageSize: Int, offset: Int, callback: Callback<List<Fhir4Record<T>>>): Task {
        val operation = userService.finishLogin(true)
                .flatMap { userService.uID }
                .flatMap { uid ->
                    recordService.fetchFhir4Records(
                            uid,
                            resourceType,
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
