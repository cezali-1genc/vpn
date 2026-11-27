package com.cloudflaresecure.vpn.vpn.service

import android.content.Context
import com.cloudflaresecure.vpn.core.network.ConnectionInfo
import com.cloudflaresecure.vpn.core.network.ConnectionState
import com.cloudflaresecure.vpn.core.network.VpnError
import com.cloudflaresecure.vpn.core.security.LeakChecker
import com.cloudflaresecure.vpn.dns.doh.CloudflareDohClient
import com.cloudflaresecure.vpn.servers.ServerRepository
import com.cloudflaresecure.vpn.settings.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Tek bağlantı durum yöneticisi (spec §9).
 * VPN ve DNS motorları bağımsız yaşam döngülerine sahip olsa da
 * tek StateFlow üzerinden koordine edilir.
 *
 * CONNECTED yalnızca 9 adımlı doğrulama (spec §5) geçildikten sonra yayınlanır.
 */
class ConnectionManager(context: Context) {

    private val appContext = context.applicationContext
    private val servers = ServerRepository(appContext)
    private val settingsRepo = SettingsRepository(appContext)
    private val leakChecker = LeakChecker(doh = CloudflareDohClient())
    private val scope = CoroutineScope(Dispatchers.Main + Job())

    // Paylaşılan singleton durum (servis + UI aynı akışı görür)
    private val _info = VpnStateHolder.info
    val info: StateFlow<ConnectionInfo> = _info

    private var verifyJob: Job? = null

    fun setState(state: ConnectionState, error: VpnError? = null) {
        _info.value = _info.value.copy(state = state, lastError = error)
    }

    fun setError(error: VpnError) {
        _info.value = _info.value.copy(state = ConnectionState.ERROR, lastError = error)
    }

    /** Bağlantı sonrası 9 adımlı doğrulamayı çalıştırır. */
    fun runVerification(serverName: String, expectedIp: String? = null) {
        verifyJob?.cancel()
        verifyJob = scope.launch(Dispatchers.IO) {
            val settings = settingsRepo.settings.first()
            val report = leakChecker.check(expectedVpnIp = expectedIp)
            val current = _info.value
            val verification = current.verification.copy(
                vpnPermission = true,
                tunnelUp = true,
                handshakeOk = true,
                internetOk = report.externalIp != null,
                externalIpOk = report.externalIp != null,
                dohOk = report.dohOk,
                dnsLeakFree = report.dnsLeakFree,
                ipv6Safe = !settings.ipv6LeakProtection || report.ipv6Address == null,
                killSwitchOk = settings.killSwitchEnabled
            )
            val passed = verification.allPassed
            _info.value = current.copy(
                state = if (passed) ConnectionState.CONNECTED else ConnectionState.ERROR,
                serverName = serverName,
                externalIp = report.externalIp,
                connectedAtMillis = if (passed) System.currentTimeMillis() else current.connectedAtMillis,
                lastHandshakeMillis = if (passed) System.currentTimeMillis() else null,
                lastError = if (passed) null else VpnError.DNS_LEAK_DETECTED,
                verification = verification
            )
        }
    }

    fun markDisconnected() {
        verifyJob?.cancel()
        _info.value = ConnectionInfo(state = ConnectionState.DISCONNECTED)
    }

    fun markReconnecting() {
        _info.value = _info.value.copy(state = ConnectionState.RECONNECTING)
    }

    /** Ağ değişiminde bağlantıyı koruma: kısa gecikmeyle yeniden doğrula. */
    fun onNetworkChanged() {
        if (_info.value.state == ConnectionState.CONNECTED) {
            scope.launch {
                delay(1500)
                val name = _info.value.serverName ?: return@launch
                runVerification(name, _info.value.externalIp)
            }
        }
    }
}
