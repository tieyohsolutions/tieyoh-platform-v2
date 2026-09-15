package com.tieyoh.mobile

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

data class SignupRequest(
    val email: String,
    val password: String,
    val displayName: String,
    val firstName: String? = null,
    val lastName: String? = null,
    // Already collected and check-availability'd on "Let's get started" --
    // sent here so the account is created with it attached instead of
    // asking again on the profile-setup screen.
    val phone: String? = null
)

data class LoginRequest(
    val email: String,
    val password: String
)

data class AuthResponse(
    val id: Long,
    val email: String,
    val displayName: String,
    val tieyohId: String? = null,
    val profilePictureBase64: String? = null,
    val autoAcceptCircleInvites: Boolean? = null
)

data class OtpRequestRequest(
    val email: String,
    val purpose: String,
    // Only meaningful (and only sent) for purpose = "SIGNUP" -- lets the
    // backend screen email and phone together before a code is generated.
    val phone: String? = null
)

data class OtpVerifyRequest(
    val email: String,
    val purpose: String,
    val code: String
)

interface AuthApi {

    @POST("api/auth/signup")
    suspend fun signup(@Body request: SignupRequest): AuthResponse

    @POST("api/auth/login")
    suspend fun login(@Body request: LoginRequest): AuthResponse

    @POST("api/auth/otp/request")
    suspend fun requestOtp(@Body request: OtpRequestRequest): Response<Unit>

    @POST("api/auth/otp/verify")
    suspend fun verifyOtp(@Body request: OtpVerifyRequest): Response<Unit>
}
