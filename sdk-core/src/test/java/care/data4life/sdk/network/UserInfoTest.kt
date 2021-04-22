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
import care.data4life.sdk.network.model.NetworkModelContract
import care.data4life.sdk.network.model.UserInfo
import care.data4life.sdk.network.typeadapter.EncryptedKeyTypeAdapter
import com.squareup.moshi.Moshi
import io.mockk.mockk
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class UserInfoTest {
    @Test
    fun `It fulfils UserInfo`() {
        val info: Any = UserInfo("123", mockk(), null, mockk())
        assertTrue(info is NetworkModelContract.UserInfo)
    }

    @Test
    fun `It sets a default CommonKeyId, if no was given`() {
        val info = UserInfo("123", mockk(), null, mockk())

        assertEquals(
            expected = NetworkModelContract.DEFAULT_COMMON_KEY_ID,
            actual = info.commonKeyId
        )
    }

    @Test
    fun `Given a UserInfo is serialized, it transforms into the valid json format`() {
        val moshi = Moshi.Builder()
            .add(EncryptedKeyTypeAdapter())
            .build()
        val adapter = moshi.adapter<UserInfo>(UserInfo::class.java)

        assertEquals(
            expected = SERIALIZED_USER_INFO,
            actual = adapter.toJson(USER_INFO)
        )
    }

    @Test
    fun `Given a UserInfo is deserialized, it transforms into the valid UserInfo`() {
        val moshi = Moshi.Builder()
            .add(EncryptedKeyTypeAdapter())
            .build()
        val adapter = moshi.adapter<UserInfo>(UserInfo::class.java)

        assertEquals(
            expected = USER_INFO,
            actual = adapter.fromJson(SERIALIZED_USER_INFO)
        )
    }

    companion object {
        private val COMMON_KEY = EncryptedKey("abc")
        private val TAG_ENCRYPTION_KEY = EncryptedKey("gh")

        private const val SERIALIZED_USER_INFO =
            "{\"common_key\":\"abc\",\"common_key_id\":\"ID\",\"sub\":\"42\",\"tag_encryption_key\":\"gh\"}"
        private val USER_INFO = UserInfo(
            "42",
            COMMON_KEY,
            "ID",
            TAG_ENCRYPTION_KEY
        )
    }
}
