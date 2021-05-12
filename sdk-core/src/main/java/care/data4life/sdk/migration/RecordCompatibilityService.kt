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
import care.data4life.sdk.crypto.CryptoContract
import care.data4life.sdk.network.NetworkingContract
import care.data4life.sdk.network.util.SearchTagsPipe
import care.data4life.sdk.tag.Annotations
import care.data4life.sdk.tag.TaggingContract
import care.data4life.sdk.tag.TaggingContract.Companion.ANNOTATION_KEY
import care.data4life.sdk.tag.Tags

// see: https://gesundheitscloud.atlassian.net/browse/SDK-572
// see: https://gesundheitscloud.atlassian.net/browse/SDK-525
@Migration("This class should only be used due to migration purpose.")
class RecordCompatibilityService internal constructor(
    private val cryptoService: CryptoContract.Service,
    private val tagCryptoService: TaggingContract.CryptoService,
    private val compatibilityEncoder: MigrationContract.CompatibilityEncoder = CompatibilityEncoder,
    private val searchTagsPipeBuilderFactory: NetworkingContract.SearchTagsPipeFactory = SearchTagsPipe
) : MigrationContract.CompatibilityService {
    private fun encryptTags(
        tagEncryptionKey: GCKey,
        tagGroupKey: String,
        tagOrGroup: List<String>
    ): List<String> {
        return tagCryptoService.encryptList(
            tagOrGroup.toList(),
            tagEncryptionKey,
            tagGroupKey
        )
    }

    private fun addTags(
        tagGroupKey: String,
        tagOrGroup: Triple<String, String, String>,
        tagEncryptionKey: GCKey,
        pipe: NetworkingContract.SearchTagsPipeIn
    ) {
        pipe.addOrTuple(
            encryptTags(
                tagEncryptionKey,
                tagGroupKey,
                tagOrGroup.toList()
            )
        )
    }

    private fun mapTags(
        tags: Tags,
        tagEncryptionKey: GCKey,
        pipe: NetworkingContract.SearchTagsPipeIn
    ) {
        tags.map { tagGroup ->
            Pair(
                tagGroup.key + TaggingContract.DELIMITER,
                compatibilityEncoder.encode(tagGroup.value)
            )
        }.map { encodedTagGroup ->
            addTags(
                encodedTagGroup.first,
                encodedTagGroup.second,
                tagEncryptionKey,
                pipe
            )
        }
    }

    private fun mapAnnotations(
        annotations: Annotations,
        tagEncryptionKey: GCKey,
        pipe: NetworkingContract.SearchTagsPipeIn
    ) {
        annotations.map { annotation ->
            Pair(
                ANNOTATION_KEY + TaggingContract.DELIMITER,
                compatibilityEncoder.encode(annotation).copy(
                    second = compatibilityEncoder.normalize(annotation)
                )
            )
        }.map { encodedTagGroup ->
            addTags(
                encodedTagGroup.first,
                encodedTagGroup.second,
                tagEncryptionKey,
                pipe
            )
        }
    }

    override fun resolveSearchTags(
        tags: Tags,
        annotation: Annotations
    ): NetworkingContract.SearchTagsPipeOut {
        val pipe = searchTagsPipeBuilderFactory.newPipe()
        val tagEncryptionKey = cryptoService.fetchTagEncryptionKey()

        mapTags(tags, tagEncryptionKey, pipe)
        mapAnnotations(annotation, tagEncryptionKey, pipe)


        return pipe.seal()
    }
}
