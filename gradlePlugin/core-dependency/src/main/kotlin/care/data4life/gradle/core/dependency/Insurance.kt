/* ktlint-disable filename */
/*
 * Copyright (c) 2022 D4L data4life gGmbH / All rights reserved.
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

package care.data4life.gradle.core.dependency

import org.gradle.api.Project

private val modules = listOf(
    "kotlin-stdlib-jdk7",
    "kotlin-stdlib-jdk8",
    "kotlin-stdlib",
    "kotlin-stdlib-common",
    "kotlin-reflect"
)

// Taken from https://github.com/bitPogo/kmock/blob/main/gradlePlugin/kmock-dependency/src/main/kotlin/tech/antibytes/gradle/kmock/dependency/Insurance.kt
fun Project.ensureKotlinVersion(version: String? = null) {
    configurations.all {
        resolutionStrategy.eachDependency {
            if (requested.group == "org.jetbrains.kotlin" && requested.name in modules) {
                useVersion(version ?: Version.kotlin)
                because("Avoid resolution conflicts")
            }
        }
    }
}
