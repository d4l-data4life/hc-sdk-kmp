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

import care.data4life.sdk.fhir.FhirContract
import care.data4life.sdk.lang.D4LException
import java.io.IOException
import kotlin.collections.HashMap

typealias Tags = HashMap<String, String>

class TaggingContract {

    interface Service {
        fun appendDefaultTags(
                resource: Any,
                oldTags: HashMap<String, String>?
        ): HashMap<String, String>

        fun tagVersion(tags: Tags, version: FhirContract.FhirVersion)

        fun getTagsFromType(resourceType: Class<Any>?): Tags

    }

    interface EncryptionService {
        @Throws(IOException::class)
        fun encryptTags(tags: Tags): MutableList<String>

        @Throws(IOException::class)
        fun decryptTags(encryptedTags: List<String>):Tags

        @Throws(IOException::class)
        fun encryptAnnotations(annotations: List<String>): MutableList<String>

        @Throws(IOException::class)
        fun decryptAnnotations(encryptedAnnotations: List<String>): List<String>
    }

    interface Helper {
        fun convertToTagMap(tagList: List<String>): HashMap<String, String>

        @Throws(D4LException::class)
        fun encode(tag: String): String

        fun decode(encodedTag: String): String
    }

    companion object {
        const val DELIMITER = "="
        const val TAG_RESOURCE_TYPE = "resourcetype"
        const val TAG_CLIENT = "client"
        const val TAG_UPDATED_BY_CLIENT = "updatedbyclient"
        const val TAG_PARTNER = "partner"
        const val TAG_UPDATED_BY_PARTNER = "updatedbypartner"
        const val TAG_FHIR_VERSION = "fhirversion"
        const val TAG_APPDATA_KEY = "flag"
        const val TAG_APPDATA_VALUE = "appdata"
        const val SEPARATOR = "#"
    }
}
