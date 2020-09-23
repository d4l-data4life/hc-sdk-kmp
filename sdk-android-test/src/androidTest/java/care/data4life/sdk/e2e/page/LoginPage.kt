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

import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiScrollable
import androidx.test.uiautomator.UiSelector
import androidx.test.uiautomator.Until
import java.lang.Thread.sleep
import java.util.regex.Pattern

class LoginPage : BasePage() {

    override fun waitForPage() {
        device.wait(Until.hasObject(By.pkg("com.android.chrome").depth(0)), TIMEOUT)
    }


    fun doLogin(email: String, password: String): HomePage {
        sleep(TIMEOUT_SHORT)

        val selector = UiSelector()

        // dismiss Chrome welcome screen
        val accept = device.findObject(selector.resourceId("com.android.chrome:id/terms_accept"))
        if (accept.exists()) {
            accept.click()
        }
        val noThanks = device.findObject(selector.resourceId("com.android.chrome:id/negative_button"))
        if (noThanks.exists()) {
            noThanks.click()
        }

        device.waitForIdle()
        waitByRegex(Pattern.compile("(EMAIL|E-MAIL-ADRESSE)"))

        // show login tab
        val loginTab = device.findObject(selector.className("android.view.View").textMatches("(Login|Anmelden)"))
        if (loginTab.exists()) {
            loginTab.click()
            device.waitForIdle()
        }

        // accept cookies
        val acceptCookies = device.findObject(selector.className("android.widget.Button").textMatches("(Akzeptieren|Accept)"))
        if (acceptCookies.exists()) {
            acceptCookies.click()
            device.waitForIdle()
        }

        // close translate popup message
        val closeTranslatePopup = device.findObject(selector.resourceId("com.android.chrome:id/infobar_close_button"))
        if (closeTranslatePopup.exists()) {
            closeTranslatePopup.click()
            device.waitForIdle()
        }

        // scroll to bottom
        val wv = UiScrollable(selector.classNameMatches("android.webkit.WebView"))
        wv.scrollForward()
        wv.scrollToEnd(10)

        // enter credentials and press submit button
        val emailInput = device.findObject(selector.resourceId("emailInput"))
        emailInput.text = email
        device.waitForIdle()

        val passwordInput = device.findObject(selector.resourceId("passwordInput"))
        passwordInput.text = password
        device.waitForIdle()

        val submit = device.findObject(selector.resourceId("loginButton"))
        submit.click()

        device.waitForIdle()
        device.wait(Until.hasObject(By.pkg("care.data4life.sdk.test").depth(0)), TIMEOUT)

        return HomePage()
    }

}
