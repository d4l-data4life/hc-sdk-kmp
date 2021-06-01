package care.data4life.sdk.auth

import care.data4life.sdk.call.CallHandler
import care.data4life.sdk.call.Task
import care.data4life.sdk.listener.Callback
import care.data4life.sdk.listener.ResultListener
import care.data4life.sdk.test.util.GenericTestDataProvider.ALIAS
import io.mockk.every
import io.mockk.mockk
import io.reactivex.Completable
import io.reactivex.Single
import org.junit.Before
import org.junit.Test
import kotlin.test.assertSame
import kotlin.test.assertTrue

class AuthClientTest {
    private val userService: AuthContract.UserService = mockk()
    private val callHandler: CallHandler = mockk()
    private lateinit var client: AuthContract.Client

    @Before
    fun setUp() {
        client = AuthClient(ALIAS, userService, callHandler)
    }

    @Test
    fun `it fulfils AuthClient`() {
        val client: Any = AuthClient(ALIAS, mockk(), mockk())

        assertTrue(client is AuthContract.Client)
    }

    @Test
    fun `Given getUserSessionToken is called, with a ResultListener it returns the corresponding Task`() {
        // Given
        val listener: ResultListener<String> = mockk()
        val expected: Task = mockk()

        val token = Single.just("token")

        every { userService.refreshSessionToken(ALIAS) } returns token
        every { callHandler.executeSingle(token, listener) } returns expected

        // When
        val task = client.getUserSessionToken(listener)

        // Then
        assertSame(
            actual = task,
            expected = expected,
        )
    }

    @Test
    fun `Given isUserLoggedIn is called, with a ResultListener it returns the corresponding Task`() {
        // Given
        val listener: ResultListener<Boolean> = mockk()
        val expected: Task = mockk()

        val isLoggedIn = Single.just(true)

        every { userService.isLoggedIn(ALIAS) } returns isLoggedIn
        every { callHandler.executeSingle(isLoggedIn, listener) } returns expected

        // When
        val task = client.isUserLoggedIn(listener)

        // Then
        assertSame(
            actual = task,
            expected = expected,
        )
    }

    @Test
    fun `Given logout is called, with a Callback it returns the corresponding Task`() {
        // Given
        val listener: Callback = mockk()
        val expected: Task = mockk()

        val logout: Completable = mockk()

        every { userService.logout() } returns logout

        every { callHandler.executeCompletable(logout, listener) } returns expected

        // When
        val task = client.logout(listener)

        // Then
        assertSame(
            actual = task,
            expected = expected,
        )
    }
}
