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
import care.data4life.sdk.data.DataResource
import care.data4life.sdk.lang.D4LException
import care.data4life.sdk.util.Base64
import care.data4life.sdk.wrapper.SdkFhirParser
import care.data4life.sdk.wrapper.WrapperContract
import io.reactivex.Single
import java.util.*
import kotlin.collections.HashMap

// TODO rename it in something like ResourceCryptoService
// TODO remove @JvmOverloads when Data4LifeClient changed to Kotlin
// TODO use of Single is not necessary as it's finalized with blockingGet()
// TODO internal
class FhirService @JvmOverloads constructor(
        private val cryptoService: CryptoService,
        private val parserFhir3: FhirParser<Any> = Fhir().createStu3Parser(),
        private val parserFhir4: FhirParser<Any> = Fhir().createR4Parser()
) : FhirContract.Service {
    private val parser: WrapperContract.FhirParser = SdkFhirParser

    @Deprecated("Use the new Api")
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

    @Deprecated("Use the new Api")
    fun <T : Fhir3Resource> encryptResource(
            dataKey: GCKey,
            resource: T
    ): String = _encryptResource(dataKey, resource)

    override fun _encryptResource(dataKey: GCKey, resource: Any): String {
        return if(resource is DataResource) {
            encryptDataResource(dataKey, resource)
        } else {
            encryptFhirResource(dataKey, resource)
        }
    }

    private fun encryptFhirResource(dataKey: GCKey, resource: Any): String {
        return Single
                .just(resource)
                .map { parser.fromResource(it) }
                .flatMap { cryptoService.encryptString(dataKey, it) }
                .onErrorResumeNext { error ->
                    Single.error(
                            EncryptionFailed("Failed to encrypt resource", error) as D4LException)
                }
                .blockingGet()
    }

    private fun encryptDataResource(dataKey: GCKey, resource: DataResource): String {
        return Base64.encodeToString(
                cryptoService.encrypt(
                        dataKey,
                        resource.asByteArray()
                ).blockingGet()
        )
    }

    override fun <T : Any> decryptResource(
            dataKey: GCKey,
            tags: HashMap<String, String>,
            encryptedResource: String
    ): T {
        return if(tags.containsKey(TAG_APPDATA_KEY)) {
            decryptData(dataKey, encryptedResource) as T
        } else  {
            decryptFhir(dataKey, tags[TAG_RESOURCE_TYPE]!!, tags, encryptedResource)
        }
    }

    private fun <T: Any> parseFhir(
            decryptedResourceJson: String,
            tags: HashMap<String, String>,
            resourceType: String
    ): T {
        return if( tags[TAG_FHIR_VERSION] == Fhir4Version.version ) {
            parser.toFhir4(resourceType, decryptedResourceJson)
        } else {
            parser.toFhir3(resourceType, decryptedResourceJson)
        } as T
    }

    private fun <T: Any> decryptFhir(
            dataKey: GCKey,
            resourceType: String,
            tags: HashMap<String, String>,
            encryptedResource: String
    ): T {
        return Single
                .just(encryptedResource)
                .filter { it.isNotBlank() }
                .map { cryptoService.decryptString(dataKey, it).blockingGet() }
                .map { parseFhir<T>(it, tags, resourceType) }
                .toSingle()
                .onErrorResumeNext { error ->
                    Single.error(
                            DecryptionFailed("Failed to decrypt resource", error) as D4LException)
                }
                .blockingGet()
    }

    private fun decryptData(
            dataKey: GCKey,
            encryptedResource: String
    ): DataResource = DataResource(
            cryptoService.decrypt(dataKey, Base64.decode(encryptedResource)).blockingGet()
    )

    //ToDo share it between the TaggingService and this class
    companion object {
        private val US_LOCALE = Locale.US
        private val TAG_RESOURCE_TYPE = "resourceType".toLowerCase(US_LOCALE)
        private val TAG_FHIR_VERSION = "fhirVersion".toLowerCase(US_LOCALE)
        private const val TAG_APPDATA_KEY = "flag"
    }

}
