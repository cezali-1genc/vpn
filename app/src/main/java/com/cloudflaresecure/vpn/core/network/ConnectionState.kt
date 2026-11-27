package com.cloudflaresecure.vpn.core.network

/**
 * Spec §5 — merkezi bağlantı durumları (StateFlow ile yönetilir).
 * CONNECTED yalnızca gerçek WireGuard bağlantısı + zorunlu doğrulamalar
 * (handshake, dış IP, DoH, sızıntı kontrolü) başarılıysa gösterilir.
 */
enum class ConnectionState {
    DISCONNECTED,
    PREPARING,
    CONNECTING,
    AUTHENTICATING,
    CONNECTED,
    RECONNECTING,
    DISCONNECTING,
    ERROR
}

data class ConnectionInfo(
    val state: ConnectionState = ConnectionState.DISCONNECTED,
    val serverName: String? = null,
    val externalIp: String? = null,
    val connectedAtMillis: Long? = null,
    val lastHandshakeMillis: Long? = null,
    val lastError: VpnError? = null,
    val verification: VerificationResult = VerificationResult()
)

/** Spec §5 — 9 adımlı bağlantı doğrulaması sonuçları (gerçek değerler). */
data class VerificationResult(
    val vpnPermission: Boolean = false,
    val tunnelUp: Boolean = false,
    val handshakeOk: Boolean = false,
    val internetOk: Boolean = false,
    val externalIpOk: Boolean = false,
    val dohOk: Boolean = false,
    val dnsLeakFree: Boolean = false,
    val ipv6Safe: Boolean = false,
    val killSwitchOk: Boolean = false
) {
    val allPassed: Boolean
        get() = vpnPermission && tunnelUp && handshakeOk && internetOk &&
            externalIpOk && dohOk && dnsLeakFree && ipv6Safe && killSwitchOk
}
