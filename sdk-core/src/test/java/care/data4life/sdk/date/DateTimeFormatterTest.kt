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

import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.threeten.bp.LocalDate
import org.threeten.bp.LocalDateTime

class DateTimeFormatterTest {
    @Before
    fun setUp() {
        mockkStatic(LocalDate::class)
        mockkStatic(LocalDateTime::class)
    }

    @After
    fun tearDown() {
        unmockkStatic(LocalDate::class)
        unmockkStatic(LocalDateTime::class)
    }

    @Test
    fun `It fulfills DateTimeFormatter`() {
        val formatter: Any = SdkDateTimeFormatter
        assertTrue(formatter is DateHelperContract.DateTimeFormatter)
    }

    @Test
    fun `Given, formatDate is called with a LocalDate, it serializes the given date in a preset pattern`() {
        // Given
        val date = LocalDate.of(2020, 5, 3)

        // When
        val actual = SdkDateTimeFormatter.formatDate(date)

        // Then
        assertEquals(
            expected = "2020-05-03",
            actual = actual
        )
    }

    @Test
    fun `Given, formatDateTime is called with a LocalDateTime, it serializes the given date in a preset pattern`() {
        // Given
        val dateTime = LocalDateTime.of(
            2020,
            2,
            4,
            11,
            34,
            2,
            278000000
        )

        // When
        val actual = SdkDateTimeFormatter.formatDateTime(dateTime)

        // Then
        assertEquals(
            expected = "2020-02-04T11:34:02.278Z",
            actual = actual
        )
    }

    @Test
    fun `Given now is called, it returns the formatted date for now`() {
        // Given
        every {
            LocalDate.now(SdkDateTimeFormatter.UTC_ZONE_ID)
        } returns LocalDate.of(2020, 5, 3)

        // When
        val actual = SdkDateTimeFormatter.now()

        // Then
        assertEquals(
            expected = "2020-05-03",
            actual = actual
        )

        verify(exactly = 1) { LocalDate.now(SdkDateTimeFormatter.UTC_ZONE_ID) }
    }

    @Test
    fun `Given, parseDate is called with a String, it returns a LocalDate`() {
        // Given
        val date = "2020-05-03"

        // When
        val actual = SdkDateTimeFormatter.parseDate(date)

        // Then
        assertEquals(
            expected = LocalDate.of(2020, 5, 3),
            actual = actual
        )

        verify(exactly = 1) {
            LocalDate.parse(
                date,
                SdkDateTimeFormatter.DATE_FORMATTER
            )
        }
    }

    @Test
    fun `Given, parseDateTime is called with a String, it returns a LocalDateTime`() {
        // Given
        val dateTime = "2020-05-03T07:45:08.234123"

        // When
        val actual = SdkDateTimeFormatter.parseDateTime(dateTime)

        // Then
        assertEquals(
            expected = LocalDateTime.of(2020, 5, 3, 7, 45, 8, 234123000),
            actual = actual
        )

        verify(exactly = 1) {
            LocalDateTime.parse(
                dateTime,
                SdkDateTimeFormatter.DATE_TIME_FORMATTER
            )
        }
    }
}
