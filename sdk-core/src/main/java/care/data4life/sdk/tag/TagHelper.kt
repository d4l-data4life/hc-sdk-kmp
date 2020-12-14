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
package care.data4life.sdk.tag

import java.util.*

object TagHelper {

    private const val TAG_DELIMITER = "="

    @JvmStatic
    fun convertToTagList(tags: HashMap<String, String>): List<String> {
        return mutableListOf<String>().also {
            for ((key, value) in tags) {
                it.add(key + TAG_DELIMITER + value)
            }
        }
    }

    @JvmStatic
    fun convertToTagMap(tagList: List<String>): HashMap<String, String> {
        val tags = HashMap<String, String>()
        for (entry in tagList) {
            val split = entry.split(TAG_DELIMITER)
            if (split.size == 2) {
                val key = split[0]
                val value = split[1]
                if (key.isNotBlank() && value.isNotBlank()) {
                    tags[key] = value
                }
            }
        }
        return tags
    }
}
