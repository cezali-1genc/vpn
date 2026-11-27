package com.cloudflaresecure.vpn.vpn.routing

import com.cloudflaresecure.vpn.core.network.ConnectionState

/**
 * Gerçek Kill Switch mantığı (spec §6).
 *
 * - VPN kesilirse / tünel hatasında fail-closed: trafik engellenir,
 *   şifresiz DNS'e düşülmez, bağlantı ERROR/RECONNECTING'e alınır.
 * - Android lockdown modu sistem ayarıdır; uygulama bunu kendi başına
 *   etkinleştirdiğini İDDİA ETMEZ. Kullanıcı, Güvenlik ekranındaki
 *   rehberle Android Ayarları > Ağ > VPN > Always-on/Lockdown'a yönlendirilir.
 */
class KillSwitchManager {

    data class Decision(
        val shouldBlockTraffic: Boolean,
        val shouldReconnect: Boolean,
        val reason: String
    )

    fun decide(
        state: ConnectionState,
        killSwitchEnabled: Boolean,
        autoReconnect: Boolean
    ): Decision {
        return when (state) {
            ConnectionState.CONNECTED -> Decision(false, false, "Tünel aktif")
            ConnectionState.ERROR, ConnectionState.DISCONNECTED -> {
                if (killSwitchEnabled) {
                    Decision(
                        shouldBlockTraffic = true,
                        shouldReconnect = autoReconnect,
                        reason = "Kill Switch: tünel yokken trafik engellendi (fail-closed)"
                    )
                } else {
                    Decision(false, autoReconnect, "Kill Switch kapalı")
                }
            }
            ConnectionState.RECONNECTING, ConnectionState.CONNECTING,
            ConnectionState.PREPARING, ConnectionState.AUTHENTICATING -> {
                Decision(
                    shouldBlockTraffic = killSwitchEnabled,
                    shouldReconnect = true,
                    reason = "Bağlantı kurulurken güvenli bekleme"
                )
            }
            ConnectionState.DISCONNECTING -> Decision(false, false, "Kullanıcı kesiyor")
        }
    }
}
