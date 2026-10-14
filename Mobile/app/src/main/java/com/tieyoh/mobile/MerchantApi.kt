package com.tieyoh.mobile

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

interface MerchantApi {

    @GET("api/merchants/mobile")
    suspend fun getMerchants(): List<MerchantDto>

    // q is a single combined query OR'd server-side across the merchant's
    // name and every location field (address, city, state, ZIP) -- see
    // MerchantRepository.searchEligibleMerchants. merchantType is a
    // separate, independent category filter, still ANDed in when present.
    @GET("api/merchants/mobile/search")
    suspend fun searchMerchants(
        @Query("q") q: String? = null,
        @Query("merchantType") merchantType: String? = null
    ): List<MerchantDto>

    // Same single combined query as searchMerchants above, but only ever
    // returns merchants that have signed up to accept scanned Tieyoh
    // payment QR codes (MerchantRepository.searchEligiblePartneredMerchants)
    // -- backs "Find Partnered Merchants" (PartneredMerchantSearchScreen.kt).
    @GET("api/merchants/mobile/partnered/search")
    suspend fun searchPartneredMerchants(
        @Query("q") q: String? = null,
        @Query("merchantType") merchantType: String? = null
    ): List<MerchantDto>

    @GET("api/merchants/mobile/nearby")
    suspend fun getNearbyMerchants(
        @Query("lat") lat: Double,
        @Query("lng") lng: Double,
        @Query("radiusMiles") radiusMiles: Double = 25.0
    ): List<MerchantDto>

    // Same "near me" search as getNearbyMerchants above, restricted to
    // partnered merchants -- backs the Map tab on "Find Partnered
    // Merchants".
    @GET("api/merchants/mobile/partnered/nearby")
    suspend fun getNearbyPartneredMerchants(
        @Query("lat") lat: Double,
        @Query("lng") lng: Double,
        @Query("radiusMiles") radiusMiles: Double = 25.0
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

    val userApi: UserApi by lazy {
        retrofit.create(UserApi::class.java)
    }

    val friendApi: FriendApi by lazy {
        retrofit.create(FriendApi::class.java)
    }

    val connectionRequestApi: ConnectionRequestApi by lazy {
        retrofit.create(ConnectionRequestApi::class.java)
    }

    val notificationApi: NotificationApi by lazy {
        retrofit.create(NotificationApi::class.java)
    }

    val invitationApi: InvitationApi by lazy {
        retrofit.create(InvitationApi::class.java)
    }

    val feedApi: FeedApi by lazy {
        retrofit.create(FeedApi::class.java)
    }

    val giftApi: GiftApi by lazy {
        retrofit.create(GiftApi::class.java)
    }

    val googleWalletApi: GoogleWalletApi by lazy {
        retrofit.create(GoogleWalletApi::class.java)
    }
}
