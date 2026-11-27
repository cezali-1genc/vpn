package com.cloudflaresecure.vpn.servers

import java.util.UUID

/**
 * Gerçek WireGuard sunucusu kaydı (spec §4B).
 * Private key ASLA bu modelde düz metin saklanmaz; SecureConfigStore'da
 * Keystore ile şifrelenerek tutulur, burada yalnızca referans id bulunur.
 */
data class VpnServer(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val country: String = "",
    val city: String = "",
    val endpointHost: String,
    val endpointPort: Int = 51820,
    val serverPublicKey: String,
    val clientAddress: String, // örn. 10.8.0.2/32
    val allowedIps: String = "0.0.0.0/0, ::/0",
    val persistentKeepalive: Int = 25,
    val isActive: Boolean = false,
    val lastLatencyMs: Long? = null,
    val lastHealthCheckMillis: Long? = null,
    val isHealthy: Boolean? = null
) {
    fun endpoint(): String = "$endpointHost:$endpointPort"
}

/** Sunucu doğrulama — geçersiz kaydı kaydetmeyi engeller. */
object ServerValidator {
    private val base64Key = Regex("^[A-Za-z0-9+/]{43}=$")

    fun validate(server: VpnServer): String? {
        if (server.name.isBlank()) return "Sunucu adı boş olamaz."
        if (server.endpointHost.isBlank()) return "Sunucu adresi boş olamaz."
        if (server.endpointPort !in 1..65535) return "Port 1-65535 aralığında olmalı."
        if (!base64Key.matches(server.serverPublicKey.trim()))
            return "Sunucu public key'i geçerli WireGuard base64 formatında olmalı (44 karakter)."
        if (server.clientAddress.isBlank()) return "İstemci VPN adresi boş olamaz."
        return null
    }
}
