package com.cloudflaresecure.vpn.dns.doh

import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.net.InetAddress

/**
 * RFC 1035 / RFC 8484 binary DNS mesajı kodlayıcı-çözücü (spec §3).
 * DoH motoru bu codec ile gerçek binary DNS mesajları üretir/çözer.
 * Desteklenen tipler: A (1), CNAME (5), AAAA (28).
 */
object DnsMessageCodec {

    const val TYPE_A = 1
    const val TYPE_CNAME = 5
    const val TYPE_AAAA = 28
    const val CLASS_IN = 1

    data class Question(val name: String, val type: Int, val clazz: Int = CLASS_IN)

    data class Record(
        val name: String,
        val type: Int,
        val clazz: Int,
        val ttl: Long,
        val rdata: ByteArray
    ) {
        fun asIpString(): String? = when (type) {
            TYPE_A -> if (rdata.size == 4) rdata.joinToString(".") { (it.toInt() and 0xFF).toString() } else null
            TYPE_AAAA -> if (rdata.size == 16) {
                InetAddress.getByAddress(rdata).hostAddress
            } else null
            else -> null
        }
    }

    data class Message(
        val id: Int,
        val isResponse: Boolean,
        val rcode: Int,
        val questions: List<Question>,
        val answers: List<Record>
    )

    fun encodeQuery(id: Int, name: String, type: Int): ByteArray {
        val out = ByteArrayOutputStream()
        val d = DataOutputStream(out)
        d.writeShort(id)
        d.writeShort(0x0100) // RD=1 (recursion desired)
        d.writeShort(1) // QDCOUNT
        d.writeShort(0) // ANCOUNT
        d.writeShort(0) // NSCOUNT
        d.writeShort(0) // ARCOUNT
        writeName(d, name)
        d.writeShort(type)
        d.writeShort(CLASS_IN)
        d.flush()
        return out.toByteArray()
    }

    fun decodeMessage(bytes: ByteArray): Message {
        require(bytes.size >= 12) { "DNS mesajı çok kısa" }
        var pos = 0
        fun u16(): Int {
            val v = ((bytes[pos].toInt() and 0xFF) shl 8) or (bytes[pos + 1].toInt() and 0xFF)
            pos += 2
            return v
        }
        fun u32(): Long {
            var v = 0L
            repeat(4) { v = (v shl 8) or (bytes[pos].toInt() and 0xFF).toLong(); pos++ }
            return v
        }
        val id = u16()
        val flags = u16()
        val isResponse = (flags and 0x8000) != 0
        val rcode = flags and 0x000F
        val qd = u16(); val an = u16(); val ns = u16(); val ar = u16()

        fun readName(): String {
            val labels = mutableListOf<String>()
            var jumped = false
            var jumpPos = -1
            var guard = 0
            while (true) {
                if (++guard > 128) throw IllegalArgumentException("DNS isim döngüsü")
                val len = bytes[pos].toInt() and 0xFF
                if (len == 0) {
                    pos++
                    break
                }
                if ((len and 0xC0) == 0xC0) {
                    val ptr = ((len and 0x3F) shl 8) or (bytes[pos + 1].toInt() and 0xFF)
                    if (!jumped) { jumpPos = pos + 2; jumped = true }
                    pos = ptr
                } else {
                    pos++
                    val start = pos
                    pos += len
                    labels.add(String(bytes, start, len, Charsets.US_ASCII))
                }
            }
            if (jumped) pos = jumpPos
            return labels.joinToString(".")
        }

        val questions = mutableListOf<Question>()
        repeat(qd) {
            val n = readName(); val t = u16(); val c = u16()
            questions.add(Question(n, t, c))
        }
        fun readRecords(count: Int): List<Record> {
            val list = mutableListOf<Record>()
            repeat(count) {
                val n = readName(); val t = u16(); val c = u16(); val ttl = u32(); val rdlen = u16()
                val rdata = bytes.copyOfRange(pos, pos + rdlen)
                pos += rdlen
                list.add(Record(n, t, c, ttl, rdata))
            }
            return list
        }
        val answers = readRecords(an)
        readRecords(ns); readRecords(ar) // atıf/ek kayıtlar atlanır
        return Message(id, isResponse, rcode, questions, answers)
    }

    private fun writeName(d: DataOutputStream, name: String) {
        val clean = name.trim().trimEnd('.')
        if (clean.isEmpty()) { d.writeByte(0); return }
        for (label in clean.split('.')) {
            val b = label.toByteArray(Charsets.US_ASCII)
            require(b.size in 1..63) { "Geçersiz DNS etiketi: $label" }
            d.writeByte(b.size)
            d.write(b)
        }
        d.writeByte(0)
    }
}
