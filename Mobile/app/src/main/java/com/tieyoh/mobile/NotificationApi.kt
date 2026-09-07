package com.tieyoh.mobile

import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

data class NotificationResult(
    val id: Long,
    val type: String,
    val actorUserId: Long?,
    val actorDisplayName: String?,
    val message: String,
    val read: Boolean,
    val createdAt: String?,
    val connectionRequestId: Long?,
    val giftId: Long? = null
)

interface NotificationApi {

    @GET("api/users/{userId}/notifications")
    suspend fun getNotifications(@Path("userId") userId: Long): List<NotificationResult>

    @POST("api/users/{userId}/notifications/mark-read")
    suspend fun markAllRead(@Path("userId") userId: Long)

    @POST("api/users/{userId}/notifications/{notificationId}/mark-read")
    suspend fun markRead(@Path("userId") userId: Long, @Path("notificationId") notificationId: Long)
}
