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
import care.data4life.sdk.model.definitions.DataRecord;
import io.reactivex.Completable;
import io.reactivex.Single;

/**
 * Deprecated with version v1.9.0
 * <p>
 * Will be removed in version v2.0.0
 */
@Deprecated
class LegacyDataClient implements SdkContract.LegacyDataClient {

    protected CallHandler handler;
    protected String alias;
    public UserService userService;
    protected RecordService recordService;


    LegacyDataClient(
            String alias,
            UserService userService,
            RecordService recordService,
            CallHandler handler
    ) {
        this.alias = alias;
        this.userService = userService;
        this.recordService = recordService;
        this.handler = handler;
    }

    @Override
    public <T extends DomainResource> void createRecord(T resource, ResultListener<Record<T>> listener) {
        Single<Record<T>> operation = userService.finishLogin(true)
                .flatMap(ignore -> userService.getUID())
                .flatMap(uid -> recordService.createRecord(resource, uid));
        handler.executeSingle(operation, listener);
    }

    @Override
    public <T extends DomainResource> void createRecord(
            T resource,
            ResultListener<Record<T>> listener,
            List<String> annotations
    ) {
        Single<Record<T>> operation = userService.finishLogin(true)
                .flatMap(ignore -> userService.getUID())
                .flatMap(uid -> recordService.createRecord(resource, uid, annotations));
        handler.executeSingle(operation, listener);
    }

    @Override
    public <T extends DomainResource> void createRecords(List<T> resources, ResultListener<CreateResult<T>> listener) {
        Single<CreateResult<T>> operation = userService.finishLogin(true)
                .flatMap(ignore -> userService.getUID())
                .flatMap(uid -> recordService.createRecords(resources, uid));
        handler.executeSingle(operation, listener);
    }

    @Override
    public <T extends DomainResource> void updateRecord(T resource, ResultListener<Record<T>> listener) {
        Single<Record<T>> operation = userService.finishLogin(true)
                .flatMap(ignore -> userService.getUID())
                .flatMap(uid -> recordService.updateRecord(resource, uid));
        handler.executeSingle(operation, listener);
    }

    @Override
    public <T extends DomainResource> void updateRecord(
            T resource,
            ResultListener<Record<T>> listener,
            List<String> annotations
    ) {
        Single<Record<T>> operation = userService.finishLogin(true)
                .flatMap(ignore -> userService.getUID())
                .flatMap(uid -> recordService.updateRecord(resource, uid, annotations));
        handler.executeSingle(operation, listener);
    }

    @Override
    public <T extends DomainResource> void updateRecords(List<T> resources, ResultListener<UpdateResult<T>> listener) {
        Single<UpdateResult<T>> operation = userService.finishLogin(true)
                .flatMap(ignore -> userService.getUID())
                .flatMap(uid -> recordService.updateRecords(resources, uid));
        handler.executeSingle(operation, listener);
    }

    @Override
    public void deleteRecord(String recordId, Callback listener) {
        Completable operation = userService.getUID()
                .flatMapCompletable(uid -> recordService.deleteRecord(recordId, uid));
        handler.executeCompletable(operation, listener);
    }

    @Override
    public void deleteRecords(List<String> recordIds, ResultListener<DeleteResult> listener) {
        Single<DeleteResult> operation = userService.getUID()
                .flatMap(uid -> recordService.deleteRecords(recordIds, uid));
        handler.executeSingle(operation, listener);
    }

    @Override
    public Task countRecords(@Nullable Class<? extends DomainResource> clazz, ResultListener<Integer> listener) {
        Single<Integer> operation = userService.getUID()
                .flatMap(uid -> recordService.countRecords(clazz, uid));
        return handler.executeSingle(operation, listener);
    }

    @Override
    public Task countRecords(
            @Nullable Class<? extends DomainResource> clazz,
            ResultListener<Integer> listener,
            List<String> annotations
    ) {
        Single<Integer> operation = userService.getUID()
                .flatMap(uid -> recordService.countRecords(clazz, uid, annotations));
        return handler.executeSingle(operation, listener);
    }

    @Override
    public <T extends DomainResource> Task fetchRecord(String recordId, ResultListener<Record<T>> listener) {
        Single<Record<T>> operation = userService.getUID()
                .flatMap(uid -> recordService.fetchRecord(recordId, uid));
        return handler.executeSingle(operation, listener);
    }

    @Override
    public <T extends DomainResource> Task fetchRecords(List<String> recordIds, ResultListener<FetchResult<T>> listener) {
        Single<FetchResult<T>> operation = userService.getUID()
                .flatMap(uid -> recordService.fetchRecords(recordIds, uid));
        return handler.executeSingle(operation, listener);
    }

    @Override
    public <T extends DomainResource> Task fetchRecords(Class<T> resourceType, @Nullable LocalDate startDate, @Nullable LocalDate endDate, Integer pageSize, Integer offset, ResultListener<List<Record<T>>> listener) {
        Single<List<Record<T>>> operation = userService.getUID()
                .flatMap(uid -> recordService.fetchRecords(uid, resourceType, startDate, endDate, pageSize, offset));
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
        Single<List<Record<T>>> operation = userService.getUID()
                .flatMap(uid -> recordService.fetchRecords(
                        uid,
                        resourceType,
                        annotations,
                        startDate,
                        endDate,
                        pageSize,
                        offset
                ));
        return handler.executeSingle(operation, listener);
    }

    @Override
    public <T extends DomainResource> Task downloadRecord(String recordId, ResultListener<Record<T>> listener) {
        Single<Record<T>> operation = userService.getUID()
                .flatMap(uid -> recordService.downloadRecord(recordId, uid));
        return handler.executeSingle(operation, listener);
    }

    @Override
    public <T extends DomainResource> Task downloadRecords(List<String> recordIds, ResultListener<DownloadResult<T>> listener) {
        Single<DownloadResult<T>> operation = userService.getUID()
                .flatMap(uid -> recordService.downloadRecords(recordIds, uid));
        return handler.executeSingle(operation, listener);
    }

    @Override
    public Task downloadAttachment(String recordId, String attachmentId, DownloadType type, ResultListener<Attachment> listener) {
        Single<Attachment> operation = userService.getUID()
                .flatMap(uid -> recordService.downloadAttachment(recordId, attachmentId, uid, type));
        return handler.executeSingle(operation, listener);
    }

    @Override
    public Task downloadAttachments(String recordId, List<String> attachmentIds, DownloadType type, ResultListener<List<Attachment>> listener) {
        Single<List<Attachment>> operation = userService.getUID()
                .flatMap(uid -> recordService.downloadAttachments(recordId, attachmentIds, uid, type));
        return handler.executeSingle(operation, listener);
    }

    private void deleteAttachment(String attachmentId, ResultListener<Boolean> listener) {
        Single<Boolean> operation = userService.getUID()
                .flatMap(uid -> recordService.deleteAttachment(attachmentId, uid));
        handler.executeSingle(operation, listener);
    }

    @Override
    public void createDataRecord(byte[] data, ResultListener<DataRecord> resultListener, List<String> annotations) {
        Single<DataRecord> operation = userService.finishLogin(true)
                .flatMap(ignore -> userService.getUID())
                .flatMap(uid -> recordService.createRecord(data, uid, annotations));
        handler.executeSingle(operation, resultListener);
    }

    @Override
    public Task fetchDataRecord(String appDataId, ResultListener<DataRecord> resultListener) {
        Single<DataRecord> operation = userService.getUID()
                .flatMap(uid -> recordService.fetchAppDataRecord(appDataId, uid));
        return handler.executeSingle(operation, resultListener);
    }

    @Override
    public Task fetchDataRecords(
            List<String> annotations,
            @Nullable LocalDate startDate,
            @Nullable LocalDate endDate,
            Integer pageSize,
            Integer offset,
            ResultListener<List<DataRecord>> listener
    ) {
        Single<List<DataRecord>> operation = userService.getUID()
                .flatMap(uid -> recordService.fetchRecords(
                        uid,
                        annotations,
                        startDate,
                        endDate,
                        pageSize,
                        offset
                ));
        return handler.executeSingle(operation, listener);
    }

    @Override
    public void updateDataRecord(
            byte[] data,
            @Nullable List<String> annotations,
            String recordId,
            ResultListener<DataRecord> resultListener
    ) {
        Single<DataRecord> operation = userService.finishLogin(true)
                .flatMap(ignore -> userService.getUID())
                .flatMap(uid -> recordService.updateRecord(data, recordId, uid, annotations));
        handler.executeSingle(operation, resultListener);
    }

    @Override
    public void deleteDataRecord(String dataId, Callback callback) {
        deleteRecord(dataId, callback);
    }
}
