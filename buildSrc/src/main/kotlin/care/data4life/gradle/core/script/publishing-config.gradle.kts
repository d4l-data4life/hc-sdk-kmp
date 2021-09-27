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

package care.data4life.gradle.core.script

import care.data4life.gradle.core.config.LibraryConfig

/**
 * Install:
 *
 * Just add id("care.data4life.gradle.core.script.publishing-config") to your project module build.gradle.kts plugins section
 *
 * plugins {
 *     id("care.data4life.gradle.core.script.publishing-config")
 * }
 *
 * Usage:
 *
 * To publish to to https://github.com/d4l-data4life/maven-repository/ just run:
 * - ./gradlew publishFeature
 * - ./gradlew publishSnapshot
 * - ./gradlew publishRelease
 *
 * This requires a care.data4life.gradle.core.config.LibraryConfig configured
 */
plugins {
    `maven-publish`
}

publishing {
    repositories {
        maven {
            name = "GitHubPackages"
            setUrl("https://maven.pkg.github.com/${LibraryConfig.githubOwner}/${LibraryConfig.githubRepository}")
            credentials {
                username = (project.findProperty("gpr.user")
                    ?: System.getenv("PACKAGE_REGISTRY_UPLOAD_USERNAME")).toString()
                password = (project.findProperty("gpr.key")
                    ?: System.getenv("PACKAGE_REGISTRY_UPLOAD_TOKEN")).toString()
            }
        }

        val target = "file://${project.rootProject.buildDir}/gitPublish"

        maven {
            name = "ReleasePackages"
            setUrl("$target/maven-releases/releases")
        }

        maven {
            name = "SnapshotPackages"
            setUrl("$target/maven-snapshots/snapshots")
        }

        maven {
            name = "FeaturePackages"
            setUrl("$target/maven-features/features")
        }
    }

    publications {
        withType<MavenPublication> {
            groupId = LibraryConfig.PublishConfig.groupId

            pom {
                description.set(LibraryConfig.PublishConfig.description)
                url.set(LibraryConfig.PublishConfig.url)
                inceptionYear.set(LibraryConfig.PublishConfig.year)

                licenses {
                    license {
                        name.set(LibraryConfig.PublishConfig.licenseName)
                        url.set(LibraryConfig.PublishConfig.licenseUrl)
                        distribution.set(LibraryConfig.PublishConfig.licenseDistribution)
                    }
                }

                developers {
                    developer {
                        id.set(LibraryConfig.PublishConfig.developerId)
                        name.set(LibraryConfig.PublishConfig.developerName)
                        email.set(LibraryConfig.PublishConfig.developerEmail)
                    }
                }

                scm {
                    connection.set(LibraryConfig.PublishConfig.scmConnection)
                    developerConnection.set(LibraryConfig.PublishConfig.scmDeveloperConnection)
                    url.set(LibraryConfig.PublishConfig.scmUrl)
                }
            }
        }
    }
}
