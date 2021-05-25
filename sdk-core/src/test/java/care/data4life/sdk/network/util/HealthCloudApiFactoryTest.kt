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
import care.data4life.sdk.network.typeadapter.EncryptedKeyTypeAdapter
import com.squareup.moshi.Moshi
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkConstructor
import io.mockk.unmockkStatic
import io.mockk.verify
import okhttp3.OkHttpClient
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.moshi.MoshiConverterFactory
import kotlin.test.assertSame
import kotlin.test.assertTrue

class HealthCloudApiFactoryTest {
    @Test
    fun `It fulfils HealthCloudApiFactory`() {
        val factory: Any = HealthCloudApiFactory

        assertTrue(factory is NetworkingContract.HealthCloudApiFactory)
    }

    @Test
    fun `Given getInstance is called, it creates a HealthCloudApi`() {
        // Given
        mockkConstructor(Moshi.Builder::class)
        mockkConstructor(Retrofit.Builder::class)
        mockkStatic(RxJava2CallAdapterFactory::class)
        mockkStatic(MoshiConverterFactory::class)

        val platform = "NVIP"
        val url = "https://notimportant.com"
        val client: OkHttpClient = mockk()
        val environment: NetworkingContract.Environment = mockk()
        val service: HealthCloudApi = mockk()

        val retrofitBuilder: Retrofit.Builder = mockk()
        val retrofit: Retrofit = mockk()
        val moshiBuilder: Moshi.Builder = mockk()
        val moshi: Moshi = mockk()
        val rxJAdapterFactory: RxJava2CallAdapterFactory = mockk()
        val moshiConverter: MoshiConverterFactory = mockk()

        every { environment.getApiBaseURL(platform) } returns url

        val slot = slot<Any>()

        every {
            anyConstructed<Moshi.Builder>()
                .add(capture(slot))
        } answers {
            if (slot.captured is EncryptedKeyTypeAdapter) {
                moshiBuilder
            } else {
                throw RuntimeException("Moshi was not set up properly")
            }
        }
        every { moshiBuilder.build() } returns moshi

        every { MoshiConverterFactory.create(moshi) } returns moshiConverter
        every { RxJava2CallAdapterFactory.create() } returns rxJAdapterFactory

        every {
            anyConstructed<Retrofit.Builder>()
                .addCallAdapterFactory(rxJAdapterFactory)
        } returns retrofitBuilder
        every {
            retrofitBuilder.addConverterFactory(moshiConverter)
        } returns retrofitBuilder
        every {
            retrofitBuilder.client(client)
        } returns retrofitBuilder
        every {
            retrofitBuilder.baseUrl(url)
        } returns retrofitBuilder
        every {
            retrofitBuilder.build()
        } returns retrofit
        every { retrofit.create(HealthCloudApi::class.java) } returns service

        // When
        val actual = HealthCloudApiFactory.getInstance(client, platform, environment)

        // Then
        assertSame(
            actual = actual,
            expected = service
        )

        verify(exactly = 1) {
            anyConstructed<Retrofit.Builder>()
                .addCallAdapterFactory(rxJAdapterFactory)
        }
        verify(exactly = 1) {
            retrofitBuilder.addConverterFactory(moshiConverter)
        }
        verify(exactly = 1) {
            retrofitBuilder.client(client)
        }
        verify(exactly = 1) {
            retrofitBuilder.baseUrl(url)
        }
        verify(exactly = 1) { retrofit.create(HealthCloudApi::class.java) }

        unmockkConstructor(Moshi.Builder::class)
        unmockkConstructor(Retrofit.Builder::class)
        unmockkStatic(RxJava2CallAdapterFactory::class)
        unmockkStatic(MoshiConverterFactory::class)
    }
}
