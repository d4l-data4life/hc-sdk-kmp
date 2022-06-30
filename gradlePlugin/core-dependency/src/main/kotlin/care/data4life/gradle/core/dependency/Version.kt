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

object Version {

    val gradlePlugin = GradlePlugin
    val multiplatform = Multiplatform
    val multiplatformTest = MultiplatformTest
    val jvm = Jvm
    val jvmTest = JvmTest
    val android = Android
    val androidTest = AndroidTest

    /**
     * [Kotlin](https://github.com/JetBrains/kotlin)
     */
    const val kotlin = "1.6.21"

    val d4l = D4L

    object D4L {
        /**
         * [hc-fhir-sdk-java](https://github.com/d4l-data4life/hc-fhir-sdk-java)
         */
        const val fhirSdk = "1.8.0"

        /**
         * [hc-fhir-helper-sdk-kmp](https://github.com/d4l-data4life/hc-fhir-helper-sdk-kmp)
         */
        const val fhirHelper = "1.9.0"
    }

    object GradlePlugin {
        const val kotlin = Version.kotlin
        const val android = "7.3.0-beta02"

        /**
         * [Kover]()https://github.com/Kotlin/kotlinx-kover)
         */
        const val kover = "0.5.0"

        /**
         * [Dexcount](https://github.com/KeepSafe/dexcount-gradle-plugin)
         */
        const val dexcount = "3.1.0"

        /**
         * [Gradle DownloadTask](https://github.com/michel-kraemer/gradle-download-task)
         */
        const val downloadTask = "5.1.0"

        /**
         * [Dokka - Documentation Engine for Kotlin](https://github.com/Kotlin/dokka)
         */
        const val dokka = "0.10.1"

        /**
         * [Gradle Groovy](https://github.com/apache/groovy)
         */
        const val groovyAll = "3.0.9"

        /**
         * [Gradle OWASP](https://github.com/jeremylong/dependency-check-gradle)
         */
        const val owasp = "7.1.0.1"

        /**
         * [Gradle HttpComponents](https://hc.apache.org/httpcomponents-client-4.5.x/current/httpclient/dependency-info.html)
         */
        const val httpComponents = "4.5.13"

        /**
         * [Gradle Android Maven](https://hc.apache.org/httpcomponents-client-4.5.x/current/httpclient/project-info.html)
         * Abandoned!!!
         */
        const val androidMaven = "2.1"

        /**
         * [Gradle Android Jacoco](https://github.com/vanniktech/gradle-android-junit-jacoco-plugin)
         */
        const val androidJacoco = "0.16.0"
    }

    object Multiplatform {

        val kotlin = KotlinX
        val d4l = D4L

        object KotlinX {
            /**
             *  https://github.com/Kotlin/kotlinx.coroutines
             */
            const val coroutines = "1.6.2"

            /**
             * [Serialization](https://github.com/Kotlin/kotlinx.serialization)
             */
            const val serialization = "1.3.3"

            /**
             * [DateTime](https://github.com/Kotlin/kotlinx-datetime)
             */
            const val dateTime = "0.2.1"
        }

        object D4L {
            /**
             * [hc-util-sdk-kmp](https://github.com/d4l-data4life/hc-util-sdk-kmp)
             */
            const val util = "1.13.0"

            /**
             * [hc-auth-sdk-kmp](https://github.com/d4l-data4life/hc-auth-sdk-kmp)
             */
            const val auth = "1.16.0"

            /**
             * [hc-crypto-sdk-kmp](https://github.com/d4l-data4life/hc-crypto-sdk-kmp)
             */
            const val crypto = "1.15.0"

            /**
             * [hc-securestore-sdk-kmp](https://github.com/d4l-data4life/hc-securestore-sdk-kmp)
             */
            const val securestore = "1.15.0"
        }

        /**
         * [Koin](https://github.com/InsertKoinIO/koin)
         */
        const val koin = "3.1.6"

        /**
         * [Ktor](https://ktor.io)
         */
        const val ktor = "2.0.2"

        /**
         * [tink](https://github.com/google/tink)
         */
        const val tink = "1.4.0"
    }

    object MultiplatformTest {
        /**
         * [mockk](http://mockk.io)
         */
        const val mockK = "1.10.6"
    }

    object Jvm {
        // Crypto
        /**
         * [BouncyCastle](http://www.bouncycastle.org/java.html)
         */
        const val bouncyCastle = "1.71"

        // Authorization
        /**
         * [scribe](https://github.com/scribejava/scribejava)
         */
        const val scribe = "6.3.0"

        const val javaXAnnotation = "3.0.2"

        // Network
        /**
         * [okHttp](https://github.com/square/okhttp)
         */
        const val okHttp = "4.9.3"

        /**
         *
         *[retrofit](https://github.com/square/retrofit)
         */
        const val retrofit = "2.9.0"

        // Data
        /**
         * [moshi](https://github.com/square/moshi)
         */
        const val moshi = "1.13.0"

        /**
         *
         *[gson](https://github.com/square/retrofit/tree/master/retrofit-converters/gson)
         */
        const val gson = "2.9.0"

        // Date
        /**
         * [ThreeTen Backport](https://www.threeten.org/threetenbp)
         */
        const val threeTenBP = "1.6.0"

        /**
         * [RxJava](https://github.com/ReactiveX/RxJava)
         */
        const val rxJava = "2.2.21"

        // Ui
        /**
         * [clikt](https://github.com/ajalt/clikt)
         */
        const val clikt = "1.7.0"
    }

    object JvmTest {
        // Junit Test
        const val jUnit = "4.13.2"

        const val truth = "0.44"

        const val jacoco = "0.8.8"

        const val jsonAssert = "1.5.0"

        /**
         * [mockito](https://github.com/mockito/mockito)
         */
        const val mockito = "4.5.1"
    }

    object Android {

        val androidX = AndroidX

        /**
         * [Android Desugar](https://developer.android.com/studio/write/java8-support)
         */
        const val desugar = "1.1.5"

        object AndroidX {
            /**
             * [AndroidX](https://developer.android.com/jetpack/androidx)
             */
            const val core = "1.1.0"
            const val ktx = "1.7.0"
            const val appCompat = "1.4.2"
            const val browser = "1.4.0"

            const val constraintLayout = "2.1.4"
            const val swipeRefreshLayout = "1.1.0"
        }

        /**
         * [Material Android](https://github.com/material-components/material-components-android)
         */
        const val material = "1.6.1"

        /**
         * [PlayService Base](https://developers.google.com/android/guides/setup)
         */
        const val googlePlayServices = "18.0.1"

        // Authorization
        /**
         * [appAuth](https://github.com/openid/AppAuth-Android)
         */
        const val appAuth = "0.11.1"

        // Date
        /**
         * [ThreeTen Android Backport](https://github.com/JakeWharton/ThreeTenABP)
         */
        const val threeTenABP = "1.4.0"

        /**
         * [RxAndroid](https://github.com/ReactiveX/RxAndroid)
         */
        const val rxAndroid = "2.1.1"

        /**
         * [photoView](https://github.com/chrisbanes/PhotoView)
         */
        const val photoView = "2.3.0"

        /**
         * [pdfView](https://github.com/barteksc/AndroidPdfViewer)
         */
        const val pdfView = "3.2.0-beta.1"
    }

    object AndroidTest {
        /**
         * [AndroidXTest](https://developer.android.com/jetpack/androidx/releases/test)
         */
        const val androidXTestCore = "1.4.0"
        const val androidXTestRunner = "1.4.0"
        const val androidXTestOrchestrator = "1.4.1"
        const val androidXTestRules = "1.4.0"
        const val androidXEspresso = "3.4.0"
        const val androidXUiAutomator = "2.2.0"
        const val androidXTestExtJUnit = "1.1.3"

        // Out of life
        const val kakao = "1.4.0-androidx"

        /**
         * [robolectric](http://robolectric.org/)
         */
        const val robolectric = "4.8"

        const val truthAndroid = "0.44"
    }
}
