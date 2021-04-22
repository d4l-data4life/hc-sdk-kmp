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

package care.data4life.sdk.lang

sealed class CoreRuntimeException(
    message: String? = null,
    cause: Throwable? = null
) : D4LRuntimeException(message, cause) {

    constructor(message: String?) : this(message, null)

    constructor(cause: Throwable?) : this(cause?.toString(), cause)

    constructor() : this(null, null)

    class InvalidManifest :
        CoreRuntimeException(message = "CLIENT_ID, CLIENT_SECRET or REDIRECT_URL definition not found in the AndroidManifest.xml")

    class ClientIdMalformed :
        CoreRuntimeException(message = "CliendId should contain split character: `#`")

    class ApplicationMetadataInaccessible :
        CoreRuntimeException(message = "Application manifest metadata inaccessible")

    class InternalFailure : CoreRuntimeException(message = "Internal failure")
    class UnsupportedOperation : CoreRuntimeException(message = "Unsupported operation")
    class Default(message: String? = "Default", cause: Throwable? = null) :
        CoreRuntimeException(message = message, cause = cause) {
        constructor(message: String?) : this(message, null)
    }
}
