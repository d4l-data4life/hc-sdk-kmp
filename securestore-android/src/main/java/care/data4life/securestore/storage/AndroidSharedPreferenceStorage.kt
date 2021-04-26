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

package care.data4life.securestore.storage

import android.content.Context
import android.content.SharedPreferences
import care.data4life.securestore.SecureStoreContract
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class AndroidSharedPreferenceStorage(
    context: Context,
    name: String
) : SecureStoreContract.Storage {

    private val preferences: SharedPreferences =
        context.getSharedPreferences(name, Context.MODE_PRIVATE)
    private val lock: ReentrantLock = ReentrantLock()

    override fun addData(alias: String, data: CharArray) {
        lock.withLock {
            preferences
                .edit()
                .putString(alias, String(data)) // TODO save without using string?
                .apply()
        }
    }

    override fun removeData(alias: String) {
        lock.withLock {
            preferences
                .edit()
                .remove(alias)
                .apply()
        }
    }

    override fun containsData(alias: String): Boolean {
        lock.withLock {
            return preferences.contains(alias)
        }
    }

    override fun getData(alias: String): CharArray? {
        lock.withLock {
            return preferences
                .getString(alias, null)?.toCharArray()
        }
    }

    override fun clear() {
        lock.withLock {
            preferences
                .edit()
                .clear()
                .apply()
        }
    }
}
