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

import care.data4life.sdk.fhir.Fhir3Attachment
import care.data4life.sdk.fhir.Fhir3AttachmentHelper
import care.data4life.sdk.fhir.Fhir3Identifier
import care.data4life.sdk.fhir.Fhir3Resource
import care.data4life.sdk.fhir.Fhir4Attachment
import care.data4life.sdk.fhir.Fhir4AttachmentHelper
import care.data4life.sdk.fhir.Fhir4Identifier
import care.data4life.sdk.fhir.Fhir4Resource
import care.data4life.sdk.lang.CoreRuntimeException
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue
import care.data4life.fhir.r4.model.DocumentReference as Fhir4DocumentReference
import care.data4life.fhir.stu3.model.DocumentReference as Fhir3DocumentReference

class FhirAttachmentHelperTest {
    @Before
    fun setUp() {
        mockkStatic(Fhir3AttachmentHelper::class)
        mockkStatic(Fhir4AttachmentHelper::class)
    }

    @After
    fun tearDown() {
        unmockkStatic(Fhir3AttachmentHelper::class)
        unmockkStatic(Fhir4AttachmentHelper::class)
    }

    @Test
    fun `It fulfils FhirAttachmentHelper`() {
        val helper: Any = SdkFhirAttachmentHelper
        assertTrue(helper is WrapperInternalContract.FhirAttachmentHelper)
    }

    @Test
    fun `Given, hasAttachment is called with a Fhir3Resource, it delegates to and returns of the wrapped Fhir3AttachmentHelper`() {
        // Given
        val resource1: Fhir3DocumentReference = mockk()
        val resource2: Fhir3DocumentReference = mockk()

        every { Fhir3AttachmentHelper.hasAttachment(resource1) } returns false
        every { Fhir3AttachmentHelper.hasAttachment(resource2) } returns true

        // Then
        assertFalse(SdkFhirAttachmentHelper.hasAttachment(resource1))
        assertTrue(SdkFhirAttachmentHelper.hasAttachment(resource2))

        verify(exactly = 1) { Fhir3AttachmentHelper.hasAttachment(resource1) }
        verify(exactly = 1) { Fhir3AttachmentHelper.hasAttachment(resource2) }
    }

    @Test
    fun `Given, hasAttachment is called with a Fhir4Resource, it delegates to and returns of the wrapped Fhir4AttachmentHelper`() {
        // Given
        val resource1: Fhir4DocumentReference = mockk()
        val resource2: Fhir4DocumentReference = mockk()

        every { Fhir4AttachmentHelper.hasAttachment(resource1) } returns false
        every { Fhir4AttachmentHelper.hasAttachment(resource2) } returns true

        // Then
        assertFalse(SdkFhirAttachmentHelper.hasAttachment(resource1))
        assertTrue(SdkFhirAttachmentHelper.hasAttachment(resource2))

        verify(exactly = 1) { Fhir4AttachmentHelper.hasAttachment(resource1) }
        verify(exactly = 1) { Fhir4AttachmentHelper.hasAttachment(resource2) }
    }

    @Test
    fun `Given, hasAttachment is called with a non FhirResource, it fails`() {
        assertFailsWith<CoreRuntimeException.InternalFailure> {
            SdkFhirAttachmentHelper.hasAttachment("fail me!")
        }
    }

    @Test
    fun `Given, getAttachment is called with Fhir3Resource, it delegates to and returns of the wrapped Fhir3AttachmentHelper`() {
        // Given
        val resource: Fhir3DocumentReference = mockk()
        val attachments: MutableList<Fhir3Attachment> = mockk()

        every { Fhir3AttachmentHelper.getAttachment(resource) } returns attachments

        // Then
        assertSame(
            attachments as MutableList<*>,
            SdkFhirAttachmentHelper.getAttachment(resource) as MutableList<*>
        )

        verify(exactly = 1) { Fhir3AttachmentHelper.getAttachment(resource) }
    }

    @Test
    fun `Given, getAttachment is called with Fhir3Resource and the response of the wrapped Fhir3AttachmentHelper is null, it returns null`() {
        // Given
        val resource: Fhir3DocumentReference = mockk()

        every { Fhir3AttachmentHelper.getAttachment(resource) } returns null

        // When
        val result = SdkFhirAttachmentHelper.getAttachment(resource)

        // Then
        assertNull(result)

        verify(exactly = 1) { Fhir3AttachmentHelper.getAttachment(resource) }
    }

    @Test
    fun `Given, getAttachment is called with Fhir4Resource, it delegates to and returns of the wrapped Fhir4AttachmentHelper`() {
        // Given
        val resource: Fhir4DocumentReference = mockk()
        val attachments: MutableList<Fhir4Attachment> = mockk()

        every { Fhir4AttachmentHelper.getAttachment(resource) } returns attachments

        // Then
        assertSame(
            attachments as MutableList<*>,
            SdkFhirAttachmentHelper.getAttachment(resource) as MutableList<*>
        )

        verify(exactly = 1) { Fhir4AttachmentHelper.getAttachment(resource) }
    }

    @Test
    fun `Given, getAttachment is called with Fhir4Resource and the response of the wrapped Fhir4AttachmentHelper is null, it returns null`() {
        // Given
        val resource: Fhir4DocumentReference = mockk()

        every { Fhir4AttachmentHelper.getAttachment(resource) } returns null

        // When
        val result = SdkFhirAttachmentHelper.getAttachment(resource)

        // Then
        assertNull(result)

        verify(exactly = 1) { Fhir4AttachmentHelper.getAttachment(resource) }
    }

    @Test
    fun `Given, getAttachment is called with a non FhirResource, it fails`() {
        assertFailsWith<CoreRuntimeException.InternalFailure> {
            SdkFhirAttachmentHelper.getAttachment("fail me!")
        }
    }

    @Test
    fun `Given, updateAttachmentData is called with a Fhir3Resource and null as Attachment, it delegates it to the Fhir3AttachmentHelper`() {
        // Given
        val resource: Fhir3DocumentReference = mockk()

        every { Fhir3AttachmentHelper.updateAttachmentData(resource, null) } just Runs

        // When
        SdkFhirAttachmentHelper.updateAttachmentData(resource, null)

        // Then
        verify(exactly = 1) { Fhir3AttachmentHelper.updateAttachmentData(resource, null) }
    }

    @Test
    fun `Given, updateAttachmentData is called with a Fhir3Resource and a empty HashMap as Attachment, it delegates it to the Fhir3AttachmentHelper`() {
        // Given
        val resource: Fhir3DocumentReference = mockk()
        val data: HashMap<Fhir3Attachment, String> = mockk()

        every { Fhir3AttachmentHelper.updateAttachmentData(resource, data) } just Runs

        // When
        @Suppress("UNCHECKED_CAST")
        SdkFhirAttachmentHelper.updateAttachmentData(resource, data as HashMap<Any, String?>)

        // Then
        verify(exactly = 1) { Fhir3AttachmentHelper.updateAttachmentData(resource, data) }
    }

    @Test
    fun `Given, updateAttachmentData is called with a Fhir3Resource and a HashMap Attachment to String, it delegates to and returns of the wrapped Fhir3AttachmentHelper`() {
        // Given
        val resource: Fhir3DocumentReference = mockk()

        val attachments = hashMapOf(
            mockk<Fhir3Attachment>() to "1",
            mockk<Fhir3Attachment>() to "2"
        )

        every {
            Fhir3AttachmentHelper.updateAttachmentData(resource, attachments)
        } just Runs

        // When
        @Suppress("UNCHECKED_CAST")
        SdkFhirAttachmentHelper.updateAttachmentData(resource, attachments as HashMap<Any, String?>)

        // Then
        verify(exactly = 1) {
            Fhir3AttachmentHelper.updateAttachmentData(resource, attachments)
        }
    }

    @Test
    fun `Given, updateAttachmentData is called with a Fhir4Resource and null as Attachment, it delegates it to the Fhir4AttachmentHelper`() {
        // Given
        val resource: Fhir4DocumentReference = mockk()

        every { Fhir4AttachmentHelper.updateAttachmentData(resource, null) } just Runs

        // When
        SdkFhirAttachmentHelper.updateAttachmentData(resource, null)

        // Then
        verify(exactly = 1) { Fhir4AttachmentHelper.updateAttachmentData(resource, null) }
    }

    @Test
    fun `Given, updateAttachmentData is called with a Fhir4Resource and a empty HashMap as Attachment, it delegates it to the Fhir4AttachmentHelper`() {
        // Given
        val resource: Fhir4DocumentReference = mockk()
        val data: HashMap<Fhir4Attachment, String> = mockk()

        every { Fhir4AttachmentHelper.updateAttachmentData(resource, data) } just Runs

        // When
        @Suppress("UNCHECKED_CAST")
        SdkFhirAttachmentHelper.updateAttachmentData(resource, data as HashMap<Any, String?>)

        // Then
        verify(exactly = 1) { Fhir4AttachmentHelper.updateAttachmentData(resource, data) }
    }

    @Test
    fun `Given, updateAttachmentData is called with a Fhir4Resource and a HashMap Attachment to String, it delegates to and returns of the wrapped Fhir4AttachmentHelper`() {
        // Given
        val resource: Fhir4DocumentReference = mockk()

        val attachments = hashMapOf(
            mockk<Fhir4Attachment>() to "1",
            mockk<Fhir4Attachment>() to "2"
        )

        every {
            Fhir4AttachmentHelper.updateAttachmentData(resource, attachments)
        } just Runs

        // When
        @Suppress("UNCHECKED_CAST")
        SdkFhirAttachmentHelper.updateAttachmentData(resource, attachments as HashMap<Any, String?>)

        // Then
        verify(exactly = 1) {
            @Suppress("UNCHECKED_CAST")
            Fhir4AttachmentHelper.updateAttachmentData(
                resource,
                attachments as HashMap<Fhir4Attachment, String>
            )
        }
    }

    @Test
    fun `Given, updateAttachmentData is called with a non FhirResource, it fails`() {
        assertFailsWith<CoreRuntimeException.InternalFailure> {
            SdkFhirAttachmentHelper.getAttachment("fail me!")
        }
    }

    @Test
    fun `Given, getIdentifier is called with a Fhir3Resource, it delegates to and returns of the wrapped Fhir3AttachmentHelper`() {
        // Given
        val resource: Fhir3Resource = mockk()

        val identifiers: MutableList<Fhir3Identifier> = mockk()

        every { Fhir3AttachmentHelper.getIdentifier(resource) } returns identifiers

        // Then
        assertSame(
            identifiers,
            SdkFhirAttachmentHelper.getIdentifier(resource)
        )

        verify(exactly = 1) { Fhir3AttachmentHelper.getIdentifier(resource) }
    }

    @Test
    fun `Given, getIdentifier is called with a Fhir3Resource, it returns the result of the Fhir3AttachmentHelper`() {
        // Given
        val resource: Fhir3Resource = mockk()

        every { Fhir3AttachmentHelper.getIdentifier(resource) } returns null

        // When
        val result = SdkFhirAttachmentHelper.getIdentifier(resource)

        // Then
        assertNull(result)

        verify(exactly = 1) { Fhir3AttachmentHelper.getIdentifier(resource) }
    }

    @Test
    fun `Given, getIdentifier is called with a Fhir4Resource, it delegates to and returns of the wrapped Fhir4AttachmentHelper`() {
        // Given
        val resource: Fhir4Resource = mockk()

        @Suppress("UNCHECKED_CAST")
        val identifiers: MutableList<Fhir4Identifier> = mockk()

        every { Fhir4AttachmentHelper.getIdentifier(resource) } returns identifiers

        // Then
        assertSame(
            identifiers,
            SdkFhirAttachmentHelper.getIdentifier(resource)
        )

        verify(exactly = 1) { Fhir4AttachmentHelper.getIdentifier(resource) }
    }

    @Test
    fun `Given, getIdentifier is called with a Fhir4Resource, it returns the result of the Fhir4AttachmentHelper`() {
        // Given
        val resource: Fhir4Resource = mockk()

        every { Fhir4AttachmentHelper.getIdentifier(resource) } returns null

        // When
        val result = SdkFhirAttachmentHelper.getIdentifier(resource)

        // Then
        assertNull(result)

        verify(exactly = 1) { Fhir4AttachmentHelper.getIdentifier(resource) }
    }

    @Test
    fun `Given, getIdentifier is called with a non FhirResource, it fails`() {
        assertFailsWith<CoreRuntimeException.InternalFailure> {
            SdkFhirAttachmentHelper.getIdentifier("fail me!")
        }
    }

    @Test
    fun `Given, setIdentifier is called with a Fhir3Resource and a empty List, it returns the result of the Fhir3AttachmentHelper`() {
        // Given
        val resource: Fhir3Resource = mockk()
        every { Fhir3AttachmentHelper.setIdentifier(any(), any()) } just Runs

        // When
        SdkFhirAttachmentHelper.setIdentifier(resource, listOf())

        // Then
        verify(exactly = 1) { Fhir3AttachmentHelper.setIdentifier(any(), any()) }
    }

    @Test
    fun `Given, setIdentifier is called with a Fhir3Resource and a List of Identifiers, it delegates to and returns of the wrapped Fhir3AttachmentHelper`() {
        // Given
        val resource: Fhir3Resource = mockk()
        val identifiers: List<Fhir3Identifier> = listOf(
            mockk(),
            mockk()
        )

        every { Fhir3AttachmentHelper.setIdentifier(resource, identifiers) } just Runs

        // When
        SdkFhirAttachmentHelper.setIdentifier(resource, identifiers)

        // Then
        verify(exactly = 1) { Fhir3AttachmentHelper.setIdentifier(resource, identifiers) }
    }

    @Test
    fun `Given, setIdentifier is called with a Fhir4Resource and a empty List, it returns the result of the Fhir4AttachmentHelper`() {
        // Given
        val resource: Fhir4Resource = mockk()
        every { Fhir4AttachmentHelper.setIdentifier(any(), any()) } just Runs

        // When
        SdkFhirAttachmentHelper.setIdentifier(resource, listOf())

        // Then
        verify(exactly = 1) { Fhir4AttachmentHelper.setIdentifier(any(), any()) }
    }

    @Test
    fun `Given, setIdentifier is called with a Fhir4Resource and a List of Identifiers, it delegates to and returns of the wrapped Fhir4AttachmentHelper`() {
        // Given
        val resource: Fhir4Resource = mockk()
        val identifiers: List<Fhir4Identifier> = listOf(
            mockk(),
            mockk()
        )

        every { Fhir4AttachmentHelper.setIdentifier(resource, identifiers) } just Runs

        // When
        SdkFhirAttachmentHelper.setIdentifier(resource, identifiers)

        // Then
        verify(exactly = 1) { Fhir4AttachmentHelper.setIdentifier(resource, identifiers) }
    }

    @Test
    fun `Given, setIdentifier is called with a non FhirResource, it fails`() {
        assertFailsWith<CoreRuntimeException.InternalFailure> {
            SdkFhirAttachmentHelper.setIdentifier("fail me!", mockk())
        }
    }

    @Test
    fun `Given, appendIdentifier is called with a Fhir3Resource, a Identifier and a Assigner, it delegates to and returns of the wrapped Fhir3AttachmentHelper`() {
        // Given
        val resource: Fhir3Resource = mockk()
        val identifier = "id"
        val assigner = "me"

        every { Fhir3AttachmentHelper.appendIdentifier(resource, identifier, assigner) } just Runs

        // When
        SdkFhirAttachmentHelper.appendIdentifier(resource, identifier, assigner)

        // Then
        verify(exactly = 1) {
            Fhir3AttachmentHelper.appendIdentifier(
                resource,
                identifier,
                assigner
            )
        }
    }

    @Test
    fun `Given, appendIdentifier is called with a Fhir4Resource, a Identifier and a Assigner, it delegates to and returns of the wrapped Fhir4AttachmentHelper`() {
        // Given
        val resource: Fhir4Resource = mockk()
        val identifier = "id"
        val assigner = "me"

        every { Fhir4AttachmentHelper.appendIdentifier(resource, identifier, assigner) } just Runs

        // When
        SdkFhirAttachmentHelper.appendIdentifier(resource, identifier, assigner)

        // Then
        verify(exactly = 1) {
            Fhir4AttachmentHelper.appendIdentifier(
                resource,
                identifier,
                assigner
            )
        }
    }

    @Test
    fun `Given, appendIdentifier is called with a non FhirResource, it fails`() {
        assertFailsWith<CoreRuntimeException.InternalFailure> {
            SdkFhirAttachmentHelper.appendIdentifier("fail me!", "me", "you")
        }
    }
}
