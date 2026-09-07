package com.tieyoh.mobile

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

// One contact the user explicitly picked to invite. Mirrors the backend's
// InvitationRecipientRequest -- only ever built from contacts the person
// checked in InviteFriendsScreen, never a whole address book dump.
data class InvitationRecipientRequest(
    val displayName: String,
    val email: String?,
    val phone: String?
)

// See InvitationBatchRequest.java for why inviterTieyohId travels as a
// plain field rather than being derived from a session: this backend
// doesn't have session auth yet, and every other endpoint already works
// this way. It's the inviter's Tieyoh @t- id (e.g. "@t-thg"), not their
// internal numeric account id.
data class InvitationBatchRequest(
    val inviterTieyohId: String,
    val recipients: List<InvitationRecipientRequest>
)

// inviteUrl is null when the recipient's phone/email already belongs to a
// real Tieyoh account -- they were linked straight into the circle instead
// of getting a link, so there's nothing to share for them.
data class InvitationResult(
    val displayName: String,
    val inviteUrl: String?
)

data class InvitationBatchResponse(
    val invitations: List<InvitationResult>
)

data class InvitationStatusResult(
    val valid: Boolean,
    val status: String,
    val recipientName: String?,
    val inviterUserId: Long?
)

interface InvitationApi {

    @POST("api/invitations/batch")
    suspend fun createBatch(@Body request: InvitationBatchRequest): InvitationBatchResponse

    @GET("api/invitations/{token}")
    suspend fun getStatus(@Path("token") token: String): InvitationStatusResult
}
