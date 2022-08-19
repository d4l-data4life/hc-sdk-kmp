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

import com.squareup.moshi.Moshi
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.Test

class DocumentUploadResponseTest {
    @Test
    fun `It fulfils DocumentUploadResponse`() {
        val response: Any = DocumentUploadResponse("test")
        assertTrue(response is NetworkModelContract.DocumentUploadResponse)
    }

    @Test
    fun `Given a DocumentUploadResponse is serialized it transforms into a valid JSON`() {
        val moshi = Moshi.Builder()
            .build()
        val adapter = moshi.adapter<DocumentUploadResponse>(DocumentUploadResponse::class.java)

        assertEquals(
            actual = adapter.toJson(DOCUMENT_UPLOAD_RESPONSE),
            expected = DOCUMENT_UPLOAD_RESPONSE_JSON
        )
    }

    @Test
    fun `Given a DocumentUploadResponse in JSON format is deserialized it transforms into DocumentUploadResponse`() {
        val moshi = Moshi.Builder()
            .build()
        val adapter = moshi.adapter<DocumentUploadResponse>(DocumentUploadResponse::class.java)

        assertEquals(
            actual = adapter.fromJson(DOCUMENT_UPLOAD_RESPONSE_JSON),
            expected = DOCUMENT_UPLOAD_RESPONSE
        )
    }

    companion object {
        private const val DOCUMENT_UPLOAD_RESPONSE_JSON = "{\"document_id\":\"test\"}"
        private val DOCUMENT_UPLOAD_RESPONSE = DocumentUploadResponse("test")
    }
}
