package com.cloudflaresecure.vpn

import com.cloudflaresecure.vpn.dns.doh.DnsMessageCodec
import org.junit.Assert.*
import org.junit.Test

class DnsMessageCodecTest {

    @Test
    fun encodeQuery_producesValidHeader() {
        val bytes = DnsMessageCodec.encodeQuery(0x1234, "one.one.one.one", DnsMessageCodec.TYPE_A)
        assertTrue(bytes.size > 12)
        assertEquals(0x12, bytes[0].toInt() and 0xFF)
        assertEquals(0x34, bytes[1].toInt() and 0xFF)
    }

    @Test
    fun decodeQuery_roundTrip() {
        val wire = DnsMessageCodec.encodeQuery(42, "example.com", DnsMessageCodec.TYPE_A)
        val msg = DnsMessageCodec.decodeMessage(wire)
        assertEquals(42, msg.id)
        assertFalse(msg.isResponse)
        assertEquals(1, msg.questions.size)
        assertEquals("example.com", msg.questions[0].name)
        assertEquals(DnsMessageCodec.TYPE_A, msg.questions[0].type)
    }

    @Test
    fun decode_shortMessage_throws() {
        try {
            DnsMessageCodec.decodeMessage(byteArrayOf(1, 2, 3))
            fail("Beklenen: IllegalArgumentException")
        } catch (e: IllegalArgumentException) {
            // beklenen
        }
    }
}
