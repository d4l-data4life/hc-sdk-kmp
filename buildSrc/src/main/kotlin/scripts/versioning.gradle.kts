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
package scripts

/**
 * Usage:
 *
 * You need to add following dependencies to the buildSrc/build.gradle.kts
 *
 * dependencies {
 *     implementation("com.palantir.gradle.gitversion:gradle-git-version:0.12.3")
 * }
 *
 * and ensure that the gradlePluginPortal is available
 *
 * repositories {
 *     gradlePluginPortal()
 * }
 *
 * Now just add id("scripts.versioning") to your rootProject build.gradle.kts plugins
 *
 * plugins {
 *     id("scripts.versioning")
 * }
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
 *
 */
plugins {
    id("com.palantir.git-version")
}

val versionDetails: groovy.lang.Closure<com.palantir.gradle.gitversion.VersionDetails> by extra
val patternNoQualifierBranch = "main|release/.*".toRegex()
val patternFeatureBranch = "feature/(.*)".toRegex()
val patternTicketNumber = "[A-Z]{2,8}-.*/(.*)".toRegex()

fun versionName(): String {
    val details = versionDetails()

    return when {
        details.branchName == null -> versionNameWithQualifier(details)
        patternNoQualifierBranch.matches(details.branchName) -> versionNameWithQualifier(details)
        patternFeatureBranch.matches(details.branchName) -> versionNameFeature(details)
        else -> throw UnsupportedOperationException("branch name not supported: ${details.branchName}")
    }
}

fun versionNameFeature(details: com.palantir.gradle.gitversion.VersionDetails): String {
    var featureName = patternFeatureBranch.matchEntire(details.branchName)!!.groups[1]!!.value

    if (patternTicketNumber.matches(featureName)) {
        featureName = patternTicketNumber.matchEntire(featureName)!!.groups[1]!!.value
    }

    return versionNameWithQualifier(details, featureName)
}

fun versionNameWithQualifier(
    details: com.palantir.gradle.gitversion.VersionDetails,
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
