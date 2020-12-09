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

import care.data4life.fhir.stu3.model.DomainResource
import io.mockk.mockk
import io.mockk.mockkClass
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class RecordTest {
    @Test
    fun `Given a Record it has an empty string identifier by default`() {
        val record = Record<DomainResource>(null, null)

        assertEquals(
                "",
                record.identifier
        )
    }

    @Test
    fun `Given a Record it has no Resource by default`() {
        val record = Record<DomainResource>(null, null)

        assertNull(record.resource)
    }

    @Test
    fun `Given a Record, which is initialized with a Resource, it returns a given Resource`() {
        val resource = mockkClass(DomainResource::class)
        val record = Record(resource, null)

        assertSame(
                record.resource,
                resource
        )
    }

    @Test
    fun `Given a Record, which is initialized with a Resource, its FhireResource is a alias of Resource`() {
        val resource = mockkClass(DomainResource::class)
        val record1 = Record<DomainResource>(null, null)
        val record2 = Record(resource, null)

        assertNull(record1.resource)
        assertNull(record1.fhirResource)
        assertSame(
                record2.resource,
                resource
        )
        assertSame(
                record2.fhirResource,
                resource
        )
    }

    @Test
    fun `Given a Record it has no Meta by default`() {
        val record = Record<DomainResource>(null, null)

        assertNull(record.fhirResource)
    }

    @Test
    fun `Given a Record, which is initialized with a Meta, it returns a given Meta`() {
        val meta = mockkClass(Meta::class)
        val record = Record<DomainResource>(null, meta)

        assertSame(
                record.meta,
                meta
        )
    }

    @Test
    fun `Given a Record it has no Annotations by default`() {
        val record = Record<DomainResource>(null, null)

        assertNull(record.fhirResource)
    }

    @Test
    fun `Given a Record, which is initialized with a Annotations, it returns a given Annotations`() {
        val annotations = listOf<String>()
        val record = Record<DomainResource>(null, null, annotations)

        assertSame(
                record.annotations,
                annotations
        )
    }

    @Test
    fun `Given tow Records, on a compare, it returns true if they are equal`() {
        val resource = mockkClass(DomainResource::class)
        val meta = mockkClass(Meta::class)
        val annotations = listOf("a", "b", "c")

        val record1 = Record(
                resource,
                meta,
                annotations
        )

        val record2 = Record(
                resource,
                meta,
                annotations
        )

        assertTrue(record1 == record2)
    }

    @Test
    fun `Given tow Records, on a compare, it returns false if they have different resources`() {
        val meta = mockkClass(Meta::class)
        val annotations = listOf("a", "b", "c")

        val record1 = Record(
                mockkClass(DomainResource::class),
                meta,
                annotations
        )

        val record2 = Record(
                mockkClass(DomainResource::class),
                meta,
                annotations
        )

        assertFalse(record1 == record2)
    }

    @Test
    fun `Given tow Records, on a compare, it returns false if they have different Metas`() {
        val resource = mockkClass(DomainResource::class)
        val annotations = listOf("a", "b", "c")

        val record1 = Record(
                resource,
                mockk(),
                annotations
        )

        val record2 = Record(
                resource,
                mockk(),
                annotations
        )

        assertFalse(record1 == record2)
    }

    @Test
    fun `Given tow Records, on a compare, it returns false if they have different annotations`() {
        val resource = mockkClass(DomainResource::class)
        val meta = mockkClass(Meta::class)

        val record1 = Record(
                resource,
                meta,
                listOf("a", "b", "c")
        )

        val record2 = Record(
                resource,
                meta,
                listOf("d", "e", "f")
        )

        assertFalse(record1 == record2)
    }

    @Test
    fun `Given a Record, it has a stable HashCode`() {
        val resource = mockkClass(DomainResource::class)
        val meta = mockkClass(Meta::class)
        val annotations = listOf("a", "b", "c")

        val record1 = Record(
                resource,
                meta,
                annotations
        )

        val record2 = Record(
                resource,
                meta,
                annotations
        )

        val record3 = Record(
                mockkClass(DomainResource::class),
                meta,
                annotations
        )

        val record4 = Record(
                resource,
                mockk(),
                annotations
        )

        val record5 = Record(
                resource,
                meta,
                listOf("d", "e", "f")
        )

        assertTrue(record1.hashCode() == record2.hashCode())
        assertFalse(record1.hashCode() == record3.hashCode())
        assertFalse(record1.hashCode() == record4.hashCode())
        assertFalse(record1.hashCode() == record5.hashCode())
    }
}
