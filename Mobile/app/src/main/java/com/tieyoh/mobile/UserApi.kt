package com.tieyoh.mobile

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

data class ProfileSetupRequest(
    val tieyohId: String,
    // Optional now -- the new onboarding flow persists phone at signup
    // instead of asking again here. Still present for Edit Profile.
    val phone: String? = null,
    val address: String?,
    val gender: String,
    // Both optional -- see the backend ProfileSetupRequest for why
    // birthday isn't a hard-required field on this screen.
    val birthMonth: Int? = null,
    val birthDay: Int? = null,
    // Comma-separated labels from HolidayOptions -- also optional.
    val holidaysCelebrated: String? = null,
    // Optional -- only Edit Profile ever sends this (leave null to keep
    // the current email). Signup sets email separately, through AuthApi.
    val email: String? = null
)

// Backs the Edit Profile screen: everything ProfileSetupRequest can set,
// read back so that screen can pre-fill -- including tieyohId/gender,
// which still have to be resent unchanged on save since the PATCH
// endpoint treats them as required, not partial-update fields. Also
// carries the two Privacy toggles so Edit Profile's Privacy section can
// pre-fill those from the same load.
data class UserProfileResult(
    val email: String?,
    val tieyohId: String?,
    val phone: String?,
    val address: String?,
    val gender: String?,
    val birthMonth: Int?,
    val birthDay: Int?,
    val holidaysCelebrated: String?,
    val autoAcceptCircleInvites: Boolean?,
    val shareGiftingWithCircle: Boolean?
)

data class MerchantSelectionRequest(
    val merchantIds: List<Long>
)

data class IdAvailabilityResponse(
    val available: Boolean
)

data class UserSearchResult(
    val id: Long,
    val tieyohId: String?,
    val displayName: String,
    val maskedPhone: String?,
    val maskedEmail: String?,
    val profilePictureBase64: String? = null
)

data class ProfilePictureRequest(
    val imageBase64: String?
)

data class UserSettingsRequest(
    val autoAcceptCircleInvites: Boolean? = null,
    val shareGiftingWithCircle: Boolean? = null
)

interface UserApi {

    @GET("api/users/check-id")
    suspend fun checkIdAvailable(@Query("id") id: String): IdAvailabilityResponse

    @PATCH("api/users/{id}/profile")
    suspend fun saveProfile(
        @Path("id") userId: Long,
        @Body request: ProfileSetupRequest
    ): AuthResponse

    @POST("api/users/{id}/merchants")
    suspend fun saveMerchantSelections(
        @Path("id") userId: Long,
        @Body request: MerchantSelectionRequest
    )

    // Backs the invite screen's live search bar -- q can be a fragment of
    // an @t id, a phone number, or an email; excludeUserId keeps the
    // signed-in user from matching themselves.
    @GET("api/users/search")
    suspend fun searchUsers(
        @Query("q") q: String,
        @Query("excludeUserId") excludeUserId: Long
    ): List<UserSearchResult>

    @PATCH("api/users/{id}/profile-picture")
    suspend fun updateProfilePicture(
        @Path("id") userId: Long,
        @Body request: ProfilePictureRequest
    ): AuthResponse

    @PATCH("api/users/{id}/settings")
    suspend fun updateSettings(
        @Path("id") userId: Long,
        @Body request: UserSettingsRequest
    ): AuthResponse

    // Backs the Send Gift flow's "Their favorite places" step -- the
    // recipient's own saved wish-list merchants.
    @GET("api/users/{id}/merchants")
    suspend fun getMerchantSelections(@Path("id") userId: Long): List<MerchantDto>

    // Backs the Edit Profile screen's initial load.
    @GET("api/users/{id}/profile")
    suspend fun getProfile(@Path("id") userId: Long): UserProfileResult
}
