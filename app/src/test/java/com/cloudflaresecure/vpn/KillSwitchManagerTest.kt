package com.cloudflaresecure.vpn

import com.cloudflaresecure.vpn.core.network.ConnectionState
import com.cloudflaresecure.vpn.vpn.routing.KillSwitchManager
import org.junit.Assert.*
import org.junit.Test

class KillSwitchManagerTest {

    private val manager = KillSwitchManager()

    @Test
    fun connected_doesNotBlock() {
        val d = manager.decide(ConnectionState.CONNECTED, true, true)
        assertFalse(d.shouldBlockTraffic)
    }

    @Test
    fun errorWithKillSwitch_blocks() {
        val d = manager.decide(ConnectionState.ERROR, true, true)
        assertTrue(d.shouldBlockTraffic)
        assertTrue(d.shouldReconnect)
    }

    @Test
    fun errorWithoutKillSwitch_doesNotBlock() {
        val d = manager.decide(ConnectionState.ERROR, false, false)
        assertFalse(d.shouldBlockTraffic)
    }
}
