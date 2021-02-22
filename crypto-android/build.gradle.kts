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
    id("com.android.library")
    id("kotlin-platform-android")
}

android {
    compileSdkVersion(LibraryConfig.android.compileSdkVersion)

    defaultConfig {
        minSdkVersion(LibraryConfig.android.minSdkVersion)
        targetSdkVersion(LibraryConfig.android.targetSdkVersion)

        versionCode = 1
        versionName = "${project.version}"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        testInstrumentationRunnerArguments(mapOf(
                "clearPackageData" to "true"
        ))

        buildTypes {
            buildTypes {
                getByName("debug"){
                    setMatchingFallbacks("debug","release")
                }
            }
        }
    }

    resourcePrefix("d4l_crypto_")

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android.txt"), "proguard-rules.pro")
        }
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

    lintOptions {
        isAbortOnError = false
    }

    compileOptions {
        isCoreLibraryDesugaringEnabled = false

        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

}

dependencies {
    coreLibraryDesugaring(Dependencies.Android.androidDesugar)

    expectedBy(project(":crypto-common"))

    implementation(Dependencies.Multiplatform.D4L.utilAndroid)
    implementation(Dependencies.Multiplatform.Kotlin.stdlibAndroid)
    implementation(Dependencies.Android.AndroidX.appCompat)
    implementation(Dependencies.Android.bouncyCastleJdk15)
    implementation(Dependencies.Android.moshi)
    compileOnly(Dependencies.java.javaXAnnotation)


    testImplementation(Dependencies.Android.Test.junit)
    testImplementation(Dependencies.Multiplatform.Test.Kotlin.testJvm)
    testImplementation(Dependencies.Multiplatform.Test.Kotlin.testJvmJunit)
    testImplementation(Dependencies.Multiplatform.Test.MockK.jdk)


    androidTestImplementation(Dependencies.Android.AndroidTest.runner)
    androidTestImplementation(Dependencies.Android.AndroidTest.espressoCore)
    androidTestImplementation(Dependencies.Multiplatform.Test.Kotlin.testJvm)
    androidTestImplementation(Dependencies.Multiplatform.Test.Kotlin.testJvmJunit)
    androidTestImplementation(Dependencies.Multiplatform.Test.MockK.android)
}

apply(from = "${project.rootDir}/gradle/deploy-android.gradle")
