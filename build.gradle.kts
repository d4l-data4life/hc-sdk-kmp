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

import care.data4life.gradle.core.dependency.bintray
import care.data4life.gradle.core.dependency.d4l
import care.data4life.gradle.core.dependency.ensureKotlinVersion
import care.data4life.gradle.core.dependency.gitHub
import care.data4life.gradle.core.dependency.jitPack

plugins {
    id("care.data4life.gradle.core.dependency")

    id("care.data4life.gradle.core.script.dependency-updates")
    id("care.data4life.gradle.core.script.download-scripts")
    id("care.data4life.gradle.core.script.publishing")
    id("care.data4life.gradle.core.script.quality-spotless")
    id("care.data4life.gradle.core.script.versioning")
}

allprojects {
    repositories {
        google()
        mavenCentral()

        gitHub(project)

        d4l()

        bintray()
        jitPack()
    }

    ensureKotlinVersion()

    apply(plugin = "org.owasp.dependencycheck")

    apply(plugin = "org.jetbrains.dokka")

    val dokka by tasks.getting(org.jetbrains.dokka.gradle.DokkaTask::class) {
        outputFormat = "html"
        outputDirectory = "$buildDir/dokka"
    }

    val dokkaJar by tasks.creating(Jar::class) {
        group = JavaBasePlugin.DOCUMENTATION_GROUP
        description = "Assembles Kotlin docs with Dokka"
        archiveClassifier.set("javadoc")
        from(dokka)
        dependsOn(dokka)
    }

    configurations.all {
        exclude(group = "care.data4life.hc-util-sdk-kmp", module = "util-android-debug")
        exclude(group = "care.data4life.hc-securestore-sdk-kmp", module = "securestore-android-debug")
        exclude(group = "care.data4life.hc-util-sdk-kmp", module = "error-android-debug")
    }
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}

tasks.withType<JavaCompile> {
    sourceCompatibility = "1.8"
    targetCompatibility = "1.8"
}

tasks.named<Wrapper>("wrapper") {
    gradleVersion = "7.5.1"
    distributionType = Wrapper.DistributionType.ALL
}
