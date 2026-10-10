# Research — NetDiagDZ

Date: 2026-10-10. All claims verified against docs below; no fabricated APIs.

## Platform decisions (verified)
- Kotlin + Jetpack Compose Material 3, minSdk 26, target/compile 34, Java 17, AGP 8.6.1, Kotlin 1.9.25, KSP 1.9.25-1.0.20, Compose BOM 2024.09.02, Room 2.6.1, coroutines 1.8.1. Pinned in gradle files.
- Pure-JDK networking (`InetAddress.isReachable`, `InetAddress.getAllByName`, `java.net.Socket`) — no extra deps, works offline on LAN, no root.
- Room for report history (offline, no account). `values-fr` + `values-ar` (RTL via `supportsRtl=true`).

## Permissions (each declared + justified in manifest + README)
| Permission | Feature needing it | Verified rule |
|---|---|---|
| INTERNET | ping/DNS/port sockets | Normal permission, needed for any socket |
| ACCESS_NETWORK_STATE | tell "no network" apart from "host down" | Normal permission |
| ACCESS_WIFI_STATE | WifiManager SSID/RSSI/link speed/frequency/IP | Normal permission |
| ACCESS_FINE_LOCATION | Android 10+ hides SSID/BSSID without location grant | Runtime request in MainActivity; nothing stored |

No contacts/camera/mic/storage requested.

## Limits found during research
- `isReachable()` on some OEMs falls back to TCP-echo; results vary — UI reports "injoignable" honestly.
- SSID/BSSID return `<unknown ssid>` without location grant or on Android 12+ with approximate location.
- LAN scan is sequential, 250 ms/host → worst case ~60 s for a full /24; kept sequential to stay polite on café routers.
- Port ranges capped at 65 ports per check to avoid abuse/hangs.

## Alternatives rejected
- Flutter/React Native: heavier toolchain, worse Room-equivalent offline story for this scope.
- Nmap-style SYN scan / raw sockets: needs root — rejected.
- Cloud account/sync: rejected per brief (no account, minimal internet).
