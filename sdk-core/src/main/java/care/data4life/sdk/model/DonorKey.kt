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

package care.data4life.sdk.model

import care.data4life.sdk.util.toBytes
import care.data4life.sdk.util.toChars
import com.squareup.moshi.FromJson
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import com.squareup.moshi.Moshi
import com.squareup.moshi.ToJson
import okio.Buffer
import okio.ByteString
import java.io.CharArrayWriter
import java.lang.reflect.Type

data class DonorKey(
        val t: CharArray,
        val priv: CharArray,
        val pub: CharArray,
        val v: CharArray,
        val scope: CharArray
) {
    fun toJsonChars(): CharArray {
        val buffer = Buffer()
        Moshi.Builder().build().adapter(DonorKey::class.java).toJson(buffer,this)
        return buffer.readByteArray().toChars()
    }
    companion object {
        fun fromJsonChars(donorKey: CharArray): DonorKey {
            val buffer = Buffer()
            buffer.write(donorKey.toBytes())
            return requireNotNull(Moshi.Builder().build().adapter(DonorKey::class.java).fromJson(buffer)) { "Deserialization of DonorKey failed" }
        }
    }
}

