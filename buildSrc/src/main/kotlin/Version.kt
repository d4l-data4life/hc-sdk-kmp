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

object Version {
    // D4L
    const val sdkUtil = "1.6.1"

    /**
     * [hc-fhir-sdk-java](https://github.com/d4l-data4life/hc-fhir-sdk-java)
     */
    const val fhirSdk = "1.0.0"

    /**
     * [hc-fhir-helper-sdk-kmp](https://github.com/d4l-data4life/hc-fhir-helper-sdk-kmp)
     */
    const val fhirHelper = "1.4.0"

    // kotlin
    const val kotlin = "1.3.72"

    /**
     *  https://github.com/Kotlin/kotlinx.coroutines
     */
    const val kotlinCoroutines = "1.3.3"

    object GradlePlugin {
        const val kotlin = Version.kotlin
        const val android = "4.1.1"

        /**
         * [Dexcount](https://github.com/KeepSafe/dexcount-gradle-plugin)
         */
        const val dexcount = "1.0.2"

        /**
         * [Gradle DownloadTask](https://github.com/michel-kraemer/gradle-download-task)
         */
        const val downloadTask = "3.4.3"

        /**
         * [Dokka - Documentation Engine for Kotlin](https://github.com/Kotlin/dokka)
         */
        const val dokka = "0.10.1"
    }

    // Java
    const val javaXAnnotation = "3.0.2"
    const val jacocoVersion = "0.8.3"

    // Android
    const val androidDesugar = "1.0.5"

    // AndroidX
    const val androidXKtx = "1.2.0"
    const val androidXAppCompat = "1.1.0"
    const val androidXBrowser = "1.2.0"

    const val androidXConstraintLayout = "1.1.3"

    // Material
    const val material = "1.1.0"

    // Google
    const val googlePlayServices = "16.1.0"

    // Crypto
    /**
     * [BouncyCastle](http://www.bouncycastle.org/java.html)
     */
    const val bouncyCastle = "1.64"

    // Tink
    /**
     * [tink](https://github.com/google/tink)
     */
    const val tink = "1.2.2"

    // Authorization
    /**
     * [appAuth](https://github.com/openid/AppAuth-Android)
     */
    const val appAuth = "0.7.1"

    /**
     * [appAuthPatch](https://github.com/gesundheitscloud/AppAuth-Android)
     */
    const val appAuthPatch = "9e3cc033ff"

    /**
     * [scribe](https://github.com/scribejava/scribejava)
     */
    const val scribe = "6.1.0"

    // Network
    /**
     * [okHttp](https://github.com/square/okhttp)
     */
    const val okHttp = "4.7.2"

    /**
     *
     *[retrofit](https://github.com/square/retrofit)
     */
    const val retrofit = "2.9.0"

    // Data
    /**
     * [moshi](https://github.com/square/moshi)
     */
    const val moshi = "1.8.0"

    /**
     *
     *[gson](https://github.com/square/retrofit/tree/master/retrofit-converters/gson)
     */
    const val gson = "2.9.0"


    // Date

    /**
     * [ThreeTen Backport](https://www.threeten.org/threetenbp)
     */
    const val threeTenBP = "1.4.4"

    /**
     * [ThreeTen Android Backport](https://github.com/JakeWharton/ThreeTenABP)
     */
    const val threeTenABP = "1.2.4"

    // Injection
    /**
     * [Koin](https://github.com/InsertKoinIO/koin)
     */
    const val koin = "2.0.1"

    // Rx
    /**
     * [RxJava](https://github.com/ReactiveX/RxJava)
     */
    const val rxJava = "2.2.19"

    /**
     * [RxAndroid](https://github.com/ReactiveX/RxAndroid)
     */
    const val rxAndroid = "2.1.1"

    // Ui
    /**
     * [clikt](https://github.com/ajalt/clikt)
     */
    const val clikt = "1.7.0"

    /**
     * [photoView](https://github.com/chrisbanes/PhotoView)
     */
    const val photoView = "2.0.0"

    /**
     * [pdfView](https://github.com/barteksc/AndroidPdfViewer)
     */
    const val pdfView = "3.1.0-beta.1"


    // Junit Test
    const val testJUnit = "4.12"

    /**
     * [mockk](http://mockk.io)
     */
    const val testMockk = "1.10.0"

    const val testTruth = "0.44"
    const val testTruthAndroid = "0.44"

    /**
     * [mockito](https://github.com/mockito/mockito)
     */
    const val testMockito = "2.27.0"

    const val testJsonAssert = "1.5.0"

    /**
     * [robolectric](http://robolectric.org/)
     */
    const val robolectric = "4.3.1"

    // Android Test
    const val androidTestCore = "1.0.0"
    const val androidTest = "1.1.1"
    const val androidTestEspresso = "3.1.1"
    const val androidXUiAutomator = "2.2.0"

    const val androidXKakao = "1.4.0-androidx"

}
