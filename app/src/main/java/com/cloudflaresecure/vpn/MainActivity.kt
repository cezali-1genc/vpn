package com.cloudflaresecure.vpn

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import com.cloudflaresecure.vpn.settings.SettingsRepository
import com.cloudflaresecure.vpn.settings.VpnSettings
import com.cloudflaresecure.vpn.ui.dns.DnsScreen
import com.cloudflaresecure.vpn.ui.dns.DnsViewModel
import com.cloudflaresecure.vpn.ui.home.HomeScreen
import com.cloudflaresecure.vpn.ui.home.HomeViewModel
import com.cloudflaresecure.vpn.ui.security.SecurityScreen
import com.cloudflaresecure.vpn.ui.servers.ServersScreen
import com.cloudflaresecure.vpn.ui.servers.ServersViewModel
import com.cloudflaresecure.vpn.ui.settings.HelpScreen
import com.cloudflaresecure.vpn.ui.settings.SettingsScreen
import com.cloudflaresecure.vpn.ui.theme.CloudflareVpnTheme
import kotlinx.coroutines.launch

private data class Tab(val label: String, val icon: ImageVector)

class MainActivity : ComponentActivity() {

    private lateinit var homeViewModel: HomeViewModel
    private lateinit var serversViewModel: ServersViewModel
    private lateinit var dnsViewModel: DnsViewModel
    private lateinit var settingsRepo: SettingsRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        homeViewModel = HomeViewModel(this)
        serversViewModel = ServersViewModel(this)
        dnsViewModel = DnsViewModel()
        settingsRepo = SettingsRepository(this)

        setContent {
            val settings by settingsRepo.settings.collectAsState(initial = VpnSettings())
            CloudflareVpnTheme(darkTheme = settings.darkTheme) {
                VpnApp(
                    settings = settings,
                    onUpdateSettings = { transform -> updateSettings(transform) }
                )
            }
        }
    }

    private fun updateSettings(transform: suspend (VpnSettings) -> VpnSettings) {
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            settingsRepo.update(transform)
        }
    }

    @Composable
    private fun VpnApp(settings: VpnSettings, onUpdateSettings: (suspend (VpnSettings) -> VpnSettings) -> Unit) {
        val tabs = listOf(
            Tab("Ana Sayfa", Icons.Filled.Home),
            Tab("Sunucular", Icons.Filled.Public),
            Tab("DNS", Icons.Filled.Language),
            Tab("Güvenlik", Icons.Filled.Lock),
            Tab("Ayarlar", Icons.Filled.Settings)
        )
        var selected by remember { mutableIntStateOf(0) }
        val homeState by homeViewModel.uiState.collectAsState()
        val serversState by serversViewModel.uiState.collectAsState()
        val dnsState by dnsViewModel.uiState.collectAsState()
        val context = LocalContext.current
        val scope = rememberCoroutineScope()

        Scaffold(
            bottomBar = {
                NavigationBar {
                    tabs.forEachIndexed { index, tab ->
                        NavigationBarItem(
                            selected = selected == index,
                            onClick = { selected = index },
                            icon = { Icon(tab.icon, contentDescription = tab.label) },
                            label = { Text(tab.label) }
                        )
                    }
                }
            }
        ) { padding ->
            androidx.compose.foundation.layout.Box(
                modifier = Modifier.padding(padding)
            ) {
            when (selected) {
                0 -> HomeScreen(
                    state = homeState,
                    onConnect = { homeViewModel.onConnectClicked(this@MainActivity) },
                    onDisconnect = { homeViewModel.onDisconnectClicked(context) },
                    onMeasureSpeed = { homeViewModel.measureSpeed() },
                    onOpenServers = { selected = 1 }
                )
                1 -> ServersScreen(
                    state = serversState,
                    onAdd = { s, k -> serversViewModel.addServer(s, k) },
                    onDelete = { serversViewModel.deleteServer(it) },
                    onSetActive = { serversViewModel.setActive(it) },
                    onTest = { serversViewModel.testServer(it) },
                    onImport = { text, name -> serversViewModel.importConfig(text, name) }
                )
                2 -> DnsScreen(
                    state = dnsState,
                    onTest = { dnsViewModel.testDoh() },
                    onLeak = { dnsViewModel.checkLeak() },
                    onClear = { dnsViewModel.clearCache() }
                )
                3 -> SecurityScreen(
                    settings = settings,
                    onKillSwitch = { v -> scope.launch { onUpdateSettings { it.copy(killSwitchEnabled = v) } } },
                    onDnsLeak = { v -> scope.launch { onUpdateSettings { it.copy(dnsLeakProtection = v) } } },
                    onIpv6Leak = { v -> scope.launch { onUpdateSettings { it.copy(ipv6LeakProtection = v) } } }
                )
                4 -> Column {
                    SettingsScreen(
                        settings = settings,
                        onAutoConnect = { v -> scope.launch { onUpdateSettings { it.copy(autoConnect = v) } } },
                        onConnectOnBoot = { v -> scope.launch { onUpdateSettings { it.copy(connectOnBoot = v) } } },
                        onAutoReconnect = { v -> scope.launch { onUpdateSettings { it.copy(autoReconnect = v) } } },
                        onDarkTheme = { v -> scope.launch { onUpdateSettings { it.copy(darkTheme = v) } } },
                        onNotifications = { v -> scope.launch { onUpdateSettings { it.copy(notificationsEnabled = v) } } },
                        onLanguage = { v -> scope.launch { onUpdateSettings { it.copy(languageCode = v) } } },
                        onClearHistory = { dnsViewModel.clearCache() },
                        versionName = BuildConfig.VERSION_NAME
                    )
                }
            }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == HomeViewModel.VPN_REQUEST_CODE) {
            homeViewModel.onVpnPermissionResult(resultCode == Activity.RESULT_OK, this)
        }
    }
}
