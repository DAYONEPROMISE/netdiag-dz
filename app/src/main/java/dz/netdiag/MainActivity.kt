package dz.netdiag

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.*
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
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
        enableEdgeToEdge()
        db = Room.databaseBuilder(applicationContext, AppDb::class.java, "netdiagdz.db").build()
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            locPerm.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
        setContent {
            NetDiagDZTheme {
                App(db)
            }
        }
    }
}

/** 2026 baseline: dynamic color (Android 12+), system dark mode, edge-to-edge aware. */
@Composable
fun NetDiagDZTheme(content: @Composable () -> Unit) {
    val ctx = LocalContext.current
    val dark = androidx.compose.foundation.isSystemInDarkTheme()
    // Dynamic color needs Android 12+ (API 31); older devices get static M3 schemes.
    val scheme = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        if (dark) dynamicDarkColorScheme(ctx) else dynamicLightColorScheme(ctx)
    } else {
        if (dark) darkColorScheme() else lightColorScheme()
    }
    MaterialTheme(colorScheme = scheme, content = content)
}

private data class Dest(val id: String, val fr: String, val ar: String, val icon: ImageVector)

private val DESTS = listOf(
    Dest("ping", "Ping", "\u0628\u064a\u0646\u063a", Icons.Filled.Bolt),
    Dest("dns", "DNS", "DNS", Icons.Filled.Language),
    Dest("ports", "Ports", "\u0627\u0644\u0645\u0646\u0627\u0641\u0630", Icons.Filled.Tune),
    Dest("lan", "LAN", "\u0627\u0644\u0634\u0628\u0643\u0629", Icons.Filled.Devices),
    Dest("wifi", "Wi-Fi", "\u0648\u0627\u064a \u0641\u0627\u064a", Icons.Filled.Wifi),
    Dest("history", "Rapports", "\u0627\u0644\u062a\u0642\u0627\u0631\u064a\u0631", Icons.Filled.History),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun App(db: AppDb) {
    var tab by remember { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()
    fun save(kind: String, target: String, summary: String) {
        scope.launch(Dispatchers.IO) { db.reports().insert(Report(kind = kind, target = target, summary = summary)) }
    }
    // Adaptive: rail on wide screens (tablets/foldables), bottom bar on phones.
    BoxWithConstraints {
        val wide = maxWidth >= 600.dp
        val content: @Composable () -> Unit = {
            when (tab) {
                0 -> PingScreen(::save)
                1 -> DnsScreen(::save)
                2 -> PortScreen(::save)
                3 -> LanScreen(::save)
                4 -> WifiScreen(::save)
                5 -> HistoryScreen(db)
            }
        }
        if (wide) {
            Row(Modifier.fillMaxSize()) {
                NavigationRail {
                    DESTS.forEachIndexed { i, d ->
                        NavigationRailItem(
                            selected = tab == i,
                            onClick = { tab = i },
                            icon = { Icon(d.icon, contentDescription = d.fr) },
                            label = { Text(d.fr) },
                        )
                    }
                }
                RailContent(content)
            }
        } else {
            Scaffold(
                topBar = { TopAppBar(title = { Text("NetDiagDZ", Modifier.semantics { heading() }) }) },
                bottomBar = {
                    NavigationBar {
                        DESTS.forEachIndexed { i, d ->
                            NavigationBarItem(
                                selected = tab == i,
                                onClick = { tab = i },
                                icon = { Icon(d.icon, contentDescription = d.fr) },
                                label = { Text(d.fr) },
                            )
                        }
                    }
                }
            ) { pad ->
                Column(
                    Modifier.padding(pad)
                        .consumeWindowInsets(pad)
                        .padding(12.dp)
                ) { content() }
            }
        }
    }
}

@Composable
private fun RowScope.RailContent(content: @Composable () -> Unit) {
    Column(
        Modifier.weight(1f)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(12.dp)
    ) {
        Text("NetDiagDZ", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.semantics { heading() })
        Spacer(Modifier.height(8.dp))
        content()
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
    Field("H\u00f4te / Host / \u0627\u0644\u0645\u0636\u064a\u0641", host) { host = it }
    Button(onClick = {
        if (!NetTools.isValidHost(host)) { out = "H\u00f4te invalide / Invalid host"; return@Button }
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
            out = if (ips.isEmpty()) "$host : \u00e9chec DNS / DNS failed" else "$host ->\n" + ips.joinToString("\n")
            save("dns", host, out.take(500))
            busy = false
        }
    }, enabled = !busy) { Text("R\u00e9soudre / Lookup") }
    Text(out, modifier = Modifier.padding(top = 8.dp))
}

@Composable
fun PortScreen(save: (String, String, String) -> Unit) {
    var host by remember { mutableStateOf("192.168.1.1") }
    var ports by remember { mutableStateOf("80, 443, 8080") }
    var out by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    Field("H\u00f4te", host) { host = it }
    Field("Ports (ex: 80,443,8000-8002)", ports) { ports = it }
    Button(onClick = {
        val list = NetTools.parsePorts(ports)
        if (!NetTools.isValidHost(host) || list.isEmpty()) { out = "H\u00f4te ou ports invalides"; return@Button }
        busy = true; out = "..."
        scope.launch {
            val res = withContext(Dispatchers.IO) { list.map { checkPort(host, it) } }
            out = res.joinToString("\n") { "${it.host}:${it.port} " + (if (it.open) "OUVERT (${it.timeMs}ms)" else "ferm\u00e9") }
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
    Field("IP de r\u00e9f\u00e9rence (pr\u00e9fixe /24 auto)", base) { base = it }
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
    var info by remember { mutableStateOf("\u2014") }
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
        "SSID: ${c.ssid}\nBSSID: ${c.bssid}\nRSSI: ${c.rssi} dBm\nD\u00e9bit/Link: ${c.linkSpeed} Mbps\nFr\u00e9quence: ${c.frequency} MHz\nIP: ${c.ipAddress.let { "${it and 0xFF}.${it shr 8 and 0xFF}.${it shr 16 and 0xFF}.${it shr 24 and 0xFF}" }}"
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
