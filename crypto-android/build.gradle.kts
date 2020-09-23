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

        adbOptions {
            timeOutInMs(10 * 60 * 1000)
            installOptions("-d")
        }

        buildTypes {
            buildTypes {
                getByName("debug"){
                    matchingFallbacks = listOf("debug","release")
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

        unitTests.all(KotlinClosure1<Any, Test>({
            (this as Test).also { testTask ->
                testTask.testLogging {
                    events("passed", "skipped", "failed", "standardOut", "standardError")
                }
            }
        }, unitTests))

        execution = "ANDROID_TEST_ORCHESTRATOR"
    }

    lintOptions {
        isAbortOnError = false
    }

    compileOptions {
        coreLibraryDesugaringEnabled = false

        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

}

dependencies {
    coreLibraryDesugaring(Dependency.Android.androidDesugar)

    expectedBy(project(":crypto-common"))

    implementation(Dependency.Multiplatform.D4L.utilAndroid)
    implementation(Dependency.Multiplatform.Kotlin.stdlibAndroid)
    implementation(Dependency.Android.AndroidX.appCompat)
    implementation(Dependency.Android.bouncyCastleJdk15)
    implementation(Dependency.Android.moshi)
    compileOnly(Dependency.java.javaXAnnotation)


    testImplementation(Dependency.Android.Test.junit)
    testImplementation(Dependency.Multiplatform.Test.Kotlin.testJvm)
    testImplementation(Dependency.Multiplatform.Test.Kotlin.testJvmJunit)
    testImplementation(Dependency.Multiplatform.Test.MockK.jdk)


    androidTestImplementation(Dependency.Android.AndroidTest.runner)
    androidTestImplementation(Dependency.Android.AndroidTest.espressoCore)
    androidTestImplementation(Dependency.Multiplatform.Test.Kotlin.testJvm)
    androidTestImplementation(Dependency.Multiplatform.Test.Kotlin.testJvmJunit)
    androidTestImplementation(Dependency.Multiplatform.Test.MockK.android)
}

apply(from = "${project.rootDir}/gradle/deploy-android.gradle")
