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

object Dependency {

    object GradlePlugin {
        const val android = "com.android.tools.build:gradle:${Version.GradlePlugin.android}"
        const val kotlin = "org.jetbrains.kotlin:kotlin-gradle-plugin:${Version.GradlePlugin.kotlin}"

        const val dexcount = "com.getkeepsafe.dexcount:dexcount-gradle-plugin:${Version.GradlePlugin.dexcount}"

        const val downloadTask = "de.undercouch:gradle-download-task:${Version.GradlePlugin.downloadTask}"

        const val dokka = "org.jetbrains.dokka:dokka-gradle-plugin:${Version.GradlePlugin.dokka}"
    }

    object Multiplatform {
        object Kotlin {
            const val stdlibCommon = "org.jetbrains.kotlin:kotlin-stdlib-common:${Version.kotlin}"
            const val stdlib = "org.jetbrains.kotlin:kotlin-stdlib:${Version.kotlin}"
            const val stdlibJdk8 = "org.jetbrains.kotlin:kotlin-stdlib-jdk8:${Version.kotlin}"
            const val stdlibAndroid = "org.jetbrains.kotlin:kotlin-stdlib:${Version.kotlin}"
        }

        object Coroutines {
            // https://github.com/Kotlin/kotlinx.coroutines
            const val common = "org.jetbrains.kotlinx:kotlinx-coroutines-core-common:${Version.kotlinCoroutines}"
            const val jdk = "org.jetbrains.kotlinx:kotlinx-coroutines-core:${Version.kotlinCoroutines}"
            const val android = "org.jetbrains.kotlinx:kotlinx-coroutines-android:${Version.kotlinCoroutines}"
            const val test = "org.jetbrains.kotlinx:kotlinx-coroutines-test:${Version.kotlinCoroutines}"
        }

        object D4L {
            const val utilCommon = "care.data4life.hc-util-sdk-kmp:util-metadata:${Version.sdkUtil}"
            const val utilJvm = "care.data4life.hc-util-sdk-kmp:util-jvm:${Version.sdkUtil}"
            const val utilAndroid = "care.data4life.hc-util-sdk-kmp:util-android:${Version.sdkUtil}"

            const val fhirSdk = "care.data4life.hc-fhir-sdk-java:hc-fhir-sdk-java:${Version.fhirSdk}"

            const val fhirHelperCommon = "care.data4life.hc-fhir-helper-sdk-kmp:fhir-helper-metadata:${Version.fhirHelper}"
            const val fhirHelperAndroid = "care.data4life.hc-fhir-helper-sdk-kmp:fhir-helper-android:${Version.fhirHelper}"
            const val fhirHelperJvm = "care.data4life.hc-fhir-helper-sdk-kmp:fhir-helper-jvm:${Version.fhirHelper}"
        }

        object Test {
            object Kotlin {
                const val testCommon = "org.jetbrains.kotlin:kotlin-test-common:${Version.kotlin}"

                const val testAnnotationsCommon =
                        "org.jetbrains.kotlin:kotlin-test-annotations-common:${Version.kotlin}"
                const val testJvm = "org.jetbrains.kotlin:kotlin-test:${Version.kotlin}"

                const val testJvmJunit = "org.jetbrains.kotlin:kotlin-test-junit:${Version.kotlin}"
            }

            object MockK {
                const val common = "io.mockk:mockk-common:${Version.testMockk}"
                const val android = "io.mockk:mockk-android:${Version.testMockk}"
                const val jdk = "io.mockk:mockk:${Version.testMockk}"
            }
        }
    }


    object Android {
        // Kotlin
        const val kotlinStdLib = "org.jetbrains.kotlin:kotlin-stdlib:${Version.kotlin}"
        const val kotlinReflect = "org.jetbrains.kotlin:kotlin-reflect:${Version.kotlin}"


        // Android
        const val androidDesugar = "com.android.tools:desugar_jdk_libs:${Version.androidDesugar}"

        // Android X
        object AndroidX {
            const val ktx = "androidx.core:core-ktx:${Version.androidXKtx}"
            const val appCompat = "androidx.appcompat:appcompat:${Version.androidXAppCompat}"
            const val browser = "androidx.browser:browser:${Version.androidXBrowser}"
            const val constraintLayout = "androidx.constraintlayout:constraintlayout:${Version.androidXConstraintLayout}"
        }

        // Material
        const val material = "com.google.android.material:material:${Version.material}"

        // Google
        const val googlePlayServicesBase = "com.google.android.gms:play-services-base:${Version.googlePlayServices}"

        // Crypto
        const val bouncyCastleJdk15 = "org.bouncycastle:bcprov-jdk15on:${Version.bouncyCastle}"
        const val tink = "com.google.crypto.tink:tink-android:${Version.tink}"

        // Authorization
        const val appAuth = "net.openid:appauth:${Version.appAuth}"
        const val appAuthPatch = "com.github.gesundheitscloud:AppAuth-Android:${Version.appAuthPatch}"

        // Data
        const val moshi = "com.squareup.moshi:moshi:${Version.moshi}"

        // Date
        const val threeTenABP = "com.jakewharton.threetenabp:threetenabp:${Version.threeTenABP}"

        // UI
        const val photoView = "com.github.chrisbanes:PhotoView:${Version.photoView}"
        const val pdfView = "com.github.barteksc:android-pdf-viewer:${Version.pdfView}"

        // Injection
        const val koinCore = "org.koin:koin-core:${Version.koin}"
        const val koinJava = "org.koin:koin-java:${Version.koin}"
        const val testKoin = "org.koin:koin-test:${Version.koin}"

        // Rx
        const val rxJava2 = "io.reactivex.rxjava2:rxandroid:${Version.rxAndroid}"

        // Network
        const val okHttp = "com.squareup.okhttp3:okhttp:${Version.okHttp}"
        const val okHttpLoggingInterceptor = "com.squareup.okhttp3:logging-interceptor:${Version.okHttp}"

        const val retrofit = "com.squareup.retrofit2:retrofit:${Version.retrofit}"
        const val retrofitConverterMoshi = "com.squareup.retrofit2:converter-moshi:${Version.retrofit}"
        const val retrofitAdapterRxJava = "com.squareup.retrofit2:adapter-rxjava2:${Version.retrofit}"


        // Test
        object Test {
            const val core = "androidx.test:core:${Version.androidTestCore}"

            const val robolectric = "org.robolectric:robolectric:${Version.robolectric}"

            const val junit = "junit:junit:${Version.testJUnit}"

            const val truth = "com.google.truth:truth:${Version.testTruthAndroid}"

            const val jsonAssert = "org.skyscreamer:jsonassert:${Version.testJsonAssert}"

            const val okHttpMockWebServer = "com.squareup.okhttp3:mockwebserver:${Version.okHttp}"
        }


        // AndroidTest
        object AndroidTest {
            const val core = "androidx.test:core:${Version.androidTestCore}"
            const val runner = "androidx.test:runner:${Version.androidTest}"
            const val rules = "androidx.test:rules:${Version.androidTest}"

            const val orchestrator = "androidx.test:orchestrator:${Version.androidTest}"

            const val espressoCore = "androidx.test.espresso:espresso-core:${Version.androidTestEspresso}"
            const val espressoIntents = "androidx.test.espresso:espresso-intents:${Version.androidTestEspresso}"
            const val espressoWeb = "androidx.test.espresso:espresso-web:${Version.androidTestEspresso}"

            const val uiAutomator = "androidx.test.uiautomator:uiautomator:${Version.androidXUiAutomator}"

            const val kakao = "com.github.wmontwe:Kakao:${Version.androidXKakao}"

            const val truth = "com.google.truth:truth:${Version.testTruthAndroid}"

            const val okHttpMockWebServer = "com.squareup.okhttp3:mockwebserver:${Version.okHttp}"
        }
    }


    val java = Java

    object Java {
        const val javaXAnnotation = "com.google.code.findbugs:jsr305:${Version.javaXAnnotation}"

        // Kotlin
        const val kotlinStdlibJdk8 = "org.jetbrains.kotlin:kotlin-stdlib-jdk8:${Version.kotlin}"


        // Crypto
        const val bouncyCastleJdk15 = "org.bouncycastle:bcprov-jdk15on:${Version.bouncyCastle}"

        // Authorization
        const val scribeCore = "com.github.scribejava:scribejava-core:${Version.scribe}"

        // Data
        const val moshi = "com.squareup.moshi:moshi:${Version.moshi}"

        // Date
        const val threeTenBP = "org.threeten:threetenbp:${Version.threeTenBP}"

        // Injection
        const val koinCore = "org.koin:koin-core:${Version.koin}"
        const val koinJava = "org.koin:koin-java:${Version.koin}"

        // Ui
        const val cmdClickt = "com.github.ajalt:clikt:${Version.clikt}"

        // RX
        const val rxJava = "io.reactivex.rxjava2:rxjava:${Version.rxJava}"

        // Network
        const val okHttp = "com.squareup.okhttp3:okhttp:${Version.okHttp}"
        const val okHttpLoggingInterceptor = "com.squareup.okhttp3:logging-interceptor:${Version.okHttp}"

        const val retrofit = "com.squareup.retrofit2:retrofit:${Version.retrofit}"
        const val retrofitConverterMoshi = "com.squareup.retrofit2:converter-moshi:${Version.retrofit}"
        const val retrofitAdapterRxJava = "com.squareup.retrofit2:adapter-rxjava2:${Version.retrofit}"


        object Test {
            const val junit = "junit:junit:${Version.testJUnit}"

            const val kotlinTest = "org.jetbrains.kotlin:kotlin-test:${Version.kotlin}"
            const val kotlinJunit = "org.jetbrains.kotlin:kotlin-test-junit:${Version.kotlin}"

            const val truth = "com.google.truth:truth:${Version.testTruth}"

            const val mockitoInline = "org.mockito:mockito-inline:${Version.testMockito}"
            const val mockitoCore = "org.mockito:mockito-core:${Version.testMockito}"

            const val jsonAssert = "org.skyscreamer:jsonassert:${Version.testJsonAssert}"

            const val koin = "org.koin:koin-test:${Version.koin}"

            const val okHttpMockWebServer = "com.squareup.okhttp3:mockwebserver:${Version.okHttp}"
        }
    }
}
