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

package care.data4life.sdk;

import care.data4life.crypto.GCKey;
import care.data4life.sdk.lang.FileException;
import io.reactivex.Single;

class FileService {

    private String alias;
    private ApiService apiService;
    private CryptoService cryptoService;

    FileService(String alias, ApiService apiService, CryptoService cryptoService) {
        this.alias = alias;
        this.apiService = apiService;
        this.cryptoService = cryptoService;
    }

    Single<byte[]> downloadFile(GCKey key, String userId, String fileId) {
        return apiService
                .downloadDocument(alias, userId, fileId)
                .flatMap(downloadedFile -> cryptoService.decrypt(key, downloadedFile))
                .onErrorResumeNext(error -> Single.error(new FileException.DownloadFailed(error)));
    }

    Single<String> uploadFile(GCKey key, String userId, byte[] data) {
        return cryptoService
                .encrypt(key, data)
                .flatMap(encryptedData -> apiService.uploadDocument(alias, userId, encryptedData))
                .onErrorResumeNext(error -> Single.error(new FileException.UploadFailed(error)));
    }

    Single<Boolean> deleteFile(String userId, String fileId) {
        return apiService.deleteDocument(alias, userId, fileId);
    }
}
