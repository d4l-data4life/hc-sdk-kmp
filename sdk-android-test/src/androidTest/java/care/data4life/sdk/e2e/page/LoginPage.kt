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

import android.webkit.WebView
import android.widget.Button
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiScrollable
import androidx.test.uiautomator.UiSelector
import androidx.test.uiautomator.Until
import care.data4life.sdk.e2e.util.Auth2FAHelper
import care.data4life.sdk.e2e.util.User
import java.lang.Thread.sleep

class LoginPage : BasePage() {

    override fun waitForPage() {
        device.wait(Until.hasObject(By.pkg("com.android.chrome").depth(0)), TIMEOUT)
    }

    fun doLogin(user: User): HomePage {
        sleep(TIMEOUT_SHORT)

        // Chrome
        dismissChromeWelcomeScreen()

        // AuthApp
        dismissAuthAppCookie()

        // Page login/register
        ensurePageLoaded()
        clickButton(authAppButtonLogin, true)

        // Page enter email/password
        ensurePageLoaded()
        enterText(authAppInputEmail, user.email, true)
        enterText(authAppInputPassword, user.password, true)
        clickButton(authAppButtonSubmitLogin, true)

        // Chrome
        sleep(TIMEOUT_SHORT)
        dismissChromeSavePassword()

        // AuthApp

        // Page Phone number
        // FIXME is not supported as ids missing on the page

        // Page 2FA
        ensurePageLoaded()
        val code = Auth2FAHelper.fetchCurrent2faCode(user.phoneNumber)
        enterText(authAppInputPinV2, code, true)
        unselectRememberDeviceCheckbox()
        clickButton(authAppButtonSmsCodeSubmit, true)

        // In case 2FA code was wrong, retry 3 times
        repeat(3) {
            resendCode(user.phoneNumber)
        }

        sleep(TIMEOUT_SHORT)

        return HomePage()
    }

    // Chrome
    private fun dismissChromeWelcomeScreen() {
        // dismiss Chrome welcome screen
        val accept = device.findObject(
            UiSelector().resourceId("com.android.chrome:id/terms_accept")
        )
        if (accept.exists()) {
            accept.click()
            device.waitForIdle()
        }
        val noThanks = device.findObject(
            UiSelector().resourceId("com.android.chrome:id/negative_button")
        )
        if (noThanks.exists()) {
            noThanks.click()
            device.waitForIdle()
        }
    }

    private fun dismissChromeSavePassword() {
        dismissChromeInfobar()
    }

    private fun dismissChromeInfobar() {
        val closeNotifyPopup = device.findObject(
            UiSelector().resourceId("com.android.chrome:id/infobar_close_button")
        )
        closeNotifyPopup.waitForExists(TIMEOUT_SHORT)
        if (closeNotifyPopup.exists()) {
            closeNotifyPopup.click()
            device.waitForIdle()
        }
    }

    // Auth App

    // FIXME cookie consent needs a stable ID
    private fun dismissAuthAppCookie() {
        val acceptCookies = device.findObject(
            UiSelector().instance(0)
                .className(Button::class.java)
                .descriptionMatches("(Accept|Akzeptieren)")
        )
        acceptCookies.waitForExists(TIMEOUT_SHORT)
        if (acceptCookies.exists()) {
            acceptCookies.click()
            device.waitForIdle()
        }
    }

    private fun unselectRememberDeviceCheckbox() {
        val rememberCheckBox = device.findObject(
            UiSelector().resourceId("d4l-checkbox-remember")
        )
        rememberCheckBox.waitForExists(TIMEOUT_SHORT)
        if (rememberCheckBox.exists() && rememberCheckBox.isChecked) {
            rememberCheckBox.click()
        }
    }

    private fun resendCode(phoneNumber: String) {
        val dismissButton = device.findObject(
            UiSelector().className("android.widget.Button").textMatches("(DISMISS)")
        )
        dismissButton.waitForExists(TIMEOUT_SHORT)
        if (dismissButton.exists()) {
            dismissButton.click()

            val resend = device.findObject(
                UiSelector().resourceId("d4l-button-resend-sms-code")
            )
            resend.click()

            sleep(TIMEOUT_SHORT)
            val code = Auth2FAHelper.fetchCurrent2faCode(phoneNumber)
            enterText(authAppInputPinV2, code, true)
        }
    }

    // Helper

    private fun ensurePageLoaded() {
        scrollToTop(1)
        scrollToBottom(1)
    }

    private fun scrollToBottom(maxSwipes: Int) {
        UiScrollable(UiSelector().className(WebView::class.java))
            .scrollToEnd(maxSwipes)
    }

    private fun scrollToTop(maxSwipes: Int) {
        UiScrollable(UiSelector().className(WebView::class.java))
            .scrollToBeginning(maxSwipes)
    }

    private fun clickButton(resourceId: String, required: Boolean?) {
        val button = device.findObject(UiSelector().instance(0).resourceId(resourceId))
        button.waitForExists(TIMEOUT_SHORT)
        if (button.exists()) {
            button.click()
            device.waitForIdle()
        } else {
            if (required != null && required) throw IllegalStateException("Button not found: $resourceId")
        }
    }

    private fun enterText(resourceId: String, text: String, required: Boolean?) {
        val input = device.findObject(UiSelector().instance(0).resourceId(resourceId))
        input.waitForExists(TIMEOUT_SHORT)
        if (input.exists()) {
            input.text = text
            device.waitForIdle()
        } else {
            if (required != null && required) throw IllegalStateException("Input not found: $resourceId")
        }
    }

    companion object {
        // Page register/login
        const val authAppButtonLogin = "d4l-button-login"

        // Page enter email/password
        const val authAppInputEmail = "d4l-email"
        const val authAppInputPassword = "d4l-password"
        const val authAppButtonSubmitLogin = "d4l-button-submit-login"

        // Page phone number
        const val authAppInputPhoneCountryCode = "d4l-code"
        const val authAppInputPhoneNumber = "d4l-phone-number"
        const val authAppButtonPhoneNumber = "d4l-button-submit-login"

        // Page 2FA
        const val authAppInputPinV2 = "d4l-pin"
        const val authAppButtonSmsCodeSubmit = "d4l-button-submit-sms-code"
    }
}
