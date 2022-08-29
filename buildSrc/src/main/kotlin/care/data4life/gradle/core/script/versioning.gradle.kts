/*
 * Copyright (c) 2021 D4L data4life gGmbH / All rights reserved.
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

package care.data4life.gradle.core.script

import care.data4life.gradle.gitversion.VersionDetails

/**
 * Versioning task to calculate the version based on git tags and branch names using [Gradle Git Version](https://github.com/d4l-data4life/gradle-git-version)
 *
 * Install:
 *
 * You need to add following dependencies to the buildSrc/build.gradle.kts
 *
 * dependencies {
 *     implementation("care.data4life.gradle.gitversion:gradle-git-version:0.12.4-d4l")
 * }
 *
 * and ensure that the gradlePluginPortal is available
 *
 * repositories {
 *     gradlePluginPortal()
 * }
 *
 * Now just add id("care.data4life.gradle.core.script.versioning") to your rootProject build.gradle.kts plugins
 *
 * plugins {
 *     id("care.data4life.gradle.core.script.versioning")
 * }
 *
 * Usage:
 *
 * Versions will be calculated based on the latest git tag v* and branch name. if no tag is present a git hash will be used instead
 *
 * Branch == main == tag (v0.1.0) -> uses latest tag -> v0.1.0
 * Branch == main -> uses latest tag (v0.1.0) + SNAPSHOT -> v0.1.0-SNAPSHOT
 * Branch == feature/branch_name -> uses latest tag (v0.1.0) + branch name + SNAPSHOT -> v0.1.0-branch_name
 * Branch == feature/[SDK-123]/branch_name -> uses latest tag (v0.1.0) + branch name + SNAPSHOT -> v0.1.0-branch_name
 *
 * Review the generated version:
 * - ./gradlew versionInfo
 */
plugins {
    id("care.data4life.git-version")
}

val versionDetails: groovy.lang.Closure<VersionDetails> by extra
val patternNoQualifierBranch = "main|release/.*".toRegex()
val patternFeatureBranch = "feature/(.*)".toRegex()
val patternDependabotBranch = "dependabot/(.*)".toRegex()
val patternTicketNumber = "[A-Z]{2,8}-.*/(.*)".toRegex()

fun versionName(): String {
    val details = versionDetails()

    return when {
        details.branchName == null -> versionNameWithQualifier(details)
        patternNoQualifierBranch.matches(details.branchName) -> versionNameWithQualifier(details)
        patternFeatureBranch.matches(details.branchName) -> versionNameFeature(details)
        patternDependabotBranch.matches(details.branchName) -> versionNameDependabot(details)
        else -> throw UnsupportedOperationException("branch name not supported: ${details.branchName}")
    }
}

fun versionNameFeature(details: VersionDetails): String {
    var featureName = patternFeatureBranch.matchEntire(details.branchName)!!.groups[1]!!.value

    if (patternTicketNumber.matches(featureName)) {
        featureName = patternTicketNumber.matchEntire(featureName)!!.groups[1]!!.value
    }

    return versionNameWithQualifier(details, featureName)
}

fun versionNameDependabot(details: VersionDetails): String {
    var dependabotName = patternDependabotBranch.matchEntire(details.branchName)!!.groups[1]!!.value

    dependabotName = dependabotName
        .replace("_", "-")
        .replace("/", "-")

    return versionNameWithQualifier(details, "bump-$dependabotName")
}

fun versionNameWithQualifier(
    details: VersionDetails,
    name: String = ""
): String {
    val version = if (!details.isCleanTag) {
        var versionCleaned = details.version.substringBefore(".dirty")
        if (details.commitDistance > 0) {
            versionCleaned = versionCleaned.substringBefore("-")
        }
        if (name.isBlank()) {
            "${versionCleaned}-SNAPSHOT"
        } else {
            "${versionCleaned}-${name}-SNAPSHOT"
        }
    } else {
        details.version
    }

    return version.substringAfter("v")
}

val versionInfo: Task by tasks.creating() {
    group = "versioning"

    doLast {
        println("VersionName: ${versionName()}")
        println("VersionDetails: ${versionDetails()}")
    }
}

allprojects {
    version = versionName()
}
