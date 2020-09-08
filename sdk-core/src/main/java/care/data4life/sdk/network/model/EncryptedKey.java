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

import java.util.Objects;

public class EncryptedKey {

    private String encryptedKeyBase64;


    public EncryptedKey(String encryptedKeyBase64) {
        this.encryptedKeyBase64 = encryptedKeyBase64;
    }

    public String getEncryptedKey() {
        return encryptedKeyBase64;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EncryptedKey that = (EncryptedKey) o;
        return Objects.equals(encryptedKeyBase64, that.encryptedKeyBase64);
    }

    @Override
    public int hashCode() {
        return Objects.hash(encryptedKeyBase64);
    }
}
