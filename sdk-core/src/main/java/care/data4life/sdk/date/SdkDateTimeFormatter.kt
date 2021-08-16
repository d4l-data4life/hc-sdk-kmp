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

package care.data4life.sdk.date

import care.data4life.sdk.date.DateHelperContract.DateTimeFormatter.Companion.DATE_FORMAT
import care.data4life.sdk.date.DateHelperContract.DateTimeFormatter.Companion.DATE_TIME_FORMAT
import care.data4life.sdk.model.Meta
import care.data4life.sdk.model.ModelContract
import care.data4life.sdk.network.model.NetworkModelContract.DecryptedBaseRecord
import org.threeten.bp.LocalDate
import org.threeten.bp.LocalDateTime
import org.threeten.bp.ZoneId
import org.threeten.bp.format.DateTimeFormatter
import org.threeten.bp.format.DateTimeFormatterBuilder
import java.util.Locale

internal object SdkDateTimeFormatter : DateHelperContract.DateTimeFormatter {
    val DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern(DATE_FORMAT, Locale.US)
    val DATE_TIME_FORMATTER: DateTimeFormatter = DateTimeFormatterBuilder()
        .parseLenient()
        .appendPattern(DATE_TIME_FORMAT)
        .toFormatter(Locale.US)
    val UTC_ZONE_ID: ZoneId = ZoneId.of("UTC")
    private val UTC_DATE_TIME_FORMATTING: DateTimeFormatter = DATE_TIME_FORMATTER.withZone(
        UTC_ZONE_ID
    )

    override fun now(): String = formatDate(LocalDate.now(UTC_ZONE_ID))

    override fun formatDate(date: LocalDate): String = DATE_FORMATTER.format(date)

    override fun formatDateTime(
        dateTime: LocalDateTime
    ): String = "${UTC_DATE_TIME_FORMATTING.format(dateTime)}Z"

    override fun parseDate(date: String): LocalDate = LocalDate.parse(date, DATE_FORMATTER)

    override fun parseDateTime(
        dateTime: String
    ): LocalDateTime = LocalDateTime.parse(dateTime, DATE_TIME_FORMATTER)

    override fun buildMeta(
        record: DecryptedBaseRecord<*>
    ): ModelContract.Meta = Meta(
        LocalDate.parse(record.customCreationDate, DATE_FORMATTER),
        LocalDateTime.parse(record.updatedDate, DATE_TIME_FORMATTER)
    )
}
