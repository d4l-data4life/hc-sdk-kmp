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

package care.data4life.sdk.network.util

import care.data4life.sdk.network.HealthCloudApi
import care.data4life.sdk.network.NetworkingContract
import care.data4life.sdk.network.NetworkingInternalContract
import care.data4life.sdk.network.typeadapter.EncryptedKeyTypeAdapter
import com.squareup.moshi.Moshi
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.moshi.MoshiConverterFactory

object HealthCloudApiFactory :
    NetworkingInternalContract.HealthCloudApiFactory {
    private fun buildMoshi(): Moshi {
        return Moshi.Builder()
            .add(EncryptedKeyTypeAdapter())
            .build()
    }

    override fun getInstance(
        client: OkHttpClient,
        platform: String,
        environment: NetworkingContract.Environment
    ): HealthCloudApi {
        return Retrofit.Builder()
            .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
            .addConverterFactory(MoshiConverterFactory.create(buildMoshi()))
            .baseUrl(environment.getApiBaseURL(platform))
            .client(client)
            .build()
            .create(HealthCloudApi::class.java)
    }
}
