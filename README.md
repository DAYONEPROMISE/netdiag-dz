# NetDiagDZ — network diagnostics toolkit for Algerian IT technicians

Kotlin + Jetpack Compose (Material 3) + Room. French/Arabic. Offline-first, no account.

Features: ping (4x), DNS lookup, TCP port check (list/ranges), LAN /24 scan,
Wi-Fi signal details (SSID/RSSI/link speed/frequency/IP), intervention report history (Room).

## Permissions (each justified)
| Permission | Why |
|---|---|
| INTERNET | Open sockets for ping/DNS/port checks — the core feature |
| ACCESS_NETWORK_STATE | Distinguish "no network" from "host down" |
| ACCESS_WIFI_STATE | Read WifiManager connection info |
| ACCESS_FINE_LOCATION | Android 10+ requires location grant to expose SSID/BSSID; no location stored |

No contacts/camera/mic/storage. LAN scan stays on the local /24, sequential, 250ms timeout.

## Build
CI runs `./gradlew :app:testDebugUnitTest`, `:app:lintDebug`, `:app:assembleDebug` and uploads `app-debug.apk`.

## License
MIT — all code is clean-room original.
