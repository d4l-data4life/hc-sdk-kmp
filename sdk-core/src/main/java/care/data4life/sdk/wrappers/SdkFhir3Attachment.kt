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

package care.data4life.sdk.wrappers

import care.data4life.sdk.fhir.Fhir3Attachment

internal class SdkFhir3Attachment(
        private val attachment: Fhir3Attachment
): WrappersContract.Attachment {
    override var id: String?
        get() = attachment.id
        set(id) { attachment.id = id}

    override var data: String?
        get() = attachment.data
        set(data) {attachment.data = data}

    override var hash: String?
        get() = attachment.hash
        set(hash) {attachment.hash = hash}

    override var size: Int?
        get() = attachment.size
        set(size) {attachment.size = size}

    override fun unwrap(): Fhir3Attachment = this.attachment


}
