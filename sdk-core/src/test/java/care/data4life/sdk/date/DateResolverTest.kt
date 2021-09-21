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
import io.mockk.every
import io.mockk.mockkObject
import io.mockk.unmockkObject
import io.mockk.verify
import org.junit.Test
import org.threeten.bp.LocalDate
import org.threeten.bp.LocalDateTime
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DateResolverTest {
    @Test
    fun `It fulfils DateResolver`() {
        val resolver: Any = DateResolver

        assertTrue(resolver is DateHelperContract.DateResolver)
    }

    @Test
    fun `Given resolveCreationDate is called with a CreationDateRange, which is null it returns a Pair of null`() {
        // When
        val result = DateResolver.resolveCreationDate(null)

        // Then
        assertEquals(
            actual = result,
            expected = Pair(null, null)
        )
    }

    @Test
    fun `Given resolveCreationDate is called with a CreationDateRange, which contains a startDate it returns a Pair with a formatted StartDate and null`() {
        mockkObject(SdkDateTimeFormatter)

        // Given
        val startDate = LocalDate.now()
        val formattedStartDate = "start"

        val creationDateRange = SdkContract.CreationDateRange(
            startDate = startDate,
            endDate = null
        )

        every { SdkDateTimeFormatter.formatDate(startDate) } returns formattedStartDate

        // When
        val result = DateResolver.resolveCreationDate(creationDateRange)

        // Then
        assertEquals(
            actual = result,
            expected = Pair(formattedStartDate, null)
        )

        verify(atMost = 1) { SdkDateTimeFormatter.formatDate(startDate) }

        unmockkObject(SdkDateTimeFormatter)
    }

    @Test
    fun `Given resolveCreationDate is called with a CreationDateRange, which contains a endDate it returns a Pair with a formatted EndDate and null`() {
        mockkObject(SdkDateTimeFormatter)

        // Given
        val endDate = LocalDate.now()
        val formattedEndDate = "end"

        val creationDateRange = SdkContract.CreationDateRange(
            endDate = endDate,
            startDate = null
        )

        every { SdkDateTimeFormatter.formatDate(endDate) } returns formattedEndDate

        // When
        val result = DateResolver.resolveCreationDate(creationDateRange)

        // Then
        assertEquals(
            actual = result,
            expected = Pair(null, formattedEndDate)
        )

        verify(atMost = 1) { SdkDateTimeFormatter.formatDate(endDate) }

        unmockkObject(SdkDateTimeFormatter)
    }

    @Test
    fun `Given resolveCreationDate is called with a CreationDateRange, which contains a startDate and endDate it returns a Pair with a formatted Start and EndDate and null`() {
        mockkObject(SdkDateTimeFormatter)

        // Given
        val startDate = LocalDate.now()
        val formattedStartDate = "start"

        val endDate = LocalDate.parse("2020-05-23")
        val formattedEndDate = "end"

        val creationDateRange = SdkContract.CreationDateRange(
            startDate = startDate,
            endDate = endDate
        )

        every { SdkDateTimeFormatter.formatDate(startDate) } returns formattedStartDate
        every { SdkDateTimeFormatter.formatDate(endDate) } returns formattedEndDate

        // When
        val result = DateResolver.resolveCreationDate(creationDateRange)

        // Then
        assertEquals(
            actual = result,
            expected = Pair(formattedStartDate, formattedEndDate)
        )

        verify(atMost = 1) { SdkDateTimeFormatter.formatDate(startDate) }
        verify(atMost = 1) { SdkDateTimeFormatter.formatDate(endDate) }

        unmockkObject(SdkDateTimeFormatter)
    }

    @Test
    fun `Given resolveUpdateDate is called with a UpdateDateTimeRange, which is null it returns a Pair of null`() {
        // When
        val result = DateResolver.resolveUpdateDate(null)

        // Then
        assertEquals(
            actual = result,
            expected = Pair(null, null)
        )
    }

    @Test
    fun `Given resolveUpdateDate is called with a UpdateDateTimeRange, which contains a startDateTime it returns a Pair with a formatted StartDateTime and null`() {
        mockkObject(SdkDateTimeFormatter)

        // Given
        val startDateTime = LocalDateTime.now()
        val formattedStartDateTime = "start"

        val updateDateRange = SdkContract.UpdateDateTimeRange(
            startDateTime = startDateTime,
            endDateTime = null
        )

        every { SdkDateTimeFormatter.formatDateTime(startDateTime) } returns formattedStartDateTime

        // When
        val result = DateResolver.resolveUpdateDate(updateDateRange)

        // Then
        assertEquals(
            actual = result,
            expected = Pair(formattedStartDateTime, null)
        )

        verify(atMost = 1) { SdkDateTimeFormatter.formatDateTime(startDateTime) }

        unmockkObject(SdkDateTimeFormatter)
    }

    @Test
    fun `Given resolveUpdateDate is called with a UpdateDateTimeRange, which contains a endDateTime it returns a Pair with a formatted EndDateTime and null`() {
        mockkObject(SdkDateTimeFormatter)

        // Given
        val endDateTime = LocalDateTime.now()
        val formattedEndDateTime = "end"

        val updateDateRange = SdkContract.UpdateDateTimeRange(
            startDateTime = null,
            endDateTime = endDateTime
        )

        every { SdkDateTimeFormatter.formatDateTime(endDateTime) } returns formattedEndDateTime

        // When
        val result = DateResolver.resolveUpdateDate(updateDateRange)

        // Then
        assertEquals(
            actual = result,
            expected = Pair(null, formattedEndDateTime)
        )

        verify(atMost = 1) { SdkDateTimeFormatter.formatDateTime(endDateTime) }

        unmockkObject(SdkDateTimeFormatter)
    }

    @Test
    fun `Given resolveUpdateDate is called with a UpdateDateTimeRange, which contains a startDateTime and endDateTime it returns a Pair with a formatted StartDateTime and EndDateTime and null`() {
        mockkObject(SdkDateTimeFormatter)

        // Given
        val startDateTime = LocalDateTime.of(30, 12, 24, 0, 0)
        val formattedStartDateTime = "start"
        val endDateTime = LocalDateTime.now()
        val formattedEndDateTime = "end"

        val updateDateRange = SdkContract.UpdateDateTimeRange(
            startDateTime = startDateTime,
            endDateTime = endDateTime
        )

        every { SdkDateTimeFormatter.formatDateTime(startDateTime) } returns formattedStartDateTime
        every { SdkDateTimeFormatter.formatDateTime(endDateTime) } returns formattedEndDateTime

        // When
        val result = DateResolver.resolveUpdateDate(updateDateRange)

        // Then
        assertEquals(
            actual = result,
            expected = Pair(formattedStartDateTime, formattedEndDateTime)
        )

        verify(atMost = 1) { SdkDateTimeFormatter.formatDateTime(startDateTime) }
        verify(atMost = 1) { SdkDateTimeFormatter.formatDateTime(endDateTime) }

        unmockkObject(SdkDateTimeFormatter)
    }
}
