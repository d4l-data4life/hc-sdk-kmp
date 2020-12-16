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
    id("com.github.dcendents.android-maven")
}

android {
    compileSdkVersion(AndroidConfig.compileSdkVersion)

    defaultConfig {
        minSdkVersion(AndroidConfig.minSdkVersion)
        targetSdkVersion(AndroidConfig.targetSdkVersion)

        versionCode = LibraryConfig.versionCode
        versionName = LibraryConfig.versionName

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        testInstrumentationRunnerArguments(mapOf(
                "clearPackageData" to "true"
        ))
    }

    resourcePrefix("d4l_securestore_")

    buildTypes {
        getByName("debug") {
            isTestCoverageEnabled = false
            setMatchingFallbacks("debug", "release")
        }
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

    expectedBy(project(":securestore-common"))

    api(Dependencies.Multiplatform.D4L.utilAndroid)
    implementation(Dependencies.Multiplatform.Kotlin.stdlibAndroid)
    implementation(Dependencies.Android.AndroidX.appCompat)
    compileOnly(Dependencies.java.javaXAnnotation)

    implementation(Dependencies.Android.tink)


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
