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

import care.data4life.fhir.Fhir
import care.data4life.fhir.FhirParser
import care.data4life.sdk.lang.CoreRuntimeException

internal object ResourceParser: WrapperContract.ResourceParser {
    private val fhir3Parser: FhirParser<Any> = Fhir().createStu3Parser()
    private val fhir4Parser: FhirParser<Any> = Fhir().createR4Parser()

    override fun toFhir3(resourceType: String, source: String): WrapperContract.Resource {
        val clazz = FhirElementFactory.getFhir3ClassForType(resourceType)
                ?: throw CoreRuntimeException.InternalFailure()

        val rawResource = fhir3Parser.toFhir(clazz, source)
                ?: throw CoreRuntimeException.InternalFailure()

        return ResourceFactory.wrap(rawResource)!!
    }

    override fun toFhir4(resourceType: String, source: String): WrapperContract.Resource {
        val clazz = FhirElementFactory.getFhir4ClassForType(resourceType)
                ?: throw CoreRuntimeException.InternalFailure()

        val rawResource = fhir4Parser.toFhir(clazz, source)
                ?: throw CoreRuntimeException.InternalFailure()

        return ResourceFactory.wrap(rawResource)!!
    }

    override fun fromResource(resource: WrapperContract.Resource): String {
        return when(resource.type) {
            WrapperContract.Resource.TYPE.FHIR3 -> fhir3Parser.fromFhir(resource.unwrap())
            WrapperContract.Resource.TYPE.FHIR4 -> TODO()
            WrapperContract.Resource.TYPE.DATA -> throw CoreRuntimeException.InternalFailure()
        }
    }

}
