package com.whycun.garlicapp.data.remote

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import java.util.concurrent.TimeUnit

interface GarlicApi {

    @GET("prices.json")
    suspend fun getPrices(): PriceResponse

    @GET("news.json")
    suspend fun getNews(): NewsResponse

    companion object {
        // 主URL（GitHub原始文件）
        private const val BASE_URL = "https://raw.githubusercontent.com/whycun/garlic-assistant/main/data/"

        // 备用URL（jsDelivr CDN）
        const val CDN_BASE = "https://cdn.jsdelivr.net/gh/whycun/garlic-assistant@main/"

        fun create(): GarlicApi {
            val logger = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BASIC
            }
            val client = OkHttpClient.Builder()
                .addInterceptor(logger)
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .build()

            return Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(GarlicApi::class.java)
        }
    }
}
