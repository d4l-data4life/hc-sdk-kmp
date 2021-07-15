
rootProject.name = "hc-sdk-kmp"

include(
    ":sample-android", ":sample-jvm",

    ":sdk-core", ":sdk-android", "sdk-jvm", "sdk-ingestion",

    ":sdk-android-test",

    ":sdk-doc"
)

val includeAuth: String by settings
if (includeAuth.toBoolean()) {
    val version = "1.13.2"
    includeBuild("../hc-auth-sdk-kmp") {
        dependencySubstitution {
            substitute(module("care.data4life.hc-auth-sdk-kmp:auth:$version"))
                .using(project(":auth"))
            substitute(module("care.data4life.hc-auth-sdk-kmp:auth-jvm:$version"))
                .using(project(":auth"))
            substitute(module("care.data4life.hc-auth-sdk-kmp:auth-android:$version"))
                .using(project(":auth"))
        }
    }
}

val includeCrypto: String by settings
if (includeCrypto.toBoolean()) {
    val version = "1.13.2"
    includeBuild("../hc-crypto-sdk-kmp") {
        dependencySubstitution {
            substitute(module("care.data4life.hc-crypto-sdk-kmp:crypto:$version"))
                .using(project(":crypto"))
            substitute(module("care.data4life.hc-crypto-sdk-kmp:crypto-jvm:$version"))
                .using(project(":crypto"))
            substitute(module("care.data4life.hc-crypto-sdk-kmp:crypto-android:$version"))
                .using(project(":crypto"))
        }
    }
}

val includeSecurestore: String by settings
if (includeSecurestore.toBoolean()) {
    val version = "1.13.2"
    includeBuild("../hc-securestore-sdk-kmp") {
        dependencySubstitution {
            substitute(module("care.data4life.hc-securestore-sdk-kmp:securestore:$version"))
                .using(
                    project(":securestore")
                )
            substitute(module("care.data4life.hc-securestore-sdk-kmp:securestore-jvm:$version"))
                .using(
                    project(":securestore")
                )
            substitute(module("care.data4life.hc-securestore-sdk-kmp:securestore-android:$version"))
                .using(
                    project(":securestore")
                )
        }
    }
}
