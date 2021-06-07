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
apply(plugin = "jacoco")

tasks.named<Test>("test") {
    testLogging {
        events("passed", "skipped", "failed", "standardOut", "standardError")
    }
}

tasks.named<JacocoReport>("jacocoTestReport") {
    dependsOn(tasks.withType<Test>(Test::class.java))

    reports {
        html.isEnabled = true
        xml.isEnabled = true
        csv.isEnabled = true

        html.destination = layout.buildDirectory.dir("reports/jacoco/test/${project.name}").get().asFile
        csv.destination = layout.buildDirectory.file("reports/jacoco/test/${project.name}.csv").get().asFile
        xml.destination = layout.buildDirectory.file("reports/jacoco/test/${project.name}.xml").get().asFile
    }
}
