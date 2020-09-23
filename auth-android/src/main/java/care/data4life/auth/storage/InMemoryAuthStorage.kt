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
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class InMemoryAuthStorage : AuthorizationContract.Storage {

    private var store = mutableMapOf<String, String>()
    private val lock = ReentrantLock()

    override fun clear() {
        lock.withLock {
            store.clear()
        }
    }

    override fun readAuthState(alias: String): String? {
        lock.withLock {
            return store[alias]
        }
    }

    override fun writeAuthState(alias: String, authState: String) {
        lock.withLock {
            store[alias] = authState
        }
    }

    override fun containsAuthState(alias: String): Boolean {
        lock.withLock {
            return store.containsKey(alias)
        }
    }

    override fun removeAuthState(alias: String) {
        lock.withLock {
            store.remove(alias)
        }
    }
}
