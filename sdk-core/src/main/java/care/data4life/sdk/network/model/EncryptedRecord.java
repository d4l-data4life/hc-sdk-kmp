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
import java.util.Objects;

public class EncryptedRecord {

    private static final String DEFAULT_COMMON_KEY_ID = "00000000-0000-0000-0000-000000000000";


    @Json(name = "common_key_id")
    private String commonKeyId;
    @Json(name = "record_id")
    private String identifier;
    @Json(name = "encrypted_tags")
    private List<String> encryptedTags;
    @Json(name = "encrypted_body")
    private String encryptedBody;
    @Json(name = "createdAt")
    private String updatedDate;
    @Json(name = "date")
    private String customCreationDate;
    private int version;
    @Json(name = "encrypted_key")
    private EncryptedKey encryptedDataKey;
    @Json(name = "attachment_key")
    private EncryptedKey encryptedAttachmentsKey;
    @Json(name = "model_version")
    private int modelVersion;


    public EncryptedRecord(String commonKeyId,
                           String identifier,
                           List<String> encryptedTags,
                           String encryptedBody,
                           String customCreationDate,
                           EncryptedKey encryptedDataKey,
                           EncryptedKey encryptedAttachmentsKey,
                           int modelVersion) {
        this.commonKeyId = commonKeyId;
        this.identifier = identifier;
        this.encryptedTags = encryptedTags;
        this.encryptedBody = encryptedBody;
        this.customCreationDate = customCreationDate;
        this.encryptedDataKey = encryptedDataKey;
        this.encryptedAttachmentsKey = encryptedAttachmentsKey;
        this.modelVersion = modelVersion;
    }

    public String getCommonKeyId() {
        if (commonKeyId == null) {
            commonKeyId = DEFAULT_COMMON_KEY_ID;
        }
        return commonKeyId;
    }

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public List<String> getEncryptedTags() {
        return encryptedTags;
    }

    public void setEncryptedTags(List<String> encryptedTags) {
        this.encryptedTags = encryptedTags;
    }

    public String getEncryptedBody() {
        return encryptedBody;
    }

    public void setEncryptedBody(String encryptedBody) {
        this.encryptedBody = encryptedBody;
    }

    public String getUpdatedDate() {
        return updatedDate;
    }

    public void setUpdatedDate(String updatedDate) {
        this.updatedDate = updatedDate;
    }

    public String getCustomCreationDate() {
        return customCreationDate;
    }

    public void setCustomCreationDate(String customCreationDate) {
        this.customCreationDate = customCreationDate;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public EncryptedKey getEncryptedDataKey() {
        return encryptedDataKey;
    }

    public EncryptedKey getEncryptedAttachmentsKey() {
        return encryptedAttachmentsKey;
    }

    public int getModelVersion() {
        return modelVersion;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EncryptedRecord that = (EncryptedRecord) o;
        return version == that.version &&
                modelVersion == that.modelVersion &&
                Objects.equals(identifier, that.identifier) &&
                Objects.equals(encryptedTags, that.encryptedTags) &&
                Objects.equals(encryptedBody, that.encryptedBody) &&
                Objects.equals(updatedDate, that.updatedDate) &&
                Objects.equals(customCreationDate, that.customCreationDate) &&
                Objects.equals(encryptedDataKey, that.encryptedDataKey) &&
                Objects.equals(encryptedAttachmentsKey, that.encryptedAttachmentsKey);
    }

    @Override
    public int hashCode() {
        return Objects.hash(identifier, encryptedTags, encryptedBody, updatedDate, customCreationDate, version, encryptedDataKey, encryptedAttachmentsKey, modelVersion);
    }
}
