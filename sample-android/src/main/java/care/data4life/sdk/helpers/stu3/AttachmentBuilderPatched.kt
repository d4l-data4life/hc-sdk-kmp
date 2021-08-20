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

package care.data4life.sdk.helpers.stu3

import care.data4life.fhir.stu3.model.Attachment
import care.data4life.fhir.stu3.model.FhirDateTime
import care.data4life.sdk.config.DataRestriction
import care.data4life.sdk.lang.DataRestrictionException
import care.data4life.sdk.util.Base64
import care.data4life.sdk.util.HashUtil
import care.data4life.sdk.util.MimeType

object AttachmentBuilderPatched {

    /**
     * Helper method for building Attachments.
     *
     * @param title is attachment title
     * @param creationDate is the date when attachment was created
     * @param contentType is the Mime type of the [data]
     * @param data is the attachment content
     * @return [Attachment]
     * @throws UnsupportedFileType will be thrown if [data] is not one of the following types: JPEG, PNG, TIFF, PDF or DCM
     * @throws MaxDataSizeViolation will be thrown if [data] is bigger then 10MB
     */
    @JvmStatic
    @Throws(DataRestrictionException.UnsupportedFileType::class, DataRestrictionException.MaxDataSizeViolation::class)
    fun buildWith(
        title: String,
        creationDate: FhirDateTime,
        contentType: String,
        data: ByteArray
    ): Attachment {

        if (MimeType.recognizeMimeType(data) == MimeType.UNKNOWN) {
            throw DataRestrictionException.UnsupportedFileType()
        } else if (data.size > DataRestriction.DATA_SIZE_MAX_BYTES) {
            throw DataRestrictionException.MaxDataSizeViolation()
        }

        val attachment = Attachment()
        attachment.title = title
        attachment.creation = creationDate
        attachment.contentType = contentType
        attachment.data = Base64.encodeToString(data)
        attachment.size = data.size
        attachment.hash = Base64.encodeToString(HashUtil.sha1(data))
        return attachment
    }
}
