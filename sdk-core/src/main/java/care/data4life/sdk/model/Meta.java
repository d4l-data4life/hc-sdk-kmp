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

import org.threeten.bp.LocalDate;
import org.threeten.bp.LocalDateTime;

import java.io.Serializable;

import care.data4life.fhir.stu3.model.DomainResource;

public class Meta implements Serializable {
    private LocalDate createdDate;
    private LocalDateTime updatedDate;

    /**
     * Meta is container for server side related informations about records.
     *
     * @param createdDate is date when resource was created and is set by the SDK in the moment when resource enters {@link care.data4life.sdk.RecordService#createRecord(DomainResource, String)} method
     * @param updatedDate is date when record was last time updated on the server side.
     *                    Creating new record is also considered as update operation and will result in
     *                    updating {@code updatedDate}.
     */
    public Meta(LocalDate createdDate, LocalDateTime updatedDate) {
        this.createdDate = createdDate;
        this.updatedDate = updatedDate;
    }

    public LocalDate getCreatedDate() {
        return createdDate;
    }

    public LocalDateTime getUpdatedDate() {
        return updatedDate;
    }
}
