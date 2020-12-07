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
import org.threeten.bp.LocalDateTime;
import org.threeten.bp.ZoneId;
import org.threeten.bp.format.DateTimeFormatter;
import org.threeten.bp.format.DateTimeFormatterBuilder;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import javax.annotation.Nullable;

import care.data4life.crypto.GCKey;
import care.data4life.crypto.GCKeyPair;
import care.data4life.crypto.KeyType;
import care.data4life.fhir.stu3.model.Attachment;
import care.data4life.fhir.stu3.model.DomainResource;
import care.data4life.fhir.stu3.model.FhirElementFactory;
import care.data4life.fhir.stu3.model.Identifier;
import care.data4life.fhir.stu3.util.FhirAttachmentHelper;
import care.data4life.sdk.config.DataRestriction;
import care.data4life.sdk.config.DataRestrictionException;
import care.data4life.sdk.lang.CoreRuntimeException;
import care.data4life.sdk.lang.D4LException;
import care.data4life.sdk.lang.DataValidationException;
import care.data4life.sdk.model.AppDataRecord;
import care.data4life.sdk.model.CreateResult;
import care.data4life.sdk.model.DeleteResult;
import care.data4life.sdk.model.DownloadResult;
import care.data4life.sdk.model.DownloadType;
import care.data4life.sdk.model.EmptyRecord;
import care.data4life.sdk.model.FetchResult;
import care.data4life.sdk.model.Meta;
import care.data4life.sdk.model.ModelVersion;
import care.data4life.sdk.model.Record;
import care.data4life.sdk.model.UpdateResult;
import care.data4life.sdk.model.definitions.DataRecord;
import care.data4life.sdk.network.model.CommonKeyResponse;
import care.data4life.sdk.network.model.DecryptedRecord;
import care.data4life.sdk.network.model.DecryptedAppDataRecord;
import care.data4life.sdk.network.model.definitions.DecryptedBaseRecord;
import care.data4life.sdk.network.model.EncryptedKey;
import care.data4life.sdk.network.model.EncryptedRecord;
import care.data4life.sdk.model.EmptyRecord;
import care.data4life.sdk.network.model.definitions.DecryptedDataRecord;
import care.data4life.sdk.network.model.definitions.DecryptedFhirRecord;
import care.data4life.sdk.util.Base64;
import care.data4life.sdk.util.HashUtil;
import care.data4life.sdk.util.MimeType;
import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.Single;
import kotlin.Pair;

import static care.data4life.sdk.RecordService.RemoveRestoreOperation.REMOVE;
import static care.data4life.sdk.RecordService.RemoveRestoreOperation.RESTORE;
import static care.data4life.sdk.RecordService.UploadDownloadOperation.DOWNLOAD;
import static care.data4life.sdk.RecordService.UploadDownloadOperation.UPDATE;
import static care.data4life.sdk.RecordService.UploadDownloadOperation.UPLOAD;
import static care.data4life.sdk.TaggingService.TAG_RESOURCE_TYPE;

@SuppressWarnings("unchecked")
class RecordService {

    enum UploadDownloadOperation {
        UPLOAD,
        DOWNLOAD,
        UPDATE
    }

    enum RemoveRestoreOperation {
        REMOVE,
        RESTORE
    }

    private interface GetDecryptedResource {
        String run();
    }

    private interface GetEncryptedAttachment {
        @Nullable
        EncryptedKey run(GCKey commonKey);
    }

    private interface GetTags {
        HashMap<String, String> run();
    }

    private interface DecryptSource<T> {
        T run(
                HashMap<String, String>tags,
                List<String> annotations,
                GCKey dataKey,
                GCKey commonKey
        );
    }

    private static final String DATE_FORMAT = "yyyy-MM-dd";
    private static final String DATE_TIME_FORMAT = "yyyy-MM-dd'T'HH:mm:ss[.SSS]";
    private static final String EMPTY_RECORD_ID = "";
    static final String DOWNSCALED_ATTACHMENT_IDS_FMT = "d4l_f_p_t"; //d4l -> namespace, f-> full, p -> preview, t -> thumbnail
    static final String SPLIT_CHAR = "#";
    static final int DOWNSCALED_ATTACHMENT_IDS_SIZE = 4;
    static final int FULL_ATTACHMENT_ID_POS = 1;
    static final int PREVIEW_ID_POS = 2;
    static final int THUMBNAIL_ID_POS = 3;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern(DATE_FORMAT, Locale.US);
    private static final DateTimeFormatter DATE_TIME_FORMATTER = new DateTimeFormatterBuilder()
            .parseLenient()
            .appendPattern(DATE_TIME_FORMAT)
            .toFormatter(Locale.US);
    private static final ZoneId UTC_ZONE_ID = ZoneId.of("UTC");


    private String partnerId;
    private String alias;
    private ApiService apiService;
    private TagEncryptionService tagEncryptionService;
    private TaggingService taggingService;
    private FhirService fhirService;
    private AttachmentService attachmentService;
    private CryptoService cryptoService;
    private SdkContract.ErrorHandler errorHandler;

    RecordService(String partnerId,
                  String alias,
                  ApiService apiService,
                  TagEncryptionService tagEncryptionService,
                  TaggingService taggingService,
                  FhirService fhirService,
                  AttachmentService attachmentService,
                  CryptoService cryptoService,
                  SdkContract.ErrorHandler errorHandler) {
        this.partnerId = partnerId;
        this.alias = alias;
        this.apiService = apiService;
        this.tagEncryptionService = tagEncryptionService;
        this.taggingService = taggingService;
        this.fhirService = fhirService;
        this.attachmentService = attachmentService;
        this.cryptoService = cryptoService;
        this.errorHandler = errorHandler;
    }

    <T extends DomainResource> Single<Record<T>> createRecord(T resource, String userId)
            throws DataRestrictionException.UnsupportedFileType, DataRestrictionException.MaxDataSizeViolation {
        return createRecord(resource, userId, Collections.emptyList());
    }

    <T extends DomainResource> Single<Record<T>> createRecord(
            T resource,
            String userId,
            List<String> annotations
    ) throws DataRestrictionException.UnsupportedFileType, DataRestrictionException.MaxDataSizeViolation
    {
        checkDataRestrictions(resource);
        HashMap<Attachment, String> data = extractUploadData(resource);
        String createdDate = DATE_FORMATTER.format(LocalDate.now(UTC_ZONE_ID));
        Single<DecryptedFhirRecord<T> > createRecord = Single.just(createdDate)
                .map(createdAt -> {
                    HashMap<String, String> tags = taggingService.appendDefaultTags(
                            resource.getResourceType(),
                            null
                    );
                    GCKey dataKey = cryptoService.generateGCKey().blockingGet();
                    return new DecryptedRecord<>(
                            null,
                            resource,
                            tags,
                            annotations,
                            createdAt,
                            null,
                            dataKey,
                            null,
                            ModelVersion.CURRENT
                    );
                });

        return createRecord
                .map(record -> uploadData(record, null, userId))
                .map(this::removeUploadData)
                .map(this::encryptRecord)
                .flatMap(
                        encryptedRecord -> apiService.createRecord(alias, userId, encryptedRecord)
                )
                .map(encryptedRecord -> (DecryptedFhirRecord<T> ) decryptRecord(encryptedRecord, userId))
                .map(record -> restoreUploadData(record, resource, data))
                .map(this::assignResourceId)
                .map(
                        decryptedRecord -> new Record<>(
                                decryptedRecord.getResource(),
                                buildMeta(decryptedRecord),
                                decryptedRecord.getAnnotations()
                        )
                );
    }

    <T extends DomainResource> Single<CreateResult<T>> createRecords(List<T> resources, String userId) {
        List<Pair<T, D4LException>> failedOperations = new ArrayList<>();

        return Observable
                .fromCallable(() -> resources)
                .flatMapIterable(resource -> resource)
                .flatMapSingle(resource ->
                        createRecord(resource, userId)
                                .onErrorReturn(error -> {
                                    failedOperations.add(new Pair<>(resource, errorHandler.handleError(error)));
                                    return new EmptyRecord<>();
                                }))
                .filter(record -> !(record instanceof EmptyRecord))
                .toList()
                .map(successOperations -> new CreateResult<>(successOperations, failedOperations));
    }

    Completable deleteRecord(String recordId, String userId) {
        return apiService.deleteRecord(alias, recordId, userId);
    }

    Single<DeleteResult> deleteRecords(List<String> recordIds, String userId) {
        List<Pair<String, D4LException>> failedDeletes = new ArrayList<>();

        return Observable
                .fromCallable(() -> recordIds)
                .flatMapIterable(recordId -> recordId)
                .flatMapSingle(recordId -> deleteRecord(recordId, userId)
                        .doOnError(error -> failedDeletes.add(new Pair<>(recordId, errorHandler.handleError(error))))
                        .toSingleDefault(recordId)
                        .onErrorReturnItem(EMPTY_RECORD_ID))
                .filter(recordId -> !recordId.isEmpty())
                .toList()
                .map(successfulDeletes -> new DeleteResult(successfulDeletes, failedDeletes));
    }

    <T extends DomainResource> Single<Record<T>> fetchRecord(String recordId, String userId) {
        return apiService
                .fetchRecord(alias, userId, recordId)
                .map((EncryptedRecord encryptedRecord) -> (DecryptedFhirRecord<T> ) decryptRecord(encryptedRecord, userId))
                .map(this::assignResourceId)
                .map(decryptedRecord -> new Record<>(decryptedRecord.getResource(), buildMeta(decryptedRecord)));
    }

    <T extends DomainResource> Single<FetchResult<T>> fetchRecords(List<String> recordIds, String userId) {
        List<Pair<String, D4LException>> failedFetches = new ArrayList<>();

        return Observable
                .fromCallable(() -> recordIds)
                .flatMapIterable(recordId -> recordId)
                .flatMapSingle(recordId ->
                        this.<T>fetchRecord(recordId, userId)
                                .onErrorReturn(error -> {
                                    failedFetches.add(new Pair<>(recordId, errorHandler.handleError(error)));
                                    return new EmptyRecord<>();
                                }))
                .filter(record -> !(record instanceof EmptyRecord))
                .toList()
                .map(successfulFetches -> new FetchResult<>(successfulFetches, failedFetches));
    }

    <T extends DomainResource> Single<List<Record<T>>> fetchRecords(
            String userId,
            Class<T> resourceType,
            LocalDate startDate,
            LocalDate endDate,
            Integer pageSize,
            Integer offset
    ) {
        return fetchRecords(userId, resourceType, Collections.emptyList(), startDate, endDate, pageSize, offset);
    }

    <T extends DomainResource> Single<List<Record<T>>> fetchRecords(
            String userId,
            Class<T> resourceType,
            List<String> annotations,
            LocalDate startDate,
            LocalDate endDate,
            Integer pageSize,
            Integer offset
    ) {
        return fetch(
                    userId,
                    annotations,
                    startDate,
                    endDate,
                    pageSize,
                    offset,
                    () -> taggingService.getTagFromType(FhirElementFactory.getFhirTypeForClass(resourceType))
                )
                .map(encryptedRecord -> (DecryptedFhirRecord<T> ) decryptRecord(encryptedRecord, userId))
                .filter(
                        decryptedRecord -> resourceType.isAssignableFrom(
                                Objects.requireNonNull(decryptedRecord.getResource())
                                        .getClass()
                        )
                )
                .filter( decryptedRecord -> Objects.requireNonNull(
                            decryptedRecord.getAnnotations()
                        ).containsAll(annotations)
                )
                .map(this::assignResourceId)
                .map(decryptedRecord -> new Record<>(
                        decryptedRecord.getResource(),
                        buildMeta(decryptedRecord),
                        decryptedRecord.getAnnotations()
                ))
                .toList();
    }

    Single<Attachment> downloadAttachment(String recordId, String attachmentId, String userId, DownloadType type) {
        ArrayList<String> ids = new ArrayList<>();
        ids.add(attachmentId);
        return downloadAttachments(recordId, ids, userId, type).map(it -> it.get(0));
    }

    Single<List<Attachment>> downloadAttachments(String recordId, List<String> attachmentIds, String userId, DownloadType type) {
        return apiService
                .fetchRecord(alias, userId, recordId)
                .map(encryptedRecord -> decryptRecord(encryptedRecord, userId))
                .flatMap(decryptedRecord -> downloadAttachmentsFromStorage(attachmentIds, userId, type, decryptedRecord));
    }

    private Single<? extends List<Attachment>> downloadAttachmentsFromStorage(List<String> attachmentIds, String userId, DownloadType type, DecryptedFhirRecord<DomainResource> decryptedRecord) throws DataValidationException.IdUsageViolation, DataValidationException.InvalidAttachmentPayloadHash {
        if (FhirAttachmentHelper.hasAttachment(decryptedRecord.getResource())) {
            DomainResource resource = decryptedRecord.getResource();
            List<Attachment> attachments = FhirAttachmentHelper.getAttachment(resource);
            ArrayList<Attachment> validAttachments = new ArrayList<>();
            for (Attachment attachment : attachments) {
                if (attachmentIds.contains(attachment.id)) {
                    validAttachments.add(attachment);
                }
            }
            if (validAttachments.size() != attachmentIds.size())
                throw new DataValidationException.IdUsageViolation("Please provide correct attachment ids!");

            setAttachmentIdForDownloadType(validAttachments, FhirAttachmentHelper.getIdentifier(resource), type);
            return attachmentService.downloadAttachments(validAttachments, decryptedRecord.getAttachmentsKey(), userId)
                    .flattenAsObservable(items -> items)
                    .map(attachment -> {
                        if (attachment.id.contains(SPLIT_CHAR)) updateAttachmentMeta(attachment);
                        return attachment;
                    })
                    .toList();
        }
        throw new IllegalArgumentException("Expected a record of a type that has attachment");
    }

    public Single<Boolean> deleteAttachment(String attachmentId, String userId) {
        return attachmentService.deleteAttachment(attachmentId, userId);
    }

    <T extends DomainResource> Single<Record<T>> downloadRecord(String recordId, String userId) {
        return apiService
                .fetchRecord(alias, userId, recordId)
                .map((EncryptedRecord encryptedRecord) -> (DecryptedFhirRecord<T> ) decryptRecord(encryptedRecord, userId))
                .map(decryptedRecord -> downloadData(decryptedRecord, userId))
                .map(decryptedRecord -> {
                    checkDataRestrictions(decryptedRecord.getResource());
                    return decryptedRecord;
                })
                .map(this::assignResourceId)
                .map(decryptedRecord -> new Record<>(decryptedRecord.getResource(), buildMeta(decryptedRecord)));
    }

    <T extends DomainResource> Single<DownloadResult<T>> downloadRecords(List<String> recordIds, String userId) {
        List<Pair<String, D4LException>> failedDownloads = new ArrayList<>();

        return Observable
                .fromCallable(() -> recordIds)
                .flatMapIterable(recordId -> recordId)
                .flatMapSingle(recordId ->
                        this.<T>downloadRecord(recordId, userId)
                                .onErrorReturn(error -> {
                                    failedDownloads.add(new Pair<>(recordId, errorHandler.handleError(error)));
                                    return new EmptyRecord<>();
                                }))
                .filter(record -> !(record instanceof EmptyRecord))
                .toList()
                .map(successfulDownloads -> new DownloadResult<>(successfulDownloads, failedDownloads));
    }

    <T extends DomainResource> Single<Record<T>> updateRecord(T resource, String userId)
            throws DataRestrictionException.UnsupportedFileType, DataRestrictionException.MaxDataSizeViolation {
        return updateRecord(resource,userId, null);
    }

    <T extends DomainResource> Single<Record<T>> updateRecord(T resource, String userId, List<String> annotations)
            throws DataRestrictionException.UnsupportedFileType, DataRestrictionException.MaxDataSizeViolation {
        checkDataRestrictions(resource);
        HashMap<Attachment, String> data = extractUploadData(resource);
        String recordId = resource.id;

        return apiService
                .fetchRecord(alias, userId, recordId)
                .map(encryptedRecord -> decryptRecord(encryptedRecord, userId))
                .map(decryptedRecord -> uploadData(decryptedRecord, resource, userId))
                .map(decryptedRecord -> {
                    cleanObsoleteAdditionalIdentifiers(resource);
                    decryptedRecord.setResource(resource);
                    if(annotations!=null) {
                        decryptedRecord.setAnnotations(annotations);
                    }
                    return decryptedRecord;
                })
                .map(this::removeUploadData)
                .map(this::encryptRecord)
                .flatMap(encryptedRecord -> apiService.updateRecord(alias, userId, recordId, encryptedRecord))
                .map(encryptedRecord -> (DecryptedFhirRecord<T>) decryptRecord(encryptedRecord, userId))
                .map(decryptedRecord -> restoreUploadData(decryptedRecord, resource, data))
                .map(this::assignResourceId)
                .map(decryptedRecord -> new Record<>(
                        decryptedRecord.getResource(),
                        buildMeta(decryptedRecord),
                        decryptedRecord.getAnnotations())
                );
    }

    <T extends DomainResource> Single<UpdateResult<T>> updateRecords(List<T> resources, String userId) {
        List<Pair<T, D4LException>> failedUpdates = new ArrayList<>();

        return Observable
                .fromCallable(() -> resources)
                .flatMapIterable(resource -> resource)
                .flatMapSingle(resource ->
                        updateRecord(resource, userId)
                                .onErrorReturn(error -> {
                                    failedUpdates.add(new Pair<>(resource, errorHandler.handleError(error)));
                                    return new EmptyRecord<>();
                                }))
                .filter(record -> !(record instanceof EmptyRecord))
                .toList()
                .map(successfulUpdates -> new UpdateResult<>(successfulUpdates, failedUpdates));
    }

    Single<Integer> countRecords(Class<? extends DomainResource> type, String userId) {
        return countRecords(type,userId,Collections.emptyList());
    }
    Single<Integer> countRecords(Class<? extends DomainResource> type, String userId, List<String> annotations) {
        if (type == null) return apiService.getCount(alias, userId, null);

        return Single
                .fromCallable(() -> taggingService.getTagFromType(FhirElementFactory.getFhirTypeForClass(type)))
                .map(tags -> tagEncryptionService.encryptTags(tags))
                .map(tags -> {
                    tags.addAll(tagEncryptionService.encryptAnnotations(annotations));
                    return tags;
                })
                .flatMap(encryptedTags -> apiService.getCount(alias, userId, encryptedTags));
    }

    Single<DataRecord> createAppDataRecord(byte[] resource, String userId, List<String> annotations) {
        String createdDate = DATE_FORMATTER.format(LocalDate.now(UTC_ZONE_ID));
        Single<DecryptedDataRecord> createRecord = Single.just(createdDate)
                .map(createdAt -> {
                    HashMap<String, String> tags = taggingService.appendDefaultAnnotatedTags(
                            null,
                            null
                    );
                    GCKey dataKey = cryptoService.generateGCKey().blockingGet();
                    return new DecryptedAppDataRecord(
                            null,
                            resource,
                            tags,
                            annotations,
                            createdAt,
                            null,
                            dataKey,
                            ModelVersion.CURRENT
                    );
                });

        return createRecord
                .map(this::encryptAppDataRecord)
                .flatMap(encryptedRecord -> apiService.createRecord(alias, userId, encryptedRecord))
                .map(encryptedRecord -> decryptAppDataRecord(encryptedRecord, userId))
                .map(decryptedRecord -> new AppDataRecord(
                                Objects.requireNonNull(decryptedRecord.getIdentifier()),
                                decryptedRecord.getResource(),
                                buildMeta(decryptedRecord),
                                annotations
                        )
                );
    }

    Single<DataRecord> updateAppDataRecord(
            byte[] resource,
            String userId,
            String recordId,
            @Nullable List<String> annotations
    )  {
        return apiService
                .fetchRecord(alias, userId, recordId)
                .map(encryptedRecord -> decryptAppDataRecord(encryptedRecord, userId))
                .map(decryptedRecord -> decryptedRecord.copyWithResourceAnnotations(resource,annotations))
                .map(this::encryptAppDataRecord)
                .flatMap(encryptedRecord -> apiService.updateRecord(alias, userId, recordId, encryptedRecord))
                .map(encryptedRecord -> decryptAppDataRecord(encryptedRecord, userId))
                .map(decryptedRecord -> new AppDataRecord(
                        Objects.requireNonNull(decryptedRecord.getIdentifier()),
                        decryptedRecord.getResource(),
                        buildMeta(decryptedRecord),
                        decryptedRecord.getAnnotations()
                    )
                );
    }

    Single<DataRecord> fetchAppDataRecord(String recordId, String userId) {
        return apiService
                .fetchRecord(alias, userId, recordId)
                .map(encryptedRecord -> decryptAppDataRecord(encryptedRecord, userId))
                .map(decryptedRecord -> new AppDataRecord(
                        Objects.requireNonNull(decryptedRecord.getIdentifier()),
                        decryptedRecord.getResource(),
                        buildMeta(decryptedRecord),
                        decryptedRecord.getAnnotations()
                ));
    }

    Single<List<DataRecord>> fetchAppDataRecords(
            String userId,
            List<String> annotations,
            LocalDate startDate,
            LocalDate endDate,
            Integer pageSize,
            Integer offset
    ) {
        return fetch(
                userId,
                annotations,
                startDate,
                endDate,
                pageSize,
                offset,
                () -> taggingService.appendAppDataTags(new HashMap<>())
            )
            .map(encryptedRecord -> decryptAppDataRecord(encryptedRecord,userId))
            .map(decryptedRecord -> (DataRecord) new AppDataRecord(
                    Objects.requireNonNull(decryptedRecord.getIdentifier()),
                    decryptedRecord.getResource(),
                    buildMeta(decryptedRecord),
                    decryptedRecord.getAnnotations()
            ))
            .toList();
    }

    //region utility methods
    private Observable<EncryptedRecord> fetch(
            String userId,
            List<String> annotations,
            LocalDate startDate,
            LocalDate endDate,
            Integer pageSize,
            Integer offset,
            GetTags getTags
    ) {
        String startTime = startDate != null ? DATE_FORMATTER.format(startDate) : null;
        String endTime = endDate != null ? DATE_FORMATTER.format(endDate) : null;

        return Observable
                .fromCallable(getTags::run)
                .map(tags -> tagEncryptionService.encryptTags(tags))
                .map(tags -> {
                    tags.addAll(tagEncryptionService.encryptAnnotations(annotations));
                    return tags;
                })
                .flatMap(
                        encryptedTags -> apiService.fetchRecords(
                                alias,
                                userId,
                                startTime,
                                endTime,
                                pageSize,
                                offset,
                                encryptedTags
                        )
                )
                .flatMapIterable(encryptedRecords -> encryptedRecords);
    }


    private EncryptedRecord encrypt(
            DecryptedBaseRecord record,
            GetDecryptedResource getDecryptedResource,
            GetEncryptedAttachment getEncryptedAttachment
    ) throws IOException {
        List<String> encryptedTags = tagEncryptionService.encryptTags(record.getTags());
        List<String> encryptedAnnotations = tagEncryptionService.encryptAnnotations(record.getAnnotations());
        encryptedTags.addAll(encryptedAnnotations);

        String decryptedResource = getDecryptedResource.run();

        GCKey commonKey = cryptoService.fetchCurrentCommonKey();
        String currentCommonKeyId = cryptoService.getCurrentCommonKeyId();

        EncryptedKey encryptedDataKey = cryptoService.encryptSymmetricKey(
                commonKey,
                KeyType.DATA_KEY,
                record.getDataKey()
        ).blockingGet();

        EncryptedKey encryptedAttachmentsKey = getEncryptedAttachment.run(commonKey);

        return new EncryptedRecord(
                currentCommonKeyId,
                record.getIdentifier(),
                encryptedTags,
                decryptedResource,
                record.getCustomCreationDate(),
                encryptedDataKey,
                encryptedAttachmentsKey,
                record.getModelVersion()
        );
    }

    private <T> T decrypt(
            EncryptedRecord record,
            String userId,
            DecryptSource<T> decryptSource
    ) throws IOException, DataValidationException.ModelVersionNotSupported {
        if (!ModelVersion.isModelVersionSupported(record.getModelVersion())) {
            throw new DataValidationException.ModelVersionNotSupported("Please update SDK to latest version!");
        }
        HashMap<String, String> tags = tagEncryptionService.decryptTags(record.getEncryptedTags());
        List<String> annotations = tagEncryptionService.decryptAnnotations(record.getEncryptedTags());

        String commonKeyId = record.getCommonKeyId();

        boolean commonKeyStored = cryptoService.hasCommonKey(commonKeyId);
        GCKey commonKey;
        if (commonKeyStored) {
            commonKey = cryptoService.getCommonKeyById(commonKeyId);
        } else {
            CommonKeyResponse commonKeyResponse = apiService.fetchCommonKey(
                    alias,
                    userId,
                    commonKeyId
            ).blockingGet();
            EncryptedKey encryptedKey = commonKeyResponse.getCommonKey();

            GCKeyPair gcKeyPair = cryptoService.fetchGCKeyPair().blockingGet();
            commonKey = cryptoService.asymDecryptSymetricKey(gcKeyPair, encryptedKey).blockingGet();
            cryptoService.storeCommonKey(commonKeyId, commonKey);
        }

        GCKey dataKey = cryptoService.symDecryptSymmetricKey(commonKey, record.getEncryptedDataKey()).blockingGet();

        return decryptSource.run(
                tags,
                annotations,
                dataKey,
                commonKey
        );
    }

    <T extends DomainResource> EncryptedRecord encryptRecord(DecryptedFhirRecord<T>  record) throws IOException {
        return encrypt(
                record,
                () -> fhirService.encryptResource(record.getDataKey(), record.getResource()),
                (commonKey) -> {
                    if (record.getAttachmentsKey() == null) {
                        return null;
                    } else {
                        return cryptoService.encryptSymmetricKey(
                                commonKey,
                                KeyType.ATTACHMENT_KEY,
                                record.getAttachmentsKey()
                        ).blockingGet();
                    }
                }
        );
    }

    <T extends DomainResource> DecryptedFhirRecord<T>  decryptRecord(EncryptedRecord record, String userId)
            throws IOException, DataValidationException.ModelVersionNotSupported {

        return decrypt(
                record,
                userId,
                (HashMap<String, String>tags, List<String> annotations, GCKey dataKey, GCKey commonKey ) -> {
                    GCKey attachmentsKey = null;
                    if (record.getEncryptedAttachmentsKey() != null) {
                        attachmentsKey = cryptoService.symDecryptSymmetricKey(
                                commonKey,
                                record.getEncryptedAttachmentsKey()
                        ).blockingGet();
                    }

                    T resource = null;
                    if (record.getEncryptedBody() != null && !record.getEncryptedBody().isEmpty()) {
                        resource = fhirService.decryptResource(
                                dataKey,
                                tags.get(TAG_RESOURCE_TYPE),
                                record.getEncryptedBody()
                        );
                    }

                    return new DecryptedRecord<>(
                            record.getIdentifier(),
                            resource,
                            tags,
                            annotations,
                            record.getCustomCreationDate(),
                            record.getUpdatedDate(),
                            dataKey,
                            attachmentsKey,
                            record.getModelVersion()
                    );
                }
        );
    }

    EncryptedRecord encryptAppDataRecord(DecryptedDataRecord record) throws IOException {
        return encrypt(
                record,
                () -> Base64.INSTANCE.encodeToString(
                        cryptoService.encrypt(record.getDataKey(),record.getResource()).blockingGet()
                ),
                (i) -> null
        );
    }

    DecryptedDataRecord decryptAppDataRecord(EncryptedRecord record, String userId)
            throws IOException, DataValidationException.ModelVersionNotSupported {
        return decrypt(
                record,
                userId,
                (HashMap<String, String>tags, List<String> annotations, GCKey dataKey, GCKey commonKey ) -> {
                    byte [] resource = Base64.INSTANCE.decode(record.getEncryptedBody());
                    if (record.getEncryptedBody() != null && !record.getEncryptedBody().isEmpty()) {
                        resource = cryptoService.decrypt(dataKey, resource).blockingGet();
                    }

                    return new DecryptedAppDataRecord(
                            record.getIdentifier(),
                            resource,
                            tags,
                            annotations,
                            record.getCustomCreationDate(),
                            record.getUpdatedDate(),
                            dataKey,
                            record.getModelVersion()
                    );
                }
        );
    }

    <T extends DomainResource> HashMap<Attachment, String> extractUploadData(T resource) {
        List<Attachment> attachments = FhirAttachmentHelper.getAttachment(resource);
        if (attachments == null || attachments.isEmpty()) return null;
        HashMap<Attachment, String> data = new HashMap<>(attachments.size());
        for (Attachment attachment : attachments) {
            if (attachment != null && attachment.data != null) {
                data.put(attachment, attachment.data);
            }
        }
        return data.isEmpty() ? null : data;
    }

    <T extends DomainResource> DecryptedFhirRecord<T>  removeUploadData(DecryptedFhirRecord<T>  record) {
        return removeOrRestoreUploadData(REMOVE, record, null, null);
    }

    <T extends DomainResource> DecryptedFhirRecord<T>  restoreUploadData(DecryptedFhirRecord<T>  record, T originalResource, HashMap<Attachment, String> attachmentData) {
        return removeOrRestoreUploadData(RESTORE, record, originalResource, attachmentData);
    }

    <T extends DomainResource> DecryptedFhirRecord<T>  removeOrRestoreUploadData(RemoveRestoreOperation operation, DecryptedFhirRecord<T>  record, T originalResource, HashMap<Attachment, String> attachmentData) {
        if (operation == RESTORE) {
            if (originalResource != null) record.setResource(originalResource);
            if (attachmentData == null) return record;
        }

        List<Attachment> attachments = FhirAttachmentHelper.getAttachment(record.getResource());
        if (attachments == null || attachments.isEmpty()) return record;

        FhirAttachmentHelper.updateAttachmentData(record.getResource(), attachmentData);

        return record;
    }

    <T extends DomainResource> DecryptedFhirRecord<T>  uploadData(DecryptedFhirRecord<T>  record, T newResource, String userId) throws DataValidationException.IdUsageViolation, DataValidationException.ExpectedFieldViolation, DataValidationException.InvalidAttachmentPayloadHash {
        return uploadOrDownloadData(newResource == null ? UPLOAD : UPDATE, record, newResource, userId);
    }

    <T extends DomainResource> DecryptedFhirRecord<T>  downloadData(DecryptedFhirRecord<T>  record, String userId) throws DataValidationException.IdUsageViolation, DataValidationException.ExpectedFieldViolation, DataValidationException.InvalidAttachmentPayloadHash {
        return uploadOrDownloadData(DOWNLOAD, record, null, userId);
    }

    <T extends DomainResource> DecryptedFhirRecord<T>  uploadOrDownloadData(UploadDownloadOperation operation, DecryptedFhirRecord<T>  record, T newResource, String userId) throws DataValidationException.IdUsageViolation, DataValidationException.ExpectedFieldViolation, DataValidationException.InvalidAttachmentPayloadHash {
        DomainResource resource = record.getResource();

        if (!FhirAttachmentHelper.hasAttachment(resource)) return record;

        List<Attachment> attachments = (FhirAttachmentHelper.getAttachment(resource) == null) ? new ArrayList<>() : FhirAttachmentHelper.getAttachment(resource);

        if ((operation == UPLOAD || operation == DOWNLOAD) && attachments == null) return record;

        if (operation == UPLOAD && record.getAttachmentsKey() == null) {
            record.setAttachmentsKey(cryptoService.generateGCKey().blockingGet());
        }

        List<Attachment> validAttachments = new ArrayList<>();

        if (operation == UPLOAD || operation == DOWNLOAD) {
            for (Attachment attachment : attachments) {
                if (attachment == null) continue;
                if (operation == UPLOAD && attachment.id != null) {
                    throw new DataValidationException.IdUsageViolation("Attachment.id should be null");
                } else if (operation == UPLOAD && (attachment.hash == null || attachment.size == null)) {
                    throw new DataValidationException.ExpectedFieldViolation(
                            "Attachment.hash and Attachment.size expected");
                } else if (operation == UPLOAD && !getValidHash(attachment).equals(attachment.hash)) {
                    throw new DataValidationException.InvalidAttachmentPayloadHash(
                            "Attachment.hash is not valid");
                } else if (operation == DOWNLOAD && attachment.id == null) {
                    throw new DataValidationException.IdUsageViolation("Attachment.id expected");
                }
                validAttachments.add(attachment);
            }
        } else if (operation == UPDATE) {
            HashMap<String, Attachment> oldAttachments = new HashMap<>();
            for (Attachment attachment : attachments) {
                if (attachment == null) continue;
                else if (attachment.id != null) {
                    oldAttachments.put(attachment.id, attachment);
                }
            }
            resource = newResource;
            List<Attachment> newAttachments = FhirAttachmentHelper.getAttachment(newResource);
            for (Attachment newAttachment : newAttachments) {
                if (newAttachment == null) continue;
                else if (newAttachment.hash == null || newAttachment.size == null) {
                    throw new DataValidationException.ExpectedFieldViolation(
                            "Attachment.hash and Attachment.size expected");
                } else if (!getValidHash(newAttachment).equals(newAttachment.hash)) {
                    throw new DataValidationException.InvalidAttachmentPayloadHash(
                            "Attachment.hash is not valid");
                } else if (newAttachment.id == null) {
                    validAttachments.add(newAttachment);
                } else {
                    Attachment oldAttachment = oldAttachments.get(newAttachment.id);
                    if (oldAttachment == null) {
                        throw new DataValidationException.IdUsageViolation(
                                "Valid Attachment.id expected");
                    }
                    if (oldAttachment.hash == null || !newAttachment.hash.equals(oldAttachment.hash)) {
                        validAttachments.add(newAttachment);
                    }
                }
            }
        }
        if (validAttachments.isEmpty()) return record;
        else if (operation == UPLOAD || operation == UPDATE) {
            List<Pair<Attachment, List<String>>> result = attachmentService.uploadAttachments(validAttachments, record.getAttachmentsKey(), userId).blockingGet();
            updateDomainResourceIdentifier(resource, result);
        } else if (operation == DOWNLOAD) {
            attachmentService.downloadAttachments(attachments, record.getAttachmentsKey(), userId).blockingGet();
        } else {
            throw new CoreRuntimeException.UnsupportedOperation();
        }

        return record;
    }

    private void updateDomainResourceIdentifier(DomainResource d, List<Pair<Attachment, List<String>>> result) {
        StringBuilder sb = new StringBuilder();
        for (Pair<Attachment, List<String>> pair : result) {
            if (pair.getSecond() == null) continue; //Attachment is not of image type

            sb.setLength(0);
            sb.append(DOWNSCALED_ATTACHMENT_IDS_FMT).append(SPLIT_CHAR).append(pair.getFirst().id);
            for (String additionalId : pair.getSecond()) {
                sb.append(SPLIT_CHAR).append(additionalId);
            }
            FhirAttachmentHelper.appendIdentifier(d, sb.toString(), partnerId);
        }
    }


    void setAttachmentIdForDownloadType(List<Attachment> attachments, List<Identifier> identifiers, DownloadType type) throws DataValidationException.IdUsageViolation {
        for (Attachment attachment : attachments) {
            String[] additionalIds = extractAdditionalAttachmentIds(identifiers, attachment.id);
            if (additionalIds == null) continue;

            switch (type) {
                case Full:
                    break;
                case Medium:
                    attachment.id += SPLIT_CHAR + additionalIds[PREVIEW_ID_POS];
                    break;
                case Small:
                    attachment.id += SPLIT_CHAR + additionalIds[THUMBNAIL_ID_POS];
                    break;
                default:
                    throw new CoreRuntimeException.UnsupportedOperation();
            }
        }
    }

    String[] extractAdditionalAttachmentIds(List<Identifier> additionalIds, String attachmentId) throws DataValidationException.IdUsageViolation {
        if (additionalIds == null) return null;

        for (Identifier i : additionalIds) {
            String[] parts = splitAdditionalAttachmentId(i);

            if (parts == null) continue;
            else if (parts[FULL_ATTACHMENT_ID_POS].equals(attachmentId)) return parts;
        }
        return null; //Attachment is not of image type
    }

    @Nullable
    String[] splitAdditionalAttachmentId(Identifier identifier) throws DataValidationException.IdUsageViolation {
        if (identifier.value == null) return null;
        else if (!identifier.value.startsWith(DOWNSCALED_ATTACHMENT_IDS_FMT)) return null;

        String[] parts = identifier.value.split(SPLIT_CHAR);

        if (parts.length != DOWNSCALED_ATTACHMENT_IDS_SIZE) {
            throw new DataValidationException.IdUsageViolation(identifier.value);
        }

        return parts;
    }

    Attachment updateAttachmentMeta(Attachment attachment) {
        byte[] data = Base64.INSTANCE.decode(attachment.data);
        attachment.size = data.length;
        attachment.hash = Base64.INSTANCE.encodeToString(HashUtil.INSTANCE.sha1(data));
        return attachment;
    }

    String getValidHash(Attachment attachment) {
        byte[] data = Base64.INSTANCE.decode(attachment.data);
        return Base64.INSTANCE.encodeToString(HashUtil.INSTANCE.sha1(data));
    }

    <T extends DomainResource> void cleanObsoleteAdditionalIdentifiers(T resource) throws DataValidationException.IdUsageViolation {
        if (FhirAttachmentHelper.hasAttachment(resource)&& !FhirAttachmentHelper.getAttachment(resource).isEmpty()) {
            List<Identifier> identifiers = FhirAttachmentHelper.getIdentifier(resource);

            List<Attachment> currentAttachments = FhirAttachmentHelper.getAttachment(resource);
            List<String> currentAttachmentIds = new ArrayList<>(currentAttachments.size());
            for (Attachment attachment : currentAttachments)
                currentAttachmentIds.add(attachment.id);
            if (identifiers == null) return;

            List<Identifier> updatedIdentifiers = new ArrayList<>();
            Iterator<Identifier> identifierIterator = identifiers.iterator();
            while (identifierIterator.hasNext()) {
                Identifier next = identifierIterator.next();

                String[] parts = splitAdditionalAttachmentId(next);
                if (parts == null || currentAttachmentIds.contains(parts[FULL_ATTACHMENT_ID_POS])) {
                    updatedIdentifiers.add(next);
                } else {
                    identifierIterator.remove();
                }
            }
            FhirAttachmentHelper.setIdentifier(resource, updatedIdentifiers);
        }
    }

    <T extends DomainResource> void checkDataRestrictions(T resource) throws DataRestrictionException.MaxDataSizeViolation, DataRestrictionException.UnsupportedFileType {

        if (!FhirAttachmentHelper.hasAttachment(resource)) return;

        List<Attachment> attachments = FhirAttachmentHelper.getAttachment(resource);
        for (Attachment attachment : attachments) {
            if (attachment == null || attachment.data == null) return;

            byte[] data = Base64.INSTANCE.decode(attachment.data);
            if (MimeType.Companion.recognizeMimeType(data) == MimeType.UNKNOWN) {
                throw new DataRestrictionException.UnsupportedFileType();
            } else if (data.length > DataRestriction.DATA_SIZE_MAX_BYTES) {
                throw new DataRestrictionException.MaxDataSizeViolation();
            }
        }

    }

    <T extends DomainResource> DecryptedFhirRecord<T>  assignResourceId(DecryptedFhirRecord<T>  record) {
        record.getResource().id = record.getIdentifier();
        return record;
    }

    Meta buildMeta(DecryptedBaseRecord record) {
        LocalDate createdDate = LocalDate.parse(record.getCustomCreationDate(), DATE_FORMATTER);
        LocalDateTime updatedDate = LocalDateTime.parse(record.getUpdatedDate(), DATE_TIME_FORMATTER);
        return new Meta(createdDate, updatedDate);
    }
    //endregion
}
