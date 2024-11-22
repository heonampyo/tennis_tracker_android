package kr.co.pasaedan

import android.content.Context

object PreferenceManager {
    private const val PREF_NAME = "user_preferences"
    private const val KEY_USER_NAME = "user_name"
    private const val KEY_FCM_TOKEN = "fcm_token"

    fun getUserName(context: Context): String? {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .getString(KEY_USER_NAME, null)
    }

    fun setUserName(context: Context, name: String) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_USER_NAME, name.replace("\\s".toRegex(), ""))
            .apply()
    }

    fun getFcmToken(context: Context): String? {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .getString(KEY_FCM_TOKEN, null)
    }

    fun setFcmToken(context: Context, fcmToken: String) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_FCM_TOKEN, fcmToken)
            .apply()
    }
}