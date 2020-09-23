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

package care.data4life.sdk.sample.presentation.login

import care.data4life.sdk.Data4LifeClient
import care.data4life.sdk.sample.presentation.BaseView
import care.data4life.sdk.sample.presentation.View
import care.data4life.sdk.sample.presentation.data.Message
import care.data4life.sdk.sample.presentation.main.SingleMainView
import org.koin.core.KoinComponent
import org.koin.core.inject
import org.koin.core.parameter.parametersOf

class LoginView(private val alias: String) : BaseView(), KoinComponent {


    private val client: Data4LifeClient by inject { parametersOf(alias) }
    override val type: String = "login"


    override fun renderContent(): View {
        renderMessage(Message("Fetching the Authorization URL..."))

        val authUrl = client.authorizationUrl

        renderMessage(Message("Got the Authorization URL!"))

        renderMessage(Message(authUrl))

        renderMessage(Message("Please open this url in your browser and authorize"))
        renderEmptyLine()
        renderEmptyLine()

        renderMessage(Message("And paste the received callbackUrl here"))
        val callbackUrl = renderPrompt()

        val authorized = client.finishLogin(callbackUrl)

        if (authorized) {
            renderMessage(Message("You're successfully authorized!"))
        } else {
            renderMessage(Message("Authorization failed! Please try again."))
        }

        return SingleMainView(alias)
    }

}
