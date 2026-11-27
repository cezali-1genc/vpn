package com.cloudflaresecure.vpn.ui.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.cloudflaresecure.vpn.core.network.ConnectionState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    state: HomeUiState,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit,
    onMeasureSpeed: () -> Unit,
    onOpenServers: () -> Unit
) {
    val info = state.info
    val connected = info.state == ConnectionState.CONNECTED
    val busy = info.state == ConnectionState.CONNECTING ||
        info.state == ConnectionState.PREPARING ||
        info.state == ConnectionState.AUTHENTICATING ||
        info.state == ConnectionState.RECONNECTING

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Cloudflare Secure VPN", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(4.dp))
        Text(
            when (info.state) {
                ConnectionState.CONNECTED -> "Bağlı — güvenli tünel aktif"
                ConnectionState.DISCONNECTED -> "Bağlı değil"
                ConnectionState.ERROR -> "Hata: ${info.lastError?.title ?: "bilinmeyen"}"
                else -> "Bağlanıyor…"
            },
            color = when {
                connected -> Color(0xFF4CAF50)
                info.state == ConnectionState.ERROR -> Color(0xFFFF5252)
                else -> MaterialTheme.colorScheme.onSurface
            }
        )
        Spacer(Modifier.height(24.dp))

        // Büyük bağlantı düğmesi (spec §8)
        Button(
            onClick = { if (connected) onDisconnect() else onConnect() },
            enabled = !busy,
            modifier = Modifier.size(180.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (connected) Color(0xFFB3261E) else Color(0xFF0057C2)
            )
        ) {
            Text(
                if (connected) "Bağlantıyı Kes" else "Bağlan",
                style = MaterialTheme.typography.titleLarge
            )
        }
        Spacer(Modifier.height(24.dp))

        // Sunucu kartı
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text("Sunucu", style = MaterialTheme.typography.titleMedium)
                Text(state.serverName ?: "Sunucu seçilmedi — Sunucular ekranından ekleyin")
                Spacer(Modifier.height(8.dp))
                OutlinedButton(onClick = onOpenServers) { Text("Sunucu Değiştir") }
            }
        }
        Spacer(Modifier.height(12.dp))

        // IP + durum kartı
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text("Dış IP", style = MaterialTheme.typography.titleMedium)
                Text(info.externalIp ?: "—")
                Spacer(Modifier.height(4.dp))
                Text("Durum: ${info.state.name}")
                if (info.connectedAtMillis != null) {
                    Text("Bağlantı süresi: ${elapsed(info.connectedAtMillis)}")
                }
            }
        }
        Spacer(Modifier.height(12.dp))

        // Hız kartı (gerçek ölçüm)
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text("Hız", style = MaterialTheme.typography.titleMedium)
                Text("İndirme: ${state.downloadMbps?.let { "%.2f Mbps".format(it) } ?: "—"}")
                Text("Yükleme: ${state.uploadMbps?.let { "%.2f Mbps".format(it) } ?: "—"}")
                Spacer(Modifier.height(8.dp))
                Button(onClick = onMeasureSpeed, enabled = !state.isMeasuring) {
                    Text(if (state.isMeasuring) "Ölçülüyor…" else "Hızı Ölç")
                }
            }
        }
        Spacer(Modifier.height(12.dp))

        // DNS + Güvenlik kartı
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text("DNS", style = MaterialTheme.typography.titleMedium)
                Text("Cloudflare DoH (chrome.cloudflare-dns.com)")
                Text("DoH: ${if (info.verification.dohOk) "OK" else "—"} • Sızıntı yok: ${if (info.verification.dnsLeakFree) "Evet" else "—"}")
                Spacer(Modifier.height(4.dp))
                Text("Güvenlik: Kill Switch ${if (info.verification.killSwitchOk) "aktif" else "—"} • IPv6 güvenli: ${if (info.verification.ipv6Safe) "Evet" else "—"}")
            }
        }

        state.message?.let {
            Spacer(Modifier.height(12.dp))
            Text(it, color = MaterialTheme.colorScheme.error)
        }
        info.lastError?.let {
            Spacer(Modifier.height(8.dp))
            Text(it.userMessage(), color = MaterialTheme.colorScheme.error)
        }
    }
}

private fun elapsed(sinceMillis: Long): String {
    val sec = (System.currentTimeMillis() - sinceMillis) / 1000
    return "${sec / 60}dk ${sec % 60}sn"
}
