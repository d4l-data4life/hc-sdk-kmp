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
import care.data4life.sdk.lang.CoreRuntimeException
import care.data4life.sdk.network.model.DecryptedRecord
import care.data4life.sdk.network.model.DecryptedRecordGuard
import care.data4life.sdk.network.model.NetworkRecordContract
import io.mockk.every
import io.mockk.verify
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
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

        assertTrue(builder is NetworkRecordContract.Builder)
    }

    @Test
    fun `Given tags are accessed, it returns null by default`() {
        // Given
        val builder = DecryptedRecordBuilder()

        // Then
        assertNull(builder.tags)
    }

    @Test
    fun `Given, setTags is called, with its proper payload and tags are accessed, it returns the payload of setTags`() {
        // Given
        val builder = DecryptedRecordBuilder().setTags(tags)

        // Then
        assertEquals(
                tags,
                builder.tags
        )
    }

    @Test
    fun `Given dataKey is accessed, it returns null by default`() {
        // Given
        val builder = DecryptedRecordBuilder()

        // Then
        assertNull(builder.dataKey)
    }

    @Test
    fun `Given, setDataKey is called, with its proper payload and dataKey is accessed, it returns the payload of setTags`() {
        // Given
        val builder = DecryptedRecordBuilder().setDataKey(dataKey)

        // Then
        assertEquals(
                dataKey,
                builder.dataKey
        )
    }

    @Test
    fun `Given, build is called with a DomainResource, Tags, CreationDate, DataKey and ModelVersion, it returns a DecryptedRecord`() {
        // When
        val record: Any = DecryptedRecordBuilder().build(
                resource,
                tags,
                creationDate,
                dataKey,
                modelVersion
        )

        // Then
        assertTrue(record is NetworkRecordContract.DecryptedRecord)
        assertEquals(
                record,
                DecryptedRecord(
                        null,
                        resource,
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
    fun `Given, build is called with a valid Resource, CreationDate, DataKey and ModelVersion, but without using a setter or delegating Tags, it fails with a InternalFailure`() {
        // When
        try {
            DecryptedRecordBuilder()
                    .build(
                            resource,
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
                            resource,
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
                            resource,
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
                            resource,
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
                            resource,
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
                            resource,
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
                            resource,
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
                            resource,
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
                        resource,
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
                        resource,
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
                        resource,
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
                        resource,
                        modelVersion = delegatedModelVersion
                )

        // Then
        assertEquals(
                record.modelVersion,
                delegatedModelVersion
        )
    }

    @Test
    fun `Given, mandatory and optional setters are called with their appropriate payload, it returns a DecryptedRecord`() {
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
                .build(resource)

        // Then
        assertEquals(
                record,
                DecryptedRecord(
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
    fun `Given, build is called with a valid Resource, Tags, CreationDate, DataKey and ModelVersion, it calls the Limit Guard, with the given Tags and empty Annotations`() {
        // Given
        every { DecryptedRecordGuard.checkTagsAndAnnotationsLimits(tags, listOf()) } returns Unit

        // When
        DecryptedRecordBuilder().build(
                resource,
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
                    resource,
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
                    resource,
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
        every { DecryptedRecordGuard.checkDataLimit(resource) } returns Unit

        DecryptedRecordBuilder()
                .setAnnotations(annotations)
                .build(
                        resource,
                        tags,
                        creationDate,
                        dataKey,
                        modelVersion
                )

        // Then
        verify { DecryptedRecordGuard.checkDataLimit(resource) }
    }
}
