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

package care.data4life.sdk.sample.presentation.menu

import care.data4life.sdk.sample.presentation.BaseView
import care.data4life.sdk.sample.presentation.View
import care.data4life.sdk.sample.presentation.data.Menu
import care.data4life.sdk.sample.presentation.data.MenuEntry
import care.data4life.sdk.sample.presentation.data.Message
import care.data4life.sdk.sample.presentation.main.MultiMainView
import care.data4life.sdk.sample.presentation.main.SingleMainView

class MenuView : BaseView() {

    override val type: String = "menu"

    private val menu = Menu(
        listOf(
            MenuEntry(SINGLE_USER),
            MenuEntry(MULTI_USER),
            MenuEntry(EXIT)
        )
    )

    override fun renderContent(): View {
        renderMenu(menu)

        val input = renderPrompt()

        input?.let {
            when (it.toLowerCase()) {
                SINGLE_USER -> return SingleMainView(SINGLE_USER_ALIAS)
                MULTI_USER -> return MultiMainView()
                EXIT -> System.exit(0)
                else -> renderMessage(Message("Unknown command"))
            }
        }

        return MenuView()
    }

    companion object {
        const val SINGLE_USER = "single"
        const val MULTI_USER = "multi"
        const val EXIT = "exit"

        const val SINGLE_USER_ALIAS = "singleUserAlias"
    }
}
