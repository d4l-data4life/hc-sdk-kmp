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

import care.data4life.crypto.GCKey
import care.data4life.sdk.lang.DataValidationException
import care.data4life.sdk.model.DownloadType
import care.data4life.sdk.wrapper.WrapperContract

interface ThumbnailContract {

    interface Service {

        fun uploadDownscaledImages(
                attachmentsKey: GCKey,
                userId: String,
                attachment: WrapperContract.Attachment,
                originalData: ByteArray
        ): List<String>

        fun updateResourceIdentifier(
                resource: WrapperContract.Resource,
                result: List<Pair<WrapperContract.Attachment, List<String>?>>
        )

        @Throws(DataValidationException.IdUsageViolation::class)
        fun setAttachmentIdForDownloadType(
                attachments: List<WrapperContract.Attachment>,
                identifiers: List<Any>?,
                type: DownloadType?
        )

        @Throws(DataValidationException.IdUsageViolation::class)
        fun cleanObsoleteAdditionalIdentifiers(resource: WrapperContract.Resource?)
    }
}
