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
    id("me.champeau.gradle.japicmp")
    // id("scripts.jacoco-android")
}

group = LibraryConfig.group

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

        manifestPlaceholders(
            mapOf<String, Any>(
                "clientId" to d4lClientConfig[Environment.DEVELOPMENT].id,
                "clientSecret" to d4lClientConfig[Environment.DEVELOPMENT].secret,
                "redirectScheme" to d4lClientConfig[Environment.DEVELOPMENT].redirectScheme,
                "environment" to "${Environment.DEVELOPMENT}",
                "platform" to d4lClientConfig.platform,
                "debug" to "false"
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
        // execution = "ANDROIDX_TEST_ORCHESTRATOR"
        animationsDisabled = true

        unitTests {
            all {
                it.testLogging {
                    events("passed", "skipped", "failed", "standardOut", "standardError")
                }
            }
        }
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

apply(from = "${project.rootDir}/gradle/deploy-android-sdk.gradle")

val compatibilityBase by configurations.creating {
    description =
        "Configuration for the base version of the SDK we want to verify compatibility against"
}

dependencies {
    coreLibraryDesugaring(Dependencies.Android.androidDesugar)

    api(project(":sdk-core")) {
        exclude(group = "org.threeten", module = "threetenbp")
        exclude(module = "securestore-jvm")
        exclude(module = "crypto-jvm")
        exclude(module = "auth-jvm")
        exclude(group = "care.data4life.hc-util-sdk-kmp", module = "util-jvm")
    }
    implementation(project(":securestore-android"))
    implementation(project(":crypto-android"))
    implementation(project(":auth-android"))

    api(Dependencies.Multiplatform.D4L.utilAndroid)

    implementation(Dependencies.Multiplatform.D4L.fhirSdk)
    implementation(Dependencies.Android.threeTenABP)

    implementation(Dependencies.Android.AndroidX.appCompat)
    implementation(Dependencies.Android.AndroidX.browser)

    implementation(Dependencies.Android.rxJava2)

    implementation(Dependencies.Android.okHttp)
    implementation(Dependencies.Android.okHttpLoggingInterceptor)

    implementation(Dependencies.Android.retrofit)
    implementation(Dependencies.Android.retrofitConverterMoshi)
    implementation(Dependencies.Android.retrofitAdapterRxJava)

    implementation(Dependencies.Android.appAuthPatch)

    implementation(Dependencies.Android.bouncyCastleJdk15)

    compileOnly(Dependencies.Java.javaXAnnotation)

    testImplementation(Dependencies.Android.Test.junit)
    testImplementation(Dependencies.Android.Test.truth)

    testImplementation("org.mockito:mockito-inline:2.27.0")
    testImplementation("org.powermock:powermock-core:1.7.3")
    testImplementation("org.powermock:powermock-module-junit4:1.7.3")
    testImplementation("org.powermock:powermock-api-mockito2:1.7.3")

    testImplementation(Dependencies.Android.Test.okHttpMockWebServer)
    testImplementation(Dependencies.Android.Test.jsonAssert)

    testImplementation(Dependencies.Multiplatform.D4L.fhirHelperAndroid) {
        exclude(group = "care.data4life.hc-util-sdk-kmp", module = "util-android")
        exclude(group = "care.data4life.hc-fhir-sdk-java", module = "hc-fhir-sdk-java")
    }

    testImplementation(Dependencies.Multiplatform.D4L.fhirHelperJvm) {
        exclude(group = "care.data4life.hc-fhir-sdk-java", module = "hc-fhir-sdk-java")
    }

    androidTestImplementation(Dependencies.Android.AndroidX.constraintLayout)
    androidTestImplementation(Dependencies.Android.material)

    androidTestImplementation(Dependencies.Multiplatform.Kotlin.stdlibAndroid)
    androidTestImplementation(Dependencies.Multiplatform.Coroutines.android)

    androidTestImplementation(Dependencies.Multiplatform.D4L.fhirHelperAndroid) {
        exclude(group = "care.data4life.hc-util-sdk-kmp", module = "util-android")
        exclude(group = "care.data4life.hc-fhir-sdk-java", module = "hc-fhir-sdk-java")
    }

    androidTestImplementation(Dependencies.Multiplatform.Test.Kotlin.testJvm)
    androidTestImplementation(Dependencies.Multiplatform.Test.Kotlin.testJvmJunit)

    androidTestImplementation(Dependencies.Android.AndroidTest.runner)
    androidTestImplementation(Dependencies.Android.AndroidTest.rules)
    androidTestImplementation(Dependencies.Android.AndroidTest.orchestrator)

    androidTestImplementation(Dependencies.Android.AndroidTest.espressoCore)
    androidTestImplementation(Dependencies.Android.AndroidTest.espressoIntents)
    androidTestImplementation(Dependencies.Android.AndroidTest.espressoWeb)

    androidTestImplementation(Dependencies.Android.AndroidTest.uiAutomator)
    androidTestImplementation(Dependencies.Android.AndroidTest.kakao)

    androidTestImplementation(Dependencies.Android.googlePlayServicesBase)
    androidTestImplementation(Dependencies.Android.AndroidTest.truth)

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
