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
    id("com.android.application")
    id("kotlin-android")
    id("com.getkeepsafe.dexcount")
}

val d4lClientConfig = D4LConfigHelper.loadClientConfigAndroid("$rootDir")
val d4LTestConfig = D4LConfigHelper.loadTestConfigAndroid("$rootDir")

android {
    compileSdkVersion(AndroidConfig.compileSdkVersion)

    defaultConfig {
        minSdkVersion(AndroidConfig.minSdkVersion)
        targetSdkVersion(AndroidConfig.targetSdkVersion)

        applicationId = "care.data4life.sdk.sample"
        versionCode = 1
        versionName = "1.0"

        multiDexEnabled = true

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        testInstrumentationRunnerArguments(mapOf(
                "clearPackageData" to "true"
        ))

        adbOptions {
            timeOutInMs(10 * 60 * 1000)
            installOptions("-d")
        }

        manifestPlaceholders = mapOf<String, Any>(
                "clientId" to d4lClientConfig[Environment.DEVELOPMENT].id,
                "clientSecret" to d4lClientConfig[Environment.DEVELOPMENT].secret,
                "redirectScheme" to d4lClientConfig[Environment.DEVELOPMENT].redirectScheme,
                "environment" to "${Environment.DEVELOPMENT}",
                "platform" to d4lClientConfig.platform,
                "debug" to "true"
        )
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        coreLibraryDesugaringEnabled = false

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

        unitTests.all(KotlinClosure1<Any, Test>({
            (this as Test).also { testTask ->
                testTask.testLogging {
                    events("passed", "skipped", "failed", "standardOut", "standardError")
                }
            }
        }, unitTests))

        execution = "ANDROID_TEST_ORCHESTRATOR"
    }
}

dependencies {
    coreLibraryDesugaring(Dependency.Android.androidDesugar)

    implementation(project(":sdk-android")) {
        exclude(group = "org.threeten", module = "threetenbp")
        exclude(group = "com.github.d4l-data4life", module = "securestore-jvm")
        exclude(group = "com.github.d4l-data4life", module = "crypto-jvm")
        exclude(group = "com.github.d4l-data4life", module = "auth-jvm")
        exclude(group = "com.github.d4l-data4life.mpp-util-sdk", module = "util-jvm")
        exclude(group = "com.github.d4l-data4life.mpp-fhir-helper-sdk", module = "fhir-helper-jvm")
    }

    implementation(Dependency.Multiplatform.D4L.fhirHelperAndroid) {
        exclude(group = "com.github.d4l-data4life.mpp-util-sdk", module = "util-android")
    }

    implementation(Dependency.Multiplatform.D4L.utilAndroid)

    implementation(Dependency.Android.threeTenABP)

    implementation(Dependency.Android.kotlinStdLib)

    implementation(Dependency.Android.AndroidX.appCompat)
    implementation(Dependency.Android.AndroidX.constraintLayout)
    implementation(Dependency.Android.material)
    implementation(Dependency.Android.photoView)
    implementation(Dependency.Android.pdfView)

    implementation(Dependency.Android.googlePlayServicesBase)

    implementation(Dependency.Android.moshi)


    testImplementation(Dependency.Android.Test.junit)


    androidTestImplementation(Dependency.Android.AndroidTest.runner)
}
