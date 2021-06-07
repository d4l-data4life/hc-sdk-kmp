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
    kotlin("kapt")
    id("jacoco")
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
    api(Dependencies.Multiplatform.D4L.utilJvm)
    implementation(Dependencies.Multiplatform.Kotlin.stdlib)

    implementation(project(":securestore-jvm"))
    implementation(project(":crypto-jvm"))
    implementation(project(":auth-jvm"))
    implementation(Dependencies.Multiplatform.D4L.fhirSdk)
    implementation(Dependencies.Java.threeTenBP)

    compileOnly(Dependencies.Java.javaXAnnotation)

    implementation(Dependencies.Java.rxJava)

    implementation(Dependencies.Java.okHttp)
    implementation(Dependencies.Java.okHttpLoggingInterceptor)

    implementation(Dependencies.Java.retrofit)
    implementation(Dependencies.Java.retrofitConverterMoshi)
    implementation(Dependencies.Java.retrofitAdapterRxJava)

    implementation(Dependencies.Java.moshi)

    kapt(Dependencies.Java.moshiCodeGen)
    kaptTest(Dependencies.Java.moshiCodeGen)

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
