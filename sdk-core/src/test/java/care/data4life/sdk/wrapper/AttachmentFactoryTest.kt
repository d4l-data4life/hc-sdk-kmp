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

package care.data4life.sdk.wrapper

import care.data4life.sdk.lang.CoreRuntimeException
import care.data4life.sdk.resource.Fhir3Attachment
import care.data4life.sdk.resource.Fhir4Attachment
import io.mockk.mockk
import org.junit.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class AttachmentFactoryTest {
    @Test
    fun `It fulfils AttachmentFactory`() {
        val factory: Any = SdkAttachmentFactory
        assertTrue(factory is WrapperFactoryContract.AttachmentFactory)
    }

    @Test
    fun `Given, wrap is called with a non FhirAttachment, it fails with a CoreRuntimeExceptionInternalFailure`() {
        assertFailsWith<CoreRuntimeException.InternalFailure> {
            SdkAttachmentFactory.wrap("fail me!")
        }
    }

    @Test
    fun `Given, wrap is called with a Fhir3Attachment, it returns a Attachment`() {
        // Given
        val givenAttachment: Fhir3Attachment = mockk()

        // When
        val wrapped: Any = SdkAttachmentFactory.wrap(givenAttachment)

        // Then
        assertTrue(wrapped is WrapperContract.Attachment)
    }

    @Test
    fun `Given, wrap is called with a Fhir4Attachment, it returns a Attachment`() {
        // Given
        val givenAttachment: Fhir4Attachment = mockk()

        // When
        val wrapped: Any = SdkAttachmentFactory.wrap(givenAttachment)

        // Then
        assertTrue(wrapped is WrapperContract.Attachment)
    }
}
