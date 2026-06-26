# BankPulse

A privacy-first personal-finance Android app that reads your bank transaction SMS
**entirely on-device**, tracks per-bank balances, and surfaces insights: recurring
expenses, your main income source, and where your money goes. Generates a weekly
report notification.

Nothing ever leaves your phone. There is no server, no account, no network call.

---

## Build the APK

You need **Android Studio** (Hedgehog or newer).

1. Open Android Studio → **Open** → select this `BankPulse` folder.
2. Wait for it to sync. The first sync downloads the Gradle wrapper and
   dependencies automatically — no `gradle-wrapper.jar` is shipped on purpose;
   Android Studio regenerates it.
3. **Build → Build Bundle(s) / APK(s) → Build APK(s)**.
4. When it finishes, click **locate** in the popup. The file is at:
   `app/build/outputs/apk/debug/app-debug.apk`
5. Copy that `.apk` to your phone and install it (you'll need to allow
   "install from unknown sources").

A debug APK installs fine for personal use. To build a release/signed APK use
**Build → Generate Signed Bundle / APK**.

---

## Why I can't hand you a prebuilt .apk

Compiling an Android APK requires the Google Android SDK and build toolchain,
which aren't reachable from the environment I run in. So instead you get the
full, ready-to-compile project — one click in Android Studio produces the .apk.

---

## First run

1. Launch the app. It asks for **SMS** and **notification** permission.
2. On grant, it scans your existing SMS inbox once and back-fills every bank
   transaction it can recognize.
3. New transactions are caught live as bank SMS arrive.
4. Every Monday ~9 AM it posts a weekly-report notification (in / out / net +
   top spending category).

> **Play Store note:** Google heavily restricts `READ_SMS`/`RECEIVE_SMS`. This
> app is meant to be **sideloaded** for personal use. Publishing it to the Play
> Store would require a special-use declaration and would likely be rejected.
> Personal sideloading is unaffected.

---

## Customizing

- **Add a bank / new SMS format** → `sms/SmsParser.kt`
  (`bankFromSender` map + the credit/debit vocabulary and regexes).
- **Tune spending categories** → `sms/Categorizer.kt` (keyword → bucket map).
- **Exact prototype fonts** → drop Archivo + IBM Plex Mono `.ttf` into
  `app/src/main/res/font/` and reference them in `ui/theme/Type.kt`
  (currently uses system Monospace for amounts as a stand-in).
- **Recurring-expense sensitivity** → `repo/Analytics.kt` (`detectRecurring`,
  the amount-tolerance and cadence thresholds).

---

## How it works (architecture)

- **`sms/SmsParser.kt`** — the brain. Regex-based parser for Indian bank SMS.
  Detects direction (earliest action verb wins), amount (grouped *or* plain
  numbers), counterparty, account tail, and post-txn balance.
- **`data/`** — Room database; each transaction is keyed by a hash of its SMS so
  re-scanning never creates duplicates.
- **`repo/Analytics.kt`** — pure functions that compute balances, in/out totals,
  main provider, top destination, category breakdown, and recurring detection.
- **`work/WeeklyReportWorker.kt`** — WorkManager job for the Monday report.
- **`ui/`** — Jetpack Compose. Four screens: Dashboard, Transactions
  (tap a row to see the source SMS), Reports, Insights.
