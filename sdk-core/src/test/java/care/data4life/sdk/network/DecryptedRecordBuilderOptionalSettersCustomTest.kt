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

import care.data4life.sdk.network.model.DecryptedAppDataRecord
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class DecryptedRecordBuilderOptionalSettersCustomTest : DecryptedRecordBuilderTestBase() {
    @Before
    fun setUp() {
        init()
    }

    @Test
    fun `Given, build is called with a ByteArray, Tags, CreationDate, DataKey and ModelVersion, it returns a DecryptedDataRecord`() {
        // When
        val record = DecryptedRecordBuilderImpl().build(
                customResource,
                tags,
                creationDate,
                dataKey,
                modelVersion
        )

        assertEquals(
                record,
                DecryptedAppDataRecord(
                        "",
                        customResource,
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
    fun `Given, setIdentifier is called with a String, it sets the Identifier on build`() {
        // When
        val builder = DecryptedRecordBuilderImpl()
                .setIdentifier(identifier)

        // Then
        val record = builder.build(
                customResource,
                tags,
                creationDate,
                dataKey,
                modelVersion
        )

        assertEquals(
                record,
                DecryptedAppDataRecord(
                        identifier,
                        customResource,
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
    fun `Given, setIdentifier is called with null, it resets the Identifier on build`() {
        // When
        val builder = DecryptedRecordBuilderImpl()
                .setIdentifier(identifier)
                .setIdentifier(null)

        // Then
        val record = builder.build(
                customResource,
                tags,
                creationDate,
                dataKey,
                modelVersion
        )

        assertEquals(
                record,
                DecryptedAppDataRecord(
                        "",
                        customResource,
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
    fun `Given, setAnnotations is called with a List of Strings, it sets the Annotations on build`() {
        // When
        val builder = DecryptedRecordBuilderImpl()
                .setAnnotations(annotations)

        // Then
        val record = builder.build(
                customResource,
                tags,
                creationDate,
                dataKey,
                modelVersion
        )

        assertEquals(
                record,
                DecryptedAppDataRecord(
                        "",
                        customResource,
                        tags,
                        annotations,
                        creationDate,
                        null,
                        dataKey,
                        modelVersion
                )
        )
    }

    @Test
    fun `Given, setAnnotations is called with null, it resets the Annotations on build`() {
        // When
        val builder = DecryptedRecordBuilderImpl()
                .setAnnotations(annotations)
                .setAnnotations(null)

        // Then
        val record = builder.build(
                customResource,
                tags,
                creationDate,
                dataKey,
                modelVersion
        )

        assertEquals(
                record,
                DecryptedAppDataRecord(
                        "",
                        customResource,
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
    fun `Given, setUpdateDate is called with a Strings, it sets the UpdateDate on build`() {
        // When
        val builder = DecryptedRecordBuilderImpl()
                .setUpdateDate(updateDate)

        // Then
        val record = builder.build(
                customResource,
                tags,
                creationDate,
                dataKey,
                modelVersion
        )

        assertEquals(
                record,
                DecryptedAppDataRecord(
                        "",
                        customResource,
                        tags,
                        listOf(),
                        creationDate,
                        updateDate,
                        dataKey,
                        modelVersion
                )
        )
    }

    @Test
    fun `Given, setUpdateDate is called with null, it resets the UpdateDate on build`() {
        // When
        val builder = DecryptedRecordBuilderImpl()
                .setUpdateDate(updateDate)
                .setUpdateDate(null)

        // Then
        val record = builder.build(
                customResource,
                tags,
                creationDate,
                dataKey,
                modelVersion
        )

        assertEquals(
                record,
                DecryptedAppDataRecord(
                        "",
                        customResource,
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
    fun `Given, setAttachmentKey is called with a GCKey, it sets the AttachmentKey on build`() {
        // When
        val builder = DecryptedRecordBuilderImpl()
                .setAttachmentKey(attachmentKey)

        // Then
        val record = builder.build(
                customResource,
                tags,
                creationDate,
                dataKey,
                modelVersion
        )

        assertEquals(
                record,
                DecryptedAppDataRecord(
                        "",
                        customResource,
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
    fun `Given, setAttachmentKey is called with null, it resets the AttachmentKey on build`() {
        // When
        val builder = DecryptedRecordBuilderImpl()
                .setAttachmentKey(attachmentKey)
                .setAttachmentKey(null)

        // Then
        val record = builder.build(
                customResource,
                tags,
                creationDate,
                dataKey,
                modelVersion
        )

        assertEquals(
                record,
                DecryptedAppDataRecord(
                        "",
                        customResource,
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
    fun `Given, arbitrary are called, it uses the combination on build`() {
        // When
        val builder = DecryptedRecordBuilderImpl()
                .setIdentifier(identifier)
                .setAnnotations(annotations)
                .setUpdateDate(updateDate)
                .setAttachmentKey(attachmentKey)

        // Then
        val record = builder.build(
                customResource,
                tags,
                creationDate,
                dataKey,
                modelVersion
        )

        assertEquals(
                record,
                DecryptedAppDataRecord(
                        identifier,
                        customResource,
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
    fun `Given, clear is called, it resets all optional setters`() {
        // When
        val builder = DecryptedRecordBuilderImpl()
                .setIdentifier(identifier)
                .setAnnotations(annotations)
                .setUpdateDate(updateDate)
                .setAttachmentKey(attachmentKey)
                .clear()

        // Then
        val record = builder.build(
                customResource,
                tags,
                creationDate,
                dataKey,
                modelVersion
        )

        assertEquals(
                record,
                DecryptedAppDataRecord(
                        "",
                        customResource,
                        tags,
                        listOf(),
                        creationDate,
                        null,
                        dataKey,
                        modelVersion
                )
        )
    }
}
