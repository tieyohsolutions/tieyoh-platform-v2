package com.tieyoh.mobile

import retrofit2.http.Body
import retrofit2.http.POST

data class SignupRequest(
    val email: String,
    val password: String,
    val displayName: String
)

data class LoginRequest(
    val email: String,
    val password: String
)

data class AuthResponse(
    val id: Long,
    val email: String,
    val displayName: String
)

interface AuthApi {

    @POST("api/auth/signup")
    suspend fun signup(@Body request: SignupRequest): AuthResponse

    @POST("api/auth/login")
    suspend fun login(@Body request: LoginRequest): AuthResponse
}
