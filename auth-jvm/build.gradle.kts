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
    id("kotlin-platform-jvm")
    id("java-library")
    kotlin("kapt")
}

apply(from = "${project.rootDir}/gradle/jacoco.gradle.kts")
apply(from = "${project.rootDir}/gradle/deploy-java.gradle")

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

kotlin {
    tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        kotlinOptions.jvmTarget = "1.8"
    }
}

dependencies {
    expectedBy(project(":auth-common"))

    implementation(project(":securestore-jvm"))
    implementation(Dependencies.Multiplatform.D4L.util)

    implementation(Dependencies.Multiplatform.Kotlin.stdlibJdk8)

    implementation(Dependencies.Java.scribeCore)

    implementation(Dependencies.Java.moshi)
    kapt(Dependencies.Java.moshiCodeGen)
    kaptTest(Dependencies.Java.moshiCodeGen)

    testImplementation(Dependencies.Java.Test.junit)
    testImplementation(Dependencies.Multiplatform.Test.Kotlin.testJvm)
    testImplementation(Dependencies.Multiplatform.Test.Kotlin.testJvmJunit)
    testImplementation(Dependencies.Multiplatform.Test.MockK.jdk)
}
