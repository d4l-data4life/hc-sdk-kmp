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
import care.data4life.sdk.fhir.Fhir3Resource
import care.data4life.sdk.fhir.Fhir4Resource
import care.data4life.sdk.fhir.FhirContract
import care.data4life.sdk.lang.DataValidationException
import care.data4life.sdk.network.model.DecryptedDataRecord
import care.data4life.sdk.network.model.DecryptedR4Record
import care.data4life.sdk.network.model.DecryptedRecord
import care.data4life.sdk.network.model.EncryptedKey
import care.data4life.sdk.network.model.NetworkModelContract
import care.data4life.sdk.network.model.NetworkModelContract.DecryptedBaseRecord
import care.data4life.sdk.network.model.RecordEncryptionService
import care.data4life.sdk.tag.Annotations
import care.data4life.sdk.tag.EncryptedTagsAndAnnotations
import care.data4life.sdk.tag.TaggingContract
import care.data4life.sdk.tag.Tags
import care.data4life.sdk.test.util.GenericTestDataProvider.ALIAS
import care.data4life.sdk.test.util.GenericTestDataProvider.PARTNER_ID
import care.data4life.sdk.test.util.GenericTestDataProvider.RECORD_ID
import care.data4life.sdk.test.util.GenericTestDataProvider.USER_ID
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.spyk
import io.mockk.unmockkConstructor
import io.mockk.verify
import io.mockk.verifyOrder
import io.reactivex.Single
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame

class RecordServiceCryptoTest {
    private lateinit var recordService: RecordService
    private val apiService: ApiService = mockk()
    private val cryptoService: CryptoContract.Service = mockk()
    private val fhirService: FhirContract.Service = mockk()
    private val tagEncryptionService: TaggingContract.EncryptionService = mockk()
    private val taggingService: TaggingContract.Service = mockk()
    private val attachmentService: AttachmentContract.Service = mockk()
    private val errorHandler: SdkContract.ErrorHandler = mockk()
    private val tags: Tags = mapOf(
        "potato" to "soup",
        "resourcetype" to "pumpkin"
    )
    private val annotations: Annotations = listOf("tomato", "soup")

    private val encryptedTagsAndAnnotations: EncryptedTagsAndAnnotations = mockk()
    private val dataKey: GCKey = mockk()
    private val commonKey: GCKey = mockk()
    private val attachmentKey: GCKey = mockk()
    private val encryptedResource: String = "potato"
    private val encryptedDataKey: EncryptedKey = mockk()
    private val encryptedAttachmentKey: EncryptedKey = mockk()

    private val creationDate = "2020-05-03"

    @Before
    fun setUp() {
        clearAllMocks()
        mockkConstructor(RecordEncryptionService::class)

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
    }

    @After
    fun tearDown() {
        unmockkConstructor(RecordEncryptionService::class)
    }

    @Test
    fun `Given, fromResource is called with a Resource and Annotations it delegates it to the ResourceEncryptionService`() {
        // Given
        val resource: Any = mockk()
        val annotations: Annotations = mockk()
        val expected: DecryptedBaseRecord<Any> = mockk()

        every {
            anyConstructed<RecordEncryptionService>().fromResource(
                resource,
                annotations
            )
        } returns expected

        // When
        val actual = recordService.fromResource(resource, annotations).blockingGet()

        // Then
        assertSame(
            actual = actual,
            expected = expected
        )

        verify(exactly = 1) {
            anyConstructed<RecordEncryptionService>().fromResource(
                resource,
                annotations
            )
        }
    }

    @Test
    fun `Given, encryptRecord is called with a DecryptedRecord it delegates it to the ResourceEncryptionService`() {
        // Given
        val record: DecryptedBaseRecord<Any> = mockk()
        val annotations: Annotations = mockk()
        val expected: NetworkModelContract.EncryptedRecord = mockk()

        every {
            anyConstructed<RecordEncryptionService>().encrypt(record)
        } returns expected

        // When
        val actual = recordService.encryptRecord(record)

        // Then
        assertSame(
            actual = actual,
            expected = expected
        )

        verify(exactly = 1) {
            anyConstructed<RecordEncryptionService>().encrypt(record)
        }
    }

    private fun <T : Any> decryptRecordFlow(
        encryptedRecord: NetworkModelContract.EncryptedRecord,
        modelVersion: Int,
        commonKeyId: String,
        resource: T,
        updateDate: String? = null,
        encryptedAttachmentKey: NetworkModelContract.EncryptedKey? = null
    ) {
        every { encryptedRecord.modelVersion } returns modelVersion
        every { encryptedRecord.commonKeyId } returns commonKeyId
        every { encryptedRecord.encryptedTags } returns encryptedTagsAndAnnotations
        every { encryptedRecord.encryptedDataKey } returns encryptedDataKey
        every { encryptedRecord.encryptedBody } returns encryptedResource
        every { encryptedRecord.customCreationDate } returns creationDate
        every { encryptedRecord.identifier } returns RECORD_ID
        every { encryptedRecord.updatedDate } returns updateDate
        every { encryptedRecord.encryptedAttachmentsKey } returns encryptedAttachmentKey

        every {
            tagEncryptionService.decryptTagsAndAnnotations(encryptedTagsAndAnnotations)
        } returns Pair(tags, annotations)
        every { cryptoService.hasCommonKey(commonKeyId) } returns true
        every { cryptoService.getCommonKeyById(commonKeyId) } returns commonKey
        every {
            cryptoService.symDecryptSymmetricKey(commonKey, encryptedDataKey)
        } returns Single.just(dataKey)

        if (encryptedAttachmentKey is NetworkModelContract.EncryptedKey) {
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

    private fun <T : Any> verfiyDecryptRecordFlow(commonKeyId: String) {
        verifyOrder {
            tagEncryptionService.decryptTagsAndAnnotations(encryptedTagsAndAnnotations)
            cryptoService.hasCommonKey(commonKeyId)
            cryptoService.getCommonKeyById(commonKeyId)
            cryptoService.symDecryptSymmetricKey(commonKey, encryptedDataKey)
            fhirService.decryptResource<T>(
                dataKey,
                tags,
                encryptedResource
            )
        }
    }

    private fun <T : Any> verfiyDecryptRecordWithFetchingKeyFlow(
        commonKeyId: String,
        encryptedAttachmentKey: NetworkModelContract.EncryptedKey
    ) {
        verifyOrder {
            tagEncryptionService.decryptTagsAndAnnotations(encryptedTagsAndAnnotations)
            cryptoService.hasCommonKey(commonKeyId)
            cryptoService.getCommonKeyById(commonKeyId)
            cryptoService.symDecryptSymmetricKey(commonKey, encryptedDataKey)
            cryptoService.symDecryptSymmetricKey(commonKey, encryptedAttachmentKey)
            fhirService.decryptResource<T>(
                dataKey,
                tags,
                encryptedResource
            )
        }
    }

    @Test
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
            USER_ID
        )

        // Then
        assertEquals(
            actual = decrypted,
            expected = DecryptedRecord(
                RECORD_ID,
                resource,
                tags,
                annotations,
                creationDate,
                null,
                dataKey,
                null,
                modelVersion
            )
        )

        verfiyDecryptRecordFlow<Fhir3Resource>(commonKeyId)
    }

    @Test
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
            USER_ID
        )

        // Then
        assertEquals(
            actual = decrypted,
            expected = DecryptedR4Record(
                RECORD_ID,
                resource,
                tags,
                annotations,
                creationDate,
                null,
                dataKey,
                null,
                modelVersion
            )
        )

        verfiyDecryptRecordFlow<Fhir3Resource>(commonKeyId)
    }

    @Test
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
            USER_ID
        )

        // Then
        assertEquals(
            actual = decrypted,
            expected = DecryptedDataRecord(
                RECORD_ID,
                resource,
                tags,
                annotations,
                creationDate,
                null,
                dataKey,
                modelVersion
            )
        )

        verfiyDecryptRecordFlow<Fhir3Resource>(commonKeyId)
    }

    @Test
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
            USER_ID
        )

        // Then
        assertEquals(
            actual = decrypted,
            expected = DecryptedRecord(
                RECORD_ID,
                resource,
                tags,
                annotations,
                creationDate,
                updateDate,
                dataKey,
                null,
                modelVersion
            )
        )

        verfiyDecryptRecordFlow<Fhir3Resource>(commonKeyId)
    }

    @Test
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
            USER_ID
        )

        // Then
        assertEquals(
            actual = decrypted,
            expected = DecryptedR4Record(
                RECORD_ID,
                resource,
                tags,
                annotations,
                creationDate,
                updateDate,
                dataKey,
                null,
                modelVersion
            )
        )

        verfiyDecryptRecordFlow<Fhir4Resource>(commonKeyId)
    }

    @Test
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
            USER_ID
        )

        // Then
        assertEquals(
            actual = decrypted,
            expected = DecryptedDataRecord(
                RECORD_ID,
                resource,
                tags,
                annotations,
                creationDate,
                updateDate,
                dataKey,
                modelVersion
            )
        )

        verfiyDecryptRecordFlow<DataResource>(commonKeyId)
    }

    @Test
    fun `Given, decryptRecord is called with a EncryptedRecord and UserId, it throws an error, if the ModelVersion is not supported, for Fhir3`() {
        // Given
        val modelVersion = 1
        val encryptedRecord: NetworkModelContract.EncryptedRecord = mockk()

        every { encryptedRecord.modelVersion } returns modelVersion + 23

        // When
        val exception = assertFailsWith<DataValidationException.ModelVersionNotSupported> {
            recordService.decryptRecord<Fhir3Resource>(
                encryptedRecord,
                USER_ID
            )
        }
        assertEquals(
            actual = exception.message,
            expected = "Please update SDK to latest version!"
        )
    }

    @Test
    fun `Given, decryptRecord is called with a EncryptedRecord and UserId, it throws an error, if the ModelVersion is not supported, for Fhir4`() {
        // Given
        val modelVersion = 1
        val encryptedRecord: NetworkModelContract.EncryptedRecord = mockk()

        every { encryptedRecord.modelVersion } returns modelVersion + 23

        // When
        val exception = assertFailsWith<DataValidationException.ModelVersionNotSupported> {
            recordService.decryptRecord<Fhir4Resource>(
                encryptedRecord,
                USER_ID
            )
        }
        assertEquals(
            actual = exception.message,
            expected = "Please update SDK to latest version!"
        )
    }

    @Test
    fun `Given, decryptRecord is called with a EncryptedRecord and UserId, it throws an error, if the ModelVersion is not supported, for arbitrary data`() {
        // Given
        val modelVersion = 1
        val encryptedRecord: NetworkModelContract.EncryptedRecord = mockk()

        every { encryptedRecord.modelVersion } returns modelVersion + 23

        // When
        val exception = assertFailsWith<DataValidationException.ModelVersionNotSupported> {
            recordService.decryptRecord<Fhir4Resource>(
                encryptedRecord,
                USER_ID
            )
        }
        assertEquals(
            actual = exception.message,
            expected = "Please update SDK to latest version!"
        )
    }

    @Test
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
            encryptedAttachmentKey = encryptedAttachmentKey
        )

        // When
        val decrypted = recordService.decryptRecord<Fhir3Resource>(
            encryptedRecord,
            USER_ID
        )

        // Then
        assertEquals(
            actual = decrypted,
            expected = DecryptedRecord(
                RECORD_ID,
                resource,
                tags,
                annotations,
                creationDate,
                null,
                dataKey,
                attachmentKey,
                modelVersion
            )
        )

        verfiyDecryptRecordWithFetchingKeyFlow<Fhir3Resource>(
            commonKeyId,
            encryptedAttachmentKey
        )
    }

    @Test
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
            encryptedAttachmentKey = encryptedAttachmentKey
        )

        // When
        val decrypted = recordService.decryptRecord<Fhir4Resource>(
            encryptedRecord,
            USER_ID
        )

        // Then
        assertEquals(
            actual = decrypted,
            expected = DecryptedR4Record(
                RECORD_ID,
                resource,
                tags,
                annotations,
                creationDate,
                null,
                dataKey,
                attachmentKey,
                modelVersion
            )
        )

        verfiyDecryptRecordWithFetchingKeyFlow<Fhir4Resource>(
            commonKeyId,
            encryptedAttachmentKey
        )
    }
}
