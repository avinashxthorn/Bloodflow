# 🌷 Bloodflow

**Bloodflow** is a native Android menstrual-cycle companion built with **Kotlin + Jetpack Compose**. Its visual language uses soft rose, lavender, rounded cards, and a cycle-progress ring.

## Features
- First-launch onboarding to set the last period start, typical cycle length (21–45 days) and bleeding duration (1–10 days).
- Dashboard with cycle-day ring and predicted next period.
- Month-by-month calendar with logged and predicted period days.
- Period history: add and remove period-start dates.
- Daily mood, symptom, and private-note logging; view and delete journal entries.
- Personal cycle settings and permanent in-app local data deletion.
- Optional Google sign-in, manual encrypted backup/restore (requires user-owned Firebase configuration), and guest mode; no advertising or analytics SDK.

## Install the Android APK (no Android Studio required)
1. Open **[GitHub Actions](https://github.com/avinashxthorn/Bloodflow/actions/workflows/android-apk.yml)**.
2. Open the latest successful run of **Build Bloodflow APK**. If a run has not started, select **Run workflow**.
3. At the bottom of the run page, download **Bloodflow-debug-APK** under Artifacts (GitHub sign-in may be required).
4. Unzip the artifact and install `app-debug.apk` on Android.

The APK comes from the **assembleDebug** job, is signed with Android's standard debug key, and is suitable for personal testing—not Play Store publishing. A workflow artifact is not available until the build succeeds. This repository does **not** contain a pre-built APK.

## Build locally
Requires JDK 17, Android SDK Platform 35, Gradle 8.11.1+ and Android build tools. From the repository root:

```sh
gradle :app:assembleDebug
```

Output: `app/build/outputs/apk/debug/app-debug.apk`.

## Privacy & medical limitations
All diary entries are saved in Android app-private SharedPreferences on the device and are **not encrypted**. Android backups are disabled. Clearing app data or uninstalling deletes the stored data; optional encrypted cloud backup and restore is available after Firebase setup.

**Predictions are approximations** based on a manually selected typical cycle length; the app does not detect ovulation, diagnose medical conditions, or provide reliable fertility or contraception guidance. For severe pain, heavy bleeding, abnormal bleeding or concerns, consult a qualified clinician.

## Roadmap
- Opt-in notification reminders
- Export/import of encrypted backups
- Accessible language selection
- Automated test coverage, release signing and Play Store readiness

## Updating an existing installation
Download the latest **successful** APK from GitHub Actions. Install it over the existing Bloodflow app without uninstalling. Data stored by the application survives app relaunches, force stop, and removing the task from Android Recents.

**Signing caveat:** Android only installs updates in place when both APKs have the same application ID and signing certificate. Builds made on ephemeral CI runners may use different debug signing keys. This workflow caches its debug key for subsequent builds, but caches can expire and it cannot match APKs built **before** the cache was first created. If Android says "App not installed" when updating, do **not** uninstall if you need your saved data. Future production builds should use a permanently maintained private release key.

## Version 1.1.0
- More visible personalized nickname editing and greeting
- New rose-pink lotus and droplet launcher icon
- Persistent saved name and entries, with synchronous disk commit
- APK versionCode raised to 2 for recognizable upgrade

## Version 1.2.0 — onboarding and fertility awareness
- First-run three-screen swipeable introduction: track periods, log symptoms, learn fertility awareness and privacy. The introduction is shown once after upgrade (unless skipped).
- Fertility tab and calendar amber indicators for *potentially fertile* days when configured cycle length is 26–32 days and recorded intervals meet that range. For other lengths, shows *uncertain* rather than pretending to identify safe dates.
- Date-by-date exploration with clear warning that no date is guaranteed safe for unprotected sex.
- Softer text entry design on name and private journal fields, refined app icon, and updated version code 3.
- All existing data remains in the same on-device store. Do not uninstall the old version when updating if you need your data.

**Medical note:** This is an approximate calendar awareness tool only. It is not a validated contraception method or fertility diagnosis, does not prevent sexually transmitted infections, and cannot verify ovulation. If pregnancy prevention matters, use effective contraception and consult a qualified healthcare professional.

## Version 1.3.0: Google account and encrypted backup
See [FIREBASE_SETUP.md](FIREBASE_SETUP.md) for the required Firebase console steps, repository Actions secrets, Firestore rules, and signing fingerprint. Without this setup, Google sign-in is intentionally disabled while guest mode remains usable.
