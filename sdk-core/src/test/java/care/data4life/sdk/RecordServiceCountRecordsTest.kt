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

import care.data4life.fhir.stu3.model.Patient
import care.data4life.sdk.RecordServiceTestBase.Companion.ALIAS
import care.data4life.sdk.RecordServiceTestBase.Companion.PARTNER_ID
import care.data4life.sdk.RecordServiceTestBase.Companion.USER_ID
import care.data4life.sdk.attachment.AttachmentContract
import care.data4life.sdk.fhir.Fhir3Resource
import care.data4life.sdk.fhir.Fhir4Resource
import care.data4life.sdk.fhir.FhirContract
import care.data4life.sdk.tag.TaggingContract
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.spyk
import io.mockk.verify
import io.reactivex.Single
import org.junit.Before
import org.junit.Test
import java.io.IOException
import kotlin.test.assertEquals
import kotlin.test.assertTrue

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
    private val defaultAnnotations = listOf<String>()
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
    fun `Given, countFhir3Records is called with a Fhir3Resource, a UserId and Annotations, it returns amount of occurrences`() {
        // Given
        val expected = 42
        val annotations: List<String> = mockk()

        every { taggingService.getTagsFromType(Fhir3Resource::class.java as Class<Any>) } returns tags
        every { tagEncryptionService.encryptTags(tags) } returns encryptedTags
        every { tagEncryptionService.encryptAnnotations(annotations) } returns encryptedAnnotations
        every { encryptedTags.addAll(encryptedAnnotations) } returns true
        every { apiService.getCount(ALIAS, USER_ID, encryptedTags) } returns Single.just(expected)

        // When
        val observer = recordService.countFhir3Records(
                Fhir3Resource::class.java,
                USER_ID,
                annotations
        ).test().await()

        // Then
        val result = observer
                .assertNoErrors()
                .assertComplete()
                .assertValueCount(1)
                .values()[0]

        assertEquals(
                expected = expected,
                actual = result
        )
        verify(exactly = 1) { taggingService.getTagsFromType(Fhir3Resource::class.java as Class<Any>) }
        verify(exactly = 1) { tagEncryptionService.encryptTags(tags) }
        verify(exactly = 1) { tagEncryptionService.encryptAnnotations(annotations) }
        verify(exactly = 1) { encryptedTags.addAll(encryptedAnnotations) }
        verify(exactly = 1) { apiService.getCount(ALIAS, USER_ID, encryptedTags) }
    }

    @Test
    @Throws(InterruptedException::class, IOException::class)
    fun `Given, countFhir4Records is called with a Fhir4Resource, a UserId and Annotations, it returns amount of occurrences`() {
        // Given
        val expected = 42
        val annotations: List<String> = mockk()

        every { taggingService.getTagsFromType(Fhir4Resource::class.java as Class<Any>) } returns tags
        every { tagEncryptionService.encryptTags(tags) } returns encryptedTags
        every { tagEncryptionService.encryptAnnotations(annotations) } returns encryptedAnnotations
        every { encryptedTags.addAll(encryptedAnnotations) } returns true
        every { apiService.getCount(ALIAS, USER_ID, encryptedTags) } returns Single.just(expected)

        // When
        val observer = recordService.countFhir4Records(
                Fhir4Resource::class.java,
                USER_ID,
                annotations
        ).test().await()

        // Then
        val result = observer
                .assertNoErrors()
                .assertComplete()
                .assertValueCount(1)
                .values()[0]

        assertEquals(
                expected = expected,
                actual = result
        )
        verify(exactly = 1) { taggingService.getTagsFromType(Fhir4Resource::class.java as Class<Any>) }
        verify(exactly = 1) { tagEncryptionService.encryptTags(tags) }
        verify(exactly = 1) { tagEncryptionService.encryptAnnotations(annotations) }
        verify(exactly = 1) { encryptedTags.addAll(encryptedAnnotations) }
        verify(exactly = 1) { apiService.getCount(ALIAS, USER_ID, encryptedTags) }
    }

    @Test
    @Throws(InterruptedException::class, IOException::class)
    fun `Given, countAllFhir3Records is called, with a UserId and Annotations, it returns amount of occurrences`() {
        // Given
        val expected = 42
        val annotations: List<String> = mockk()
        val emptyTags = slot<HashMap<String, String>>()
        val markedTags = hashMapOf("mark" to "mark")

        every {
            taggingService.tagVersion(capture(emptyTags), FhirContract.FhirVersion.FHIR_3)
        } answers {
            assertTrue(emptyTags.captured.isEmpty())
            emptyTags.captured["mark"] = "mark"
        }
        every { tagEncryptionService.encryptTags(markedTags) } returns encryptedTags
        every { tagEncryptionService.encryptAnnotations(annotations) } returns encryptedAnnotations
        every { encryptedTags.addAll(encryptedAnnotations) } returns true
        every { apiService.getCount(ALIAS, USER_ID, encryptedTags) } returns Single.just(expected)

        // When
        val observer = recordService.countAllFhir3Records(
                USER_ID,
                annotations
        ).test().await()

        // Then
        val result = observer
                .assertNoErrors()
                .assertComplete()
                .assertValueCount(1)
                .values()[0]

        assertEquals(
                expected = expected,
                actual = result
        )
        verify(exactly = 1) { taggingService.tagVersion(markedTags, FhirContract.FhirVersion.FHIR_3) }
        verify(exactly = 1) { tagEncryptionService.encryptTags(markedTags) }
        verify(exactly = 1) { tagEncryptionService.encryptAnnotations(annotations) }
        verify(exactly = 1) { encryptedTags.addAll(encryptedAnnotations) }
        verify(exactly = 1) { apiService.getCount(ALIAS, USER_ID, encryptedTags) }
    }

    @Test
    @Throws(InterruptedException::class, IOException::class)
    fun `Given, countRecords is called, with a UserId and resourceType, it delegate its call to countFhir3Records`() {
        // Given
        val resourceType = Patient::class.java
        val expected = 23

        every {
            recordService.countFhir3Records(
                    resourceType,
                    USER_ID,
                    defaultAnnotations
            )
        } returns Single.just(expected)

        val observer = recordService.countRecords(
                resourceType,
                USER_ID
        ).test().await()

        // Then
        val result = observer
                .assertNoErrors()
                .assertComplete()
                .assertValueCount(1)
                .values()[0]

        assertEquals(
                expected = expected,
                actual = result
        )
        verify(exactly = 1) {
            recordService.countFhir3Records(
                    resourceType,
                    USER_ID,
                    defaultAnnotations
            )
        }
    }

    @Test
    @Throws(InterruptedException::class, IOException::class)
    fun `Given, countRecords is called, with a UserId, resourceType and Annotations, it delegate its call to countFhir3Records`() {
        // Given
        val resourceType = Patient::class.java
        val expected = 23
        val annotations: List<String> = mockk()

        every {
            recordService.countFhir3Records(
                    resourceType,
                    USER_ID,
                    annotations
            )
        } returns Single.just(expected)

        val observer = recordService.countRecords(
                resourceType,
                USER_ID,
                annotations
        ).test().await()

        // Then
        val result = observer
                .assertNoErrors()
                .assertComplete()
                .assertValueCount(1)
                .values()[0]

        assertEquals(
                expected = expected,
                actual = result
        )
        verify(exactly = 1) {
            recordService.countFhir3Records(
                    resourceType,
                    USER_ID,
                    annotations
            )
        }
    }

    @Test
    @Throws(InterruptedException::class, IOException::class)
    fun `Given, countRecords is called, with a null as resourceType and a UserId, it delegate its call to countAllFhir3Records`() {
        // Given
        val expected = 23

        every {
            recordService.countAllFhir3Records(
                    USER_ID,
                    defaultAnnotations
            )
        } returns Single.just(expected)

        val observer = recordService.countRecords(
                null,
                USER_ID
        ).test().await()

        // Then
        val result = observer
                .assertNoErrors()
                .assertComplete()
                .assertValueCount(1)
                .values()[0]

        assertEquals(
                expected = expected,
                actual = result
        )
        verify(exactly = 1) {
            recordService.countAllFhir3Records(
                    USER_ID,
                    defaultAnnotations
            )
        }
    }

    @Test
    @Throws(InterruptedException::class, IOException::class)
    fun `Given, countRecords is called, with a null as resourceType, a UserId and Annotations, it delegate its call to countAllFhir3Records`() {
        // Given
        val expected = 23
        val annotations: List<String> = mockk()

        every {
            recordService.countAllFhir3Records(
                    USER_ID,
                    annotations
            )
        } returns Single.just(expected)

        val observer = recordService.countRecords(
                null,
                USER_ID,
                annotations
        ).test().await()

        // Then
        val result = observer
                .assertNoErrors()
                .assertComplete()
                .assertValueCount(1)
                .values()[0]

        assertEquals(
                expected = expected,
                actual = result
        )
        verify(exactly = 1) {
            recordService.countAllFhir3Records(
                    USER_ID,
                    annotations
            )
        }
    }
}
