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

package care.data4life.sdk.e2e

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import androidx.test.rule.ActivityTestRule
import care.data4life.sdk.Data4LifeClient
import care.data4life.sdk.e2e.page.HomePage
import care.data4life.sdk.e2e.util.NetworkUtil
import care.data4life.sdk.e2e.util.TestConfigLoader
import care.data4life.sdk.helpers.stu3.FhirHelperConfig
import com.jakewharton.threetenabp.AndroidThreeTen
import org.junit.AfterClass
import org.junit.Assume
import org.junit.BeforeClass

open class BaseTestLogin {

    companion object {
        private val CLIENT_ID = "care.data4life.sdk.CLIENT_ID"
        private val DELIMTIER_CHAR = "#"
        private val PARTNER_ID_POS = 0

        private var isNetConnected: Boolean = false
        private val rule = ActivityTestRule(CrossSDKActivity::class.java, false, false)
        private lateinit var activity: CrossSDKActivity
        private lateinit var loginLogoutPage: HomePage

        @JvmStatic
        protected lateinit var client: Data4LifeClient // SUT

        @BeforeClass
        @JvmStatic
        fun suiteSetup() {
            isNetConnected = NetworkUtil.isOnline()
            Assume.assumeTrue("Internet connection required", isNetConnected)

            activity = rule.launchActivity(null)
            AndroidThreeTen.init(activity)
            Data4LifeClient.init(activity)
            FhirHelperConfig.init(getPartnerId(activity))

            val user = TestConfigLoader.load().user
            client = Data4LifeClient.getInstance()

            loginLogoutPage = HomePage()
                .isVisible()
                .openLoginPage()
                .doLogin(user)
                .isVisible()
        }

        private fun getPartnerId(ctx: Context): String {
            val applicationInfo: ApplicationInfo
            try {
                applicationInfo = ctx.getPackageManager()
                    .getApplicationInfo(ctx.getPackageName(), PackageManager.GET_META_DATA)
            } catch (e: PackageManager.NameNotFoundException) {
                throw IllegalStateException("Unable to retrieve clientId!")
            }

            return applicationInfo.metaData.get(CLIENT_ID).toString()
                .split(DELIMTIER_CHAR)[PARTNER_ID_POS]
        }

        @AfterClass
        @JvmStatic
        fun suiteCleanUp() {
            if (!isNetConnected) return

            loginLogoutPage
                .doLogout()
                .isVisible()

            activity.explicitFinish()
        }
    }
}
