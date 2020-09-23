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
import care.data4life.securestore.keystore.AndroidCompatKeystore
import care.data4life.securestore.keystore.CompatKeystore
import care.data4life.securestore.security.SymmetricKey
import java.nio.ByteBuffer
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec

@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
class AndroidCompatCryptor(
        val context: Context,
        private val keystore: CompatKeystore = AndroidCompatKeystore(context),
        private val base64: Base64 = Base64
) : SecureStoreContract.Cryptor {


    override fun encrypt(data: CharArray): CharArray {
        val symmetricKey = keystore.loadKey()

        val iv = ByteArray(SymmetricKey.KEY_IV_SIZE)
        SecureRandom().nextBytes(iv)

        val cipher = Cipher.getInstance(SymmetricKey.KEY_TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, symmetricKey.getKey(), GCMParameterSpec(SymmetricKey.KEY_IV_TAG_LENGTH, iv))

        val encrypted = cipher.doFinal(data.toBytes())

        return base64.encode(ByteBuffer.allocate(iv.size + encrypted.size)
                .put(iv)
                .put(encrypted)
                .array()).toChars()
    }

    override fun decrypt(data: CharArray): CharArray {
        val symmetricKey = keystore.loadKey()

        val decodedData = base64.decode(data.toBytes())

        val buffer = ByteBuffer.wrap(decodedData)
        val iv = ByteArray(SymmetricKey.KEY_IV_SIZE)
        buffer.get(iv)

        val encrypted = ByteArray(decodedData.size - iv.size)
        buffer.get(encrypted)

        val cipher = Cipher.getInstance(SymmetricKey.KEY_TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, symmetricKey.getKey(), GCMParameterSpec(SymmetricKey.KEY_IV_TAG_LENGTH, iv))

        return cipher.doFinal(encrypted).toChars()
    }

    override fun clear() {
        keystore.clear()
    }


    companion object {
        private const val DEFAULT_CHARSET = "UTF-8"
    }

}
