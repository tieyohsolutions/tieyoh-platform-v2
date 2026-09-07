package com.tieyoh.mobile

import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Path

data class ConnectionRequestBody(
    val senderId: Long,
    val recipientId: Long
)

data class ConnectionRequestRespondBody(
    val accept: Boolean
)

data class ConnectionRequestResult(
    val id: Long,
    val senderId: Long,
    val recipientId: Long,
    val status: String
)

interface ConnectionRequestApi {

    @POST("api/connection-requests")
    suspend fun sendRequest(@Body body: ConnectionRequestBody): ConnectionRequestResult

    @POST("api/connection-requests/{id}/respond")
    suspend fun respond(
        @Path("id") id: Long,
        @Body body: ConnectionRequestRespondBody
    ): ConnectionRequestResult
}
