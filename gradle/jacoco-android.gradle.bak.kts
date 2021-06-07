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
plugins {
    id("com.android.library")
    id("kotlin-platform-android")
    jacoco
}

jacoco {
    toolVersion = Versions.jacocoVersion
}

android {
    buildTypes {
        getByName("debug") {
            isTestCoverageEnabled = true
        }
    }

    testOptions {
        isReturnDefaultValues = true
        isIncludeAndroidResources = true

        all {
            it.jvmArgs("-noverify", "-ea")
            it.testLogging {
                events("passed", "skipped", "failed", "standardOut", "standardError")
            }
        }
    }
}

tasks.named<Test>("unitTest") {
    configure<JacocoTaskExtension> {
        setDestinationFile(layout.buildDirectory.file("jacoco/${name}.exec").get().asFile)
        isIncludeNoLocationClasses = true
        excludes = listOf("jdk.internal.*", "kotlin")
    }
}

tasks.jacocoTestReport {
    dependsOn(
        listOf(
            "testDebugUnitTest",
            "createDebugCoverageReport"
        )
    )

    reports {
        html.isEnabled = true
        xml.isEnabled = true
        csv.isEnabled = true

        html.destination = layout.buildDirectory.dir("reports/jacoco/test/${project.name}").get().asFile
        csv.destination = layout.buildDirectory.file("reports/jacoco/test/${project.name}.csv").get().asFile
        xml.destination = layout.buildDirectory.file("reports/jacoco/test/${project.name}.xml").get().asFile
    }

    val filter = listOf(
        "**/R.class",
        "**/R$*.class",
        "**/BuildConfig.*",
        "**/Manifest*.*",
        "**/*Test*.*",
        "android/**/*.*",
        // Kotlin specific
        "**/*\$Lambda\$*.*",
        "**/*\$inlined\$*.*"
    )

    sourceDirectories.setFrom(
        fileTree("${project.buildDir}") {
            include(
                "src/main/java/**",
                "src/main/kotlin/**",
                "src/debug/java/**",
                "src/debug/kotlin/**"
            )
        }
    )

    classDirectories.setFrom(
        fileTree("${project.buildDir}") {
            include(
                "**/classes/**/main/**",
                "**/intermediates/classes/debug/**",
                "**/intermediates/javac/debug/*/classes/**",
                "**/tmp/kotlin-classes/debug/**"
            )

            exclude(filter)
        }
    )
    executionData.setFrom(
        fileTree(
            "dir" to buildDir,
            "includes" to listOf(
                "outputs/code_coverage/**/*.ec",
                "jacoco/*.exec",
                "outputs/code-coverage/connected/*coverage.ec"
            )
        )
    )
}
