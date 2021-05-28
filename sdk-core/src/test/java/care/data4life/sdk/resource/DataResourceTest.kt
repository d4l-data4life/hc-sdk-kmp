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

package care.data4life.sdk.resource

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotSame
import kotlin.test.assertSame
import kotlin.test.assertTrue

class DataResourceTest {
    @Test
    fun `It fulfils DataResource`() {
        val resource: Any = DataResource(ByteArray(0))

        assertTrue(resource is DataResource)
    }

    @Test
    fun `It exposes its value`() {
        // Given
        val value = ByteArray(23)

        // When
        val resource = DataResource(value)

        // Then
        assertSame(
            actual = resource.value,
            expected = value
        )
    }

    @Test
    fun `Given asByteArray is called it returns a clone of the value`() {
        // Given
        val value = ByteArray(23)

        // When
        val resource = DataResource(value)

        // Then
        assertNotSame(
            actual = resource.asByteArray(),
            illegal = value
        )

        assertTrue(value.contentEquals(resource.asByteArray()))
    }

    @Test
    fun `Given hashCode is called it returns the content hash code of the wrapped ByteArray`() {
        // Given
        val value = ByteArray(23)

        // When
        val resource = DataResource(value)

        // Then
        assertEquals(
            expected = value.contentHashCode(),
            actual = resource.hashCode()
        )
    }

    @Test
    fun `It does not equals non DataResources`() {
        // Given
        val value = ByteArray(23)

        // When
        val resource = DataResource(value)

        // Then
        assertFalse(resource.equals(value))
    }

    @Test
    fun `It does not equals if the given value differs`() {
        // Given
        val value = ByteArray(23)

        // When
        val resource = DataResource(ByteArray(24))

        // Then
        assertFalse(resource.equals(value))
    }

    @Test
    fun `It equals if the values are equal`() {
        // Given
        val value = ByteArray(23)

        // When
        val resource = DataResource(value.clone())

        // Then
        assertEquals(
            resource,
            DataResource(value)
        )
    }
    @Test
    fun `It equals if the values are equal and the given Resource is derived from Resource`() {
        @Suppress("ArrayInDataClass")
        data class ShallowDataResource(override val value: ByteArray) :
            ResourceContract.DataResource {
            override fun asByteArray(): ByteArray = TODO("Not yet implemented")
        }

        // Given
        val value = ByteArray(23)

        // When
        val resource = DataResource(value.clone())

        // Then
        assertTrue(resource.equals(ShallowDataResource(value)))
    }
}
