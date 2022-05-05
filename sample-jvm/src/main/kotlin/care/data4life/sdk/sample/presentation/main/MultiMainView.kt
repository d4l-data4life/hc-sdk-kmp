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
import care.data4life.sdk.sample.presentation.BaseView
import care.data4life.sdk.sample.presentation.View
import care.data4life.sdk.sample.presentation.data.Menu
import care.data4life.sdk.sample.presentation.data.MenuEntry
import care.data4life.sdk.sample.presentation.data.Message
import care.data4life.sdk.sample.presentation.login.FinishLoginView
import care.data4life.sdk.sample.presentation.login.StartLogin
import care.data4life.sdk.sample.presentation.menu.MenuView
import care.data4life.sdk.sample.presentation.upload.UploadView
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import org.koin.core.parameter.parametersOf

class MultiMainView : BaseView(), KoinComponent {

    override fun renderContent(): View {
        renderMessage(Message("Please enter the user alias for the next action"))
        val alias = renderPrompt() ?: return MultiMainView()

        renderMenu(menu)
        val input = renderPrompt()

        input?.let {
            when (it.toLowerCase()) {
                LOGIN -> return StartLogin(alias)
                FINISH_LOGIN.toLowerCase() -> return FinishLoginView(alias)
                LOGOUT -> logout(alias)
                UPLOAD -> return UploadView(alias, true)
                ENTRY_START -> return MenuView()
                EXIT -> System.exit(0)
                else -> renderMessage(Message("Unknown command $it"))
            }
        }
        return MultiMainView()
    }

    private fun logout(alias: String) {
        val client: Data4LifeClient = get { parametersOf(alias) }

        client.logout(object : Callback {
            override fun onSuccess() {
                renderMessage(Message("$alias was successfully logged out."))
            }

            override fun onError(exception: D4LException) {
                renderMessage(Message("Failed to logout $alias"))
            }
        })
    }

    override val type: String = "Multi User Main"

    private val menu = Menu(
        listOf(
            MenuEntry(LOGIN),
            MenuEntry(FINISH_LOGIN),
            MenuEntry(LOGOUT),
            MenuEntry(UPLOAD),
            MenuEntry(START),
            MenuEntry(EXIT)
        )
    )

    companion object {
        private const val LOGIN = "login"
        private const val FINISH_LOGIN = "finishLogin"
        private const val LOGOUT = "logout"
        private const val UPLOAD = "upload"
        private const val EXIT = "exit"
        private const val START = "back to start menu (start)"
        private const val ENTRY_START = "start"
    }
}
