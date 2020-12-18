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

package care.data4life.sdk.network

import care.data4life.crypto.GCKey
import care.data4life.sdk.data.DataResource
import care.data4life.sdk.fhir.Fhir4Resource
import care.data4life.sdk.lang.CoreRuntimeException
import care.data4life.sdk.network.model.DecryptedR4Record
import care.data4life.sdk.network.model.DecryptedRecord
import care.data4life.sdk.network.model.DecryptedRecordGuard
import care.data4life.sdk.network.model.definitions.DecryptedFhir3Record
import care.data4life.sdk.network.model.NetworkModelContract
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito

class DecryptedRecordBuilderTest : DecryptedRecordBuilderTestBase() {
    @Before
    fun setUp() {
        init()
    }

    @After
    fun tearDown() {
        stop()
    }

    @Test
    fun `it is a DecryptedRecordBuilder`() {
        val builder: Any = DecryptedRecordBuilder()

        assertTrue(builder is NetworkModelContract.DecryptedRecordBuilder)
    }

    @Test
    fun `Given, build is called with a unknown Resource, Tags, CreationDate, DataKey and ModelVersion, it fails with a InternalFailure`() {
        try {
            DecryptedRecordBuilder()
                    .build(
                            "something",
                            tags,
                            creationDate,
                            dataKey,
                            modelVersion
                    )
            assertTrue(false)// FIXME: This is stupid
        } catch (e: Exception) {
            // Then
            assertTrue(e is CoreRuntimeException.InternalFailure)
        }
    }

    @Test
    fun `Given, build is called with a Fhir4Resource, Tags, CreationDate, DataKey and ModelVersion, it returns a DecryptedFhir4Record`() {
        // When
        val fhir4Resource = mockk<Fhir4Resource>()
        val record = DecryptedRecordBuilder().build(
                fhir4Resource,
                tags,
                creationDate,
                dataKey,
                modelVersion
        )

        // Then
        assertTrue(record is DecryptedR4Record<*>)
        assertEquals(
                record,
                DecryptedR4Record(
                        null,
                        fhir4Resource,
                        tags,
                        listOf(),
                        creationDate,
                        null,
                        dataKey,
                        null,
                        modelVersion
                )
        )
    }

    @Test
    fun `Given, build is called with null, Tags, CreationDate, DataKey and ModelVersion, it returns a DecryptedFhir3Record`() {
        // When
        val record = DecryptedRecordBuilder().build(
                null,
                tags,
                creationDate,
                dataKey,
                modelVersion
        )

        // Then
        assertTrue(record is DecryptedFhir3Record<*>)
        assertEquals(
                record,
                DecryptedRecord(
                        null,
                        null,
                        tags,
                        listOf(),
                        creationDate,
                        null,
                        dataKey,
                        null,
                        modelVersion
                )
        )
    }

    @Test
    fun `Given, build is called with a Fhir3Resource, Tags, CreationDate, DataKey and ModelVersion, it returns a DecryptedFhir3Record`() {
        // When
        val record = DecryptedRecordBuilder().build(
                fhirResource,
                tags,
                creationDate,
                dataKey,
                modelVersion
        )

        // Then
        assertTrue(record is DecryptedFhir3Record<*>)
        assertEquals(
                record,
                DecryptedRecord(
                        null,
                        fhirResource,
                        tags,
                        listOf(),
                        creationDate,
                        null,
                        dataKey,
                        null,
                        modelVersion
                )
        )
    }

    @Test
    fun `Given, build is called with ByteArray, Tags, CreationDate, DataKey and ModelVersion, it returns a DecryptedDataRecord`() {
        // When
        val record = DecryptedRecordBuilder().build(
                DataResource(customResource),
                tags,
                creationDate,
                dataKey,
                modelVersion
        )

        // Then
        assertTrue(record is care.data4life.sdk.network.model.definitions.DecryptedCustomDataRecord)
        assertEquals(
                record,
                care.data4life.sdk.network.model.DecryptedDataRecord(
                        null,
                        DataResource(customResource),
                        tags,
                        listOf(),
                        creationDate,
                        null,
                        dataKey,
                        modelVersion
                )
        )
    }

    @Test
    fun `Given, build is called with a valid Resource, CreationDate, DataKey and ModelVersion, but without using a setter or delegating Tags, it fails with a InternalFailure`() {
        // When
        try {
            DecryptedRecordBuilder()
                    .build(
                            DataResource(customResource),
                            creationDate = creationDate,
                            dataKey = dataKey,
                            modelVersion = modelVersion
                    )
            assertTrue(false)// FIXME: This is stupid
        } catch (e: Exception) {
            // Then
            assertTrue(e is CoreRuntimeException.InternalFailure)
        }
    }

    @Test
    fun `Given, build is called with a valid Resource, Tags, DataKey and ModelVersion, but without using a setter or delegating a CreationDate, it fails with a InternalFailure`() {
        // When
        try {
            DecryptedRecordBuilder()
                    .build(
                            DataResource(customResource),
                            tags = tags,
                            dataKey = dataKey,
                            modelVersion = modelVersion
                    )
            assertTrue(false)// FIXME: This is stupid
        } catch (e: Exception) {
            // Then
            assertTrue(e is CoreRuntimeException.InternalFailure)
        }
    }

    @Test
    fun `Given, build is called with a valid Resource, Tags, CreationDate and ModelVersion, but without using a setter or delegating a DataKey, it fails with a InternalFailure`() {
        // When
        try {
            DecryptedRecordBuilder()
                    .build(
                            DataResource(customResource),
                            tags = tags,
                            creationDate = creationDate,
                            modelVersion = modelVersion
                    )
            assertTrue(false)// FIXME: This is stupid
        } catch (e: Exception) {
            // Then
            assertTrue(e is CoreRuntimeException.InternalFailure)
        }
    }

    @Test
    fun `Given, build is called with a valid Resource, Tags, and CreationDate, but without using a setter or delegating a ModelVersion, it fails with a InternalFailure`() {
        // When
        try {
            DecryptedRecordBuilder()
                    .build(
                            DataResource(customResource),
                            tags = tags,
                            creationDate = creationDate,
                            dataKey = dataKey
                    )
            assertTrue(false)// FIXME: This is stupid
        } catch (e: Exception) {
            // Then
            assertTrue(e is CoreRuntimeException.InternalFailure)
        }
    }

    @Test
    fun `Given, build is called with a valid Resource, null for Tags, CreationDate, DataKey and ModelVersion, but without using a setter for Tags, it fails with a InternalFailure`() {
        // When
        try {
            DecryptedRecordBuilder()
                    .build(
                            DataResource(customResource),
                            null,
                            creationDate,
                            dataKey,
                            modelVersion
                    )
            assertTrue(false)// FIXME: This is stupid
        } catch (e: Exception) {
            // Then
            assertTrue(e is CoreRuntimeException.InternalFailure)
        }
    }

    @Test
    fun `Given, build is called with a valid Resource, Tags, null for a CreationDate, DataKey and ModelVersion, but without using a setter for a CreationDate, it fails with a InternalFailure`() {
        // When
        try {
            DecryptedRecordBuilder()
                    .build(
                            DataResource(customResource),
                            tags = tags,
                            creationDate = null,
                            dataKey = dataKey,
                            modelVersion = modelVersion
                    )
            assertTrue(false)// FIXME: This is stupid
        } catch (e: Exception) {
            // Then
            assertTrue(e is CoreRuntimeException.InternalFailure)
        }
    }

    @Test
    fun `Given, build is called with a valid Resource, Tags, CreationDate, null for a DataKey and ModelVersion, but without using a setter for a DataKey, it fails with a InternalFailure`() {
        // When
        try {
            DecryptedRecordBuilder()
                    .build(
                            DataResource(customResource),
                            tags = tags,
                            creationDate = creationDate,
                            dataKey = null,
                            modelVersion = modelVersion
                    )
            assertTrue(false)// FIXME: This is stupid
        } catch (e: Exception) {
            // Then
            assertTrue(e is CoreRuntimeException.InternalFailure)
        }
    }

    @Test
    fun `Given, build is called with a valid Resource, Tags, CreationDate, DataKey and null for a ModelVersion, but without using a setter for a ModelVersion, it fails with a InternalFailure`() {
        try {
            DecryptedRecordBuilder()
                    .build(
                            DataResource(customResource),
                            tags = tags,
                            creationDate = creationDate,
                            dataKey = dataKey,
                            modelVersion = null
                    )
            assertTrue(false)// FIXME: This is stupid
        } catch (e: Exception) {
            // Then
            assertTrue(e is CoreRuntimeException.InternalFailure)
        }
    }

    @Test
    fun `Given, build is called with a valid Resource and Tags, while all mandatory values are already given by setter, it uses the delegated Tags over the setted Tags`() {
        // Given
        @Suppress("UNCHECKED_CAST")
        val delegatedTags = Mockito.mock(HashMap::class.java) as HashMap<String, String>
        // When
        val record = DecryptedRecordBuilder()
                .setTags(tags)
                .setCreationDate(creationDate)
                .setDataKey(dataKey)
                .setModelVersion(modelVersion)
                .build(
                        DataResource(customResource),
                        tags = delegatedTags
                )

        // Then
        assertEquals(
                record.tags,
                delegatedTags
        )
    }

    @Test
    fun `Given, build is called with a valid Resource and a CreationDate, while all mandatory values are already given by setter, it uses the delegated CreationDate over the setted CreationDate`() {
        // Given
        val delegatedDate = "2011-10-12"
        // When
        val record = DecryptedRecordBuilder()
                .setTags(tags)
                .setCreationDate(creationDate)
                .setDataKey(dataKey)
                .setModelVersion(modelVersion)
                .build(
                        DataResource(customResource),
                        creationDate = delegatedDate
                )

        // Then
        assertEquals(
                record.customCreationDate,
                delegatedDate
        )
    }

    @Test
    fun `Given, build is called with a valid Resource and a DataKey, while all mandatory values are already given by setter, it uses the delegated DataKey over the setted DataKey`() {
        // Given
        val delegatedDataKey = Mockito.mock(GCKey::class.java)
        // When
        val record = DecryptedRecordBuilder()
                .setTags(tags)
                .setCreationDate(creationDate)
                .setDataKey(dataKey)
                .setModelVersion(modelVersion)
                .build(
                        DataResource(customResource),
                        dataKey = delegatedDataKey
                )

        // Then
        assertEquals(
                record.dataKey,
                delegatedDataKey
        )
    }

    @Test
    fun `Given, build is called with a valid Resource and a ModelVersion, while all mandatory values are already given by setter, it uses the delegated ModelVersion over the setted ModelVersion`() {
        // Given
        val delegatedModelVersion = 23
        // When
        val record = DecryptedRecordBuilder()
                .setTags(tags)
                .setCreationDate(creationDate)
                .setDataKey(dataKey)
                .setModelVersion(modelVersion)
                .build(
                        DataResource(customResource),
                        modelVersion = delegatedModelVersion
                )

        // Then
        assertEquals(
                record.modelVersion,
                delegatedModelVersion
        )
    }

    @Test
    fun `Given, mandatory and optional setters are called with their appropriate payload, it returns a DecryptedFhir3Record`() {
        // Given
        val builder = DecryptedRecordBuilder()

        // When
        val record = builder
                .setIdentifier(identifier)
                .setTags(tags)
                .setAnnotations(annotations)
                .setCreationDate(creationDate)
                .setUpdateDate(updateDate)
                .setDataKey(dataKey)
                .setAttachmentKey(attachmentKey)
                .setModelVersion(modelVersion)
                .build(fhirResource)

        // Then
        assertEquals(
                record,
                DecryptedRecord(
                        identifier,
                        fhirResource,
                        tags,
                        annotations,
                        creationDate,
                        updateDate,
                        dataKey,
                        attachmentKey,
                        modelVersion
                )
        )
    }

    @Test
    fun `Given, mandatory and optional setters are called with their appropriate payload, it returns a DecryptedFhir4Record`() {
        // Given
        val builder = DecryptedRecordBuilder()
        val resource = mockk<Fhir4Resource>()

        // When
        val record = builder
                .setIdentifier(identifier)
                .setTags(tags)
                .setAnnotations(annotations)
                .setCreationDate(creationDate)
                .setUpdateDate(updateDate)
                .setDataKey(dataKey)
                .setAttachmentKey(attachmentKey)
                .setModelVersion(modelVersion)
                .build(resource)

        // Then
        assertEquals(
                record,
                DecryptedR4Record(
                        identifier,
                        resource,
                        tags,
                        annotations,
                        creationDate,
                        updateDate,
                        dataKey,
                        attachmentKey,
                        modelVersion
                )
        )
    }

    @Test
    fun `Given, mandatory and optional setters are called with their appropriate payload, it returns a DecryptedDataRecord`() {
        // Given
        val builder = DecryptedRecordBuilder()

        // When
        val record = builder
                .setIdentifier(identifier)
                .setTags(tags)
                .setAnnotations(annotations)
                .setCreationDate(creationDate)
                .setUpdateDate(updateDate)
                .setDataKey(dataKey)
                .setAttachmentKey(attachmentKey)
                .setModelVersion(modelVersion)
                .build(DataResource(customResource))

        assertEquals(
                record,
                care.data4life.sdk.network.model.DecryptedDataRecord(
                        identifier,
                        DataResource(customResource),
                        tags,
                        annotations,
                        creationDate,
                        updateDate,
                        dataKey,
                        modelVersion
                )
        )
    }

    @Test
    fun `Given, build is called with a valid Resource, Tags, CreationDate, DataKey and ModelVersion, it calls the Limit Guard, with the given Tags and empty Annotations`() {
        // Given
        every { DecryptedRecordGuard.checkTagsAndAnnotationsLimits(tags, listOf()) } returns Unit

        // When
        DecryptedRecordBuilder().build(
                DataResource(customResource),
                tags,
                creationDate,
                dataKey,
                modelVersion
        )

        // Then
        verify { DecryptedRecordGuard.checkTagsAndAnnotationsLimits(tags, listOf()) }
    }

    @Test
    fun `Given, build is called with a valid Resource, Tags, CreationDate, DataKey and ModelVersion, while Tags had been already set, it calls the Limit Guard, with the delegated Tags and empty Annotations`() {
        // Given
        @Suppress("UNCHECKED_CAST")
        val delegatedTags = Mockito.mock(HashMap::class.java) as HashMap<String, String>

        every { DecryptedRecordGuard.checkTagsAndAnnotationsLimits(delegatedTags, listOf()) } returns Unit

        // When
        DecryptedRecordBuilder()
                .setTags(tags)
                .build(
                    DataResource(customResource),
                    delegatedTags,
                    creationDate,
                    dataKey,
                    modelVersion
                )

        // Then
        verify { DecryptedRecordGuard.checkTagsAndAnnotationsLimits(delegatedTags, listOf()) }
    }

    @Test
    fun `Given, build is called with a valid Resource, Tags, CreationDate, DataKey and ModelVersion, while Annotations had been set, it calls the Limit Guard, with the given Tags and Annotations`() {
        // When
        every { DecryptedRecordGuard.checkTagsAndAnnotationsLimits(tags, annotations) } returns Unit

        DecryptedRecordBuilder()
                .setAnnotations(annotations)
                .build(
                    DataResource(customResource),
                    tags,
                    creationDate,
                    dataKey,
                    modelVersion
                )

        // Then
        verify { DecryptedRecordGuard.checkTagsAndAnnotationsLimits(tags, annotations) }
    }

    @Test
    fun `Given, build is called with a ByteArray, Tags, CreationDate, DataKey and ModelVersion, it calls the Limit Guard, with the given Resource`() {
        // When
        every { DecryptedRecordGuard.checkDataLimit(customResource) } returns Unit

        DecryptedRecordBuilder()
                .setAnnotations(annotations)
                .build(
                        DataResource(customResource),
                        tags,
                        creationDate,
                        dataKey,
                        modelVersion
                )

        // Then
        verify { DecryptedRecordGuard.checkDataLimit(customResource) }
    }

    @Test
    fun `Given, build is called with null as a Resource, Tags, CreationDate, DataKey and ModelVersion, it does not calls the Limit Guard, with the given Resource`() {
        // When
        every { DecryptedRecordGuard.checkDataLimit(any()) } returns Unit

        DecryptedRecordBuilder()
                .setAnnotations(annotations)
                .build(
                        null,
                        tags,
                        creationDate,
                        dataKey,
                        modelVersion
                )

        // Then
        verify(exactly = 0) { DecryptedRecordGuard.checkDataLimit(any()) }
    }

    @Test
    fun `Given, build is called with a FhirResource, Tags, CreationDate, DataKey and ModelVersion, it does not calls the Limit Guard, with the given Resource`() {
        // When
        every { DecryptedRecordGuard.checkDataLimit(any()) } returns Unit

        DecryptedRecordBuilder()
                .setAnnotations(annotations)
                .build(
                        null,
                        tags,
                        creationDate,
                        dataKey,
                        modelVersion
                )

        // Then
        verify(exactly = 0) { DecryptedRecordGuard.checkDataLimit(any()) }
    }
}
