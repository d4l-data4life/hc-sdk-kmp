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

buildscript {
    repositories {
        google()
        mavenCentral()
        jcenter()
    }

    dependencies {
        classpath(GradlePlugin.android)
        classpath(GradlePlugin.kotlin)
        classpath(GradlePlugin.kapt)

        // https://github.com/vanniktech/gradle-android-junit-jacoco-plugin
        classpath("com.vanniktech:gradle-android-junit-jacoco-plugin:0.16.0")

        classpath("org.codehaus.groovy:groovy-all:2.4.15")

        // https://github.com/dcendents/android-maven-gradle-plugin
        classpath("com.github.dcendents:android-maven-gradle-plugin:2.1")

        classpath(GradlePlugin.dexcount)

        // https://github.com/melix/japicmp-gradle-plugin
        classpath("me.champeau.gradle:japicmp-gradle-plugin:0.2.9")

        classpath(GradlePlugin.downloadTask)
        classpath("org.apache.httpcomponents:httpclient:4.5.11")

        classpath(GradlePlugin.dokka)

        // https://github.com/jeremylong/dependency-check-gradle
        classpath("org.owasp:dependency-check-gradle:5.3.0")
    }
}

plugins {
    id("scripts.dependency-updates")
    id("scripts.download-scripts")
    id("scripts.versioning")
    id("scripts.quality-spotless")
    id("scripts.publishing")
}

allprojects {
    repositories {
        google()
        mavenCentral()

        gitHub(project)

        d4l()

        jitPack()

        bintray()
    }

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
        exclude(group = "care.data4life.hc-result-sdk-kmp", module = "error-android-debug")
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
    gradleVersion = "6.9.1"
    distributionType = Wrapper.DistributionType.ALL
}
