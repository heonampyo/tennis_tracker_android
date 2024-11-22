package kr.co.pasaedan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// FcmViewModel.kt
class FcmViewModel : ViewModel() {
    private val repository = FcmRepository()

    private val _updateState = MutableStateFlow<Result<FcmTokenResponse>?>(null)
    val updateState = _updateState.asStateFlow()
    
    fun updateFcmToken(userId: String, fcmToken: String) {
        viewModelScope.launch {
            _updateState.value = null // 로딩 상태 초기화
            
            repository.updateFcmToken(userId, fcmToken)
                .onSuccess { response ->
                    _updateState.value = Result.success(response)
                }
                .onFailure { exception ->
                    _updateState.value = Result.failure(exception)
                }
        }
    }
}