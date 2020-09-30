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
import org.junit.Assert
import org.junit.Before
import org.junit.Test

private const val VALID_ACCESS_TOKEN = "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJvd25lcjoxZTE1MzAwYy05MDA1LTRjMWYtOWVkNS05YmM0ZTUyOWQ1YzAiLCJpc3MiOiJ1cm46Z2hjIiwiZXhwIjoxNTgwOTA2NTg0LCJuYmYiOjE1ODA5MDU1NjQsImlhdCI6MTU4MDkwNTYyNCwianRpIjoiM2RmMDZiZDgtNTMwZS00NWJhLWEwMGItYzcwMTE0NDY3MDcwIiwiZ2hjOmFpZCI6ImY0MmRiY2ViLTk2MTQtNDlkOC1hYjQ2LTI0NmU1NGQ3ZjhjMSIsImdoYzpjaWQiOiIzMWJlMTE5ZS0zNzgyLTRkYjgtYTI0YS0xNDkwZWVhMjdlZDMjd2ViIiwiZ2hjOnVpZCI6IjFlMTUzMDBjLTkwMDUtNGMxZi05ZWQ1LTliYzRlNTI5ZDVjMCIsImdoYzpzY29wZSI6InBlcm06ciBwZXJtOncgcmVjOnIgcmVjOncgYXR0YWNobWVudDpyIGF0dGFjaG1lbnQ6dyB1c2VyOnIgdXNlcjp3IHVzZXI6cSJ9.snIsQdFmPwGy9YcUSAgSlOjXHl3cBnlLkMHtOs6ybvPwaiaJUb823hCd5KAxn2PdQhan0Cl3a5ErHIybviqCyqLEGx68_urBALUnZTWK-aun7UOZZESJD0iBZW3ywZwsFHqp60gRNMYYoFCCZZqGjQAg3D7M594DC7aadunxPD1xncCkNJzYEM8ZnEdkzvu6e6rj8Uw27dv9DAjCoz7Fy1KdimIz4vcsjitq-n0mTA5EZVHo84bnBWop8-QlNuzlv5R10XJwaeCvwAMAoVzQWXEeIke4XtVmvyF0kDOptZSp7gP3fpucXud29h1XZtkc2XkILL5AbXu6fG9ueQnaGZb7be4zBiIC-N1ubkkC18DQ_zxg7XnkBZz3zGOk3litCqeqd_Ir1DCQmxM6nsfNanNFehb7ppZyPIVz8Lo6XQxQcbqPwXO2EW1tAezM9m9Yo50nyRW827RqZK4so8QgQx6SGV_EsszzX37mYoLENYNDVjOMaCxWboZrD9XDJCpBVa3hO5lRK_2bQRQ6HwYmDHhu5RAE3LGqe_XyHOiwJcElkoQ2VrwVbkEoTyjBXV2Vy9My6gW9Uzl0ZWAyl1MhyC30-N9xjN20arA5RLMHVfVPxmhOeKSkSBJTTTd86HJPJPnN1y1ymgNBY96VyZqweM4Jk-kndOZFyjkw6a0mRl8"
private const val VALID_ACCESS_TOKEN_CLIENT_ID = "31be119e-3782-4db8-a24a-1490eea27ed3#web"
private const val INVALID_ACCESS_TOKEN = "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.bm90anNvbnsic3ViIjoib3duZXI6MWUxNTMwMGMtOTAwNS00YzFmLTllZDUtOWJjNGU1MjlkNWMwIiwiaXNzIjoidXJuOmdoYyIsImV4cCI6MTU4MDkwNjU4NCwibmJmIjoxNTgwOTA1NTY0LCJpYXQiOjE1ODA5MDU2MjQsImp0aSI6IjNkZjA2YmQ4LTUzMGUtNDViYS1hMDBiLWM3MDExNDQ2NzA3MCIsImdoYzphaWQiOiJmNDJkYmNlYi05NjE0LTQ5ZDgtYWI0Ni0yNDZlNTRkN2Y4YzEiLCJnaGM6Y2lkIjoiMzFiZTExOWUtMzc4Mi00ZGI4LWEyNGEtMTQ5MGVlYTI3ZWQzI3dlYiIsImdoYzp1aWQiOiIxZTE1MzAwYy05MDA1LTR.snIsQdFmPwGy9YcUSAgSlOjXHl3cBnlLkMHtOs6ybvPwaiaJUb823hCd5KAxn2PdQhan0Cl3a5ErHIybviqCyqLEGx68_urBALUnZTWK-aun7UOZZESJD0iBZW3ywZwsFHqp60gRNMYYoFCCZZqGjQAg3D7M594DC7aadunxPD1xncCkNJzYEM8ZnEdkzvu6e6rj8Uw27dv9DAjCoz7Fy1KdimIz4vcsjitq-n0mTA5EZVHo84bnBWop8-QlNuzlv5R10XJwaeCvwAMAoVzQWXEeIke4XtVmvyF0kDOptZSp7gP3fpucXud29h1XZtkc2XkILL5AbXu6fG9ueQnaGZb7be4zBiIC-N1ubkkC18DQ_zxg7XnkBZz3zGOk3litCqeqd_Ir1DCQmxM6nsfNanNFehb7ppZyPIVz8Lo6XQxQcbqPwXO2EW1tAezM9m9Yo50nyRW827RqZK4so8QgQx6SGV_EsszzX37mYoLENYNDVjOMaCxWboZrD9XDJCpBVa3hO5lRK_2bQRQ6HwYmDHhu5RAE3LGqe_XyHOiwJcElkoQ2VrwVbkEoTyjBXV2Vy9My6gW9Uzl0ZWAyl1MhyC30-N9xjN20arA5RLMHVfVPxmhOeKSkSBJTTTd86HJPJPnN1y1ymgNBY96VyZqweM4Jk-kndOZFyjkw6a0mRl8"
private const val NO_CLIENT_ID_ACCESS_TOKEN = "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJvd25lcjoxZTE1MzAwYy05MDA1LTRjMWYtOWVkNS05YmM0ZTUyOWQ1YzAiLCJpc3MiOiJ1cm46Z2hjIiwiZXhwIjoxNTgwOTA2NTg0LCJuYmYiOjE1ODA5MDU1NjQsImlhdCI6MTU4MDkwNTYyNCwianRpIjoiM2RmMDZiZDgtNTMwZS00NWJhLWEwMGItYzcwMTE0NDY3MDcwIiwiZ2hjOmFpZCI6ImY0MmRiY2ViLTk2MTQtNDlkOC1hYjQ2LTI0NmU1NGQ3ZjhjMSIsImdoYzp1aWQiOiIxZTE1MzAwYy05MDA1LTRjMWYtOWVkNS05YmM0ZTUyOWQ1YzAiLCJnaGM6c2NvcGUiOiJwZXJtOnIgcGVybTp3IHJlYzpyIHJlYzp3IGF0dGFjaG1lbnQ6ciBhdHRhY2htZW50OncgdXNlcjpyIHVzZXI6dyB1c2VyOnEifQ.snIsQdFmPwGy9YcUSAgSlOjXHl3cBnlLkMHtOs6ybvPwaiaJUb823hCd5KAxn2PdQhan0Cl3a5ErHIybviqCyqLEGx68_urBALUnZTWK-aun7UOZZESJD0iBZW3ywZwsFHqp60gRNMYYoFCCZZqGjQAg3D7M594DC7aadunxPD1xncCkNJzYEM8ZnEdkzvu6e6rj8Uw27dv9DAjCoz7Fy1KdimIz4vcsjitq-n0mTA5EZVHo84bnBWop8-QlNuzlv5R10XJwaeCvwAMAoVzQWXEeIke4XtVmvyF0kDOptZSp7gP3fpucXud29h1XZtkc2XkILL5AbXu6fG9ueQnaGZb7be4zBiIC-N1ubkkC18DQ_zxg7XnkBZz3zGOk3litCqeqd_Ir1DCQmxM6nsfNanNFehb7ppZyPIVz8Lo6XQxQcbqPwXO2EW1tAezM9m9Yo50nyRW827RqZK4so8QgQx6SGV_EsszzX37mYoLENYNDVjOMaCxWboZrD9XDJCpBVa3hO5lRK_2bQRQ6HwYmDHhu5RAE3LGqe_XyHOiwJcElkoQ2VrwVbkEoTyjBXV2Vy9My6gW9Uzl0ZWAyl1MhyC30-N9xjN20arA5RLMHVfVPxmhOeKSkSBJTTTd86HJPJPnN1y1ymgNBY96VyZqweM4Jk-kndOZFyjkw6a0mRl8"

class AccessTokenDecoderTest {
    private var accessTokenDecoder: AccessTokenDecoder? = null

    @Before
    fun setUp() {
        accessTokenDecoder = AccessTokenDecoder()
    }

    @Test
    fun extractClientID_whenGivenCorrectToken_shouldExtractClientId() {
        val clientId = accessTokenDecoder!!.extractClientId(VALID_ACCESS_TOKEN.toByteArray())
        Assert.assertEquals(VALID_ACCESS_TOKEN_CLIENT_ID, clientId)
    }

    @Test(expected = D4LRuntimeException::class)
    fun extractClientID_whenGivenInvalidToken_shouldThrowError() {
        accessTokenDecoder!!.extractClientId(INVALID_ACCESS_TOKEN.toByteArray())
    }

    @Test(expected = D4LRuntimeException::class)
    fun extractClientID_whenGivenTokenWithoutClientId_shouldThrowError() {
        accessTokenDecoder!!.extractClientId(NO_CLIENT_ID_ACCESS_TOKEN.toByteArray())
    }
}