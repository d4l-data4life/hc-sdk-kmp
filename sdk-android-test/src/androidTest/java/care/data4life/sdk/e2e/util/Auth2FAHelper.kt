/*
 * Copyright (c) 2020 D4L data4life gGmbH - All rights reserved.
 */

package care.data4life.sdk.e2e.util

import care.data4life.sdk.e2e.util.TwillioService.Companion.WRONG_PIN
import com.google.gson.annotations.SerializedName
import okhttp3.Credentials
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import org.threeten.bp.LocalDate
import org.threeten.bp.format.DateTimeFormatter
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import java.io.IOException
import java.util.Locale

interface TwillioService {

    @GET("2010-04-01/Accounts/{account_sid}/Messages.json")
    fun get2FACode(
        @Path("account_sid") accountSid: String,
        @Query("dateSent") dateSent: String,
        @Query("to") phoneNumber: String,
        @Query("pageSize") page: Int
    ): Call<ListMessage>

    companion object {
        const val BASE_URL = "https://api.twilio.com"
        const val WRONG_PIN = "000000"
    }
}

class Message {
    @SerializedName("body")
    var body: String? = null

    @SerializedName("error_code")
    var error_code: String? = null

    @SerializedName("from")
    var from: String? = null

    @SerializedName("to")
    var to: String? = null
}

class ListMessage {
    @SerializedName("messages")
    var messages: List<Message>? = null

    @SerializedName("first_page_uri")
    var first_page_uri: String? = null

    @SerializedName("end")
    var end: Int? = null
}

private class BasicAuthInterceptor(user: String, password: String) : Interceptor {

    private val credentials: String = Credentials.basic(user, password)

    @Throws(IOException::class)
    override fun intercept(chain: Interceptor.Chain): okhttp3.Response {
        val request = chain.request()
        val authenticatedRequest = request.newBuilder()
            .header("Authorization", credentials).build()
        return chain.proceed(authenticatedRequest)
    }
}

object Auth2FAHelper {

    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.US)
    private val config = TestConfigLoader.load().twillio

    private fun initTwillioService(): TwillioService {

        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(BasicAuthInterceptor(config.authSid, config.authToken))
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl(TwillioService.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        return retrofit.create(TwillioService::class.java)
    }

    private fun fetchLatest2FACode(phoneNumber: String, date: String): ListMessage? {
        val call: Call<ListMessage> =
            initTwillioService().get2FACode(config.accountSid, date, phoneNumber, 1)
        return call.execute().body()
    }

    fun fetchCurrent2faCode(phoneNumber: String): String {
        val date = dateFormatter.format(LocalDate.now())
        val message = fetchLatest2FACode(phoneNumber, date)?.messages?.get(0)?.body
        return message?.substring(
            startIndex = message.length - 6,
            endIndex = message.length
        ) ?: WRONG_PIN
    }
}
