package com.cloudflaresecure.vpn.settings

/** Kullanıcı ayarları (spec §4F). Varsayılanlar güvenli tarafta. */
data class VpnSettings(
    val autoConnect: Boolean = false,
    val connectOnBoot: Boolean = false,
    val autoReconnect: Boolean = true,
    val killSwitchEnabled: Boolean = true,
    val dnsLeakProtection: Boolean = true,
    val ipv6LeakProtection: Boolean = true,
    val dohEndpoint: String = "https://chrome.cloudflare-dns.com/dns-query",
    val activeServerId: String? = null,
    val darkTheme: Boolean = true,
    val notificationsEnabled: Boolean = true,
    val languageCode: String = "tr"
)
