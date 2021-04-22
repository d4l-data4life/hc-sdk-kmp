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
import care.data4life.sdk.crypto.CryptoContract
import care.data4life.sdk.data.DataResource
import care.data4life.sdk.fhir.Fhir3Attachment
import care.data4life.sdk.fhir.Fhir3Resource
import care.data4life.sdk.fhir.Fhir4Attachment
import care.data4life.sdk.fhir.Fhir4Resource
import care.data4life.sdk.fhir.FhirContract
import care.data4life.sdk.lang.CoreRuntimeException
import care.data4life.sdk.lang.DataValidationException
import care.data4life.sdk.network.model.definitions.DecryptedBaseRecord
import care.data4life.sdk.tag.TaggingContract
import care.data4life.sdk.test.util.GenericTestDataProvider.ALIAS
import care.data4life.sdk.test.util.GenericTestDataProvider.PARTNER_ID
import care.data4life.sdk.test.util.GenericTestDataProvider.USER_ID
import care.data4life.sdk.wrapper.SdkAttachmentFactory
import care.data4life.sdk.wrapper.SdkFhirAttachmentHelper
import care.data4life.sdk.wrapper.WrapperContract
import io.mockk.Called
import io.mockk.clearAllMocks
import io.mockk.every
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
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame

class RecordServiceAttachmentUpdateTest {
    private lateinit var recordService: RecordService
    private val apiService: ApiService = mockk()
    private val cryptoService: CryptoContract.Service = mockk()
    private val fhirService: FhirContract.Service = mockk()
    private val tagEncryptionService: TaggingContract.EncryptionService = mockk()
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
    fun `Given, updateData is called with a DecryptedRecord, which contains a Fhir3Resource, a Fhir3 resource and a UserId, it fails, if the new attachment has no hash`() {
        // Given
        val oldResource: Fhir3Resource = mockk()
        val newResource: Fhir3Resource = mockk()
        val decryptedRecord: DecryptedBaseRecord<Any> = mockk()

        val newAttachments: MutableList<Fhir3Attachment> = mutableListOf(
            mockk()
        )
        val wrappedNewAttachment: WrapperContract.Attachment = spyk()

        every { wrappedNewAttachment.hash } returns null

        every { decryptedRecord.resource } returns oldResource
        every { SdkFhirAttachmentHelper.hasAttachment(oldResource) } returns true
        every { SdkFhirAttachmentHelper.getAttachment(oldResource) } returns null
        every { SdkFhirAttachmentHelper.getAttachment(newResource) } returns newAttachments as MutableList<Any?>
        every { SdkAttachmentFactory.wrap(newAttachments[0]) } returns wrappedNewAttachment

        // Then
        val exception = assertFailsWith<DataValidationException.ExpectedFieldViolation> {
            // When
            recordService.updateData(decryptedRecord, newResource, USER_ID)
        }

        assertEquals(
            actual = exception.message,
            expected = "Attachment.hash and Attachment.size expected"
        )
    }

    @Test
    fun `Given, updateData is called with a DecryptedRecord, which contains a Fhir3Resource, a Fhir3 resource and a UserId, it fails, if the new attachment has no size`() {
        // Given
        val oldResource: Fhir3Resource = mockk()
        val newResource: Fhir3Resource = mockk()
        val decryptedRecord: DecryptedBaseRecord<Any> = mockk()

        val newAttachments: MutableList<Fhir3Attachment> = mutableListOf(
            mockk()
        )
        val wrappedNewAttachment: WrapperContract.Attachment = spyk()

        every { wrappedNewAttachment.hash } returns "hash"
        every { wrappedNewAttachment.size } returns null

        every { decryptedRecord.resource } returns oldResource
        every { SdkFhirAttachmentHelper.hasAttachment(oldResource) } returns true
        every { SdkFhirAttachmentHelper.getAttachment(oldResource) } returns null
        every { SdkFhirAttachmentHelper.getAttachment(newResource) } returns newAttachments as MutableList<Any?>
        every { SdkAttachmentFactory.wrap(newAttachments[0]) } returns wrappedNewAttachment

        // Then
        val exception = assertFailsWith<DataValidationException.ExpectedFieldViolation> {
            // When
            recordService.updateData(decryptedRecord, newResource, USER_ID)
        }

        assertEquals(
            actual = exception.message,
            expected = "Attachment.hash and Attachment.size expected"
        )
    }

    @Test
    fun `Given, updateData is called with a DecryptedRecord, which contains a Fhir3Resource, a Fhir3 resource and a UserId, it fails, if the new attachment has no valid hash`() {
        // Given
        val oldResource: Fhir3Resource = mockk()
        val newResource: Fhir3Resource = mockk()
        val decryptedRecord: DecryptedBaseRecord<Any> = mockk()

        val newAttachments: MutableList<Fhir3Attachment> = mutableListOf(
            mockk()
        )
        val wrappedNewAttachment: WrapperContract.Attachment = spyk()

        every { wrappedNewAttachment.hash } returns "hash"
        every { wrappedNewAttachment.size } returns 42

        every { decryptedRecord.resource } returns oldResource
        every { SdkFhirAttachmentHelper.hasAttachment(oldResource) } returns true
        every { SdkFhirAttachmentHelper.getAttachment(oldResource) } returns null
        every { SdkFhirAttachmentHelper.getAttachment(newResource) } returns newAttachments as MutableList<Any?>
        every { SdkAttachmentFactory.wrap(newAttachments[0]) } returns wrappedNewAttachment
        every { recordService.getValidHash(wrappedNewAttachment) } returns "not what you expect"

        // Then
        val exception = assertFailsWith<DataValidationException.InvalidAttachmentPayloadHash> {
            // When
            recordService.updateData(decryptedRecord, newResource, USER_ID)
        }

        assertEquals(
            actual = exception.message,
            expected = "Attachment.hash is not valid"
        )
    }

    @Test
    fun `Given, updateData is called with a DecryptedRecord, which contains a Fhir3Resource, a Fhir3 resource and a UserId, it fails, due to id mismatch of old and new attachments`() {
        // Given
        val oldResource: Fhir3Resource = mockk()
        val newResource: Fhir3Resource = mockk()
        val decryptedRecord: DecryptedBaseRecord<Any> = mockk()

        val oldAttachments: MutableList<Fhir3Attachment> = mutableListOf(
            mockk()
        )
        val wrappedOldAttachment: WrapperContract.Attachment = spyk()

        val newAttachments: MutableList<Fhir3Attachment> = mutableListOf(
            mockk()
        )
        val wrappedNewAttachment: WrapperContract.Attachment = spyk()
        val hash = "hash"

        every { wrappedOldAttachment.id } returns "oldId"

        every { wrappedNewAttachment.id } returns "newId"
        every { wrappedNewAttachment.hash } returns hash
        every { wrappedNewAttachment.size } returns 42

        every { decryptedRecord.resource } returns oldResource
        every { SdkFhirAttachmentHelper.hasAttachment(oldResource) } returns true
        every { SdkFhirAttachmentHelper.getAttachment(oldResource) } returns oldAttachments as MutableList<Any?>
        every { SdkAttachmentFactory.wrap(oldAttachments[0]) } returns wrappedOldAttachment

        every { SdkFhirAttachmentHelper.getAttachment(newResource) } returns newAttachments as MutableList<Any?>
        every { SdkAttachmentFactory.wrap(newAttachments[0]) } returns wrappedNewAttachment
        every { recordService.getValidHash(wrappedNewAttachment) } returns hash

        // Then
        val exception = assertFailsWith<DataValidationException.IdUsageViolation> {
            // When
            recordService.updateData(decryptedRecord, newResource, USER_ID)
        }

        assertEquals(
            actual = exception.message,
            expected = "Valid Attachment.id expected"
        )
    }

    @Test
    fun `Given, updateData is called with a DecryptedRecord, which contains a Fhir3Resource, a Fhir3 resource and a UserId, it uploads it`() {
        // Given
        val oldResource: Fhir3Resource = mockk()
        val newResource: Fhir3Resource = mockk()
        val decryptedRecord: DecryptedBaseRecord<Any> = mockk()
        val attachmentKey: GCKey = mockk()
        val hash = "hash"

        val newAttachments: MutableList<Fhir3Attachment> = mutableListOf(
            mockk()
        )
        val wrappedNewAttachment: WrapperContract.Attachment = spyk()

        val updatedAttachments = listOf<Pair<WrapperContract.Attachment, List<String>>>(
            mockk()
        )

        every { wrappedNewAttachment.hash } returns hash
        every { wrappedNewAttachment.size } returns 42

        every { decryptedRecord.resource } returns oldResource
        every { decryptedRecord.attachmentsKey } returns attachmentKey

        every { SdkFhirAttachmentHelper.hasAttachment(oldResource) } returns true
        every { SdkFhirAttachmentHelper.getAttachment(oldResource) } returns null
        every { SdkFhirAttachmentHelper.getAttachment(newResource) } returns newAttachments as MutableList<Any?>
        every { SdkAttachmentFactory.wrap(newAttachments[0]) } returns wrappedNewAttachment
        every { recordService.getValidHash(wrappedNewAttachment) } returns hash

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
            recordService.getValidHash(wrappedNewAttachment)
            attachmentService.upload(
                listOf(wrappedNewAttachment),
                attachmentKey,
                USER_ID
            )
            recordService.updateFhirResourceIdentifier(newResource, updatedAttachments)
        }
    }

    @Test
    fun `Given, updateData is called with a DecryptedRecord, which contains a Fhir3Resource, a Fhir3 resource and a UserId, it ignores Attachments, which are null`() {
        // Given
        val oldResource: Fhir3Resource = mockk()
        val newResource: Fhir3Resource = mockk()
        val decryptedRecord: DecryptedBaseRecord<Any> = mockk()
        val attachmentKey: GCKey = mockk()
        val hash = "hash"

        val newAttachments: MutableList<Fhir3Attachment?> = mutableListOf(
            null,
            mockk()
        )
        val wrappedNewAttachment: WrapperContract.Attachment = spyk()

        val updatedAttachments = listOf<Pair<WrapperContract.Attachment, List<String>>>(
            mockk()
        )

        every { wrappedNewAttachment.hash } returns hash
        every { wrappedNewAttachment.size } returns 42

        every { decryptedRecord.resource } returns oldResource
        every { decryptedRecord.attachmentsKey } returns attachmentKey

        every { SdkFhirAttachmentHelper.hasAttachment(oldResource) } returns true
        every { SdkFhirAttachmentHelper.getAttachment(oldResource) } returns null
        every { SdkFhirAttachmentHelper.getAttachment(newResource) } returns newAttachments as MutableList<Any?>
        every { SdkAttachmentFactory.wrap(newAttachments[1]!!) } returns wrappedNewAttachment
        every { recordService.getValidHash(wrappedNewAttachment) } returns hash

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
            recordService.getValidHash(wrappedNewAttachment)
            attachmentService.upload(
                listOf(wrappedNewAttachment),
                attachmentKey,
                USER_ID
            )
            recordService.updateFhirResourceIdentifier(newResource, updatedAttachments)
        }
    }

    @Test
    fun `Given, updateData is called with a DecryptedRecord, which contains a Fhir3Resource, a Fhir3 resource and a UserId, it uploads it, while resolving the AttachmentKey`() {
        // Given
        val oldResource: Fhir3Resource = mockk()
        val newResource: Fhir3Resource = mockk()
        val decryptedRecord: DecryptedBaseRecord<Any> = mockk()
        val attachmentKey: GCKey = mockk()
        val hash = "hash"

        val newAttachments: MutableList<Fhir3Attachment> = mutableListOf(
            mockk()
        )
        val wrappedNewAttachment: WrapperContract.Attachment = spyk()

        val updatedAttachments = listOf<Pair<WrapperContract.Attachment, List<String>>>(
            mockk()
        )

        every { wrappedNewAttachment.hash } returns hash
        every { wrappedNewAttachment.size } returns 42

        every { decryptedRecord.resource } returns oldResource
        every { decryptedRecord.attachmentsKey } returnsMany listOf(null, attachmentKey)

        every { SdkFhirAttachmentHelper.hasAttachment(oldResource) } returns true
        every { SdkFhirAttachmentHelper.getAttachment(oldResource) } returns null
        every { SdkFhirAttachmentHelper.getAttachment(newResource) } returns newAttachments as MutableList<Any?>
        every { SdkAttachmentFactory.wrap(newAttachments[0]) } returns wrappedNewAttachment
        every { recordService.getValidHash(wrappedNewAttachment) } returns hash

        every { cryptoService.generateGCKey() } returns Single.just(attachmentKey)
        every { decryptedRecord.attachmentsKey = attachmentKey } returns Unit

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
            recordService.getValidHash(wrappedNewAttachment)
            cryptoService.generateGCKey()
            decryptedRecord.attachmentsKey = attachmentKey
            attachmentService.upload(
                listOf(wrappedNewAttachment),
                attachmentKey,
                USER_ID
            )
            recordService.updateFhirResourceIdentifier(newResource, updatedAttachments)
        }
    }

    @Test
    fun `Given, updateData is called with a DecryptedRecord, which contains a Fhir3Resource, a Fhir3 resource and a UserId, it will not it, if the old and new attachment hash matches`() {
        // Given
        val oldResource: Fhir3Resource = mockk()
        val newResource: Fhir3Resource = mockk()
        val decryptedRecord: DecryptedBaseRecord<Any> = mockk()
        val attachmentKey: GCKey = mockk()
        val hash = "hash"
        val id = "id"

        val oldAttachments: MutableList<Fhir3Attachment> = mutableListOf(
            mockk()
        )
        val wrappedOldAttachment: WrapperContract.Attachment = spyk()

        val newAttachments: MutableList<Fhir3Attachment> = mutableListOf(
            mockk()
        )
        val wrappedNewAttachment: WrapperContract.Attachment = spyk()

        val updatedAttachments = listOf<Pair<WrapperContract.Attachment, List<String>>>(
            mockk()
        )

        every { wrappedOldAttachment.id } returns id
        every { wrappedOldAttachment.hash } returns hash

        every { wrappedNewAttachment.id } returns id
        every { wrappedNewAttachment.hash } returns hash
        every { wrappedNewAttachment.size } returns 42

        every { decryptedRecord.resource } returns oldResource
        every { decryptedRecord.attachmentsKey } returns attachmentKey

        every { SdkFhirAttachmentHelper.hasAttachment(oldResource) } returns true
        every { SdkFhirAttachmentHelper.getAttachment(oldResource) } returns null

        every { SdkFhirAttachmentHelper.getAttachment(oldResource) } returns oldAttachments as MutableList<Any?>
        every { SdkAttachmentFactory.wrap(oldAttachments[0]) } returns wrappedOldAttachment

        every { SdkFhirAttachmentHelper.getAttachment(newResource) } returns newAttachments as MutableList<Any?>
        every { SdkAttachmentFactory.wrap(newAttachments[0]) } returns wrappedNewAttachment

        every { recordService.getValidHash(wrappedNewAttachment) } returns hash

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
            recordService.getValidHash(wrappedNewAttachment)
        }
        verify { attachmentService.upload(any(), attachmentKey, USER_ID) wasNot Called }
    }

    @Test
    fun `Given, updateData is called with a DecryptedRecord, which contains a Fhir3Resource, a Fhir3 resource and a UserId, it uploads it, while the old attachments have no hash`() {
        // Given
        val oldResource: Fhir3Resource = mockk()
        val newResource: Fhir3Resource = mockk()
        val decryptedRecord: DecryptedBaseRecord<Any> = mockk()
        val attachmentKey: GCKey = mockk()
        val hash = "hash"
        val id = "id"

        val oldAttachments: MutableList<Fhir3Attachment> = mutableListOf(
            mockk()
        )
        val wrappedOldAttachment: WrapperContract.Attachment = spyk()

        val newAttachments: MutableList<Fhir3Attachment> = mutableListOf(
            mockk()
        )
        val wrappedNewAttachment: WrapperContract.Attachment = spyk()

        val updatedAttachments = listOf<Pair<WrapperContract.Attachment, List<String>>>(
            mockk()
        )

        every { wrappedOldAttachment.id } returns id
        every { wrappedOldAttachment.hash } returns null

        every { wrappedNewAttachment.id } returns id
        every { wrappedNewAttachment.hash } returns hash
        every { wrappedNewAttachment.size } returns 42

        every { decryptedRecord.resource } returns oldResource
        every { decryptedRecord.attachmentsKey } returns attachmentKey

        every { SdkFhirAttachmentHelper.hasAttachment(oldResource) } returns true
        every { SdkFhirAttachmentHelper.getAttachment(oldResource) } returns null

        every { SdkFhirAttachmentHelper.getAttachment(oldResource) } returns oldAttachments as MutableList<Any?>
        every { SdkAttachmentFactory.wrap(oldAttachments[0]) } returns wrappedOldAttachment

        every { SdkFhirAttachmentHelper.getAttachment(newResource) } returns newAttachments as MutableList<Any?>
        every { SdkAttachmentFactory.wrap(newAttachments[0]) } returns wrappedNewAttachment

        every { recordService.getValidHash(wrappedNewAttachment) } returns hash

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
            recordService.getValidHash(wrappedNewAttachment)
            attachmentService.upload(
                listOf(wrappedNewAttachment),
                attachmentKey,
                USER_ID
            )
            recordService.updateFhirResourceIdentifier(newResource, updatedAttachments)
        }
    }

    @Test
    fun `Given, updateData is called with a DecryptedRecord, which contains a Fhir3Resource, a Fhir3 resource and a UserId, it uploads it, while the old and new attachment do not match`() {
        // Given
        val oldResource: Fhir3Resource = mockk()
        val newResource: Fhir3Resource = mockk()
        val decryptedRecord: DecryptedBaseRecord<Any> = mockk()
        val attachmentKey: GCKey = mockk()
        val hash = "hash"
        val id = "id"

        val oldAttachments: MutableList<Fhir3Attachment> = mutableListOf(
            mockk()
        )
        val wrappedOldAttachment: WrapperContract.Attachment = spyk()

        val newAttachments: MutableList<Fhir3Attachment> = mutableListOf(
            mockk()
        )
        val wrappedNewAttachment: WrapperContract.Attachment = spyk()

        val updatedAttachments = listOf<Pair<WrapperContract.Attachment, List<String>>>(
            mockk()
        )

        every { wrappedOldAttachment.id } returns id
        every { wrappedOldAttachment.hash } returns "oldHash"

        every { wrappedNewAttachment.id } returns id
        every { wrappedNewAttachment.hash } returns hash
        every { wrappedNewAttachment.size } returns 42

        every { decryptedRecord.resource } returns oldResource
        every { decryptedRecord.attachmentsKey } returns attachmentKey

        every { SdkFhirAttachmentHelper.hasAttachment(oldResource) } returns true
        every { SdkFhirAttachmentHelper.getAttachment(oldResource) } returns null

        every { SdkFhirAttachmentHelper.getAttachment(oldResource) } returns oldAttachments as MutableList<Any?>
        every { SdkAttachmentFactory.wrap(oldAttachments[0]) } returns wrappedOldAttachment

        every { SdkFhirAttachmentHelper.getAttachment(newResource) } returns newAttachments as MutableList<Any?>
        every { SdkAttachmentFactory.wrap(newAttachments[0]) } returns wrappedNewAttachment

        every { recordService.getValidHash(wrappedNewAttachment) } returns hash

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
            recordService.getValidHash(wrappedNewAttachment)
            attachmentService.upload(
                listOf(wrappedNewAttachment),
                attachmentKey,
                USER_ID
            )
            recordService.updateFhirResourceIdentifier(newResource, updatedAttachments)
        }
    }

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
    fun `Given, updateData is called with a DecryptedRecord, which contains a Fhir4Resource, a Fhir4 resource and a UserId, it fails, if the new attachment has no hash`() {
        // Given
        val oldResource: Fhir4Resource = mockk()
        val newResource: Fhir4Resource = mockk()
        val decryptedRecord: DecryptedBaseRecord<Any> = mockk()

        val newAttachments: MutableList<Fhir4Attachment> = mutableListOf(
            mockk()
        )
        val wrappedNewAttachment: WrapperContract.Attachment = spyk()

        every { wrappedNewAttachment.hash } returns null

        every { decryptedRecord.resource } returns oldResource
        every { SdkFhirAttachmentHelper.hasAttachment(oldResource) } returns true
        every { SdkFhirAttachmentHelper.getAttachment(oldResource) } returns null
        every { SdkFhirAttachmentHelper.getAttachment(newResource) } returns newAttachments as MutableList<Any?>
        every { SdkAttachmentFactory.wrap(newAttachments[0]) } returns wrappedNewAttachment

        // Then
        val exception = assertFailsWith<DataValidationException.ExpectedFieldViolation> {
            // When
            recordService.updateData(decryptedRecord, newResource, USER_ID)
        }

        assertEquals(
            actual = exception.message,
            expected = "Attachment.hash and Attachment.size expected"
        )
    }

    @Test
    fun `Given, updateData is called with a DecryptedRecord, which contains a Fhir4Resource, a Fhir4 resource and a UserId, it fails, if the new attachment has no size`() {
        // Given
        val oldResource: Fhir4Resource = mockk()
        val newResource: Fhir4Resource = mockk()
        val decryptedRecord: DecryptedBaseRecord<Any> = mockk()

        val newAttachments: MutableList<Fhir4Attachment> = mutableListOf(
            mockk()
        )
        val wrappedNewAttachment: WrapperContract.Attachment = spyk()

        every { wrappedNewAttachment.hash } returns "hash"
        every { wrappedNewAttachment.size } returns null

        every { decryptedRecord.resource } returns oldResource
        every { SdkFhirAttachmentHelper.hasAttachment(oldResource) } returns true
        every { SdkFhirAttachmentHelper.getAttachment(oldResource) } returns null
        every { SdkFhirAttachmentHelper.getAttachment(newResource) } returns newAttachments as MutableList<Any?>
        every { SdkAttachmentFactory.wrap(newAttachments[0]) } returns wrappedNewAttachment

        // Then
        val exception = assertFailsWith<DataValidationException.ExpectedFieldViolation> {
            // When
            recordService.updateData(decryptedRecord, newResource, USER_ID)
        }

        assertEquals(
            actual = exception.message,
            expected = "Attachment.hash and Attachment.size expected"
        )
    }

    @Test
    fun `Given, updateData is called with a DecryptedRecord, which contains a Fhir4Resource, a Fhir4 resource and a UserId, it fails, if the new attachment has no valid hash`() {
        // Given
        val oldResource: Fhir4Resource = mockk()
        val newResource: Fhir4Resource = mockk()
        val decryptedRecord: DecryptedBaseRecord<Any> = mockk()

        val newAttachments: MutableList<Fhir4Attachment> = mutableListOf(
            mockk()
        )
        val wrappedNewAttachment: WrapperContract.Attachment = spyk()

        every { wrappedNewAttachment.hash } returns "hash"
        every { wrappedNewAttachment.size } returns 42

        every { decryptedRecord.resource } returns oldResource
        every { SdkFhirAttachmentHelper.hasAttachment(oldResource) } returns true
        every { SdkFhirAttachmentHelper.getAttachment(oldResource) } returns null
        every { SdkFhirAttachmentHelper.getAttachment(newResource) } returns newAttachments as MutableList<Any?>
        every { SdkAttachmentFactory.wrap(newAttachments[0]) } returns wrappedNewAttachment
        every { recordService.getValidHash(wrappedNewAttachment) } returns "not what you expect"

        // Then
        val exception = assertFailsWith<DataValidationException.InvalidAttachmentPayloadHash> {
            // When
            recordService.updateData(decryptedRecord, newResource, USER_ID)
        }

        assertEquals(
            actual = exception.message,
            expected = "Attachment.hash is not valid"
        )
    }

    @Test
    fun `Given, updateData is called with a DecryptedRecord, which contains a Fhir4Resource, a Fhir4 resource and a UserId, it fails, due to id mismatch of old and new attachments`() {
        // Given
        val oldResource: Fhir4Resource = mockk()
        val newResource: Fhir4Resource = mockk()
        val decryptedRecord: DecryptedBaseRecord<Any> = mockk()

        val oldAttachments: MutableList<Fhir4Attachment> = mutableListOf(
            mockk()
        )
        val wrappedOldAttachment: WrapperContract.Attachment = spyk()

        val newAttachments: MutableList<Fhir4Attachment> = mutableListOf(
            mockk()
        )
        val wrappedNewAttachment: WrapperContract.Attachment = spyk()
        val hash = "hash"

        every { wrappedOldAttachment.id } returns "oldId"

        every { wrappedNewAttachment.id } returns "newId"
        every { wrappedNewAttachment.hash } returns hash
        every { wrappedNewAttachment.size } returns 42

        every { decryptedRecord.resource } returns oldResource
        every { SdkFhirAttachmentHelper.hasAttachment(oldResource) } returns true
        every { SdkFhirAttachmentHelper.getAttachment(oldResource) } returns oldAttachments as MutableList<Any?>
        every { SdkAttachmentFactory.wrap(oldAttachments[0]) } returns wrappedOldAttachment

        every { SdkFhirAttachmentHelper.getAttachment(newResource) } returns newAttachments as MutableList<Any?>
        every { SdkAttachmentFactory.wrap(newAttachments[0]) } returns wrappedNewAttachment
        every { recordService.getValidHash(wrappedNewAttachment) } returns hash

        // Then
        val exception = assertFailsWith<DataValidationException.IdUsageViolation> {
            // When
            recordService.updateData(decryptedRecord, newResource, USER_ID)
        }

        assertEquals(
            actual = exception.message,
            expected = "Valid Attachment.id expected"
        )
    }

    @Test
    fun `Given, updateData is called with a DecryptedRecord, which contains a Fhir4Resource, a Fhir4 resource and a UserId, it uploads it`() {
        // Given
        val oldResource: Fhir4Resource = mockk()
        val newResource: Fhir4Resource = mockk()
        val decryptedRecord: DecryptedBaseRecord<Any> = mockk()
        val attachmentKey: GCKey = mockk()
        val hash = "hash"

        val newAttachments: MutableList<Fhir4Attachment> = mutableListOf(
            mockk()
        )
        val wrappedNewAttachment: WrapperContract.Attachment = spyk()

        val updatedAttachments = listOf<Pair<WrapperContract.Attachment, List<String>>>(
            mockk()
        )

        every { wrappedNewAttachment.hash } returns hash
        every { wrappedNewAttachment.size } returns 42

        every { decryptedRecord.resource } returns oldResource
        every { decryptedRecord.attachmentsKey } returns attachmentKey

        every { SdkFhirAttachmentHelper.hasAttachment(oldResource) } returns true
        every { SdkFhirAttachmentHelper.getAttachment(oldResource) } returns null
        every { SdkFhirAttachmentHelper.getAttachment(newResource) } returns newAttachments as MutableList<Any?>
        every { SdkAttachmentFactory.wrap(newAttachments[0]) } returns wrappedNewAttachment
        every { recordService.getValidHash(wrappedNewAttachment) } returns hash

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
            recordService.getValidHash(wrappedNewAttachment)
            attachmentService.upload(
                listOf(wrappedNewAttachment),
                attachmentKey,
                USER_ID
            )
            recordService.updateFhirResourceIdentifier(newResource, updatedAttachments)
        }
    }

    @Test
    fun `Given, updateData is called with a DecryptedRecord, which contains a Fhir4Resource, a Fhir4 resource and a UserId, it ignores Attachments, which are null`() {
        // Given
        val oldResource: Fhir4Resource = mockk()
        val newResource: Fhir4Resource = mockk()
        val decryptedRecord: DecryptedBaseRecord<Any> = mockk()
        val attachmentKey: GCKey = mockk()
        val hash = "hash"

        val newAttachments: MutableList<Fhir4Attachment?> = mutableListOf(
            null,
            mockk()
        )
        val wrappedNewAttachment: WrapperContract.Attachment = spyk()

        val updatedAttachments = listOf<Pair<WrapperContract.Attachment, List<String>>>(
            mockk()
        )

        every { wrappedNewAttachment.hash } returns hash
        every { wrappedNewAttachment.size } returns 42

        every { decryptedRecord.resource } returns oldResource
        every { decryptedRecord.attachmentsKey } returns attachmentKey

        every { SdkFhirAttachmentHelper.hasAttachment(oldResource) } returns true
        every { SdkFhirAttachmentHelper.getAttachment(oldResource) } returns null
        every { SdkFhirAttachmentHelper.getAttachment(newResource) } returns newAttachments as MutableList<Any?>
        every { SdkAttachmentFactory.wrap(newAttachments[1]!!) } returns wrappedNewAttachment
        every { recordService.getValidHash(wrappedNewAttachment) } returns hash

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
            recordService.getValidHash(wrappedNewAttachment)
            attachmentService.upload(
                listOf(wrappedNewAttachment),
                attachmentKey,
                USER_ID
            )
            recordService.updateFhirResourceIdentifier(newResource, updatedAttachments)
        }
    }

    @Test
    fun `Given, updateData is called with a DecryptedRecord, which contains a Fhir4Resource, a Fhir4 resource and a UserId, it uploads it, while resolving the AttachmentKey`() {
        // Given
        val oldResource: Fhir4Resource = mockk()
        val newResource: Fhir4Resource = mockk()
        val decryptedRecord: DecryptedBaseRecord<Any> = mockk()
        val attachmentKey: GCKey = mockk()
        val hash = "hash"

        val newAttachments: MutableList<Fhir4Attachment> = mutableListOf(
            mockk()
        )
        val wrappedNewAttachment: WrapperContract.Attachment = spyk()

        val updatedAttachments = listOf<Pair<WrapperContract.Attachment, List<String>>>(
            mockk()
        )

        every { wrappedNewAttachment.hash } returns hash
        every { wrappedNewAttachment.size } returns 42

        every { decryptedRecord.resource } returns oldResource
        every { decryptedRecord.attachmentsKey } returnsMany listOf(null, attachmentKey)

        every { SdkFhirAttachmentHelper.hasAttachment(oldResource) } returns true
        every { SdkFhirAttachmentHelper.getAttachment(oldResource) } returns null
        every { SdkFhirAttachmentHelper.getAttachment(newResource) } returns newAttachments as MutableList<Any?>
        every { SdkAttachmentFactory.wrap(newAttachments[0]) } returns wrappedNewAttachment
        every { recordService.getValidHash(wrappedNewAttachment) } returns hash

        every { cryptoService.generateGCKey() } returns Single.just(attachmentKey)
        every { decryptedRecord.attachmentsKey = attachmentKey } returns Unit

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
            recordService.getValidHash(wrappedNewAttachment)
            cryptoService.generateGCKey()
            decryptedRecord.attachmentsKey = attachmentKey
            attachmentService.upload(
                listOf(wrappedNewAttachment),
                attachmentKey,
                USER_ID
            )
            recordService.updateFhirResourceIdentifier(newResource, updatedAttachments)
        }
    }

    @Test
    fun `Given, updateData is called with a DecryptedRecord, which contains a Fhir4Resource, a Fhir4 resource and a UserId, it will not it, if the old and new attachment hash matches`() {
        // Given
        val oldResource: Fhir4Resource = mockk()
        val newResource: Fhir4Resource = mockk()
        val decryptedRecord: DecryptedBaseRecord<Any> = mockk()
        val attachmentKey: GCKey = mockk()
        val hash = "hash"
        val id = "id"

        val oldAttachments: MutableList<Fhir4Attachment> = mutableListOf(
            mockk()
        )
        val wrappedOldAttachment: WrapperContract.Attachment = spyk()

        val newAttachments: MutableList<Fhir4Attachment> = mutableListOf(
            mockk()
        )
        val wrappedNewAttachment: WrapperContract.Attachment = spyk()

        val updatedAttachments = listOf<Pair<WrapperContract.Attachment, List<String>>>(
            mockk()
        )

        every { wrappedOldAttachment.id } returns id
        every { wrappedOldAttachment.hash } returns hash

        every { wrappedNewAttachment.id } returns id
        every { wrappedNewAttachment.hash } returns hash
        every { wrappedNewAttachment.size } returns 42

        every { decryptedRecord.resource } returns oldResource
        every { decryptedRecord.attachmentsKey } returns attachmentKey

        every { SdkFhirAttachmentHelper.hasAttachment(oldResource) } returns true
        every { SdkFhirAttachmentHelper.getAttachment(oldResource) } returns null

        every { SdkFhirAttachmentHelper.getAttachment(oldResource) } returns oldAttachments as MutableList<Any?>
        every { SdkAttachmentFactory.wrap(oldAttachments[0]) } returns wrappedOldAttachment

        every { SdkFhirAttachmentHelper.getAttachment(newResource) } returns newAttachments as MutableList<Any?>
        every { SdkAttachmentFactory.wrap(newAttachments[0]) } returns wrappedNewAttachment

        every { recordService.getValidHash(wrappedNewAttachment) } returns hash

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
            recordService.getValidHash(wrappedNewAttachment)
        }
        verify { attachmentService.upload(any(), attachmentKey, USER_ID) wasNot Called }
    }

    @Test
    fun `Given, updateData is called with a DecryptedRecord, which contains a Fhir4Resource, a Fhir4 resource and a UserId, it uploads it, while the old attachments have no hash`() {
        // Given
        val oldResource: Fhir4Resource = mockk()
        val newResource: Fhir4Resource = mockk()
        val decryptedRecord: DecryptedBaseRecord<Any> = mockk()
        val attachmentKey: GCKey = mockk()
        val hash = "hash"
        val id = "id"

        val oldAttachments: MutableList<Fhir4Attachment> = mutableListOf(
            mockk()
        )
        val wrappedOldAttachment: WrapperContract.Attachment = spyk()

        val newAttachments: MutableList<Fhir4Attachment> = mutableListOf(
            mockk()
        )
        val wrappedNewAttachment: WrapperContract.Attachment = spyk()

        val updatedAttachments = listOf<Pair<WrapperContract.Attachment, List<String>>>(
            mockk()
        )

        every { wrappedOldAttachment.id } returns id
        every { wrappedOldAttachment.hash } returns null

        every { wrappedNewAttachment.id } returns id
        every { wrappedNewAttachment.hash } returns hash
        every { wrappedNewAttachment.size } returns 42

        every { decryptedRecord.resource } returns oldResource
        every { decryptedRecord.attachmentsKey } returns attachmentKey

        every { SdkFhirAttachmentHelper.hasAttachment(oldResource) } returns true
        every { SdkFhirAttachmentHelper.getAttachment(oldResource) } returns null

        every { SdkFhirAttachmentHelper.getAttachment(oldResource) } returns oldAttachments as MutableList<Any?>
        every { SdkAttachmentFactory.wrap(oldAttachments[0]) } returns wrappedOldAttachment

        every { SdkFhirAttachmentHelper.getAttachment(newResource) } returns newAttachments as MutableList<Any?>
        every { SdkAttachmentFactory.wrap(newAttachments[0]) } returns wrappedNewAttachment

        every { recordService.getValidHash(wrappedNewAttachment) } returns hash

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
            recordService.getValidHash(wrappedNewAttachment)
            attachmentService.upload(
                listOf(wrappedNewAttachment),
                attachmentKey,
                USER_ID
            )
            recordService.updateFhirResourceIdentifier(newResource, updatedAttachments)
        }
    }

    @Test
    fun `Given, updateData is called with a DecryptedRecord, which contains a Fhir4Resource, a Fhir4 resource and a UserId, it uploads it, while the old and new attachment do not match`() {
        // Given
        val oldResource: Fhir4Resource = mockk()
        val newResource: Fhir4Resource = mockk()
        val decryptedRecord: DecryptedBaseRecord<Any> = mockk()
        val attachmentKey: GCKey = mockk()
        val hash = "hash"
        val id = "id"

        val oldAttachments: MutableList<Fhir4Attachment> = mutableListOf(
            mockk()
        )
        val wrappedOldAttachment: WrapperContract.Attachment = spyk()

        val newAttachments: MutableList<Fhir4Attachment> = mutableListOf(
            mockk()
        )
        val wrappedNewAttachment: WrapperContract.Attachment = spyk()

        val updatedAttachments = listOf<Pair<WrapperContract.Attachment, List<String>>>(
            mockk()
        )

        every { wrappedOldAttachment.id } returns id
        every { wrappedOldAttachment.hash } returns "oldHash"

        every { wrappedNewAttachment.id } returns id
        every { wrappedNewAttachment.hash } returns hash
        every { wrappedNewAttachment.size } returns 42

        every { decryptedRecord.resource } returns oldResource
        every { decryptedRecord.attachmentsKey } returns attachmentKey

        every { SdkFhirAttachmentHelper.hasAttachment(oldResource) } returns true
        every { SdkFhirAttachmentHelper.getAttachment(oldResource) } returns null

        every { SdkFhirAttachmentHelper.getAttachment(oldResource) } returns oldAttachments as MutableList<Any?>
        every { SdkAttachmentFactory.wrap(oldAttachments[0]) } returns wrappedOldAttachment

        every { SdkFhirAttachmentHelper.getAttachment(newResource) } returns newAttachments as MutableList<Any?>
        every { SdkAttachmentFactory.wrap(newAttachments[0]) } returns wrappedNewAttachment

        every { recordService.getValidHash(wrappedNewAttachment) } returns hash

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
            recordService.getValidHash(wrappedNewAttachment)
            attachmentService.upload(
                listOf(wrappedNewAttachment),
                attachmentKey,
                USER_ID
            )
            recordService.updateFhirResourceIdentifier(newResource, updatedAttachments)
        }
    }
}
