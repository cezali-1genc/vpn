package com.cloudflaresecure.vpn.ui.home

import android.app.Activity
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cloudflaresecure.vpn.core.network.ConnectionInfo
import com.cloudflaresecure.vpn.core.network.ConnectionState
import com.cloudflaresecure.vpn.core.network.VpnError
import com.cloudflaresecure.vpn.monitoring.SpeedMonitor
import com.cloudflaresecure.vpn.servers.ServerRepository
import com.cloudflaresecure.vpn.settings.SettingsRepository
import com.cloudflaresecure.vpn.vpn.service.CloudflareVpnService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class HomeUiState(
    val info: ConnectionInfo = ConnectionInfo(),
    val serverName: String? = null,
    val downloadMbps: Double? = null,
    val uploadMbps: Double? = null,
    val pingMs: Long? = null,
    val isMeasuring: Boolean = false,
    val message: String? = null
)

class HomeViewModel(context: Context) : ViewModel() {

    private val appContext = context.applicationContext
    private val servers = ServerRepository(appContext)
    private val settingsRepo = SettingsRepository(appContext)
    private val speedMonitor = SpeedMonitor()
    private val connectionManager = com.cloudflaresecure.vpn.vpn.service.ConnectionManager(appContext)

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            connectionManager.info.collect { info ->
                _uiState.value = _uiState.value.copy(info = info, serverName = info.serverName)
            }
        }
        viewModelScope.launch {
            val settings = settingsRepo.settings.first()
            val server = settings.activeServerId?.let { servers.getById(it) }
            _uiState.value = _uiState.value.copy(serverName = server?.name)
        }
    }

    fun onConnectClicked(activity: Activity) {
        val prepareIntent = CloudflareVpnService.prepare(activity)
        if (prepareIntent != null) {
            // Sistem VPN izni gerekli
            _uiState.value = _uiState.value.copy(
                message = "VPN izni gerekli. Sistem penceresini onaylayın."
            )
            activity.startActivityForResult(prepareIntent, VPN_REQUEST_CODE)
        } else {
            CloudflareVpnService.start(activity)
        }
    }

    fun onVpnPermissionResult(granted: Boolean, activity: Activity) {
        if (granted) {
            CloudflareVpnService.start(activity)
        } else {
            _uiState.value = _uiState.value.copy(
                info = _uiState.value.info.copy(
                    state = ConnectionState.ERROR,
                    lastError = VpnError.VPN_PERMISSION_DENIED
                ),
                message = VpnError.VPN_PERMISSION_DENIED.userMessage()
            )
        }
    }

    fun onDisconnectClicked(context: Context) {
        CloudflareVpnService.stop(context)
        connectionManager.markDisconnected()
    }

    fun measureSpeed() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isMeasuring = true)
            val down = speedMonitor.measureDownload()
            val up = speedMonitor.measureUpload()
            _uiState.value = _uiState.value.copy(
                downloadMbps = down,
                uploadMbps = up,
                isMeasuring = false,
                message = if (down == null) "Hız ölçümü başarısız — bağlantınızı kontrol edin." else null
            )
        }
    }

    fun clearMessage() {
        _uiState.value = _uiState.value.copy(message = null)
    }

    companion object {
        const val VPN_REQUEST_CODE = 100
    }
}
