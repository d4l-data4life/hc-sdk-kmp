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

import care.data4life.sdk.lang.CoreRuntimeException
import care.data4life.sdk.lang.D4LException
import care.data4life.sdk.lang.DataValidationException
import care.data4life.sdk.network.model.DecryptedRecord
import care.data4life.sdk.network.model.definitions.DecryptedBaseRecord
import care.data4life.sdk.test.util.AttachmentBuilder
import care.data4life.sdk.test.util.MedicationBuilder
import care.data4life.sdk.test.util.ObservationBuilder
import care.data4life.sdk.test.util.PatientBuilder
import care.data4life.sdk.test.util.QuestionnaireResponseBuilder
import care.data4life.sdk.util.Base64
import care.data4life.sdk.util.HashUtil
import care.data4life.sdk.wrapper.SdkAttachmentFactory
import care.data4life.sdk.wrapper.WrapperContract
import com.google.common.truth.Truth
import io.mockk.every
import io.mockk.spyk
import io.mockk.verify
import io.reactivex.Single
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito

class RecordServiceUploadsUpdatesDownloadsTest : RecordServiceTestBase() {
    @Before
    fun setup() {
        init()
    }

    @After
    fun tearDown() {
        stop()
    }

    @Test
    @Throws(DataValidationException.ExpectedFieldViolation::class,
            DataValidationException.IdUsageViolation::class,
            DataValidationException.InvalidAttachmentPayloadHash::class)
    fun uploadOrDownloadData_calls__uploadData() {
        // Given
        Mockito.doReturn(mockDecryptedFhir3Record).`when`(recordService)._uploadData(
                mockDecryptedFhir3Record,
                USER_ID
        )

        // When
        val record = recordService.uploadOrDownloadData(RecordService.UploadDownloadOperation.UPLOAD,
                mockDecryptedFhir3Record,
                null,
                USER_ID
        )

        // Then
        Truth.assertThat(record).isEqualTo(mockDecryptedFhir3Record)
        inOrder.verify(recordService)._uploadData(mockDecryptedFhir3Record, USER_ID)
        inOrder.verifyNoMoreInteractions()
    }

    @Test
    @Throws(DataValidationException.ExpectedFieldViolation::class,
            DataValidationException.IdUsageViolation::class,
            DataValidationException.InvalidAttachmentPayloadHash::class)
    fun uploadData_calls__uploadData_when_no_new_resource_was_given() {
        // Given
        Mockito.doReturn(mockDecryptedFhir3Record).`when`(recordService)._uploadData(
                mockDecryptedFhir3Record,
                USER_ID
        )

        // When
        val record = recordService.uploadData(mockDecryptedFhir3Record, null, USER_ID)

        // Then
        Truth.assertThat(record).isEqualTo(mockDecryptedFhir3Record)
        inOrder.verify(recordService).uploadData(mockDecryptedFhir3Record, null, USER_ID)
        inOrder.verify(recordService)._uploadData(mockDecryptedFhir3Record, USER_ID)
        inOrder.verifyNoMoreInteractions()
    }

    @Test
    fun `Given, _uploadData is called with a non DecryptedFhirRecord and UserId, it reflects it`() {
        // Given
        val document = buildDocumentReference()

        @Suppress("UNCHECKED_CAST")
        val decryptedRecord = Mockito.mock(DecryptedBaseRecord::class.java) as DecryptedBaseRecord<Any>

        val downscaledIds = listOf("downscaledId_1", "downscaledId_2")
        val uploadResult = listOf(Pair(SdkAttachmentFactory.wrap(document.content[0].attachment), downscaledIds))

        every { cryptoService.generateGCKey() } returns Single.just(mockAttachmentKey)
        every {
            recordServiceK.getValidHash(
                    eq(SdkAttachmentFactory.wrap(document.content[0].attachment))
            )
        } returns DATA_HASH

        every {
            attachmentService.upload(
                    listOf(SdkAttachmentFactory.wrap(document.content[0].attachment)),
                    mockAttachmentKey,
                    USER_ID
            )
        } returns Single.just(uploadResult)
        // When
        val record = recordServiceK._uploadData(decryptedRecord, USER_ID)

        // Then
        Truth.assertThat(record).isSameInstanceAs(decryptedRecord)
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
                arrayListOf(),
                null,
                null,
                null,
                null,
                -1
        )
        val downscaledIds = listOf("downscaledId_1", "downscaledId_2")
        val uploadResult = listOf(Pair(SdkAttachmentFactory.wrap(document.content[0].attachment), downscaledIds))

        every { cryptoService.generateGCKey() } returns Single.just(mockAttachmentKey)
        every {
            recordServiceK.getValidHash(
                    eq(SdkAttachmentFactory.wrap(document.content[0].attachment))
            )
        } returns DATA_HASH

        every {
            attachmentService.upload(
                    listOf(SdkAttachmentFactory.wrap(document.content[0].attachment)),
                    mockAttachmentKey,
                    USER_ID
            )
        } returns Single.just(uploadResult)

        every {
            recordServiceK.getValidHash(
                    SdkAttachmentFactory.wrap(document.content[0].attachment)
            )
        } returns DATA_HASH

        every {
            attachmentService.upload(
                    listOf(SdkAttachmentFactory.wrap(document.content[0].attachment)),
                    mockAttachmentKey,
                    USER_ID
            )
        } returns Single.just(uploadResult)

        // When
        val record = recordServiceK._uploadData(decryptedRecord, USER_ID)

        // Then
        Truth.assertThat(record).isEqualTo(decryptedRecord)
        Truth.assertThat(record.attachmentsKey).isEqualTo(mockAttachmentKey)

        /*inOrder.verify(recordService)._uploadData(decryptedRecord, USER_ID)
        inOrder.verify(mockCryptoService).generateGCKey()
        inOrder.verify(recordService).getValidHash(eq(SdkAttachmentFactory.wrap(document.content[0].attachment)))
        inOrder.verify(mockAttachmentService).upload(
                ArgumentMatchers.any(),
                ArgumentMatchers.eq(mockAttachmentKey),
                ArgumentMatchers.eq(USER_ID)
        )
        inOrder.verifyNoMoreInteractions()*/
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
                arrayListOf(),
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
                arrayListOf(),
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
        /*inOrder.verify(recordService)._uploadData(decryptedRecord, USER_ID)
        inOrder.verify(mockCryptoService).generateGCKey()
        inOrder.verify(recordService).getValidHash(eq(SdkAttachmentFactory.wrap(document.content[0].attachment)))
        inOrder.verifyNoMoreInteractions()*/
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
        val uploadResult: MutableList<Pair<WrapperContract.Attachment, List<String>?>> = arrayListOf()
        uploadResult.add(Pair(SdkAttachmentFactory.wrap(firstAttachment), listOf(PREVIEW_ID, THUMBNAIL_ID)))
        uploadResult.add(Pair(SdkAttachmentFactory.wrap(secondAttachment), null))
        val dummyDecryptedRecord = DecryptedRecord(
                RECORD_ID,
                docRef,
                null,
                arrayListOf(),
                null,
                null,
                null,
                null,
                -1
        )

        every { cryptoService.generateGCKey() } returns Single.just(mockAttachmentKey)

        every {
            recordServiceK.getValidHash(
                    SdkAttachmentFactory.wrap(firstAttachment)
            )
        } returns DATA_HASH

        every {
            recordServiceK.getValidHash(
                    SdkAttachmentFactory.wrap(secondAttachment)
            )
        } returns DATA_HASH

        every {
            attachmentService.upload(
                    any(),
                    mockAttachmentKey,
                    USER_ID
            )
        } returns Single.fromCallable {
            firstAttachment.id = ATTACHMENT_ID
            secondAttachment.id = ATTACHMENT_ID
            uploadResult as List<Pair<WrapperContract.Attachment, List<String>>>
        }

        //when
        val doc = recordServiceK._uploadData(
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
        Mockito.doReturn(mockDecryptedFhir3Record).`when`(recordService).updateData(
                mockDecryptedFhir3Record,
                mockDocumentReference,
                USER_ID
        )

        // When
        val record = recordService.uploadOrDownloadData(RecordService.UploadDownloadOperation.UPDATE,
                mockDecryptedFhir3Record,
                mockDocumentReference,
                USER_ID
        )

        // Then
        Truth.assertThat(record).isEqualTo(mockDecryptedFhir3Record)

        inOrder.verify(recordService).updateData(
                mockDecryptedFhir3Record,
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
        Mockito.doReturn(mockDecryptedFhir3Record).`when`(recordService).updateData(
                mockDecryptedFhir3Record,
                mockDocumentReference,
                USER_ID
        )

        // When
        val record = recordService.uploadData(
                mockDecryptedFhir3Record,
                mockDocumentReference,
                USER_ID
        )

        // Then
        Truth.assertThat(record).isEqualTo(mockDecryptedFhir3Record)
        inOrder.verify(recordService).uploadData(
                mockDecryptedFhir3Record,
                mockDocumentReference,
                USER_ID
        )
        inOrder.verify(recordService).updateData(
                mockDecryptedFhir3Record,
                mockDocumentReference,
                USER_ID
        )
        inOrder.verifyNoMoreInteractions()
    }


    @Test
    fun `Given, updateData is called with a non DecryptedFhirRecord, DomainResource and UserId, it reflects it`() {
        // Given
        val recordService = spyk(recordService)
        val attachmentService = spyk(mockAttachmentService)

        @Suppress("UNCHECKED_CAST")
        val decryptedRecord = Mockito.mock(DecryptedBaseRecord::class.java) as DecryptedBaseRecord<Any>

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

        val downscaledIds = listOf("downscaledId_1", "downscaledId_2")
        val uploadResult = listOf(
                Pair(SdkAttachmentFactory.wrap(updatedDocument.content[0].attachment), downscaledIds))

        every { cryptoService.generateGCKey() } returns Single.just(mockAttachmentKey)

        every {
            recordServiceK.getValidHash(
                    SdkAttachmentFactory.wrap(updatedDocument.content[0].attachment)
            )
        } returns DATA_HASH

        every {
            attachmentService.upload(
                    listOf(SdkAttachmentFactory.wrap(updatedAttachment)),
                    mockAttachmentKey,
                    USER_ID
            )
        } returns Single.just(uploadResult)


        // When
        val record = recordService.updateData(decryptedRecord, updatedDocument, USER_ID)

        // Then
        Truth.assertThat(record).isSameInstanceAs(decryptedRecord)

        verify(exactly = 0) { cryptoService.generateGCKey() }
        verify(exactly = 0) {
            attachmentService.upload(
                    listOf(SdkAttachmentFactory.wrap(updatedAttachment)),
                    mockAttachmentKey,
                    USER_ID
            )
        }
        /*
        inOrder.verify(recordService).updateData(decryptedRecord, updatedDocument, USER_ID)
        inOrder.verifyNoMoreInteractions()
         */
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
                arrayListOf(),
                null,
                null,
                null,
                mockAttachmentKey,
                -1
        )
        val downscaledIds = listOf("downscaledId_1", "downscaledId_2")
        val uploadResult = listOf(
                Pair(SdkAttachmentFactory.wrap(updatedDocument.content[0].attachment), downscaledIds)
        )

        every {
            recordServiceK.getValidHash(
                    SdkAttachmentFactory.wrap(updatedDocument.content[0].attachment)
            )
        } returns updatedHash

        every {
            attachmentService.upload(
                    listOf(SdkAttachmentFactory.wrap(updatedAttachment)),
                    mockAttachmentKey,
                    USER_ID
            )
        } returns Single.just(uploadResult)

        // When
        recordServiceK.updateData(decryptedRecord, updatedDocument, USER_ID)

        // Then

        verify(exactly = 1) {
            recordServiceK.getValidHash(
                    SdkAttachmentFactory.wrap(updatedAttachment)
            )
        }
        verify(exactly = 1) {
            attachmentService.upload(
                    listOf(SdkAttachmentFactory.wrap(updatedAttachment)),
                    mockAttachmentKey,
                    USER_ID
            )
        }

        /*inOrder.verify(recordService).updateData(decryptedRecord, updatedDocument, USER_ID)
        inOrder.verify(recordService).getValidHash(eq(SdkAttachmentFactory.wrap(updatedDocument.content[0].attachment)))
        inOrder.verify(mockAttachmentService).upload(
                ArgumentMatchers.eq(listOf(SdkAttachmentFactory.wrap(updatedAttachment))),
                ArgumentMatchers.eq(mockAttachmentKey),
                ArgumentMatchers.eq(USER_ID)
        )
        inOrder.verifyNoMoreInteractions()*/
    }

    @Test
    @Throws(DataValidationException.ExpectedFieldViolation::class,
            DataValidationException.IdUsageViolation::class,
            DataValidationException.InvalidAttachmentPayloadHash::class)
    fun `Given, updateData is called with a DecryptedFhirRecord, a non FhirResource and a UserId, it fails with a CoreRuntimeExceptionUnsupportedOperation`() {
        // Given
        val document = buildDocumentReference()

        @Suppress("UNCHECKED_CAST")
        val decryptedRecord = DecryptedRecord(
                RECORD_ID,
                document,
                null,
                arrayListOf(),
                null,
                null,
                null,
                null,
                -1
        ) as DecryptedBaseRecord<Any>

        try {
            // When
            recordService.updateData(decryptedRecord, "something", USER_ID)
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
                arrayListOf(),
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
                arrayListOf(),
                null,
                null,
                null,
                null,
                -1
        )

        every {
            recordServiceK.getValidHash(
                    eq(SdkAttachmentFactory.wrap(document.content[0].attachment))
            )
        } returns "i cannot never ever be valid"

        // When
        try {
            document.content[0].attachment.hash = "hash"
            recordServiceK.updateData(decryptedRecord, document, USER_ID)
            Assert.fail("Exception expected!")
        } catch (e: D4LException) {

            // Then
            Truth.assertThat(e.javaClass).isEqualTo(DataValidationException.InvalidAttachmentPayloadHash::class.java)
            Truth.assertThat(e.message).isEqualTo("Attachment.hash is not valid")
        }

        verify(exactly = 1) {
            recordServiceK.getValidHash(
                    eq(SdkAttachmentFactory.wrap(document.content[0].attachment))
            )
        }

        /*inOrder.verify(recordService).updateData(decryptedRecord, document, USER_ID)
        inOrder.verify(recordService).getValidHash(eq(SdkAttachmentFactory.wrap(document.content[0].attachment)))
        inOrder.verifyNoMoreInteractions()*/
    }

    @Test
    @Throws(DataValidationException.IdUsageViolation::class,
            DataValidationException.ExpectedFieldViolation::class,
            DataValidationException.InvalidAttachmentPayloadHash::class)
    fun updateData_throws_whenOldAttachmentHashNotPresent() {
        // Given
        val oldDocument = buildDocumentReference()
        val oldAttachment = oldDocument.content[0].attachment
        oldAttachment.id = "no id"
        oldAttachment.size = null
        oldAttachment.hash = null
        val updatedDocument = buildDocumentReference()
        val updatedAttachment = updatedDocument.content[0].attachment
        updatedAttachment.id = "id"
        updatedAttachment.size = 0
        updatedAttachment.hash = DATA_HASH
        val decryptedRecord = DecryptedRecord(
                RECORD_ID,
                oldDocument,
                null,
                arrayListOf(),
                null,
                null,
                null,
                mockAttachmentKey,
                -1
        )
        val uploadResult = arrayListOf<Pair<WrapperContract.Attachment, List<String>>>()
        val downscaledIds = listOf("downscaledId_1", "downscaledId_2")
        uploadResult.add(Pair(SdkAttachmentFactory.wrap(updatedAttachment), downscaledIds))

        every { recordServiceK.getValidHash(SdkAttachmentFactory.wrap(updatedAttachment)) } returns DATA_HASH
        every {
            attachmentService.upload(
                    listOf(SdkAttachmentFactory.wrap(updatedAttachment)),
                    mockAttachmentKey,
                    USER_ID
            )
        } returns Single.just(uploadResult)

        // When
        try {
            recordServiceK.updateData(decryptedRecord, updatedDocument, USER_ID)
            Assert.fail("Exception expected!")
        } catch (e: D4LException) {

            // Then
            Truth.assertThat(e.javaClass).isEqualTo(DataValidationException.IdUsageViolation::class.java)
            Truth.assertThat(e.message).isEqualTo("Valid Attachment.id expected")
        }

        // Then
        verify(exactly = 0) {
            attachmentService.upload(
                    listOf(SdkAttachmentFactory.wrap(updatedAttachment)),
                    mockAttachmentKey,
                    USER_ID
            )
        }
        verify(exactly = 1) { recordServiceK.getValidHash(SdkAttachmentFactory.wrap(updatedAttachment)) }
        /*inOrder.verify(recordService).updateData(decryptedRecord, updatedDocument, USER_ID)
        inOrder.verify(recordService).getValidHash(eq(SdkAttachmentFactory.wrap(updatedDocument.content[0].attachment)))
        inOrder.verifyNoMoreInteractions()*/
    }

    @Test
    @Throws(DataValidationException.ExpectedFieldViolation::class,
            DataValidationException.IdUsageViolation::class,
            DataValidationException.InvalidAttachmentPayloadHash::class)
    fun updateData_throws_whenValidAttachmentIdNotPresent() {
        // Given
        val recordService = spyk(recordService)

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
                arrayListOf(),
                null,
                null,
                null,
                null,
                -1
        )

        every {
            recordService.getValidHash(
                    eq(SdkAttachmentFactory.wrap(updatedDocument.content[0].attachment))
            )
        } returns "hash"
        /*
        Mockito.`when`(recordService.getValidHash(
                eq(SdkAttachmentFactory.wrap(updatedDocument.content[0].attachment))
        )).thenReturn("hash")*/
        // When
        try {
            recordService.updateData(decryptedRecord, updatedDocument, USER_ID)
            Assert.fail("Exception expected!")
        } catch (e: D4LException) {

            // Then
            Truth.assertThat(e.javaClass).isEqualTo(DataValidationException.IdUsageViolation::class.java)
            Truth.assertThat(e.message).isEqualTo("Valid Attachment.id expected")
        }

        verify(exactly = 1) {
            recordService.getValidHash(
                    eq(SdkAttachmentFactory.wrap(updatedDocument.content[0].attachment))
            )
        }
        /*inOrder.verify(recordService).updateData(decryptedRecord, updatedDocument, USER_ID)
        inOrder.verify(recordService).getValidHash(eq(SdkAttachmentFactory.wrap(updatedDocument.content[0].attachment)))
        inOrder.verifyNoMoreInteractions()*/
    }

    @Test
    @Throws(DataValidationException.InvalidAttachmentPayloadHash::class,
            DataValidationException.ExpectedFieldViolation::class,
            DataValidationException.IdUsageViolation::class)
    fun updateData_notUploadsAttachment_whenHashesMatch() {
        // Given
        val recordService = spyk(recordService)

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
                arrayListOf(),
                null,
                null,
                null,
                mockAttachmentKey,
                -1
        )

        every {
            recordService.getValidHash(
                    eq(SdkAttachmentFactory.wrap(updatedDocument.content[0].attachment))
            )
        } returns "hash"

        // When
        recordService.updateData(decryptedRecord, updatedDocument, USER_ID)

        verify(exactly = 1) {
            recordService.getValidHash(
                    eq(SdkAttachmentFactory.wrap(updatedDocument.content[0].attachment))
            )
        }
        // Then
        /*inOrder.verify(recordService).updateData(decryptedRecord, updatedDocument, USER_ID)
        inOrder.verify(recordService).getValidHash(eq(SdkAttachmentFactory.wrap(updatedDocument.content[0].attachment)))
        inOrder.verifyNoMoreInteractions()*/
    }

    @Test
    @Throws(DataValidationException.IdUsageViolation::class,
            DataValidationException.ExpectedFieldViolation::class,
            DataValidationException.InvalidAttachmentPayloadHash::class)
    fun updateData_doesAppendWhenOldHashIsNUll() {
        // Given
        val oldDocument = buildDocumentReference()
        val oldAttachment = oldDocument.content[0].attachment
        oldAttachment.id = "id"
        oldAttachment.size = 0
        oldAttachment.hash = null
        val updatedDocument = buildDocumentReference()
        val updatedAttachment = updatedDocument.content[0].attachment
        updatedAttachment.id = "id"
        updatedAttachment.size = 0
        updatedAttachment.hash = DATA_HASH
        val decryptedRecord = DecryptedRecord(
                RECORD_ID,
                oldDocument,
                null,
                arrayListOf(),
                null,
                null,
                null,
                mockAttachmentKey,
                -1
        )
        val uploadResult = arrayListOf<Pair<WrapperContract.Attachment, List<String>>>()
        val downscaledIds = listOf("downscaledId_1", "downscaledId_2")
        uploadResult.add(Pair(SdkAttachmentFactory.wrap(updatedAttachment), downscaledIds))

        every {
            attachmentService.upload(
                    listOf(SdkAttachmentFactory.wrap(updatedAttachment)),
                    mockAttachmentKey,
                    USER_ID
            )
        } returns Single.just(uploadResult)
        every { recordServiceK.getValidHash(SdkAttachmentFactory.wrap(updatedAttachment)) } returns DATA_HASH

        // When
        recordServiceK.updateData(decryptedRecord, updatedDocument, USER_ID)

        // Then
        verify(exactly = 1) {
            attachmentService.upload(
                    listOf(SdkAttachmentFactory.wrap(updatedAttachment)),
                    mockAttachmentKey,
                    USER_ID
            )
        }
        verify(exactly = 1) { recordServiceK.getValidHash(SdkAttachmentFactory.wrap(updatedAttachment)) }
        // Then
        /*inOrder.verify(recordService).updateData(decryptedRecord, updatedDocument, USER_ID)
        inOrder.verify(recordService).getValidHash(eq(SdkAttachmentFactory.wrap(updatedDocument.content[0].attachment)))
        inOrder.verifyNoMoreInteractions()*/
    }

    @Test
    @Throws(DataValidationException.ExpectedFieldViolation::class,
            DataValidationException.IdUsageViolation::class,
            DataValidationException.InvalidAttachmentPayloadHash::class)
    fun uploadOrDownloadData_calls_downloadData() {
        // Given
        Mockito.doReturn(mockDecryptedFhir3Record).`when`(recordService).downloadData(mockDecryptedFhir3Record, USER_ID)

        // When
        val record = recordService.uploadOrDownloadData(RecordService.UploadDownloadOperation.DOWNLOAD,
                mockDecryptedFhir3Record,
                null,
                USER_ID
        )

        // Then
        Truth.assertThat(record).isEqualTo(mockDecryptedFhir3Record)
        inOrder.verify(recordService).uploadOrDownloadData(RecordService.UploadDownloadOperation.DOWNLOAD,
                mockDecryptedFhir3Record,
                null,
                USER_ID
        )
        inOrder.verify(recordService).downloadData(mockDecryptedFhir3Record, USER_ID)
        inOrder.verifyNoMoreInteractions()
    }

    @Test
    @Throws(DataValidationException.IdUsageViolation::class, DataValidationException.InvalidAttachmentPayloadHash::class)
    fun `Given, downloadData is called with a non DecryptedFhirRecord and a UserId, it reflects it`() {
        // Given
        val document = buildDocumentReference()
        document.content[0].attachment.id = "id"
        @Suppress("UNCHECKED_CAST")
        val decryptedRecord = Mockito.mock(DecryptedBaseRecord::class.java) as DecryptedBaseRecord<Any>

        every {
            attachmentService.download(
                    listOf(SdkAttachmentFactory.wrap(document.content[0].attachment)),
                    mockAttachmentKey,
                    USER_ID
            )
        } returns Single.just(arrayListOf())

        // When
        val record = recordServiceK.downloadData(decryptedRecord, USER_ID)

        // Then
        Truth.assertThat(record).isSameInstanceAs(decryptedRecord)

        verify(exactly = 0) {
            attachmentService.download(
                    listOf(SdkAttachmentFactory.wrap(document.content[0].attachment)),
                    mockAttachmentKey,
                    USER_ID
            )
        }
        /*inOrder.verify(recordService).downloadData(decryptedRecord, USER_ID)
        inOrder.verifyNoMoreInteractions()*/
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
                arrayListOf(),
                null,
                null,
                null,
                mockAttachmentKey,
                -1
        )

        every {
            attachmentService.download(
                    listOf(SdkAttachmentFactory.wrap(document.content[0].attachment)),
                    mockAttachmentKey,
                    USER_ID
            )
        } returns Single.just(arrayListOf())

        // When
        val record = recordServiceK.downloadData(decryptedRecord, USER_ID)

        // Then
        Truth.assertThat(record).isEqualTo(decryptedRecord)

        verify(exactly = 1) {
            attachmentService.download(
                    listOf(SdkAttachmentFactory.wrap(document.content[0].attachment)),
                    mockAttachmentKey,
                    USER_ID
            )
        }
        /*inOrder.verify(recordService).downloadData(decryptedRecord, USER_ID)
        inOrder.verify(mockAttachmentService).download(
                ArgumentMatchers.any(),
                ArgumentMatchers.eq(mockAttachmentKey),
                ArgumentMatchers.eq(USER_ID)
        )
        inOrder.verifyNoMoreInteractions()*/
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
                arrayListOf(),
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
    @Throws(DataValidationException.ExpectedFieldViolation::class,
            DataValidationException.IdUsageViolation::class,
            DataValidationException.InvalidAttachmentPayloadHash::class)
    fun uploadOrDownloadData_shouldUploadData_Patient() {
        // Given
        val recordService = Mockito.spy(
                RecordService(
                        PARTNER_ID,
                        ALIAS,
                        mockApiService,
                        mockTagEncryptionService,
                        mockTaggingService,
                        mockFhirService,
                        attachmentService,
                        mockCryptoService,
                        mockErrorHandler
                )
        )

        Mockito.`when`(mockCryptoService.generateGCKey()).thenReturn(Single.just(mockAttachmentKey))
        val patient = PatientBuilder.buildPatient()
        val decryptedRecord = DecryptedRecord(
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
        decryptedRecord.identifier = RECORD_ID
        Mockito.`when`(mockCryptoService.generateGCKey()).thenReturn(Single.just(mockAttachmentKey))
        val uploadResult = arrayListOf<Pair<WrapperContract.Attachment, List<String>>>()
        val downscaledIds: MutableList<String> = arrayListOf()
        downscaledIds.add("downscaledId_1")
        downscaledIds.add("downscaledId_2")
        uploadResult.add(Pair<WrapperContract.Attachment, List<String>>(SdkAttachmentFactory.wrap(patient.photo!![0]), downscaledIds))

        every {
            attachmentService.upload(
                    any(),
                    mockAttachmentKey,
                    USER_ID
            )
        } returns Single.just(uploadResult)

        val encrypted = ByteArray(1)
        every { HashUtil.sha1(any()) } returns encrypted
        every { Base64.encodeToString(encrypted) } returns DATA_HASH

        // When
        val record = recordService.uploadOrDownloadData(
                RecordService.UploadDownloadOperation.UPLOAD,
                decryptedRecord,
                null,
                USER_ID
        )

        // Then
        Truth.assertThat(record).isEqualTo(decryptedRecord)
        Truth.assertThat(record.attachmentsKey).isEqualTo(mockAttachmentKey)

        verify(exactly = 1) { Base64.decode(SdkAttachmentFactory.wrap(patient.photo!![0]).data!!) }
        verify(exactly = 1) {
            attachmentService.upload(
                    any(),
                    mockAttachmentKey,
                    USER_ID
            )
        }
        /*inOrder.verify(recordService).uploadOrDownloadData(
                RecordService.UploadDownloadOperation.UPLOAD,
                decryptedRecord,
                null,
                USER_ID
        )
        inOrder.verify(mockCryptoService).generateGCKey()
        /*inOrder.verify(mockAttachmentService).upload(
                ArgumentMatchers.any(),
                ArgumentMatchers.eq(mockAttachmentKey),
                ArgumentMatchers.eq(USER_ID)
        )*/
        inOrder.verifyNoMoreInteractions()*/
    }

    @Test
    @Throws(DataValidationException.ExpectedFieldViolation::class,
            DataValidationException.IdUsageViolation::class,
            DataValidationException.InvalidAttachmentPayloadHash::class)
    fun uploadOrDownloadData_shouldDownloadData_Patient() {
        // Given
        val recordService = Mockito.spy(
                RecordService(
                        PARTNER_ID,
                        ALIAS,
                        mockApiService,
                        mockTagEncryptionService,
                        mockTaggingService,
                        mockFhirService,
                        attachmentService,
                        mockCryptoService,
                        mockErrorHandler
                )
        )

        val patient = PatientBuilder.buildPatient()
        patient.photo!![0].id = "id"
        val decryptedRecord = DecryptedRecord(
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

        every {
            attachmentService.download(
                    any(),
                    mockAttachmentKey,
                    USER_ID
            )
        } returns Single.just(listOf())

        // When
        val record = recordService.uploadOrDownloadData(
                RecordService.UploadDownloadOperation.DOWNLOAD,
                decryptedRecord,
                null,
                USER_ID
        )

        // Then
        Truth.assertThat(record).isEqualTo(decryptedRecord)
        /*inOrder.verify(recordService).uploadOrDownloadData(
                RecordService.UploadDownloadOperation.DOWNLOAD,
                decryptedRecord,
                null,
                USER_ID
        )
        inOrder.verify(mockAttachmentService).download(
                ArgumentMatchers.any(),
                ArgumentMatchers.eq(mockAttachmentKey),
                ArgumentMatchers.eq(USER_ID)
        )
        inOrder.verifyNoMoreInteractions()*/
        verify(exactly = 1) {
            attachmentService.download(
                    any(),
                    mockAttachmentKey,
                    USER_ID
            )
        }
    }

    @Test
    @Throws(DataValidationException.ExpectedFieldViolation::class,
            DataValidationException.IdUsageViolation::class,
            DataValidationException.InvalidAttachmentPayloadHash::class)
    fun uploadOrDownloadData_shouldDownloadData_Medication() {
        // Given
        val recordService = Mockito.spy(
                RecordService(
                        PARTNER_ID,
                        ALIAS,
                        mockApiService,
                        mockTagEncryptionService,
                        mockTaggingService,
                        mockFhirService,
                        attachmentService,
                        mockCryptoService,
                        mockErrorHandler
                )
        )

        val medication = MedicationBuilder.buildMedication()
        medication.image!![0].id = "id"
        val decryptedRecord = DecryptedRecord(
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

        every {
            attachmentService.download(
                    any(),
                    mockAttachmentKey,
                    USER_ID
            )
        } returns Single.just(listOf())

        // When
        val record = recordService.uploadOrDownloadData(
                RecordService.UploadDownloadOperation.DOWNLOAD,
                decryptedRecord,
                null,
                USER_ID
        )

        // Then
        Truth.assertThat(record).isEqualTo(decryptedRecord)
        /*inOrder.verify(recordService).uploadOrDownloadData(
                RecordService.UploadDownloadOperation.DOWNLOAD,
                decryptedRecord,
                null,
                USER_ID
        )
        inOrder.verify(mockAttachmentService).download(
                ArgumentMatchers.any(),
                ArgumentMatchers.eq(mockAttachmentKey),
                ArgumentMatchers.eq(USER_ID)
        )
        inOrder.verifyNoMoreInteractions()*/
        verify(exactly = 1) {
            attachmentService.download(
                    any(),
                    mockAttachmentKey,
                    USER_ID
            )
        }
    }

    @Test
    @Throws(DataValidationException.ExpectedFieldViolation::class,
            DataValidationException.IdUsageViolation::class,
            DataValidationException.InvalidAttachmentPayloadHash::class)
    fun uploadOrDownloadData_shouldUploadData_Medication() {
        // Given
        val recordService = Mockito.spy(
                RecordService(
                        PARTNER_ID,
                        ALIAS,
                        mockApiService,
                        mockTagEncryptionService,
                        mockTaggingService,
                        mockFhirService,
                        attachmentService,
                        mockCryptoService,
                        mockErrorHandler
                )
        )

        Mockito.`when`(mockCryptoService.generateGCKey()).thenReturn(Single.just(mockAttachmentKey))
        val medication = MedicationBuilder.buildMedication()
        val decryptedRecord = DecryptedRecord(
                null,
                medication,
                null,
                arrayListOf(),
                null,
                null,
                null,
                null,
                -1
        )
        decryptedRecord.identifier = RECORD_ID
        Mockito.`when`(mockCryptoService.generateGCKey()).thenReturn(Single.just(mockAttachmentKey))

        val uploadResult: MutableList<Pair<WrapperContract.Attachment, List<String>>> = arrayListOf()
        uploadResult.add(Pair<WrapperContract.Attachment, List<String>>(SdkAttachmentFactory.wrap(medication.image!![0]), listOf()))

        every {
            attachmentService.upload(
                    any(),
                    mockAttachmentKey,
                    USER_ID
            )
        } returns Single.just(listOf())

        val encrypted = ByteArray(1)
        every { HashUtil.sha1(any()) } returns encrypted
        every { Base64.encodeToString(encrypted) } returns DATA_HASH

        // When
        val record = recordService.uploadOrDownloadData(
                RecordService.UploadDownloadOperation.UPLOAD,
                decryptedRecord,
                null,
                USER_ID
        )

        // Then
        Truth.assertThat(record).isEqualTo(decryptedRecord)
        Truth.assertThat(record.attachmentsKey).isEqualTo(mockAttachmentKey)
        verify(exactly = 1) {
            attachmentService.upload(
                    any(),
                    mockAttachmentKey,
                    USER_ID
            )
        }
        verify(exactly = 1) { Base64.decode(SdkAttachmentFactory.wrap(medication.image!![0]).data!!) }

        /*inOrder.verify(recordService).uploadOrDownloadData(
                RecordService.UploadDownloadOperation.UPLOAD,
                decryptedRecord,
                null,
                USER_ID
        )
        inOrder.verify(mockCryptoService).generateGCKey()
        inOrder.verify(mockAttachmentService).upload(
                ArgumentMatchers.any(),
                ArgumentMatchers.eq(mockAttachmentKey),
                ArgumentMatchers.eq(USER_ID)
        )
        inOrder.verifyNoMoreInteractions()*/
    }

    @Test
    @Throws(DataValidationException.ExpectedFieldViolation::class,
            DataValidationException.IdUsageViolation::class,
            DataValidationException.InvalidAttachmentPayloadHash::class)
    fun uploadOrDownloadData_shouldUploadData_Observation() {
        // Given
        val recordService = Mockito.spy(
                RecordService(
                        PARTNER_ID,
                        ALIAS,
                        mockApiService,
                        mockTagEncryptionService,
                        mockTaggingService,
                        mockFhirService,
                        attachmentService,
                        mockCryptoService,
                        mockErrorHandler
                )
        )

        Mockito.`when`(mockCryptoService.generateGCKey()).thenReturn(Single.just(mockAttachmentKey))
        val observation = ObservationBuilder.buildObservationWithComponent()
        observation.component!![0].valueAttachment!!.id = null
        val decryptedRecord = DecryptedRecord(
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
        decryptedRecord.identifier = RECORD_ID
        Mockito.`when`(mockCryptoService.generateGCKey()).thenReturn(Single.just(mockAttachmentKey))

        val uploadResult: MutableList<Pair<WrapperContract.Attachment, List<String>>> = arrayListOf()
        val downscaledIds: MutableList<String> = arrayListOf()
        downscaledIds.add("downscaledId_1")
        downscaledIds.add("downscaledId_2")
        uploadResult.add(Pair(SdkAttachmentFactory.wrap(observation.component!![0].valueAttachment!!), downscaledIds))
        uploadResult.add(Pair(SdkAttachmentFactory.wrap(observation.valueAttachment!!), downscaledIds))

        every {
            attachmentService.upload(
                    any(),
                    mockAttachmentKey,
                    USER_ID
            )
        } returns Single.just(listOf())

        val encrypted = ByteArray(1)
        every { HashUtil.sha1(any()) } returns encrypted
        every { Base64.encodeToString(encrypted) } returns DATA_HASH

        // When
        val record = recordService.uploadOrDownloadData(
                RecordService.UploadDownloadOperation.UPLOAD,
                decryptedRecord,
                null,
                USER_ID
        )

        // Then
        Truth.assertThat(record).isEqualTo(decryptedRecord)
        Truth.assertThat(record.attachmentsKey).isEqualTo(mockAttachmentKey)

        verify(exactly = 1) {
            attachmentService.upload(
                    any(),
                    mockAttachmentKey,
                    USER_ID
            )
        }
        verify(exactly = 2) { Base64.decode(SdkAttachmentFactory.wrap(observation.component!![0].valueAttachment!!).data!!) }
        /*inOrder.verify(recordService).uploadOrDownloadData(
                RecordService.UploadDownloadOperation.UPLOAD,
                decryptedRecord,
                null,
                USER_ID
        )
        inOrder.verify(mockCryptoService).generateGCKey()
        inOrder.verify(mockAttachmentService).upload(
                ArgumentMatchers.any(),
                ArgumentMatchers.eq(mockAttachmentKey),
                ArgumentMatchers.eq(USER_ID)
        )
        inOrder.verifyNoMoreInteractions()*/
    }

    @Test
    @Throws(DataValidationException.ExpectedFieldViolation::class,
            DataValidationException.IdUsageViolation::class,
            DataValidationException.InvalidAttachmentPayloadHash::class)
    fun uploadOrDownloadData_shouldDownloadData_Observation() {
        // Given
        val recordService = Mockito.spy(
                RecordService(
                        PARTNER_ID,
                        ALIAS,
                        mockApiService,
                        mockTagEncryptionService,
                        mockTaggingService,
                        mockFhirService,
                        attachmentService,
                        mockCryptoService,
                        mockErrorHandler
                )
        )

        val observation = ObservationBuilder.buildObservationWithComponent()
        observation.component!![0].valueAttachment!!.id = "id1"
        observation.valueAttachment!!.id = "id0"
        val decryptedRecord = DecryptedRecord(
                RECORD_ID,
                observation,
                null,
                arrayListOf(),
                null,
                null,
                null,
                mockAttachmentKey,
                -1
        )

        every {
            attachmentService.download(
                    any(),
                    mockAttachmentKey,
                    USER_ID
            )
        } returns Single.just(listOf())

        // When
        val record = recordService.uploadOrDownloadData(
                RecordService.UploadDownloadOperation.DOWNLOAD,
                decryptedRecord,
                null,
                USER_ID
        )

        // Then
        Truth.assertThat(record).isEqualTo(decryptedRecord)
        verify(exactly = 1) {
            attachmentService.download(
                    any(),
                    mockAttachmentKey,
                    USER_ID
            )
        }
        /*inOrder.verify(recordService).uploadOrDownloadData(
                RecordService.UploadDownloadOperation.DOWNLOAD,
                decryptedRecord,
                null,
                USER_ID
        )
        inOrder.verify(mockAttachmentService).download(
                ArgumentMatchers.any(),
                ArgumentMatchers.eq(mockAttachmentKey),
                ArgumentMatchers.eq(USER_ID)
        )
        inOrder.verifyNoMoreInteractions()*/
    }

    @Test
    @Throws(DataValidationException.ExpectedFieldViolation::class,
            DataValidationException.IdUsageViolation::class,
            DataValidationException.InvalidAttachmentPayloadHash::class)
    fun uploadOrDownloadData_shouldUploadData_QuestionnaireResponse() {
        // Given
        val recordService = Mockito.spy(
                RecordService(
                        PARTNER_ID,
                        ALIAS,
                        mockApiService,
                        mockTagEncryptionService,
                        mockTaggingService,
                        mockFhirService,
                        attachmentService,
                        mockCryptoService,
                        mockErrorHandler
                )
        )

        val questionnaireResponse = QuestionnaireResponseBuilder.buildQuestionnaireResponse()
        val decryptedRecord = DecryptedRecord(
                RECORD_ID,
                questionnaireResponse,
                null,
                arrayListOf(),
                null,
                null,
                null,
                null,
                -1
        )

        val uploadResult: MutableList<Pair<WrapperContract.Attachment, List<String>>> = arrayListOf()
        val downscaledIds: MutableList<String> = mutableListOf("downscaledId_1", "downscaledId_2")
        uploadResult.add(Pair<WrapperContract.Attachment, List<String>>(
                SdkAttachmentFactory.wrap(questionnaireResponse.item!![0].answer!![0].valueAttachment!!),
                downscaledIds)
        )
        Mockito.`when`(mockCryptoService.generateGCKey()).thenReturn(Single.just(mockAttachmentKey))
        Mockito.`when`(mockCryptoService.generateGCKey()).thenReturn(Single.just(mockAttachmentKey))

        every {
            attachmentService.upload(
                    any(),
                    mockAttachmentKey,
                    USER_ID
            )
        } returns Single.just(listOf())

        val encrypted = ByteArray(1)
        every { HashUtil.sha1(any()) } returns encrypted
        every { Base64.encodeToString(encrypted) } returns DATA_HASH

        // When
        val record = recordService.uploadOrDownloadData(
                RecordService.UploadDownloadOperation.UPLOAD,
                decryptedRecord,
                null,
                USER_ID
        )

        // Then
        Truth.assertThat(record).isEqualTo(decryptedRecord)
        Truth.assertThat(record.attachmentsKey).isEqualTo(mockAttachmentKey)
        verify(exactly = 1) {
            attachmentService.upload(
                    any(),
                    mockAttachmentKey,
                    USER_ID
            )
        }
        verify(exactly = 1) {
            Base64.decode(
                    SdkAttachmentFactory.wrap(questionnaireResponse.item!![0].answer!![0].valueAttachment!!).data!!)
        }
        /*inOrder.verify(recordService).uploadOrDownloadData(
                RecordService.UploadDownloadOperation.UPLOAD,
                decryptedRecord,
                null,
                USER_ID
        )
        inOrder.verify(mockCryptoService).generateGCKey()
        inOrder.verify(mockAttachmentService).upload(
                ArgumentMatchers.any(),
                ArgumentMatchers.eq(mockAttachmentKey),
                ArgumentMatchers.eq(USER_ID)
        )
        inOrder.verifyNoMoreInteractions()*/
    }

    @Test
    @Throws(DataValidationException.ExpectedFieldViolation::class,
            DataValidationException.IdUsageViolation::class,
            DataValidationException.InvalidAttachmentPayloadHash::class)
    fun uploadOrDownloadData_shouldDownloadData_QuestionnaireResponse() {
        // Given
        val recordService = Mockito.spy(
                RecordService(
                        PARTNER_ID,
                        ALIAS,
                        mockApiService,
                        mockTagEncryptionService,
                        mockTaggingService,
                        mockFhirService,
                        attachmentService,
                        mockCryptoService,
                        mockErrorHandler
                )
        )

        val questionnaireResponse = QuestionnaireResponseBuilder.buildQuestionnaireResponse()
        questionnaireResponse.item!![0].answer!![0].valueAttachment!!.id = "id"
        val decryptedRecord = DecryptedRecord(
                RECORD_ID,
                questionnaireResponse,
                null,
                arrayListOf(),
                null,
                null,
                null,
                mockAttachmentKey,
                -1
        )

        every {
            attachmentService.download(
                    any(),
                    mockAttachmentKey,
                    USER_ID
            )
        } returns Single.just(listOf())

        // When
        val record = recordService.uploadOrDownloadData(
                RecordService.UploadDownloadOperation.DOWNLOAD,
                decryptedRecord,
                null,
                USER_ID
        )

        // Then
        Truth.assertThat(record).isEqualTo(decryptedRecord)

        verify(exactly = 1) {
            attachmentService.download(
                    any(),
                    mockAttachmentKey,
                    USER_ID
            )
        }
        /*inOrder.verify(recordService).uploadOrDownloadData(
                RecordService.UploadDownloadOperation.DOWNLOAD,
                decryptedRecord,
                null,
                USER_ID
        )
        inOrder.verify(mockAttachmentService).download(
                ArgumentMatchers.any(),
                ArgumentMatchers.eq(mockAttachmentKey),
                ArgumentMatchers.eq(USER_ID)
        )
        inOrder.verifyNoMoreInteractions()*/
    }

    @Test
    @Throws(DataValidationException.IdUsageViolation::class,
            DataValidationException.ExpectedFieldViolation::class,
            DataValidationException.InvalidAttachmentPayloadHash::class)
    fun uploadOrDownloadData_shouldAppendAdditionalIdentifiers_Patient() {
        //given
        val recordService = Mockito.spy(
                RecordService(
                        PARTNER_ID,
                        ALIAS,
                        mockApiService,
                        mockTagEncryptionService,
                        mockTaggingService,
                        mockFhirService,
                        attachmentService,
                        mockCryptoService,
                        mockErrorHandler
                )
        )

        val patient = PatientBuilder.buildPatient()
        patient.photo!!.add(AttachmentBuilder.buildAttachment(null))
        val firstAttachment = patient.photo!![0]
        firstAttachment.title = "image"
        firstAttachment.hash = DATA_HASH
        val secondAttachment = patient.photo!![1]
        secondAttachment.title = "pdf"
        secondAttachment.hash = DATA_HASH
        val uploadResult: MutableList<Pair<WrapperContract.Attachment, List<String>?>> = arrayListOf()
        uploadResult.add(Pair(SdkAttachmentFactory.wrap(firstAttachment), listOf(PREVIEW_ID, THUMBNAIL_ID)))
        uploadResult.add(Pair(SdkAttachmentFactory.wrap(secondAttachment), null))
        val dummyDecryptedRecord = DecryptedRecord(
                RECORD_ID,
                patient,
                null,
                arrayListOf(),
                null,
                null,
                null,
                null,
                -1
        )
        Mockito.`when`(mockCryptoService.generateGCKey()).thenReturn(Single.just(mockAttachmentKey))

        val encrypted = ByteArray(1)
        every { HashUtil.sha1(any()) } returns encrypted
        every { Base64.encodeToString(encrypted) } returns DATA_HASH

        every {
            attachmentService.upload(
                    any(),
                    mockAttachmentKey,
                    USER_ID
            )
        } returns Single.fromCallable {
            uploadResult.also {
                firstAttachment.id = ATTACHMENT_ID
                secondAttachment.id = ATTACHMENT_ID
            } as List<Pair<WrapperContract.Attachment, List<String>>> //This cast is technically wrong, how ever this is the assumption of updateFhirResourceIdentifier
        }

        //when
        val pat = recordService.uploadOrDownloadData(
                RecordService.UploadDownloadOperation.UPLOAD,
                dummyDecryptedRecord,
                null,
                USER_ID
        ).resource

        //then
        Truth.assertThat(pat).isEqualTo(patient)
        Truth.assertThat(pat!!.identifier).hasSize(1)
        Truth.assertThat(pat.identifier!![0].value).isEqualTo(
                RecordService.DOWNSCALED_ATTACHMENT_IDS_FMT + "#" + ATTACHMENT_ID + "#"
                        + PREVIEW_ID + "#" + THUMBNAIL_ID
        )
        Truth.assertThat(pat.identifier!![0].assigner!!.reference).isEqualTo(PARTNER_ID)

        verify {
            Base64.decode(
                    SdkAttachmentFactory.wrap(patient.photo!![0]).data!!)
        }
        verify {
            Base64.decode(
                    SdkAttachmentFactory.wrap(secondAttachment).data!!)
        }
    }

    @Test
    @Throws(DataValidationException.IdUsageViolation::class,
            DataValidationException.ExpectedFieldViolation::class,
            DataValidationException.InvalidAttachmentPayloadHash::class)
    fun uploadOrDownloadData_shouldNotAppendAdditionalIdentifiers_Medication() {
        //given
        val recordService = Mockito.spy(
                RecordService(
                        PARTNER_ID,
                        ALIAS,
                        mockApiService,
                        mockTagEncryptionService,
                        mockTaggingService,
                        mockFhirService,
                        attachmentService,
                        mockCryptoService,
                        mockErrorHandler
                )
        )

        val medication = MedicationBuilder.buildMedication()
        medication.image!!.add(AttachmentBuilder.buildAttachment(null))
        val firstAttachment = medication.image!![0]
        firstAttachment.title = "image"
        val secondAttachment = medication.image!![1]
        secondAttachment.title = "pdf"
        val uploadResult: MutableList<Pair<WrapperContract.Attachment, List<String>>> = arrayListOf()
        uploadResult.add(Pair(SdkAttachmentFactory.wrap(firstAttachment), listOf(PREVIEW_ID, THUMBNAIL_ID)))
        uploadResult.add(Pair(SdkAttachmentFactory.wrap(secondAttachment), listOf()))
        val dummyDecryptedRecord = DecryptedRecord(
                RECORD_ID,
                medication,
                null,
                arrayListOf(),
                null,
                null,
                null,
                null,
                -1
        )
        Mockito.`when`(mockCryptoService.generateGCKey()).thenReturn(Single.just(mockAttachmentKey))

        val encrypted = ByteArray(1)
        every { HashUtil.sha1(any()) } returns encrypted
        every { Base64.encodeToString(encrypted) } returns DATA_HASH

        every {
            attachmentService.upload(
                    any(),
                    mockAttachmentKey,
                    USER_ID
            )
        } returns Single.fromCallable {
            uploadResult.also {
                firstAttachment.id = ATTACHMENT_ID
                secondAttachment.id = ATTACHMENT_ID
            } as List<Pair<WrapperContract.Attachment, List<String>>> //This cast is technically wrong, how ever this is the assumption of updateFhirResourceIdentifier
        }

        //when
        val med = recordService.uploadOrDownloadData(
                RecordService.UploadDownloadOperation.UPLOAD,
                dummyDecryptedRecord,
                null,
                USER_ID
        ).resource

        //then
        Truth.assertThat(med).isEqualTo(medication)

        verify {
            Base64.decode(
                    SdkAttachmentFactory.wrap(firstAttachment).data!!)
        }
        verify {
            Base64.decode(
                    SdkAttachmentFactory.wrap(secondAttachment).data!!)
        }
    }

    @Test
    @Throws(DataValidationException.IdUsageViolation::class,
            DataValidationException.ExpectedFieldViolation::class,
            DataValidationException.InvalidAttachmentPayloadHash::class)
    fun uploadOrDownloadData_shouldAppendAdditionalIdentifiers_Observation() {
        //given
        val recordService = Mockito.spy(
                RecordService(
                        PARTNER_ID,
                        ALIAS,
                        mockApiService,
                        mockTagEncryptionService,
                        mockTaggingService,
                        mockFhirService,
                        attachmentService,
                        mockCryptoService,
                        mockErrorHandler
                )
        )

        val observation = ObservationBuilder.buildObservationWithComponent()
        observation.component!![0].valueAttachment!!.id = null
        observation.component!!.add(ObservationBuilder.buildComponent(null, AttachmentBuilder.buildAttachment(null)))
        val firstAttachment = observation.component!![0].valueAttachment
        firstAttachment!!.title = "image"
        firstAttachment.hash = DATA_HASH
        val secondAttachment = observation.component!![1].valueAttachment
        secondAttachment!!.title = "pdf"
        secondAttachment.hash = DATA_HASH
        val attachment = observation.valueAttachment
        attachment!!.title = "doc"
        attachment.hash = DATA_HASH
        val uploadResult: MutableList<Pair<WrapperContract.Attachment, List<String>?>> = arrayListOf()
        uploadResult.add(Pair(SdkAttachmentFactory.wrap(firstAttachment), listOf(PREVIEW_ID, THUMBNAIL_ID)))
        uploadResult.add(Pair(SdkAttachmentFactory.wrap(secondAttachment), null))
        uploadResult.add(Pair(SdkAttachmentFactory.wrap(attachment), null))
        val dummyDecryptedRecord = DecryptedRecord(
                RECORD_ID,
                observation,
                null,
                arrayListOf(),
                null,
                null,
                null,
                null,
                -1
        )
        Mockito.`when`(mockCryptoService.generateGCKey()).thenReturn(Single.just(mockAttachmentKey))
        val encrypted = ByteArray(1)
        every { HashUtil.sha1(any()) } returns encrypted
        every { Base64.encodeToString(encrypted) } returns DATA_HASH

        every {
            attachmentService.upload(
                    any(),
                    mockAttachmentKey,
                    USER_ID
            )
        } returns Single.fromCallable {
            uploadResult.also {
                firstAttachment.id = ATTACHMENT_ID
                secondAttachment.id = ATTACHMENT_ID
                attachment.id = ATTACHMENT_ID
            } as List<Pair<WrapperContract.Attachment, List<String>>> //This cast is technically wrong, how ever this is the assumption of updateFhirResourceIdentifier
        }

        //when
        val obs = recordService.uploadOrDownloadData(
                RecordService.UploadDownloadOperation.UPLOAD,
                dummyDecryptedRecord,
                null,
                USER_ID
        ).resource

        //then
        Truth.assertThat(obs).isEqualTo(observation)
        Truth.assertThat(obs!!.identifier).hasSize(1)
        Truth.assertThat(obs.identifier!![0].value).isEqualTo(
                RecordService.DOWNSCALED_ATTACHMENT_IDS_FMT + "#" + ATTACHMENT_ID + "#" +
                        PREVIEW_ID + "#" + THUMBNAIL_ID
        )
        Truth.assertThat(obs.identifier!![0].assigner!!.reference).isEqualTo(PARTNER_ID)

        verify {
            Base64.decode(
                    SdkAttachmentFactory.wrap(attachment).data!!)
        }
        verify {
            Base64.decode(
                    SdkAttachmentFactory.wrap(firstAttachment).data!!)
        }
        verify {
            Base64.decode(
                    SdkAttachmentFactory.wrap(secondAttachment).data!!)
        }
    }

    @Test
    @Throws(DataValidationException.IdUsageViolation::class,
            DataValidationException.ExpectedFieldViolation::class,
            DataValidationException.InvalidAttachmentPayloadHash::class)
    fun uploadOrDownloadData_shouldAppendAdditionalIdentifiers_QuestionnaireResponse() {
        //given
        val recordService = Mockito.spy(
                RecordService(
                        PARTNER_ID,
                        ALIAS,
                        mockApiService,
                        mockTagEncryptionService,
                        mockTaggingService,
                        mockFhirService,
                        attachmentService,
                        mockCryptoService,
                        mockErrorHandler
                )
        )
        val questionnaireResponse = QuestionnaireResponseBuilder.buildQuestionnaireResponse()
        questionnaireResponse.item!!.add(QuestionnaireResponseBuilder.buildItem("", AttachmentBuilder.buildAttachment(null)))
        val firstAttachment = questionnaireResponse.item!![0].answer!![0].valueAttachment
        firstAttachment!!.title = "image"
        firstAttachment.hash = DATA_HASH
        val secondAttachment = questionnaireResponse.item!![1].answer!![0].valueAttachment
        secondAttachment!!.title = "pdf"
        secondAttachment.hash = DATA_HASH
        val uploadResult: MutableList<Pair<WrapperContract.Attachment, List<String>?>> = mutableListOf()
        uploadResult.add(Pair(SdkAttachmentFactory.wrap(firstAttachment), listOf(PREVIEW_ID, THUMBNAIL_ID)))
        uploadResult.add(Pair(SdkAttachmentFactory.wrap(secondAttachment), null))
        val dummyDecryptedRecord = DecryptedRecord(
                RECORD_ID,
                questionnaireResponse,
                null,
                arrayListOf(),
                null,
                null,
                null,
                null,
                -1
        )
        Mockito.`when`(mockCryptoService.generateGCKey()).thenReturn(Single.just(mockAttachmentKey))

        val encrypted = ByteArray(1)
        every { HashUtil.sha1(any()) } returns encrypted
        every { Base64.encodeToString(encrypted) } returns DATA_HASH

        every {
            attachmentService.upload(
                    any(),
                    mockAttachmentKey,
                    USER_ID
            )
        } returns Single.fromCallable {
            uploadResult.also {
                firstAttachment.id = ATTACHMENT_ID
                secondAttachment.id = ATTACHMENT_ID
            } as List<Pair<WrapperContract.Attachment, List<String>>> //This cast is technically wrong, how ever this is the assumption of updateFhirResourceIdentifier
        }

        //when
        val response = recordService.uploadOrDownloadData(
                RecordService.UploadDownloadOperation.UPLOAD,
                dummyDecryptedRecord,
                null,
                USER_ID
        ).resource

        //then
        Truth.assertThat(response).isEqualTo(questionnaireResponse)
        Truth.assertThat(response!!.identifier!!.value).isEqualTo(
                RecordService.DOWNSCALED_ATTACHMENT_IDS_FMT + "#" + ATTACHMENT_ID + "#" +
                        PREVIEW_ID + "#" + THUMBNAIL_ID
        )
        Truth.assertThat(response.identifier!!.assigner!!.reference).isEqualTo(PARTNER_ID)

        verify {
            Base64.decode(
                    SdkAttachmentFactory.wrap(firstAttachment).data!!)
        }
        verify {
            Base64.decode(
                    SdkAttachmentFactory.wrap(secondAttachment).data!!)
        }
    }
}
