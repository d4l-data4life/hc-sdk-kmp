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

import care.data4life.fhir.stu3.model.FhirDateTime as Fhir3DateTime
import care.data4life.fhir.stu3.model.FhirDate as Fhir3Date
import care.data4life.fhir.stu3.util.FhirDateTimeParser
import care.data4life.sdk.fhir.Fhir3Attachment
import care.data4life.sdk.wrappers.SdkFhir3Attachment
import io.mockk.every
import io.mockk.mockkClass
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FhirDateValidationTest {
    @Test
    fun `it is a FhirDateValidator`() {
        assertTrue( (FhirDateValidator as Any) is AttachmentContract.FhirDateValidator)
    }

    @Test
    fun `Given, isInvalidDate is called with a Attachment, which contains a null as FhirDateTime, it returns true`() {
        // Given
        val fhirAttachment = Fhir3Attachment()

        fhirAttachment.creation = null

        // Then
        assertTrue(FhirDateValidator.isInvalidateDate(SdkFhir3Attachment(fhirAttachment)))
    }

    @Test
    fun `Given, isInvalidDate is called with a Attachment, which contains a null as FhirDate, it returns true`() {
        // Given
        val fhirAttachment = Fhir3Attachment()
        val fhirDateTime = mockkClass(Fhir3DateTime::class)

        every { fhirDateTime.date } returns null

        fhirAttachment.creation = fhirDateTime

        // Then
        assertTrue(FhirDateValidator.isInvalidateDate(SdkFhir3Attachment(fhirAttachment)))
    }

    @Test
    fun `Given, isInvalidDate is called with a Attachment, which contains a null as Date, it returns false`() {
        // Given
        val fhirAttachment = Fhir3Attachment()
        val fhirDateTime = mockkClass(Fhir3DateTime::class)
        val fhirDate = mockkClass(Fhir3Date::class)

        every { fhirDateTime.date } returns fhirDate
        every { fhirDate.toDate() } returns null

        fhirAttachment.creation = fhirDateTime

        // Then
        assertTrue(FhirDateValidator.isInvalidateDate(SdkFhir3Attachment(fhirAttachment)))
    }

    @Test
    fun `Given, isInvalidDate is called with a Attachment, which contains a FhirDate before 2019-09-15, it returns false`() {
        // Given
        val fhirAttachment = Fhir3Attachment()
        val fhirDateTime = FhirDateTimeParser.parseDateTime(
                "2011-11-11"
        )

        fhirAttachment.creation = fhirDateTime

        // Then
        assertFalse(FhirDateValidator.isInvalidateDate(SdkFhir3Attachment(fhirAttachment)))
    }

    @Test
    fun `Given, isInvalidDate is called with a Attachment, which contains a FhirDate after 2019-09-15, it returns true`() {
        // Given
        val fhirAttachment = Fhir3Attachment()
        val fhirDateTime = FhirDateTimeParser.parseDateTime(
                "2021-12-21"
        )

        fhirAttachment.creation = fhirDateTime

        // Then
        assertTrue(FhirDateValidator.isInvalidateDate(SdkFhir3Attachment(fhirAttachment)))
    }
}
