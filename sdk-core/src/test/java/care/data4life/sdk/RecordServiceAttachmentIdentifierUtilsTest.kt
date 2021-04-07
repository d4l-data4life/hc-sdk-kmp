/*
 * Copyright (c) 2021 D4L data4life gGmbH / All rights reserved.
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

import care.data4life.sdk.RecordServiceTestBase.Companion.ALIAS
import care.data4life.sdk.RecordServiceTestBase.Companion.PARTNER_ID
import care.data4life.sdk.attachment.AttachmentContract
import care.data4life.sdk.attachment.ThumbnailService
import care.data4life.sdk.attachment.ThumbnailService.Companion.SPLIT_CHAR
import care.data4life.sdk.data.DataResource
import care.data4life.sdk.fhir.Fhir3Attachment
import care.data4life.sdk.fhir.Fhir3Identifier
import care.data4life.sdk.fhir.Fhir3Resource
import care.data4life.sdk.fhir.Fhir4Attachment
import care.data4life.sdk.fhir.Fhir4Identifier
import care.data4life.sdk.fhir.Fhir4Resource
import care.data4life.sdk.fhir.FhirContract
import care.data4life.sdk.lang.DataValidationException
import care.data4life.sdk.model.DownloadType
import care.data4life.sdk.record.RecordContract.Service.Companion.DOWNSCALED_ATTACHMENT_IDS_FMT
import care.data4life.sdk.record.RecordContract.Service.Companion.PREVIEW_ID_POS
import care.data4life.sdk.record.RecordContract.Service.Companion.THUMBNAIL_ID_POS
import care.data4life.sdk.tag.TaggingContract
import care.data4life.sdk.wrapper.SdkAttachmentFactory
import care.data4life.sdk.wrapper.SdkFhirAttachmentHelper
import care.data4life.sdk.wrapper.SdkIdentifierFactory
import care.data4life.sdk.wrapper.WrapperContract
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.spyk
import io.mockk.unmockkObject
import io.mockk.verify
import io.mockk.verifyOrder
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertSame

class RecordServiceAttachmentIdentifierUtilsTest {
    private lateinit var recordService: RecordService
    private lateinit var apiService: ApiService
    private lateinit var cryptoService: CryptoService
    private lateinit var fhirService: FhirContract.Service
    private lateinit var tagEncryptionService: TaggingContract.EncryptionService
    private lateinit var taggingService: TaggingContract.Service
    private lateinit var attachmentService: AttachmentContract.Service
    private lateinit var errorHandler: SdkContract.ErrorHandler

    @Before
    fun setUp() {
        apiService = mockk()
        cryptoService = mockk()
        fhirService = mockk()
        tagEncryptionService = mockk()
        taggingService = mockk()
        attachmentService = mockk()
        errorHandler = mockk()

        recordService = spyk(
            RecordService(
                PARTNER_ID,
                ALIAS,
                apiService,
                tagEncryptionService,
                taggingService,
                fhirService,
                attachmentService,
                cryptoService,
                errorHandler,
                mockk()
            )
        )

        mockkObject(SdkFhirAttachmentHelper)
        mockkObject(SdkAttachmentFactory)
        mockkObject(SdkIdentifierFactory)
    }

    @After
    fun tearDown() {
        unmockkObject(SdkFhirAttachmentHelper)
        unmockkObject(SdkAttachmentFactory)
        unmockkObject(SdkIdentifierFactory)
    }

    @Test
    fun `Given, splitAdditionalAttachmentId is called, with a WrappedIdentifier, it returns null, if the Identifiers value is null`() {
        // Given
        val additionalIdentifier: WrapperContract.Identifier = mockk()

        every { additionalIdentifier.value } returns null

        // When
        val additionalIds = recordService.splitAdditionalAttachmentId(additionalIdentifier)

        // Then
        assertNull(additionalIds)
    }

    @Test
    fun `Given, splitAdditionalAttachmentId is called, with a WrappedIdentifier, it returns null, if the Identifiers value does not start with DOWNSCALED_ATTACHMENT_IDS_FMT`() {
        // Given
        val additionalIdentifier: WrapperContract.Identifier = mockk()

        every { additionalIdentifier.value } returns "potato"

        // When
        val additionalIds = recordService.splitAdditionalAttachmentId(additionalIdentifier)

        // Then
        assertNull(additionalIds)
    }

    @Test
    fun `Given, splitAdditionalAttachmentId is called, with a WrappedIdentifier, it fails, if the parts of the Identifiers value does not match DOWNSCALED_ATTACHMENT_IDS_SIZE`() {
        // Given
        val additionalIdentifier: WrapperContract.Identifier = mockk()

        every { additionalIdentifier.value } returns DOWNSCALED_ATTACHMENT_IDS_FMT

        // Then
        val error = assertFailsWith<DataValidationException.IdUsageViolation> {
            // When
            recordService.splitAdditionalAttachmentId(additionalIdentifier)
        }

        assertEquals(
            actual = error.message,
            expected = DOWNSCALED_ATTACHMENT_IDS_FMT
        )
    }

    @Test
    fun `Given, splitAdditionalAttachmentId is called, with a WrappedIdentifier, it splits the Identifiers value`() {
        // Given
        val additionalIdentifier: WrapperContract.Identifier = mockk()
        val value = listOf(
            DOWNSCALED_ATTACHMENT_IDS_FMT,
            "potato",
            "tomato",
            "soup"
        )

        every { additionalIdentifier.value } returns value.joinToString(ThumbnailService.SPLIT_CHAR)

        // When
        val parts = recordService.splitAdditionalAttachmentId(additionalIdentifier)

        // Then
        assertEquals(
            actual = parts,
            expected = value
        )
    }

    @Test
    fun `Given, cleanObsoleteAdditionalIdentifiers is called with DataResource, it does nothing`() {
        // Given
        val resource: DataResource = mockk()

        // When
        recordService.cleanObsoleteAdditionalIdentifiers(resource)

        // Then
        verify(exactly = 0) { SdkFhirAttachmentHelper.getAttachment(any()) }
    }

    @Test
    fun `Given, cleanObsoleteAdditionalIdentifiers is called with Fhir3Resource, which is not capable of having Attachments, it does nothing`() {
        // Given
        val resource: Fhir3Resource = mockk()

        every { SdkFhirAttachmentHelper.hasAttachment(resource) } returns false
        every { SdkFhirAttachmentHelper.getAttachment(resource) } returns mockk()

        // When
        recordService.cleanObsoleteAdditionalIdentifiers(resource)

        // Then
        verify(exactly = 0) { SdkFhirAttachmentHelper.getIdentifier(any()) }
        verify(exactly = 0) { SdkFhirAttachmentHelper.getAttachment(resource) }
    }

    @Test
    fun `Given, cleanObsoleteAdditionalIdentifiers is called with Fhir3Resource, which has not Attachments, it does nothing`() {
        // Given
        val resource: Fhir3Resource = mockk()
        val attachments: MutableList<Fhir3Attachment?> = mutableListOf()

        every { SdkFhirAttachmentHelper.hasAttachment(resource) } returns true
        every { SdkFhirAttachmentHelper.getAttachment(resource) } returns attachments as MutableList<Any?>

        // When
        recordService.cleanObsoleteAdditionalIdentifiers(resource)

        // Then
        verify(exactly = 0) { SdkFhirAttachmentHelper.getIdentifier(any()) }
    }

    @Test
    fun `Given, cleanObsoleteAdditionalIdentifiers is called with Fhir3Resource, which has Attachments, but no Identifiers, it does nothing`() {
        // Given
        val resource: Fhir3Resource = mockk()
        val attachments: MutableList<Fhir3Attachment?> = mutableListOf(
            mockk()
        )

        every { SdkFhirAttachmentHelper.hasAttachment(resource) } returns true
        every { SdkFhirAttachmentHelper.getAttachment(resource) } returns attachments as MutableList<Any?>
        every { SdkFhirAttachmentHelper.getIdentifier(resource) } returns null

        // When
        recordService.cleanObsoleteAdditionalIdentifiers(resource)

        // Then
        verify(exactly = 0) { SdkFhirAttachmentHelper.setIdentifier(any(), any()) }
    }

    @Test
    fun `Given, cleanObsoleteAdditionalIdentifiers is called with Fhir3Resource, which has Attachments, but no actual Identifiers, it updates the resource, with a empty list`() {
        // Given
        val resource: Fhir3Resource = mockk(relaxed = true)
        val attachments: MutableList<Fhir3Attachment?> = mutableListOf(
            mockk()
        )
        val identifiers: MutableList<Fhir3Identifier> = mutableListOf()

        every { SdkFhirAttachmentHelper.hasAttachment(resource) } returns true
        every { SdkFhirAttachmentHelper.getAttachment(resource) } returns attachments as MutableList<Any?>
        every { SdkFhirAttachmentHelper.getIdentifier(resource) } returns identifiers
        every { SdkFhirAttachmentHelper.setIdentifier(resource, listOf()) } just Runs

        // When
        recordService.cleanObsoleteAdditionalIdentifiers(resource)

        // Then
        verifyOrder {
            recordService.cleanObsoleteAdditionalIdentifiers(resource)
            SdkFhirAttachmentHelper.hasAttachment(resource)
            SdkFhirAttachmentHelper.getAttachment(resource)
            SdkFhirAttachmentHelper.getIdentifier(resource)
            SdkFhirAttachmentHelper.setIdentifier(resource, listOf())
        }
    }

    @Test
    fun `Given, cleanObsoleteAdditionalIdentifiers is called with Fhir3Resource, which has Attachments and Identifiers, it updates the Identifiers, if a Identifier is not splittable`() {
        // Given
        val resource: Fhir3Resource = mockk(relaxed = true)
        val attachmentId = "id"
        val attachments: MutableList<Any> = mutableListOf(
            mockk()
        )
        val wrappedAttachment: WrapperContract.Attachment = mockk()

        val identifiers: MutableList<Fhir3Identifier> = mutableListOf(
            mockk()
        )
        val wrappedIdentifier: WrapperContract.Identifier = mockk()

        every { wrappedAttachment.id } returns attachmentId
        every { wrappedIdentifier.unwrap<Fhir3Identifier>() } returns identifiers[0]

        every { SdkFhirAttachmentHelper.hasAttachment(resource) } returns true
        every { SdkFhirAttachmentHelper.getAttachment(resource) } returns attachments as MutableList<Any?>
        every { SdkFhirAttachmentHelper.getIdentifier(resource) } returns identifiers
        every { SdkAttachmentFactory.wrap(attachments[0]) } returns wrappedAttachment
        every { SdkIdentifierFactory.wrap(identifiers[0]) } returns wrappedIdentifier
        every { recordService.splitAdditionalAttachmentId(wrappedIdentifier) } returns null
        every { SdkFhirAttachmentHelper.setIdentifier(resource, listOf(identifiers[0])) } just Runs

        // When
        recordService.cleanObsoleteAdditionalIdentifiers(resource)

        // Then
        verifyOrder {
            recordService.cleanObsoleteAdditionalIdentifiers(resource)
            SdkFhirAttachmentHelper.hasAttachment(resource)
            SdkFhirAttachmentHelper.getAttachment(resource)
            SdkFhirAttachmentHelper.getIdentifier(resource)
            SdkAttachmentFactory.wrap(attachments[0])
            SdkIdentifierFactory.wrap(identifiers[0])
            recordService.splitAdditionalAttachmentId(wrappedIdentifier)
            SdkFhirAttachmentHelper.setIdentifier(resource, listOf(identifiers[0]))
        }
    }

    @Test
    fun `Given, cleanObsoleteAdditionalIdentifiers is called with Fhir3Resource, which has Attachments and Identifiers, it updates the Identifiers, if a Identifier matches the AttachmentId`() {
        // Given
        val resource: Fhir3Resource = mockk(relaxed = true)
        val attachmentId = "id"
        val attachments: MutableList<Any> = mutableListOf(
            mockk()
        )
        val wrappedAttachment: WrapperContract.Attachment = mockk()

        val identifiers: MutableList<Fhir3Identifier> = mutableListOf(
            mockk()
        )
        val wrappedIdentifier: WrapperContract.Identifier = mockk()
        val splittedIdentifier = listOf("any", attachmentId)

        every { wrappedAttachment.id } returns attachmentId
        every { wrappedIdentifier.unwrap<Fhir3Identifier>() } returns identifiers[0]

        every { SdkFhirAttachmentHelper.hasAttachment(resource) } returns true
        every { SdkFhirAttachmentHelper.getAttachment(resource) } returns attachments as MutableList<Any?>
        every { SdkFhirAttachmentHelper.getIdentifier(resource) } returns identifiers
        every { SdkAttachmentFactory.wrap(attachments[0]) } returns wrappedAttachment
        every { SdkIdentifierFactory.wrap(identifiers[0]) } returns wrappedIdentifier
        every { recordService.splitAdditionalAttachmentId(wrappedIdentifier) } returns splittedIdentifier
        every { SdkFhirAttachmentHelper.setIdentifier(resource, listOf(identifiers[0])) } just Runs

        // When
        recordService.cleanObsoleteAdditionalIdentifiers(resource)

        // Then
        verifyOrder {
            recordService.cleanObsoleteAdditionalIdentifiers(resource)
            SdkFhirAttachmentHelper.hasAttachment(resource)
            SdkFhirAttachmentHelper.getAttachment(resource)
            SdkFhirAttachmentHelper.getIdentifier(resource)
            SdkAttachmentFactory.wrap(attachments[0])
            SdkIdentifierFactory.wrap(identifiers[0])
            recordService.splitAdditionalAttachmentId(wrappedIdentifier)
            SdkFhirAttachmentHelper.setIdentifier(resource, listOf(identifiers[0]))
        }
    }

    @Test
    fun `Given, cleanObsoleteAdditionalIdentifiers is called with Fhir3Resource, which has Attachments and Identifiers, it ignores the Identifiers, if a Identifier does not match the AttachmentId`() {
        // Given
        val resource: Fhir3Resource = mockk(relaxed = true)
        val attachmentId = "id"
        val attachments: MutableList<Any> = mutableListOf(
            mockk()
        )
        val wrappedAttachment: WrapperContract.Attachment = mockk()

        val identifiers: MutableList<Fhir3Identifier> = mutableListOf(
            mockk()
        )
        val wrappedIdentifier: WrapperContract.Identifier = mockk()
        val splittedIdentifier = listOf("any", "any")

        every { wrappedAttachment.id } returns attachmentId
        every { wrappedIdentifier.unwrap<Fhir3Identifier>() } returns identifiers[0]

        every { SdkFhirAttachmentHelper.hasAttachment(resource) } returns true
        every { SdkFhirAttachmentHelper.getAttachment(resource) } returns attachments as MutableList<Any?>
        every { SdkFhirAttachmentHelper.getIdentifier(resource) } returns identifiers
        every { SdkAttachmentFactory.wrap(attachments[0]) } returns wrappedAttachment
        every { SdkIdentifierFactory.wrap(identifiers[0]) } returns wrappedIdentifier
        every { recordService.splitAdditionalAttachmentId(wrappedIdentifier) } returns splittedIdentifier
        every { SdkFhirAttachmentHelper.setIdentifier(resource, listOf()) } just Runs

        // When
        recordService.cleanObsoleteAdditionalIdentifiers(resource)

        // Then
        verifyOrder {
            recordService.cleanObsoleteAdditionalIdentifiers(resource)
            SdkFhirAttachmentHelper.hasAttachment(resource)
            SdkFhirAttachmentHelper.getAttachment(resource)
            SdkFhirAttachmentHelper.getIdentifier(resource)
            SdkAttachmentFactory.wrap(attachments[0])
            SdkIdentifierFactory.wrap(identifiers[0])
            recordService.splitAdditionalAttachmentId(wrappedIdentifier)
            SdkFhirAttachmentHelper.setIdentifier(resource, listOf())
        }
    }

    @Test
    fun `Given, cleanObsoleteAdditionalIdentifiers is called with Fhir3Resource, which has Attachments and Identifiers, it updates the Identifiers and ignores Attachments, which are null`() {
        // Given
        val resource: Fhir3Resource = mockk(relaxed = true)
        val attachmentId = "id"
        val attachments: MutableList<Any?> = mutableListOf(
            null,
            mockk()
        )
        val wrappedAttachment: WrapperContract.Attachment = mockk()

        val identifiers: MutableList<Fhir3Identifier> = mutableListOf(
            mockk()
        )
        val wrappedIdentifier: WrapperContract.Identifier = mockk()
        val splittedIdentifier = listOf("any", attachmentId)

        every { wrappedAttachment.id } returns attachmentId
        every { wrappedIdentifier.unwrap<Fhir3Identifier>() } returns identifiers[0]

        every { SdkFhirAttachmentHelper.hasAttachment(resource) } returns true
        every { SdkFhirAttachmentHelper.getAttachment(resource) } returns attachments
        every { SdkFhirAttachmentHelper.getIdentifier(resource) } returns identifiers
        every { SdkAttachmentFactory.wrap(attachments[1]!!) } returns wrappedAttachment
        every { SdkIdentifierFactory.wrap(identifiers[0]) } returns wrappedIdentifier
        every { recordService.splitAdditionalAttachmentId(wrappedIdentifier) } returns splittedIdentifier
        every { SdkFhirAttachmentHelper.setIdentifier(resource, listOf(identifiers[0])) } just Runs

        // When
        recordService.cleanObsoleteAdditionalIdentifiers(resource)

        // Then
        verifyOrder {
            recordService.cleanObsoleteAdditionalIdentifiers(resource)
            SdkFhirAttachmentHelper.hasAttachment(resource)
            SdkFhirAttachmentHelper.getAttachment(resource)
            SdkFhirAttachmentHelper.getIdentifier(resource)
            SdkAttachmentFactory.wrap(attachments[1]!!)
            SdkIdentifierFactory.wrap(identifiers[0])
            recordService.splitAdditionalAttachmentId(wrappedIdentifier)
            SdkFhirAttachmentHelper.setIdentifier(resource, listOf(identifiers[0]))
        }
    }

    @Test
    fun `Given, cleanObsoleteAdditionalIdentifiers is called with Fhir4Resource, which is not capable of having Attachments, it does nothing`() {
        // Given
        val resource: Fhir4Resource = mockk()

        every { SdkFhirAttachmentHelper.hasAttachment(resource) } returns false
        every { SdkFhirAttachmentHelper.getAttachment(resource) } returns mockk()

        // When
        recordService.cleanObsoleteAdditionalIdentifiers(resource)

        // Then
        verify(exactly = 0) { SdkFhirAttachmentHelper.getIdentifier(any()) }
        verify(exactly = 0) { SdkFhirAttachmentHelper.getAttachment(resource) }
    }

    @Test
    fun `Given, cleanObsoleteAdditionalIdentifiers is called with Fhir4Resource, which has not Attachments, it does nothing`() {
        // Given
        val resource: Fhir4Resource = mockk()
        val attachments: MutableList<Fhir4Attachment?> = mutableListOf()

        every { SdkFhirAttachmentHelper.hasAttachment(resource) } returns true
        every { SdkFhirAttachmentHelper.getAttachment(resource) } returns attachments as MutableList<Any?>

        // When
        recordService.cleanObsoleteAdditionalIdentifiers(resource)

        // Then
        verify(exactly = 0) { SdkFhirAttachmentHelper.getIdentifier(any()) }
    }

    @Test
    fun `Given, cleanObsoleteAdditionalIdentifiers is called with Fhir4Resource, which has Attachments, but no Identifiers, it does nothing`() {
        // Given
        val resource: Fhir4Resource = mockk()
        val attachments: MutableList<Fhir4Attachment?> = mutableListOf(
            mockk()
        )

        every { SdkFhirAttachmentHelper.hasAttachment(resource) } returns true
        every { SdkFhirAttachmentHelper.getAttachment(resource) } returns attachments as MutableList<Any?>
        every { SdkFhirAttachmentHelper.getIdentifier(resource) } returns null

        // When
        recordService.cleanObsoleteAdditionalIdentifiers(resource)

        // Then
        verify(exactly = 0) { SdkFhirAttachmentHelper.setIdentifier(any(), any()) }
    }

    @Test
    fun `Given, cleanObsoleteAdditionalIdentifiers is called with Fhir4Resource, which has Attachments, but no actual Identifiers, it updates the resource, with a empty list`() {
        // Given
        val resource: Fhir4Resource = mockk(relaxed = true)
        val attachments: MutableList<Fhir4Attachment?> = mutableListOf(
            mockk()
        )
        val identifiers: MutableList<Fhir4Identifier> = mutableListOf()

        every { SdkFhirAttachmentHelper.hasAttachment(resource) } returns true
        every { SdkFhirAttachmentHelper.getAttachment(resource) } returns attachments as MutableList<Any?>
        every { SdkFhirAttachmentHelper.getIdentifier(resource) } returns identifiers
        every { SdkFhirAttachmentHelper.setIdentifier(resource, listOf()) } just Runs

        // When
        recordService.cleanObsoleteAdditionalIdentifiers(resource)

        // Then
        verifyOrder {
            recordService.cleanObsoleteAdditionalIdentifiers(resource)
            SdkFhirAttachmentHelper.hasAttachment(resource)
            SdkFhirAttachmentHelper.getAttachment(resource)
            SdkFhirAttachmentHelper.getIdentifier(resource)
            SdkFhirAttachmentHelper.setIdentifier(resource, listOf())
        }
    }

    @Test
    fun `Given, cleanObsoleteAdditionalIdentifiers is called with Fhir4Resource, which has Attachments and Identifiers, it updates the Identifiers, if a Identifier is not splittable`() {
        // Given
        val resource: Fhir4Resource = mockk(relaxed = true)
        val attachmentId = "id"
        val attachments: MutableList<Any> = mutableListOf(
            mockk()
        )
        val wrappedAttachment: WrapperContract.Attachment = mockk()

        val identifiers: MutableList<Fhir4Identifier> = mutableListOf(
            mockk()
        )
        val wrappedIdentifier: WrapperContract.Identifier = mockk()

        every { wrappedAttachment.id } returns attachmentId
        every { wrappedIdentifier.unwrap<Fhir4Identifier>() } returns identifiers[0]

        every { SdkFhirAttachmentHelper.hasAttachment(resource) } returns true
        every { SdkFhirAttachmentHelper.getAttachment(resource) } returns attachments as MutableList<Any?>
        every { SdkFhirAttachmentHelper.getIdentifier(resource) } returns identifiers
        every { SdkAttachmentFactory.wrap(attachments[0]) } returns wrappedAttachment
        every { SdkIdentifierFactory.wrap(identifiers[0]) } returns wrappedIdentifier
        every { recordService.splitAdditionalAttachmentId(wrappedIdentifier) } returns null
        every { SdkFhirAttachmentHelper.setIdentifier(resource, listOf(identifiers[0])) } just Runs

        // When
        recordService.cleanObsoleteAdditionalIdentifiers(resource)

        // Then
        verifyOrder {
            recordService.cleanObsoleteAdditionalIdentifiers(resource)
            SdkFhirAttachmentHelper.hasAttachment(resource)
            SdkFhirAttachmentHelper.getAttachment(resource)
            SdkFhirAttachmentHelper.getIdentifier(resource)
            SdkAttachmentFactory.wrap(attachments[0])
            SdkIdentifierFactory.wrap(identifiers[0])
            recordService.splitAdditionalAttachmentId(wrappedIdentifier)
            SdkFhirAttachmentHelper.setIdentifier(resource, listOf(identifiers[0]))
        }
    }

    @Test
    fun `Given, cleanObsoleteAdditionalIdentifiers is called with Fhir4Resource, which has Attachments and Identifiers, it updates the Identifiers, if a Identifier matches the AttachmentId`() {
        // Given
        val resource: Fhir4Resource = mockk(relaxed = true)
        val attachmentId = "id"
        val attachments: MutableList<Any> = mutableListOf(
            mockk()
        )
        val wrappedAttachment: WrapperContract.Attachment = mockk()

        val identifiers: MutableList<Fhir4Identifier> = mutableListOf(
            mockk()
        )
        val wrappedIdentifier: WrapperContract.Identifier = mockk()
        val splittedIdentifier = listOf("any", attachmentId)

        every { wrappedAttachment.id } returns attachmentId
        every { wrappedIdentifier.unwrap<Fhir4Identifier>() } returns identifiers[0]

        every { SdkFhirAttachmentHelper.hasAttachment(resource) } returns true
        every { SdkFhirAttachmentHelper.getAttachment(resource) } returns attachments as MutableList<Any?>
        every { SdkFhirAttachmentHelper.getIdentifier(resource) } returns identifiers
        every { SdkAttachmentFactory.wrap(attachments[0]) } returns wrappedAttachment
        every { SdkIdentifierFactory.wrap(identifiers[0]) } returns wrappedIdentifier
        every { recordService.splitAdditionalAttachmentId(wrappedIdentifier) } returns splittedIdentifier
        every { SdkFhirAttachmentHelper.setIdentifier(resource, listOf(identifiers[0])) } just Runs

        // When
        recordService.cleanObsoleteAdditionalIdentifiers(resource)

        // Then
        verifyOrder {
            recordService.cleanObsoleteAdditionalIdentifiers(resource)
            SdkFhirAttachmentHelper.hasAttachment(resource)
            SdkFhirAttachmentHelper.getAttachment(resource)
            SdkFhirAttachmentHelper.getIdentifier(resource)
            SdkAttachmentFactory.wrap(attachments[0])
            SdkIdentifierFactory.wrap(identifiers[0])
            recordService.splitAdditionalAttachmentId(wrappedIdentifier)
            SdkFhirAttachmentHelper.setIdentifier(resource, listOf(identifiers[0]))
        }
    }

    @Test
    fun `Given, cleanObsoleteAdditionalIdentifiers is called with Fhir4Resource, which has Attachments and Identifiers, it ignores the Identifiers, if a Identifier does not match the AttachmentId`() {
        // Given
        val resource: Fhir4Resource = mockk(relaxed = true)
        val attachmentId = "id"
        val attachments: MutableList<Any> = mutableListOf(
            mockk()
        )
        val wrappedAttachment: WrapperContract.Attachment = mockk()

        val identifiers: MutableList<Fhir4Identifier> = mutableListOf(
            mockk()
        )
        val wrappedIdentifier: WrapperContract.Identifier = mockk()
        val splittedIdentifier = listOf("any", "any")

        every { wrappedAttachment.id } returns attachmentId
        every { wrappedIdentifier.unwrap<Fhir4Identifier>() } returns identifiers[0]

        every { SdkFhirAttachmentHelper.hasAttachment(resource) } returns true
        every { SdkFhirAttachmentHelper.getAttachment(resource) } returns attachments as MutableList<Any?>
        every { SdkFhirAttachmentHelper.getIdentifier(resource) } returns identifiers
        every { SdkAttachmentFactory.wrap(attachments[0]) } returns wrappedAttachment
        every { SdkIdentifierFactory.wrap(identifiers[0]) } returns wrappedIdentifier
        every { recordService.splitAdditionalAttachmentId(wrappedIdentifier) } returns splittedIdentifier
        every { SdkFhirAttachmentHelper.setIdentifier(resource, listOf()) } just Runs

        // When
        recordService.cleanObsoleteAdditionalIdentifiers(resource)

        // Then
        verifyOrder {
            recordService.cleanObsoleteAdditionalIdentifiers(resource)
            SdkFhirAttachmentHelper.hasAttachment(resource)
            SdkFhirAttachmentHelper.getAttachment(resource)
            SdkFhirAttachmentHelper.getIdentifier(resource)
            SdkAttachmentFactory.wrap(attachments[0])
            SdkIdentifierFactory.wrap(identifiers[0])
            recordService.splitAdditionalAttachmentId(wrappedIdentifier)
            SdkFhirAttachmentHelper.setIdentifier(resource, listOf())
        }
    }

    @Test
    fun `Given, cleanObsoleteAdditionalIdentifiers is called with Fhir4Resource, which has Attachments and Identifiers, it updates the Identifiers and ignores Attachments, which are null`() {
        // Given
        val resource: Fhir4Resource = mockk(relaxed = true)
        val attachmentId = "id"
        val attachments: MutableList<Any?> = mutableListOf(
            null,
            mockk()
        )
        val wrappedAttachment: WrapperContract.Attachment = mockk()

        val identifiers: MutableList<Fhir4Identifier> = mutableListOf(
            mockk()
        )
        val wrappedIdentifier: WrapperContract.Identifier = mockk()
        val splittedIdentifier = listOf("any", attachmentId)

        every { wrappedAttachment.id } returns attachmentId
        every { wrappedIdentifier.unwrap<Fhir4Identifier>() } returns identifiers[0]

        every { SdkFhirAttachmentHelper.hasAttachment(resource) } returns true
        every { SdkFhirAttachmentHelper.getAttachment(resource) } returns attachments
        every { SdkFhirAttachmentHelper.getIdentifier(resource) } returns identifiers
        every { SdkAttachmentFactory.wrap(attachments[1]!!) } returns wrappedAttachment
        every { SdkIdentifierFactory.wrap(identifiers[0]) } returns wrappedIdentifier
        every { recordService.splitAdditionalAttachmentId(wrappedIdentifier) } returns splittedIdentifier
        every { SdkFhirAttachmentHelper.setIdentifier(resource, listOf(identifiers[0])) } just Runs

        // When
        recordService.cleanObsoleteAdditionalIdentifiers(resource)

        // Then
        verifyOrder {
            recordService.cleanObsoleteAdditionalIdentifiers(resource)
            SdkFhirAttachmentHelper.hasAttachment(resource)
            SdkFhirAttachmentHelper.getAttachment(resource)
            SdkFhirAttachmentHelper.getIdentifier(resource)
            SdkAttachmentFactory.wrap(attachments[1]!!)
            SdkIdentifierFactory.wrap(identifiers[0])
            recordService.splitAdditionalAttachmentId(wrappedIdentifier)
            SdkFhirAttachmentHelper.setIdentifier(resource, listOf(identifiers[0]))
        }
    }

    @Test
    fun `Given, extractAdditionalAttachmentIds a null and an AttachmentId, it returns null`() {
        assertNull(
            recordService.extractAdditionalAttachmentIds(null, "any")
        )
    }

    @Test
    fun `Given, extractAdditionalAttachmentIds a list of Fhir3Identifier and an AttachmentId, it returns null, if no part of a Identifier is splittable`() {
        // Given
        val identifiers: List<Fhir3Identifier> = listOf(
            mockk()
        )
        val wrappedIdentifier: WrapperContract.Identifier = mockk()

        val attachmentId = "id"

        every { SdkIdentifierFactory.wrap(identifiers[0]) } returns wrappedIdentifier
        every { recordService.splitAdditionalAttachmentId(wrappedIdentifier) } returns null

        // When
        val extracted = recordService.extractAdditionalAttachmentIds(identifiers, attachmentId)

        // Then
        assertNull(extracted)
    }

    @Test
    fun `Given, extractAdditionalAttachmentIds a list of Fhir3Identifier and an AttachmentId, it returns null, if no part of a Identifier matches the AttachmentId`() {
        // Given
        val identifiers: List<Fhir3Identifier> = listOf(
            mockk()
        )
        val wrappedIdentifier: WrapperContract.Identifier = mockk()

        val attachmentId = "id"
        val parts = listOf("tomato", "soup")

        every { SdkIdentifierFactory.wrap(identifiers[0]) } returns wrappedIdentifier
        every { recordService.splitAdditionalAttachmentId(wrappedIdentifier) } returns parts

        // When
        val extracted = recordService.extractAdditionalAttachmentIds(identifiers, attachmentId)

        // Then
        assertNull(extracted)
    }

    @Test
    fun `Given, extractAdditionalAttachmentIds a list of Fhir3Identifier and an AttachmentId, it returns the first parts of a Identifier, which match the AttachmentId`() {
        // Given
        val identifiers: List<Fhir3Identifier> = listOf(
            mockk()
        )
        val wrappedIdentifier: WrapperContract.Identifier = mockk()

        val attachmentId = "id"
        val parts = listOf("tomato", attachmentId)

        every { SdkIdentifierFactory.wrap(identifiers[0]) } returns wrappedIdentifier
        every { recordService.splitAdditionalAttachmentId(wrappedIdentifier) } returns parts

        // When
        val extracted = recordService.extractAdditionalAttachmentIds(identifiers, attachmentId)

        // Then
        assertSame(
            actual = extracted,
            expected = parts
        )
    }

    @Test
    fun `Given, extractAdditionalAttachmentIds a list of Fhir4Identifier and an AttachmentId, it returns null, if no part of a Identifier is splittable`() {
        // Given
        val identifiers: List<Fhir4Identifier> = listOf(
            mockk()
        )
        val wrappedIdentifier: WrapperContract.Identifier = mockk()

        val attachmentId = "id"

        every { SdkIdentifierFactory.wrap(identifiers[0]) } returns wrappedIdentifier
        every { recordService.splitAdditionalAttachmentId(wrappedIdentifier) } returns null

        // When
        val extracted = recordService.extractAdditionalAttachmentIds(identifiers, attachmentId)

        // Then
        assertNull(extracted)
    }

    @Test
    fun `Given, extractAdditionalAttachmentIds a list of Fhir4Identifier and an AttachmentId, it returns null, if no part of a Identifier matches the AttachmentId`() {
        // Given
        val identifiers: List<Fhir4Identifier> = listOf(
            mockk()
        )
        val wrappedIdentifier: WrapperContract.Identifier = mockk()

        val attachmentId = "id"
        val parts = listOf("tomato", "soup")

        every { SdkIdentifierFactory.wrap(identifiers[0]) } returns wrappedIdentifier
        every { recordService.splitAdditionalAttachmentId(wrappedIdentifier) } returns parts

        // When
        val extracted = recordService.extractAdditionalAttachmentIds(identifiers, attachmentId)

        // Then
        assertNull(extracted)
    }

    @Test
    fun `Given, extractAdditionalAttachmentIds a list of Fhir4Identifier and an AttachmentId, it returns the first parts of a Identifier, which match the AttachmentId`() {
        // Given
        val identifiers: List<Fhir4Identifier> = listOf(
            mockk()
        )
        val wrappedIdentifier: WrapperContract.Identifier = mockk()

        val attachmentId = "id"
        val parts = listOf("tomato", attachmentId)

        every { SdkIdentifierFactory.wrap(identifiers[0]) } returns wrappedIdentifier
        every { recordService.splitAdditionalAttachmentId(wrappedIdentifier) } returns parts

        // When
        val extracted = recordService.extractAdditionalAttachmentIds(identifiers, attachmentId)

        // Then
        assertSame(
            actual = extracted,
            expected = parts
        )
    }

    @Test
    fun `Given, setAttachmentIdForDownloadType is called with a list of Fhir3Attachments, a list of Fhir3Identifier and a DownloadType, it does nothing, if the extracted Ids are null`() {
        // When
        val attachments: List<Fhir3Attachment> = listOf(
            mockk()
        )
        val attachmentId = "id"
        val wrappedAttachment: WrapperContract.Attachment = mockk()
        val identifiers: List<Fhir3Identifier> = mockk()

        every { wrappedAttachment.id } returns attachmentId

        every { SdkAttachmentFactory.wrap(attachments[0]) } returns wrappedAttachment
        every {
            recordService.extractAdditionalAttachmentIds(
                identifiers,
                attachmentId
            )
        } returns null

        // When
        recordService.setAttachmentIdForDownloadType(
            attachments,
            identifiers,
            DownloadType.Small
        )

        // Then
        verify(exactly = 0) { wrappedAttachment.id = any() }
    }

    @Test
    fun `Given, setAttachmentIdForDownloadType is called with a list of Fhir3Attachments, a list of Fhir3Identifier and a DownloadType, it does nothing, if DownloadType is FULL`() {
        // When
        val extracted: List<String> = mockk()
        val attachments: List<Fhir3Attachment> = listOf(
            mockk()
        )
        val attachmentId = "id"
        val wrappedAttachment: WrapperContract.Attachment = mockk()
        val identifiers: List<Fhir3Identifier> = mockk()

        every { wrappedAttachment.id } returns attachmentId

        every { SdkAttachmentFactory.wrap(attachments[0]) } returns wrappedAttachment
        every {
            recordService.extractAdditionalAttachmentIds(
                identifiers,
                attachmentId
            )
        } returns extracted

        // When
        recordService.setAttachmentIdForDownloadType(
            attachments,
            identifiers,
            DownloadType.Full
        )

        // Then
        verify(exactly = 0) { wrappedAttachment.id = any() }
    }

    @Test
    fun `Given, setAttachmentIdForDownloadType is called with a list of Fhir3Attachments, a list of Fhir3Identifier and a DownloadType, it adds PREVIEW_ID_POS to the AttachmentId, if DownloadType is MEDIUM`() {
        // When
        val extracted: List<String> = listOf(
            "tomato",
            "potato",
            "cucumber",
            "soup"
        )
        val attachments: List<Fhir3Attachment> = listOf(
            mockk()
        )
        val attachmentId = "id"
        val wrappedAttachment: WrapperContract.Attachment = mockk()
        val identifiers: List<Fhir3Identifier> = mockk()

        every { wrappedAttachment.id } returns attachmentId
        every {
            wrappedAttachment.id = attachmentId + SPLIT_CHAR + extracted[PREVIEW_ID_POS]
        } just Runs

        every { SdkAttachmentFactory.wrap(attachments[0]) } returns wrappedAttachment
        every {
            recordService.extractAdditionalAttachmentIds(
                identifiers,
                attachmentId
            )
        } returns extracted

        // When
        recordService.setAttachmentIdForDownloadType(
            attachments,
            identifiers,
            DownloadType.Medium
        )

        // Then
        verify(exactly = 1) {
            wrappedAttachment.id = attachmentId + SPLIT_CHAR + extracted[PREVIEW_ID_POS]
        }
    }

    @Test
    fun `Given, setAttachmentIdForDownloadType is called with a list of Fhir3Attachments, a list of Fhir3Identifier and a DownloadType, it adds THUMBNAIL_ID_POS to the AttachmentId, if DownloadType is SMALL`() {
        // When
        val extracted: List<String> = listOf(
            "tomato",
            "potato",
            "cucumber",
            "soup"
        )
        val attachments: List<Fhir3Attachment> = listOf(
            mockk()
        )
        val attachmentId = "id"
        val wrappedAttachment: WrapperContract.Attachment = mockk()
        val identifiers: List<Fhir3Identifier> = mockk()

        every { wrappedAttachment.id } returns attachmentId
        every {
            wrappedAttachment.id = attachmentId + SPLIT_CHAR + extracted[THUMBNAIL_ID_POS]
        } just Runs

        every { SdkAttachmentFactory.wrap(attachments[0]) } returns wrappedAttachment
        every {
            recordService.extractAdditionalAttachmentIds(
                identifiers,
                attachmentId
            )
        } returns extracted

        // When
        recordService.setAttachmentIdForDownloadType(
            attachments,
            identifiers,
            DownloadType.Small
        )

        // Then
        verify(exactly = 1) {
            wrappedAttachment.id = attachmentId + SPLIT_CHAR + extracted[THUMBNAIL_ID_POS]
        }
    }

    @Test
    fun `Given, setAttachmentIdForDownloadType is called with a list of Fhir4Attachments, a list of Fhir4Identifier and a DownloadType, it does nothing, if the extracted Ids are null`() {
        // When
        val attachments: List<Fhir4Attachment> = listOf(
            mockk()
        )
        val attachmentId = "id"
        val wrappedAttachment: WrapperContract.Attachment = mockk()
        val identifiers: List<Fhir4Identifier> = mockk()

        every { wrappedAttachment.id } returns attachmentId

        every { SdkAttachmentFactory.wrap(attachments[0]) } returns wrappedAttachment
        every {
            recordService.extractAdditionalAttachmentIds(
                identifiers,
                attachmentId
            )
        } returns null

        // When
        recordService.setAttachmentIdForDownloadType(
            attachments,
            identifiers,
            DownloadType.Small
        )

        // Then
        verify(exactly = 0) { wrappedAttachment.id = any() }
    }

    @Test
    fun `Given, setAttachmentIdForDownloadType is called with a list of Fhir4Attachments, a list of Fhir4Identifier and a DownloadType, it does nothing, if DownloadType is FULL`() {
        // When
        val extracted: List<String> = mockk()
        val attachments: List<Fhir4Attachment> = listOf(
            mockk()
        )
        val attachmentId = "id"
        val wrappedAttachment: WrapperContract.Attachment = mockk()
        val identifiers: List<Fhir4Identifier> = mockk()

        every { wrappedAttachment.id } returns attachmentId

        every { SdkAttachmentFactory.wrap(attachments[0]) } returns wrappedAttachment
        every {
            recordService.extractAdditionalAttachmentIds(
                identifiers,
                attachmentId
            )
        } returns extracted

        // When
        recordService.setAttachmentIdForDownloadType(
            attachments,
            identifiers,
            DownloadType.Full
        )

        // Then
        verify(exactly = 0) { wrappedAttachment.id = any() }
    }

    @Test
    fun `Given, setAttachmentIdForDownloadType is called with a list of Fhir4Attachments, a list of Fhir4Identifier and a DownloadType, it adds PREVIEW_ID_POS to the AttachmentId, if DownloadType is MEDIUM`() {
        // When
        val extracted: List<String> = listOf(
            "tomato",
            "potato",
            "cucumber",
            "soup"
        )
        val attachments: List<Fhir4Attachment> = listOf(
            mockk()
        )
        val attachmentId = "id"
        val wrappedAttachment: WrapperContract.Attachment = mockk()
        val identifiers: List<Fhir4Identifier> = mockk()

        every { wrappedAttachment.id } returns attachmentId
        every {
            wrappedAttachment.id = attachmentId + SPLIT_CHAR + extracted[PREVIEW_ID_POS]
        } just Runs

        every { SdkAttachmentFactory.wrap(attachments[0]) } returns wrappedAttachment
        every {
            recordService.extractAdditionalAttachmentIds(
                identifiers,
                attachmentId
            )
        } returns extracted

        // When
        recordService.setAttachmentIdForDownloadType(
            attachments,
            identifiers,
            DownloadType.Medium
        )

        // Then
        verify(exactly = 1) {
            wrappedAttachment.id = attachmentId + SPLIT_CHAR + extracted[PREVIEW_ID_POS]
        }
    }

    @Test
    fun `Given, setAttachmentIdForDownloadType is called with a list of Fhir4Attachments, a list of Fhir4Identifier and a DownloadType, it adds THUMBNAIL_ID_POS to the AttachmentId, if DownloadType is SMALL`() {
        // When
        val extracted: List<String> = listOf(
            "tomato",
            "potato",
            "cucumber",
            "soup"
        )
        val attachments: List<Fhir4Attachment> = listOf(
            mockk()
        )
        val attachmentId = "id"
        val wrappedAttachment: WrapperContract.Attachment = mockk()
        val identifiers: List<Fhir4Identifier> = mockk()

        every { wrappedAttachment.id } returns attachmentId
        every {
            wrappedAttachment.id = attachmentId + SPLIT_CHAR + extracted[THUMBNAIL_ID_POS]
        } just Runs

        every { SdkAttachmentFactory.wrap(attachments[0]) } returns wrappedAttachment
        every {
            recordService.extractAdditionalAttachmentIds(
                identifiers,
                attachmentId
            )
        } returns extracted

        // When
        recordService.setAttachmentIdForDownloadType(
            attachments,
            identifiers,
            DownloadType.Small
        )

        // Then
        verify(exactly = 1) {
            wrappedAttachment.id = attachmentId + SPLIT_CHAR + extracted[THUMBNAIL_ID_POS]
        }
    }
}
