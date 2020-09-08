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

package care.data4life.securestore.cryptor

import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import care.data4life.sdk.util.Base64
import care.data4life.sdk.util.toBytes
import care.data4life.sdk.util.toChars
import care.data4life.securestore.SecureStoreContract
import com.google.crypto.tink.Aead
import com.google.crypto.tink.aead.AeadConfig
import com.google.crypto.tink.aead.AeadFactory
import com.google.crypto.tink.aead.AeadKeyTemplates
import com.google.crypto.tink.integration.android.AndroidKeysetManager

@RequiresApi(Build.VERSION_CODES.M)
class TinkAndroidCryptor(private val context: Context) : SecureStoreContract.Cryptor {

    private val EMPTY_ASSOCIATED_DATA = ByteArray(0)

    private var cryptor: Aead?
        get() = if (field == null) {
            cryptor = initCryptor()
            field
        } else field

    init {
        AeadConfig.register()
        cryptor = initCryptor()
    }

    override fun encrypt(data: CharArray): CharArray {
        val encrypted = cryptor!!.encrypt(data.toBytes(), EMPTY_ASSOCIATED_DATA)

        return Base64.encode(encrypted).toChars()
    }

    override fun decrypt(data: CharArray): CharArray {
        val decoded = Base64.decode(data.toBytes())

        return cryptor!!.decrypt(decoded, EMPTY_ASSOCIATED_DATA).toChars()
    }

    override fun clear() {
        val appContext = context.applicationContext
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            appContext.deleteSharedPreferences(PREFERENCE_NAME)
        } else {
            appContext.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE).edit().clear().commit()
            cryptor = null
        }
    }

    private fun initCryptor() = AeadFactory.getPrimitive(initKeysetManager(context).keysetHandle)

    companion object {
        private const val DEFAULT_CHARSET = "UTF-8"

        private const val PREFERENCE_NAME = "care.data4life.securestore.storage.tink"
        private const val KEYSET_NAME = "securestore_keyset"
        private const val MASTER_KEY_URI = "android-keystore://securestore_master_key_id"

        fun initKeysetManager(context: Context): AndroidKeysetManager {
            return AndroidKeysetManager.Builder()
                    .withSharedPref(context, KEYSET_NAME, PREFERENCE_NAME)
                    .withKeyTemplate(AeadKeyTemplates.AES256_GCM)
                    .withMasterKeyUri(MASTER_KEY_URI)
                    .build()
        }
    }
}
