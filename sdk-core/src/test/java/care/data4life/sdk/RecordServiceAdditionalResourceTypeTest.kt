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
package care.data4life.sdk

import care.data4life.fhir.stu3.model.Attachment
import care.data4life.fhir.stu3.model.DomainResource
import care.data4life.fhir.stu3.model.Identifier
import care.data4life.fhir.stu3.util.FhirAttachmentHelper
import care.data4life.sdk.config.DataRestriction.DATA_SIZE_MAX_BYTES
import care.data4life.sdk.config.DataRestrictionException
import care.data4life.sdk.lang.DataValidationException
import care.data4life.sdk.model.DownloadType
import care.data4life.sdk.network.model.DecryptedRecord
import care.data4life.sdk.test.util.AttachmentBuilder
import care.data4life.sdk.test.util.MedicationBuilder
import care.data4life.sdk.test.util.ObservationBuilder
import care.data4life.sdk.test.util.PatientBuilder
import care.data4life.sdk.test.util.QuestionnaireBuilder
import care.data4life.sdk.test.util.QuestionnaireResponseBuilder
import care.data4life.sdk.util.MimeType
import com.google.common.truth.Truth
import io.reactivex.Single
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers
import org.mockito.Mockito
import java.io.IOException
import java.util.*

class RecordServiceAdditionalResourceTypeTest : RecordServiceTestBase() {
    @Before
    fun setup() {
        init()
    }

    @After
    fun tearDown() {
        stop()
    }

    @Test
    fun extractUploadData_shouldReturnExtractedData_Patient() {
        // Given
        val patient = PatientBuilder.buildPatient()

        // When
        val data = recordService.extractUploadData(patient)

        // Then
        Truth.assertThat(data).hasSize(1)
        Truth.assertThat(data!![patient.photo!![0]]).isEqualTo(DATA)
        inOrder.verify(recordService).extractUploadData(patient)
        inOrder.verifyNoMoreInteractions()
    }

    @Test
    fun extractUploadData_shouldReturnExtractedData_Observation() {
        // Given
        val observation = ObservationBuilder.buildObservationWithComponent()

        // When
        val data = recordService.extractUploadData(observation)

        // Then
        Truth.assertThat(data).hasSize(2)
        Truth.assertThat(data!![observation.component!![0].valueAttachment]).isEqualTo(DATA)
        inOrder.verify(recordService).extractUploadData(observation)
        inOrder.verifyNoMoreInteractions()
    }

    @Test
    fun extractUploadData_shouldReturnExtractedData_Observation_Component() {
        // Given
        val observation = ObservationBuilder.buildObservationWithComponent()

        // When
        val data = recordService.extractUploadData(observation)

        // Then
        Truth.assertThat(data).hasSize(2)
        Truth.assertThat(data!![observation.component!![0].valueAttachment]).isEqualTo(DATA)
        inOrder.verify(recordService).extractUploadData(observation)
        inOrder.verifyNoMoreInteractions()
    }

    @Test
    fun extractUploadData_shouldReturnExtractedData_Medication() {
        // Given
        val medication = MedicationBuilder.buildMedication()

        // When
        val data = recordService.extractUploadData(medication)

        // Then
        Truth.assertThat(data).hasSize(1)
        Truth.assertThat(data!![medication.image!![0]]).isEqualTo(DATA)
        inOrder.verify(recordService).extractUploadData(medication)
        inOrder.verifyNoMoreInteractions()
    }

    @Test
    fun extractUploadData_shouldReturnExtractedData_Questionnaire() {
        // Given
        val questionnaire = QuestionnaireBuilder.buildQuestionnaire()

        // When
        val data = recordService.extractUploadData(questionnaire)

        // Then
        Truth.assertThat(data).hasSize(1)
        Truth.assertThat(data!![questionnaire.item!![0].initialAttachment]).isEqualTo(DATA)
        inOrder.verify(recordService).extractUploadData(questionnaire)
        inOrder.verifyNoMoreInteractions()
    }

    @Test
    fun extractUploadData_shouldReturnExtractedData_QuestionnaireResponse() {
        // Given
        val questionnaireResponse = QuestionnaireResponseBuilder.buildQuestionnaireResponse()

        // When
        val data = recordService.extractUploadData(questionnaireResponse)

        // Then
        Truth.assertThat(data).hasSize(1)
        Truth.assertThat(data!![questionnaireResponse.item!![0].answer!![0].valueAttachment]).isEqualTo(DATA)
        inOrder.verify(recordService).extractUploadData(questionnaireResponse)
        inOrder.verifyNoMoreInteractions()
    }

    @Test
    fun extractUploadData_shouldReturnNull_whenPhotoIsNull() {
        // Given
        val patient = PatientBuilder.buildPatient()
        patient.photo = null

        // When
        val data = recordService.extractUploadData(patient)

        // Then
        Truth.assertThat(data).isNull()
        inOrder.verify(recordService).extractUploadData(patient)
        inOrder.verifyNoMoreInteractions()
    }

    @Test
    fun extractUploadData_shouldNotReturnNull_whenComponentIsNull() {
        // Given
        val observation = ObservationBuilder.buildObservationWithComponent()
        observation.component = null

        // When
        val data = recordService.extractUploadData(observation)

        // Then
        Truth.assertThat(data).hasSize(1)
        inOrder.verify(recordService).extractUploadData(observation)
        inOrder.verifyNoMoreInteractions()
    }

    @Test
    fun extractUploadData_shouldReturnNull_whenObservationAttachmentIsNull() {
        // Given
        val observation = ObservationBuilder.buildObservation()
        observation.valueAttachment = null

        // When
        val data = recordService.extractUploadData(observation)

        // Then
        Truth.assertThat(data).isNull()
        inOrder.verify(recordService).extractUploadData(observation)
        inOrder.verifyNoMoreInteractions()
    }

    @Test
    fun extractUploadData_shouldReturnNull_whenQuestionnaireItemIsNull() {
        // Given
        val questionnaire = QuestionnaireBuilder.buildQuestionnaire()
        questionnaire.item = null

        // When
        val data = recordService.extractUploadData(questionnaire)

        // Then
        Truth.assertThat(data).isNull()
        inOrder.verify(recordService).extractUploadData(questionnaire)
        inOrder.verifyNoMoreInteractions()
    }

    @Test
    fun extractUploadData_shouldReturnNull_whenQuestionnaireResponseItemIsNull() {
        // Given
        val response = QuestionnaireResponseBuilder.buildQuestionnaireResponse()
        response.item = null

        // When
        val data = recordService.extractUploadData(response)

        // Then
        Truth.assertThat(data).isNull()
        inOrder.verify(recordService).extractUploadData(response)
        inOrder.verifyNoMoreInteractions()
    }

    @Test
    fun extractUploadData_shouldReturnValueAttachment_whenAttachmentIsNull_Observation() {
        // Given
        val observation = ObservationBuilder.buildObservationWithComponent()
        val component = ObservationBuilder.buildComponent(null, null)
        observation.component!![0] = component

        // When
        val data = recordService.extractUploadData(observation)

        // Then
        Truth.assertThat(data).isNotNull()
        inOrder.verify(recordService).extractUploadData(observation)
        inOrder.verifyNoMoreInteractions()
    }

    @Test
    fun extractUploadData_shouldReturnNull_whenAllAttachmentIsNull_Observation() {
        // Given
        val observation = ObservationBuilder.buildObservationWithComponent()
        val component = ObservationBuilder.buildComponent(null, null)
        observation.component!![0] = component
        observation.valueAttachment = null

        // When
        val data = recordService.extractUploadData(observation)

        // Then
        Truth.assertThat(data).isNull()
        inOrder.verify(recordService).extractUploadData(observation)
        inOrder.verifyNoMoreInteractions()
    }

    @Test
    fun extractUploadData_shouldReturnNull_whenAttachmentIsNull_QuestionnaireResponse() {
        // Given
        val questionnaireResponse = QuestionnaireResponseBuilder.buildQuestionnaireResponse()
        val questionnaireResponseItem = QuestionnaireResponseBuilder.buildItem("", null)
        questionnaireResponse.item!![0] = questionnaireResponseItem
        // When
        val data = recordService.extractUploadData(questionnaireResponse)

        // Then
        Truth.assertThat(data).isNull()
        inOrder.verify(recordService).extractUploadData(questionnaireResponse)
        inOrder.verifyNoMoreInteractions()
    }

    @Test
    fun extractUploadData_shouldReturnNull_whenAnswerIsNull_QuestionnaireResponse() {
        // Given
        val questionnaireResponse = QuestionnaireResponseBuilder.buildQuestionnaireResponse()
        questionnaireResponse.item!![0].answer!![0] = null

        // When
        val data = recordService.extractUploadData(questionnaireResponse)

        // Then
        Truth.assertThat(data).isNull()
        inOrder.verify(recordService).extractUploadData(questionnaireResponse)
        inOrder.verifyNoMoreInteractions()
    }

    @Test
    fun removeOrRestoreUploadData_shouldRemoveUploadData_fromPatient() {
        // Given
        val patient = PatientBuilder.buildPatient()
        val decryptedRecord  = DecryptedRecord(
                null,
                patient,
                null,
                arrayListOf(),
                null,
                null,
                null,
                null,
                -1
        )

        // When
        val record = recordService.removeOrRestoreUploadData(
                RecordService.RemoveRestoreOperation.REMOVE, 
                decryptedRecord,
                patient, 
                null)

        // Then
        Truth.assertThat(record).isEqualTo(decryptedRecord)
        Truth.assertThat(record.resource).isEqualTo(patient)
        Truth.assertThat(patient.photo!![0].data).isNull()
        inOrder.verify(recordService).removeOrRestoreUploadData(
                RecordService.RemoveRestoreOperation.REMOVE, 
                decryptedRecord, 
                patient, 
                null
        )
        inOrder.verifyNoMoreInteractions()
    }

    @Test
    fun removeOrRestoreUploadData_shouldRemoveUploadData_fromObservation() {
        // Given
        val observation = ObservationBuilder.buildObservationWithComponent()
        val decryptedRecord  = DecryptedRecord(
                null,
                observation,
                null,
                arrayListOf(),
                null,
                null,
                null,
                null,
                -1
        )

        // When
        val record = recordService.removeOrRestoreUploadData(
                RecordService.RemoveRestoreOperation.REMOVE,
                decryptedRecord, 
                observation,
                null
        )

        // Then
        Truth.assertThat(record).isEqualTo(decryptedRecord)
        Truth.assertThat(record.resource).isEqualTo(observation)
        Truth.assertThat(observation.component!![0].valueAttachment!!.data).isNull()
        Truth.assertThat(observation.valueAttachment!!.data).isNull()
        inOrder.verify(recordService).removeOrRestoreUploadData(
                RecordService.RemoveRestoreOperation.REMOVE, 
                decryptedRecord, 
                observation, 
                null
        )
        inOrder.verifyNoMoreInteractions()
    }

    @Test
    fun removeOrRestoreUploadData_shouldRemoveUploadData_fromQuestionnaireResponse() {
        // Given
        val questionnaireResponse = QuestionnaireResponseBuilder.buildQuestionnaireResponse()
        val decryptedRecord  = DecryptedRecord(
                null,
                questionnaireResponse,
                null,
                arrayListOf(),
                null,
                null,
                null,
                null,
                -1
        )

        // When
        val record = recordService.removeOrRestoreUploadData(
                RecordService.RemoveRestoreOperation.REMOVE, 
                decryptedRecord,
                questionnaireResponse,
                null
        )

        // Then
        Truth.assertThat(record).isEqualTo(decryptedRecord)
        Truth.assertThat(record.resource).isEqualTo(questionnaireResponse)
        Truth.assertThat(questionnaireResponse.item!![0].answer!![0].valueAttachment!!.data).isNull()
        inOrder.verify(recordService).removeOrRestoreUploadData(
                RecordService.RemoveRestoreOperation.REMOVE, 
                decryptedRecord, 
                questionnaireResponse, 
                null
        )
        inOrder.verifyNoMoreInteractions()
    }

    @Test
    fun removeOrRestoreUploadData_shouldRestoreUploadData_fromPatient() {
        // Given
        val patient = PatientBuilder.buildPatient()
        patient.photo!![0].data = null
        val decryptedRecord  = DecryptedRecord(
                null,
                null,
                null,
                arrayListOf(),
                null,
                null,
                null,
                null,
                -1
        )
        val uploadData = HashMap<Attachment, String?>()
        uploadData[patient.photo!![0]] = DATA

        // When
        @Suppress("UNCHECKED_CAST")
        val record = recordService.removeOrRestoreUploadData(
                RecordService.RemoveRestoreOperation.RESTORE,
                decryptedRecord as DecryptedRecord<DomainResource>,
                patient,
                uploadData 
        )

        // Then
        Truth.assertThat(record).isEqualTo(decryptedRecord)
        Truth.assertThat(record.resource).isEqualTo(patient)
        Truth.assertThat(patient.photo!![0].data).isEqualTo(DATA)
        inOrder.verify(recordService).removeOrRestoreUploadData(
                RecordService.RemoveRestoreOperation.RESTORE,
                decryptedRecord, 
                patient,
                uploadData
        )
        inOrder.verifyNoMoreInteractions()
    }

    @Test
    fun removeOrRestoreUploadData_shouldRestoreUploadData_fromObservation() {
        // Given
        val observation = ObservationBuilder.buildObservationWithComponent()
        observation.component!![0].valueAttachment!!.data = null
        val decryptedRecord  = DecryptedRecord(
                null,
                null,
                null,
                arrayListOf(),
                null,
                null,
                null,
                null,
                -1
        )
        val uploadData = HashMap<Attachment?, String?>()
        uploadData[observation.component!![0].valueAttachment] = DATA

        // When
        @Suppress("UNCHECKED_CAST")
        val record = recordService.removeOrRestoreUploadData(
                RecordService.RemoveRestoreOperation.RESTORE,
                decryptedRecord as DecryptedRecord<DomainResource>,
                observation,
                uploadData as HashMap<Attachment, String?>
        )

        // Then
        Truth.assertThat(record).isEqualTo(decryptedRecord)
        Truth.assertThat(record.resource).isEqualTo(observation)
        Truth.assertThat(observation.component!![0].valueAttachment!!.data).isEqualTo(DATA)
        inOrder.verify(recordService).removeOrRestoreUploadData(
                RecordService.RemoveRestoreOperation.RESTORE,
                decryptedRecord,
                observation, 
                uploadData
        )
        inOrder.verifyNoMoreInteractions()
    }

    @Test
    fun removeOrRestoreUploadData_shouldRestoreUploadData_fromQuestionnaireResponse() {
        // Given
        val questionnaireResponse = QuestionnaireResponseBuilder.buildQuestionnaireResponse()
        questionnaireResponse.item!![0].answer!![0].valueAttachment!!.data = null
        val decryptedRecord  = DecryptedRecord(
                null,
                null,
                null,
                arrayListOf(), null,
                null,
                null,
                null,
                -1
        )
        val uploadData = HashMap<Attachment?, String?>()
        uploadData[questionnaireResponse.item!![0].answer!![0].valueAttachment] = DATA

        // When
        @Suppress("UNCHECKED_CAST") 
        val record = recordService.removeOrRestoreUploadData(
                RecordService.RemoveRestoreOperation.RESTORE,
                decryptedRecord as DecryptedRecord<DomainResource>, 
                questionnaireResponse,
                uploadData as HashMap<Attachment, String?>
        )

        // Then
        Truth.assertThat(record).isEqualTo(decryptedRecord)
        Truth.assertThat(record.resource).isEqualTo(questionnaireResponse)
        Truth.assertThat(questionnaireResponse.item!![0].answer!![0].valueAttachment!!.data).isEqualTo(DATA)
        inOrder.verify(recordService).removeOrRestoreUploadData(RecordService.RemoveRestoreOperation.RESTORE, decryptedRecord, questionnaireResponse, uploadData)
        inOrder.verifyNoMoreInteractions()
    }

    @Test
    @Throws(DataValidationException.IdUsageViolation::class)
    fun cleanObsoleteAdditionalIdentifiers_shouldCleanObsoleteIdentifiers_Patient() {
        //given
        val currentId = ADDITIONAL_ID
        val obsoleteId = ADDITIONAL_ID.replaceFirst(ATTACHMENT_ID.toRegex(), "obsoleteId")
        val otherId = "otherId"
        val currentIdentifier = FhirAttachmentHelper.buildIdentifier(currentId, ASSIGNER)
        val obsoleteIdentifier = FhirAttachmentHelper.buildIdentifier(obsoleteId, ASSIGNER)
        val otherIdentifier = FhirAttachmentHelper.buildIdentifier(otherId, ASSIGNER)
        val identifiers: MutableList<Identifier> = arrayListOf()
        identifiers.add(currentIdentifier)
        identifiers.add(obsoleteIdentifier)
        identifiers.add(otherIdentifier)
        val patient = PatientBuilder.buildPatient()
        patient.photo!![0].id = ATTACHMENT_ID
        patient.identifier = identifiers

        //when
        recordService.cleanObsoleteAdditionalIdentifiers(patient)

        //then
        Truth.assertThat(patient.identifier).hasSize(2)
        Truth.assertThat(patient.identifier!![0]).isEqualTo(currentIdentifier)
        Truth.assertThat(patient.identifier!![1]).isEqualTo(otherIdentifier)
    }

    @Test
    @Throws(DataValidationException.IdUsageViolation::class)
    fun cleanObsoleteAdditionalIdentifiers_shouldNotCleanObsoleteIdentifiers_Medication() {
        //given
        val medicine = MedicationBuilder.buildMedication()
        medicine.image!![0].id = ATTACHMENT_ID

        //when
        recordService.cleanObsoleteAdditionalIdentifiers(medicine)
        assert(true)
    }

    @Test
    @Throws(DataValidationException.IdUsageViolation::class)
    fun cleanObsoleteAdditionalIdentifiers_shouldCleanObsoleteIdentifiers_Observation() {
        //given
        val currentId = ADDITIONAL_ID
        val obsoleteId = ADDITIONAL_ID.replaceFirst(ATTACHMENT_ID.toRegex(), "obsoleteId")
        val otherId = "otherId"
        val valueId = RecordService.DOWNSCALED_ATTACHMENT_IDS_FMT + RecordService.SPLIT_CHAR +
                "valueAttachment" + RecordService.SPLIT_CHAR + PREVIEW_ID +
                RecordService.SPLIT_CHAR + THUMBNAIL_ID
        val currentIdentifier = FhirAttachmentHelper.buildIdentifier(currentId, ASSIGNER)
        val obsoleteIdentifier = FhirAttachmentHelper.buildIdentifier(obsoleteId, ASSIGNER)
        val otherIdentifier = FhirAttachmentHelper.buildIdentifier(otherId, ASSIGNER)
        val valueIdentifier = FhirAttachmentHelper.buildIdentifier(valueId, ASSIGNER)
        val identifiers: MutableList<Identifier> = arrayListOf()
        identifiers.add(currentIdentifier)
        identifiers.add(obsoleteIdentifier)
        identifiers.add(otherIdentifier)
        identifiers.add(valueIdentifier)
        val obs = ObservationBuilder.buildObservationWithComponent()
        obs.component!![0].valueAttachment!!.id = ATTACHMENT_ID
        obs.valueAttachment!!.id = "valueAttachment"
        obs.identifier = identifiers

        //when
        recordService.cleanObsoleteAdditionalIdentifiers(obs)

        //then
        Truth.assertThat(obs.identifier).hasSize(3)
        Truth.assertThat(obs.identifier!![0]).isEqualTo(currentIdentifier)
        Truth.assertThat(obs.identifier!![1]).isEqualTo(otherIdentifier)
        Truth.assertThat(obs.identifier!![2]).isEqualTo(valueIdentifier)
    }

    @Test
    @Throws(DataValidationException.IdUsageViolation::class)
    fun cleanObsoleteAdditionalIdentifiers_shouldCleanObsoleteIdentifiers_QuestionnaireResponse() {
        //given
        val currentId = ADDITIONAL_ID
        val currentIdentifier = FhirAttachmentHelper.buildIdentifier(currentId, ASSIGNER)
        val response = QuestionnaireResponseBuilder.buildQuestionnaireResponse()
        response.item!![0].answer!![0].valueAttachment!!.id = ATTACHMENT_ID
        response.identifier = currentIdentifier

        //when
        recordService.cleanObsoleteAdditionalIdentifiers(response)

        //then
        Truth.assertThat(response.identifier).isEqualTo(currentIdentifier)
    }

    @Test
    @Throws(DataRestrictionException.UnsupportedFileType::class, DataRestrictionException.MaxDataSizeViolation::class)
    fun checkForUnsupportedData_shouldReturnSuccessfully_Medication() {
        // Given
        val pdf = arrayOfNulls<Byte>(DATA_SIZE_MAX_BYTES)
        System.arraycopy(
                MimeType.PDF.byteSignature()[0] as Any,
                0,
                pdf,
                0,
                MimeType.PDF.byteSignature()[0]?.size!!
        )
        val medication = MedicationBuilder.buildMedication(unboxByteArray(pdf))

        // When
        recordService.checkDataRestrictions(medication)

        // Then
        inOrder.verify(recordService).checkDataRestrictions(medication)
        inOrder.verifyNoMoreInteractions()
    }

    @Test
    @Throws(DataRestrictionException.UnsupportedFileType::class, DataRestrictionException.MaxDataSizeViolation::class)
    fun checkForUnsupportedData_shouldReturnSuccessfully_Observation() {
        // Given
        val pdf = arrayOfNulls<Byte>(DATA_SIZE_MAX_BYTES)
        System.arraycopy(
                MimeType.PDF.byteSignature()[0] as Any,
                0,
                pdf,
                0,
                MimeType.PDF.byteSignature()[0]?.size!!
        )
        val observation = ObservationBuilder.buildObservation(unboxByteArray(pdf))

        // When
        recordService.checkDataRestrictions(observation)

        // Then
        inOrder.verify(recordService).checkDataRestrictions(observation)
        inOrder.verifyNoMoreInteractions()
    }

    @Test
    @Throws(DataRestrictionException.UnsupportedFileType::class, DataRestrictionException.MaxDataSizeViolation::class)
    fun checkForUnsupportedData_shouldReturnSuccessfully_QuestionnaireResponse() {
        // Given
        val pdf = arrayOfNulls<Byte>(DATA_SIZE_MAX_BYTES)
        System.arraycopy(
                MimeType.PDF.byteSignature()[0] as Any,
                0,
                pdf,
                0,
                MimeType.PDF.byteSignature()[0]?.size!!
        )
        val response = QuestionnaireResponseBuilder.buildQuestionnaireResponse(unboxByteArray(pdf))

        // When
        recordService.checkDataRestrictions(response)

        // Then
        inOrder.verify(recordService).checkDataRestrictions(response)
        inOrder.verifyNoMoreInteractions()
    }

    @Test
    @Throws(InterruptedException::class,
            IOException::class,
            DataValidationException.ModelVersionNotSupported::class,
            DataValidationException.InvalidAttachmentPayloadHash::class)
    fun downloadAttachment_shouldDownloadAttachment_Medication() {
        // Given
        val medication = MedicationBuilder.buildMedication()
        val attachment = AttachmentBuilder.buildAttachment(ATTACHMENT_ID)
        val secondAttachmentId = "secondId"
        val secondAttachment = AttachmentBuilder.buildAttachment(secondAttachmentId)
        medication.image!![0] = attachment
        medication.image!!.add(secondAttachment)
        val decryptedRecord  = DecryptedRecord(
                RECORD_ID,
                medication,
                null,
                arrayListOf(),
                null,
                null,
                null,
                mockAttachmentKey,
                -1
        )

        val attachments = ArrayList<Attachment>()
        attachments.add(attachment)
        attachments.add(secondAttachment)

        Mockito.`when`(mockApiService.fetchRecord(ALIAS, USER_ID, RECORD_ID))
                .thenReturn(Single.just(mockEncryptedRecord))
        Mockito.doReturn(decryptedRecord).`when`(recordService)
                .decryptRecord<DomainResource>(mockEncryptedRecord, USER_ID)
        Mockito.`when`(mockAttachmentService.downloadAttachments(
                ArgumentMatchers.argThat { arg -> arg.contains(attachment) },
                ArgumentMatchers.eq(mockAttachmentKey),
                ArgumentMatchers.eq(USER_ID))
        ).thenReturn(Single.just(attachments))

        // when
        val attachmentIds = listOf(ATTACHMENT_ID, secondAttachmentId)
        val test = recordService.downloadAttachments(RECORD_ID, attachmentIds, USER_ID, DownloadType.Full).test().await()

        // then
        val result = test
                .assertNoErrors()
                .assertComplete()
                .assertValue(attachments)
                .values()[0]
        Truth.assertThat(result[0].id).isEqualTo(ATTACHMENT_ID)
        Truth.assertThat(result[1].id).isEqualTo(secondAttachmentId)
    }

    @Test
    @Throws(IOException::class,
            InterruptedException::class,
            DataValidationException.ModelVersionNotSupported::class,
            DataValidationException.InvalidAttachmentPayloadHash::class)
    fun downloadAttachments_shouldDownloadAttachments_Patient() {
        // Given
        Mockito.`when`(mockApiService.fetchRecord(ALIAS, USER_ID, RECORD_ID)).thenReturn(Single.just(mockEncryptedRecord))
        val patient = PatientBuilder.buildPatient()
        val attachment = AttachmentBuilder.buildAttachment(ATTACHMENT_ID)
        val secondAttachmentId = "secondId"
        val secondAttachment = AttachmentBuilder.buildAttachment(secondAttachmentId)
        patient.photo!![0] = attachment
        patient.photo!!.add(secondAttachment)
        val decryptedRecord  = DecryptedRecord(
                RECORD_ID,
                patient,
                null,
                arrayListOf(),
                null,
                null,
                null,
                mockAttachmentKey,
                -1
        )
        Mockito.doReturn(decryptedRecord).`when`(recordService).decryptRecord<DomainResource>(mockEncryptedRecord, USER_ID)
        val attachments = ArrayList<Attachment>()
        attachments.add(attachment)
        attachments.add(secondAttachment)
        Mockito.`when`(mockAttachmentService.downloadAttachments(
                ArgumentMatchers.argThat { arg -> arg.containsAll(listOf(attachment, secondAttachment)) },
                ArgumentMatchers.eq(mockAttachmentKey),
                ArgumentMatchers.eq(USER_ID))
        ).thenReturn(Single.just(attachments))

        // when
        val attachmentIds = listOf(ATTACHMENT_ID, secondAttachmentId)
        val test = recordService.downloadAttachments(
                RECORD_ID,
                attachmentIds,
                USER_ID,
                DownloadType.Full
        ).test().await()

        // then
        val result = test
                .assertNoErrors()
                .assertComplete()
                .assertValue(attachments)
                .values()[0]
        Truth.assertThat(result[0].id).isEqualTo(ATTACHMENT_ID)
        Truth.assertThat(result[1].id).isEqualTo(secondAttachmentId)
    }

    @Test
    @Throws(DataRestrictionException.UnsupportedFileType::class, DataRestrictionException.MaxDataSizeViolation::class)
    fun updateRecord_shouldThrow_forUnsupportedData_Observation() {
        // Given
        val invalidData = byteArrayOf(0x00)
        val obs = ObservationBuilder.buildObservation(invalidData)

        // When
        try {
            recordService.updateRecord(obs, USER_ID).test().await()
            Assert.fail("Exception expected!")
        } catch (ex: Exception) {
            // Then
            Truth.assertThat(ex).isInstanceOf(DataRestrictionException.UnsupportedFileType::class.java)
        }
        inOrder.verify(recordService).updateRecord(obs, USER_ID)
        inOrder.verify(recordService).checkDataRestrictions(obs)
        inOrder.verifyNoMoreInteractions()
    }

    @Test
    @Throws(DataRestrictionException.UnsupportedFileType::class, DataRestrictionException.MaxDataSizeViolation::class)
    fun updateRecord_shouldThrow_forFileSizeLimitationBreach_QuestionnaireResponse() {
        // Given
        val invalidSizePdf = arrayOfNulls<Byte>(DATA_SIZE_MAX_BYTES + 1)
        System.arraycopy(
                MimeType.PDF.byteSignature()[0] as Any,
                0,
                invalidSizePdf,
                0,
                MimeType.PDF.byteSignature()[0]?.size!!
        )
        val response = QuestionnaireResponseBuilder.buildQuestionnaireResponse(unboxByteArray(invalidSizePdf))

        // When
        try {
            recordService.updateRecord(response, USER_ID).test().await()
            Assert.fail("Exception expected!")
        } catch (ex: Exception) {
            // Then
            Truth.assertThat(ex).isInstanceOf(DataRestrictionException.MaxDataSizeViolation::class.java)
        }
        inOrder.verify(recordService).updateRecord(response, USER_ID)
        inOrder.verify(recordService).checkDataRestrictions(response)
        inOrder.verifyNoMoreInteractions()
    }

    companion object {
        fun unboxByteArray(array: Array<Byte?>): ByteArray {
            val result = ByteArray(array.size)
            var i = 0
            for (b in array) result[i++] = b ?: 0
            return result
        }
    }
}
