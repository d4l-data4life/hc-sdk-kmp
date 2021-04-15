/*
 * Copyright (c) 2021. D4L data4life gGmbH / All rights reserved.
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

import systems.danger.kotlin.danger
import systems.danger.kotlin.fail
import systems.danger.kotlin.onGitHub
import systems.danger.kotlin.warn

danger(args) {
    val allSourceFiles = git.modifiedFiles + git.createdFiles
    val isChangelogUpdated = allSourceFiles.contains("CHANGELOG.adoc")

    onGitHub {
        val branchName = pullRequest.head.label.substringAfter(":")
        val isFeatureBranch =
            "(?:feature\\/(?:[A-Z]{2,8}-\\d{1,6}\\/)?(?:add|change|remove|fix|bump|security)-[a-z,0-9,-]*)"
                .toRegex()
                .matches(branchName)
        val isReleaseBranch =
            "(?:release\\/(?:\\d{1,3}\\.\\d{1,3}(?:\\.\\d{1,3})?)(?:\\/prepare-\\d{1,3}\\.\\d{1,3}\\.\\d{1,3})?)"
                .toRegex()
                .matches(branchName)
        val isFeatureTitle =
            "(?:(?:\\[[A-Z]{2,8}-\\d{1,6}\\]\\s)?(?:Add|Change|Remove|Fix|Bump|Security)\\s.*)"
                .toRegex()
                .matches(pullRequest.title)
        val isReleaseTitle = "(?:(?:Prepare )?Release \\d{1,3}\\.\\d{1,3}\\.\\d{1,3})"
            .toRegex()
            .matches(pullRequest.title)


        if (!isFeatureBranch && !isReleaseBranch) {
            fail(
                "Branch name is not following our pattern:\n" +
                    "\nrelease/1.2(.3)(/prepare-1.2.3)\n" +
                    "\nfeature/(SDK-123)/add|change|remove|fix|bump|security-feature-title\n" +
                    "\n\n" +
                    "\n Current name: $branchName"
            )
        }

        if (isFeatureBranch) {
            if (!isFeatureTitle) {
                fail(
                    "Title is not following our pattern:\n" +
                        "\n[ticket_id](optional) Add|Change|Remove|Fix|Bump|Security {Feature title}"
                )
            }
        }

        if (isReleaseBranch) {
            if (!isReleaseTitle) {
                fail(
                    "Title is not following our pattern: Prepare Release major.minor.patch (1.2.0)"
                )
            }
        }

        // General
        if (pullRequest.assignee == null) {
            warn("Please assign someone to merge this PR")
        }

        if (pullRequest.milestone == null) {
            warn("Set a milestone please")
        }

        if (pullRequest.body.length < 10) {
            warn("Please include a description of your PR changes")
        }

        // Changelog
        if (isChangelogUpdated) {
            warn("Changes should be reflected in the CHANGELOG.adoc")
        }

        // Size
        val changes = (pullRequest.additions ?: 0) - (pullRequest.deletions ?: 0)
        if (changes > 2000) {
            fail("This Pull-Request is way to big, please slice it into smaller pull-requests.")
        } else if (changes > 1000) {
            warn("Too Big Pull-Request, keep changes smaller")
        } else if (changes > 500) {
            warn("Large Pull-Request, try to keep changes smaller if you can")
        }
    }
}
