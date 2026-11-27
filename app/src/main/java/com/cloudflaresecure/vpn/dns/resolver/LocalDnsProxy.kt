package com.cloudflaresecure.vpn.dns.resolver

import com.cloudflaresecure.vpn.dns.doh.CloudflareDohClient
import com.cloudflaresecure.vpn.dns.doh.DnsMessageCodec
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress

/**
 * TUN üzerinden gelen DNS paketlerini işleyen yerel DNS proxy (spec §3).
 *
 * Akış: İstemci DNS sorgusu (UDP) -> bu proxy -> Cloudflare DoH -> yanıt geri.
 * DoH başarısız olursa SERVFAIL döner; şifresiz DNS'e düşülmez (fail-closed).
 *
 * Not: Android'de TUN fd'sinden doğrudan okuma yerine, VpnService.Builder
 * .addDnsServer("127.0.0.1") + bu proxy deseni kullanılır; CloudflareVpnService
 * bu sınıfı başlatır. DoH soketleri VpnService.protect() ile korunur.
 */
class LocalDnsProxy(
    private val doh: CloudflareDohClient,
    private val port: Int = 5353
) {
    private var socket: DatagramSocket? = null
    private var job: Job? = null

    @Synchronized
    fun start(scope: CoroutineScope) {
        if (socket != null) return
        val s = DatagramSocket(port, InetAddress.getByName("127.0.0.1"))
        socket = s
        job = scope.launch(Dispatchers.IO) {
            val buf = ByteArray(512)
            while (!s.isClosed) {
                try {
                    val pkt = DatagramPacket(buf, buf.size)
                    s.receive(pkt)
                    val queryBytes = pkt.data.copyOfRange(pkt.offset, pkt.offset + pkt.length)
                    launch {
                        val reply = resolve(queryBytes)
                        if (reply != null) {
                            s.send(DatagramPacket(reply, reply.size, pkt.address, pkt.port))
                        }
                    }
                } catch (e: Exception) {
                    if (s.isClosed) break
                }
            }
        }
    }

    private suspend fun resolve(queryBytes: ByteArray): ByteArray? {
        return try {
            val q = DnsMessageCodec.decodeMessage(queryBytes)
            val question = q.questions.firstOrNull() ?: return servFail(queryBytes)
            val result = doh.query(question.name, question.type)
            // Yanıt her zaman istemci ID'si ile kurulur (buildResponse q.id kullanır).
            buildResponse(q, result.message)
        } catch (e: Exception) {
            servFail(queryBytes)
        }
    }

    private fun buildResponse(
        query: DnsMessageCodec.Message,
        answer: DnsMessageCodec.Message
    ): ByteArray? {
        // Yanıt inşası: sorgu sorusu + DoH cevap kayıtları.
        // Kayıtların wire kodlaması için ham DoH yanıtı gerekir; bu katman
        // codec üzerinden yeniden kodlar (A/AAAA için).
        return try {
            val out = java.io.ByteArrayOutputStream()
            val d = java.io.DataOutputStream(out)
            d.writeShort(query.id)
            d.writeShort(0x8180) // QR=1, RD=1, RA=1, RCODE=0
            d.writeShort(query.questions.size)
            d.writeShort(answer.answers.size)
            d.writeShort(0); d.writeShort(0)
            for (qq in query.questions) {
                writeName(d, qq.name); d.writeShort(qq.type); d.writeShort(qq.clazz)
            }
            for (rec in answer.answers) {
                writeName(d, rec.name)
                d.writeShort(rec.type); d.writeShort(rec.clazz)
                d.writeInt(rec.ttl.toInt())
                d.writeShort(rec.rdata.size); d.write(rec.rdata)
            }
            d.flush()
            out.toByteArray()
        } catch (e: Exception) {
            null
        }
    }

    private fun writeName(d: java.io.DataOutputStream, name: String) {
        val clean = name.trim().trimEnd('.')
        if (clean.isEmpty()) { d.writeByte(0); return }
        for (label in clean.split('.')) {
            val b = label.toByteArray(Charsets.US_ASCII)
            d.writeByte(b.size); d.write(b)
        }
        d.writeByte(0)
    }

    private fun servFail(queryBytes: ByteArray): ByteArray? {
        if (queryBytes.size < 12) return null
        val copy = queryBytes.copyOf()
        copy[3] = ((copy[3].toInt() and 0xF0) or 0x02).toByte() // RCODE=2 SERVFAIL
        return copy
    }

    @Synchronized
    fun stop() {
        job?.cancel()
        socket?.close()
        socket = null
        job = null
    }
}
