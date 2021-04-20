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

package care.data4life.sdk.test.util

import care.data4life.sdk.fhir.Fhir3Attachment
import care.data4life.sdk.fhir.Fhir4Attachment

object TestAttachmentHelper {
    @JvmStatic
    fun buildFhir3Attachment(
            id: String?,
            data: String?,
            size: Int?,
            hash: String?
    ): Fhir3Attachment {
        val attachment = Fhir3Attachment()
        attachment.id = id
        attachment.data = data
        attachment.size = size
        attachment.hash = hash
        return attachment
    }

    fun buildFhir4Attachment(
            id: String?,
            data: String?,
            size: Int?,
            hash: String?
    ): Fhir4Attachment {
        val attachment = Fhir4Attachment()
        attachment.id = id
        attachment.data = data
        attachment.size = size
        attachment.hash = hash
        return attachment
    }

    fun makeAttachmentData(
            type: ByteArray,
            size: Int
    ): ByteArray {
        val payload = ByteArray(size)
        System.arraycopy(
                type,
                0,
                payload,
                0,
                type.size
        )

        return payload
    }

    fun byteArrayOf(elements: Array<Byte?>): ByteArray {
        val array = ByteArray(elements.size)
        for (idx in elements.indices) {
            array[idx] = elements[idx] ?: 0
        }

        return array
    }
}
