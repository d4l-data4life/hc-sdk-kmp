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

package care.data4life.sdk.auth

import care.data4life.sdk.SdkContract
import io.reactivex.Completable
import io.reactivex.Single

interface AuthContract {

    interface Client : SdkContract.AuthClient

    interface UserService {
        val userID: Single<String>

        fun finishLogin(isAuthorized: Boolean): Single<Boolean>
        fun isLoggedIn(alias: String): Single<Boolean>
        fun logout(): Completable
        fun refreshSessionToken(alias: String): Single<String>
    }
}
