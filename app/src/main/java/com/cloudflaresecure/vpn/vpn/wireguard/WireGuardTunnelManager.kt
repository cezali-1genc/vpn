package com.cloudflaresecure.vpn.vpn.wireguard

import android.content.Context
import com.wireguard.android.backend.GoBackend
import com.wireguard.android.backend.Tunnel
import com.wireguard.config.Config

/**
 * Resmî WireGuard Tunnel Library sarmalayıcı (spec §2).
 * Gerçek TUN arayüzü + şifreli WireGuard bağlantısı GoBackend üzerinden kurulur.
 */
class WireGuardTunnelManager(context: Context) : Tunnel.StateChangeListener {

    private val backend = GoBackend(context)
    private var tunnel: Tunnel? = null

    var onStateChanged: ((Tunnel.State) -> Unit)? = null

    @Synchronized
    fun setConfig(config: Config, tunnelName: String = "cloudflare-secure-vpn"): Tunnel.State {
        val existing = tunnel
        val t = if (existing == null) {
            backend.createTunnel(tunnelName, this)
        } else {
            existing
        }
        tunnel = t
        return t.setConfig(config)
    }

    @Synchronized
    fun start(): Tunnel.State {
        val t = tunnel ?: throw IllegalStateException("Önce setConfig çağrılmalı")
        return t.onStateChange(Tunnel.State.UP)
    }

    @Synchronized
    fun stop(): Tunnel.State {
        val t = tunnel ?: return Tunnel.State.DOWN
        return try {
            t.onStateChange(Tunnel.State.DOWN)
        } finally {
            tunnel = null
        }
    }

    fun state(): Tunnel.State = tunnel?.state ?: Tunnel.State.DOWN

    override fun onStateChange(state: Tunnel.State) {
        onStateChanged?.invoke(state)
    }
}
