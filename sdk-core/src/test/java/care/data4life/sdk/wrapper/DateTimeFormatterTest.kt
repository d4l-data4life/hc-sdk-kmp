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

package care.data4life.sdk.wrapper

import care.data4life.sdk.network.model.DecryptedDataRecord
import care.data4life.sdk.network.model.DecryptedR4Record
import care.data4life.sdk.network.model.DecryptedRecord
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.threeten.bp.LocalDate
import org.threeten.bp.LocalDateTime
import kotlin.test.assertEquals
import kotlin.test.assertTrue

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
        assertTrue(formatter is WrapperContract.DateTimeFormatter)
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
    fun `Given, buildMeta is called with a DecryptedFhir3Record, it returns a Meta object`() {
        // Given
        val creationDate = "2020-05-03"
        val updateDate = "2020-05-03T07:45:08.234123"
        val record: DecryptedRecord<*> = mockk()

        every { record.customCreationDate } returns creationDate
        every { record.updatedDate } returns updateDate

        // When
        val actual = SdkDateTimeFormatter.buildMeta(record)

        // Then
        assertEquals(
            expected = LocalDate.of(2020, 5, 3),
            actual = actual.createdDate
        )

        assertEquals(
            expected = LocalDateTime.of(2020, 5, 3, 7, 45, 8, 234123000),
            actual = actual.updatedDate
        )
    }

    @Test
    fun `Given, buildMeta is called with a DecryptedFhir43Record, it returns a Meta object`() {
        // Given
        val creationDate = "2020-05-03"
        val updateDate = "2020-05-03T07:45:08.234123"
        val record: DecryptedR4Record<*> = mockk()

        every { record.customCreationDate } returns creationDate
        every { record.updatedDate } returns updateDate

        // When
        val actual = SdkDateTimeFormatter.buildMeta(record)

        // Then
        assertEquals(
            expected = LocalDate.of(2020, 5, 3),
            actual = actual.createdDate
        )

        assertEquals(
            expected = LocalDateTime.of(2020, 5, 3, 7, 45, 8, 234123000),
            actual = actual.updatedDate
        )

        verify(exactly = 1) {
            LocalDate.parse(
                creationDate,
                SdkDateTimeFormatter.DATE_FORMATTER
            )
        }
        verify(exactly = 1) {
            LocalDateTime.parse(
                updateDate,
                SdkDateTimeFormatter.DATE_TIME_FORMATTER
            )
        }
    }

    @Test
    fun `Given, buildMeta is called with a DecryptedDataRecord, it returns a Meta object`() {
        // Given
        val creationDate = "2020-05-03"
        val updateDate = "2020-05-03T07:45:08.234123"
        val record: DecryptedDataRecord = mockk()

        every { record.customCreationDate } returns creationDate
        every { record.updatedDate } returns updateDate

        // When
        val actual = SdkDateTimeFormatter.buildMeta(record)

        // Then
        assertEquals(
            expected = LocalDate.of(2020, 5, 3),
            actual = actual.createdDate
        )

        assertEquals(
            expected = LocalDateTime.of(2020, 5, 3, 7, 45, 8, 234123000),
            actual = actual.updatedDate
        )

        verify(exactly = 1) {
            LocalDate.parse(
                creationDate,
                SdkDateTimeFormatter.DATE_FORMATTER
            )
        }
        verify(exactly = 1) {
            LocalDateTime.parse(
                updateDate,
                SdkDateTimeFormatter.DATE_TIME_FORMATTER
            )
        }
    }
}
