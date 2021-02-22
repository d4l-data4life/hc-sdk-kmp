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

import care.data4life.sdk.call.CallHandler
import care.data4life.sdk.call.Task
import care.data4life.sdk.listener.Callback
import care.data4life.sdk.listener.ResultListener

class AuthClient(
        private val alias: String,
        private val userService: UserService,
        private val handler: CallHandler
) : AuthContract.Client {

    override fun getUserSessionToken(listener: ResultListener<String>): Task {
        val operation = userService.getSessionToken(alias)
        return handler.executeSingle(operation, listener)
    }

    override fun isUserLoggedIn(listener: ResultListener<Boolean>): Task {
        val operation = userService.isLoggedIn(alias)
        return handler.executeSingle(operation, listener)
    }

    override fun logout(listener: Callback): Task {
        return handler.executeCompletable(userService.logout(), listener)
    }
}
