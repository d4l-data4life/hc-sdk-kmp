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
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.Mockito;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import care.data4life.crypto.GCKey;
import care.data4life.fhir.stu3.model.Attachment;
import care.data4life.fhir.stu3.model.CarePlan;
import care.data4life.fhir.stu3.model.Identifier;
import care.data4life.fhir.stu3.model.Medication;
import care.data4life.fhir.stu3.model.Observation;
import care.data4life.fhir.stu3.model.Patient;
import care.data4life.fhir.stu3.model.Questionnaire;
import care.data4life.fhir.stu3.model.QuestionnaireResponse;
import care.data4life.fhir.stu3.util.FhirAttachmentHelper;
import care.data4life.sdk.config.DataRestriction;
import care.data4life.sdk.config.DataRestrictionException;
import care.data4life.sdk.lang.D4LException;
import care.data4life.sdk.lang.DataValidationException;
import care.data4life.sdk.model.DownloadType;
import care.data4life.sdk.model.Meta;
import care.data4life.sdk.model.ModelVersion;
import care.data4life.sdk.model.Record;
import care.data4life.sdk.network.model.DecryptedRecord;
import care.data4life.sdk.network.model.EncryptedKey;
import care.data4life.sdk.network.model.EncryptedRecord;
import care.data4life.sdk.test.util.AttachmentBuilder;
import care.data4life.sdk.test.util.MedicationBuilder;
import care.data4life.sdk.test.util.ObservationBuilder;
import care.data4life.sdk.test.util.PatientBuilder;
import care.data4life.sdk.test.util.QuestionnaireBuilder;
import care.data4life.sdk.test.util.QuestionnaireResponseBuilder;
import care.data4life.sdk.util.MimeType;
import io.reactivex.Single;
import io.reactivex.observers.TestObserver;
import kotlin.Pair;

import static care.data4life.sdk.RecordService.DOWNSCALED_ATTACHMENT_IDS_FMT;
import static care.data4life.sdk.RecordService.RemoveRestoreOperation.REMOVE;
import static care.data4life.sdk.RecordService.RemoveRestoreOperation.RESTORE;
import static care.data4life.sdk.RecordService.SPLIT_CHAR;
import static care.data4life.sdk.RecordService.UploadDownloadOperation.DOWNLOAD;
import static care.data4life.sdk.RecordService.UploadDownloadOperation.UPLOAD;
import static com.google.common.truth.Truth.assertThat;
import static java.util.Arrays.asList;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;


public class RecordServiceAdditionalResourceTypeTest {
    private static final String PARTNER_ID = "partnerId";
    private static final String USER_ID = "userId";
    private static final String ENCRYPTED_RESOURCE = "encryptedResource";
    private static final String RESOURCE_TYPE = "resourcetype";
    private static final String RECORD_ID = "recordId";
    private static final String ALIAS = "alias";
    private static final String DATA = "data";
    private static final String ATTACHMENT_ID = "attachmentId";
    private static final String THUMBNAIL_ID = "thumbnailId";
    private static final String PREVIEW_ID = "previewId";
    private static final String ASSIGNER = "assigner";
    private static final String ADDITIONAL_ID = DOWNSCALED_ATTACHMENT_IDS_FMT + SPLIT_CHAR + ATTACHMENT_ID + SPLIT_CHAR + PREVIEW_ID + SPLIT_CHAR + THUMBNAIL_ID;
    private static final String DATA_HASH = "dataHash";

    //SUT
    private RecordService recordService;
    private ApiService mockApiService;
    private TagEncryptionService mockTagEncryptionService;
    private TaggingService mockTaggingService;
    private FhirService mockFhirService;
    private AttachmentService mockAttachmentService;
    private CryptoService mockCryptoService;
    private D4LErrorHandler mockErrorHandler;

    private CarePlan mockCarePlan;
    private HashMap<String, String> mockTags;
    private HashMap<Attachment, String> mockUploadData;
    private List<String> mockEncryptedTags;
    private GCKey mockDataKey;
    private GCKey mockAttachmentKey;
    private EncryptedKey mockEncryptedDataKey;
    private EncryptedRecord mockEncryptedRecord;
    private DecryptedRecord mockDecryptedRecord;
    private Meta mockMeta;
    private D4LException mockD4LException;
    private Record<CarePlan> mockRecord;

    private InOrder inOrder;

    @Before
    public void setup() {
        mockApiService = mock(ApiService.class);
        mockTagEncryptionService = mock(TagEncryptionService.class);
        mockTaggingService = mock(TaggingService.class);
        mockFhirService = mock(FhirService.class);
        mockAttachmentService = mock(AttachmentService.class);
        mockCryptoService = mock(CryptoService.class);
        mockErrorHandler = mock(D4LErrorHandler.class);
        recordService = spy(new RecordService(
                PARTNER_ID,
                ALIAS,
                mockApiService,
                mockTagEncryptionService,
                mockTaggingService,
                mockFhirService,
                mockAttachmentService,
                mockCryptoService,
                mockErrorHandler));

        mockCarePlan = mock(CarePlan.class);
        mockTags = mock(HashMap.class);
        mockUploadData = mock(HashMap.class);
        mockEncryptedTags = mock(List.class);
        mockDataKey = mock(GCKey.class);
        mockAttachmentKey = mock(GCKey.class);
        mockEncryptedDataKey = mock(EncryptedKey.class);
        mockEncryptedRecord = mock(EncryptedRecord.class);
        mockDecryptedRecord = mock(DecryptedRecord.class);
        mockMeta = mock(Meta.class);
        mockD4LException = mock(D4LException.class);
        mockRecord = mock(Record.class);

        when(mockRecord.getFhirResource()).thenReturn(mockCarePlan);
        when(mockRecord.getMeta()).thenReturn(mockMeta);
        when(mockDecryptedRecord.getTags()).thenReturn(mockTags);
        when(mockDecryptedRecord.getDataKey()).thenReturn(mockDataKey);
        when(mockDecryptedRecord.getResource()).thenReturn(mockCarePlan);
        when(mockDecryptedRecord.getModelVersion()).thenReturn(ModelVersion.CURRENT);
        when(mockTags.get(RESOURCE_TYPE)).thenReturn(CarePlan.resourceType);
        when(mockEncryptedRecord.getEncryptedTags()).thenReturn(mockEncryptedTags);
        when(mockEncryptedRecord.getEncryptedDataKey()).thenReturn(mockEncryptedDataKey);
        when(mockEncryptedRecord.getEncryptedBody()).thenReturn(ENCRYPTED_RESOURCE);
        when(mockEncryptedRecord.getModelVersion()).thenReturn(ModelVersion.CURRENT);
        when(mockEncryptedRecord.getIdentifier()).thenReturn(RECORD_ID);
        when(mockErrorHandler.handleError(any(Exception.class))).thenReturn(mockD4LException);

        inOrder = Mockito.inOrder(
                mockApiService,
                mockTagEncryptionService,
                mockTaggingService,
                mockFhirService,
                mockAttachmentService,
                mockCryptoService,
                mockErrorHandler,
                recordService);
    }

    @Test
    public void extractUploadData_shouldReturnExtractedData_Patient() {
        // Given
        Patient patient = PatientBuilder.buildPatient();

        // When
        HashMap<Attachment, String> data = recordService.extractUploadData(patient);

        // Then
        assertThat(data).hasSize(1);
        assertThat(data.get(patient.photo.get(0))).isEqualTo(DATA);

        inOrder.verify(recordService).extractUploadData(patient);
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void extractUploadData_shouldReturnExtractedData_Observation() {
        // Given
        Observation observation = ObservationBuilder.buildObservationWithComponent();

        // When
        HashMap<Attachment, String> data = recordService.extractUploadData(observation);

        // Then
        assertThat(data).hasSize(2);
        assertThat(data.get(observation.component.get(0).valueAttachment)).isEqualTo(DATA);

        inOrder.verify(recordService).extractUploadData(observation);
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void extractUploadData_shouldReturnExtractedData_Observation_Component() {
        // Given
        Observation observation = ObservationBuilder.buildObservationWithComponent();

        // When
        HashMap<Attachment, String> data = recordService.extractUploadData(observation);

        // Then
        assertThat(data).hasSize(2);
        assertThat(data.get(observation.component.get(0).valueAttachment)).isEqualTo(DATA);

        inOrder.verify(recordService).extractUploadData(observation);
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void extractUploadData_shouldReturnExtractedData_Medication() {
        // Given
        Medication medication = MedicationBuilder.buildMedication();

        // When
        HashMap<Attachment, String> data = recordService.extractUploadData(medication);

        // Then
        assertThat(data).hasSize(1);
        assertThat(data.get(medication.image.get(0))).isEqualTo(DATA);

        inOrder.verify(recordService).extractUploadData(medication);
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void extractUploadData_shouldReturnExtractedData_Questionnaire() {
        // Given
        Questionnaire questionnaire = QuestionnaireBuilder.buildQuestionnaire();

        // When
        HashMap<Attachment, String> data = recordService.extractUploadData(questionnaire);

        // Then
        assertThat(data).hasSize(1);
        assertThat(data.get(questionnaire.item.get(0).initialAttachment)).isEqualTo(DATA);

        inOrder.verify(recordService).extractUploadData(questionnaire);
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void extractUploadData_shouldReturnExtractedData_QuestionnaireResponse() {
        // Given
        QuestionnaireResponse questionnaireResponse = QuestionnaireResponseBuilder.buildQuestionnaireResponse();

        // When
        HashMap<Attachment, String> data = recordService.extractUploadData(questionnaireResponse);

        // Then
        assertThat(data).hasSize(1);
        assertThat(data.get(questionnaireResponse.item.get(0).answer.get(0).valueAttachment)).isEqualTo(DATA);

        inOrder.verify(recordService).extractUploadData(questionnaireResponse);
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void extractUploadData_shouldReturnNull_whenPhotoIsNull() {
        // Given
        Patient patient = PatientBuilder.buildPatient();
        patient.photo = null;

        // When
        HashMap<Attachment, String> data = recordService.extractUploadData(patient);

        // Then
        assertThat(data).isNull();

        inOrder.verify(recordService).extractUploadData(patient);
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void extractUploadData_shouldNotReturnNull_whenComponentIsNull() {
        // Given
        Observation observation = ObservationBuilder.buildObservationWithComponent();
        observation.component = null;

        // When
        HashMap<Attachment, String> data = recordService.extractUploadData(observation);

        // Then
        assertThat(data).hasSize(1);
        inOrder.verify(recordService).extractUploadData(observation);
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void extractUploadData_shouldReturnNull_whenObservationAttachmentIsNull() {
        // Given
        Observation observation = ObservationBuilder.buildObservation();
        observation.valueAttachment = null;

        // When
        HashMap<Attachment, String> data = recordService.extractUploadData(observation);

        // Then
        assertThat(data).isNull();
        inOrder.verify(recordService).extractUploadData(observation);
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void extractUploadData_shouldReturnNull_whenQuestionnaireItemIsNull() {
        // Given
        Questionnaire questionnaire = QuestionnaireBuilder.buildQuestionnaire();
        questionnaire.item = null;

        // When
        HashMap<Attachment, String> data = recordService.extractUploadData(questionnaire);

        // Then
        assertThat(data).isNull();

        inOrder.verify(recordService).extractUploadData(questionnaire);
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void extractUploadData_shouldReturnNull_whenQuestionnaireResponseItemIsNull() {
        // Given
        QuestionnaireResponse response = QuestionnaireResponseBuilder.buildQuestionnaireResponse();
        response.item = null;

        // When
        HashMap<Attachment, String> data = recordService.extractUploadData(response);

        // Then
        assertThat(data).isNull();

        inOrder.verify(recordService).extractUploadData(response);
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void extractUploadData_shouldReturnValueAttachment_whenAttachmentIsNull_Observation() {
        // Given
        Observation observation = ObservationBuilder.buildObservationWithComponent();
        Observation.ObservationComponent component = ObservationBuilder.buildComponent(null, null);
        observation.component.set(0, component);

        // When
        HashMap<Attachment, String> data = recordService.extractUploadData(observation);

        // Then
        assertThat(data).isNotNull();

        inOrder.verify(recordService).extractUploadData(observation);
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void extractUploadData_shouldReturnNull_whenAllAttachmentIsNull_Observation() {
        // Given
        Observation observation = ObservationBuilder.buildObservationWithComponent();
        Observation.ObservationComponent component = ObservationBuilder.buildComponent(null, null);
        observation.component.set(0, component);
        observation.valueAttachment = null;

        // When
        HashMap<Attachment, String> data = recordService.extractUploadData(observation);

        // Then
        assertThat(data).isNull();

        inOrder.verify(recordService).extractUploadData(observation);
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void extractUploadData_shouldReturnNull_whenAttachmentIsNull_QuestionnaireResponse() {
        // Given
        QuestionnaireResponse questionnaireResponse = QuestionnaireResponseBuilder.buildQuestionnaireResponse();
        QuestionnaireResponse.QuestionnaireResponseItem questionnaireResponseItem = QuestionnaireResponseBuilder.buildItem("", null);
        questionnaireResponse.item.set(0, questionnaireResponseItem);
        // When
        HashMap<Attachment, String> data = recordService.extractUploadData(questionnaireResponse);

        // Then
        assertThat(data).isNull();

        inOrder.verify(recordService).extractUploadData(questionnaireResponse);
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void extractUploadData_shouldReturnNull_whenAnswerIsNull_QuestionnaireResponse() {
        // Given
        QuestionnaireResponse questionnaireResponse = QuestionnaireResponseBuilder.buildQuestionnaireResponse();
        questionnaireResponse.item.get(0).answer.set(0, null);

        // When
        HashMap<Attachment, String> data = recordService.extractUploadData(questionnaireResponse);

        // Then
        assertThat(data).isNull();

        inOrder.verify(recordService).extractUploadData(questionnaireResponse);
        inOrder.verifyNoMoreInteractions();
    }


    @Test
    public void removeOrRestoreUploadData_shouldRemoveUploadData_fromPatient() {
        // Given
        Patient patient = PatientBuilder.buildPatient();
        DecryptedRecord decryptedRecord = new DecryptedRecord<>(
                null,
                patient,
                null,
                new ArrayList<>(),
                null,
                null,
                null,
                null,
                -1
        );

        // When
        DecryptedRecord record = recordService.removeOrRestoreUploadData(REMOVE, decryptedRecord, patient, null);

        // Then
        assertThat(record).isEqualTo(decryptedRecord);
        assertThat(record.getResource()).isEqualTo(patient);
        assertThat(patient.photo.get(0).data).isNull();

        inOrder.verify(recordService).removeOrRestoreUploadData(REMOVE, decryptedRecord, patient, null);
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void removeOrRestoreUploadData_shouldRemoveUploadData_fromObservation() {
        // Given
        Observation observation = ObservationBuilder.buildObservationWithComponent();
        DecryptedRecord decryptedRecord = new DecryptedRecord<>(
                null,
                observation,
                null,
                new ArrayList<>(),
                null,
                null,
                null,
                null,
                -1
        );

        // When
        DecryptedRecord record = recordService.removeOrRestoreUploadData(REMOVE, decryptedRecord, observation, null);

        // Then
        assertThat(record).isEqualTo(decryptedRecord);
        assertThat(record.getResource()).isEqualTo(observation);
        assertThat(observation.component.get(0).valueAttachment.data).isNull();
        assertThat(observation.valueAttachment.data).isNull();

        inOrder.verify(recordService).removeOrRestoreUploadData(REMOVE, decryptedRecord, observation, null);
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void removeOrRestoreUploadData_shouldRemoveUploadData_fromQuestionnaireResponse() {
        // Given
        QuestionnaireResponse questionnaireResponse = QuestionnaireResponseBuilder.buildQuestionnaireResponse();
        DecryptedRecord decryptedRecord = new DecryptedRecord<>(
                null,
                questionnaireResponse,
                null,
                new ArrayList<>(),
                null,
                null,
                null,
                null,
                -1
        );

        // When
        DecryptedRecord record = recordService.removeOrRestoreUploadData(REMOVE, decryptedRecord, questionnaireResponse, null);

        // Then
        assertThat(record).isEqualTo(decryptedRecord);
        assertThat(record.getResource()).isEqualTo(questionnaireResponse);
        assertThat(questionnaireResponse.item.get(0).answer.get(0).valueAttachment.data).isNull();

        inOrder.verify(recordService).removeOrRestoreUploadData(REMOVE, decryptedRecord, questionnaireResponse, null);
        inOrder.verifyNoMoreInteractions();
    }


    @Test
    public void removeOrRestoreUploadData_shouldRestoreUploadData_fromPatient() {
        // Given
        Patient patient = PatientBuilder.buildPatient();
        patient.photo.get(0).data = null;
        DecryptedRecord decryptedRecord = new DecryptedRecord(
                null,
                null,
                null,
                new ArrayList<>(),
                null,
                null,
                null,
                null,
                -1
        );
        HashMap<Attachment, String> uploadData = new HashMap<>();
        uploadData.put(patient.photo.get(0), DATA);

        // When
        DecryptedRecord record = recordService.removeOrRestoreUploadData(RESTORE, decryptedRecord, patient, uploadData);

        // Then
        assertThat(record).isEqualTo(decryptedRecord);
        assertThat(record.getResource()).isEqualTo(patient);
        assertThat(patient.photo.get(0).data).isEqualTo(DATA);

        inOrder.verify(recordService).removeOrRestoreUploadData(RESTORE, decryptedRecord, patient, uploadData);
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void removeOrRestoreUploadData_shouldRestoreUploadData_fromObservation() {
        // Given
        Observation observation = ObservationBuilder.buildObservationWithComponent();
        observation.component.get(0).valueAttachment.data = null;
        DecryptedRecord decryptedRecord = new DecryptedRecord(
                null,
                null,
                null,
                new ArrayList<>(),
                null,
                null,
                null,
                null,
                -1
        );
        HashMap<Attachment, String> uploadData = new HashMap<>();
        uploadData.put(observation.component.get(0).valueAttachment, DATA);

        // When
        DecryptedRecord record = recordService.removeOrRestoreUploadData(RESTORE, decryptedRecord, observation, uploadData);

        // Then
        assertThat(record).isEqualTo(decryptedRecord);
        assertThat(record.getResource()).isEqualTo(observation);
        assertThat(observation.component.get(0).valueAttachment.data).isEqualTo(DATA);

        inOrder.verify(recordService).removeOrRestoreUploadData(RESTORE, decryptedRecord, observation, uploadData);
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void removeOrRestoreUploadData_shouldRestoreUploadData_fromQuestionnaireResponse() {
        // Given
        QuestionnaireResponse questionnaireResponse = QuestionnaireResponseBuilder.buildQuestionnaireResponse();
        questionnaireResponse.item.get(0).answer.get(0).valueAttachment.data = null;
        DecryptedRecord decryptedRecord = new DecryptedRecord(
                null,
                null,
                null,
                new ArrayList<>(),null,
                null,
                null,
                null,
                -1
        );
        HashMap<Attachment, String> uploadData = new HashMap<>();
        uploadData.put(questionnaireResponse.item.get(0).answer.get(0).valueAttachment, DATA);

        // When
        DecryptedRecord record = recordService.removeOrRestoreUploadData(RESTORE, decryptedRecord, questionnaireResponse, uploadData);

        // Then
        assertThat(record).isEqualTo(decryptedRecord);
        assertThat(record.getResource()).isEqualTo(questionnaireResponse);
        assertThat(questionnaireResponse.item.get(0).answer.get(0).valueAttachment.data).isEqualTo(DATA);

        inOrder.verify(recordService).removeOrRestoreUploadData(RESTORE, decryptedRecord, questionnaireResponse, uploadData);
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void uploadOrDownloadData_shouldUploadData_Patient() throws DataValidationException.ExpectedFieldViolation, DataValidationException.IdUsageViolation, DataValidationException.InvalidAttachmentPayloadHash {
        // Given
        when(mockCryptoService.generateGCKey()).thenReturn(Single.just(mockAttachmentKey));
        Patient patient = PatientBuilder.buildPatient();
        DecryptedRecord decryptedRecord = new DecryptedRecord<>(
                null,
                patient,
                null,
                new ArrayList<>(),
                null,
                null,
                null,
                null,
                -1
        );
        decryptedRecord.setIdentifier(RECORD_ID);
        when(mockCryptoService.generateGCKey()).thenReturn(Single.just(mockAttachmentKey));
        List<Pair<Attachment, List<String>>> uploadResult = new ArrayList<>();
        List<String> downscaledIds = new ArrayList<>();
        downscaledIds.add("downscaledId_1");
        downscaledIds.add("downscaledId_2");
        uploadResult.add(new Pair<>(patient.photo.get(0), downscaledIds));
        when(mockAttachmentService.uploadAttachments(any(), eq(mockAttachmentKey), eq(USER_ID))).thenReturn(Single.just(uploadResult));
        when(recordService.getValidHash(patient.photo.get(0))).thenReturn(DATA_HASH);

        // When
        DecryptedRecord record = recordService.uploadOrDownloadData(UPLOAD, decryptedRecord, null, USER_ID);

        // Then
        assertThat(record).isEqualTo(decryptedRecord);
        assertThat(record.getAttachmentsKey()).isEqualTo(mockAttachmentKey);

        inOrder.verify(recordService).uploadOrDownloadData(UPLOAD, decryptedRecord, null, USER_ID);
        inOrder.verify(mockCryptoService).generateGCKey();
        inOrder.verify(mockAttachmentService).uploadAttachments(any(), eq(mockAttachmentKey), eq(USER_ID));
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void uploadOrDownloadData_shouldDownloadData_Patient() throws DataValidationException.ExpectedFieldViolation, DataValidationException.IdUsageViolation, DataValidationException.InvalidAttachmentPayloadHash {
        // Given
        Patient patient = PatientBuilder.buildPatient();
        patient.photo.get(0).id = "id";
        DecryptedRecord decryptedRecord = new DecryptedRecord<>(
                RECORD_ID,
                patient,
                null,
                new ArrayList<>(),
                null,
                null,
                null,
                mockAttachmentKey,
                -1
        );
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
    public void uploadOrDownloadData_shouldDownloadData_Medication() throws DataValidationException.ExpectedFieldViolation, DataValidationException.IdUsageViolation, DataValidationException.InvalidAttachmentPayloadHash {
        // Given
        Medication medication = MedicationBuilder.buildMedication();
        medication.image.get(0).id = "id";
        DecryptedRecord decryptedRecord = new DecryptedRecord<>(
                RECORD_ID,
                medication,
                null,
                new ArrayList<>(),
                null,
                null,
                null,
                mockAttachmentKey,
                -1
        );
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
    public void uploadOrDownloadData_shouldUploadData_Medication() throws DataValidationException.ExpectedFieldViolation, DataValidationException.IdUsageViolation, DataValidationException.InvalidAttachmentPayloadHash {
        // Given
        when(mockCryptoService.generateGCKey()).thenReturn(Single.just(mockAttachmentKey));
        Medication medication = MedicationBuilder.buildMedication();
        DecryptedRecord decryptedRecord = new DecryptedRecord<>(
                null,
                medication,
                null,
                new ArrayList<>(),
                null,
                null,
                null,
                null,
                -1
        );
        decryptedRecord.setIdentifier(RECORD_ID);
        when(mockCryptoService.generateGCKey()).thenReturn(Single.just(mockAttachmentKey));
        List<Pair<Attachment, List<String>>> uploadResult = new ArrayList<>();
        uploadResult.add(new Pair<>(medication.image.get(0), null));
        when(mockAttachmentService.uploadAttachments(any(), eq(mockAttachmentKey), eq(USER_ID))).thenReturn(Single.just(uploadResult));
        when(recordService.getValidHash(medication.image.get(0))).thenReturn(DATA_HASH);

        // When
        DecryptedRecord record = recordService.uploadOrDownloadData(UPLOAD, decryptedRecord, null, USER_ID);

        // Then
        assertThat(record).isEqualTo(decryptedRecord);
        assertThat(record.getAttachmentsKey()).isEqualTo(mockAttachmentKey);

        inOrder.verify(recordService).uploadOrDownloadData(UPLOAD, decryptedRecord, null, USER_ID);
        inOrder.verify(mockCryptoService).generateGCKey();
        inOrder.verify(mockAttachmentService).uploadAttachments(any(), eq(mockAttachmentKey), eq(USER_ID));
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void uploadOrDownloadData_shouldUploadData_Observation() throws DataValidationException.ExpectedFieldViolation, DataValidationException.IdUsageViolation, DataValidationException.InvalidAttachmentPayloadHash {
        // Given
        when(mockCryptoService.generateGCKey()).thenReturn(Single.just(mockAttachmentKey));
        Observation observation = ObservationBuilder.buildObservationWithComponent();
        observation.component.get(0).valueAttachment.id = null;
        DecryptedRecord decryptedRecord = new DecryptedRecord<>(
                null,
                observation,
                null,
                new ArrayList<>(),
                null,
                null,
                null,
                null,
                -1
        );
        decryptedRecord.setIdentifier(RECORD_ID);
        when(mockCryptoService.generateGCKey()).thenReturn(Single.just(mockAttachmentKey));
        when(recordService.getValidHash(observation.valueAttachment)).thenReturn(DATA_HASH);
        List<Pair<Attachment, List<String>>> uploadResult = new ArrayList<>();
        List<String> downscaledIds = new ArrayList<>();
        downscaledIds.add("downscaledId_1");
        downscaledIds.add("downscaledId_2");
        uploadResult.add(new Pair<>(observation.component.get(0).valueAttachment, downscaledIds));
        uploadResult.add(new Pair<>(observation.valueAttachment, downscaledIds));
        when(mockAttachmentService.uploadAttachments(any(), eq(mockAttachmentKey), eq(USER_ID))).thenReturn(Single.just(uploadResult));
        when(recordService.getValidHash(observation.component.get(0).valueAttachment)).thenReturn(DATA_HASH);

        // When
        DecryptedRecord record = recordService.uploadOrDownloadData(UPLOAD, decryptedRecord, null, USER_ID);

        // Then
        assertThat(record).isEqualTo(decryptedRecord);
        assertThat(record.getAttachmentsKey()).isEqualTo(mockAttachmentKey);

        inOrder.verify(recordService).uploadOrDownloadData(UPLOAD, decryptedRecord, null, USER_ID);
        inOrder.verify(mockCryptoService).generateGCKey();
        inOrder.verify(mockAttachmentService).uploadAttachments(any(), eq(mockAttachmentKey), eq(USER_ID));
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void uploadOrDownloadData_shouldDownloadData_Observation() throws DataValidationException.ExpectedFieldViolation, DataValidationException.IdUsageViolation, DataValidationException.InvalidAttachmentPayloadHash {
        // Given
        Observation observation = ObservationBuilder.buildObservationWithComponent();
        observation.component.get(0).valueAttachment.id = "id1";
        observation.valueAttachment.id = "id0";
        DecryptedRecord decryptedRecord = new DecryptedRecord<>(
                RECORD_ID,
                observation,
                null,
                new ArrayList<>(),
                null,
                null,
                null,
                mockAttachmentKey,
                -1
        );
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
    public void uploadOrDownloadData_shouldUploadData_QuestionnaireResponse() throws DataValidationException.ExpectedFieldViolation, DataValidationException.IdUsageViolation, DataValidationException.InvalidAttachmentPayloadHash {
        // Given
        when(mockCryptoService.generateGCKey()).thenReturn(Single.just(mockAttachmentKey));
        QuestionnaireResponse questionnaireResponse = QuestionnaireResponseBuilder.buildQuestionnaireResponse();
        DecryptedRecord decryptedRecord = new DecryptedRecord<>(
                null,
                questionnaireResponse,
                null,
                new ArrayList<>(),
                null,
                null,
                null,
                null,
                -1
        );
        decryptedRecord.setIdentifier(RECORD_ID);
        when(mockCryptoService.generateGCKey()).thenReturn(Single.just(mockAttachmentKey));
        List<Pair<Attachment, List<String>>> uploadResult = new ArrayList<>();
        List<String> downscaledIds = new ArrayList<>();
        downscaledIds.add("downscaledId_1");
        downscaledIds.add("downscaledId_2");
        uploadResult.add(new Pair<>(questionnaireResponse.item.get(0).answer.get(0).valueAttachment, downscaledIds));
        when(mockAttachmentService.uploadAttachments(any(), eq(mockAttachmentKey), eq(USER_ID))).thenReturn(Single.just(uploadResult));
        when(recordService.getValidHash(questionnaireResponse.item.get(0).answer.get(0).valueAttachment)).thenReturn(DATA_HASH);

        // When
        DecryptedRecord record = recordService.uploadOrDownloadData(UPLOAD, decryptedRecord, null, USER_ID);

        // Then
        assertThat(record).isEqualTo(decryptedRecord);
        assertThat(record.getAttachmentsKey()).isEqualTo(mockAttachmentKey);

        inOrder.verify(recordService).uploadOrDownloadData(UPLOAD, decryptedRecord, null, USER_ID);
        inOrder.verify(mockCryptoService).generateGCKey();
        inOrder.verify(mockAttachmentService).uploadAttachments(any(), eq(mockAttachmentKey), eq(USER_ID));
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void uploadOrDownloadData_shouldDownloadData_QuestionnaireResponse() throws DataValidationException.ExpectedFieldViolation, DataValidationException.IdUsageViolation, DataValidationException.InvalidAttachmentPayloadHash {
        // Given
        QuestionnaireResponse questionnaireResponse = QuestionnaireResponseBuilder.buildQuestionnaireResponse();
        questionnaireResponse.item.get(0).answer.get(0).valueAttachment.id = "id";
        DecryptedRecord decryptedRecord = new DecryptedRecord<>(
                RECORD_ID,
                questionnaireResponse,
                null,
                new ArrayList<>(),
                null,
                null,
                null,
                mockAttachmentKey,
                -1
        );
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
    public void uploadOrDownloadData_shouldAppendAdditionalIdentifiers_Patient() throws DataValidationException.IdUsageViolation, DataValidationException.ExpectedFieldViolation, DataValidationException.InvalidAttachmentPayloadHash {
        //given
        Patient patient = PatientBuilder.buildPatient();
        patient.photo.add(AttachmentBuilder.buildAttachment(null));
        Attachment firstAttachment = patient.photo.get(0);
        firstAttachment.title = "image";
        firstAttachment.hash = DATA_HASH;
        Attachment secondAttachment = patient.photo.get(1);
        secondAttachment.title = "pdf";
        secondAttachment.hash = DATA_HASH;

        List<Pair<Attachment, List<String>>> uploadResult = new ArrayList<>();
        uploadResult.add(new Pair<>(firstAttachment, asList(PREVIEW_ID, THUMBNAIL_ID)));
        uploadResult.add(new Pair<>(secondAttachment, null));

        DecryptedRecord<Patient> dummyDecryptedRecord = new DecryptedRecord<>(
                RECORD_ID,
                patient,
                null,
                new ArrayList<>(),
                null,
                null,
                null,
                null,
                -1
        );
        when(mockCryptoService.generateGCKey()).thenReturn(Single.just(mockAttachmentKey));
        when(recordService.getValidHash(patient.photo.get(0))).thenReturn(DATA_HASH);
        when(recordService.getValidHash(secondAttachment)).thenReturn(DATA_HASH);
        when(mockAttachmentService.uploadAttachments(any(), eq(mockAttachmentKey), eq(USER_ID))).thenReturn(Single.fromCallable(() -> {
            firstAttachment.id = ATTACHMENT_ID;
            secondAttachment.id = ATTACHMENT_ID;
            return uploadResult;
        }));

        //when
        DecryptedRecord<Patient> result = recordService.uploadOrDownloadData(UPLOAD, dummyDecryptedRecord, null, USER_ID);

        //then
        Patient pat = result.getResource();
        assertThat(pat).isEqualTo(patient);
        assertThat(pat.identifier).hasSize(1);
        assertThat(pat.identifier.get(0).value).isEqualTo(RecordService.DOWNSCALED_ATTACHMENT_IDS_FMT + "#" + ATTACHMENT_ID + "#" + PREVIEW_ID + "#" + THUMBNAIL_ID);
        assertThat(pat.identifier.get(0).assigner.reference).isEqualTo(PARTNER_ID);
    }

    @Test
    public void uploadOrDownloadData_shouldNotAppendAdditionalIdentifiers_Medication() throws DataValidationException.IdUsageViolation, DataValidationException.ExpectedFieldViolation, DataValidationException.InvalidAttachmentPayloadHash {
        //given
        Medication medication = MedicationBuilder.buildMedication();
        medication.image.add(AttachmentBuilder.buildAttachment(null));
        Attachment firstAttachment = medication.image.get(0);
        firstAttachment.title = "image";
        Attachment secondAttachment = medication.image.get(1);
        secondAttachment.title = "pdf";

        List<Pair<Attachment, List<String>>> uploadResult = new ArrayList<>();
        uploadResult.add(new Pair<>(firstAttachment, asList(PREVIEW_ID, THUMBNAIL_ID)));
        uploadResult.add(new Pair<>(secondAttachment, null));

        DecryptedRecord<Medication> dummyDecryptedRecord = new DecryptedRecord<>(
                RECORD_ID,
                medication,
                null,
                new ArrayList<>(),
                null,
                null,
                null,
                null,
                -1
        );
        when(mockCryptoService.generateGCKey()).thenReturn(Single.just(mockAttachmentKey));
        when(recordService.getValidHash(firstAttachment)).thenReturn(DATA_HASH);
        when(recordService.getValidHash(secondAttachment)).thenReturn(DATA_HASH);
        when(mockAttachmentService.uploadAttachments(any(), eq(mockAttachmentKey), eq(USER_ID))).thenReturn(Single.fromCallable(() -> {
            firstAttachment.id = ATTACHMENT_ID;
            secondAttachment.id = ATTACHMENT_ID;
            return uploadResult;
        }));

        //when
        DecryptedRecord<Medication> result = recordService.uploadOrDownloadData(UPLOAD, dummyDecryptedRecord, null, USER_ID);

        //then
        Medication med = result.getResource();
        assertThat(med).isEqualTo(medication);
    }

    @Test
    public void uploadOrDownloadData_shouldAppendAdditionalIdentifiers_Observation() throws DataValidationException.IdUsageViolation, DataValidationException.ExpectedFieldViolation, DataValidationException.InvalidAttachmentPayloadHash {
        //given
        Observation observation = ObservationBuilder.buildObservationWithComponent();
        observation.component.get(0).valueAttachment.id = null;
        observation.component.add(ObservationBuilder.buildComponent(null, AttachmentBuilder.buildAttachment(null)));
        Attachment firstAttachment = observation.component.get(0).valueAttachment;
        firstAttachment.title = "image";
        firstAttachment.hash = DATA_HASH;
        Attachment secondAttachment = observation.component.get(1).valueAttachment;
        secondAttachment.title = "pdf";
        secondAttachment.hash = DATA_HASH;
        Attachment attachment = observation.valueAttachment;
        attachment.title = "doc";
        attachment.hash = DATA_HASH;

        List<Pair<Attachment, List<String>>> uploadResult = new ArrayList<>();
        uploadResult.add(new Pair<>(firstAttachment, asList(PREVIEW_ID, THUMBNAIL_ID)));
        uploadResult.add(new Pair<>(secondAttachment, null));
        uploadResult.add(new Pair<>(attachment, null));

        DecryptedRecord<Observation> dummyDecryptedRecord = new DecryptedRecord<>(
                RECORD_ID,
                observation,
                null,
                new ArrayList<>(),
                null,
                null,
                null,
                null,
                -1
        );
        when(mockCryptoService.generateGCKey()).thenReturn(Single.just(mockAttachmentKey));
        when(recordService.getValidHash(firstAttachment)).thenReturn(DATA_HASH);
        when(recordService.getValidHash(secondAttachment)).thenReturn(DATA_HASH);
        when(recordService.getValidHash(attachment)).thenReturn(DATA_HASH);
        when(mockAttachmentService.uploadAttachments(any(), eq(mockAttachmentKey), eq(USER_ID))).thenReturn(Single.fromCallable(() -> {
            firstAttachment.id = ATTACHMENT_ID;
            secondAttachment.id = ATTACHMENT_ID;
            attachment.id = ATTACHMENT_ID;
            return uploadResult;
        }));

        //when
        DecryptedRecord<Observation> result = recordService.uploadOrDownloadData(UPLOAD, dummyDecryptedRecord, null, USER_ID);

        //then
        Observation obs = result.getResource();
        assertThat(obs).isEqualTo(observation);
        assertThat(obs.identifier).hasSize(1);
        assertThat(obs.identifier.get(0).value).isEqualTo(RecordService.DOWNSCALED_ATTACHMENT_IDS_FMT + "#" + ATTACHMENT_ID + "#" + PREVIEW_ID + "#" + THUMBNAIL_ID);
        assertThat(obs.identifier.get(0).assigner.reference).isEqualTo(PARTNER_ID);
    }

    @Test
    public void uploadOrDownloadData_shouldAppendAdditionalIdentifiers_QuestionnaireResponse() throws DataValidationException.IdUsageViolation, DataValidationException.ExpectedFieldViolation, DataValidationException.InvalidAttachmentPayloadHash {
        //given
        QuestionnaireResponse questionnaireResponse = QuestionnaireResponseBuilder.buildQuestionnaireResponse();
        questionnaireResponse.item.add(QuestionnaireResponseBuilder.buildItem("", AttachmentBuilder.buildAttachment(null)));
        Attachment firstAttachment = questionnaireResponse.item.get(0).answer.get(0).valueAttachment;
        firstAttachment.title = "image";
        firstAttachment.hash = DATA_HASH;
        Attachment secondAttachment = questionnaireResponse.item.get(1).answer.get(0).valueAttachment;
        secondAttachment.title = "pdf";
        secondAttachment.hash = DATA_HASH;

        List<Pair<Attachment, List<String>>> uploadResult = new ArrayList<>();
        uploadResult.add(new Pair<>(firstAttachment, asList(PREVIEW_ID, THUMBNAIL_ID)));
        uploadResult.add(new Pair<>(secondAttachment, null));

        DecryptedRecord<QuestionnaireResponse> dummyDecryptedRecord = new DecryptedRecord<>(
                RECORD_ID,
                questionnaireResponse,
                null,
                new ArrayList<>(),
                null,
                null,
                null,
                null,
                -1
        );
        when(mockCryptoService.generateGCKey()).thenReturn(Single.just(mockAttachmentKey));
        when(recordService.getValidHash(firstAttachment)).thenReturn(DATA_HASH);
        when(recordService.getValidHash(secondAttachment)).thenReturn(DATA_HASH);
        when(mockAttachmentService.uploadAttachments(any(), eq(mockAttachmentKey), eq(USER_ID))).thenReturn(Single.fromCallable(() -> {
            firstAttachment.id = ATTACHMENT_ID;
            secondAttachment.id = ATTACHMENT_ID;
            return uploadResult;
        }));

        //when
        DecryptedRecord<QuestionnaireResponse> result = recordService.uploadOrDownloadData(UPLOAD, dummyDecryptedRecord, null, USER_ID);

        //then
        QuestionnaireResponse response = result.getResource();
        assertThat(response).isEqualTo(questionnaireResponse);
        assertThat(response.identifier.value).isEqualTo(RecordService.DOWNSCALED_ATTACHMENT_IDS_FMT + "#" + ATTACHMENT_ID + "#" + PREVIEW_ID + "#" + THUMBNAIL_ID);
        assertThat(response.identifier.assigner.reference).isEqualTo(PARTNER_ID);
    }

    @Test
    public void cleanObsoleteAdditionalIdentifiers_shouldCleanObsoleteIdentifiers_Patient() throws DataValidationException.IdUsageViolation {
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

        Patient patient = PatientBuilder.buildPatient();
        patient.photo.get(0).id = ATTACHMENT_ID;
        patient.identifier = identifiers;

        //when
        recordService.cleanObsoleteAdditionalIdentifiers(patient);

        //then
        assertThat(patient.identifier).hasSize(2);
        assertThat(patient.identifier.get(0)).isEqualTo(currentIdentifier);
        assertThat(patient.identifier.get(1)).isEqualTo(otherIdentifier);
    }

    @Test
    public void cleanObsoleteAdditionalIdentifiers_shouldNotCleanObsoleteIdentifiers_Medication() throws DataValidationException.IdUsageViolation {
        //given
        Medication medicine = MedicationBuilder.buildMedication();
        medicine.image.get(0).id = ATTACHMENT_ID;

        //when
        recordService.cleanObsoleteAdditionalIdentifiers(medicine);

        //then
        assert (true);
    }

    @Test
    public void cleanObsoleteAdditionalIdentifiers_shouldCleanObsoleteIdentifiers_Observation() throws DataValidationException.IdUsageViolation {
        //given
        String currentId = ADDITIONAL_ID;
        String obsoleteId = ADDITIONAL_ID.replaceFirst(ATTACHMENT_ID, "obsoleteId");
        String otherId = "otherId";
        String valueId = DOWNSCALED_ATTACHMENT_IDS_FMT + SPLIT_CHAR + "valueAttachment" + SPLIT_CHAR + PREVIEW_ID + SPLIT_CHAR + THUMBNAIL_ID;
        Identifier currentIdentifier = FhirAttachmentHelper.buildIdentifier(currentId, ASSIGNER);
        Identifier obsoleteIdentifier = FhirAttachmentHelper.buildIdentifier(obsoleteId, ASSIGNER);
        Identifier otherIdentifier = FhirAttachmentHelper.buildIdentifier(otherId, ASSIGNER);
        Identifier valueIdentifier = FhirAttachmentHelper.buildIdentifier(valueId, ASSIGNER);
        List<Identifier> identifiers = new ArrayList<>();
        identifiers.add(currentIdentifier);
        identifiers.add(obsoleteIdentifier);
        identifiers.add(otherIdentifier);
        identifiers.add(valueIdentifier);

        Observation obs = ObservationBuilder.buildObservationWithComponent();
        obs.component.get(0).valueAttachment.id = ATTACHMENT_ID;
        obs.valueAttachment.id = "valueAttachment";
        obs.identifier = identifiers;

        //when
        recordService.cleanObsoleteAdditionalIdentifiers(obs);

        //then
        assertThat(obs.identifier).hasSize(3);
        assertThat(obs.identifier.get(0)).isEqualTo(currentIdentifier);
        assertThat(obs.identifier.get(1)).isEqualTo(otherIdentifier);
        assertThat(obs.identifier.get(2)).isEqualTo(valueIdentifier);
    }

    @Test
    public void cleanObsoleteAdditionalIdentifiers_shouldCleanObsoleteIdentifiers_QuestionnaireResponse() throws DataValidationException.IdUsageViolation {
        //given
        String currentId = ADDITIONAL_ID;
        String obsoleteId = ADDITIONAL_ID.replaceFirst(ATTACHMENT_ID, "obsoleteId");
        String otherId = "otherId";
        Identifier currentIdentifier = FhirAttachmentHelper.buildIdentifier(currentId, ASSIGNER);
        Identifier obsoleteIdentifier = FhirAttachmentHelper.buildIdentifier(obsoleteId, ASSIGNER);
        Identifier otherIdentifier = FhirAttachmentHelper.buildIdentifier(otherId, ASSIGNER);


        QuestionnaireResponse response = QuestionnaireResponseBuilder.buildQuestionnaireResponse();
        response.item.get(0).answer.get(0).valueAttachment.id = ATTACHMENT_ID;
        response.identifier = currentIdentifier;

        //when
        recordService.cleanObsoleteAdditionalIdentifiers(response);

        //then
        assertThat(response.identifier).isEqualTo(currentIdentifier);
    }

    @Test
    public void checkForUnsupportedData_shouldReturnSuccessfully_Medication() throws DataRestrictionException.UnsupportedFileType, DataRestrictionException.MaxDataSizeViolation {
        // Given
        Byte[] pdf = new Byte[DataRestriction.DATA_SIZE_MAX_BYTES];
        System.arraycopy(MimeType.PDF.byteSignature()[0], 0, pdf, 0, MimeType.PDF.byteSignature()[0].length);
        Medication medication = MedicationBuilder.buildMedication(unboxByteArray(pdf));

        // When
        recordService.checkDataRestrictions(medication);

        // Then
        inOrder.verify(recordService).checkDataRestrictions(medication);
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void checkForUnsupportedData_shouldReturnSuccessfully_Observation() throws DataRestrictionException.UnsupportedFileType, DataRestrictionException.MaxDataSizeViolation {
        // Given
        Byte[] pdf = new Byte[DataRestriction.DATA_SIZE_MAX_BYTES];
        System.arraycopy(MimeType.PDF.byteSignature()[0], 0, pdf, 0, MimeType.PDF.byteSignature()[0].length);
        Observation observation = ObservationBuilder.buildObservation(unboxByteArray(pdf));

        // When
        recordService.checkDataRestrictions(observation);

        // Then
        inOrder.verify(recordService).checkDataRestrictions(observation);
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void checkForUnsupportedData_shouldReturnSuccessfully_QuestionnaireResponse() throws DataRestrictionException.UnsupportedFileType, DataRestrictionException.MaxDataSizeViolation {
        // Given
        Byte[] pdf = new Byte[DataRestriction.DATA_SIZE_MAX_BYTES];
        System.arraycopy(MimeType.PDF.byteSignature()[0], 0, pdf, 0, MimeType.PDF.byteSignature()[0].length);
        QuestionnaireResponse response = QuestionnaireResponseBuilder.buildQuestionnaireResponse(unboxByteArray(pdf));

        // When
        recordService.checkDataRestrictions(response);

        // Then
        inOrder.verify(recordService).checkDataRestrictions(response);
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void downloadAttachment_shouldDownloadAttachment_Medication() throws InterruptedException, IOException, DataValidationException.ModelVersionNotSupported, DataValidationException.InvalidAttachmentPayloadHash {
        // Given
        when(mockApiService.fetchRecord(ALIAS, USER_ID, RECORD_ID)).thenReturn(Single.just(mockEncryptedRecord));
        Medication medication = MedicationBuilder.buildMedication();
        Attachment attachment = AttachmentBuilder.buildAttachment(ATTACHMENT_ID);
        String secondAttachmentId = "secondId";
        Attachment secondAttachment = AttachmentBuilder.buildAttachment(secondAttachmentId);
        medication.image.set(0, attachment);
        medication.image.add(secondAttachment);
        DecryptedRecord decryptedRecord = new DecryptedRecord<>(
                RECORD_ID,
                medication,
                null,
                new ArrayList<>(),
                null,
                null,
                null,
                mockAttachmentKey,
                -1
        );
        doReturn(decryptedRecord).when(recordService).decryptRecord(mockEncryptedRecord, USER_ID);
        ArrayList<Attachment> attachments = new ArrayList<>();
        attachments.add(attachment);
        attachments.add(secondAttachment);
        when(mockAttachmentService.downloadAttachments(argThat(arg -> arg.contains(attachment)), eq(mockAttachmentKey), eq(USER_ID))).thenReturn(Single.just(attachments));

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
    public void downloadAttachments_shouldDownloadAttachments_Patient() throws IOException, InterruptedException, DataValidationException.ModelVersionNotSupported, DataValidationException.InvalidAttachmentPayloadHash {
        // Given
        when(mockApiService.fetchRecord(ALIAS, USER_ID, RECORD_ID)).thenReturn(Single.just(mockEncryptedRecord));
        Patient patient = PatientBuilder.buildPatient();
        Attachment attachment = AttachmentBuilder.buildAttachment(ATTACHMENT_ID);
        String secondAttachmentId = "secondId";
        Attachment secondAttachment = AttachmentBuilder.buildAttachment(secondAttachmentId);
        patient.photo.set(0, attachment);
        patient.photo.add(secondAttachment);
        DecryptedRecord decryptedRecord = new DecryptedRecord<>(
                RECORD_ID,
                patient,
                null,
                new ArrayList<>(),
                null,
                null,
                null,
                mockAttachmentKey,
                -1
        );
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
    public void updateRecord_shouldThrow_forUnsupportedData_Observation() throws DataRestrictionException.UnsupportedFileType, DataRestrictionException.MaxDataSizeViolation {
        // Given
        byte[] invalidData = {0x00};
        Observation obs = ObservationBuilder.buildObservation(invalidData);

        // When
        try {
            recordService.updateRecord(obs, USER_ID).test().await();
            fail("Exception expected!");
        } catch (Exception ex) {
            // Then
            assertThat(ex).isInstanceOf(DataRestrictionException.UnsupportedFileType.class);
        }

        inOrder.verify(recordService).updateRecord(obs, USER_ID);
        inOrder.verify(recordService).checkDataRestrictions(obs);
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void updateRecord_shouldThrow_forFileSizeLimitationBreach_QuestionnaireResponse() throws DataRestrictionException.UnsupportedFileType, DataRestrictionException.MaxDataSizeViolation {
        // Given
        Byte[] invalidSizePdf = new Byte[DataRestriction.DATA_SIZE_MAX_BYTES + 1];
        System.arraycopy(MimeType.PDF.byteSignature()[0], 0, invalidSizePdf, 0, MimeType.PDF.byteSignature()[0].length);
        QuestionnaireResponse response = QuestionnaireResponseBuilder.buildQuestionnaireResponse(unboxByteArray(invalidSizePdf));

        // When
        try {
            recordService.updateRecord(response, USER_ID).test().await();
            fail("Exception expected!");
        } catch (Exception ex) {
            // Then
            assertThat(ex).isInstanceOf(DataRestrictionException.MaxDataSizeViolation.class);
        }

        inOrder.verify(recordService).updateRecord(response, USER_ID);
        inOrder.verify(recordService).checkDataRestrictions(response);
        inOrder.verifyNoMoreInteractions();
    }


    static byte[] unboxByteArray(Byte[] array) {
        byte[] result = new byte[array.length];

        int i = 0;
        for (Byte b : array) result[i++] = (b == null ? 0 : b);

        return result;
    }
}
