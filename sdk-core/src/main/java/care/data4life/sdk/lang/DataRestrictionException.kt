/*
 * Copyright (c) 2021 D4L data4life gGmbH / All rights reserved.
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

import care.data4life.sdk.config.DataRestriction.DATA_SIZE_MAX_MB

/**
 * Exception class that will be thrown in case of data restriction violations like file size is too large or
 * file type is unsupported.
 */
@Deprecated(message = "This Error will move with the next release to the top level SDKs, since it is a DomainError.")
sealed class DataRestrictionException(message: String? = null, cause: Throwable? = null) :
    D4LException(message, cause) {

    constructor() : this(null, null)
    constructor(message: String?) : this(message, null)
    constructor(cause: Throwable?) : this(cause?.toString(), cause)

    class MaxDataSizeViolation :
        DataRestrictionException(message = "The file size has to be smaller or equal to ${DATA_SIZE_MAX_MB}MB!")

    class UnsupportedFileType :
        DataRestrictionException(message = "Only this file types are supported: JPEG, PNG, TIFF, PDF and DCM!")
}
