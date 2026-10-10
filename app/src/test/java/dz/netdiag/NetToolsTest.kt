package dz.netdiag

import org.junit.Assert.*
import org.junit.Test

class NetToolsTest {
    @Test fun validHosts() {
        assertTrue(NetTools.isValidHost("8.8.8.8"))
        assertTrue(NetTools.isValidHost("192.168.1.1"))
        assertTrue(NetTools.isValidHost("google.dz"))
        assertTrue(NetTools.isValidHost("localhost"))
        assertFalse(NetTools.isValidHost(""))
        assertFalse(NetTools.isValidHost("999.1.1.1"))
        assertFalse(NetTools.isValidHost("bad host!"))
    }

    @Test fun ports() {
        assertTrue(NetTools.isValidPort(80))
        assertFalse(NetTools.isValidPort(0))
        assertFalse(NetTools.isValidPort(70000))
        assertEquals(listOf(80, 443), NetTools.parsePorts("80,443"))
        assertEquals(listOf(8000, 8001, 8002), NetTools.parsePorts("8000-8002"))
        assertEquals(listOf(22), NetTools.parsePorts("22, abc, 99999"))
        assertTrue(NetTools.parsePorts("").isEmpty())
    }

    @Test fun subnet() {
        val hosts = NetTools.hostsOf24("192.168.1.1")
        assertEquals(254, hosts.size)
        assertEquals("192.168.1.1", hosts.first())
        assertEquals("192.168.1.254", hosts.last())
        assertTrue(NetTools.hostsOf24("bad").isEmpty())
        assertEquals("192.168.1", NetTools.prefixOf("192.168.1.20"))
    }

    @Test fun pingSummary() {
        assertEquals("no reply", NetTools.pingSummary(emptyList()))
        val s = NetTools.pingSummary(listOf(10L, 20L, 30L))
        assertTrue(s.contains("avg 20ms"))
    }
}
