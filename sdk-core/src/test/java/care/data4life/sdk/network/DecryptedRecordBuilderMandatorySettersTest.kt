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

import care.data4life.sdk.lang.CoreRuntimeException
import care.data4life.sdk.network.model.DecryptedRecord
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class DecryptedRecordBuilderMandatorySettersTest: DecryptedRecordBuilderTestBase() {

    @Before
    fun setUp() {
        init()
    }

    @After
    fun tearDown() {
        stop()
    }

    @Test
    fun `Given, no mandatory setters are called, it fails if build is called with a DecryptedRecord`() {
        // Given
        val builder = DecryptedRecordBuilder()

        // When
        try {
            builder.build(resource)
        } catch (e: Exception) {
            // Then
            assertTrue(e is CoreRuntimeException.InternalFailure)
        }
    }

    @Test
    fun `Given, setTags is called with a HashMap String to String, but the other mandatory setters are not called, it fails if build is called with a DecryptedRecord`() {
        // Given
        val builder = DecryptedRecordBuilder()

        // When
        try {
            builder.setTags(tags).build(resource)
        } catch (e: Exception) {
            // Then
            assertTrue(e is CoreRuntimeException.InternalFailure)
        }
    }

    @Test
    fun `Given, setCreationDate is called with a String, but the other mandatory setters are not called, it fails if build is called with a DecryptedRecord`() {
        // Given
        val builder = DecryptedRecordBuilder()

        // When
        try {
            builder.setCreationDate(creationDate).build(resource)
        } catch (e: Exception) {
            // Then
            assertTrue(e is CoreRuntimeException.InternalFailure)
        }
    }

    @Test
    fun `Given, setDataKey is called with a GCKey, but the other mandatory setters are not called, it fails if build is called with a DecryptedRecord`() {
        // Given
        val builder = DecryptedRecordBuilder()

        // When
        try {
            builder.setDataKey(dataKey).build(resource)
        } catch (e: Exception) {
            // Then
            assertTrue(e is CoreRuntimeException.InternalFailure)
        }
    }

    @Test
    fun `Given, setModelVersion is called with a Int, but the other mandatory setters are not called, it fails if build is called with a DecryptedRecord`() {
        // Given
        val builder = DecryptedRecordBuilder()

        // When
        try {
            builder.setModelVersion(modelVersion).build(resource)
        } catch (e: Exception) {
            // Then
            assertTrue(e is CoreRuntimeException.InternalFailure)
        }
    }

    @Test
    fun `Given, setTags and setCreationDate are called with their appropriate payload, but the other mandatory setters are not called, it fails if build is called with a DecryptedRecord`() {
        // Given
        val builder = DecryptedRecordBuilder()

        // When
        try {
            builder
                    .setTags(tags)
                    .setCreationDate(creationDate)
                    .build(resource)
        } catch (e: Exception) {
            // Then
            assertTrue(e is CoreRuntimeException.InternalFailure)
        }
    }

    @Test
    fun `Given, setTags and setDataKey are called with their appropriate payload, but the other mandatory setters are not called, it fails if build is called with a DecryptedRecord`() {
        // Given
        val builder = DecryptedRecordBuilder()

        // When
        try {
            builder
                    .setTags(tags)
                    .setDataKey(dataKey)
                    .build(resource)
        } catch (e: Exception) {
            // Then
            assertTrue(e is CoreRuntimeException.InternalFailure)
        }
    }

    @Test
    fun `Given, setTags and setModelVersion are called with their appropriate payload, but the other mandatory setters are not called, it fails if build is called with a DecryptedRecord`() {
        // Given
        val builder = DecryptedRecordBuilder()

        // When
        try {
            builder
                    .setTags(tags)
                    .setModelVersion(modelVersion)
                    .build(resource)
        } catch (e: Exception) {
            // Then
            assertTrue(e is CoreRuntimeException.InternalFailure)
        }
    }

    @Test
    fun `Given, setCreationDate and setDataKey are called with their appropriate payload, but the other mandatory setters are not called, it fails if build is called with a DecryptedRecord`() {
        // Given
        val builder = DecryptedRecordBuilder()

        // When
        try {
            builder
                    .setCreationDate(creationDate)
                    .setDataKey(dataKey)
                    .build(resource)
        } catch (e: Exception) {
            // Then
            assertTrue(e is CoreRuntimeException.InternalFailure)
        }
    }

    @Test
    fun `Given, setCreationDate and setModelVersion are called with their appropriate payload, but the other mandatory setters are not called, it fails if build is called with a DecryptedRecord`() {
        // Given
        val builder = DecryptedRecordBuilder()

        // When
        try {
            builder
                    .setCreationDate(creationDate)
                    .setModelVersion(modelVersion)
                    .build(resource)
        } catch (e: Exception) {
            // Then
            assertTrue(e is CoreRuntimeException.InternalFailure)
        }
    }

    @Test
    fun `Given, setDataKey and setModelVersion are called with their appropriate payload, but the other mandatory setters are not called, it fails if build is called with a DecryptedRecord`() {
        // Given
        val builder = DecryptedRecordBuilder()

        // When
        try {
            builder
                    .setDataKey(dataKey)
                    .setModelVersion(modelVersion)
                    .build(resource)
        } catch (e: Exception) {
            // Then
            assertTrue(e is CoreRuntimeException.InternalFailure)
        }
    }

    @Test
    fun `Given, setTags, setCreationDate and setDataKey are called with their appropriate payload, but the other mandatory setters are not called, it fails if build is called with a DecryptedRecord`() {
        // Given
        val builder = DecryptedRecordBuilder()

        // When
        try {
            builder
                    .setTags(tags)
                    .setCreationDate(creationDate)
                    .setDataKey(dataKey)
                    .build(resource)
        } catch (e: Exception) {
            // Then
            assertTrue(e is CoreRuntimeException.InternalFailure)
        }
    }

    @Test
    fun `Given, setTags, setCreationDate and setModelVersion are called with their appropriate payload, but the other mandatory setters are not called, it fails if build is called with a DecryptedRecord`() {
        // Given
        val builder = DecryptedRecordBuilder()

        // When
        try {
            builder
                    .setTags(tags)
                    .setCreationDate(creationDate)
                    .setModelVersion(modelVersion)
                    .build(resource)
        } catch (e: Exception) {
            // Then
            assertTrue(e is CoreRuntimeException.InternalFailure)
        }
    }

    @Test
    fun `Given, setTags, setDataKey and setModelVersion are called with their appropriate payload, but the other mandatory setters are not called, it fails if build is called with a DecryptedRecord`() {
        // Given
        val builder = DecryptedRecordBuilder()

        // When
        try {
            builder
                    .setTags(tags)
                    .setDataKey(dataKey)
                    .setModelVersion(modelVersion)
                    .build(resource)
        } catch (e: Exception) {
            // Then
            assertTrue(e is CoreRuntimeException.InternalFailure)
        }
    }

    @Test
    fun `Given, setCreationDate, setDataKey and setModelVersion are called with their appropriate payload, but the other mandatory setters are not called, it fails if build is called with a DecryptedRecord`() {
        // Given
        val builder = DecryptedRecordBuilder()

        // When
        try {
            builder
                    .setCreationDate(creationDate)
                    .setDataKey(dataKey)
                    .setModelVersion(modelVersion)
                    .build(resource)
        } catch (e: Exception) {
            // Then
            assertTrue(e is CoreRuntimeException.InternalFailure)
        }
    }

    @Test
    fun `Given, setTags, setCreationDate, setDataKey and setModelVersion are called with their appropriate payload, it returns a DecryptedDataRecord a DecryptedRecord`() {
        // Given
        val builder = DecryptedRecordBuilder()

        // When
        val record = builder
                .setTags(tags)
                .setCreationDate(creationDate)
                .setDataKey(dataKey)
                .setModelVersion(modelVersion)
                .build(resource)

        // Then
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
    fun `Given, setTags is called with null, it resets the Tags`() {
        // Given
        val builder = DecryptedRecordBuilder()

        // When
        try {
            builder
                    .setTags(tags)
                    .setCreationDate(creationDate)
                    .setDataKey(dataKey)
                    .setModelVersion(modelVersion)
                    .setTags(null)
                    .build(resource)
            assertTrue(false)// FIXME: This is stupid
        } catch (e: Exception) {
            // Then
            assertTrue(e is CoreRuntimeException.InternalFailure)
        }
    }

    @Test
    fun `Given, setCreationDate is called with null, it resets the CreationDate`() {
        // Given
        val builder = DecryptedRecordBuilder()

        // When
        try {
            builder
                    .setTags(tags)
                    .setCreationDate(creationDate)
                    .setDataKey(dataKey)
                    .setModelVersion(modelVersion)
                    .setCreationDate(null)
                    .build(resource)
            assertTrue(false)// FIXME: This is stupid
        } catch (e: Exception) {
            // Then
            assertTrue(e is CoreRuntimeException.InternalFailure)
        }
    }

    @Test
    fun `Given, setDataKey is called with null, it resets the DataKey`() {
        // Given
        val builder = DecryptedRecordBuilder()

        // When
        try {
            builder
                    .setTags(tags)
                    .setCreationDate(creationDate)
                    .setDataKey(dataKey)
                    .setModelVersion(modelVersion)
                    .setDataKey(null)
                    .build(resource)
            assertTrue(false)// FIXME: This is stupid
        } catch (e: Exception) {
            // Then
            assertTrue(e is CoreRuntimeException.InternalFailure)
        }
    }

    @Test
    fun `Given, setModelVersion is called with null, it resets the DataKey`() {
        // Given
        val builder = DecryptedRecordBuilder()

        // When
        try {
            builder
                    .setTags(tags)
                    .setCreationDate(creationDate)
                    .setDataKey(dataKey)
                    .setModelVersion(modelVersion)
                    .setModelVersion(null)
                    .build(resource)
            assertTrue(false)// FIXME: This is stupid
        } catch (e: Exception) {
            // Then
            assertTrue(e is CoreRuntimeException.InternalFailure)
        }
    }

    @Test
    fun `Given, clear is called, it resets all mandatory values`() {
        val builder = DecryptedRecordBuilder()

        // When
        try {
            builder
                    .setTags(tags)
                    .setCreationDate(creationDate)
                    .setDataKey(dataKey)
                    .setModelVersion(modelVersion)
                    .clear()
                    .build(resource)
            assertTrue(false)// FIXME: This is stupid
        } catch (e: Exception) {
            assertTrue(e is CoreRuntimeException.InternalFailure)
        }

        try {
            builder
                    .setTags(tags)
                    .setCreationDate(creationDate)
                    .setDataKey(dataKey)
                    .setModelVersion(modelVersion)
                    .clear()
                    .setTags(tags)
                    .build(resource)
            assertTrue(false)// FIXME: This is stupid
        } catch (e: Exception) {
            assertTrue(e is CoreRuntimeException.InternalFailure)
        }

        try {
            builder
                    .setTags(tags)
                    .setCreationDate(creationDate)
                    .setDataKey(dataKey)
                    .setModelVersion(modelVersion)
                    .clear()
                    .setTags(tags)
                    .setCreationDate(creationDate)
                    .build(resource)
            assertTrue(false)// FIXME: This is stupid
        } catch (e: Exception) {
            assertTrue(e is CoreRuntimeException.InternalFailure)
        }

        try {
            builder
                    .setTags(tags)
                    .setCreationDate(creationDate)
                    .setDataKey(dataKey)
                    .setModelVersion(modelVersion)
                    .clear()
                    .setTags(tags)
                    .setCreationDate(creationDate)
                    .setDataKey(dataKey)
                    .build(resource)
            assertTrue(false)// FIXME: This is stupid
        } catch (e: Exception) {
            assertTrue(e is CoreRuntimeException.InternalFailure)
        }

        val record = builder
                .setTags(tags)
                .setCreationDate(creationDate)
                .setDataKey(dataKey)
                .setModelVersion(modelVersion)
                .clear()
                .setTags(tags)
                .setCreationDate(creationDate)
                .setDataKey(dataKey)
                .setModelVersion(modelVersion)
                .build(resource)

        // Then
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
}
