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

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

import care.data4life.crypto.GCKey;
import care.data4life.fhir.stu3.model.DomainResource;

public class DecryptedRecord<T extends DomainResource> implements Serializable {

    private String identifier;
    private T resource;
    private HashMap<String, String> tags;
    private List<String> annotations;


    private String customCreationDate;
    private String updatedDate;
    private GCKey dataKey;
    private GCKey attachmentsKey;
    private int modelVersion;


    public DecryptedRecord(String identifier, T resource, HashMap<String, String> tags, String customCreationDate, String updatedDate, GCKey dataKey, GCKey attachmentsKey, int modelVersion) {
        this(identifier, resource, tags, Collections.emptyList(), customCreationDate, updatedDate, dataKey, attachmentsKey, modelVersion);
    }

    public DecryptedRecord(String identifier, T resource, HashMap<String, String> tags, List<String> annotations, String customCreationDate, String updatedDate, GCKey dataKey, GCKey attachmentsKey, int modelVersion) {
        this.identifier = identifier;
        this.resource = resource;
        this.tags = tags;
        this.annotations = annotations;
        this.customCreationDate = customCreationDate;
        this.updatedDate = updatedDate;
        this.dataKey = dataKey;
        this.attachmentsKey = attachmentsKey;
        this.modelVersion = modelVersion;
    }

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public T getResource() {
        return resource;
    }

    public void setResource(T resource) {
        this.resource = resource;
    }

    public HashMap<String, String> getTags() {
        return tags;
    }

    public void setTags(HashMap<String, String> tags) {
        this.tags = tags;
    }

    public String getCustomCreationDate() {
        return customCreationDate;
    }

    public void setCustomCreationDate(String customCreationDate) {
        this.customCreationDate = customCreationDate;
    }

    public String getUpdatedDate() {
        return updatedDate;
    }

    public void setUpdatedDate(String updatedDate) {
        this.updatedDate = updatedDate;
    }

    public GCKey getDataKey() {
        return dataKey;
    }

    public void setDataKey(GCKey dataKey) {
        this.dataKey = dataKey;
    }

    public GCKey getAttachmentsKey() {
        return attachmentsKey;
    }

    public void setAttachmentsKey(GCKey attachmentsKey) {
        this.attachmentsKey = attachmentsKey;
    }

    public int getModelVersion() {
        return modelVersion;
    }

    public void setModelVersion(int modelVersion) {
        this.modelVersion = modelVersion;
    }

    public List<String> getAnnotations() {
        return annotations;
    }

    public void setAnnotations(List<String> annotations) {
        this.annotations = annotations;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DecryptedRecord<?> that = (DecryptedRecord<?>) o;
        return modelVersion == that.modelVersion &&
                Objects.equals(identifier, that.identifier) &&
                Objects.equals(resource, that.resource) &&
                Objects.equals(tags, that.tags) &&
                Objects.equals(customCreationDate, that.customCreationDate) &&
                Objects.equals(updatedDate, that.updatedDate) &&
                Objects.equals(dataKey, that.dataKey) &&
                Objects.equals(attachmentsKey, that.attachmentsKey);
    }

    @Override
    public int hashCode() {
        return Objects.hash(identifier, resource, tags, customCreationDate, updatedDate, dataKey, attachmentsKey, modelVersion);
    }
}
