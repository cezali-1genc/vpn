package com.cloudflaresecure.vpn.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore("vpn_settings")

/** DataStore tabanlı ayar deposu (spec §4F). */
class SettingsRepository(private val context: Context) {

    private object Keys {
        val AUTO_CONNECT = booleanPreferencesKey("auto_connect")
        val CONNECT_ON_BOOT = booleanPreferencesKey("connect_on_boot")
        val AUTO_RECONNECT = booleanPreferencesKey("auto_reconnect")
        val KILL_SWITCH = booleanPreferencesKey("kill_switch")
        val DNS_LEAK = booleanPreferencesKey("dns_leak_protection")
        val IPV6_LEAK = booleanPreferencesKey("ipv6_leak_protection")
        val DOH_ENDPOINT = stringPreferencesKey("doh_endpoint")
        val ACTIVE_SERVER = stringPreferencesKey("active_server_id")
        val DARK_THEME = booleanPreferencesKey("dark_theme")
        val NOTIFICATIONS = booleanPreferencesKey("notifications_enabled")
        val LANGUAGE = stringPreferencesKey("language_code")
    }

    val settings: Flow<VpnSettings> = context.dataStore.data.map { p ->
        VpnSettings(
            autoConnect = p[Keys.AUTO_CONNECT] ?: false,
            connectOnBoot = p[Keys.CONNECT_ON_BOOT] ?: false,
            autoReconnect = p[Keys.AUTO_RECONNECT] ?: true,
            killSwitchEnabled = p[Keys.KILL_SWITCH] ?: true,
            dnsLeakProtection = p[Keys.DNS_LEAK] ?: true,
            ipv6LeakProtection = p[Keys.IPV6_LEAK] ?: true,
            dohEndpoint = p[Keys.DOH_ENDPOINT] ?: "https://chrome.cloudflare-dns.com/dns-query",
            activeServerId = p[Keys.ACTIVE_SERVER],
            darkTheme = p[Keys.DARK_THEME] ?: true,
            notificationsEnabled = p[Keys.NOTIFICATIONS] ?: true,
            languageCode = p[Keys.LANGUAGE] ?: "tr"
        )
    }

    suspend fun update(transform: suspend (VpnSettings) -> VpnSettings) {
        val current = settings.firstOrNull() ?: VpnSettings()
        val next = transform(current)
        context.dataStore.edit { p ->
            p[Keys.AUTO_CONNECT] = next.autoConnect
            p[Keys.CONNECT_ON_BOOT] = next.connectOnBoot
            p[Keys.AUTO_RECONNECT] = next.autoReconnect
            p[Keys.KILL_SWITCH] = next.killSwitchEnabled
            p[Keys.DNS_LEAK] = next.dnsLeakProtection
            p[Keys.IPV6_LEAK] = next.ipv6LeakProtection
            p[Keys.DOH_ENDPOINT] = next.dohEndpoint
            if (next.activeServerId != null) p[Keys.ACTIVE_SERVER] = next.activeServerId
            p[Keys.DARK_THEME] = next.darkTheme
            p[Keys.NOTIFICATIONS] = next.notificationsEnabled
            p[Keys.LANGUAGE] = next.languageCode
        }
    }

    suspend fun setActiveServer(id: String?) {
        context.dataStore.edit { p ->
            if (id != null) p[Keys.ACTIVE_SERVER] = id else p.remove(Keys.ACTIVE_SERVER)
        }
    }
}
