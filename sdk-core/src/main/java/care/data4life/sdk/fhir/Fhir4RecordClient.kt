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
import care.data4life.sdk.auth.AuthContract
import care.data4life.sdk.call.CallHandler
import care.data4life.sdk.call.Callback
import care.data4life.sdk.call.Fhir4Record
import care.data4life.sdk.call.Task
import care.data4life.sdk.model.DownloadType
import care.data4life.sdk.record.RecordContract
import care.data4life.sdk.tag.Annotations
import io.reactivex.Single
import org.threeten.bp.LocalDate

internal class Fhir4RecordClient(
    private val userService: AuthContract.UserService,
    private val recordService: RecordContract.Service,
    private val handler: CallHandler
) : SdkContract.Fhir4RecordClient {

    private fun <T> executeOperationFlow(
        operation: (userId: String) -> Single<T>,
        callback: Callback<T>
    ): Task {
        val flow = userService.finishLogin(true)
            .flatMap { userService.userID }
            .flatMap { userId -> operation(userId) }
        return handler.executeSingle(flow, callback)
    }

    override fun <T : Fhir4Resource> create(
        resource: T,
        annotations: Annotations,
        callback: Callback<Fhir4Record<T>>
    ): Task = executeOperationFlow(
        { userId -> recordService.createRecord(userId, resource, annotations) },
        callback
    )

    override fun <T : Fhir4Resource> update(
        recordId: String,
        resource: T,
        annotations: Annotations,
        callback: Callback<Fhir4Record<T>>
    ): Task = executeOperationFlow(
        { userId -> recordService.updateRecord(userId, recordId, resource, annotations) },
        callback
    )

    override fun <T : Fhir4Resource> fetch(
        recordId: String,
        callback: Callback<Fhir4Record<T>>
    ): Task = executeOperationFlow(
        { userId -> recordService.fetchFhir4Record(userId, recordId) },
        callback
    )

    override fun <T : Fhir4Resource> search(
        resourceType: Class<T>,
        annotations: Annotations,
        startDate: LocalDate?,
        endDate: LocalDate?,
        pageSize: Int,
        offset: Int,
        callback: Callback<List<Fhir4Record<T>>>
    ): Task = executeOperationFlow(
        { userId ->
            recordService.fetchFhir4Records(
                userId,
                resourceType,
                annotations,
                startDate,
                endDate,
                pageSize,
                offset
            )
        },
        callback
    )

    override fun <T : Fhir4Resource> download(
        recordId: String,
        callback: Callback<Fhir4Record<T>>
    ): Task = executeOperationFlow(
        { userId -> recordService.downloadFhir4Record(recordId, userId) },
        callback
    )

    override fun <T : Fhir4Resource> count(
        resourceType: Class<T>,
        annotations: Annotations,
        callback: Callback<Int>
    ): Task = executeOperationFlow(
        { userId -> recordService.countFhir4Records(resourceType, userId, annotations) },
        callback
    )

    override fun delete(
        recordId: String,
        callback: Callback<Boolean>
    ): Task = executeOperationFlow(
        { userId -> recordService.deleteRecord(userId, recordId).toSingle { true } },
        callback
    )

    override fun downloadAttachment(
        recordId: String,
        attachmentId: String,
        type: DownloadType,
        callback: Callback<Fhir4Attachment>
    ): Task = executeOperationFlow(
        { userId -> recordService.downloadFhir4Attachment(recordId, attachmentId, userId, type) },
        callback
    )

    override fun downloadAttachments(
        recordId: String,
        attachmentIds: List<String>,
        type: DownloadType,
        callback: Callback<List<Fhir4Attachment>>
    ): Task = executeOperationFlow(
        { userId -> recordService.downloadFhir4Attachments(recordId, attachmentIds, userId, type) },
        callback
    )
}
