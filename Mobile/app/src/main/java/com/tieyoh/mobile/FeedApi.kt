package com.tieyoh.mobile

import retrofit2.http.GET
import retrofit2.http.Query

// Mirrors the backend's PersonSummary. Never used as an identifier by
// itself on this side either -- userId is what SendGiftScreen and any
// future "gift this person" action actually key off of.
data class FeedPerson(
    val userId: Long,
    val displayName: String,
    val tieyohId: String?,
    val profilePictureBase64: String?
)

data class FeedGiftInfo(
    val amount: Double,
    val currency: String?,
    // The gift's "thank you" replies -- see GiftCommentResult in
    // GiftApi.kt. Rendered inline under the gift sentence by
    // GiftActivityCard so a reply shows up on the Feed itself, not just
    // as a notification to the sender.
    val comments: List<GiftCommentResult>? = null
)

data class FeedMerchantSummary(
    val merchantId: Long?,
    val tmid: String?,
    val brandName: String?,
    val logoUrl: String?,
    val category: String?
)

// One Feed entry. Only the fields relevant to `eventType` are non-null --
// see FeedItemResult on the backend for the full explanation. New event
// types show up here as new nullable fields, same as there.
data class FeedItem(
    val eventType: String,
    val eventId: String,
    // Epoch millis -- see FeedItemResult on the backend for why this
    // isn't a formatted date string.
    val occurredAt: Long? = null,
    // GIFT_ACTIVITY
    val sender: FeedPerson? = null,
    val recipient: FeedPerson? = null,
    val gift: FeedGiftInfo? = null,
    val merchant: FeedMerchantSummary? = null,
    // UPCOMING_BIRTHDAY
    val person: FeedPerson? = null,
    val daysUntil: Int? = null,
    // UPCOMING_HOLIDAY
    val holidayName: String? = null,
    val displayMessage: String? = null
)

data class FeedResponse(
    val items: List<FeedItem>,
    val page: Int,
    val size: Int,
    val hasMore: Boolean
)

interface FeedApi {

    // viewerId is a plain query param -- there's no session/auth context
    // in this backend yet, same trust model as every other endpoint.
    @GET("api/feed")
    suspend fun getFeed(
        @Query("viewerId") viewerId: Long,
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 20
    ): FeedResponse
}
