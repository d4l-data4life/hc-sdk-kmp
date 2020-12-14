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

import care.data4life.sdk.RecordService
import care.data4life.sdk.SdkContract
import care.data4life.sdk.auth.UserService
import care.data4life.sdk.call.CallHandler
import care.data4life.sdk.call.Callback
import care.data4life.sdk.call.Fhir4Record
import care.data4life.sdk.data.DataResource

internal class Fhir4RecordClient(
        private val userService: UserService,
        private val recordService: RecordService,
        private val handler: CallHandler
) : SdkContract.Fhir4RecordClient {

    override fun <T : Fhir4Resource> create(resource: T, annotations: List<String>, callback: Callback<Fhir4Record<T>>) {
        val operation = userService.finishLogin(true)
                .flatMap { userService.uID }
                .flatMap { uid -> recordService.createFhir4Record(uid, resource, annotations) }

        handler.executeSingle(operation, callback)
    }

    override fun <T : Fhir4Resource> update(recordId: String, resource: DataResource, annotations: List<String>, callback: Callback<Fhir4Record<T>>) {
        TODO("Not yet implemented")
    }
}
