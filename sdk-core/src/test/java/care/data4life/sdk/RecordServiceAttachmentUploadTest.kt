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
import care.data4life.sdk.attachment.AttachmentContract
import care.data4life.sdk.attachment.AttachmentGuardian
import care.data4life.sdk.crypto.CryptoContract
import care.data4life.sdk.data.DataResource
import care.data4life.sdk.fhir.Fhir3Attachment
import care.data4life.sdk.fhir.Fhir3Resource
import care.data4life.sdk.fhir.Fhir4Attachment
import care.data4life.sdk.fhir.Fhir4Resource
import care.data4life.sdk.fhir.FhirContract
import care.data4life.sdk.network.NetworkingContract
import care.data4life.sdk.network.model.NetworkModelContract.DecryptedBaseRecord
import care.data4life.sdk.tag.TaggingContract
import care.data4life.sdk.test.util.GenericTestDataProvider.ALIAS
import care.data4life.sdk.test.util.GenericTestDataProvider.PARTNER_ID
import care.data4life.sdk.test.util.GenericTestDataProvider.USER_ID
import care.data4life.sdk.wrapper.SdkAttachmentFactory
import care.data4life.sdk.wrapper.SdkFhirAttachmentHelper
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
import io.mockk.verifyOrder
import io.reactivex.Single
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertSame

class RecordServiceAttachmentUploadTest {
    private lateinit var recordService: RecordService
    private val apiService: NetworkingContract.Service = mockk()
    private val cryptoService: CryptoContract.Service = mockk()
    private val resourceCryptoService: FhirContract.CryptoService = mockk()
    private val tagCryptoService: TaggingContract.CryptoService = mockk()
    private val taggingService: TaggingContract.Service = mockk()
    private val attachmentService: AttachmentContract.Service = mockk()
    private val errorHandler: SdkContract.ErrorHandler = mockk()

    @Before
    fun setup() {
        clearAllMocks()

        recordService = spyk(
            RecordService(
                PARTNER_ID,
                ALIAS,
                apiService,
                tagCryptoService,
                taggingService,
                resourceCryptoService,
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
    fun `Given, uploadData is called with a DecryptedRecord, which contains a DataResource, and UserId, it reflects it`() {
        // Given
        val resource: DataResource = mockk()
        val decryptedRecord: DecryptedBaseRecord<DataResource> = mockk()

        every { decryptedRecord.resource } returns resource

        // When
        val record = recordService.uploadData(decryptedRecord, USER_ID)

        // Then
        assertSame(
            actual = record,
            expected = decryptedRecord
        )

        verifyOrder {
            recordService.uploadData(decryptedRecord, USER_ID)
        }

        verify { attachmentService.upload(any(), any(), any()) wasNot Called }
    }

    @Test
    fun `Given, uploadData is called with a DecryptedRecord, which contains a Fhir3 resource, and UserId, it reflects it, if it cannot contain an attachment`() {
        // Given
        val resource: Fhir3Resource = mockk(relaxed = true)
        val decryptedRecord: DecryptedBaseRecord<Fhir3Resource> = mockk()

        every { decryptedRecord.resource } returns resource
        every { SdkFhirAttachmentHelper.hasAttachment(resource) } returns false
        every { SdkFhirAttachmentHelper.getAttachment(any()) } returns mockk()

        // When
        val record = recordService.uploadData(decryptedRecord, USER_ID)

        // Then
        assertSame(
            actual = record,
            expected = decryptedRecord
        )

        verifyOrder {
            recordService.uploadData(decryptedRecord, USER_ID)
        }

        verify { attachmentService.upload(any(), any(), any()) wasNot Called }
    }

    @Test
    fun `Given, uploadData is called with a DecryptedRecord, which contains a Fhir3 resource, and UserId, it reflects it, if it contains no Attachments`() {
        // Given
        val resource: Fhir3Resource = mockk(relaxed = true)
        val decryptedRecord: DecryptedBaseRecord<Fhir3Resource> = mockk()

        every { decryptedRecord.resource } returns resource
        every { SdkFhirAttachmentHelper.hasAttachment(resource) } returns true
        every { SdkFhirAttachmentHelper.getAttachment(resource) } returns null

        // When
        val record = recordService.uploadData(decryptedRecord, USER_ID)

        // Then
        assertSame(
            actual = record,
            expected = decryptedRecord
        )

        verify { attachmentService.upload(any(), any(), any()) wasNot Called }
    }

    @Test
    fun `Given, uploadData is called with a DecryptedRecord, which contains a Fhir3 resource, and UserId, it uploads the records Attachments, after the constrais are met`() {
        // Given
        mockkObject(AttachmentGuardian)

        val resource: Fhir3Resource = mockk(relaxed = true)
        val decryptedRecord: DecryptedBaseRecord<Fhir3Resource> = mockk()
        val attachmentKey: GCKey = mockk()
        val rawAttachments: MutableList<Fhir3Attachment> = mutableListOf(
            mockk()
        )
        val wrappedAttachment: WrapperContract.Attachment = spyk()
        val hash = "hash"
        val updatedAttachments = listOf<Pair<WrapperContract.Attachment, List<String>>>(
            mockk()
        )

        every { decryptedRecord.attachmentsKey } returns attachmentKey

        every { wrappedAttachment.id } returns null
        every { wrappedAttachment.hash } returns hash
        every { wrappedAttachment.size } returns 42

        every { decryptedRecord.resource } returns resource
        every { SdkFhirAttachmentHelper.hasAttachment(resource) } returns true
        every { SdkFhirAttachmentHelper.getAttachment(resource) } returns rawAttachments as MutableList<Any?>
        every { SdkAttachmentFactory.wrap(rawAttachments[0]) } returns wrappedAttachment

        every { AttachmentGuardian.guardId(wrappedAttachment) } just Runs
        every { AttachmentGuardian.guardSize(wrappedAttachment) } just Runs
        every { AttachmentGuardian.guardHash(wrappedAttachment) } returns true

        every {
            attachmentService.upload(
                listOf(wrappedAttachment),
                attachmentKey,
                USER_ID
            )
        } returns Single.just(updatedAttachments)
        every {
            recordService.updateFhirResourceIdentifier(
                resource,
                updatedAttachments
            )
        } returns Unit

        // Then
        val record = recordService.uploadData(decryptedRecord, USER_ID)
        // When
        assertSame(
            actual = record,
            expected = decryptedRecord
        )

        verifyOrder {
            attachmentService.upload(
                listOf(wrappedAttachment),
                attachmentKey,
                USER_ID
            )
            recordService.updateFhirResourceIdentifier(resource, updatedAttachments)
        }

        verify(exactly = 1) {
            AttachmentGuardian.guardId(wrappedAttachment)
            AttachmentGuardian.guardSize(wrappedAttachment)
            AttachmentGuardian.guardHash(wrappedAttachment)
        }

        mockkObject(AttachmentGuardian)
    }

    @Test
    fun `Given, uploadData is called with a DecryptedRecord, which contains a Fhir3 resource, and UserId, it ignores Attachments, which are null`() {
        // Given
        mockkObject(AttachmentGuardian)

        val resource: Fhir3Resource = mockk(relaxed = true)
        val decryptedRecord: DecryptedBaseRecord<Fhir3Resource> = mockk()
        val attachmentKey: GCKey = mockk()
        val rawAttachments: MutableList<Fhir3Attachment?> = mutableListOf(
            null,
            mockk()
        )
        val wrappedAttachment: WrapperContract.Attachment = spyk()
        val hash = "hash"
        val updatedAttachments = listOf<Pair<WrapperContract.Attachment, List<String>>>(
            mockk()
        )

        every { decryptedRecord.attachmentsKey } returns attachmentKey

        every { wrappedAttachment.id } returns null
        every { wrappedAttachment.hash } returns hash
        every { wrappedAttachment.size } returns 42

        every { decryptedRecord.resource } returns resource
        every { SdkFhirAttachmentHelper.hasAttachment(resource) } returns true
        every { SdkFhirAttachmentHelper.getAttachment(resource) } returns rawAttachments as MutableList<Any?>
        every { SdkAttachmentFactory.wrap(rawAttachments[1]!!) } returns wrappedAttachment

        every { AttachmentGuardian.guardId(wrappedAttachment) } just Runs
        every { AttachmentGuardian.guardSize(wrappedAttachment) } just Runs
        every { AttachmentGuardian.guardHash(wrappedAttachment) } returns true

        every {
            attachmentService.upload(
                listOf(wrappedAttachment),
                attachmentKey,
                USER_ID
            )
        } returns Single.just(updatedAttachments)
        every {
            recordService.updateFhirResourceIdentifier(
                resource,
                updatedAttachments
            )
        } returns Unit

        // Then
        val record = recordService.uploadData(decryptedRecord, USER_ID)
        // When
        assertSame(
            actual = record,
            expected = decryptedRecord
        )

        verifyOrder {
            attachmentService.upload(
                listOf(wrappedAttachment),
                attachmentKey,
                USER_ID
            )
            recordService.updateFhirResourceIdentifier(resource, updatedAttachments)
        }

        verify(exactly = 1) {
            AttachmentGuardian.guardId(wrappedAttachment)
            AttachmentGuardian.guardSize(wrappedAttachment)
            AttachmentGuardian.guardHash(wrappedAttachment)
        }

        mockkObject(AttachmentGuardian)
    }

    @Test
    fun `Given, uploadData is called with a DecryptedRecord, which contains a Fhir3 resource, no attachment key, and UserId, it fetches the key and uploads the records Attachments`() {
        // Given
        mockkObject(AttachmentGuardian)

        val resource: Fhir3Resource = mockk(relaxed = true)
        val decryptedRecord: DecryptedBaseRecord<Fhir3Resource> = mockk()
        val attachmentKey: GCKey = mockk()
        val rawAttachments: MutableList<Fhir3Attachment> = mutableListOf(
            mockk()
        )
        val wrappedAttachment: WrapperContract.Attachment = spyk()
        val hash = "hash"
        val updatedAttachments = listOf<Pair<WrapperContract.Attachment, List<String>>>(
            mockk()
        )

        every { decryptedRecord.attachmentsKey } returnsMany listOf(null, attachmentKey)

        every { wrappedAttachment.id } returns null
        every { wrappedAttachment.hash } returns hash
        every { wrappedAttachment.size } returns 42

        every { decryptedRecord.resource } returns resource
        every { SdkFhirAttachmentHelper.hasAttachment(resource) } returns true
        every { SdkFhirAttachmentHelper.getAttachment(resource) } returns rawAttachments as MutableList<Any?>
        every { SdkAttachmentFactory.wrap(rawAttachments[0]) } returns wrappedAttachment
        every { cryptoService.generateGCKey() } returns Single.just(attachmentKey)
        every { decryptedRecord.attachmentsKey = attachmentKey } returns Unit

        every { AttachmentGuardian.guardId(wrappedAttachment) } just Runs
        every { AttachmentGuardian.guardSize(wrappedAttachment) } just Runs
        every { AttachmentGuardian.guardHash(wrappedAttachment) } returns true

        every {
            attachmentService.upload(
                listOf(wrappedAttachment),
                attachmentKey,
                USER_ID
            )
        } returns Single.just(updatedAttachments)
        every {
            recordService.updateFhirResourceIdentifier(
                resource,
                updatedAttachments
            )
        } returns Unit

        // Then
        val record = recordService.uploadData(decryptedRecord, USER_ID)
        // When
        assertSame(
            actual = record,
            expected = decryptedRecord
        )

        verifyOrder {
            recordService.getValidHash(wrappedAttachment)
            cryptoService.generateGCKey()
            decryptedRecord.attachmentsKey = attachmentKey
            attachmentService.upload(
                listOf(wrappedAttachment),
                attachmentKey,
                USER_ID
            )
            recordService.updateFhirResourceIdentifier(resource, updatedAttachments)
        }

        verify(exactly = 1) {
            AttachmentGuardian.guardId(wrappedAttachment)
            AttachmentGuardian.guardSize(wrappedAttachment)
            AttachmentGuardian.guardHash(wrappedAttachment)
        }

        mockkObject(AttachmentGuardian)
    }

    // FHIR4
    @Test
    fun `Given, uploadData is called with a DecryptedRecord, which contains a Fhir4 resource, and UserId, it reflects it, if it cannot contain an attachment`() {
        // Given
        val resource: Fhir4Resource = mockk(relaxed = true)
        val decryptedRecord: DecryptedBaseRecord<Fhir4Resource> = mockk()

        every { decryptedRecord.resource } returns resource
        every { SdkFhirAttachmentHelper.hasAttachment(resource) } returns false
        every { SdkFhirAttachmentHelper.getAttachment(any()) } returns mockk()

        // When
        val record = recordService.uploadData(decryptedRecord, USER_ID)

        // Then
        assertSame(
            actual = record,
            expected = decryptedRecord
        )

        verifyOrder {
            recordService.uploadData(decryptedRecord, USER_ID)
        }

        verify { attachmentService.upload(any(), any(), any()) wasNot Called }
    }

    @Test
    fun `Given, uploadData is called with a DecryptedRecord, which contains a Fhir4 resource, and UserId, it reflects it, if it contains no Attachments`() {
        // Given
        val resource: Fhir4Resource = mockk(relaxed = true)
        val decryptedRecord: DecryptedBaseRecord<Fhir4Resource> = mockk()

        every { decryptedRecord.resource } returns resource
        every { SdkFhirAttachmentHelper.hasAttachment(resource) } returns true
        every { SdkFhirAttachmentHelper.getAttachment(resource) } returns null

        // When
        val record = recordService.uploadData(decryptedRecord, USER_ID)

        // Then
        assertSame(
            actual = record,
            expected = decryptedRecord
        )

        verify { attachmentService.upload(any(), any(), any()) wasNot Called }
    }

    @Test
    fun `Given, uploadData is called with a DecryptedRecord, which contains a Fhir4 resource, and UserId, it uploads the records Attachments, after the constrais are met`() {
        // Given
        mockkObject(AttachmentGuardian)

        val resource: Fhir4Resource = mockk(relaxed = true)
        val decryptedRecord: DecryptedBaseRecord<Fhir4Resource> = mockk()
        val attachmentKey: GCKey = mockk()
        val rawAttachments: MutableList<Fhir4Attachment> = mutableListOf(
            mockk()
        )
        val wrappedAttachment: WrapperContract.Attachment = spyk()
        val hash = "hash"
        val updatedAttachments = listOf<Pair<WrapperContract.Attachment, List<String>>>(
            mockk()
        )

        every { decryptedRecord.attachmentsKey } returns attachmentKey

        every { wrappedAttachment.id } returns null
        every { wrappedAttachment.hash } returns hash
        every { wrappedAttachment.size } returns 42

        every { decryptedRecord.resource } returns resource
        every { SdkFhirAttachmentHelper.hasAttachment(resource) } returns true
        every { SdkFhirAttachmentHelper.getAttachment(resource) } returns rawAttachments as MutableList<Any?>
        every { SdkAttachmentFactory.wrap(rawAttachments[0]) } returns wrappedAttachment

        every { AttachmentGuardian.guardId(wrappedAttachment) } just Runs
        every { AttachmentGuardian.guardSize(wrappedAttachment) } just Runs
        every { AttachmentGuardian.guardHash(wrappedAttachment) } returns true

        every {
            attachmentService.upload(
                listOf(wrappedAttachment),
                attachmentKey,
                USER_ID
            )
        } returns Single.just(updatedAttachments)
        every {
            recordService.updateFhirResourceIdentifier(
                resource,
                updatedAttachments
            )
        } returns Unit

        // Then
        val record = recordService.uploadData(decryptedRecord, USER_ID)
        // When
        assertSame(
            actual = record,
            expected = decryptedRecord
        )

        verifyOrder {
            attachmentService.upload(
                listOf(wrappedAttachment),
                attachmentKey,
                USER_ID
            )
            recordService.updateFhirResourceIdentifier(resource, updatedAttachments)
        }

        verify(exactly = 1) {
            AttachmentGuardian.guardId(wrappedAttachment)
            AttachmentGuardian.guardSize(wrappedAttachment)
            AttachmentGuardian.guardHash(wrappedAttachment)
        }

        mockkObject(AttachmentGuardian)
    }

    @Test
    fun `Given, uploadData is called with a DecryptedRecord, which contains a Fhir4 resource, and UserId, it ignores Attachments, which are null`() {
        // Given
        mockkObject(AttachmentGuardian)

        val resource: Fhir4Resource = mockk(relaxed = true)
        val decryptedRecord: DecryptedBaseRecord<Fhir4Resource> = mockk()
        val attachmentKey: GCKey = mockk()
        val rawAttachments: MutableList<Fhir4Attachment?> = mutableListOf(
            null,
            mockk()
        )
        val wrappedAttachment: WrapperContract.Attachment = spyk()
        val hash = "hash"
        val updatedAttachments = listOf<Pair<WrapperContract.Attachment, List<String>>>(
            mockk()
        )

        every { decryptedRecord.attachmentsKey } returns attachmentKey

        every { wrappedAttachment.id } returns null
        every { wrappedAttachment.hash } returns hash
        every { wrappedAttachment.size } returns 42

        every { decryptedRecord.resource } returns resource
        every { SdkFhirAttachmentHelper.hasAttachment(resource) } returns true
        every { SdkFhirAttachmentHelper.getAttachment(resource) } returns rawAttachments as MutableList<Any?>
        every { SdkAttachmentFactory.wrap(rawAttachments[1]!!) } returns wrappedAttachment

        every { AttachmentGuardian.guardId(wrappedAttachment) } just Runs
        every { AttachmentGuardian.guardSize(wrappedAttachment) } just Runs
        every { AttachmentGuardian.guardHash(wrappedAttachment) } returns true

        every {
            attachmentService.upload(
                listOf(wrappedAttachment),
                attachmentKey,
                USER_ID
            )
        } returns Single.just(updatedAttachments)
        every {
            recordService.updateFhirResourceIdentifier(
                resource,
                updatedAttachments
            )
        } returns Unit

        // Then
        val record = recordService.uploadData(decryptedRecord, USER_ID)
        // When
        assertSame(
            actual = record,
            expected = decryptedRecord
        )

        verifyOrder {
            attachmentService.upload(
                listOf(wrappedAttachment),
                attachmentKey,
                USER_ID
            )
            recordService.updateFhirResourceIdentifier(resource, updatedAttachments)
        }

        verify(exactly = 1) {
            AttachmentGuardian.guardId(wrappedAttachment)
            AttachmentGuardian.guardSize(wrappedAttachment)
            AttachmentGuardian.guardHash(wrappedAttachment)
        }

        mockkObject(AttachmentGuardian)
    }

    @Test
    fun `Given, uploadData is called with a DecryptedRecord, which contains a Fhir4 resource, no attachment key, and UserId, it fetches the key and uploads the records Attachments`() {
        // Given
        mockkObject(AttachmentGuardian)

        val resource: Fhir4Resource = mockk(relaxed = true)
        val decryptedRecord: DecryptedBaseRecord<Fhir4Resource> = mockk()
        val attachmentKey: GCKey = mockk()
        val rawAttachments: MutableList<Fhir4Attachment> = mutableListOf(
            mockk()
        )
        val wrappedAttachment: WrapperContract.Attachment = spyk()
        val hash = "hash"
        val updatedAttachments = listOf<Pair<WrapperContract.Attachment, List<String>>>(
            mockk()
        )

        every { decryptedRecord.attachmentsKey } returnsMany listOf(null, attachmentKey)

        every { wrappedAttachment.id } returns null
        every { wrappedAttachment.hash } returns hash
        every { wrappedAttachment.size } returns 42

        every { decryptedRecord.resource } returns resource
        every { SdkFhirAttachmentHelper.hasAttachment(resource) } returns true
        every { SdkFhirAttachmentHelper.getAttachment(resource) } returns rawAttachments as MutableList<Any?>
        every { SdkAttachmentFactory.wrap(rawAttachments[0]) } returns wrappedAttachment
        every { cryptoService.generateGCKey() } returns Single.just(attachmentKey)
        every { decryptedRecord.attachmentsKey = attachmentKey } returns Unit

        every { AttachmentGuardian.guardId(wrappedAttachment) } just Runs
        every { AttachmentGuardian.guardSize(wrappedAttachment) } just Runs
        every { AttachmentGuardian.guardHash(wrappedAttachment) } returns true

        every {
            attachmentService.upload(
                listOf(wrappedAttachment),
                attachmentKey,
                USER_ID
            )
        } returns Single.just(updatedAttachments)
        every {
            recordService.updateFhirResourceIdentifier(
                resource,
                updatedAttachments
            )
        } returns Unit

        // Then
        val record = recordService.uploadData(decryptedRecord, USER_ID)
        // When
        assertSame(
            actual = record,
            expected = decryptedRecord
        )

        verifyOrder {
            recordService.getValidHash(wrappedAttachment)
            cryptoService.generateGCKey()
            decryptedRecord.attachmentsKey = attachmentKey
            attachmentService.upload(
                listOf(wrappedAttachment),
                attachmentKey,
                USER_ID
            )
            recordService.updateFhirResourceIdentifier(resource, updatedAttachments)
        }

        verify(exactly = 1) {
            AttachmentGuardian.guardId(wrappedAttachment)
            AttachmentGuardian.guardSize(wrappedAttachment)
            AttachmentGuardian.guardHash(wrappedAttachment)
        }

        mockkObject(AttachmentGuardian)
    }
}
