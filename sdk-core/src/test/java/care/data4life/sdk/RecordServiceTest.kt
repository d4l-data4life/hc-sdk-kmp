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
import care.data4life.fhir.stu3.model.CarePlan
import care.data4life.fhir.stu3.model.DocumentReference
import care.data4life.fhir.stu3.model.DocumentReference.DocumentReferenceContent
import care.data4life.fhir.stu3.model.DomainResource
import care.data4life.fhir.stu3.model.Identifier
import care.data4life.fhir.stu3.model.Organization
import care.data4life.fhir.stu3.util.FhirAttachmentHelper
import care.data4life.sdk.config.DataRestriction.DATA_SIZE_MAX_BYTES
import care.data4life.sdk.config.DataRestrictionException
import care.data4life.sdk.lang.CoreRuntimeException
import care.data4life.sdk.lang.D4LException
import care.data4life.sdk.lang.DataValidationException
import care.data4life.sdk.model.DownloadType
import care.data4life.sdk.network.model.DecryptedRecord
import care.data4life.sdk.test.util.AttachmentBuilder
import care.data4life.sdk.util.Base64.encodeToString
import care.data4life.sdk.util.MimeType
import com.google.common.truth.Truth
import io.reactivex.Completable
import io.reactivex.Single
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers
import org.mockito.Mockito
import org.threeten.bp.LocalDate
import org.threeten.bp.LocalDateTime
import java.io.IOException
import java.util.*

class RecordServiceTest : RecordServiceTestBase() {
    @Before
    fun setup() {
        init()
    }

    @After
    fun tearDown() {
        stop()
    }

    //region utility methods
    @Test
    fun extractUploadData_shouldReturnExtractedData() {
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
        val attachment: Attachment? = null
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
    fun removeUploadData_shouldCall_removeOrRestoreUploadData() {
        // Given
        Mockito.doReturn(mockDecryptedRecord)
                .`when`(recordService)
                .removeOrRestoreUploadData(
                        RecordService.RemoveRestoreOperation.REMOVE,
                        mockDecryptedRecord,
                        null,
                        null
                )

        // When
        val record = recordService.removeUploadData(mockDecryptedRecord)

        // Then
        Truth.assertThat(record).isEqualTo(mockDecryptedRecord)
        inOrder.verify(recordService).removeUploadData(mockDecryptedRecord)
        inOrder.verify(recordService).removeOrRestoreUploadData(
                RecordService.RemoveRestoreOperation.REMOVE, 
                mockDecryptedRecord, 
                null,
                null
        )
        inOrder.verifyNoMoreInteractions()
    }

    @Test
    fun restoreUploadData_shouldCall_removeOrRestoreUploadData() {
        // Given
        Mockito.doReturn(mockDecryptedRecord)
                .`when`(recordService)
                .removeOrRestoreUploadData(RecordService.RemoveRestoreOperation.RESTORE,
                        mockDecryptedRecord,
                        mockDocumentReference,
                        mockUploadData
                )

        // When
        val record = recordService.restoreUploadData(
                mockDecryptedRecord,
                mockDocumentReference,
                mockUploadData
        )

        // Then
        Truth.assertThat(record).isEqualTo(mockDecryptedRecord)
        inOrder.verify(recordService).restoreUploadData(
                mockDecryptedRecord,
                mockDocumentReference,
                mockUploadData
        )
        inOrder.verify(recordService).removeOrRestoreUploadData(RecordService.RemoveRestoreOperation.RESTORE,
                mockDecryptedRecord,
                mockDocumentReference,
                mockUploadData
        )
        inOrder.verifyNoMoreInteractions()
    }

    @Test
    fun removeOrRestoreUploadData_shouldRemoveUploadData() {
        // Given
        val document = buildDocumentReference()
        val decryptedRecord = DecryptedRecord(
                null,
                document,
                null,
                ArrayList(),
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
                document,
                mockUploadData
        )

        // Then
        Truth.assertThat(record).isEqualTo(decryptedRecord)
        Truth.assertThat(record.resource).isEqualTo(document)
        Truth.assertThat(document.content[0].attachment.data).isNull()
        inOrder.verify(recordService).removeOrRestoreUploadData(
                RecordService.RemoveRestoreOperation.REMOVE,
                decryptedRecord, 
                document,
                mockUploadData
        )
        inOrder.verifyNoMoreInteractions()
    }

    @Test
    fun removeOrRestoreUploadData_shouldRestoreUploadData() {
        // Given
        val document = buildDocumentReference()
        document.content[0].attachment.data = null
        val decryptedRecord = DecryptedRecord(
                null,
                null,
                null,
                ArrayList(),
                null,
                null,
                null,
                null,
                -1
        )
        val uploadData = HashMap<Attachment, String?>()
        uploadData[document.content[0].attachment] = DATA

        // When
        @Suppress("UNCHECKED_CAST") 
        val record = recordService.removeOrRestoreUploadData(
                RecordService.RemoveRestoreOperation.RESTORE,
                decryptedRecord as DecryptedRecord<DocumentReference>, 
                document, 
                uploadData
        )

        // Then
        Truth.assertThat(record).isEqualTo(decryptedRecord)
        Truth.assertThat(record.resource).isEqualTo(document)
        Truth.assertThat(document.content[0].attachment.data).isEqualTo(DATA)
        inOrder.verify(recordService).removeOrRestoreUploadData(
                RecordService.RemoveRestoreOperation.RESTORE,
                decryptedRecord, 
                document,
                uploadData
        )
        inOrder.verifyNoMoreInteractions()
    }

    @Test
    @Throws(DataValidationException.ExpectedFieldViolation::class,
            DataValidationException.IdUsageViolation::class,
            DataValidationException.InvalidAttachmentPayloadHash::class)
    fun uploadOrDownloadData_calls__uploadData() {
        // Given
        Mockito.doReturn(mockDecryptedRecord).`when`(recordService)._uploadData(
                mockDecryptedRecord,
                USER_ID
        )

        // When
        val record = recordService.uploadOrDownloadData(RecordService.UploadDownloadOperation.UPLOAD,
                mockDecryptedRecord,
                null,
                USER_ID
        )

        // Then
        Truth.assertThat(record).isEqualTo(mockDecryptedRecord)
        inOrder.verify(recordService)._uploadData(mockDecryptedRecord, USER_ID)
        inOrder.verifyNoMoreInteractions()
    }

    @Test
    @Throws(DataValidationException.ExpectedFieldViolation::class,
            DataValidationException.IdUsageViolation::class,
            DataValidationException.InvalidAttachmentPayloadHash::class)
    fun uploadData_calls__uploadData_when_no_new_resource_was_given() {
        // Given
        Mockito.doReturn(mockDecryptedRecord).`when`(recordService)._uploadData(
                mockDecryptedRecord,
                USER_ID
        )

        // When
        val record = recordService.uploadData(mockDecryptedRecord, null, USER_ID)

        // Then
        Truth.assertThat(record).isEqualTo(mockDecryptedRecord)
        inOrder.verify(recordService).uploadData(mockDecryptedRecord, null, USER_ID)
        inOrder.verify(recordService)._uploadData(mockDecryptedRecord, USER_ID)
        inOrder.verifyNoMoreInteractions()
    }

    @Test
    @Throws(DataValidationException.ExpectedFieldViolation::class,
            DataValidationException.IdUsageViolation::class,
            DataValidationException.InvalidAttachmentPayloadHash::class)
    fun _uploadData_uploads_data() {
        // Given
        val document = buildDocumentReference()
        val decryptedRecord = DecryptedRecord(
                RECORD_ID,
                document,
                null,
                ArrayList(),
                null,
                null,
                null,
                null,
                -1
        )
        val downscaledIds = listOf("downscaledId_1", "downscaledId_2")
        val uploadResult = listOf(Pair(document.content[0].attachment, downscaledIds))
        Mockito.`when`(mockCryptoService.generateGCKey()).thenReturn(Single.just(mockAttachmentKey))
        Mockito.`when`(mockCryptoService.generateGCKey()).thenReturn(Single.just(mockAttachmentKey))
        Mockito.`when`(recordService.getValidHash(document.content[0].attachment)).thenReturn(DATA_HASH)
        Mockito.`when`(mockAttachmentService.uploadAttachments(
                ArgumentMatchers.any(),
                ArgumentMatchers.eq(mockAttachmentKey),
                ArgumentMatchers.eq(USER_ID))
        ).thenReturn(Single.just(uploadResult))

        // When
        val record = recordService._uploadData(decryptedRecord, USER_ID)

        // Then
        Truth.assertThat(record).isEqualTo(decryptedRecord)
        Truth.assertThat(record.attachmentsKey).isEqualTo(mockAttachmentKey)
        inOrder.verify(recordService)._uploadData(decryptedRecord, USER_ID)
        inOrder.verify(mockCryptoService).generateGCKey()
        inOrder.verify(recordService).getValidHash(document.content[0].attachment)
        inOrder.verify(mockAttachmentService).uploadAttachments(
                ArgumentMatchers.any(),
                ArgumentMatchers.eq(mockAttachmentKey),
                ArgumentMatchers.eq(USER_ID)
        )
        inOrder.verifyNoMoreInteractions()
    }

    @Test
    @Throws(DataValidationException.ExpectedFieldViolation::class,
            DataValidationException.IdUsageViolation::class,
            DataValidationException.InvalidAttachmentPayloadHash::class)
    fun _uploadData_throws_whenAttachmentIdIsSetDuringUploadFlow() {
        // Given
        val document = buildDocumentReference()
        document.content[0].attachment.id = "unexpectedId"
        val decryptedRecord = DecryptedRecord(
                RECORD_ID,
                document,
                null,
                ArrayList(),
                null,
                null,
                null,
                null,
                -1
        )
        Mockito.`when`(mockCryptoService.generateGCKey()).thenReturn(Single.just(mockAttachmentKey))

        // When
        try {
            recordService._uploadData(decryptedRecord, USER_ID)
            Assert.fail("Exception expected!")
        } catch (e: D4LException) {

            // Then
            Truth.assertThat(e.javaClass).isEqualTo(DataValidationException.IdUsageViolation::class.java)
            Truth.assertThat(e.message).isEqualTo("Attachment.id should be null")
        }
        inOrder.verify(recordService)._uploadData(decryptedRecord, USER_ID)
        inOrder.verify(mockCryptoService).generateGCKey()
        inOrder.verifyNoMoreInteractions()
    }

    @Test
    @Throws(DataValidationException.ExpectedFieldViolation::class,
            DataValidationException.IdUsageViolation::class,
            DataValidationException.InvalidAttachmentPayloadHash::class)
    fun _uploadData_throws_whenInvalidHashAttachmentDuringUploadFlow() {
        // Given
        val document = buildDocumentReference()
        document.content[0].attachment.id = null
        document.content[0].attachment.hash = "hash"
        val decryptedRecord = DecryptedRecord(
                RECORD_ID,
                document,
                null,
                ArrayList(),
                null,
                null,
                null,
                null,
                -1
        )
        Mockito.`when`(mockCryptoService.generateGCKey()).thenReturn(Single.just(mockAttachmentKey))

        // When
        try {
            recordService._uploadData(decryptedRecord, USER_ID)
            Assert.fail("Exception expected!")
        } catch (e: D4LException) {

            // Then
            Truth.assertThat(e.javaClass).isEqualTo(DataValidationException.InvalidAttachmentPayloadHash::class.java)
            Truth.assertThat(e.message).isEqualTo("Attachment.hash is not valid")
        }
        inOrder.verify(recordService)._uploadData(decryptedRecord, USER_ID)
        inOrder.verify(mockCryptoService).generateGCKey()
        inOrder.verify(recordService).getValidHash(document.content[0].attachment)
        inOrder.verifyNoMoreInteractions()
    }

    @Test
    @Throws(DataValidationException.IdUsageViolation::class,
            DataValidationException.ExpectedFieldViolation::class,
            DataValidationException.InvalidAttachmentPayloadHash::class)
    fun _uploadData_appends_AdditionalIdentifiers() {
        //given
        val docRef = buildDocumentReference()
        docRef.content.add(buildDocRefContent(AttachmentBuilder.buildAttachment(null)))
        val firstAttachment = docRef.content[0].attachment
        firstAttachment.title = "image"
        val secondAttachment = docRef.content[1].attachment
        secondAttachment.title = "pdf"
        val uploadResult: MutableList<Pair<Attachment, List<String>?>> = ArrayList()
        uploadResult.add(Pair(firstAttachment, listOf(PREVIEW_ID, THUMBNAIL_ID)))
        uploadResult.add(Pair<Attachment, List<String>?>(secondAttachment, null))
        val dummyDecryptedRecord = DecryptedRecord(
                RECORD_ID,
                docRef,
                null,
                ArrayList(),
                null,
                null,
                null,
                null,
                -1
        )
        Mockito.`when`(mockCryptoService.generateGCKey()).thenReturn(Single.just(mockAttachmentKey))
        Mockito.`when`(recordService.getValidHash(secondAttachment)).thenReturn(DATA_HASH)
        Mockito.`when`(recordService.getValidHash(firstAttachment)).thenReturn(DATA_HASH)
        Mockito.`when`(mockAttachmentService.uploadAttachments(
                ArgumentMatchers.any(),
                ArgumentMatchers.eq(mockAttachmentKey),
                ArgumentMatchers.eq(USER_ID))
        ).thenReturn(Single.fromCallable {
            firstAttachment.id = ATTACHMENT_ID
            secondAttachment.id = ATTACHMENT_ID
            uploadResult
        })

        //when
        val doc = recordService._uploadData(
                dummyDecryptedRecord,
                USER_ID
        ).resource

        //then
        Truth.assertThat(doc).isEqualTo(docRef)
        Truth.assertThat(doc.identifier).hasSize(1)
        Truth.assertThat(doc.identifier!![0].value).isEqualTo(
                RecordService.DOWNSCALED_ATTACHMENT_IDS_FMT + "#" + ATTACHMENT_ID + "#" +
                        PREVIEW_ID + "#" + THUMBNAIL_ID
        )
        Truth.assertThat(doc.identifier!![0].assigner!!.reference).isEqualTo(PARTNER_ID)
    }

    @Test
    @Throws(DataValidationException.ExpectedFieldViolation::class,
            DataValidationException.IdUsageViolation::class,
            DataValidationException.InvalidAttachmentPayloadHash::class)
    fun uploadOrDownloadData_calls_updateData() {
        // Given
        Mockito.doReturn(mockDecryptedRecord).`when`(recordService).updateData(
                mockDecryptedRecord,
                mockDocumentReference,
                USER_ID
        )

        // When
        val record = recordService.uploadOrDownloadData(RecordService.UploadDownloadOperation.UPDATE,
                mockDecryptedRecord,
                mockDocumentReference,
                USER_ID
        )

        // Then
        Truth.assertThat(record).isEqualTo(mockDecryptedRecord)
        inOrder.verify(recordService).updateData(
                mockDecryptedRecord,
                mockDocumentReference,
                USER_ID
        )
        inOrder.verifyNoMoreInteractions()
    }

    @Test
    @Throws(DataValidationException.ExpectedFieldViolation::class,
            DataValidationException.IdUsageViolation::class,
            DataValidationException.InvalidAttachmentPayloadHash::class)
    fun uploadData_calls_updateData_when_a_new_resource_was_given() {
        // Given
        Mockito.doReturn(mockDecryptedRecord).`when`(recordService).updateData(
                mockDecryptedRecord,
                mockDocumentReference,
                USER_ID
        )

        // When
        val record = recordService.uploadData(
                mockDecryptedRecord,
                mockDocumentReference,
                USER_ID
        )

        // Then
        Truth.assertThat(record).isEqualTo(mockDecryptedRecord)
        inOrder.verify(recordService).uploadData(
                mockDecryptedRecord,
                mockDocumentReference,
                USER_ID
        )
        inOrder.verify(recordService).updateData(
                mockDecryptedRecord,
                mockDocumentReference,
                USER_ID
        )
        inOrder.verifyNoMoreInteractions()
    }

    @Test
    @Throws(DataValidationException.ExpectedFieldViolation::class,
            DataValidationException.IdUsageViolation::class,
            DataValidationException.InvalidAttachmentPayloadHash::class)
    fun updateData_updates_data() {
        // Given
        val oldDocument = buildDocumentReference()
        val oldAttachment = oldDocument.content[0].attachment
        oldAttachment.id = "id"
        oldAttachment.size = 0
        oldAttachment.hash = "hash"
        val updatedHash = "hash2"
        val updatedDocument = buildDocumentReference()
        val updatedAttachment = updatedDocument.content[0].attachment
        updatedAttachment.id = "id"
        updatedAttachment.size = 0
        updatedAttachment.hash = updatedHash
        val decryptedRecord = DecryptedRecord(
                RECORD_ID,
                oldDocument,
                null,
                ArrayList(),
                null,
                null,
                null,
                mockAttachmentKey,
                -1
        )
        val downscaledIds = listOf("downscaledId_1", "downscaledId_2")
        val uploadResult = listOf(
                Pair(updatedDocument.content[0].attachment, downscaledIds)
        )
        Mockito.`when`(mockCryptoService.generateGCKey()).thenReturn(Single.just(mockAttachmentKey))
        Mockito.`when`(mockCryptoService.generateGCKey()).thenReturn(Single.just(mockAttachmentKey))
        Mockito.`when`(recordService.getValidHash(updatedDocument.content[0].attachment))
                .thenReturn(DATA_HASH)
        Mockito.`when`(mockAttachmentService.uploadAttachments(
                ArgumentMatchers.eq(listOf(updatedAttachment)),
                ArgumentMatchers.eq(mockAttachmentKey),
                ArgumentMatchers.eq(USER_ID))
        ).thenReturn(Single.just(uploadResult))
        Mockito.`when`(recordService.getValidHash(updatedDocument.content[0].attachment)).thenReturn(updatedHash)

        // When
        recordService.updateData(decryptedRecord, updatedDocument, USER_ID)

        // Then
        inOrder.verify(recordService).updateData(decryptedRecord, updatedDocument, USER_ID)
        inOrder.verify(recordService).getValidHash(updatedDocument.content[0].attachment)
        inOrder.verify(mockAttachmentService).uploadAttachments(
                ArgumentMatchers.eq(listOf(updatedAttachment)),
                ArgumentMatchers.eq(mockAttachmentKey),
                ArgumentMatchers.eq(USER_ID)
        )
        inOrder.verifyNoMoreInteractions()
    }

    @Test
    @Throws(DataValidationException.ExpectedFieldViolation::class,
            DataValidationException.IdUsageViolation::class,
            DataValidationException.InvalidAttachmentPayloadHash::class)
    fun updateData_throws_CoreRuntimeExceptionUnsupportedOperation_whenNewResourceIsNull() {
        // Given
        val document = buildDocumentReference()
        val decryptedRecord = DecryptedRecord(
                RECORD_ID,
                document,
                null,
                ArrayList(),
                null,
                null,
                null,
                null,
                -1
        )
        try {
            // When
            recordService.updateData(decryptedRecord, null, USER_ID)
        } catch (e: CoreRuntimeException) {
            // Then
            Truth.assertThat(e.javaClass).isEqualTo(CoreRuntimeException.UnsupportedOperation::class.java)
        }
    }

    @Test
    @Throws(DataValidationException.ExpectedFieldViolation::class, DataValidationException.IdUsageViolation::class, DataValidationException.InvalidAttachmentPayloadHash::class)
    fun updateData_throws_whenAttachmentHashOrSizeNotPresent() {
        // Given
        val document = buildDocumentReference()
        val decryptedRecord = DecryptedRecord(
                RECORD_ID,
                document,
                null,
                ArrayList(),
                null,
                null,
                null,
                null,
                -1
        )

        // When
        try {
            document.content[0].attachment.hash = null
            document.content[0].attachment.size = 0
            recordService.updateData(decryptedRecord, document, USER_ID)
            Assert.fail("Exception expected!")
        } catch (e: D4LException) {

            // Then
            Truth.assertThat(e.javaClass).isEqualTo(DataValidationException.ExpectedFieldViolation::class.java)
            Truth.assertThat(e.message).isEqualTo("Attachment.hash and Attachment.size expected")
        }
        try {
            document.content[0].attachment.hash = "hash"
            document.content[0].attachment.size = null
            recordService.updateData(decryptedRecord, document, USER_ID)
            Assert.fail("Exception expected!")
        } catch (e: D4LException) {

            // Then
            Truth.assertThat(e.javaClass).isEqualTo(DataValidationException.ExpectedFieldViolation::class.java)
            Truth.assertThat(e.message).isEqualTo("Attachment.hash and Attachment.size expected")
        }
        inOrder.verify(recordService, Mockito.times(2)).updateData(decryptedRecord, document, USER_ID)
        inOrder.verifyNoMoreInteractions()
    }

    @Test
    @Throws(DataValidationException.ExpectedFieldViolation::class,
            DataValidationException.IdUsageViolation::class,
            DataValidationException.InvalidAttachmentPayloadHash::class)
    fun updateData_throws_whenInvalidHashAttachmentDuringUpdateFlow() {
        // Given
        val document = buildDocumentReference()
        val decryptedRecord = DecryptedRecord(
                RECORD_ID,
                document,
                null,
                ArrayList(),
                null,
                null,
                null,
                null,
                -1
        )

        // When
        try {
            document.content[0].attachment.hash = "hash"
            recordService.updateData(decryptedRecord, document, USER_ID)
            Assert.fail("Exception expected!")
        } catch (e: D4LException) {

            // Then
            Truth.assertThat(e.javaClass).isEqualTo(DataValidationException.InvalidAttachmentPayloadHash::class.java)
            Truth.assertThat(e.message).isEqualTo("Attachment.hash is not valid")
        }
        inOrder.verify(recordService).updateData(decryptedRecord, document, USER_ID)
        inOrder.verify(recordService).getValidHash(document.content[0].attachment)
        inOrder.verifyNoMoreInteractions()
    }

    @Test
    @Throws(DataValidationException.IdUsageViolation::class,
            DataValidationException.ExpectedFieldViolation::class,
            DataValidationException.InvalidAttachmentPayloadHash::class)
    fun updateData_throws_whenOldAttachmentHashNotPresent() {
        // Given
        val oldDocument = buildDocumentReference()
        val oldAttachment = oldDocument.content[0].attachment
        oldAttachment.id = "id"
        oldAttachment.size = null
        oldAttachment.hash = null
        val updatedDocument = buildDocumentReference()
        val updatedAttachment = updatedDocument.content[0].attachment
        updatedAttachment.id = "id"
        updatedAttachment.size = 0
        updatedAttachment.hash = "hash"
        val decryptedRecord = DecryptedRecord(
                RECORD_ID,
                oldDocument,
                null,
                ArrayList(),
                null,
                null,
                null,
                mockAttachmentKey,
                -1
        )
        val uploadResult: MutableList<Pair<Attachment, List<String>>> = ArrayList()
        val downscaledIds = listOf("downscaledId_1", "downscaledId_2")
        uploadResult.add(Pair(updatedAttachment, downscaledIds))
        Mockito.`when`(mockAttachmentService.uploadAttachments(
                ArgumentMatchers.eq(listOf(updatedAttachment)),
                ArgumentMatchers.eq(mockAttachmentKey),
                ArgumentMatchers.eq(USER_ID))
        ).thenReturn(Single.just(uploadResult))

        // When
        try {
            recordService.updateData(decryptedRecord, updatedDocument, USER_ID)
            Assert.fail("Exception expected!")
        } catch (e: D4LException) {

            // Then
            Truth.assertThat(e.javaClass).isEqualTo(DataValidationException.InvalidAttachmentPayloadHash::class.java)
            Truth.assertThat(e.message).isEqualTo("Attachment.hash is not valid")
        }


        // Then
        inOrder.verify(recordService).updateData(decryptedRecord, updatedDocument, USER_ID)
        inOrder.verify(recordService).getValidHash(updatedDocument.content[0].attachment)
        inOrder.verifyNoMoreInteractions()
    }

    @Test
    @Throws(DataValidationException.IdUsageViolation::class,
            DataValidationException.ExpectedFieldViolation::class,
            DataValidationException.InvalidAttachmentPayloadHash::class)
    fun updateData_throws_Attachment_whenHashesDontMatch() {
        // Given
        val oldDocument = buildDocumentReference()
        val oldAttachment = oldDocument.content[0].attachment
        oldAttachment.id = "id"
        oldAttachment.size = 0
        oldAttachment.hash = "oldHash"
        val updatedDocument = buildDocumentReference()
        val updatedAttachment = updatedDocument.content[0].attachment
        updatedAttachment.id = "id"
        updatedAttachment.size = 0
        updatedAttachment.hash = "newHash"
        val decryptedRecord = DecryptedRecord(
                RECORD_ID,
                oldDocument,
                null,
                ArrayList(),
                null,
                null,
                null,
                mockAttachmentKey,
                -1
        )
        val uploadResult: MutableList<Pair<Attachment, List<String>>> = ArrayList()
        val downscaledIds = listOf("downscaledId_1", "downscaledId_2")
        uploadResult.add(Pair(updatedAttachment, downscaledIds))
        Mockito.`when`(mockAttachmentService.uploadAttachments(
                ArgumentMatchers.eq(listOf(updatedAttachment)),
                ArgumentMatchers.eq(mockAttachmentKey),
                ArgumentMatchers.eq(USER_ID))
        ).thenReturn(Single.just(uploadResult))


        // When
        try {
            recordService.updateData(decryptedRecord, updatedDocument, USER_ID)
            Assert.fail("Exception expected!")
        } catch (e: D4LException) {

            // Then
            Truth.assertThat(e.javaClass).isEqualTo(DataValidationException.InvalidAttachmentPayloadHash::class.java)
            Truth.assertThat(e.message).isEqualTo("Attachment.hash is not valid")
        }

        // Then
        inOrder.verify(recordService).updateData(decryptedRecord, updatedDocument, USER_ID)
        inOrder.verify(recordService).getValidHash(updatedDocument.content[0].attachment)
        inOrder.verifyNoMoreInteractions()
    }

    @Test
    @Throws(DataValidationException.ExpectedFieldViolation::class,
            DataValidationException.IdUsageViolation::class,
            DataValidationException.InvalidAttachmentPayloadHash::class)
    fun updateData_throws_whenValidAttachmentIdNotPresent() {
        // Given
        val oldDocument = buildDocumentReference()
        val oldAttachment = oldDocument.content[0].attachment
        oldAttachment.id = "id1"
        oldAttachment.size = 0
        oldAttachment.hash = "hash"
        val updatedDocument = buildDocumentReference()
        val updatedAttachment = updatedDocument.content[0].attachment
        updatedAttachment.id = "id2"
        updatedAttachment.size = 0
        updatedAttachment.hash = "hash"
        val decryptedRecord = DecryptedRecord(
                RECORD_ID,
                oldDocument,
                null,
                ArrayList(),
                null,
                null,
                null,
                null,
                -1
        )
        Mockito.`when`(recordService.getValidHash(updatedDocument.content[0].attachment))
                .thenReturn("hash")
        // When
        try {
            recordService.updateData(decryptedRecord, updatedDocument, USER_ID)
            Assert.fail("Exception expected!")
        } catch (e: D4LException) {

            // Then
            Truth.assertThat(e.javaClass).isEqualTo(DataValidationException.IdUsageViolation::class.java)
            Truth.assertThat(e.message).isEqualTo("Valid Attachment.id expected")
        }
        inOrder.verify(recordService).updateData(decryptedRecord, updatedDocument, USER_ID)
        inOrder.verify(recordService).getValidHash(updatedDocument.content[0].attachment)
        inOrder.verifyNoMoreInteractions()
    }

    @Test
    @Throws(DataValidationException.InvalidAttachmentPayloadHash::class,
            DataValidationException.ExpectedFieldViolation::class,
            DataValidationException.IdUsageViolation::class)
    fun updateData_notUploadsAttachment_whenHashesMatch() {
        // Given
        val oldDocument = buildDocumentReference()
        val oldAttachment = oldDocument.content[0].attachment
        oldAttachment.id = "id"
        oldAttachment.size = 0
        oldAttachment.hash = "hash"
        val updatedDocument = buildDocumentReference()
        val updatedAttachment = updatedDocument.content[0].attachment
        updatedAttachment.id = "id"
        updatedAttachment.size = 0
        updatedAttachment.hash = "hash"
        val decryptedRecord = DecryptedRecord(
                RECORD_ID,
                oldDocument,
                null,
                ArrayList(),
                null,
                null,
                null,
                mockAttachmentKey,
                -1
        )
        Mockito.`when`(recordService.getValidHash(updatedDocument.content[0].attachment))
                .thenReturn("hash")
        // When
        recordService.updateData(decryptedRecord, updatedDocument, USER_ID)

        // Then
        inOrder.verify(recordService).updateData(decryptedRecord, updatedDocument, USER_ID)
        inOrder.verify(recordService).getValidHash(updatedDocument.content[0].attachment)
        inOrder.verifyNoMoreInteractions()
    }

    @Test
    @Throws(DataValidationException.ExpectedFieldViolation::class,
            DataValidationException.IdUsageViolation::class,
            DataValidationException.InvalidAttachmentPayloadHash::class)
    fun uploadOrDownloadData_calls_downloadData() {
        // Given
        Mockito.doReturn(mockDecryptedRecord).`when`(recordService).downloadData(mockDecryptedRecord, USER_ID)

        // When
        val record = recordService.uploadOrDownloadData(RecordService.UploadDownloadOperation.DOWNLOAD,
                mockDecryptedRecord,
                null,
                USER_ID
        )

        // Then
        Truth.assertThat(record).isEqualTo(mockDecryptedRecord)
        inOrder.verify(recordService).uploadOrDownloadData(RecordService.UploadDownloadOperation.DOWNLOAD,
                mockDecryptedRecord,
                null,
                USER_ID
        )
        inOrder.verify(recordService).downloadData(mockDecryptedRecord, USER_ID)
        inOrder.verifyNoMoreInteractions()
    }

    @Test
    @Throws(DataValidationException.IdUsageViolation::class, DataValidationException.InvalidAttachmentPayloadHash::class)
    fun downloadData_shouldDownloadData() {
        // Given
        val document = buildDocumentReference()
        document.content[0].attachment.id = "id"
        val decryptedRecord = DecryptedRecord(
                RECORD_ID,
                document,
                null,
                ArrayList(),
                null,
                null,
                null,
                mockAttachmentKey,
                -1
        )
        Mockito.`when`(mockAttachmentService.downloadAttachments(
                ArgumentMatchers.any(),
                ArgumentMatchers.eq(mockAttachmentKey),
                ArgumentMatchers.eq(USER_ID))
        ).thenReturn(Single.just(ArrayList()))

        // When
        val record = recordService.downloadData(decryptedRecord, USER_ID)

        // Then
        Truth.assertThat(record).isEqualTo(decryptedRecord)
        inOrder.verify(recordService).downloadData(decryptedRecord, USER_ID)
        inOrder.verify(mockAttachmentService).downloadAttachments(
                ArgumentMatchers.any(),
                ArgumentMatchers.eq(mockAttachmentKey),
                ArgumentMatchers.eq(USER_ID)
        )
        inOrder.verifyNoMoreInteractions()
    }

    @Test
    @Throws(DataValidationException.ExpectedFieldViolation::class,
            DataValidationException.IdUsageViolation::class,
            DataValidationException.InvalidAttachmentPayloadHash::class)
    fun downloadData_shouldThrow_whenAttachmentIdIsNotSetDuringDownloadFlow() {
        // Given
        val document = buildDocumentReference()
        document.content[0].attachment.id = null
        val decryptedRecord = DecryptedRecord(
                RECORD_ID,
                document,
                null,
                ArrayList(),
                null,
                null,
                null,
                null,
                -1
        )

        // When
        try {
            recordService.downloadData(decryptedRecord, USER_ID)
            Assert.fail("Exception expected!")
        } catch (e: D4LException) {

            // Then
            Truth.assertThat(e.javaClass).isEqualTo(DataValidationException.IdUsageViolation::class.java)
            Truth.assertThat(e.message).isEqualTo("Attachment.id expected")
        }
        inOrder.verify(recordService).downloadData(decryptedRecord, USER_ID)
        inOrder.verifyNoMoreInteractions()
    }

    @Test
    @Throws(DataValidationException.IdUsageViolation::class)
    fun cleanObsoleteAdditionalIdentifiers_shouldCleanObsoleteIdentifiers() {
        //given
        val currentId = ADDITIONAL_ID
        val obsoleteId = ADDITIONAL_ID.replaceFirst(ATTACHMENT_ID.toRegex(), "obsoleteId")
        val otherId = "otherId"
        val currentIdentifier = FhirAttachmentHelper.buildIdentifier(currentId, ASSIGNER)
        val obsoleteIdentifier = FhirAttachmentHelper.buildIdentifier(obsoleteId, ASSIGNER)
        val otherIdentifier = FhirAttachmentHelper.buildIdentifier(otherId, ASSIGNER)
        val identifiers: MutableList<Identifier> = ArrayList()
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
        val additionalId = FhirAttachmentHelper.buildIdentifier(ADDITIONAL_ID, ASSIGNER)
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
        Truth.assertThat(attachment.id).isEqualTo(ATTACHMENT_ID + RecordService.SPLIT_CHAR + PREVIEW_ID)

        //given
        attachment.id = ATTACHMENT_ID
        //when downloadType is Small
        recordService.setAttachmentIdForDownloadType(attachments, identifiers, DownloadType.Small)
        //then
        Truth.assertThat(attachment.id).isEqualTo(ATTACHMENT_ID + RecordService.SPLIT_CHAR + THUMBNAIL_ID)
    }

    @Test
    @Throws(DataValidationException.IdUsageViolation::class)
    fun extractAdditionalAttachmentIds_shouldExtractAdditionalIds() {
        //given
        val additionalIdentifier = FhirAttachmentHelper.buildIdentifier(ADDITIONAL_ID, ASSIGNER)

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
        val identifier = FhirAttachmentHelper.buildIdentifier("otherId", ASSIGNER)

        //when
        val additionalIds = recordService.extractAdditionalAttachmentIds(listOf(identifier), ATTACHMENT_ID)

        //then
        Truth.assertThat(additionalIds).isNull()
    }

    @Test
    @Throws(DataValidationException.IdUsageViolation::class)
    fun splitAdditionalAttachmentId_shouldSplitAdditionalId() {
        //given
        val additionalIdentifier = FhirAttachmentHelper.buildIdentifier(ADDITIONAL_ID, ASSIGNER)

        //when
        val additionalIds = recordService.splitAdditionalAttachmentId(additionalIdentifier)

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
        val additionalIdentifier = FhirAttachmentHelper.buildIdentifier(null, ASSIGNER)
        //when
        val additionalIds = recordService.splitAdditionalAttachmentId(additionalIdentifier)
        //then
        Truth.assertThat(additionalIds).isNull()
    }

    @Test
    @Throws(DataValidationException.IdUsageViolation::class)
    fun splitAdditionalAttachmentId_shouldReturnNull_whenAdditionalIdentifierIsNotAdditionalAttachmentId() {
        //given
        val additionalIdentifier = FhirAttachmentHelper.buildIdentifier("otherId", ASSIGNER)

        //when
        val additionalIds = recordService.splitAdditionalAttachmentId(additionalIdentifier)

        //then
        Truth.assertThat(additionalIds).isNull()
    }

    @Test
    fun splitAdditionalAttachmentId_shouldThrow_whenAdditionalAttachmentIdIsMalformed() {
        //given
        val malformedAdditionalId = ADDITIONAL_ID + RecordService.SPLIT_CHAR + "unexpectedId"
        val additionalIdentifier = FhirAttachmentHelper.buildIdentifier(malformedAdditionalId, ASSIGNER)

        //when
        try {
            recordService.splitAdditionalAttachmentId(additionalIdentifier)
            Assert.fail("Exception expected!")
        } catch (ex: DataValidationException.IdUsageViolation) {

            //then
            Truth.assertThat(ex.message).isEqualTo(malformedAdditionalId)
        }
    }

    @Test
    fun updateAttachmentMeta_shouldUpdateAttachmentMeta() {
        //given
        val attachment = Attachment()
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
    fun checkForUnsupportedData_shouldReturnSuccessfully() {
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
    fun checkForUnsupportedData_shouldThrow_forUnsupportedData() {
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
    fun checkForUnsupportedData_shouldThrow_whenFileSizeLimitIsReached() {
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
    fun buildMeta_shouldBuildMeta_whenUpdatedDateMillisecondsArePresent() {
        // Given
        val updatedDateWithMilliseconds = "2019-02-28T17:21:08.234123"
        Mockito.`when`(mockDecryptedRecord.customCreationDate).thenReturn("2019-02-28")
        Mockito.`when`(mockDecryptedRecord.updatedDate).thenReturn(updatedDateWithMilliseconds)

        // When
        val meta = recordService.buildMeta(mockDecryptedRecord)

        // Then
        Truth.assertThat(meta.createdDate).isEqualTo(LocalDate.of(2019, 2, 28))
        Truth.assertThat(meta.updatedDate).isEqualTo(LocalDateTime.of(2019, 2, 28, 17, 21, 8, 234123000))
        inOrder.verify(recordService).buildMeta(mockDecryptedRecord)
        inOrder.verifyNoMoreInteractions()
    }

    @Test
    fun buildMeta_shouldBuildMeta_whenUpdatedDateMillisecondsAreNotPresent() {
        // Given
        val updatedDateWithMilliseconds = "2019-02-28T17:21:08"
        Mockito.`when`(mockDecryptedRecord.customCreationDate).thenReturn("2019-02-28")
        Mockito.`when`(mockDecryptedRecord.updatedDate).thenReturn(updatedDateWithMilliseconds)

        // When
        val meta = recordService.buildMeta(mockDecryptedRecord)

        // Then
        Truth.assertThat(meta.createdDate).isEqualTo(
                LocalDate.of(2019, 2, 28)
        )
        Truth.assertThat(meta.updatedDate).isEqualTo(
                LocalDateTime.of(2019, 2, 28, 17, 21, 8)
        )
        inOrder.verify(recordService).buildMeta(mockDecryptedRecord)
        inOrder.verifyNoMoreInteractions()
    }

    //endregion
    @Test
    @Throws(InterruptedException::class)
    fun deleteRecord_shouldDeleteRecord() {
        // Given
        Mockito.`when`(mockApiService.deleteRecord(ALIAS, RECORD_ID, USER_ID)).thenReturn(Completable.complete())

        // When
        val subscriber = recordService.deleteRecord(RECORD_ID, USER_ID).test().await()

        // Then
        subscriber.assertNoErrors().assertComplete()
        inOrder.verify(recordService).deleteRecord(RECORD_ID, USER_ID)
        inOrder.verify(mockApiService).deleteRecord(ALIAS, RECORD_ID, USER_ID)
        inOrder.verifyNoMoreInteractions()
    }

    @Test
    @Throws(InterruptedException::class)
    fun deleteRecords_shouldDeleteRecords() {
        // Given
        Mockito.doReturn(Completable.complete()).`when`(recordService).deleteRecord(RECORD_ID, USER_ID)
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
        inOrder.verify(recordService, Mockito.times(2)).deleteRecord(RECORD_ID, USER_ID)
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
        Mockito.doReturn(mockDecryptedRecord)
                .`when`(recordService)
                .decryptRecord<DomainResource>(mockEncryptedRecord, USER_ID)
        Mockito.doReturn(mockDecryptedRecord)
                .`when`(recordService)
                .downloadData(mockDecryptedRecord, USER_ID)
        Mockito.doReturn(mockMeta).`when`(recordService).buildMeta(mockDecryptedRecord)

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
        inOrder.verify(recordService).decryptRecord<DomainResource>(mockEncryptedRecord, USER_ID)
        inOrder.verify(recordService).buildMeta(mockDecryptedRecord)
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
        Mockito.doReturn(mockDecryptedRecord)
                .`when`(recordService)
                .decryptRecord<DomainResource>(mockEncryptedRecord, USER_ID)
        Mockito.doReturn(mockDecryptedRecord)
                .`when`(recordService).
                downloadData(mockDecryptedRecord, USER_ID)
        Mockito.`when`(mockDecryptedRecord.resource).thenReturn(doc)

        // When
        val observer = recordService.downloadRecord<DomainResource>(RECORD_ID, USER_ID).test().await()

        // Then
        val errors = observer.errors()
        Truth.assertThat(errors).hasSize(1)
        Truth.assertThat(errors[0]).isInstanceOf(DataRestrictionException.UnsupportedFileType::class.java)
        inOrder.verify(mockApiService).fetchRecord(ALIAS, USER_ID, RECORD_ID)
        inOrder.verify(recordService).decryptRecord<DomainResource>(mockEncryptedRecord, USER_ID)
        inOrder.verify(recordService).downloadData(mockDecryptedRecord, USER_ID)
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
        Mockito.doReturn(mockDecryptedRecord)
                .`when`(recordService)
                .decryptRecord<DomainResource>(mockEncryptedRecord, USER_ID)
        Mockito.doReturn(mockDecryptedRecord)
                .`when`(recordService)
                .downloadData(mockDecryptedRecord, USER_ID)
        Mockito.`when`(mockDecryptedRecord.resource).thenReturn(doc)

        // When
        val observer = recordService.downloadRecord<DomainResource>(RECORD_ID, USER_ID).test().await()

        // Then
        val errors = observer.errors()
        Truth.assertThat(errors).hasSize(1)
        Truth.assertThat(errors[0]).isInstanceOf(DataRestrictionException.MaxDataSizeViolation::class.java)
        inOrder.verify(mockApiService).fetchRecord(ALIAS, USER_ID, RECORD_ID)
        inOrder.verify(recordService).decryptRecord<DomainResource>(mockEncryptedRecord, USER_ID)
        inOrder.verify(recordService).downloadData(mockDecryptedRecord, USER_ID)
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
                .downloadRecord<DomainResource>(RECORD_ID, USER_ID)

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
        inOrder.verify(recordService).downloadRecords<DomainResource>(recordIds, USER_ID)
        inOrder.verify(recordService, Mockito.times(2))
                .downloadRecord<DomainResource>(RECORD_ID, USER_ID)
        inOrder.verifyNoMoreInteractions()
    }

    @Test
    @Throws(InterruptedException::class,
            IOException::class,
            DataValidationException.ModelVersionNotSupported::class,
            DataValidationException.InvalidAttachmentPayloadHash::class)
    fun downloadAttachment_shouldDownloadAttachment() {
        // Given
        Mockito.`when`(mockApiService.fetchRecord(ALIAS, USER_ID, RECORD_ID))
                .thenReturn(Single.just(mockEncryptedRecord))
        val document = buildDocumentReference()
        val attachment = AttachmentBuilder.buildAttachment(ATTACHMENT_ID)
        document.content[0].attachment = attachment
        val decryptedRecord = DecryptedRecord(
                RECORD_ID,
                document,
                null,
                ArrayList(),
                null,
                null,
                null,
                mockAttachmentKey,
                -1
        )
        Mockito.doReturn(decryptedRecord)
                .`when`(recordService)
                .decryptRecord<DomainResource>(mockEncryptedRecord, USER_ID)
        val attachments = ArrayList<Attachment>()
        attachments.add(attachment)
        Mockito.`when`(
                mockAttachmentService.downloadAttachments(
                        ArgumentMatchers.argThat { arg -> arg.contains(attachment) },
                        ArgumentMatchers.eq(mockAttachmentKey),
                        ArgumentMatchers.eq(USER_ID)
                )
        ).thenReturn(Single.just(attachments))

        // when
        val test = recordService.downloadAttachment(RECORD_ID, ATTACHMENT_ID, USER_ID, DownloadType.Full).test().await()

        // then
        val result = test
                .assertNoErrors()
                .assertComplete()
                .assertValue(attachment)
                .values()[0]
        Truth.assertThat(result.id).isEqualTo(ATTACHMENT_ID)
    }

    @Test
    @Throws(IOException::class,
            InterruptedException::class,
            DataValidationException.ModelVersionNotSupported::class,
            DataValidationException.InvalidAttachmentPayloadHash::class)
    fun downloadAttachments_shouldDownloadAttachments() {
        // Given
        Mockito.`when`(mockApiService.fetchRecord(ALIAS, USER_ID, RECORD_ID)).thenReturn(Single.just(mockEncryptedRecord))
        val document = buildDocumentReference()
        val attachment = AttachmentBuilder.buildAttachment(ATTACHMENT_ID)
        val secondAttachmentId = "secondId"
        val secondAttachment = AttachmentBuilder.buildAttachment(secondAttachmentId)
        document.content[0].attachment = attachment
        document.content = listOf(document.content[0], DocumentReferenceContent(secondAttachment))
        val decryptedRecord = DecryptedRecord(
                RECORD_ID,
                document,
                null,
                ArrayList(),
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
        Mockito.`when`(
                mockAttachmentService.downloadAttachments(
                        ArgumentMatchers.argThat { arg -> arg.containsAll(listOf(attachment, secondAttachment)) },
                        ArgumentMatchers.eq(mockAttachmentKey),
                        ArgumentMatchers.eq(USER_ID)
                )
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
                ArrayList(),
                null,
                null,
                null,
                mockAttachmentKey,
                -1
        )
        Mockito.doReturn(decryptedRecord).`when`(recordService)
                .decryptRecord<DomainResource>(mockEncryptedRecord, USER_ID)
        val attachmentIds = listOf(ATTACHMENT_ID, "invalidAttachmentId")

        //when
        val test = recordService.downloadAttachments(RECORD_ID, attachmentIds, USER_ID, DownloadType.Full).test().await()

        //then
        val errors = test.errors()
        Truth.assertThat(errors).hasSize(1)
        Truth.assertThat(errors[0]).isInstanceOf(DataValidationException.IdUsageViolation::class.java)
        Truth.assertThat(errors[0]!!.message).isEqualTo("Please provide correct attachment ids!")
    }
}
