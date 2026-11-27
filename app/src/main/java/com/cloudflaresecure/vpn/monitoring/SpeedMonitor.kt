package com.cloudflaresecure.vpn.monitoring

import android.net.TrafficStats
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit
import kotlin.math.max

/**
 * Gerçek hız/ping ölçümü (spec §4E).
 * Sahte hız üretilmez:
 * - Ping: DoH sorgu gecikmesi (gerçek ağ turu).
 * - İndirme/yükleme: TrafficStats sayaç farkı (gerçek aktarılan veri).
 * - Aktif hız testi: Cloudflare'a gerçek veri aktarımı (kullanıcı başlatır).
 */
class SpeedMonitor(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()
) {
    data class SpeedResult(
        val downloadMbps: Double?,
        val uploadMbps: Double?,
        val pingMs: Long?,
        val bytesReceived: Long,
        val bytesTransmitted: Long
    )

    suspend fun measureTrafficDelta(intervalMs: Long = 1000): SpeedResult =
        withContext(Dispatchers.IO) {
            val rx0 = TrafficStats.getTotalRxBytes()
            val tx0 = TrafficStats.getTotalTxBytes()
            kotlinx.coroutines.delay(intervalMs)
            val rx1 = TrafficStats.getTotalRxBytes()
            val tx1 = TrafficStats.getTotalTxBytes()
            val rx = max(0, rx1 - rx0)
            val tx = max(0, tx1 - tx0)
            val seconds = intervalMs / 1000.0
            SpeedResult(
                downloadMbps = rx * 8 / 1_000_000.0 / seconds,
                uploadMbps = tx * 8 / 1_000_000.0 / seconds,
                pingMs = null,
                bytesReceived = rx,
                bytesTransmitted = tx
            )
        }

    /** Gerçek indirme testi: Cloudflare hız uç noktasından veri indirir. */
    suspend fun measureDownload(sizeBytes: Int = 5_000_000): Double? = withContext(Dispatchers.IO) {
        try {
            val req = Request.Builder()
                .url("https://speed.cloudflare.com/__down?bytes=$sizeBytes")
                .get().build()
            val started = System.currentTimeMillis()
            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return@withContext null
                val body = resp.body ?: return@withContext null
                val bytes = body.bytes()
                val elapsedSec = (System.currentTimeMillis() - started) / 1000.0
                if (elapsedSec <= 0) return@withContext null
                bytes.size * 8 / 1_000_000.0 / elapsedSec
            }
        } catch (e: Exception) {
            null
        }
    }

    suspend fun measureUpload(sizeBytes: Int = 1_000_000): Double? = withContext(Dispatchers.IO) {
        try {
            val payload = ByteArray(sizeBytes) { 0 }
            val body = payload.toRequestBody("application/octet-stream".toMediaTypeOrNull())
            val req = Request.Builder()
                .url("https://speed.cloudflare.com/__up")
                .post(body).build()
            val started = System.currentTimeMillis()
            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return@withContext null
                val elapsedSec = (System.currentTimeMillis() - started) / 1000.0
                if (elapsedSec <= 0) return@withContext null
                sizeBytes * 8 / 1_000_000.0 / elapsedSec
            }
        } catch (e: Exception) {
            null
        }
    }

}
