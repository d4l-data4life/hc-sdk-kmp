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
import care.data4life.fhir.stu3.model.DomainResource
import care.data4life.sdk.network.model.DecryptedAppDataRecord
import care.data4life.sdk.network.model.DecryptedRecord
import care.data4life.sdk.network.model.definitions.DecryptedRecordBuilder
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito

class DecryptedRecordBuilderTest {
    private lateinit var identifier: String
    private lateinit var tags: HashMap<String, String>
    private lateinit var annotations: List<String>
    private lateinit var creationDate: String
    private lateinit var updateDate: String
    private lateinit var dataKey: GCKey
    private var attachmentKey: GCKey? = null
    private var modelVersion: Int = 0
    private lateinit var fhirResource: DomainResource
    private lateinit var customResource: ByteArray

    @Before
    fun setUp() {
        identifier = "potato"
        @Suppress("UNCHECKED_CAST")
        tags = Mockito.mock(HashMap::class.java) as HashMap<String, String>
        @Suppress("UNCHECKED_CAST")
        annotations = Mockito.mock(List::class.java) as List<String>
        creationDate = "A Date"
        updateDate = "2020-05-03"
        dataKey = Mockito.mock(GCKey::class.java)
        attachmentKey = Mockito.mock(GCKey::class.java)
        modelVersion = 42
        fhirResource = Mockito.mock(DomainResource::class.java)
        customResource = ByteArray(42)
    }


    @Test
    fun `it is a DecryptedRecordBuilder`() {
        val builder: Any = DecryptedRecordBuilderImpl()

        Assert.assertTrue(builder is DecryptedRecordBuilder)
    }

    @Test
    fun `Given, mandatory and optional setters are called with their appropriate payload, it returns a DecryptedFhireRecord`() {
        // Given
        val builder = DecryptedRecordBuilderImpl()

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
        Assert.assertEquals(
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
    fun `Given, mandatory and optional setters are called with their appropriate payload, it returns a DecryptedDataRecord`() {
        // Given
        val builder = DecryptedRecordBuilderImpl()

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
                .build(customResource)

        Assert.assertEquals(
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
}
