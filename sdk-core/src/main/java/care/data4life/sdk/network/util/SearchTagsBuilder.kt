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

data class SearchTags(
    override val tagGroups: String
) : NetworkingContract.SearchTags

class SearchTagsBuilder private constructor() : NetworkingContract.SearchTagsBuilder {
    private val orTuples: MutableList<List<String>> = mutableListOf()

    override fun addOrTuple(tuple: List<String>): NetworkingContract.SearchTagsBuilder {
        orTuples.add(tuple)
        return this
    }

    private fun joinTuples(
        tuple: List<String>
    ): String {
        val unified = tuple.toSet()

        return if (unified.size > 1) {
            "(${unified.joinToString(",")})"
        } else {
            unified.joinToString(",")
        }
    }

    private fun formatTags(): String {
        return orTuples
            .toSet()
            .joinToString(",", transform = ::joinTuples)
    }

    override fun seal(): NetworkingContract.SearchTags = SearchTags(formatTags())

    companion object Factory : NetworkingContract.SearchTagsBuilderFactory {
        override fun newBuilder(): NetworkingContract.SearchTagsBuilder = SearchTagsBuilder()
    }
}
