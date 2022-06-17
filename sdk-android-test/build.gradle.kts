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

import care.data4life.gradle.core.config.D4LConfigHelper
import care.data4life.gradle.core.config.LibraryConfig
import care.data4life.gradle.core.dependency.Dependency
import care.data4life.gradle.core.config.Environment

plugins {
    id("com.android.application")
    id("kotlin-android")
}

val d4lClientConfig = D4LConfigHelper.loadClientConfigAndroid("$rootDir")
val d4LTestConfig = D4LConfigHelper.loadTestConfigAndroid("$rootDir")

android {
    compileSdkVersion(LibraryConfig.android.compileSdkVersion)

    defaultConfig {
        minSdkVersion(LibraryConfig.android.minSdkVersion)
        targetSdkVersion(LibraryConfig.android.targetSdkVersion)

        applicationId = "care.data4life.sdk.e2e"
        versionCode = 1
        versionName = "1.0"

        multiDexEnabled = true

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        testInstrumentationRunnerArguments["clearPackageData"] = "true"

        manifestPlaceholders.putAll(
            mapOf<String, Any>(
                "clientId" to d4lClientConfig[Environment.DEVELOPMENT].id,
                "clientSecret" to d4lClientConfig[Environment.DEVELOPMENT].secret,
                "redirectScheme" to d4lClientConfig[Environment.DEVELOPMENT].redirectScheme,
                "environment" to "${Environment.DEVELOPMENT}",
                "platform" to d4lClientConfig.platform,
                "debug" to "true"
            )
        )
    }

    buildTypes {
        buildTypes {
            getByName("debug") {
                setMatchingFallbacks("debug", "release")
            }
        }
        getByName("release") {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        isCoreLibraryDesugaringEnabled = false

        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    packagingOptions {
        exclude("META-INF/DEPENDENCIES")
        exclude("META-INF/*.kotlin_module")
    }

    lintOptions {
        isAbortOnError = false
    }

    testOptions {
        animationsDisabled = true

        unitTests.all {
            it.testLogging {
                events("passed", "skipped", "failed", "standardOut", "standardError")
            }
        }

        execution = "ANDROID_TEST_ORCHESTRATOR"
    }
}

dependencies {
    coreLibraryDesugaring(Dependency.Android.androidDesugar)

    implementation(project(":sdk-android")) {
        exclude(group = "org.threeten", module = "threetenbp")
        exclude(group = "care.data4life", module = "securestore-jvm")
        exclude(group = "care.data4life", module = "crypto-jvm")
        exclude(group = "care.data4life", module = "auth-jvm")
        exclude(group = "care.data4life.hc-util-sdk-kmp", module = "util")
        exclude(group = "care.data4life.hc-util-sdk-kmp", module = "error-jvm")
        exclude(group = "care.data4life.hc-fhir-helper-sdk-kmp", module = "fhir-helper")
    }

    implementation(Dependency.Android.threeTenABP)

    implementation(Dependency.Android.kotlinStdLib)

    implementation(Dependency.Android.AndroidX.appCompat)
    implementation(Dependency.Android.AndroidX.constraintLayout)
    implementation(Dependency.Android.AndroidX.browser)
    implementation(Dependency.Android.material)

    implementation(Dependency.Multiplatform.D4L.util)
    implementation(Dependency.Multiplatform.D4L.error)

    implementation(Dependency.Android.googlePlayServicesBase)


    testImplementation(Dependency.JvmTest.junit)

    androidTestUtil(Dependency.AndroidTest.orchestrator)

    androidTestImplementation(Dependency.AndroidTest.core)
    androidTestImplementation(Dependency.AndroidTest.runner)
    androidTestImplementation(Dependency.AndroidTest.rules)
    androidTestImplementation(Dependency.AndroidTest.extJUnit)

    androidTestImplementation(Dependency.MultiplatformTest.Kotlin.testJvm)
    androidTestImplementation(Dependency.MultiplatformTest.Kotlin.testJvmJunit)

    androidTestImplementation(Dependency.AndroidTest.espressoCore)
    androidTestImplementation(Dependency.AndroidTest.espressoIntents)
    androidTestImplementation(Dependency.AndroidTest.espressoWeb)

    androidTestImplementation(Dependency.AndroidTest.uiAutomator)
    androidTestImplementation(Dependency.AndroidTest.kakao)

    androidTestImplementation(Dependency.Jvm.okHttp)
    androidTestImplementation(Dependency.Jvm.okHttpLoggingInterceptor)
    androidTestImplementation(Dependency.Jvm.retrofit)

    androidTestImplementation(Dependency.Jvm.moshi)
    androidTestImplementation(Dependency.Jvm.gson)

    androidTestImplementation(Dependency.Multiplatform.D4L.fhirHelperAndroid) {
        exclude(group = "care.data4life.hc-util-sdk-kmp", module = "util")
        exclude(group = "care.data4life.hc-fhir-sdk-java", module = "hc-fhir-sdk-java")
    }
}

val androidTestAssetsPath = "${projectDir}/src/androidTest/assets"
val unitTestAssetsPath = "${projectDir}/src/test/assets"

val provideTestConfig: Task by tasks.creating {
    doLast {
        val androidTestAsset = File(androidTestAssetsPath)
        if (!androidTestAsset.exists()) androidTestAsset.mkdirs()
        File(androidTestAssetsPath, "test_config.json").writeText(
            D4LConfigHelper.toJson(
                d4LTestConfig
            )
        )
        val unitTestAsset = File(unitTestAssetsPath)
        if (!unitTestAsset.exists()) unitTestAsset.mkdirs()
        File(
            unitTestAssetsPath,
            "test_config.json"
        ).writeText(D4LConfigHelper.toJson(d4LTestConfig))
    }
}

tasks.named("clean") {
    doLast {
        delete("${androidTestAssetsPath}/test_config.json")
        delete("${unitTestAssetsPath}/test_config.json")
    }
}
