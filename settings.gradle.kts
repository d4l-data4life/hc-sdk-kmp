
rootProject.name = "hc-sdk-kmp"

include(
    ":sample-android", ":sample-jvm",

    ":sdk-core", ":sdk-android", "sdk-jvm", "sdk-ingestion",

    ":sdk-android-test",

    ":sdk-doc"
)
