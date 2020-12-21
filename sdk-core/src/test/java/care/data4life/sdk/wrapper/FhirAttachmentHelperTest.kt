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

import care.data4life.fhir.stu3.model.DocumentReference as Fhir3DocumentReference
import care.data4life.fhir.r4.model.DocumentReference as Fhir4DocumentReference
import care.data4life.sdk.fhir.Fhir3Attachment
import care.data4life.sdk.fhir.Fhir3AttachmentHelper
import care.data4life.sdk.fhir.Fhir3Identifier
import care.data4life.sdk.fhir.Fhir3Resource
import care.data4life.sdk.fhir.Fhir4Attachment
import care.data4life.sdk.fhir.Fhir4AttachmentHelper
import care.data4life.sdk.fhir.Fhir4Identifier
import care.data4life.sdk.fhir.Fhir4Resource
import care.data4life.sdk.lang.CoreRuntimeException
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.verify
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito

class FhirAttachmentHelperTest {
    @Before
    fun setUp() {
        mockkStatic(Fhir3AttachmentHelper::class)
        mockkStatic(Fhir4AttachmentHelper::class)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `it is a FhirAttachmentHelper`() {
        assertTrue((SdkFhirAttachmentHelper as Any) is HelperContract.FhirAttachmentHelper)
    }

    @Test
    fun `Given, hasAttachment is called with a Fhir3Resource, it delegates to and returns of the wrapped Fhir3AttachmentHelper`() {
        // Given
        val resource1 = Mockito.mock(Fhir3DocumentReference::class.java)
        val resource2 = Mockito.mock(Fhir3DocumentReference::class.java)

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
        val resource1 = Mockito.mock(Fhir4DocumentReference::class.java)
        val resource2 = Mockito.mock(Fhir4DocumentReference::class.java)

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
        // Given
        try {
            // When
            SdkFhirAttachmentHelper.hasAttachment("fail me!")
            assertTrue(false)//Fixme
        } catch (e: Exception) {
            // Then
            assertTrue(e is CoreRuntimeException.InternalFailure)
        }
    }

    @Test
    fun `Given, getAttachment is called with Fhir3Resource, it delegates to and returns of the wrapped Fhir3AttachmentHelper`() {
        // Given
        val resource = Mockito.mock(Fhir3DocumentReference::class.java)
        @Suppress("UNCHECKED_CAST")
        val attachments = Mockito.mock(MutableList::class.java) as MutableList<Fhir3Attachment>

        every { Fhir3AttachmentHelper.getAttachment(resource) } returns attachments

        // Then
        assertSame(
                attachments,
                SdkFhirAttachmentHelper.getAttachment(resource)
        )


        verify(exactly = 1) { Fhir3AttachmentHelper.getAttachment(resource) }
    }

    @Test
    fun `Given, getAttachment is called with Fhir3Resource and the response of the wrapped Fhir3AttachmentHelper is null, it returns null`() {
        // Given
        val resource = Mockito.mock(Fhir3DocumentReference::class.java)

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
        val resource = Mockito.mock(Fhir4DocumentReference::class.java)
        @Suppress("UNCHECKED_CAST")
        val attachments = Mockito.mock(MutableList::class.java) as MutableList<Fhir4Attachment>

        every { Fhir4AttachmentHelper.getAttachment(resource) } returns attachments

        // Then
        assertSame(
                attachments,
                SdkFhirAttachmentHelper.getAttachment(resource)
        )


        verify(exactly = 1) { Fhir4AttachmentHelper.getAttachment(resource) }
    }

    @Test
    fun `Given, getAttachment is called with Fhir4Resource and the response of the wrapped Fhir4AttachmentHelper is null, it returns null`() {
        // Given
        val resource = Mockito.mock(Fhir4DocumentReference::class.java)

        every { Fhir4AttachmentHelper.getAttachment(resource) } returns null

        // When
        val result = SdkFhirAttachmentHelper.getAttachment(resource)

        // Then
        assertNull(result)

        verify(exactly = 1) { Fhir4AttachmentHelper.getAttachment(resource) }
    }

    @Test
    fun `Given, getAttachment is called with a non FhirResource, it fails`() {
        // Given
        try {
            // When
            SdkFhirAttachmentHelper.getAttachment("fail me!")
            assertTrue(false)//Fixme
        } catch (e: Exception) {
            // Then
            assertTrue(e is CoreRuntimeException.InternalFailure)
        }
    }

    @Test
    fun `Given, updateAttachmentData is called with a Fhir3Resource and null as attachment, it delegates it to the Fhir3AttachmentHelper`() {
        // Given
        val resource = Mockito.mock(Fhir3DocumentReference::class.java)

        every { Fhir3AttachmentHelper.updateAttachmentData(resource, null) } returns Unit

        // When
        SdkFhirAttachmentHelper.updateAttachmentData(resource, null)

        // Then
        verify(exactly = 1) { Fhir3AttachmentHelper.updateAttachmentData(resource, null) }
    }

    @Test
    fun `Given, updateAttachmentData is called with a Fhir3Resource and a empty HashMap as attachement, it delegates it to the Fhir3AttachmentHelper`() {
        // Given
        val resource = Mockito.mock(Fhir3DocumentReference::class.java)

        every { Fhir3AttachmentHelper.updateAttachmentData(resource, hashMapOf()) } returns Unit

        // When
        SdkFhirAttachmentHelper.updateAttachmentData(resource, hashMapOf())

        // Then
        verify(exactly = 1) { Fhir3AttachmentHelper.updateAttachmentData(resource, hashMapOf()) }
    }

    @Test
    fun `Given, updateAttachmentData is called with a Fhir3Resource and a HashMap Attachment to String, it delegates to and returns of the wrapped Fhir3AttachmentHelper`() {
        // Given
        val resource = Mockito.mock(Fhir3DocumentReference::class.java)

        @Suppress("UNCHECKED_CAST")
        val attachments = hashMapOf(
                Mockito.mock(Fhir3Attachment::class.java) to "1",
                Mockito.mock(Fhir3Attachment::class.java) to "2"
        ) as HashMap<Any, String?>?

        every {
            @Suppress("UNCHECKED_CAST")
            Fhir3AttachmentHelper.updateAttachmentData(resource, attachments as HashMap<Fhir3Attachment, String>)
        } returns Unit

        // When
        SdkFhirAttachmentHelper.updateAttachmentData(resource, attachments)

        // Then
        verify(exactly = 1) {
            @Suppress("UNCHECKED_CAST")
            Fhir3AttachmentHelper.updateAttachmentData(resource, attachments as HashMap<Fhir3Attachment, String>)
        }
    }

    @Test
    fun `Given, updateAttachmentData is called with a Fhir4Resource and null as attachment, it delegates it to the Fhir4AttachmentHelper`() {
        // Given
        val resource = Mockito.mock(Fhir4DocumentReference::class.java)

        every { Fhir4AttachmentHelper.updateAttachmentData(resource, null) } returns Unit

        // When
        SdkFhirAttachmentHelper.updateAttachmentData(resource, null)

        // Then
        verify(exactly = 1) { Fhir4AttachmentHelper.updateAttachmentData(resource, null) }
    }

    @Test
    fun `Given, updateAttachmentData is called with a Fhir4Resource and a empty HashMap as attachement, it delegates it to the Fhir4AttachmentHelper`() {
        // Given
        val resource = Mockito.mock(Fhir4DocumentReference::class.java)

        every { Fhir4AttachmentHelper.updateAttachmentData(resource, hashMapOf()) } returns Unit

        // When
        SdkFhirAttachmentHelper.updateAttachmentData(resource, hashMapOf())

        // Then
        verify(exactly = 1) { Fhir4AttachmentHelper.updateAttachmentData(resource, hashMapOf()) }
    }

    @Test
    fun `Given, updateAttachmentData is called with a Fhir4Resource and a HashMap Attachment to String, it delegates to and returns of the wrapped Fhir4AttachmentHelper`() {
        // Given
        val resource = Mockito.mock(Fhir4DocumentReference::class.java)

        @Suppress("UNCHECKED_CAST")
        val attachments = hashMapOf(
                Mockito.mock(Fhir4Attachment::class.java) to "1",
                Mockito.mock(Fhir4Attachment::class.java) to "2"
        ) as HashMap<Any, String?>?

        every {
            @Suppress("UNCHECKED_CAST")
            Fhir4AttachmentHelper.updateAttachmentData(resource, attachments as HashMap<Fhir4Attachment, String>)
        } returns Unit

        // When
        SdkFhirAttachmentHelper.updateAttachmentData(resource, attachments)

        // Then
        verify(exactly = 1) {
            @Suppress("UNCHECKED_CAST")
            Fhir4AttachmentHelper.updateAttachmentData(resource, attachments as HashMap<Fhir4Attachment, String>)
        }
    }

    @Test
    fun `Given, updateAttachmentData is called with a non FhirResource, it fails`() {
        // Given
        try {
            // When
            SdkFhirAttachmentHelper.getAttachment("fail me!")
            assertTrue(false)//Fixme
        } catch (e: Exception) {
            // Then
            assertTrue(e is CoreRuntimeException.InternalFailure)
        }
    }

    @Test
    fun `Given, getIdentifier is called with a Fhir3Resource, it delegates to and returns of the wrapped Fhir3AttachmentHelper`() {
        // Given
        val resource = Mockito.mock(Fhir3Resource::class.java)
        @Suppress("UNCHECKED_CAST")
        val identifiers = Mockito.mock(MutableList::class.java) as MutableList<Fhir3Identifier>

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
        val resource = Mockito.mock(Fhir3Resource::class.java)

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
        val resource = Mockito.mock(Fhir4Resource::class.java)
        @Suppress("UNCHECKED_CAST")
        val identifiers = Mockito.mock(MutableList::class.java) as MutableList<Fhir4Identifier>

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
        val resource = Mockito.mock(Fhir4Resource::class.java)

        every { Fhir4AttachmentHelper.getIdentifier(resource) } returns null

        // When
        val result = SdkFhirAttachmentHelper.getIdentifier(resource)

        // Then
        assertNull(result)

        verify(exactly = 1) { Fhir4AttachmentHelper.getIdentifier(resource) }
    }

    @Test
    fun `Given, getIdentifier is called with a non FhirResource, it fails`() {
        // Given
        try {
            // When
            SdkFhirAttachmentHelper.getIdentifier("fail me!")
            assertTrue(false)//Fixme
        } catch (e: Exception) {
            // Then
            assertTrue(e is CoreRuntimeException.InternalFailure)
        }
    }

    @Test
    fun `Given, setIdentifier is called with a Fhir3Resource and a empty List, it returns the result of the Fhir3AttachmentHelper`() {
        // Given
        val resource = Mockito.mock(Fhir3Resource::class.java)
        every { Fhir3AttachmentHelper.setIdentifier(any(), any()) } returns Unit

        // When
        SdkFhirAttachmentHelper.setIdentifier(resource, listOf())

        // Then
        verify(exactly = 1) { Fhir3AttachmentHelper.setIdentifier(any(), any()) }
    }


    @Test
    fun `Given, setIdentifier is called with a Fhir3Resource and a List of Identifiers, it delegates to and returns of the wrapped Fhir3AttachmentHelper`() {
        // Given
        val resource = Mockito.mock(Fhir3Resource::class.java)
        val identifiers = listOf(
                Mockito.mock(Fhir3Identifier::class.java),
                Mockito.mock(Fhir3Identifier::class.java)
        )

        every { Fhir3AttachmentHelper.setIdentifier(resource, identifiers) } returns Unit


        // When
        SdkFhirAttachmentHelper.setIdentifier(resource, identifiers)

        // Then
        verify(exactly = 1) { Fhir3AttachmentHelper.setIdentifier(resource, identifiers) }
    }

    @Test
    fun `Given, setIdentifier is called with a Fhir4Resource and a empty List, it returns the result of the Fhir4AttachmentHelper`() {
        // Given
        val resource = Mockito.mock(Fhir4Resource::class.java)
        every { Fhir4AttachmentHelper.setIdentifier(any(), any()) } returns Unit

        // When
        SdkFhirAttachmentHelper.setIdentifier(resource, listOf())

        // Then
        verify(exactly = 1) { Fhir4AttachmentHelper.setIdentifier(any(), any()) }
    }


    @Test
    fun `Given, setIdentifier is called with a Fhir4Resource and a List of Identifiers, it delegates to and returns of the wrapped Fhir4AttachmentHelper`() {
        // Given
        val resource = Mockito.mock(Fhir4Resource::class.java)
        val identifiers = listOf(
                Mockito.mock(Fhir4Identifier::class.java),
                Mockito.mock(Fhir4Identifier::class.java)
        )

        every { Fhir4AttachmentHelper.setIdentifier(resource, identifiers) } returns Unit


        // When
        SdkFhirAttachmentHelper.setIdentifier(resource, identifiers)

        // Then
        verify(exactly = 1) { Fhir4AttachmentHelper.setIdentifier(resource, identifiers) }
    }

    @Test
    fun `Given, setIdentifier is called with a non FhirResource, it fails`() {
        // Given
        try {
            // When
            SdkFhirAttachmentHelper.setIdentifier("fail me!", mockk())
            assertTrue(false)//Fixme
        } catch (e: Exception) {
            // Then
            assertTrue(e is CoreRuntimeException.InternalFailure)
        }
    }

    @Test
    fun `Given, appendIdentifier is called with a Fhir3Resource, a Identifier and a Assigner, it delegates to and returns of the wrapped Fhir3AttachmentHelper`() {
        // Given
        val resource = Mockito.mock(Fhir3Resource::class.java)
        val identifier = "id"
        val assigner = "me"

        every { Fhir3AttachmentHelper.appendIdentifier(resource, identifier, assigner) } returns Unit

        // When
        SdkFhirAttachmentHelper.appendIdentifier(resource, identifier, assigner)

        // Then
        verify(exactly = 1) { Fhir3AttachmentHelper.appendIdentifier(resource, identifier, assigner) }
    }

    @Test
    fun `Given, appendIdentifier is called with a Fhir4Resource, a Identifier and a Assigner, it delegates to and returns of the wrapped Fhir4AttachmentHelper`() {
        // Given
        val resource = Mockito.mock(Fhir4Resource::class.java)
        val identifier = "id"
        val assigner = "me"

        every { Fhir4AttachmentHelper.appendIdentifier(resource, identifier, assigner) } returns Unit

        // When
        SdkFhirAttachmentHelper.appendIdentifier(resource, identifier, assigner)

        // Then
        verify(exactly = 1) { Fhir4AttachmentHelper.appendIdentifier(resource, identifier, assigner) }
    }

    @Test
    fun `Given, appendIdentifier is called with a non FhirResource, it fails`() {
        // Given
        try {
            // When
            SdkFhirAttachmentHelper.appendIdentifier("fail me!", "me", "you")
            assertTrue(false)//Fixme
        } catch (e: Exception) {
            // Then
            assertTrue(e is CoreRuntimeException.InternalFailure)
        }
    }
}
