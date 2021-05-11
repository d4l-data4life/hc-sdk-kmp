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

import care.data4life.crypto.GCKey
import care.data4life.sdk.lang.D4LException
import care.data4life.sdk.migration.Migration
import java.io.IOException

typealias Tags = Map<String, String>
typealias Annotations = List<String>
typealias EncryptedTagsAndAnnotations = List<String>

class TaggingContract {

    interface Service {
        fun appendDefaultTags(resource: Any, oldTags: Tags?): Tags

        fun getTagsFromType(resourceType: Class<out Any>): Tags
    }

    interface CryptoService {
        fun encryptTagsAndAnnotations(
            tags: Tags,
            annotations: Annotations,
            tagEncryptionKey: GCKey? = null
        ): EncryptedTagsAndAnnotations

        fun decryptTagsAndAnnotations(
            encryptedTagsAndAnnotations: EncryptedTagsAndAnnotations
        ): Pair<Tags, Annotations>

        @Throws(IOException::class)
        @Migration("This method should only be used for migration purpose.")
        fun encryptList(
            plainList: List<String>,
            encryptionKey: GCKey,
            prefix: String = ""
        ): MutableList<String>

        companion object {
            internal val IV = ByteArray(16)
        }
    }

    interface Converter {
        @Throws(D4LException::class)
        fun toTags(tagList: List<String>): Tags
    }

    // TODO: make this package internal
    interface Encoding {
        @Throws(D4LException::class)
        fun encode(tag: String): String

        @Migration("This method should only be used for migration purpose.")
        fun normalize(tag: String): String

        fun decode(encodedTag: String): String
    }

    companion object {
        const val ANNOTATION_KEY = "custom"
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
