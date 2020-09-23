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

package care.data4life.sdk.sample.presentation

import care.data4life.sdk.sample.presentation.data.Menu
import care.data4life.sdk.sample.presentation.data.Message
import com.github.ajalt.clikt.output.TermUi

abstract class BaseView : View {

    override fun render(): View {
        renderTitle()

        return renderContent()
    }

    abstract fun renderContent(): View


    fun renderMenu(menu: Menu) {
        renderEmptyLine()
        TermUi.echo("Options:")

        menu.entries.forEach {
            TermUi.echo("- ${it.title}")
        }


    }

    fun renderTitle() {
        renderEmptyLine()

        TermUi.echo(type.toUpperCase())
    }

    fun renderMessage(message: Message) {
        renderEmptyLine()

        TermUi.echo(message.text)
    }

    fun renderPrompt(): String? {
        renderEmptyLine()

        return TermUi.prompt(text = "")
    }


    fun renderEmptyLine() {
        TermUi.echo("")
    }
}
