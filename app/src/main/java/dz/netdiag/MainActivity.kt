package dz.netdiag

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.wifi.WifiManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.room.Room
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    private lateinit var db: AppDb
    private val locPerm = registerForActivityResult(ActivityResultContracts.RequestPermission()) {}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        db = Room.databaseBuilder(applicationContext, AppDb::class.java, "netdiagdz.db").build()
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            locPerm.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
        setContent {
            MaterialTheme(colorScheme = lightColorScheme()) {
                App(db)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun App(db: AppDb) {
    var tab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Ping", "DNS", "Ports", "LAN", "Wi-Fi", "Rapports")
    val scope = rememberCoroutineScope()
    fun save(kind: String, target: String, summary: String) {
        scope.launch(Dispatchers.IO) { db.reports().insert(Report(kind = kind, target = target, summary = summary)) }
    }
    Scaffold(topBar = { TopAppBar(title = { Text("NetDiagDZ") }) }) { pad ->
        Column(Modifier.padding(pad).padding(12.dp)) {
            ScrollableTabRow(selectedTabIndex = tab) {
                tabs.forEachIndexed { i, t -> Tab(selected = tab == i, onClick = { tab = i }, text = { Text(t) }) }
            }
            Spacer(Modifier.height(8.dp))
            when (tab) {
                0 -> PingScreen(::save)
                1 -> DnsScreen(::save)
                2 -> PortScreen(::save)
                3 -> LanScreen(::save)
                4 -> WifiScreen(::save)
                5 -> HistoryScreen(db)
            }
        }
    }
}

@Composable
fun Field(label: String, value: String, onChange: (String) -> Unit) {
    OutlinedTextField(value = value, onValueChange = onChange, label = { Text(label) },
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), singleLine = true)
}

@Composable
fun PingScreen(save: (String, String, String) -> Unit) {
    var host by remember { mutableStateOf("8.8.8.8") }
    var out by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    Field("Hôte / Host / المضيف", host) { host = it }
    Button(onClick = {
        if (!NetTools.isValidHost(host)) { out = "Hôte invalide / Invalid host"; return@Button }
        busy = true; out = "..."
        scope.launch {
            val res = withContext(Dispatchers.IO) {
                (1..4).map { pingHost(host) }
            }
            val times = res.filter { it.reachable }.map { it.timeMs }
            out = if (times.isEmpty()) "$host : injoignable / unreachable" else "$host : ${NetTools.pingSummary(times)}"
            save("ping", host, out)
            busy = false
        }
    }, enabled = !busy) { Text("Ping (4x)") }
    Text(out, modifier = Modifier.padding(top = 8.dp))
}

@Composable
fun DnsScreen(save: (String, String, String) -> Unit) {
    var host by remember { mutableStateOf("google.dz") }
    var out by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    Field("Nom DNS / hostname", host) { host = it }
    Button(onClick = {
        if (!NetTools.isValidHost(host)) { out = "Nom invalide"; return@Button }
        busy = true; out = "..."
        scope.launch {
            val ips = withContext(Dispatchers.IO) { dnsLookup(host) }
            out = if (ips.isEmpty()) "$host : échec DNS / DNS failed" else "$host ->\n" + ips.joinToString("\n")
            save("dns", host, out.take(500))
            busy = false
        }
    }, enabled = !busy) { Text("Résoudre / Lookup") }
    Text(out, modifier = Modifier.padding(top = 8.dp))
}

@Composable
fun PortScreen(save: (String, String, String) -> Unit) {
    var host by remember { mutableStateOf("192.168.1.1") }
    var ports by remember { mutableStateOf("80, 443, 8080") }
    var out by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    Field("Hôte", host) { host = it }
    Field("Ports (ex: 80,443,8000-8002)", ports) { ports = it }
    Button(onClick = {
        val list = NetTools.parsePorts(ports)
        if (!NetTools.isValidHost(host) || list.isEmpty()) { out = "Hôte ou ports invalides"; return@Button }
        busy = true; out = "..."
        scope.launch {
            val res = withContext(Dispatchers.IO) { list.map { checkPort(host, it) } }
            out = res.joinToString("\n") { "${it.host}:${it.port} " + (if (it.open) "OUVERT (${it.timeMs}ms)" else "fermé") }
            save("port", "$host [$ports]", out.take(500))
            busy = false
        }
    }, enabled = !busy) { Text("Tester / Check") }
    Text(out, modifier = Modifier.padding(top = 8.dp))
}

@Composable
fun LanScreen(save: (String, String, String) -> Unit) {
    val ctx = LocalContext.current
    var base by remember { mutableStateOf("192.168.1.1") }
    var out by remember { mutableStateOf<List<String>>(emptyList()) }
    var busy by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    Field("IP de référence (préfixe /24 auto)", base) { base = it }
    Button(onClick = {
        val prefix = NetTools.prefixOf(base.ifBlank { "192.168.1.1" })
        if (prefix.isEmpty()) return@Button
        busy = true; out = listOf("scan ...")
        scope.launch {
            val found = withContext(Dispatchers.IO) {
                val res = mutableListOf<String>()
                // Sequential isReachable scan of 254 hosts; timeout 250ms keeps it LAN-only and fast.
                // Coroutine-friendly: runs on Dispatchers.IO, no internet needed.
                for (h in NetTools.hostsOf24("$prefix.1")) {
                    try {
                        if (java.net.InetAddress.getByName(h).isReachable(250)) res.add(h)
                    } catch (_: Exception) {}
                }
                res
            }
            out = if (found.isEmpty()) listOf("aucun appareil / none found") else found
            save("lan", "$prefix.0/24", "found ${found.size}: ${found.take(20).joinToString(",")}")
            busy = false
        }
    }, enabled = !busy) { Text("Scanner LAN (/24)") }
    LazyColumn { items(out) { Text(it, modifier = Modifier.padding(2.dp)) } }
}

@Composable
fun WifiScreen(save: (String, String, String) -> Unit) {
    val ctx = LocalContext.current
    var info by remember { mutableStateOf("—") }
    Button(onClick = {
        info = readWifi(ctx)
        save("wifi", "wlan", info.take(500))
    }) { Text("Lire signal Wi-Fi / Read Wi-Fi") }
    Text(info, modifier = Modifier.padding(top = 8.dp))
}

fun readWifi(ctx: Context): String {
    return try {
        val wm = ctx.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val c = wm.connectionInfo
        "SSID: ${c.ssid}\nBSSID: ${c.bssid}\nRSSI: ${c.rssi} dBm\nDébit/Link: ${c.linkSpeed} Mbps\nFréquence: ${c.frequency} MHz\nIP: ${c.ipAddress.let { "${it and 0xFF}.${it shr 8 and 0xFF}.${it shr 16 and 0xFF}.${it shr 24 and 0xFF}" }}"
    } catch (e: Exception) {
        "Wi-Fi indisponible: ${e.message}"
    }
}

@Composable
fun HistoryScreen(db: AppDb) {
    val scope = rememberCoroutineScope()
    var items by remember { mutableStateOf<List<Report>>(emptyList()) }
    LaunchedEffect(Unit) { items = withContext(Dispatchers.IO) { db.reports().list() } }
    Row {
        Button(onClick = { scope.launch { items = withContext(Dispatchers.IO) { db.reports().list() } } }) { Text("Actualiser") }
        Spacer(Modifier.width(8.dp))
        Button(onClick = { scope.launch { withContext(Dispatchers.IO) { db.reports().clear() }; items = emptyList() } }) { Text("Effacer") }
    }
    LazyColumn {
        items(items) {
            Card(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                Column(Modifier.padding(8.dp)) {
                    Text("[${it.kind}] ${it.target}", style = MaterialTheme.typography.titleSmall)
                    Text(it.summary.take(400), style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}
