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

import care.data4life.sdk.ApiService
import care.data4life.sdk.CryptoService
import care.data4life.sdk.network.model.NetworkModelContract.EncryptedRecord
import care.data4life.sdk.tag.Annotations
import care.data4life.sdk.tag.TagEncryptionHelper
import care.data4life.sdk.tag.TaggingContract
import care.data4life.sdk.tag.TaggingContract.Companion.ANNOTATION_KEY
import care.data4life.sdk.tag.Tags
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.functions.BiFunction
import java.io.IOException

// see: https://gesundheitscloud.atlassian.net/browse/SDK-525
@Migration("This class should only be used due to migration purpose.")
class RecordCompatibilityService internal constructor(
        private val apiService: ApiService,
        private val tagEncryptionService: TaggingContract.EncryptionService,
        private val cryptoService: CryptoService,
        private val tagHelper: TaggingContract.Helper = TagEncryptionHelper
) : MigrationContract.CompatibilityService {
    private fun encrypt(
            plainTags: Tags,
            plainAnnotations: Annotations
    ): Pair<List<String>, List<String>> {
        return Pair(
                tagEncryptionService.encryptTagsAndAnnotations(plainTags, plainAnnotations),
                encryptTags(plainTags)
                        .also { encryptedTags ->
                            encryptedTags.addAll(
                                    encryptAnnotations(plainAnnotations)
                            )
                        }
        )
    }

    @Throws(IOException::class)
    private fun encryptTags(tags: Tags): MutableList<String> {
        val encryptionKey = cryptoService.fetchTagEncryptionKey()
        return tags
                .map { entry -> entry.key + TaggingContract.DELIMITER + tagHelper.normalize(entry.value) }
                .let { normalizedTags -> tagEncryptionService.encryptList(normalizedTags, encryptionKey) }
    }

    @Throws(IOException::class)
    private fun encryptAnnotations(
            annotations: Annotations
    ): MutableList<String> {
        val encryptionKey = cryptoService.fetchTagEncryptionKey()
        return tagEncryptionService.encryptList(
                annotations,
                encryptionKey,
                ANNOTATION_KEY + TaggingContract.DELIMITER
        )
    }

    private fun needsDoubleCall(
            encodedAndEncryptedTags: List<String>,
            encryptedTags: List<String>
    ): Boolean = encodedAndEncryptedTags.sorted() != encryptedTags.sorted()

    override fun searchRecords(
            alias: String,
            userId: String,
            startDate: String?,
            endDate: String?,
            pageSize: Int,
            offSet: Int,
            tags: Tags,
            annotations: Annotations
    ): Observable<List<EncryptedRecord>> {
        val (encodedAndEncryptedTags, encryptedTags) = encrypt(tags, annotations)
        return if (needsDoubleCall(encodedAndEncryptedTags, encryptedTags)) {
            Observable.zip(
                    apiService.fetchRecords(
                            alias,
                            userId,
                            startDate,
                            endDate,
                            pageSize,
                            offSet,
                            encodedAndEncryptedTags
                    ),
                    apiService.fetchRecords(
                            alias,
                            userId,
                            startDate,
                            endDate,
                            pageSize,
                            offSet,
                            encryptedTags
                    ),
                    BiFunction<List<EncryptedRecord>, List<EncryptedRecord>, List<EncryptedRecord>> { records1, records2 ->
                        mutableListOf<EncryptedRecord>().also {
                            it.addAll(records1)
                            it.addAll(records2)
                        }
                    }
            )
        } else {
            apiService.fetchRecords(
                    alias,
                    userId,
                    startDate,
                    endDate,
                    pageSize,
                    offSet,
                    encodedAndEncryptedTags
            ) as Observable<List<EncryptedRecord>>
        }
    }

    override fun countRecords(
            alias: String,
            userId: String,
            tags: Tags,
            annotations: Annotations
    ): Single<Int> {
        val (encodedAndEncryptedTags, encryptedTags) = encrypt(tags, annotations)

        return Single.zip(
                apiService.getCount(alias, userId, encodedAndEncryptedTags),
                apiService.getCount(alias, userId, encryptedTags),
                BiFunction<Int, Int, Int> { c1, c2 -> c1 + c2 }
        )
    }

}
