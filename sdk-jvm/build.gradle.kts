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

import care.data4life.gradle.core.config.LibraryConfig
import care.data4life.gradle.core.dependency.Dependency

plugins {
    id("com.github.johnrengelman.shadow") version "5.2.0"
    id("java-library")
    id("kotlin")
    id("maven-publish")
}

apply(from = "${project.rootDir}/gradle/jacoco-java.gradle.kts")
apply(from = "${project.rootDir}/gradle/deploy-java.gradle")

group = LibraryConfig.group

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

dependencies {
    api(project(":sdk-core")) {
        // TODO
        exclude(group = "care.data4life", module = "securestore-android")
        exclude(group = "care.data4life", module = "crypto-android")
        exclude(group = "care.data4life", module = "auth-android")
        exclude(group = "care.data4life.hc-util-sdk-kmp", module = "util-android")
        exclude(group = "care.data4life.hc-fhir-sdk-java", module = "hc-fhir-sdk-java")
    }
    implementation(Dependency.Multiplatform.D4L.authJvm)
    implementation(Dependency.Multiplatform.D4L.cryptoJvm)
    implementation(Dependency.Multiplatform.D4L.securestoreJvm)
    implementation(Dependency.Multiplatform.D4L.utilJvm)
    implementation(Dependency.Multiplatform.D4L.errorJvm)
    implementation(Dependency.jvm.fhirSdk)

    implementation(Dependency.Jvm.threeTenBP)
    implementation(Dependency.Jvm.rxJava)
    implementation(Dependency.Jvm.moshi)
    implementation(Dependency.Jvm.scribeCore)

    compileOnly(Dependency.Jvm.javaXAnnotation)

    testImplementation(Dependency.JvmTest.junit)
    testImplementation(Dependency.JvmTest.mockitoCore)
    testImplementation(Dependency.JvmTest.truth)
    testImplementation(Dependency.JvmTest.jsonAssert)
}

tasks {
    named<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar>("shadowJar") {
        exclude("bcprov-jdk18on-1.71.jar")
    }
}
