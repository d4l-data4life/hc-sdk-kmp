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

package care.data4life.securestore

interface SecureStoreContract {

    interface SecureStore {

        /**
         * Inserts given data under provided alias
         *
         * @param alias under which to insert data
         * @param data to be inserted
         */
        fun addData(alias: String, data: CharArray)

        /**
         * Removes data with associated alias if present.
         *
         * @param alias of data to be removed
         */
        fun removeData(alias: String)

        /**
         * Return <tt>true</tt> if this store contains the data with specified alias
         *
         * @param alias of data
         * @return <tt>true</tt> if store contains data
         */
        fun containsData(alias: String): Boolean

        /**
         * Returns data for given alias or null.
         *
         * @param alias of data to return
         * @return data for given alias or null
         */
        fun getData(alias: String): CharArray?

        /**
         * Removes all data from SecureStore
         */
        fun clear()
    }

    interface Cryptor {

        /**
         * Encrypt the data with given key
         *
         * @param data to encrypt
         * @return encrypted data
         */
        fun encrypt(data: CharArray): CharArray

        /**
         * Decrypt the data with given key
         *
         * @param data to decrypt
         * @return decrypted data
         */
        fun decrypt(data: CharArray): CharArray

        /**
         * Clears cryptor state
         */
        fun clear()
    }


    interface Storage {

        /**
         * Inserts given data under provided alias.
         *
         * @param alias under which to insert data
         * @param data   to be inserted
         */
        fun addData(alias: String, data: CharArray)

        /**
         * Removes data with associated alias if present.
         *
         * @param alias of data to be removed
         */
        fun removeData(alias: String)

        /**
         * Return <tt>true</tt> if this store contains the data with specified alias
         *
         * @param alias of data
         * @return <tt>true</tt> if store contains data
         */
        fun containsData(alias: String): Boolean

        /**
         * Returns data for given alias or null.
         *
         * @param alias of data to return
         * @return data for given alias or null
         */
        fun getData(alias: String): CharArray?

        /**
         * Removes all data from Storage
         */
        fun clear()
    }
}
