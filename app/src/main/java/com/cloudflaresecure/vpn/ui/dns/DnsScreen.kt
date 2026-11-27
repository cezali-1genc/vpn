package com.cloudflaresecure.vpn.ui.dns

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.cloudflaresecure.vpn.dns.doh.CloudflareDohClient

@Composable
fun DnsScreen(state: DnsUiState, onTest: () -> Unit, onLeak: () -> Unit, onClear: () -> Unit) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("DNS Merkezi", style = MaterialTheme.typography.headlineSmall)
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text("Cloudflare DoH", style = MaterialTheme.typography.titleMedium)
                Text("Birincil: ${CloudflareDohClient.PRIMARY_ENDPOINT}")
                Text("Yedek: ${CloudflareDohClient.FALLBACK_ENDPOINT}")
                Text("Standart: 1.1.1.1 / 1.0.0.1 • Malware: 1.1.1.2 / 1.0.0.2 • Yetişkin: 1.1.1.3 / 1.0.0.3")
                Text("Durum: ${state.dohOk?.let { if (it) "Bağlı" else "Başarısız" } ?: "—"}")
                Text("Gecikme: ${state.latencyMs?.let { "$it ms" } ?: "—"}")
                Text("Sorgu sayısı: ${state.queryCount} • Önbellek: ${state.cacheSize}")
                Text("Dış IP: ${state.externalIp ?: "—"}")
                Text("Sızıntı yok: ${state.dnsLeakFree?.let { if (it) "Evet" else "Hayır" } ?: "—"}")
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onTest, enabled = !state.isTesting) { Text("DoH Test") }
            OutlinedButton(onClick = onLeak, enabled = !state.isTesting) { Text("Sızıntı Kontrolü") }
            OutlinedButton(onClick = onClear) { Text("Önbelleği Temizle") }
        }
        Text("DoH başarısız olursa şifresiz DNS'e düşülmez; yedek DoH denenir, o da olmazsa bağlantı engellenir (fail-closed).")
        state.message?.let { Text(it) }
    }
}
