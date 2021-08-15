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
import care.data4life.sdk.crypto.CryptoContract
import care.data4life.sdk.data.DataResource
import care.data4life.sdk.date.DateHelperContract
import care.data4life.sdk.fhir.Fhir3Resource
import care.data4life.sdk.fhir.Fhir4Resource
import care.data4life.sdk.fhir.FhirContract
import care.data4life.sdk.lang.CoreRuntimeException
import care.data4life.sdk.model.ModelContract
import care.data4life.sdk.network.NetworkingContract
import care.data4life.sdk.tag.Annotations
import care.data4life.sdk.tag.TaggingContract
import care.data4life.sdk.tag.Tags
import care.data4life.sdk.test.util.GenericTestDataProvider.ALIAS
import io.mockk.Runs
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import io.reactivex.Single
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RecordCryptoServiceTest {
    private lateinit var service: NetworkModelContract.CryptoService
    private var apiService: NetworkingContract.Service = mockk()
    private val cryptoService: CryptoContract.Service = mockk()
    private val taggingService: TaggingContract.Service = mockk()
    private val tagCryptoService: TaggingContract.CryptoService = mockk()
    private val resourceCryptoService: FhirContract.CryptoService = mockk()
    private val dateTimeFormatter: DateHelperContract.DateTimeFormatter = mockk()
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

    private fun runFromResourceFlow(
        resource: Any,
        annotations: Annotations,
        tags: Tags = mockk(),
        creationDate: String = "nevermore",
        dataKey: GCKey = mockk()
    ) {
        every { taggingService.appendDefaultTags(resource, null) } returns tags
        every { limitGuard.checkTagsAndAnnotationsLimits(tags, annotations) } just Runs
        every { dateTimeFormatter.now() } returns creationDate
        every { cryptoService.generateGCKey() } returns Single.just(dataKey)
    }

    private fun runFromResourceArbitraryDataFlow(
        resource: DataResource,
        annotations: Annotations,
        resourceValue: ByteArray = ByteArray(42),
        tags: Tags = mockk(),
        creationDate: String = "nevermore",
        dataKey: GCKey = mockk()
    ) {
        every { resource.value } returns resourceValue
        every { limitGuard.checkDataLimit(resourceValue) } just Runs
        runFromResourceFlow(resource, annotations, tags, creationDate, dataKey)
    }

    @Test
    fun `It fulfils EncryptionService`() {
        val service: Any = this.service

        assertTrue(service is NetworkModelContract.CryptoService)
    }

    @Test
    fun `Given, fromResource is called with a unknown resource, it fails`() {
        // Given
        val annotations: Annotations = mockk()
        val resource = "any"

        runFromResourceFlow(resource, annotations)

        // Then
        assertFailsWith<CoreRuntimeException.UnsupportedOperation> {
            // When
            service.fromResource(resource, annotations)
        }
    }

    // FHIR3
    @Test
    fun `Given, fromResource is called with a Fhir3 resource and Annotations, it returns a DecryptedFhir3Record`() {
        // Given
        val annotations: Annotations = mockk()
        val resource: Fhir3Resource = mockk()

        runFromResourceFlow(resource, annotations)

        // When
        val record = service.fromResource(resource, annotations)

        // Then
        assertTrue(record is NetworkModelInternalContract.DecryptedFhir3Record)
    }

    @Test
    fun `Given, fromResource is called with a Fhir3 resource and Annotations, it amends the given Resource and Annotations to the DecryptedFhir3Record`() {
        // Given
        val annotations: Annotations = mockk()
        val resource: Fhir3Resource = mockk()

        runFromResourceFlow(resource, annotations)

        // When
        val record = service.fromResource(resource, annotations)

        // Then
        assertEquals(
            actual = record.resource,
            expected = resource
        )

        assertEquals(
            actual = record.annotations,
            expected = annotations
        )
    }

    @Test
    fun `Given, fromResource is called with a Fhir3 resource and Annotations, it determines the tags for the given Resource and amends them`() {
        // Given
        val annotations: Annotations = mockk()
        val resource: Fhir3Resource = mockk()
        val tags: Tags = mockk()

        runFromResourceFlow(
            resource,
            annotations,
            tags = tags
        )

        // When
        val record = service.fromResource(resource, annotations)

        // Then
        assertEquals(
            actual = record.tags,
            expected = tags
        )

        verify(exactly = 1) { taggingService.appendDefaultTags(resource, null) }
    }

    @Test
    fun `Given, fromResource is called with a Fhir3 resource and Annotations, it calls the LimitGuard with the resolved Tags and Annotations`() {
        // Given
        val annotations: Annotations = mockk()
        val resource: Fhir3Resource = mockk()
        val tags: Tags = mockk()

        runFromResourceFlow(
            resource,
            annotations,
            tags = tags
        )

        // When
        service.fromResource(resource, annotations)

        // Then
        verify(exactly = 1) { limitGuard.checkTagsAndAnnotationsLimits(tags, annotations) }
    }

    @Test
    fun `Given, fromResource is called with a Fhir3 resource and Annotations, it fetches the current date and amends it as creationDate to the DecryptedFhir3Record`() {
        // Given
        val annotations: Annotations = mockk()
        val resource: Fhir3Resource = mockk()
        val creationDate = "There is no better time then now"

        runFromResourceFlow(
            resource,
            annotations,
            creationDate = creationDate
        )

        // When
        val record = service.fromResource(resource, annotations)

        // Then
        assertEquals(
            actual = record.customCreationDate,
            expected = creationDate
        )

        verify(exactly = 1) { dateTimeFormatter.now() }
    }

    @Test
    fun `Given, fromResource is called with a Fhir3 resource and Annotations, it generates a DataKey and amends it as dataKey to the DecryptedFhir3Record`() {
        // Given
        val annotations: Annotations = mockk()
        val resource: Fhir3Resource = mockk()
        val dataKey: GCKey = mockk()

        runFromResourceFlow(
            resource,
            annotations,
            dataKey = dataKey
        )

        // When
        val record = service.fromResource(resource, annotations)

        // Then
        assertEquals(
            actual = record.dataKey,
            expected = dataKey
        )

        verify(exactly = 1) { cryptoService.generateGCKey() }
    }

    @Test
    fun `Given, fromResource is called with a Fhir3 resource and Annotations, it sets the current ModelVersion to the DecryptedFhir3Record`() {
        // Given
        val annotations: Annotations = mockk()
        val resource: Fhir3Resource = mockk()

        runFromResourceFlow(
            resource,
            annotations
        )

        // When
        val record = service.fromResource(resource, annotations)

        // Then
        assertEquals(
            actual = record.modelVersion,
            expected = ModelContract.ModelVersion.CURRENT
        )
    }

    @Test
    fun `Given, fromResource is called with a Fhir3 resource and Annotations, it sets the Identifier, AttachmentKey and UpdateDate null at the DecryptedFhir3Record`() {
        // Given
        val annotations: Annotations = mockk()
        val resource: Fhir3Resource = mockk()

        runFromResourceFlow(
            resource,
            annotations
        )

        // When
        val record = service.fromResource(resource, annotations)

        // Then
        assertNull(record.identifier)
        assertNull(record.attachmentsKey)
        assertNull(record.updatedDate)
    }

    // FHIR4
    @Test
    fun `Given, fromResource is called with a Fhir4 resource and Annotations, it returns a DecryptedFhir4Record`() {
        // Given
        val annotations: Annotations = mockk()
        val resource: Fhir4Resource = mockk()

        runFromResourceFlow(resource, annotations)

        // When
        val record = service.fromResource(resource, annotations)

        // Then
        assertTrue(record is NetworkModelInternalContract.DecryptedFhir4Record)
    }

    @Test
    fun `Given, fromResource is called with a Fhir4 resource and Annotations, it amends the given Resource and Annotations to the DecryptedFhir4Record`() {
        // Given
        val annotations: Annotations = mockk()
        val resource: Fhir4Resource = mockk()

        runFromResourceFlow(resource, annotations)

        // When
        val record = service.fromResource(resource, annotations)

        // Then
        assertEquals(
            actual = record.resource,
            expected = resource
        )

        assertEquals(
            actual = record.annotations,
            expected = annotations
        )
    }

    @Test
    fun `Given, fromResource is called with a Fhir4 resource and Annotations, it determines the tags for the given Resource and amends them`() {
        // Given
        val annotations: Annotations = mockk()
        val resource: Fhir4Resource = mockk()
        val tags: Tags = mockk()

        runFromResourceFlow(
            resource,
            annotations,
            tags = tags
        )

        // When
        val record = service.fromResource(resource, annotations)

        // Then
        assertEquals(
            actual = record.tags,
            expected = tags
        )

        verify(exactly = 1) { taggingService.appendDefaultTags(resource, null) }
    }

    @Test
    fun `Given, fromResource is called with a Fhir4 resource and Annotations, it calls the LimitGuard with the resolved Tags and Annotations`() {
        // Given
        val annotations: Annotations = mockk()
        val resource: Fhir4Resource = mockk()
        val tags: Tags = mockk()

        runFromResourceFlow(
            resource,
            annotations,
            tags = tags
        )

        // When
        service.fromResource(resource, annotations)

        // Then
        verify(exactly = 1) { limitGuard.checkTagsAndAnnotationsLimits(tags, annotations) }
    }

    @Test
    fun `Given, fromResource is called with a Fhir4 resource and Annotations, it fetches the current date and amends it as creationDate to the DecryptedFhir4Record`() {
        // Given
        val annotations: Annotations = mockk()
        val resource: Fhir4Resource = mockk()
        val creationDate = "There is no better time then now"

        runFromResourceFlow(
            resource,
            annotations,
            creationDate = creationDate
        )

        // When
        val record = service.fromResource(resource, annotations)

        // Then
        assertEquals(
            actual = record.customCreationDate,
            expected = creationDate
        )

        verify(exactly = 1) { dateTimeFormatter.now() }
    }

    @Test
    fun `Given, fromResource is called with a Fhir4 resource and Annotations, it generates a DataKey and amends it as dataKey to the DecryptedFhir4Record`() {
        // Given
        val annotations: Annotations = mockk()
        val resource: Fhir4Resource = mockk()
        val dataKey: GCKey = mockk()

        runFromResourceFlow(
            resource,
            annotations,
            dataKey = dataKey
        )

        // When
        val record = service.fromResource(resource, annotations)

        // Then
        assertEquals(
            actual = record.dataKey,
            expected = dataKey
        )

        verify(exactly = 1) { cryptoService.generateGCKey() }
    }

    @Test
    fun `Given, fromResource is called with a Fhir4 resource and Annotations, it sets the current ModelVersion to the DecryptedFhir4Record`() {
        // Given
        val annotations: Annotations = mockk()
        val resource: Fhir4Resource = mockk()

        runFromResourceFlow(
            resource,
            annotations
        )

        // When
        val record = service.fromResource(resource, annotations)

        // Then
        assertEquals(
            actual = record.modelVersion,
            expected = ModelContract.ModelVersion.CURRENT
        )
    }

    @Test
    fun `Given, fromResource is called with a Fhir4 resource and Annotations, it sets the Identifier, AttachmentKey and UpdateDate null at the DecryptedFhir4Record`() {
        // Given
        val annotations: Annotations = mockk()
        val resource: Fhir4Resource = mockk()

        runFromResourceFlow(
            resource,
            annotations
        )

        // When
        val record = service.fromResource(resource, annotations)

        // Then
        assertNull(record.identifier)
        assertNull(record.attachmentsKey)
        assertNull(record.updatedDate)
    }

    // Arbitrary Data
    @Test
    fun `Given, fromResource is called with a DataResource and Annotations, it returns a DecryptedCustomDataRecord`() {
        // Given
        val annotations: Annotations = mockk()
        val resource: DataResource = mockk()

        runFromResourceArbitraryDataFlow(resource, annotations)

        // When
        val record = service.fromResource(resource, annotations)

        // Then
        assertTrue(record is NetworkModelInternalContract.DecryptedCustomDataRecord)
    }

    @Test
    fun `Given, fromResource is called with a DataResource and Annotations, it delegates the Resource to the LimitGuard`() {
        // Given
        val annotations: Annotations = mockk()
        val resource: DataResource = mockk()
        val resourceValue = ByteArray(23)

        runFromResourceArbitraryDataFlow(
            resource,
            annotations,
            resourceValue = resourceValue
        )

        // When
        service.fromResource(resource, annotations)

        // Then
        verify(exactly = 1) { limitGuard.checkDataLimit(resourceValue) }
    }

    @Test
    fun `Given, fromResource is called with a DataResource and Annotations, it amends the given Resource and Annotations to the DecryptedCustomDataRecord`() {
        // Given
        val annotations: Annotations = mockk()
        val resource: DataResource = mockk()

        runFromResourceArbitraryDataFlow(resource, annotations)

        // When
        val record = service.fromResource(resource, annotations)

        // Then
        assertEquals(
            actual = record.resource,
            expected = resource
        )

        assertEquals(
            actual = record.annotations,
            expected = annotations
        )
    }

    @Test
    fun `Given, fromResource is called with a DataResource and Annotations, it determines the tags for the given Resource and amends them`() {
        // Given
        val annotations: Annotations = mockk()
        val resource: DataResource = mockk()
        val tags: Tags = mockk()

        runFromResourceArbitraryDataFlow(
            resource,
            annotations,
            tags = tags
        )

        // When
        val record = service.fromResource(resource, annotations)

        // Then
        assertEquals(
            actual = record.tags,
            expected = tags
        )

        verify(exactly = 1) { taggingService.appendDefaultTags(resource, null) }
    }

    @Test
    fun `Given, fromResource is called with a DataResource and Annotations, it calls the LimitGuard with the resolved Tags and Annotations`() {
        // Given
        val annotations: Annotations = mockk()
        val resource: DataResource = mockk()
        val tags: Tags = mockk()

        runFromResourceArbitraryDataFlow(
            resource,
            annotations,
            tags = tags
        )

        // When
        service.fromResource(resource, annotations)

        // Then
        verify(exactly = 1) { limitGuard.checkTagsAndAnnotationsLimits(tags, annotations) }
    }

    @Test
    fun `Given, fromResource is called with a DataResource and Annotations, it fetches the current date and amends it as creationDate to the DecryptedCustomDataRecord`() {
        // Given
        val annotations: Annotations = mockk()
        val resource: DataResource = mockk()
        val creationDate = "There is no better time then now"

        runFromResourceArbitraryDataFlow(
            resource,
            annotations,
            creationDate = creationDate
        )

        // When
        val record = service.fromResource(resource, annotations)

        // Then
        assertEquals(
            actual = record.customCreationDate,
            expected = creationDate
        )

        verify(exactly = 1) { dateTimeFormatter.now() }
    }

    @Test
    fun `Given, fromResource is called with a DataResource and Annotations, it generates a DataKey and amends it as dataKey to the DecryptedCustomDataRecord`() {
        // Given
        val annotations: Annotations = mockk()
        val resource: DataResource = mockk()
        val dataKey: GCKey = mockk()

        runFromResourceArbitraryDataFlow(
            resource,
            annotations,
            dataKey = dataKey
        )

        // When
        val record = service.fromResource(resource, annotations)

        // Then
        assertEquals(
            actual = record.dataKey,
            expected = dataKey
        )

        verify(exactly = 1) { cryptoService.generateGCKey() }
    }

    @Test
    fun `Given, fromResource is called with a DataResource and Annotations, it sets the current ModelVersion to the DecryptedCustomDataRecord`() {
        // Given
        val annotations: Annotations = mockk()
        val resource: DataResource = mockk()

        runFromResourceArbitraryDataFlow(
            resource,
            annotations
        )

        // When
        val record = service.fromResource(resource, annotations)

        // Then
        assertEquals(
            actual = record.modelVersion,
            expected = ModelContract.ModelVersion.CURRENT
        )
    }

    @Test
    fun `Given, fromResource is called with a DataResource and Annotations, it sets the Identifier, AttachmentKey and UpdateDate null at the DecryptedCustomDataRecord`() {
        // Given
        val annotations: Annotations = mockk()
        val resource: DataResource = mockk()

        runFromResourceArbitraryDataFlow(
            resource,
            annotations
        )

        // When
        val record = service.fromResource(resource, annotations)

        // Then
        assertNull(record.identifier)
        assertNull(record.updatedDate)
    }
}
