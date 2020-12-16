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

import care.data4life.fhir.stu3.model.DocumentReference
import care.data4life.sdk.fhir.Fhir3Attachment
import care.data4life.sdk.lang.CoreRuntimeException
import care.data4life.sdk.test.util.AttachmentBuilder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.ArrayList

class FhirElementFactoryTest {
    private fun buildDocRefContent(attachment: Fhir3Attachment): DocumentReference.DocumentReferenceContent {
        return DocumentReference.DocumentReferenceContent(attachment)
    }

    private fun buildDocumentReference(): DocumentReference {
        val content = buildDocRefContent(AttachmentBuilder.buildAttachment(null))
        val contents: MutableList<DocumentReference.DocumentReferenceContent> = ArrayList()
        contents.add(content)
        return DocumentReference(
                null,
                null,
                null,
                contents
        )
    }

    @Test
    fun `it is a FhirElementFactory`() {
        assertTrue((FhirElementFactory as Any) is FhirElementFactory)
    }

    @Test
    fun `Given, getFhirTypeForClass is called with a valid FhirResource Class, it returns its name`() {
        // Given
        val resource = buildDocumentReference()

        // When
        val name = FhirElementFactory.getFhirTypeForClass(resource::class.java)

        // Then
        assertEquals(
                "DocumentReference",
                name
        )
    }

    @Test
    fun `Given, getFhirTypeForClass is called with a non valid FhirResource Class, it returns fails with CoreRuntimeExceptionInternalFailure`() {
        try {
            // When
            FhirElementFactory.getFhirTypeForClass(String::class.java)
            assertTrue(false)//Fixme
        } catch (e: Exception) {
            // Then
            assertTrue(e is CoreRuntimeException.InternalFailure)
        }
    }

    @Test
    fun `Given, getFhir3ClassForType is called with a Fhir3Resource String, it returns a Fhir3 resource class`() {
        val klass = FhirElementFactory.getFhir3ClassForType("DocumentReference")

        assertEquals(
                klass,
                DocumentReference::class.java
        )
    }

    @Test
    fun `Given, getFhir3ClassForType is called with a unknown Fhir3Resource String, it returns a null`() {
        assertNull(FhirElementFactory.getFhir3ClassForType("I will bug you"))
    }
}
