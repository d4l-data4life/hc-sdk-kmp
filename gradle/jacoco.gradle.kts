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


apply(plugin = "jacoco")

val d4lClientConfig = D4LConfigHelper.loadClientConfigAndroid("$rootDir")
val d4LTestConfig = D4LConfigHelper.loadTestConfigAndroid("$rootDir")
val unitTestAssetsPath = "${projectDir}/src/test/resources"

val provideTestConfig: Task by tasks.creating {
    doLast {
        val unitTestAsset = File(unitTestAssetsPath)
        if (!unitTestAsset.exists()) unitTestAsset.mkdirs()
        File(unitTestAssetsPath, "test_config.json").writeText(D4LConfigHelper.toJson(d4LTestConfig))
        File(unitTestAssetsPath, "client_config.json").writeText(D4LConfigHelper.toJson(d4lClientConfig))
    }
}

tasks.named("clean") {
    doLast {
        delete("${unitTestAssetsPath}/test_config.json")
        delete("${unitTestAssetsPath}/client_config.json")
    }
}

tasks.named<Test>("test") {
    testLogging {
        events("passed", "skipped", "failed", "standardOut", "standardError")
    }
}

tasks.named<JacocoReport>("jacocoTestReport") {
    reports {
        xml.isEnabled = true
        csv.isEnabled = true
    }
    dependsOn(tasks.named("test"))
}

tasks.named("test") {
    dependsOn(provideTestConfig)
}
