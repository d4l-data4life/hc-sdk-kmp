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
    id("kotlin")
    id("java")
    id("application")

    id("com.github.johnrengelman.shadow") version "5.2.0"
}

apply(from = "${project.rootDir}/gradle/jacoco-java.gradle.kts")

val d4lClientConfig = D4LConfigHelper.loadClientConfigAndroid("$rootDir")
val d4LTestConfig = D4LConfigHelper.loadTestConfigAndroid("$rootDir")

group = LibraryConfig.group

application {
    mainClassName = "care.data4life.sdk.sample.AppKt"
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

tasks.withType<Jar> {
    manifest {
        attributes["Main-Class"] = "AppKt"
    }
}


dependencies {
    implementation(project(":sdk-jvm")) {
        exclude(group = "care.data4life", module = "securestore-android")
        exclude(group = "care.data4life", module = "crypto-android")
        exclude(group = "care.data4life.hc-util-sdk-kmp", module = "util-android")
    }
    implementation(project(":crypto-jvm"))

    implementation(Dependencies.Multiplatform.D4L.authJvm)
    implementation(Dependencies.Multiplatform.D4L.securestoreJvm)
    implementation(Dependencies.Multiplatform.D4L.fhirHelperJvm) {
        exclude(group = "care.data4life.hc-util-sdk-kmp", module = "util-jvm")
        exclude(group = "care.data4life.hc-fhir-sdk-java", module = "hc-fhir-sdk-java")
    }
    implementation(Dependencies.Multiplatform.D4L.utilJvm)
    implementation(Dependencies.Multiplatform.D4L.fhirSdk)

    implementation(Dependencies.Java.kotlinStdlibJdk8)
    implementation(Dependencies.Multiplatform.Coroutines.jdk)

    implementation(Dependencies.Java.koinCore)
    implementation(Dependencies.Java.moshi)
    implementation(Dependencies.Java.cmdClickt)
    implementation(Dependencies.Java.threeTenBP)

    testImplementation(Dependencies.Java.Test.koin)
}

val androidTestAssetsPath = "${projectDir}/src/androidTest/assets"
val assetsPath = "${projectDir}/src/main/resources"

val provideTestConfig: Task by tasks.creating {
    doLast {
        val asset = File(assetsPath)
        if (!asset.exists()) asset.mkdirs()
        File(assetsPath, "client_config.json").writeText(D4LConfigHelper.toJson(d4lClientConfig))
    }
}

tasks.named("clean") {
    doLast {
        delete("${androidTestAssetsPath}/client_config.json")
        delete("${assetsPath}/client_config.json")
    }
}
