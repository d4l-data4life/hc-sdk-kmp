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

package care.data4life.sdk.auth

import care.data4life.auth.AuthorizationService
import care.data4life.sdk.ApiService
import care.data4life.sdk.CryptoSecureStore
import care.data4life.sdk.CryptoService
import care.data4life.sdk.log.Log
import care.data4life.sdk.network.model.VersionList
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.verify
import io.reactivex.Single
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito

class UserServiceTest {

    companion object {
        private const val AUTH_STATE = "authState"
        private const val USER_ALIAS = "userAlias"
        private const val KEY_USER_ID = "user_id"
        private const val AUTH_TOKEN = "authToken"
    }

    private lateinit var apiService: ApiService
    private lateinit var storage: CryptoSecureStore
    private lateinit var userService: UserService
    private lateinit var cryptoService: CryptoService
    private lateinit var authService: AuthorizationService

    @Before
    fun setUp() {
        authService = Mockito.mock(AuthorizationService::class.java)
        userService = Mockito.mock(UserService::class.java)
        storage = Mockito.mock(CryptoSecureStore::class.java)
        apiService = Mockito.mock(ApiService::class.java)
        cryptoService = Mockito.mock(CryptoService::class.java)
        userService = Mockito.spy(UserService(USER_ALIAS, authService, apiService, storage, cryptoService))
    }

    @Test
    fun `Given VersionList is fetched, when the call fails return true`() {
        val error = RuntimeException("error")
        val response: Single<VersionList> = Single.fromCallable { throw error }
        mockkObject(Log)
        coEvery { apiService.fetchVersionInfo("alias") } returns response
        //when
        val testSubscriber = userService.getVersionInfo("1.9.0")
                ?.test()
                ?.await()
        //then
        testSubscriber?.assertValue(true)
        verify(exactly = 1) { Log.error(error, "Version not supported") }
    }

    @Test
    fun `Given VersionList is fetched, when version supported return true`() {
        val versions: VersionList = mockk()
        val response: Single<VersionList> = Single.just(versions)
        val currentVersion = "1.9.0"
        coEvery { apiService.fetchVersionInfo(currentVersion) } returns response
        coEvery { versions.isSupported(currentVersion) } returns true
        //when
        val testSubscriber = userService.getVersionInfo("1.9.0")
                ?.test()
        //then
        testSubscriber?.assertValue(true)
                ?.assertNoErrors()

    }
}
