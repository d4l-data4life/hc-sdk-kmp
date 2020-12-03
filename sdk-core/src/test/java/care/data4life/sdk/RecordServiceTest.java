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

import org.junit.Before;
import org.junit.Test;
import org.threeten.bp.LocalDate;
import org.threeten.bp.LocalDateTime;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import care.data4life.crypto.KeyType;
import care.data4life.fhir.stu3.model.Attachment;
import care.data4life.fhir.stu3.model.CarePlan;
import care.data4life.fhir.stu3.model.DocumentReference;
import care.data4life.fhir.stu3.model.DomainResource;
import care.data4life.fhir.stu3.model.Identifier;
import care.data4life.fhir.stu3.model.Organization;
import care.data4life.fhir.stu3.util.FhirAttachmentHelper;
import care.data4life.sdk.config.DataRestriction;
import care.data4life.sdk.config.DataRestrictionException;
import care.data4life.sdk.lang.D4LException;
import care.data4life.sdk.lang.DataValidationException;
import care.data4life.sdk.model.DeleteResult;
import care.data4life.sdk.model.DownloadResult;
import care.data4life.sdk.model.DownloadType;
import care.data4life.sdk.model.FetchResult;
import care.data4life.sdk.model.Meta;
import care.data4life.sdk.model.ModelVersion;
import care.data4life.sdk.model.Record;
import care.data4life.sdk.model.UpdateResult;
import care.data4life.sdk.network.model.DecryptedRecord;
import care.data4life.sdk.network.model.EncryptedKey;
import care.data4life.sdk.network.model.EncryptedRecord;
import care.data4life.sdk.test.util.AttachmentBuilder;
import care.data4life.sdk.util.Base64;
import care.data4life.sdk.util.MimeType;
import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.observers.TestObserver;
import kotlin.Pair;

import static care.data4life.sdk.RecordService.DOWNSCALED_ATTACHMENT_IDS_FMT;
import static care.data4life.sdk.RecordService.DOWNSCALED_ATTACHMENT_IDS_SIZE;
import static care.data4life.sdk.RecordService.FULL_ATTACHMENT_ID_POS;
import static care.data4life.sdk.RecordService.PREVIEW_ID_POS;
import static care.data4life.sdk.RecordService.RemoveRestoreOperation.REMOVE;
import static care.data4life.sdk.RecordService.RemoveRestoreOperation.RESTORE;
import static care.data4life.sdk.RecordService.SPLIT_CHAR;
import static care.data4life.sdk.RecordService.THUMBNAIL_ID_POS;
import static care.data4life.sdk.RecordService.UploadDownloadOperation.DOWNLOAD;
import static care.data4life.sdk.RecordService.UploadDownloadOperation.UPDATE;
import static care.data4life.sdk.RecordService.UploadDownloadOperation.UPLOAD;
import static com.google.common.truth.Truth.assertThat;
import static java.util.Arrays.asList;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;


public class RecordServiceTest extends RecordServiceTestBase {

    public RecordServiceTest() {
        super();
    }

    @Before
    public void setup() {
        init();
    }

    //region utility methods
    @Test
    public void encryptRecord_shouldReturnEncryptedRecord() throws IOException {
        // Given
        String currentCommonKeyId = "currentCommonKeyId";
        when(mockTagEncryptionService.encryptTags(mockTags)).thenReturn(mockEncryptedTags);
        when(mockFhirService.encryptResource(mockDataKey, mockCarePlan)).thenReturn(ENCRYPTED_RESOURCE);
        when(mockCryptoService.fetchCurrentCommonKey()).thenReturn(mockCommonKey);
        when(mockCryptoService.getCurrentCommonKeyId()).thenReturn(currentCommonKeyId);
        when(mockCryptoService.encryptSymmetricKey(mockCommonKey, KeyType.DATA_KEY, mockDataKey)).thenReturn(Single.just(mockEncryptedDataKey));
        //TODO add attachmentKey crypto mock

        // When
        EncryptedRecord encryptedRecord = recordService.encryptRecord(mockDecryptedRecord);

        // Then
        assertThat(encryptedRecord.getCommonKeyId()).isEqualTo(currentCommonKeyId);
        inOrder.verify(mockTagEncryptionService).encryptTags(mockTags);
        inOrder.verify(mockFhirService).encryptResource(mockDataKey, mockCarePlan);
        inOrder.verify(mockCryptoService).fetchCurrentCommonKey();
        inOrder.verify(mockCryptoService).encryptSymmetricKey(mockCommonKey, KeyType.DATA_KEY, mockDataKey);
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void decryptRecord_shouldReturnDecryptedRecord() throws IOException, DataValidationException.ModelVersionNotSupported {
        // Given
        String commonKeyId = "mockCommonKeyId";
        when(mockEncryptedRecord.getModelVersion()).thenReturn(1);
        when(mockEncryptedRecord.getCommonKeyId()).thenReturn(commonKeyId);
        when(mockTagEncryptionService.decryptTags(mockEncryptedTags)).thenReturn(mockTags);
        when(mockCryptoService.hasCommonKey(anyString())).thenReturn(true);
        when(mockCryptoService.getCommonKeyById(anyString())).thenReturn(mockCommonKey);
        when(mockCryptoService.symDecryptSymmetricKey(mockCommonKey, mockEncryptedDataKey)).thenReturn(Single.just(mockDataKey));
        //TODO add attachmentKey decrypt mock
        when(mockFhirService.decryptResource(mockDataKey, CarePlan.resourceType, ENCRYPTED_RESOURCE)).thenReturn(mockCarePlan);

        // When
        recordService.decryptRecord(mockEncryptedRecord, USER_ID);

        // Then
        inOrder.verify(mockTagEncryptionService).decryptTags(mockEncryptedTags);
        inOrder.verify(mockCryptoService).hasCommonKey(commonKeyId);
        inOrder.verify(mockCryptoService).getCommonKeyById(commonKeyId);
        inOrder.verify(mockCryptoService).symDecryptSymmetricKey(mockCommonKey, mockEncryptedDataKey);
        inOrder.verify(mockFhirService).decryptResource(mockDataKey, CarePlan.resourceType, ENCRYPTED_RESOURCE);
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void decryptRecord_shouldThrow_forUnsupportedModelVersion() throws IOException {
        // Given
        when(mockEncryptedRecord.getModelVersion()).thenReturn(ModelVersion.CURRENT + 1);


        // When
        try {
            recordService.decryptRecord(mockEncryptedRecord, USER_ID);
            fail("Exception expected!");
        } catch (D4LException e) {
            // Then
            assertThat(e.getClass()).isEqualTo(DataValidationException.ModelVersionNotSupported.class);
            assertThat(e.getMessage()).isEqualTo("Please update SDK to latest version!");
        }
    }

    @Test
    public void extractUploadData_shouldReturnExtractedData() {
        // Given
        DocumentReference document = buildDocumentReference();

        // When
        HashMap<Attachment, String> data = recordService.extractUploadData(document);

        // Then
        assertThat(data).hasSize(1);
        assertThat(data.get(document.content.get(0).attachment)).isEqualTo(DATA);

        inOrder.verify(recordService).extractUploadData(document);
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void extractUploadData_shouldReturnNull_whenInadequateResourceProvided() {
        // Given
        Organization organization = new Organization();

        // When
        HashMap<Attachment, String> data = recordService.extractUploadData(organization);

        // Then
        assertThat(data).isNull();

        inOrder.verify(recordService).extractUploadData(organization);
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void extractUploadData_shouldReturnNull_whenContentIsNull() {
        // Given
        List<DocumentReference.DocumentReferenceContent> content = null;
        DocumentReference document = new DocumentReference(null, null, null, content);

        // When
        HashMap<Attachment, String> data = recordService.extractUploadData(document);

        // Then
        assertThat(data).isNull();

        inOrder.verify(recordService).extractUploadData(document);
        inOrder.verifyNoMoreInteractions();
    }


    @Test
    public void extractUploadData_shouldReturnNull_whenAttachmentIsNull() {
        // Given
        Attachment attachment = null;
        DocumentReference.DocumentReferenceContent content = new DocumentReference.DocumentReferenceContent(attachment);
        DocumentReference document = new DocumentReference(null, null, null, asList(content));

        // When
        HashMap<Attachment, String> data = recordService.extractUploadData(document);

        // Then
        assertThat(data).isNull();

        inOrder.verify(recordService).extractUploadData(document);
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void extractUploadData_shouldReturnNull_whenAttachmentDataIsNull() {
        // Given
        DocumentReference document = buildDocumentReference();
        document.content.get(0).attachment.data = null;

        // When
        HashMap<Attachment, String> data = recordService.extractUploadData(document);

        // Then
        assertThat(data).isNull();

        inOrder.verify(recordService).extractUploadData(document);
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void removeUploadData_shouldCall_removeOrRestoreUploadData() {
        // Given
        doReturn(mockDecryptedRecord).when(recordService).removeOrRestoreUploadData(REMOVE, mockDecryptedRecord, null, null);

        // When
        DecryptedRecord record = recordService.removeUploadData(mockDecryptedRecord);

        // Then
        assertThat(record).isEqualTo(mockDecryptedRecord);

        inOrder.verify(recordService).removeUploadData(mockDecryptedRecord);
        inOrder.verify(recordService).removeOrRestoreUploadData(REMOVE, mockDecryptedRecord, null, null);
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void restoreUploadData_shouldCall_removeOrRestoreUploadData() {
        // Given
        doReturn(mockDecryptedRecord)
                .when(recordService)
                .removeOrRestoreUploadData(
                    RESTORE,
                    (DecryptedRecord)mockDecryptedRecord,
                    mockDocumentReference,
                    mockUploadData
                );

        // When
        DecryptedRecord record = recordService.restoreUploadData(
                (DecryptedRecord)mockDecryptedRecord,
                mockDocumentReference,
                mockUploadData
        );

        // Then
        assertThat(record).isEqualTo(mockDecryptedRecord);

        inOrder.verify(recordService).restoreUploadData(
                (DecryptedRecord)mockDecryptedRecord,
                mockDocumentReference,
                mockUploadData
        );
        inOrder.verify(recordService).removeOrRestoreUploadData(
                RESTORE,
                (DecryptedRecord)mockDecryptedRecord,
                mockDocumentReference,
                mockUploadData
        );
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void removeOrRestoreUploadData_shouldRemoveUploadData() {
        // Given
        DocumentReference document = buildDocumentReference();
        DecryptedRecord decryptedRecord = new DecryptedRecord<>(
                null,
                document,
                null,
                null,
                null,
                null,
                null,
                -1
        );

        // When
        DecryptedRecord record = recordService.removeOrRestoreUploadData(REMOVE, decryptedRecord, document, mockUploadData);

        // Then
        assertThat(record).isEqualTo(decryptedRecord);
        assertThat(record.getResource()).isEqualTo(document);
        assertThat(document.content.get(0).attachment.data).isNull();

        inOrder.verify(recordService).removeOrRestoreUploadData(REMOVE, decryptedRecord, document, mockUploadData);
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void removeOrRestoreUploadData_shouldRestoreUploadData() {
        // Given
        DocumentReference document = buildDocumentReference();
        document.content.get(0).attachment.data = null;
        DecryptedRecord decryptedRecord = new DecryptedRecord(
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                -1
        );
        HashMap<Attachment, String> uploadData = new HashMap<>();
        uploadData.put(document.content.get(0).attachment, DATA);

        // When
        DecryptedRecord record = recordService.removeOrRestoreUploadData(RESTORE, decryptedRecord, document, uploadData);

        // Then
        assertThat(record).isEqualTo(decryptedRecord);
        assertThat(record.getResource()).isEqualTo(document);
        assertThat(document.content.get(0).attachment.data).isEqualTo(DATA);

        inOrder.verify(recordService).removeOrRestoreUploadData(RESTORE, decryptedRecord, document, uploadData);
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void uploadData_shouldCall_uploadOrDownloadData() throws DataValidationException.ExpectedFieldViolation, DataValidationException.IdUsageViolation, DataValidationException.InvalidAttachmentPayloadHash {
        // Given
        doReturn(mockDecryptedRecord).when(recordService).uploadOrDownloadData(UPLOAD, mockDecryptedRecord, null, USER_ID);

        // When
        DecryptedRecord record = recordService.uploadData(mockDecryptedRecord, null, USER_ID);

        // Then
        assertThat(record).isEqualTo(mockDecryptedRecord);

        inOrder.verify(recordService).uploadData(mockDecryptedRecord, null, USER_ID);
        inOrder.verify(recordService).uploadOrDownloadData(UPLOAD, mockDecryptedRecord, null, USER_ID);
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void uploadData_shouldCall_uploadOrDownloadData_asUPDATE() throws DataValidationException.ExpectedFieldViolation, DataValidationException.IdUsageViolation, DataValidationException.InvalidAttachmentPayloadHash {
        // Given
        doReturn(mockDecryptedRecord).when(recordService).uploadOrDownloadData(
                UPDATE,
                (DecryptedRecord)mockDecryptedRecord,
                mockDocumentReference,
                USER_ID
        );

        // When
        DecryptedRecord record = recordService.uploadData(
                (DecryptedRecord)mockDecryptedRecord,
                mockDocumentReference,
                USER_ID
        );

        // Then
        assertThat(record).isEqualTo(mockDecryptedRecord);

        inOrder.verify(recordService).uploadData(
                (DecryptedRecord)mockDecryptedRecord,
                mockDocumentReference,
                USER_ID
        );
        inOrder.verify(recordService).uploadOrDownloadData(
                UPDATE,
                (DecryptedRecord)mockDecryptedRecord,
                mockDocumentReference,
                USER_ID
        );
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void downloadData_shouldCall_uploadOrDownloadData() throws DataValidationException.ExpectedFieldViolation, DataValidationException.IdUsageViolation, DataValidationException.InvalidAttachmentPayloadHash {
        // Given
        doReturn(mockDecryptedRecord).when(recordService).uploadOrDownloadData(DOWNLOAD, mockDecryptedRecord, null, USER_ID);

        // When
        DecryptedRecord record = recordService.downloadData(mockDecryptedRecord, USER_ID);

        // Then
        assertThat(record).isEqualTo(mockDecryptedRecord);

        inOrder.verify(recordService).downloadData(mockDecryptedRecord, USER_ID);
        inOrder.verify(recordService).uploadOrDownloadData(DOWNLOAD, mockDecryptedRecord, null, USER_ID);
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void uploadOrDownloadData_shouldUploadData() throws DataValidationException.ExpectedFieldViolation, DataValidationException.IdUsageViolation, DataValidationException.InvalidAttachmentPayloadHash {
        // Given
        when(mockCryptoService.generateGCKey()).thenReturn(Single.just(mockAttachmentKey));
        DocumentReference document = buildDocumentReference();
        DecryptedRecord decryptedRecord = new DecryptedRecord<>(null, document, null, null, null, null, null, -1);
        decryptedRecord.setIdentifier(RECORD_ID);
        when(mockCryptoService.generateGCKey()).thenReturn(Single.just(mockAttachmentKey));
        List<Pair<Attachment, List<String>>> uploadResult = new ArrayList<>();
        List<String> downscaledIds = new ArrayList<>();
        downscaledIds.add("downscaledId_1");
        downscaledIds.add("downscaledId_2");
        uploadResult.add(new Pair<>(document.content.get(0).attachment, downscaledIds));
        when(recordService.getValidHash(document.content.get(0).attachment)).thenReturn(DATA_HASH);
        when(mockAttachmentService.uploadAttachments(any(), eq(mockAttachmentKey), eq(USER_ID))).thenReturn(Single.just(uploadResult));

        // When
        DecryptedRecord record = recordService.uploadOrDownloadData(UPLOAD, decryptedRecord, null, USER_ID);

        // Then
        assertThat(record).isEqualTo(decryptedRecord);
        assertThat(record.getAttachmentsKey()).isEqualTo(mockAttachmentKey);

        inOrder.verify(recordService).uploadOrDownloadData(UPLOAD, decryptedRecord, null, USER_ID);
        inOrder.verify(mockCryptoService).generateGCKey();
        inOrder.verify(recordService).getValidHash(document.content.get(0).attachment);
        inOrder.verify(mockAttachmentService).uploadAttachments(any(), eq(mockAttachmentKey), eq(USER_ID));
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void uploadOrDownloadData_shouldThrow_whenAttachmentIdIsSetDuringUploadFlow() throws DataValidationException.ExpectedFieldViolation, DataValidationException.IdUsageViolation, DataValidationException.InvalidAttachmentPayloadHash {
        // Given
        DocumentReference document = buildDocumentReference();
        document.content.get(0).attachment.id = "unexpectedId";
        DecryptedRecord decryptedRecord = new DecryptedRecord<>(null, document, null, null, null, null, null, -1);
        decryptedRecord.setIdentifier(RECORD_ID);
        when(mockCryptoService.generateGCKey()).thenReturn(Single.just(mockAttachmentKey));

        // When
        try {
            recordService.uploadOrDownloadData(UPLOAD, decryptedRecord, null, USER_ID);
            fail("Exception expected!");
        } catch (D4LException e) {

            // Then
            assertThat(e.getClass()).isEqualTo(DataValidationException.IdUsageViolation.class);
            assertThat(e.getMessage()).isEqualTo("Attachment.id should be null");
        }

        inOrder.verify(recordService).uploadOrDownloadData(UPLOAD, decryptedRecord, null, USER_ID);
        inOrder.verify(mockCryptoService).generateGCKey();
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void uploadOrDownloadData_shouldThrow_whenInvalidHashAttachmentDuringUploadFlow() throws DataValidationException.ExpectedFieldViolation, DataValidationException.IdUsageViolation, DataValidationException.InvalidAttachmentPayloadHash {
        // Given
        DocumentReference document = buildDocumentReference();
        document.content.get(0).attachment.id = null;
        document.content.get(0).attachment.hash = "hash";
        DecryptedRecord decryptedRecord = new DecryptedRecord<>(RECORD_ID, document, null, null, null, null, null, -1);
        decryptedRecord.setIdentifier(RECORD_ID);
        when(mockCryptoService.generateGCKey()).thenReturn(Single.just(mockAttachmentKey));

        // When
        try {
            recordService.uploadOrDownloadData(UPLOAD, decryptedRecord, null, USER_ID);
            fail("Exception expected!");
        } catch (D4LException e) {

            // Then
            assertThat(e.getClass()).isEqualTo(DataValidationException.InvalidAttachmentPayloadHash.class);
            assertThat(e.getMessage()).isEqualTo("Attachment.hash is not valid");
        }

        inOrder.verify(recordService).uploadOrDownloadData(UPLOAD, decryptedRecord, null, USER_ID);
        inOrder.verify(mockCryptoService).generateGCKey();
        inOrder.verify(recordService).getValidHash(document.content.get(0).attachment);
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void uploadOrDownloadData_shouldDownloadData() throws DataValidationException.ExpectedFieldViolation, DataValidationException.IdUsageViolation, DataValidationException.InvalidAttachmentPayloadHash {
        // Given
        DocumentReference document = buildDocumentReference();
        document.content.get(0).attachment.id = "id";
        DecryptedRecord decryptedRecord = new DecryptedRecord<>(RECORD_ID, document, null, null, null, null, mockAttachmentKey, -1);
        when(mockAttachmentService.downloadAttachments(any(), eq(mockAttachmentKey), eq(USER_ID))).thenReturn(Single.just(new ArrayList<>()));

        // When
        DecryptedRecord record = recordService.uploadOrDownloadData(DOWNLOAD, decryptedRecord, null, USER_ID);

        // Then
        assertThat(record).isEqualTo(decryptedRecord);

        inOrder.verify(recordService).uploadOrDownloadData(DOWNLOAD, decryptedRecord, null, USER_ID);
        inOrder.verify(mockAttachmentService).downloadAttachments(any(), eq(mockAttachmentKey), eq(USER_ID));
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void uploadOrDownloadData_shouldThrow_whenAttachmentIdIsNotSetDuringDownloadFlow() throws DataValidationException.ExpectedFieldViolation, DataValidationException.IdUsageViolation, DataValidationException.InvalidAttachmentPayloadHash {
        // Given
        DocumentReference document = buildDocumentReference();
        document.content.get(0).attachment.id = null;
        DecryptedRecord decryptedRecord = new DecryptedRecord<>(RECORD_ID, document, null, null, null, null, null, -1);

        // When
        try {
            recordService.uploadOrDownloadData(DOWNLOAD, decryptedRecord, document, USER_ID);
            fail("Exception expected!");
        } catch (D4LException e) {

            // Then
            assertThat(e.getClass()).isEqualTo(DataValidationException.IdUsageViolation.class);
            assertThat(e.getMessage()).isEqualTo("Attachment.id expected");
        }

        inOrder.verify(recordService).uploadOrDownloadData(DOWNLOAD, decryptedRecord, document, USER_ID);
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void uploadOrDownloadData_shouldThrow_whenAttachmentHashOrSizeNotPresent() throws DataValidationException.ExpectedFieldViolation, DataValidationException.IdUsageViolation, DataValidationException.InvalidAttachmentPayloadHash {
        // Given
        DocumentReference document = buildDocumentReference();
        DecryptedRecord decryptedRecord = new DecryptedRecord<>(RECORD_ID, document, null, null, null, null, null, -1);

        // When
        try {
            document.content.get(0).attachment.hash = null;
            document.content.get(0).attachment.size = 0;
            recordService.uploadOrDownloadData(UPDATE, decryptedRecord, document, USER_ID);
            fail("Exception expected!");
        } catch (D4LException e) {

            // Then
            assertThat(e.getClass()).isEqualTo(DataValidationException.ExpectedFieldViolation.class);
            assertThat(e.getMessage()).isEqualTo("Attachment.hash and Attachment.size expected");
        }

        try {
            document.content.get(0).attachment.hash = "hash";
            document.content.get(0).attachment.size = null;
            recordService.uploadOrDownloadData(UPDATE, decryptedRecord, document, USER_ID);
            fail("Exception expected!");
        } catch (D4LException e) {

            // Then
            assertThat(e.getClass()).isEqualTo(DataValidationException.ExpectedFieldViolation.class);
            assertThat(e.getMessage()).isEqualTo("Attachment.hash and Attachment.size expected");
        }

        inOrder.verify(recordService, times(2)).uploadOrDownloadData(UPDATE, decryptedRecord, document, USER_ID);
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void uploadOrDownloadData_shouldThrow_whenInvalidHashAttachmentDuringUpdateFlow() throws DataValidationException.ExpectedFieldViolation, DataValidationException.IdUsageViolation, DataValidationException.InvalidAttachmentPayloadHash {
        // Given
        DocumentReference document = buildDocumentReference();
        DecryptedRecord decryptedRecord = new DecryptedRecord<>(RECORD_ID, document, null, null, null, null, null, -1);

        // When
        try {
            document.content.get(0).attachment.hash = "hash";
            recordService.uploadOrDownloadData(UPDATE, decryptedRecord, document, USER_ID);
            fail("Exception expected!");
        } catch (D4LException e) {

            // Then
            assertThat(e.getClass()).isEqualTo(DataValidationException.InvalidAttachmentPayloadHash.class);
            assertThat(e.getMessage()).isEqualTo("Attachment.hash is not valid");
        }

        inOrder.verify(recordService).uploadOrDownloadData(UPDATE, decryptedRecord, document, USER_ID);
        inOrder.verify(recordService).getValidHash(document.content.get(0).attachment);
        inOrder.verifyNoMoreInteractions();
    }


    @Test
    public void uploadOrDownloadData_shouldThrow_whenOldAttachmentHashNotPresent() throws DataValidationException.IdUsageViolation, DataValidationException.ExpectedFieldViolation, DataValidationException.InvalidAttachmentPayloadHash {
        // Given
        DocumentReference oldDocument = buildDocumentReference();
        Attachment oldAttachment = oldDocument.content.get(0).attachment;
        oldAttachment.id = "id";
        oldAttachment.size = null;
        oldAttachment.hash = null;

        DocumentReference updatedDocument = buildDocumentReference();
        Attachment updatedAttachment = updatedDocument.content.get(0).attachment;
        updatedAttachment.id = "id";
        updatedAttachment.size = 0;
        updatedAttachment.hash = "hash";

        DecryptedRecord decryptedRecord = new DecryptedRecord<>(RECORD_ID, oldDocument, null, null, null, null, mockAttachmentKey, -1);
        List<Pair<Attachment, List<String>>> uploadResult = new ArrayList<>();
        List<String> downscaledIds = new ArrayList<>();
        downscaledIds.add("downscaledId_1");
        downscaledIds.add("downscaledId_2");
        uploadResult.add(new Pair<>(updatedAttachment, downscaledIds));
        when(mockAttachmentService.uploadAttachments(eq(asList(updatedAttachment)), eq(mockAttachmentKey), eq(USER_ID))).thenReturn(Single.just(uploadResult));

        // When
        try {
            recordService.uploadOrDownloadData(UPDATE, decryptedRecord, updatedDocument, USER_ID);
            fail("Exception expected!");
        } catch (D4LException e) {

            // Then
            assertThat(e.getClass()).isEqualTo(DataValidationException.InvalidAttachmentPayloadHash.class);
            assertThat(e.getMessage()).isEqualTo("Attachment.hash is not valid");
        }


        // Then
        inOrder.verify(recordService).uploadOrDownloadData(UPDATE, decryptedRecord, updatedDocument, USER_ID);
        inOrder.verify(recordService).getValidHash(updatedDocument.content.get(0).attachment);
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void uploadOrDownloadData_shouldThrowAttachment_whenHashesDontMatch() throws DataValidationException.IdUsageViolation, DataValidationException.ExpectedFieldViolation, DataValidationException.InvalidAttachmentPayloadHash {
        // Given
        DocumentReference oldDocument = buildDocumentReference();
        Attachment oldAttachment = oldDocument.content.get(0).attachment;
        oldAttachment.id = "id";
        oldAttachment.size = 0;
        oldAttachment.hash = "oldHash";

        DocumentReference updatedDocument = buildDocumentReference();
        Attachment updatedAttachment = updatedDocument.content.get(0).attachment;
        updatedAttachment.id = "id";
        updatedAttachment.size = 0;
        updatedAttachment.hash = "newHash";

        DecryptedRecord decryptedRecord = new DecryptedRecord<>(RECORD_ID, oldDocument, null, null, null, null, mockAttachmentKey, -1);
        List<Pair<Attachment, List<String>>> uploadResult = new ArrayList<>();
        List<String> downscaledIds = new ArrayList<>();
        downscaledIds.add("downscaledId_1");
        downscaledIds.add("downscaledId_2");
        uploadResult.add(new Pair<>(updatedAttachment, downscaledIds));
        when(mockAttachmentService.uploadAttachments(eq(asList(updatedAttachment)), eq(mockAttachmentKey), eq(USER_ID))).thenReturn(Single.just(uploadResult));


        // When
        try {
            recordService.uploadOrDownloadData(UPDATE, decryptedRecord, updatedDocument, USER_ID);
            fail("Exception expected!");
        } catch (D4LException e) {

            // Then
            assertThat(e.getClass()).isEqualTo(DataValidationException.InvalidAttachmentPayloadHash.class);
            assertThat(e.getMessage()).isEqualTo("Attachment.hash is not valid");
        }

        // Then
        inOrder.verify(recordService).uploadOrDownloadData(UPDATE, decryptedRecord, updatedDocument, USER_ID);
        inOrder.verify(recordService).getValidHash(updatedDocument.content.get(0).attachment);
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void uploadOrDownloadData_shouldThrow_whenValidAttachmentIdNotPresent() throws DataValidationException.ExpectedFieldViolation, DataValidationException.IdUsageViolation, DataValidationException.InvalidAttachmentPayloadHash {
        // Given
        DocumentReference oldDocument = buildDocumentReference();
        Attachment oldAttachment = oldDocument.content.get(0).attachment;
        oldAttachment.id = "id1";
        oldAttachment.size = 0;
        oldAttachment.hash = "hash";

        DocumentReference updatedDocument = buildDocumentReference();
        Attachment updatedAttachment = updatedDocument.content.get(0).attachment;
        updatedAttachment.id = "id2";
        updatedAttachment.size = 0;
        updatedAttachment.hash = "hash";

        when(recordService.getValidHash(updatedDocument.content.get(0).attachment)).thenReturn("hash");
        DecryptedRecord decryptedRecord = new DecryptedRecord<>(RECORD_ID, oldDocument, null, null, null, null, null, -1);

        // When
        try {
            recordService.uploadOrDownloadData(UPDATE, decryptedRecord, updatedDocument, USER_ID);
            fail("Exception expected!");
        } catch (D4LException e) {

            // Then
            assertThat(e.getClass()).isEqualTo(DataValidationException.IdUsageViolation.class);
            assertThat(e.getMessage()).isEqualTo("Valid Attachment.id expected");
        }

        inOrder.verify(recordService).uploadOrDownloadData(UPDATE, decryptedRecord, updatedDocument, USER_ID);
        inOrder.verify(recordService).getValidHash(updatedDocument.content.get(0).attachment);
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void uploadOrDownloadData_shouldNotUploadAttachment_whenHashesMatch() throws DataValidationException.InvalidAttachmentPayloadHash, DataValidationException.ExpectedFieldViolation, DataValidationException.IdUsageViolation {
        // Given
        DocumentReference oldDocument = buildDocumentReference();
        Attachment oldAttachment = oldDocument.content.get(0).attachment;
        oldAttachment.id = "id";
        oldAttachment.size = 0;
        oldAttachment.hash = "hash";

        DocumentReference updatedDocument = buildDocumentReference();
        Attachment updatedAttachment = updatedDocument.content.get(0).attachment;
        updatedAttachment.id = "id";
        updatedAttachment.size = 0;
        updatedAttachment.hash = "hash";

        when(recordService.getValidHash(updatedDocument.content.get(0).attachment)).thenReturn("hash");
        DecryptedRecord decryptedRecord = new DecryptedRecord<>(RECORD_ID, oldDocument, null, null, null, null, mockAttachmentKey, -1);

        // When
        recordService.uploadOrDownloadData(UPDATE, decryptedRecord, updatedDocument, USER_ID);

        // Then
        inOrder.verify(recordService).uploadOrDownloadData(UPDATE, decryptedRecord, updatedDocument, USER_ID);
        inOrder.verify(recordService).getValidHash(updatedDocument.content.get(0).attachment);
        inOrder.verifyNoMoreInteractions();
    }


    @Test
    public void uploadOrDownloadData_shouldAppendAdditionalIdentifiers() throws DataValidationException.IdUsageViolation, DataValidationException.ExpectedFieldViolation, DataValidationException.InvalidAttachmentPayloadHash {
        //given
        DocumentReference docRef = buildDocumentReference();
        docRef.content.add(buildDocRefContent(AttachmentBuilder.buildAttachment(null)));
        Attachment firstAttachment = docRef.content.get(0).attachment;
        firstAttachment.title = "image";
        Attachment secondAttachment = docRef.content.get(1).attachment;
        secondAttachment.title = "pdf";

        List<Pair<Attachment, List<String>>> uploadResult = new ArrayList<>();
        uploadResult.add(new Pair<>(firstAttachment, asList(PREVIEW_ID, THUMBNAIL_ID)));
        uploadResult.add(new Pair<>(secondAttachment, null));

        DecryptedRecord<DocumentReference> dummyDecryptedRecord = new DecryptedRecord<>(RECORD_ID, docRef, null, null, null, null, null, -1);
        when(mockCryptoService.generateGCKey()).thenReturn(Single.just(mockAttachmentKey));
        when(recordService.getValidHash(secondAttachment)).thenReturn(DATA_HASH);
        when(recordService.getValidHash(firstAttachment)).thenReturn(DATA_HASH);
        when(mockAttachmentService.uploadAttachments(any(), eq(mockAttachmentKey), eq(USER_ID))).thenReturn(Single.fromCallable(() -> {
            firstAttachment.id = ATTACHMENT_ID;
            secondAttachment.id = ATTACHMENT_ID;
            return uploadResult;
        }));

        //when
        DecryptedRecord<DocumentReference> result = recordService.uploadOrDownloadData(UPLOAD, dummyDecryptedRecord, null, USER_ID);

        //then
        DocumentReference doc = result.getResource();
        assertThat(doc).isEqualTo(docRef);
        assertThat(doc.identifier).hasSize(1);
        assertThat(doc.identifier.get(0).value).isEqualTo(RecordService.DOWNSCALED_ATTACHMENT_IDS_FMT + "#" + ATTACHMENT_ID + "#" + PREVIEW_ID + "#" + THUMBNAIL_ID);
        assertThat(doc.identifier.get(0).assigner.reference).isEqualTo(PARTNER_ID);
    }

    @Test
    public void cleanObsoleteAdditionalIdentifiers_shouldCleanObsoleteIdentifiers() throws DataValidationException.IdUsageViolation {
        //given
        String currentId = ADDITIONAL_ID;
        String obsoleteId = ADDITIONAL_ID.replaceFirst(ATTACHMENT_ID, "obsoleteId");
        String otherId = "otherId";
        Identifier currentIdentifier = FhirAttachmentHelper.buildIdentifier(currentId, ASSIGNER);
        Identifier obsoleteIdentifier = FhirAttachmentHelper.buildIdentifier(obsoleteId, ASSIGNER);
        Identifier otherIdentifier = FhirAttachmentHelper.buildIdentifier(otherId, ASSIGNER);
        List<Identifier> identifiers = new ArrayList<>();
        identifiers.add(currentIdentifier);
        identifiers.add(obsoleteIdentifier);
        identifiers.add(otherIdentifier);

        DocumentReference doc = buildDocumentReference();
        doc.content.get(0).attachment.id = ATTACHMENT_ID;
        doc.identifier = identifiers;

        //when
        recordService.cleanObsoleteAdditionalIdentifiers(doc);

        //then
        assertThat(doc.identifier).hasSize(2);
        assertThat(doc.identifier.get(0)).isEqualTo(currentIdentifier);
        assertThat(doc.identifier.get(1)).isEqualTo(otherIdentifier);
    }

    @Test
    public void setAttachmentIdForDownloadType_shouldSetAttachmentId() throws DataValidationException.IdUsageViolation {
        //given
        Attachment attachment = AttachmentBuilder.buildAttachment(ATTACHMENT_ID);
        Identifier additionalId = FhirAttachmentHelper.buildIdentifier(ADDITIONAL_ID, ASSIGNER);
        List<Attachment> attachments = asList(attachment);
        List<Identifier> identifiers = asList(additionalId);

        //when downloadType is Full
        recordService.setAttachmentIdForDownloadType(attachments, identifiers, DownloadType.Full);
        //then
        assertThat(attachment.id).isEqualTo(ATTACHMENT_ID);

        //given
        attachment.id = ATTACHMENT_ID;
        //when downloadType is Medium
        recordService.setAttachmentIdForDownloadType(attachments, identifiers, DownloadType.Medium);
        //then
        assertThat(attachment.id).isEqualTo(ATTACHMENT_ID + SPLIT_CHAR + PREVIEW_ID);

        //given
        attachment.id = ATTACHMENT_ID;
        //when downloadType is Small
        recordService.setAttachmentIdForDownloadType(attachments, identifiers, DownloadType.Small);
        //then
        assertThat(attachment.id).isEqualTo(ATTACHMENT_ID + SPLIT_CHAR + THUMBNAIL_ID);
    }

    @Test
    public void extractAdditionalAttachmentIds_shouldExtractAdditionalIds() throws DataValidationException.IdUsageViolation {
        //given
        Identifier additionalIdentifier = FhirAttachmentHelper.buildIdentifier(ADDITIONAL_ID, ASSIGNER);

        //when
        String[] additionalIds = recordService.extractAdditionalAttachmentIds(asList(additionalIdentifier), ATTACHMENT_ID);

        //then
        int d4lNamespacePos = 0;
        assertThat(additionalIds).hasLength(DOWNSCALED_ATTACHMENT_IDS_SIZE);
        assertThat(additionalIds[d4lNamespacePos]).isEqualTo(DOWNSCALED_ATTACHMENT_IDS_FMT);
        assertThat(additionalIds[FULL_ATTACHMENT_ID_POS]).isEqualTo(ATTACHMENT_ID);
        assertThat(additionalIds[PREVIEW_ID_POS]).isEqualTo(PREVIEW_ID);
        assertThat(additionalIds[THUMBNAIL_ID_POS]).isEqualTo(THUMBNAIL_ID);
    }

    @Test
    public void extractAdditionalAttachmentIds_shouldReturnNull_whenAdditionalIdentifiersAreNull() throws DataValidationException.IdUsageViolation {
        //when
        String[] additionalIds = recordService.extractAdditionalAttachmentIds(null, ATTACHMENT_ID);

        //then
        assertThat(additionalIds).isNull();
    }

    @Test
    public void extractAdditionalAttachmentIds_shouldReturnNull_whenAdditionalIdentifiersAreNotAdditionalAttachmentIds() throws DataValidationException.IdUsageViolation {
        //given
        Identifier identifier = FhirAttachmentHelper.buildIdentifier("otherId", ASSIGNER);

        //when
        String[] additionalIds = recordService.extractAdditionalAttachmentIds(asList(identifier), ATTACHMENT_ID);

        //then
        assertThat(additionalIds).isNull();
    }

    @Test
    public void splitAdditionalAttachmentId_shouldSplitAdditionalId() throws DataValidationException.IdUsageViolation {
        //given
        Identifier additionalIdentifier = FhirAttachmentHelper.buildIdentifier(ADDITIONAL_ID, ASSIGNER);

        //when
        String[] additionalIds = recordService.splitAdditionalAttachmentId(additionalIdentifier);

        //then
        int d4lNamespacePos = 0;
        assertThat(additionalIds).hasLength(DOWNSCALED_ATTACHMENT_IDS_SIZE);
        assertThat(additionalIds[d4lNamespacePos]).isEqualTo(DOWNSCALED_ATTACHMENT_IDS_FMT);
        assertThat(additionalIds[FULL_ATTACHMENT_ID_POS]).isEqualTo(ATTACHMENT_ID);
        assertThat(additionalIds[PREVIEW_ID_POS]).isEqualTo(PREVIEW_ID);
        assertThat(additionalIds[THUMBNAIL_ID_POS]).isEqualTo(THUMBNAIL_ID);
    }

    @Test
    public void splitAdditionalAttachmentId_shouldReturnNull_whenAdditionalIdentifierIsNull() throws DataValidationException.IdUsageViolation {
        //given
        Identifier additionalIdentifier = FhirAttachmentHelper.buildIdentifier(null, ASSIGNER);
        //when
        String[] additionalIds = recordService.splitAdditionalAttachmentId(additionalIdentifier);
        //then
        assertThat(additionalIds).isNull();
    }

    @Test
    public void splitAdditionalAttachmentId_shouldReturnNull_whenAdditionalIdentifierIsNotAdditionalAttachmentId() throws DataValidationException.IdUsageViolation {
        //given
        Identifier additionalIdentifier = FhirAttachmentHelper.buildIdentifier("otherId", ASSIGNER);

        //when
        String[] additionalIds = recordService.splitAdditionalAttachmentId(additionalIdentifier);

        //then
        assertThat(additionalIds).isNull();
    }

    @Test
    public void splitAdditionalAttachmentId_shouldThrow_whenAdditionalAttachmentIdIsMalformed() {
        //given
        String malformedAdditionalId = ADDITIONAL_ID + SPLIT_CHAR + "unexpectedId";
        Identifier additionalIdentifier = FhirAttachmentHelper.buildIdentifier(malformedAdditionalId, ASSIGNER);

        //when
        try {
            String[] additionalIds = recordService.splitAdditionalAttachmentId(additionalIdentifier);
            fail("Exception expected!");
        } catch (DataValidationException.IdUsageViolation ex) {

            //then
            assertThat(ex.getMessage()).isEqualTo(malformedAdditionalId);
        }
    }

    @Test
    public void updateAttachmentMeta_shouldUpdateAttachmentMeta() {
        //given
        Attachment attachment = new Attachment();
        byte[] data = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xDB};
        String dataBase64 = Base64.INSTANCE.encodeToString(data);
        int oldSize = 0;
        String oldHash = "oldHash";

        attachment.data = dataBase64;
        attachment.size = oldSize;
        attachment.hash = oldHash;

        //when
        recordService.updateAttachmentMeta(attachment);

        //then
        assertThat(attachment.data).isEqualTo(dataBase64);
        assertThat(attachment.size).isEqualTo(data.length);
        assertThat(attachment.hash).isEqualTo("obkanHeotP32HiKllYhs/aRLUAc=");
    }

    @Test
    public void checkForUnsupportedData_shouldReturnSuccessfully() throws DataRestrictionException.UnsupportedFileType, DataRestrictionException.MaxDataSizeViolation {
        // Given
        Byte[] pdf = new Byte[DataRestriction.DATA_SIZE_MAX_BYTES];
        System.arraycopy(MimeType.PDF.byteSignature()[0], 0, pdf, 0, MimeType.PDF.byteSignature()[0].length);
        DocumentReference doc = buildDocumentReference(unboxByteArray(pdf));

        // When
        recordService.checkDataRestrictions(doc);

        // Then
        inOrder.verify(recordService).checkDataRestrictions(doc);
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void checkForUnsupportedData_shouldThrow_forUnsupportedData() throws DataRestrictionException.UnsupportedFileType, DataRestrictionException.MaxDataSizeViolation {
        // Given
        byte[] invalidData = {0x00};
        DocumentReference doc = buildDocumentReference(invalidData);

        // When
        try {
            recordService.checkDataRestrictions(doc);
            fail("Exception expected!");
        } catch (D4LException e) {

            // Then
            assertThat(e.getClass()).isEqualTo(DataRestrictionException.UnsupportedFileType.class);
        }

        // Then
        inOrder.verify(recordService).checkDataRestrictions(doc);
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void checkForUnsupportedData_shouldThrow_whenFileSizeLimitIsReached() throws DataRestrictionException.UnsupportedFileType, DataRestrictionException.MaxDataSizeViolation {
        // Given
        Byte[] invalidSizePdf = new Byte[DataRestriction.DATA_SIZE_MAX_BYTES + 1];
        System.arraycopy(MimeType.PDF.byteSignature()[0], 0, invalidSizePdf, 0, MimeType.PDF.byteSignature()[0].length);
        DocumentReference doc = buildDocumentReference(unboxByteArray(invalidSizePdf));

        // When
        try {
            recordService.checkDataRestrictions(doc);
            fail("Exception expected!");
        } catch (D4LException e) {

            // Then
            assertThat(e.getClass()).isEqualTo(DataRestrictionException.MaxDataSizeViolation.class);
        }

        // Then
        inOrder.verify(recordService).checkDataRestrictions(doc);
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void buildMeta_shouldBuildMeta_whenUpdatedDateMillisecondsArePresent() {
        // Given
        String updatedDateWithMilliseconds = "2019-02-28T17:21:08.234123";

        when(mockDecryptedRecord.getCustomCreationDate()).thenReturn("2019-02-28");
        when(mockDecryptedRecord.getUpdatedDate()).thenReturn(updatedDateWithMilliseconds);

        // When
        Meta meta = recordService.buildMeta(mockDecryptedRecord);

        // Then
        assertThat(meta.getCreatedDate()).isEqualTo(LocalDate.of(2019, 2, 28));
        assertThat(meta.getUpdatedDate()).isEqualTo(LocalDateTime.of(2019, 2, 28, 17, 21, 8, 234123000));

        inOrder.verify(recordService).buildMeta(mockDecryptedRecord);
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void buildMeta_shouldBuildMeta_whenUpdatedDateMillisecondsAreNotPresent() {
        // Given
        String updatedDateWithMilliseconds = "2019-02-28T17:21:08";

        when(mockDecryptedRecord.getCustomCreationDate()).thenReturn("2019-02-28");
        when(mockDecryptedRecord.getUpdatedDate()).thenReturn(updatedDateWithMilliseconds);

        // When
        Meta meta = recordService.buildMeta(mockDecryptedRecord);

        // Then
        assertThat(meta.getCreatedDate()).isEqualTo(LocalDate.of(2019, 2, 28));
        assertThat(meta.getUpdatedDate()).isEqualTo(LocalDateTime.of(2019, 2, 28, 17, 21, 8));

        inOrder.verify(recordService).buildMeta(mockDecryptedRecord);
        inOrder.verifyNoMoreInteractions();
    }
    //endregion

    @Test
    public void deleteRecord_shouldDeleteRecord() throws InterruptedException {
        // Given
        when(mockApiService.deleteRecord(ALIAS, RECORD_ID, USER_ID)).thenReturn(Completable.complete());

        // When
        TestObserver<Void> subscriber = recordService.deleteRecord(RECORD_ID, USER_ID).test().await();

        // Then
        subscriber.assertNoErrors().assertComplete();

        inOrder.verify(recordService).deleteRecord(RECORD_ID, USER_ID);
        inOrder.verify(mockApiService).deleteRecord(ALIAS, RECORD_ID, USER_ID);
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void deleteRecords_shouldDeleteRecords() throws InterruptedException {
        // Given
        doReturn(Completable.complete()).when(recordService).deleteRecord(RECORD_ID, USER_ID);
        List<String> ids = asList(RECORD_ID, RECORD_ID);

        // When
        TestObserver<DeleteResult> observer = recordService.deleteRecords(ids, USER_ID).test().await();

        // Then
        DeleteResult result = observer
                .assertNoErrors()
                .assertComplete()
                .assertValueCount(1)
                .values().get(0);

        assertThat(result.getFailedDeletes()).hasSize(0);
        assertThat(result.getSuccessfulDeletes()).hasSize(2);

        inOrder.verify(recordService).deleteRecords(ids, USER_ID);
        inOrder.verify(recordService, times(2)).deleteRecord(RECORD_ID, USER_ID);
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void fetchRecord_shouldReturnFetchedRecord() throws InterruptedException, IOException, DataValidationException.ModelVersionNotSupported {
        // Given
        when(mockApiService.fetchRecord(ALIAS, USER_ID, RECORD_ID)).thenReturn(Single.just(mockEncryptedRecord));
        doReturn(mockDecryptedRecord).when(recordService).decryptRecord(mockEncryptedRecord, USER_ID);
        doReturn(mockMeta).when(recordService).buildMeta(mockDecryptedRecord);

        // When
        TestObserver<Record<CarePlan>> observer = recordService.<CarePlan>fetchRecord(RECORD_ID, USER_ID).test().await();

        // Then
        Record<CarePlan> record = observer
                .assertNoErrors()
                .assertComplete()
                .assertValueCount(1)
                .values().get(0);

        assertThat(record.getMeta()).isEqualTo(mockMeta);
        assertThat(record.getFhirResource()).isEqualTo(mockCarePlan);

        inOrder.verify(mockApiService).fetchRecord(ALIAS, USER_ID, RECORD_ID);
        inOrder.verify(recordService).decryptRecord(mockEncryptedRecord, USER_ID);
        inOrder.verify(recordService).buildMeta(mockDecryptedRecord);
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void fetchRecordsByUserIds_shouldReturnFetchedRecords() throws InterruptedException {
        // Given
        doReturn(Single.just(mockRecord)).when(recordService).fetchRecord(RECORD_ID, USER_ID);
        List<String> ids = asList(RECORD_ID, RECORD_ID);

        // When
        TestObserver<FetchResult<CarePlan>> observer = recordService.<CarePlan>fetchRecords(ids, USER_ID).test().await();

        // Then
        FetchResult<CarePlan> result = observer
                .assertNoErrors()
                .assertComplete()
                .assertValueCount(1)
                .values().get(0);

        assertThat(result.getSuccessfulFetches()).hasSize(2);
        assertThat(result.getFailedFetches()).hasSize(0);

        inOrder.verify(recordService).fetchRecords(ids, USER_ID);
        inOrder.verify(recordService, times(2)).fetchRecord(RECORD_ID, USER_ID);
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void fetchRecordsByResourceType_shouldReturnFetchedRecords() throws InterruptedException, IOException, DataValidationException.ModelVersionNotSupported {
        // Given
        List<EncryptedRecord> encryptedRecords = asList(mockEncryptedRecord, mockEncryptedRecord);

        when(mockTaggingService.getTagFromType(CarePlan.resourceType)).thenReturn(mockTags);
        when(mockTagEncryptionService.encryptTags(mockTags)).thenReturn(mockEncryptedTags);
        when(mockApiService.fetchRecords(eq(ALIAS), eq(USER_ID), isNull(), isNull(), eq(10), eq(0), eq(mockEncryptedTags))).thenReturn(Observable.just(encryptedRecords));
        doReturn(mockDecryptedRecord).when(recordService).decryptRecord(mockEncryptedRecord, USER_ID);
        doReturn(mockMeta).when(recordService).buildMeta(mockDecryptedRecord);

        // When
        TestObserver<List<Record<CarePlan>>> observer = recordService.fetchRecords(
                USER_ID,
                CarePlan.class,
                null,
                null,
                10,
                0).test().await();

        // Then
        List<Record<CarePlan>> fetched = observer
                .assertNoErrors()
                .assertComplete()
                .assertValueCount(1)
                .values().get(0);

        assertThat(fetched).hasSize(2);
        assertThat(fetched.get(0).getMeta()).isEqualTo(mockMeta);
        assertThat(fetched.get(0).getFhirResource()).isEqualTo(mockCarePlan);
        assertThat(fetched.get(1).getMeta()).isEqualTo(mockMeta);
        assertThat(fetched.get(1).getFhirResource()).isEqualTo(mockCarePlan);

        inOrder.verify(mockTaggingService).getTagFromType(CarePlan.resourceType);
        inOrder.verify(mockTagEncryptionService).encryptTags(mockTags);
        inOrder.verify(mockApiService).fetchRecords(eq(ALIAS), eq(USER_ID), isNull(), isNull(), eq(10), eq(0), eq(mockEncryptedTags));
        inOrder.verify(recordService).decryptRecord(mockEncryptedRecord, USER_ID);
        inOrder.verify(recordService).buildMeta(mockDecryptedRecord);
        inOrder.verify(recordService).decryptRecord(mockEncryptedRecord, USER_ID);
        inOrder.verify(recordService).buildMeta(mockDecryptedRecord);
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void downloadRecord_shouldReturnDownloadedRecord() throws InterruptedException, IOException, DataValidationException.ModelVersionNotSupported, DataValidationException.ExpectedFieldViolation, DataValidationException.IdUsageViolation, DataValidationException.InvalidAttachmentPayloadHash {
        // Given
        when(mockApiService.fetchRecord(ALIAS, USER_ID, RECORD_ID)).thenReturn(Single.just(mockEncryptedRecord));
        when(mockCarePlan.getResourceType()).thenReturn(CarePlan.resourceType);
        doReturn(mockDecryptedRecord).when(recordService).decryptRecord(mockEncryptedRecord, USER_ID);
        doReturn(mockDecryptedRecord).when(recordService).downloadData(mockDecryptedRecord, USER_ID);
        doReturn(mockMeta).when(recordService).buildMeta(mockDecryptedRecord);

        // When
        TestObserver<Record<CarePlan>> observer = recordService.<CarePlan>downloadRecord(RECORD_ID, USER_ID).test().await();

        // Then
        Record<CarePlan> result = observer
                .assertNoErrors()
                .assertComplete()
                .assertValueCount(1)
                .values().get(0);

        assertThat(result.getMeta()).isEqualTo(mockMeta);
        assertThat(result.getFhirResource()).isEqualTo(mockCarePlan);

        inOrder.verify(mockApiService).fetchRecord(ALIAS, USER_ID, RECORD_ID);
        inOrder.verify(recordService).decryptRecord(mockEncryptedRecord, USER_ID);
        inOrder.verify(recordService).buildMeta(mockDecryptedRecord);
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void downloadRecord_shouldThrow_forUnsupportedData() throws InterruptedException, IOException, DataValidationException.ModelVersionNotSupported, DataValidationException.ExpectedFieldViolation, DataRestrictionException.UnsupportedFileType, DataRestrictionException.MaxDataSizeViolation, DataValidationException.IdUsageViolation, DataValidationException.InvalidAttachmentPayloadHash {
        // Given
        byte[] invalidData = {0x00};
        DocumentReference doc = buildDocumentReference(invalidData);

        when(mockApiService.fetchRecord(ALIAS, USER_ID, RECORD_ID)).thenReturn(Single.just(mockEncryptedRecord));
        doReturn(mockDecryptedRecord).when(recordService).decryptRecord(mockEncryptedRecord, USER_ID);
        doReturn(mockDecryptedRecord).when(recordService).downloadData(mockDecryptedRecord, USER_ID);
        when(mockDecryptedRecord.getResource()).thenReturn(doc);

        // When
        TestObserver<Record<DocumentReference>> observer = recordService.<DocumentReference>downloadRecord(RECORD_ID, USER_ID).test().await();

        // Then
        List<Throwable> errors = observer.errors();
        assertThat(errors).hasSize(1);
        assertThat(errors.get(0)).isInstanceOf(DataRestrictionException.UnsupportedFileType.class);

        inOrder.verify(mockApiService).fetchRecord(ALIAS, USER_ID, RECORD_ID);
        inOrder.verify(recordService).decryptRecord(mockEncryptedRecord, USER_ID);
        inOrder.verify(recordService).downloadData(mockDecryptedRecord, USER_ID);
        inOrder.verify(recordService).checkDataRestrictions(doc);
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void downloadRecord_shouldThrow_forFileSizeLimitationBreach() throws InterruptedException, IOException, DataValidationException.ModelVersionNotSupported, DataValidationException.ExpectedFieldViolation, DataRestrictionException.UnsupportedFileType, DataRestrictionException.MaxDataSizeViolation, DataValidationException.IdUsageViolation, DataValidationException.InvalidAttachmentPayloadHash {
        // Given
        Byte[] invalidSizePdf = new Byte[DataRestriction.DATA_SIZE_MAX_BYTES + 1];
        System.arraycopy(MimeType.PDF.byteSignature()[0], 0, invalidSizePdf, 0, MimeType.PDF.byteSignature()[0].length);
        DocumentReference doc = buildDocumentReference(unboxByteArray(invalidSizePdf));

        when(mockApiService.fetchRecord(ALIAS, USER_ID, RECORD_ID)).thenReturn(Single.just(mockEncryptedRecord));
        doReturn(mockDecryptedRecord).when(recordService).decryptRecord(mockEncryptedRecord, USER_ID);
        doReturn(mockDecryptedRecord).when(recordService).downloadData(mockDecryptedRecord, USER_ID);
        when(mockDecryptedRecord.getResource()).thenReturn(doc);

        // When
        TestObserver<Record<DocumentReference>> observer = recordService.<DocumentReference>downloadRecord(RECORD_ID, USER_ID).test().await();

        // Then
        List<Throwable> errors = observer.errors();
        assertThat(errors).hasSize(1);
        assertThat(errors.get(0)).isInstanceOf(DataRestrictionException.MaxDataSizeViolation.class);

        inOrder.verify(mockApiService).fetchRecord(ALIAS, USER_ID, RECORD_ID);
        inOrder.verify(recordService).decryptRecord(mockEncryptedRecord, USER_ID);
        inOrder.verify(recordService).downloadData(mockDecryptedRecord, USER_ID);
        inOrder.verify(recordService).checkDataRestrictions(doc);
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void downloadRecords_shouldReturnDownloadedRecords() throws InterruptedException {
        // Given
        List<String> recordIds = asList(RECORD_ID, RECORD_ID);
        doReturn(Single.just(mockRecord)).when(recordService).downloadRecord(RECORD_ID, USER_ID);

        // When
        TestObserver<DownloadResult<CarePlan>> observer = recordService.<CarePlan>downloadRecords(recordIds, USER_ID).test().await();

        // Then
        DownloadResult<CarePlan> result = observer
                .assertNoErrors()
                .assertComplete()
                .assertValueCount(1)
                .values().get(0);

        assertThat(result.getFailedDownloads()).hasSize(0);
        assertThat(result.getSuccessfulDownloads()).hasSize(2);

        inOrder.verify(recordService).downloadRecords(recordIds, USER_ID);
        inOrder.verify(recordService, times(2)).downloadRecord(RECORD_ID, USER_ID);
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void downloadAttachment_shouldDownloadAttachment() throws InterruptedException, IOException, DataValidationException.ModelVersionNotSupported, DataValidationException.InvalidAttachmentPayloadHash {
        // Given
        when(mockApiService.fetchRecord(ALIAS, USER_ID, RECORD_ID)).thenReturn(Single.just(mockEncryptedRecord));
        DocumentReference document = buildDocumentReference();
        Attachment attachment = AttachmentBuilder.buildAttachment(ATTACHMENT_ID);
        document.content.get(0).attachment = attachment;
        DecryptedRecord decryptedRecord = new DecryptedRecord<>(RECORD_ID, document, null, null, null, null, mockAttachmentKey, -1);
        doReturn(decryptedRecord).when(recordService).decryptRecord(mockEncryptedRecord, USER_ID);
        ArrayList<Attachment> attachments = new ArrayList<>();
        attachments.add(attachment);
        when(mockAttachmentService.downloadAttachments(argThat(arg -> arg.contains(attachment)), eq(mockAttachmentKey), eq(USER_ID))).thenReturn(Single.just(attachments));

        // when
        TestObserver<Attachment> test = recordService.downloadAttachment(RECORD_ID, ATTACHMENT_ID, USER_ID, DownloadType.Full).test().await();

        // then
        Attachment result = test
                .assertNoErrors()
                .assertComplete()
                .assertValue(attachment)
                .values().get(0);

        assertThat(result.id).isEqualTo(ATTACHMENT_ID);
    }

    @Test
    public void downloadAttachments_shouldDownloadAttachments() throws IOException, InterruptedException, DataValidationException.ModelVersionNotSupported, DataValidationException.InvalidAttachmentPayloadHash {
        // Given
        when(mockApiService.fetchRecord(ALIAS, USER_ID, RECORD_ID)).thenReturn(Single.just(mockEncryptedRecord));
        DocumentReference document = buildDocumentReference();
        Attachment attachment = AttachmentBuilder.buildAttachment(ATTACHMENT_ID);
        String secondAttachmentId = "secondId";
        Attachment secondAttachment = AttachmentBuilder.buildAttachment(secondAttachmentId);
        document.content.get(0).attachment = attachment;
        document.content = asList(document.content.get(0), new DocumentReference.DocumentReferenceContent(secondAttachment));
        DecryptedRecord decryptedRecord = new DecryptedRecord<>(RECORD_ID, document, null, null, null, null, mockAttachmentKey, -1);
        doReturn(decryptedRecord).when(recordService).decryptRecord(mockEncryptedRecord, USER_ID);
        ArrayList<Attachment> attachments = new ArrayList<>();
        attachments.add(attachment);
        attachments.add(secondAttachment);
        when(mockAttachmentService.downloadAttachments(argThat(arg -> arg.containsAll(asList(attachment, secondAttachment))), eq(mockAttachmentKey), eq(USER_ID))).thenReturn(Single.just(attachments));

        // when
        List<String> attachmentIds = asList(ATTACHMENT_ID, secondAttachmentId);
        TestObserver<List<Attachment>> test = recordService.downloadAttachments(RECORD_ID, attachmentIds, USER_ID, DownloadType.Full).test().await();

        // then
        List<Attachment> result = test
                .assertNoErrors()
                .assertComplete()
                .assertValue(attachments)
                .values().get(0);

        assertThat(result.get(0).id).isEqualTo(ATTACHMENT_ID);
        assertThat(result.get(1).id).isEqualTo(secondAttachmentId);
    }

    @Test
    public void downloadAttachments_shouldThrow_whenInvalidAttachmentIdsProvided() throws IOException, InterruptedException, DataValidationException.ModelVersionNotSupported {
        //given
        when(mockApiService.fetchRecord(ALIAS, USER_ID, RECORD_ID)).thenReturn(Single.just(mockEncryptedRecord));
        DocumentReference document = buildDocumentReference();
        document.content.get(0).attachment.id = ATTACHMENT_ID;

        DecryptedRecord decryptedRecord = new DecryptedRecord<>(RECORD_ID, document, null, null, null, null, mockAttachmentKey, -1);
        doReturn(decryptedRecord).when(recordService).decryptRecord(mockEncryptedRecord, USER_ID);
        List<String> attachmentIds = asList(ATTACHMENT_ID, "invalidAttachmentId");

        //when
        TestObserver<List<Attachment>> test = recordService.downloadAttachments(RECORD_ID, attachmentIds, USER_ID, DownloadType.Full).test().await();

        //then
        List<Throwable> errors = test.errors();
        assertThat(errors).hasSize(1);
        assertThat(errors.get(0)).isInstanceOf(DataValidationException.IdUsageViolation.class);
        assertThat(errors.get(0).getMessage()).isEqualTo("Please provide correct attachment ids!");
    }

    @Test
    public void updateRecord_shouldReturnUpdatedRecord() throws InterruptedException, IOException, DataValidationException.ModelVersionNotSupported, DataRestrictionException.UnsupportedFileType, DataRestrictionException.MaxDataSizeViolation {
        // Given
        mockCarePlan.id = RECORD_ID;
        when(mockCarePlan.getResourceType()).thenReturn(CarePlan.resourceType);
        when(mockApiService.fetchRecord(ALIAS, USER_ID, RECORD_ID)).thenReturn(Single.just(mockEncryptedRecord));
        doReturn(mockDecryptedRecord).when(recordService).decryptRecord(mockEncryptedRecord, USER_ID);
        doReturn(mockEncryptedRecord).when(recordService).encryptRecord(mockDecryptedRecord);
        when(mockApiService.updateRecord(ALIAS, USER_ID, RECORD_ID, mockEncryptedRecord)).thenReturn(Single.just(mockEncryptedRecord));
        doReturn(mockMeta).when(recordService).buildMeta(mockDecryptedRecord);

        // When
        TestObserver<Record<CarePlan>> observer = recordService.updateRecord(mockCarePlan, USER_ID).test().await();

        // Then
        Record<CarePlan> result = observer.assertNoErrors()
                .assertComplete()
                .assertValueCount(1)
                .values().get(0);

        assertThat(result.getMeta()).isEqualTo(mockMeta);
        assertThat(result.getFhirResource()).isEqualTo(mockCarePlan);

        inOrder.verify(mockApiService).fetchRecord(ALIAS, USER_ID, RECORD_ID);
        inOrder.verify(recordService).decryptRecord(mockEncryptedRecord, USER_ID);
        inOrder.verify(recordService).encryptRecord(mockDecryptedRecord);
        inOrder.verify(mockApiService).updateRecord(ALIAS, USER_ID, RECORD_ID, mockEncryptedRecord);
        inOrder.verify(recordService).buildMeta(mockDecryptedRecord);
        inOrder.verifyNoMoreInteractions();

        // Cleanup
        mockCarePlan.id = null;
    }

    @Test
    public void updateRecord_shouldThrow_forUnsupportedData() throws DataRestrictionException.UnsupportedFileType, DataRestrictionException.MaxDataSizeViolation {
        // Given
        byte[] invalidData = {0x00};
        DocumentReference doc = buildDocumentReference(invalidData);

        // When
        try {
            recordService.updateRecord(doc, USER_ID).test().await();
            fail("Exception expected!");
        } catch (Exception ex) {
            // Then
            assertThat(ex).isInstanceOf(DataRestrictionException.UnsupportedFileType.class);
        }

        inOrder.verify(recordService).updateRecord(doc, USER_ID);
        inOrder.verify(recordService).checkDataRestrictions(doc);
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void updateRecord_shouldThrow_forFileSizeLimitationBreach() throws DataRestrictionException.UnsupportedFileType, DataRestrictionException.MaxDataSizeViolation {
        // Given
        Byte[] invalidSizePdf = new Byte[DataRestriction.DATA_SIZE_MAX_BYTES + 1];
        System.arraycopy(MimeType.PDF.byteSignature()[0], 0, invalidSizePdf, 0, MimeType.PDF.byteSignature()[0].length);
        DocumentReference doc = buildDocumentReference(unboxByteArray(invalidSizePdf));

        // When
        try {
            recordService.updateRecord(doc, USER_ID).test().await();
            fail("Exception expected!");
        } catch (Exception ex) {
            // Then
            assertThat(ex).isInstanceOf(DataRestrictionException.MaxDataSizeViolation.class);
        }

        inOrder.verify(recordService).updateRecord(doc, USER_ID);
        inOrder.verify(recordService).checkDataRestrictions(doc);
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void updateRecords_shouldReturnUpdatedRecords() throws InterruptedException, DataRestrictionException.UnsupportedFileType, DataRestrictionException.MaxDataSizeViolation {
        // Given
        List<CarePlan> resources = asList(mockCarePlan, mockCarePlan);
        doReturn(Single.just(mockRecord)).when(recordService).updateRecord(mockCarePlan, USER_ID);

        // When
        TestObserver<UpdateResult<CarePlan>> observer = recordService.updateRecords(resources, USER_ID).test().await();

        // Then
        UpdateResult<CarePlan> result = observer
                .assertNoErrors()
                .assertComplete()
                .assertValueCount(1)
                .values().get(0);

        assertThat(result.getFailedUpdates()).hasSize(0);
        assertThat(result.getSuccessfulUpdates()).hasSize(2);
        assertThat(result.getSuccessfulUpdates()).containsExactly(mockRecord, mockRecord);

        inOrder.verify(recordService).updateRecords(resources, USER_ID);
        inOrder.verify(recordService, times(2)).updateRecord(mockCarePlan, USER_ID);
        inOrder.verifyNoMoreInteractions();
    }

    public static DocumentReference buildDocumentReference() {
        DocumentReference.DocumentReferenceContent content = buildDocRefContent(AttachmentBuilder.buildAttachment(null));
        List<DocumentReference.DocumentReferenceContent> contents = new ArrayList<>();
        contents.add(content);
        return new DocumentReference(null, null, null, contents);
    }

    public static DocumentReference.DocumentReferenceContent buildDocRefContent(Attachment attachment) {
        return new DocumentReference.DocumentReferenceContent(attachment);
    }

    public static DocumentReference buildDocumentReference(byte[] data) {
        DocumentReference doc = buildDocumentReference();
        doc.content.get(0).attachment.data = Base64.INSTANCE.encodeToString(data);
        return doc;
    }

    static byte[] unboxByteArray(Byte[] array) {
        byte[] result = new byte[array.length];

        int i = 0;
        for (Byte b : array) result[i++] = (b == null ? 0 : b);

        return result;
    }
}
