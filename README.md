# Psyche

A personal Android app with a local-only Finance Tracker built as a monthly
**Budget Pass** and a live activity board. It is set up for command-line
development without Android Studio.

The finance module is the first Psyche feature. It is intentionally single-owner:
PHP / en-PH formatting, 12-hour time, no account system, no network permission,
and no analytics or cloud backup.

## What is in the build

- Monthly cap with optional category allocations.
- Expenses, income, and transfers in one local ledger.
- A default Cash account at ₱0 that cannot be archived.
- Cash, debit/e-wallet, and credit accounts, including credit limit and outstanding.
- Expenses and transfers are limited to the source account's available cash or card credit.
- Paying a credit card is a cash/debit transfer capped at the card's outstanding balance.
- Savings goals funded by transfers from an account.
- Quick capture from the yellow FAB or an optional ongoing notification.
- Transfers from the FAB capture sheet or the Accounts summary; contributions cannot exceed the goal or the source account's available money.
- Lock screen hides the pass by default, with a setting to reveal it in the shade.
- System, light, or dark appearance in Settings; text size follows Android.
- Activity edit/delete with undo, month switching, filters, and running budget left.
- Room persistence on the phone.

The old journaling scaffold and in-memory repository have been removed.

## Toolchain

| Tool | Version |
|---|---|
| JDK | 17 (Temurin 17.0.20.1) |
| Gradle | 9.6.0 (wrapper) |
| Android Gradle Plugin | 9.4.1 |
| Kotlin | 2.4.21 |
| Compose BOM | 2026.09.00 |
| Room | 2.8.5 |
| KSP | 2.3.12 |
| compileSdk | 37.0 |
| targetSdk | 36 |
| minSdk | 26 |

`compileSdkMinor = 0` is required because Android 17 ships as the `android-37.0`
platform package rather than a plain `android-37`.

## Build and install

```powershell
.\gradlew.bat :app:assembleDebug
adb install -r app\build\outputs\apk\debug\app-debug.apk
adb shell am start -n com.psyche.app/.MainActivity
```

`adb install -r` preserves the Room database. To exercise the true first-run
experience (empty pass, “Set your cap”), clear app storage first:

```powershell
adb shell pm clear com.psyche.app
```

`adb install -r` is preferred over Gradle's `installDebug` task over wireless ADB:
ddmlib sometimes times out waiting for the device even though the APK installs fine.

## First run

1. Set a monthly cap.
2. Open the default Cash account and set its opening balance to the real cash you hold.
3. Optionally add categories and allocate part of the cap.
4. Add goals or a credit account later from their tabs.

No amounts are prefilled. The default Cash account starts at ₱0 and cannot be
archived. The notification prompt appears after the cap is set; declining it
leaves the FAB as the capture path.

## Data and privacy

- Database: `psyche-finance.db` in the app's private storage.
- No `INTERNET` permission.
- `android:allowBackup="false"` so the ledger is not sent to cloud backup.
- Uninstalling or clearing app data removes the ledger permanently.
- The notification is silent and ongoing; its amount is visible in the shade but
  the pass is hidden from the lock screen unless the setting is enabled.

## Project structure

```
app/src/main/java/com/psyche/app/
├── MainActivity.kt
├── CaptureActivity.kt
├── PsycheApplication.kt
├── data/
│   ├── FinanceDatabase.kt
│   ├── FinanceDao.kt
│   ├── FinanceEntities.kt
│   ├── FinanceModels.kt
│   └── FinanceRepository.kt
├── notifications/
│   └── BudgetNotifier.kt
├── ui/
│   ├── FinanceUiState.kt
│   ├── FinanceViewModel.kt
│   ├── PsycheApp.kt
│   ├── capture/
│   ├── components/
│   ├── screens/
│   ├── sheets/
│   └── theme/
└── util/
```

The visual system is documented in [DESIGN.md](DESIGN.md); the product truth is in
[PRODUCT.md](PRODUCT.md) and the build brief in
[FINANCE-TRACKER-BRIEF.md](FINANCE-TRACKER-BRIEF.md).

## Unit tests

The project has a JUnit 4 / Truth / Turbine / `kotlinx-coroutines-test` test
classpath. There are no Finance Tracker unit tests committed yet.

```powershell
.\gradlew.bat testDebugUnitTest
```

## Wireless debugging (Android 11+)

1. Phone and laptop on the same Wi-Fi network. Turn off any VPN on the laptop.
2. On the phone: **Settings → System → Developer options → Wireless debugging**.
3. Tap **Pair device with pairing code** and note the IP, pairing port, and code.
4. On the laptop:

```powershell
adb pair <ip>:<pair-port>
# enter the 6-digit code

adb connect <ip>:<connect-port>   # different port, shown on the Wireless debugging screen
adb devices -l
```

The connect port changes when the screen locks or Wi-Fi reconnects. Re-read it
from the Wireless debugging screen, or run `adb mdns services`.

Helper script:

```powershell
.\dev.cmd connect 192.168.1.50:41234
.\dev.cmd devices
.\dev.cmd test
.\dev.cmd deploy
.\dev.cmd logcat
```

Use `dev.cmd`, not `dev.ps1`: from `cmd.exe` (or by double-clicking), a `.ps1`
file is handed to the file association and opens in an editor instead of running.
If you prefer PowerShell directly:

```powershell
powershell -ExecutionPolicy Bypass -File .\dev.ps1 deploy
```

## Notes

- The application ID is `com.psyche.app`. Change it before publishing if needed.
- The debug keystore is generated automatically on the first build at
  `%USERPROFILE%\.android\debug.keystore`.
- `gradle.properties` is tuned for a machine with roughly 6 GB of usable RAM:
  1.5 GB Gradle heap, 1 GB Kotlin daemon, two workers, no parallel builds.
