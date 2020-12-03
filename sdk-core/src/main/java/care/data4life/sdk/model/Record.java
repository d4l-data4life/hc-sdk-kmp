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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;

import care.data4life.fhir.stu3.model.DomainResource;
import care.data4life.sdk.model.definitions.FhirRecord;

public class Record<T extends DomainResource> implements FhirRecord<T> {
    private final String identifier = "";
    private final T resource;
    private final Meta meta;
    private List<String> annotations;

    public Record(T fhirResource, Meta meta) {
        this.resource = fhirResource;
        this.meta = meta;
    }

    public Record(T fhirResource, Meta meta, List<String> annotations) {
        this.resource = fhirResource;
        this.meta = meta;
        this.annotations = annotations;
    }

    @NotNull
    @Override
    public String getIdentifier() { return identifier; }

    @Nullable
    @Override
    public T getFhirResource() { return resource; }

    @Nullable
    @Override
    public T getResource() {
        return resource;
    }

    @Nullable
    @Override
    public Meta getMeta() {
        return meta;
    }

    @Nullable
    @Override
    public List<String> getAnnotations() { return annotations; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Record<?> record = (Record<?>) o;
        return identifier.equals(record.identifier) &&
                Objects.equals(resource, record.resource) &&
                Objects.equals(meta, record.meta) &&
                Objects.equals(annotations, record.annotations);
    }

    @Override
    public int hashCode() {
        return Objects.hash(identifier, resource, meta, annotations);
    }
}
