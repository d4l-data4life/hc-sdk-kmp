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

import care.data4life.sdk.network.model.CommonKeyResponse
import care.data4life.sdk.network.model.EncryptedKey
import care.data4life.sdk.network.model.NetworkModelContract
import care.data4life.sdk.network.typeadapter.EncryptedKeyTypeAdapter
import com.squareup.moshi.Moshi
import io.mockk.mockk
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CommonKeyResponseTest {
    @Test
    fun `It fulfils CommonKeyResponse`() {
        val response: Any = CommonKeyResponse(mockk())
        assertTrue(response is NetworkModelContract.CommonKeyResponse)
    }

    @Test
    fun `Given a CommonKeyResponse is serialized it transforms it into valid JSON`() {
        val moshi = Moshi.Builder()
            .add(EncryptedKeyTypeAdapter())
            .build()
        val adapter = moshi.adapter<CommonKeyResponse>(CommonKeyResponse::class.java)

        assertEquals(
            actual = adapter.toJson(COMMON_KEY_RESPONSE),
            expected = COMMON_KEY_RESPONSE_JSON
        )
    }

    @Test
    fun `Given a CommonKeyResponse in JSON format is deserialized it transforms it into CommonKeyResponse`() {
        val moshi = Moshi.Builder()
            .add(EncryptedKeyTypeAdapter())
            .build()
        val adapter = moshi.adapter<CommonKeyResponse>(CommonKeyResponse::class.java)

        assertEquals(
            actual = adapter.fromJson(COMMON_KEY_RESPONSE_JSON),
            expected = COMMON_KEY_RESPONSE
        )
    }

    companion object {
        private val ENCRYPTED_COMMON_KEY = EncryptedKey("abc")

        const val COMMON_KEY_RESPONSE_JSON = "{\"common_key\":\"abc\"}"
        private val COMMON_KEY_RESPONSE = CommonKeyResponse(ENCRYPTED_COMMON_KEY)
    }
}
