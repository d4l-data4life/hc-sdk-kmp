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

import care.data4life.sdk.lang.D4LException
import care.data4life.sdk.listener.Callback
import care.data4life.sdk.listener.ResultListener

interface SdkContract {

    /**
     * Legacy Client interface
     *
     * Deprecated with version v1.9.0
     * <p>
     * Will be removed in version v2.0.0
     */
    @Deprecated(message = "Deprecated with version v1.9.0 and will be removed in version v2.0.0", level = DeprecationLevel.WARNING)
    interface LegacyClient : AuthClient, SdkContractLegacy.Client


    interface AuthClient {
        /**
         * Get the currently active User session token if present.
         *
         * @param listener result contains either User session token or Error
         */
        fun getUserSessionToken(listener: ResultListener<String>)

        /**
         * Checks if user is logged in.
         *
         * @param listener resulting Boolean indicates if the user is logged in or not or Error
         */
        fun isUserLoggedIn(listener: ResultListener<Boolean>)

        /**
         * Logout the user
         *
         * @param listener either [Callback.onSuccess] is called or [Callback.onError]
         */
        fun logout(listener: Callback)
    }

    interface ErrorHandler {
        fun handleError(error: Throwable?): D4LException?
    }
}
