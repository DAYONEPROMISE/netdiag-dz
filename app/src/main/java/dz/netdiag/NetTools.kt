package dz.netdiag

import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket

/** Pure, unit-testable helpers. All clean-room code, MIT. */
object NetTools {
    fun isValidHost(host: String): Boolean {
        val h = host.trim()
        if (h.isEmpty() || h.length > 253) return false
        if (h == "localhost") return true
        val ipv4 = Regex("""^(\d{1,3}\.){3}\d{1,3}$""")
        if (ipv4.matches(h)) {
            return h.split(".").all { (it.toIntOrNull() ?: 256) in 0..255 }
        }
        return Regex("""^[A-Za-z0-9]([A-Za-z0-9._-]{0,251}[A-Za-z0-9])?$""").matches(h)
    }

    fun isValidPort(port: Int): Boolean = port in 1..65535

    fun parsePorts(spec: String): List<Int> {
        // Accepts "80", "80,443", "8000-8002". Invalid tokens are dropped.
        val out = mutableSetOf<Int>()
        for (token in spec.split(",")) {
            val t = token.trim()
            if (t.isEmpty()) continue
            if ("-" in t) {
                val parts = t.split("-").map { it.trim().toIntOrNull() }
                val a = parts.getOrNull(0)
                val b = parts.getOrNull(1)
                if (a != null && b != null && isValidPort(a) && isValidPort(b) && a <= b && b - a <= 64) {
                    for (p in a..b) out.add(p)
                }
            } else {
                val p = t.toIntOrNull()
                if (p != null && isValidPort(p)) out.add(p)
            }
        }
        return out.sorted().take(65)
    }

    /** Enumerate /24 host addresses from a base like "192.168.1". Skips .0 and .255. */
    fun hostsOf24(base: String): List<String> {
        if (!Regex("""^(\d{1,3}\.){3}\d{1,3}$""").matches(base.trim())) return emptyList()
        val prefix = base.trim().substringBeforeLast(".")
        val octets = prefix.split(".")
        if (octets.size != 3 || octets.any { (it.toIntOrNull() ?: -1) !in 0..255 }) return emptyList()
        return (1..254).map { "$prefix.$it" }
    }

    fun prefixOf(ip: String): String {
        val parts = ip.trim().split(".")
        return if (parts.size == 4) parts.take(3).joinToString(".") else ""
    }

    fun pingSummary(timesMs: List<Long>): String {
        if (timesMs.isEmpty()) return "no reply"
        val min = timesMs.min()
        val max = timesMs.max()
        val avg = timesMs.average()
        return "avg ${avg.toLong()}ms min ${min}ms max ${max}ms (${timesMs.size} replies)"
    }
}

data class PingResult(val host: String, val reachable: Boolean, val timeMs: Long)
data class PortResult(val host: String, val port: Int, val open: Boolean, val timeMs: Long)

suspend fun pingHost(host: String, timeoutMs: Int = 1500): PingResult {
    return try {
        val t0 = System.currentTimeMillis()
        val ok = InetAddress.getByName(host).isReachable(timeoutMs)
        PingResult(host, ok, System.currentTimeMillis() - t0)
    } catch (e: Exception) {
        PingResult(host, false, -1)
    }
}

suspend fun dnsLookup(host: String): List<String> {
    return try {
        InetAddress.getAllByName(host).map { it.hostAddress ?: "" }.filter { it.isNotEmpty() }.distinct()
    } catch (e: Exception) {
        emptyList()
    }
}

suspend fun checkPort(host: String, port: Int, timeoutMs: Int = 2000): PortResult {
    return try {
        val t0 = System.currentTimeMillis()
        Socket().use { s ->
            s.connect(InetSocketAddress(host, port), timeoutMs)
        }
        PortResult(host, port, true, System.currentTimeMillis() - t0)
    } catch (e: Exception) {
        PortResult(host, port, false, -1)
    }
}
