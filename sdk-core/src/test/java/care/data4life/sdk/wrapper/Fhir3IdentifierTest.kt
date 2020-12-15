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

import care.data4life.sdk.fhir.Fhir3Identifier
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class Fhir3IdentifierTest {
    @Test
    fun `it is a Identifier`() {
        val wrapper: Any = SdkFhir3Identifier(Fhir3Identifier())
        assertTrue(wrapper is WrapperContract.Identifier)
    }

    @Test
    fun `Given a wrapped Fhir3Identifier, it allows read access to value`() {
        // Given
        val value = "potato"
        val fhir3Identifier = Fhir3Identifier()
        fhir3Identifier.value  = value

        // When
        val result = SdkFhir3Identifier(fhir3Identifier).value

        // Then
        assertEquals(
                value,
                result
        )
    }

    @Test
    fun `Given a wrapped Fhir3Identifier, it allows write access to value`() {
        // Given
        val value = "potato"
        val fhir3Identifier = Fhir3Identifier()

        // When
        SdkFhir3Identifier(fhir3Identifier).value = value

        // Then
        assertEquals(
                value,
                fhir3Identifier.value
        )
    }
}
