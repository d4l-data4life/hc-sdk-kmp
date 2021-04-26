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

package care.data4life.auth

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class LoginActivity : AppCompatActivity() {
    private var isAuthIntentCalled = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.d4l_auth_login_activity)
    }

    override fun onResume() {
        super.onResume()
        if (!isAuthIntentCalled) {
            isAuthIntentCalled = true
            val authorizationIntent: Intent? = intent.extras?.getParcelable(AUTHORIZATION_INTENT)
            startActivityForResult(authorizationIntent, D4L_AUTH_CODE)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == D4L_AUTH_CODE) {
            if (resultCode == Activity.RESULT_CANCELED) {
                if (authorizationListener == null) return

                val authException = net.openid.appauth.AuthorizationException.fromIntent(data)
                authorizationListener =
                    if (authException != null && net.openid.appauth.AuthorizationException.GeneralErrors.USER_CANCELED_AUTH_FLOW == authException) {
                        authorizationListener?.onError(
                            AuthorizationException.Canceled(),
                            loginFinishedCbk
                        )
                        null
                    } else {
                        authorizationListener?.onError(
                            AuthorizationException.FailedToLogin(),
                            loginFinishedCbk
                        )
                        null
                    }
            } else if (resultCode == Activity.RESULT_OK) {
                if (authorizationListener == null) throw RuntimeException("authorizationListener not set!")

                authorizationListener?.onSuccess(data, loginFinishedCbk)
                authorizationListener = null
            }
        }
    }

    private val loginFinishedCbk = object : AuthorizationService.Callback {
        override fun onSuccess() {
            setResult(Activity.RESULT_OK)
            finish()
        }

        override fun onError(error: Throwable) {
            val result = Intent()

            if (error is AuthorizationException.Canceled) {
                result.putExtra(KEY_CANCELED, error.message)
            } else {
                result.putExtra(KEY_ERROR, error.message)
            }
            setResult(Activity.RESULT_CANCELED, result)

            finish()
        }
    }

    companion object {
        private const val D4L_AUTH_CODE = 9906
        const val KEY_ERROR = "error"
        const val KEY_CANCELED = "canceled"
        internal const val AUTHORIZATION_INTENT = "AUTHORIZATION_INTENT"
        internal var authorizationListener: AuthorizationService.AuthorizationListener? = null
    }
}
