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

package care.data4life.sdk.attachment

import care.data4life.sdk.fhir.Fhir3Attachment
import care.data4life.sdk.fhir.Fhir3DateTimeParser
import care.data4life.sdk.wrapper.WrapperContract

internal object CompatibilityValidator: AttachmentContract.CompatibilityValidator {
    private val fhir3SeparationDate = Fhir3DateTimeParser.parseDateTime(
            "2019-09-15"
    )

    private fun isHahableFhir3Date(attachment: Fhir3Attachment): Boolean {
        return attachment.creation?.date?.toDate()?.after(fhir3SeparationDate.date.toDate()) ?: true
    }

    override fun isHashable(attachment: WrapperContract.Attachment): Boolean {
        val rawAttachment = attachment.unwrap() as Any
        return if(rawAttachment is Fhir3Attachment ) {
            isHahableFhir3Date(rawAttachment)
        } else {
            true
        }
    }

}
