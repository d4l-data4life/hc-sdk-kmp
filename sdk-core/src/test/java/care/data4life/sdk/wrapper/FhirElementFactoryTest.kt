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
import care.data4life.sdk.RecordServiceTestBase
import care.data4life.sdk.lang.CoreRuntimeException
import care.data4life.sdk.test.util.AttachmentBuilder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.ArrayList

class FhirElementFactoryTest {
    private fun buildDocumentReference(): DocumentReference {
        val content = RecordServiceTestBase.buildDocRefContent(AttachmentBuilder.buildAttachment(null))
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
        assertTrue((SdkFhirElementFactory as Any) is WrapperContract.FhirElementFactory)
    }

    @Test
    fun `Given, getFhirTypeForClass is called with a valid FhirResource Class, it returns its name`() {
        // Given
        val resource = buildDocumentReference()

        // When
        val name = SdkFhirElementFactory.getFhirTypeForClass(resource::class.java)

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
            SdkFhirElementFactory.getFhirTypeForClass(String::class.java)
            assertTrue(false)//Fixme
        } catch (e: Exception) {
            // Then
            assertTrue(e is CoreRuntimeException.InternalFailure)
        }
    }
}
