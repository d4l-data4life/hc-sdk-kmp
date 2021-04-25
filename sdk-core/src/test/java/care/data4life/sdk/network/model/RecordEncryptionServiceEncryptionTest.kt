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

package care.data4life.sdk.network.model

import care.data4life.crypto.GCKey
import care.data4life.crypto.KeyType
import care.data4life.sdk.crypto.CryptoContract
import care.data4life.sdk.data.DataResource
import care.data4life.sdk.fhir.Fhir3Resource
import care.data4life.sdk.fhir.Fhir4Resource
import care.data4life.sdk.fhir.FhirContract
import care.data4life.sdk.network.model.NetworkModelContract.DecryptedBaseRecord
import care.data4life.sdk.tag.Annotations
import care.data4life.sdk.tag.EncryptedTagsAndAnnotations
import care.data4life.sdk.tag.TaggingContract
import care.data4life.sdk.tag.Tags
import care.data4life.sdk.test.util.GenericTestDataProvider.COMMON_KEY_ID
import care.data4life.sdk.wrapper.WrapperContract
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifyOrder
import io.reactivex.Single
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RecordEncryptionServiceEncryptionTest {
    private lateinit var service: NetworkModelContract.EncryptionService
    private val cryptoService: CryptoContract.Service = mockk()
    private val taggingService: TaggingContract.Service = mockk()
    private val tagEncryptionService: TaggingContract.EncryptionService = mockk()
    private val fhirService: FhirContract.Service = mockk()
    private val dateTimeFormatter: WrapperContract.DateTimeFormatter = mockk()
    private val limitGuard: NetworkModelContract.LimitGuard = mockk()

    @Before
    fun setUp() {
        clearAllMocks()

        service = RecordEncryptionService(
            taggingService,
            tagEncryptionService,
            limitGuard,
            cryptoService,
            fhirService,
            dateTimeFormatter
        )
    }

    private inline fun <reified T : Any> encryptionFlow(
        decryptedRecord: DecryptedBaseRecord<T>,
        identifier: String? = null,
        creationDate: String? = null,
        modelVersion: Int = -1,
        tags: Tags = mockk(),
        annotations: Annotations = emptyList(),
        encryptedTagsAndAnnotations: EncryptedTagsAndAnnotations = mockk(),
        commonKey: GCKey = mockk(),
        commonKeyId: String = "does not matter",
        dataKey: GCKey = mockk(),
        attachmentKey: GCKey? = null,
        resource: T = mockk(),
        encryptedResource: String = "what so ever",
        encryptedDataKey: EncryptedKey = mockk(),
        encryptedAttachmentKey: EncryptedKey? = null
    ) {
        every { decryptedRecord.identifier } returns identifier
        every { decryptedRecord.customCreationDate } returns creationDate
        every { decryptedRecord.modelVersion } returns modelVersion

        every { decryptedRecord.tags } returns tags
        every { decryptedRecord.annotations } returns annotations
        every {
            tagEncryptionService.encryptTagsAndAnnotations(tags, annotations)
        } returns encryptedTagsAndAnnotations

        every { cryptoService.fetchCurrentCommonKey() } returns commonKey
        every { cryptoService.currentCommonKeyId } returns commonKeyId

        every { decryptedRecord.dataKey } returns dataKey
        every { decryptedRecord.attachmentsKey } returns attachmentKey
        every { decryptedRecord.resource } returns resource

        every { fhirService._encryptResource(dataKey, resource) } returns encryptedResource

        every {
            cryptoService.encryptSymmetricKey(
                commonKey,
                KeyType.DATA_KEY,
                dataKey
            )
        } returns Single.just(encryptedDataKey)

        if (attachmentKey is GCKey) {
            every {
                cryptoService.encryptSymmetricKey(
                    commonKey,
                    KeyType.ATTACHMENT_KEY,
                    attachmentKey
                )
            } returns Single.just(encryptedAttachmentKey)
        }
    }

    // FHIR3
    @Test
    fun `Given, encrypt is called with a DecryptedRecord, which contains a Fhir3 resource and a UserId, it returns a EncryptedRecord`() {
        // Given
        val decryptedRecord: DecryptedBaseRecord<Fhir3Resource> = mockk()

        encryptionFlow(decryptedRecord)

        // When
        val encryptedRecord: Any = service.encrypt(decryptedRecord)

        // Then
        assertTrue(encryptedRecord is NetworkModelContract.EncryptedRecord)
    }

    @Test
    fun `Given, encrypt is called with a DecryptedRecord, which contains a Fhir3 resource and a UserId, it sets the given Identifier in the EncryptedRecord, while it is null`() {
        // Given
        val decryptedRecord: DecryptedBaseRecord<Fhir3Resource> = mockk()

        encryptionFlow(
            decryptedRecord,
            identifier = null
        )

        // When
        val encryptedRecord = service.encrypt(decryptedRecord)

        // Then
        assertNull(encryptedRecord.identifier)
    }

    @Test
    fun `Given, encrypt is called with a DecryptedRecord, which contains a Fhir3 resource and a UserId, it sets the given Identifier in the EncryptedRecord, while it is not null`() {
        // Given
        val decryptedRecord: DecryptedBaseRecord<Fhir3Resource> = mockk()
        val identifier = "something"

        encryptionFlow(
            decryptedRecord,
            identifier = identifier
        )

        // When
        val encryptedRecord = service.encrypt(decryptedRecord)

        // Then
        assertEquals(
            expected = identifier,
            actual = encryptedRecord.identifier
        )
    }

    @Test
    fun `Given, encrypt is called with a DecryptedRecord, which contains a Fhir3 resource and a UserId, it sets the given CreationDate in the EncryptedRecord, if it is null`() {
        // Given
        val decryptedRecord: DecryptedBaseRecord<Fhir3Resource> = mockk()

        encryptionFlow(
            decryptedRecord,
            creationDate = null
        )

        // When
        val encryptedRecord = service.encrypt(decryptedRecord)

        // Then
        assertNull(encryptedRecord.customCreationDate)
    }

    @Test
    fun `Given, encrypt is called with a DecryptedRecord, which contains a Fhir3 resource and a UserId, it sets the given CreationDate in the EncryptedRecord, while it is not null`() {
        // Given
        val decryptedRecord: DecryptedBaseRecord<Fhir3Resource> = mockk()
        val creationDate = "something"

        encryptionFlow(
            decryptedRecord,
            creationDate = creationDate
        )

        // When
        val encryptedRecord = service.encrypt(decryptedRecord)

        // Then
        assertEquals(
            expected = creationDate,
            actual = encryptedRecord.customCreationDate
        )
    }

    @Test
    fun `Given, encrypt is called with a DecryptedRecord, which contains a Fhir3 resource and a UserId, it sets the given ModelVersion in the EncryptedRecord`() {
        // Given
        val decryptedRecord: DecryptedBaseRecord<Fhir3Resource> = mockk()
        val version = 23

        encryptionFlow(
            decryptedRecord,
            modelVersion = version
        )

        // When
        val encryptedRecord = service.encrypt(decryptedRecord)

        // Then
        assertEquals(
            expected = version,
            actual = encryptedRecord.modelVersion
        )
    }

    @Test
    fun `Given, encrypt is called with a DecryptedRecord, which contains a Fhir3 resource and a UserId, it encrypts the given Tags and Annotation, and includes them in the EncryptedRecord`() {
        // Given
        val decryptedRecord: DecryptedBaseRecord<Fhir3Resource> = mockk()
        val tags: Tags = mockk()
        val annotations: Annotations = mockk()
        val encryptedTagsAndAnnotations: EncryptedTagsAndAnnotations = mockk()

        encryptionFlow(
            decryptedRecord,
            tags = tags,
            annotations = annotations,
            encryptedTagsAndAnnotations = encryptedTagsAndAnnotations
        )

        // When
        val encryptedRecord = service.encrypt(decryptedRecord)

        // Then
        assertEquals(
            actual = encryptedRecord.encryptedTags,
            expected = encryptedTagsAndAnnotations
        )

        verify(exactly = 1) { tagEncryptionService.encryptTagsAndAnnotations(tags, annotations) }
    }

    @Test
    fun `Given, encrypt is called with a DecryptedRecord, which contains a Fhir3 resource and a UserId, it determines the CommonKeyId and includes it in the EncryptedRecord`() {
        // Given
        val decryptedRecord: DecryptedBaseRecord<Fhir3Resource> = mockk()
        val commonKeyId: String = COMMON_KEY_ID

        encryptionFlow(
            decryptedRecord,
            commonKeyId = commonKeyId
        )

        // When
        val encryptedRecord = service.encrypt(decryptedRecord)

        // Then
        assertEquals(
            actual = encryptedRecord.commonKeyId,
            expected = commonKeyId
        )

        verifyOrder {
            cryptoService.fetchCurrentCommonKey()
            cryptoService.currentCommonKeyId
        }
    }

    @Test
    fun `Given, encrypt is called with a DecryptedRecord, which contains a Fhir3 resource and a UserId, it encrypts the resource and includes it in the EncryptedRecord`() {
        // Given
        val decryptedRecord: DecryptedBaseRecord<Fhir3Resource> = mockk()
        val dataKey: GCKey = mockk()
        val resource: Fhir3Resource = mockk()
        val encryptedResource = "secret"

        encryptionFlow(
            decryptedRecord,
            resource = resource,
            dataKey = dataKey,
            encryptedResource = encryptedResource
        )

        // When
        val encryptedRecord = service.encrypt(decryptedRecord)

        // Then
        assertEquals(
            actual = encryptedRecord.encryptedBody,
            expected = encryptedResource
        )

        verify(exactly = 1) { fhirService._encryptResource(dataKey, resource) }
    }

    @Test
    fun `Given, encrypt is called with a DecryptedRecord, which contains a Fhir3 resource and a UserId, it encrypts the dataKey and includes it in the EncryptedRecord`() {
        // Given
        val decryptedRecord: DecryptedBaseRecord<Fhir3Resource> = mockk()
        val dataKey: GCKey = mockk()
        val commonKey: GCKey = mockk()
        val encryptedDataKey: EncryptedKey = mockk()

        encryptionFlow(
            decryptedRecord,
            dataKey = dataKey,
            commonKey = commonKey,
            encryptedDataKey = encryptedDataKey
        )

        // When
        val encryptedRecord = service.encrypt(decryptedRecord)

        // Then
        assertEquals(
            actual = encryptedRecord.encryptedDataKey,
            expected = encryptedDataKey
        )

        verify(exactly = 1) {
            cryptoService.encryptSymmetricKey(
                commonKey,
                KeyType.DATA_KEY,
                dataKey
            )
        }
    }

    @Test
    fun `Given, encrypt is called with a DecryptedRecord, which contains a Fhir3 resource and a UserId, it includes null as EncryptedAttachmentKey, if the AttachmentKey was null`() {
        // Given
        val decryptedRecord: DecryptedBaseRecord<Fhir3Resource> = mockk()
        val commonKey: GCKey = mockk()

        encryptionFlow(
            decryptedRecord,
            attachmentKey = null,
            commonKey = commonKey
        )

        // When
        val encryptedRecord = service.encrypt(decryptedRecord)

        // Then
        assertNull(encryptedRecord.encryptedAttachmentsKey)

        verify(exactly = 0) {
            cryptoService.encryptSymmetricKey(
                commonKey,
                KeyType.ATTACHMENT_KEY,
                any()
            )
        }
    }

    @Test
    fun `Given, encrypt is called with a DecryptedRecord, which contains a Fhir3 resource and a UserId, it encrypts a given AttachmentKey and includes it into the EncryptedRecord`() {
        // Given
        val decryptedRecord: DecryptedBaseRecord<Fhir3Resource> = mockk()
        val commonKey: GCKey = mockk()
        val attachmentKey: GCKey = mockk()
        val encryptedAttachmentKey: EncryptedKey = mockk()

        encryptionFlow(
            decryptedRecord,
            attachmentKey = attachmentKey,
            commonKey = commonKey,
            encryptedAttachmentKey = encryptedAttachmentKey
        )

        // When
        val encryptedRecord = service.encrypt(decryptedRecord)

        // Then
        assertEquals(
            actual = encryptedRecord.encryptedAttachmentsKey,
            expected = encryptedAttachmentKey
        )

        verify(exactly = 1) {
            cryptoService.encryptSymmetricKey(
                commonKey,
                KeyType.ATTACHMENT_KEY,
                attachmentKey
            )
        }
    }

    // FHIR4
    @Test
    fun `Given, encrypt is called with a DecryptedRecord, which contains a Fhir4 resource and a UserId, it returns a EncryptedRecord`() {
        // Given
        val decryptedRecord: DecryptedBaseRecord<Fhir4Resource> = mockk()

        encryptionFlow(decryptedRecord)

        // When
        val encryptedRecord: Any = service.encrypt(decryptedRecord)

        // Then
        assertTrue(encryptedRecord is NetworkModelContract.EncryptedRecord)
    }

    @Test
    fun `Given, encrypt is called with a DecryptedRecord, which contains a Fhir4 resource and a UserId, it sets the given Identifier in the EncryptedRecord, while it is null`() {
        // Given
        val decryptedRecord: DecryptedBaseRecord<Fhir4Resource> = mockk()

        encryptionFlow(
            decryptedRecord,
            identifier = null
        )

        // When
        val encryptedRecord = service.encrypt(decryptedRecord)

        // Then
        assertNull(encryptedRecord.identifier)
    }

    @Test
    fun `Given, encrypt is called with a DecryptedRecord, which contains a Fhir4 resource and a UserId, it sets the given Identifier in the EncryptedRecord, while it is not null`() {
        // Given
        val decryptedRecord: DecryptedBaseRecord<Fhir4Resource> = mockk()
        val identifier = "something"

        encryptionFlow(
            decryptedRecord,
            identifier = identifier
        )

        // When
        val encryptedRecord = service.encrypt(decryptedRecord)

        // Then
        assertEquals(
            expected = identifier,
            actual = encryptedRecord.identifier
        )
    }

    @Test
    fun `Given, encrypt is called with a DecryptedRecord, which contains a Fhir4 resource and a UserId, it sets the given CreationDate in the EncryptedRecord, if it is null`() {
        // Given
        val decryptedRecord: DecryptedBaseRecord<Fhir4Resource> = mockk()

        encryptionFlow(
            decryptedRecord,
            creationDate = null
        )

        // When
        val encryptedRecord = service.encrypt(decryptedRecord)

        // Then
        assertNull(encryptedRecord.customCreationDate)
    }

    @Test
    fun `Given, encrypt is called with a DecryptedRecord, which contains a Fhir4 resource and a UserId, it sets the given CreationDate in the EncryptedRecord, while it is not null`() {
        // Given
        val decryptedRecord: DecryptedBaseRecord<Fhir4Resource> = mockk()
        val creationDate = "something"

        encryptionFlow(
            decryptedRecord,
            creationDate = creationDate
        )

        // When
        val encryptedRecord = service.encrypt(decryptedRecord)

        // Then
        assertEquals(
            expected = creationDate,
            actual = encryptedRecord.customCreationDate
        )
    }

    @Test
    fun `Given, encrypt is called with a DecryptedRecord, which contains a Fhir4 resource and a UserId, it sets the given ModelVersion in the EncryptedRecord`() {
        // Given
        val decryptedRecord: DecryptedBaseRecord<Fhir4Resource> = mockk()
        val version = 23

        encryptionFlow(
            decryptedRecord,
            modelVersion = version
        )

        // When
        val encryptedRecord = service.encrypt(decryptedRecord)

        // Then
        assertEquals(
            expected = version,
            actual = encryptedRecord.modelVersion
        )
    }

    @Test
    fun `Given, encrypt is called with a DecryptedRecord, which contains a Fhir4 resource and a UserId, it encrypts the given Tags and Annotation, and includes them in the EncryptedRecord`() {
        // Given
        val decryptedRecord: DecryptedBaseRecord<Fhir4Resource> = mockk()
        val tags: Tags = mockk()
        val annotations: Annotations = mockk()
        val encryptedTagsAndAnnotations: EncryptedTagsAndAnnotations = mockk()

        encryptionFlow(
            decryptedRecord,
            tags = tags,
            annotations = annotations,
            encryptedTagsAndAnnotations = encryptedTagsAndAnnotations
        )

        // When
        val encryptedRecord = service.encrypt(decryptedRecord)

        // Then
        assertEquals(
            actual = encryptedRecord.encryptedTags,
            expected = encryptedTagsAndAnnotations
        )

        verify(exactly = 1) { tagEncryptionService.encryptTagsAndAnnotations(tags, annotations) }
    }

    @Test
    fun `Given, encrypt is called with a DecryptedRecord, which contains a Fhir4 resource and a UserId, it determines the CommonKeyId and includes it in the EncryptedRecord`() {
        // Given
        val decryptedRecord: DecryptedBaseRecord<Fhir4Resource> = mockk()
        val commonKeyId: String = COMMON_KEY_ID

        encryptionFlow(
            decryptedRecord,
            commonKeyId = commonKeyId
        )

        // When
        val encryptedRecord = service.encrypt(decryptedRecord)

        // Then
        assertEquals(
            actual = encryptedRecord.commonKeyId,
            expected = commonKeyId
        )

        verifyOrder {
            cryptoService.fetchCurrentCommonKey()
            cryptoService.currentCommonKeyId
        }
    }

    @Test
    fun `Given, encrypt is called with a DecryptedRecord, which contains a Fhir4 resource and a UserId, it encrypts the resource and includes it in the EncryptedRecord`() {
        // Given
        val decryptedRecord: DecryptedBaseRecord<Fhir4Resource> = mockk()
        val dataKey: GCKey = mockk()
        val resource: Fhir4Resource = mockk()
        val encryptedResource = "secret"

        encryptionFlow(
            decryptedRecord,
            resource = resource,
            dataKey = dataKey,
            encryptedResource = encryptedResource
        )

        // When
        val encryptedRecord = service.encrypt(decryptedRecord)

        // Then
        assertEquals(
            actual = encryptedRecord.encryptedBody,
            expected = encryptedResource
        )

        verify(exactly = 1) { fhirService._encryptResource(dataKey, resource) }
    }

    @Test
    fun `Given, encrypt is called with a DecryptedRecord, which contains a Fhir4 resource and a UserId, it encrypts the dataKey and includes it in the EncryptedRecord`() {
        // Given
        val decryptedRecord: DecryptedBaseRecord<Fhir4Resource> = mockk()
        val dataKey: GCKey = mockk()
        val commonKey: GCKey = mockk()
        val encryptedDataKey: EncryptedKey = mockk()

        encryptionFlow(
            decryptedRecord,
            dataKey = dataKey,
            commonKey = commonKey,
            encryptedDataKey = encryptedDataKey
        )

        // When
        val encryptedRecord = service.encrypt(decryptedRecord)

        // Then
        assertEquals(
            actual = encryptedRecord.encryptedDataKey,
            expected = encryptedDataKey
        )

        verify(exactly = 1) {
            cryptoService.encryptSymmetricKey(
                commonKey,
                KeyType.DATA_KEY,
                dataKey
            )
        }
    }

    @Test
    fun `Given, encrypt is called with a DecryptedRecord, which contains a Fhir4 resource and a UserId, it includes null as EncryptedAttachmentKey, if the AttachmentKey was null`() {
        // Given
        val decryptedRecord: DecryptedBaseRecord<Fhir4Resource> = mockk()
        val commonKey: GCKey = mockk()

        encryptionFlow(
            decryptedRecord,
            attachmentKey = null,
            commonKey = commonKey
        )

        // When
        val encryptedRecord = service.encrypt(decryptedRecord)

        // Then
        assertNull(encryptedRecord.encryptedAttachmentsKey)

        verify(exactly = 0) {
            cryptoService.encryptSymmetricKey(
                commonKey,
                KeyType.ATTACHMENT_KEY,
                any()
            )
        }
    }

    @Test
    fun `Given, encrypt is called with a DecryptedRecord, which contains a Fhir4 resource and a UserId, it encrypts a given AttachmentKey and includes it into the EncryptedRecord`() {
        // Given
        val decryptedRecord: DecryptedBaseRecord<Fhir4Resource> = mockk()
        val commonKey: GCKey = mockk()
        val attachmentKey: GCKey = mockk()
        val encryptedAttachmentKey: EncryptedKey = mockk()

        encryptionFlow(
            decryptedRecord,
            attachmentKey = attachmentKey,
            commonKey = commonKey,
            encryptedAttachmentKey = encryptedAttachmentKey
        )

        // When
        val encryptedRecord = service.encrypt(decryptedRecord)

        // Then
        assertEquals(
            actual = encryptedRecord.encryptedAttachmentsKey,
            expected = encryptedAttachmentKey
        )

        verify(exactly = 1) {
            cryptoService.encryptSymmetricKey(
                commonKey,
                KeyType.ATTACHMENT_KEY,
                attachmentKey
            )
        }
    }

    // Arbitrary Data
    @Test
    fun `Given, encrypt is called with a DecryptedRecord, which contains a DataResource and a UserId, it returns a EncryptedRecord`() {
        // Given
        val decryptedRecord: DecryptedBaseRecord<DataResource> = mockk()

        encryptionFlow(decryptedRecord)

        // When
        val encryptedRecord: Any = service.encrypt(decryptedRecord)

        // Then
        assertTrue(encryptedRecord is NetworkModelContract.EncryptedRecord)
    }

    @Test
    fun `Given, encrypt is called with a DecryptedRecord, which contains a DataResource and a UserId, it sets the given Identifier in the EncryptedRecord, while it is null`() {
        // Given
        val decryptedRecord: DecryptedBaseRecord<DataResource> = mockk()

        encryptionFlow(
            decryptedRecord,
            identifier = null
        )

        // When
        val encryptedRecord = service.encrypt(decryptedRecord)

        // Then
        assertNull(encryptedRecord.identifier)
    }

    @Test
    fun `Given, encrypt is called with a DecryptedRecord, which contains a DataResource and a UserId, it sets the given Identifier in the EncryptedRecord, while it is not null`() {
        // Given
        val decryptedRecord: DecryptedBaseRecord<DataResource> = mockk()
        val identifier = "something"

        encryptionFlow(
            decryptedRecord,
            identifier = identifier
        )

        // When
        val encryptedRecord = service.encrypt(decryptedRecord)

        // Then
        assertEquals(
            expected = identifier,
            actual = encryptedRecord.identifier
        )
    }

    @Test
    fun `Given, encrypt is called with a DecryptedRecord, which contains a DataResource and a UserId, it sets the given CreationDate in the EncryptedRecord, if it is null`() {
        // Given
        val decryptedRecord: DecryptedBaseRecord<DataResource> = mockk()

        encryptionFlow(
            decryptedRecord,
            creationDate = null
        )

        // When
        val encryptedRecord = service.encrypt(decryptedRecord)

        // Then
        assertNull(encryptedRecord.customCreationDate)
    }

    @Test
    fun `Given, encrypt is called with a DecryptedRecord, which contains a DataResource and a UserId, it sets the given CreationDate in the EncryptedRecord, while it is not null`() {
        // Given
        val decryptedRecord: DecryptedBaseRecord<DataResource> = mockk()
        val creationDate = "something"

        encryptionFlow(
            decryptedRecord,
            creationDate = creationDate
        )

        // When
        val encryptedRecord = service.encrypt(decryptedRecord)

        // Then
        assertEquals(
            expected = creationDate,
            actual = encryptedRecord.customCreationDate
        )
    }

    @Test
    fun `Given, encrypt is called with a DecryptedRecord, which contains a DataResource and a UserId, it sets the given ModelVersion in the EncryptedRecord`() {
        // Given
        val decryptedRecord: DecryptedBaseRecord<DataResource> = mockk()
        val version = 23

        encryptionFlow(
            decryptedRecord,
            modelVersion = version
        )

        // When
        val encryptedRecord = service.encrypt(decryptedRecord)

        // Then
        assertEquals(
            expected = version,
            actual = encryptedRecord.modelVersion
        )
    }

    @Test
    fun `Given, encrypt is called with a DecryptedRecord, which contains a DataResource and a UserId, it encrypts the given Tags and Annotation, and includes them in the EncryptedRecord`() {
        // Given
        val decryptedRecord: DecryptedBaseRecord<DataResource> = mockk()
        val tags: Tags = mockk()
        val annotations: Annotations = mockk()
        val encryptedTagsAndAnnotations: EncryptedTagsAndAnnotations = mockk()

        encryptionFlow(
            decryptedRecord,
            tags = tags,
            annotations = annotations,
            encryptedTagsAndAnnotations = encryptedTagsAndAnnotations
        )

        // When
        val encryptedRecord = service.encrypt(decryptedRecord)

        // Then
        assertEquals(
            actual = encryptedRecord.encryptedTags,
            expected = encryptedTagsAndAnnotations
        )

        verify(exactly = 1) { tagEncryptionService.encryptTagsAndAnnotations(tags, annotations) }
    }

    @Test
    fun `Given, encrypt is called with a DecryptedRecord, which contains a DataResource and a UserId, it determines the CommonKeyId and includes it in the EncryptedRecord`() {
        // Given
        val decryptedRecord: DecryptedBaseRecord<DataResource> = mockk()
        val commonKeyId: String = COMMON_KEY_ID

        encryptionFlow(
            decryptedRecord,
            commonKeyId = commonKeyId
        )

        // When
        val encryptedRecord = service.encrypt(decryptedRecord)

        // Then
        assertEquals(
            actual = encryptedRecord.commonKeyId,
            expected = commonKeyId
        )

        verifyOrder {
            cryptoService.fetchCurrentCommonKey()
            cryptoService.currentCommonKeyId
        }
    }

    @Test
    fun `Given, encrypt is called with a DecryptedRecord, which contains a DataResource and a UserId, it encrypts the resource and includes it in the EncryptedRecord`() {
        // Given
        val decryptedRecord: DecryptedBaseRecord<DataResource> = mockk()
        val dataKey: GCKey = mockk()
        val resource: DataResource = mockk()
        val encryptedResource = "secret"

        encryptionFlow(
            decryptedRecord,
            resource = resource,
            dataKey = dataKey,
            encryptedResource = encryptedResource
        )

        // When
        val encryptedRecord = service.encrypt(decryptedRecord)

        // Then
        assertEquals(
            actual = encryptedRecord.encryptedBody,
            expected = encryptedResource
        )

        verify(exactly = 1) { fhirService._encryptResource(dataKey, resource) }
    }

    @Test
    fun `Given, encrypt is called with a DecryptedRecord, which contains a DataResource and a UserId, it encrypts the dataKey and includes it in the EncryptedRecord`() {
        // Given
        val decryptedRecord: DecryptedBaseRecord<DataResource> = mockk()
        val dataKey: GCKey = mockk()
        val commonKey: GCKey = mockk()
        val encryptedDataKey: EncryptedKey = mockk()

        encryptionFlow(
            decryptedRecord,
            dataKey = dataKey,
            commonKey = commonKey,
            encryptedDataKey = encryptedDataKey
        )

        // When
        val encryptedRecord = service.encrypt(decryptedRecord)

        // Then
        assertEquals(
            actual = encryptedRecord.encryptedDataKey,
            expected = encryptedDataKey
        )

        verify(exactly = 1) {
            cryptoService.encryptSymmetricKey(
                commonKey,
                KeyType.DATA_KEY,
                dataKey
            )
        }
    }

    @Test
    fun `Given, encrypt is called with a DecryptedRecord, which contains a DataResource and a UserId, it includes null as EncryptedAttachmentKey, if the AttachmentKey was null`() {
        // Given
        val decryptedRecord: DecryptedBaseRecord<DataResource> = mockk()
        val commonKey: GCKey = mockk()

        encryptionFlow(
            decryptedRecord,
            attachmentKey = null,
            commonKey = commonKey
        )

        // When
        val encryptedRecord = service.encrypt(decryptedRecord)

        // Then
        assertNull(encryptedRecord.encryptedAttachmentsKey)

        verify(exactly = 0) {
            cryptoService.encryptSymmetricKey(
                commonKey,
                KeyType.ATTACHMENT_KEY,
                any()
            )
        }
    }

    @Test
    fun `Given, encrypt is called with a DecryptedRecord, which contains a DataResource and a UserId, it encrypts a given AttachmentKey and includes it into the EncryptedRecord`() {
        // Given
        val decryptedRecord: DecryptedBaseRecord<DataResource> = mockk()
        val commonKey: GCKey = mockk()
        val attachmentKey: GCKey = mockk()
        val encryptedAttachmentKey: EncryptedKey = mockk()

        encryptionFlow(
            decryptedRecord,
            attachmentKey = attachmentKey,
            commonKey = commonKey,
            encryptedAttachmentKey = encryptedAttachmentKey
        )

        // When
        val encryptedRecord = service.encrypt(decryptedRecord)

        // Then
        assertEquals(
            actual = encryptedRecord.encryptedAttachmentsKey,
            expected = encryptedAttachmentKey
        )

        verify(exactly = 1) {
            cryptoService.encryptSymmetricKey(
                commonKey,
                KeyType.ATTACHMENT_KEY,
                attachmentKey
            )
        }
    }
}
