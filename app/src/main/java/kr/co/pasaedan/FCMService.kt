package kr.co.pasaedan

import android.widget.Toast
import com.google.firebase.messaging.FirebaseMessagingService

class FCMService : FirebaseMessagingService() {
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        // 새로운 토큰 발급 시 처리
        Toast.makeText(this, "onNewToken : $token", Toast.LENGTH_LONG).show()
//        sendRegistrationTokenToServer(token)
    }
}
