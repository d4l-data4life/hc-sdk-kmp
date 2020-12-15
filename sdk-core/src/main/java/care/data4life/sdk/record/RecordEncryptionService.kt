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

package care.data4life.sdk.record

import care.data4life.crypto.GCKey
import care.data4life.crypto.KeyType
import care.data4life.sdk.ApiService
import care.data4life.sdk.CryptoService
import care.data4life.sdk.data.DataResource
import care.data4life.sdk.fhir.FhirContract
import care.data4life.sdk.lang.DataValidationException
import care.data4life.sdk.model.ModelVersion
import care.data4life.sdk.network.DecryptedRecordBuilder
import care.data4life.sdk.network.model.EncryptedKey
import care.data4life.sdk.network.model.EncryptedRecord
import care.data4life.sdk.network.model.NetworkRecordContract
import care.data4life.sdk.tag.TagEncryptionService
import care.data4life.sdk.tag.TaggingService
import care.data4life.sdk.util.Base64
import care.data4life.sdk.wrapper.WrapperContract
import care.data4life.sdk.wrapper.WrapperFactoryContract

internal class RecordEncryptionService(
        private val alias: String,
        private val tagEncryptionService: TagEncryptionService,
        private val cryptoService: CryptoService,
        private val fhirService: FhirContract.Service,
        private val apiService: ApiService,
        private val resourceWrapperFactory: WrapperFactoryContract.ResourceFactory
): RecordEncryptionContract.Service {
    private fun getEncryptedResourceAndAttachment(
            record: NetworkRecordContract.DecryptedRecord<WrapperContract.Resource>,
            commonKey: GCKey
    ): Pair<String, EncryptedKey?> {
        return if (record.resource.type == WrapperContract.Resource.TYPE.DATA) {
            Pair(
                    Base64.encodeToString(
                            cryptoService.encrypt(
                                    record.dataKey!!,
                                    (record.resource.unwrap() as DataResource).asByteArray()
                            ).blockingGet()
                    ),
                    null
            )
        } else {
            Pair(
                    fhirService.encryptResource(record.dataKey!!, record.resource),
                    if (record.attachmentsKey == null) {
                        null
                    } else {
                        cryptoService.encryptSymmetricKey(
                                commonKey,
                                KeyType.ATTACHMENT_KEY,
                                record.attachmentsKey!!
                        ).blockingGet()
                    }
            )
        }
    }

    override fun encryptRecord(
            record: NetworkRecordContract.DecryptedRecord<WrapperContract.Resource>
    ): EncryptedRecord {
        val encryptedTags = tagEncryptionService.encryptTags(record.tags!!).also {
            (it as MutableList<String>).addAll(tagEncryptionService.encryptAnnotations(record.annotations))
        }

        val commonKey = cryptoService.fetchCurrentCommonKey()
        val currentCommonKeyId = cryptoService.currentCommonKeyId
        val encryptedDataKey = cryptoService.encryptSymmetricKey(
                commonKey,
                KeyType.DATA_KEY,
                record.dataKey!!
        ).blockingGet()

        val (encryptedResource, encryptedAttachmentsKey) = getEncryptedResourceAndAttachment(
                record,
                commonKey
        )

        return EncryptedRecord(
                currentCommonKeyId,
                record.identifier,
                encryptedTags,
                encryptedResource,
                record.customCreationDate,
                encryptedDataKey,
                encryptedAttachmentsKey,
                record.modelVersion
        )
    }

    private fun getCommonKey(commonKeyId: String, userId: String): GCKey {
        val commonKeyStored = cryptoService.hasCommonKey(commonKeyId)
        return if (commonKeyStored) {
            cryptoService.getCommonKeyById(commonKeyId)
        } else {
            val commonKeyResponse = apiService.fetchCommonKey(
                    alias,
                    userId,
                    commonKeyId
            ).blockingGet()

            cryptoService.asymDecryptSymetricKey(
                    cryptoService.fetchGCKeyPair().blockingGet(),
                    commonKeyResponse.commonKey
            ).blockingGet().also {
                cryptoService.storeCommonKey(commonKeyId, it)
            }
        }
    }

    override fun decryptRecord(
            record: EncryptedRecord,
            userId: String
    ): NetworkRecordContract.DecryptedRecord<WrapperContract.Resource> {
        if (!ModelVersion.isModelVersionSupported(record.modelVersion)) {
            throw DataValidationException.ModelVersionNotSupported("Please update SDK to latest version!")
        }

        val decryptedTags = tagEncryptionService.decryptTags(record.encryptedTags)
        val builder = DecryptedRecordBuilder()
                .setIdentifier(record.identifier)
                .setTags(
                        decryptedTags
                )
                .setAnnotations(
                        tagEncryptionService.decryptAnnotations(record.encryptedTags)
                )
                .setCreationDate(record.customCreationDate)
                .setUpdateDate(record.updatedDate)
                .setModelVersion(record.modelVersion)

        val commonKeyId = record.commonKeyId
        val commonKey: GCKey = getCommonKey(commonKeyId, userId)
        val decryptedDataKey = cryptoService.symDecryptSymmetricKey(commonKey, record.encryptedDataKey).blockingGet()
        builder.setDataKey(
                decryptedDataKey
        )

        if (record.encryptedAttachmentsKey != null) {
            builder.setAttachmentKey(
                    cryptoService.symDecryptSymmetricKey(
                            commonKey,
                            record.encryptedAttachmentsKey
                    ).blockingGet()
            )
        }

        val resource: Any? = when {
            record.encryptedBody == null || record.encryptedBody.isEmpty() -> null
            decryptedTags.containsKey(TaggingService.TAG_RESOURCE_TYPE) -> fhirService.decryptResource(
                    decryptedDataKey,
                    decryptedTags,
                    record.encryptedBody
            )
            else -> DataResource(
                    cryptoService.decrypt(
                            decryptedDataKey,
                            Base64.decode(record.encryptedBody)
                    ).blockingGet()
            ).let { resourceWrapperFactory.wrap(it) }
        }

        // FIXME: Do we need a null Type
        @Suppress("UNCHECKED_CAST")
        return builder.build(resource) as NetworkRecordContract.DecryptedRecord<WrapperContract.Resource>
    }

}
