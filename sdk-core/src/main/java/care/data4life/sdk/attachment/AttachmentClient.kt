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

package care.data4life.sdk.attachment

import care.data4life.sdk.CryptoService
import care.data4life.sdk.lang.CoreRuntimeException
import care.data4life.sdk.lang.DataValidationException
import care.data4life.sdk.network.model.NetworkRecordContract
import care.data4life.sdk.wrapper.AttachmentFactory
import care.data4life.sdk.wrapper.FhirAttachmentHelper
import care.data4life.sdk.wrapper.HelperContract
import care.data4life.sdk.wrapper.WrapperContract
import care.data4life.sdk.wrapper.WrapperFactoryContract
import kotlin.collections.HashMap

class AttachmentClient(
        private val attachmentService: AttachmentContract.Service,
        private val cryptoService: CryptoService,
        private val thumbnailService: ThumbnailContract.Service
): AttachmentContract.Client {
    private val fhirAttachmentHelper: HelperContract.FhirAttachmentHelper = FhirAttachmentHelper
    private val attachmentFactory: WrapperFactoryContract.AttachmentFactory = AttachmentFactory

    private fun unpackAttachments(
            attachments: HashMap<WrapperContract.Attachment, String?>
    ): HashMap<Any, String?> {
        val rawAttachments = hashMapOf<Any, String?>()

        for(attachment in attachments.keys) {
            rawAttachments[attachment.unwrap()] = attachments[attachment]
        }

        return rawAttachments
    }

    private fun setUploadData(
            record: NetworkRecordContract.DecryptedRecord,
            attachmentData: HashMap<Any, String?>?
    ) {
        val rawResource = record.resource.unwrap()
        val attachments = fhirAttachmentHelper.getAttachment(rawResource)
        if (attachments.isNotEmpty()) {
            fhirAttachmentHelper.updateAttachmentData(
                    rawResource,
                    attachmentData
            )
        }
    }

    override fun removeUploadData(
            record: NetworkRecordContract.DecryptedRecord
    ): NetworkRecordContract.DecryptedRecord {
        return record.also {
            if (record.resource.type != WrapperContract.Resource.TYPE.DATA) {
                setUploadData(
                        record,
                        null
                )
            }
        }
    }

    override fun restoreUploadData(
            record: NetworkRecordContract.DecryptedRecord,
            originalResource: WrapperContract.Resource?,
            attachmentData: HashMap<WrapperContract.Attachment, String?>?
    ): NetworkRecordContract.DecryptedRecord {
        if (
                record.resource.type != WrapperContract.Resource.TYPE.DATA &&
                originalResource != null &&
                originalResource.type != WrapperContract.Resource.TYPE.DATA
        ) {
            record.resource = originalResource

            if (attachmentData != null) {
                @Suppress("UNCHECKED_CAST")
                setUploadData(
                        record,
                        unpackAttachments(attachmentData)
                )
            }
        }

        return record
    }

    @Throws(DataValidationException.IdUsageViolation::class,
            DataValidationException.ExpectedFieldViolation::class,
            DataValidationException.InvalidAttachmentPayloadHash::class)
    override fun uploadData(
            record: NetworkRecordContract.DecryptedRecord,
            userId: String
    ): NetworkRecordContract.DecryptedRecord {
        if (record.resource.type == WrapperContract.Resource.TYPE.DATA) {
            return record
        }

        val resource = record.resource.unwrap()

        if (!fhirAttachmentHelper.hasAttachment(resource)) return record
        val attachments = fhirAttachmentHelper.getAttachment(resource)

        if (record.attachmentsKey == null) {
            record.attachmentsKey = cryptoService.generateGCKey().blockingGet()
        }

        val validAttachments: MutableList<WrapperContract.Attachment> = arrayListOf()
        for (rawAttachment in attachments) {
            val attachment = attachmentFactory.wrap(rawAttachment)

            when {
                attachment == null -> {/* do nothing*/}
                attachment.id != null ->
                    throw DataValidationException.IdUsageViolation("Attachment.id should be null")

                attachment.hash == null || attachment.size == null ->
                    throw DataValidationException.ExpectedFieldViolation(
                            "Attachment.hash and Attachment.size expected"
                    )
                attachmentService.getValidHash(attachment) != attachment.hash ->
                    throw DataValidationException.InvalidAttachmentPayloadHash(
                            "Attachment.hash is not valid"
                    )
                else -> validAttachments.add(attachment)
            }
        }

        if (validAttachments.isNotEmpty()) {
            thumbnailService.updateResourceIdentifier(
                    record.resource,
                    attachmentService.upload(
                            validAttachments,
                            // FIXME this is forced
                            record.attachmentsKey!!,
                            userId
                    ).blockingGet()
            )
        }
        return record
    }

    @Throws(DataValidationException.IdUsageViolation::class,
            DataValidationException.ExpectedFieldViolation::class,
            DataValidationException.InvalidAttachmentPayloadHash::class,
            CoreRuntimeException.UnsupportedOperation::class)
    override fun updateData(
            record: NetworkRecordContract.DecryptedRecord,
            newResource: WrapperContract.Resource?,
            userId: String?
    ): NetworkRecordContract.DecryptedRecord {
         if (record.resource.type == WrapperContract.Resource.TYPE.DATA) {
            return record
        }

        if ( newResource == null || newResource.type == WrapperContract.Resource.TYPE.DATA) {
            throw CoreRuntimeException.UnsupportedOperation()
        }

        var resource = record.resource.unwrap()
        if (!fhirAttachmentHelper.hasAttachment(resource)) return record
        val attachments = fhirAttachmentHelper.getAttachment(resource)

        val validAttachments: MutableList<WrapperContract.Attachment> = arrayListOf()
        val oldAttachments: HashMap<String?, WrapperContract.Attachment> = hashMapOf()

        for (rawAttachment in attachments) {
            val attachment = attachmentFactory.wrap(rawAttachment)

            if (attachment?.id != null) {
                oldAttachments[attachment.id] = attachment
            }
        }

        resource = newResource.unwrap()
        val newAttachments = fhirAttachmentHelper.getAttachment(resource)
        for (rawNewAttachment in newAttachments) {
            val newAttachment = attachmentFactory.wrap(rawNewAttachment)

            when {
                newAttachment == null -> {/* do nothing*/}
                newAttachment.hash == null || newAttachment.size == null ->
                    throw DataValidationException.ExpectedFieldViolation(
                            "Attachment.hash and Attachment.size expected"
                    )
                attachmentService.getValidHash(newAttachment) != newAttachment.hash ->
                    throw DataValidationException.InvalidAttachmentPayloadHash(
                            "Attachment.hash is not valid"
                    )
                newAttachment.id == null -> validAttachments.add(newAttachment)
                else -> {
                    val oldAttachment = oldAttachments[newAttachment.id]
                            ?: throw DataValidationException.IdUsageViolation(
                                    "Valid Attachment.id expected"
                            )
                    if (oldAttachment.hash == null || newAttachment.hash != oldAttachment.hash) {
                        validAttachments.add(newAttachment)
                    }
                }
            }
        }
        if (validAttachments.isNotEmpty()) {
            thumbnailService.updateResourceIdentifier(
                    newResource,
                    attachmentService.upload(
                            validAttachments,
                            // FIXME this is forced
                            record.attachmentsKey!!,
                            // FIXME this is forced
                            userId!!
                    ).blockingGet()
            )
        }
        return record
    }

    @Throws(DataValidationException.IdUsageViolation::class, DataValidationException.InvalidAttachmentPayloadHash::class)
    override fun downloadData(
            record: NetworkRecordContract.DecryptedRecord,
            userId: String?
    ): NetworkRecordContract.DecryptedRecord {
        if (record.resource.type == WrapperContract.Resource.TYPE.DATA) {
            return record
        }

        val resource = record.resource.unwrap()

        if (!fhirAttachmentHelper.hasAttachment(resource)) return record

        val attachments = fhirAttachmentHelper.getAttachment(resource)
        val wrappedAttachments = mutableListOf<WrapperContract.Attachment>()

        for(rawAttachment in attachments) {
            val attachment = attachmentFactory.wrap(rawAttachment)
            attachment?.id ?: throw DataValidationException.IdUsageViolation("Attachment.id expected")

            wrappedAttachments.add(attachment)
        }

        @Suppress("CheckResult")
        attachmentService.download(
                wrappedAttachments,
                // FIXME this is forced
                record.attachmentsKey!!,
                // FIXME this is forced
                userId!!
        ).blockingGet()

        return record
    }
}
