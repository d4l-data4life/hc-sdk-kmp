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
import care.data4life.crypto.GCKeyPair
import care.data4life.sdk.crypto.CryptoContract
import care.data4life.sdk.data.DataResource
import care.data4life.sdk.fhir.Fhir3Resource
import care.data4life.sdk.fhir.Fhir4Resource
import care.data4life.sdk.fhir.FhirContract
import care.data4life.sdk.lang.CoreRuntimeException
import care.data4life.sdk.lang.DataValidationException
import care.data4life.sdk.model.ModelContract
import care.data4life.sdk.model.ModelContract.ModelVersion.Companion.CURRENT
import care.data4life.sdk.network.NetworkingContract
import care.data4life.sdk.tag.Annotations
import care.data4life.sdk.tag.EncryptedTagsAndAnnotations
import care.data4life.sdk.tag.TaggingContract
import care.data4life.sdk.tag.Tags
import care.data4life.sdk.test.util.GenericTestDataProvider.ALIAS
import care.data4life.sdk.test.util.GenericTestDataProvider.USER_ID
import care.data4life.sdk.wrapper.WrapperContract
import io.mockk.Runs
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifyOrder
import io.reactivex.Single
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RecordCryptoServiceDecryptionTest {
    private lateinit var service: NetworkModelContract.EncryptionService
    private var apiService: NetworkingContract.Service = mockk()
    private val cryptoService: CryptoContract.Service = mockk()
    private val taggingService: TaggingContract.Service = mockk()
    private val tagCryptoService: TaggingContract.CryptoService = mockk()
    private val resourceCryptoService: FhirContract.CryptoService = mockk()
    private val dateTimeFormatter: WrapperContract.DateTimeFormatter = mockk()
    private val limitGuard: NetworkModelContract.LimitGuard = mockk()
    private val modelVersion: ModelContract.ModelVersion = mockk()

    @Before
    fun setUp() {
        clearAllMocks()

        service = RecordCryptoService(
            ALIAS,
            apiService,
            taggingService,
            tagCryptoService,
            limitGuard,
            cryptoService,
            resourceCryptoService,
            dateTimeFormatter,
            modelVersion
        )
    }

    private fun runCommonKeyFetch(
        userId: String,
        commonKey: GCKey,
        commonKeyId: String,
        encryptedCommonKey: EncryptedKey,
        commonKeyResponse: CommonKeyResponse,
        keyPair: GCKeyPair
    ) {
        every { commonKeyResponse.commonKey } returns encryptedCommonKey

        every {
            apiService.fetchCommonKey(
                ALIAS,
                userId,
                commonKeyId
            )
        } returns Single.just(commonKeyResponse)

        every {
            cryptoService.fetchGCKeyPair()
        } returns Single.just(keyPair)

        every {
            cryptoService.asymDecryptSymetricKey(
                keyPair,
                encryptedCommonKey
            )
        } returns Single.just(commonKey)

        every { cryptoService.storeCommonKey(commonKeyId, commonKey) } just Runs
    }

    private inline fun <reified T : Any> runDecryptFlow(
        encryptedRecord: NetworkModelContract.EncryptedRecord,
        resource: T = mockk(),
        userId: String = USER_ID,
        version: Int = CURRENT,
        modelCheck: Boolean = true,
        identifier: String? = null,
        creationDate: String? = null,
        updateDate: String? = null,
        tags: Tags = mockk(),
        annotations: Annotations = emptyList(),
        encryptedTagsAndAnnotations: EncryptedTagsAndAnnotations = mockk(),
        commonKey: GCKey = mockk(),
        commonKeyId: String = "does not matter",
        commonKeyIsStored: Boolean = true,
        dataKey: GCKey = mockk(),
        attachmentKey: GCKey? = null,
        encryptedResource: String = "what so ever",
        encryptedDataKey: EncryptedKey = mockk(),
        encryptedAttachmentKey: EncryptedKey? = null,
        encryptedCommonKey: EncryptedKey = mockk(),
        commonKeyResponse: CommonKeyResponse = mockk(),
        keyPair: GCKeyPair = mockk()
    ) {
        every { encryptedRecord.modelVersion } returns version
        every { modelVersion.isModelVersionSupported(version) } returns modelCheck

        every { encryptedRecord.identifier } returns identifier
        every { encryptedRecord.customCreationDate } returns creationDate
        every { encryptedRecord.updatedDate } returns updateDate

        every { encryptedRecord.encryptedTags } returns encryptedTagsAndAnnotations

        every {
            tagCryptoService.decryptTagsAndAnnotations(encryptedTagsAndAnnotations)
        } returns Pair(tags, annotations)

        every { encryptedRecord.commonKeyId } returns commonKeyId
        every { encryptedRecord.encryptedDataKey } returns encryptedDataKey
        every { encryptedRecord.encryptedAttachmentsKey } returns encryptedAttachmentKey

        every { cryptoService.hasCommonKey(commonKeyId) } returns commonKeyIsStored
        if (commonKeyIsStored) {
            every { cryptoService.getCommonKeyById(commonKeyId) } returns commonKey
        } else {
            runCommonKeyFetch(
                userId,
                commonKey,
                commonKeyId,
                encryptedCommonKey,
                commonKeyResponse,
                keyPair
            )
        }

        every {
            cryptoService.symDecryptSymmetricKey(
                commonKey,
                encryptedDataKey
            )
        } returns Single.just(dataKey)

        if (encryptedAttachmentKey is EncryptedKey) {
            every {
                cryptoService.symDecryptSymmetricKey(
                    commonKey,
                    encryptedAttachmentKey
                )
            } returns Single.just(attachmentKey)
        }

        every { encryptedRecord.encryptedBody } returns encryptedResource

        every {
            resourceCryptoService.decryptResource<T>(dataKey, tags, encryptedResource)
        } returns resource
    }

    private inline fun <reified T : DataResource> runDecryptDataFlow(
        encryptedRecord: NetworkModelContract.EncryptedRecord,
        resource: T = mockk(),
        resourceValue: ByteArray = ByteArray(0),
        userId: String = USER_ID,
        version: Int = CURRENT,
        modelCheck: Boolean = true,
        identifier: String? = null,
        creationDate: String? = null,
        updateDate: String? = null,
        tags: Tags = mockk(),
        annotations: Annotations = emptyList(),
        encryptedTagsAndAnnotations: EncryptedTagsAndAnnotations = mockk(),
        commonKey: GCKey = mockk(),
        commonKeyId: String = "does not matter",
        commonKeyIsStored: Boolean = true,
        dataKey: GCKey = mockk(),
        attachmentKey: GCKey? = null,
        encryptedResource: String = "what so ever",
        encryptedDataKey: EncryptedKey = mockk(),
        encryptedAttachmentKey: EncryptedKey? = null,
        encryptedCommonKey: EncryptedKey = mockk(),
        commonKeyResponse: CommonKeyResponse = mockk(),
        keyPair: GCKeyPair = mockk()
    ) {
        every { resource.value } returns resourceValue
        every { limitGuard.checkDataLimit(resourceValue) } just Runs

        runDecryptFlow(
            encryptedRecord,
            resource,
            userId,
            version,
            modelCheck,
            identifier,
            creationDate,
            updateDate,
            tags,
            annotations,
            encryptedTagsAndAnnotations,
            commonKey,
            commonKeyId,
            commonKeyIsStored,
            dataKey,
            attachmentKey,
            encryptedResource,
            encryptedDataKey,
            encryptedAttachmentKey,
            encryptedCommonKey,
            commonKeyResponse,
            keyPair
        )
    }

    // FHIR3
    @Test
    fun `Given, decrypt is called with a EncryptedRecord, it fails, if the ModelVersion is not supported, while decrypting for Fhir3`() {
        // Given
        val encryptedRecord: NetworkModelContract.EncryptedRecord = mockk()
        val modelVersion = 23

        runDecryptFlow<Fhir3Resource>(
            encryptedRecord,
            version = modelVersion,
            modelCheck = false
        )

        // Then
        val error = assertFailsWith<DataValidationException.ModelVersionNotSupported> {
            // When
            service.decrypt<Fhir3Resource>(encryptedRecord, USER_ID)
        }

        assertEquals(
            actual = error.message,
            expected = "Please update SDK to latest version!"
        )
    }

    @Test
    fun `Given, decrypt is called with a EncryptedRecord, it returns a DecryptedRecord for Fhir3`() {
        // Given
        val encryptedRecord: NetworkModelContract.EncryptedRecord = mockk()

        runDecryptFlow<Fhir3Resource>(encryptedRecord)

        // When
        val decryptedRecord: Any = service.decrypt<Fhir3Resource>(encryptedRecord, USER_ID)

        // Then
        assertTrue(decryptedRecord is DecryptedRecord<*>)
    }

    @Test
    fun `Given, decrypt is called with a EncryptedRecord, it sets the Identifier of EncryptedRecord to the DecryptedRecord for Fhir3, if it is null`() {
        // Given
        val encryptedRecord: NetworkModelContract.EncryptedRecord = mockk()

        runDecryptFlow<Fhir3Resource>(
            encryptedRecord,
            identifier = null
        )

        // When
        val decryptedRecord = service.decrypt<Fhir3Resource>(encryptedRecord, USER_ID)

        // Then
        assertNull(decryptedRecord.identifier)
    }

    @Test
    fun `Given, decrypt is called with a EncryptedRecord, it sets the Identifier of EncryptedRecord to the DecryptedRecord for Fhir3, if it is not null`() {
        // Given
        val encryptedRecord: NetworkModelContract.EncryptedRecord = mockk()
        val id = "Something"

        runDecryptFlow<Fhir3Resource>(
            encryptedRecord,
            identifier = id
        )

        // When
        val decryptedRecord = service.decrypt<Fhir3Resource>(encryptedRecord, USER_ID)

        // Then
        assertEquals(
            actual = decryptedRecord.identifier,
            expected = id
        )
    }

    @Test
    fun `Given, decrypt is called with a EncryptedRecord, it sets the CreationDate of EncryptedRecord to the  DecryptedRecord for Fhir3, if it is null`() {
        // Given
        val encryptedRecord: NetworkModelContract.EncryptedRecord = mockk()

        runDecryptFlow<Fhir3Resource>(
            encryptedRecord,
            creationDate = null
        )

        // When
        val decryptedRecord = service.decrypt<Fhir3Resource>(encryptedRecord, USER_ID)

        // Then
        assertNull(decryptedRecord.customCreationDate)
    }

    @Test
    fun `Given, decrypt is called with a EncryptedRecord, it sets the CreationDate of EncryptedRecord to the  DecryptedRecord for Fhir3, if it is not null`() {
        // Given
        val encryptedRecord: NetworkModelContract.EncryptedRecord = mockk()
        val creationDate = "some day"

        runDecryptFlow<Fhir3Resource>(
            encryptedRecord,
            creationDate = creationDate
        )

        // When
        val decryptedRecord = service.decrypt<Fhir3Resource>(encryptedRecord, USER_ID)

        // Then
        assertEquals(
            actual = decryptedRecord.customCreationDate,
            expected = creationDate
        )
    }

    @Test
    fun `Given, decrypt is called with a EncryptedRecord, it sets the UpdateDate of EncryptedRecord to the  DecryptedRecord for Fhir3, if it is null`() {
        // Given
        val encryptedRecord: NetworkModelContract.EncryptedRecord = mockk()

        runDecryptFlow<Fhir3Resource>(
            encryptedRecord,
            updateDate = null
        )

        // When
        val decryptedRecord = service.decrypt<Fhir3Resource>(encryptedRecord, USER_ID)

        // Then
        assertNull(decryptedRecord.updatedDate)
    }

    @Test
    fun `Given, decrypt is called with a EncryptedRecord, it sets the UpdateDate of EncryptedRecord to the  DecryptedRecord for Fhir3, if it is not null`() {
        // Given
        val encryptedRecord: NetworkModelContract.EncryptedRecord = mockk()
        val updateDate = "some day"

        runDecryptFlow<Fhir3Resource>(
            encryptedRecord,
            updateDate = updateDate
        )

        // When
        val decryptedRecord = service.decrypt<Fhir3Resource>(encryptedRecord, USER_ID)

        // Then
        assertEquals(
            actual = decryptedRecord.updatedDate,
            expected = updateDate
        )
    }

    @Test
    fun `Given, decrypt is called with a EncryptedRecord, it sets the ModelVersion of EncryptedRecord to the DecryptedRecord for Fhir3`() {
        // Given
        val encryptedRecord: NetworkModelContract.EncryptedRecord = mockk()
        val version = 42

        runDecryptFlow<Fhir3Resource>(
            encryptedRecord,
            version = version
        )

        // When
        val decryptedRecord = service.decrypt<Fhir3Resource>(encryptedRecord, USER_ID)

        // Then
        assertEquals(
            actual = decryptedRecord.modelVersion,
            expected = version
        )
    }

    @Test
    fun `Given, decrypt is called with a EncryptedRecord, it decrypts the Tags and Annotation and includes them in the DecryptedRecord for Fhir3`() {
        // Given
        val encryptedRecord: NetworkModelContract.EncryptedRecord = mockk()
        val tags: Tags = mockk()
        val annotations: Annotations = mockk()
        val encryptedTagsAndAnnotations: EncryptedTagsAndAnnotations = mockk()

        runDecryptFlow<Fhir3Resource>(
            encryptedRecord,
            tags = tags,
            annotations = annotations,
            encryptedTagsAndAnnotations = encryptedTagsAndAnnotations
        )

        // When
        val decryptedRecord = service.decrypt<Fhir3Resource>(encryptedRecord, USER_ID)

        // Then
        assertEquals(
            actual = decryptedRecord.tags,
            expected = tags
        )

        assertEquals(
            actual = decryptedRecord.annotations,
            expected = annotations
        )

        verify(exactly = 1) {
            tagCryptoService.decryptTagsAndAnnotations(encryptedTagsAndAnnotations)
        }
    }

    @Test
    fun `Given, decrypt is called with a EncryptedRecord, it resolves the commonKey, which is not stored, and decrypts the dataKey, which is then included in the DecryptedRecord for Fhir3`() {
        // Given
        val encryptedRecord: NetworkModelContract.EncryptedRecord = mockk()
        val userId = "jjjjj"
        val commonKey: GCKey = mockk()
        val commonKeyId = "IDDDD"
        val commonKeyResponse: CommonKeyResponse = mockk()
        val encryptedCommonKey: EncryptedKey = mockk()
        val keyPair: GCKeyPair = mockk()
        val encryptedDataKey: EncryptedKey = mockk()
        val dataKey: GCKey = mockk()

        runDecryptFlow<Fhir3Resource>(
            encryptedRecord,
            userId = userId,
            commonKey = commonKey,
            commonKeyId = commonKeyId,
            encryptedDataKey = encryptedDataKey,
            encryptedCommonKey = encryptedCommonKey,
            commonKeyResponse = commonKeyResponse,
            keyPair = keyPair,
            commonKeyIsStored = false,
            dataKey = dataKey
        )

        // When
        val decryptedRecord = service.decrypt<Fhir3Resource>(encryptedRecord, userId)

        // Then
        assertEquals(
            actual = decryptedRecord.dataKey,
            expected = dataKey
        )

        verifyOrder {
            cryptoService.hasCommonKey(commonKeyId)
            apiService.fetchCommonKey(
                ALIAS,
                userId,
                commonKeyId
            )
            cryptoService.fetchGCKeyPair()
            cryptoService.asymDecryptSymetricKey(
                keyPair,
                encryptedCommonKey
            )
            cryptoService.storeCommonKey(commonKeyId, commonKey)
            cryptoService.symDecryptSymmetricKey(
                commonKey,
                encryptedDataKey
            )
        }
    }

    @Test
    fun `Given, decrypt is called with a EncryptedRecord, it resolves the commonKey, which is stored, and decrypts the dataKey, which is then included in the DecryptedRecord for Fhir3`() {
        // Given
        val encryptedRecord: NetworkModelContract.EncryptedRecord = mockk()
        val commonKey: GCKey = mockk()
        val commonKeyId = "IDDDD"
        val encryptedDataKey: EncryptedKey = mockk()
        val dataKey: GCKey = mockk()

        runDecryptFlow<Fhir3Resource>(
            encryptedRecord,
            commonKey = commonKey,
            commonKeyId = commonKeyId,
            encryptedDataKey = encryptedDataKey,
            dataKey = dataKey
        )

        // When
        val decryptedRecord = service.decrypt<Fhir3Resource>(encryptedRecord, USER_ID)

        // Then
        assertEquals(
            actual = decryptedRecord.dataKey,
            expected = dataKey
        )

        verifyOrder {
            cryptoService.hasCommonKey(commonKeyId)
            cryptoService.getCommonKeyById(commonKeyId)
            cryptoService.symDecryptSymmetricKey(
                commonKey,
                encryptedDataKey
            )
        }
    }

    @Test
    fun `Given, decrypt is called with a EncryptedRecord, it sets the AttachmentKey to null  DecryptedRecord for Fhir3, if the EncryptedRecord has none`() {
        // Given
        val encryptedRecord: NetworkModelContract.EncryptedRecord = mockk()

        runDecryptFlow<Fhir3Resource>(
            encryptedRecord,
            encryptedAttachmentKey = null
        )

        // When
        val decryptedRecord = service.decrypt<Fhir3Resource>(encryptedRecord, USER_ID)

        // Then
        assertNull(decryptedRecord.attachmentsKey)

        verify(exactly = 1) {
            cryptoService.symDecryptSymmetricKey(
                any(),
                any()
            )
        }
    }

    @Test
    fun `Given, decrypt is called with a EncryptedRecord, it decrypts the AttachmentKey and attaches it to the DecryptedRecord for Fhir3`() {
        // Given
        val encryptedRecord: NetworkModelContract.EncryptedRecord = mockk()
        val commonKey: GCKey = mockk()
        val commonKeyId = "IDDDD"
        val encryptedAttachmentKey: EncryptedKey = mockk()
        val attachmentKey: GCKey = mockk()

        runDecryptFlow<Fhir3Resource>(
            encryptedRecord,
            commonKeyId = commonKeyId,
            commonKey = commonKey,
            encryptedAttachmentKey = encryptedAttachmentKey,
            attachmentKey = attachmentKey
        )

        // When
        val decryptedRecord = service.decrypt<Fhir3Resource>(encryptedRecord, USER_ID)

        // Then
        assertEquals(
            actual = decryptedRecord.attachmentsKey,
            expected = attachmentKey
        )

        verifyOrder {
            cryptoService.hasCommonKey(commonKeyId)
            cryptoService.getCommonKeyById(commonKeyId)
            cryptoService.symDecryptSymmetricKey(
                commonKey,
                any()
            )
            cryptoService.symDecryptSymmetricKey(
                commonKey,
                encryptedAttachmentKey
            )
        }
    }

    @Test
    fun `Given, decrypt is called with a EncryptedRecord, it decrypts the encrypted body and fails, while decrypting for Fhir3, if it is empty`() {
        // Given
        val encryptedRecord: NetworkModelContract.EncryptedRecord = mockk()

        runDecryptFlow<Fhir3Resource>(
            encryptedRecord,
            encryptedResource = ""
        )

        // Then
        assertFailsWith<CoreRuntimeException.InternalFailure> {
            // When
            service.decrypt<Fhir3Resource>(encryptedRecord, USER_ID)
        }
    }

    @Test
    fun `Given, decrypt is called with a EncryptedRecord, it decrypts the Resource and attaches it to the DecryptedRecord for Fhir3`() {
        // Given
        val encryptedRecord: NetworkModelContract.EncryptedRecord = mockk()
        val tags: Tags = mockk()
        val dataKey: GCKey = mockk()
        val resource: Fhir3Resource = mockk()
        val encryptedResource = "I am the secret Resource"

        runDecryptFlow(
            encryptedRecord,
            resource = resource,
            tags = tags,
            dataKey = dataKey,
            encryptedResource = encryptedResource
        )

        // When
        val decryptedRecord = service.decrypt<Fhir3Resource>(encryptedRecord, USER_ID)

        // Then
        assertEquals(
            expected = resource,
            actual = decryptedRecord.resource
        )

        verifyOrder {
            cryptoService.hasCommonKey(any())
            cryptoService.getCommonKeyById(any())
            cryptoService.symDecryptSymmetricKey(
                any(),
                any()
            )
            resourceCryptoService.decryptResource<Fhir3Resource>(
                dataKey,
                tags,
                encryptedResource
            )
        }
    }

    // FHIR4
    @Test
    fun `Given, decrypt is called with a EncryptedRecord, it fails, if the ModelVersion is not supported, while decrypting for Fhir4`() {
        // Given
        val encryptedRecord: NetworkModelContract.EncryptedRecord = mockk()
        val modelVersion = 23

        runDecryptFlow<Fhir4Resource>(
            encryptedRecord,
            version = modelVersion,
            modelCheck = false
        )

        // Then
        val error = assertFailsWith<DataValidationException.ModelVersionNotSupported> {
            // When
            service.decrypt<Fhir4Resource>(encryptedRecord, USER_ID)
        }

        assertEquals(
            actual = error.message,
            expected = "Please update SDK to latest version!"
        )
    }

    @Test
    fun `Given, decrypt is called with a EncryptedRecord, it returns a DecryptedRecord for Fhir4`() {
        // Given
        val encryptedRecord: NetworkModelContract.EncryptedRecord = mockk()

        runDecryptFlow<Fhir4Resource>(encryptedRecord)

        // When
        val decryptedRecord: Any = service.decrypt<Fhir4Resource>(encryptedRecord, USER_ID)

        // Then
        assertTrue(decryptedRecord is DecryptedR4Record<*>)
    }

    @Test
    fun `Given, decrypt is called with a EncryptedRecord, it sets the Identifier of EncryptedRecord to the DecryptedRecord for Fhir4, if it is null`() {
        // Given
        val encryptedRecord: NetworkModelContract.EncryptedRecord = mockk()

        runDecryptFlow<Fhir4Resource>(
            encryptedRecord,
            identifier = null
        )

        // When
        val decryptedRecord = service.decrypt<Fhir4Resource>(encryptedRecord, USER_ID)

        // Then
        assertNull(decryptedRecord.identifier)
    }

    @Test
    fun `Given, decrypt is called with a EncryptedRecord, it sets the Identifier of EncryptedRecord to the DecryptedRecord for Fhir4, if it is not null`() {
        // Given
        val encryptedRecord: NetworkModelContract.EncryptedRecord = mockk()
        val id = "Something"

        runDecryptFlow<Fhir4Resource>(
            encryptedRecord,
            identifier = id
        )

        // When
        val decryptedRecord = service.decrypt<Fhir4Resource>(encryptedRecord, USER_ID)

        // Then
        assertEquals(
            actual = decryptedRecord.identifier,
            expected = id
        )
    }

    @Test
    fun `Given, decrypt is called with a EncryptedRecord, it sets the CreationDate of EncryptedRecord to the  DecryptedRecord for Fhir4, if it is null`() {
        // Given
        val encryptedRecord: NetworkModelContract.EncryptedRecord = mockk()

        runDecryptFlow<Fhir4Resource>(
            encryptedRecord,
            creationDate = null
        )

        // When
        val decryptedRecord = service.decrypt<Fhir4Resource>(encryptedRecord, USER_ID)

        // Then
        assertNull(decryptedRecord.customCreationDate)
    }

    @Test
    fun `Given, decrypt is called with a EncryptedRecord, it sets the CreationDate of EncryptedRecord to the  DecryptedRecord for Fhir4, if it is not null`() {
        // Given
        val encryptedRecord: NetworkModelContract.EncryptedRecord = mockk()
        val creationDate = "some day"

        runDecryptFlow<Fhir4Resource>(
            encryptedRecord,
            creationDate = creationDate
        )

        // When
        val decryptedRecord = service.decrypt<Fhir4Resource>(encryptedRecord, USER_ID)

        // Then
        assertEquals(
            actual = decryptedRecord.customCreationDate,
            expected = creationDate
        )
    }

    @Test
    fun `Given, decrypt is called with a EncryptedRecord, it sets the UpdateDate of EncryptedRecord to the  DecryptedRecord for Fhir4, if it is null`() {
        // Given
        val encryptedRecord: NetworkModelContract.EncryptedRecord = mockk()

        runDecryptFlow<Fhir4Resource>(
            encryptedRecord,
            updateDate = null
        )

        // When
        val decryptedRecord = service.decrypt<Fhir4Resource>(encryptedRecord, USER_ID)

        // Then
        assertNull(decryptedRecord.updatedDate)
    }

    @Test
    fun `Given, decrypt is called with a EncryptedRecord, it sets the UpdateDate of EncryptedRecord to the  DecryptedRecord for Fhir4, if it is not null`() {
        // Given
        val encryptedRecord: NetworkModelContract.EncryptedRecord = mockk()
        val updateDate = "some day"

        runDecryptFlow<Fhir4Resource>(
            encryptedRecord,
            updateDate = updateDate
        )

        // When
        val decryptedRecord = service.decrypt<Fhir4Resource>(encryptedRecord, USER_ID)

        // Then
        assertEquals(
            actual = decryptedRecord.updatedDate,
            expected = updateDate
        )
    }

    @Test
    fun `Given, decrypt is called with a EncryptedRecord, it sets the ModelVersion of EncryptedRecord to the DecryptedRecord for Fhir4`() {
        // Given
        val encryptedRecord: NetworkModelContract.EncryptedRecord = mockk()
        val version = 42

        runDecryptFlow<Fhir4Resource>(
            encryptedRecord,
            version = version
        )

        // When
        val decryptedRecord = service.decrypt<Fhir4Resource>(encryptedRecord, USER_ID)

        // Then
        assertEquals(
            actual = decryptedRecord.modelVersion,
            expected = version
        )
    }

    @Test
    fun `Given, decrypt is called with a EncryptedRecord, it decrypts the Tags and Annotation and includes them in the DecryptedRecord for Fhir4`() {
        // Given
        val encryptedRecord: NetworkModelContract.EncryptedRecord = mockk()
        val tags: Tags = mockk()
        val annotations: Annotations = mockk()
        val encryptedTagsAndAnnotations: EncryptedTagsAndAnnotations = mockk()

        runDecryptFlow<Fhir4Resource>(
            encryptedRecord,
            tags = tags,
            annotations = annotations,
            encryptedTagsAndAnnotations = encryptedTagsAndAnnotations
        )

        // When
        val decryptedRecord = service.decrypt<Fhir4Resource>(encryptedRecord, USER_ID)

        // Then
        assertEquals(
            actual = decryptedRecord.tags,
            expected = tags
        )

        assertEquals(
            actual = decryptedRecord.annotations,
            expected = annotations
        )

        verify(exactly = 1) {
            tagCryptoService.decryptTagsAndAnnotations(encryptedTagsAndAnnotations)
        }
    }

    @Test
    fun `Given, decrypt is called with a EncryptedRecord, it resolves the commonKey, which is not stored, and decrypts the dataKey, which is then included in the DecryptedRecord for Fhir4`() {
        // Given
        val encryptedRecord: NetworkModelContract.EncryptedRecord = mockk()
        val userId = "jjjjj"
        val commonKey: GCKey = mockk()
        val commonKeyId = "IDDDD"
        val commonKeyResponse: CommonKeyResponse = mockk()
        val encryptedCommonKey: EncryptedKey = mockk()
        val keyPair: GCKeyPair = mockk()
        val encryptedDataKey: EncryptedKey = mockk()
        val dataKey: GCKey = mockk()

        runDecryptFlow<Fhir4Resource>(
            encryptedRecord,
            userId = userId,
            commonKey = commonKey,
            commonKeyId = commonKeyId,
            encryptedDataKey = encryptedDataKey,
            encryptedCommonKey = encryptedCommonKey,
            commonKeyResponse = commonKeyResponse,
            keyPair = keyPair,
            commonKeyIsStored = false,
            dataKey = dataKey
        )

        // When
        val decryptedRecord = service.decrypt<Fhir4Resource>(encryptedRecord, userId)

        // Then
        assertEquals(
            actual = decryptedRecord.dataKey,
            expected = dataKey
        )

        verifyOrder {
            cryptoService.hasCommonKey(commonKeyId)
            apiService.fetchCommonKey(
                ALIAS,
                userId,
                commonKeyId
            )
            cryptoService.fetchGCKeyPair()
            cryptoService.asymDecryptSymetricKey(
                keyPair,
                encryptedCommonKey
            )
            cryptoService.storeCommonKey(commonKeyId, commonKey)
            cryptoService.symDecryptSymmetricKey(
                commonKey,
                encryptedDataKey
            )
        }
    }

    @Test
    fun `Given, decrypt is called with a EncryptedRecord, it resolves the commonKey, which is stored, and decrypts the dataKey, which is then included in the DecryptedRecord for Fhir4`() {
        // Given
        val encryptedRecord: NetworkModelContract.EncryptedRecord = mockk()
        val commonKey: GCKey = mockk()
        val commonKeyId = "IDDDD"
        val encryptedDataKey: EncryptedKey = mockk()
        val dataKey: GCKey = mockk()

        runDecryptFlow<Fhir4Resource>(
            encryptedRecord,
            commonKey = commonKey,
            commonKeyId = commonKeyId,
            encryptedDataKey = encryptedDataKey,
            dataKey = dataKey
        )

        // When
        val decryptedRecord = service.decrypt<Fhir4Resource>(encryptedRecord, USER_ID)

        // Then
        assertEquals(
            actual = decryptedRecord.dataKey,
            expected = dataKey
        )

        verifyOrder {
            cryptoService.hasCommonKey(commonKeyId)
            cryptoService.getCommonKeyById(commonKeyId)
            cryptoService.symDecryptSymmetricKey(
                commonKey,
                encryptedDataKey
            )
        }
    }

    @Test
    fun `Given, decrypt is called with a EncryptedRecord, it sets the AttachmentKey to null  DecryptedRecord for Fhir4, if the EncryptedRecord has none`() {
        // Given
        val encryptedRecord: NetworkModelContract.EncryptedRecord = mockk()

        runDecryptFlow<Fhir4Resource>(
            encryptedRecord,
            encryptedAttachmentKey = null
        )

        // When
        val decryptedRecord = service.decrypt<Fhir4Resource>(encryptedRecord, USER_ID)

        // Then
        assertNull(decryptedRecord.attachmentsKey)

        verify(exactly = 1) {
            cryptoService.symDecryptSymmetricKey(
                any(),
                any()
            )
        }
    }

    @Test
    fun `Given, decrypt is called with a EncryptedRecord, it decrypts the AttachmentKey and attaches it to the DecryptedRecord for Fhir4`() {
        // Given
        val encryptedRecord: NetworkModelContract.EncryptedRecord = mockk()
        val commonKey: GCKey = mockk()
        val commonKeyId = "IDDDD"
        val encryptedAttachmentKey: EncryptedKey = mockk()
        val attachmentKey: GCKey = mockk()

        runDecryptFlow<Fhir4Resource>(
            encryptedRecord,
            commonKeyId = commonKeyId,
            commonKey = commonKey,
            encryptedAttachmentKey = encryptedAttachmentKey,
            attachmentKey = attachmentKey
        )

        // When
        val decryptedRecord = service.decrypt<Fhir4Resource>(encryptedRecord, USER_ID)

        // Then
        assertEquals(
            actual = decryptedRecord.attachmentsKey,
            expected = attachmentKey
        )

        verifyOrder {
            cryptoService.hasCommonKey(commonKeyId)
            cryptoService.getCommonKeyById(commonKeyId)
            cryptoService.symDecryptSymmetricKey(
                commonKey,
                any()
            )
            cryptoService.symDecryptSymmetricKey(
                commonKey,
                encryptedAttachmentKey
            )
        }
    }

    @Test
    fun `Given, decrypt is called with a EncryptedRecord, it decrypts the encrypted body and fails, while decrypting for Fhir4, if it is empty`() {
        // Given
        val encryptedRecord: NetworkModelContract.EncryptedRecord = mockk()

        runDecryptFlow<Fhir4Resource>(
            encryptedRecord,
            encryptedResource = ""
        )

        // Then
        assertFailsWith<CoreRuntimeException.InternalFailure> {
            // When
            service.decrypt<Fhir4Resource>(encryptedRecord, USER_ID)
        }
    }

    @Test
    fun `Given, decrypt is called with a EncryptedRecord, it decrypts the Resource and attaches it to the DecryptedRecord for Fhir4`() {
        // Given
        val encryptedRecord: NetworkModelContract.EncryptedRecord = mockk()
        val tags: Tags = mockk()
        val dataKey: GCKey = mockk()
        val resource: Fhir4Resource = mockk()
        val encryptedResource = "I am the secret Resource"

        runDecryptFlow(
            encryptedRecord,
            resource = resource,
            tags = tags,
            dataKey = dataKey,
            encryptedResource = encryptedResource
        )

        // When
        val decryptedRecord = service.decrypt<Fhir4Resource>(encryptedRecord, USER_ID)

        // Then
        assertEquals(
            expected = resource,
            actual = decryptedRecord.resource
        )

        verifyOrder {
            cryptoService.hasCommonKey(any())
            cryptoService.getCommonKeyById(any())
            cryptoService.symDecryptSymmetricKey(
                any(),
                any()
            )
            resourceCryptoService.decryptResource<Fhir4Resource>(
                dataKey,
                tags,
                encryptedResource
            )
        }
    }

    // Arbitrary Data
    @Test
    fun `Given, decrypt is called with a EncryptedRecord, it fails, if the ModelVersion is not supported, while decrypting for a DataResource`() {
        // Given
        val encryptedRecord: NetworkModelContract.EncryptedRecord = mockk()
        val modelVersion = 23

        runDecryptDataFlow<DataResource>(
            encryptedRecord,
            version = modelVersion,
            modelCheck = false
        )

        // Then
        val error = assertFailsWith<DataValidationException.ModelVersionNotSupported> {
            // When
            service.decrypt<DataResource>(encryptedRecord, USER_ID)
        }

        assertEquals(
            actual = error.message,
            expected = "Please update SDK to latest version!"
        )
    }

    @Test
    fun `Given, decrypt is called with a EncryptedRecord, it returns a DecryptedRecord for a DataResource`() {
        // Given
        val encryptedRecord: NetworkModelContract.EncryptedRecord = mockk()

        runDecryptDataFlow<DataResource>(encryptedRecord)

        // When
        val decryptedRecord: Any = service.decrypt<DataResource>(encryptedRecord, USER_ID)

        // Then
        assertTrue(decryptedRecord is DecryptedDataRecord)
    }

    @Test
    fun `Given, decrypt is called with a EncryptedRecord, it sets the Identifier of EncryptedRecord to the DecryptedRecord for a DataResource, if it is null`() {
        // Given
        val encryptedRecord: NetworkModelContract.EncryptedRecord = mockk()

        runDecryptDataFlow<DataResource>(
            encryptedRecord,
            identifier = null
        )

        // When
        val decryptedRecord = service.decrypt<DataResource>(encryptedRecord, USER_ID)

        // Then
        assertNull(decryptedRecord.identifier)
    }

    @Test
    fun `Given, decrypt is called with a EncryptedRecord, it sets the Identifier of EncryptedRecord to the DecryptedRecord for a DataResource, if it is not null`() {
        // Given
        val encryptedRecord: NetworkModelContract.EncryptedRecord = mockk()
        val id = "Something"

        runDecryptDataFlow<DataResource>(
            encryptedRecord,
            identifier = id
        )

        // When
        val decryptedRecord = service.decrypt<DataResource>(encryptedRecord, USER_ID)

        // Then
        assertEquals(
            actual = decryptedRecord.identifier,
            expected = id
        )
    }

    @Test
    fun `Given, decrypt is called with a EncryptedRecord, it sets the CreationDate of EncryptedRecord to the  DecryptedRecord for a DataResource, if it is null`() {
        // Given
        val encryptedRecord: NetworkModelContract.EncryptedRecord = mockk()

        runDecryptDataFlow<DataResource>(
            encryptedRecord,
            creationDate = null
        )

        // When
        val decryptedRecord = service.decrypt<DataResource>(encryptedRecord, USER_ID)

        // Then
        assertNull(decryptedRecord.customCreationDate)
    }

    @Test
    fun `Given, decrypt is called with a EncryptedRecord, it sets the CreationDate of EncryptedRecord to the  DecryptedRecord for a DataResource, if it is not null`() {
        // Given
        val encryptedRecord: NetworkModelContract.EncryptedRecord = mockk()
        val creationDate = "some day"

        runDecryptDataFlow<DataResource>(
            encryptedRecord,
            creationDate = creationDate
        )

        // When
        val decryptedRecord = service.decrypt<DataResource>(encryptedRecord, USER_ID)

        // Then
        assertEquals(
            actual = decryptedRecord.customCreationDate,
            expected = creationDate
        )
    }

    @Test
    fun `Given, decrypt is called with a EncryptedRecord, it sets the UpdateDate of EncryptedRecord to the  DecryptedRecord for a DataResource, if it is null`() {
        // Given
        val encryptedRecord: NetworkModelContract.EncryptedRecord = mockk()

        runDecryptDataFlow<DataResource>(
            encryptedRecord,
            updateDate = null
        )

        // When
        val decryptedRecord = service.decrypt<DataResource>(encryptedRecord, USER_ID)

        // Then
        assertNull(decryptedRecord.updatedDate)
    }

    @Test
    fun `Given, decrypt is called with a EncryptedRecord, it sets the UpdateDate of EncryptedRecord to the  DecryptedRecord for a DataResource, if it is not null`() {
        // Given
        val encryptedRecord: NetworkModelContract.EncryptedRecord = mockk()
        val updateDate = "some day"

        runDecryptDataFlow<DataResource>(
            encryptedRecord,
            updateDate = updateDate
        )

        // When
        val decryptedRecord = service.decrypt<DataResource>(encryptedRecord, USER_ID)

        // Then
        assertEquals(
            actual = decryptedRecord.updatedDate,
            expected = updateDate
        )
    }

    @Test
    fun `Given, decrypt is called with a EncryptedRecord, it sets the ModelVersion of EncryptedRecord to the DecryptedRecord for a DataResource`() {
        // Given
        val encryptedRecord: NetworkModelContract.EncryptedRecord = mockk()
        val version = 42

        runDecryptDataFlow<DataResource>(
            encryptedRecord,
            version = version
        )

        // When
        val decryptedRecord = service.decrypt<DataResource>(encryptedRecord, USER_ID)

        // Then
        assertEquals(
            actual = decryptedRecord.modelVersion,
            expected = version
        )
    }

    @Test
    fun `Given, decrypt is called with a EncryptedRecord, it decrypts the Tags and Annotation and includes them in the DecryptedRecord for a DataResource`() {
        // Given
        val encryptedRecord: NetworkModelContract.EncryptedRecord = mockk()
        val tags: Tags = mockk()
        val annotations: Annotations = mockk()
        val encryptedTagsAndAnnotations: EncryptedTagsAndAnnotations = mockk()

        runDecryptDataFlow<DataResource>(
            encryptedRecord,
            tags = tags,
            annotations = annotations,
            encryptedTagsAndAnnotations = encryptedTagsAndAnnotations
        )

        // When
        val decryptedRecord = service.decrypt<DataResource>(encryptedRecord, USER_ID)

        // Then
        assertEquals(
            actual = decryptedRecord.tags,
            expected = tags
        )

        assertEquals(
            actual = decryptedRecord.annotations,
            expected = annotations
        )

        verify(exactly = 1) {
            tagCryptoService.decryptTagsAndAnnotations(encryptedTagsAndAnnotations)
        }
    }

    @Test
    fun `Given, decrypt is called with a EncryptedRecord, it resolves the commonKey, which is not stored, and decrypts the dataKey, which is then included in the DecryptedRecord for a DataResource`() {
        // Given
        val encryptedRecord: NetworkModelContract.EncryptedRecord = mockk()
        val userId = "jjjjj"
        val commonKey: GCKey = mockk()
        val commonKeyId = "IDDDD"
        val commonKeyResponse: CommonKeyResponse = mockk()
        val encryptedCommonKey: EncryptedKey = mockk()
        val keyPair: GCKeyPair = mockk()
        val encryptedDataKey: EncryptedKey = mockk()
        val dataKey: GCKey = mockk()

        runDecryptDataFlow<DataResource>(
            encryptedRecord,
            userId = userId,
            commonKey = commonKey,
            commonKeyId = commonKeyId,
            encryptedDataKey = encryptedDataKey,
            encryptedCommonKey = encryptedCommonKey,
            commonKeyResponse = commonKeyResponse,
            keyPair = keyPair,
            commonKeyIsStored = false,
            dataKey = dataKey
        )

        // When
        val decryptedRecord = service.decrypt<DataResource>(encryptedRecord, userId)

        // Then
        assertEquals(
            actual = decryptedRecord.dataKey,
            expected = dataKey
        )

        verifyOrder {
            cryptoService.hasCommonKey(commonKeyId)
            apiService.fetchCommonKey(
                ALIAS,
                userId,
                commonKeyId
            )
            cryptoService.fetchGCKeyPair()
            cryptoService.asymDecryptSymetricKey(
                keyPair,
                encryptedCommonKey
            )
            cryptoService.storeCommonKey(commonKeyId, commonKey)
            cryptoService.symDecryptSymmetricKey(
                commonKey,
                encryptedDataKey
            )
        }
    }

    @Test
    fun `Given, decrypt is called with a EncryptedRecord, it resolves the commonKey, which is stored, and decrypts the dataKey, which is then included in the DecryptedRecord for a DataResource`() {
        // Given
        val encryptedRecord: NetworkModelContract.EncryptedRecord = mockk()
        val commonKey: GCKey = mockk()
        val commonKeyId = "IDDDD"
        val encryptedDataKey: EncryptedKey = mockk()
        val dataKey: GCKey = mockk()

        runDecryptDataFlow<DataResource>(
            encryptedRecord,
            commonKey = commonKey,
            commonKeyId = commonKeyId,
            encryptedDataKey = encryptedDataKey,
            dataKey = dataKey
        )

        // When
        val decryptedRecord = service.decrypt<DataResource>(encryptedRecord, USER_ID)

        // Then
        assertEquals(
            actual = decryptedRecord.dataKey,
            expected = dataKey
        )

        verifyOrder {
            cryptoService.hasCommonKey(commonKeyId)
            cryptoService.getCommonKeyById(commonKeyId)
            cryptoService.symDecryptSymmetricKey(
                commonKey,
                encryptedDataKey
            )
        }
    }

    @Test
    fun `Given, decrypt is called with a EncryptedRecord, it decrypts the encrypted body and fails, while decrypting for a DataResource, if it is empty`() {
        // Given
        val encryptedRecord: NetworkModelContract.EncryptedRecord = mockk()

        runDecryptDataFlow<DataResource>(
            encryptedRecord,
            encryptedResource = ""
        )

        // Then
        assertFailsWith<CoreRuntimeException.InternalFailure> {
            // When
            service.decrypt<DataResource>(encryptedRecord, USER_ID)
        }
    }

    @Test
    fun `Given, decrypt is called with a EncryptedRecord, it decrypts the Resource, checks if its valid and attaches it to the DecryptedRecord for a DataResource`() {
        // Given
        val encryptedRecord: NetworkModelContract.EncryptedRecord = mockk()
        val tags: Tags = mockk()
        val dataKey: GCKey = mockk()
        val resource: DataResource = mockk()
        val encryptedResource = "I am the secret Resource"
        val resourceValue = ByteArray(23)

        runDecryptDataFlow(
            encryptedRecord,
            resource = resource,
            tags = tags,
            dataKey = dataKey,
            encryptedResource = encryptedResource,
            resourceValue = resourceValue
        )

        // When
        val decryptedRecord = service.decrypt<DataResource>(encryptedRecord, USER_ID)

        // Then
        assertEquals(
            expected = resource,
            actual = decryptedRecord.resource
        )

        verifyOrder {
            cryptoService.hasCommonKey(any())
            cryptoService.getCommonKeyById(any())
            cryptoService.symDecryptSymmetricKey(
                any(),
                any()
            )
            resourceCryptoService.decryptResource<DataResource>(
                dataKey,
                tags,
                encryptedResource
            )
            limitGuard.checkDataLimit(resourceValue)
        }
    }
}
