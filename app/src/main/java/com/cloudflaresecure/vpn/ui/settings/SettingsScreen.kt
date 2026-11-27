package com.cloudflaresecure.vpn.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.cloudflaresecure.vpn.settings.VpnSettings

@Composable
fun SettingsScreen(
    settings: VpnSettings,
    onAutoConnect: (Boolean) -> Unit,
    onConnectOnBoot: (Boolean) -> Unit,
    onAutoReconnect: (Boolean) -> Unit,
    onDarkTheme: (Boolean) -> Unit,
    onNotifications: (Boolean) -> Unit,
    onLanguage: (String) -> Unit,
    onClearHistory: () -> Unit,
    versionName: String
) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("Ayarlar", style = MaterialTheme.typography.headlineSmall)
        SettingSwitch("Otomatik bağlanma", settings.autoConnect, onAutoConnect)
        SettingSwitch("Başlangıçta VPN", settings.connectOnBoot, onConnectOnBoot)
        SettingSwitch("Otomatik yeniden bağlanma", settings.autoReconnect, onAutoReconnect)
        SettingSwitch("Koyu tema", settings.darkTheme, onDarkTheme)
        SettingSwitch("Bildirimler", settings.notificationsEnabled, onNotifications)
        Text("Dil", style = MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = settings.languageCode == "tr", onClick = { onLanguage("tr") }, label = { Text("Türkçe") })
            FilterChip(selected = settings.languageCode == "en", onClick = { onLanguage("en") }, label = { Text("English") })
        }
        HorizontalDivider()
        OutlinedButton(onClick = onClearHistory) { Text("Bağlantı geçmişini temizle") }
        Text("Sürüm: $versionName")
        Text("Gizlilik: VPN trafiğiniz reklam/takip amacıyla kullanılmaz ve izinsiz analiz edilmez.")
    }
}

@Composable
private fun SettingSwitch(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label)
        Switch(checked = checked, onCheckedChange = onChange)
    }
}
