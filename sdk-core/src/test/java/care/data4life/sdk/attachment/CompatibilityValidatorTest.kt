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
import care.data4life.sdk.fhir.Fhir3Attachment
import care.data4life.sdk.fhir.Fhir3DateTimeParser
import care.data4life.sdk.wrapper.SdkFhir3Attachment
import care.data4life.sdk.wrapper.WrapperContract
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkClass
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CompatibilityValidatorTest {
    @Test
    fun `it is a FhirDateValidator`() {
        assertTrue( (CompatibilityValidator as Any) is AttachmentContract.CompatibilityValidator)
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
        val fhirDateTime = mockkClass(Fhir3DateTime::class)

        every { fhirDateTime.date } returns null

        fhirAttachment.creation = fhirDateTime

        // Then
        assertTrue(CompatibilityValidator.isHashable(SdkFhir3Attachment(fhirAttachment)))
    }

    @Test
    fun `Given, isHashable is called with a Attachment, which contains a null as Date, it returns true`() {
        // Given
        val fhirAttachment = Fhir3Attachment()
        val fhirDateTime = mockkClass(Fhir3DateTime::class)
        val fhirDate = mockkClass(Fhir3Date::class)

        every { fhirDateTime.date } returns fhirDate
        every { fhirDate.toDate() } returns null

        fhirAttachment.creation = fhirDateTime

        // Then
        assertTrue(CompatibilityValidator.isHashable(SdkFhir3Attachment(fhirAttachment)))
    }

    @Test
    fun `Given, isHashable is called with a Attachment, which contains a Fhir3Date before 2019-09-15, it returns false`() {
        // Given
        val fhirAttachment = Fhir3Attachment()
        val fhirDateTime = Fhir3DateTimeParser.parseDateTime(
                "2011-11-11"
        )

        fhirAttachment.creation = fhirDateTime

        // Then
        assertFalse(CompatibilityValidator.isHashable(SdkFhir3Attachment(fhirAttachment)))
    }

    @Test
    fun `Given, isHashable is called with a Attachment, which contains a Fhir3Date after 2019-09-15, it returns true`() {
        // Given
        val fhirAttachment = Fhir3Attachment()
        val fhirDateTime = Fhir3DateTimeParser.parseDateTime(
                "2021-12-21"
        )

        fhirAttachment.creation = fhirDateTime

        // Then
        assertTrue(CompatibilityValidator.isHashable(SdkFhir3Attachment(fhirAttachment)))
    }

    @Test
    fun `Given, isHashable is called with a Attachment, which does contain a non Fhir3Attachment, it returns true`() {
        val attachment = mockk<WrapperContract.Attachment>()

        every { attachment.unwrap<String>() } returns "not Fhir3"

        assertTrue(CompatibilityValidator.isHashable(attachment))
    }
}
