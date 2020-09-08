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

import androidx.test.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import java.util.regex.Pattern

abstract class BasePage {

    val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())!!

    init {
        this.waitForPage()
    }

    abstract fun waitForPage()

    fun waitByResource(resourceName: String) {
        device.wait(Until.hasObject(By.res(resourceName)), TIMEOUT)
    }

    fun waitByText(text: String) {
        device.wait(Until.hasObject(By.text(text)), TIMEOUT)
    }

    fun waitByRegex(pattern: Pattern) {
        device.wait(Until.hasObject(By.text(pattern)), TIMEOUT)
    }

    companion object {
        const val TIMEOUT = 1000 * 60L

        const val TIMEOUT_SHORT = 1000 * 5L
    }
}
