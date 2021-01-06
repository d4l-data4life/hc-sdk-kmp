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

import org.gradle.api.tasks.Exec

plugins {
    id("care.data4life.git-publish")
}

afterEvaluate {
    configure<care.data4life.gradle.git.publish.GitPublishExtension> {
        repoUri.set("git@github.com:d4l-data4life/maven-repository.git")

        branch.set("main")

        contents {
        }

        preserve {
            include("**/*")
        }

        commitMessage.set("Publish ${LibraryConfig.name} ${project.version}")
    }
}

task<Exec>("publishFeature") {
    group = "publishing"

    commandLine("./gradlew",
            "gitPublishReset",
            "publishAllPublicationsToFeaturePackagesRepository",
            "gitPublishCommit",
            "gitPublishPush"
    )
}

task<Exec>("publishSnapshot") {
    group = "publishing"

    commandLine("./gradlew",
            "gitPublishReset",
            "publishAllPublicationsToSnapshotPackagesRepository",
            "gitPublishCommit",
            "gitPublishPush"
    )
}

task<Exec>("publishRelease") {
    group = "publishing"

    commandLine("./gradlew",
            "gitPublishReset",
            "publishAllPublicationsToReleasePackagesRepository",
            "gitPublishCommit",
            "gitPublishPush"
    )
}
