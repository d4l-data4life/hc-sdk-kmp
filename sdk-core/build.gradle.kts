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
    id("java-library")
    id("kotlin")
}

apply(from = "${project.rootDir}/gradle/jacoco-java.gradle")
apply(from = "${project.rootDir}/gradle/deploy-java.gradle")

group = LibraryConfig.group


java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

dependencies {
    api(Dependencies.Multiplatform.D4L.utilJvm)
    implementation(Dependencies.Multiplatform.Kotlin.stdlib)

    implementation(project(":securestore-jvm"))
    implementation(project(":crypto-jvm"))
    implementation(project(":auth-jvm"))
    implementation(Dependencies.Multiplatform.D4L.fhirSdk)
    implementation(Dependencies.Java.threeTenBP)

    compileOnly(Dependencies.java.javaXAnnotation)

    implementation(Dependencies.Java.rxJava)

    implementation(Dependencies.Java.okHttp)
    implementation(Dependencies.Java.okHttpLoggingInterceptor)

    implementation(Dependencies.Java.retrofit)
    implementation(Dependencies.Java.retrofitConverterMoshi)
    implementation(Dependencies.Java.retrofitAdapterRxJava)

    testImplementation(Dependencies.Multiplatform.D4L.fhirHelperJvm) {
        exclude(group = "care.data4life.hc-fhir-sdk-java", module = "hc-fhir-sdk-java")
    }
    testImplementation(Dependencies.Java.Test.junit)
    testImplementation(Dependencies.Java.Test.kotlinTest)

    testImplementation(Dependencies.Java.Test.mockitoInline)
    testImplementation(Dependencies.Java.Test.truth)

    testImplementation(Dependencies.Multiplatform.Test.MockK.jdk)

    testImplementation(Dependencies.Java.Test.okHttpMockWebServer)
    testImplementation(Dependencies.Java.Test.jsonAssert)
}
