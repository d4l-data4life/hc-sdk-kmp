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

import care.data4life.sdk.attachment.AttachmentContract
import care.data4life.sdk.config.DataRestrictionException
import care.data4life.sdk.crypto.CryptoContract
import care.data4life.sdk.data.DataResource
import care.data4life.sdk.fhir.Fhir3Attachment
import care.data4life.sdk.fhir.Fhir3Resource
import care.data4life.sdk.fhir.Fhir4Attachment
import care.data4life.sdk.fhir.Fhir4Resource
import care.data4life.sdk.fhir.FhirContract
import care.data4life.sdk.network.model.definitions.DecryptedBaseRecord
import care.data4life.sdk.tag.TaggingContract
import care.data4life.sdk.test.util.GenericTestDataProvider.ALIAS
import care.data4life.sdk.test.util.GenericTestDataProvider.ATTACHMENT_ID
import care.data4life.sdk.test.util.GenericTestDataProvider.PARTNER_ID
import care.data4life.sdk.test.util.GenericTestDataProvider.PDF
import care.data4life.sdk.test.util.GenericTestDataProvider.PDF_OVERSIZED
import care.data4life.sdk.test.util.GenericTestDataProvider.UNKNOWN
import care.data4life.sdk.test.util.GenericTestDataProvider.USER_ID
import care.data4life.sdk.test.util.TestResourceHelper.getJSONResource
import care.data4life.sdk.util.Base64
import care.data4life.sdk.wrapper.SdkAttachmentFactory
import care.data4life.sdk.wrapper.SdkFhirAttachmentHelper
import care.data4life.sdk.wrapper.SdkFhirParser
import care.data4life.sdk.wrapper.WrapperContract
import io.mockk.Called
import io.mockk.Runs
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.spyk
import io.mockk.unmockkObject
import io.mockk.verify
import io.reactivex.Single
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class RecordServiceAttachmentUtilsTest {
    private lateinit var recordService: RecordService
    private val apiService: ApiService = mockk()
    private val cryptoService: CryptoContract.Service = mockk()
    private val fhirService: FhirContract.Service = mockk()
    private val tagEncryptionService: TaggingContract.EncryptionService = mockk()
    private val taggingService: TaggingContract.Service = mockk()
    private val attachmentService: AttachmentContract.Service = mockk()
    private val errorHandler: SdkContract.ErrorHandler = mockk()

    @Before
    fun setUp() {
        clearAllMocks()

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
    }

    @After
    fun tearDown() {
        unmockkObject(SdkFhirAttachmentHelper)
        unmockkObject(SdkAttachmentFactory)
    }

    @Test
    fun `Given, deleteAttachment is called with an AttachmentId and a UserId, it delegates the call to the AttachmentService and returns it result`() {
        // Given
        val expected = false

        every { attachmentService.delete(ATTACHMENT_ID, USER_ID) } returns Single.just(expected)

        // When
        val actual = recordService.deleteAttachment(ATTACHMENT_ID, USER_ID).blockingGet()

        assertSame(
            actual = actual,
            expected = expected
        )

        verify(exactly = 1) { attachmentService.delete(ATTACHMENT_ID, USER_ID) }
    }

    @Test
    fun `Given, checkDataRestrictions is called, with a non FhirResource, it ignores it`() {
        // Given
        val resource: DataResource = mockk()

        // When
        recordService.checkDataRestrictions(resource)

        // Then
        assertTrue(true)
    }

    @Test
    fun `Given, checkDataRestrictions is called, with a Fhir3 resource, it does nothing if the resource is capable of Attachments`() {
        // Given
        val resource: Fhir3Resource = mockk()

        every { SdkFhirAttachmentHelper.hasAttachment(resource) } returns false
        every { SdkFhirAttachmentHelper.getAttachment(resource) } returns mockk()

        // When
        recordService.checkDataRestrictions(resource)

        // Then
        assertTrue(true)
        verify { SdkFhirAttachmentHelper.getAttachment(resource)!!.wasNot(Called) }
    }

    @Test
    fun `Given, checkDataRestrictions is called, with a Fhir3 resource, it does nothing if the resource contains no Attachments`() {
        // Given
        val resource: Fhir3Resource = mockk()

        every { SdkFhirAttachmentHelper.hasAttachment(resource) } returns true
        every { SdkFhirAttachmentHelper.getAttachment(resource) } returns null

        // When
        recordService.checkDataRestrictions(resource)

        // Then
        assertTrue(true)
    }

    @Test
    fun `Given, checkDataRestrictions is called, with a Fhir3 resource, it fails, if an attachments MimeType is unknown`() {
        // Given
        mockkObject(Base64)

        val resource: Fhir3Resource = mockk()

        val attachments: MutableList<Fhir3Attachment> = mutableListOf(
            mockk()
        )
        val encodedPayload = "test"
        val decodedPayload = UNKNOWN
        val wrappedAttachment: WrapperContract.Attachment = spyk()

        every { wrappedAttachment.data } returns encodedPayload

        every { SdkFhirAttachmentHelper.hasAttachment(resource) } returns true
        every { SdkFhirAttachmentHelper.getAttachment(resource) } returns attachments as MutableList<Any?>
        every { SdkAttachmentFactory.wrap(attachments[0]) } returns wrappedAttachment
        every { Base64.decode(encodedPayload) } returns decodedPayload

        // Then
        assertFailsWith<DataRestrictionException.UnsupportedFileType> {
            // When
            recordService.checkDataRestrictions(resource)
        }

        unmockkObject(Base64)
    }

    @Test
    fun `Given, checkDataRestrictions is called, with a Fhir3 resource, it fails, if an attachment exceeds the size limitation`() {
        // Given
        mockkObject(Base64)

        val resource: Fhir3Resource = mockk()

        val attachments: MutableList<Fhir3Attachment> = mutableListOf(
            mockk()
        )
        val encodedPayload = "test"
        val decodedPayload = PDF_OVERSIZED
        val wrappedAttachment: WrapperContract.Attachment = spyk()

        every { wrappedAttachment.data } returns encodedPayload

        every { SdkFhirAttachmentHelper.hasAttachment(resource) } returns true
        every { SdkFhirAttachmentHelper.getAttachment(resource) } returns attachments as MutableList<Any?>
        every { SdkAttachmentFactory.wrap(attachments[0]) } returns wrappedAttachment
        every { Base64.decode(encodedPayload) } returns decodedPayload

        // Then
        assertFailsWith<DataRestrictionException.MaxDataSizeViolation> {
            // When
            recordService.checkDataRestrictions(resource)
        }

        unmockkObject(Base64)
    }

    @Test
    fun `Given, checkDataRestrictions is called, with a Fhir3 resource, it does nothing, if an attachment is valid`() {
        // Given
        mockkObject(Base64)

        val resource: Fhir3Resource = mockk()

        val attachments: MutableList<Fhir3Attachment> = mutableListOf(
            mockk()
        )
        val encodedPayload = "test"
        val decodedPayload = PDF
        val wrappedAttachment: WrapperContract.Attachment = spyk()

        every { wrappedAttachment.data } returns encodedPayload

        every { SdkFhirAttachmentHelper.hasAttachment(resource) } returns true
        every { SdkFhirAttachmentHelper.getAttachment(resource) } returns attachments as MutableList<Any?>
        every { SdkAttachmentFactory.wrap(attachments[0]) } returns wrappedAttachment
        every { Base64.decode(encodedPayload) } returns decodedPayload

        // When
        recordService.checkDataRestrictions(resource)

        // Then
        assertTrue(true)

        unmockkObject(Base64)
    }

    @Test
    fun `Given, checkDataRestrictions is called, with a Fhir3 resource, it does nothing if the resource contains null as Attachments`() {
        // Given
        mockkObject(Base64)

        val resource: Fhir3Resource = mockk()

        val attachments: MutableList<Fhir3Attachment?> = mutableListOf(
            null,
            mockk()
        )
        val encodedPayload = "test"
        val decodedPayload = PDF
        val wrappedAttachment: WrapperContract.Attachment = spyk()

        every { wrappedAttachment.data } returns encodedPayload

        every { SdkFhirAttachmentHelper.hasAttachment(resource) } returns true
        every { SdkFhirAttachmentHelper.getAttachment(resource) } returns attachments as MutableList<Any?>
        every { SdkAttachmentFactory.wrap(attachments[1]!!) } returns wrappedAttachment
        every { Base64.decode(encodedPayload) } returns decodedPayload

        // When
        recordService.checkDataRestrictions(resource)

        // Then
        assertTrue(true)

        verify(exactly = 1) { Base64.decode(any<String>()) }

        unmockkObject(Base64)
    }

    @Test
    fun `Given, checkDataRestrictions is called, with a Fhir3 resource, it does nothing if the resource has no data`() {
        // Given
        mockkObject(Base64)

        val resource: Fhir3Resource = mockk()

        val attachments: MutableList<Fhir3Attachment> = mutableListOf(
            mockk(),
            mockk()
        )
        val encodedPayload = "test"
        val decodedPayload = PDF
        val wrappedAttachmentWithoutData: WrapperContract.Attachment = spyk()
        val wrappedAttachment: WrapperContract.Attachment = spyk()

        every { wrappedAttachment.data } returns encodedPayload

        every { wrappedAttachmentWithoutData.data } returns null

        every { SdkFhirAttachmentHelper.hasAttachment(resource) } returns true
        every { SdkFhirAttachmentHelper.getAttachment(resource) } returns attachments as MutableList<Any?>
        every { SdkAttachmentFactory.wrap(attachments[0]) } returns wrappedAttachmentWithoutData
        every { SdkAttachmentFactory.wrap(attachments[1]) } returns wrappedAttachment
        every { Base64.decode(encodedPayload) } returns decodedPayload

        // When
        recordService.checkDataRestrictions(resource)

        // Then
        assertTrue(true)

        verify(exactly = 1) { Base64.decode(any<String>()) }

        unmockkObject(Base64)
    }

    @Test
    fun `Given, checkDataRestrictions is called, with a Fhir4 resource, it does nothing if the resource is capable of Attachments`() {
        // Given
        val resource: Fhir4Resource = mockk()

        every { SdkFhirAttachmentHelper.hasAttachment(resource) } returns false
        every { SdkFhirAttachmentHelper.getAttachment(resource) } returns mockk()

        // When
        recordService.checkDataRestrictions(resource)

        // Then
        assertTrue(true)
        verify { SdkFhirAttachmentHelper.getAttachment(resource)!!.wasNot(Called) }
    }

    @Test
    fun `Given, checkDataRestrictions is called, with a Fhir4 resource, it does nothing if the resource contains no Attachments`() {
        // Given
        val resource: Fhir4Resource = mockk()

        every { SdkFhirAttachmentHelper.hasAttachment(resource) } returns true
        every { SdkFhirAttachmentHelper.getAttachment(resource) } returns null

        // When
        recordService.checkDataRestrictions(resource)

        // Then
        assertTrue(true)
    }

    @Test
    fun `Given, checkDataRestrictions is called, with a Fhir4 resource, it fails, if an attachments MimeType is unknown`() {
        // Given
        mockkObject(Base64)

        val resource: Fhir4Resource = mockk()

        val attachments: MutableList<Fhir4Attachment> = mutableListOf(
            mockk()
        )
        val encodedPayload = "test"
        val decodedPayload = UNKNOWN
        val wrappedAttachment: WrapperContract.Attachment = spyk()

        every { wrappedAttachment.data } returns encodedPayload

        every { SdkFhirAttachmentHelper.hasAttachment(resource) } returns true
        every { SdkFhirAttachmentHelper.getAttachment(resource) } returns attachments as MutableList<Any?>
        every { SdkAttachmentFactory.wrap(attachments[0]) } returns wrappedAttachment
        every { Base64.decode(encodedPayload) } returns decodedPayload

        // Then
        assertFailsWith<DataRestrictionException.UnsupportedFileType> {
            // When
            recordService.checkDataRestrictions(resource)
        }

        unmockkObject(Base64)
    }

    @Test
    fun `Given, checkDataRestrictions is called, with a Fhir4 resource, it fails, if an attachment exceeds the size limitation`() {
        // Given
        mockkObject(Base64)

        val resource: Fhir4Resource = mockk()

        val attachments: MutableList<Fhir4Attachment> = mutableListOf(
            mockk()
        )
        val encodedPayload = "test"
        val decodedPayload = PDF_OVERSIZED
        val wrappedAttachment: WrapperContract.Attachment = spyk()

        every { wrappedAttachment.data } returns encodedPayload

        every { SdkFhirAttachmentHelper.hasAttachment(resource) } returns true
        every { SdkFhirAttachmentHelper.getAttachment(resource) } returns attachments as MutableList<Any?>
        every { SdkAttachmentFactory.wrap(attachments[0]) } returns wrappedAttachment
        every { Base64.decode(encodedPayload) } returns decodedPayload

        // Then
        assertFailsWith<DataRestrictionException.MaxDataSizeViolation> {
            // When
            recordService.checkDataRestrictions(resource)
        }

        unmockkObject(Base64)
    }

    @Test
    fun `Given, checkDataRestrictions is called, with a Fhir4 resource, it does nothing, if an attachment is valid`() {
        // Given
        mockkObject(Base64)

        val resource: Fhir4Resource = mockk()

        val attachments: MutableList<Fhir4Attachment> = mutableListOf(
            mockk()
        )
        val encodedPayload = "test"
        val decodedPayload = PDF
        val wrappedAttachment: WrapperContract.Attachment = spyk()

        every { wrappedAttachment.data } returns encodedPayload

        every { SdkFhirAttachmentHelper.hasAttachment(resource) } returns true
        every { SdkFhirAttachmentHelper.getAttachment(resource) } returns attachments as MutableList<Any?>
        every { SdkAttachmentFactory.wrap(attachments[0]) } returns wrappedAttachment
        every { Base64.decode(encodedPayload) } returns decodedPayload

        // When
        recordService.checkDataRestrictions(resource)

        // Then
        assertTrue(true)

        unmockkObject(Base64)
    }

    @Test
    fun `Given, checkDataRestrictions is called, with a Fhir4 resource, it does nothing if the resource contains null as Attachments`() {
        // Given
        mockkObject(Base64)

        val resource: Fhir4Resource = mockk()

        val attachments: MutableList<Fhir4Attachment?> = mutableListOf(
            null,
            mockk()
        )
        val encodedPayload = "test"
        val decodedPayload = PDF
        val wrappedAttachment: WrapperContract.Attachment = spyk()

        every { wrappedAttachment.data } returns encodedPayload

        every { SdkFhirAttachmentHelper.hasAttachment(resource) } returns true
        every { SdkFhirAttachmentHelper.getAttachment(resource) } returns attachments as MutableList<Any?>
        every { SdkAttachmentFactory.wrap(attachments[1]!!) } returns wrappedAttachment
        every { Base64.decode(encodedPayload) } returns decodedPayload

        // When
        recordService.checkDataRestrictions(resource)

        // Then
        assertTrue(true)

        verify(exactly = 1) { Base64.decode(any<String>()) }

        unmockkObject(Base64)
    }

    @Test
    fun `Given, checkDataRestrictions is called, with a Fhir4 resource, it does nothing if the resource has no data`() {
        // Given
        mockkObject(Base64)

        val resource: Fhir4Resource = mockk()

        val attachments: MutableList<Fhir4Attachment> = mutableListOf(
            mockk(),
            mockk()
        )
        val encodedPayload = "test"
        val decodedPayload = PDF
        val wrappedAttachmentWithoutData: WrapperContract.Attachment = spyk()
        val wrappedAttachment: WrapperContract.Attachment = spyk()

        every { wrappedAttachment.data } returns encodedPayload

        every { wrappedAttachmentWithoutData.data } returns null

        every { SdkFhirAttachmentHelper.hasAttachment(resource) } returns true
        every { SdkFhirAttachmentHelper.getAttachment(resource) } returns attachments as MutableList<Any?>
        every { SdkAttachmentFactory.wrap(attachments[0]) } returns wrappedAttachmentWithoutData
        every { SdkAttachmentFactory.wrap(attachments[1]) } returns wrappedAttachment
        every { Base64.decode(encodedPayload) } returns decodedPayload

        // When
        recordService.checkDataRestrictions(resource)

        // Then
        assertTrue(true)

        verify(exactly = 1) { Base64.decode(any<String>()) }

        unmockkObject(Base64)
    }

    @Test
    fun `Given, checkDataRestrictions is called, with a Resource, which has non extractable Attachments, it returns without a failure`() {
        // Given
        val resourceStr = getJSONResource(
            "fhir4",
            "s4h-patient-example.patient"
        )

        val doc = SdkFhirParser.toFhir4("Patient", resourceStr)

        // When
        recordService.checkDataRestrictions(doc)

        // Then
        assertTrue(true)
    }

    @Test
    fun `Given, extractUploadData is called with a non FhirResource, it returns null`() {
        // When
        val data = recordService.extractUploadData("something")
        // Then
        assertNull(data)
    }

    @Test
    fun `Given, extractUploadData is called with a Fhir3 resource, it returns null, if it is not capable of Attachments`() {
        // Given
        val resource: Fhir3Resource = mockk()

        every { SdkFhirAttachmentHelper.hasAttachment(resource) } returns false
        every { SdkFhirAttachmentHelper.getAttachment(resource) } returns mockk()

        // When
        val data = recordService.extractUploadData(resource)

        // Then
        assertNull(data)
        verify { SdkFhirAttachmentHelper.getAttachment(resource)!!.wasNot(Called) }
    }

    @Test
    fun `Given, extractUploadData is called with a Fhir3 resource, it returns null, if it has no Attachments`() {
        // Given
        val resource: Fhir3Resource = mockk()

        every { SdkFhirAttachmentHelper.hasAttachment(resource) } returns true
        every { SdkFhirAttachmentHelper.getAttachment(resource) } returns null

        // When
        val data = recordService.extractUploadData(resource)

        // Then
        assertNull(data)
    }

    @Test
    fun `Given, extractUploadData is called with a Fhir3 resource, it returns a map of the extracted Attachments`() {
        // Given
        val resource: Fhir3Resource = mockk()
        val attachments: MutableList<Any> = mutableListOf(
            mockk()
        )
        val payload = "data"

        val wrappedAttachment: WrapperContract.Attachment = spyk()

        every { wrappedAttachment.data } returns payload

        every { SdkFhirAttachmentHelper.hasAttachment(resource) } returns true
        every { SdkFhirAttachmentHelper.getAttachment(resource) } returns attachments as MutableList<Any?>

        every { SdkAttachmentFactory.wrap(attachments[0]) } returns wrappedAttachment

        // When
        val data = recordService.extractUploadData(resource) as HashMap<Any, String>

        // Then
        assertEquals(
            actual = data,
            expected = hashMapOf(attachments[0] to payload)
        )
    }

    @Test
    fun `Given, extractUploadData is called with a Fhir3 resource, it returns null, if the Attachments only contains null`() {
        // Given
        val resource: Fhir3Resource = mockk()
        val attachments: MutableList<Fhir3Attachment?> = mutableListOf(
            null,
            mockk()
        )
        val payload = "data"

        val wrappedAttachment: WrapperContract.Attachment = spyk()

        every { wrappedAttachment.data } returns payload

        every { SdkFhirAttachmentHelper.hasAttachment(resource) } returns true
        every { SdkFhirAttachmentHelper.getAttachment(resource) } returns attachments as MutableList<Any?>

        every { SdkAttachmentFactory.wrap(attachments[1]!!) } returns wrappedAttachment

        // When
        val data = recordService.extractUploadData(resource)

        // Then
        assertEquals(
            actual = data,
            expected = hashMapOf(attachments[1] to payload) as HashMap<Any, String?>
        )
    }

    @Test
    fun `Given, extractUploadData is called with a Fhir3 resource, it returns null, if the Attachments have no actual data`() {
        // Given
        val resource: Fhir3Resource = mockk()
        val attachments: MutableList<Fhir3Attachment> = mutableListOf(
            mockk(),
            mockk()
        )
        val payload = "data"

        val wrappedAttachment: WrapperContract.Attachment = spyk()
        val wrapperAttachmentWithoutData: WrapperContract.Attachment = spyk()

        every { wrappedAttachment.data } returns payload
        every { wrapperAttachmentWithoutData.data } returns null

        every { SdkFhirAttachmentHelper.hasAttachment(resource) } returns true
        every { SdkFhirAttachmentHelper.getAttachment(resource) } returns attachments as MutableList<Any?>

        every { SdkAttachmentFactory.wrap(attachments[0]) } returns wrapperAttachmentWithoutData
        every { SdkAttachmentFactory.wrap(attachments[1]) } returns wrappedAttachment

        // When
        val data = recordService.extractUploadData(resource)

        // Then
        assertEquals(
            actual = data,
            expected = hashMapOf(attachments[1] to payload) as HashMap<Any, String?>
        )
    }

    @Test
    fun `Given, extractUploadData is called with a Fhir4 resource, it returns null, if it is not capable of Attachments`() {
        // Given
        val resource: Fhir4Resource = mockk()

        every { SdkFhirAttachmentHelper.hasAttachment(resource) } returns false
        every { SdkFhirAttachmentHelper.getAttachment(resource) } returns mockk()

        // When
        val data = recordService.extractUploadData(resource)

        // Then
        assertNull(data)
        verify { SdkFhirAttachmentHelper.getAttachment(resource)!!.wasNot(Called) }
    }

    @Test
    fun `Given, extractUploadData is called with a Fhir4 resource, it returns null, if it has no Attachments`() {
        // Given
        val resource: Fhir4Resource = mockk()

        every { SdkFhirAttachmentHelper.hasAttachment(resource) } returns true
        every { SdkFhirAttachmentHelper.getAttachment(resource) } returns null

        // When
        val data = recordService.extractUploadData(resource)

        // Then
        assertNull(data)
    }

    @Test
    fun `Given, extractUploadData is called with a Fhir4 resource, it returns a map of the extracted Attachments`() {
        // Given
        val resource: Fhir4Resource = mockk()
        val attachments: MutableList<Any> = mutableListOf(
            mockk()
        )
        val payload = "data"

        val wrappedAttachment: WrapperContract.Attachment = spyk()

        every { wrappedAttachment.data } returns payload

        every { SdkFhirAttachmentHelper.hasAttachment(resource) } returns true
        every { SdkFhirAttachmentHelper.getAttachment(resource) } returns attachments as MutableList<Any?>

        every { SdkAttachmentFactory.wrap(attachments[0]) } returns wrappedAttachment

        // When
        val data = recordService.extractUploadData(resource) as HashMap<Any, String>

        // Then
        assertEquals(
            actual = data,
            expected = hashMapOf(attachments[0] to payload)
        )
    }

    @Test
    fun `Given, extractUploadData is called with a Fhir4 resource, it returns null, if the Attachments only contains null`() {
        // Given
        val resource: Fhir4Resource = mockk()
        val attachments: MutableList<Fhir4Attachment?> = mutableListOf(
            null,
            mockk()
        )
        val payload = "data"

        val wrappedAttachment: WrapperContract.Attachment = spyk()

        every { wrappedAttachment.data } returns payload

        every { SdkFhirAttachmentHelper.hasAttachment(resource) } returns true
        every { SdkFhirAttachmentHelper.getAttachment(resource) } returns attachments as MutableList<Any?>

        every { SdkAttachmentFactory.wrap(attachments[1]!!) } returns wrappedAttachment

        // When
        val data = recordService.extractUploadData(resource)

        // Then
        assertEquals(
            actual = data,
            expected = hashMapOf(attachments[1] to payload) as HashMap<Any, String?>
        )
    }

    @Test
    fun `Given, extractUploadData is called with a Fhir4 resource, it returns null, if the Attachments have no actual data`() {
        // Given
        val resource: Fhir4Resource = mockk()
        val attachments: MutableList<Fhir4Attachment> = mutableListOf(
            mockk(),
            mockk()
        )
        val payload = "data"

        val wrappedAttachment: WrapperContract.Attachment = spyk()
        val wrapperAttachmentWithoutData: WrapperContract.Attachment = spyk()

        every { wrappedAttachment.data } returns payload
        every { wrapperAttachmentWithoutData.data } returns null

        every { SdkFhirAttachmentHelper.hasAttachment(resource) } returns true
        every { SdkFhirAttachmentHelper.getAttachment(resource) } returns attachments as MutableList<Any?>

        every { SdkAttachmentFactory.wrap(attachments[0]) } returns wrapperAttachmentWithoutData
        every { SdkAttachmentFactory.wrap(attachments[1]) } returns wrappedAttachment

        // When
        val data = recordService.extractUploadData(resource)

        // Then
        assertEquals(
            actual = data,
            expected = hashMapOf(attachments[1] to payload) as HashMap<Any, String?>
        )
    }

    @Test
    fun `Given, removeUploadData is called, with a DecryptedRecord, which contains a non Fhir resource, it ignores it`() {
        // Given
        val resource: DataResource = mockk()
        val decryptedRecord: DecryptedBaseRecord<DataResource> = mockk()

        every { decryptedRecord.resource } returns resource

        // When
        val record = recordService.removeUploadData(decryptedRecord)

        // Then
        assertSame(
            actual = record,
            expected = decryptedRecord
        )

        verify(exactly = 0) { SdkFhirAttachmentHelper.updateAttachmentData(any(), any()) }
    }

    @Test
    fun `Given, removeUploadData is called, with a DecryptedRecord, which contains a Fhir3 resource, it ignores it, if it is not capable of Attachments`() {
        // Given
        val resource: Fhir3Resource = mockk()
        val decryptedRecord: DecryptedBaseRecord<Fhir3Resource> = mockk()

        every { decryptedRecord.resource } returns resource

        every { SdkFhirAttachmentHelper.hasAttachment(resource) } returns false
        every { SdkFhirAttachmentHelper.getAttachment(resource) } returns mockk()

        // When
        val record = recordService.removeUploadData(decryptedRecord)

        // Then
        assertSame(
            actual = record,
            expected = decryptedRecord
        )

        verify(exactly = 0) { SdkFhirAttachmentHelper.updateAttachmentData(any(), any()) }
        verify { SdkFhirAttachmentHelper.getAttachment(resource)!!.wasNot(Called) }
    }

    @Test
    fun `Given, removeUploadData is called, with a DecryptedRecord, which contains a Fhir3 resource, it ignores it, if it does not contain Attachments`() {
        // Given
        val resource: Fhir3Resource = mockk()
        val decryptedRecord: DecryptedBaseRecord<Fhir3Resource> = mockk()

        every { decryptedRecord.resource } returns resource

        every { SdkFhirAttachmentHelper.hasAttachment(resource) } returns true
        every { SdkFhirAttachmentHelper.getAttachment(resource) } returns null

        // When
        val record = recordService.removeUploadData(decryptedRecord)

        // Then
        assertSame(
            actual = record,
            expected = decryptedRecord
        )

        verify(exactly = 0) { SdkFhirAttachmentHelper.updateAttachmentData(any(), any()) }
    }

    @Test
    fun `Given, removeUploadData is called, with a DecryptedRecord, which contains a Fhir3 resource, it ignores it, if it does not contain actual Attachments`() {
        // Given
        val resource: Fhir3Resource = mockk()
        val decryptedRecord: DecryptedBaseRecord<Fhir3Resource> = mockk()

        every { decryptedRecord.resource } returns resource

        every { SdkFhirAttachmentHelper.hasAttachment(resource) } returns true
        every { SdkFhirAttachmentHelper.getAttachment(resource) } returns mutableListOf()

        // When
        val record = recordService.removeUploadData(decryptedRecord)

        // Then
        assertSame(
            actual = record,
            expected = decryptedRecord
        )

        verify(exactly = 0) { SdkFhirAttachmentHelper.updateAttachmentData(any(), any()) }
    }

    @Test
    fun `Given, removeUploadData is called, with a DecryptedRecord, which contains a Fhir3 resource, it removes the Attachments`() {
        // Given
        val resource: Fhir3Resource = mockk()
        val decryptedRecord: DecryptedBaseRecord<Fhir3Resource> = mockk()
        val attachments: MutableList<Fhir3Attachment> = mutableListOf(
            mockk()
        )

        every { decryptedRecord.resource } returns resource

        every { SdkFhirAttachmentHelper.hasAttachment(resource) } returns true
        every { SdkFhirAttachmentHelper.getAttachment(resource) } returns attachments as MutableList<Any?>
        every { SdkFhirAttachmentHelper.updateAttachmentData(resource, null) } just Runs

        // When
        val record = recordService.removeUploadData(decryptedRecord)

        // Then
        assertSame(
            actual = record,
            expected = decryptedRecord
        )

        verify(exactly = 1) { SdkFhirAttachmentHelper.updateAttachmentData(resource, null) }
    }

    @Test
    fun `Given, removeUploadData is called, with a DecryptedRecord, which contains a Fhir4 resource, it ignores it, if it is not capable of Attachments`() {
        // Given
        val resource: Fhir4Resource = mockk()
        val decryptedRecord: DecryptedBaseRecord<Fhir4Resource> = mockk()

        every { decryptedRecord.resource } returns resource

        every { SdkFhirAttachmentHelper.hasAttachment(resource) } returns false
        every { SdkFhirAttachmentHelper.getAttachment(resource) } returns mockk()

        // When
        val record = recordService.removeUploadData(decryptedRecord)

        // Then
        assertSame(
            actual = record,
            expected = decryptedRecord
        )

        verify(exactly = 0) { SdkFhirAttachmentHelper.updateAttachmentData(any(), any()) }
        verify { SdkFhirAttachmentHelper.getAttachment(resource)!!.wasNot(Called) }
    }

    @Test
    fun `Given, removeUploadData is called, with a DecryptedRecord, which contains a Fhir4 resource, it ignores it, if it does not contain Attachments`() {
        // Given
        val resource: Fhir4Resource = mockk()
        val decryptedRecord: DecryptedBaseRecord<Fhir4Resource> = mockk()

        every { decryptedRecord.resource } returns resource

        every { SdkFhirAttachmentHelper.hasAttachment(resource) } returns true
        every { SdkFhirAttachmentHelper.getAttachment(resource) } returns null

        // When
        val record = recordService.removeUploadData(decryptedRecord)

        // Then
        assertSame(
            actual = record,
            expected = decryptedRecord
        )

        verify(exactly = 0) { SdkFhirAttachmentHelper.updateAttachmentData(any(), any()) }
    }

    @Test
    fun `Given, removeUploadData is called, with a DecryptedRecord, which contains a Fhir4 resource, it ignores it, if it does not contain actual Attachments`() {
        // Given
        val resource: Fhir4Resource = mockk()
        val decryptedRecord: DecryptedBaseRecord<Fhir4Resource> = mockk()

        every { decryptedRecord.resource } returns resource

        every { SdkFhirAttachmentHelper.hasAttachment(resource) } returns true
        every { SdkFhirAttachmentHelper.getAttachment(resource) } returns mutableListOf()

        // When
        val record = recordService.removeUploadData(decryptedRecord)

        // Then
        assertSame(
            actual = record,
            expected = decryptedRecord
        )

        verify(exactly = 0) { SdkFhirAttachmentHelper.updateAttachmentData(any(), any()) }
    }

    @Test
    fun `Given, removeUploadData is called, with a DecryptedRecord, which contains a Fhir4 resource, it removes the Attachments`() {
        // Given
        val resource: Fhir4Resource = mockk()
        val decryptedRecord: DecryptedBaseRecord<Fhir4Resource> = mockk()
        val attachments: MutableList<Fhir4Attachment> = mutableListOf(
            mockk()
        )

        every { decryptedRecord.resource } returns resource

        every { SdkFhirAttachmentHelper.hasAttachment(resource) } returns true
        every { SdkFhirAttachmentHelper.getAttachment(resource) } returns attachments as MutableList<Any?>
        every { SdkFhirAttachmentHelper.updateAttachmentData(resource, null) } just Runs

        // When
        val record = recordService.removeUploadData(decryptedRecord)

        // Then
        assertSame(
            actual = record,
            expected = decryptedRecord
        )

        verify(exactly = 1) { SdkFhirAttachmentHelper.updateAttachmentData(resource, null) }
    }

    @Test
    fun `Given, restoreUploadData is called, with a DecryptedRecord, which contains no Fhir resource, a resource and Attachmentdata, it reflects the record`() {
        // Given
        val resource: DataResource = mockk()
        val originalResource: DataResource = mockk()
        val attachmentPayload: HashMap<Any, String?> = mockk()
        val decryptedRecord: DecryptedBaseRecord<DataResource> = spyk()

        every { decryptedRecord.resource } returns resource

        // When
        val record = recordService.restoreUploadData(
            decryptedRecord,
            originalResource,
            attachmentPayload
        )

        // Then
        assertSame(
            actual = record,
            expected = decryptedRecord
        )

        verify(exactly = 0) { SdkFhirAttachmentHelper.updateAttachmentData(any(), any()) }
    }

    @Test
    fun `Given, restoreUploadData is called, with a DecryptedRecord, which contains a Fhir3 resource, a resource, which is not Fhir and Attachmentdata, it reflects the record`() {
        // Given
        val resource: Fhir3Resource = spyk()
        val originalResource: DataResource = mockk()
        val attachmentPayload: HashMap<Any, String?> = mockk()
        val decryptedRecord: DecryptedBaseRecord<Any> = spyk()

        every { decryptedRecord.resource } returns resource

        every { SdkFhirAttachmentHelper.hasAttachment(originalResource) }

        // When
        val record = recordService.restoreUploadData(
            decryptedRecord,
            originalResource as Any,
            attachmentPayload
        )

        // Then
        assertSame(
            actual = record,
            expected = decryptedRecord
        )

        verify(exactly = 0) { SdkFhirAttachmentHelper.updateAttachmentData(any(), any()) }
    }

    @Test
    fun `Given, restoreUploadData is called, with a DecryptedRecord, which contains a Fhir3 resource, a resource and Attachmentdata, it reflects the record, if the original resource cannot contain Attachments`() {
        // Given
        val resource: Fhir3Resource = spyk()
        val originalResource: Fhir3Resource = mockk()
        val attachmentPayload: HashMap<Any, String?> = mockk()
        val decryptedRecord: DecryptedBaseRecord<Fhir3Resource> = spyk()

        every { decryptedRecord.resource } returns resource
        every { decryptedRecord.resource = originalResource } just Runs

        every { SdkFhirAttachmentHelper.hasAttachment(originalResource) } returns false

        // When
        val record = recordService.restoreUploadData(
            decryptedRecord,
            originalResource,
            attachmentPayload
        )

        // Then
        assertSame(
            actual = record,
            expected = decryptedRecord
        )

        verify(exactly = 1) { decryptedRecord.resource = originalResource }
        verify(exactly = 0) { SdkFhirAttachmentHelper.updateAttachmentData(any(), any()) }
    }

    @Test
    fun `Given, restoreUploadData is called, with a DecryptedRecord, which contains a Fhir3 resource, a resource and Attachmentdata, it reflects the record, if the Attachments are null`() {
        // Given
        val resource: Fhir3Resource = spyk()
        val originalResource: Fhir3Resource = mockk()
        val decryptedRecord: DecryptedBaseRecord<Fhir3Resource> = spyk()

        every { decryptedRecord.resource } returnsMany listOf(resource, originalResource)
        every { decryptedRecord.resource = originalResource } just Runs

        every { SdkFhirAttachmentHelper.hasAttachment(originalResource) } returns false

        // When
        val record = recordService.restoreUploadData(
            decryptedRecord,
            originalResource,
            null
        )

        // Then
        assertSame(
            actual = record,
            expected = decryptedRecord
        )

        verify(exactly = 1) { decryptedRecord.resource = originalResource }
        verify(exactly = 0) { SdkFhirAttachmentHelper.updateAttachmentData(any(), any()) }
    }

    @Test
    fun `Given, restoreUploadData is called, with a DecryptedRecord, which contains a Fhir3 resource, a resource and Attachmentdata, it reflects the record, if the original resource does not contain actual Attachments`() {
        // Given
        val resource: Fhir3Resource = spyk()
        val originalResource: Fhir3Resource = spyk()
        val attachmentPayload: HashMap<Any, String?> = mockk()
        val decryptedRecord: DecryptedBaseRecord<Fhir3Resource> = mockk()

        every { decryptedRecord.resource } returnsMany listOf(resource, originalResource)
        every { decryptedRecord.resource = originalResource } just Runs

        every { SdkFhirAttachmentHelper.hasAttachment(originalResource) } returns true
        every { SdkFhirAttachmentHelper.getAttachment(originalResource) } returns mutableListOf()

        // When
        val record = recordService.restoreUploadData(
            decryptedRecord,
            originalResource,
            attachmentPayload
        )

        // Then
        assertSame(
            actual = record,
            expected = decryptedRecord
        )

        verify(exactly = 1) { decryptedRecord.resource = originalResource }
        verify(exactly = 0) { SdkFhirAttachmentHelper.updateAttachmentData(any(), any()) }
    }

    @Test
    fun `Given, restoreUploadData is called, with a DecryptedRecord, which contains a Fhir3 resource, a resource and Attachmentdata, it restors Attachmentsdata`() {
        // Given
        val resource: Fhir3Resource = spyk()
        val originalResource: Fhir3Resource = spyk()
        val attachmentPayload: HashMap<Any, String?> = mockk()
        val decryptedRecord: DecryptedBaseRecord<Fhir3Resource> = mockk()

        every { decryptedRecord.resource } returnsMany listOf(resource, originalResource)
        every { decryptedRecord.resource = originalResource } just Runs

        every { SdkFhirAttachmentHelper.hasAttachment(originalResource) } returns true
        every { SdkFhirAttachmentHelper.getAttachment(originalResource) } returns mutableListOf(
            mockk()
        )

        // When
        val record = recordService.restoreUploadData(
            decryptedRecord,
            originalResource,
            attachmentPayload
        )

        // Then
        assertSame(
            actual = record,
            expected = decryptedRecord
        )

        verify(exactly = 1) { decryptedRecord.resource = originalResource }
        verify(exactly = 1) {
            SdkFhirAttachmentHelper.updateAttachmentData(
                originalResource,
                attachmentPayload
            )
        }
    }

    @Test
    fun `Given, restoreUploadData is called, with a DecryptedRecord, which contains a Fhir4 resource, a resource and Attachmentdata, it reflects the record, if the original resource cannot contain Attachments`() {
        // Given
        val resource: Fhir4Resource = spyk()
        val originalResource: Fhir4Resource = mockk()
        val attachmentPayload: HashMap<Any, String?> = mockk()
        val decryptedRecord: DecryptedBaseRecord<Fhir4Resource> = spyk()

        every { decryptedRecord.resource } returns resource
        every { decryptedRecord.resource = originalResource } just Runs

        every { SdkFhirAttachmentHelper.hasAttachment(originalResource) } returns false

        // When
        val record = recordService.restoreUploadData(
            decryptedRecord,
            originalResource,
            attachmentPayload
        )

        // Then
        assertSame(
            actual = record,
            expected = decryptedRecord
        )

        verify(exactly = 1) { decryptedRecord.resource = originalResource }
        verify(exactly = 0) { SdkFhirAttachmentHelper.updateAttachmentData(any(), any()) }
    }

    @Test
    fun `Given, restoreUploadData is called, with a DecryptedRecord, which contains a Fhir4 resource, a resource and Attachmentdata, it reflects the record, if the Attachments are null`() {
        // Given
        val resource: Fhir4Resource = spyk()
        val originalResource: Fhir4Resource = mockk()
        val decryptedRecord: DecryptedBaseRecord<Fhir4Resource> = spyk()

        every { decryptedRecord.resource } returnsMany listOf(resource, originalResource)
        every { decryptedRecord.resource = originalResource } just Runs

        every { SdkFhirAttachmentHelper.hasAttachment(originalResource) } returns false

        // When
        val record = recordService.restoreUploadData(
            decryptedRecord,
            originalResource,
            null
        )

        // Then
        assertSame(
            actual = record,
            expected = decryptedRecord
        )

        verify(exactly = 1) { decryptedRecord.resource = originalResource }
        verify(exactly = 0) { SdkFhirAttachmentHelper.updateAttachmentData(any(), any()) }
    }

    @Test
    fun `Given, restoreUploadData is called, with a DecryptedRecord, which contains a Fhir4 resource, a resource and Attachmentdata, it reflects the record, if the original resource does not contain actual Attachments`() {
        // Given
        val resource: Fhir4Resource = spyk()
        val originalResource: Fhir4Resource = spyk()
        val attachmentPayload: HashMap<Any, String?> = mockk()
        val decryptedRecord: DecryptedBaseRecord<Fhir4Resource> = mockk()

        every { decryptedRecord.resource } returnsMany listOf(resource, originalResource)
        every { decryptedRecord.resource = originalResource } just Runs

        every { SdkFhirAttachmentHelper.hasAttachment(originalResource) } returns true
        every { SdkFhirAttachmentHelper.getAttachment(originalResource) } returns mutableListOf()

        // When
        val record = recordService.restoreUploadData(
            decryptedRecord,
            originalResource,
            attachmentPayload
        )

        // Then
        assertSame(
            actual = record,
            expected = decryptedRecord
        )

        verify(exactly = 1) { decryptedRecord.resource = originalResource }
        verify(exactly = 0) { SdkFhirAttachmentHelper.updateAttachmentData(any(), any()) }
    }

    @Test
    fun `Given, restoreUploadData is called, with a DecryptedRecord, which contains a Fhir4 resource, a resource and Attachmentdata, it restors Attachmentsdata`() {
        // Given
        val resource: Fhir4Resource = spyk()
        val originalResource: Fhir4Resource = spyk()
        val attachmentPayload: HashMap<Any, String?> = mockk()
        val decryptedRecord: DecryptedBaseRecord<Fhir4Resource> = mockk()

        every { decryptedRecord.resource } returnsMany listOf(resource, originalResource)
        every { decryptedRecord.resource = originalResource } just Runs

        every { SdkFhirAttachmentHelper.hasAttachment(originalResource) } returns true
        every { SdkFhirAttachmentHelper.getAttachment(originalResource) } returns mutableListOf(
            mockk()
        )

        // When
        val record = recordService.restoreUploadData(
            decryptedRecord,
            originalResource,
            attachmentPayload
        )

        // Then
        assertSame(
            actual = record,
            expected = decryptedRecord
        )

        verify(exactly = 1) { decryptedRecord.resource = originalResource }
        verify(exactly = 1) {
            SdkFhirAttachmentHelper.updateAttachmentData(
                originalResource,
                attachmentPayload
            )
        }
    }
}
