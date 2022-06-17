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

plugins {
    id("com.android.library")
    id("kotlin-android")
    id("com.vanniktech.android.junit.jacoco")
}

group = LibraryConfig.group

apply(from = "${project.rootDir}/gradle/deploy-android-sdk.gradle")

val d4lClientConfig = D4LConfigHelper.loadClientConfigAndroid("$rootDir")
val d4LTestConfig = D4LConfigHelper.loadTestConfigAndroid("$rootDir")

android {
    compileSdkVersion(LibraryConfig.android.compileSdkVersion)

    defaultConfig {
        minSdkVersion(LibraryConfig.android.minSdkVersion)
        targetSdkVersion(LibraryConfig.android.targetSdkVersion)

        // Workaround BuildConfig for Libraries not anymore containing VERSION_NAME
        // https://commonsware.com/blog/2020/10/14/android-studio-4p1-library-modules-version-code.html
        // FIXME LibraryConfig.versionName
        buildConfigField("String", "VERSION_NAME", "\"${project.version}\"")

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        testInstrumentationRunnerArguments(
            mapOf(
                "clearPackageData" to "true"
            )
        )
    }

    buildTypes {
        getByName("debug") {
            setMatchingFallbacks("debug", "release")
        }
        getByName("release") {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android.txt"), "proguard-rules.pro")
        }
    }

    resourcePrefix("d4l_sdk_")

    testOptions {
        animationsDisabled = true

        unitTests.all {
            it.testLogging {
                events("passed", "skipped", "failed", "standardOut", "standardError")
            }
        }

        testVariants.forEach {
            it.mergedFlavor.manifestPlaceholders.putAll(
                d4lClientConfig.toConfigMap(care.data4life.gradle.core.config.Environment.DEVELOPMENT, true)
            )
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
    coreLibraryDesugaring(Dependency.Android.androidDesugar)

    implementation(project(":sdk-core")) {
        // TODO
        exclude(group = "org.threeten", module = "threetenbp")
        exclude(module = "securestore-jvm")
        exclude(module = "crypto-jvm")
        exclude(module = "auth-jvm")
        exclude(group = "care.data4life.hc-util-sdk-kmp", module = "util-jvm")
        exclude(group = "care.data4life.hc-util-sdk-kmp", module = "error-jvm")
    }

    implementation(Dependency.Multiplatform.D4L.auth)
    implementation(Dependency.Multiplatform.D4L.crypto)
    implementation(Dependency.Multiplatform.D4L.securestoreAndroid)
    implementation(Dependency.Multiplatform.D4L.utilAndroid)
    implementation(Dependency.Multiplatform.D4L.errorAndroid)

    implementation(Dependency.Jvm.fhirSdk)
    implementation(Dependency.Android.threeTenABP)

    implementation(Dependency.Android.AndroidX.appCompat)
    implementation(Dependency.Android.AndroidX.browser)

    implementation(Dependency.Android.rxJava2)

    implementation(Dependency.Android.okHttp)
    implementation(Dependency.Android.okHttpLoggingInterceptor)

    implementation(Dependency.Android.retrofit)
    implementation(Dependency.Android.retrofitConverterMoshi)
    implementation(Dependency.Android.retrofitAdapterRxJava)

    implementation(Dependency.Android.appAuth)

    implementation(Dependency.Android.bouncyCastleJdk15)

    // FIXME could be removed
    compileOnly(Dependency.Jvm.javaXAnnotation)

    testImplementation(Dependency.JvmTest.junit)
    testImplementation(Dependency.AndroidTest.truth)

    testImplementation(Dependency.MultiplatformTest.mockK)

    // FIXME
    testImplementation("org.mockito:mockito-inline:2.9.0")
    testImplementation("org.powermock:powermock-core:1.7.3")
    testImplementation("org.powermock:powermock-module-junit4:1.7.3")
    testImplementation("org.powermock:powermock-api-mockito2:1.7.3")

    testImplementation(Dependency.JvmTest.okHttpMockWebServer)
    testImplementation(Dependency.JvmTest.jsonAssert)

    testImplementation(Dependency.Multiplatform.D4L.fhirHelperAndroid) {
        exclude(group = "care.data4life.hc-util-sdk-kmp", module = "util-android")
        exclude(group = "care.data4life.hc-fhir-sdk-java", module = "error-java")
        exclude(group = "care.data4life.hc-fhir-sdk-java", module = "hc-fhir-sdk-java")
    }

    androidTestImplementation(Dependency.Android.AndroidX.constraintLayout)
    androidTestImplementation(Dependency.Android.material)

    androidTestImplementation(Dependency.Multiplatform.Kotlin.stdlibAndroid)
    androidTestImplementation(Dependency.Multiplatform.KotlinX.coroutinesCore)

    androidTestImplementation(Dependency.Multiplatform.D4L.fhirHelperAndroid) {
        exclude(group = "care.data4life.hc-util-sdk-kmp", module = "util-android")
        exclude(group = "care.data4life.hc-fhir-sdk-java", module = "error-android")
        exclude(group = "care.data4life.hc-fhir-sdk-java", module = "hc-fhir-sdk-java")
    }

    androidTestImplementation(Dependency.MultiplatformTest.Kotlin.testJvm)
    androidTestImplementation(Dependency.MultiplatformTest.Kotlin.testJvmJunit)

    androidTestImplementation(Dependency.AndroidTest.runner)
    androidTestImplementation(Dependency.AndroidTest.rules)
    androidTestImplementation(Dependency.AndroidTest.orchestrator)

    androidTestImplementation(Dependency.AndroidTest.espressoCore)
    androidTestImplementation(Dependency.AndroidTest.espressoIntents)
    androidTestImplementation(Dependency.AndroidTest.espressoWeb)

    androidTestImplementation(Dependency.AndroidTest.uiAutomator)
    androidTestImplementation(Dependency.AndroidTest.kakao)

    androidTestImplementation(Dependency.Android.googlePlayServicesBase)
    androidTestImplementation(Dependency.AndroidTest.truth)
}

apply(from = "$projectDir/gradle/downloadFromDevDocs.gradle")

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
