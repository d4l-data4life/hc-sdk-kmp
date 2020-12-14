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

package care.data4life.sdk.fhir

typealias Fhir3Resource = care.data4life.fhir.stu3.model.DomainResource
typealias Fhir4Resource = care.data4life.fhir.r4.model.DomainResource

typealias Fhir3Attachment = care.data4life.fhir.stu3.model.Attachment
typealias Fhir4Attachment = care.data4life.fhir.r4.model.Attachment

typealias Fhir3Identifier = care.data4life.fhir.stu3.model.Identifier
typealias Fhir4Identifier = care.data4life.fhir.r4.model.Identifier

typealias Fhir3DateTimeParser = care.data4life.fhir.stu3.util.FhirDateTimeParser
typealias Fhir4DateTimeParser = care.data4life.fhir.r4.util.FhirDateTimeParser

interface FhirVersion {
    val version: String
}

fun Fhir3Resource.asVersionable() = object : FhirVersion {
    override val version: String
        get() = "3.0.1"
}

fun Fhir4Resource.asVersionable() = object : FhirVersion {
    override val version: String
        get() = "4.0.1"

}
