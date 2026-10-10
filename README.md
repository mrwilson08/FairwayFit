# FairwayFit

Golf-specific fitness app for Android (Kotlin + Jetpack Compose). Stability and speed training, a baseline fitness test with progress charts, reminders, Health Connect (Garmin) data, and optional Firebase cloud sync.

## TL;DR

1. Install **Android Studio** (current stable). Open the `FairwayFit` folder, let Gradle sync.
2. Plug in your Pixel 10 (USB debugging on) and press **Run**.
3. Optional: add Firebase later for cloud sync (steps below). The app runs fine without it.

## What's in it

- **Home**: today's session (rotates Stability / Speed & Power / Total Golf Circuit / Mobility on your training days), weekly goal ring, FairwayFit score, watch data, 8-minute Pre-Round Warm-Up.
- **Train**: 5 workouts, a 25-exercise library with how-to steps and coaching cues, and a guided workout player (set/rep counter, hold timers, rest timers, screen stays on).
- **Fitness test**: 8 tests (single-leg balance L/R, plank, side plank, broad jump, 30-second squats, sit-and-reach, trunk rotation). The first run is your **baseline**; retest every 4 weeks.
- **Progress**: score trend line, baseline-vs-latest by category, per-test sparklines with change vs baseline, workouts per week.
- **Settings**: reminders (days + time), Health Connect, cloud sync.

## Where to change things

| To change | Edit |
|---|---|
| Exercises, workouts, test definitions, scoring targets | `domain/Content.kt` |
| Scoring math | `domain/Models.kt` (`Scoring`) |
| Colors and fonts | `ui/theme/Theme.kt` |
| Weekly rotation | `Plan` in `domain/Content.kt` |

Scores run 0–100 between each test's `floor` and `target`. Those numbers are generic benchmarks I chose, not clinical norms. Tune them to you after your baseline.

## Cloud sync (optional)

Sync is merge-by-id: every test result and workout log is immutable and has a unique id, so a sync uploads what the cloud lacks and downloads what the phone lacks.

1. Create a project at console.firebase.google.com.
2. Add an Android app with package name `com.fairwayfit.app`.
3. Download `google-services.json` and put it in `FairwayFit/app/`.
4. In **Authentication**, enable **Email/Password**.
5. In **Firestore Database**, create a database and set these rules:

```
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    match /users/{uid}/{document=**} {
      allow read, write: if request.auth != null && request.auth.uid == uid;
    }
  }
}
```

6. Sync Gradle, rebuild, then sign in or create an account under **Settings → Cloud sync**.

Without `google-services.json` the Firebase plugin is skipped on purpose, and Settings shows "Not configured".

## Garmin data

The Forerunner 570 doesn't need a Garmin SDK. In **Garmin Connect → Settings → Connected Apps → Health Connect**, enable sharing. Then tap **Connect** on the Home screen. FairwayFit reads weight, resting heart rate, last sleep and 7-day average steps (read-only).

## Notes and known limits

- Dark theme only, by design.
- US units (inches, pounds).
- Reminders use WorkManager, so Android may deliver them a few minutes late.
- Library versions in `gradle/libs.versions.toml` were current when written. If Android Studio offers updates, accepting them is fine.
- Gradle wrapper: only `gradle-wrapper.properties` is included. Android Studio downloads Gradle 8.9 itself. If you want `./gradlew` on the command line, run `gradle wrapper` once.
- General fitness guidance, not medical advice.

---

## Pebble watch + Google Health (added Oct 2026)

**Untested:** none of this has been compiled. Build, then paste any errors back.

### Build-tool upgrade (required by PebbleKit 1.2.0)
Kotlin 2.3.20, AGP 8.13.1, Gradle 8.13, KSP 2.3.12, Room 2.7.2, compileSdk 36. If Android Studio asks to
update the Gradle wrapper, accept. The KSP version is the most likely thing to need adjusting.

### Pebble setup
1. Install the Core Devices Pebble app on the phone and pair the watch.
2. Install the Pebble SDK (not Android Studio), then in `pebble-watchapp/` run `pebble build`.
3. Sideload `build/pebble-watchapp.pbw` from the Pebble app (open the file with it).
4. In FairwayFit: Settings -> Pebble watch -> on -> "Send test to watch".
5. During a workout the watch mirrors the step. SELECT = start hold / next. UP = +15 s rest.
6. A timeline pin for your next session is kept up to date (follows reminder days/time).

Protocol is defined in `data/pebble/PebbleProtocol.kt` and mirrored in `pebble-watchapp/src/c/main.c`.
If `pebble build` rejects the `messageKeys` object form in package.json, switch to the array form
(`["TYPE","TITLE",...,"COMMAND"]`); the SDK then assigns numbers and the phone keys 0-8 must match order.

### Google Health API
Not built. Google is not onboarding new projects and the Fitbit Web API ends Oct 30, 2026. Health Connect
(already working) covers weight, sleep, resting HR and steps. If your Google Cloud project can enable the
Google Health API, add a client behind an interface later.

### New / changed files
New: `data/pebble/*`, `pebble-watchapp/*`. Changed: `libs.versions.toml`, `gradle-wrapper.properties`,
`app/build.gradle.kts`, `AndroidManifest.xml`, `SettingsStore.kt`, `FairwayFitApp.kt`, `Reminders.kt`,
`PlayerScreen.kt`, `SettingsScreen.kt`.
