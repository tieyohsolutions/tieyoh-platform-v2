package com.tieyoh.mobile

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

// senderId is trusted straight from the client, same as every other
// write in this app -- no session/auth context exists yet.
data class GiftRequest(
    val senderId: Long,
    val recipientId: Long,
    val merchantId: Long?,
    val amount: Double,
    // "CONTACTS" or "PRIVATE" from this screen today; the backend also
    // accepts "PUBLIC" but nothing in the UI offers it yet.
    val visibility: String
)

data class GiftResult(
    val id: Long,
    val senderId: Long,
    val recipientId: Long,
    val merchantId: Long?,
    val amount: Double,
    val currency: String?,
    val visibility: String?,
    val status: String?,
    val createdAt: String?
)

// Backs GiftReceivedScreen -- unlike GiftResult, this resolves sender/
// recipient/merchant display names server-side, since that screen is
// reached later (from a notification tap) with none of that context
// already in memory the way the sender's own confirmation has it right
// after sendGift() succeeds.
data class GiftDetailResult(
    val id: Long,
    val senderId: Long,
    val senderDisplayName: String?,
    val recipientId: Long,
    val recipientDisplayName: String?,
    val merchantId: Long?,
    val merchantName: String?,
    val amount: Double,
    val currency: String?,
    val visibility: String?,
    val status: String?,
    val createdAt: String?
)

// authorUserId is trusted straight from the client, same as the rest of
// this app. "body" is either a short typed message or a single emoji --
// the Thank Sender sheet decides which, the backend just stores text.
data class GiftCommentRequest(
    val authorUserId: Long,
    val body: String
)

data class GiftCommentResult(
    val id: Long,
    val giftId: Long,
    val authorUserId: Long,
    val authorDisplayName: String?,
    val body: String,
    val createdAt: String?
)

interface GiftApi {

    @POST("api/gifts")
    suspend fun sendGift(@Body request: GiftRequest): GiftResult

    @GET("api/gifts/{id}")
    suspend fun getGift(@Path("id") id: Long): GiftDetailResult

    @POST("api/gifts/{giftId}/comments")
    suspend fun addComment(@Path("giftId") giftId: Long, @Body request: GiftCommentRequest): GiftCommentResult

    @GET("api/gifts/{giftId}/comments")
    suspend fun getComments(@Path("giftId") giftId: Long): List<GiftCommentResult>
}
