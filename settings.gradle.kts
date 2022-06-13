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
pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
    }
}

includeBuild("./gradlePlugin/core-dependency")

include(
    ":sample-android", ":sample-jvm",

    ":sdk-core", ":sdk-android", "sdk-jvm", "sdk-ingestion",

    ":sdk-android-test"
)

val includeAuth: String by settings
if (includeAuth.toBoolean()) {
    includeBuild("../hc-auth-sdk-kmp") {
        dependencySubstitution {
            substitute(module("care.data4life.hc-auth-sdk-kmp:auth"))
                .using(project(":auth"))
            substitute(module("care.data4life.hc-auth-sdk-kmp:auth-jvm"))
                .using(project(":auth"))
            substitute(module("care.data4life.hc-auth-sdk-kmp:auth-android"))
                .using(project(":auth"))
        }
    }
}

val includeCrypto: String by settings
if (includeCrypto.toBoolean()) {
    includeBuild("../hc-crypto-sdk-kmp") {
        dependencySubstitution {
            substitute(module("care.data4life.hc-crypto-sdk-kmp:crypto"))
                .using(project(":crypto"))
            substitute(module("care.data4life.hc-crypto-sdk-kmp:crypto-jvm"))
                .using(project(":crypto"))
            substitute(module("care.data4life.hc-crypto-sdk-kmp:crypto-android"))
                .using(project(":crypto"))
        }
    }
}

val includeSecurestore: String by settings
if (includeSecurestore.toBoolean()) {
    includeBuild("../hc-securestore-sdk-kmp") {
        dependencySubstitution {
            substitute(module("care.data4life.hc-securestore-sdk-kmp:securestore"))
                .using(
                    project(":securestore")
                )
            substitute(module("care.data4life.hc-securestore-sdk-kmp:securestore-jvm"))
                .using(
                    project(":securestore")
                )
            substitute(module("care.data4life.hc-securestore-sdk-kmp:securestore-android"))
                .using(
                    project(":securestore")
                )
        }
    }
}

rootProject.name = "hc-sdk-kmp"
