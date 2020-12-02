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
import io.mockk.mockk
import io.mockk.mockkClass
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DecryptedAppDataRecordTest {
    @Test
    fun `Given tow DecryptedAppDataRecord, it returns true on a compare, if they are equal`() {
        val id = "123"
        val data = "potato".toByteArray()
        val tags = hashMapOf("soup" to "tomato")
        val annotations = listOf("a", "b", "c")
        val creation = "today"
        val update = "yesterday"
        val dataKey = mockkClass(GCKey::class)
        val version = 42

        val record1 = DecryptedAppDataRecord(
                identifier = id,
                appData = data,
                tags = tags,
                annotations = annotations,
                customCreationDate = creation,
                updatedDate = update,
                dataKey = dataKey,
                modelVersion = version
        )

        val record2 = DecryptedAppDataRecord(
                identifier = id,
                appData = data,
                tags = tags,
                annotations = annotations,
                customCreationDate = creation,
                updatedDate = update,
                dataKey = dataKey,
                modelVersion = version
        )

        assertTrue(record1 == record2)
    }

    @Test
    fun `Given a DecryptedAppDataRecord and something else, it returns false on a compare, if they are equal`() {
        val record = DecryptedAppDataRecord(
                identifier = "123",
                appData = "potato".toByteArray(),
                tags = hashMapOf("soup" to "tomato"),
                annotations = listOf("a", "b", "c"),
                customCreationDate = "today",
                updatedDate = "yesterday",
                dataKey = mockkClass(GCKey::class),
                modelVersion = 42
        )

        assertFalse(record.equals(null))
        assertFalse(record.equals("null"))
    }

    @Test
    fun `Given tow DecryptedAppDataRecords, it returns false on a compare, if they have different ids`() {
        val data = "potato".toByteArray()
        val tags = hashMapOf("soup" to "tomato")
        val annotations = listOf("a", "b", "c")
        val creation = "today"
        val update = "yesterday"
        val dataKey = mockkClass(GCKey::class)
        val version = 42

        val record1 = DecryptedAppDataRecord(
                identifier = "23",
                appData = data,
                tags = tags,
                annotations = annotations,
                customCreationDate = creation,
                updatedDate = update,
                dataKey = dataKey,
                modelVersion = version
        )

        val record2 = DecryptedAppDataRecord(
                identifier = "7",
                appData = data,
                tags = tags,
                annotations = annotations,
                customCreationDate = creation,
                updatedDate = update,
                dataKey = dataKey,
                modelVersion = version
        )

        assertFalse(record1 == record2)
    }

    @Test
    fun `Given tow DecryptedAppDataRecords, it returns false on a compare, if they have different appData`() {
        val id = "123"
        val tags = hashMapOf("soup" to "tomato")
        val annotations = listOf("a", "b", "c")
        val creation = "today"
        val update = "yesterday"
        val dataKey = mockkClass(GCKey::class)
        val version = 42

        val record1 = DecryptedAppDataRecord(
                identifier = id,
                appData = "potato".toByteArray(),
                tags = tags,
                annotations = annotations,
                customCreationDate = creation,
                updatedDate = update,
                dataKey = dataKey,
                modelVersion = version
        )

        val record2 = DecryptedAppDataRecord(
                identifier = id,
                appData = "tomato".toByteArray(),
                tags = tags,
                annotations = annotations,
                customCreationDate = creation,
                updatedDate = update,
                dataKey = dataKey,
                modelVersion = version
        )

        assertFalse(record1 == record2)
    }

    @Test
    fun `Given tow DecryptedAppDataRecords, it returns false on a compare, if they have different tags`() {
        val id = "123"
        val data = "potato".toByteArray()
        val annotations = listOf("a", "b", "c")
        val creation = "today"
        val update = "yesterday"
        val dataKey = mockkClass(GCKey::class)
        val version = 42

        val record1 = DecryptedAppDataRecord(
                identifier = id,
                appData = data,
                tags = hashMapOf("soup" to "tomato"),
                annotations = annotations,
                customCreationDate = creation,
                updatedDate = update,
                dataKey = dataKey,
                modelVersion = version
        )

        val record2 = DecryptedAppDataRecord(
                identifier = id,
                appData = data,
                tags = hashMapOf("tomato" to "soup"),
                annotations = annotations,
                customCreationDate = creation,
                updatedDate = update,
                dataKey = dataKey,
                modelVersion = version
        )

        assertFalse(record1 == record2)
    }

    @Test
    fun `Given tow DecryptedAppDataRecords, it returns false on a compare, if they have different annotations`() {
        val id = "123"
        val data = "potato".toByteArray()
        val tags = hashMapOf("soup" to "tomato")
        val creation = "today"
        val update = "yesterday"
        val dataKey = mockkClass(GCKey::class)
        val version = 42

        val record1 = DecryptedAppDataRecord(
                identifier = id,
                appData = data,
                tags = tags,
                annotations = listOf("a", "b", "c"),
                customCreationDate = creation,
                updatedDate = update,
                dataKey = dataKey,
                modelVersion = version
        )

        val record2 = DecryptedAppDataRecord(
                identifier = id,
                appData = data,
                tags = tags,
                annotations = listOf("c", "d", "e"),
                customCreationDate = creation,
                updatedDate = update,
                dataKey = dataKey,
                modelVersion = version
        )

        assertFalse(record1 == record2)
    }

    @Test
    fun `Given tow DecryptedAppDataRecords, it returns false on a compare, if they have different customCreationDates`() {
        val id = "123"
        val data = "potato".toByteArray()
        val tags = hashMapOf("soup" to "tomato")
        val annotations = listOf("a", "b", "c")
        val update = "yesterday"
        val dataKey = mockkClass(GCKey::class)
        val version = 42

        val record1 = DecryptedAppDataRecord(
                identifier = id,
                appData = data,
                tags = tags,
                annotations = annotations,
                customCreationDate = "today",
                updatedDate = update,
                dataKey = dataKey,
                modelVersion = version
        )

        val record2 = DecryptedAppDataRecord(
                identifier = id,
                appData = data,
                tags = tags,
                annotations = annotations,
                customCreationDate = "tomorrow",
                updatedDate = update,
                dataKey = dataKey,
                modelVersion = version
        )

        assertFalse(record1 == record2)
    }

    @Test
    fun `Given tow DecryptedAppDataRecords, it returns false on a compare, if they have different updatedDates`() {
        val id = "123"
        val data = "potato".toByteArray()
        val tags = hashMapOf("soup" to "tomato")
        val annotations = listOf("a", "b", "c")
        val creation = "today"
        val dataKey = mockkClass(GCKey::class)
        val version = 42

        val record1 = DecryptedAppDataRecord(
                identifier = id,
                appData = data,
                tags = tags,
                annotations = annotations,
                customCreationDate = creation,
                updatedDate = "yesterday",
                dataKey = dataKey,
                modelVersion = version
        )

        val record2 = DecryptedAppDataRecord(
                identifier = id,
                appData = data,
                tags = tags,
                annotations = annotations,
                customCreationDate = creation,
                updatedDate = "before today",
                dataKey = dataKey,
                modelVersion = version
        )

        assertFalse(record1 == record2)
    }

    @Test
    fun `Given tow DecryptedAppDataRecords, it returns false on a compare, if they have different dataKeys`() {
        val id = "123"
        val data = "potato".toByteArray()
        val tags = hashMapOf("soup" to "tomato")
        val annotations = listOf("a", "b", "c")
        val creation = "today"
        val update = "yesterday"
        val version = 42

        val record1 = DecryptedAppDataRecord(
                identifier = id,
                appData = data,
                tags = tags,
                annotations = annotations,
                customCreationDate = creation,
                updatedDate = update,
                dataKey = mockk(),
                modelVersion = version
        )

        val record2 = DecryptedAppDataRecord(
                identifier = id,
                appData = data,
                tags = tags,
                annotations = annotations,
                customCreationDate = creation,
                updatedDate = update,
                dataKey = mockk(),
                modelVersion = version
        )

        assertFalse(record1 == record2)
    }

    @Test
    fun `Given tow DecryptedAppDataRecords, it returns false on a compare, if they have different versions`() {
        val id = "123"
        val data = "potato".toByteArray()
        val tags = hashMapOf("soup" to "tomato")
        val annotations = listOf("a", "b", "c")
        val creation = "today"
        val update = "yesterday"
        val dataKey = mockkClass(GCKey::class)

        val record1 = DecryptedAppDataRecord(
                identifier = id,
                appData = data,
                tags = tags,
                annotations = annotations,
                customCreationDate = creation,
                updatedDate = update,
                dataKey = dataKey,
                modelVersion = 23
        )

        val record2 = DecryptedAppDataRecord(
                identifier = id,
                appData = data,
                tags = tags,
                annotations = annotations,
                customCreationDate = creation,
                updatedDate = update,
                dataKey = dataKey,
                modelVersion = 7
        )

        assertFalse(record1 == record2)
    }

    @Test
    fun `Given a DecryptedAppDataRecord, it is cloneable with new appData, while coping the old annotations`() {
        val expectedData = "tomato"
        val expectedAnnotations = listOf("my", "little", "pony")
        val record = DecryptedAppDataRecord(
                identifier = "123",
                appData = "potato".toByteArray(),
                tags = hashMapOf("soup" to "tomato"),
                annotations = expectedAnnotations,
                customCreationDate = "today",
                updatedDate = "yesterday",
                dataKey = mockkClass(GCKey::class),
                modelVersion = 42
        ).copyWithResourceAnnotaions(
                expectedData.toByteArray()
        )

        assertTrue(record.appData.contentEquals(expectedData.toByteArray()))
        assertEquals(
                expectedAnnotations,
                record.annotations
        )
    }

    @Test
    fun `Given a DecryptedAppDataRecord, it is cloneable with new appData and annotations`() {
        val expectedData = "tomato"
        val expectedAnnotations = listOf("my", "little", "pony")
        val record = DecryptedAppDataRecord(
                identifier = "123",
                appData = "potato".toByteArray(),
                tags = hashMapOf("soup" to "tomato"),
                annotations = listOf("a", "b", "c"),
                customCreationDate = "today",
                updatedDate = "yesterday",
                dataKey = mockkClass(GCKey::class),
                modelVersion = 42
        ).copyWithResourceAnnotaions(
                expectedData.toByteArray(),
                expectedAnnotations
        )

        assertTrue(record.appData.contentEquals(expectedData.toByteArray()))
        assertEquals(
                expectedAnnotations,
                record.annotations
        )
    }

    @Test
    fun `Given a DecryptedAppDataRecord, it has a stable HashCode`() {
        val id = "123"
        val data = "potato".toByteArray()
        val tags = hashMapOf("soup" to "tomato")
        val annotations = listOf("a", "b", "c")
        val creation = "today"
        val update = "yesterday"
        val dataKey = mockkClass(GCKey::class)
        val version = 42

        val record1 = DecryptedAppDataRecord(
                identifier = id,
                appData = data,
                tags = tags,
                annotations = annotations,
                customCreationDate = creation,
                updatedDate = update,
                dataKey = dataKey,
                modelVersion = version
        )

        val record2 = record1.copy()
        val record3 = record1.copy(identifier = null)
        val record4 = record1.copy(appData = "something".toByteArray())
        val record5 = record1.copy(tags = hashMapOf("tomator" to "soup"))
        val record6 = record1.copy(annotations = listOf("k", "f", "d"))
        val record7 = record1.copy(customCreationDate = "not today")
        val record8 = record1.copy(updatedDate = "not tomorrow")
        val record9 = record1.copy(dataKey = mockk())
        val record10 = record1.copy(modelVersion = 23)

        assertTrue(record1.hashCode() == record2.hashCode())
        assertFalse(record1.hashCode() == record3.hashCode())
        assertFalse(record1.hashCode() == record4.hashCode())
        assertFalse(record1.hashCode() == record5.hashCode())
        assertFalse(record1.hashCode() == record6.hashCode())
        assertFalse(record1.hashCode() == record7.hashCode())
        assertFalse(record1.hashCode() == record8.hashCode())
        assertFalse(record1.hashCode() == record9.hashCode())
        assertFalse(record1.hashCode() == record10.hashCode())
    }
}
