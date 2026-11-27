package com.cloudflaresecure.vpn

import android.app.Application
import com.cloudflaresecure.vpn.notifications.VpnNotificationHelper

class CloudflareVpnApp : Application() {
    override fun onCreate() {
        super.onCreate()
        VpnNotificationHelper.ensureChannel(this)
    }
}
