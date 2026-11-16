package com.tieyoh.mobile

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

interface MerchantApi {

    @GET("api/merchants/mobile")
    suspend fun getMerchants(): List<MerchantDto>

    @GET("api/merchants/mobile/search")
    suspend fun searchMerchants(
        @Query("name") name: String? = null,
        @Query("zip") zip: String? = null,
        @Query("address") address: String? = null,
        @Query("merchantType") merchantType: String? = null
    ): List<MerchantDto>
}

object RetrofitClient {

    private const val BASE_URL = "http://10.0.2.2:8080/"

    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    val merchantApi: MerchantApi by lazy {
        retrofit.create(MerchantApi::class.java)
    }

    val authApi: AuthApi by lazy {
        retrofit.create(AuthApi::class.java)
    }
}
