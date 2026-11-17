# Build progress (resume from here in a new chat)

App: UniBud (repo `unibud`), package `com.unibud.app`. Android, Kotlin, Compose, Room, Firebase (later).

- [x] Stage 1: Bluetooth core (scan, identify, connection monitor, GATT probe), capability resolver, control gateway
- [x] Stage 2: Room database (device profiles and features, EQ presets, settings) and repositories
- [x] Stage 3: UI (theme, placeholder splash and welcome, navigation, Home, Devices, Control, Audio, Info, Settings)
- [x] Phone optimization pass: throttled scan updates, release shrinking, CI APK workflow
- [x] Redesign from the mockup (neon theme, animated splash and sign-in, new Home, Control Center, Audio Studio, Info, Profile), launcher icon, stronger brand detection, Android volume and media controls
- [x] Stage 4 code: Google sign-in (Firebase Auth + Credential Manager) and Firestore sync (presets, device names, theme). Needs google-services.json from the user's Firebase project
- [ ] Old Stage 4 notes: Firebase Auth (Google Sign-In), Firestore sync + security rules, optional Storage backup.
      Never upload pairing keys or raw Bluetooth data. Hash device addresses before they go to the cloud.
- [ ] Stage 5: first manufacturer integration(s) using documented or authorized protocols, tests on real devices
- [x] Logo and launch animation, from the user's mockup (`LogoMark` in `ui/components/NeoComponents.kt`, launcher icon in `res/drawable`)

Not done yet from the spec: "saved sound configurations" (needs a readable device state or a real controller), custom font files.

Rules to keep: never send undocumented commands; never show unsupported features as available; never fake a successful control result.
