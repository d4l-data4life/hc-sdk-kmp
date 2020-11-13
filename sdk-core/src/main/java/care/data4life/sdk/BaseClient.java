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
import care.data4life.sdk.lang.TaskException;
import care.data4life.sdk.listener.Callback;
import care.data4life.sdk.listener.ResultListener;
import care.data4life.sdk.log.Log;
import care.data4life.sdk.log.Logger;
import care.data4life.sdk.model.CreateResult;
import care.data4life.sdk.model.DeleteResult;
import care.data4life.sdk.model.DownloadResult;
import care.data4life.sdk.model.DownloadType;
import care.data4life.sdk.model.FetchResult;
import care.data4life.sdk.model.Record;
import care.data4life.sdk.model.UpdateResult;
import care.data4life.sdk.network.model.DecryptedRecord;
import care.data4life.sdk.network.model.EncryptedRecord;
import io.reactivex.Completable;
import io.reactivex.Single;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

abstract class BaseClient implements SdkContract.Client {
    protected String alias;
    protected UserService userService;
    protected RecordService recordService;
    protected SdkContract.ErrorHandler errorHandler;

    protected static final String CLIENT_ID_SPLIT_CHAR = "#";
    protected static final int PARTNER_ID_INDEX = 0;

    BaseClient(
            String alias,
            UserService userService,
            RecordService recordService,
            SdkContract.ErrorHandler errorHandler) {
        this.alias = alias;
        this.userService = userService;
        this.recordService = recordService;
        this.errorHandler = errorHandler;
    }

    public static void setLogger(Logger logger) {
        Log.setLogger(logger);
    }

    @Override
    public void getUserSessionToken(ResultListener<String> listener) {
        Single<String> operation = userService.getSessionToken(alias);
        executeSingle(operation, listener);
    }

    @Override
    public void isUserLoggedIn(ResultListener<Boolean> listener) {
        Single<Boolean> operation = userService.isLoggedIn(alias);
        executeSingle(operation, listener);
    }

    @Override
    public void logout(Callback listener) {
        executeCompletable(userService.logout(), listener);
    }

    @Override
    public <T extends DomainResource> void createRecord(T resource, ResultListener<Record<T>> listener) {
        Single<Record<T>> operation = userService.finishLogin(true)
                .flatMap(ignore -> userService.getUID())
                .flatMap(uid -> recordService.createRecord(resource, uid));
        executeSingle(operation, listener);
    }

    @Override
    public <T extends DomainResource> void createRecords(List<T> resources, ResultListener<CreateResult<T>> listener) {
        Single<CreateResult<T>> operation = userService.finishLogin(true)
                .flatMap(ignore -> userService.getUID())
                .flatMap(uid -> recordService.createRecords(resources, uid));
        executeSingle(operation, listener);
    }

    @Override
    public <T extends DomainResource> void updateRecord(T resource, ResultListener<Record<T>> listener) {
        Single<Record<T>> operation = userService.finishLogin(true)
                .flatMap(ignore -> userService.getUID())
                .flatMap(uid -> recordService.updateRecord(resource, uid));
        executeSingle(operation, listener);
    }

    @Override
    public <T extends DomainResource> void updateRecords(List<T> resources, ResultListener<UpdateResult<T>> listener) {
        Single<UpdateResult<T>> operation = userService.finishLogin(true)
                .flatMap(ignore -> userService.getUID())
                .flatMap(uid -> recordService.updateRecords(resources, uid));
        executeSingle(operation, listener);
    }

    @Override
    public void deleteRecord(String recordId, Callback listener) {
        Completable operation = userService.getUID()
                .flatMapCompletable(uid -> recordService.deleteRecord(recordId, uid));
        executeCompletable(operation, listener);
    }

    @Override
    public void deleteRecords(List<String> recordIds, ResultListener<DeleteResult> listener) {
        Single<DeleteResult> operation = userService.getUID()
                .flatMap(uid -> recordService.deleteRecords(recordIds, uid));
        executeSingle(operation, listener);
    }

    @Override
    public Task countRecords(@Nullable Class<? extends DomainResource> clazz, ResultListener<Integer> listener) {
        Single<Integer> operation = userService.getUID()
                .flatMap(uid -> recordService.countRecords(clazz, uid));
        return executeSingle(operation, listener);
    }

    @Override
    public <T extends DomainResource> Task fetchRecord(String recordId, ResultListener<Record<T>> listener) {
        Single<Record<T>> operation = userService.getUID()
                .flatMap(uid -> recordService.fetchRecord(recordId, uid));
        return executeSingle(operation, listener);
    }

    @Override
    public <T extends DomainResource> Task fetchRecords(List<String> recordIds, ResultListener<FetchResult<T>> listener) {
        Single<FetchResult<T>> operation = userService.getUID()
                .flatMap(uid -> recordService.fetchRecords(recordIds, uid));
        return executeSingle(operation, listener);
    }

    @Override
    public <T extends DomainResource> Task fetchRecords(Class<T> resourceType, @Nullable LocalDate startDate, @Nullable LocalDate endDate, Integer pageSize, Integer offset, ResultListener<List<Record<T>>> listener) {
        Single<List<Record<T>>> operation = userService.getUID()
                .flatMap(uid -> recordService.fetchRecords(uid, resourceType, startDate, endDate, pageSize, offset));
        return executeSingle(operation, listener);
    }

    @Override
    public <T extends DomainResource> Task downloadRecord(String recordId, ResultListener<Record<T>> listener) {
        Single<Record<T>> operation = userService.getUID()
                .flatMap(uid -> recordService.downloadRecord(recordId, uid));
        return executeSingle(operation, listener);
    }

    @Override
    public <T extends DomainResource> Task downloadRecords(List<String> recordIds, ResultListener<DownloadResult<T>> listener) {
        Single<DownloadResult<T>> operation = userService.getUID()
                .flatMap(uid -> recordService.downloadRecords(recordIds, uid));
        return executeSingle(operation, listener);
    }

    @Override
    public Task downloadAttachment(String recordId, String attachmentId, DownloadType type, ResultListener<Attachment> listener) {
        Single<Attachment> operation = userService.getUID()
                .flatMap(uid -> recordService.downloadAttachment(recordId, attachmentId, uid, type));
        return executeSingle(operation, listener);
    }

    @Override
    public Task downloadAttachments(String recordId, List<String> attachmentIds, DownloadType type, ResultListener<List<Attachment>> listener) {
        Single<List<Attachment>> operation = userService.getUID()
                .flatMap(uid -> recordService.downloadAttachments(recordId, attachmentIds, uid, type));
        return executeSingle(operation, listener);
    }

    @Override
    public void createAppData(byte[] appData, List<String> annotations, ResultListener<AppDataRecord> resultListener) {
        Single<AppDataRecord> operation = userService.finishLogin(true)
                .flatMap(ignore -> userService.getUID())
                .flatMap(uid -> recordService.createAppDataRecord(appData, annotations, uid));
        executeSingle(operation, resultListener);
    }

    @Override
    public Task fetchAppData(String appDataId, ResultListener<AppDataRecord> resultListener) {
        Single<AppDataRecord> operation = userService.getUID()
                .flatMap(uid -> recordService.fetchAppDataRecord(appDataId, uid));
        return executeSingle(operation, resultListener);
    }

    @Override
    public Task fetchAppData(List<String> annotations, @Nullable LocalDate startDate, @Nullable LocalDate endDate, Integer pageSize, Integer offset, ResultListener<List<AppDataRecord>> listener) {
        Single<List<AppDataRecord>> operation = userService.getUID()
                .flatMap(uid -> recordService.fetchAppDataRecords(uid, startDate, endDate, pageSize, offset));
        return executeSingle(operation, listener);
    }

    @Override
    public void updateAppData(byte[] appData, String recordId, ResultListener<AppDataRecord> resultListener) {
        Single<AppDataRecord> operation = userService.finishLogin(true)
                .flatMap(ignore -> userService.getUID())
                .flatMap(uid -> recordService.updateAppDataRecord(appData, recordId, uid));
        executeSingle(operation, resultListener);
    }

    @Override
    public void deleteAppData(String appDataId, Callback callback) {
        deleteRecord(appDataId, callback);
    }

    private void deleteAttachment(String attachmentId, ResultListener<Boolean> listener) {
        Single<Boolean> operation = userService.getUID()
                .flatMap(uid -> recordService.deleteAttachment(attachmentId, uid));
        executeSingle(operation, listener);
    }

    private <T> Task executeSingle(Single<T> operation, ResultListener<T> listener) {
        final Task task = new Task(null);

        Disposable operationHandle =
                operation
                        .doOnDispose(() -> {
                            if (task.isCanceled())
                                listener.onError(errorHandler.handleError(new TaskException.CancelException()));
                        })
                        .doFinally(task::finish)
                        .subscribeOn(Schedulers.io())
                        .subscribe(
                                listener::onSuccess,
                                error -> {
                                    if (!task.isActive()) return;
                                    listener.onError(errorHandler.handleError(error));
                                }
                        );

        task.setOperationHandle(operationHandle);
        return task;
    }

    private Task executeCompletable(Completable operation, Callback listener) {
        final Task task = new Task(null);

        Disposable operationHandle =
                operation
                        .doOnDispose(() -> {
                            if (task.isCanceled())
                                listener.onError(errorHandler.handleError(new TaskException.CancelException()));
                        })
                        .doFinally(task::finish)
                        .subscribeOn(Schedulers.io())
                        .subscribe(
                                listener::onSuccess,
                                error -> {
                                    if (!task.isActive()) return;
                                    listener.onError(errorHandler.handleError(error));
                                }
                        );

        task.setOperationHandle(operationHandle);
        return task;
    }
}
