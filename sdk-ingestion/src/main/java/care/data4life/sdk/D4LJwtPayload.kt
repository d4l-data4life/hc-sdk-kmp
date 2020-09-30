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

import care.data4life.crypto.Json

/**
 * Class encoding a JWT token payload of the type used by PHDP. For documentation of the format,
 * please refer to the documentation of the access token format in the dev-doc documentation
 * repository.
 *
 * Note that this class does NOT perform the various checks required by the APIs. It does not
 * require any fields to be present in the JSON to be parsed, either (though it will always
 * fill in the "iss" claim in instances since it has a fixed value).
 */
data class D4LJwtPayload(
        /* Standard (registered) claims */
        val iss: String = "urn:ghc",                        // "Issuer" claim
        val sub: String? = null,                            // "Subject" claim
        val exp: Double? = null,                            // "Expiration Time" claim
        val nbf: Double? = null,                            // "Not Before" claim
        val iat: Double? = null,                            // "Issued At" claim
        val jti: String? = null,                            // "JWT ID" claim
        /* Private D4L claims */
        @field:Json("ghc:scope") val ghc_scope: String? = null,      // Scopes - could consider adding a type adapter for the scopes that parses them in to a list
        @field:Json("ghc:uid") val ghc_uid: String? = null,          // The source User ID which requested the JWT (not always the subject)
        @field:Json("ghc:cid") val ghc_cid: String? = null,          // The client ID which requested the JWT
        @field:Json("ghc:aid") val ghc_aid: String? = null           // The app ID which requested the JWT
)