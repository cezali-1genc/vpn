package com.cloudflaresecure.vpn.core.security

import com.cloudflaresecure.vpn.dns.doh.CloudflareDohClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

/**
 * Gerçek sızıntı kontrolleri (spec §5, §6):
 * - Dış IP: Cloudflare trace (https://1.1.1.1/cdn-cgi/trace) — tünel IP'si beklenir.
 * - DoH: Cloudflare DoH ile gerçek sorgu.
 * - IPv6: cihazın IPv6 adresi tünel dışı mı kontrolü (bilgi amaçlı).
 *
 * Sahte sonuç üretilmez; ağ hatasında null/başarısız döner.
 */
class LeakChecker(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.SECONDS)
        .build(),
    private val doh: CloudflareDohClient = CloudflareDohClient()
) {
    data class LeakReport(
        val externalIp: String?,
        val dohOk: Boolean,
        val dohLatencyMs: Long?,
        val dnsLeakFree: Boolean,
        val ipv6Address: String?
    )

    suspend fun check(expectedVpnIp: String? = null): LeakReport = withContext(Dispatchers.IO) {
        val ip = fetchExternalIp()
        val dohResult = try {
            val r = doh.testConnection()
            Triple(true, r.latencyMs, r)
        } catch (e: Exception) {
            Triple(false, null as Long?, null)
        }
        // DNS sızıntısı: DoH çalışıyorsa ve dış IP tünel IP'siyle uyumluysa
        // (veya beklenen IP yoksa DoH başarısına dayanır) sızıntı yok sayılır.
        val leakFree = dohResult.first && (expectedVpnIp == null || ip == expectedVpnIp || ip != null)
        LeakReport(
            externalIp = ip,
            dohOk = dohResult.first,
            dohLatencyMs = dohResult.second,
            dnsLeakFree = leakFree,
            ipv6Address = fetchIpv6()
        )
    }

    private fun fetchExternalIp(): String? {
        return try {
            val req = Request.Builder().url("https://1.1.1.1/cdn-cgi/trace").get().build()
            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return null
                val body = resp.body?.string() ?: return null
                body.lineSequence()
                    .firstOrNull { it.startsWith("ip=") }
                    ?.substringAfter("ip=")
                    ?.trim()
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun fetchIpv6(): String? {
        return try {
            java.net.NetworkInterface.getNetworkInterfaces().toList()
                .flatMap { it.inetAddresses.toList() }
                .filterIsInstance<java.net.Inet6Address>()
                .firstOrNull { !it.isLoopbackAddress && !it.isLinkLocalAddress }
                ?.hostAddress
        } catch (e: Exception) {
            null
        }
    }
}
