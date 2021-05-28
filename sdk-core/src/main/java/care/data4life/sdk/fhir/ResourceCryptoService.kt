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
import care.data4life.sdk.crypto.CryptoContract
import care.data4life.sdk.data.DataContract
import care.data4life.sdk.data.DataResource
import care.data4life.sdk.tag.TaggingContract.Companion.TAG_APPDATA_KEY
import care.data4life.sdk.tag.TaggingContract.Companion.TAG_FHIR_VERSION
import care.data4life.sdk.tag.TaggingContract.Companion.TAG_RESOURCE_TYPE
import care.data4life.sdk.tag.Tags
import care.data4life.sdk.wrapper.SdkFhirParser
import care.data4life.sdk.wrapper.WrapperContract

// TODO internal
class ResourceCryptoService constructor(
    private val cryptoService: CryptoContract.Service
) : FhirContract.CryptoService {
    private val parser: WrapperContract.FhirParser = SdkFhirParser

    override fun encryptResource(dataKey: GCKey, resource: Any): String {
        return if (resource is DataContract.Resource) {
            encryptDataResource(dataKey, resource)
        } else {
            encryptFhirResource(dataKey, resource)
        }
    }

    private fun propagateEncryptionErrors(encryption: () -> String): String {
        return try {
            encryption()
        } catch (error: Exception) {
            throw EncryptionFailed("Failed to encrypt resource", error)
        }
    }

    private fun encryptFhirResource(dataKey: GCKey, resource: Any): String {
        return propagateEncryptionErrors {
            parser.fromResource(resource).let { serializedResource ->
                cryptoService.encryptAndEncodeString(
                    dataKey,
                    serializedResource
                ).blockingGet()
            }
        }
    }

    private fun encryptDataResource(
        dataKey: GCKey,
        resource: DataContract.Resource
    ): String {
        return propagateEncryptionErrors {
            cryptoService.encryptAndEncodeByteArray(
                dataKey,
                resource.asByteArray()
            ).blockingGet()
        }
    }

    override fun <T : Any> decryptResource(
        dataKey: GCKey,
        tags: Tags,
        encryptedResource: String
    ): T {
        validateEncryptedResource(encryptedResource)

        return if (tags.containsKey(TAG_APPDATA_KEY)) {
            decryptData(dataKey, encryptedResource) as T
        } else {
            decryptFhir(dataKey, tags[TAG_RESOURCE_TYPE]!!, tags, encryptedResource)
        }
    }

    private fun validateEncryptedResource(encryptedResource: String) {
        if (encryptedResource.isBlank()) {
            throw DecryptionFailed("Failed to decrypt resource")
        }
    }

    private fun <T> propagateDecryptionErrors(decryption: () -> T): T {
        return try {
            decryption()
        } catch (error: Exception) {
            throw DecryptionFailed("Failed to decrypt resource", error)
        }
    }

    private fun <T : Any> decryptFhir(
        dataKey: GCKey,
        resourceType: String,
        tags: Tags,
        encryptedResource: String
    ): T {
        return propagateDecryptionErrors {
            parseFhir(
                cryptoService.decodeAndDecryptString(dataKey, encryptedResource).blockingGet(),
                tags,
                resourceType
            )
        }
    }

    private fun <T : Any> parseFhir(
        decryptedResourceJson: String,
        tags: Tags,
        resourceType: String
    ): T {
        return if (tags[TAG_FHIR_VERSION] == FhirContract.FhirVersion.FHIR_4.version) {
            parser.toFhir4(resourceType, decryptedResourceJson)
        } else {
            parser.toFhir3(resourceType, decryptedResourceJson)
        } as T
    }

    private fun decryptData(
        dataKey: GCKey,
        encryptedResource: String
    ): DataContract.Resource {
        return try {
            DataResource(
                cryptoService.decodeAndDecryptByteArray(
                    dataKey,
                    encryptedResource
                ).blockingGet()
            )
        } catch (error: Exception) {
            throw DecryptionFailed("Failed to decrypt resource", error)
        }
    }
}
