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
import care.data4life.sdk.crypto.CryptoContract
import care.data4life.sdk.lang.FileException
import care.data4life.sdk.network.NetworkingContract
import io.reactivex.Single

// TODO internal
class FileService(
    private val alias: String,
    private val apiService: NetworkingContract.Service,
    private val cryptoService: CryptoContract.Service
) : AttachmentContract.FileService {

    override fun downloadFile(key: GCKey, userId: String, fileId: String): Single<ByteArray> {
        return apiService
            .downloadDocument(alias, userId, fileId)
            .flatMap { downloadedFile -> cryptoService.decrypt(key, downloadedFile) }
            .onErrorResumeNext { error -> Single.error(FileException.DownloadFailed(error)) }
    }

    override fun uploadFile(key: GCKey, userId: String, data: ByteArray): Single<String> {
        return cryptoService
            .encrypt(key, data)
            .flatMap { encryptedData -> apiService.uploadDocument(alias, userId, encryptedData) }
            .onErrorResumeNext { error -> Single.error(FileException.UploadFailed(error)) }
    }

    override fun deleteFile(
        userId: String,
        fileId: String
    ): Single<Boolean> = apiService.deleteDocument(alias, userId, fileId)
}
