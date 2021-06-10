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

import com.android.build.gradle.api.LibraryVariant

plugins {
    id("com.android.library")
    id("kotlin-android")
    id("kotlin-android-extensions")
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

        getByName("release") {
            isTestCoverageEnabled = true
        }
    }

    testOptions {
        animationsDisabled = true

        unitTests {
            isReturnDefaultValues = true
            isIncludeAndroidResources = true

            all {
                // see: https://stackoverflow.com/questions/48945710/after-upgrade-of-gradle-to-the-version-3-0-1-something-generates-redundant-jacoc
                it.systemProperty("jacoco-agent.destfile", "${buildDir.path}/jacoco/jacoco.exec")
                // see: https://github.com/gradle/kotlin-dsl-samples/issues/440
                it.jvmArgs("-noverify", "-ea")
                it.extensions
                    .getByType(JacocoTaskExtension::class.java)
                    .isIncludeNoLocationClasses = true

                // see: https://issuetracker.google.com/issues/178015739?pli=1
                it.extensions
                    .getByType(JacocoTaskExtension::class.java)
                    .excludes = listOf("jdk.internal.*", "kotlin.*", "com.library.*")

                it.extensions
                    .getByType(JacocoTaskExtension::class.java)
                    .includes = listOf("com.application.*")

                it.testLogging {
                    events("passed", "skipped", "failed", "standardOut", "standardError")
                }
            }
        }
    }
}

project.afterEvaluate {
    android.libraryVariants.forEach { variant ->
        val variantName = variant.name
        val capName = variantName.capitalize()
        val unitTests = "test${capName}UnitTest"
        val instrumentedTests = "create${capName}CoverageReport"

        val task by tasks.register("jacoco${capName}TestReport", JacocoReport::class) {
            group = "Verification"
            description = "Generate coverage reports for the ${variantName.capitalize()}."
            this.prepareCoverage(
                variant,
                variantName,
                listOf(unitTests, instrumentedTests)
            )
        }
    }
}

fun JacocoReport.prepareCoverage(
    variant: LibraryVariant,
    variantName: String,
    dependencies: List<String>
) {
    dependsOn(dependencies)

    reports {
        html.isEnabled = true
        xml.isEnabled = true
        csv.isEnabled = true

        html.destination = layout.buildDirectory.dir(
            "reports/jacoco/test/${variantName}/${project.name}"
        ).get().asFile
        csv.destination = layout.buildDirectory.file(
            "reports/jacoco/test/${variantName}/${project.name}.csv"
        ).get().asFile
        xml.destination = layout.buildDirectory.file(
            "reports/jacoco/test/${variantName}/${project.name}.xml"
        ).get().asFile
    }

    val filter = listOf(
        "**/databinding/**/*.*",
        "**/android/databinding/*Binding.*",
        "**/BR.*",
        "**/R.class",
        "**/R$*.class",
        "**/BuildConfig.*",
        "**/Manifest*.*",
        "**/*Test*.*",
        "android/**/*.*",
        "**/*\$Lambda\$*.*",
        "**/*\$inlined\$*.*"
    )

    val javaClasses = fileTree(variant.javaCompileProvider.get().destinationDir) {
        exclude(filter)
    }
    val kotlinClasses = fileTree(
        mapOf(
            "dir" to "${buildDir}/tmp/kotlin-classes/${variantName}",
            "exclude" to filter
        )
    )

    val additionalSources = mutableListOf<File>()
    subprojects.forEach {
        additionalSources.addAll(it.the<SourceSetContainer>()["main"].allSource.srcDirs)
    }
    additionalSources.add(file("${buildDir}/generated/source/buildConfig/${variantName}"))
    additionalSources.add(file("${buildDir}/generated/source/r/${variantName}"))

    sourceDirectories.setFrom(
        files(
            "${project.projectDir}/src/main/java",
            "${project.projectDir}/src/${variantName}/java",
            "${project.projectDir}/src/main/kotlin",
            "${project.projectDir}/src/${variantName}/kotlin"
        )
    )

    classDirectories.setFrom(files(listOf(javaClasses, kotlinClasses).toSet()))
    classDirectories.from(
        fileTree(
            mapOf(
                "dir" to "${project.buildDir}",
                "includes" to listOf(
                    "**/tmp/kotlin-classes",
                    "**/classes/**/main",
                    "**/intermediates/classes"
                )
            )
        )
    )
    additionalSourceDirs.setFrom(additionalSources)

    executionData.setFrom(
        fileTree(
            mapOf(
                "dir" to "${project.buildDir}",
                "includes" to listOf("**/*.exec", "**/*.ec")
            )
        )
    )
}
