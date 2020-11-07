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

import care.data4life.crypto.GCKey
import care.data4life.fhir.stu3.model.DomainResource
import care.data4life.fhir.stu3.model.Extension
import care.data4life.sdk.lang.D4LException
import care.data4life.sdk.model.Meta


data class AppDataResource(
        val resource: CharArray?,
        var id: String? = null,
        val userInfo: Map<String, String>
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as AppDataResource

        if (resource != null) {
            if (other.resource == null) return false
            if (!resource.contentEquals(other.resource)) return false
        } else if (other.resource != null) return false
        if (id != other.id) return false
        if (userInfo != other.userInfo) return false

        return true
    }

    override fun hashCode(): Int {
        var result = resource?.contentHashCode() ?: 0
        result = 31 * result + (id?.hashCode() ?: 0)
        result = 31 * result + userInfo.hashCode()
        return result
    }
}

data class AppDataRecord(val appDataResource: AppDataResource,val meta: Meta)

data class DecryptedAppDataRecord(
        val identifier: String?,
        val appData: AppDataResource,
        val tags: HashMap<String, String>,
        val customCreationDate: String,
        val updatedDate: String?,
        val dataKey: GCKey,
        val modelVersion: Int
) {
    fun copyWithResource(appData: AppDataResource) = copy(appData = appData)
}
