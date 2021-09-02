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

package care.data4life.sdk.model

import care.data4life.sdk.call.DataRecord
import care.data4life.sdk.call.Fhir4Record
import care.data4life.sdk.crypto.GCKey
import care.data4life.sdk.data.DataResource
import care.data4life.sdk.date.SdkDateTimeFormatter
import care.data4life.sdk.fhir.Fhir3Resource
import care.data4life.sdk.fhir.Fhir4Resource
import care.data4life.sdk.lang.CoreRuntimeException
import care.data4life.sdk.model.ModelContract.Fhir3Record
import care.data4life.sdk.model.ModelInternalContract.RecordFactory
import care.data4life.sdk.network.model.DecryptedDataRecord
import care.data4life.sdk.network.model.DecryptedR4Record
import care.data4life.sdk.network.model.DecryptedRecord
import care.data4life.sdk.network.model.NetworkModelContract.DecryptedBaseRecord
import care.data4life.sdk.tag.Annotations
import care.data4life.sdk.tag.Tags
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.threeten.bp.LocalDate
import org.threeten.bp.LocalDateTime
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class RecordMapperTest {
    private lateinit var id: String
    private lateinit var tags: Tags
    private lateinit var annotations: Annotations
    private lateinit var dataKey: GCKey
    private lateinit var attachmentKey: GCKey
    private var modelVersion = 0

    @Before
    fun setUp() {
        mockkObject(SdkDateTimeFormatter)

        id = "id"
        tags = mockk()
        annotations = mockk()
        dataKey = mockk()
        attachmentKey = mockk()
        modelVersion = 42
    }

    @After
    fun tearDown() {
        unmockkObject(SdkDateTimeFormatter)
    }

    @Test
    fun `it is a RecordFactory`() {
        val factory: Any = RecordMapper

        assertTrue(factory is RecordFactory)
    }

    @Test
    fun `Given, getInstance is called with a DecryptedFhir3Record, it returns a Fhir3Record`() {
        // Given
        val givenCreationDate = "2020-05-03"
        val givenUpdateDate = "2019-02-28T17:21:08.234123"

        val expectedCreationDate = LocalDate.of(2020, 5, 3)
        val expectedUpdateDate = LocalDateTime.of(2019, 2, 28, 21, 8, 23, 4123000)
        val status = ModelContract.RecordStatus.Active

        val resource: Fhir3Resource = mockk()

        val decryptedRecord = DecryptedRecord(
            id,
            resource,
            tags,
            annotations,
            givenCreationDate,
            givenUpdateDate,
            dataKey,
            attachmentKey,
            modelVersion,
            status
        )

        every { SdkDateTimeFormatter.parseDate(givenCreationDate) } returns expectedCreationDate
        every { SdkDateTimeFormatter.parseDateTime(givenUpdateDate) } returns expectedUpdateDate

        // When
        val record = RecordMapper.getInstance(decryptedRecord)

        // Then
        assertTrue(record is Fhir3Record<*>)
        assertEquals(
            actual = record.identifier,
            expected = ""
        )
        assertEquals(
            actual = record.resource,
            expected = resource
        )
        assertEquals(
            actual = record.meta,
            expected = Meta(
                expectedCreationDate,
                expectedUpdateDate,
                status
            )
        )
        assertEquals(
            actual = record.annotations,
            expected = annotations
        )
    }

    @Test
    fun `Given, getInstance is called with a DecryptedFhir4Record, it returns a Fhir4Record`() {
        // Given
        val givenCreationDate = "2020-05-03"
        val givenUpdateDate = "2019-02-28T17:21:08.234123"

        val expectedCreationDate = LocalDate.of(2020, 5, 3)
        val expectedUpdateDate = LocalDateTime.of(2019, 2, 28, 21, 8, 23, 4123000)
        val status = ModelContract.RecordStatus.Active

        val resource: Fhir4Resource = mockk()

        val decryptedRecord = DecryptedR4Record(
            id,
            resource,
            tags,
            annotations,
            givenCreationDate,
            givenUpdateDate,
            dataKey,
            attachmentKey,
            modelVersion,
            status
        )

        every { SdkDateTimeFormatter.parseDate(givenCreationDate) } returns expectedCreationDate
        every { SdkDateTimeFormatter.parseDateTime(givenUpdateDate) } returns expectedUpdateDate

        // When
        val record = RecordMapper.getInstance(decryptedRecord)

        // Then
        assertTrue(record is Fhir4Record<*>)
        assertEquals(
            actual = record.identifier,
            expected = id
        )
        assertEquals(
            actual = record.resource,
            expected = resource
        )
        assertEquals(
            actual = record.meta,
            expected = Meta(
                expectedCreationDate,
                expectedUpdateDate,
                status
            )
        )
        assertEquals(
            actual = record.annotations,
            expected = annotations
        )
    }

    @Test
    fun `Given, getInstance is called with a DecryptedDataRecord, it returns a DataRecord`() {
        // Given
        val givenCreationDate = "2020-05-03"
        val givenUpdateDate = "2019-02-28T17:21:08.234123"

        val expectedCreationDate = LocalDate.of(2020, 5, 3)
        val expectedUpdateDate = LocalDateTime.of(2019, 2, 28, 21, 8, 23, 4123000)
        val status = ModelContract.RecordStatus.Active

        val resource: DataResource = mockk()

        val decryptedRecord = DecryptedDataRecord(
            id,
            resource,
            tags,
            annotations,
            givenCreationDate,
            givenUpdateDate,
            dataKey,
            modelVersion,
            status
        )

        every { SdkDateTimeFormatter.parseDate(givenCreationDate) } returns expectedCreationDate
        every { SdkDateTimeFormatter.parseDateTime(givenUpdateDate) } returns expectedUpdateDate

        // When
        val record = RecordMapper.getInstance(decryptedRecord)

        // Then
        assertTrue(record is DataRecord<*>)
        assertEquals(
            actual = record.identifier,
            expected = id
        )
        assertEquals(
            actual = record.resource,
            expected = resource
        )
        assertEquals(
            actual = record.meta,
            expected = Meta(
                expectedCreationDate,
                expectedUpdateDate,
                status
            )
        )
        assertEquals(
            actual = record.annotations,
            expected = annotations
        )
    }

    @Test
    fun `Given, getInstance is called with a unknown DecryptedRecord implementation, it fails with a InternalFailure`() {
        // Given
        val givenCreationDate = "2020-05-03"
        val givenUpdateDate = "2019-02-28T17:21:08.234123"

        val expectedCreationDate = LocalDate.of(2020, 5, 3)
        val expectedUpdateDate = LocalDateTime.of(2019, 2, 28, 21, 8, 23, 4123000)
        val status = ModelContract.RecordStatus.Active

        val decryptedRecord = DecryptedUnknownRecord(
            id,
            "fail",
            tags,
            annotations,
            givenCreationDate,
            givenUpdateDate,
            dataKey,
            attachmentKey,
            modelVersion,
            status
        )

        every { SdkDateTimeFormatter.parseDate(givenCreationDate) } returns expectedCreationDate
        every { SdkDateTimeFormatter.parseDateTime(givenUpdateDate) } returns expectedUpdateDate

        // When
        assertFailsWith<CoreRuntimeException.InternalFailure> {
            RecordMapper.getInstance(
                decryptedRecord
            )
        }
    }

    private data class DecryptedUnknownRecord<T : Any>(
        override var identifier: String?,
        override var resource: T,
        override var tags: Tags,
        override var annotations: Annotations,
        override var customCreationDate: String?,
        override var updatedDate: String?,
        override var dataKey: GCKey,
        override var attachmentsKey: GCKey?,
        override var modelVersion: Int,
        override var status: ModelContract.RecordStatus
    ) : DecryptedBaseRecord<T>
}
