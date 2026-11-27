package com.cloudflaresecure.vpn.vpn.service

import com.cloudflaresecure.vpn.core.network.ConnectionInfo
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Uygulama genelinde paylaşılan bağlantı durumu (singleton).
 * CloudflareVpnService yazar, UI (HomeViewModel) okur — böylece
 * servis ve arayüz aynı StateFlow'u görür (spec §9: tek durum yöneticisi).
 */
object VpnStateHolder {
    val info = MutableStateFlow(ConnectionInfo())
}
