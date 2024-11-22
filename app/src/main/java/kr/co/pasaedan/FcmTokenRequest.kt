package kr.co.pasaedan

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

// FcmTokenModels.kt
data class FcmTokenRequest(
    val userId: String,
    val fcmToken: String
)

data class FcmTokenResponse(
    val message: String,
    val user: User
)

data class User(
    val id: String,
    val fcmToken: String
    // 필요한 다른 사용자 필드들...
)

data class ErrorResponse(
    val error: String
)

// ApiService.kt
interface ApiService {
    @POST("api/fcm")
    suspend fun updateFcmToken(
        @Body request: FcmTokenRequest
    ): Response<FcmTokenResponse>
}