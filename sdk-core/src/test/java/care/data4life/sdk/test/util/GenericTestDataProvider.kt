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

package care.data4life.sdk.test.util

import care.data4life.sdk.attachment.ThumbnailService
import care.data4life.sdk.config.DataRestriction
import care.data4life.sdk.record.RecordContract
import care.data4life.sdk.util.Base64
import care.data4life.sdk.util.MimeType
import org.threeten.bp.format.DateTimeFormatter
import org.threeten.bp.format.DateTimeFormatterBuilder
import java.util.*

object GenericTestDataProvider {
    val UNKNOWN = TestAttachmentHelper.makeAttachmentData(
            "Potato".toByteArray(),
            DataRestriction.DATA_SIZE_MAX_BYTES
    )

    val PDF = TestAttachmentHelper.makeAttachmentData(
            TestAttachmentHelper.byteArrayOf(MimeType.PDF.byteSignature()[0]!!),
            DataRestriction.DATA_SIZE_MAX_BYTES
    )

    val PDF_ENCODED = Base64.encodeToString(PDF)

    val PDF_OVERSIZED = TestAttachmentHelper.makeAttachmentData(
            TestAttachmentHelper.byteArrayOf(MimeType.PDF.byteSignature()[0]!!),
            DataRestriction.DATA_SIZE_MAX_BYTES + 1
    )

    val PDF_OVERSIZED_ENCODED = Base64.encodeToString(PDF_OVERSIZED)

    const val RECORD_ID = "recordId"

    const val RECORD_ID_COMPATIBILITY = "otherRecordId"

    const val USER_ID = "userId"

    const val PARTNER_ID = "partnerId"

    const val CLIENT_ID = "$PARTNER_ID#TEST"

    const val ALIAS = "alias"

    const val DATA_PAYLOAD = "data"

    const val DATA_SIZE = 42

    const val DATA_HASH = "dataHash"

    const val ATTACHMENT_ID = "attachmentId"

    const val THUMBNAIL_ID = "thumbnailId"

    const val PREVIEW_ID = "previewId"

    const val ASSIGNER = "assigner"

    const val CREATION_DATE = "2020-05-04"

    const val UPDATE_DATE = "2020-05-04T07:45:08.234123"

    const val COMMON_KEY_ID = "commonKeyId"

    const val ARBITRARY_DATA_KEY = "appdata"

    const val OFFSET = 23

    const val PAGE_SIZE = 42

    const val ADDITIONAL_ID = RecordContract.Service.DOWNSCALED_ATTACHMENT_IDS_FMT +
            ThumbnailService.SPLIT_CHAR +
            ATTACHMENT_ID +
            ThumbnailService.SPLIT_CHAR +
            PREVIEW_ID +
            ThumbnailService.SPLIT_CHAR +
            THUMBNAIL_ID

    val OBSOLETE_ID = ADDITIONAL_ID.replaceFirst(ATTACHMENT_ID, "obsoleteId")

    const val OTHER_ID = "otherId"

    const val VALUE_INDICATOR = "valueAttachment"

    const val VALUE_ID = RecordContract.Service.DOWNSCALED_ATTACHMENT_IDS_FMT +
            ThumbnailService.SPLIT_CHAR +
            VALUE_INDICATOR +
            ThumbnailService.SPLIT_CHAR +
            PREVIEW_ID +
            ThumbnailService.SPLIT_CHAR +
            THUMBNAIL_ID

    val IV = ByteArray(16)

    private const val DATE_FORMAT = "yyyy-MM-dd"

    private const val DATE_TIME_FORMAT = "yyyy-MM-dd'T'HH:mm:ss[.SSS]"

    val DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern(DATE_FORMAT, Locale.US)

    val DATE_TIME_FORMATTER: DateTimeFormatter = DateTimeFormatterBuilder()
            .parseLenient()
            .appendPattern(DATE_TIME_FORMAT)
            .toFormatter(Locale.US)
}
