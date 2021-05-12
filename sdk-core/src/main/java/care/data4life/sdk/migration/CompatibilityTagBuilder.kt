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

package care.data4life.sdk.migration

import care.data4life.crypto.GCKey
import care.data4life.sdk.network.NetworkingContract
import care.data4life.sdk.network.util.SearchTagsPipe
import care.data4life.sdk.tag.TaggingContract

internal class CompatibilityTagBuilder private constructor(
    private val tagCryptoService: TaggingContract.CryptoService,
    private val tagEncryptionKey: GCKey,
    private val pipe: NetworkingContract.SearchTagsPipeIn
) : MigrationContract.CompatibilityTagBuilder {
    private fun encryptTags(
        tagEncryptionKey: GCKey,
        tagGroupKey: String,
        tagOrGroup: Triple<String, String, String>
    ): List<String> {
        return tagCryptoService.encryptList(
            tagOrGroup.toList(),
            tagEncryptionKey,
            tagGroupKey
        )
    }

    override fun add(
        tagGroupKey: String,
        tagOrGroup: Triple<String, String, String>
    ): MigrationContract.CompatibilityTagBuilder {
        val encryptedOrGroup = encryptTags(
            tagEncryptionKey,
            "$tagGroupKey${TaggingContract.DELIMITER}",
            tagOrGroup
        )

        pipe.addOrTuple(encryptedOrGroup)

        return this
    }

    override fun build(): NetworkingContract.SearchTagsPipeOut = pipe.seal()

    companion object Factory : MigrationContract.CompatibilityTagBuilderFactory {
        override fun newBuilder(
            tagCryptoService: TaggingContract.CryptoService,
            tagEncryptionKey: GCKey
        ): MigrationContract.CompatibilityTagBuilder = CompatibilityTagBuilder(
            tagCryptoService,
            tagEncryptionKey,
            SearchTagsPipe.newPipe()
        )
    }
}
