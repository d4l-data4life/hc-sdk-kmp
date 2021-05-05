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

package care.data4life.sdk.network.model

import care.data4life.sdk.network.typeadapter.EncryptedKeyTypeAdapter
import com.squareup.moshi.Moshi
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class EncryptedRecordTest {
    @Test
    fun `It fulfils EncryptedRecord`() {
        val record: Any = ENCRYPTED_RECORD
        assertTrue(record is NetworkModelContract.EncryptedRecord)
    }

    @Test
    fun `It sets a default CommonKeyId, if no was given`() {
        val record = EncryptedRecord(
            _commonKeyId = null,
            identifier = "abc",
            encryptedTags = listOf("a", "b", "c"),
            encryptedBody = "test",
            customCreationDate = "asdasd",
            encryptedDataKey = ENCRYPTED_DATA_KEY,
            encryptedAttachmentsKey = ENCRYPTED_ATTACHMENT_KEY,
            modelVersion = 23,
            updatedDate = "awqwe"
        )

        assertEquals(
            expected = NetworkModelContract.DEFAULT_COMMON_KEY_ID,
            actual = record.commonKeyId
        )
    }

    @Test
    fun `Given a EncryptedRecord is serialized, it transforms into a valid JSON`() {
        val moshi = Moshi.Builder()
            .add(EncryptedKeyTypeAdapter())
            .build()
        val adapter = moshi.adapter<EncryptedRecord>(EncryptedRecord::class.java)

        assertEquals(
            expected = ENCRYPTED_RECORD_JSON,
            actual = adapter.toJson(ENCRYPTED_RECORD)
        )

        assertEquals(
            expected = ENCRYPTED_RECORD_WITH_NULL_FIELDS_JSON,
            actual = adapter.toJson(ENCRYPTED_RECORD_WITH_NULL_FIELDS)
        )
    }

    @Test
    fun `Given a EncryptedRecord is deserialized it transforms into EncryptedRecord`() {
        val moshi = Moshi.Builder()
            .add(EncryptedKeyTypeAdapter())
            .build()
        val adapter = moshi.adapter<EncryptedRecord>(EncryptedRecord::class.java)

        assertEquals(
            actual = ENCRYPTED_RECORD,
            expected = adapter.fromJson(ENCRYPTED_RECORD_JSON)
        )

        assertEquals(
            actual = ENCRYPTED_RECORD_WITH_NULL_FIELDS,
            expected = adapter.fromJson(ENCRYPTED_RECORD_WITH_NULL_FIELDS_JSON)
        )
    }

    companion object {
        private val ENCRYPTED_DATA_KEY = EncryptedKey("abc")
        private val ENCRYPTED_ATTACHMENT_KEY = EncryptedKey("gh")

        private const val ENCRYPTED_RECORD_JSON =
            "{\"common_key_id\":\"asd\",\"record_id\":\"abc\",\"encrypted_tags\":[\"a\",\"b\",\"c\"],\"encrypted_body\":\"test\",\"date\":\"asdasd\",\"encrypted_key\":\"abc\",\"attachment_key\":\"gh\",\"model_version\":23,\"createdAt\":\"awqwe\",\"version\":0}"
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

        private const val ENCRYPTED_RECORD_WITH_NULL_FIELDS_JSON =
            "{\"common_key_id\":\"asd\",\"record_id\":\"abc\",\"encrypted_tags\":[\"a\",\"b\",\"c\"],\"encrypted_body\":\"test\",\"encrypted_key\":\"abc\",\"model_version\":23,\"version\":0}"
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
