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
import care.data4life.crypto.error.CryptoException
import care.data4life.sdk.CryptoService
import care.data4life.sdk.data.DataResource
import care.data4life.sdk.lang.D4LException
import care.data4life.sdk.util.Base64
import care.data4life.sdk.wrapper.FhirParser
import care.data4life.sdk.wrapper.ResourceFactory
import care.data4life.sdk.wrapper.WrapperContract
import io.reactivex.Single
import java.util.HashMap

// TODO use of Single is not necessary as it's finalized with blockingGet()
// TODO internal
class FhirService constructor(
        private val cryptoService: CryptoService
): FhirContract.Service {
    private val parser = FhirParser

    @Deprecated("Use the Contract method")
    @Suppress("UNCHECKED_CAST")
    fun <T : Fhir3Resource> decryptResource(dataKey: GCKey, resourceType: String, encryptedResource: String): T {
        TODO()
        /*return Single
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
                .blockingGet() as T*/
    }


    @Deprecated("Use the Contract method")
    fun <T : Fhir3Resource> encryptResource(
            dataKey: GCKey,
            resource: T
    ): String = encryptResource(dataKey, ResourceFactory.wrap(resource)!!)

    override fun encryptResource(dataKey: GCKey, resource: WrapperContract.Resource): String {
        return if(resource.type == WrapperContract.Resource.TYPE.DATA) {
            encryptDataResource(dataKey, resource)
        } else {
            encryptFhirResource(dataKey, resource)
        }
    }

    private fun encryptFhirResource(dataKey: GCKey, resource: WrapperContract.Resource): String {
        return Single
                .just(resource)
                .map {parser.fromResource(it) }
                .flatMap { json: String -> cryptoService.encryptString(dataKey, json) }
                .onErrorResumeNext { error ->
                    Single.error(
                            CryptoException.EncryptionFailed("Failed to encrypt resource", error) as D4LException)
                }
                .blockingGet()
    }

    private fun encryptDataResource(dataKey: GCKey, resource: WrapperContract.Resource): String {
        return Base64.encodeToString(
                cryptoService.encrypt(
                        dataKey,
                        (resource.unwrap() as DataResource).asByteArray()
                ).blockingGet()
        )
    }

    override fun decryptResource(
            dataKey: GCKey,
            tags: HashMap<String, String>,
            encryptedResource: String
    ): WrapperContract.Resource {
        TODO("Not yet implemented")
    }

}
