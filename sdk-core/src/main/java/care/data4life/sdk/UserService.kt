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

import care.data4life.sdk.log.Log
import care.data4life.sdk.network.model.UserInfo
import io.reactivex.Completable
import io.reactivex.Single

internal class UserService(private val alias: String,
                           private val oAuthService: OAuthService,
                           private val apiService: ApiService,
                           private val secureStore: CryptoSecureStore,
                           private val cryptoService: CryptoService) {
    val uID: Single<String>
        get() = Single.fromCallable { secureStore.getSecret("${alias}_user_id", String::class.java) }

    fun finishLogin(isAuthorized: Boolean): Single<Boolean> {
        return Single.just(isAuthorized)
                .flatMap { apiService.fetchUserInfo(alias) }
                .map { userInfo: UserInfo ->
                    secureStore.storeSecret("${alias}_user_id", userInfo.uid)
                    val gcKeyPair = cryptoService
                            .fetchGCKeyPair()
                            .blockingGet()
                    val commonKey = cryptoService
                            .asymDecryptSymetricKey(gcKeyPair, userInfo.commonKey)
                            .blockingGet()
                    val commonKeyId = userInfo.commonKeyId
                    cryptoService.storeCommonKey(commonKeyId, commonKey)
                    cryptoService.storeCurrentCommonKeyId(commonKeyId)
                    val tek = cryptoService
                            .symDecryptSymmetricKey(commonKey, userInfo.tagEncryptionKey)
                            .blockingGet()
                    cryptoService.storeTagEncryptionKey(tek)
                    true
                }
                .doOnError { Log.error(it, "Failed to finish login") }
    }

    fun isLoggedIn(alias: String): Single<Boolean> {
        return Single.fromCallable { oAuthService.isAuthorized(alias) }
                .onErrorReturnItem(false)
    }

    fun logout(): Completable {
        return apiService
                .logout(alias)
                .doOnError { throwable: Throwable -> Log.error(throwable, "Failed to logout") }
                .doOnComplete { secureStore.clear() }
    }

    fun getSessionToken(alias: String): Single<String> {
        return Single.fromCallable { oAuthService.refreshAccessToken(alias) }
    }

}
