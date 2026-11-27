package com.cloudflaresecure.vpn.vpn.wireguard

import android.content.Context
import com.wireguard.android.backend.GoBackend
import com.wireguard.android.backend.Tunnel
import com.wireguard.config.Config

/** Resmî WireGuard GoBackend adaptörü (WireGuard Android 1.0.20231018 API). */
class WireGuardTunnelManager(context: Context) : Tunnel {

    private val backend = GoBackend(context)
    private var config: Config? = null
    private var name = "cloudflare-vpn"

    var onStateChanged: ((Tunnel.State) -> Unit)? = null

    override fun getName(): String = name

    override fun onStateChange(newState: Tunnel.State) {
        onStateChanged?.invoke(newState)
    }

    @Synchronized
    fun setConfig(config: Config, tunnelName: String = "cloudflare-vpn"): Tunnel.State {
        require(!Tunnel.isNameInvalid(tunnelName)) { "WireGuard tünel adı geçersiz" }
        name = tunnelName
        this.config = config
        return backend.getState(this)
    }

    @Synchronized
    fun start(): Tunnel.State {
        val activeConfig = config ?: throw IllegalStateException("Önce setConfig çağrılmalı")
        return backend.setState(this, Tunnel.State.UP, activeConfig)
    }

    @Synchronized
    fun stop(): Tunnel.State = backend.setState(this, Tunnel.State.DOWN, null)

    @Synchronized
    fun state(): Tunnel.State = backend.getState(this)
}
