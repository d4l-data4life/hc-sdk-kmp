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

public class UserInfo {

    private static final String DEFAULT_COMMON_KEY_ID = "00000000-0000-0000-0000-000000000000";

    @Json(name = "sub")
    private String uid;
    @Json(name = "common_key")
    private EncryptedKey commonKey;
    @Json(name = "common_key_id")
    private String commonKeyId;
    @Json(name = "tag_encryption_key")
    private EncryptedKey tagEncryptionKey;

    public String getUid() {
        return uid;
    }

    public EncryptedKey getCommonKey() {
        return commonKey;
    }

    public String getCommonKeyId() {
        if (commonKeyId == null || commonKeyId.isEmpty()) {
            commonKeyId = DEFAULT_COMMON_KEY_ID;
        }
        return commonKeyId;
    }

    public EncryptedKey getTagEncryptionKey() {
        return tagEncryptionKey;
    }

}
