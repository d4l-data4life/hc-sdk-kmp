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

/**
 * Exception class that will be thrown in case of data validation violations.
 */
sealed class DataValidationException(message: String? = null, cause: Throwable? = null) : D4LException(message, cause) {

    constructor() : this(null, null)
    constructor(message: String?) : this(message, null)
    constructor(cause: Throwable?) : this(cause?.toString(), cause)


    class ModelVersionNotSupported(message: String? = "Model version not supported") : DataValidationException(message = message)
    class IdUsageViolation(message: String? = "DomainResource.id is reserved for SDK internal operations") : DataValidationException(message = message)
    class ExpectedFieldViolation(message: String? = "Field value was expected") : DataValidationException(message = message)
    class InvalidAttachmentPayloadHash(message: String? = "Attachment hash is invalid") : DataValidationException(message = message)
    class TagsAndAnnotationsLimitViolation(message: String? = "Annotations and Tags are exceeding maximum length") : DataValidationException(message = message)
    class CustomDataLimitViolation(message: String? = "The given record data exceeds the maximum size") : DataValidationException(message = message)
    class AnnotationFormatViolation(message: String) : DataValidationException(message = message)
    class AnnotationViolation(message: String) : DataValidationException(message = message)
}

