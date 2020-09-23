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

package care.data4life.sdk.model;

import java.util.List;

import care.data4life.sdk.lang.D4LException;
import kotlin.Pair;

public class DeleteResult {
    private List<String> successfulDeletes;
    private List<Pair<String, D4LException>> failedDeletes;

    public DeleteResult(List<String> successfulDeletes, List<Pair<String, D4LException>> failedDeletes) {
        this.successfulDeletes = successfulDeletes;
        this.failedDeletes = failedDeletes;
    }

    public List<String> getSuccessfulDeletes() {
        return successfulDeletes;
    }

    public List<Pair<String, D4LException>> getFailedDeletes() {
        return failedDeletes;
    }
}
