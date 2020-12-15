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

import care.data4life.sdk.data.DataResource
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class DataResourceTest {
    @Test
    fun `it is a Resource`() {
        assertTrue((SdkDataResource(mockk()) as Any) is WrapperContract.Resource)
    }

    @Test
    fun `Given a wrapped DataResource, it has the type Data`() {
        assertEquals(
                SdkDataResource(mockk()).type,
                WrapperContract.Resource.TYPE.DATA
        )
    }

    @Test
    fun `Given a wrapped DataResource, it allows read access to id`() {
        // Given
        val id = ""
        val resource = DataResource(ByteArray(12))

        // When
        val result = SdkDataResource(resource).identifier

        // Then
        assertEquals(
                id,
                result
        )
    }

    @Test
    fun `Given, unwrap is called, it returns the wrapped DataResource`() {
        // Given
        val resource = DataResource(ByteArray(12))

        // Then
        assertSame(
                SdkDataResource(resource).unwrap(),
                resource
        )
    }
}
