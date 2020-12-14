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

import care.data4life.crypto.GCKey
import care.data4life.crypto.error.CryptoException.DecryptionFailed
import care.data4life.crypto.error.CryptoException.EncryptionFailed
import care.data4life.fhir.Fhir
import care.data4life.fhir.FhirParser
import care.data4life.fhir.stu3.model.FhirElementFactory
import care.data4life.sdk.CryptoService
import care.data4life.sdk.lang.D4LException
import io.reactivex.Single

// TODO remove @JvmOverloads when Data4LifeClient changed to Kotlin
// TODO use of Single is not necessary as it's finalized with blockingGet()
// TODO internal
class FhirService @JvmOverloads constructor(
        private val cryptoService: CryptoService,
        private val parserFhir3: FhirParser<Any> = Fhir().createStu3Parser(),
        private val parserFhir4: FhirParser<Any> = Fhir().createR4Parser()
) {

    @Suppress("UNCHECKED_CAST")
    fun <T : Fhir3Resource> decryptResource(dataKey: GCKey, resourceType: String, encryptedResource: String): T {
        return Single
                .just(encryptedResource)
                .filter { encResource: String -> !encResource.isBlank() }
                .map { encResource: String -> cryptoService.decryptString(dataKey, encResource).blockingGet() }
                .map<Any> { decryptedResourceJson: String ->
                    parserFhir3.toFhir(FhirElementFactory.getClassForFhirType(resourceType), decryptedResourceJson)
                }
                .toSingle()
                .onErrorResumeNext { error ->
                    Single.error(
                            DecryptionFailed("Failed to decrypt resource", error) as D4LException)
                }
                .blockingGet() as T
    }



    fun <T : Fhir3Resource> encryptResource(dataKey: GCKey, resource: T): String {
        return Single
                .just(resource)
                .map { fhirObject: T -> parserFhir3.fromFhir(fhirObject) }
                .flatMap { json: String -> cryptoService.encryptString(dataKey, json) }
                .onErrorResumeNext { error ->
                    Single.error(
                            EncryptionFailed("Failed to encrypt resource", error) as D4LException)
                }
                .blockingGet()
    }

}
