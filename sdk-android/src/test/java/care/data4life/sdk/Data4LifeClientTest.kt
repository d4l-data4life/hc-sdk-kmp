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
package care.data4life.sdk

import android.content.Intent
import care.data4life.sdk.auth.AuthorizationService
import care.data4life.sdk.auth.UserService
import care.data4life.sdk.call.CallHandler
import care.data4life.sdk.crypto.CryptoService
import care.data4life.sdk.crypto.GCAsymmetricKey
import care.data4life.sdk.crypto.GCKeyPair
import com.google.common.truth.Truth
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.reactivex.Single
import org.junit.Before
import org.junit.Test

class Data4LifeClientTest {
    private val userService: UserService = mockk()
    private lateinit var instance: Data4LifeClient
    private var authorizationService: AuthorizationService = mockk()
    private var cryptoService: CryptoService = mockk()
    private var recordService: RecordService = mockk()
    private var callHandler: CallHandler = mockk()

    @Before
    fun setUp() {
        clearAllMocks()

        instance = Data4LifeClient(
            ALIAS,
            cryptoService,
            authorizationService,
            userService,
            recordService,
            callHandler
        )
    }

    @Test
    fun hasUserId() {
        // Given
        val uId = "uid"

        every { userService.finishLogin(IS_LOGGED_IN) } returns Single.just(IS_LOGGED_IN)
        every { userService.userID } returns Single.just(uId)

        // When
        val userId = instance.userId

        // Then
        Truth.assertThat(userId).isEqualTo(uId)
    }

    @Test
    fun authorizationIntent_withScopes() {
        // Given
        val pubKey = "Pubkey"
        val scopes = mutableSetOf("scope")

        val publicKey: GCAsymmetricKey = mockk()
        val keyPair: GCKeyPair = mockk()

        every { keyPair.publicKey } returns publicKey
        every { cryptoService.generateGCKeyPair() } returns Single.just(keyPair)
        every {
            cryptoService.convertAsymmetricKeyToBase64ExchangeKey(publicKey)
        } returns Single.just(pubKey)

        val intent: Intent = mockk()

        every {
            authorizationService.loginIntent(
                any(),
                scopes,
                pubKey,
                any()
            )
        } returns intent

        // When
        val loginIntent = instance.getLoginIntent(null, scopes)

        // Then
        Truth.assertThat(loginIntent).isEqualTo(intent)
    }

    companion object {
        private const val IS_LOGGED_IN = true
        private const val ALIAS = "alias"
    }
}
