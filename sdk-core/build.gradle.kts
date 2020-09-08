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

version = LibraryConfig.version
group = LibraryConfig.group


java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

dependencies {
    api(Dependency.Multiplatform.D4L.utilJvm)
    implementation(Dependency.Multiplatform.Kotlin.stdlib)

    implementation(project(":securestore-jvm"))
    implementation(project(":crypto-jvm"))
    implementation(project(":auth-jvm"))
    implementation(Dependency.Multiplatform.D4L.fhirSdk)
    implementation(Dependency.Java.threeTenBP)

    compileOnly(Dependency.java.javaXAnnotation)

    implementation(Dependency.Java.rxJava)

    implementation(Dependency.Java.okHttp)
    implementation(Dependency.Java.okHttpLoggingInterceptor)

    implementation(Dependency.Java.retrofit)
    implementation(Dependency.Java.retrofitConverterMoshi)
    implementation(Dependency.Java.retrofitAdapterRxJava)

    testImplementation(Dependency.Multiplatform.D4L.fhirHelperJvm)
    testImplementation(Dependency.Java.Test.junit)

    testImplementation(Dependency.Java.Test.mockitoInline)
    testImplementation(Dependency.Java.Test.truth)

    testImplementation(Dependency.Multiplatform.Test.MockK.jdk)

    testImplementation(Dependency.Java.Test.okHttpMockWebServer)
    testImplementation(Dependency.Java.Test.jsonAssert)
}
