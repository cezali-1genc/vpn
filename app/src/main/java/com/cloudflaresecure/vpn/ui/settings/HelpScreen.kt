package com.cloudflaresecure.vpn.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun HelpScreen(versionName: String) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Yardım ve Destek", style = MaterialTheme.typography.headlineSmall)
        Text("Bağlantı sorun giderme", style = MaterialTheme.typography.titleMedium)
        Text("1) Sunucu tanımlı mı kontrol edin. 2) İnternet bağlantınızı doğrulayın. 3) Sunucu > Test Et ile erişilebilirliği ölçün. 4) DNS ekranından DoH Test çalıştırın.")
        Text("DNS sorunları", style = MaterialTheme.typography.titleMedium)
        Text("DoH başarısız olursa uygulama şifresiz DNS'e düşmez. Yedek endpoint denenir; sorun sürerse ağınızı değiştirin.")
        Text("WireGuard yapılandırma yardımı", style = MaterialTheme.typography.titleMedium)
        Text("Sunucu kurulumu için docs/server-setup.md dosyasındaki Ubuntu adımlarını izleyin. Private key'inizi kimseyle paylaşmayın.")
        Text("Güvenlik açıklamaları", style = MaterialTheme.typography.titleMedium)
        Text("Trafik WireGuard ile şifrelenir, DNS Cloudflare DoH ile çözülür. Kill Switch tünel kesilirse trafiği engeller.")
        Text("Sürüm: $versionName")
        Text("Açık kaynak lisansları: WireGuard (GPLv2/Apache-2.0 — bkz. NOTICE), OkHttp (Apache-2.0), Jetpack Compose (Apache-2.0).")
    }
}
