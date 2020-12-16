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

package care.data4life.sdk.record

import care.data4life.crypto.GCKey
import care.data4life.crypto.KeyType
import care.data4life.sdk.ApiService
import care.data4life.sdk.CryptoService
import care.data4life.sdk.data.DataResource
import care.data4life.sdk.fhir.FhirContract
import care.data4life.sdk.lang.D4LException
import care.data4life.sdk.lang.DataValidationException
import care.data4life.sdk.model.ModelVersion
import care.data4life.sdk.network.DecryptedRecordBuilder
import care.data4life.sdk.network.model.EncryptedKey
import care.data4life.sdk.network.model.EncryptedRecord
import care.data4life.sdk.network.model.NetworkRecordContract
import care.data4life.sdk.tag.TagEncryptionService
import care.data4life.sdk.tag.TaggingService
import care.data4life.sdk.util.Base64
import care.data4life.sdk.wrapper.ResourceFactory
import care.data4life.sdk.wrapper.WrapperContract
import com.google.common.truth.Truth
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.mockkObject
import io.mockk.unmockkAll
import io.mockk.unmockkObject
import io.mockk.verify
import io.reactivex.Single
import org.junit.After
import org.junit.Assert
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException

class RecordEncryptionTest {
    private lateinit var tagEncryptionService: TagEncryptionService
    private lateinit var cryptoService: CryptoService
    private lateinit var fhirService: FhirContract.Service
    private lateinit var apiService: ApiService
    private val ALIAS = "alias"
    private val USER_ID = "user_id"

    private lateinit var recordEncryptionService: RecordEncryptionContract.Service

    @Before
    fun setUp() {
        tagEncryptionService = mockk()
        cryptoService = mockk()
        fhirService = mockk()
        apiService = mockk()

        recordEncryptionService = RecordEncryptionService(
                ALIAS,
                tagEncryptionService,
                cryptoService,
                fhirService,
                apiService
        )

        mockkObject(Base64)
        mockkObject(ResourceFactory)
    }

    @After
    fun tearDown() {
        unmockkObject(Base64)
        unmockkObject(ResourceFactory)
    }

    @Test
    fun `it is a RecordEncryptionFactory`() {
        val service: Any = RecordEncryptionService(
                ALIAS,
                tagEncryptionService,
                cryptoService,
                fhirService,
                apiService
        )
        assertTrue((service as Any) is RecordEncryptionContract.Service)
    }

    @Test
    fun `Given, encryptRecord is called with a DecryptedRecord, which conatains a non FhirResource, it returns a EncryptedRecord`() {
        // Given
        val dataKey = mockk<GCKey>()
        val tags = mockk<HashMap<String, String>>()
        val resource = mockk<WrapperContract.Resource>()
        val rawResource = mockk<DataResource>()
        val primitiveResult = ByteArray(23)
        val encryptedPrimitiveResult = ByteArray(1)
        val record = mockk<NetworkRecordContract.DecryptedRecord>(relaxed = true)
        val annotations = mockk<List<String>>()
        val id = "abc"
        val creationDate = "heute"
        val version = 42

        val currentCommonKeyId = "currentCommonKeyId"
        val encryptedTags = mockk<List<String>>(relaxed = true)
        val encryptedAnnotations = mockk<List<String>>()
        val commonKey = mockk<GCKey>()
        val encryptedDataKey = mockk<EncryptedKey>()
        val encryptedResource = "encrypted"

        every { rawResource.asByteArray() } returns primitiveResult
        every { resource.type } returns WrapperContract.Resource.TYPE.DATA
        every { resource.unwrap() } returns rawResource
        every { record.attachmentsKey } returns null
        every { record.resource } returns resource
        every { record.tags } returns tags
        every { record.annotations } returns annotations
        every { record.dataKey } returns dataKey
        every { record.identifier } returns id
        every { record.customCreationDate } returns creationDate
        every { record.modelVersion } returns version

        every { tagEncryptionService.encryptTags(tags) } returns encryptedTags
        every { tagEncryptionService.encryptAnnotations(annotations) } returns encryptedAnnotations

        every { cryptoService.fetchCurrentCommonKey() } returns commonKey
        every { cryptoService.currentCommonKeyId } returns currentCommonKeyId
        every {
            cryptoService.encryptSymmetricKey(
                    commonKey,
                    KeyType.DATA_KEY,
                    dataKey
            )
        } returns Single.just(encryptedDataKey)
        every {
            cryptoService.encrypt(
                    dataKey,
                    primitiveResult
            )
        } returns Single.just(encryptedPrimitiveResult)
        every { Base64.encodeToString(encryptedPrimitiveResult) } returns encryptedResource


        // When
        val encryptedRecord = recordEncryptionService.encryptRecord(record)

        // Then
        Truth.assertThat(encryptedRecord.encryptedBody).isEqualTo(encryptedResource)
        Truth.assertThat(encryptedRecord.commonKeyId).isEqualTo(currentCommonKeyId)
        Truth.assertThat(encryptedRecord.encryptedAttachmentsKey).isNull()

        verify(exactly = 1) {
            cryptoService.encrypt(
                    dataKey,
                    primitiveResult
            )
        }
        verify(exactly = 1) { Base64.encodeToString(encryptedPrimitiveResult) }
    }

    @Test
    @Throws(IOException::class)
    fun `Given, encryptRecord is called with a DecryptedRecord, with a non DataResource, it returns a EncryptedRecord`() {
        // Given
        val dataKey = mockk<GCKey>()
        val tags = mockk<HashMap<String, String>>()
        val resource = mockk<WrapperContract.Resource>()
        val record = mockk<NetworkRecordContract.DecryptedRecord>(relaxed = true)
        val annotations = mockk<List<String>>()
        val id = "abc"
        val creationDate = "heute"
        val version = 42

        val currentCommonKeyId = "currentCommonKeyId"
        val encryptedTags = mockk<List<String>>(relaxed = true)
        val encryptedAnnotations = mockk<List<String>>()
        val commonKey = mockk<GCKey>()
        val encryptedDataKey = mockk<EncryptedKey>()
        val encryptedResource = "encrypted"

        every { resource.type } returns WrapperContract.Resource.TYPE.FHIR3
        every { record.attachmentsKey } returns null
        every { record.resource } returns resource
        every { record.tags } returns tags
        every { record.annotations } returns annotations
        every { record.dataKey } returns dataKey
        every { record.identifier } returns id
        every { record.customCreationDate } returns creationDate
        every { record.modelVersion } returns version

        every { tagEncryptionService.encryptTags(tags) } returns encryptedTags
        every { tagEncryptionService.encryptAnnotations(annotations) } returns encryptedAnnotations

        every { fhirService.encryptResource(dataKey, resource) } returns encryptedResource

        every { cryptoService.fetchCurrentCommonKey() } returns commonKey
        every { cryptoService.currentCommonKeyId } returns currentCommonKeyId
        every {
            cryptoService.encryptSymmetricKey(
                    commonKey,
                    KeyType.DATA_KEY,
                    dataKey
            )
        } returns Single.just(encryptedDataKey)


        // When
        val encryptedRecord = recordEncryptionService.encryptRecord(record)

        // Then
        Truth.assertThat(encryptedRecord.encryptedBody).isEqualTo(encryptedResource)
        Truth.assertThat(encryptedRecord.commonKeyId).isEqualTo(currentCommonKeyId)
        Truth.assertThat(encryptedRecord.encryptedAttachmentsKey).isNull()

        verify(exactly = 1) { fhirService.encryptResource(dataKey, resource) }
        verify(exactly = 0) {
            cryptoService.encryptSymmetricKey(
                    commonKey,
                    KeyType.ATTACHMENT_KEY,
                    any()
            )
        }
    }

    @Test
    @Throws(IOException::class)
    fun `Given, encryptRecord is called with a DecryptedRecord, which contains a FhirResource, it adds a encrypted AttachmentKey, if the DecryptedRecord contains a AttachmentKey`() {
        // Given
        val dataKey = mockk<GCKey>()
        val tags = mockk<HashMap<String, String>>()
        val resource = mockk<WrapperContract.Resource>()
        val record = mockk<NetworkRecordContract.DecryptedRecord>(relaxed = true)
        val annotations = mockk<List<String>>()
        val id = "abc"
        val creationDate = "heute"
        val version = 42
        val attachmentKey = mockk<GCKey>()

        val currentCommonKeyId = "currentCommonKeyId"
        val encryptedTags = mockk<List<String>>(relaxed = true)
        val encryptedAnnotations = mockk<List<String>>()
        val commonKey = mockk<GCKey>()
        val encryptedDataKey = mockk<EncryptedKey>()
        val encryptedResource = "encrypted"
        val encryptedAttachmentKey = mockk<EncryptedKey>()

        every { resource.type } returns WrapperContract.Resource.TYPE.FHIR3
        every { record.attachmentsKey } returns attachmentKey
        every { record.resource } returns resource
        every { record.tags } returns tags
        every { record.annotations } returns annotations
        every { record.dataKey } returns dataKey
        every { record.identifier } returns id
        every { record.customCreationDate } returns creationDate
        every { record.modelVersion } returns version

        every { tagEncryptionService.encryptTags(tags) } returns encryptedTags
        every { tagEncryptionService.encryptAnnotations(annotations) } returns encryptedAnnotations

        every { fhirService.encryptResource(dataKey, resource) } returns encryptedResource

        every { cryptoService.fetchCurrentCommonKey() } returns commonKey
        every { cryptoService.currentCommonKeyId } returns currentCommonKeyId
        every {
            cryptoService.encryptSymmetricKey(
                    commonKey,
                    KeyType.DATA_KEY,
                    dataKey
            )
        } returns Single.just(encryptedDataKey)

        every {
            cryptoService.encryptSymmetricKey(
                    commonKey,
                    KeyType.ATTACHMENT_KEY,
                    attachmentKey
            )
        } returns Single.just(encryptedAttachmentKey)


        // When
        val encryptedRecord = recordEncryptionService.encryptRecord(record)

        // Then
        Truth.assertThat(encryptedRecord.encryptedBody).isEqualTo(encryptedResource)
        Truth.assertThat(encryptedRecord.commonKeyId).isEqualTo(currentCommonKeyId)
        Truth.assertThat(encryptedRecord.encryptedAttachmentsKey).isEqualTo(encryptedAttachmentKey)

        verify(exactly = 1) { fhirService.encryptResource(dataKey, resource) }
        verify(exactly = 1) {
            cryptoService.encryptSymmetricKey(
                    commonKey,
                    KeyType.ATTACHMENT_KEY,
                    attachmentKey
            )
        }
    }

    @Test
    @Throws(IOException::class, DataValidationException.ModelVersionNotSupported::class)
    fun `Given, decryptRecord is called with a EncryptedRecord and UserId, it fails for unsupported ModelVersions`() {
        // Given
        val encryptedRecord = mockk<EncryptedRecord>()
        val modelVersion = ModelVersion.CURRENT+1

        every { encryptedRecord.modelVersion } returns modelVersion

        // When
        try {
            recordEncryptionService.decryptRecord(encryptedRecord, USER_ID)
            Assert.fail("Exception expected!")
        } catch (e: D4LException) {
            // Then
            Truth.assertThat(e.javaClass).isEqualTo(DataValidationException.ModelVersionNotSupported::class.java)
            Truth.assertThat(e.message).isEqualTo("Please update SDK to latest version!")
        }
    }

    @Test
    @Throws(IOException::class, DataValidationException.ModelVersionNotSupported::class)
    fun `Given, decryptRecord is called with a EncryptedRecord, which encrypts a DataResource, and UserId, it returns a DecryptedRecord`() {
        // Given
        val encryptedRecord = mockk<EncryptedRecord>()
        val modelVersion = ModelVersion.CURRENT
        val id = "id"
        val encryptedTags = mockk<List<String>>(relaxed = true)
        val creationDate = "heute"
        val updateDate = null
        val encryptedResource = "encrypted"
        val encryptedDataKey = mockk<EncryptedKey>()
        val encryptedAttachmentKey = null
        val commonKeyId = "123"


        val commonKey = mockk<GCKey>()
        val decryptedTags = mockk<HashMap<String, String>>()
        val decryptedAnnotations = mockk<List<String>>()
        val decryptedDataKey = mockk<GCKey>()
        val decodedResource = ByteArray(1)
        val decryptedResource = ByteArray(42)
        val wrappedResource = mockk<WrapperContract.Resource>()
        val decryptedRecord = mockk<NetworkRecordContract.DecryptedRecord>()


        every { encryptedRecord.identifier } returns id
        every { encryptedRecord.encryptedTags } returns encryptedTags
        every { encryptedRecord.encryptedBody } returns encryptedResource
        every { encryptedRecord.customCreationDate } returns creationDate
        every { encryptedRecord.updatedDate } returns updateDate
        every { encryptedRecord.encryptedDataKey } returns encryptedDataKey
        every { encryptedRecord.encryptedAttachmentsKey } returns encryptedAttachmentKey
        every { encryptedRecord.commonKeyId } returns commonKeyId
        every { encryptedRecord.modelVersion } returns modelVersion

        every { tagEncryptionService.decryptTags(encryptedTags) } returns decryptedTags
        every { tagEncryptionService.decryptAnnotations(encryptedTags) } returns decryptedAnnotations

        every { cryptoService.hasCommonKey(commonKeyId) } returns true
        every { cryptoService.getCommonKeyById(commonKeyId) } returns commonKey
        every { cryptoService.symDecryptSymmetricKey(commonKey, encryptedDataKey) } returns Single.just(decryptedDataKey)

        every { decryptedTags.containsKey(TaggingService.TAG_RESOURCE_TYPE) } returns false

        every { Base64.decode(encryptedResource) } returns decodedResource
        every { cryptoService.decrypt(
                decryptedDataKey,
                decodedResource
        ) } returns Single.just(decryptedResource)

        // FIXME: This is a weakpoint
        every { ResourceFactory.wrap(any()) } returns wrappedResource

        mockkConstructor(DecryptedRecordBuilder::class)
        every { anyConstructed<DecryptedRecordBuilder>().build(wrappedResource) } returns decryptedRecord

        // When
        val decrypted = recordEncryptionService.decryptRecord(encryptedRecord, USER_ID)

        // Then
        Truth.assertThat(decrypted).isSameInstanceAs(decryptedRecord)


        verify(exactly = 1) {cryptoService.symDecryptSymmetricKey(
                commonKey,
                encryptedDataKey
        ) }
        verify(exactly = 1) {cryptoService.symDecryptSymmetricKey(
                commonKey,
                any()
        ) }
        verify(exactly = 1) { Base64.decode(encryptedResource) }
        verify(exactly = 1) { cryptoService.decrypt(
                decryptedDataKey,
                decodedResource
        ) }
        verify(exactly = 1) { anyConstructed<DecryptedRecordBuilder>().build(wrappedResource) }
    }

    @Test
    @Throws(IOException::class, DataValidationException.ModelVersionNotSupported::class)
    fun `Given, decryptRecord is called with a EncryptedRecord, which encrypts a FhirResource, and UserId, it returns a DecryptedRecord`() {
        // Given
        val encryptedRecord = mockk<EncryptedRecord>()
        val modelVersion = ModelVersion.CURRENT
        val id = "id"
        val encryptedTags = mockk<List<String>>(relaxed = true)
        val creationDate = "heute"
        val updateDate = null
        val encryptedResource = "encrypted"
        val encryptedDataKey = mockk<EncryptedKey>()
        val encryptedAttachmentKey = null
        val commonKeyId = "123"


        val commonKey = mockk<GCKey>()
        val decryptedTags = mockk<HashMap<String, String>>()
        val decryptedAnnotations = mockk<List<String>>()
        val decryptedDataKey = mockk<GCKey>()
        val wrappedResource = mockk<WrapperContract.Resource>()
        val decryptedRecord = mockk<NetworkRecordContract.DecryptedRecord>()


        every { encryptedRecord.identifier } returns id
        every { encryptedRecord.encryptedTags } returns encryptedTags
        every { encryptedRecord.encryptedBody } returns encryptedResource
        every { encryptedRecord.customCreationDate } returns creationDate
        every { encryptedRecord.updatedDate } returns updateDate
        every { encryptedRecord.encryptedDataKey } returns encryptedDataKey
        every { encryptedRecord.encryptedAttachmentsKey } returns encryptedAttachmentKey
        every { encryptedRecord.commonKeyId } returns commonKeyId
        every { encryptedRecord.modelVersion } returns modelVersion

        every { tagEncryptionService.decryptTags(encryptedTags) } returns decryptedTags
        every { tagEncryptionService.decryptAnnotations(encryptedTags) } returns decryptedAnnotations

        every { cryptoService.hasCommonKey(commonKeyId) } returns true
        every { cryptoService.getCommonKeyById(commonKeyId) } returns commonKey
        every { cryptoService.symDecryptSymmetricKey(commonKey, encryptedDataKey) } returns Single.just(decryptedDataKey)

        every { decryptedTags.containsKey(TaggingService.TAG_RESOURCE_TYPE) } returns true

        every { fhirService.decryptResource(
                decryptedDataKey,
                decryptedTags,
                encryptedResource
        ) } returns wrappedResource

        mockkConstructor(DecryptedRecordBuilder::class)
        every { anyConstructed<DecryptedRecordBuilder>().build(wrappedResource) } returns decryptedRecord

        // When
        val decrypted = recordEncryptionService.decryptRecord(encryptedRecord, USER_ID)

        // Then
        Truth.assertThat(decrypted).isSameInstanceAs(decryptedRecord)


        verify(exactly = 1) {cryptoService.symDecryptSymmetricKey(
                commonKey,
                encryptedDataKey
        ) }
        verify(exactly = 1) {cryptoService.symDecryptSymmetricKey(
                commonKey,
                any()
        ) }
        verify(exactly = 1) { fhirService.decryptResource(
                decryptedDataKey,
                decryptedTags,
                encryptedResource
        ) }
        verify(exactly = 1) { anyConstructed<DecryptedRecordBuilder>().build(wrappedResource) }
    }

    @Test
    @Throws(IOException::class)
    fun `Given, encryptRecord is called with a DecryptedRecord, it adds a encrypted AttachmentKey, if the DecryptedRecord contains a AttachmentKey`() {
        // Given
        val encryptedRecord = mockk<EncryptedRecord>()
        val modelVersion = ModelVersion.CURRENT
        val id = "id"
        val encryptedTags = mockk<List<String>>(relaxed = true)
        val creationDate = "heute"
        val updateDate = null
        val encryptedResource = "encrypted"
        val encryptedDataKey = mockk<EncryptedKey>()
        val encryptedAttachmentKey = mockk<EncryptedKey>()
        val commonKeyId = "123"

        val commonKey = mockk<GCKey>()
        val decryptedTags = mockk<HashMap<String, String>>()
        val decryptedAnnotations = mockk<List<String>>()
        val decryptedDataKey = mockk<GCKey>()
        val decryptedAttachmentKey = mockk<GCKey>()
        val wrappedResource = mockk<WrapperContract.Resource>()
        val decryptedRecord = mockk<NetworkRecordContract.DecryptedRecord>()


        every { encryptedRecord.identifier } returns id
        every { encryptedRecord.encryptedTags } returns encryptedTags
        every { encryptedRecord.encryptedBody } returns encryptedResource
        every { encryptedRecord.customCreationDate } returns creationDate
        every { encryptedRecord.updatedDate } returns updateDate
        every { encryptedRecord.encryptedDataKey } returns encryptedDataKey
        every { encryptedRecord.encryptedAttachmentsKey } returns encryptedAttachmentKey
        every { encryptedRecord.commonKeyId } returns commonKeyId
        every { encryptedRecord.modelVersion } returns modelVersion

        every { tagEncryptionService.decryptTags(encryptedTags) } returns decryptedTags
        every { tagEncryptionService.decryptAnnotations(encryptedTags) } returns decryptedAnnotations

        every { cryptoService.hasCommonKey(commonKeyId) } returns true
        every { cryptoService.getCommonKeyById(commonKeyId) } returns commonKey
        every { cryptoService.symDecryptSymmetricKey(commonKey, encryptedDataKey) } returns Single.just(decryptedDataKey)
        every { cryptoService.symDecryptSymmetricKey(commonKey, encryptedAttachmentKey) } returns Single.just(decryptedAttachmentKey)

        every { decryptedTags.containsKey(TaggingService.TAG_RESOURCE_TYPE) } returns true

        every { fhirService.decryptResource(
                decryptedDataKey,
                decryptedTags,
                encryptedResource
        ) } returns wrappedResource

        mockkConstructor(DecryptedRecordBuilder::class)
        every { anyConstructed<DecryptedRecordBuilder>().setAttachmentKey(decryptedAttachmentKey) } returns mockk()
        every { anyConstructed<DecryptedRecordBuilder>().build(wrappedResource) } returns decryptedRecord

        // When
        val decrypted = recordEncryptionService.decryptRecord(encryptedRecord, USER_ID)

        // Then
        Truth.assertThat(decrypted).isSameInstanceAs(decryptedRecord)


        verify(exactly = 1) {cryptoService.symDecryptSymmetricKey(
                commonKey,
                encryptedDataKey
        ) }
        verify(exactly = 1) {cryptoService.symDecryptSymmetricKey(
                commonKey,
                encryptedAttachmentKey
        ) }
        verify(exactly = 1) { fhirService.decryptResource(
                decryptedDataKey,
                decryptedTags,
                encryptedResource
        ) }
        verify(exactly = 1) { anyConstructed<DecryptedRecordBuilder>().setAttachmentKey(decryptedAttachmentKey) }
        verify(exactly = 1) { anyConstructed<DecryptedRecordBuilder>().build(wrappedResource) }
    }

    @Test
    @Throws(IOException::class, DataValidationException.ModelVersionNotSupported::class)
    fun `Given, decryptRecord is called with a EncryptedRecord and UserId, it adds a UpdateDate, if the EncryptedRecord contains a UpdateDate`() {
        // Given
        val encryptedRecord = mockk<EncryptedRecord>()
        val modelVersion = ModelVersion.CURRENT
        val id = "id"
        val encryptedTags = mockk<List<String>>(relaxed = true)
        val creationDate = "heute"
        val updateDate = "morgen"
        val encryptedResource = "encrypted"
        val encryptedDataKey = mockk<EncryptedKey>()
        val encryptedAttachmentKey = mockk<EncryptedKey>()
        val commonKeyId = "123"

        val builder = mockk<NetworkRecordContract.Builder>(relaxed = true)

        val commonKey = mockk<GCKey>()
        val decryptedTags = mockk<HashMap<String, String>>()
        val decryptedAnnotations = mockk<List<String>>()
        val decryptedDataKey = mockk<GCKey>()
        val decryptedAttachmentKey = mockk<GCKey>()
        val wrappedResource = mockk<WrapperContract.Resource>()
        val decryptedRecord = mockk<NetworkRecordContract.DecryptedRecord>()

        every { encryptedRecord.identifier } returns id
        every { encryptedRecord.encryptedTags } returns encryptedTags
        every { encryptedRecord.encryptedBody } returns encryptedResource
        every { encryptedRecord.customCreationDate } returns creationDate
        every { encryptedRecord.updatedDate } returns updateDate
        every { encryptedRecord.encryptedDataKey } returns encryptedDataKey
        every { encryptedRecord.encryptedAttachmentsKey } returns encryptedAttachmentKey
        every { encryptedRecord.commonKeyId } returns commonKeyId
        every { encryptedRecord.modelVersion } returns modelVersion

        every { tagEncryptionService.decryptTags(encryptedTags) } returns decryptedTags
        every { tagEncryptionService.decryptAnnotations(encryptedTags) } returns decryptedAnnotations

        every { cryptoService.hasCommonKey(commonKeyId) } returns true
        every { cryptoService.getCommonKeyById(commonKeyId) } returns commonKey
        every { cryptoService.symDecryptSymmetricKey(commonKey, encryptedDataKey) } returns Single.just(decryptedDataKey)
        every { cryptoService.symDecryptSymmetricKey(commonKey, encryptedAttachmentKey) } returns Single.just(decryptedAttachmentKey)

        every { decryptedTags.containsKey(TaggingService.TAG_RESOURCE_TYPE) } returns true

        every { fhirService.decryptResource(
                decryptedDataKey,
                decryptedTags,
                encryptedResource
        ) } returns wrappedResource

        mockkConstructor(DecryptedRecordBuilder::class)
        every { anyConstructed<DecryptedRecordBuilder>().setUpdateDate(updateDate) } returns builder
        every { builder.setModelVersion(any()) } returns builder
        every { builder.build(wrappedResource) } returns decryptedRecord

        // When
        val decrypted = recordEncryptionService.decryptRecord(encryptedRecord, USER_ID)

        // Then
        Truth.assertThat(decrypted).isSameInstanceAs(decryptedRecord)


        verify(exactly = 1) {cryptoService.symDecryptSymmetricKey(
                commonKey,
                encryptedDataKey
        ) }
        verify(exactly = 1) {cryptoService.symDecryptSymmetricKey(
                commonKey,
                encryptedAttachmentKey
        ) }
        verify(exactly = 1) { fhirService.decryptResource(
                decryptedDataKey,
                decryptedTags,
                encryptedResource
        ) }
        verify(exactly = 1) { anyConstructed<DecryptedRecordBuilder>().setUpdateDate(updateDate) }
        verify(exactly = 1) { builder.build(wrappedResource) }
    }

    // ToDo: uncached CommenKey
    // ToDo: decrypted Properties
}
