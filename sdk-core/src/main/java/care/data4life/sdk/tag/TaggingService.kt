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
package care.data4life.sdk.tag

import care.data4life.sdk.fhir.Fhir3Resource
import care.data4life.sdk.fhir.Fhir3Version
import care.data4life.sdk.fhir.Fhir4Resource
import care.data4life.sdk.fhir.Fhir4Version
import care.data4life.sdk.wrapper.SdkFhirElementFactory
import care.data4life.sdk.wrapper.WrapperContract
import java.util.*

// TODO internal
class TaggingService(
        private val clientId: String
) : TaggingContract.Service {
    private val partnerId: String = clientId.substringBefore(SEPARATOR)
    private val fhirElementFactory: WrapperContract.FhirElementFactory = SdkFhirElementFactory

    private fun appendCommonDefaultTags(
            resourceType: String?,
            oldTags: HashMap<String, String>?
    ): HashMap<String, String> {
        val tags = hashMapOf<String, String>()
        if (oldTags != null && oldTags.isNotEmpty()) {
            tags.putAll(oldTags)
        }
        if (resourceType != null && resourceType.isNotEmpty()) {
            tags[TAG_RESOURCE_TYPE] = resourceType.toLowerCase(US_LOCALE)
        }
        if (!tags.containsKey(TAG_CLIENT)) {
            tags[TAG_CLIENT] = clientId
        } else {
            tags[TAG_UPDATED_BY_CLIENT] = clientId
        }
        if (!tags.containsKey(TAG_PARTNER)) {
            tags[TAG_PARTNER] = partnerId
        } else {
            tags[TAG_UPDATED_BY_PARTNER] = partnerId
        }
        return tags
    }

    override fun appendDefaultTags(
            resource: Any,
            oldTags: HashMap<String, String>?
    ): HashMap<String, String> {
        return when (resource) {
            is Fhir3Resource -> appendCommonDefaultTags(resource.resourceType, oldTags).also {
                if (!it.containsKey(TAG_FHIR_VERSION)) {
                    it[TAG_FHIR_VERSION] = Fhir3Version.version
                }
            }
            is Fhir4Resource -> appendCommonDefaultTags(resource.resourceType, oldTags).also {
                if (!it.containsKey(TAG_FHIR_VERSION)) {
                    it[TAG_FHIR_VERSION] = Fhir4Version.version
                }
            }
            else -> appendCommonDefaultTags(null, oldTags).also {
                it[TAG_APPDATA_KEY] = TAG_APPDATA_VALUE
            }
        }
    }

    override fun getTagFromType(
            resourceType: Class<Any>?
    ): HashMap<String, String> {
        return hashMapOf<String, String>().also {
            if (resourceType == null) {
                it[TAG_APPDATA_KEY] = TAG_APPDATA_VALUE
            } else {
                it[TAG_RESOURCE_TYPE] = fhirElementFactory.getFhirTypeForClass(resourceType)!!.toLowerCase(US_LOCALE)
            }
        }
    }

    companion object {
        private val US_LOCALE = Locale.US
        const val TAG_DELIMITER = "="
        val TAG_RESOURCE_TYPE = "resourceType".toLowerCase(US_LOCALE)
        const val TAG_CLIENT = "client"
        private val TAG_UPDATED_BY_CLIENT = "updatedByClient".toLowerCase(US_LOCALE)
        private const val TAG_PARTNER = "partner"
        private val TAG_UPDATED_BY_PARTNER = "updatedByPartner".toLowerCase(US_LOCALE)
        private val TAG_FHIR_VERSION = "fhirVersion".toLowerCase(US_LOCALE)
        private const val TAG_APPDATA_KEY = "flag"
        private const val TAG_APPDATA_VALUE = "appdata"
        private const val SEPARATOR = "#"
    }

}
