# Cut Tracker

Personal Android app for a fixed 12-week cut (Oct 14 2026 to Jan 6 2027) and the chest phase after it. Offline, local-only, no accounts, no network. Kotlin, Jetpack Compose, Room, DataStore, CameraX, Glance.

The spec is `CUT_TRACKER_HANDOFF.md`; the look and copy come from `cut-tracker-prototype.html`. Build notes and decisions are in `PROGRESS.md`.

## Build

```bash
./gradlew :app:assembleDebug
```

```bash
./gradlew :app:testDebugUnitTest
```

Install on a connected phone (USB debugging on):

```bash
./gradlew :app:installDebug
```

## Release build

Signing needs `keystore.properties` at the repo root (gitignored) next to the keystore it points at:

```
storeFile=../cut-tracker.jks
storePassword=...
keyAlias=cut
keyPassword=...
```

Create a key once (keep it; a different key cannot update an installed app, you would have to uninstall first):

```bash
keytool -genkeypair -v -keystore cut-tracker.jks -alias cut -keyalg RSA -keysize 2048 -validity 10000
```

Then:

```bash
./gradlew :app:assembleRelease
```

The signed APK lands at `app/build/outputs/apk/release/app-release.apk`.

## Sideload

1. Copy `app-release.apk` to the phone (USB, Drive, Quick Share, or `adb install app/build/outputs/apk/release/app-release.apk`).
2. Open it. Android asks to allow installs from that source once (Files, Chrome, or whatever opened it). Allow it.
3. Install. Updates install over the top as long as the same key signed them; data is kept.
4. On first open: allow notifications (the daily weigh-in nudge) and, when you shoot a progress photo, the camera.

Photos stay in app-private storage. Settings > Data has Export JSON, Export with photos (zip), and Import.

## Home screen widgets

Long-press the home screen, Widgets, Cut Tracker. The 4x2 widget shows today's mode, the next item (tap it to check it), the Send bar and the chain. The 2x1 widget shows the chain and the bar.
