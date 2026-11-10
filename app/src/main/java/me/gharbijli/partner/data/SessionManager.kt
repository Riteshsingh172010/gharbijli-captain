package me.gharbijli.partner.data

import android.content.Context
import android.content.SharedPreferences

class SessionManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("gharbijli_partner_session", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_TOKEN = "jwt_token"
        private const val KEY_PARTNER_ID = "partner_id"
        private const val KEY_PARTNER_NAME = "partner_name"
        private const val KEY_PARTNER_PHONE = "partner_phone"
        private const val KEY_IS_ONLINE = "partner_is_online"
    }

    fun saveSession(token: String, partner: PartnerProfile?) {
        prefs.edit().apply {
            putString(KEY_TOKEN, token)
            putString(KEY_PARTNER_ID, partner?.id)
            putString(KEY_PARTNER_NAME, partner?.name)
            putString(KEY_PARTNER_PHONE, partner?.phone)
            putBoolean(KEY_IS_ONLINE, partner?.isOnline ?: false)
            apply()
        }
    }

    fun getToken(): String? = prefs.getString(KEY_TOKEN, null)

    fun getPartnerName(): String = prefs.getString(KEY_PARTNER_NAME, "Partner Electrician") ?: "Partner Electrician"

    fun getPartnerPhone(): String = prefs.getString(KEY_PARTNER_PHONE, "") ?: ""

    fun isOnline(): Boolean = prefs.getBoolean(KEY_IS_ONLINE, false)

    fun setOnline(online: Boolean) {
        prefs.edit().putBoolean(KEY_IS_ONLINE, online).apply()
    }

    fun isLoggedIn(): Boolean = !getToken().isNullOrBlank()

    fun clearSession() {
        prefs.edit().clear().apply()
    }
}
