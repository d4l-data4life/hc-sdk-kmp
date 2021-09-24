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

package care.data4life.gradle.core.dependency

object GradlePlugin {
    const val android = "com.android.tools.build:gradle:${Version.GradlePlugin.android}"
    const val kotlin = "org.jetbrains.kotlin:kotlin-gradle-plugin:${Version.GradlePlugin.kotlin}"

    const val dexcount = "com.getkeepsafe.dexcount:dexcount-gradle-plugin:${Version.GradlePlugin.dexcount}"

    const val downloadTask = "de.undercouch:gradle-download-task:${Version.GradlePlugin.downloadTask}"

    const val dokka = "org.jetbrains.dokka:dokka-gradle-plugin:${Version.GradlePlugin.dokka}"

    const val groovyAll = "org.codehaus.groovy:groovy-all:${Version.GradlePlugin.groovyAll}"

    const val owasp = "org.owasp:dependency-check-gradle:${Version.GradlePlugin.owasp}"

    const val japicmp = "me.champeau.gradle:japicmp-gradle-plugin:${Version.GradlePlugin.japicmp}"

    const val httpComponents = "org.apache.httpcomponents:httpclient:${Version.GradlePlugin.httpComponents}"

    const val androidMaven = "com.github.dcendents:android-maven-gradle-plugin:${Version.GradlePlugin.androidMaven}"

    const val androidJacoco = "com.vanniktech:gradle-android-junit-jacoco-plugin:${Version.GradlePlugin.androidJacoco}"
}
