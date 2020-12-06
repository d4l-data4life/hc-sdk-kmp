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

import io.mockk.mockk
import io.mockk.mockkClass
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppDataRecordTest {
    @Test
    fun `Given tow AppDataRecords, it returns true on a compare, if they are equal`() {
        val identifier = "1234"
        val resource = "test".toByteArray()
        val meta = mockkClass(Meta::class)
        val annotations = listOf("a", "b", "c")

        val record1 = AppDataRecord(
                appDataResource = resource,
                identifier = identifier,
                meta = meta,
                annotations = annotations
        )

        val record2 = AppDataRecord(
                appDataResource = resource,
                identifier = identifier,
                meta = meta,
                annotations = annotations
        )

        assertTrue(record1 == record2)
    }

    @Test
    fun `Given a AppDataRecord and something else, it returns false on a compare`() {
        val record = AppDataRecord(
                appDataResource =  "test".toByteArray(),
                identifier = "1234",
                meta = mockkClass(Meta::class),
                annotations = listOf("a", "b", "c")
        )

        assertFalse(record.equals(null))
        assertFalse(record.equals("null"))
    }

    @Test
    fun `Given tow AppDataRecords, it returns false on a compare, if the resources are different`() {
        val identifier = "1234"
        val meta = mockkClass(Meta::class)
        val annotations = listOf("a", "b", "c")

        val record1 = AppDataRecord(
                appDataResource = "test1".toByteArray(),
                identifier = identifier,
                meta = meta,
                annotations = annotations
        )

        val record2 = AppDataRecord(
                appDataResource = "test2".toByteArray(),
                identifier = identifier,
                meta = meta,
                annotations = annotations
        )

        assertFalse(record1 == record2)
    }

    @Test
    fun `Given tow AppDataRecords, it returns false on a compare, if the identifiers are different`() {
        val resource = "test".toByteArray()
        val meta = mockkClass(Meta::class)
        val annotations = listOf("a", "b", "c")

        val record1 = AppDataRecord(
                appDataResource = resource,
                identifier = "123",
                meta = meta,
                annotations = annotations
        )

        val record2 = AppDataRecord(
                appDataResource = resource,
                identifier = "345",
                meta = meta,
                annotations = annotations
        )

        assertFalse(record1 == record2)
    }

    @Test
    fun `Given tow AppDataRecords, it returns false on a compare, if the metas are different`() {
        val identifier = "1234"
        val resource = "test".toByteArray()
        val annotations = listOf("a", "b", "c")

        val record1 = AppDataRecord(
                appDataResource = resource,
                identifier = identifier,
                meta = mockk(),
                annotations = annotations
        )

        val record2 = AppDataRecord(
                appDataResource = resource,
                identifier = identifier,
                meta = mockk(),
                annotations = annotations
        )

        assertFalse(record1 == record2)
    }

    @Test
    fun `Given tow AppDataRecords, it returns false on a compare, if the annotations are different`() {
        val identifier = "1234"
        val resource = "test".toByteArray()
        val meta = mockkClass(Meta::class)

        val record1 = AppDataRecord(
                appDataResource = resource,
                identifier = identifier,
                meta = meta,
                annotations = listOf("a", "b", "c")
        )

        val record2 = AppDataRecord(
                appDataResource = resource,
                identifier = identifier,
                meta = meta,
                annotations = listOf("d", "e", "f")
        )

        assertFalse(record1 == record2)
    }

    @Test
    fun `Given a AppDataRecord, it has a stable HashCode`() {
        val identifier = "1234"
        val resource = "test".toByteArray()
        val meta = mockkClass(Meta::class)
        val annotations = listOf("a", "b", "c")

        val record1 = AppDataRecord(
                appDataResource = resource,
                identifier = identifier,
                meta = meta,
                annotations = annotations
        )

        val record2 = record1.copy()
        val record3 = record1.copy(appDataResource = "resource".toByteArray())
        val record4 = record1.copy(identifier = "23")
        val record5 = record1.copy(meta = mockk())
        val record6 = record1.copy(annotations = listOf("e","f","g"))

        assertTrue(record1.hashCode() == record2.hashCode())
        assertFalse(record1.hashCode() == record3.hashCode())
        assertFalse(record1.hashCode() == record4.hashCode())
        assertFalse(record1.hashCode() == record5.hashCode())
        assertFalse(record1.hashCode() == record6.hashCode())
    }
}
