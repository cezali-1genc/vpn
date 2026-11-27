package com.cloudflaresecure.vpn

import com.cloudflaresecure.vpn.servers.ServerValidator
import com.cloudflaresecure.vpn.servers.VpnServer
import org.junit.Assert.*
import org.junit.Test

class ServerValidatorTest {

    private val validKey = "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA="

    @Test
    fun validServer_passes() {
        val s = VpnServer(
            name = "Test", endpointHost = "vpn.example.com",
            serverPublicKey = validKey, clientAddress = "10.8.0.2/32"
        )
        assertNull(ServerValidator.validate(s))
    }

    @Test
    fun blankName_fails() {
        val s = VpnServer(
            name = "", endpointHost = "vpn.example.com",
            serverPublicKey = validKey, clientAddress = "10.8.0.2/32"
        )
        assertNotNull(ServerValidator.validate(s))
    }

    @Test
    fun invalidKey_fails() {
        val s = VpnServer(
            name = "Test", endpointHost = "vpn.example.com",
            serverPublicKey = "kısa", clientAddress = "10.8.0.2/32"
        )
        assertNotNull(ServerValidator.validate(s))
    }

    @Test
    fun invalidPort_fails() {
        val s = VpnServer(
            name = "Test", endpointHost = "vpn.example.com", endpointPort = 0,
            serverPublicKey = validKey, clientAddress = "10.8.0.2/32"
        )
        assertNotNull(ServerValidator.validate(s))
    }
}
