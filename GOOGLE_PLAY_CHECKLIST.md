# Google Play Console Publishing Checklist — RUNVANTA

## 1. App Details (Google Play Console)
- **App name (<= 30 chars):** `RUNVANTA` (8/30 chars — no emojis, no ALL CAPS buzzwords)
- **Package name:** `com.aistudio.runvanta.qwhost`
- **Default language:** English (United States) – `en-US`
- **App or game:** Game (Arcade / Action)
- **Free or paid:** Free (Ad-supported, zero IAPs)
- **Publisher / Company:** QW HOSTING (`https://qwhosting.com` | `info@qwhosting.com`)

## 2. Production Configuration Before Release
- [ ] Replace test `admob_app_id`, `admob_banner_id`, `admob_interstitial_id`, and `admob_rewarded_id` in `app/src/main/res/values/strings.xml` with production AdMob IDs.
- [ ] Host `PRIVACY_POLICY.md` at a live HTTPS URL (e.g., `https://qwhosting.com/runvanta-privacy`) and enter it in Play Console **App content > Privacy policy**.
- [ ] Enable **Play App Signing** and accept US Export Laws (standard Android HTTPS/AdMob SDK encryption exemption applies; no custom encryption).

## 3. Permissions & Policy Verification
- Declared permissions: `INTERNET`, `ACCESS_NETWORK_STATE`, `VIBRATE` (all normal install-time permissions).
- Zero dangerous permissions: Uses zero-permission Android Photo Picker (`PickVisualMedia`). No `READ_EXTERNAL_STORAGE`, `READ_MEDIA_IMAGES`, `CAMERA`, `LOCATION`, or `CONTACTS`.
- Target SDK: `36` (Meets August 31, 2026 Google Play target API requirement).

## 4. Data Safety Form Guidance
- **Does your app collect or share user data?** Yes (via Google Mobile Ads SDK for Advertising ID & diagnostics/analytics).
- **Photos & Videos:** Local on-device processing only; NOT collected or shared off-device.
- **Data deletion:** Users can delete custom characters individually or reset all local progress in Settings.
- **Ads declaration:** Yes, app contains ads (AdMob Banner, Interstitial, Rewarded).
