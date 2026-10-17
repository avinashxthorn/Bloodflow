# 🌷 Bloodflow

**Bloodflow** is a native Android menstrual-cycle companion built with **Kotlin + Jetpack Compose**. Its visual language uses soft rose, lavender, rounded cards, and a cycle-progress ring.

## Features
- First-launch onboarding to set the last period start, typical cycle length (21–45 days) and bleeding duration (1–10 days).
- Dashboard with cycle-day ring and predicted next period.
- Month-by-month calendar with logged and predicted period days.
- Period history: add and remove period-start dates.
- Daily mood, symptom, and private-note logging; view and delete journal entries.
- Personal cycle settings and permanent in-app local data deletion.
- No login, ads, analytics SDK or network permission.

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
All diary entries are saved in Android app-private SharedPreferences on the device and are **not encrypted**. Android backups are disabled. Clearing app data or uninstalling deletes the stored data; there is currently no export or multi-device sync.

**Predictions are approximations** based on a manually selected typical cycle length; the app does not detect ovulation, diagnose medical conditions, or provide reliable fertility or contraception guidance. For severe pain, heavy bleeding, abnormal bleeding or concerns, consult a qualified clinician.

## Roadmap
- Opt-in notification reminders
- Export/import of encrypted backups
- Accessible language selection
- Automated test coverage, release signing and Play Store readiness
