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

package care.data4life.sdk.network.model;

import com.squareup.moshi.Json;

import java.util.List;

import static care.data4life.sdk.network.model.Version.KEY_UNSUPPORTED;

public class VersionInfo {
    @Json(name = "versions")
    private List<Version> versionsList;

    public VersionInfo(List<Version> versionsList) {
        this.versionsList = versionsList;
    }

    public Boolean isSupported(int currentVersion) {
        boolean isSupported = true;
        for (Version version : versionsList) {
            if (version.getVersionCode() == currentVersion && version.getStatus().equals(KEY_UNSUPPORTED)) {
                isSupported = false;
                break;
            }

        }
        return isSupported;
    }
}
