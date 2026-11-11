package me.gharbijli.partner.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import me.gharbijli.partner.data.SessionManager

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action == Intent.ACTION_BOOT_COMPLETED ||
            action == Intent.ACTION_MY_PACKAGE_REPLACED ||
            action == "android.intent.action.QUICKBOOT_POWERON" ||
            action == "com.htc.intent.action.QUICKBOOT_POWERON"
        ) {
            val sessionManager = SessionManager(context)
            if (sessionManager.isLoggedIn() && sessionManager.isOnline()) {
                CaptainDutyService.startDuty(context)
            }
        }
    }
}
