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

package care.data4life.sdk.sample.presentation.main

import care.data4life.sdk.Data4LifeClient
import care.data4life.sdk.lang.D4LException
import care.data4life.sdk.listener.Callback
import care.data4life.sdk.listener.ResultListener
import care.data4life.sdk.sample.presentation.BaseView
import care.data4life.sdk.sample.presentation.View
import care.data4life.sdk.sample.presentation.data.Menu
import care.data4life.sdk.sample.presentation.data.MenuEntry
import care.data4life.sdk.sample.presentation.data.Message
import care.data4life.sdk.sample.presentation.login.LoginView
import care.data4life.sdk.sample.presentation.menu.MenuView
import care.data4life.sdk.sample.presentation.upload.UploadView
import kotlinx.coroutines.runBlocking
import org.koin.core.KoinComponent
import org.koin.core.inject
import org.koin.core.parameter.parametersOf

class SingleMainView(private val alias: String) : BaseView(), KoinComponent {


    private val client: Data4LifeClient by inject { parametersOf(alias) }
    private val menuLoggedOut = Menu(listOf(
            MenuEntry(LOGIN),
            MenuEntry(START),
            MenuEntry(EXIT)
    ))
    private val menuLoggedIn = Menu(listOf(
            MenuEntry(LOGOUT),
            MenuEntry(UPLOAD),
            MenuEntry(START),
            MenuEntry(EXIT)
    ))


    override val type: String = "singleMain"

    override fun renderContent(): View {
        val isLoggedIn = checkLoginState()

        return when {
            isLoggedIn == null -> {
                renderMessage(Message("failed to check login state!!"))
                renderDefaultMenu()
            }
            isLoggedIn -> renderAuthorizedMenu()
            else -> renderDefaultMenu()
        }
    }

    private fun renderAuthorizedMenu(): View {
        renderMenu(menuLoggedIn)

        val input = renderPrompt()

        input?.let {
            when (it.toLowerCase()) {
                LOGOUT -> logout()
                UPLOAD -> return UploadView(alias)
                START -> return showMenuView()
                EXIT -> System.exit(0)
                else -> {
                    renderMessage(Message("Unknown command"))
                }
            }
        }

        return this
    }

    private fun showMenuView(): View {
        return MenuView()
    }

    private fun renderDefaultMenu(): View {
        renderMenu(menuLoggedOut)

        val input = renderPrompt()

        input?.let {
            when (it.toLowerCase()) {
                LOGIN -> return LoginView(alias)
                START -> return showMenuView()
                EXIT -> System.exit(0)
                else -> renderMessage(Message("Unknown command"))
            }
        }

        return this
    }

    private fun checkLoginState(): Boolean? {
        var isLoggedIn: Boolean? = null

        runBlocking {
            client.isUserLoggedIn(object : ResultListener<Boolean> {
                override fun onSuccess(t: Boolean?) {
                    isLoggedIn = t ?: false
                }

                override fun onError(exception: D4LException?) {
                    exception?.printStackTrace()
                    isLoggedIn = false
                }
            })
        }

        return isLoggedIn
    }

    private fun logout() {
        runBlocking {
            client.logout(object : Callback {
                override fun onSuccess() {
                    renderMessage(Message("Successfully logged out!"))
                    renderDefaultMenu()
                }

                override fun onError(exception: D4LException?) {
                    renderMessage(Message("Logout failed: $exception"))
                    renderAuthorizedMenu()
                }
            })
        }
    }


    companion object {
        private const val LOGIN = "login"
        private const val LOGOUT = "logout"
        private const val UPLOAD = "upload"
        private const val EXIT = "exit"
        private const val START = "start"
    }

}
