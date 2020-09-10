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

val d4lClientConfig = D4LConfigHelper.loadClientConfigAndroid("$rootDir")
val d4LTestConfig = D4LConfigHelper.loadTestConfigAndroid("$rootDir")

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

        manifestPlaceholders = mapOf<String, Any>(
                "clientId" to d4lClientConfig[Environment.DEVELOPMENT].id,
                "clientSecret" to d4lClientConfig[Environment.DEVELOPMENT].secret,
                "redirectScheme" to d4lClientConfig[Environment.DEVELOPMENT].redirectScheme,
                "environment" to "${Environment.DEVELOPMENT}",
                "platform" to d4lClientConfig.platform,
                "debug" to "true"
        )
    }

    resourcePrefix("d4l_auth_")

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

    expectedBy(project(":auth-common"))

    api(Dependency.Multiplatform.D4L.utilAndroid)

    implementation(project(":securestore-android")) {
        exclude(group = "care.data4life.hc-util-sdk-kmp", module = "util-jvm")
    }
    implementation(Dependency.Multiplatform.Kotlin.stdlibAndroid)
    implementation(Dependency.Multiplatform.Coroutines.android)
    implementation(Dependency.Android.AndroidX.appCompat)
    implementation(Dependency.Android.AndroidX.browser)
    implementation(Dependency.Android.appAuthPatch)


    testImplementation(Dependency.Android.Test.core)
    testImplementation(Dependency.Android.Test.junit)
    testImplementation(Dependency.Multiplatform.Test.Kotlin.testJvm)
    testImplementation(Dependency.Multiplatform.Test.Kotlin.testJvmJunit)
    testImplementation(Dependency.Multiplatform.Test.MockK.jdk)
    testImplementation(Dependency.Android.Test.robolectric)


    androidTestImplementation(Dependency.Android.AndroidTest.core)
    androidTestImplementation(Dependency.Android.AndroidTest.runner)
    androidTestImplementation(Dependency.Android.AndroidTest.rules)

    androidTestImplementation(Dependency.Android.AndroidTest.espressoCore)

    androidTestImplementation(Dependency.Multiplatform.Test.Kotlin.testJvm)
    androidTestImplementation(Dependency.Multiplatform.Test.Kotlin.testJvmJunit)
    androidTestImplementation(Dependency.Multiplatform.Test.MockK.android)
}

apply(from = "${project.rootDir}/gradle/deploy-android.gradle")

val androidTestAssetsPath = "${projectDir}/src/androidTest/assets"
val unitTestAssetsPath = "${projectDir}/src/test/assets"

val provideTestConfig: Task by tasks.creating {
    doLast {
        val androidTestAsset = File(androidTestAssetsPath)
        if (!androidTestAsset.exists()) androidTestAsset.mkdirs()
        File(androidTestAssetsPath, "test_config.json").writeText(D4LConfigHelper.toJson(d4LTestConfig))
        val unitTestAsset = File(unitTestAssetsPath)
        if (!unitTestAsset.exists()) unitTestAsset.mkdirs()
        File(unitTestAssetsPath, "test_config.json").writeText(D4LConfigHelper.toJson(d4LTestConfig))
    }
}

tasks.named("clean") {
    doLast {
        delete("${androidTestAssetsPath}/test_config.json")
        delete("${unitTestAssetsPath}/test_config.json")
    }
}
