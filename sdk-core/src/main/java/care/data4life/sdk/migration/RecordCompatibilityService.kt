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

import care.data4life.sdk.crypto.CryptoContract
import care.data4life.sdk.migration.MigrationInternalContract.CompatibilityTag
import care.data4life.sdk.crypto.GCKey
import care.data4life.sdk.network.NetworkingContract
import care.data4life.sdk.network.util.SearchTagsBuilder
import care.data4life.sdk.tag.Annotations
import care.data4life.sdk.tag.TaggingContract
import care.data4life.sdk.tag.TaggingContract.Companion.ANNOTATION_KEY
import care.data4life.sdk.tag.TaggingContract.Companion.DELIMITER
import care.data4life.sdk.tag.Tags

private data class OrGroupEntry(
    val key: String,
    val orGroup: CompatibilityTag
)

// see: https://gesundheitscloud.atlassian.net/browse/SDK-572
// see: https://gesundheitscloud.atlassian.net/browse/SDK-525
// see: https://gesundheitscloud.atlassian.net/browse/SDK-631
@Migration("This class should only be used due to migration purpose.")
class RecordCompatibilityService internal constructor(
    private val cryptoService: CryptoContract.Service,
    private val tagCryptoService: TaggingContract.CryptoService,
    private val compatibilityEncoder: MigrationInternalContract.CompatibilityEncoder = CompatibilityEncoder,
    private val searchTagsBuilderFactory: NetworkingContract.SearchTagsBuilderFactory = SearchTagsBuilder
) : MigrationContract.CompatibilityService {
    private fun compatibilityTagsToList(tag: CompatibilityTag): List<String> {
        return listOf(
            tag.validEncoding,
            tag.kmpLegacyEncoding,
            tag.jsLegacyEncoding,
            tag.iosLegacyEncoding
        )
    }

    private fun encryptTags(
        tagEncryptionKey: GCKey,
        tagGroupEntry: OrGroupEntry
    ): List<String> {
        return tagCryptoService.encryptList(
            compatibilityTagsToList(tagGroupEntry.orGroup),
            tagEncryptionKey,
            tagGroupEntry.key
        )
    }

    private fun mapTags(
        tags: Tags,
        tagEncryptionKey: GCKey
    ): List<List<String>> {
        return tags.map { tagGroup ->
            OrGroupEntry(
                tagGroup.key + DELIMITER,
                compatibilityEncoder.encode(tagGroup.value)
            )
        }.map { encodedTagGroup ->
            encryptTags(
                tagEncryptionKey,
                encodedTagGroup
            )
        }
    }

    private fun mapAnnotations(
        annotations: Annotations,
        tagEncryptionKey: GCKey
    ): List<List<String>> {
        return annotations.map { annotation ->
            OrGroupEntry(
                ANNOTATION_KEY + DELIMITER,
                compatibilityEncoder.encode(annotation).copy(kmpLegacyEncoding = annotation)
            )
        }.map { encodedTagGroup ->
            encryptTags(
                tagEncryptionKey,
                encodedTagGroup
            )
        }
    }

    override fun resolveSearchTags(
        tags: Tags,
        annotation: Annotations
    ): NetworkingContract.SearchTags {
        val builder = searchTagsBuilderFactory.newBuilder()
        val tagEncryptionKey = cryptoService.fetchTagEncryptionKey()

        mapTags(tags, tagEncryptionKey).forEach { tagGroup -> builder.addOrTuple(tagGroup) }
        mapAnnotations(annotation, tagEncryptionKey).forEach { tagGroup -> builder.addOrTuple(tagGroup) }

        return builder.seal()
    }
}
