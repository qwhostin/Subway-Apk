# RUNVANTA — 3D Endless Runner for Android
**Publisher:** QW HOSTING (`https://qwhosting.com` | `info@qwhosting.com`)  
**Package Name:** `com.aistudio.runvanta.qwhost`  
**Target SDK:** Android API 36 (Minimum SDK 24)  
**Monetization:** Free-to-Play, Google AdMob Only (No IAPs, No Subscriptions, No Paywalls)

## Overview
RUNVANTA is a native Android 3D endless-runner game powered by **Kotlin**, **Jetpack Compose**, and a custom **OpenGL ES 3.0 3D engine**, featuring **Play As Anyone** — local-only Photo Picker face cropping & 3D character texture mapping.

> **Platform Note:** Because this project is built directly for the Android Studio Kotlin/Gradle toolchain, the 3D engine, articulated character animation, procedural tracks, audio synthesizer, and UI are implemented natively in Kotlin + OpenGL ES 3.0 + Jetpack Compose rather than Unity C#, producing a lightweight, fast-loading Android APK/AAB targeting API 36.

## Key Features
- **Play As Anyone (Character Studio):** Zero-permission Android Photo Picker (`PickVisualMedia`), interactive pan/zoom face alignment, local-only PNG texture baking onto a 3D cartoon head, customizable hairstyles, outfits, shoes, accessories, and trail effects.
- **3D Endless Runner Engine:** Three-lane swipe & keyboard controls, object-pooled track segments, 5 obstacle types (low barriers, overhead arches, full blocks, moving drones, broken track gaps), spinning coins, 4 power-ups (Magnet, 2x Multiplier, Shield, Turbo Dash), and near-miss combos.
- **6 Game Modes:** Classic, Easy, Normal, Hard, Extreme, and Custom Run (11 configurable safe-limit parameters).
- **8 Original 3D Environments:** Neon City, Tropical Island, Desert Highway, Mountain Valley, Futuristic Spaceport, Cyberpunk District, Snow Adventure, Ancient Fantasy City.
- **6 Original Default Characters:** Kael Volt, Zuri Blaze, Orion Prime, Nyx Cipher, Bram Frost, Aria Myth.
- **Progression & Economy:** Virtual coin economy, Daily Missions, Weekly Challenges, Lifetime Achievements, and Personal Records saved locally via Room & DataStore.
- **Procedural Audio Synthesizer:** Original synthesized background music and 11 SFX with independent music/SFX sliders.
- **Google AdMob & UMP Consent:** Official Google Mobile Ads SDK & User Messaging Platform consent with test ad unit IDs.

## Build Instructions
### Debug APK
```bash
gradle :app:assembleDebug
```
Output: `app/build/outputs/apk/debug/app-debug.apk`

### Unit & Robolectric Tests
```bash
gradle :app:testDebugUnitTest
```

### Signed Release AAB (Google Play)
1. Replace the test AdMob App ID and Ad Unit IDs in `app/src/main/res/values/strings.xml` with your production AdMob IDs.
2. Configure release keystore environment variables (`KEYSTORE_PATH`, `STORE_PASSWORD`, `KEY_PASSWORD`) or Android Studio's **Build > Generate Signed Bundle / APK**.
3. Run:
```bash
gradle :app:bundleRelease
```
Output: `app/build/outputs/bundle/release/app-release.aab`
