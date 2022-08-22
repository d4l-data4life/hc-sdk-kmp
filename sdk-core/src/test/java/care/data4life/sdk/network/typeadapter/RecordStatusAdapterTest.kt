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

package care.data4life.sdk.network.typeadapter

import care.data4life.sdk.lang.CoreRuntimeException
import care.data4life.sdk.model.ModelContract
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import org.junit.Test

class RecordStatusAdapterTest {
    @Test
    fun `Given toJson is called with an RecordStatus it returns its identifier`() {
        // Given
        val statuus = mapOf(
            ModelContract.RecordStatus.Active.id to ModelContract.RecordStatus.Active,
            ModelContract.RecordStatus.Pending.id to ModelContract.RecordStatus.Pending,
            ModelContract.RecordStatus.Deleted.id to ModelContract.RecordStatus.Deleted
        )

        val adapter = RecordStatusAdapter()

        statuus.forEach { (identifier, status) ->
            // When
            val actual = adapter.toJson(status)

            // Then
            assertEquals(
                actual = actual,
                expected = identifier
            )
        }
    }

    @Test
    fun `Given formJson is called with a String which is a unknown Status it fails`() {
        // Given
        val status = "None"

        val adapter = RecordStatusAdapter()

        val error = assertFailsWith<CoreRuntimeException.Default> {
            adapter.fromJson(status)
        }

        assertEquals(
            actual = error.message,
            expected = "Unknown record status None."
        )
    }

    @Test
    fun `Given formJson is called with a String it returns a RecordStatus`() {
        // Given
        val statuus = mapOf(
            ModelContract.RecordStatus.Active.id to ModelContract.RecordStatus.Active,
            ModelContract.RecordStatus.Pending.id to ModelContract.RecordStatus.Pending,
            ModelContract.RecordStatus.Deleted.id to ModelContract.RecordStatus.Deleted
        )

        val adapter = RecordStatusAdapter()

        statuus.forEach { (identifier, status) ->
            // When
            val actual = adapter.fromJson(identifier)

            // Then
            assertEquals(
                actual = actual,
                expected = status
            )
        }
    }
}
