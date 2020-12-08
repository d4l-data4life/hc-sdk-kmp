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

package care.data4life.sdk.sample

import care.data4life.auth.Authorization
import care.data4life.auth.AuthorizationContract
import care.data4life.auth.storage.InMemoryAuthStorage
import care.data4life.sdk.Data4LifeClient
import care.data4life.sdk.log.Logger
import care.data4life.sdk.network.Environment
import care.data4life.sdk.sample.util.ClientConfigLoader
import care.data4life.securestore.SecureStore
import care.data4life.securestore.SecureStoreContract
import care.data4life.securestore.SecureStoreCryptor
import care.data4life.securestore.SecureStoreStorage
import org.koin.dsl.module
import java.util.concurrent.ConcurrentHashMap

val storage: AuthorizationContract.Storage = InMemoryAuthStorage()
val secureStorage: SecureStoreContract.SecureStore = SecureStore(SecureStoreCryptor(), SecureStoreStorage())

val map = ConcurrentHashMap<String, Data4LifeClient>(2)

val appModule = module {
    Data4LifeClient.setLogger(object : Logger {
        override fun info(message: String) {
            println("[INFO] $message")
        }

        override fun debug(message: String) {
            println("[DEBUG] $message")
        }

        override fun error(t: Throwable, message: String?) {
            println("[ERROR] $message")
            t.printStackTrace()
        }
    })

    factory {
        val alias: String = it[0]
        if (!map.contains(alias)) map[alias] = createSDK(alias)
        map[alias]!!
    }
}


private fun createSDK(alias: String): Data4LifeClient {
    val config = ClientConfigLoader.load()

    return Data4LifeClient.init(alias,
            config[care.data4life.sdk.sample.util.Environment.DEVELOPMENT].id,
            config[care.data4life.sdk.sample.util.Environment.DEVELOPMENT].secret,
            Environment.DEVELOPMENT,
            config[care.data4life.sdk.sample.util.Environment.DEVELOPMENT].redirectScheme,
            config.platform,
            Authorization.defaultScopes,
            secureStorage,
            storage
    )
}


