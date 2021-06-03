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

package care.data4life.sdk.wrapper

import care.data4life.sdk.attachment.AttachmentContract
import care.data4life.sdk.lang.ImageResizeException
import care.data4life.sdk.log.Log
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import io.mockk.verify
import org.junit.Before
import org.junit.Test
import kotlin.test.assertSame
import kotlin.test.assertTrue

class SDKResizerTest {
    private val actualResizer: AttachmentContract.ImageResizer = mockk()
    private lateinit var wrapper: WrapperContract.SDKImageResizer

    @Before
    fun setUp() {
        wrapper = SDKImageResizer(actualResizer)
    }

    @Test
    fun `It fulfils SDKImageResizer`() {
        val wrapper: Any = SDKImageResizer(mockk())

        assertTrue(wrapper is WrapperContract.SDKImageResizer)
    }

    @Test
    fun `Given isResizable is called with a Payload it delegates and returns the result of the wrapped resizer`() {
        // Given
        val payload = ByteArray(42)
        val expected = true

        every { actualResizer.isResizable(payload) } returns expected

        // When
        val result = wrapper.isResizable(payload)

        // Then
        assertSame(
            actual = result,
            expected = expected
        )
        verify(exactly = 1) { actualResizer.isResizable(payload) }
    }

    @Test
    fun `Given resize is called with a Payload and a Height it delegates the call to the wrapped resizer and returns resized image`() {
        // Given
        val payload = ByteArray(42)
        val height = 7
        val downsizedImage = ByteArray(23)

        every {
            actualResizer.resizeToHeight(
                payload,
                height,
                AttachmentContract.ImageResizer.DEFAULT_JPEG_QUALITY_PERCENT
            )
        } returns downsizedImage

        // When
        val result = wrapper.resize(payload, height)

        // Then
        assertSame(
            actual = result,
            expected = downsizedImage
        )
    }

    @Test
    fun `Given resize is called with a Payload and a Height it logs and returns INVALID_DOWNSCALED_IMAGE if the wrapped resizer fails with a ImageResizeException`() {
        // Given
        mockkObject(Log)

        val payload = ByteArray(42)
        val height = 7
        val error = ImageResizeException.JpegWriterMissing()

        every {
            actualResizer.resizeToHeight(
                payload,
                height,
                AttachmentContract.ImageResizer.DEFAULT_JPEG_QUALITY_PERCENT
            )
        } throws error

        // When
        val result = wrapper.resize(payload, height)

        // Then
        assertSame(
            actual = result,
            expected = payload
        )

        verify(exactly = 1) { Log.error(error, error.message) }

        unmockkObject(Log)
    }
}
