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

package care.data4life.crypto.error

import care.data4life.sdk.lang.D4LException

sealed class CryptoException(
    message: String? = null,
    cause: Throwable? = null
) : D4LException(message = message, cause = cause) {

    constructor(message: String?) : this(message, null)

    constructor(cause: Throwable?) : this(cause?.toString(), cause)

    constructor() : this(null, null)

    class InvalidKeyType(val name: String) : CryptoException("Key type '$name' is not supported")
    class InvalidKeyVersion(val version: Int) : CryptoException(
        "Key version '$version' is not supported"
    )

    class EncryptionFailed(message: String? = null, cause: Throwable? = null) :
        CryptoException(message, cause) {
        constructor(message: String? = null) : this(message, null)
    }

    class DecryptionFailed(message: String? = null, cause: Throwable? = null) :
        CryptoException(message, cause) {
        constructor(message: String? = null) : this(message, null)
    }

    class KeyEncryptionFailed(message: String?) : CryptoException(message)
    class KeyDecryptionFailed(message: String?) : CryptoException(message)
    class KeyGenerationFailed(message: String?) : CryptoException(message)
    class KeyFetchingFailed(message: String?) : CryptoException(message)
}
