package com.cloudflaresecure.vpn.ui.security

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.cloudflaresecure.vpn.settings.VpnSettings

@Composable
fun SecurityScreen(
    settings: VpnSettings,
    onKillSwitch: (Boolean) -> Unit,
    onDnsLeak: (Boolean) -> Unit,
    onIpv6Leak: (Boolean) -> Unit
) {
    val context = LocalContext.current
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Güvenlik Merkezi", style = MaterialTheme.typography.headlineSmall)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Kill Switch")
            Switch(checked = settings.killSwitchEnabled, onCheckedChange = onKillSwitch)
        }
        Text("VPN kesilirse trafiği engeller (fail-closed).")
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("DNS Sızıntı Koruması")
            Switch(checked = settings.dnsLeakProtection, onCheckedChange = onDnsLeak)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("IPv6 Sızıntı Koruması")
            Switch(checked = settings.ipv6LeakProtection, onCheckedChange = onIpv6Leak)
        }
        Divider()
        Text("Always-on VPN / Lockdown rehberi", style = MaterialTheme.typography.titleMedium)
        Text(
            "Uygulama Android lockdown modunu kendi başına etkinleştirmez. " +
                "Maksimum koruma için: Android Ayarları > Ağ ve internet > VPN > " +
                "Cloudflare Secure VPN > Always-on VPN'i açın ve 'VPN olmadan bağlantıları engelle' seçeneğini işaretleyin."
        )
        Button(onClick = { openVpnSettings(context) }) { Text("Android VPN Ayarlarını Aç") }
        Text(
            "Anahtarlar Android Keystore ile şifrelenir. Private key'ler APK içine gömülmez, " +
                "loglanmaz ve analitik sistemine gönderilmez."
        )
    }
}

private fun openVpnSettings(context: Context) {
    try {
        context.startActivity(Intent(Settings.ACTION_NETWORK_OPERATOR_SETTINGS))
    } catch (e: Exception) {
        try {
            context.startActivity(Intent(Settings.ACTION_WIRELESS_SETTINGS))
        } catch (e2: Exception) {
            context.startActivity(Intent(Settings.ACTION_SETTINGS))
        }
    }
    // Not: doğrudan VPN ayar ekranı OEM'e göre değişir; genel ağ ayarları açılır.
    @Suppress("UNUSED_EXPRESSION")
    Uri.parse("android.settings.VPN_SETTINGS")
}
