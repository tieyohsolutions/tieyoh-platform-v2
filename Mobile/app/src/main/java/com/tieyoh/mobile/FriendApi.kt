package com.tieyoh.mobile

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

// Mirrors the backend's AddFriendRequest: set contactUserId when adding
// someone found via user search (already has a Tieyoh account), or leave
// it null and fill displayName + phone/email for a raw invite.
data class AddFriendRequest(
    val contactUserId: Long?,
    val displayName: String,
    val phone: String?,
    val email: String?
)

// A row in the signed-in user's own circle. status is "ACTIVE" (real
// Tieyoh account, full access) or "PENDING" (invited, not joined yet).
data class FriendResult(
    val id: Long,
    val contactUserId: Long?,
    val displayName: String,
    val tieyohId: String?,
    val phone: String?,
    val email: String?,
    val status: String,
    val profilePictureBase64: String?
)

// A person in the feed's network section: relationship is "DIRECT" (a real
// circle member -- tap to gift) or "SECOND_DEGREE" (a contact of one of
// your contacts -- tap to send a connection request).
// connectionRequestStatus is null, "PENDING_SENT", or "PENDING_RECEIVED".
data class FeedPersonResult(
    val userId: Long,
    val displayName: String,
    val tieyohId: String?,
    val profilePictureBase64: String?,
    val relationship: String,
    val connectionRequestStatus: String?
)

// One phone-book contact to check, and what came back. Mirrors the
// backend's ContactMatchBatchRequest/ContactMatchResult -- matchedUserId
// is null when this phone/email isn't a real (ACTIVE) Tieyoh account, and
// the client groups those under "invite" instead of "connect".
// mutualFriendsCount is only meaningful when matched.
data class ContactMatchBatchRequest(
    val contacts: List<InvitationRecipientRequest>
)

data class ContactMatchResult(
    val displayName: String,
    val phone: String?,
    val email: String?,
    val matchedUserId: Long?,
    val matchedTieyohId: String?,
    val matchedProfilePictureBase64: String?,
    val mutualFriendsCount: Int?
)

interface FriendApi {

    // Keyed by the owner's Tieyoh @t- id (e.g. "@t-thg"), not their
    // internal numeric account id -- see the backend's FriendConnection
    // for why. getNetwork (the feed's second-degree view) is unrelated and
    // still keyed by numeric id, unchanged.
    @GET("api/users/{ownerTieyohId}/friends")
    suspend fun getFriends(@Path("ownerTieyohId") ownerTieyohId: String): List<FriendResult>

    @POST("api/users/{ownerTieyohId}/friends")
    suspend fun addFriend(
        @Path("ownerTieyohId") ownerTieyohId: String,
        @Body request: AddFriendRequest
    ): FriendResult

    // Read-only classification, no side effects -- see the backend
    // controller's comment. Used by the "People you may know" onboarding
    // screen before the user has tapped Connect or Invite on anything.
    @POST("api/users/{ownerTieyohId}/friends/check-contacts")
    suspend fun checkContacts(
        @Path("ownerTieyohId") ownerTieyohId: String,
        @Body request: ContactMatchBatchRequest
    ): List<ContactMatchResult>

    @GET("api/users/{userId}/network")
    suspend fun getNetwork(@Path("userId") userId: Long): List<FeedPersonResult>
}
