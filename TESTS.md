# Test table — NetDiagDZ

Unit tests: `app/src/test/java/dz/netdiag/NetToolsTest.kt`, run in CI via `:app:testDebugUnitTest`.

| # | Test | Covers | Expected |
|---|---|---|---|
| T1 | validHosts | `isValidHost` IPv4/hostname/localhost/empty/`999.1.1.1`/`bad host!` | true,true,true,true,false,false,false |
| T2 | ports | `isValidPort` 80/0/70000; `parsePorts` list, range, junk-tolerant, empty | `[80,443]`, `[8000,8001,8002]`, `[22]`, `[]` |
| T3 | subnet | `hostsOf24` size/first/last, bad input; `prefixOf` | 254, `192.168.1.1`, `192.168.1.254`, empty, `192.168.1` |
| T4 | pingSummary | empty + `[10,20,30]` | `no reply`, contains `avg 20ms` |

Manual (device, not in CI): ping 8.8.8.8 → `avg …ms`; DNS `google.dz` → IPs; ports `192.168.1.1: 80,443` → OUVERT/fermé; LAN /24 → list; Wi-Fi button → SSID/RSSI; Rapports → save/refresh/clear persists across restart.

## Limitations
- No local Android SDK in this build environment → local `./gradlew` not runnable here; CI (test+lint+assembleDebug) is the verified build path.
- `isReachable` accuracy varies by OEM/Android version.
- SSID needs location grant; otherwise `<unknown ssid>`.
- Full /24 worst case ~60 s (sequential, polite).

## Costs
- Build/hosting: GitHub Actions free tier + `upload-artifact` + release APK. No backend, no keys, no paid services. Total marginal cost: $0.

## First-10-customers plan (Alger)
1–2: 2 cybercafés (Bab Ezzouar, Oran) — free pilot, LAN scan + reports. 3–4: 2 freelance IT techs — WhatsApp/Viber groups. 5–6: 2 university IT clubs (USTHB, ESI). 7: 1 computer repair shop. 8: 1 coworking space. 9–10: 2 Wilaya ISP resellers. Channel: APK sideload + GitHub release link, FR/AR flyer. Ask: logo placement + referral for a paid Pro (export PDF, scheduled checks).
