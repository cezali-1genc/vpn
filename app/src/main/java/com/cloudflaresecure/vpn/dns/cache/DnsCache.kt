package com.cloudflaresecure.vpn.dns.cache

import com.cloudflaresecure.vpn.dns.doh.DnsMessageCodec
import java.util.LinkedHashMap

/**
 * TTL duyarlı DNS önbelleği (spec §3).
 * Süresi dolmuş kayıtlar asla servis edilmez; LRU ile boyut sınırlanır.
 */
class DnsCache(private val maxEntries: Int = 512) {

    private data class Entry(val message: DnsMessageCodec.Message, val expiresAt: Long)

    private val map = object : LinkedHashMap<String, Entry>(64, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Entry>?): Boolean =
            size > maxEntries
    }

    @Synchronized
    fun get(name: String, type: Int, now: Long = System.currentTimeMillis()): DnsMessageCodec.Message? {
        val key = key(name, type)
        val e = map[key] ?: return null
        if (now > e.expiresAt) { map.remove(key); return null }
        return e.message
    }

    @Synchronized
    fun put(message: DnsMessageCodec.Message, now: Long = System.currentTimeMillis()) {
        val minTtl = message.answers.minOfOrNull { it.ttl } ?: 0L
        if (minTtl <= 0) return
        val q = message.questions.firstOrNull() ?: return
        map[key(q.name, q.type)] = Entry(message, now + minTtl * 1000)
    }

    @Synchronized
    fun clear() = map.clear()

    @Synchronized
    fun size(): Int = map.size

    private fun key(name: String, type: Int) = "${name.lowercase()}/$type"
}
