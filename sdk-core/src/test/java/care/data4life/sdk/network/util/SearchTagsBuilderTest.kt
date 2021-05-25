/*
 * Copyright (c) 2021 D4L data4life gGmbH / All rights reserved.
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

package care.data4life.sdk.network.util

import care.data4life.sdk.network.NetworkingContract
import io.mockk.mockk
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue

class SearchTagsBuilderTest {
    private lateinit var builder: NetworkingContract.SearchTagsBuilder

    @Before
    fun setUp() {
        builder = SearchTagsBuilder.newBuilder()
    }

    @Test
    fun `It fulfils SearchTagsBuilderFactory`() {
        val factory: Any = SearchTagsBuilder

        assertTrue(factory is NetworkingContract.SearchTagsBuilderFactory)
    }

    @Test
    fun `Given newBuilder is called, it returns a SearchTagsBuilder`() {
        val builder: Any = SearchTagsBuilder.newBuilder()

        assertTrue(builder is NetworkingContract.SearchTagsBuilder)
    }

    @Test
    fun `Given seal is called on existing builder, it returns a SearchTags`() {
        val builder: Any = SearchTagsBuilder.newBuilder().seal()

        assertTrue(builder is NetworkingContract.SearchTags)
    }

    @Test
    fun `Given addOrTuple on existing builder is called, it returns the SearchTagsBuilder`() {
        val builder: Any = this.builder.addOrTuple(mockk())

        assertSame(
            actual = builder,
            expected = builder
        )
    }

    @Test
    fun `Given before seal is called on existing builder, addOrTuple with an arbitrary number of singles it returns a comma separated list`() {
        // Given
        val singles = listOf(
            listOf("a"),
            listOf("b"),
            listOf("c"),
            listOf("d"),
            listOf("e"),
        )

        // When
        var builder = this.builder
        singles.forEach { single -> builder = builder.addOrTuple(single) }

        val searchTags = builder.seal().tagGroups

        // Then
        assertEquals(
            actual = searchTags,
            expected = "a,b,c,d,e"
        )
    }

    @Test
    fun `Given before seal is called on existing builder, addOrTuple with an arbitrary number of singles, which contain duplicates, it removes them and it returns a comma separated list`() {
        // Given
        val singles = listOf(
            listOf("a"),
            listOf("b"),
            listOf("a"),
            listOf("d"),
            listOf("a"),
        )

        // When
        var builder = this.builder
        singles.forEach { single -> builder = builder.addOrTuple(single) }

        val searchTags = builder.seal().tagGroups

        // Then
        assertEquals(
            actual = searchTags,
            expected = "a,b,d"
        )
    }

    @Test
    fun `Given before seal is called on existing builder, addOrTuple with an arbitrary number of arbitrary tuples it removes duplicates in the tuples, while bracing them`() {
        // Given
        val singles = listOf(
            listOf("a", "a", "b"),
            listOf("c", "d", "c"),
            listOf("e", "f")
        )

        // When
        var builder = this.builder
        singles.forEach { single -> builder = builder.addOrTuple(single) }

        val searchTags = builder.seal().tagGroups

        // Then
        assertEquals(
            actual = searchTags,
            expected = "(a,b),(c,d),(e,f)"
        )
    }

    @Test
    fun `Given before seal is called on existing builder, addOrTuple with an arbitrary number of arbitrary tuples it returns a comma separated list, while bracing tuples`() {
        // Given
        val singles = listOf(
            listOf("a", "b", "c"),
            listOf("d", "e", "f"),
            listOf("g", "h")
        )

        // When
        var builder = this.builder
        singles.forEach { single -> builder = builder.addOrTuple(single) }

        val searchTags = builder.seal().tagGroups

        // Then
        assertEquals(
            actual = searchTags,
            expected = "(a,b,c),(d,e,f),(g,h)"
        )
    }

    @Test
    fun `Given before seal is called on existing builder, addOrTuple with an arbitrary number of arbitrary tuples it returns a comma separated list, it determines braces correctly`() {
        // Given
        val singles = listOf(
            listOf("a", "a"),
            listOf("b", "c"),
            listOf("d", "d", "d"),
            listOf("e", "f")
        )

        // When
        var builder = this.builder
        singles.forEach { single -> builder = builder.addOrTuple(single) }

        val searchTags = builder.seal().tagGroups

        // Then
        assertEquals(
            actual = searchTags,
            expected = "a,(b,c),d,(e,f)"
        )
    }
}
