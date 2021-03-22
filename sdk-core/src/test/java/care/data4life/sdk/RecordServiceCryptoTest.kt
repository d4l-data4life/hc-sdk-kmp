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
import care.data4life.crypto.KeyType
import care.data4life.sdk.attachment.AttachmentContract
import care.data4life.sdk.data.DataResource
import care.data4life.sdk.fhir.Fhir3Resource
import care.data4life.sdk.fhir.Fhir4Resource
import care.data4life.sdk.fhir.FhirContract
import care.data4life.sdk.lang.DataValidationException
import care.data4life.sdk.network.model.DecryptedDataRecord
import care.data4life.sdk.network.model.DecryptedR4Record
import care.data4life.sdk.network.model.DecryptedRecord
import care.data4life.sdk.network.model.EncryptedKey
import care.data4life.sdk.network.model.NetworkModelContract
import care.data4life.sdk.network.model.definitions.DecryptedBaseRecord
import care.data4life.sdk.network.model.definitions.DecryptedFhir3Record
import care.data4life.sdk.network.model.definitions.DecryptedFhir4Record
import care.data4life.sdk.tag.TaggingContract
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import io.reactivex.Single
import org.junit.Before
import org.junit.Test
import java.io.IOException
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull


class RecordServiceCryptoTest {
    private lateinit var recordService: RecordService
    private lateinit var apiService: ApiService
    private lateinit var cryptoService: CryptoService
    private lateinit var fhirService: FhirContract.Service
    private lateinit var tagEncryptionService: TaggingContract.EncryptionService
    private lateinit var taggingService: TaggingContract.Service
    private lateinit var attachmentService: AttachmentContract.Service
    private lateinit var errorHandler: SdkContract.ErrorHandler
    private lateinit var tags: HashMap<String, String>
    private lateinit var annotations: List<String>

    private lateinit var encryptedTagsAndAnnotations: List<String>
    private lateinit var dataKey: GCKey
    private lateinit var commonKey: GCKey
    private lateinit var attachmentKey: GCKey
    private lateinit var encryptedResource: String
    private lateinit var encryptedDataKey: EncryptedKey
    private lateinit var encryptedAttachmentKey: EncryptedKey

    @Before
    fun setUp() {
        apiService = mockk()
        cryptoService = mockk()
        fhirService = mockk()
        tagEncryptionService = mockk()
        taggingService = mockk()
        attachmentService = mockk()
        errorHandler = mockk()
        tags = hashMapOf("potato" to "soup", "resourcetype" to "pumpkin")
        annotations = listOf("tomato", "soup")

        dataKey = mockk()
        commonKey = mockk()
        attachmentKey = mockk()

        encryptedTagsAndAnnotations = mockk()
        encryptedResource = "potato"
        encryptedDataKey = mockk()
        encryptedAttachmentKey = mockk()

        recordService = spyk(
                RecordService(
                        RecordServiceTestBase.PARTNER_ID,
                        RecordServiceTestBase.ALIAS,
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
    }

    fun <T: Any> encryptRecordFlow(
            decryptedRecord: DecryptedBaseRecord<T>,
            currentCommonKeyId: String,
            resource: T,
            attachmentKey: GCKey? = null
    ) {
        every { decryptedRecord.tags } returns tags
        every { decryptedRecord.annotations } returns annotations
        every { decryptedRecord.dataKey } returns dataKey
        every { decryptedRecord.resource } returns resource
        every { decryptedRecord.attachmentsKey } returns attachmentKey

        every {
            tagEncryptionService.encryptTagsAndAnnotations(tags, annotations)
        } returns encryptedTagsAndAnnotations
        every { fhirService._encryptResource(dataKey, resource) } returns encryptedResource
        every { cryptoService.fetchCurrentCommonKey() } returns commonKey
        every { cryptoService.currentCommonKeyId } returns currentCommonKeyId
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

    fun <T: Any> verifyEncryptRecordFlow(
            resource: T,
            attachmentKey: GCKey? = null
    ) {
        verify(exactly = 1) { tagEncryptionService.encryptTagsAndAnnotations(tags, annotations) }
        verify(exactly = 1) { fhirService._encryptResource(dataKey, resource) }
        verify(exactly = 1) { cryptoService.fetchCurrentCommonKey() }
        verify(exactly = 1) { cryptoService.currentCommonKeyId }
        verify(exactly = 1) {
            cryptoService.encryptSymmetricKey(
                    commonKey,
                    KeyType.DATA_KEY,
                    dataKey
            )
        }

        if (attachmentKey is GCKey) {
            verify(exactly = 1) {
                cryptoService.encryptSymmetricKey(
                        commonKey,
                        KeyType.ATTACHMENT_KEY,
                        attachmentKey
                )
            }
        }
    }

    @Test
    @Throws(IOException::class)
    fun `Given, encryptRecord is called with a DecryptedRecord for Fhir3, it returns a EncryptedRecord`() {
        // Given
        val currentCommonKeyId = "currentCommonKeyId"
        val resource: Fhir3Resource = mockk()
        val decryptedRecord: DecryptedFhir3Record<Fhir3Resource> = mockk(relaxed = true)

        encryptRecordFlow(
                decryptedRecord,
                currentCommonKeyId,
                resource
        )

        // When
        val encryptedRecord = recordService.encryptRecord(decryptedRecord)

        // Then
        assertEquals(
                expected = currentCommonKeyId,
                actual = encryptedRecord.commonKeyId
        )
        assertNull(encryptedRecord.encryptedAttachmentsKey)
        verifyEncryptRecordFlow(resource)

    }

    @Test
    @Throws(IOException::class)
    fun `Given, encryptRecord is called with a DecryptedRecord for Fhir4, it returns a EncryptedRecord`() {
        // Given
        val currentCommonKeyId = "currentCommonKeyId"
        val resource: Fhir4Resource = mockk()
        val decryptedRecord: DecryptedFhir4Record<Fhir4Resource> = mockk(relaxed = true)

        encryptRecordFlow(
                decryptedRecord,
                currentCommonKeyId,
                resource
        )

        // When
        val encryptedRecord = recordService.encryptRecord(decryptedRecord)

        // Then
        assertEquals(
                expected = currentCommonKeyId,
                actual = encryptedRecord.commonKeyId
        )
        assertNull(encryptedRecord.encryptedAttachmentsKey)

        verifyEncryptRecordFlow(resource)
    }

    @Test
    @Throws(IOException::class)
    fun `Given, encryptRecord is called with a DecryptedRecord for arbitrary data, it returns a EncryptedRecord`() {
        // Given
        val currentCommonKeyId = "currentCommonKeyId"
        val resource: DataResource = mockk()
        val decryptedRecord: DecryptedDataRecord = mockk(relaxed = true)

        encryptRecordFlow(
                decryptedRecord,
                currentCommonKeyId,
                resource
        )

        // When
        val encryptedRecord = recordService.encryptRecord(decryptedRecord)

        // Then
        assertEquals(
                expected = currentCommonKeyId,
                actual = encryptedRecord.commonKeyId
        )
        assertNull(encryptedRecord.encryptedAttachmentsKey)

        verifyEncryptRecordFlow(resource)
    }

    @Test
    @Throws(IOException::class)
    fun `Given, encryptRecord is called with a DecryptedRecord for Fhir3, it adds a encrypted AttachmentKey, if the DecryptedRecord contains a AttachmentKey`() {
        // Given
        val currentCommonKeyId = "currentCommonKeyId"
        val resource: Fhir3Resource = mockk()
        val decryptedRecord: DecryptedFhir3Record<Fhir3Resource> = mockk(relaxed = true)

        encryptRecordFlow(
                decryptedRecord,
                currentCommonKeyId,
                resource,
                attachmentKey
        )

        // When
        val encryptedRecord = recordService.encryptRecord(decryptedRecord)

        // Then
        assertEquals(
                expected = currentCommonKeyId,
                actual = encryptedRecord.commonKeyId
        )
        assertEquals(
                expected = encryptedAttachmentKey,
                actual = encryptedRecord.encryptedAttachmentsKey
        )

        verifyEncryptRecordFlow(resource, attachmentKey)
    }

    @Test
    @Throws(IOException::class)
    fun `Given, encryptRecord is called with a DecryptedRecord for Fhir4, it adds a encrypted AttachmentKey, if the DecryptedRecord contains a AttachmentKey`() {
        // Given
        val currentCommonKeyId = "currentCommonKeyId"
        val resource: Fhir4Resource = mockk()
        val decryptedRecord: DecryptedFhir4Record<Fhir4Resource> = mockk(relaxed = true)

        encryptRecordFlow(
                decryptedRecord,
                currentCommonKeyId,
                resource,
                attachmentKey
        )

        // When
        val encryptedRecord = recordService.encryptRecord(decryptedRecord)

        // Then
        assertEquals(
                expected = currentCommonKeyId,
                actual = encryptedRecord.commonKeyId
        )
        assertEquals(
                expected = encryptedAttachmentKey,
                actual = encryptedRecord.encryptedAttachmentsKey
        )

        verifyEncryptRecordFlow(resource, attachmentKey)
    }

    @Test
    @Throws(IOException::class)
    fun `Given, encryptRecord is called with a DecryptedRecord for arbitrary data, it adds a encrypted AttachmentKey, if the DecryptedRecord contains a AttachmentKey`() {
        // Given
        val currentCommonKeyId = "currentCommonKeyId"
        val resource: Fhir4Resource = mockk()
        val decryptedRecord: DecryptedFhir4Record<Fhir4Resource> = mockk(relaxed = true)

        encryptRecordFlow(
                decryptedRecord,
                currentCommonKeyId,
                resource,
                attachmentKey
        )

        // When
        val encryptedRecord = recordService.encryptRecord(decryptedRecord)

        // Then
        assertEquals(
                expected = currentCommonKeyId,
                actual = encryptedRecord.commonKeyId
        )
        assertEquals(
                expected = encryptedAttachmentKey,
                actual = encryptedRecord.encryptedAttachmentsKey
        )

        verifyEncryptRecordFlow(resource, attachmentKey)
    }

    private fun <T: Any> decryptRecordFlow(
            encryptedRecord: NetworkModelContract.EncryptedRecord,
            modelVersion: Int,
            commonKeyId: String,
            resource: T,
            updateDate: String? = null,
            encryptedAttachmentsKey: NetworkModelContract.EncryptedKey? = null
    ) {
        every { encryptedRecord.modelVersion } returns modelVersion
        every { encryptedRecord.commonKeyId } returns commonKeyId
        every { encryptedRecord.encryptedTags } returns encryptedTagsAndAnnotations
        every { encryptedRecord.encryptedDataKey } returns encryptedDataKey
        every { encryptedRecord.encryptedBody } returns encryptedResource
        every { encryptedRecord.customCreationDate } returns RecordServiceTestBase.CREATION_DATE
        every { encryptedRecord.identifier } returns RecordServiceTestBase.RECORD_ID
        every { encryptedRecord.updatedDate } returns updateDate
        every { encryptedRecord.encryptedAttachmentsKey } returns encryptedAttachmentsKey

        every {
            tagEncryptionService.decryptTagsAndAnnotations(encryptedTagsAndAnnotations)
        } returns Pair(tags, annotations)
        every { cryptoService.hasCommonKey(commonKeyId) } returns true
        every { cryptoService.getCommonKeyById(commonKeyId) } returns commonKey
        every {
            cryptoService.symDecryptSymmetricKey(commonKey, encryptedDataKey)
        } returns Single.just(dataKey)

        if (encryptedAttachmentsKey is NetworkModelContract.EncryptedKey) {
            every {
                cryptoService.symDecryptSymmetricKey(commonKey, encryptedAttachmentKey)
            } returns Single.just(attachmentKey)
        }

        every {
            fhirService.decryptResource<T>(
                    dataKey,
                    tags,
                    encryptedResource
            )
        } returns resource
    }

    private fun <T: Any> verfiyDecryptRecordFlow(
            commonKeyId: String,
            encryptedAttachmentsKey: NetworkModelContract.EncryptedKey? = null
    ) {
        verify(exactly = 1) {
            tagEncryptionService.decryptTagsAndAnnotations(encryptedTagsAndAnnotations)
        }
        verify(exactly = 1) { cryptoService.hasCommonKey(commonKeyId) }
        verify(exactly = 1) { cryptoService.getCommonKeyById(commonKeyId) }
        verify(exactly = 1) { cryptoService.symDecryptSymmetricKey(commonKey, encryptedDataKey) }
        if (encryptedAttachmentsKey is NetworkModelContract.EncryptedKey) {
            verify(exactly = 1) {
                cryptoService.symDecryptSymmetricKey(commonKey, encryptedAttachmentKey)
            }
        }
        verify(exactly = 1) {
            fhirService.decryptResource<T>(
                    dataKey,
                    tags,
                    encryptedResource
            )
        }
    }

    @Test
    @Throws(IOException::class, DataValidationException.ModelVersionNotSupported::class)
    fun `Given, decryptRecord is called with a EncryptedRecord and UserId, it returns a DecryptedRecord for Fhir3`() {
        // Given
        val commonKeyId = "mockCommonKeyId"
        val modelVersion = 1
        val resource: Fhir3Resource = mockk()
        val encryptedRecord: NetworkModelContract.EncryptedRecord = mockk()

        decryptRecordFlow(
                encryptedRecord,
                modelVersion,
                commonKeyId,
                resource
        )

        // When
        val decrypted = recordService.decryptRecord<Fhir3Resource>(
                encryptedRecord,
                RecordServiceTestBase.USER_ID
        )

        // Then
        assertEquals(
                actual = decrypted,
                expected = DecryptedRecord(
                        RecordServiceTestBase.RECORD_ID,
                        resource,
                        tags,
                        annotations,
                        RecordServiceTestBase.CREATION_DATE,
                        null,
                        dataKey,
                        null,
                        modelVersion
                )
        )

        verfiyDecryptRecordFlow<Fhir3Resource>(commonKeyId)
    }

    @Test
    @Throws(IOException::class, DataValidationException.ModelVersionNotSupported::class)
    fun `Given, decryptRecord is called with a EncryptedRecord and UserId, it returns a DecryptedRecord for Fhir4`() {
        // Given
        val commonKeyId = "mockCommonKeyId"
        val modelVersion = 1
        val resource: Fhir4Resource = mockk()
        val encryptedRecord: NetworkModelContract.EncryptedRecord = mockk()

        decryptRecordFlow(
                encryptedRecord,
                modelVersion,
                commonKeyId,
                resource
        )

        // When
        val decrypted = recordService.decryptRecord<Fhir4Resource>(
                encryptedRecord,
                RecordServiceTestBase.USER_ID
        )

        // Then
        assertEquals(
                actual = decrypted,
                expected = DecryptedR4Record(
                        RecordServiceTestBase.RECORD_ID,
                        resource,
                        tags,
                        annotations,
                        RecordServiceTestBase.CREATION_DATE,
                        null,
                        dataKey,
                        null,
                        modelVersion
                )
        )

        verfiyDecryptRecordFlow<Fhir3Resource>(commonKeyId)
    }

    @Test
    @Throws(IOException::class, DataValidationException.ModelVersionNotSupported::class)
    fun `Given, decryptRecord is called with a EncryptedRecord and UserId, it returns a DecryptedRecord for arbitrary data`() {
        // Given
        val commonKeyId = "mockCommonKeyId"
        val modelVersion = 1
        val resource: DataResource = mockk()
        val plainResource = ByteArray(23)
        val encryptedRecord: NetworkModelContract.EncryptedRecord = mockk()

        every { resource.asByteArray() } returns plainResource

        decryptRecordFlow(
                encryptedRecord,
                modelVersion,
                commonKeyId,
                resource
        )

        // When
        val decrypted = recordService.decryptRecord<DataResource>(
                encryptedRecord,
                RecordServiceTestBase.USER_ID
        )

        // Then
        assertEquals(
                actual = decrypted,
                expected = DecryptedDataRecord(
                        RecordServiceTestBase.RECORD_ID,
                        resource,
                        tags,
                        annotations,
                        RecordServiceTestBase.CREATION_DATE,
                        null,
                        dataKey,
                        modelVersion
                )
        )

        verfiyDecryptRecordFlow<Fhir3Resource>(commonKeyId)
    }

    @Test
    @Throws(IOException::class, DataValidationException.ModelVersionNotSupported::class)
    fun `Given, decryptRecord is called with a EncryptedRecord and UserId, which contains a UpdateDate, it returns a DecryptedRecord for Fhir3`() {
        // Given
        val commonKeyId = "mockCommonKeyId"
        val modelVersion = 1
        val updateDate = "2020-05-03T07:45:08.234123"
        val resource: Fhir3Resource = mockk()
        val encryptedRecord: NetworkModelContract.EncryptedRecord = mockk()

        decryptRecordFlow(
                encryptedRecord,
                modelVersion,
                commonKeyId,
                resource,
                updateDate = updateDate
        )

        // When
        val decrypted = recordService.decryptRecord<Fhir3Resource>(
                encryptedRecord,
                RecordServiceTestBase.USER_ID
        )

        // Then
        assertEquals(
                actual = decrypted,
                expected = DecryptedRecord(
                        RecordServiceTestBase.RECORD_ID,
                        resource,
                        tags,
                        annotations,
                        RecordServiceTestBase.CREATION_DATE,
                        updateDate,
                        dataKey,
                        null,
                        modelVersion
                )
        )

        verfiyDecryptRecordFlow<Fhir3Resource>(commonKeyId)
    }

    @Test
    @Throws(IOException::class, DataValidationException.ModelVersionNotSupported::class)
    fun `Given, decryptRecord is called with a EncryptedRecord and UserId, which contains a UpdateDate, it returns a DecryptedRecord for Fhir4`() {
        // Given
        val commonKeyId = "mockCommonKeyId"
        val modelVersion = 1
        val updateDate = "2020-05-03T07:45:08.234123"
        val resource: Fhir4Resource = mockk()
        val encryptedRecord: NetworkModelContract.EncryptedRecord = mockk()

        decryptRecordFlow(
                encryptedRecord,
                modelVersion,
                commonKeyId,
                resource,
                updateDate = updateDate
        )

        // When
        val decrypted = recordService.decryptRecord<Fhir4Resource>(
                encryptedRecord,
                RecordServiceTestBase.USER_ID
        )

        // Then
        assertEquals(
                actual = decrypted,
                expected = DecryptedR4Record(
                        RecordServiceTestBase.RECORD_ID,
                        resource,
                        tags,
                        annotations,
                        RecordServiceTestBase.CREATION_DATE,
                        updateDate,
                        dataKey,
                        null,
                        modelVersion
                )
        )

        verfiyDecryptRecordFlow<Fhir4Resource>(commonKeyId)
    }

    @Test
    @Throws(IOException::class, DataValidationException.ModelVersionNotSupported::class)
    fun `Given, decryptRecord is called with a EncryptedRecord and UserId, which contains a UpdateDate, it returns a DecryptedRecord for arbitrary data`() {
        // Given
        val commonKeyId = "mockCommonKeyId"
        val modelVersion = 1
        val updateDate = "2020-05-03T07:45:08.234123"
        val resource: DataResource = mockk()
        val plainResource = ByteArray(23)
        val encryptedRecord: NetworkModelContract.EncryptedRecord = mockk()

        every { resource.asByteArray() } returns plainResource

        decryptRecordFlow(
                encryptedRecord,
                modelVersion,
                commonKeyId,
                resource,
                updateDate = updateDate
        )

        // When
        val decrypted = recordService.decryptRecord<DataResource>(
                encryptedRecord,
                RecordServiceTestBase.USER_ID
        )

        // Then
        assertEquals(
                actual = decrypted,
                expected = DecryptedDataRecord(
                        RecordServiceTestBase.RECORD_ID,
                        resource,
                        tags,
                        annotations,
                        RecordServiceTestBase.CREATION_DATE,
                        updateDate,
                        dataKey,
                        modelVersion
                )
        )

        verfiyDecryptRecordFlow<DataResource>(commonKeyId)
    }

    @Test
    @Throws(IOException::class)
    fun `Given, decryptRecord is called with a EncryptedRecord and UserId, it throws an error, if the ModelVersion is not supported, for Fhir3`() {
        // Given
        val modelVersion = 1
        val encryptedRecord: NetworkModelContract.EncryptedRecord = mockk()

        every { encryptedRecord.modelVersion } returns modelVersion+23

        // When
        val exception = assertFailsWith<DataValidationException.ModelVersionNotSupported> {
            recordService.decryptRecord<Fhir3Resource>(
                    encryptedRecord,
                    RecordServiceTestBase.USER_ID
            )
        }
        assertEquals(
                actual = exception.message,
                expected = "Please update SDK to latest version!"
        )
    }

    @Test
    @Throws(IOException::class)
    fun `Given, decryptRecord is called with a EncryptedRecord and UserId, it throws an error, if the ModelVersion is not supported, for Fhir4`() {
        // Given
        val modelVersion = 1
        val encryptedRecord: NetworkModelContract.EncryptedRecord = mockk()

        every { encryptedRecord.modelVersion } returns modelVersion+23

        // When
        val exception = assertFailsWith<DataValidationException.ModelVersionNotSupported> {
            recordService.decryptRecord<Fhir4Resource>(
                    encryptedRecord,
                    RecordServiceTestBase.USER_ID
            )
        }
        assertEquals(
                actual = exception.message,
                expected = "Please update SDK to latest version!"
        )
    }

    @Test
    @Throws(IOException::class)
    fun `Given, decryptRecord is called with a EncryptedRecord and UserId, it throws an error, if the ModelVersion is not supported, for arbitrary data`() {
        // Given
        val modelVersion = 1
        val encryptedRecord: NetworkModelContract.EncryptedRecord = mockk()

        every { encryptedRecord.modelVersion } returns modelVersion+23

        // When
        val exception = assertFailsWith<DataValidationException.ModelVersionNotSupported> {
            recordService.decryptRecord<Fhir4Resource>(
                    encryptedRecord,
                    RecordServiceTestBase.USER_ID
            )
        }
        assertEquals(
                actual = exception.message,
                expected = "Please update SDK to latest version!"
        )
    }

    @Test
    @Throws(IOException::class, DataValidationException.ModelVersionNotSupported::class)
    fun `Given, decryptRecord is called with a EncryptedRecord and UserId, it adds a decrypted AttachmentKey, if the EncryptedRecord contains a encrypted AttachmentKey, for Fhir3`() {
        // Given
        val commonKeyId = "mockCommonKeyId"
        val modelVersion = 1
        val resource: Fhir3Resource = mockk()
        val encryptedRecord: NetworkModelContract.EncryptedRecord = mockk()

        decryptRecordFlow(
                encryptedRecord,
                modelVersion,
                commonKeyId,
                resource,
                encryptedAttachmentsKey = encryptedAttachmentKey
        )

        // When
        val decrypted = recordService.decryptRecord<Fhir3Resource>(
                encryptedRecord,
                RecordServiceTestBase.USER_ID
        )

        // Then
        assertEquals(
                actual = decrypted,
                expected = DecryptedRecord(
                        RecordServiceTestBase.RECORD_ID,
                        resource,
                        tags,
                        annotations,
                        RecordServiceTestBase.CREATION_DATE,
                        null,
                        dataKey,
                        attachmentKey,
                        modelVersion
                )
        )

        verfiyDecryptRecordFlow<Fhir3Resource>(
                commonKeyId,
                encryptedAttachmentsKey = encryptedAttachmentKey
        )
    }

    @Test
    @Throws(IOException::class, DataValidationException.ModelVersionNotSupported::class)
    fun `Given, decryptRecord is called with a EncryptedRecord and UserId, it adds a decrypted AttachmentKey, if the EncryptedRecord contains a encrypted AttachmentKey, for Fhir4`() {
        // Given
        val commonKeyId = "mockCommonKeyId"
        val modelVersion = 1
        val resource: Fhir4Resource = mockk()
        val encryptedRecord: NetworkModelContract.EncryptedRecord = mockk()

        decryptRecordFlow(
                encryptedRecord,
                modelVersion,
                commonKeyId,
                resource,
                encryptedAttachmentsKey = encryptedAttachmentKey
        )

        // When
        val decrypted = recordService.decryptRecord<Fhir4Resource>(
                encryptedRecord,
                RecordServiceTestBase.USER_ID
        )

        // Then
        assertEquals(
                actual = decrypted,
                expected = DecryptedR4Record(
                        RecordServiceTestBase.RECORD_ID,
                        resource,
                        tags,
                        annotations,
                        RecordServiceTestBase.CREATION_DATE,
                        null,
                        dataKey,
                        attachmentKey,
                        modelVersion
                )
        )

        verfiyDecryptRecordFlow<Fhir4Resource>(
                commonKeyId,
                encryptedAttachmentsKey = encryptedAttachmentKey
        )
    }
}
