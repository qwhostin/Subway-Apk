# RUNVANTA — Complete Google Play Console Submission Kit
**Company:** QW HOSTING (`https://qwhosting.com`) | **Support Email:** `info@qwhosting.com`

---

## 1. Copy-Paste Store Listing Details
- **App name (8 / 30 chars):** `RUNVANTA`
- **Package name:** `com.aistudio.runvanta.qwhost`
- **Default language:** `English (United States) – en-US`
- **App or game:** `Game`
- **Category:** `Arcade` (or `Action`)
- **Free or paid:** `Free`
- **Short description (77 / 80 chars):**
  `Sprint through 8 3D worlds and transform any permitted photo into your runner!`
- **Full description (Policy-compliant — no buzzwords, no ALL CAPS spam, no unverified claims):**
  `RUNVANTA is a fast-paced 3D endless-runner arcade game built by QW HOSTING.

  PLAY AS ANYONE IN CHARACTER STUDIO
  Pick a photograph from your device using the Android Photo Picker, frame and scale the face, and watch it map directly onto an original 3D cartoon character. Customize hairstyles, outfits, footwear, accessories, skin tones, and sprint trail effects. All photos stay strictly on your device—they are never uploaded to external servers or used for AI training, and you can delete saved characters at any time. (Always make sure you have permission before using another person's photo.)

  8 ORIGINAL 3D WORLDS
  Dash across Neon City, Tropical Island, Desert Highway, Mountain Valley, Futuristic Spaceport, Cyberpunk District, Snow Adventure, and Ancient Fantasy City—each with distinct lighting, obstacles, and atmospheric music.

  6 GAME MODES
  Choose Classic, Easy, Normal, Hard, Extreme, or build your own challenge in Custom Run with configurable speed, obstacle frequency, coin rate, power-ups, lives, and run duration.

  MISSIONS, ACHIEVEMENTS & FAIR PLAY
  Earn virtual coins during runs, complete daily missions and weekly challenges, unlock characters and worlds through normal gameplay, and track personal records offline. No in-app purchases, no subscriptions, and no paywalls.`

---

## 2. Declarations & App Content
- **Developer Program Policies:** Check **Confirm**
- **Play App Signing:** Check **Accept**
- **US export laws:** Check **Accept** (App uses standard Android HTTPS/TLS and Google Mobile Ads SDK only; no custom encryption)
- **Privacy Policy URL:** Host `PRIVACY_POLICY.md` at `https://qwhosting.com/runvanta-privacy` and enter that URL
- **App Access:** `All functionality is available without special access` (No login or account required)
- **Ads:** `Yes, my app contains ads` (Google AdMob Banner, frequency-capped Interstitial, optional Rewarded)
- **In-app purchases:** `No`

---

## 3. Data Safety Form Answers
- **Does your app collect or share any of the required user data types?** `Yes` *(solely because Google Mobile Ads SDK accesses Device/Ad IDs and crash/ad diagnostics when consented)*
- **Photos and videos:** **Not collected / Not shared** — selected via Android System Photo Picker and processed 100% locally on-device; excluded from cloud backup (`faces/`).
- **Device or other IDs (Advertising ID):** Collected/shared ephemerally by **Google Mobile Ads SDK** for **Advertising or marketing** & **Analytics**, encrypted in transit, subject to UMP consent.
- **Data Deletion:** Users can delete custom characters individually in Character Studio or wipe all local progress in **Settings > Reset All Local Progress**.

---

## 4. One Final Step Before Generating the Release `.aab`
In `app/src/main/res/values/strings.xml`, replace the 4 official Google **test** AdMob IDs (`ca-app-pub-3940256099942544...`) with your live **QW HOSTING** AdMob App ID and Ad Unit IDs, then generate the signed `.aab` via **AI Studio Settings > Generate AAB** (or Android Studio **Build > Generate Signed Bundle / APK**).
