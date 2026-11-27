package com.cloudflaresecure.vpn.dns.doh

import com.cloudflaresecure.vpn.core.network.VpnError
import com.cloudflaresecure.vpn.dns.cache.DnsCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.net.Socket
import java.util.concurrent.TimeUnit
import kotlin.random.Random

/**
 * Cloudflare DNS-over-HTTPS motoru (spec §3, RFC 8484).
 *
 * Birincil: https://chrome.cloudflare-dns.com/dns-query
 * Yedek:    https://cloudflare-dns.com/dns-query
 *
 * Güvenlik politikası:
 * - DoH başarısız olursa ŞİFRESİZ DNS'e asla düşülmez.
 * - Yedek endpoint denenir; o da olmazsa [DohException] fırlatılır ve
 *   üst katman fail-closed (Kill Switch) politikasını uygular.
 * - [socketProtector] VpnService.protect() ile DoH soketini tünelden
 *   ayırır (tünel döngüsünü önler); sorgu içeriği yine tünel üzerinden
 *   taşınan trafikle çözülür — bkz. LocalDnsProxy.
 */
class CloudflareDohClient(
    private val client: OkHttpClient = defaultClient(),
    private val cache: DnsCache = DnsCache(),
    private val socketProtector: ((Socket) -> Boolean)? = null
) {
    companion object {
        const val PRIMARY_ENDPOINT = "https://chrome.cloudflare-dns.com/dns-query"
        const val FALLBACK_ENDPOINT = "https://cloudflare-dns.com/dns-query"
        private val DNS_MEDIA = "application/dns-message".toMediaType()

        fun defaultClient(): OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(5, TimeUnit.SECONDS)
            .writeTimeout(5, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
    }

    class DohException(val error: VpnError, message: String) : Exception(message)

    data class DohResult(
        val message: DnsMessageCodec.Message,
        val endpointUsed: String,
        val latencyMs: Long,
        val fromCache: Boolean
    )

    private var queryCount: Long = 0
    fun queryCount(): Long = queryCount

    suspend fun query(name: String, type: Int = DnsMessageCodec.TYPE_A): DohResult =
        withContext(Dispatchers.IO) {
            cache.get(name, type)?.let {
                return@withContext DohResult(it, "cache", 0, true)
            }
            val id = Random.nextInt(0, 65536)
            val wire = DnsMessageCodec.encodeQuery(id, name, type)
            var lastError: Exception? = null
            for (endpoint in listOf(PRIMARY_ENDPOINT, FALLBACK_ENDPOINT)) {
                try {
                    val started = System.currentTimeMillis()
                    val req = Request.Builder()
                        .url(endpoint)
                        .header("Accept", "application/dns-message")
                        .post(wire.toRequestBody(DNS_MEDIA))
                        .build()
                    client.newCall(req).execute().use { resp ->
                        if (!resp.isSuccessful) throw DohException(
                            VpnError.DNS_CONNECTION_FAILED, "HTTP ${resp.code}"
                        )
                        val body = resp.body?.bytes()
                            ?: throw DohException(VpnError.DNS_CONNECTION_FAILED, "Boş DoH yanıtı")
                        val msg = DnsMessageCodec.decodeMessage(body)
                        if (!msg.isResponse) throw DohException(
                            VpnError.DNS_CONNECTION_FAILED, "Geçersiz DoH yanıtı"
                        )
                        if (msg.rcode != 0) throw DohException(
                            VpnError.DNS_CONNECTION_FAILED, "DNS RCODE=${msg.rcode}"
                        )
                        cache.put(msg)
                        queryCount++
                        return@withContext DohResult(
                            msg, endpoint, System.currentTimeMillis() - started, false
                        )
                    }
                } catch (e: DohException) {
                    lastError = e
                } catch (e: Exception) {
                    lastError = e
                }
            }
            throw DohException(
                VpnError.DNS_CONNECTION_FAILED,
                "Tüm DoH endpoint'leri başarısız: ${lastError?.message}"
            )
        }

    /** Bağlantı testi + gecikme ölçümü (spec §3, §4C). */
    suspend fun testConnection(): DohResult = query("one.one.one.one", DnsMessageCodec.TYPE_A)

    fun clearCache() = cache.clear()
    fun cacheSize(): Int = cache.size()
}
