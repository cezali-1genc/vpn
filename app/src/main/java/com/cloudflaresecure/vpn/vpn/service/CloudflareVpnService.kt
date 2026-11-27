package com.cloudflaresecure.vpn.vpn.service

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Network
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import com.cloudflaresecure.vpn.core.network.ConnectionState
import com.cloudflaresecure.vpn.core.network.VpnError
import com.cloudflaresecure.vpn.dns.doh.CloudflareDohClient
import com.cloudflaresecure.vpn.dns.resolver.LocalDnsProxy
import com.cloudflaresecure.vpn.notifications.VpnNotificationHelper
import com.cloudflaresecure.vpn.servers.ServerRepository
import com.cloudflaresecure.vpn.settings.SettingsRepository
import com.cloudflaresecure.vpn.vpn.routing.KillSwitchManager
import com.cloudflaresecure.vpn.vpn.wireguard.WireGuardConfigParser
import com.cloudflaresecure.vpn.vpn.wireguard.WireGuardTunnelManager
import com.wireguard.android.backend.Tunnel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.net.InetAddress

/**
 * Gerçek VPN motoru: Android VpnService + WireGuard (spec §2).
 *
 * - TUN arayüzü Builder ile kurulur (IPv4 + IPv6 güvenli yönlendirme).
 * - WireGuard tüneli GoBackend ile açılır.
 * - DNS: yerel proxy (127.0.0.1) + Cloudflare DoH; şifresiz DNS'e düşülmez.
 * - onRevoke(), ağ değişimi, otomatik yeniden bağlanma, kaynak temizliği.
 */
class CloudflareVpnService : VpnService() {

    companion object {
        const val ACTION_CONNECT = "com.cloudflaresecure.vpn.CONNECT"
        const val ACTION_DISCONNECT = "com.cloudflaresecure.vpn.DISCONNECT"
        const val ACTION_RECONNECT = "com.cloudflaresecure.vpn.RECONNECT"

        fun start(context: Context) {
            val intent = Intent(context, CloudflareVpnService::class.java)
                .setAction(ACTION_CONNECT)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, CloudflareVpnService::class.java))
        }

        /** Android VPN izni gerekli mi? */
        fun prepare(context: Context): Intent? = VpnService.prepare(context)
    }

    private val scope = CoroutineScope(Dispatchers.Main + Job())
    private lateinit var tunnelManager: WireGuardTunnelManager
    private lateinit var connectionManager: ConnectionManager
    private lateinit var dnsProxy: LocalDnsProxy
    private lateinit var dohClient: CloudflareDohClient
    private var tunFd: ParcelFileDescriptor? = null
    private var networkCallback: ConnectivityManager.NetworkCallback? = null
    private var reconnectAttempts = 0

    override fun onCreate() {
        super.onCreate()
        tunnelManager = WireGuardTunnelManager(this)
        connectionManager = ConnectionManager(this)
        dohClient = CloudflareDohClient(socketProtector = { socket -> protect(socket) })
        dnsProxy = LocalDnsProxy(dohClient)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_DISCONNECT -> {
                disconnect(userInitiated = true)
                return START_NOT_STICKY
            }
            ACTION_RECONNECT -> {
                scope.launch { connect() }
                return START_STICKY
            }
            else -> {
                scope.launch { connect() }
                return START_STICKY
            }
        }
    }

    private suspend fun connect() {
        connectionManager.setState(ConnectionState.PREPARING)
        val settings = SettingsRepository(this).settings.first()
        val serverId = settings.activeServerId
        if (serverId == null) {
            connectionManager.setError(VpnError.SERVER_NOT_CONFIGURED)
            stopSelf()
            return
        }
        val serverRepo = ServerRepository(this)
        val server = serverRepo.getById(serverId)
        val privateKey = serverRepo.privateKeyFor(serverId)
        if (server == null || privateKey.isNullOrBlank()) {
            connectionManager.setError(VpnError.SERVER_NOT_CONFIGURED)
            stopSelf()
            return
        }

        connectionManager.setState(ConnectionState.CONNECTING)
        val config = try {
            WireGuardConfigParser.parse(
                WireGuardConfigParser.buildConfText(server, privateKey)
            )
        } catch (e: WireGuardConfigParser.ParseException) {
            connectionManager.setError(e.error)
            stopSelf()
            return
        }

        // TUN arayüzü: IPv4 tam tünel + IPv6 güvenli engelleme/yönlendirme
        val builder = Builder()
            .setSession("Cloudflare Secure VPN")
            .setMtu(1420)
            .addAddress("10.8.0.2", 32)
            .addRoute("0.0.0.0", 0)
            .addDnsServer("127.0.0.1")
            .setBlocking(true)
        // IPv6: her durumda adres + route tanımlanır (establish() null dönmesin).
        // Koruma açıksa IPv6 tünel içine alınır ve WireGuard üzerinden taşınır;
        // sunucu IPv6 desteklemiyorsa paketler düşer (sızıntı olmaz).
        builder.addAddress("fd00:8::2", 128)
        builder.addRoute("::", 0)
        tunFd = builder.establish()
        if (tunFd == null) {
            connectionManager.setError(VpnError.SERVICE_START_FAILED)
            stopSelf()
            return
        }

        // Foreground bildirimi (Android 14+ foregroundServiceType ile uyumlu)
        startForeground(
            VpnNotificationHelper.NOTIFICATION_ID,
            VpnNotificationHelper.buildNotification(this, server.name, 0)
        )

        connectionManager.setState(ConnectionState.AUTHENTICATING)
        try {
            tunnelManager.setConfig(config, "cloudflare-secure-vpn")
            val state = tunnelManager.start()
            if (state != Tunnel.State.UP) {
                connectionManager.setError(VpnError.HANDSHAKE_TIMEOUT)
                scheduleReconnect(settings.autoReconnect)
                return
            }
        } catch (e: Exception) {
            connectionManager.setError(VpnError.TUNNEL_INTERRUPTED)
            scheduleReconnect(settings.autoReconnect)
            return
        }

        // DNS proxy'yi başlat (TUN DNS -> DoH)
        dnsProxy.start(scope)
        registerNetworkCallback()
        reconnectAttempts = 0

        // 9 adımlı doğrulama sonrası CONNECTED yayınlanır
        connectionManager.runVerification(server.name)
    }

    private fun disconnect(userInitiated: Boolean) {
        connectionManager.setState(ConnectionState.DISCONNECTING)
        try { tunnelManager.stop() } catch (e: Exception) { /* yoksay */ }
        try { dnsProxy.stop() } catch (e: Exception) { /* yoksay */ }
        unregisterNetworkCallback()
        try { tunFd?.close() } catch (e: Exception) { /* yoksay */ }
        tunFd = null
        connectionManager.markDisconnected()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
        if (!userInitiated) {
            // Kill Switch: beklenmedik kesintide fail-closed
            val decision = KillSwitchManager().decide(
                ConnectionState.DISCONNECTED,
                killSwitchEnabled = true,
                autoReconnect = true
            )
            if (decision.shouldReconnect) {
                scope.launch {
                    delay(2000)
                    connect()
                }
            }
        }
    }

    private fun scheduleReconnect(autoReconnect: Boolean) {
        if (!autoReconnect) {
            disconnect(userInitiated = false)
            return
        }
        reconnectAttempts++
        if (reconnectAttempts > 5) {
            connectionManager.setError(VpnError.HANDSHAKE_TIMEOUT)
            disconnect(userInitiated = true)
            return
        }
        connectionManager.markReconnecting()
        scope.launch {
            delay(3000L * reconnectAttempts)
            connect()
        }
    }

    private fun registerNetworkCallback() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) return
        val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                connectionManager.onNetworkChanged()
            }
            override fun onLost(network: Network) {
                // Ağ kaybı: tünel korunmaya çalışılır, Kill Switch devreye girer
                connectionManager.markReconnecting()
            }
        }
        networkCallback = callback
        try { cm.registerDefaultNetworkCallback(callback) } catch (e: Exception) { /* yoksay */ }
    }

    private fun unregisterNetworkCallback() {
        val cb = networkCallback ?: return
        try {
            val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            cm.unregisterNetworkCallback(cb)
        } catch (e: Exception) { /* yoksay */ }
        networkCallback = null
    }

    override fun onRevoke() {
        // Sistem VPN'i geri çekti (kullanıcı sistemden kapattı)
        disconnect(userInitiated = true)
    }

    override fun onDestroy() {
        try { tunnelManager.stop() } catch (e: Exception) { /* yoksay */ }
        try { dnsProxy.stop() } catch (e: Exception) { /* yoksay */ }
        unregisterNetworkCallback()
        try { tunFd?.close() } catch (e: Exception) { /* yoksay */ }
        tunFd = null
        super.onDestroy()
    }

    /** DoH soketini tünelden korur (döngüyü önler). */
    override fun protect(socket: java.net.Socket): Boolean = super.protect(socket)

    /** Sunucu endpoint çözümlemesi için tünel dışı DNS gerekebilir. */
    @Suppress("DEPRECATION")
    fun resolveEndpoint(host: String): InetAddress? = try {
        InetAddress.getByName(host)
    } catch (e: Exception) {
        null
    }
}
