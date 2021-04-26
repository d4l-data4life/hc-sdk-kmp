/*
 * Copyright (c) 2021 D4L data4life gGmbH / All rights reserved.
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

package care.data4life.sdk.network

import okhttp3.CertificatePinner
import okhttp3.Response

interface NetworkingContract {
    interface Service

    interface Interceptor : okhttp3.Interceptor {
        override fun intercept(chain: okhttp3.Interceptor.Chain): Response
    }

    interface CertificatePinnerFactory {
        fun getInstance(baseUrl: String, pin: String): CertificatePinner
    }

    interface Environment {
        fun getApiBaseURL(platform: String): String
        fun getCertificatePin(platform: String): String
    }

    interface EnvironmentFactory {
        fun fromName(name: String?): Environment
    }

    companion object {
        const val REQUEST_TIMEOUT = 2
        const val HEADER_ALIAS = "gc_alias"
        const val HEADER_ACCESS_TOKEN = "access_token"
        const val HEADER_AUTHORIZATION = "Authorization"
        const val HEADER_BASIC_AUTH = "basic_auth"
        const val HEADER_GC_SDK_VERSION = "GC-SDK-Version"
        const val HEADER_TOTAL_COUNT = "x-total-count"
        const val PARAM_FILE_NUMBER = "file_number"
        const val PARAM_TEK = "tek"
        const val FORMAT_BEARER_TOKEN = "Bearer %s"
        const val FORMAT_BASIC_AUTH = "Basic %s"
        const val FORMAT_ANDROID_CLIENT_NAME = "Android %s"
        const val MEDIA_TYPE_OCTET_STREAM = "application/octet-stream"
        const val HTTP_401_UNAUTHORIZED = 401
        const val AUTHORIZATION_WITH_ACCESS_TOKEN = "Authorization: access_token"
        const val AUTHORIZATION_WITH_BASIC_AUTH = "Authorization: basic_auth"
        const val HEADER_CONTENT_TYPE_OCTET_STREAM = "content-type: application/octet-stream"
    }
}
