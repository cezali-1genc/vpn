package com.cloudflaresecure.vpn.ui.servers

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cloudflaresecure.vpn.servers.ServerRepository
import com.cloudflaresecure.vpn.servers.ServerValidator
import com.cloudflaresecure.vpn.servers.VpnServer
import com.cloudflaresecure.vpn.settings.SettingsRepository
import com.cloudflaresecure.vpn.vpn.wireguard.WireGuardConfigParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.Socket
import java.net.InetSocketAddress

data class ServersUiState(
    val servers: List<VpnServer> = emptyList(),
    val activeServerId: String? = null,
    val message: String? = null,
    val testingId: String? = null
)

class ServersViewModel(context: Context) : ViewModel() {

    private val appContext = context.applicationContext
    private val repo = ServerRepository(appContext)
    private val settingsRepo = SettingsRepository(appContext)

    private val _uiState = MutableStateFlow(ServersUiState())
    val uiState: StateFlow<ServersUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repo.servers.collect { list ->
                _uiState.value = _uiState.value.copy(servers = list)
            }
        }
        viewModelScope.launch {
            settingsRepo.settings.collect { s ->
                _uiState.value = _uiState.value.copy(activeServerId = s.activeServerId)
            }
        }
    }

    fun addServer(server: VpnServer, privateKey: String) {
        viewModelScope.launch {
            try {
                repo.add(server, privateKey)
                _uiState.value = _uiState.value.copy(message = "Sunucu eklendi: ${server.name}")
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(message = "Eklenemedi: ${e.message}")
            }
        }
    }

    fun deleteServer(serverId: String) {
        viewModelScope.launch {
            repo.delete(serverId)
            _uiState.value = _uiState.value.copy(message = "Sunucu silindi.")
        }
    }

    fun setActive(serverId: String) {
        viewModelScope.launch {
            settingsRepo.setActiveServer(serverId)
            _uiState.value = _uiState.value.copy(message = "Aktif sunucu güncellendi.")
        }
    }

    /** Gerçek erişilebilirlik testi: endpoint'e TCP bağlantısı dener. */
    fun testServer(server: VpnServer) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(testingId = server.id)
            val latency = withContext(Dispatchers.IO) {
                try {
                    val started = System.currentTimeMillis()
                    Socket().use { socket ->
                        socket.connect(InetSocketAddress(server.endpointHost, server.endpointPort), 5000)
                    }
                    System.currentTimeMillis() - started
                } catch (e: Exception) {
                    null
                }
            }
            val updated = server.copy(
                lastLatencyMs = latency,
                isHealthy = latency != null,
                lastHealthCheckMillis = System.currentTimeMillis()
            )
            repo.update(updated)
            _uiState.value = _uiState.value.copy(
                testingId = null,
                message = if (latency != null) "${server.name}: erişilebilir (${latency} ms)"
                else "${server.name}: erişilemiyor — endpoint/port kontrol edin."
            )
        }
    }

    /** .conf metnini içe aktarır (QR metni de aynı formatta yapıştırılabilir). */
    fun importConfig(confText: String, name: String) {
        viewModelScope.launch {
            if (!WireGuardConfigParser.looksLikeWireGuardConfig(confText)) {
                _uiState.value = _uiState.value.copy(message = "Geçersiz WireGuard yapılandırması.")
                return@launch
            }
            try {
                val config = WireGuardConfigParser.parse(confText)
                val iface = config.`interface`
                val peer = config.peers.firstOrNull()
                if (peer == null) {
                    _uiState.value = _uiState.value.copy(message = "Peer bilgisi bulunamadı.")
                    return@launch
                }
                val endpoint = peer.endpoint.get().toString() // host:port
                val host = endpoint.substringBeforeLast(":")
                val port = endpoint.substringAfterLast(":").toIntOrNull() ?: 51820
                val server = VpnServer(
                    name = name.ifBlank { host },
                    endpointHost = host,
                    endpointPort = port,
                    serverPublicKey = peer.publicKey.base64,
                    clientAddress = iface.addresses.firstOrNull()?.toString() ?: "10.8.0.2/32"
                )
                val error = ServerValidator.validate(server)
                if (error != null) {
                    _uiState.value = _uiState.value.copy(message = error)
                    return@launch
                }
                repo.add(server, iface.privateKey.base64)
                _uiState.value = _uiState.value.copy(message = "Yapılandırma içe aktarıldı: ${server.name}")
            } catch (e: WireGuardConfigParser.ParseException) {
                _uiState.value = _uiState.value.copy(message = e.error.userMessage())
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(message = "İçe aktarma hatası: ${e.message}")
            }
        }
    }

    fun clearMessage() {
        _uiState.value = _uiState.value.copy(message = null)
    }
}
