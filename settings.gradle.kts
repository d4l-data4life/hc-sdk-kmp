rootProject.name = "mpp-sdk"

include(
        ":sample-android", ":sample-jvm",

        ":sdk-core", ":sdk-android", "sdk-jvm",

        ":sdk-android-test",

        ":sdk-doc",

        ":securestore-common", ":securestore-android", ":securestore-jvm",

        ":crypto-common", ":crypto-android", ":crypto-jvm",

        ":auth-common", ":auth-android", ":auth-jvm"
)
