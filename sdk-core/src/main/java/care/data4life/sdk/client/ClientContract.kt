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

import care.data4life.sdk.call.CallContract
import care.data4life.sdk.call.Callback
import care.data4life.sdk.call.Task
import care.data4life.sdk.model.DownloadType
import care.data4life.sdk.tag.Annotations
import org.threeten.bp.LocalDate

interface ClientContract {
    interface ResourceClient<Resource> {
        fun <T : Resource> create(
            resource: T,
            annotations: Annotations,
            callback: Callback<CallContract.Record<T>>
        ): Task

        fun <T : Resource> update(
            recordId: String,
            resource: T,
            annotations: Annotations,
            callback: Callback<CallContract.Record<T>>
        ): Task

        fun delete(recordId: String, callback: Callback<Boolean>): Task

        fun <T : Resource> fetch(
            recordId: String,
            callback: Callback<CallContract.Record<T>>
        ): Task
    }

    interface ResourceSearchClient<Resource> {
        fun <T : Resource> search(
            resourceType: Class<T>,
            annotations: Annotations,
            startDate: LocalDate?,
            endDate: LocalDate?,
            pageSize: Int,
            offset: Int,
            callback: Callback<List<CallContract.Record<T>>>
        ): Task

        fun <T : Resource> count(
            resourceType: Class<T>,
            annotations: Annotations,
            callback: Callback<Int>
        ): Task
    }

    interface ResourcelessSearchClient<Resource> {
        fun <T : Resource> search(
            annotations: Annotations,
            startDate: LocalDate?,
            endDate: LocalDate?,
            pageSize: Int,
            offset: Int,
            callback: Callback<List<CallContract.Record<T>>>
        ): Task

        fun count(
            annotations: Annotations,
            callback: Callback<Int>
        ): Task
    }

    interface AttachmentClient<Resource, Attachment> {
        fun <T : Resource> download(
            recordId: String,
            callback: Callback<CallContract.Record<T>>
        ): Task

        fun downloadAttachment(
            recordId: String,
            attachmentId: String,
            type: DownloadType,
            callback: Callback<Attachment>
        ): Task

        fun downloadAttachments(
            recordId: String,
            attachmentIds: List<String>,
            type: DownloadType,
            callback: Callback<List<Attachment>>
        ): Task
    }
}
