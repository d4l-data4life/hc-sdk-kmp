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
    id("kotlin-android")
    id("com.vanniktech.android.junit.jacoco")
    id("me.champeau.gradle.japicmp")
}

apply(from = "${project.rootDir}/gradle/deploy-android-sdk.gradle")

version = LibraryConfig.version
group = LibraryConfig.group

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

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android.txt"), "proguard-rules.pro")
        }
    }

    resourcePrefix("d4l_sdk_")

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

val compatibilityBase by configurations.creating {
    description = "Configuration for the base version of the SDK we want to verify compatibility against"
}

dependencies {
    coreLibraryDesugaring(Dependency.Android.androidDesugar)

    api(project(":sdk-core")) {
        exclude(group = "org.threeten", module = "threetenbp")
        exclude(module = "securestore-jvm")
        exclude(module = "crypto-jvm")
        exclude(module = "crypto-jvm")
        exclude(module = "auth-jvm")
        exclude(group = "care.data4life.hc-util-sdk-kmp", module = "util-jvm")
    }
    implementation(project(":securestore-android"))
    implementation(project(":crypto-android"))
    implementation(project(":auth-android"))

    api(Dependency.Multiplatform.D4L.utilAndroid)

    implementation(Dependency.Multiplatform.D4L.fhirSdk)
    implementation(Dependency.Android.threeTenABP)

    implementation(Dependency.Android.AndroidX.appCompat)
    implementation(Dependency.Android.AndroidX.browser)

    implementation(Dependency.Android.rxJava2)

    implementation(Dependency.Android.okHttp)
    implementation(Dependency.Android.okHttpLoggingInterceptor)

    implementation(Dependency.Android.retrofit)
    implementation(Dependency.Android.retrofitConverterMoshi)
    implementation(Dependency.Android.retrofitAdapterRxJava)

    implementation(Dependency.Android.appAuthPatch)

    implementation(Dependency.Android.bouncyCastleJdk15)

    compileOnly(Dependency.java.javaXAnnotation)

    testImplementation(Dependency.Android.Test.junit)
    testImplementation(Dependency.Android.Test.truth)

    testImplementation("org.mockito:mockito-inline:2.9.0")
    testImplementation("org.powermock:powermock-core:1.7.3")
    testImplementation("org.powermock:powermock-module-junit4:1.7.3")
    testImplementation("org.powermock:powermock-api-mockito2:1.7.3")

    testImplementation(Dependency.Android.Test.okHttpMockWebServer)
    testImplementation(Dependency.Android.Test.jsonAssert)

    testImplementation(Dependency.Multiplatform.D4L.fhirHelperAndroid) {
        exclude(group = "care.data4life.hc-util-sdk-kmp", module = "util-android")
    }

    androidTestImplementation(Dependency.Android.AndroidX.constraintLayout)
    androidTestImplementation(Dependency.Android.material)

    androidTestImplementation(Dependency.Multiplatform.Kotlin.stdlibAndroid)
    androidTestImplementation(Dependency.Multiplatform.Coroutines.android)

    androidTestImplementation(Dependency.Multiplatform.D4L.fhirHelperAndroid) {
        exclude(group = "care.data4life.hc-util-sdk-kmp", module = "util-android")
    }

    androidTestImplementation(Dependency.Multiplatform.Test.Kotlin.testJvm)
    androidTestImplementation(Dependency.Multiplatform.Test.Kotlin.testJvmJunit)

    androidTestImplementation(Dependency.Android.AndroidTest.runner)
    androidTestImplementation(Dependency.Android.AndroidTest.rules)
    androidTestImplementation(Dependency.Android.AndroidTest.orchestrator)

    androidTestImplementation(Dependency.Android.AndroidTest.espressoCore)
    androidTestImplementation(Dependency.Android.AndroidTest.espressoIntents)
    androidTestImplementation(Dependency.Android.AndroidTest.espressoWeb)

    androidTestImplementation(Dependency.Android.AndroidTest.uiAutomator)
    androidTestImplementation(Dependency.Android.AndroidTest.kakao)

    androidTestImplementation(Dependency.Android.googlePlayServicesBase)
    androidTestImplementation(Dependency.Android.AndroidTest.truth)

    compatibilityBase("care.data4life:hc-sdk-kmp:${LibraryConfig.referenceSdkVersion}") {
        isTransitive = false
        isForce = true
    }
}

val clearJar by tasks.registering(Delete::class) {
    delete("build/outputs/ProjectName.jar")
}

val genCurrentJar by tasks.creating(Jar::class) {
    dependsOn("assemble")
    from("build/intermediates/classes/release/")
    archiveFileName.set("sdk-api.jar")
}

val genReferenceJar by tasks.creating {
    doLast {
        copy {
            from(compatibilityBase)
            into("$buildDir/outputs/jar/")
            rename("hc-sdk-kmp-${LibraryConfig.referenceSdkVersion}.jar", "reference-sdk.zip")
        }

        copy {
            from(zipTree("$buildDir/outputs/jar/reference-sdk.zip"))
            into("$buildDir/outputs/jar/")
            include("classes.jar")
            rename("classes.jar", "reference-sdk.jar")
        }
    }
}

val generateSdkCompatibilityReport by tasks.creating(me.champeau.gradle.japicmp.JapicmpTask::class) {
    dependsOn(genCurrentJar, genReferenceJar)

    oldClasspath = files("$buildDir/outputs/jar/reference-sdk.zip")
    newClasspath = files(genCurrentJar.archivePath)
    isOnlyModified = true
    isFailOnModification = true
    txtOutputFile = file("$buildDir/reports/compatibility/japi.txt")
    isIgnoreMissingClasses = true
    packageIncludes = listOf("care.data4life.*")
    classExcludes = listOf(
            "care.data4life.crypto.R",
            "care.data4life.auth.R",
            "care.data4life.securestore.R",
            "care.data4life.sdk.R"
    )
}

apply(from = "$projectDir/gradle/downloadFromDevDocs.gradle")

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
