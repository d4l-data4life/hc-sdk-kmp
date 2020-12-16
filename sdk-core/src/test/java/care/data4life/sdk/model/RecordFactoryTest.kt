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

import care.data4life.crypto.GCKey
import care.data4life.sdk.data.DataResource
import care.data4life.sdk.fhir.Fhir3Resource
import care.data4life.sdk.model.definitions.DataRecord
import care.data4life.sdk.model.definitions.Fhir3Record
import care.data4life.sdk.model.definitions.RecordFactory
import care.data4life.sdk.network.model.DecryptedRecord
import care.data4life.sdk.wrapper.WrapperContract
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.threeten.bp.LocalDate
import org.threeten.bp.LocalDateTime

class RecordFactoryTest {
    private lateinit var id: String
    private lateinit var fhir3Resource: Fhir3Resource
    private lateinit var dataResource: DataResource
    private lateinit var  tags: HashMap<String, String>
    private lateinit var annotations: List<String>
    private lateinit var creationDate: LocalDate
    private lateinit var updateDate: LocalDateTime
    private lateinit var  dataKey :GCKey
    private lateinit var attachmentKey :GCKey
    private var modelVersion = 0

    @Before
    fun setUp() {
        mockkStatic(LocalDate::class)
        mockkStatic(LocalDateTime::class)

        id = "id"
        fhir3Resource = mockk()
        dataResource = mockk()
        tags = mockk()
        annotations = mockk()
        creationDate = mockk()
        updateDate = mockk()
        dataKey =  mockk()
        attachmentKey = mockk()
        modelVersion = 42
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `it is a RecordFactory`() {
        assertTrue((SdkRecordFactory as Any) is RecordFactory)
    }

    @Test
    fun `Given, getInstance is called with a DecryptedFhir3Record, it returns a Fhir3Record`() {
        // Given
        val resource = mockk<WrapperContract.Resource>()
        val givenCreationDate = "2020-05-03"
        val givenUpdateDate = "2019-02-28T17:21:08.234123"

        every { resource.type } returns WrapperContract.Resource.TYPE.FHIR3
        every { resource.unwrap() } returns fhir3Resource


        every { LocalDate.parse(givenCreationDate, any()) } returns creationDate
        every { LocalDateTime.parse(givenUpdateDate, any()) } returns updateDate

        val decryptedRecord = DecryptedRecord(
                id,
                resource,
                tags,
                annotations,
                givenCreationDate,
                givenUpdateDate,
                dataKey,
                attachmentKey,
                modelVersion
        )

        // When
        val record = SdkRecordFactory.getInstance(decryptedRecord)

        // Then
        assertTrue(record is Fhir3Record<*>)
        // FIXME: Meta & Record should be a data class
        assertEquals(
                record.resource,
                fhir3Resource
        )
        assertEquals(
                record.meta!!.createdDate,
                creationDate
        )
        assertEquals(
                record.meta!!.updatedDate,
                updateDate
        )
        assertEquals(
                record.annotations,
                annotations
        )
    }

    @Test
    fun `Given, getInstance is called with a DecryptedDataRecord, it returns a DataRecord`() {
        // Given
        val resource = mockk<WrapperContract.Resource>()
        val givenCreationDate = "2020-05-03"
        val givenUpdateDate = "2019-02-28T17:21:08.234123"

        val rawResource = ByteArray(23)

        every { dataResource.asByteArray() } returns rawResource

        every { resource.type } returns WrapperContract.Resource.TYPE.DATA
        every { resource.unwrap() } returns dataResource

        every { LocalDate.parse(givenCreationDate, any()) } returns creationDate
        every { LocalDateTime.parse(givenUpdateDate, any()) } returns updateDate

        val decryptedRecord = DecryptedRecord(
                id,
                resource,
                tags,
                annotations,
                givenCreationDate,
                givenUpdateDate,
                dataKey,
                attachmentKey,
                modelVersion
        )

        // When
        val record = SdkRecordFactory.getInstance(decryptedRecord)

        // Then
        assertTrue(record is DataRecord)
        // FIXME: Meta & Record should be a data class
        assertEquals(
                record.identifier,
                id
        )
        assertEquals(
                record.resource,
                rawResource
        )
        assertEquals(
                record.meta!!.createdDate,
                creationDate
        )
        assertEquals(
                record.meta!!.updatedDate,
                updateDate
        )
        assertEquals(
                record.annotations,
                annotations
        )
    }
}
