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

import java.net.URI

buildscript {
    repositories {
        google()
        mavenCentral()
        jcenter()

        maven("https://dl.bintray.com/data4life/maven")
    }

    dependencies {
        classpath(Dependency.GradlePlugin.android)
        classpath(Dependency.GradlePlugin.kotlin)

        // https://github.com/vanniktech/gradle-android-junit-jacoco-plugin
        classpath("com.vanniktech:gradle-android-junit-jacoco-plugin:0.16.0")

        classpath("org.codehaus.groovy:groovy-all:2.4.15")

        // https://github.com/dcendents/android-maven-gradle-plugin
        classpath("com.github.dcendents:android-maven-gradle-plugin:2.1")

        classpath(Dependency.GradlePlugin.dexcount)

        // https://github.com/melix/japicmp-gradle-plugin
        classpath("me.champeau.gradle:japicmp-gradle-plugin:0.2.9")

        classpath(Dependency.GradlePlugin.downloadTask)
        classpath("org.apache.httpcomponents:httpclient:4.5.11")

        classpath(Dependency.GradlePlugin.dokka)

        // https://github.com/jeremylong/dependency-check-gradle
        classpath("org.owasp:dependency-check-gradle:5.3.0")

        // https://github.com/d4l-data4life/gradle-git-publish
        classpath("care.data4life:gradle-git-publish:3.2.0")
    }
}

plugins {
    // https://github.com/ben-manes/gradle-versions-plugin
    id("com.github.ben-manes.versions") version "0.36.0"
}

allprojects {
    repositories {
        google()
        jcenter()
        maven("https://jitpack.io")
        maven {
            url = URI("https://maven.pkg.github.com/d4l-data4life/hc-util-sdk-kmp")
            credentials {
                username = project.findProperty("gpr.user") as String? ?: System.getenv("PACKAGE_REGISTRY_USERNAME")
                password = project.findProperty("gpr.key") as String? ?: System.getenv("PACKAGE_REGISTRY_TOKEN")
            }
        }
        maven {
            url = URI("https://maven.pkg.github.com/d4l-data4life/hc-fhir-sdk-java")
            credentials {
                username = project.findProperty("gpr.user") as String? ?: System.getenv("PACKAGE_REGISTRY_USERNAME")
                password = project.findProperty("gpr.key") as String? ?: System.getenv("PACKAGE_REGISTRY_TOKEN")
            }
        }
        maven {
            url = URI("https://maven.pkg.github.com/d4l-data4life/hc-fhir-helper-sdk-kmp")
            credentials {
                username = project.findProperty("gpr.user") as String? ?: System.getenv("PACKAGE_REGISTRY_USERNAME")
                password = project.findProperty("gpr.key") as String? ?: System.getenv("PACKAGE_REGISTRY_TOKEN")
            }
        }
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
}

tasks.register("clean", Delete::class.java) {
    delete(rootProject.buildDir)
}

tasks.named<Wrapper>("wrapper") {
    gradleVersion = "6.7.1"
    distributionType = Wrapper.DistributionType.ALL
}


