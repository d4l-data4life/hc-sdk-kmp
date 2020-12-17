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

import care.data4life.crypto.GCKey
import care.data4life.fhir.stu3.util.FhirDateTimeParser
import care.data4life.sdk.ImageResizer
import care.data4life.sdk.attachment.ThumbnailService.Companion.SPLIT_CHAR
import care.data4life.sdk.fhir.Fhir3Attachment
import care.data4life.sdk.lang.DataValidationException
import care.data4life.sdk.lang.ImageResizeException
import care.data4life.sdk.log.Log
import care.data4life.sdk.util.Base64.decode
import care.data4life.sdk.util.Base64.encodeToString
import care.data4life.sdk.util.HashUtil.sha1
import care.data4life.sdk.wrappers.SdkAttachmentFactory
import care.data4life.sdk.wrappers.definitions.Attachment
import care.data4life.sdk.wrappers.definitions.AttachmentFactory
import io.reactivex.Observable
import io.reactivex.Single
import java.util.*

// TODO add internal
class AttachmentService internal constructor(
        private val fileService: FileService,
        // TODO move imageResizer to thumbnail service
        private val imageResizer: ImageResizer
) : AttachmentContract.Service {
    private val attachmentFactory: AttachmentFactory = SdkAttachmentFactory


    override fun upload(
            attachments: List<Fhir3Attachment>,
            attachmentsKey: GCKey,
            userId: String
    ): Single<List<Pair<Fhir3Attachment, List<String>>>> {
        return Observable.fromIterable(attachments)
                .filter { attachment -> attachment.data != null }
                .map { attachment ->
                    val originalData = decode(attachment.data!!)
                    attachment.id = fileService.uploadFile(attachmentsKey, userId, originalData).blockingGet()
                    var additionalIds = uploadDownscaledImages(attachmentsKey, userId, attachment, originalData)
                    Pair(attachment, additionalIds)
                }
                .filter { (first) -> first.id != null }
                .toList()
    }

    @Throws(DataValidationException.InvalidAttachmentPayloadHash::class)
    override fun download(
            attachments: List<Fhir3Attachment>,
            attachmentsKey: GCKey,
            userId: String
    ): Single<List<Fhir3Attachment>> {
        return Observable
                .fromCallable { attachments }
                .flatMapIterable { attachmentList -> attachmentList }
                .map { attachment ->
                    var attachmentId = attachment.id
                    var isPreview = false
                    if (attachment.id!!.contains(SPLIT_CHAR)) {
                        attachmentId = attachment.id!!.split(SPLIT_CHAR).toTypedArray()[DOWNSCALED_ATTACHMENT_ID_POS]
                        isPreview = true
                    }
                    val data = fileService.downloadFile(attachmentsKey, userId, attachmentId!!).blockingGet()
                    val validationDate = FhirDateTimeParser.parseDateTime(HASH_VALIDATION_DATE)
                    if (!isPreview && attachment.creation!!.date.toDate().after(validationDate.date.toDate()) && attachment.hash != encodeToString(sha1(data))) {
                        throw DataValidationException.InvalidAttachmentPayloadHash(
                                "Attachment.hash is not valid")
                    } else {
                        attachment.hash = encodeToString(sha1(data))
                    }
                    attachment.data = encodeToString(data)
                    attachment
                }
                .toList()
    }

    override fun delete(attachmentId: String, userId: String): Single<Boolean> {
        return fileService.deleteFile(userId, attachmentId)
    }


    // TODO -> thumbnail service
    private fun uploadDownscaledImages(
            attachmentsKey: GCKey,
            userId: String,
            attachmentTmp: Any,
            originalData: ByteArray
    ): List<String> {
        val attachment = if( attachmentTmp !is Attachment) {
            attachmentFactory.wrap(attachmentTmp)
        } else {
            attachmentTmp
        }

        var additionalIds = mutableListOf<String>()
        if (imageResizer.isResizable(originalData)) {
            additionalIds = ArrayList()
            var downscaledId: String?
            for (position in 0..1) {
                downscaledId = resizeAndUpload(
                        attachmentsKey,
                        userId,
                        attachment,
                        originalData,
                        if (position == POSITION_PREVIEW) ImageResizer.DEFAULT_PREVIEW_SIZE_PX else ImageResizer.DEFAULT_THUMBNAIL_SIZE_PX
                )
                if (downscaledId != null)
                    additionalIds.add(downscaledId)
            }
        }
        return additionalIds
    }

    fun _upload(
            attachments: List<Attachment>,
            attachmentsKey: GCKey,
            userId: String
    ): Single<List<Pair<Attachment, List<String>>>> {
        return Observable.fromIterable(attachments)
                .filter { it.data != null }
                .map { attachment ->
                    val originalData = decode(attachment.data!!)
                    attachment.id = fileService.uploadFile(attachmentsKey, userId, originalData).blockingGet()
                    val additionalIds = uploadDownscaledImages(
                            attachmentsKey,
                            userId,
                            attachment,
                            originalData
                    )
                    Pair(attachment, additionalIds)
                }
                .filter { (first) -> first.id != null } //FIXME: Should this be removed?
                .toList()
    }

    @Throws(DataValidationException.InvalidAttachmentPayloadHash::class)
    fun _download(
            attachments: List<Attachment>,
            attachmentsKey: GCKey,
            userId: String
    ): Single<List<Attachment>> {
        return Observable
                .fromCallable { attachments }
                .flatMapIterable { it }
                .filter{ it.id != null }
                .map { attachment ->
                    var attachmentId = attachment.id!!
                    var isPreview = false
                    if (attachmentId.contains(SPLIT_CHAR)) {
                        attachmentId = attachmentId.split(SPLIT_CHAR)[DOWNSCALED_ATTACHMENT_ID_POS]
                        isPreview = true
                    }

                    val data = fileService.downloadFile(attachmentsKey, userId, attachmentId).blockingGet()
                    val newHash = encodeToString(sha1(data))

                    if (!isPreview &&
                            LegacyDateValidator.isInvalidateDate(attachment) &&
                            attachment.hash != newHash
                    ) {
                        throw DataValidationException.InvalidAttachmentPayloadHash()
                    } else {
                        attachment.also {
                            it.data = encodeToString(data)
                            it.hash = newHash
                        }
                    }
                }
                .toList()
    }

    // TODO -> thumbnail service
    private fun resizeAndUpload(
            attachmentsKey: GCKey,
            userId: String,
            attachmentTmp: Any,
            originalData: ByteArray,
            targetHeight: Int
    ): String? {
        val attachment = if( attachmentTmp !is Attachment) {
            attachmentFactory.wrap(attachmentTmp)
        } else {
            attachmentTmp
        }

        val downscaledImage = try {
            imageResizer.resizeToHeight(originalData, targetHeight, ImageResizer.DEFAULT_JPEG_QUALITY_PERCENT)
        } catch (exception: ImageResizeException.JpegWriterMissing) {
            Log.error(exception, exception.message)
            return null
        }
        return if (downscaledImage == null) { //currentSizePx <= targetSizePx
            attachment.id //nothing to upload
        } else fileService.uploadFile(attachmentsKey, userId, downscaledImage).blockingGet()
    }

    companion object {
        private const val DOWNSCALED_ATTACHMENT_ID_POS = 1
        private const val POSITION_PREVIEW = 0
        private const val HASH_VALIDATION_DATE = "2019-09-15"
    }
}
