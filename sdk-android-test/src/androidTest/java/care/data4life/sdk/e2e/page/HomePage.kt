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

package care.data4life.sdk.e2e.page

import care.data4life.sdk.e2e.R
import com.agoda.kakao.KButton
import com.agoda.kakao.KView
import com.agoda.kakao.Screen

class HomePage : BasePage() {

    private val screen = LoginLogoutScreen()

    override fun waitForPage() {
        waitByResource("care.data4life.sdk.test:id/rootCL")
    }

    fun openLoginPage(): LoginPage {
        screen.loginLogoutBtn { click() }

        return LoginPage()
    }

    fun doLogout(): HomePage {
        screen.loginLogoutBtn { click() }

        return this
    }

    fun isVisible(): HomePage {
        screen.root { isDisplayed() }

        return this
    }

    class LoginLogoutScreen : Screen<LoginLogoutScreen>() {
        val root = KView { withId(R.id.rootCL) }

        val loginLogoutBtn = KButton { withId(R.id.loginLogoutBtn) }
    }
}
