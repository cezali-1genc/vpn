package com.cloudflaresecure.vpn.ui.servers

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.cloudflaresecure.vpn.servers.VpnServer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServersScreen(
    state: ServersUiState,
    onAdd: (VpnServer, String) -> Unit,
    onDelete: (String) -> Unit,
    onSetActive: (String) -> Unit,
    onTest: (VpnServer) -> Unit,
    onImport: (String, String) -> Unit
) {
    var showAdd by remember { mutableStateOf(false) }
    var showImport by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { showAdd = true }) { Text("Sunucu Ekle") }
            OutlinedButton(onClick = { showImport = true }) { Text(".conf İçe Aktar") }
        }
        Spacer(Modifier.height(12.dp))
        if (state.servers.isEmpty()) {
            Text("Henüz gerçek sunucu tanımlı değil. VPN sunucusu olmadan bağlantı kurulamaz.")
            Text("Sunucu kurulumu için docs/server-setup.md dosyasına bakın.")
        }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(state.servers) { server ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text(server.name, style = MaterialTheme.typography.titleMedium)
                        Text("${server.endpoint()} • ${server.country} ${server.city}")
                        Text("Gecikme: ${server.lastLatencyMs?.let { "$it ms" } ?: "—"} • Sağlık: ${server.isHealthy?.let { if (it) "OK" else "Erişilemiyor" } ?: "—"}")
                        if (state.activeServerId == server.id) Text("Aktif sunucu")
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (state.activeServerId != server.id) {
                                TextButton(onClick = { onSetActive(server.id) }) { Text("Aktif Yap") }
                            }
                            TextButton(
                                onClick = { onTest(server) },
                                enabled = state.testingId != server.id
                            ) {
                                Text(if (state.testingId == server.id) "Test ediliyor…" else "Test Et")
                            }
                            TextButton(onClick = { onDelete(server.id) }) { Text("Sil") }
                        }
                    }
                }
            }
        }
        state.message?.let {
            Spacer(Modifier.height(8.dp))
            Text(it)
        }
    }

    if (showAdd) {
        AddServerDialog(
            onDismiss = { showAdd = false },
            onConfirm = { server, key ->
                onAdd(server, key)
                showAdd = false
            }
        )
    }
    if (showImport) {
        ImportConfigDialog(
            onDismiss = { showImport = false },
            onConfirm = { text, name ->
                onImport(text, name)
                showImport = false
            }
        )
    }
}

@Composable
private fun AddServerDialog(onDismiss: () -> Unit, onConfirm: (VpnServer, String) -> Unit) {
    var name by remember { mutableStateOf("") }
    var host by remember { mutableStateOf("") }
    var port by remember { mutableStateOf("51820") }
    var pubKey by remember { mutableStateOf("") }
    var privKey by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("10.8.0.2/32") }
    var country by remember { mutableStateOf("") }
    var city by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Sunucu Ekle") },
        text = {
            Column {
                OutlinedTextField(name, { name = it }, label = { Text("Ad") })
                OutlinedTextField(host, { host = it }, label = { Text("Sunucu adresi (host)") })
                OutlinedTextField(port, { port = it }, label = { Text("UDP port") })
                OutlinedTextField(pubKey, { pubKey = it }, label = { Text("Sunucu public key") })
                OutlinedTextField(privKey, { privKey = it }, label = { Text("İstemci private key (şifreli saklanır)") })
                OutlinedTextField(address, { address = it }, label = { Text("İstemci adresi (örn. 10.8.0.2/32)") })
                OutlinedTextField(country, { country = it }, label = { Text("Ülke") })
                OutlinedTextField(city, { city = it }, label = { Text("Şehir") })
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onConfirm(
                    VpnServer(
                        name = name, endpointHost = host,
                        endpointPort = port.toIntOrNull() ?: 51820,
                        serverPublicKey = pubKey, clientAddress = address,
                        country = country, city = city
                    ),
                    privKey
                )
            }) { Text("Ekle") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Vazgeç") } }
    )
}

@Composable
private fun ImportConfigDialog(onDismiss: () -> Unit, onConfirm: (String, String) -> Unit) {
    var name by remember { mutableStateOf("") }
    var text by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("WireGuard .conf İçe Aktar") },
        text = {
            Column {
                OutlinedTextField(name, { name = it }, label = { Text("Sunucu adı") })
                OutlinedTextField(text, { text = it }, label = { Text(".conf içeriği / QR metni") }, minLines = 6)
            }
        },
        confirmButton = { TextButton(onClick = { onConfirm(text, name) }) { Text("İçe Aktar") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Vazgeç") } }
    )
}
