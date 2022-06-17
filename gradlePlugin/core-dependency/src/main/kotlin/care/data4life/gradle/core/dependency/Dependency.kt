/*
 * Copyright (c) 2021 D4L data4life gGmbH / All rights reserved.
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

package care.data4life.gradle.core.dependency

object Dependency {

    val gradlePlugin = GradlePlugin
    val multiplatform = Multiplatform
    val multiplatformTest = MultiplatformTest
    val jvm = Jvm
    val jvmTest = JvmTest
    val android = Android
    val androidTest = AndroidTest

    object Multiplatform {

        val kotlin = Kotlin
        val kotlinX = KotlinX

        object Kotlin {
            const val stdlibCommon = "org.jetbrains.kotlin:kotlin-stdlib-common:${Version.kotlin}"
            const val stdlib = "org.jetbrains.kotlin:kotlin-stdlib:${Version.kotlin}"
            const val stdlibJdk8 = "org.jetbrains.kotlin:kotlin-stdlib-jdk8:${Version.kotlin}"
            const val stdlibAndroid = "org.jetbrains.kotlin:kotlin-stdlib:${Version.kotlin}"
        }

        object KotlinX {
            const val coroutinesCore =
                "org.jetbrains.kotlinx:kotlinx-coroutines-core:${Version.multiplatform.kotlin.coroutines}"
            const val serializationJson =
                "org.jetbrains.kotlinx:kotlinx-serialization-json:${Version.multiplatform.kotlin.serialization}"
            const val dateTime = "org.jetbrains.kotlinx:kotlinx-datetime:${Version.multiplatform.kotlin.dateTime}"
        }

        object D4L {
            const val util = "care.data4life.hc-util-sdk-kmp:util:${Version.multiplatform.d4l.util}"
            const val utilAndroid = "care.data4life.hc-util-sdk-kmp:util-android:${Version.multiplatform.d4l.util}"
            const val utilJvm = "care.data4life.hc-util-sdk-kmp:util-jvm:${Version.multiplatform.d4l.util}"

            const val error = "care.data4life.hc-util-sdk-kmp:error:${Version.multiplatform.d4l.util}"
            const val errorAndroid = "care.data4life.hc-util-sdk-kmp:error-android:${Version.multiplatform.d4l.util}"
            const val errorJvm = "care.data4life.hc-util-sdk-kmp:error-jvm:${Version.multiplatform.d4l.util}"

            // FIXME
            const val fhirHelperCommon =
                "care.data4life.hc-fhir-helper-sdk-kmp:fhir-helper-metadata:${Version.d4l.fhirHelper}"
            const val fhirHelperAndroid =
                "care.data4life.hc-fhir-helper-sdk-kmp:fhir-helper-android:${Version.d4l.fhirHelper}"
            const val fhirHelperJvm = "care.data4life.hc-fhir-helper-sdk-kmp:fhir-helper-jvm:${Version.d4l.fhirHelper}"

            const val auth = "care.data4life.hc-auth-sdk-kmp:auth:${Version.multiplatform.d4l.auth}"
            const val authAndroid = "care.data4life.hc-auth-sdk-kmp:auth-android:${Version.multiplatform.d4l.auth}"
            const val authJvm = "care.data4life.hc-auth-sdk-kmp:auth-jvm:${Version.multiplatform.d4l.auth}"

            const val crypto = "care.data4life.hc-crypto-sdk-kmp:crypto:${Version.multiplatform.d4l.crypto}"
            const val cryptoAndroid =
                "care.data4life.hc-crypto-sdk-kmp:crypto-android:${Version.multiplatform.d4l.crypto}"
            const val cryptoJvm = "care.data4life.hc-crypto-sdk-kmp:crypto-jvm:${Version.multiplatform.d4l.crypto}"

            const val securestore =
                "care.data4life.hc-securestore-sdk-kmp:securestore:${Version.multiplatform.d4l.securestore}"
            const val securestoreAndroid =
                "care.data4life.hc-securestore-sdk-kmp:securestore-android:${Version.multiplatform.d4l.securestore}"
            const val securestoreJvm =
                "care.data4life.hc-securestore-sdk-kmp:securestore-jvm:${Version.multiplatform.d4l.securestore}"
        }

        const val koinCore = "io.insert-koin:koin-core:${Version.multiplatform.koin}"

        const val ktorCore = "io.ktor:ktor-client-core:${Version.multiplatform.ktor}"
        const val ktorCio = "io.ktor:ktor-client-cio:${Version.multiplatform.ktor}"
        const val ktorClientAuth = "io.ktor:ktor-client-auth:${Version.multiplatform.ktor}"
        const val ktorClientLogging = "io.ktor:ktor-client-logging:${Version.multiplatform.ktor}"
        const val ktorClientContentNegotiation = "io.ktor:ktor-client-content-negotiation:${Version.multiplatform.ktor}"
        const val ktorSerializationJson = "io.ktor:ktor-serialization-kotlinx-json:${Version.multiplatform.ktor}"
    }

    object MultiplatformTest {
        object Kotlin {
            const val testCommon = "org.jetbrains.kotlin:kotlin-test-common:${Version.kotlin}"

            const val testAnnotationsCommon = "org.jetbrains.kotlin:kotlin-test-annotations-common:${Version.kotlin}"
            const val testJvm = "org.jetbrains.kotlin:kotlin-test:${Version.kotlin}"

            const val testJvmJunit = "org.jetbrains.kotlin:kotlin-test-junit:${Version.kotlin}"
        }

        const val mockK = "io.mockk:mockk:${Version.multiplatformTest.mockK}"

        const val coroutines =
            "org.jetbrains.kotlinx:kotlinx-coroutines-test:${Version.multiplatform.kotlin.coroutines}"

        const val koin = "io.insert-koin:koin-test:${Version.multiplatform.koin}"

        const val ktorClientMock = "io.ktor:ktor-client-mock:${Version.multiplatform.ktor}"
    }

    object Jvm {
        const val fhirSdk = "care.data4life.hc-fhir-sdk-java:fhir-java:${Version.d4l.fhirSdk}"

        const val javaXAnnotation = "com.google.code.findbugs:jsr305:${Version.jvm.javaXAnnotation}"

        // Kotlin
        const val kotlinStdlibJdk8 = "org.jetbrains.kotlin:kotlin-stdlib-jdk8:${Version.kotlin}"

        // Authorization
        const val scribeCore = "com.github.scribejava:scribejava-core:${Version.jvm.scribe}"

        // Crypto
        const val bouncyCastleJdk15 = "org.bouncycastle:bcprov-jdk15on:${Version.jvm.bouncyCastle}"

        // Data
        const val moshi = "com.squareup.moshi:moshi:${Version.jvm.moshi}"
        const val moshiCodeGen = "com.squareup.moshi:moshi-kotlin-codegen:${Version.jvm.moshi}"

        // Date
        const val threeTenBP = "org.threeten:threetenbp:${Version.jvm.threeTenBP}"

        // Ui
        const val cmdClickt = "com.github.ajalt:clikt:${Version.jvm.clikt}"

        // RX
        const val rxJava = "io.reactivex.rxjava2:rxjava:${Version.jvm.rxJava}"

        // Network
        const val okHttp = "com.squareup.okhttp3:okhttp:${Version.jvm.okHttp}"
        const val okHttpLoggingInterceptor = "com.squareup.okhttp3:logging-interceptor:${Version.jvm.okHttp}"

        const val retrofit = "com.squareup.retrofit2:retrofit:${Version.jvm.retrofit}"
        const val retrofitConverterMoshi = "com.squareup.retrofit2:converter-moshi:${Version.jvm.retrofit}"
        const val retrofitAdapterRxJava = "com.squareup.retrofit2:adapter-rxjava2:${Version.jvm.retrofit}"
        const val gson = "com.squareup.retrofit2:converter-gson:${Version.jvm.gson}"
    }

    object JvmTest {
        const val junit = "junit:junit:${Version.jvmTest.jUnit}"

        const val kotlinTest = "org.jetbrains.kotlin:kotlin-test:${Version.kotlin}"
        const val kotlinJunit = "org.jetbrains.kotlin:kotlin-test-junit:${Version.kotlin}"

        const val truth = "com.google.truth:truth:${Version.jvmTest.truth}"

        const val mockitoInline = "org.mockito:mockito-inline:${Version.jvmTest.mockito}"
        const val mockitoCore = "org.mockito:mockito-core:${Version.jvmTest.mockito}"

        const val jsonAssert = "org.skyscreamer:jsonassert:${Version.jvmTest.jsonAssert}"

        const val koin = "io.insert-koin:koin-test:${Version.multiplatform.koin}"

        const val okHttpMockWebServer = "com.squareup.okhttp3:mockwebserver:${Version.jvm.okHttp}"
    }

    object Android {
        // Kotlin
        const val kotlinStdLib = "org.jetbrains.kotlin:kotlin-stdlib:${Version.kotlin}"
        const val kotlinReflect = "org.jetbrains.kotlin:kotlin-reflect:${Version.kotlin}"

        // Android
        const val androidDesugar = "com.android.tools:desugar_jdk_libs:${Version.android.desugar}"

        // Android X
        object AndroidX {
            const val ktx = "androidx.core:core-ktx:${Version.android.androidX.ktx}"
            const val appCompat = "androidx.appcompat:appcompat:${Version.android.androidX.appCompat}"
            const val browser = "androidx.browser:browser:${Version.android.androidX.browser}"
            const val constraintLayout =
                "androidx.constraintlayout:constraintlayout:${Version.android.androidX.constraintLayout}"
            const val swipeRefreshLayout =
                "androidx.swiperefreshlayout:swiperefreshlayout:${Version.android.androidX.swipeRefreshLayout}"
        }

        // Material
        const val material = "com.google.android.material:material:${Version.android.material}"

        // Google
        const val googlePlayServicesBase =
            "com.google.android.gms:play-services-base:${Version.android.googlePlayServices}"

        // Crypto
        const val bouncyCastleJdk15 = "org.bouncycastle:bcprov-jdk18on:${Version.jvm.bouncyCastle}"
        const val tink = "com.google.crypto.tink:tink-android:${Version.multiplatform.tink}"

        // Authorization
        const val appAuth = "net.openid:appauth:${Version.android.appAuth}"

        // Data
        const val moshi = "com.squareup.moshi:moshi:${Version.jvm.moshi}"

        // Date
        const val threeTenABP = "com.jakewharton.threetenabp:threetenabp:${Version.android.threeTenABP}"

        // UI
        const val photoView = "com.github.chrisbanes:PhotoView:${Version.android.photoView}"
        const val pdfView = "com.github.barteksc:android-pdf-viewer:${Version.android.pdfView}"

        // Injection
        const val koinCore = "io.insert-koin:koin-core:${Version.multiplatform.koin}"
        const val testKoin = "io.insert-koin:koin-test:${Version.multiplatform.koin}"

        // Rx
        const val rxJava2 = "io.reactivex.rxjava2:rxandroid:${Version.android.rxAndroid}"

        // Network
        const val okHttp = "com.squareup.okhttp3:okhttp:${Version.jvm.okHttp}"
        const val okHttpLoggingInterceptor = "com.squareup.okhttp3:logging-interceptor:${Version.jvm.okHttp}"

        const val retrofit = "com.squareup.retrofit2:retrofit:${Version.jvm.retrofit}"
        const val retrofitConverterMoshi = "com.squareup.retrofit2:converter-moshi:${Version.jvm.retrofit}"
        const val retrofitAdapterRxJava = "com.squareup.retrofit2:adapter-rxjava2:${Version.jvm.retrofit}"
    }

    object AndroidTest {
        const val core = "androidx.test:core:${Version.androidTest.androidXTestCore}"
        const val runner = "androidx.test:runner:${Version.androidTest.androidXTestRunner}"
        const val rules = "androidx.test:rules:${Version.androidTest.androidXTestRules}"
        const val orchestrator = "androidx.test:orchestrator:${Version.androidTest.androidXTestOrchestrator}"

        const val extJUnit = "androidx.test.ext:junit:${Version.androidTest.androidXTestExtJUnit}"

        const val espressoCore = "androidx.test.espresso:espresso-core:${Version.androidTest.androidXEspresso}"
        const val espressoIntents = "androidx.test.espresso:espresso-intents:${Version.androidTest.androidXEspresso}"
        const val espressoWeb = "androidx.test.espresso:espresso-web:${Version.androidTest.androidXEspresso}"

        const val uiAutomator = "androidx.test.uiautomator:uiautomator:${Version.androidTest.androidXUiAutomator}"

        const val robolectric = "org.robolectric:robolectric:${Version.androidTest.robolectric}"

        const val kakao = "com.github.wmontwe:Kakao:${Version.androidTest.kakao}"

        const val truth = "com.google.truth:truth:${Version.androidTest.truthAndroid}"

        const val okHttpMockWebServer = "com.squareup.okhttp3:mockwebserver:${Version.jvm.okHttp}"
    }
}
