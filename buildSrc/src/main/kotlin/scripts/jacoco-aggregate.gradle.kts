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
package scripts

import java.io.File
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.kotlin.dsl.get
import org.gradle.kotlin.dsl.the
import org.gradle.testing.jacoco.plugins.JacocoTaskExtension
import org.gradle.testing.jacoco.tasks.JacocoReport

plugins {
    jacoco
}

jacoco {
    toolVersion = Versions.jacocoVersion
}

fun registerProjectCoverage(
    name: String,
    excludeDirs: List<String>
) {
    tasks.register<JacocoReport>("jacocoProject${name.capitalize()}Report") {
        group = "Verification"
        description =
            "Generates code coverage report for the entire project for ${name.capitalize()}."

        dependsOn(subprojects.map { it.tasks.withType<JacocoReport>() })

        reports {
            html.isEnabled = true
            xml.isEnabled = true
            csv.isEnabled = true

            html.destination = layout.buildDirectory.dir(
                "reports/jacoco/test/${name}/${project.name}"
            ).get().asFile
            csv.destination = layout.buildDirectory.file(
                "reports/jacoco/test/${name}/${project.name}.csv"
            ).get().asFile
            xml.destination = layout.buildDirectory.file(
                "reports/jacoco/test/${name}/${project.name}.xml"
            ).get().asFile
        }

        val filter = mutableListOf(
            "**/databinding/**/*.*",
            "**/android/databinding/*Binding.*",
            "**/BR.*",
            "**/R.class",
            "**/R$*.class",
            "**/BuildConfig.*",
            "**/Manifest*.*",
            "**/*Test*.*",
            "**/android/**/*.*",
            "**/*\$Lambda\$*.*",
            "**/*\$inlined\$*.*",
            "**/scripts"
        ).also { it.addAll(excludeDirs) }

        val javaClasses = fileTree(
            mapOf(
                "dir" to ".",
                "includes" to listOf(
                    "**/classes/**/main/**",
                    "**/intermediates/classes/**",
                    "**/intermediates/javac/**/classes/**"
                ),
                "excludes" to filter
            )
        )

        val kotlinClasses = fileTree(
            mapOf(
                "dir" to ".",
                "include" to "**/tmp/kotlin-classes/**",
                "exclude" to filter
            )
        )

        val additionalSources = mutableListOf<File>()
        additionalSources.add(file("./**/generated/source/buildConfig/**"))
        additionalSources.add(file("./**/generated/source/r/**"))

        sourceDirectories.from(
            fileTree(
                mapOf(
                    "dir" to ".",
                    "includes" to listOf(
                        "./**/src/main/java",
                        "./**/src/*/java",
                        "./**/src/main/kotlin",
                        "./**/src/*/kotlin"
                    ),
                    "excludes" to mutableListOf(
                        "**/scripts"
                    ).also { it.addAll(excludeDirs) }
                )
            )
        )

        classDirectories.from(files(listOf(javaClasses, kotlinClasses).toSet()))
        classDirectories.from(
            fileTree(
                mapOf(
                    "dir" to ".",
                    "includes" to listOf(
                        "**/tmp/kotlin-classes",
                        "**/classes/**/main",
                        "**/intermediates/classes"
                    ),
                    "excludes" to mutableListOf(
                        "**/scripts"
                    ).also { it.addAll(excludeDirs) }
                )
            )
        )
        additionalSourceDirs.from(additionalSources)

        executionData.setFrom(
            fileTree(
                mapOf(
                    "dir" to ".",
                    "includes" to listOf("**/*.exec", "**/*.ec"),
                    "excludes" to mutableListOf(
                        "**/scripts"
                    ).also { it.addAll(excludeDirs) }
                )
            )
        )
    }
}

project.afterEvaluate {
    registerProjectCoverage("jvm", listOf("**/*android", "**/*ingestion"))
    registerProjectCoverage("android", listOf("**/*jvm", "**/debug/**", "**/*ingestion"))
    registerProjectCoverage("ingestion", listOf("**/*android", "**/*jvm"))
}
