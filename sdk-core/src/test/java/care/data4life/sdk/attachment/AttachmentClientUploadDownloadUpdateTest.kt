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

package care.data4life.sdk.attachment

import care.data4life.crypto.GCKey
import care.data4life.fhir.stu3.model.DocumentReference
import care.data4life.sdk.CryptoService
import care.data4life.sdk.fhir.Fhir3Resource
import care.data4life.sdk.lang.CoreRuntimeException
import care.data4life.sdk.lang.D4LException
import care.data4life.sdk.lang.DataValidationException
import care.data4life.sdk.network.model.NetworkRecordContract
import care.data4life.sdk.wrapper.FhirAttachmentHelper
import care.data4life.sdk.wrapper.WrapperContract
import care.data4life.sdk.wrapper.WrapperFactoryContract
import com.google.common.truth.Truth
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.reactivex.Single
import org.junit.Assert
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AttachmentClientUploadDownloadUpdateTest {
    private lateinit var fhirAttachmentHelper: FhirAttachmentHelper
    private lateinit var cryptoService: CryptoService
    private lateinit var attachmentFactory: WrapperFactoryContract.AttachmentFactory
    private lateinit var attachmentService: AttachmentContract.Service
    private lateinit var thumbnailService: ThumbnailContract.Service
    private val USER_ID = "ID"

    private lateinit var attachmentClient: AttachmentContract.Client

    @Before
    fun setUp() {
        fhirAttachmentHelper = mockk()
        cryptoService = mockk()
        attachmentFactory = mockk()
        attachmentService = mockk()
        thumbnailService = mockk()

        attachmentClient = AttachmentClient(
                fhirAttachmentHelper,
                attachmentService,
                attachmentFactory,
                cryptoService,
                thumbnailService
        )
    }

    @Test
    fun `it is a AttachmentClient`() {
        val client: Any = AttachmentClient(
                fhirAttachmentHelper,
                mockk(),
                mockk(),
                mockk(),
                mockk()
        )

        assertTrue(client is AttachmentContract.Client)
    }

    @Test
    fun `Given, _uploadData is called with DecryptedRecord, which contains a DataResource, and UserId, it reflects it`() {
        // Given
        val resource = mockk<WrapperContract.Resource>()
        val decryptedRecord = mockk<NetworkRecordContract.DecryptedRecord<WrapperContract.Resource>>()

        every { resource.type } returns WrapperContract.Resource.TYPE.DATA
        every { decryptedRecord.resource } returns resource

        // When
        val record = attachmentClient.uploadData(decryptedRecord, USER_ID)

        // Then
        Truth.assertThat(record).isSameInstanceAs(decryptedRecord)

        verify(exactly = 0) { fhirAttachmentHelper.hasAttachment(any()) }
    }

    // TODO Record fhirAttachmentHelper.hasAttachment == false
    // TODO Attachment key at Record not null

    @Test
    fun `Given, uploadData is called with DecryptedRecord, which contains a non DataResource, and UserId, it fails, AttachmentId is set during uploadFlow`() {
        // Given
        val decryptedRecord = mockk<NetworkRecordContract.DecryptedRecord<WrapperContract.Resource>>(relaxed = true)
        val resource = mockk<WrapperContract.Resource>()
        val rawResource = mockk<DocumentReference>()
        val attachmentKey = mockk<GCKey>()
        val wrappedAttachment = mockk<WrapperContract.Attachment>()
        val serviceAttachment = "A attachment"
        val serviceAttachments = mutableListOf<Any>( serviceAttachment )


        every { resource.type } returns WrapperContract.Resource.TYPE.FHIR3
        every { resource.unwrap() } returns rawResource
        every { decryptedRecord.resource } returns resource
        every { decryptedRecord.attachmentsKey } returns null

        every { fhirAttachmentHelper.hasAttachment(rawResource) } returns true
        every{ fhirAttachmentHelper.getAttachment(rawResource) } returns serviceAttachments
        every{ wrappedAttachment.id } returns "something"

        every { attachmentFactory.wrap(serviceAttachment) } returns wrappedAttachment
        every { cryptoService.generateGCKey() } returns Single.just(attachmentKey)

        // When
        try {
            attachmentClient.uploadData(decryptedRecord, USER_ID)
            Assert.fail("Exception expected!")
        } catch (e: D4LException) {

            // Then
            Truth.assertThat(e.javaClass).isEqualTo(DataValidationException.IdUsageViolation::class.java)
            Truth.assertThat(e.message).isEqualTo("Attachment.id should be null")
        }

        verify(exactly = 1) { cryptoService.generateGCKey() }
    }

    // TODO: attachment == null branch
    // TODO: attachment.hash == null || attachment.size == null branch

    @Test
    @Throws(DataValidationException.ExpectedFieldViolation::class,
            DataValidationException.IdUsageViolation::class,
            DataValidationException.InvalidAttachmentPayloadHash::class)
    fun `Given, uploadData is called with DecryptedRecord, which contains a non DataResource, and UserId, it fails, on invalid hash`() {
        // Given
        val decryptedRecord = mockk<NetworkRecordContract.DecryptedRecord<WrapperContract.Resource>>(relaxed = true)
        val resource = mockk<WrapperContract.Resource>()
        val rawResource = mockk<DocumentReference>()
        val attachmentKey = mockk<GCKey>()
        val wrappedAttachment = mockk<WrapperContract.Attachment>()
        val serviceAttachment = "A attachment"
        val serviceAttachments = mutableListOf<Any>( serviceAttachment )
        val attachmentServiceHash = "123"
        val resourceHash = "321"


        every { resource.type } returns WrapperContract.Resource.TYPE.FHIR3
        every { resource.unwrap() } returns rawResource
        every { decryptedRecord.resource } returns resource
        every { decryptedRecord.attachmentsKey } returns null

        every { fhirAttachmentHelper.hasAttachment(rawResource) } returns true
        every{ fhirAttachmentHelper.getAttachment(rawResource) } returns serviceAttachments
        every{ wrappedAttachment.id } returns null
        every { wrappedAttachment.size } returns 23

        every { attachmentFactory.wrap(serviceAttachment) } returns wrappedAttachment
        every { cryptoService.generateGCKey() } returns Single.just(attachmentKey)

        every { attachmentService.getValidHash(wrappedAttachment) } returns attachmentServiceHash
        every { wrappedAttachment.hash } returns resourceHash

        // When
        try {
            attachmentClient.uploadData(decryptedRecord, USER_ID)
            Assert.fail("Exception expected!")
        } catch (e: D4LException) {

            // Then
            Truth.assertThat(e.javaClass).isEqualTo(DataValidationException.InvalidAttachmentPayloadHash::class.java)
            Truth.assertThat(e.message).isEqualTo("Attachment.hash is not valid")
        }

        verify(exactly = 1) { cryptoService.generateGCKey() }
        verify(exactly = 1) { attachmentService.getValidHash(wrappedAttachment) }
    }

    // ToDo empty ValidAttachments
    @Test
    @Throws(DataValidationException.ExpectedFieldViolation::class,
            DataValidationException.IdUsageViolation::class,
            DataValidationException.InvalidAttachmentPayloadHash::class)
    fun `Given, uploadData is called with DecryptedRecord, which contains a non DataResource, and UserId, it uploads the attachment and updates them`() {
        // Given
        val decryptedRecord = mockk<NetworkRecordContract.DecryptedRecord<WrapperContract.Resource>>(relaxed = true)
        val resource = mockk<WrapperContract.Resource>()
        val rawResource = mockk<DocumentReference>()
        val attachmentKey = mockk<GCKey>()
        val wrappedAttachment = mockk<WrapperContract.Attachment>()
        val serviceAttachment = "A attachment"
        val serviceAttachments = mutableListOf<Any>( serviceAttachment )
        val resourceHash = "321"
        val updatedAttachments = mockk<List<Pair<WrapperContract.Attachment, List<String>>>>()

        var unblockKey = false

        every { resource.type } returns WrapperContract.Resource.TYPE.FHIR3
        every { resource.unwrap() } returns rawResource
        every { decryptedRecord.resource } returns resource
        every { decryptedRecord.attachmentsKey } answers { if(unblockKey) {
            attachmentKey
        } else {
            unblockKey = true
            null
        } }

        every { fhirAttachmentHelper.hasAttachment(rawResource) } returns true
        every{ fhirAttachmentHelper.getAttachment(rawResource) } returns serviceAttachments
        every{ wrappedAttachment.id } returns null
        every { wrappedAttachment.size } returns 23

        every { attachmentFactory.wrap(serviceAttachment) } returns wrappedAttachment
        every { cryptoService.generateGCKey() } returns Single.just(attachmentKey)

        every { attachmentService.getValidHash(wrappedAttachment) } returns resourceHash
        every {
            attachmentService.upload(listOf(wrappedAttachment), attachmentKey, USER_ID)
        } returns Single.just(updatedAttachments)
        every { wrappedAttachment.hash } returns resourceHash

        every { thumbnailService.updateResourceIdentifier(
                resource,
                updatedAttachments
        ) } returns Unit

        // When
        val record = attachmentClient.uploadData(decryptedRecord, USER_ID)

        assertSame(
                record,
                decryptedRecord
        )

        verify(exactly = 1) { cryptoService.generateGCKey() }
        verify(exactly = 1) { attachmentService.getValidHash(wrappedAttachment) }
        verify(exactly = 1) { attachmentService.upload(listOf(wrappedAttachment), attachmentKey, USER_ID) }
        verify(exactly = 1) { thumbnailService.updateResourceIdentifier(
                resource,
                updatedAttachments
        ) }
    }



    @Test
    fun `Given, updateData is called with a DecryptedRecord, which contains a DataResource, a Resource and UserId, it reflects it`() {
        // Given
        val resource = mockk<WrapperContract.Resource>()
        val decryptedRecord = mockk<NetworkRecordContract.DecryptedRecord<WrapperContract.Resource>>()

        every { resource.type } returns WrapperContract.Resource.TYPE.DATA
        every { decryptedRecord.resource } returns resource

        // When
        val record = attachmentClient.updateData(decryptedRecord, mockk(), USER_ID)

        // Then
        Truth.assertThat(record).isSameInstanceAs(decryptedRecord)

        verify(exactly = 0) { fhirAttachmentHelper.hasAttachment(any()) }
    }

    @Test
    @Throws(DataValidationException.ExpectedFieldViolation::class,
            DataValidationException.IdUsageViolation::class,
            DataValidationException.InvalidAttachmentPayloadHash::class)
    fun `Given, updateData is called with a DecryptedRecord, which contains a non DataResource, a DataResource and a UserId, it fails with a CoreRuntimeExceptionUnsupportedOperation`() {
        // Given
        val resource = mockk<WrapperContract.Resource>()
        val newResource = mockk<WrapperContract.Resource>()
        val decryptedRecord = mockk<NetworkRecordContract.DecryptedRecord<WrapperContract.Resource>>()

        every { newResource.type } returns WrapperContract.Resource.TYPE.DATA
        every { resource.type } returns WrapperContract.Resource.TYPE.FHIR3
        every { decryptedRecord.resource } returns resource

        try {
            // When
            attachmentClient.updateData(decryptedRecord, newResource, USER_ID)
        } catch (e: CoreRuntimeException) {
            // Then
            Truth.assertThat(e.javaClass).isEqualTo(CoreRuntimeException.UnsupportedOperation::class.java)
        }
    }

    //TODO: NULL new Resource -> critical!!!

    @Test
    @Throws(DataValidationException.ExpectedFieldViolation::class,
            DataValidationException.IdUsageViolation::class,
            DataValidationException.InvalidAttachmentPayloadHash::class)
    fun `Given, updateData is called with a DecryptedRecord, which contains a non DataResource, a non DataResource and a UserId, it fails, if has no size is present`() {
        // Given
        val decryptedRecord = mockk<NetworkRecordContract.DecryptedRecord<WrapperContract.Resource>>(relaxed = true)
        val newResource = mockk<WrapperContract.Resource>()
        val newRawResource = mockk<DocumentReference>()
        val resource = mockk<WrapperContract.Resource>()
        val rawResource = mockk<DocumentReference>()
        val attachmentKey = mockk<GCKey>()
        val wrappedAttachment = mockk<WrapperContract.Attachment>()
        val serviceAttachment = "A attachment"
        val serviceAttachments = mutableListOf<Any>( serviceAttachment )
        val resourceHash = "321"


        every { newResource.type } returns WrapperContract.Resource.TYPE.FHIR3
        every { newResource.unwrap() } returns newRawResource

        every { resource.type } returns WrapperContract.Resource.TYPE.FHIR3
        every { resource.unwrap() } returns rawResource

        every { decryptedRecord.resource } returns resource
        every { decryptedRecord.attachmentsKey } returns null

        every { fhirAttachmentHelper.hasAttachment(rawResource) } returns true
        every{ fhirAttachmentHelper.getAttachment(rawResource) } returns serviceAttachments

        every { fhirAttachmentHelper.hasAttachment(newRawResource) } returns true
        every { fhirAttachmentHelper.getAttachment(newRawResource) } returns serviceAttachments

        every{ wrappedAttachment.id } returns null
        every { wrappedAttachment.size } returns null
        every { wrappedAttachment.hash } returns resourceHash

        every { attachmentFactory.wrap(serviceAttachment) } returns wrappedAttachment
        every { cryptoService.generateGCKey() } returns Single.just(attachmentKey)

        every { attachmentService.getValidHash(wrappedAttachment) } returns resourceHash


        // When
        try {
            attachmentClient.updateData(decryptedRecord, newResource, USER_ID)
            Assert.fail("Exception expected!")
        } catch (e: D4LException) {

            // Then
            Truth.assertThat(e.javaClass).isEqualTo(DataValidationException.ExpectedFieldViolation::class.java)
            Truth.assertThat(e.message).isEqualTo("Attachment.hash and Attachment.size expected")
        }

        verify(exactly = 0) { cryptoService.generateGCKey() }
        verify(exactly = 0) { attachmentService.getValidHash(wrappedAttachment) }
    }

    @Test
    @Throws(DataValidationException.ExpectedFieldViolation::class,
            DataValidationException.IdUsageViolation::class,
            DataValidationException.InvalidAttachmentPayloadHash::class)
    fun `Given, updateData is called with a DecryptedRecord, which contains a non DataResource, a non DataResource and a UserId, it fails, if has no hash is present`() {
        // Given
        val decryptedRecord = mockk<NetworkRecordContract.DecryptedRecord<WrapperContract.Resource>>(relaxed = true)
        val newResource = mockk<WrapperContract.Resource>()
        val newRawResource = mockk<DocumentReference>()
        val resource = mockk<WrapperContract.Resource>()
        val rawResource = mockk<DocumentReference>()
        val attachmentKey = mockk<GCKey>()
        val wrappedAttachment = mockk<WrapperContract.Attachment>()
        val serviceAttachment = "A attachment"
        val serviceAttachments = mutableListOf<Any>( serviceAttachment )
        val attachmentServiceHash = "123"

        every { newResource.type } returns WrapperContract.Resource.TYPE.FHIR3
        every { newResource.unwrap() } returns newRawResource

        every { resource.type } returns WrapperContract.Resource.TYPE.FHIR3
        every { resource.unwrap() } returns rawResource

        every { decryptedRecord.resource } returns resource
        every { decryptedRecord.attachmentsKey } returns null

        every { fhirAttachmentHelper.hasAttachment(rawResource) } returns true
        every { fhirAttachmentHelper.getAttachment(rawResource) } returns serviceAttachments

        every { fhirAttachmentHelper.hasAttachment(newRawResource) } returns true
        every { fhirAttachmentHelper.getAttachment(newRawResource) } returns serviceAttachments

        every { wrappedAttachment.id } returns null
        every { wrappedAttachment.size } returns 23
        every { wrappedAttachment.hash } returns null

        every { attachmentFactory.wrap(serviceAttachment) } returns wrappedAttachment
        every { cryptoService.generateGCKey() } returns Single.just(attachmentKey)

        every { attachmentService.getValidHash(wrappedAttachment) } returns attachmentServiceHash


        // When
        try {
            attachmentClient.updateData(decryptedRecord, newResource, USER_ID)
            Assert.fail("Exception expected!")
        } catch (e: D4LException) {

            // Then
            Truth.assertThat(e.javaClass).isEqualTo(DataValidationException.ExpectedFieldViolation::class.java)
            Truth.assertThat(e.message).isEqualTo("Attachment.hash and Attachment.size expected")
        }

        verify(exactly = 0) { cryptoService.generateGCKey() }
        verify(exactly = 0) { attachmentService.getValidHash(wrappedAttachment) }
    }


    @Test
    @Throws(DataValidationException.ExpectedFieldViolation::class,
            DataValidationException.IdUsageViolation::class,
            DataValidationException.InvalidAttachmentPayloadHash::class)
    fun `Given, updateData is called with a DecryptedRecord, which contains a non DataResource, a non DataResource and a UserId, it fails, if the hash is invalid`() {
        // Given
        val decryptedRecord = mockk<NetworkRecordContract.DecryptedRecord<WrapperContract.Resource>>(relaxed = true)
        val newResource = mockk<WrapperContract.Resource>()
        val newRawResource = mockk<DocumentReference>()
        val resource = mockk<WrapperContract.Resource>()
        val rawResource = mockk<DocumentReference>()
        val attachmentKey = mockk<GCKey>()
        val wrappedAttachment = mockk<WrapperContract.Attachment>()
        val serviceAttachment = "A attachment"
        val serviceAttachments = mutableListOf<Any>( serviceAttachment )
        val attachmentServiceHash = "123"
        val resourceHash = "321"

        every { newResource.type } returns WrapperContract.Resource.TYPE.FHIR3
        every { newResource.unwrap() } returns newRawResource

        every { resource.type } returns WrapperContract.Resource.TYPE.FHIR3
        every { resource.unwrap() } returns rawResource

        every { decryptedRecord.resource } returns resource
        every { decryptedRecord.attachmentsKey } returns null

        every { fhirAttachmentHelper.hasAttachment(rawResource) } returns true
        every { fhirAttachmentHelper.getAttachment(rawResource) } returns serviceAttachments

        every { fhirAttachmentHelper.hasAttachment(newRawResource) } returns true
        every { fhirAttachmentHelper.getAttachment(newRawResource) } returns serviceAttachments

        every { wrappedAttachment.id } returns null
        every { wrappedAttachment.size } returns 23
        every { wrappedAttachment.hash } returns resourceHash

        every { attachmentFactory.wrap(serviceAttachment) } returns wrappedAttachment
        every { cryptoService.generateGCKey() } returns Single.just(attachmentKey)

        every { attachmentService.getValidHash(wrappedAttachment) } returns attachmentServiceHash


        // When
        try {
            attachmentClient.updateData(decryptedRecord, newResource, USER_ID)
            Assert.fail("Exception expected!")
        } catch (e: D4LException) {

            // Then
            Truth.assertThat(e.javaClass).isEqualTo(DataValidationException.InvalidAttachmentPayloadHash::class.java)
            Truth.assertThat(e.message).isEqualTo("Attachment.hash is not valid")
        }

        verify(exactly = 0) { cryptoService.generateGCKey() }
        verify(exactly = 1) { attachmentService.getValidHash(wrappedAttachment) }
    }


    @Test
    @Throws(DataValidationException.IdUsageViolation::class,
            DataValidationException.ExpectedFieldViolation::class,
            DataValidationException.InvalidAttachmentPayloadHash::class)
    fun `Given, updateData is called with a DecryptedRecord, which contains a non DataResource, a non DataResource and a UserId, it fails, if has no fitting old resource was found`(){
        // Given
        val decryptedRecord = mockk<NetworkRecordContract.DecryptedRecord<WrapperContract.Resource>>(relaxed = true)
        val newResource = mockk<WrapperContract.Resource>()
        val newRawResource = mockk<DocumentReference>()
        val oldResource = mockk<WrapperContract.Resource>()
        val oldRawResource = mockk<Fhir3Resource>()
        val attachmentKey = mockk<GCKey>()
        val newWrappedAttachment = mockk<WrapperContract.Attachment>()
        val oldWrappedAttachment = mockk<WrapperContract.Attachment>()
        val newServiceAttachment = "A old attachment"
        val oldServiceAttachment = "A new attachment"
        val resourceHash = "321"

        every { newResource.type } returns WrapperContract.Resource.TYPE.FHIR3
        every { newResource.unwrap() } returns newRawResource

        every { oldResource.type } returns WrapperContract.Resource.TYPE.FHIR3
        every { oldResource.unwrap() } returns oldRawResource

        every { decryptedRecord.resource } returns oldResource
        every { decryptedRecord.attachmentsKey } returns null

        every { fhirAttachmentHelper.hasAttachment(oldRawResource) } returns true
        every { fhirAttachmentHelper.getAttachment(oldRawResource) } returns  mutableListOf( oldServiceAttachment )

        every { fhirAttachmentHelper.hasAttachment(newRawResource) } returns true
        every { fhirAttachmentHelper.getAttachment(newRawResource) } returns  mutableListOf( newServiceAttachment )

        every { attachmentFactory.wrap(newServiceAttachment) } returns newWrappedAttachment
        every { attachmentFactory.wrap(oldServiceAttachment) } returns oldWrappedAttachment

        every { newWrappedAttachment.id } returns "notqweqweqnull"
        every { newWrappedAttachment.size } returns 23
        every { newWrappedAttachment.hash } returns resourceHash

        every { oldWrappedAttachment.id } returns "asdasd"
        every { oldWrappedAttachment.size } returns 23
        every { oldWrappedAttachment.hash } returns resourceHash


        every { cryptoService.generateGCKey() } returns Single.just(attachmentKey)

        every { attachmentService.getValidHash(newWrappedAttachment) } returns resourceHash
        every { attachmentService.getValidHash(oldWrappedAttachment) } returns resourceHash


        // When
        try {
            attachmentClient.updateData(decryptedRecord, newResource, USER_ID)
            Assert.fail("Exception expected!")
        } catch (e: D4LException) {

            // Then
            Truth.assertThat(e.javaClass).isEqualTo(DataValidationException.IdUsageViolation::class.java)
            Truth.assertThat(e.message).isEqualTo("Valid Attachment.id expected")
        }

        verify(exactly = 0) { cryptoService.generateGCKey() }
        verify(exactly = 1) { attachmentService.getValidHash(newWrappedAttachment) }
    }

    @Test
    @Throws(DataValidationException.ExpectedFieldViolation::class,
            DataValidationException.IdUsageViolation::class,
            DataValidationException.InvalidAttachmentPayloadHash::class)
    fun `Given, updateData is called with a DecryptedRecord, which contains a non DataResource, a non DataResource and a UserId, it updates Attachments`() {
        // Given
        val decryptedRecord = mockk<NetworkRecordContract.DecryptedRecord<WrapperContract.Resource>>(relaxed = true)
        val newResource = mockk<WrapperContract.Resource>()
        val newRawResource = mockk<DocumentReference>()
        val oldResource = mockk<WrapperContract.Resource>()
        val oldRawResource = mockk<Fhir3Resource>()
        val attachmentKey = mockk<GCKey>()
        val newWrappedAttachment = mockk<WrapperContract.Attachment>()
        val oldWrappedAttachment = mockk<WrapperContract.Attachment>()
        val newServiceAttachment = "A old attachment"
        val oldServiceAttachment = "A new attachment"
        val resourceHash = "321"

        val updatedAttachments = mockk<List<Pair<WrapperContract.Attachment, List<String>>>>()

        every { newResource.type } returns WrapperContract.Resource.TYPE.FHIR3
        every { newResource.unwrap() } returns newRawResource

        every { oldResource.type } returns WrapperContract.Resource.TYPE.FHIR3
        every { oldResource.unwrap() } returns oldRawResource

        every { decryptedRecord.resource } returns oldResource
        every { decryptedRecord.attachmentsKey } returns attachmentKey

        every { fhirAttachmentHelper.hasAttachment(oldRawResource) } returns true
        every { fhirAttachmentHelper.getAttachment(oldRawResource) } returns  mutableListOf( oldServiceAttachment )

        every { fhirAttachmentHelper.hasAttachment(newRawResource) } returns true
        every { fhirAttachmentHelper.getAttachment(newRawResource) } returns  mutableListOf( newServiceAttachment )

        every { attachmentFactory.wrap(newServiceAttachment) } returns newWrappedAttachment
        every { attachmentFactory.wrap(oldServiceAttachment) } returns oldWrappedAttachment

        every { newWrappedAttachment.id } returns null
        every { newWrappedAttachment.size } returns 23
        every { newWrappedAttachment.hash } returns resourceHash

        every { oldWrappedAttachment.id } returns null
        every { oldWrappedAttachment.size } returns 23
        every { oldWrappedAttachment.hash } returns resourceHash


        every { cryptoService.generateGCKey() } returns Single.just(attachmentKey)

        every { attachmentService.getValidHash(newWrappedAttachment) } returns resourceHash
        every { attachmentService.getValidHash(oldWrappedAttachment) } returns resourceHash

        every {
            attachmentService.upload(listOf(newWrappedAttachment), attachmentKey, USER_ID)
        } returns Single.just(updatedAttachments)

        every { thumbnailService.updateResourceIdentifier(
                newResource,
                updatedAttachments
        ) } returns Unit


        // When
        val record = attachmentClient.updateData(decryptedRecord, newResource, USER_ID)

        assertSame(
                record,
                decryptedRecord
        )

        verify(exactly = 0) { cryptoService.generateGCKey() }
        verify(exactly = 1) { attachmentService.getValidHash(newWrappedAttachment) }
        verify(exactly = 1) { attachmentService.upload(listOf(newWrappedAttachment), attachmentKey, USER_ID) }
        verify(exactly = 1) { thumbnailService.updateResourceIdentifier(
                newResource,
                updatedAttachments
        ) }
    }

    @Test
    fun `Given, downloadData is called with a DecryptedRecord, which contains a DataResource, and a UserId, it reflects it`() {
        // Given
        val resource = mockk<WrapperContract.Resource>()
        val decryptedRecord = mockk<NetworkRecordContract.DecryptedRecord<WrapperContract.Resource>>()

        every { resource.type } returns WrapperContract.Resource.TYPE.DATA
        every { decryptedRecord.resource } returns resource

        // When
        val record = attachmentClient.downloadData(decryptedRecord, USER_ID)

        // Then
        Truth.assertThat(record).isSameInstanceAs(decryptedRecord)

        verify(exactly = 0) { fhirAttachmentHelper.hasAttachment(any()) }
    }

    // TODO: hasAttachment == false

    @Test
    @Throws(DataValidationException.ExpectedFieldViolation::class,
            DataValidationException.IdUsageViolation::class,
            DataValidationException.InvalidAttachmentPayloadHash::class)
    fun `Given, downloadData is called with a DecryptedRecord, which contains a non DataResource, and a UserId, it fails, if the Attachment has no ID`() {
        // Given
        val decryptedRecord = mockk<NetworkRecordContract.DecryptedRecord<WrapperContract.Resource>>(relaxed = true)
        val resource = mockk<WrapperContract.Resource>()
        val rawResource = mockk<DocumentReference>()
        val attachmentKey = mockk<GCKey>()
        val wrappedAttachment = mockk<WrapperContract.Attachment>()
        val serviceAttachment = "A attachment"
        val serviceAttachments = mutableListOf<Any>( serviceAttachment )

        every { resource.type } returns WrapperContract.Resource.TYPE.FHIR3
        every { resource.unwrap() } returns rawResource
        every { decryptedRecord.resource } returns resource
        every { decryptedRecord.attachmentsKey } returns null

        every { fhirAttachmentHelper.hasAttachment(rawResource) } returns true
        every{ fhirAttachmentHelper.getAttachment(rawResource) } returns serviceAttachments

        every { attachmentFactory.wrap(serviceAttachment) } returns wrappedAttachment

        every{ wrappedAttachment.id } returns null

        // When
        try {
            attachmentClient.downloadData(decryptedRecord, USER_ID)
            Assert.fail("Exception expected!")
        } catch (e: D4LException) {

            // Then
            Truth.assertThat(e.javaClass).isEqualTo(DataValidationException.IdUsageViolation::class.java)
            Truth.assertThat(e.message).isEqualTo("Attachment.id expected")
        }
    }

    @Test
    @Throws(DataValidationException.IdUsageViolation::class, DataValidationException.InvalidAttachmentPayloadHash::class)
    fun `Given, downloadData is called with a DecryptedRecord, which contains a non DataResource, and a UserId, it downloads attachments`() {
        // Given
        val decryptedRecord = mockk<NetworkRecordContract.DecryptedRecord<WrapperContract.Resource>>(relaxed = true)
        val resource = mockk<WrapperContract.Resource>()
        val rawResource = mockk<DocumentReference>()
        val attachmentKey = mockk<GCKey>()
        val wrappedAttachment = mockk<WrapperContract.Attachment>()
        val serviceAttachment = "A attachment"
        val serviceAttachments = mutableListOf<Any>( serviceAttachment )

        every { resource.type } returns WrapperContract.Resource.TYPE.FHIR3
        every { resource.unwrap() } returns rawResource
        every { decryptedRecord.resource } returns resource
        every { decryptedRecord.attachmentsKey } returns attachmentKey

        every { fhirAttachmentHelper.hasAttachment(rawResource) } returns true
        every{ fhirAttachmentHelper.getAttachment(rawResource) } returns serviceAttachments

        every { attachmentFactory.wrap(serviceAttachment) } returns wrappedAttachment

        every{ wrappedAttachment.id } returns "abs"
        every{ attachmentService.download(
                listOf(wrappedAttachment),
                attachmentKey,
                USER_ID
        ) } returns Single.just(mockk())

        val record = attachmentClient.downloadData(decryptedRecord, USER_ID)

        // Then
        Truth.assertThat(record).isSameInstanceAs(decryptedRecord)

        verify(exactly = 1) { attachmentService.download(
                listOf(wrappedAttachment),
                attachmentKey,
                USER_ID
        ) }
    }
}
