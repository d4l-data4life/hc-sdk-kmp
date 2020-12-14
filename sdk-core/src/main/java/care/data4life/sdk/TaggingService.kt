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
package care.data4life.sdk

import care.data4life.sdk.model.ModelVersion
import java.util.*

// TODO internal
class TaggingService(
        private val clientId: String
) {
    private val partnerId: String = clientId.substringBefore(SEPARATOR)

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

    fun appendDefaultTags(
            resourceType: String?,
            oldTags: HashMap<String, String>?
    ): HashMap<String, String> = appendCommonDefaultTags(resourceType, oldTags).also {
        if (!it.containsKey(TAG_FHIR_VERSION)) {
            it[TAG_FHIR_VERSION] = ModelVersion.FHIR_VERSION
        }
    }

    fun appendAppDataTags(
            tags: HashMap<String, String>?
    ): HashMap<String, String>? = tags.also {
        if (it != null) {
            it[TAG_APPDATA_KEY] = TAG_APPDATA_VALUE
        }
    }

    fun appendDefaultAnnotatedTags(
            resourceType: String?,
            oldTags: HashMap<String, String>?
    ): HashMap<String, String> = appendAppDataTags(appendCommonDefaultTags(resourceType, oldTags))!!

    fun getTagFromType(
            resourceType: String?
    ): HashMap<String, String> = hashMapOf<String, String>().also {
        if (resourceType != null && resourceType.isNotEmpty()) {
            it[TAG_RESOURCE_TYPE] = resourceType.toLowerCase(US_LOCALE)
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
