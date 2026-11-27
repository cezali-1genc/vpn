package com.cloudflaresecure.vpn.vpn.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.cloudflaresecure.vpn.settings.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/** Cihaz açılışında "başlangıçta VPN" ayarı açıksa bağlantıyı başlatır (spec §4F). */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != Intent.ACTION_BOOT_COMPLETED) return
        CoroutineScope(Dispatchers.IO).launch {
            val settings = SettingsRepository(context).settings.first()
            if (settings.connectOnBoot && settings.activeServerId != null) {
                CloudflareVpnService.start(context)
            }
        }
    }
}
