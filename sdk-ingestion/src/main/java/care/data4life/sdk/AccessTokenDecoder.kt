/*
 * BSD 3-Clause License
 *
 * Copyright (c) 2020, D4L data4life gGmbH
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *  Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 *  Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 *
 *  Neither the name of the copyright holder nor the names of its
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package care.data4life.sdk

import care.data4life.sdk.lang.D4LRuntimeException
import com.squareup.moshi.Moshi
import java.nio.charset.StandardCharsets
import java.util.*

internal class AccessTokenDecoder {
    private val moshi: Moshi = Moshi.Builder().build()

    /**
     * Extract the client ID from the OAuth access token.
     *
     * @param accessToken Full access token
     *
     * @return Client ID
     */
    fun extractClientId(accessToken: ByteArray?): String {
        var clientId: String? = null
        try {
            /*
            Access token is a JWT of the form
            "[base 64 header].[base 64 payload].[base 64 signature]".
            The decoded payload is a JSON which contains the client ID. We manually parse this
            JSON and pick out the value of the appropriate key.
            */
            val accessTokenString = String(accessToken!!, StandardCharsets.UTF_8)
            val tokenParts = accessTokenString.split(".").toTypedArray()
            val payloadBase64 = tokenParts[1]
            val payloadString = String(Base64.getDecoder().decode(payloadBase64), StandardCharsets.UTF_8)
            val payload = moshi.adapter(D4LJwtPayload::class.java).fromJson(payloadString)
            clientId = payload!!.ghc_cid
            if (null == clientId) {
                throw Exception("Did mot find client ID in JWT")
            }
        } catch (e: Exception) {
            throw D4LRuntimeException("Access token not correctly formatted")
        }
        return clientId
    }

}