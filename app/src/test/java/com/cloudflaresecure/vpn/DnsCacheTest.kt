package com.cloudflaresecure.vpn

import com.cloudflaresecure.vpn.dns.cache.DnsCache
import com.cloudflaresecure.vpn.dns.doh.DnsMessageCodec
import org.junit.Assert.*
import org.junit.Test

class DnsCacheTest {

    private fun message(ttl: Long): DnsMessageCodec.Message {
        val q = DnsMessageCodec.Question("example.com", DnsMessageCodec.TYPE_A)
        val r = DnsMessageCodec.Record(
            "example.com", DnsMessageCodec.TYPE_A, 1, ttl,
            byteArrayOf(93, 184.toByte(), 216.toByte(), 34)
        )
        return DnsMessageCodec.Message(1, true, 0, listOf(q), listOf(r))
    }

    @Test
    fun putAndGet_withinTtl() {
        val cache = DnsCache()
        cache.put(message(300), now = 1000)
        val got = cache.get("example.com", DnsMessageCodec.TYPE_A, now = 2000)
        assertNotNull(got)
        assertEquals(1, got!!.answers.size)
    }

    @Test
    fun expiredEntry_notReturned() {
        val cache = DnsCache()
        cache.put(message(10), now = 1000)
        val got = cache.get("example.com", DnsMessageCodec.TYPE_A, now = 1000 + 11_000)
        assertNull(got)
    }

    @Test
    fun zeroTtl_notCached() {
        val cache = DnsCache()
        cache.put(message(0), now = 1000)
        assertEquals(0, cache.size())
    }
}
