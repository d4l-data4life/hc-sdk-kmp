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

import care.data4life.fhir.stu3.model.CarePlan
import care.data4life.sdk.RecordServiceTestBase.Companion.ALIAS
import care.data4life.sdk.RecordServiceTestBase.Companion.ANNOTATIONS
import care.data4life.sdk.RecordServiceTestBase.Companion.PARTNER_ID
import care.data4life.sdk.RecordServiceTestBase.Companion.USER_ID
import care.data4life.sdk.attachment.AttachmentContract
import care.data4life.sdk.fhir.FhirContract
import care.data4life.sdk.tag.TaggingContract
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import io.reactivex.Single
import org.junit.Before
import org.junit.Test
import java.io.IOException
import java.util.*
import kotlin.test.assertEquals

class RecordServiceCountRecordsTest {
    private lateinit var recordService: RecordService
    private lateinit var apiService: ApiService
    private lateinit var cryptoService: CryptoService
    private lateinit var fhirService: FhirContract.Service
    private lateinit var tagEncryptionService: TaggingContract.EncryptionService
    private lateinit var taggingService: TaggingContract.Service
    private lateinit var attachmentService: AttachmentContract.Service
    private lateinit var errorHandler: SdkContract.ErrorHandler
    private lateinit var tags: HashMap<String, String>
    private lateinit var encryptedTags: MutableList<String>
    private val defaultAnnotation: MutableList<String> = mutableListOf()
    private lateinit var encryptedAnnotations: MutableList<String>

    @Before
    fun setUp() {
        apiService = mockk()
        cryptoService = mockk()
        fhirService = mockk()
        tagEncryptionService = mockk()
        taggingService = mockk()
        attachmentService = mockk()
        errorHandler = mockk()
        tags = mockk()
        encryptedTags = mockk()
        encryptedAnnotations = mockk()

        recordService = spyk(RecordService(
                PARTNER_ID,
                ALIAS,
                apiService,
                tagEncryptionService,
                taggingService,
                fhirService,
                attachmentService,
                cryptoService,
                errorHandler
        ))
    }

    @Test
    @Throws(InterruptedException::class, IOException::class)
    fun `Given, countRecords is called with a DomainResource and a UserId, it returns amount of occurrences`() {
        // Given
        val response = 42

        every { apiService.getCount(ALIAS, USER_ID, null) } returns Single.just(response)

        // When
        val observer = recordService.countRecords(null, USER_ID).test().await()

        // Then
        val result = observer
                .assertNoErrors()
                .assertComplete()
                .assertValueCount(1)
                .values()[0]

        assertEquals(
                expected = response,
                actual = result
        )
        verify(exactly = 1) { apiService.getCount(ALIAS, USER_ID, null) }
    }

    @Test
    @Throws(InterruptedException::class, IOException::class)
    fun `Given, countRecords is called with a DomainResource, a UserId and a Tag, it returns amount of occurrences`() {
        // Given
        val response = 42

        every { taggingService.getTagsFromType(CarePlan::class.java as Class<Any>) } returns tags
        every { tagEncryptionService.encryptTags(tags) } returns encryptedTags
        every { tagEncryptionService.encryptAnnotations(defaultAnnotation) } returns defaultAnnotation
        every { encryptedTags.addAll(defaultAnnotation) } returns true
        every { apiService.getCount(ALIAS, USER_ID, encryptedTags) } returns Single.just(response)

        // When
        val observer = recordService.countRecords(CarePlan::class.java, USER_ID).test().await()

        // Then
        val result = observer
                .assertNoErrors()
                .assertComplete()
                .assertValueCount(1)
                .values()[0]

        assertEquals(
                expected = response,
                actual = result
        )
        verify(exactly = 1) { taggingService.getTagsFromType(CarePlan::class.java as Class<Any>) }
        verify(exactly = 1) { tagEncryptionService.encryptTags(tags) }
        verify(exactly = 1) { tagEncryptionService.encryptAnnotations(defaultAnnotation) }
        verify(exactly = 1) { encryptedTags.addAll(defaultAnnotation) }
        verify(exactly = 1) { apiService.getCount(ALIAS, USER_ID, encryptedTags) }
    }

    @Test
    @Throws(InterruptedException::class)
    fun `Given, countRecords is called with a DomainResource, a UserId and Annotations, it returns amount of occurrences`() {
        // Given
        val response = 42

        every { apiService.getCount(ALIAS, USER_ID, null) } returns Single.just(response)

        // When
        val observer = recordService.countRecords(null, USER_ID, ANNOTATIONS).test().await()

        // Then
        val result = observer
                .assertNoErrors()
                .assertComplete()
                .assertValueCount(1)
                .values()[0]

        assertEquals(
                expected = response,
                actual = result
        )
        verify(exactly = 1) { apiService.getCount(ALIAS, USER_ID, null) }
    }

    @Test
    @Throws(InterruptedException::class, IOException::class)
    fun `Given, countRecords is called with a DomainResource, a UserId, a Tag and Annotations, it returns amount of occurrences`() {
        // Given
        val response = 42

        every { taggingService.getTagsFromType(CarePlan::class.java as Class<Any>) } returns tags
        every { tagEncryptionService.encryptTags(tags) } returns encryptedTags
        every { tagEncryptionService.encryptAnnotations(ANNOTATIONS) } returns encryptedAnnotations
        every { encryptedTags.addAll(encryptedAnnotations) } returns true
        every { apiService.getCount(ALIAS, USER_ID, encryptedTags) } returns Single.just(response)

        // When
        val observer = recordService.countRecords(CarePlan::class.java, USER_ID, ANNOTATIONS).test().await()

        // Then
        val result = observer
                .assertNoErrors()
                .assertComplete()
                .assertValueCount(1)
                .values()[0]

        assertEquals(
                expected = response,
                actual = result
        )
        verify(exactly = 1) { taggingService.getTagsFromType(CarePlan::class.java as Class<Any>) }
        verify(exactly = 1) { tagEncryptionService.encryptTags(tags) }
        verify(exactly = 1) { tagEncryptionService.encryptAnnotations(ANNOTATIONS) }
        verify(exactly = 1) { encryptedTags.addAll(encryptedAnnotations) }
        verify(exactly = 1) { apiService.getCount(ALIAS, USER_ID, encryptedTags) }
    }
}
