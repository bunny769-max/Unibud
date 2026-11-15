# UniBud (Android)

One companion app for Bluetooth TWS earbuds. Kotlin, Jetpack Compose, Room, layered device integrations.
Real Bluetooth only: a feature is shown as available only when a real source reported it.

## Open and run
1. Open this folder in Android Studio (latest stable) and let Gradle sync. If it offers to upgrade
   AGP / Kotlin / Compose / KSP versions, accept. The pinned versions in `gradle/libs.versions.toml` are conservative.
2. Run on a **physical phone** (emulators have no Bluetooth).
3. Follow the welcome screen, allow Bluetooth access, then connect earbuds in Android settings. UniBud identifies them on its own.

## What works now
- **Home:** connected device, battery gauges, sound-mode buttons.
- **Devices:** animated scan, pair through Android's own dialog, details, saved devices.
- **Control:** every feature with an honest status and reason. Controls stay off unless a real integration exists.
- **Audio:** equalizer preset editor with local saving. Applying to earbuds needs a device that supports it.
- **Info:** model/firmware from standard BLE services, full capability list, rename a device.
- **Profile and settings:** light/dark/match phone, privacy, Bluetooth permission. Google sign-in is a disabled placeholder until Stage 4.

## Layers (see `core/`)
| Layer | Code | What it does |
|---|---|---|
| 1 Standard Bluetooth | `BluetoothScanner`, `ConnectionMonitor`, `AndroidStandardIntegration` | Bonded + Classic + BLE discovery, connection state, identification |
| 2 BLE GATT | `GattProbe`, `GattStandardIntegration` | Read-only standard Battery Level and Device Information |
| 3 Manufacturer | `DeviceIntegration` / `DeviceController` | Contract only. No vendor module yet |
| 4 Device database | `DeviceDatabase` | Brand identification. `integrationId` stays null until a real module exists |

All control actions go through `ControlGateway`, which returns `NotSupported` unless a real controller handles the feature.

## Build an APK without Android Studio
Push to `main` and GitHub Actions (`.github/workflows/build-apk.yml`) runs the unit tests and builds an optimized
release APK. Download it from the run's Artifacts (`unibud-release-apk`) and install it on your phone.
The release build is signed with the debug key so it installs for personal use. Make your own keystore before publishing.

## Phone optimizations
- Release builds use R8 code shrinking and resource shrinking, plus the profile installer for faster starts.
- Scan results reach the UI at most every 300 ms, so busy Bluetooth areas do not make the screen stutter.
- The radar animation runs only while a scan is active. Scans stop on their own after 12 seconds.
- Paired devices are read off the main thread.
- Status and navigation bar icons follow the app theme. Content is capped at 640 dp wide for landscape and foldables.
- Judge smoothness on the release build. Compose debug builds are much slower.

## Honest limits
- Android does not let apps connect or disconnect audio devices, so those buttons open Bluetooth settings.
- Per-earbud and case battery, ANC, EQ, touch controls and low latency need a manufacturer protocol.
- The earbuds-versus-headphones guess is a heuristic and is labelled with its confidence.

## Verification status
- Pure logic (`core/model`, `core/registry`, `core/capability`, `core/audio`): compiled with kotlinc, 32 unit tests pass.
- Android code (Bluetooth, Room, Compose UI, Gradle) was written carefully and syntax-checked, but **not compiled or run** because no Android SDK was available. Expect a few small compile errors on first sync.
