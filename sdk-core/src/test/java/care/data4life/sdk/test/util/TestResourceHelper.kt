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

import care.data4life.sdk.util.Base64
import care.data4life.sdk.util.HashUtil
import java.nio.charset.StandardCharsets

object TestResourceHelper {

    fun getByteResource(
            resourceFolder: String,
            resourceName: String
    ): ByteArray = this::class.java.getResource(
            "/$resourceFolder/$resourceName"
    ).readBytes()

    fun getTextResource(
            resourceFolder: String,
            resourceName: String
    ): String = this::class.java.getResource(
            "/$resourceFolder/$resourceName"
    ).readText(StandardCharsets.UTF_8)

    fun getJSONResource(
            resourceFolder: String,
            resourceName: String
    ): String = getTextResource(
            resourceFolder,
            "$resourceName.json"
    )

    fun loadTemplate(
        path: String,
        file: String,
        recordId: String,
        partnerId: String
    ): String = getJSONResource(
        path,
        file
    ).replace("\$RESOURCE_IDENTIFIERS", "d4l_f_p_t#$recordId")
        .replace("\$RESOURCE_ID", recordId)
        .replace("\$PARTNER_ID", partnerId)

    fun loadTemplateWithAttachments(
        path: String,
        file: String,
        recordId: String,
        partnerId: String,
        attachmentTitle: String,
        attachmentType: String,
        attachmentData: String
    ): String = loadTemplate(
        path,
        file,
        recordId,
        partnerId
    ).replace("\$ATTACHMENT_TITLE", attachmentTitle)
        .replace("\$ATTACHMENT_TYPE", attachmentType)
        .replace("\$ATTACHMENT_HASH", getValidHash(attachmentData))
        .replace("\$ATTACHMENT_SIZE", attachmentData.length.toString())
        .replace("\$ATTACHMENT_PAYLOAD", attachmentData)

    private fun getValidHash(
        data: String
    ): String = Base64.encodeToString(HashUtil.sha1(Base64.decode(data)))
}
