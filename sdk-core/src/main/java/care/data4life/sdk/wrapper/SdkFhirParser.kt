package care.data4life.sdk.wrapper

import care.data4life.fhir.Fhir
import care.data4life.fhir.FhirParser
import care.data4life.sdk.fhir.Fhir3Resource
import care.data4life.sdk.fhir.Fhir4Resource
import care.data4life.sdk.lang.CoreRuntimeException

internal object SdkFhirParser : WrapperContract.FhirParser {
    private val fhir3Parser: FhirParser<Any> = Fhir().createStu3Parser()
    private val fhir4Parser: FhirParser<Any> = Fhir().createR4Parser()
    private val fhirElement: WrapperContract.FhirElementFactory = SdkFhirElementFactory

    override fun toFhir3(resourceType: String, source: String): Fhir3Resource {
        val clazz = fhirElement.getFhir3ClassForType(resourceType)

        return fhir3Parser.toFhir(clazz!!, source)
    }

    override fun toFhir4(resourceType: String, source: String): Fhir4Resource {
        val clazz = fhirElement.getFhir4ClassForType(resourceType)

        return fhir4Parser.toFhir(clazz!!, source)
    }

    override fun fromResource(resource: Any): String? {
        return when (resource) {
            is Fhir3Resource -> fhir3Parser.fromFhir(resource)
            is Fhir4Resource -> fhir4Parser.fromFhir(resource)
            else -> throw CoreRuntimeException.InternalFailure()
        }
    }

}
