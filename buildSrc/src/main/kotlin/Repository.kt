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

import org.gradle.api.Project
import org.gradle.api.artifacts.dsl.RepositoryHandler
import org.gradle.kotlin.dsl.maven

object Repository {
    private const val gitHubOrgD4l = "d4l-data4life"

    val github = listOf(
        // GitHub organization, GitHub repository name, Maven dependency group
        listOf(gitHubOrgD4l, "hc-util-sdk-kmp", "care.data4life.hc-util-sdk-kmp"),
        listOf(gitHubOrgD4l, "hc-fhir-sdk-java", "care.data4life.hc-fhir-sdk-java"),
        listOf(gitHubOrgD4l, "hc-fhir-helper-sdk-kmp", "care.data4life.hc-fhir-helper-sdk-kmp")
    )

    val d4l = listOf(
        // Maven dependency group
        "care.data4life.hc-util-sdk-kmp",
        "care.data4life.hc-fhir-sdk-java",
        "hc-fhir-helper-sdk-kmp"
    )
}

fun RepositoryHandler.gitHub(project: Project) {
    Repository.github.forEach { (organization, repository, group) ->
        maven {
            setUrl("https://maven.pkg.github.com/$organization/$repository")
            credentials {
                username = project.project.findProperty("gpr.user") as String?
                    ?: System.getenv("PACKAGE_REGISTRY_USERNAME")
                password = project.project.findProperty("gpr.key") as String?
                    ?: System.getenv("PACKAGE_REGISTRY_TOKEN")
            }
            content {
                includeGroup(group)
            }
        }
    }
}

fun RepositoryHandler.d4l() {
    maven("https://raw.github.com/d4l-data4life/maven-releases/main/releases") {
        content {
            Repository.d4l.forEach { group ->
                includeGroup(group)
            }
        }
    }
    maven("https://raw.github.com/d4l-data4life/maven-snapshots/main/snapshots") {
        content {
            Repository.d4l.forEach { group ->
                includeGroup(group)
            }
        }
    }
    maven("https://raw.github.com/d4l-data4life/maven-features/main/features") {
        content {
            Repository.d4l.forEach { group ->
                includeGroup(group)
            }
        }
    }
}
