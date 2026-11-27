package com.cloudflaresecure.vpn.vpn.wireguard

import com.cloudflaresecure.vpn.core.network.VpnError
import com.cloudflaresecure.vpn.servers.VpnServer
import com.wireguard.config.Config
import java.io.ByteArrayInputStream

/**
 * WireGuard .conf metnini resmî wireguard-config kütüphanesiyle doğrular
 * ve [Config] nesnesine çevirir (spec §2, §4B).
 *
 * Güvenlik: private key yalnızca şifreli depodan alınır, loglanmaz,
 * APK içine gömülmez.
 */
object WireGuardConfigParser {

    class ParseException(val error: VpnError, message: String) : Exception(message)

    fun buildConfText(server: VpnServer, clientPrivateKeyBase64: String): String {
        require(clientPrivateKeyBase64.isNotBlank()) { "Private key boş olamaz" }
        return buildString {
            appendLine("[Interface]")
            appendLine("PrivateKey = $clientPrivateKeyBase64")
            appendLine("Address = ${server.clientAddress}")
            appendLine()
            appendLine("[Peer]")
            appendLine("PublicKey = ${server.serverPublicKey.trim()}")
            appendLine("Endpoint = ${server.endpoint()}")
            appendLine("AllowedIPs = ${server.allowedIps}")
            appendLine("PersistentKeepalive = ${server.persistentKeepalive}")
        }
    }

    fun parse(confText: String): Config {
        if (confText.isBlank()) {
            throw ParseException(VpnError.INVALID_CONFIGURATION, "Yapılandırma metni boş")
        }
        return try {
            Config.parse(ByteArrayInputStream(confText.toByteArray(Charsets.UTF_8)))
        } catch (e: Exception) {
            // Teknik detay loglanır, kullanıcıya anlaşılır hata gösterilir (spec §10)
            throw ParseException(VpnError.INVALID_CONFIGURATION, "WireGuard config ayrıştırılamadı: ${e.message}")
        }
    }

    /** QR / metin içe aktarma için hafif doğrulama (tam parse [parse] ile yapılır). */
    fun looksLikeWireGuardConfig(text: String): Boolean =
        text.contains("[Interface]", ignoreCase = true) &&
            text.contains("[Peer]", ignoreCase = true) &&
            text.contains("PrivateKey", ignoreCase = true) &&
            text.contains("PublicKey", ignoreCase = true) &&
            text.contains("Endpoint", ignoreCase = true)
}
