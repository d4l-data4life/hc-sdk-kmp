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
import care.data4life.sdk.fhir.Fhir4Attachment
import care.data4life.sdk.wrapper.SdkFhir3Attachment
import care.data4life.sdk.wrapper.SdkFhir4Attachment
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import care.data4life.fhir.stu3.model.FhirDate as Fhir3Date
import care.data4life.fhir.stu3.model.FhirDateTime as Fhir3DateTime

class CompatibilityValidatorTest {
    @Test
    fun `It fulfils FhirDateValidator`() {
        val validator: Any = CompatibilityValidator
        assertTrue(validator is AttachmentContract.CompatibilityValidator)
    }

    @Test
    fun `Given, isHashable is called with a Attachment, which contains a null as Fhir3DateTime, it returns true`() {
        // Given
        val fhirAttachment = Fhir3Attachment()

        fhirAttachment.creation = null

        // Then
        assertTrue(CompatibilityValidator.isHashable(SdkFhir3Attachment(fhirAttachment)))
    }

    @Test
    fun `Given, isHashable is called with a Attachment, which contains a null as Fhir3Date, it returns true`() {
        // Given
        val fhirAttachment = Fhir3Attachment()
        val fhirDateTime: Fhir3DateTime = mockk()

        every { fhirDateTime.date } returns null
        fhirAttachment.creation = fhirDateTime

        // When
        val result = CompatibilityValidator.isHashable(SdkFhir3Attachment(fhirAttachment))

        // Then
        assertTrue(result)
    }

    @Test
    fun `Given, isHashable is called with a Attachment, which contains a null as Date, it returns true`() {
        // Given
        val fhirAttachment = Fhir3Attachment()
        val fhirDateTime: Fhir3DateTime = mockk()
        val fhirDate: Fhir3Date = mockk()

        every { fhirDate.toDate() } returns null
        every { fhirDateTime.date } returns fhirDate
        fhirAttachment.creation = fhirDateTime

        // When
        val result = CompatibilityValidator.isHashable(SdkFhir3Attachment(fhirAttachment))

        // Then
        assertTrue(result)
    }

    @Test
    fun `Given, isHashable is called with a Attachment, which contains a Fhir3Date before 2019-09-15, it returns false`() {
        // Given
        val fhirAttachment = Fhir3Attachment()
        val fhirDateTime = Fhir3DateTimeParser.parseDateTime(
            "2011-11-11"
        )

        fhirAttachment.creation = fhirDateTime

        // When
        val result = CompatibilityValidator.isHashable(SdkFhir3Attachment(fhirAttachment))

        // Then
        assertFalse(result)
    }

    @Test
    fun `Given, isHashable is called with a Attachment, which contains a Fhir3Date after 2019-09-15, it returns true`() {
        // Given
        val fhirAttachment = Fhir3Attachment()
        val fhirDateTime = Fhir3DateTimeParser.parseDateTime(
            "2021-12-21"
        )

        fhirAttachment.creation = fhirDateTime

        // When
        val result = CompatibilityValidator.isHashable(SdkFhir3Attachment(fhirAttachment))

        // Then
        assertTrue(result)
    }

    @Test
    fun `Given, isHashable is called with a Attachment, which does contain a non Fhir3Attachment, it returns true`() {
        // Given
        val fhirAttachment: Fhir4Attachment = spyk()

        // When
        val result = CompatibilityValidator.isHashable(SdkFhir4Attachment(fhirAttachment))

        // Then
        assertTrue(result)
    }
}
