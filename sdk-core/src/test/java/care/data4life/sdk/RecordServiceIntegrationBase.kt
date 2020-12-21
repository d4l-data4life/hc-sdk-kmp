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

package care.data4life.sdk

import care.data4life.crypto.GCKey
import care.data4life.fhir.stu3.model.Attachment
import care.data4life.fhir.stu3.model.DocumentReference
import care.data4life.sdk.attachment.FileService
import care.data4life.sdk.fhir.Fhir3Attachment
import care.data4life.sdk.fhir.Fhir4Attachment
import care.data4life.sdk.model.Meta
import care.data4life.sdk.network.model.EncryptedKey
import care.data4life.sdk.network.model.EncryptedRecord
import care.data4life.sdk.record.RecordContract
import care.data4life.sdk.util.Base64
import care.data4life.sdk.util.HashUtil
import org.threeten.bp.LocalDate
import org.threeten.bp.LocalDateTime
import org.threeten.bp.format.DateTimeFormatter
import org.threeten.bp.format.DateTimeFormatterBuilder
import java.util.*

abstract class RecordServiceIntegrationBase {
    protected val commonKeyId = "commonKeyId"

    protected lateinit var dataKey: GCKey
    protected lateinit var attachmentKey: GCKey
    protected lateinit var tagEncryptionKey: GCKey
    protected lateinit var commonKey: GCKey
    protected lateinit var encryptedDataKey: EncryptedKey
    protected lateinit var encryptedAttachmentKey: EncryptedKey

    protected lateinit var recordService: RecordContract.Service
    protected lateinit var apiService: ApiService
    protected lateinit var cryptoService: CryptoService
    protected lateinit var fileService: FileService
    internal lateinit var imageResizer: ImageResizer
    internal lateinit var errorHandler: D4LErrorHandler
    protected lateinit var encryptedRecord: EncryptedRecord

    protected lateinit var encryptedBody: String
    protected lateinit var stringifiedResource: String
    
    
    companion object {
        @JvmStatic
        protected val CLIENT_ID = "TEST"
        @JvmStatic
        protected val USER_ID = "ME"
        @JvmStatic
        protected val ALIAS = "alias"
        @JvmStatic
        protected val RECORD_ID = "42"
        @JvmStatic
        protected val CREATION_DATE = "2020-12-12"
        @JvmStatic
        protected val UPDATE_DATE = "2020-12-13T17:21:08.234123"
        @JvmStatic
        protected val ATTACHMENT_PAYLOAD = "data"
        @JvmStatic
        protected val ATTACHMENT_ID = "attachmentId"
        @JvmStatic
        protected val THUMBNAIL_ID = "thumbnailId"
        @JvmStatic
        protected val PREVIEW_ID = "previewId"
        @JvmStatic
        protected val ASSIGNER = "assigner"
        @JvmStatic
        protected val IV = ByteArray(16)
        private const val DATE_FORMAT = "yyyy-MM-dd"
        private const val DATE_TIME_FORMAT = "yyyy-MM-dd'T'HH:mm:ss[.SSS]"
        protected val DATE_FORMATTER = DateTimeFormatter.ofPattern(DATE_FORMAT, Locale.US)
        @JvmStatic
        protected val DATE_TIME_FORMATTER = DateTimeFormatterBuilder()
                .parseLenient()
                .appendPattern(DATE_TIME_FORMAT)
                .toFormatter(Locale.US)

        private fun buildFhir3Attachment(id: String?): Fhir3Attachment {
            val attachment = Fhir3Attachment()
            attachment.id = id
            attachment.data = ATTACHMENT_PAYLOAD
            attachment.size = 42
            attachment.hash = Base64.encodeToString(HashUtil.sha1(Base64.decode(ATTACHMENT_PAYLOAD)))
            return attachment
        }

        @JvmStatic
        protected fun buildDocumentReferenceFhir3(id: String? = null): DocumentReference {
            val content = buildDocRefContentFhir3(buildFhir3Attachment(id))
            val contents: MutableList<DocumentReference.DocumentReferenceContent> = ArrayList()
            contents.add(content)
            return DocumentReference(
                    null,
                    null,
                    null,
                    contents
            )
        }

        private fun buildDocRefContentFhir3(attachment: Attachment): DocumentReference.DocumentReferenceContent {
            return DocumentReference.DocumentReferenceContent(attachment)
        }

        private fun buildFhir4Attachment(id: String?): Fhir4Attachment {
            val attachment = Fhir4Attachment()
            attachment.id = id
            attachment.data = ATTACHMENT_PAYLOAD
            attachment.size = 42
            attachment.hash = Base64.encodeToString(HashUtil.sha1(Base64.decode(ATTACHMENT_PAYLOAD)))
            return attachment
        }

        private fun buildDocRefContentFhir4(attachment: Fhir4Attachment): care.data4life.fhir.r4.model.DocumentReference.DocumentReferenceContent {
            return care.data4life.fhir.r4.model.DocumentReference.DocumentReferenceContent(attachment)
        }

        @JvmStatic
        protected fun buildDocumentReferenceFhir4(id: String? = null): care.data4life.fhir.r4.model.DocumentReference {
            val content = buildDocRefContentFhir4(buildFhir4Attachment(id))
            val contents: MutableList<care.data4life.fhir.r4.model.DocumentReference.DocumentReferenceContent> = ArrayList()
            contents.add(content)

            return care.data4life.fhir.r4.model.DocumentReference(
                    null,
                    contents
            )
        }

        @JvmStatic
        protected fun buildMeta(
                customCreationDate: String,
                updatedDate: String
        ): Meta = Meta(
                LocalDate.parse(customCreationDate, DATE_FORMATTER),
                LocalDateTime.parse(updatedDate, DATE_TIME_FORMATTER)
        )
    }
}
