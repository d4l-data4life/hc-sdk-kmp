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

import care.data4life.sdk.fhir.Fhir3Resource
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class Fhir3ResourceTest {
    @Test
    fun `it is a Resource`() {
        assertTrue((SdkFhir3Resource(mockk()) as Any) is WrapperContract.Resource)
    }

    @Test
    fun `Given a wrapped Fhir3Resource, it has the type FHIR3`() {
        assertEquals(
                SdkFhir3Resource(mockk()).type,
                WrapperContract.Resource.TYPE.FHIR3
        )
    }

    @Test
    fun `Given a wrapped Fhir3Resource, it allows read access to id`() {
        // Given
        val id = "soup"
        val fhir3Resource = Fhir3Resource()
        fhir3Resource.id = id

        // When
        val result = SdkFhir3Resource(fhir3Resource).identifier

        // Then
        assertEquals(
                id,
                result
        )
    }

    @Test
    fun `Given a wrapped Fhir3Resource, it allows write access to id`() {
        // Given
        val newId = "tomato"
        val fhir3Resource = Fhir3Resource()

        // When
        SdkFhir3Resource(fhir3Resource).identifier = newId

        // Then
        assertEquals(
                newId,
                fhir3Resource.id
        )
    }

    @Test
    fun `Given, unwrap is called, it returns the wrapped Fhir3Resource`() {
        // Given
        val fhir3Resource = Fhir3Resource()

        // Then
        assertSame(
                SdkFhir3Resource(fhir3Resource).unwrap(),
                fhir3Resource
        )
    }
}
