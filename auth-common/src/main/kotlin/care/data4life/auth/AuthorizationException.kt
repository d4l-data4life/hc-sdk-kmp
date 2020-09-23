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

package care.data4life.auth

import care.data4life.sdk.lang.D4LException

/**
 * Exceptions that are thrown during the authorization process and on any given operation where an
 * network request is made
 */
sealed class AuthorizationException(message: String? = null, cause: Throwable? = null)
    : D4LException(message, cause) {

    constructor(message: String?) : this(message, null)

    constructor(cause: Throwable?) : this(cause?.toString(), cause)

    class Unknown(cause: Throwable?) : AuthorizationException(cause)
    class Canceled : AuthorizationException("User canceled authorization request")
    class FailedToRestoreAccessToken : AuthorizationException("Failed to restore access token")
    class FailedToRestoreRefreshToken : AuthorizationException("Failed to restore refresh token")
    class FailedToRefreshAccessToken : AuthorizationException("Failed to refresh access token")
    class FailedToRestoreTokenState : AuthorizationException("Failed to load token state")
    class FailedToRestoreAuthState : AuthorizationException("Failed to load auth state")
    class FailedToLogin : AuthorizationException("Failed to authorize user")

}
