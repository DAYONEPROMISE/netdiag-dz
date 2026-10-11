# OpenPencil pass — netdiag-dz

## Design (Shared/OpenPencil/netdiag-dz/)
- `design.html` (ping tool + result + 6-dest nav, DZ tokens) → `design.fig`
- `lint --preset accessibility`: 0 errors, 0 warnings

## Code changes
- `MainActivity.kt`: rail + bar nav used `Text("•")` bullets (announced as "bullet" ×6 by
  TalkBack). Now per-destination Material icons (Bolt/Language/Tune/Devices/Wifi/History)
  with the destination label as `contentDescription`. Verified complete on read-back.

## APKs
`android-build.yml` adds `assembleRelease` (unsigned) + `app-release` artifact + a versioned
`release-v*-b*` GitHub Release (debug flow unchanged).
