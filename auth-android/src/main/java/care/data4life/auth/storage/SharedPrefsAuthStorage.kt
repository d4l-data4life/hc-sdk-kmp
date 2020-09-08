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

package care.data4life.auth.storage

import care.data4life.auth.AuthorizationContract
import care.data4life.securestore.SecureStoreContract

class SharedPrefsAuthStorage(private val store: SecureStoreContract.SecureStore) : AuthorizationContract.Storage {


    override fun readAuthState(alias: String): String? {
        return store.getData(key(alias))?.let { String(it) }
    }

    override fun writeAuthState(alias: String, authState: String) {
        store.addData(key(alias), authState.toCharArray())
    }

    override fun containsAuthState(alias: String): Boolean {
        return store.containsData(key(alias))
    }

    override fun removeAuthState(alias: String) {
        store.removeData(key(alias))
    }

    override fun clear() {
        store.clear()
    }

    private fun key(alias: String): String {
        return DATA_PREFIX + alias
    }


    companion object {
        private const val DATA_PREFIX = "store.auth.state."
    }
}
