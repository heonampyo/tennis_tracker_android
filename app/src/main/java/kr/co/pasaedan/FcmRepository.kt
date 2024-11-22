package kr.co.pasaedan

import com.google.gson.Gson

// FcmRepository.kt
class FcmRepository {
    suspend fun updateFcmToken(userId: String, fcmToken: String): Result<FcmTokenResponse> {
        return try {
            val response = NetworkModule.apiService.updateFcmToken(
                FcmTokenRequest(userId, fcmToken)
            )
            
            when {
                response.isSuccessful -> {
                    response.body()?.let {
                        Result.success(it)
                    } ?: Result.failure(Exception("응답이 비어있습니다."))
                }
                response.code() == 400 -> {
                    val errorBody = response.errorBody()?.string()
                    val error = Gson().fromJson(errorBody, ErrorResponse::class.java)
                    Result.failure(Exception(error.error))
                }
                else -> {
                    Result.failure(Exception("FCM 토큰 업데이트 실패: ${response.code()}"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}