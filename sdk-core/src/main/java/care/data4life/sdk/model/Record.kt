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
package care.data4life.sdk.model

import care.data4life.fhir.stu3.model.DomainResource
import care.data4life.sdk.model.definitions.FhirRecord
import java.util.*

open class Record<T : DomainResource>(
        fhirResource: T?,
        meta: Meta?,
        annotations: List<String>? = null
) : FhirRecord<T?> {
    constructor(fhirResource: T?, meta: Meta?) : this(
            fhirResource,
            meta,
            null
    )

    override val identifier: String
        get() = ""

    private val _resource: T? = fhirResource
    override val resource: T?
        get() = _resource
    override val fhirResource: T?
        get() = _resource

    private val _meta: Meta? = meta
    override val meta: Meta?
        get() = _meta

    private var _annotations: List<String>? = annotations
    override val annotations: List<String>?
        get() = _annotations

    override fun equals(other: Any?): Boolean {
        return when {
            this === other -> true
            other !is Record<*> -> false
            resource != other.resource ||
                    meta != other.meta ||
                    annotations != other.annotations -> false
            else -> true
        }
    }

    override fun hashCode(): Int = Objects.hash(resource, meta, annotations)
}
