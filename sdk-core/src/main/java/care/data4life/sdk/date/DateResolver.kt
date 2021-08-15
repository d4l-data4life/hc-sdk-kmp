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

import care.data4life.sdk.SdkContract
import org.threeten.bp.LocalDate
import org.threeten.bp.LocalDateTime

internal object DateResolver : DateHelperContract.DateResolver {
    private fun formatDate(date: LocalDate?): String? {
        return if (date is LocalDate) {
            SdkDateTimeFormatter.formatDate(date)
        } else {
            null
        }
    }

    private fun resolveCreationDateRange(
        creationDateRange: SdkContract.CreationDateRange
    ): Pair<String?, String?> {
        return Pair(
            formatDate(creationDateRange.startDate),
            formatDate(creationDateRange.endDate)
        )
    }

    override fun resolveCreationDate(creationDateRange: SdkContract.CreationDateRange?): Pair<String?, String?> {
        return if (creationDateRange is SdkContract.CreationDateRange) {
            resolveCreationDateRange(creationDateRange)
        } else {
            null to null
        }
    }

    private fun formatDateTime(dateTime: LocalDateTime?): String? {
        return if (dateTime is LocalDateTime) {
            SdkDateTimeFormatter.formatDateTime(dateTime)
        } else {
            null
        }
    }

    private fun resolveUpdateDateRange(updateDateRange: SdkContract.UpdateDateTimeRange): Pair<String?, String?> {
        return Pair(
            formatDateTime(updateDateRange.startDateTime),
            formatDateTime(updateDateRange.endDateTime)
        )
    }

    override fun resolveUpdateDate(
        updateDateTimeRange: SdkContract.UpdateDateTimeRange?
    ): Pair<String?, String?> {
        return if (updateDateTimeRange is SdkContract.UpdateDateTimeRange) {
            resolveUpdateDateRange(updateDateTimeRange)
        } else {
            null to null
        }
    }
}
