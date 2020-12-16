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


import care.data4life.sdk.fhir.Fhir3Resource
import care.data4life.sdk.model.DownloadType
import care.data4life.sdk.model.Record
import care.data4life.sdk.model.SdkRecordFactory
import care.data4life.sdk.model.definitions.BaseRecord
import care.data4life.sdk.network.model.EncryptedRecord
import care.data4life.sdk.network.model.NetworkRecordContract
import care.data4life.sdk.record.RecordContract
import care.data4life.sdk.wrapper.FhirElementFactory
import care.data4life.sdk.wrapper.ResourceHelper
import care.data4life.sdk.wrapper.WrapperContract
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import io.reactivex.Completable
import io.reactivex.Single
import org.junit.After
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class RecordServiceTest: RecordTestBase() {
    @Before
    fun setUp() {
        init()

        mockkObject(FhirElementFactory)
        mockkObject(ResourceHelper)
        mockkObject(SdkRecordFactory)
    }

    @After
    fun tearDown() {
        unmockkObject(FhirElementFactory)
        unmockkObject(ResourceHelper)
        unmockkObject(SdkRecordFactory)
    }

    @Test
    fun `it is a Service of Record`() {
        assertTrue(
                ( recordService as Any) is RecordContract.Service
        )
    }

    @Test
    fun `Given, deleteRecord is called with a UserId and a RecordId, it delegates and returns the result of the API`() {
        // Given
        val userId = "asd"
        val recordId = "ads"

        val apiResult: Completable = mockk()

        every { apiService.deleteRecord(ALIAS, recordId, userId) } returns apiResult

        // When
        val result = recordService.deleteRecord(userId, recordId)

        // Then
        assertSame(
                apiResult,
                result
        )
    }

    //ToDo unhappy path -> message == ""
    @Test
    fun `Given, deleteRecords is called with RecordIds and a UserId, it delegates and returns the results of the API`() {
        // Given
        val userId = "asd"
        val recordId = "ads"
        val recordIds = listOf(recordId)

        val apiResult: Completable = mockk()
        val message = "jippi"
        val singledResult = Single.just(message)

        every { recordService.deleteRecord(userId, recordId) } returns apiResult
        every { apiResult.doOnError(any()) } returns apiResult
        every { apiResult.toSingleDefault(recordId) } returns singledResult

        // When
        val observer = recordService.deleteRecords(recordIds, userId).test().await()

        // Then
        val result = observer
                .assertNoErrors()
                .assertComplete()
                .assertValueCount(1)
                .values()[0]

        assertSame(
                message,
                result.successfulDeletes[0]
        )
    }

    // ToDo random string generator
    @Test
    fun `Given, downloadAttachment is called with a RecordId, a AttachmentId, UserId and a DownloadType, it delegates and returns the first result of downloadAttachments`() {
        // Given
        val userId = "asd"
        val recordId = "ads"
        val attachmentId = "lllll"
        val type = DownloadType.Medium

        val response1 = mockk<WrapperContract.Attachment>()
        val response2 = mockk<WrapperContract.Attachment>()

        every {
            recordService.downloadAttachments(recordId, listOf(attachmentId), userId, type)
        } returns Single.just(listOf(response1, response2))

        // When
        val subscriber = recordService.downloadAttachment(recordId, attachmentId, userId, type).test().await()

        // Then
        val result = subscriber
                .assertNoErrors()
                .assertComplete()
                .assertValueCount(1)
                .values()[0]

        assertSame(
                response1,
                result
        )
    }

    @Test
    fun `Given, downloadAttachments is called with a RecordId, AttachmentIds, UserId and a DownloadType, it delegates and returns encountered Attachments`() {
        // Given
        val userId = "asd"
        val recordId = "ads"
        val attachmentId = "lllll"
        val attachmentIds = listOf(attachmentId)
        val type = DownloadType.Medium

        val response1 = mockk<WrapperContract.Attachment>()
        val response2 = mockk<WrapperContract.Attachment>()
        val response = listOf(response1, response2)

        val encryptedRecord = mockk<EncryptedRecord>()
        val decryptedRecord = mockk<NetworkRecordContract.DecryptedRecord>()


        every { apiService.fetchRecord(ALIAS, userId, recordId) } returns Single.just(encryptedRecord)
        every { recordCryptoService.decryptRecord(encryptedRecord, userId) } returns decryptedRecord
        every {
            attachmentService.downloadAttachmentsFromStorage(
                    attachmentIds,
                    userId,
                    type,
                    decryptedRecord
            )
        } returns Single.just(response)

        // When
        val subscriber = recordService.downloadAttachments(recordId, attachmentIds, userId, type).test().await()

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
    fun `Given, deleteAttachment is called, wit a AttachmentId and a UserId, it delegates it to the AttachmentService and returns its result`() {
        // Given
        val userId = "asd"
        val attachmentId = "ads"

        val response = true

        every { attachmentService.delete(attachmentId, userId) } returns Single.just(response)

        // When
        val subscriber = recordService.deleteAttachment(attachmentId, userId).test().await()

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
    fun `Given, downloadRecord is called, wit a RecordId and a UserId, it resolves the record and returns it`() {
        // Given
        val userId = "asd"
        val recordId = "ads"

        val resource = mockk<WrapperContract.Resource>()
        val encryptedRecord = mockk<EncryptedRecord>()
        val decryptedRecord = mockk<NetworkRecordContract.DecryptedRecord>()
        val createdRecord = mockk<Record<Fhir3Resource>>()

        every { decryptedRecord.resource } returns resource

        every { apiService.fetchRecord(ALIAS, userId, recordId) } returns Single.just(encryptedRecord)
        every { recordCryptoService.decryptRecord(encryptedRecord, userId) } returns decryptedRecord
        every { attachmentClient.downloadData(decryptedRecord, userId) } returns decryptedRecord
        every { attachmentService.checkDataRestrictions(resource) } returns mockk()
        every { ResourceHelper.assignResourceId(decryptedRecord) } returns decryptedRecord
        @Suppress("UNCHECKED_CAST")
        every { SdkRecordFactory.getInstance(decryptedRecord) } returns createdRecord as BaseRecord<Any>

        // When
        val subscriber = recordService.downloadRecord<Fhir3Resource>(recordId, userId).test().await()

        // Then
        val result = subscriber
                .assertNoErrors()
                .assertComplete()
                .assertValueCount(1)
                .values()[0]

        assertSame(
                createdRecord,
                result
        )
    }

    // ToDo Unhappy path
    @Test
    fun `Given, downloadRecords is called with RecordIds and UserIds, it streams them into downloadRecord and returns the results`() {
        // Given
        val userId = "asd"
        val recordId = "ads"
        val recordIds = listOf(recordId)


        val createdRecord = mockk<Record<Fhir3Resource>>()

        every { recordService.downloadRecord<Fhir3Resource>(recordId, userId) } returns Single.just(createdRecord)

        // When
        val observer = recordService.downloadRecords<Fhir3Resource>(recordIds, userId).test().await()

        // Then
        val result = observer
                .assertNoErrors()
                .assertComplete()
                .assertValueCount(1)
                .values()[0]

        assertSame(
                createdRecord,
                result.successfulDownloads[0]
        )
    }

    @Test
    fun `Given, countRecords is called, with a null as type, a UserId and Annotations, it delegates it to the API`() {
        val userId = "asd"
        val annotations = mockk<List<String>>()

        val response = 42

        every { apiService.getCount(ALIAS, userId, null) } returns Single.just(response)

        // When
        val observer = recordService.countRecords(null, userId, annotations).test().await()

        // Then
        val result = observer
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
    fun `Given, countRecords is called, with a Fhir3 Class type, a UserId and Annotations, it delegates it to the API`() {
        val type = Fhir3Resource::class.java
        val userId = "asd"
        val annotations = mockk<List<String>>()

        val stringOfType = "potato"
        val tags = mockk<HashMap<String, String>>(relaxed = true)
        val encryptedAnnotations = mockk<MutableList<String>>()
        val encryptedTags = mockk<MutableList<String>>(relaxed = true)

        val response = 42

        every { FhirElementFactory.getFhirTypeForClass(type) } returns stringOfType
        every { taggingService.getTagFromType(stringOfType) } returns tags
        every { tagEncryptionService.encryptTags(tags) as MutableList<String> } returns encryptedTags
        every { tagEncryptionService.encryptAnnotations(annotations) } returns encryptedAnnotations
        every { apiService.getCount(ALIAS, userId, encryptedTags) } returns Single.just(response)

        // When
        val observer = recordService.countRecords(type, userId, annotations).test().await()

        // Then
        val result = observer
                .assertNoErrors()
                .assertComplete()
                .assertValueCount(1)
                .values()[0]

        assertSame(
                response,
                result
        )
    }
    /*
    fun countRecords(
            type: Class<out Fhir3Resource>?,
            userId: String,
            annotations: List<String> = listOf()
    ): Single<Int> = if (type == null) {
        apiService.getCount(alias, userId, null)
    } else {
        Single
                .fromCallable { taggingService.getTagFromType(fhirElementFactory.getFhirTypeForClass(type)) }
                .map { tagEncryptionService.encryptTags(it) as MutableList<String> }
                .map { tags -> tags.also { it.addAll(tagEncryptionService.encryptAnnotations(annotations)) } }
                .flatMap { apiService.getCount(alias, userId, it) }
    }

     */
}
