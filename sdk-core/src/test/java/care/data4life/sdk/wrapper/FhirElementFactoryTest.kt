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
import care.data4life.sdk.fhir.Fhir4Attachment
import care.data4life.sdk.fhir.FhirContract
import care.data4life.sdk.lang.CoreRuntimeException
import care.data4life.sdk.test.util.AttachmentBuilder
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import care.data4life.fhir.r4.model.DocumentReference as DocumentReferenceFhir4
import care.data4life.fhir.stu3.model.DocumentReference as DocumentReferenceFhir3

class FhirElementFactoryTest {

    @Test
    fun `it is a FhirElementFactory`() {
        assertTrue((SdkFhirElementFactory as Any) is WrapperContract.FhirElementFactory)
    }

    @Test
    fun `Given, getFhirTypeForClass is called with a subtype of Fhir3Resource Class, it returns its name`() {
        // Given
        val resource = buildDocumentReferenceFhir3()

        // When
        val name = SdkFhirElementFactory.getFhirTypeForClass(resource::class.java)

        // Then
        assertEquals(
                expected = "DocumentReference",
                actual = name
        )
    }

    @Test
    fun `Given, getFhirTypeForClass is called with a valid Fhir4Resource Class, it returns its name`() {
        // Given
        val resource = buildDocumentReferenceFhir4()

        // When
        val name = SdkFhirElementFactory.getFhirTypeForClass(resource::class.java)

        // Then
        assertEquals(
                expected = "DocumentReference",
                actual = name
        )
    }

    @Test
    fun `Given, getFhirTypeForClass is called with a non valid FhirResource Class, it returns fails with CoreRuntimeExceptionInternalFailure`() {
        val actual = assertFailsWith<CoreRuntimeException.InternalFailure> {
            SdkFhirElementFactory.getFhirTypeForClass(String::class.java)
        }

        assertEquals(
                actual = actual.message,
                expected = "Internal failure"
        )
    }

    @Test
    fun `Given, resolveFhirType is called with a Class of a Fhir3Resource, it returns FHIR_3 as type`() {
        // Given
        val resource = buildDocumentReferenceFhir3()

        // When
        val type = SdkFhirElementFactory.resolveFhirVersion(resource::class.java)

        assertEquals(
                actual = type,
                expected = FhirContract.FhirVersion.FHIR_3
        )
    }

    @Test
    fun `Given, resolveFhirType is called with a Class of a Fhir3Resource, it returns FHIR_4 as type`() {
        // Given
        val resource = buildDocumentReferenceFhir4()

        // When
        val type = SdkFhirElementFactory.resolveFhirVersion(resource::class.java)

        assertEquals(
                actual = type,
                expected = FhirContract.FhirVersion.FHIR_4
        )
    }

    @Test
    fun `Given, resolveFhirType is called with a Class of a non FhirResource, it returns NO_FHIR as type`() {
        // Given
        val resource = "tomato"

        // When
        val type = SdkFhirElementFactory.resolveFhirVersion(resource::class.java)

        assertEquals(
                actual = type,
                expected = FhirContract.FhirVersion.UNKNOWN
        )
    }

    @Test
    fun `Given, getFhir3ClassForType is called with a Fhir3Resource String, it returns a Fhir3 resource class`() {
        val klass = SdkFhirElementFactory.getFhir3ClassForType("DocumentReference")

        assertEquals(
                actual = klass,
                expected = DocumentReferenceFhir3::class.java
        )
    }

    @Test
    fun `Given, getFhir3ClassForType is called with a Fhir3Resource in a non canonical form, it returns a Fhir3 resource class`() {
        val klass = SdkFhirElementFactory.getFhir3ClassForType("documentreference")

        assertEquals(
                actual = klass,
                expected = DocumentReferenceFhir3::class.java
        )
    }

    @Test
    fun `Given, getFhir3ClassForType is called with a unknown Fhir3Resource String, it returns a null`() {
        assertNull(SdkFhirElementFactory.getFhir3ClassForType("I will bug you"))
    }

    @Test
    fun `Given, getFhir3ClassForType is called, with any valid Fhir3Resource in string representation, it returns its resource class`() {
        val resources = listOf(
                "specimen",
                "substance",
                "valueSet",
                "referralRequest",
                "documentReference",
                "diagnosticReport",
                "medication",
                "questionnaire",
                "goal",
                "carePlan",
                "careTeam",
                "questionnaireResponse",
                "procedureRequest",
                "practitioner",
                "patient",
                "procedure",
                "condition",
                "familyMemberHistory",
                "organization",
                "medicationRequest",
                "observation",
                "provenance"
        )

        for (type in resources) {
            assertNotNull(SdkFhirElementFactory.getFhir3ClassForType(type))
        }
    }


    @Test
    fun `Given, getFhir4ClassForType is called with a Fhir4Resource String, it returns a Fhir4 resource class`() {
        val klass = SdkFhirElementFactory.getFhir4ClassForType("DocumentReference")

        assertEquals(
                actual = klass,
                expected = DocumentReferenceFhir4::class.java
        )
    }

    @Test
    fun `Given, getFhir4ClassForType is called with a Fhir4Resource in a non canonical form, it returns a Fhir4 resource class`() {
        val klass = SdkFhirElementFactory.getFhir4ClassForType("documentreference")

        assertEquals(
                actual = klass,
                expected = DocumentReferenceFhir4::class.java
        )
    }

    @Test
    fun `Given, getFhir4ClassForType is called with a unknown Fhir4Resource String, it returns a null`() {
        assertNull(SdkFhirElementFactory.getFhir4ClassForType("I will bug you"))
    }

    @Test
    fun `Given, getFhir4ClassForType is called, with any valid Fhir4Resource in string representation, it returns its resource class`() {
        val resources = listOf(
                "Specimen",
                "ServiceRequest",
                "Substance",
                "ValueSet",
                "DocumentReference",
                "DiagnosticReport",
                "Encounter",
                "Medication",
                "Questionnaire",
                "Goal",
                "CarePlan",
                "CareTeam",
                "QuestionnaireResponse",
                "Practitioner",
                "Patient",
                "Procedure",
                "Condition",
                "FamilyMemberHistory",
                "Organization",
                "MedicationRequest",
                "Observation",
                "Location",
                "Provenance"
        )

        for (type in resources) {
            assertNotNull(SdkFhirElementFactory.getFhir4ClassForType(type))
        }

    }

    companion object {
        private val DATA = "data"
        private val DATA_SIZE = 42
        private val DATA_HASH = "dataHash"

        private fun buildDocRefContentFhir3(attachment: Fhir3Attachment): DocumentReferenceFhir3.DocumentReferenceContent {
            return DocumentReferenceFhir3.DocumentReferenceContent(attachment)
        }

        private fun buildDocumentReferenceFhir3(): DocumentReferenceFhir3 {
            val content = buildDocRefContentFhir3(AttachmentBuilder.buildAttachment(null))
            val contents: MutableList<DocumentReferenceFhir3.DocumentReferenceContent> = ArrayList()
            contents.add(content)
            return DocumentReferenceFhir3(
                    null,
                    null,
                    null,
                    contents
            )
        }

        private fun buildFhir4Attachment(): Fhir4Attachment {
            val attachment = Fhir4Attachment()
            attachment.id = null
            attachment.data = DATA
            attachment.size = DATA_SIZE
            attachment.hash = DATA_HASH
            return attachment
        }

        private fun buildDocRefContentFhir4(attachment: Fhir4Attachment): DocumentReferenceFhir4.DocumentReferenceContent {
            return DocumentReferenceFhir4.DocumentReferenceContent(attachment)
        }

        private fun buildDocumentReferenceFhir4(): DocumentReferenceFhir4 {
            val content = buildDocRefContentFhir4(buildFhir4Attachment())
            val contents: MutableList<DocumentReferenceFhir4.DocumentReferenceContent> = ArrayList()
            contents.add(content)
            return DocumentReferenceFhir4(
                    null,
                    contents
            )
        }
    }
}
