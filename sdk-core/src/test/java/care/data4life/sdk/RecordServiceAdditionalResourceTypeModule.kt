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
import care.data4life.fhir.r4.model.Extension
import care.data4life.sdk.attachment.AttachmentContract
import care.data4life.sdk.attachment.ThumbnailService.Companion.SPLIT_CHAR
import care.data4life.sdk.config.DataRestriction.DATA_SIZE_MAX_BYTES
import care.data4life.sdk.config.DataRestrictionException
import care.data4life.sdk.fhir.Fhir3Attachment
import care.data4life.sdk.fhir.Fhir3Resource
import care.data4life.sdk.fhir.Fhir4Attachment
import care.data4life.sdk.fhir.Fhir4Resource
import care.data4life.sdk.fhir.FhirContract
import care.data4life.sdk.model.DownloadType
import care.data4life.sdk.network.model.DecryptedR4Record
import care.data4life.sdk.network.model.DecryptedRecord
import care.data4life.sdk.network.model.EncryptedRecord
import care.data4life.sdk.network.model.definitions.DecryptedBaseRecord
import care.data4life.sdk.record.RecordContract.Service.Companion.DOWNSCALED_ATTACHMENT_IDS_FMT
import care.data4life.sdk.tag.TaggingContract
import care.data4life.sdk.util.Base64
import care.data4life.sdk.util.MimeType
import care.data4life.sdk.wrapper.SdkAttachmentFactory
import care.data4life.sdk.wrapper.SdkFhirParser
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import io.reactivex.Single
import org.junit.Assume
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.nio.charset.StandardCharsets
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue
import care.data4life.fhir.r4.model.Identifier as Fhir4Identifier
import care.data4life.fhir.r4.model.Observation as Fhir4Observation
import care.data4life.fhir.r4.model.Patient as Fhir4Patient
import care.data4life.fhir.r4.model.Questionnaire as Fhir4Questionnaire
import care.data4life.fhir.r4.model.QuestionnaireResponse as Fhir4QuestionnaireResponse
import care.data4life.fhir.r4.util.FhirAttachmentHelper as Fhir4AttachmentHelper
import care.data4life.fhir.stu3.model.Identifier as Fhir3Identifier
import care.data4life.fhir.stu3.model.Medication as Fhir3Medication
import care.data4life.fhir.stu3.model.Observation as Fhir3Observation
import care.data4life.fhir.stu3.model.Patient as Fhir3Patient
import care.data4life.fhir.stu3.model.Questionnaire as Fhir3Questionnaire
import care.data4life.fhir.stu3.model.QuestionnaireResponse as Fhir3QuestionnaireResponse
import care.data4life.fhir.stu3.util.FhirAttachmentHelper as Fhir3AttachmentHelper


@RunWith(Parameterized::class)
class RecordServiceAdditionalResourceTypeModule {
    private lateinit var recordService: RecordService
    private lateinit var apiService: ApiService
    private lateinit var cryptoService: CryptoService
    private lateinit var fhirService: FhirContract.Service
    private lateinit var tagEncryptionService: TaggingContract.EncryptionService
    private lateinit var taggingService: TaggingContract.Service
    private lateinit var attachmentService: AttachmentContract.Service
    private lateinit var errorHandler: SdkContract.ErrorHandler

    @Before
    fun setUp() {
        apiService = mockk()
        cryptoService = mockk()
        fhirService = mockk()
        tagEncryptionService = mockk()
        taggingService = mockk()
        attachmentService = mockk()
        errorHandler = mockk()

        recordService = spyk(RecordService(
                PARTNER_ID,
                ALIAS,
                apiService,
                tagEncryptionService,
                taggingService,
                fhirService,
                attachmentService,
                cryptoService,
                errorHandler
        ))
    }
    
    @Parameterized.Parameter(value = 0)
    lateinit var resourceType: String

    @Parameterized.Parameter(value = 1)
    lateinit var resourceName: String

    @Parameterized.Parameter(value = 2)
    lateinit var resourcePrefixes: List<String>

    @Parameterized.Parameter(value = 3)
    lateinit var expectedSize: String

    private fun determineFhir3Folder(
            resourcePrefixes: List<String>
    ): String {
        return if (resourcePrefixes.contains("common")) {
            "common"
        } else {
            "fhir3"
        }
    }

    private fun determineFhir4Folder(
            resourcePrefixes: List<String>
    ): String {
        return if (resourcePrefixes.contains("common")) {
            "common"
        } else {
            "fhir4"
        }
    }

    private fun getFhireResource(
            resourceFolder: String,
            resourceName: String
    ): String = this::class.java.getResource(
            "/$resourceFolder/$resourceName.json"
    ).readText(StandardCharsets.UTF_8)

    private fun selectObservation(resource: Fhir3Observation): Fhir3Attachment {
        return if (resource.component == null) {
            resource.valueAttachment!!
        } else {
            resource.component!![0].valueAttachment!!
        }
    }

    private fun selectFhir3Attachment(resource: Fhir3Resource): Fhir3Attachment {
        return when (resource) {
            is Fhir3Medication -> resource.image!![0]
            is Fhir3Observation -> selectObservation(resource)
            is Fhir3Patient -> resource.photo!![0]
            is Fhir3Questionnaire -> resource.item!![0].initialAttachment!!
            is Fhir3QuestionnaireResponse -> resource.item!![0].answer!![0].valueAttachment!!
            else -> throw RuntimeException("Unexpected resource")
        }
    }

    private fun selectFhir4Attachment(resource: Fhir4Resource): Fhir4Attachment {
        return when (resource) {
            is Fhir4Observation -> resource.component!![0].extension!![0]!!.valueAttachment!!
            is Fhir4Patient -> resource.photo!![0]
            is Fhir4Questionnaire -> resource.item!![0].initial!![0]!!.valueAttachment!!
            is Fhir4QuestionnaireResponse -> resource.item!![0].answer!![0].valueAttachment!!
            else -> throw RuntimeException("Unexpected resource")
        }
    }

    private fun setFhir3Id(
            resource: Fhir3Resource,
            identifiers: List<Fhir3Identifier>
    ) {
        when (resource) {
            is Fhir3Medication -> return
            is Fhir3Observation -> resource.identifier = identifiers
            is Fhir3Patient -> resource.identifier = identifiers
            is Fhir3Questionnaire -> resource.identifier = identifiers
            is Fhir3QuestionnaireResponse -> resource.identifier = identifiers[0]
            else -> throw RuntimeException("Unexpected resource")
        }
    }

    private fun setFhir4Id(
            resource: Fhir4Resource,
            identifiers: List<Fhir4Identifier>
    ) {
        when (resource) {
            is Fhir4Observation -> resource.identifier = identifiers
            is Fhir4Patient -> resource.identifier = identifiers
            is Fhir4Questionnaire -> resource.identifier = identifiers
            is Fhir4QuestionnaireResponse -> resource.identifier = identifiers[0]
            else -> throw RuntimeException("Unexpected resource")
        }
    }

    private fun setFhir3Attachment(resource: Fhir3Resource, attachments: List<Fhir3Attachment>) {
        return when (resource) {
            is Fhir3Medication -> resource.image = attachments
            is Fhir3Observation -> {
                resource.component = mutableListOf(
                        Fhir3Observation.ObservationComponent(null),
                        Fhir3Observation.ObservationComponent(null)
                )
                resource.component!![0].valueAttachment = attachments[0]
                resource.component!![1].valueAttachment = attachments[1]
            }
            is Fhir3Patient -> resource.photo = attachments
            is Fhir3Questionnaire -> {
                resource.item = mutableListOf(
                        Fhir3Questionnaire.QuestionnaireItem("", null),
                        Fhir3Questionnaire.QuestionnaireItem("", null)
                )
                resource.item!![0].initialAttachment = attachments[0]
                resource.item!![1].initialAttachment = attachments[1]
            }
            is Fhir3QuestionnaireResponse -> {
                resource.item!![0].answer!![0].valueAttachment = attachments[0]
                resource.item!![0].answer!!.add(Fhir3QuestionnaireResponse.QuestionnaireResponseItemAnswer())
                resource.item!![0].answer!![1].valueAttachment = attachments[1]
            }
            else -> throw RuntimeException("Unexpected resource")
        }
    }

    private fun setFhir4Attachment(resource: Fhir4Resource, attachments: List<Fhir4Attachment>) {
        return when (resource) {
            is Fhir4Observation -> {
                resource.component!![0].extension = mutableListOf(
                        Extension("a"),
                        Extension("b")
                )


                resource.component!![0].extension!![0].valueAttachment = attachments[0]
                resource.component!![0].extension!![1].valueAttachment = attachments[1]
            }
            is Fhir4Patient -> resource.photo = attachments
            is Fhir4Questionnaire -> {
                resource.item = mutableListOf(
                        Fhir4Questionnaire.QuestionnaireItem("", null),
                        Fhir4Questionnaire.QuestionnaireItem("", null)
                )
                resource.item!![0].initial = mutableListOf(Fhir4Questionnaire.QuestionnaireItemInitial(null))
                resource.item!![0].initial!![0].valueAttachment = attachments[0]
                resource.item!![1].initial = mutableListOf(Fhir4Questionnaire.QuestionnaireItemInitial(null))
                resource.item!![1].initial!![0].valueAttachment = attachments[1]
            }
            is Fhir4QuestionnaireResponse -> {
                resource.item!![0].answer!![0].valueAttachment = attachments[0]
                resource.item!![0].answer!!.add(Fhir4QuestionnaireResponse.QuestionnaireResponseItemAnswer())
                resource.item!![0].answer!![1].valueAttachment = attachments[1]
            }
            else -> throw RuntimeException("Unexpected resource")
        }
    }

    @Test
    fun `Given, extractUploadData is called with a Fhir3Resource, it extracts its dataset`() {
        Assume.assumeTrue(resourcePrefixes.contains("fhir3") || resourcePrefixes.contains("common"))

        // Given
        val resource = SdkFhirParser.toFhir3(
                resourceType,
                getFhireResource(determineFhir3Folder(resourcePrefixes), resourceName)
        )

        // When
        val data = recordService.extractUploadData(resource)

        // Then
        assertEquals(
                actual = data?.size,
                expected = expectedSize.toInt()
        )
        assertEquals(
                actual = data!![selectFhir3Attachment(resource)],
                expected = DATA
        )
    }

    @Test
    fun `Given, extractUploadData is called with a Fhir4Resource, it extracts its dataset`() {
        Assume.assumeTrue(resourcePrefixes.contains("fhir4") || resourcePrefixes.contains("common"))

        // Given
        val resource = SdkFhirParser.toFhir4(
                resourceType,
                getFhireResource(determineFhir4Folder(resourcePrefixes), resourceName)
        )
        // When
        val data = recordService.extractUploadData(resource)

        // Then
        if (resource !is Fhir4Observation) {
            assertEquals(
                    actual = data!!.size,
                    expected = expectedSize.toInt()
            )
        }
        assertEquals(
                actual = data!![selectFhir4Attachment(resource)]!!,
                expected = DATA
        )
    }

    @Test
    fun `Given, extractUploadData is called with a Fhir3Resource, it returns null, if the attachment is null`() {
        Assume.assumeTrue(resourcePrefixes.contains("fhir3") || resourcePrefixes.contains("common"))

        // Given
        val resource = SdkFhirParser.toFhir3(
                resourceType,
                getFhireResource(determineFhir3Folder(resourcePrefixes), "$resourceName-nulled-attachment")
        )

        // When
        val data = recordService.extractUploadData(resource)

        // Then
        if (resource is Fhir3Observation && resource.component != null) {
            assertEquals(
                    actual = data!!.size,
                    expected = 1
            )
        } else {
            assertNull(data)
        }
    }

    @Test
    fun `Given, extractUploadData is called with a Fhir4Resource, it returns null, if the attachment is null`() {
        Assume.assumeTrue(resourcePrefixes.contains("fhir4") || resourcePrefixes.contains("common"))

        // Given
        val resource = SdkFhirParser.toFhir4(
                resourceType,
                getFhireResource(determineFhir4Folder(resourcePrefixes), "$resourceName-nulled-attachment")
        )

        // When
        val data = recordService.extractUploadData(resource)

        // Then
        assertNull(data)
    }

    @Test
    fun `Given, removeUploadData is called with a Fhir3Resource, it removes the upload payload from the resource`() {
        Assume.assumeTrue(resourcePrefixes.contains("fhir3") || resourcePrefixes.contains("common"))

        // Given
        val resource = SdkFhirParser.toFhir3(
                resourceType,
                getFhireResource(determineFhir3Folder(resourcePrefixes), resourceName)
        )

        val decryptedRecord = DecryptedRecord(
                null,
                resource,
                null,
                arrayListOf(),
                null,
                null,
                null,
                null,
                -1
        )

        // When
        val stripedRecord = recordService.removeUploadData(decryptedRecord)

        // Then
        assertEquals(
                actual = stripedRecord,
                expected = decryptedRecord
        )
        assertEquals(
                actual = stripedRecord.resource,
                expected = decryptedRecord.resource
        )
        assertNull(selectFhir3Attachment(stripedRecord.resource).data)
        if (stripedRecord.resource is Fhir3Observation) {
            assertNull((stripedRecord.resource as Fhir3Observation).valueAttachment!!.data)
        }
    }

    @Test
    fun `Given, removeUploadData is called with a Fhir4Resource, it removes the upload payload from the resource`() {
        Assume.assumeTrue(resourcePrefixes.contains("fhir4") || resourcePrefixes.contains("common"))

        // Given
        val resource = SdkFhirParser.toFhir4(
                resourceType,
                getFhireResource(determineFhir4Folder(resourcePrefixes), resourceName)
        )

        val decryptedRecord = DecryptedR4Record(
                null,
                resource,
                null,
                arrayListOf(),
                null,
                null,
                null,
                null,
                -1
        )

        // When
        val stripedRecord = recordService.removeUploadData(decryptedRecord)

        // Then
        assertEquals(
                actual = stripedRecord,
                expected = decryptedRecord
        )
        assertEquals(
                actual = stripedRecord.resource,
                expected = decryptedRecord.resource
        )
        assertNull(selectFhir4Attachment(stripedRecord.resource).data)
    }

    @Test
    fun `Given, restoreUploadData is called with a striped Fhir3Resource, it restores the upload payload from the resource`() {
        Assume.assumeTrue(resourcePrefixes.contains("fhir3") || resourcePrefixes.contains("common"))

        // Given
        val originalResource = SdkFhirParser.toFhir3(
                resourceType,
                getFhireResource(determineFhir3Folder(resourcePrefixes), resourceName)
        )

        val originalPayload: HashMap<Any, String?> = hashMapOf(selectFhir3Attachment(originalResource) to DATA)

        val decryptedRecord = DecryptedRecord(
                null,
                originalResource,
                null,
                arrayListOf(),
                null,
                null,
                null,
                null,
                -1
        )

        val stripedRecord = recordService.removeUploadData(decryptedRecord)

        // When
        val record = recordService.restoreUploadData(
                stripedRecord,
                stripedRecord.resource,
                originalPayload
        )

        // Then
        assertEquals(
                actual = record,
                expected = decryptedRecord
        )
        assertEquals(
                actual = record.resource,
                expected = decryptedRecord.resource
        )
        assertEquals(
                actual = selectFhir3Attachment(record.resource).data,
                expected = DATA
        )
    }

    @Test
    fun `Given, restoreUploadData is called with a Fhir4Resource, it restores the upload payload from the resource`() {
        Assume.assumeTrue(resourcePrefixes.contains("fhir4") || resourcePrefixes.contains("common"))

        // Given
        val originalResource = SdkFhirParser.toFhir4(
                resourceType,
                getFhireResource(determineFhir4Folder(resourcePrefixes), resourceName)
        )

        val originalPayload: HashMap<Any, String?> = hashMapOf(selectFhir4Attachment(originalResource) to DATA)

        val decryptedRecord = DecryptedR4Record(
                null,
                originalResource,
                null,
                arrayListOf(),
                null,
                null,
                null,
                null,
                -1
        )

        val stripedRecord = recordService.removeUploadData(decryptedRecord)

        // When
        val record = recordService.restoreUploadData(
                stripedRecord,
                stripedRecord.resource,
                originalPayload
        )

        // Then
        assertEquals(
                actual = record,
                expected = decryptedRecord
        )
        assertEquals(
                actual = record.resource,
                expected = decryptedRecord.resource
        )
        assertEquals(
                actual = selectFhir4Attachment(record.resource).data,
                expected = DATA
        )
    }

    private fun validateFhir3CleanedIdentifiers(
            resource: Fhir3Resource,
            identifiers: MutableList<Fhir3Identifier>
    ) {
        identifiers.removeAt(1)
        when (resource) {
            is Fhir3Medication ->
                assertEquals(
                        actual = selectFhir3Attachment(resource).id,
                        expected = ATTACHMENT_ID
                )
            is Fhir3Observation -> {
                val expected = if (resource.component == null) {
                    identifiers.removeAt(0)
                    2
                } else {
                    3
                }

                assertEquals(
                        actual = resource.identifier!!.size,
                        expected = expected
                )
                assertEquals(
                        actual = resource.identifier,
                        expected = identifiers
                )
            }
            is Fhir3Patient -> {
                identifiers.removeAt(2)
                assertEquals(
                        actual = resource.identifier!!.size,
                        expected = 2
                )
                assertEquals(
                        actual = resource.identifier,
                        expected = identifiers
                )
            }
            is Fhir3Questionnaire -> {
                identifiers.removeAt(2)
                assertEquals(
                        actual = resource.identifier!!.size,
                        expected = 2
                )
                assertEquals(
                        actual = resource.identifier,
                        expected = identifiers
                )
            }
            is Fhir3QuestionnaireResponse -> {
                assertEquals(
                        actual = resource.identifier,
                        expected = identifiers[0]
                )
            }
            else -> throw RuntimeException("Unexpected resource")
        }
    }

    @Test
    fun `Given, cleanObsoleteAdditionalIdentifiers is called with a Fhir3Resource, it cleans up amended identifiers`() {
        Assume.assumeTrue(resourcePrefixes.contains("fhir3") || resourcePrefixes.contains("common"))

        // Given
        val resource = SdkFhirParser.toFhir3(
                resourceType,
                getFhireResource(determineFhir3Folder(resourcePrefixes), resourceName)
        )

        val currentIdentifier = Fhir3AttachmentHelper.buildIdentifier(ADDITIONAL_ID, ASSIGNER)
        val obsoleteIdentifier = Fhir3AttachmentHelper.buildIdentifier(OBSOLETE_ID, ASSIGNER)
        val otherIdentifier = Fhir3AttachmentHelper.buildIdentifier(OTHER_ID, ASSIGNER)
        val valueIdentifier = Fhir3AttachmentHelper.buildIdentifier(VALUE_ID, ASSIGNER)

        val identifiers = mutableListOf(currentIdentifier, obsoleteIdentifier, otherIdentifier, valueIdentifier)

        selectFhir3Attachment(resource).id = ATTACHMENT_ID
        if (resource is Fhir3Observation) {
            resource.valueAttachment!!.id = VALUE_INDICATOR
        }
        setFhir3Id(resource, identifiers)

        recordService.cleanObsoleteAdditionalIdentifiers(resource)

        validateFhir3CleanedIdentifiers(resource, identifiers)
    }

    private fun validateFhir4CleanedIdentifiers(
            resource: Fhir4Resource,
            identifiers: MutableList<Fhir4Identifier>
    ) {
        identifiers.removeAt(1)
        when (resource) {
            is Fhir4Observation -> {
                assertEquals(
                        actual = resource.identifier!!.size,
                        expected = 3
                )
                assertEquals(
                        actual = resource.identifier,
                        expected = identifiers
                )
            }
            is Fhir4Patient -> {
                identifiers.removeAt(2)
                assertEquals(
                        actual = resource.identifier!!.size,
                        expected = 2
                )
                assertEquals(
                        actual = resource.identifier,
                        expected = identifiers
                )
            }
            is Fhir4Questionnaire -> {
                identifiers.removeAt(2)
                assertEquals(
                        actual = resource.identifier!!.size,
                        expected = 2
                )
                assertEquals(
                        actual = resource.identifier,
                        expected = identifiers
                )
            }
            is Fhir4QuestionnaireResponse -> {
                assertEquals(
                        actual = resource.identifier,
                        expected = identifiers[0]
                )
            }
            else -> throw RuntimeException("Unexpected resource")
        }
    }

    @Test
    fun `Given, cleanObsoleteAdditionalIdentifiers is called with a Fhir4Resource, it cleans up amended identifiers`() {
        Assume.assumeTrue(resourcePrefixes.contains("fhir4") || resourcePrefixes.contains("common"))

        // Given
        val resource = SdkFhirParser.toFhir4(
                resourceType,
                getFhireResource(determineFhir4Folder(resourcePrefixes), resourceName)
        )

        val currentIdentifier = Fhir4AttachmentHelper.buildIdentifier(ADDITIONAL_ID, ASSIGNER)
        val obsoleteIdentifier = Fhir4AttachmentHelper.buildIdentifier(OBSOLETE_ID, ASSIGNER)
        val otherIdentifier = Fhir4AttachmentHelper.buildIdentifier(OTHER_ID, ASSIGNER)
        val valueIdentifier = Fhir4AttachmentHelper.buildIdentifier(VALUE_ID, ASSIGNER)

        val identifiers = mutableListOf(currentIdentifier, obsoleteIdentifier, otherIdentifier, valueIdentifier)

        selectFhir4Attachment(resource).id = ATTACHMENT_ID
        setFhir4Id(resource, identifiers)

        recordService.cleanObsoleteAdditionalIdentifiers(resource)

        validateFhir4CleanedIdentifiers(resource, identifiers)
    }

    @Test
    fun `Given, checkForUnsupportedData is called, with a Fhir3Resource, it does nothing, if the Attachment is valid`() {
        Assume.assumeTrue(resourcePrefixes.contains("fhir3") || resourcePrefixes.contains("common"))

        // Given
        val resource = SdkFhirParser.toFhir3(
                resourceType,
                getFhireResource(determineFhir3Folder(resourcePrefixes), resourceName)
        )

        val payload = PDF

        selectFhir3Attachment(resource).data = payload
        if (resource is Fhir3Observation) {
            resource.valueAttachment!!.data = payload
        }

        // When
        recordService.checkDataRestrictions(resource)

        // Then
        assertTrue(true)
    }

    @Test
    fun `Given, checkForUnsupportedData is called, with a Fhir4Resource, it does nothing, if the Attachment is valid`() {
        Assume.assumeTrue(resourcePrefixes.contains("fhir4") || resourcePrefixes.contains("common"))

        // Given
        val resource = SdkFhirParser.toFhir4(
                resourceType,
                getFhireResource(determineFhir4Folder(resourcePrefixes), resourceName)
        )

        selectFhir4Attachment(resource).data = PDF

        // When
        recordService.checkDataRestrictions(resource)

        // Then
        assertTrue(true)
    }

    @Test
    fun `Given, checkForUnsupportedData is called, with a Fhir3Resource, it fails due to unsupported data`() {
        Assume.assumeTrue(resourcePrefixes.contains("fhir3") || resourcePrefixes.contains("common"))

        val resource = SdkFhirParser.toFhir3(
                resourceType,
                getFhireResource(determineFhir3Folder(resourcePrefixes), resourceName)
        )

        val payload = Base64.encodeToString(byteArrayOf(0))

        selectFhir3Attachment(resource).data = payload
        if (resource is Fhir3Observation) {
            resource.valueAttachment!!.data = payload
        }

        assertFailsWith<DataRestrictionException.UnsupportedFileType> {
            recordService.checkDataRestrictions(resource)
        }
    }

    @Test
    fun `Given, checkForUnsupportedData is called, with a Fhir4Resource, it fails due to unsupported data`() {
        Assume.assumeTrue(resourcePrefixes.contains("fhir4") || resourcePrefixes.contains("common"))

        val resource = SdkFhirParser.toFhir4(
                resourceType,
                getFhireResource(determineFhir4Folder(resourcePrefixes), resourceName)
        )

        selectFhir4Attachment(resource).data = Base64.encodeToString(byteArrayOf(0))

        assertFailsWith<DataRestrictionException.UnsupportedFileType> {
            recordService.checkDataRestrictions(resource)
        }
    }

    @Test
    fun `Given, checkForUnsupportedData is called, with a Fhir3Resource, it fails due  to file size limit breach`() {
        Assume.assumeTrue(resourcePrefixes.contains("fhir3") || resourcePrefixes.contains("common"))

        val resource = SdkFhirParser.toFhir3(
                resourceType,
                getFhireResource(determineFhir3Folder(resourcePrefixes), resourceName)
        )

        val payload = PDF_OVERSIZED

        selectFhir3Attachment(resource).data = payload
        if (resource is Fhir3Observation) {
            resource.valueAttachment!!.data = payload
        }

        assertFailsWith<DataRestrictionException.MaxDataSizeViolation> {
            recordService.checkDataRestrictions(resource)
        }
    }

    @Test
    fun `Given, checkForUnsupportedData is called, with a Fhir4Resource, it fails due  to file size limit breach`() {
        Assume.assumeTrue(resourcePrefixes.contains("fhir4") || resourcePrefixes.contains("common"))

        val resource = SdkFhirParser.toFhir4(
                resourceType,
                getFhireResource(determineFhir4Folder(resourcePrefixes), resourceName)
        )

        selectFhir4Attachment(resource).data = PDF_OVERSIZED

        assertFailsWith<DataRestrictionException.MaxDataSizeViolation> {
            recordService.checkDataRestrictions(resource)
        }
    }

    @Test
    fun `Given, downloadAttachment is called, with a Fhir3Resource, it downloads the attachment`() {
        Assume.assumeTrue(resourcePrefixes.contains("fhir3") || resourcePrefixes.contains("common"))

        // Given
        val resource = SdkFhirParser.toFhir3(
                resourceType,
                getFhireResource(determineFhir3Folder(resourcePrefixes), resourceName)
        )

        val fetchedRecord: EncryptedRecord = mockk()
        val attachmentKey: GCKey = mockk()

        val firstAttachmentId = "1"
        val secondAttachmentId = "2"

        val attachmentIds = listOf(firstAttachmentId, secondAttachmentId)

        val firstAttachment = buildFhir3Attachment(firstAttachmentId)
        val secondAttachment = buildFhir3Attachment(secondAttachmentId)

        val attachments = listOf(firstAttachment, secondAttachment)

        val currentIdentifier = Fhir3AttachmentHelper.buildIdentifier(ADDITIONAL_ID, ASSIGNER)

        setFhir3Attachment(resource, attachments)
        setFhir3Id(resource, listOf(currentIdentifier))

        val wrappedAttachments = listOf(
                SdkAttachmentFactory.wrap(firstAttachment),
                SdkAttachmentFactory.wrap(secondAttachment)
        )

        val decryptedRecord = DecryptedRecord(
                null,
                resource,
                null,
                listOf(),
                null,
                null,
                null,
                attachmentKey,
                -1
        )

        every { recordService.decryptRecord<Fhir3Resource>(fetchedRecord, USER_ID) } returns decryptedRecord
        every {
            apiService.fetchRecord(ALIAS, USER_ID, RECORD_ID)
        } returns Single.just(fetchedRecord)

        every {
            recordService.decryptRecord<Fhir3Resource>(fetchedRecord, USER_ID)
        } returns decryptedRecord as DecryptedBaseRecord<Fhir3Resource>

        every {
            attachmentService.download(
                    listOf(
                            SdkAttachmentFactory.wrap(firstAttachment),
                            SdkAttachmentFactory.wrap(secondAttachment)
                    ),
                    attachmentKey,
                    USER_ID
            )
        } returns Single.just(wrappedAttachments)

        // When
        val test = recordService.downloadAttachments(
                RECORD_ID,
                attachmentIds,
                USER_ID,
                DownloadType.Full
        ).test().await()

        // Then
        val result = test
                .assertNoErrors()
                .assertComplete()
                .assertValue(listOf(wrappedAttachments[0].unwrap(), wrappedAttachments[1].unwrap()))
                .values()[0]

        assertEquals(
                actual = result[0].id,
                expected = firstAttachmentId
        )
        assertEquals(
                actual = result[1].id,
                expected = secondAttachmentId
        )
    }

    @Test
    @Ignore("Not implemented yet")
    fun `Given, downloadAttachment is called, with a Fhir4Resource, it downloads the attachment`() {
        Assume.assumeTrue(resourcePrefixes.contains("fhir4") || resourcePrefixes.contains("common"))

        // Given
        val resource = SdkFhirParser.toFhir4(
                resourceType,
                getFhireResource(determineFhir4Folder(resourcePrefixes), resourceName)
        )

        val fetchedRecord: EncryptedRecord = mockk()
        val attachmentKey: GCKey = mockk()

        val firstAttachmentId = "1"
        val secondAttachmentId = "2"

        val attachmentIds = listOf(firstAttachmentId, secondAttachmentId)

        val firstAttachment = buildFhir4Attachment(firstAttachmentId)
        val secondAttachment = buildFhir4Attachment(secondAttachmentId)

        val attachments = listOf(firstAttachment, secondAttachment)

        val currentIdentifier = Fhir4AttachmentHelper.buildIdentifier(ADDITIONAL_ID, ASSIGNER)

        setFhir4Attachment(resource, attachments)
        setFhir4Id(resource, listOf(currentIdentifier))

        val wrappedAttachments = listOf(
                SdkAttachmentFactory.wrap(firstAttachment),
                SdkAttachmentFactory.wrap(secondAttachment)
        )

        val decryptedRecord = DecryptedR4Record<Fhir4Resource>(
                null,
                resource,
                null,
                listOf(),
                null,
                null,
                null,
                attachmentKey,
                -1
        )

        every { recordService.decryptRecord<Fhir4Resource>(fetchedRecord, USER_ID) } returns decryptedRecord
        every {
            apiService.fetchRecord(ALIAS, USER_ID, RECORD_ID)
        } returns Single.just(fetchedRecord)

        every {
            recordService.decryptRecord<Fhir4Resource>(fetchedRecord, USER_ID)
        } returns decryptedRecord as DecryptedBaseRecord<Fhir4Resource>

        every {
            attachmentService.download(
                    listOf(
                            SdkAttachmentFactory.wrap(firstAttachment),
                            SdkAttachmentFactory.wrap(secondAttachment)
                    ),
                    attachmentKey,
                    USER_ID
            )
        } returns Single.just(wrappedAttachments)

        // When
        val test = recordService.downloadAttachments(
                RECORD_ID,
                attachmentIds,
                USER_ID,
                DownloadType.Full
        ).test().await()

        // Then
        val result = test
                .assertNoErrors()
                .assertComplete()
                .assertValue(listOf(wrappedAttachments[0].unwrap(), wrappedAttachments[1].unwrap()))
                .values()[0]

        assertEquals(
                actual = result[0].id,
                expected = firstAttachmentId
        )
        assertEquals(
                actual = result[1].id,
                expected = secondAttachmentId
        )
    }

    companion object {
        private val PDF = makePdf(DATA_SIZE_MAX_BYTES)
        private val PDF_OVERSIZED =  makePdf(DATA_SIZE_MAX_BYTES+1)

        private const val RECORD_ID = "recordId"
        private const val USER_ID = "userId"
        private const val PARTNER_ID = "partnerId"
        private const val ALIAS = "alias"
        private const val DATA = "data"
        private const val DATA_SIZE = 42
        private const val DATA_HASH = "dataHash"
        private const val ATTACHMENT_ID = "attachmentId"
        private const val THUMBNAIL_ID = "thumbnailId"
        private const val PREVIEW_ID = "previewId"
        private const val ASSIGNER = "assigner"
        private const val ADDITIONAL_ID = DOWNSCALED_ATTACHMENT_IDS_FMT +
                SPLIT_CHAR +
                ATTACHMENT_ID +
                SPLIT_CHAR +
                PREVIEW_ID +
                SPLIT_CHAR +
                THUMBNAIL_ID
        private val OBSOLETE_ID = ADDITIONAL_ID.replaceFirst(ATTACHMENT_ID, "obsoleteId")
        private const val OTHER_ID = "otherId"
        private const val VALUE_INDICATOR = "valueAttachment"
        private val VALUE_ID = DOWNSCALED_ATTACHMENT_IDS_FMT +
                SPLIT_CHAR +
                VALUE_INDICATOR +
                SPLIT_CHAR +
                PREVIEW_ID +
                SPLIT_CHAR +
                THUMBNAIL_ID

        private fun makePdf(size: Int): String {
            val pdf = arrayOfNulls<Byte>(size)
            System.arraycopy(
                MimeType.PDF.byteSignature()[0] as Any,
                0,
                pdf,
                0,
                MimeType.PDF.byteSignature()[0]?.size!!
            )

            return Base64.encodeToString(byteArrayOf(pdf))
        }

        private fun byteArrayOf(elements: Array<Byte?>): ByteArray {
            val array = ByteArray(elements.size)
            for (idx in elements.indices) {
                array[idx] = elements[idx] ?: 0
            }

            return array
        }

        private fun buildFhir3Attachment(id: String): Fhir3Attachment {
            val attachment = Fhir3Attachment()
            attachment.id = id
            attachment.data = DATA
            attachment.size = DATA_SIZE
            attachment.hash = DATA_HASH
            return attachment
        }

        private fun buildFhir4Attachment(id: String): Fhir4Attachment {
            val attachment = Fhir4Attachment()
            attachment.id = id
            attachment.data = DATA
            attachment.size = DATA_SIZE
            attachment.hash = DATA_HASH
            return attachment
        }

        @JvmStatic
        @Parameterized.Parameters(name = "applied on {1}")
        fun parameter() = listOf(
                arrayOf(
                        "Patient",
                        "patient",
                        listOf("common"),
                        "1"
                ),
                arrayOf(
                        "QuestionnaireResponse",
                        "questionnaireResponse",
                        listOf("common"),
                        "1"
                ),
                arrayOf(
                        "Medication",
                        "medication",
                        listOf("fhir3"),
                        "1"
                ),
                arrayOf(
                        "Observation",
                        "observation",
                        listOf("fhir3"),
                        "1"
                ),
                arrayOf(
                        "Observation",
                        "observation-with-component",
                        listOf("fhir3"),
                        "2"
                ),
                arrayOf(
                        "Questionnaire",
                        "questionnaire",
                        listOf("fhir3", "fhir4"),
                        "1"
                )
        )
    }
}
