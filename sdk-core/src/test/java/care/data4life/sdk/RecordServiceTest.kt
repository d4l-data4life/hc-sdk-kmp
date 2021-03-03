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

import care.data4life.crypto.GCKey
import care.data4life.fhir.r4.FhirR4Parser
import care.data4life.fhir.stu3.model.Attachment
import care.data4life.fhir.stu3.model.CarePlan
import care.data4life.fhir.stu3.model.DocumentReference
import care.data4life.fhir.stu3.model.DocumentReference.DocumentReferenceContent
import care.data4life.fhir.stu3.model.Identifier
import care.data4life.fhir.stu3.model.Organization
import care.data4life.sdk.attachment.ThumbnailService.Companion.SPLIT_CHAR
import care.data4life.sdk.config.DataRestriction.DATA_SIZE_MAX_BYTES
import care.data4life.sdk.config.DataRestrictionException
import care.data4life.sdk.fhir.Fhir3Attachment
import care.data4life.sdk.fhir.Fhir3AttachmentHelper
import care.data4life.sdk.fhir.Fhir3Identifier
import care.data4life.sdk.fhir.Fhir3Resource
import care.data4life.sdk.fhir.Fhir4Attachment
import care.data4life.sdk.fhir.Fhir4AttachmentHelper
import care.data4life.sdk.fhir.Fhir4Resource
import care.data4life.sdk.lang.D4LException
import care.data4life.sdk.lang.DataValidationException
import care.data4life.sdk.model.DownloadType
import care.data4life.sdk.model.RecordMapper
import care.data4life.sdk.model.definitions.BaseRecord
import care.data4life.sdk.network.model.DecryptedRecord
import care.data4life.sdk.network.model.EncryptedRecord
import care.data4life.sdk.network.model.definitions.DecryptedBaseRecord
import care.data4life.sdk.network.model.definitions.DecryptedCustomDataRecord
import care.data4life.sdk.network.model.definitions.DecryptedFhir3Record
import care.data4life.sdk.network.model.definitions.DecryptedFhir4Record
import care.data4life.sdk.test.util.AttachmentBuilder
import care.data4life.sdk.util.Base64.encodeToString
import care.data4life.sdk.util.MimeType
import care.data4life.sdk.wrapper.SdkIdentifierFactory
import care.data4life.sdk.wrapper.WrapperContract
import com.google.common.truth.Truth
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkClass
import io.mockk.spyk
import io.mockk.verify
import io.reactivex.Completable
import io.reactivex.Single
import junit.framework.Assert.assertSame
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito
import org.mockito.Mockito.times
import org.threeten.bp.LocalDate
import org.threeten.bp.LocalDateTime
import java.io.IOException
import java.nio.charset.StandardCharsets
import care.data4life.fhir.r4.model.Patient as Fhir4Patient

class RecordServiceTest : RecordServiceTestBase() {

    @Before
    fun setup() {
        init()
    }

    @After
    fun tearDown() {
        stop()
    }

    private fun getResource(
            resourceName: String
    ): String = this::class.java.getResource(resourceName).readText(StandardCharsets.UTF_8)

    //region utility methods
    @Test
    fun `Given, extractUploadData is called with a non FhirResource, it returns null`() {
        // When
        val data = recordService.extractUploadData("something")
        // Then
        Truth.assertThat(data).isNull()
    }


    @Test
    fun extractUploadData_shouldReturnExtractedData_Fhir3() {
        // Given
        val document = buildDocumentReference()

        // When
        val data = recordService.extractUploadData(document)

        // Then
        Truth.assertThat(data).hasSize(1)
        Truth.assertThat(data!![document.content[0].attachment]).isEqualTo(DATA)
        inOrder.verify(recordService).extractUploadData(document)
        inOrder.verifyNoMoreInteractions()
    }

    @Test
    fun extractUploadData_shouldReturnExtractedData_Fhir4() {
        // Given
        val document = buildDocumentReferenceFhir4()

        // When
        val data = recordService.extractUploadData(document)

        // Then
        Truth.assertThat(data).hasSize(1)
        Truth.assertThat(data!![document.content[0].attachment]).isEqualTo(DATA)
        inOrder.verify(recordService).extractUploadData(document)
        inOrder.verifyNoMoreInteractions()
    }

    @Test
    fun extractUploadData_shouldReturnNull_whenInadequateResourceProvided() {
        // Given
        val organization = Organization()

        // When
        val data = recordService.extractUploadData(organization)

        // Then
        Truth.assertThat(data).isNull()
        inOrder.verify(recordService).extractUploadData(organization)
        inOrder.verifyNoMoreInteractions()
    }

    @Test
    fun extractUploadData_shouldReturnNull_whenContentIsNull() {
        // Given
        val content: List<DocumentReferenceContent>? = null
        val document = DocumentReference(
                null,
                null,
                null,
                content
        )

        // When
        val data = recordService.extractUploadData(document)

        // Then
        Truth.assertThat(data).isNull()
        inOrder.verify(recordService).extractUploadData(document)
        inOrder.verifyNoMoreInteractions()
    }

    @Test
    fun extractUploadData_shouldReturnNull_whenAttachmentIsNull() {
        // Given
        val attachment = null
        val content = DocumentReferenceContent(attachment)
        val document = DocumentReference(
                null,
                null,
                null,
                listOf(content)
        )

        // When
        val data = recordService.extractUploadData(document)

        // Then
        Truth.assertThat(data).isNull()
        inOrder.verify(recordService).extractUploadData(document)
        inOrder.verifyNoMoreInteractions()
    }

    @Test
    fun extractUploadData_shouldReturnNull_whenAttachmentDataIsNull() {
        // Given
        val document = buildDocumentReference()
        document.content[0].attachment.data = null

        // When
        val data = recordService.extractUploadData(document)

        // Then
        Truth.assertThat(data).isNull()
        inOrder.verify(recordService).extractUploadData(document)
        inOrder.verifyNoMoreInteractions()
    }

    @Test
    fun `Given, removeOrRestoreUploadData is called with REMOVE, a DecryptedFhir3Record, a Resource and Attachment, it delegates it to removeUploadData`() {
        // Given
        val document = buildDocumentReference()

        @Suppress("UNCHECKED_CAST")
        val decryptedRecord = Mockito.mock(DecryptedFhir3Record::class.java) as DecryptedFhir3Record<Fhir3Resource>

        Mockito.doReturn(mockDecryptedFhir3Record)
                .`when`(recordService)
                .removeUploadData(decryptedRecord)

        // When
        val record = recordService.removeOrRestoreUploadData(
                RecordService.RemoveRestoreOperation.REMOVE,
                decryptedRecord,
                document,
                mockUploadData
        )

        // Then
        Truth.assertThat(record).isSameInstanceAs(mockDecryptedFhir3Record)

        inOrder.verify(recordService).removeUploadData(decryptedRecord)
        inOrder.verifyNoMoreInteractions()
    }

    @Test
    fun `Given, removeUploadData is called with a non DecryptedFhir3Record, it reflects the given record`() {
        // Given
        @Suppress("UNCHECKED_CAST")
        val decryptedRecord = Mockito.mock(DecryptedCustomDataRecord::class.java) as DecryptedBaseRecord<Any>
        val attachments = mutableListOf(
                Mockito.mock(Fhir3Attachment::class.java),
                Mockito.mock(Fhir3Attachment::class.java)
        )

        Mockito.`when`(decryptedRecord.resource).thenReturn(mockDataResource)

        every { Fhir3AttachmentHelper.getAttachment(mockCarePlan) } returns attachments
        every { Fhir3AttachmentHelper.updateAttachmentData(mockCarePlan, null) } returns Unit

        // When
        val record = recordService.removeUploadData(decryptedRecord)

        // Then
        Truth.assertThat(record).isSameInstanceAs(decryptedRecord)

        verify(exactly = 0) { Fhir3AttachmentHelper.getAttachment(mockCarePlan) }
        verify(exactly = 0) { Fhir3AttachmentHelper.updateAttachmentData(mockCarePlan, null) }
    }

    @Test
    fun `Given, removeUploadData is called with a DecryptedFhir3Record, it removes the existing Attachments`() {
        // Given
        @Suppress("UNCHECKED_CAST")
        val decryptedRecord = Mockito.mock(DecryptedFhir3Record::class.java) as DecryptedFhir3Record<Fhir3Resource>
        val attachments = mutableListOf(
                Mockito.mock(Fhir3Attachment::class.java),
                Mockito.mock(Fhir3Attachment::class.java)
        )

        Mockito.`when`(decryptedRecord.resource).thenReturn(mockCarePlan)

        every { Fhir3AttachmentHelper.getAttachment(mockCarePlan) } returns attachments
        every { Fhir3AttachmentHelper.updateAttachmentData(mockCarePlan, null) } returns Unit

        // When
        val record = recordService.removeUploadData(decryptedRecord)

        // Then
        Truth.assertThat(record).isSameInstanceAs(decryptedRecord)

        verify(exactly = 1) { Fhir3AttachmentHelper.getAttachment(mockCarePlan) }
        verify(exactly = 1) { Fhir3AttachmentHelper.updateAttachmentData(mockCarePlan, null) }
    }

    @Test
    fun `Given, removeUploadData is called with a DecryptedFhir4Record, it removes the existing Attachments`() {
        // Given
        val carePlan = mockk<Fhir4Resource>()

        @Suppress("UNCHECKED_CAST")
        val decryptedRecord = Mockito.mock(DecryptedFhir4Record::class.java) as DecryptedFhir4Record<Fhir4Resource>
        val attachments = mutableListOf(
                Mockito.mock(Fhir4Attachment::class.java),
                Mockito.mock(Fhir4Attachment::class.java)
        )

        Mockito.`when`(decryptedRecord.resource).thenReturn(carePlan)

        every { Fhir4AttachmentHelper.getAttachment(carePlan) } returns attachments
        every { Fhir4AttachmentHelper.updateAttachmentData(carePlan, null) } returns Unit

        // When
        val record = recordService.removeUploadData(decryptedRecord)

        // Then
        Truth.assertThat(record).isSameInstanceAs(decryptedRecord)

        verify(exactly = 1) { Fhir4AttachmentHelper.getAttachment(carePlan) }
        verify(exactly = 1) { Fhir4AttachmentHelper.updateAttachmentData(carePlan, null) }
    }

    @Test
    fun `Given, removeUploadData is called with a DecryptedFhir3Record, it does nothing, if no Attachments exists`() {
        // Given
        @Suppress("UNCHECKED_CAST")
        val decryptedRecord = Mockito.mock(DecryptedFhir3Record::class.java) as DecryptedFhir3Record<Fhir3Resource>

        Mockito.`when`(decryptedRecord.resource).thenReturn(mockCarePlan)

        every { Fhir3AttachmentHelper.getAttachment(mockCarePlan) } returns null
        every { Fhir3AttachmentHelper.updateAttachmentData(mockCarePlan, null) } returns Unit

        // When
        val record = recordService.removeUploadData(decryptedRecord)

        // Then
        Truth.assertThat(record).isSameInstanceAs(decryptedRecord)

        verify(exactly = 1) { Fhir3AttachmentHelper.getAttachment(mockCarePlan) }
        verify(exactly = 0) { Fhir3AttachmentHelper.updateAttachmentData(mockCarePlan, null) }
    }

    @Test
    fun `Given, removeOrRestoreUploadData is called with RESTORE, a DecryptedFhir3Record, a Resource and Attachment, it delegates it to restoreUploadData`() {
        // Given
        val document = buildDocumentReference()

        @Suppress("UNCHECKED_CAST")
        val decryptedRecord = Mockito.mock(DecryptedFhir3Record::class.java) as DecryptedFhir3Record<Fhir3Resource>

        Mockito.doReturn(mockDecryptedFhir3Record)
                .`when`(recordService)
                .restoreUploadData(
                        decryptedRecord,
                        document,
                        mockUploadData as HashMap<Any, String?>
                )

        // When
        val record = recordService.removeOrRestoreUploadData(
                RecordService.RemoveRestoreOperation.RESTORE,
                decryptedRecord,
                document,
                mockUploadData
        )

        // Then
        Truth.assertThat(record).isSameInstanceAs(mockDecryptedFhir3Record)

        inOrder.verify(recordService).restoreUploadData(
                decryptedRecord,
                document,
                mockUploadData as HashMap<Any, String?>
        )
        inOrder.verifyNoMoreInteractions()
    }

    @Test
    fun `Given, restoreUploadData is called with a non DecryptedFhir3Record, a Resource and Attachment, it reflects the given Record`() {
        val document = buildDocumentReference()
        val decryptedRecord = mockkClass(DecryptedCustomDataRecord::class)
        val attachments = mutableListOf(
                Mockito.mock(Fhir3Attachment::class.java),
                Mockito.mock(Fhir3Attachment::class.java)
        )

        every { decryptedRecord.resource } returns mockDataResource
        every { Fhir3AttachmentHelper.getAttachment(mockCarePlan) } returns attachments
        every { Fhir3AttachmentHelper.updateAttachmentData(mockCarePlan, any()) } returns Unit

        // When
        @Suppress("UNCHECKED_CAST")
        val record = recordService.restoreUploadData(
                decryptedRecord as DecryptedBaseRecord<Any>,
                document,
                mockUploadData as HashMap<Any, String?>
        )

        // Then
        Truth.assertThat(record).isSameInstanceAs(decryptedRecord)

        verify(exactly = 0) { decryptedRecord.resource = any() }
    }

    @Test
    fun `Given, restoreUploadData is called with a DecryptedFhir3Record, a non FhirResource and Attachment, it reflects the given Record`() {
        val document = mockk<Fhir3Resource>()
        val decryptedRecord = mockkClass(DecryptedFhir3Record::class, relaxed = true)
        val attachments = mutableListOf(
                Mockito.mock(Fhir3Attachment::class.java),
                Mockito.mock(Fhir3Attachment::class.java)
        )

        every { decryptedRecord.resource } returns mockCarePlan
        every { Fhir3AttachmentHelper.getAttachment(mockCarePlan) } returns attachments
        every { Fhir3AttachmentHelper.updateAttachmentData(mockCarePlan, any()) } returns Unit

        // When
        @Suppress("UNCHECKED_CAST")
        val record = recordService.restoreUploadData(
                decryptedRecord as DecryptedBaseRecord<Any>,
                document,
                mockUploadData as HashMap<Any, String?>
        )

        // Then
        Truth.assertThat(record).isSameInstanceAs(decryptedRecord)
    }

    @Test
    fun `Given, restoreUploadData is called with a DecryptedFhir3Record, a Resource and Attachment, it sets the given Resource to the DecryptedFhir3Record`() {
        val document = buildDocumentReference()

        @Suppress("UNCHECKED_CAST")
        val decryptedRecord = Mockito.mock(DecryptedFhir3Record::class.java) as DecryptedFhir3Record<Fhir3Resource>
        val attachments = mutableListOf(
                Mockito.mock(Fhir3Attachment::class.java),
                Mockito.mock(Fhir3Attachment::class.java)
        )

        Mockito.`when`(decryptedRecord.resource).thenReturn(mockCarePlan)

        every { Fhir3AttachmentHelper.getAttachment(mockCarePlan) } returns attachments
        every { Fhir3AttachmentHelper.updateAttachmentData(mockCarePlan, any()) } returns Unit

        // When
        val record = recordService.restoreUploadData(
                decryptedRecord,
                document,
                mockUploadData as HashMap<Any, String?>
        )

        // Then
        Truth.assertThat(record).isSameInstanceAs(decryptedRecord)

        Mockito.verify(decryptedRecord, times(1)).resource = document
    }

    @Test
    fun `Given, restoreUploadData is called with a DecryptedFhir4Record, a Resource and Attachment, it sets the given Resource to the DecryptedFhir4Record`() {
        val carePlan = mockk<Fhir4Resource>()
        val document = buildDocumentReferenceFhir4()

        @Suppress("UNCHECKED_CAST")
        val decryptedRecord = Mockito.mock(DecryptedFhir4Record::class.java) as DecryptedFhir4Record<Fhir4Resource>
        val attachments = mutableListOf(
                Mockito.mock(Fhir4Attachment::class.java),
                Mockito.mock(Fhir4Attachment::class.java)
        )

        Mockito.`when`(decryptedRecord.resource).thenReturn(carePlan)

        every { Fhir4AttachmentHelper.getAttachment(carePlan) } returns attachments
        every { Fhir4AttachmentHelper.updateAttachmentData(carePlan, any()) } returns Unit

        // When
        val record = recordService.restoreUploadData(
                decryptedRecord,
                document,
                mockUploadData as HashMap<Any, String?>
        )

        // Then
        Truth.assertThat(record).isSameInstanceAs(decryptedRecord)

        Mockito.verify(decryptedRecord, times(1)).resource = document
    }

    @Test
    fun `Given, restoreUploadData is called with a DecryptedFhir3Record, null as a Resource and Attachment, it does not set a new Resource for the DecryptedFhir3Record`() {
        @Suppress("UNCHECKED_CAST")
        val decryptedRecord = mockkClass(DecryptedFhir3Record::class) as DecryptedFhir3Record<Fhir3Resource>
        val attachments = mutableListOf(
                Mockito.mock(Fhir3Attachment::class.java),
                Mockito.mock(Fhir3Attachment::class.java)
        )

        every { decryptedRecord.resource } returns mockCarePlan

        every { Fhir3AttachmentHelper.getAttachment(mockCarePlan) } returns attachments
        every { Fhir3AttachmentHelper.updateAttachmentData(mockCarePlan, any()) } returns Unit

        // When
        val record = recordService.restoreUploadData(
                decryptedRecord,
                null,
                mockUploadData as HashMap<Any, String?>
        )

        // Then
        Truth.assertThat(record).isSameInstanceAs(decryptedRecord)

        verify(exactly = 0) { decryptedRecord.resource = any() }
    }

    @Test
    fun `Given, restoreUploadData is called with a DecryptedFhir3Record, a Resource and Attachment, it removes the existing Attachments`() {
        val document = buildDocumentReference()

        @Suppress("UNCHECKED_CAST")
        val decryptedRecord = Mockito.mock(DecryptedFhir3Record::class.java) as DecryptedFhir3Record<Fhir3Resource>
        val attachments = mutableListOf(
                Mockito.mock(Fhir3Attachment::class.java),
                Mockito.mock(Fhir3Attachment::class.java)
        )

        Mockito.`when`(decryptedRecord.resource).thenReturn(mockCarePlan)

        every { Fhir3AttachmentHelper.getAttachment(mockCarePlan) } returns attachments
        every { Fhir3AttachmentHelper.updateAttachmentData(mockCarePlan, mockUploadData) } returns Unit

        // When
        val record = recordService.restoreUploadData(
                decryptedRecord,
                document,
                mockUploadData as HashMap<Any, String?>
        )

        // Then
        Truth.assertThat(record).isSameInstanceAs(decryptedRecord)

        verify(exactly = 1) { Fhir3AttachmentHelper.getAttachment(mockCarePlan) }
        verify(exactly = 1) { Fhir3AttachmentHelper.updateAttachmentData(mockCarePlan, mockUploadData) }
    }

    @Test
    fun `Given, restoreUploadData is called with a DecryptedFhir3Record, a Resource and Attachment, it does nothing, if no Attachments exists`() {
        val document = buildDocumentReference()

        @Suppress("UNCHECKED_CAST")
        val decryptedRecord = Mockito.mock(DecryptedFhir3Record::class.java) as DecryptedFhir3Record<Fhir3Resource>

        Mockito.`when`(decryptedRecord.resource).thenReturn(mockCarePlan)

        every { Fhir3AttachmentHelper.getAttachment(mockCarePlan) } returns null
        every { Fhir3AttachmentHelper.updateAttachmentData(mockCarePlan, any()) } returns Unit

        // When
        val record = recordService.restoreUploadData(
                decryptedRecord,
                document,
                mockUploadData as HashMap<Any, String?>
        )

        // Then
        Truth.assertThat(record).isSameInstanceAs(decryptedRecord)

        verify(exactly = 1) { Fhir3AttachmentHelper.getAttachment(mockCarePlan) }
        verify(exactly = 0) { Fhir3AttachmentHelper.updateAttachmentData(mockCarePlan, mockUploadData) }
    }

    @Test
    fun `Given, restoreUploadData is called with a DecryptedFhir3Record, a Resource and null as Attachment, it returns the DecryptedFhir3Record without invoking more actions`() {
        val document = buildDocumentReference()

        @Suppress("UNCHECKED_CAST")
        val decryptedRecord = Mockito.mock(DecryptedFhir3Record::class.java) as DecryptedFhir3Record<Fhir3Resource>
        val attachments = mutableListOf(
                Mockito.mock(Fhir3Attachment::class.java),
                Mockito.mock(Fhir3Attachment::class.java)
        )

        Mockito.`when`(decryptedRecord.resource).thenReturn(mockCarePlan)

        every { Fhir3AttachmentHelper.getAttachment(mockCarePlan) } returns attachments
        every { Fhir3AttachmentHelper.updateAttachmentData(mockCarePlan, mockUploadData) } returns Unit

        // When
        val record = recordService.restoreUploadData(
                decryptedRecord,
                document,
                null
        )

        // Then
        Truth.assertThat(record).isSameInstanceAs(decryptedRecord)

        verify(exactly = 0) { Fhir3AttachmentHelper.getAttachment(mockCarePlan) }
        verify(exactly = 0) { Fhir3AttachmentHelper.updateAttachmentData(mockCarePlan, mockUploadData) }
    }

    @Test
    @Throws(DataValidationException.IdUsageViolation::class)
    fun cleanObsoleteAdditionalIdentifiers_shouldCleanObsoleteIdentifiers() {
        //given
        val currentId = ADDITIONAL_ID
        val obsoleteId = ADDITIONAL_ID.replaceFirst(ATTACHMENT_ID.toRegex(), "obsoleteId")
        val otherId = "otherId"
        val currentIdentifier = Fhir3AttachmentHelper.buildIdentifier(currentId, ASSIGNER)
        val obsoleteIdentifier = Fhir3AttachmentHelper.buildIdentifier(obsoleteId, ASSIGNER)
        val otherIdentifier = Fhir3AttachmentHelper.buildIdentifier(otherId, ASSIGNER)
        val identifiers: MutableList<Identifier> = arrayListOf()
        identifiers.add(currentIdentifier)
        identifiers.add(obsoleteIdentifier)
        identifiers.add(otherIdentifier)
        val doc = buildDocumentReference()
        doc.content[0].attachment.id = ATTACHMENT_ID
        doc.identifier = identifiers

        //when
        recordService.cleanObsoleteAdditionalIdentifiers(doc)

        //then
        Truth.assertThat(doc.identifier).hasSize(2)
        Truth.assertThat(doc.identifier!![0]).isEqualTo(currentIdentifier)
        Truth.assertThat(doc.identifier!![1]).isEqualTo(otherIdentifier)
    }

    @Test
    @Throws(DataValidationException.IdUsageViolation::class)
    fun setAttachmentIdForDownloadType_shouldSetAttachmentId() {
        //given
        val attachment = AttachmentBuilder.buildAttachment(ATTACHMENT_ID)
        val additionalId = Fhir3AttachmentHelper.buildIdentifier(ADDITIONAL_ID, ASSIGNER)
        val attachments = listOf(attachment)
        val identifiers = listOf(additionalId)

        //when downloadType is Full
        recordService.setAttachmentIdForDownloadType(attachments, identifiers, DownloadType.Full)
        //then
        Truth.assertThat(attachment.id).isEqualTo(ATTACHMENT_ID)

        //given
        attachment.id = ATTACHMENT_ID
        //when downloadType is Medium
        recordService.setAttachmentIdForDownloadType(attachments, identifiers, DownloadType.Medium)
        //then
        Truth.assertThat(attachment.id).isEqualTo(ATTACHMENT_ID + SPLIT_CHAR + PREVIEW_ID)

        //given
        attachment.id = ATTACHMENT_ID
        //when downloadType is Small
        recordService.setAttachmentIdForDownloadType(attachments, identifiers, DownloadType.Small)
        //then
        Truth.assertThat(attachment.id).isEqualTo(ATTACHMENT_ID + SPLIT_CHAR + THUMBNAIL_ID)
    }

    @Test
    @Throws(DataValidationException.IdUsageViolation::class)
    fun extractAdditionalAttachmentIds_shouldExtractAdditionalIds() {
        //given
        val additionalIdentifier = Fhir3AttachmentHelper.buildIdentifier(ADDITIONAL_ID, ASSIGNER)

        //when
        val additionalIds = recordService.extractAdditionalAttachmentIds(listOf(additionalIdentifier), ATTACHMENT_ID)

        //then
        val d4lNamespacePos = 0
        Truth.assertThat(additionalIds).hasLength(RecordService.DOWNSCALED_ATTACHMENT_IDS_SIZE)
        Truth.assertThat(additionalIds!![d4lNamespacePos]).isEqualTo(RecordService.DOWNSCALED_ATTACHMENT_IDS_FMT)
        Truth.assertThat(additionalIds[RecordService.FULL_ATTACHMENT_ID_POS]).isEqualTo(ATTACHMENT_ID)
        Truth.assertThat(additionalIds[RecordService.PREVIEW_ID_POS]).isEqualTo(PREVIEW_ID)
        Truth.assertThat(additionalIds[RecordService.THUMBNAIL_ID_POS]).isEqualTo(THUMBNAIL_ID)
    }

    @Test
    @Throws(DataValidationException.IdUsageViolation::class)
    fun extractAdditionalAttachmentIds_shouldReturnNull_whenAdditionalIdentifiersAreNull() {
        //when
        val additionalIds = recordService.extractAdditionalAttachmentIds(null, ATTACHMENT_ID)

        //then
        Truth.assertThat(additionalIds).isNull()
    }

    @Test
    @Throws(DataValidationException.IdUsageViolation::class)
    fun extractAdditionalAttachmentIds_shouldReturnNull_whenAdditionalIdentifiersAreNotAdditionalAttachmentIds() {
        //given
        val identifier = Fhir3AttachmentHelper.buildIdentifier("otherId", ASSIGNER)

        //when
        val additionalIds = recordService.extractAdditionalAttachmentIds(listOf(identifier), ATTACHMENT_ID)

        //then
        Truth.assertThat(additionalIds).isNull()
    }

    @Test
    @Throws(DataValidationException.IdUsageViolation::class)
    fun splitAdditionalAttachmentId_shouldSplitAdditionalId() {
        //given
        val additionalIdentifier = Fhir3AttachmentHelper.buildIdentifier(ADDITIONAL_ID, ASSIGNER)

        //when
        val additionalIds = recordService.splitAdditionalAttachmentId(SdkIdentifierFactory.wrap(additionalIdentifier))

        //then
        val d4lNamespacePos = 0
        Truth.assertThat(additionalIds).hasLength(RecordService.DOWNSCALED_ATTACHMENT_IDS_SIZE)
        Truth.assertThat(additionalIds!![d4lNamespacePos]).isEqualTo(RecordService.DOWNSCALED_ATTACHMENT_IDS_FMT)
        Truth.assertThat(additionalIds[RecordService.FULL_ATTACHMENT_ID_POS]).isEqualTo(ATTACHMENT_ID)
        Truth.assertThat(additionalIds[RecordService.PREVIEW_ID_POS]).isEqualTo(PREVIEW_ID)
        Truth.assertThat(additionalIds[RecordService.THUMBNAIL_ID_POS]).isEqualTo(THUMBNAIL_ID)
    }

    @Test
    @Throws(DataValidationException.IdUsageViolation::class)
    fun splitAdditionalAttachmentId_shouldReturnNull_whenAdditionalIdentifierIsNull() {
        //given
        val additionalIdentifier = Fhir3AttachmentHelper.buildIdentifier(null, ASSIGNER)
        //when
        val additionalIds = recordService.splitAdditionalAttachmentId(SdkIdentifierFactory.wrap(additionalIdentifier))
        //then
        Truth.assertThat(additionalIds).isNull()
    }

    @Test
    @Throws(DataValidationException.IdUsageViolation::class)
    fun splitAdditionalAttachmentId_shouldReturnNull_whenAdditionalIdentifierIsNotAdditionalAttachmentId() {
        //given
        val additionalIdentifier = Fhir3AttachmentHelper.buildIdentifier("otherId", ASSIGNER)

        //when
        val additionalIds = recordService.splitAdditionalAttachmentId(SdkIdentifierFactory.wrap(additionalIdentifier))

        //then
        Truth.assertThat(additionalIds).isNull()
    }

    @Test
    fun splitAdditionalAttachmentId_shouldThrow_whenAdditionalAttachmentIdIsMalformed() {
        //given
        val malformedAdditionalId = ADDITIONAL_ID + SPLIT_CHAR + "unexpectedId"
        val additionalIdentifier = Fhir3AttachmentHelper.buildIdentifier(malformedAdditionalId, ASSIGNER)

        //when
        try {
            recordService.splitAdditionalAttachmentId(SdkIdentifierFactory.wrap(additionalIdentifier))
            Assert.fail("Exception expected!")
        } catch (ex: DataValidationException.IdUsageViolation) {

            //then
            Truth.assertThat(ex.message).isEqualTo(malformedAdditionalId)
        }
    }

    @Test
    fun updateAttachmentMeta_shouldUpdateAttachmentMeta() {
        //given
        val attachment = Fhir3Attachment()
        val data = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0xDB.toByte())
        val dataBase64 = encodeToString(data)
        val oldSize = 0
        val oldHash = "oldHash"
        attachment.data = dataBase64
        attachment.size = oldSize
        attachment.hash = oldHash

        //when
        recordService.updateAttachmentMeta(attachment)

        //then
        Truth.assertThat(attachment.data).isEqualTo(dataBase64)
        Truth.assertThat(attachment.size).isEqualTo(data.size)
        Truth.assertThat(attachment.hash).isEqualTo("obkanHeotP32HiKllYhs/aRLUAc=")
    }

    @Test
    @Throws(DataRestrictionException.UnsupportedFileType::class, DataRestrictionException.MaxDataSizeViolation::class)
    fun checkDataRestrictions_shouldReturnSuccessfully() {
        // Given
        val pdf = arrayOfNulls<Byte>(DATA_SIZE_MAX_BYTES)
        System.arraycopy(
                MimeType.PDF.byteSignature()[0] as Any,
                0,
                pdf,
                0,
                MimeType.PDF.byteSignature()[0]?.size!!
        )
        val doc = buildDocumentReference(unboxByteArray(pdf))

        // When
        recordService.checkDataRestrictions(doc)

        // Then
        inOrder.verify(recordService).checkDataRestrictions(doc)
        inOrder.verifyNoMoreInteractions()
    }

    @Test
    @Throws(DataRestrictionException.UnsupportedFileType::class, DataRestrictionException.MaxDataSizeViolation::class)
    fun checkDataRestrictions_shouldThrow_forUnsupportedData() {
        // Given
        val invalidData = byteArrayOf(0x00)
        val doc = buildDocumentReference(invalidData)

        // When
        try {
            recordService.checkDataRestrictions(doc)
            Assert.fail("Exception expected!")
        } catch (e: D4LException) {

            // Then
            Truth.assertThat(e.javaClass).isEqualTo(DataRestrictionException.UnsupportedFileType::class.java)
        }

        // Then
        inOrder.verify(recordService).checkDataRestrictions(doc)
        inOrder.verifyNoMoreInteractions()
    }

    @Test
    @Throws(DataRestrictionException.UnsupportedFileType::class, DataRestrictionException.MaxDataSizeViolation::class)
    fun checkDataRestrictions_shouldThrow_whenFileSizeLimitIsReached() {
        // Given
        val invalidSizePdf = arrayOfNulls<Byte>(DATA_SIZE_MAX_BYTES + 1)
        System.arraycopy(
                MimeType.PDF.byteSignature()[0] as Any,
                0,
                invalidSizePdf,
                0,
                MimeType.PDF.byteSignature()[0]?.size!!
        )
        val doc = buildDocumentReference(unboxByteArray(invalidSizePdf))

        // When
        try {
            recordService.checkDataRestrictions(doc)
            Assert.fail("Exception expected!")
        } catch (e: D4LException) {

            // Then
            Truth.assertThat(e.javaClass).isEqualTo(DataRestrictionException.MaxDataSizeViolation::class.java)
        }

        // Then
        inOrder.verify(recordService).checkDataRestrictions(doc)
        inOrder.verifyNoMoreInteractions()
    }

    @Test
    @Throws(DataRestrictionException.UnsupportedFileType::class, DataRestrictionException.MaxDataSizeViolation::class)
    fun `Given, checkDataRestrictions is called, with a Resource, which has non extractable Attachments, it returns without a failure`() {
        // Given
        val resourceStr = getResource("/fhir4/s4h-patient-example.patient.json")

        val doc = FhirR4Parser().toFhir(Fhir4Patient::class.java, resourceStr)

        // When
        recordService.checkDataRestrictions(doc)

        // Then
        inOrder.verify(recordService).checkDataRestrictions(doc)
        inOrder.verifyNoMoreInteractions()
    }

    @Test
    fun buildMeta_shouldBuildMeta_whenUpdatedDateMillisecondsArePresent() {
        // Given
        val updatedDateWithMilliseconds = "2019-02-28T17:21:08.234123"
        Mockito.`when`(mockDecryptedFhir3Record.customCreationDate).thenReturn("2019-02-28")
        Mockito.`when`(mockDecryptedFhir3Record.updatedDate).thenReturn(updatedDateWithMilliseconds)

        // When
        val meta = recordService.buildMeta(mockDecryptedFhir3Record)

        // Then
        Truth.assertThat(meta.createdDate).isEqualTo(LocalDate.of(2019, 2, 28))
        Truth.assertThat(meta.updatedDate).isEqualTo(LocalDateTime.of(2019, 2, 28, 17, 21, 8, 234123000))
        inOrder.verify(recordService).buildMeta(mockDecryptedFhir3Record)
        inOrder.verifyNoMoreInteractions()
    }

    @Test
    fun buildMeta_shouldBuildMeta_whenUpdatedDateMillisecondsAreNotPresent() {
        // Given
        val updatedDateWithMilliseconds = "2019-02-28T17:21:08"
        Mockito.`when`(mockDecryptedFhir3Record.customCreationDate).thenReturn("2019-02-28")
        Mockito.`when`(mockDecryptedFhir3Record.updatedDate).thenReturn(updatedDateWithMilliseconds)

        // When
        val meta = recordService.buildMeta(mockDecryptedFhir3Record)

        // Then
        Truth.assertThat(meta.createdDate).isEqualTo(
                LocalDate.of(2019, 2, 28)
        )
        Truth.assertThat(meta.updatedDate).isEqualTo(
                LocalDateTime.of(2019, 2, 28, 17, 21, 8)
        )
        inOrder.verify(recordService).buildMeta(mockDecryptedFhir3Record)
        inOrder.verifyNoMoreInteractions()
    }

    //endregion
    @Test
    @Throws(InterruptedException::class)
    fun deleteRecord_shouldDeleteRecord() {
        // Given
        Mockito.`when`(mockApiService.deleteRecord(ALIAS, RECORD_ID, USER_ID)).thenReturn(Completable.complete())

        // When
        val subscriber = recordService.deleteRecord(USER_ID, RECORD_ID).test().await()

        // Then
        subscriber.assertNoErrors().assertComplete()
        inOrder.verify(recordService).deleteRecord(USER_ID, RECORD_ID)
        inOrder.verify(mockApiService).deleteRecord(ALIAS, RECORD_ID, USER_ID)
        inOrder.verifyNoMoreInteractions()
    }

    @Test
    @Throws(InterruptedException::class)
    fun deleteRecords_shouldDeleteRecords() {
        // Given
        Mockito.doReturn(Completable.complete()).`when`(recordService).deleteRecord(USER_ID, RECORD_ID)
        val ids = listOf(RECORD_ID, RECORD_ID)

        // When
        val observer = recordService.deleteRecords(ids, USER_ID).test().await()

        // Then
        val result = observer
                .assertNoErrors()
                .assertComplete()
                .assertValueCount(1)
                .values()[0]
        Truth.assertThat(result.failedDeletes).hasSize(0)
        Truth.assertThat(result.successfulDeletes).hasSize(2)
        inOrder.verify(recordService).deleteRecords(ids, USER_ID)
        inOrder.verify(recordService, Mockito.times(2)).deleteRecord(USER_ID, RECORD_ID)
        inOrder.verifyNoMoreInteractions()
    }

    @Test
    @Throws(InterruptedException::class,
            IOException::class,
            DataValidationException.ModelVersionNotSupported::class,
            DataValidationException.ExpectedFieldViolation::class,
            DataValidationException.IdUsageViolation::class,
            DataValidationException.InvalidAttachmentPayloadHash::class)
    fun downloadRecord_shouldReturnDownloadedRecord() {
        // Given
        Mockito.`when`(mockApiService.fetchRecord(ALIAS, USER_ID, RECORD_ID))
                .thenReturn(Single.just(mockEncryptedRecord))
        Mockito.`when`(mockCarePlan.resourceType).thenReturn(CarePlan.resourceType)
        Mockito.doReturn(mockDecryptedFhir3Record)
                .`when`(recordService)
                .decryptRecord<Fhir3Resource>(mockEncryptedRecord, USER_ID)
        Mockito.doReturn(mockDecryptedFhir3Record)
                .`when`(recordService)
                .downloadData(mockDecryptedFhir3Record, USER_ID)
        @Suppress("UNCHECKED_CAST")
        every { RecordMapper.getInstance(mockDecryptedFhir3Record) } returns mockRecord as BaseRecord<Fhir3Resource>

        // When
        val observer = recordService.downloadRecord<CarePlan>(RECORD_ID, USER_ID).test().await()

        // Then
        val result = observer
                .assertNoErrors()
                .assertComplete()
                .assertValueCount(1)
                .values()[0]
        Truth.assertThat(result.meta).isEqualTo(mockMeta)
        Truth.assertThat(result.fhirResource).isEqualTo(mockCarePlan)
        inOrder.verify(mockApiService).fetchRecord(ALIAS, USER_ID, RECORD_ID)
        inOrder.verify(recordService).decryptRecord<Fhir3Resource>(mockEncryptedRecord, USER_ID)
        inOrder.verify(recordService).downloadData(mockDecryptedFhir3Record, USER_ID)
        inOrder.verify(recordService).checkDataRestrictions(mockCarePlan)
        inOrder.verify(recordService).assignResourceId(mockDecryptedFhir3Record)
        inOrder.verifyNoMoreInteractions()
    }

    @Test
    @Throws(InterruptedException::class,
            IOException::class,
            DataValidationException.ModelVersionNotSupported::class,
            DataValidationException.ExpectedFieldViolation::class,
            DataRestrictionException.UnsupportedFileType::class,
            DataRestrictionException.MaxDataSizeViolation::class,
            DataValidationException.IdUsageViolation::class,
            DataValidationException.InvalidAttachmentPayloadHash::class)
    fun downloadRecord_shouldThrow_forUnsupportedData() {
        // Given
        val invalidData = byteArrayOf(0x00)
        val doc = buildDocumentReference(invalidData)
        Mockito.`when`(mockApiService.fetchRecord(ALIAS, USER_ID, RECORD_ID))
                .thenReturn(Single.just(mockEncryptedRecord))
        Mockito.doReturn(mockDecryptedFhir3Record)
                .`when`(recordService)
                .decryptRecord<Fhir3Resource>(mockEncryptedRecord, USER_ID)
        Mockito.doReturn(mockDecryptedFhir3Record)
                .`when`(recordService).downloadData(mockDecryptedFhir3Record, USER_ID)
        Mockito.`when`(mockDecryptedFhir3Record.resource).thenReturn(doc)

        // When
        val observer = recordService.downloadRecord<Fhir3Resource>(RECORD_ID, USER_ID).test().await()

        // Then
        val errors = observer.errors()
        Truth.assertThat(errors).hasSize(1)
        Truth.assertThat(errors[0]).isInstanceOf(DataRestrictionException.UnsupportedFileType::class.java)
        inOrder.verify(mockApiService).fetchRecord(ALIAS, USER_ID, RECORD_ID)
        inOrder.verify(recordService).decryptRecord<Fhir3Resource>(mockEncryptedRecord, USER_ID)
        inOrder.verify(recordService).downloadData(mockDecryptedFhir3Record, USER_ID)
        inOrder.verify(recordService).checkDataRestrictions(doc)
        inOrder.verifyNoMoreInteractions()
    }

    @Test
    @Throws(InterruptedException::class,
            IOException::class,
            DataValidationException.ModelVersionNotSupported::class,
            DataValidationException.ExpectedFieldViolation::class,
            DataRestrictionException.UnsupportedFileType::class,
            DataRestrictionException.MaxDataSizeViolation::class,
            DataValidationException.IdUsageViolation::class,
            DataValidationException.InvalidAttachmentPayloadHash::class)
    fun downloadRecord_shouldThrow_forFileSizeLimitationBreach() {
        // Given
        val invalidSizePdf = arrayOfNulls<Byte>(DATA_SIZE_MAX_BYTES + 1)
        System.arraycopy(
                MimeType.PDF.byteSignature()[0] as Any,
                0,
                invalidSizePdf,
                0,
                MimeType.PDF.byteSignature()[0]?.size!!
        )
        val doc = buildDocumentReference(unboxByteArray(invalidSizePdf))
        Mockito.`when`(mockApiService
                .fetchRecord(ALIAS, USER_ID, RECORD_ID))
                .thenReturn(Single.just(mockEncryptedRecord))
        Mockito.doReturn(mockDecryptedFhir3Record)
                .`when`(recordService)
                .decryptRecord<Fhir3Resource>(mockEncryptedRecord, USER_ID)
        Mockito.doReturn(mockDecryptedFhir3Record)
                .`when`(recordService)
                .downloadData(mockDecryptedFhir3Record, USER_ID)
        Mockito.`when`(mockDecryptedFhir3Record.resource).thenReturn(doc)

        // When
        val observer = recordService.downloadRecord<Fhir3Resource>(RECORD_ID, USER_ID).test().await()

        // Then
        val errors = observer.errors()
        Truth.assertThat(errors).hasSize(1)
        Truth.assertThat(errors[0]).isInstanceOf(DataRestrictionException.MaxDataSizeViolation::class.java)
        inOrder.verify(mockApiService).fetchRecord(ALIAS, USER_ID, RECORD_ID)
        inOrder.verify(recordService).decryptRecord<Fhir3Resource>(mockEncryptedRecord, USER_ID)
        inOrder.verify(recordService).downloadData(mockDecryptedFhir3Record, USER_ID)
        inOrder.verify(recordService).checkDataRestrictions(doc)
        inOrder.verifyNoMoreInteractions()
    }

    @Test
    @Throws(InterruptedException::class)
    fun downloadRecords_shouldReturnDownloadedRecords() {
        // Given
        val recordIds = listOf(RECORD_ID, RECORD_ID)
        Mockito.doReturn(Single.just(mockRecord))
                .`when`(recordService)
                .downloadRecord<Fhir3Resource>(RECORD_ID, USER_ID)

        // When
        val observer = recordService.downloadRecords<CarePlan>(recordIds, USER_ID).test().await()

        // Then
        val result = observer
                .assertNoErrors()
                .assertComplete()
                .assertValueCount(1)
                .values()[0]
        Truth.assertThat(result.failedDownloads).hasSize(0)
        Truth.assertThat(result.successfulDownloads).hasSize(2)
        inOrder.verify(recordService).downloadRecords<Fhir3Resource>(recordIds, USER_ID)
        inOrder.verify(recordService, Mockito.times(2))
                .downloadRecord<Fhir3Resource>(RECORD_ID, USER_ID)
        inOrder.verifyNoMoreInteractions()
    }

    @Test
    fun `Given, downloadAttachments is called with a RecordId, AttachmentIds, UserId and a DownloadType, it delegates and returns encountered Attachments`() { // downloadAttachment_shouldDownloadAttachment
        // Given
        val userId = "asd"
        val recordId = "ads"
        val attachmentId = "lllll"
        val attachmentIds = listOf(attachmentId)
        val type = DownloadType.Medium

        val response1 = mockk<Fhir3Attachment>()
        val response2 = mockk<Fhir3Attachment>()
        val response = listOf(response1, response2)

        val encryptedRecord = mockk<EncryptedRecord>()
        val decryptedRecord = mockk<DecryptedFhir3Record<Fhir3Resource>>()


        every { apiService.fetchRecord(ALIAS, userId, recordId) } returns Single.just(encryptedRecord)
        every { recordServiceK.decryptRecord<Any>(encryptedRecord, userId) } returns decryptedRecord as DecryptedBaseRecord<Any>
        every {
            recordServiceK.downloadAttachmentsFromStorage(
                    attachmentIds,
                    userId,
                    type,
                    decryptedRecord
            )
        } returns Single.just(response)

        // When
        val subscriber = recordServiceK.downloadAttachments(recordId, attachmentIds, userId, type).test().await()

        // Then
        val result = subscriber
                .assertNoErrors()
                .assertComplete()
                .assertValueCount(1)
                .values()[0]

        assertSame(
                response,
                result
        )
    }

    @Test
    @Throws(IOException::class,
            InterruptedException::class,
            DataValidationException.ModelVersionNotSupported::class)
    fun downloadAttachments_shouldThrow_whenInvalidAttachmentIdsProvided() {
        //given
        Mockito.`when`(mockApiService.fetchRecord(ALIAS, USER_ID, RECORD_ID))
                .thenReturn(Single.just(mockEncryptedRecord))
        val document = buildDocumentReference()
        document.content[0].attachment.id = ATTACHMENT_ID
        val decryptedRecord = DecryptedRecord(
                RECORD_ID,
                document,
                null,
                arrayListOf(),
                null,
                null,
                null,
                mockAttachmentKey,
                -1
        )
        Mockito.doReturn(decryptedRecord).`when`(recordService)
                .decryptRecord<Fhir3Resource>(mockEncryptedRecord, USER_ID)
        val attachmentIds = listOf(ATTACHMENT_ID, "invalidAttachmentId")

        //when
        val test = recordService.downloadAttachments(RECORD_ID, attachmentIds, USER_ID, DownloadType.Full).test().await()

        //then
        val errors = test.errors()
        Truth.assertThat(errors).hasSize(1)
        Truth.assertThat(errors[0]).isInstanceOf(DataValidationException.IdUsageViolation::class.java)
        Truth.assertThat(errors[0]!!.message).isEqualTo("Please provide correct attachment ids!")
    }

    @Test
    fun `Given, downloadAttachmentsFromStorage is called with AttachmentIds, a UserId, a DownloadType and a DecryptedRecord, it fails, if the there are no Attachments for the provided Resource`() {
        // Given
        val attachments = mockk<List<String>>()
        val decryptedRecord = mockk<DecryptedFhir3Record<Fhir3Resource>>(relaxed = true)
        val resource = mockk<Fhir3Resource>()

        every { decryptedRecord.resource } returns resource

        every { Fhir3AttachmentHelper.hasAttachment(resource) } returns false

        // When
        try {
            recordServiceK.downloadAttachmentsFromStorage(
                    attachments,
                    USER_ID,
                    DownloadType.Full,
                    decryptedRecord

            )
            Assert.fail("Exception expected!")
        } catch (e: IllegalArgumentException) {

            // Then
            Truth.assertThat(e.javaClass).isEqualTo(IllegalArgumentException::class.java)
            Truth.assertThat(e.message).isEqualTo("Expected a record of a type that has attachment")
        }
    }

    @Test
    fun `Given, downloadAttachmentsFromStorage is called with AttachmentIds, a UserId, a DownloadType and a DecryptedRecord, it fails, if the amount of the given attachments does not match the amount of the validated Attachments`() {
        // Given
        val attachments = listOf(
                "yes"
        )
        val decryptedRecord = mockk<DecryptedFhir3Record<Fhir3Resource>>(relaxed = true)
        val resource = mockk<Fhir3Resource>()

        val serviceAttachments = mutableListOf<Any>(
                mockk<Fhir3Attachment>()
        )

        val wrappedServiceAttachment = mockk<WrapperContract.Attachment>()

        every { decryptedRecord.resource } returns resource

        every { wrappedServiceAttachment.id } returns "no"

        every { Fhir3AttachmentHelper.hasAttachment(resource) } returns true
        every { Fhir3AttachmentHelper.getAttachment(resource) } returns serviceAttachments as MutableList<Attachment>

        // When
        try {
            recordServiceK.downloadAttachmentsFromStorage(
                    attachments,
                    USER_ID,
                    DownloadType.Full,
                    decryptedRecord

            )
            Assert.fail("Exception expected!")
        } catch (e: D4LException) {

            // Then
            Truth.assertThat(e.javaClass).isEqualTo(DataValidationException.IdUsageViolation::class.java)
            Truth.assertThat(e.message).isEqualTo("Please provide correct attachment ids!")
        }
    }

    @Test
    fun `Given, downloadAttachmentsFromStorage is called with AttachmentIds, a UserId, a DownloadType and a DecryptedRecord, it downloads the requested Attachments`() {
        // Given
        val attachments = listOf(
                "yes"
        )
        val decryptedRecord = mockk<DecryptedFhir3Record<Fhir3Resource>>(relaxed = true)
        val resource = mockk<Fhir3Resource>()
        val attachmentKey = mockk<GCKey>()
        val type = DownloadType.Full

        val downloadedResource = Fhir3Attachment()
        downloadedResource.id = "abc"

        val serviceAttachment = Fhir3Attachment()
        serviceAttachment.id = attachments[0]

        val ids = listOf<Fhir3Identifier>(
                mockk(),
                mockk(),
                mockk()
        )

        val downloadedAttachment = mockk<WrapperContract.Attachment>()

        every { decryptedRecord.resource } returns resource
        every { decryptedRecord.attachmentsKey } returns attachmentKey

        every { downloadedAttachment.id } returns "no split char"
        every { downloadedAttachment.unwrap<Fhir3Attachment>() } returns downloadedResource

        every { Fhir3AttachmentHelper.hasAttachment(resource) } returns true
        every { Fhir3AttachmentHelper.getAttachment(any()) } returns mutableListOf(serviceAttachment)
        every { Fhir3AttachmentHelper.getIdentifier(resource) } returns ids


        every {
            recordServiceK.setAttachmentIdForDownloadType(
                    any(),
                    ids,
                    type
            )
        } returns Unit

        every {
            attachmentService.download(
                    any(),
                    attachmentKey,
                    USER_ID
            )
        } returns Single.just(listOf(downloadedAttachment))

        every { recordServiceK.updateAttachmentMeta(any()) } returns mockk()

        // When
        val subscriber = recordServiceK.downloadAttachmentsFromStorage(
                attachments,
                USER_ID,
                type,
                decryptedRecord

        ).test().await()
        val result = subscriber
                .assertNoErrors()
                .assertComplete()
                .assertValueCount(1)
                .values()[0]

        // Then

        Truth.assertThat(result).isEqualTo(listOf(downloadedResource))


        verify(exactly = 1) {
            attachmentService.download(
                    any(),
                    attachmentKey,
                    USER_ID
            )
        }

        verify(exactly = 1) {
            recordServiceK.setAttachmentIdForDownloadType(
                    any(),
                    ids,
                    type
            )
        }

        verify(exactly = 0) { recordServiceK.updateAttachmentMeta(any()) }
    }

    @Test
    fun `Given, downloadAttachmentsFromStorage is called with AttachmentIds, a UserId, a DownloadType and a DecryptedRecord, it downloads the requested Attachments and updates their meta`() {
        // Given
        val attachments = listOf(
                "yes"
        )
        val decryptedRecord = mockk<DecryptedFhir3Record<Fhir3Resource>>(relaxed = true)
        val resource = mockk<Fhir3Resource>()
        val attachmentKey = mockk<GCKey>()
        val type = DownloadType.Full

        val downloadedResource = Fhir3Attachment()
        downloadedResource.id = "with ${SPLIT_CHAR} char"

        val serviceAttachment = Fhir3Attachment()
        serviceAttachment.id = attachments[0]

        val ids = listOf<Fhir3Identifier>(
                mockk(),
                mockk(),
                mockk()
        )

        val downloadedAttachment = mockk<WrapperContract.Attachment>()

        val spyedService = spyk(attachmentService)

        every { decryptedRecord.resource } returns resource
        every { decryptedRecord.attachmentsKey } returns attachmentKey

        every { downloadedAttachment.id } returns "no split char"
        every { downloadedAttachment.unwrap<Fhir3Attachment>() } returns downloadedResource

        every { Fhir3AttachmentHelper.hasAttachment(resource) } returns true
        every { Fhir3AttachmentHelper.getAttachment(any()) } returns mutableListOf(serviceAttachment)
        every { Fhir3AttachmentHelper.getIdentifier(resource) } returns ids


        every {
            recordServiceK.setAttachmentIdForDownloadType(
                    any(),
                    ids,
                    type
            )
        } returns Unit

        every {
            attachmentService.download(
                    any(),
                    attachmentKey,
                    USER_ID
            )
        } returns Single.just(listOf(downloadedAttachment))

        every { recordServiceK.updateAttachmentMeta(any()) } returns mockk()

        // When
        val subscriber = recordServiceK.downloadAttachmentsFromStorage(
                attachments,
                USER_ID,
                type,
                decryptedRecord

        ).test().await()
        val result = subscriber
                .assertNoErrors()
                .assertComplete()
                .assertValueCount(1)
                .values()[0]

        // Then
        Truth.assertThat(result).isEqualTo(listOf(downloadedResource))


        verify(exactly = 1) {
            attachmentService.download(
                    any(),
                    attachmentKey,
                    USER_ID
            )
        }

        verify(exactly = 1) {
            recordServiceK.setAttachmentIdForDownloadType(
                    any(),
                    ids,
                    type
            )
        }

        verify(exactly = 1) { recordServiceK.updateAttachmentMeta(any()) }
    }

    @Test
    fun `Given, updateResourceIdentifier is called, with a Resource and a Map of Attachments to a null for a Lists of String, it does noting`() {
        // Given
        val resource = mockk<Fhir4Resource>()
        val attachment = mockk<WrapperContract.Attachment>()

        every {
            Fhir4AttachmentHelper.appendIdentifier(
                    resource,
                    any(),
                    PARTNER_ID
            )
        } returns mockk()

        // When
        recordServiceK.updateFhirResourceIdentifier(
                resource,
                listOf<Pair<WrapperContract.Attachment, List<String>?>>(attachment to null)
        )

        verify(exactly = 0) {
            Fhir4AttachmentHelper.appendIdentifier(
                    resource,
                    any(),
                    PARTNER_ID
            )
        }
    }

    @Test
    fun `Given, updateResourceIdentifier is called, with a Resource and a Map of Attachments to a Lists of String, it appends the Identifier`() {
        // Given
        val attachment = mockk<WrapperContract.Attachment>()
        val resource = mockk<Fhir4Resource>(relaxed = true)

        every { attachment.id } returns "something"

        every {
            Fhir4AttachmentHelper.appendIdentifier(
                    resource,
                    "d4l_f_p_t#something#abc",
                    PARTNER_ID
            )
        } returns mockk()

        // When
        recordServiceK.updateFhirResourceIdentifier(
                resource,
                listOf<Pair<WrapperContract.Attachment, List<String>?>>(attachment to listOf("abc"))
        )

        verify(exactly = 1) {
            Fhir4AttachmentHelper.appendIdentifier(
                    resource,
                    "d4l_f_p_t#something#abc",
                    PARTNER_ID
            )
        }
    }
}
