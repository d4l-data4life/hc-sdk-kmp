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

object Dependencies {

    object Multiplatform {
        object Kotlin {
            const val stdlibCommon = "org.jetbrains.kotlin:kotlin-stdlib-common:${Versions.kotlin}"
            const val stdlib = "org.jetbrains.kotlin:kotlin-stdlib:${Versions.kotlin}"
            const val stdlibJdk8 = "org.jetbrains.kotlin:kotlin-stdlib-jdk8:${Versions.kotlin}"
            const val stdlibAndroid = "org.jetbrains.kotlin:kotlin-stdlib:${Versions.kotlin}"
        }

        object Coroutines {
            // https://github.com/Kotlin/kotlinx.coroutines
            const val common = "org.jetbrains.kotlinx:kotlinx-coroutines-core-common:${Versions.kotlinCoroutines}"
            const val jdk = "org.jetbrains.kotlinx:kotlinx-coroutines-core:${Versions.kotlinCoroutines}"
            const val android = "org.jetbrains.kotlinx:kotlinx-coroutines-android:${Versions.kotlinCoroutines}"
            const val test = "org.jetbrains.kotlinx:kotlinx-coroutines-test:${Versions.kotlinCoroutines}"
        }

        object D4L {
            const val sdkUtil = "care.data4life.hc-util-sdk-kmp:util:${Versions.sdkUtil}"

            const val fhirSdk = "care.data4life.hc-fhir-sdk-java:fhir-java:${Versions.fhirSdk}"

            const val fhirHelperCommon = "care.data4life.hc-fhir-helper-sdk-kmp:fhir-helper-metadata:${Versions.fhirHelper}"
            const val fhirHelperAndroid = "care.data4life.hc-fhir-helper-sdk-kmp:fhir-helper-android:${Versions.fhirHelper}"
            const val fhirHelperJvm = "care.data4life.hc-fhir-helper-sdk-kmp:fhir-helper-jvm:${Versions.fhirHelper}"
        }

        object Test {
            object Kotlin {
                const val testCommon = "org.jetbrains.kotlin:kotlin-test-common:${Versions.kotlin}"

                const val testAnnotationsCommon =
                    "org.jetbrains.kotlin:kotlin-test-annotations-common:${Versions.kotlin}"
                const val testJvm = "org.jetbrains.kotlin:kotlin-test:${Versions.kotlin}"

                const val testJvmJunit = "org.jetbrains.kotlin:kotlin-test-junit:${Versions.kotlin}"
            }

            object MockK {
                const val common = "io.mockk:mockk-common:${Versions.testMockk}"
                const val android = "io.mockk:mockk-android:${Versions.testMockk}"
                const val jdk = "io.mockk:mockk:${Versions.testMockk}"
            }
        }
    }

    object Android {
        // Kotlin
        const val kotlinStdLib = "org.jetbrains.kotlin:kotlin-stdlib:${Versions.kotlin}"
        const val kotlinReflect = "org.jetbrains.kotlin:kotlin-reflect:${Versions.kotlin}"

        // Android
        const val androidDesugar = "com.android.tools:desugar_jdk_libs:${Versions.androidDesugar}"

        // Android X
        object AndroidX {
            const val ktx = "androidx.core:core-ktx:${Versions.androidXKtx}"
            const val appCompat = "androidx.appcompat:appcompat:${Versions.androidXAppCompat}"
            const val browser = "androidx.browser:browser:${Versions.androidXBrowser}"
            const val constraintLayout = "androidx.constraintlayout:constraintlayout:${Versions.androidXConstraintLayout}"
        }

        // Material
        const val material = "com.google.android.material:material:${Versions.material}"

        // Google
        const val googlePlayServicesBase = "com.google.android.gms:play-services-base:${Versions.googlePlayServices}"

        // Crypto
        const val bouncyCastleJdk15 = "org.bouncycastle:bcprov-jdk15on:${Versions.bouncyCastle}"
        const val tink = "com.google.crypto.tink:tink-android:${Versions.tink}"

        // Authorization
        const val appAuth = "net.openid:appauth:${Versions.appAuth}"
        const val appAuthPatch = "com.github.gesundheitscloud:AppAuth-Android:${Versions.appAuthPatch}"

        // Data
        const val moshi = "com.squareup.moshi:moshi:${Versions.moshi}"

        // Date
        const val threeTenABP = "com.jakewharton.threetenabp:threetenabp:${Versions.threeTenABP}"

        // UI
        const val photoView = "com.github.chrisbanes:PhotoView:${Versions.photoView}"
        const val pdfView = "com.github.barteksc:android-pdf-viewer:${Versions.pdfView}"

        // Injection
        const val koinCore = "org.koin:koin-core:${Versions.koin}"
        const val koinJava = "org.koin:koin-java:${Versions.koin}"
        const val testKoin = "org.koin:koin-test:${Versions.koin}"

        // Rx
        const val rxJava2 = "io.reactivex.rxjava2:rxandroid:${Versions.rxAndroid}"

        // Network
        const val okHttp = "com.squareup.okhttp3:okhttp:${Versions.okHttp}"
        const val okHttpLoggingInterceptor = "com.squareup.okhttp3:logging-interceptor:${Versions.okHttp}"

        const val retrofit = "com.squareup.retrofit2:retrofit:${Versions.retrofit}"
        const val retrofitConverterMoshi = "com.squareup.retrofit2:converter-moshi:${Versions.retrofit}"
        const val retrofitAdapterRxJava = "com.squareup.retrofit2:adapter-rxjava2:${Versions.retrofit}"
        const val gson = "com.squareup.retrofit2:converter-gson:${Versions.gson}"

        // Test
        object Test {
            const val core = "androidx.test:core:${Versions.androidXTestCore}"

            const val robolectric = "org.robolectric:robolectric:${Versions.robolectric}"

            const val junit = "junit:junit:${Versions.testJUnit}"

            const val truth = "com.google.truth:truth:${Versions.testTruthAndroid}"

            const val jsonAssert = "org.skyscreamer:jsonassert:${Versions.testJsonAssert}"

            const val okHttpMockWebServer = "com.squareup.okhttp3:mockwebserver:${Versions.okHttp}"
        }

        // AndroidTest
        object AndroidTest {
            const val core = "androidx.test:core:${Versions.androidXTestCore}"
            const val runner = "androidx.test:runner:${Versions.androidXTest}"
            const val rules = "androidx.test:rules:${Versions.androidXTest}"
            const val orchestrator = "androidx.test:orchestrator:${Versions.androidXTest}"

            const val extJUnit = "androidx.test.ext:junit:${Versions.androidXTestExtJUnit}"

            const val espressoCore = "androidx.test.espresso:espresso-core:${Versions.androidXEspresso}"
            const val espressoIntents = "androidx.test.espresso:espresso-intents:${Versions.androidXEspresso}"
            const val espressoWeb = "androidx.test.espresso:espresso-web:${Versions.androidXEspresso}"

            const val uiAutomator = "androidx.test.uiautomator:uiautomator:${Versions.androidXUiAutomator}"

            const val kakao = "com.github.wmontwe:Kakao:${Versions.androidXKakao}"

            const val truth = "com.google.truth:truth:${Versions.testTruthAndroid}"

            const val okHttpMockWebServer = "com.squareup.okhttp3:mockwebserver:${Versions.okHttp}"
        }
    }

    val java = Java

    object Java {
        const val javaXAnnotation = "com.google.code.findbugs:jsr305:${Versions.javaXAnnotation}"

        // Kotlin
        const val kotlinStdlibJdk8 = "org.jetbrains.kotlin:kotlin-stdlib-jdk8:${Versions.kotlin}"

        // Crypto
        const val bouncyCastleJdk15 = "org.bouncycastle:bcprov-jdk15on:${Versions.bouncyCastle}"

        // Authorization
        const val scribeCore = "com.github.scribejava:scribejava-core:${Versions.scribe}"

        // Data
        const val moshi = "com.squareup.moshi:moshi:${Versions.moshi}"
        const val moshiCodeGen = "com.squareup.moshi:moshi-kotlin-codegen:${Versions.moshi}"

        // Date
        const val threeTenBP = "org.threeten:threetenbp:${Versions.threeTenBP}"

        // Injection
        const val koinCore = "org.koin:koin-core:${Versions.koin}"
        const val koinJava = "org.koin:koin-java:${Versions.koin}"

        // Ui
        const val cmdClickt = "com.github.ajalt:clikt:${Versions.clikt}"

        // RX
        const val rxJava = "io.reactivex.rxjava2:rxjava:${Versions.rxJava}"

        // Network
        const val okHttp = "com.squareup.okhttp3:okhttp:${Versions.okHttp}"
        const val okHttpLoggingInterceptor = "com.squareup.okhttp3:logging-interceptor:${Versions.okHttp}"

        const val retrofit = "com.squareup.retrofit2:retrofit:${Versions.retrofit}"
        const val retrofitConverterMoshi = "com.squareup.retrofit2:converter-moshi:${Versions.retrofit}"
        const val retrofitAdapterRxJava = "com.squareup.retrofit2:adapter-rxjava2:${Versions.retrofit}"

        object Test {
            const val junit = "junit:junit:${Versions.testJUnit}"

            const val kotlinTest = "org.jetbrains.kotlin:kotlin-test:${Versions.kotlin}"
            const val kotlinJunit = "org.jetbrains.kotlin:kotlin-test-junit:${Versions.kotlin}"

            const val truth = "com.google.truth:truth:${Versions.testTruth}"

            const val mockitoInline = "org.mockito:mockito-inline:${Versions.testMockito}"
            const val mockitoCore = "org.mockito:mockito-core:${Versions.testMockito}"

            const val jsonAssert = "org.skyscreamer:jsonassert:${Versions.testJsonAssert}"

            const val koin = "org.koin:koin-test:${Versions.koin}"

            const val okHttpMockWebServer = "com.squareup.okhttp3:mockwebserver:${Versions.okHttp}"
        }
    }
}
