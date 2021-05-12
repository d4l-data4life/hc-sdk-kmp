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

interface AuthorizationContract {

    interface Service {

        @Throws(AuthorizationException.FailedToRestoreAccessToken::class)
        fun getAccessToken(alias: String): String

        @Throws(AuthorizationException.FailedToRestoreRefreshToken::class)
        fun getRefreshToken(alias: String): String

        @Throws(AuthorizationException.FailedToRefreshAccessToken::class)
        fun refreshAccessToken(alias: String): String

        fun isAuthorized(alias: String): Boolean

        fun clear()
    }

    interface Storage {

        /**
         * Returns auth state or null.
         *
         * @param alias of auth state
         * @return auth state or null
         */
        fun readAuthState(alias: String): String?

        /**
         * Writes given auth state to storage.
         *
         * @param alias of auth state
         * @param authState   to be inserted
         */
        fun writeAuthState(alias: String, authState: String)

        /**
         * Return <tt>true</tt> if this store contains the auth state with specified alias
         *
         * @param alias of auth state
         * @return <tt>true</tt> if store contains auth state
         */
        fun containsAuthState(alias: String): Boolean

        /**
         * Removes the auth state for the given alias
         *
         * @param alias of auth state
         */
        fun removeAuthState(alias: String)

        /**
         * Removes all authState from Storage
         */
        fun clear()
    }

    fun authorize()

    fun clear()
}
