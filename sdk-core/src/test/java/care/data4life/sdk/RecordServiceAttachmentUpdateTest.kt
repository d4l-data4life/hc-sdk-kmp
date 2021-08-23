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

import care.data4life.sdk.attachment.AttachmentContract
import care.data4life.sdk.attachment.AttachmentGuardian
import care.data4life.sdk.crypto.CryptoContract
import care.data4life.sdk.crypto.GCKey
import care.data4life.sdk.data.DataResource
import care.data4life.sdk.fhir.Fhir3Attachment
import care.data4life.sdk.fhir.Fhir3Resource
import care.data4life.sdk.fhir.Fhir4Attachment
import care.data4life.sdk.fhir.Fhir4Resource
import care.data4life.sdk.fhir.FhirContract
import care.data4life.sdk.lang.CoreRuntimeException
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
import kotlin.test.assertFailsWith
import kotlin.test.assertSame

class RecordServiceAttachmentUpdateTest {
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
    fun `Given, updateData is called with a DecryptedRecord, which contains a DataResource resource, a DataResource and the UserId, it reflects it`() {
        // Given
        val oldResource: DataResource = mockk()
        val newResource: DataResource = mockk()
        val decryptedRecord: DecryptedBaseRecord<DataResource> = mockk()

        every { decryptedRecord.resource } returns oldResource

        // When
        val record = recordService.updateData(
            decryptedRecord,
            newResource,
            USER_ID
        )

        // Then
        assertSame(
            actual = record,
            expected = decryptedRecord
        )

        verify { attachmentService.upload(any(), any(), any()) wasNot Called }
    }

    @Test
    fun `Given, updateData is called with a DecryptedRecord, which contains a Fhir3Resource, a non Fhir resource and a UserId, it fails`() {
        // Given
        val oldResource: Fhir3Resource = mockk()
        val newResource: DataResource = mockk()
        val decryptedRecord: DecryptedBaseRecord<Any> = mockk()

        every { decryptedRecord.resource } returns oldResource

        // Then
        assertFailsWith<CoreRuntimeException.UnsupportedOperation> {
            // When
            recordService.updateData(
                decryptedRecord,
                newResource,
                USER_ID
            )
        }
    }

    @Test
    fun `Given, updateData is called with a DecryptedRecord, which contains a Fhir3Resource, a Fhir3 resource and a UserId, it reflects the record, if the old resource is not capable of holding attachments`() {
        // Given
        val oldResource: Fhir3Resource = mockk()
        val newResource: Fhir3Resource = mockk()
        val decryptedRecord: DecryptedBaseRecord<Any> = mockk()

        every { decryptedRecord.resource } returns oldResource
        every { SdkFhirAttachmentHelper.hasAttachment(oldResource) } returns false
        every { SdkFhirAttachmentHelper.getAttachment(any()) } returns mockk()

        // When
        val record = recordService.updateData(
            decryptedRecord,
            newResource,
            USER_ID
        )

        // Then
        assertSame(
            actual = record,
            expected = decryptedRecord
        )

        verifyOrder {
            recordService.updateData(
                decryptedRecord,
                newResource,
                USER_ID
            )
        }

        verify { attachmentService.upload(any(), any(), any()) wasNot Called }
        verify(exactly = 0) { SdkFhirAttachmentHelper.getAttachment(any()) }
    }

    @Test
    fun `Given, updateData is called with a DecryptedRecord, which contains a Fhir3Resource, a Fhir3 resource and a UserId, it does not uploads it, if the constrains are not meet and it has no AttachmentId`() {
        // Given
        mockkObject(AttachmentGuardian)

        val oldResource: Fhir3Resource = mockk()
        val newResource: Fhir3Resource = mockk()
        val decryptedRecord: DecryptedBaseRecord<Any> = mockk()
        val attachmentKey: GCKey = mockk()
        val hash = "hash"
        val size = 23

        val newAttachments: MutableList<Fhir3Attachment> = mutableListOf(
            mockk()
        )
        val wrappedNewAttachment: WrapperContract.Attachment = spyk()

        every { wrappedNewAttachment.hash } returns hash
        every { wrappedNewAttachment.size } returns size

        every { decryptedRecord.resource } returns oldResource
        every { decryptedRecord.attachmentsKey } returns attachmentKey

        every { SdkFhirAttachmentHelper.hasAttachment(oldResource) } returns true
        every { SdkFhirAttachmentHelper.getAttachment(oldResource) } returns null
        every { SdkFhirAttachmentHelper.getAttachment(newResource) } returns newAttachments as MutableList<Any?>
        every { SdkAttachmentFactory.wrap(newAttachments[0]) } returns wrappedNewAttachment

        every { AttachmentGuardian.guardSize(wrappedNewAttachment) } just Runs
        every { AttachmentGuardian.guardHash(wrappedNewAttachment) } returns false

        // Then
        val record = recordService.updateData(decryptedRecord, newResource, USER_ID)

        // When
        assertSame(
            actual = record,
            expected = decryptedRecord
        )

        verifyOrder {
            AttachmentGuardian.guardSize(wrappedNewAttachment)
            AttachmentGuardian.guardHash(wrappedNewAttachment)
        }
        verify { attachmentService.upload(any(), any(), any()) wasNot Called }

        unmockkObject(AttachmentGuardian)
    }

    @Test
    fun `Given, updateData is called with a DecryptedRecord, which contains a Fhir3Resource, a Fhir3 resource and a UserId, it uploads it, if the constrains are meet and it has no AttachmentId`() {
        // Given
        mockkObject(AttachmentGuardian)

        val oldResource: Fhir3Resource = mockk()
        val newResource: Fhir3Resource = mockk()
        val decryptedRecord: DecryptedBaseRecord<Any> = mockk()
        val attachmentKey: GCKey = mockk()
        val hash = "hash"
        val size = 23

        val newAttachments: MutableList<Fhir3Attachment> = mutableListOf(
            mockk()
        )
        val wrappedNewAttachment: WrapperContract.Attachment = spyk()

        val updatedAttachments = listOf<Pair<WrapperContract.Attachment, List<String>>>(
            mockk()
        )

        every { wrappedNewAttachment.hash } returns hash
        every { wrappedNewAttachment.size } returns size

        every { decryptedRecord.resource } returns oldResource
        every { decryptedRecord.attachmentsKey } returns attachmentKey

        every { SdkFhirAttachmentHelper.hasAttachment(oldResource) } returns true
        every { SdkFhirAttachmentHelper.getAttachment(oldResource) } returns null
        every { SdkFhirAttachmentHelper.getAttachment(newResource) } returns newAttachments as MutableList<Any?>
        every { SdkAttachmentFactory.wrap(newAttachments[0]) } returns wrappedNewAttachment

        every { AttachmentGuardian.guardSize(wrappedNewAttachment) } just Runs
        every { AttachmentGuardian.guardHash(wrappedNewAttachment) } returns true

        every {
            attachmentService.upload(
                listOf(wrappedNewAttachment),
                attachmentKey,
                USER_ID
            )
        } returns Single.just(updatedAttachments)
        every {
            recordService.updateFhirResourceIdentifier(
                newResource,
                updatedAttachments
            )
        } returns Unit

        // Then
        val record = recordService.updateData(decryptedRecord, newResource, USER_ID)

        // When
        assertSame(
            actual = record,
            expected = decryptedRecord
        )

        verifyOrder {
            AttachmentGuardian.guardSize(wrappedNewAttachment)
            AttachmentGuardian.guardHash(wrappedNewAttachment)
            attachmentService.upload(
                listOf(wrappedNewAttachment),
                attachmentKey,
                USER_ID
            )
            recordService.updateFhirResourceIdentifier(newResource, updatedAttachments)
        }

        unmockkObject(AttachmentGuardian)
    }

    @Test
    fun `Given, updateData is called with a DecryptedRecord, which contains a Fhir3Resource, a Fhir3 resource and a UserId, it does not uploads it, if the constrains are not meet and it has a AttachmentId`() {
        // Given
        mockkObject(AttachmentGuardian)

        val oldResource: Fhir3Resource = mockk()
        val newResource: Fhir3Resource = mockk()
        val decryptedRecord: DecryptedBaseRecord<Any> = mockk()
        val attachmentKey: GCKey = mockk()
        val oldAttachmentId = "old"
        val attachmentId = "id"
        val hash = "hash"
        val size = 23

        val oldAttachments: MutableList<Fhir3Attachment> = mutableListOf(
            mockk()
        )
        val wrappedOldAttachment: WrapperContract.Attachment = spyk()

        val newAttachments: MutableList<Fhir3Attachment> = mutableListOf(
            mockk()
        )
        val wrappedNewAttachment: WrapperContract.Attachment = spyk()

        every { wrappedOldAttachment.id } returns oldAttachmentId

        every { wrappedNewAttachment.hash } returns hash
        every { wrappedNewAttachment.size } returns size
        every { wrappedNewAttachment.id } returns attachmentId

        every { decryptedRecord.resource } returns oldResource
        every { decryptedRecord.attachmentsKey } returns attachmentKey

        every { SdkFhirAttachmentHelper.hasAttachment(oldResource) } returns true
        every { SdkFhirAttachmentHelper.getAttachment(oldResource) } returns oldAttachments as MutableList<Any?>
        every { SdkAttachmentFactory.wrap(oldAttachments[0]) } returns wrappedOldAttachment

        every { SdkFhirAttachmentHelper.getAttachment(newResource) } returns newAttachments as MutableList<Any?>
        every { SdkAttachmentFactory.wrap(newAttachments[0]) } returns wrappedNewAttachment

        every { AttachmentGuardian.guardSize(wrappedNewAttachment) } just Runs
        every {
            AttachmentGuardian.guardIdAgainstExistingIds(wrappedNewAttachment, setOf(oldAttachmentId))
        } just Runs
        every { AttachmentGuardian.guardHash(wrappedNewAttachment) } returns false

        // Then
        val record = recordService.updateData(decryptedRecord, newResource, USER_ID)

        // When
        assertSame(
            actual = record,
            expected = decryptedRecord
        )

        verifyOrder {
            AttachmentGuardian.guardSize(wrappedNewAttachment)
            AttachmentGuardian.guardIdAgainstExistingIds(wrappedNewAttachment, setOf(oldAttachmentId))
            AttachmentGuardian.guardHash(wrappedNewAttachment)
        }
        verify { attachmentService.upload(any(), any(), any()) wasNot Called }

        unmockkObject(AttachmentGuardian)
    }

    @Test
    fun `Given, updateData is called with a DecryptedRecord, which contains a Fhir3Resource, a Fhir3 resource and a UserId, it uploads it, if the constrains are meet and it has a AttachmentId`() {
        // Given
        mockkObject(AttachmentGuardian)

        val oldResource: Fhir3Resource = mockk()
        val newResource: Fhir3Resource = mockk()
        val decryptedRecord: DecryptedBaseRecord<Any> = mockk()
        val attachmentKey: GCKey = mockk()
        val oldAttachmentId = "old"
        val attachmentId = "id"
        val hash = "hash"
        val size = 23

        val oldAttachments: MutableList<Fhir3Attachment> = mutableListOf(
            mockk()
        )
        val wrappedOldAttachment: WrapperContract.Attachment = mockk()

        val newAttachments: MutableList<Fhir3Attachment> = mutableListOf(
            mockk()
        )
        val wrappedNewAttachment: WrapperContract.Attachment = mockk()

        val updatedAttachments = listOf<Pair<WrapperContract.Attachment, List<String>>>(
            mockk()
        )

        every { wrappedOldAttachment.id } returns oldAttachmentId

        every { wrappedNewAttachment.id } returns attachmentId
        every { wrappedNewAttachment.hash } returns hash
        every { wrappedNewAttachment.size } returns size

        every { decryptedRecord.resource } returns oldResource
        every { decryptedRecord.attachmentsKey } returns attachmentKey

        every { SdkFhirAttachmentHelper.hasAttachment(oldResource) } returns true
        every { SdkFhirAttachmentHelper.getAttachment(oldResource) } returns oldAttachments as MutableList<Any?>
        every { SdkAttachmentFactory.wrap(oldAttachments[0]) } returns wrappedOldAttachment

        every { SdkFhirAttachmentHelper.getAttachment(newResource) } returns newAttachments as MutableList<Any?>
        every { SdkAttachmentFactory.wrap(newAttachments[0]) } returns wrappedNewAttachment

        every { AttachmentGuardian.guardSize(wrappedNewAttachment) } just Runs
        every {
            AttachmentGuardian.guardIdAgainstExistingIds(wrappedNewAttachment, setOf(oldAttachmentId))
        } just Runs
        every { AttachmentGuardian.guardHash(wrappedNewAttachment) } returns true

        every {
            attachmentService.upload(
                listOf(wrappedNewAttachment),
                attachmentKey,
                USER_ID
            )
        } returns Single.just(updatedAttachments)
        every {
            recordService.updateFhirResourceIdentifier(
                newResource,
                updatedAttachments
            )
        } returns Unit

        // Then
        val record = recordService.updateData(decryptedRecord, newResource, USER_ID)

        // When
        assertSame(
            actual = record,
            expected = decryptedRecord
        )

        verifyOrder {
            AttachmentGuardian.guardSize(wrappedNewAttachment)
            AttachmentGuardian.guardIdAgainstExistingIds(wrappedNewAttachment, setOf(oldAttachmentId))
            AttachmentGuardian.guardHash(wrappedNewAttachment)
            attachmentService.upload(
                listOf(wrappedNewAttachment),
                attachmentKey,
                USER_ID
            )
            recordService.updateFhirResourceIdentifier(newResource, updatedAttachments)
        }

        unmockkObject(AttachmentGuardian)
    }

    // FHIR4
    @Test
    fun `Given, updateData is called with a DecryptedRecord, which contains a Fhir4Resource, a non Fhir resource and a UserId, it fails`() {
        // Given
        val oldResource: Fhir4Resource = mockk()
        val newResource: DataResource = mockk()
        val decryptedRecord: DecryptedBaseRecord<Any> = mockk()

        every { decryptedRecord.resource } returns oldResource

        // Then
        assertFailsWith<CoreRuntimeException.UnsupportedOperation> {
            // When
            recordService.updateData(
                decryptedRecord,
                newResource,
                USER_ID
            )
        }
    }

    @Test
    fun `Given, updateData is called with a DecryptedRecord, which contains a Fhir4Resource, a Fhir4 resource and a UserId, it reflects the record, if the old resource is not capable of holding attachments`() {
        // Given
        val oldResource: Fhir4Resource = mockk()
        val newResource: Fhir4Resource = mockk()
        val decryptedRecord: DecryptedBaseRecord<Any> = mockk()

        every { decryptedRecord.resource } returns oldResource
        every { SdkFhirAttachmentHelper.hasAttachment(oldResource) } returns false

        // When
        val record = recordService.updateData(
            decryptedRecord,
            newResource,
            USER_ID
        )

        // Then
        assertSame(
            actual = record,
            expected = decryptedRecord
        )

        verifyOrder {
            recordService.updateData(
                decryptedRecord,
                newResource,
                USER_ID
            )
        }
        verify { attachmentService.upload(any(), any(), any()) wasNot Called }
    }

    @Test
    fun `Given, updateData is called with a DecryptedRecord, which contains a Fhir4Resource, a Fhir4 resource and a UserId, it does not uploads it, if the constrains are not meet and it has no AttachmentId`() {
        // Given
        mockkObject(AttachmentGuardian)

        val oldResource: Fhir4Resource = mockk()
        val newResource: Fhir4Resource = mockk()
        val decryptedRecord: DecryptedBaseRecord<Any> = mockk()
        val attachmentKey: GCKey = mockk()
        val hash = "hash"
        val size = 23

        val newAttachments: MutableList<Fhir4Attachment> = mutableListOf(
            mockk()
        )
        val wrappedNewAttachment: WrapperContract.Attachment = spyk()

        every { wrappedNewAttachment.hash } returns hash
        every { wrappedNewAttachment.size } returns size

        every { decryptedRecord.resource } returns oldResource
        every { decryptedRecord.attachmentsKey } returns attachmentKey

        every { SdkFhirAttachmentHelper.hasAttachment(oldResource) } returns true
        every { SdkFhirAttachmentHelper.getAttachment(oldResource) } returns null
        every { SdkFhirAttachmentHelper.getAttachment(newResource) } returns newAttachments as MutableList<Any?>
        every { SdkAttachmentFactory.wrap(newAttachments[0]) } returns wrappedNewAttachment

        every { AttachmentGuardian.guardSize(wrappedNewAttachment) } just Runs
        every { AttachmentGuardian.guardHash(wrappedNewAttachment) } returns false

        // Then
        val record = recordService.updateData(decryptedRecord, newResource, USER_ID)

        // When
        assertSame(
            actual = record,
            expected = decryptedRecord
        )

        verifyOrder {
            AttachmentGuardian.guardSize(wrappedNewAttachment)
            AttachmentGuardian.guardHash(wrappedNewAttachment)
        }
        verify { attachmentService.upload(any(), any(), any()) wasNot Called }

        unmockkObject(AttachmentGuardian)
    }

    @Test
    fun `Given, updateData is called with a DecryptedRecord, which contains a Fhir4Resource, a Fhir4 resource and a UserId, it uploads it, if the constrains are meet and it has no AttachmentId`() {
        // Given
        mockkObject(AttachmentGuardian)

        val oldResource: Fhir4Resource = mockk()
        val newResource: Fhir4Resource = mockk()
        val decryptedRecord: DecryptedBaseRecord<Any> = mockk()
        val attachmentKey: GCKey = mockk()
        val hash = "hash"
        val size = 23

        val newAttachments: MutableList<Fhir4Attachment> = mutableListOf(
            mockk()
        )
        val wrappedNewAttachment: WrapperContract.Attachment = spyk()

        val updatedAttachments = listOf<Pair<WrapperContract.Attachment, List<String>>>(
            mockk()
        )

        every { wrappedNewAttachment.hash } returns hash
        every { wrappedNewAttachment.size } returns size

        every { decryptedRecord.resource } returns oldResource
        every { decryptedRecord.attachmentsKey } returns attachmentKey

        every { SdkFhirAttachmentHelper.hasAttachment(oldResource) } returns true
        every { SdkFhirAttachmentHelper.getAttachment(oldResource) } returns null
        every { SdkFhirAttachmentHelper.getAttachment(newResource) } returns newAttachments as MutableList<Any?>
        every { SdkAttachmentFactory.wrap(newAttachments[0]) } returns wrappedNewAttachment

        every { AttachmentGuardian.guardSize(wrappedNewAttachment) } just Runs
        every { AttachmentGuardian.guardHash(wrappedNewAttachment) } returns true

        every {
            attachmentService.upload(
                listOf(wrappedNewAttachment),
                attachmentKey,
                USER_ID
            )
        } returns Single.just(updatedAttachments)
        every {
            recordService.updateFhirResourceIdentifier(
                newResource,
                updatedAttachments
            )
        } returns Unit

        // Then
        val record = recordService.updateData(decryptedRecord, newResource, USER_ID)

        // When
        assertSame(
            actual = record,
            expected = decryptedRecord
        )

        verifyOrder {
            AttachmentGuardian.guardSize(wrappedNewAttachment)
            AttachmentGuardian.guardHash(wrappedNewAttachment)
            attachmentService.upload(
                listOf(wrappedNewAttachment),
                attachmentKey,
                USER_ID
            )
            recordService.updateFhirResourceIdentifier(newResource, updatedAttachments)
        }

        unmockkObject(AttachmentGuardian)
    }

    @Test
    fun `Given, updateData is called with a DecryptedRecord, which contains a Fhir4Resource, a Fhir4 resource and a UserId, it does not uploads it, if the constrains are not meet and it has a AttachmentId`() {
        // Given
        mockkObject(AttachmentGuardian)

        val oldResource: Fhir4Resource = mockk()
        val newResource: Fhir4Resource = mockk()
        val decryptedRecord: DecryptedBaseRecord<Any> = mockk()
        val attachmentKey: GCKey = mockk()
        val oldAttachmentId = "old"
        val attachmentId = "id"
        val hash = "hash"
        val size = 23

        val oldAttachments: MutableList<Fhir4Attachment> = mutableListOf(
            mockk()
        )
        val wrappedOldAttachment: WrapperContract.Attachment = spyk()

        val newAttachments: MutableList<Fhir4Attachment> = mutableListOf(
            mockk()
        )
        val wrappedNewAttachment: WrapperContract.Attachment = spyk()

        every { wrappedOldAttachment.id } returns oldAttachmentId

        every { wrappedNewAttachment.hash } returns hash
        every { wrappedNewAttachment.size } returns size
        every { wrappedNewAttachment.id } returns attachmentId

        every { decryptedRecord.resource } returns oldResource
        every { decryptedRecord.attachmentsKey } returns attachmentKey

        every { SdkFhirAttachmentHelper.hasAttachment(oldResource) } returns true
        every { SdkFhirAttachmentHelper.getAttachment(oldResource) } returns oldAttachments as MutableList<Any?>
        every { SdkAttachmentFactory.wrap(oldAttachments[0]) } returns wrappedOldAttachment

        every { SdkFhirAttachmentHelper.getAttachment(newResource) } returns newAttachments as MutableList<Any?>
        every { SdkAttachmentFactory.wrap(newAttachments[0]) } returns wrappedNewAttachment

        every { AttachmentGuardian.guardSize(wrappedNewAttachment) } just Runs
        every {
            AttachmentGuardian.guardIdAgainstExistingIds(wrappedNewAttachment, setOf(oldAttachmentId))
        } just Runs
        every { AttachmentGuardian.guardHash(wrappedNewAttachment) } returns false

        // Then
        val record = recordService.updateData(decryptedRecord, newResource, USER_ID)

        // When
        assertSame(
            actual = record,
            expected = decryptedRecord
        )

        verifyOrder {
            AttachmentGuardian.guardSize(wrappedNewAttachment)
            AttachmentGuardian.guardIdAgainstExistingIds(wrappedNewAttachment, setOf(oldAttachmentId))
            AttachmentGuardian.guardHash(wrappedNewAttachment)
        }
        verify { attachmentService.upload(any(), any(), any()) wasNot Called }

        unmockkObject(AttachmentGuardian)
    }

    @Test
    fun `Given, updateData is called with a DecryptedRecord, which contains a Fhir4Resource, a Fhir4 resource and a UserId, it uploads it, if the constrains are meet and it has a AttachmentId`() {
        // Given
        mockkObject(AttachmentGuardian)

        val oldResource: Fhir4Resource = mockk()
        val newResource: Fhir4Resource = mockk()
        val decryptedRecord: DecryptedBaseRecord<Any> = mockk()
        val attachmentKey: GCKey = mockk()
        val oldAttachmentId = "old"
        val attachmentId = "id"
        val hash = "hash"
        val size = 23

        val oldAttachments: MutableList<Fhir4Attachment> = mutableListOf(
            mockk()
        )
        val wrappedOldAttachment: WrapperContract.Attachment = mockk()

        val newAttachments: MutableList<Fhir4Attachment> = mutableListOf(
            mockk()
        )
        val wrappedNewAttachment: WrapperContract.Attachment = mockk()

        val updatedAttachments = listOf<Pair<WrapperContract.Attachment, List<String>>>(
            mockk()
        )

        every { wrappedOldAttachment.id } returns oldAttachmentId

        every { wrappedNewAttachment.id } returns attachmentId
        every { wrappedNewAttachment.hash } returns hash
        every { wrappedNewAttachment.size } returns size

        every { decryptedRecord.resource } returns oldResource
        every { decryptedRecord.attachmentsKey } returns attachmentKey

        every { SdkFhirAttachmentHelper.hasAttachment(oldResource) } returns true
        every { SdkFhirAttachmentHelper.getAttachment(oldResource) } returns oldAttachments as MutableList<Any?>
        every { SdkAttachmentFactory.wrap(oldAttachments[0]) } returns wrappedOldAttachment

        every { SdkFhirAttachmentHelper.getAttachment(newResource) } returns newAttachments as MutableList<Any?>
        every { SdkAttachmentFactory.wrap(newAttachments[0]) } returns wrappedNewAttachment

        every { AttachmentGuardian.guardSize(wrappedNewAttachment) } just Runs
        every {
            AttachmentGuardian.guardIdAgainstExistingIds(wrappedNewAttachment, setOf(oldAttachmentId))
        } just Runs
        every { AttachmentGuardian.guardHash(wrappedNewAttachment) } returns true

        every {
            attachmentService.upload(
                listOf(wrappedNewAttachment),
                attachmentKey,
                USER_ID
            )
        } returns Single.just(updatedAttachments)
        every {
            recordService.updateFhirResourceIdentifier(
                newResource,
                updatedAttachments
            )
        } returns Unit

        // Then
        val record = recordService.updateData(decryptedRecord, newResource, USER_ID)

        // When
        assertSame(
            actual = record,
            expected = decryptedRecord
        )

        verifyOrder {
            AttachmentGuardian.guardSize(wrappedNewAttachment)
            AttachmentGuardian.guardIdAgainstExistingIds(wrappedNewAttachment, setOf(oldAttachmentId))
            AttachmentGuardian.guardHash(wrappedNewAttachment)
            attachmentService.upload(
                listOf(wrappedNewAttachment),
                attachmentKey,
                USER_ID
            )
            recordService.updateFhirResourceIdentifier(newResource, updatedAttachments)
        }

        unmockkObject(AttachmentGuardian)
    }
}
