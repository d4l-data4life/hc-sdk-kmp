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
package care.data4life.sdk.network.model

import care.data4life.sdk.network.model.NetworkModelContract.VersionStatus
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class VersionList(
    @field:Json(name = "versions")
    override val versions: List<Version>
) : NetworkModelContract.VersionList {

    private fun extractVersion(
        version: String
    ): String? = versionPattern.find(version)?.groups?.first()?.value

    private fun isKnownVersion(
        version: Version,
        currentVersion: String?
    ): Boolean = version.name == currentVersion

    override fun resolveSupportStatus(version: String): VersionStatus {
        val currentVersion = extractVersion(version)
        val knownVersion = versions.find { knownVersion -> isKnownVersion(knownVersion, currentVersion) }

        return when (knownVersion?.status) {
            "unsupported" -> VersionStatus.UNSUPPORTED
            "deprecated" -> VersionStatus.DEPRECATED
            else -> VersionStatus.SUPPORTED
        }
    }

    companion object {
        private val versionPattern = Regex("^(\\d+\\.)(\\d+\\.)(\\d)")
    }
}
