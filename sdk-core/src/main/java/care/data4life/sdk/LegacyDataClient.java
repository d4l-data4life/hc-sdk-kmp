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

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import care.data4life.fhir.stu3.model.Attachment;
import care.data4life.fhir.stu3.model.DomainResource;
import care.data4life.sdk.auth.UserService;
import care.data4life.sdk.call.CallHandler;
import care.data4life.sdk.call.Task;
import care.data4life.sdk.listener.Callback;
import care.data4life.sdk.listener.ResultListener;
import care.data4life.sdk.model.CreateResult;
import care.data4life.sdk.model.DeleteResult;
import care.data4life.sdk.model.DownloadResult;
import care.data4life.sdk.model.DownloadType;
import care.data4life.sdk.model.FetchResult;
import care.data4life.sdk.model.Record;
import care.data4life.sdk.model.UpdateResult;
import io.reactivex.Completable;
import io.reactivex.Single;

/**
 * Deprecated with version v1.9.0
 * <p>
 * Will be removed in version v2.0.0
 */
@Deprecated
public class LegacyDataClient implements SdkContract.LegacyDataClient {

    protected CallHandler handler;
    public UserService userService;
    protected RecordService recordService;


    public LegacyDataClient(
            UserService userService,
            RecordService recordService,
            CallHandler handler
    ) {
        this.userService = userService;
        this.recordService = recordService;
        this.handler = handler;
    }

    @Override
    public <T extends DomainResource> void createRecord(
            T resource,
            ResultListener<Record<T>> listener
    ) {
        Single<Record<T>> operation = userService.finishLogin(true)
                .flatMap(ignore -> userService.getUserID())
                .flatMap(uid -> recordService.createRecord(uid, resource, new ArrayList()));
        handler.executeSingle(operation, listener);
    }

    @Override
    public <T extends DomainResource> void createRecord(
            T resource,
            ResultListener<Record<T>> listener,
            List<String> annotations
    ) {
        Single<Record<T>> operation = userService.finishLogin(true)
                .flatMap(ignore -> userService.getUserID())
                .flatMap(uid -> recordService.createRecord(uid, resource, annotations));
        handler.executeSingle(operation, listener);
    }

    @Override
    public <T extends DomainResource> void createRecords(List<T> resources, ResultListener<CreateResult<T>> listener) {
        Single<CreateResult<T>> operation = userService.finishLogin(true)
                .flatMap(ignore -> userService.getUserID())
                .flatMap(uid -> recordService.createRecords(resources, uid));
        handler.executeSingle(operation, listener);
    }

    @Override
    public <T extends DomainResource> void updateRecord(T resource, ResultListener<Record<T>> listener) {
        Single<Record<T>> operation = userService.finishLogin(true)
                .flatMap(ignore -> userService.getUserID())
                .flatMap(uid -> recordService.updateRecord(uid, resource.id, resource, new ArrayList()));
        handler.executeSingle(operation, listener);
    }

    @Override
    public <T extends DomainResource> void updateRecord(
            T resource,
            ResultListener<Record<T>> listener,
            List<String> annotations
    ) {
        Single<Record<T>> operation = userService.finishLogin(true)
                .flatMap(ignore -> userService.getUserID())
                .flatMap(uid -> recordService.updateRecord(uid, resource.id, resource, annotations));
        handler.executeSingle(operation, listener);
    }

    @Override
    public <T extends DomainResource> void updateRecords(List<T> resources, ResultListener<UpdateResult<T>> listener) {
        Single<UpdateResult<T>> operation = userService.finishLogin(true)
                .flatMap(ignore -> userService.getUserID())
                .flatMap(uid -> recordService.updateRecords(resources, uid));
        handler.executeSingle(operation, listener);
    }

    @Override
    public void deleteRecord(String recordId, Callback listener) {
        Completable operation = userService.getUserID()
                .flatMapCompletable(uid -> recordService.deleteRecord(uid, recordId));
        handler.executeCompletable(operation, listener);
    }

    @Override
    public void deleteRecords(List<String> recordIds, ResultListener<DeleteResult> listener) {
        Single<DeleteResult> operation = userService.getUserID()
                .flatMap(uid -> recordService.deleteRecords(recordIds, uid));
        handler.executeSingle(operation, listener);
    }

    @Override
    public Task countRecords(@Nullable Class<? extends DomainResource> clazz, ResultListener<Integer> listener) {
        Single<Integer> operation = userService.getUserID()
                .flatMap(uid -> recordService.countRecords(clazz, uid));
        return handler.executeSingle(operation, listener);
    }

    @Override
    public Task countRecords(
            @Nullable Class<? extends DomainResource> clazz,
            ResultListener<Integer> listener,
            List<String> annotations
    ) {
        Single<Integer> operation = userService.getUserID()
                .flatMap(uid -> recordService.countRecords(clazz, uid, annotations));
        return handler.executeSingle(operation, listener);
    }

    @Override
    public <T extends DomainResource> Task fetchRecord(String recordId, ResultListener<Record<T>> listener) {
        Single<Record<T>> operation = userService.getUserID()
                .flatMap(uid -> recordService.fetchFhir3Record(uid, recordId));
        return handler.executeSingle(operation, listener);
    }

    @Override
    public <T extends DomainResource> Task fetchRecords(List<String> recordIds, ResultListener<FetchResult<T>> listener) {
        Single<FetchResult<T>> operation = userService.getUserID()
                .flatMap(uid -> recordService.fetchFhir3Records(recordIds, uid));
        return handler.executeSingle(operation, listener);
    }

    @Override
    public <T extends DomainResource> Task fetchRecords(Class<T> resourceType, @Nullable LocalDate startDate, @Nullable LocalDate endDate, Integer pageSize, Integer offset, ResultListener<List<Record<T>>> listener) {
        Single<List<Record<T>>> operation = userService.getUserID()
                .flatMap(uid -> recordService.fetchFhir3Records(uid, resourceType, startDate, endDate, pageSize, offset));
        return handler.executeSingle(operation, listener);
    }

    @Override
    public <T extends DomainResource> Task fetchRecords(
            Class<T> resourceType,
            List<String> annotations,
            @Nullable LocalDate startDate,
            @Nullable LocalDate endDate,
            Integer pageSize,
            Integer offset,
            ResultListener<List<Record<T>>> listener
    ) {
        Single<List<Record<T>>> operation = userService.getUserID()
                .flatMap(uid -> recordService.fetchFhir3Records(
                        uid,
                        resourceType,
                        annotations,
                        startDate,
                        endDate,
                        pageSize,
                        offset,
                        null
                ));
        return handler.executeSingle(operation, listener);
    }

    @Override
    public <T extends DomainResource> Task downloadRecord(String recordId, ResultListener<Record<T>> listener) {
        Single<Record<T>> operation = userService.getUserID()
                .flatMap(uid -> recordService.downloadFhir3Record(recordId, uid));
        return handler.executeSingle(operation, listener);
    }

    @Override
    public <T extends DomainResource> Task downloadRecords(List<String> recordIds, ResultListener<DownloadResult<T>> listener) {
        Single<DownloadResult<T>> operation = userService.getUserID()
                .flatMap(uid -> recordService.downloadRecords(recordIds, uid));
        return handler.executeSingle(operation, listener);
    }

    @Override
    public Task downloadAttachment(String recordId, String attachmentId, DownloadType type, ResultListener<Attachment> listener) {
        Single<Attachment> operation = userService.getUserID()
                .flatMap(uid -> recordService.downloadFhir3Attachment(recordId, attachmentId, uid, type));
        return handler.executeSingle(operation, listener);
    }

    @Override
    public Task downloadAttachments(String recordId, List<String> attachmentIds, DownloadType type, ResultListener<List<Attachment>> listener) {
        Single<List<Attachment>> operation = userService.getUserID()
                .flatMap(uid -> recordService.downloadFhir3Attachments(recordId, attachmentIds, uid, type));
        return handler.executeSingle(operation, listener);
    }

    private void deleteAttachment(String attachmentId, ResultListener<Boolean> listener) {
        Single<Boolean> operation = userService.getUserID()
                .flatMap(uid -> recordService.deleteAttachment(attachmentId, uid));
        handler.executeSingle(operation, listener);
    }
}
