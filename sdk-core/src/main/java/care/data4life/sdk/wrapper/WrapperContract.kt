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

import care.data4life.fhir.FhirException
import care.data4life.sdk.fhir.Fhir3Resource
import care.data4life.sdk.fhir.Fhir4Resource
import care.data4life.sdk.fhir.FhirContract
import care.data4life.sdk.lang.CoreRuntimeException
import care.data4life.sdk.model.ModelContract
import care.data4life.sdk.network.model.definitions.DecryptedBaseRecord
import org.threeten.bp.LocalDate

// TODO restructure this
class WrapperContract {

    interface Attachment {
        var id: String?
        var data: String?
        var hash: String?
        var size: Int?
        fun <T : Any> unwrap(): T
    }

    internal interface Identifier {
        var value: String?
        fun <T : Any> unwrap(): T
    }

    interface FhirElementFactory {

        @Throws(CoreRuntimeException.InternalFailure::class)
        fun getFhirTypeForClass(resourceType: Class<out Any>): String?

        fun resolveFhirVersion(resourceType: Class<out Any>): FhirContract.FhirVersion

        fun getFhir3ClassForType(resourceType: String): Class<out Fhir3Resource>?

        fun getFhir4ClassForType(resourceType: String): Class<out Fhir4Resource>?
    }

    interface FhirParser {
        @Throws(FhirException::class)
        fun toFhir3(resourceType: String, source: String): Fhir3Resource?

        @Throws(FhirException::class)
        fun toFhir4(resourceType: String, source: String): Fhir4Resource?

        @Throws(FhirException::class)
        fun fromResource(resource: Any): String?
    }

    internal interface DateTimeFormatter {
        fun now(): String
        fun formatDate(dateTime: LocalDate): String
        fun buildMeta(record: DecryptedBaseRecord<*>): ModelContract.Meta

        companion object {
            const val DATE_FORMAT = "yyyy-MM-dd"
            const val DATE_TIME_FORMAT = "yyyy-MM-dd'T'HH:mm:ss[.SSS]"
        }
    }
}
