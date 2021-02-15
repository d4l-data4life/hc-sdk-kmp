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

import care.data4life.sdk.fhir.Fhir3ElementFactory
import care.data4life.sdk.fhir.Fhir3Resource
import care.data4life.sdk.fhir.Fhir4ElementFactory
import care.data4life.sdk.fhir.Fhir4Resource
import care.data4life.sdk.fhir.FhirContract
import care.data4life.sdk.lang.CoreRuntimeException
import java.util.*

internal object SdkFhirElementFactory : WrapperContract.FhirElementFactory {
    private val fhir3Indicator = Fhir3Resource::class.java.`package`
    private val fhir4Indicator = Fhir4Resource::class.java.`package`

    @Throws(CoreRuntimeException.InternalFailure::class)
    override fun getFhirTypeForClass(resourceType: Class<out Any>): String? {
        @Suppress("UNCHECKED_CAST")
        return when (resourceType.`package`) {
            fhir3Indicator -> Fhir3ElementFactory.getFhirTypeForClass(resourceType as Class<Fhir3Resource>)
            fhir4Indicator -> Fhir4ElementFactory.getFhirTypeForClass(resourceType as Class<Fhir4Resource>)
            else -> throw CoreRuntimeException.InternalFailure()
        }
    }

    override fun resolveFhirVersion(resourceType: Class<out Any>): FhirContract.FhirVersion {
        return when (resourceType.`package`) {
            fhir3Indicator -> FhirContract.FhirVersion.FHIR_3
            fhir4Indicator -> FhirContract.FhirVersion.FHIR_4
            else -> FhirContract.FhirVersion.NO_FHIR
        }
    }

    private fun normalizeTypeNames(resourceType: String): String {
        val normalizedName = resourceType.toLowerCase(Locale.US)
        return if (!typeNames.containsKey(normalizedName)) {
            resourceType
        } else {
            typeNames[normalizedName]!!
        }
    }

    override fun getFhir3ClassForType(resourceType: String): Class<out Fhir3Resource>? {
        val clazz = Fhir3ElementFactory.getClassForFhirType(normalizeTypeNames(resourceType))

        @Suppress("UNCHECKED_CAST")
        return if (clazz == null) null else clazz as Class<out Fhir3Resource>
    }

    override fun getFhir4ClassForType(resourceType: String): Class<out Fhir4Resource>? {
        val clazz = Fhir4ElementFactory.getClassForFhirType(normalizeTypeNames(resourceType))

        @Suppress("UNCHECKED_CAST")
        return if (clazz == null) null else (clazz as Class<out Fhir4Resource>)
    }

    private val typeNames = mapOf(
            "Specimen".toLowerCase(Locale.US) to "Specimen",
            "ServiceRequest".toLowerCase(Locale.US) to "ServiceRequest",
            "Substance".toLowerCase(Locale.US) to "Substance",
            "ValueSet".toLowerCase(Locale.US) to "ValueSet",
            "DocumentReference".toLowerCase(Locale.US) to "DocumentReference",
            "DiagnosticReport".toLowerCase(Locale.US) to "DiagnosticReport",
            "Encounter".toLowerCase(Locale.US) to "Encounter",
            "Medication".toLowerCase(Locale.US) to "Medication",
            "Questionnaire".toLowerCase(Locale.US) to "Questionnaire",
            "Goal".toLowerCase(Locale.US) to "Goal",
            "CarePlan".toLowerCase(Locale.US) to "CarePlan",
            "CareTeam".toLowerCase(Locale.US) to "CareTeam",
            "QuestionnaireResponse".toLowerCase(Locale.US) to "QuestionnaireResponse",
            "Practitioner".toLowerCase(Locale.US) to "Practitioner",
            "Patient".toLowerCase(Locale.US) to "Patient",
            "Procedure".toLowerCase(Locale.US) to "Procedure",
            "Condition".toLowerCase(Locale.US) to "Condition",
            "FamilyMemberHistory".toLowerCase(Locale.US) to "FamilyMemberHistory",
            "Organization".toLowerCase(Locale.US) to "Organization",
            "MedicationRequest".toLowerCase(Locale.US) to "MedicationRequest",
            "Observation".toLowerCase(Locale.US) to "Observation",
            "Location".toLowerCase(Locale.US) to "Location",
            "Provenance".toLowerCase(Locale.US) to "Provenance",
            "ReferralRequest".toLowerCase(Locale.US) to "ReferralRequest",
            "ProcedureRequest".toLowerCase(Locale.US) to "ProcedureRequest"
    )
}
