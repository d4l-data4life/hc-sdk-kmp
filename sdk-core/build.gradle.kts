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
    id("java-library")
    id("kotlin")
    kotlin("kapt")

    // Publish
    id("care.data4life.gradle.core.script.publishing-config")
}

apply(from = "${project.rootDir}/gradle/jacoco-java.gradle.kts")
apply(from = "${project.rootDir}/gradle/deploy-java.gradle")

group = LibraryConfig.group

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
    api(Dependency.Multiplatform.D4L.utilJvm)
    api(Dependency.Multiplatform.D4L.resultErrorJvm)
    implementation(Dependency.Multiplatform.Kotlin.stdlib)

    implementation(Dependency.Multiplatform.D4L.authJvm)
    implementation(Dependency.Multiplatform.D4L.cryptoJvm)
    implementation(Dependency.Multiplatform.D4L.securestoreJvm)
    implementation(Dependency.Multiplatform.D4L.fhirSdk)
    implementation(Dependency.Java.threeTenBP)

    compileOnly(Dependency.Java.javaXAnnotation)

    implementation(Dependency.Java.rxJava)

    implementation(Dependency.Java.okHttp)
    implementation(Dependency.Java.okHttpLoggingInterceptor)

    implementation(Dependency.Java.retrofit)
    implementation(Dependency.Java.retrofitConverterMoshi)
    implementation(Dependency.Java.retrofitAdapterRxJava)

    implementation(Dependency.Java.moshi)

    kapt(Dependency.Java.moshiCodeGen)
    kaptTest(Dependency.Java.moshiCodeGen)

    testImplementation(Dependency.Multiplatform.D4L.fhirHelperJvm) {
        exclude(group = "care.data4life.hc-fhir-sdk-java", module = "hc-fhir-sdk-java")
    }
    testImplementation(Dependency.Java.Test.junit)
    testImplementation(Dependency.Java.Test.kotlinTest)

    testImplementation(Dependency.Java.Test.mockitoInline)
    testImplementation(Dependency.Java.Test.truth)

    testImplementation(Dependency.Multiplatform.Test.MockK.jdk)

    testImplementation(Dependency.Java.Test.okHttpMockWebServer)
    testImplementation(Dependency.Java.Test.jsonAssert)
}

configure<SourceSetContainer> {
    main {
        java.srcDirs("src/main/java", "src-gen/main/java")
    }
}

val templatesPath = "${projectDir}/src/main/resources/templates"
val configPath = "${projectDir}/src-gen/main/java/care/data4life/sdk/config"

val provideConfig: Task by tasks.creating {
    doFirst {
        val templates = File(templatesPath)
        val configs = File(configPath)

        val config = File(templates, "SDKConfig.tmpl")
            .readText()
            .replace("SDK_VERSION", version.toString())

        if (!configs.exists()) {
            if(!configs.mkdir()) {
                System.err.println("The script not able to create the config directory")
            }
        }
        File(configPath, "SDKConfig.kt").writeText(config)
    }
}

tasks.named("compileKotlin") {
    dependsOn(provideConfig)
}

tasks.named("clean") {
    doLast {
        delete("${configPath}/SDKConfig.kt")
    }
}
