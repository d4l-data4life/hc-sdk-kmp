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

package care.data4life.sdk.model

import care.data4life.fhir.stu3.model.DomainResource
import care.data4life.sdk.tag.Annotations
import org.threeten.bp.LocalDate
import org.threeten.bp.LocalDateTime
import java.io.Serializable

interface ModelContract {
    interface BaseRecord<T> {
        val identifier: String
        val resource: T
        val meta: Meta?
        val annotations: Annotations?
    }

    @Deprecated("Deprecated with version v1.9.0 and will be removed in version v2.0.0")
    interface Fhir3Record<T : DomainResource> : BaseRecord<T> {
        val fhirResource: T
            get() = resource
    }

    interface ModelVersion {
        fun isModelVersionSupported(version: Int): Boolean

        companion object {
            const val CURRENT = 1
        }
    }

    // TODO: model this platform independent
    interface Meta : Serializable {
        val createdDate: LocalDate
        val updatedDate: LocalDateTime
    }
}
