package com.cloudflaresecure.vpn

import com.cloudflaresecure.vpn.vpn.wireguard.WireGuardConfigParser
import org.junit.Assert.*
import org.junit.Test

class WireGuardConfigParserTest {

    @Test
    fun looksLike_validConfig() {
        val text = """
            [Interface]
            PrivateKey = AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=
            Address = 10.8.0.2/32
            [Peer]
            PublicKey = AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=
            Endpoint = vpn.example.com:51820
            AllowedIPs = 0.0.0.0/0
        """.trimIndent()
        assertTrue(WireGuardConfigParser.looksLikeWireGuardConfig(text))
    }

    @Test
    fun looksLike_invalidText() {
        assertFalse(WireGuardConfigParser.looksLikeWireGuardConfig("merhaba dünya"))
    }

    @Test
    fun buildConf_containsSections() {
        val server = com.cloudflaresecure.vpn.servers.VpnServer(
            name = "T", endpointHost = "vpn.example.com",
            serverPublicKey = "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=",
            clientAddress = "10.8.0.2/32"
        )
        val conf = WireGuardConfigParser.buildConfText(
            server, "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA="
        )
        assertTrue(conf.contains("[Interface]"))
        assertTrue(conf.contains("[Peer]"))
        assertTrue(conf.contains("vpn.example.com:51820"))
    }
}
