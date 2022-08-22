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

package care.data4life.sdk

import care.data4life.sdk.auth.AuthContract
import care.data4life.sdk.call.CallHandler
import care.data4life.sdk.test.util.GenericTestDataProvider.ALIAS
import care.data4life.sdk.test.util.GenericTestDataProvider.USER_ID
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.reactivex.Single
import kotlin.test.assertEquals
import org.junit.Before
import org.junit.Test

class BaseClientTest {
    class ShallowClient(
        alias: String,
        userService: AuthContract.UserService,
        recordService: RecordService,
        handler: CallHandler,
        authClient: SdkContract.AuthClient,
        data: SdkContract.DataRecordClient = createDataClient(userService, recordService, handler),
        fhir4: SdkContract.Fhir4RecordClient = createFhir4Client(
            userService,
            recordService,
            handler
        ),
        legacyDataClient: SdkContract.LegacyDataClient = createLegacyDataClient(
            userService,
            recordService,
            handler
        )
    ) : BaseClient(
        alias,
        userService,
        recordService,
        handler,
        authClient,
        data,
        fhir4,
        legacyDataClient
    )

    private val userService: AuthContract.UserService = mockk()
    private val recordService: RecordService = mockk()
    private val authClient: SdkContract.AuthClient = mockk()
    private val data: SdkContract.DataRecordClient = mockk()
    private val fhir4: SdkContract.Fhir4RecordClient = mockk()
    private val legacyDataClient: SdkContract.LegacyDataClient = mockk()
    private val handler: CallHandler = mockk()
    private lateinit var client: SdkContract.Client

    @Before
    fun setUp() {
        clearAllMocks()
    }

    @Test
    fun `Given a User is not logged in and the userID is fetched, it returns the UserId`() {
        // Given
        every { userService.finishLogin(true) } returns Single.just(true)
        every { userService.userID } returns Single.just(USER_ID)

        client = ShallowClient(
            ALIAS,
            userService,
            recordService,
            handler,
            authClient,
            data,
            fhir4,
            legacyDataClient
        )

        // When
        val id = client.userId

        // Then
        assertEquals(
            actual = id,
            expected = USER_ID
        )
    }
}
