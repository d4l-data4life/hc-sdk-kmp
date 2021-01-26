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

package care.data4life.sdk.network

import care.data4life.sdk.network.model.EncryptedKey
import care.data4life.sdk.network.model.EncryptedRecord
import care.data4life.sdk.network.typeadapter.EncryptedKeyTypeAdapter
import com.squareup.moshi.Moshi
import org.junit.Assert.assertEquals
import org.junit.Test

class EncryptedRecordTest {
    @Test
    fun `Given a EncryptedRecord is serialized, it transforms into the valid json format`() {
        val moshi = Moshi.Builder()
                .add(EncryptedKeyTypeAdapter())
                .build()
        val adapter = moshi.adapter<EncryptedRecord>(EncryptedRecord::class.java)

        assertEquals(
                ENCRYPTED_RECORD_JSON,
                adapter.toJson(ENCRYPTED_RECORD)
        )

        assertEquals(
                ENCRYPTED_RECORD_WITH_NULL_FIELDS_JSON,
                adapter.toJson(ENCRYPTED_RECORD_WITH_NULL_FIELDS)
        )
    }

    @Test
    fun `Given a EncryptedRecord is deserialized it transforms into EncryptedRecord`() {
        val moshi = Moshi.Builder()
                .add(EncryptedKeyTypeAdapter())
                .build()
        val adapter = moshi.adapter<EncryptedRecord>(EncryptedRecord::class.java)

        assertEquals(
                ENCRYPTED_RECORD,
                adapter.fromJson(ENCRYPTED_RECORD_JSON)
        )

        assertEquals(
                ENCRYPTED_RECORD_WITH_NULL_FIELDS,
                adapter.fromJson(ENCRYPTED_RECORD_WITH_NULL_FIELDS_JSON)
        )
    }

    companion object {
        private val ENCRYPTED_DATA_KEY = EncryptedKey("abc")
        private val ENCRYPTED_ATTACHMENT_KEY = EncryptedKey("gh")

        private const val ENCRYPTED_RECORD_JSON = "{\"attachment_key\":\"gh\",\"common_key_id\":\"asd\",\"createdAt\":\"awqwe\",\"date\":\"asdasd\",\"encrypted_body\":\"test\",\"encrypted_key\":\"abc\",\"encrypted_tags\":[\"a\",\"b\",\"c\"],\"model_version\":23,\"record_id\":\"abc\",\"version\":0}"
        private val ENCRYPTED_RECORD = EncryptedRecord(
                _commonKeyId = "asd",
                identifier = "abc",
                encryptedTags = listOf("a", "b", "c"),
                encryptedBody = "test",
                customCreationDate = "asdasd",
                encryptedDataKey = ENCRYPTED_DATA_KEY,
                encryptedAttachmentsKey = ENCRYPTED_ATTACHMENT_KEY,
                modelVersion = 23,
                updatedDate = "awqwe"
        )

        private const val ENCRYPTED_RECORD_WITH_NULL_FIELDS_JSON = "{\"common_key_id\":\"asd\",\"encrypted_body\":\"test\",\"encrypted_key\":\"abc\",\"encrypted_tags\":[\"a\",\"b\",\"c\"],\"model_version\":23,\"record_id\":\"abc\",\"version\":0}"
        private val ENCRYPTED_RECORD_WITH_NULL_FIELDS = EncryptedRecord(
                _commonKeyId = "asd",
                identifier = "abc",
                encryptedTags = listOf("a", "b", "c"),
                encryptedBody = "test",
                customCreationDate = null,
                encryptedDataKey = ENCRYPTED_DATA_KEY,
                encryptedAttachmentsKey = null,
                modelVersion = 23,
                updatedDate = null
        )
    }
}
