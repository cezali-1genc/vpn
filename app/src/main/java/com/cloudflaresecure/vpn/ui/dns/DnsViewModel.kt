package com.cloudflaresecure.vpn.ui.dns

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cloudflaresecure.vpn.core.security.LeakChecker
import com.cloudflaresecure.vpn.dns.doh.CloudflareDohClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class DnsUiState(
    val dohOk: Boolean? = null,
    val latencyMs: Long? = null,
    val externalIp: String? = null,
    val dnsLeakFree: Boolean? = null,
    val queryCount: Long = 0,
    val cacheSize: Int = 0,
    val isTesting: Boolean = false,
    val message: String? = null
)

class DnsViewModel : ViewModel() {

    private val doh = CloudflareDohClient()
    private val leakChecker = LeakChecker(doh = doh)

    private val _uiState = MutableStateFlow(DnsUiState())
    val uiState: StateFlow<DnsUiState> = _uiState.asStateFlow()

    fun testDoh() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isTesting = true)
            try {
                val result = doh.testConnection()
                _uiState.value = _uiState.value.copy(
                    dohOk = true,
                    latencyMs = result.latencyMs,
                    queryCount = doh.queryCount(),
                    cacheSize = doh.cacheSize(),
                    isTesting = false,
                    message = "DoH OK (${result.endpointUsed}) — ${result.latencyMs} ms"
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    dohOk = false,
                    isTesting = false,
                    message = "DoH başarısız: ${e.message}. Şifresiz DNS'e düşülmedi."
                )
            }
        }
    }

    fun checkLeak() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isTesting = true)
            val report = leakChecker.check()
            _uiState.value = _uiState.value.copy(
                externalIp = report.externalIp,
                dnsLeakFree = report.dnsLeakFree,
                dohOk = report.dohOk,
                latencyMs = report.dohLatencyMs,
                isTesting = false,
                message = if (report.dnsLeakFree) "DNS sızıntısı yok." else "Sızıntı kontrolü tamamlanamadı."
            )
        }
    }

    fun clearCache() {
        doh.clearCache()
        _uiState.value = _uiState.value.copy(cacheSize = 0, message = "DNS önbelleği temizlendi.")
    }
}
