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

package care.data4life.sdk;

import org.threeten.bp.LocalDate;

import java.util.List;

import javax.annotation.Nullable;

import care.data4life.fhir.stu3.model.Attachment;
import care.data4life.fhir.stu3.model.DomainResource;
import care.data4life.sdk.call.Task;
import care.data4life.sdk.lang.D4LException;
import care.data4life.sdk.listener.Callback;
import care.data4life.sdk.listener.ResultListener;
import care.data4life.sdk.model.CreateResult;
import care.data4life.sdk.model.DeleteResult;
import care.data4life.sdk.model.DownloadResult;
import care.data4life.sdk.model.DownloadType;
import care.data4life.sdk.model.FetchResult;
import care.data4life.sdk.model.Record;
import care.data4life.sdk.model.UpdateResult;

/**
 * Deprecated with version v1.9.0
 * <p>
 * Will be removed in version v2.0.0
 */
@Deprecated
public interface SdkContractLegacy {

    /**
     * Deprecated with version v1.9.0
     * <p>
     * Will be removed in version v2.0.0
     */
    @Deprecated
    interface DataClient {

        /**
         * Creates a record.
         *
         * @param resource the resource that shall be created
         * @param listener result contains either record or Error
         * @param <T>      the type of the created {@link Record} as a subclass of {@link DomainResource}
         * @throws D4LException if {@param resource} is DocumentReference and {@link Attachment#data} is greater than 10MB or is not of type: JPEG, PNG, TIFF, PDF or DCM
         */
        <T extends DomainResource> void createRecord(T resource, ResultListener<Record<T>> listener);

        /**
         * Creates a record.
         *
         * @param resource    the resource that shall be created
         * @param listener    result contains either record or Error
         * @param annotations custom annotations added as tags to the record
         * @param <T>         the type of the created {@link Record} as a subclass of {@link DomainResource}
         * @throws D4LException if {@param resource} is DocumentReference and {@link care.data4life.fhir.stu3.model.Attachment#data} is greater than 10MB or is not of type: JPEG, PNG, TIFF, PDF or DCM
         */
        <T extends DomainResource> void createRecord(T resource, ResultListener<Record<T>> listener, List<String> annotations);

        /**
         * Create a list of records
         *
         * @param resources The resources that shall be created
         * @param listener  result contains the list of created records or errors
         * @param <T>       the type of the records
         */
        <T extends DomainResource> void createRecords(List<T> resources, ResultListener<CreateResult<T>> listener);

        /**
         * Delete a record
         *
         * @param recordId the id of the record that shall be deleted
         * @param listener either {@link Callback#onSuccess()} or {@link Callback#onError(D4LException)} will be called
         */
        void deleteRecord(String recordId, Callback listener);

        /**
         * Delete a list of records
         *
         * @param recordIds the ids of the records that shall be deleted
         * @param listener  either {@link ResultListener#onSuccess(Object)} or {@link ResultListener#onError(D4LException)} will be called
         */
        void deleteRecords(List<String> recordIds, ResultListener<DeleteResult> listener);

        /**
         * Get the count of stored records per record type
         *
         * @param clazz    the type of the record - it can be null(than all records count would be returned)
         * @param listener either {@link ResultListener#onSuccess(Object)} or {@link ResultListener#onError(D4LException)} will be called
         * @return {@link Task} which can be used to cancel ongoing operation or to query operation status
         */
        Task countRecords(@Nullable Class<? extends DomainResource> clazz, ResultListener<Integer> listener);

        /**
         * Get the count of stored records per record type
         *
         * @param clazz       the type of the record - it can be null(than all records count would be returned)
         * @param listener    either {@link ResultListener#onSuccess(Object)} or {@link ResultListener#onError(D4LException)} will be called
         * @param annotations custom annotations added as tags to the record
         * @return {@link Task} which can be used to cancel ongoing operation or to query operation status
         */
        Task countRecords(@Nullable Class<? extends DomainResource> clazz, ResultListener<Integer> listener, List<String> annotations);

        /**
         * Fetch a record
         *
         * @param recordId the id of the record which shall be fetched
         * @param listener either {@link ResultListener#onSuccess(Object)} or {@link ResultListener#onError(D4LException)} will be called
         * @param <T>      the type of {@link Record} as a subclass of {@link DomainResource}
         * @return {@link Task} which can be used to cancel ongoing operation or to query operation status
         */
        <T extends DomainResource> Task fetchRecord(String recordId, ResultListener<Record<T>> listener);

        /**
         * Fetch records
         *
         * @param recordIds the ids of records which shall be fetched
         * @param listener  either {@link ResultListener#onSuccess(Object)} or {@link ResultListener#onError(D4LException)} will be called
         * @param <T>       the type of {@link Record} as a subclass of {@link DomainResource}
         * @return {@link Task} which can be used to cancel ongoing operation or to query operation status
         */
        <T extends DomainResource> Task fetchRecords(List<String> recordIds, ResultListener<FetchResult<T>> listener);

        /**
         * Fetch records with filters
         *
         * @param resourceType The class type of the record to fetch
         * @param creationDateRange the filtered records have a creation date after the start date or before the endDate
         * @param updateDateTimeRange the filtered records have a update dateTime after the start date or before the endDateTime
         * @param includeDeletedRecords includes deleted records into the query
         * @param pageSize     define the size page result
         * @param offset       the offset of the records list
         * @param listener     either {@link ResultListener#onSuccess(Object)} or {@link ResultListener#onError(D4LException)} will be called
         * @param <T>          the type of {@link Record} as a subclass of {@link DomainResource}
         * @return {@link Task} which can be used to cancel ongoing operation or to query operation status
         */
        <T extends DomainResource> Task fetchRecords(
                Class<T> resourceType,
                @Nullable SdkContract.CreationDateRange creationDateRange,
                @Nullable SdkContract.UpdateDateTimeRange updateDateTimeRange,
                Boolean includeDeletedRecords,
                Integer pageSize,
                Integer offset,
                ResultListener<List<Record<T>>> listener
        );

        /**
         * Fetch records with filters
         *
         * @param resourceType The class type of the record to fetch
         * @param annotations  custom annotations added as tags to the record
         * @param creationDateRange the filtered records have a creation date after the start date or before the endDate
         * @param updateDateTimeRange the filtered records have a update dateTime after the start date or before the endDateTime
         * @param includeDeletedRecords includes deleted records into the query
         * @param pageSize     define the size page result
         * @param offset       the offset of the records list
         * @param listener     either {@link ResultListener#onSuccess(Object)} or {@link ResultListener#onError(D4LException)} will be called
         * @param <T>          the type of {@link Record} as a subclass of {@link DomainResource}
         * @return {@link Task} which can be used to cancel ongoing operation or to query operation status
         */
        <T extends DomainResource> Task fetchRecords(
                Class<T> resourceType,
                List<String> annotations,
                @Nullable SdkContract.CreationDateRange creationDateRange,
                @Nullable SdkContract.UpdateDateTimeRange updateDateTimeRange,
                Boolean includeDeletedRecords,
                Integer pageSize,
                Integer offset,
                ResultListener<List<Record<T>>> listener
        );

        /**
         * Download a record. All {@link Attachment}s will get downloaded of the requested record.
         *
         * @param recordId the id of the record that shall be downloaded
         * @param listener either {@link ResultListener#onSuccess(Object)} or {@link ResultListener#onError(D4LException)} will be called
         * @param <T>      the type of {@link Record} as a subclass of {@link DomainResource}
         * @return {@link Task} which can be used to cancel ongoing operation or to query operation status
         * @throws care.data4life.sdk.lang.DataValidationException if {@param resource} is DocumentReference and {@link Attachment#data} is greater than 10MB or is not of type: JPEG, PNG, TIFF, PDF or DCM
         * @throws IllegalArgumentException if {@param recordId} is not FHIR3
         */
        <T extends DomainResource> Task downloadRecord(String recordId, ResultListener<Record<T>> listener);

        /**
         * Download a list of records. All {@link Attachment}s will get downloaded of the requested records
         *
         * @param recordIds the ids of the records that shall be downloaded
         * @param listener  either {@link ResultListener#onSuccess(Object)} or {@link ResultListener#onError(D4LException)} will be called
         * @param <T>       the type of {@link Record} as a subclass of {@link DomainResource}
         * @return {@link Task} which can be used to cancel ongoing operation or to query operation status.
         */
        <T extends DomainResource> Task downloadRecords(List<String> recordIds, ResultListener<DownloadResult<T>> listener);

        /**
         * Update a record
         *
         * @param resource the updated resource that shall be uploaded
         * @param listener either {@link ResultListener#onSuccess(Object)} or {@link ResultListener#onError(D4LException)} will be called
         * @param <T>      the type of {@link Record} as a subclass of {@link DomainResource}
         * @throws care.data4life.sdk.lang.DataValidationException if {@param resource} is DocumentReference and {@link Attachment#data} is greater than 10MB or is not of type: JPEG, PNG, TIFF, PDF or DCM
         */
        <T extends DomainResource> void updateRecord(T resource, ResultListener<Record<T>> listener);

        /**
         * Update a record
         *
         * @param resource    the updated resource that shall be uploaded
         * @param listener    either {@link ResultListener#onSuccess(Object)} or {@link ResultListener#onError(D4LException)} will be called
         * @param annotations custom annotations added as tags to the record
         * @param <T>         the type of {@link Record} as a subclass of {@link DomainResource}
         * @throws care.data4life.sdk.lang.DataValidationException if {@param resource} is DocumentReference and {@link Attachment#data} is greater than 10MB or is not of type: JPEG, PNG, TIFF, PDF or DCM
         */
        <T extends DomainResource> void updateRecord(T resource, ResultListener<Record<T>> listener, List<String> annotations);

        /**
         * Update a list of records
         *
         * @param resources the resources that shall be updated
         * @param listener  either {@link ResultListener#onSuccess(Object)} or {@link ResultListener#onError(D4LException)} will be called
         * @param <T>       the type of {@link Record} as a subclass of {@link DomainResource}
         */
        <T extends DomainResource> void updateRecords(List<T> resources, ResultListener<UpdateResult<T>> listener);

        /**
         * Download a specific attachment from a record.
         *
         * @param recordId     the id of the record the attachment belongs to
         * @param attachmentId the id of the attachment that shall be downloaded
         * @param type         the size of attachment that shall be downloaded(Full, Medium, Small).
         *                     This property has effect only for image type attachments.
         * @param listener     either {@link ResultListener#onSuccess(Object)} or {@link ResultListener#onError(D4LException)} will be called
         * @return {@link Task} which can be used to cancel ongoing operation or to query operation status.
         */
        Task downloadAttachment(String recordId, String attachmentId, DownloadType type, ResultListener<Attachment> listener);

        /**
         * Download a list of attachments from a record.
         *
         * @param recordId      the id of the record the attachments belong to
         * @param attachmentIds the list of attachment ids that shall be downloaded
         * @param type          the size of attachment that shall be downloaded(Full, Medium, Small).
         *                      This property has effect only for image type attachments.
         * @param listener      either {@link ResultListener#onSuccess(Object)} or {@link ResultListener#onError(D4LException)} will be called
         * @return {@link Task} which can be used to cancel ongoing operation or to query operation status.
         */
        Task downloadAttachments(String recordId, List<String> attachmentIds, DownloadType type, ResultListener<List<Attachment>> listener);
    }
}
