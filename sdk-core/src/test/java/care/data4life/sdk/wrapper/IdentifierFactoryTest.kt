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

import care.data4life.sdk.fhir.Fhir3AttachmentHelper
import care.data4life.sdk.fhir.Fhir4Identifier
import care.data4life.sdk.lang.CoreRuntimeException
import org.junit.Assert.assertTrue
import org.junit.Test

class IdentifierFactoryTest {
    @Test
    fun `it is a AttachmentFactory`() {
        assertTrue((SdkIdentifierFactory as Any) is WrapperFactoryContract.IdentifierFactory)
    }

    @Test
    fun `Given, wrap is called with a non FhirIdentifier, it fails with a CoreRuntimeExceptionInternalFailure`() {
        try {
            // When
            SdkIdentifierFactory.wrap("fail me!")
            assertTrue(false)//Fixme
        } catch (e: Exception) {
            // Then
            assertTrue(e is CoreRuntimeException.InternalFailure)
        }
    }

    @Test
    fun `Given, wrap is called with a Fhir3Identifier, it returns a Attachment`() {
        // Given
        val givenIdentifier = Fhir3AttachmentHelper.buildIdentifier("id", "me")

        // When
        val wrapped: Any = SdkIdentifierFactory.wrap(givenIdentifier)

        // Then
        assertTrue(wrapped is WrapperContract.Identifier)
    }

    @Test
    fun `Given, wrap is called with a Fhir4Identifier, it returns a Attachment`() {
        // When
        val wrapped: Any = SdkIdentifierFactory.wrap(Fhir4Identifier())

        // Then
        assertTrue(wrapped is WrapperContract.Identifier)
    }
}
