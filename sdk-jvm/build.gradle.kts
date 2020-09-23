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

plugins {
    id("com.github.johnrengelman.shadow") version "4.0.1"
    id("java-library")
    id("maven-publish")
}

apply(from = "${project.rootDir}/gradle/jacoco-java.gradle")
apply(from = "${project.rootDir}/gradle/deploy-java.gradle")


version = LibraryConfig.version
group = LibraryConfig.group


java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

dependencies {
    api(project(":sdk-core")) {
        exclude(group = "care.data4life", module = "securestore-android")
        exclude(group = "care.data4life", module = "crypto-android")
        exclude(group = "care.data4life", module = "auth-android")
        exclude(group = "care.data4life.hc-util-sdk-kmp", module = "util-android")
    }
    implementation(project(":securestore-jvm"))
    implementation(project(":crypto-jvm"))
    implementation(project(":auth-jvm"))
    implementation(Dependency.Multiplatform.D4L.utilJvm)
    implementation(Dependency.Multiplatform.D4L.fhirSdk)

    implementation(Dependency.Java.threeTenBP)
    implementation(Dependency.Java.rxJava)
    implementation(Dependency.Java.moshi)

    compileOnly(Dependency.java.javaXAnnotation)

    testImplementation(Dependency.Java.Test.junit)
    testImplementation(Dependency.Java.Test.mockitoCore)
    testImplementation(Dependency.Java.Test.truth)
    testImplementation(Dependency.Java.Test.jsonAssert)
}

tasks {
    named<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar>("shadowJar") {
        exclude("bcprov-jdk15on-1.64.jar")
    }
}
