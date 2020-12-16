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

import care.data4life.sdk.attachment.AttachmentContract
import care.data4life.sdk.attachment.ThumbnailContract
import care.data4life.sdk.record.RecordEncryptionContract
import care.data4life.sdk.tag.TagEncryptionService
import care.data4life.sdk.tag.TaggingService
import io.mockk.mockk
import io.mockk.spyk

abstract class RecordTestBase {
    protected lateinit var apiService: ApiService
    protected lateinit var tagEncryptionService: TagEncryptionService
    protected lateinit var taggingService: TaggingService
    protected lateinit var attachmentService: AttachmentContract.Service
    protected lateinit var attachmentClient: AttachmentContract.Client
    protected lateinit var thumbnailService: ThumbnailContract.Service
    protected lateinit var cryptoService: CryptoService
    protected lateinit var recordCryptoService: RecordEncryptionContract.Service
    protected lateinit var errorHandler: SdkContract.ErrorHandler

    protected lateinit var recordService: RecordService

    fun init() {
        apiService = mockk()
        tagEncryptionService = mockk()
        taggingService = mockk()
        attachmentService = mockk()
        attachmentClient = mockk()
        thumbnailService = mockk()
        cryptoService = mockk()
        recordCryptoService = mockk()
        errorHandler = mockk()

        recordService = RecordService(
                ALIAS,
                apiService,
                tagEncryptionService,
                taggingService,
                attachmentService,
                attachmentClient,
                thumbnailService,
                cryptoService,
                recordCryptoService,
                errorHandler
        )

        recordService = spyk(recordService)
    }

    companion object {
        internal const val ALIAS = "alias"
    }
}
