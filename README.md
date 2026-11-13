# Psyche

A personal Android app with a local-only Finance Tracker built as a
**Balance Pass** and a live activity board. It is set up for command-line
development without Android Studio.

The finance module is the first Psyche feature. It is intentionally single-owner:
PHP / en-PH formatting, 12-hour time, no account system, no network permission,
and no analytics or cloud backup.

## What is in the build

- A live **net position** across active accounts and savings goals:
  `cash + savings + prepaid card balance − credit card outstanding`.
- A signed net number that can be positive or negative, with **MONEY** and
  **OWED** beside it.
- A debt-coverage indicator: whether the money on hand could settle every
  active credit card today, or how much is short.
- Calculated balances for every active account. Credit rows show the negative
  balance, outstanding amount, limit, and available credit.
- Cash, debit/e-wallet, and credit accounts, including credit limit and
  outstanding.
- Savings goals funded by transfers; goal money still counts in the net
  position.
- Expenses, income, and transfers in one local ledger.
- Card payments are transfers: they reduce cash and outstanding together, so
  the net position does not change.
- Archived accounts are excluded from totals; their history stays in the
  record.
- Quick capture from the yellow FAB.
- A home-screen widget with the net position and up to four custom one-tap
  buttons.
- Custom one-tap transactions (label, amount, expense/income, account, category)
  that log instantly from the widget.
- Activity edit/delete with undo, month switching, filters, and a running net
  after each row.
- Expense categories as optional labels; there is no monthly cap or category
  allocation.
- System, light, or dark appearance in Settings; text size follows Android.
- Room persistence on the phone.

The old journaling scaffold, in-memory repository, and budget/cap feature have
been removed.

## One-tap widget

1. Open **Settings → One-tap actions** and add up to four presets.
2. Tap **Add widget** there, or long-press the home screen and open
   **Widgets → Psyche**.
3. Tap a preset on the widget to log it immediately. The widget reprints the
   net position and money/owed line; the new entry appears as the lit row when
   the app opens.

The widget uses the fixed dark Balance Pass palette so it stays legible on any
wallpaper. An expense preset the source account cannot cover is marked **LOW**
and turns danger-red; tapping it reports the exact shortfall instead of logging.
The repository still refuses the overdraw as the final guard. Credit presets
check available credit rather than cash, and income presets have no balance
check. Corrections and undo happen in Activity; a widget tap has no undo surface
of its own.

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

Use the build helper from the project root:

```powershell
.\build.cmd -Build      # build the debug APK only
.\build.cmd -Install    # build and install on the connected phone
```

`-b` and `-i` are short aliases. `-Install` builds first, then installs
`app\build\outputs\apk\debug\app-debug.apk` with `adb install -r`; it does not
launch the app. Open the app yourself, or run:

```powershell
adb shell am start -n com.psyche.app/.MainActivity
```

If more than one device is connected, pass `-Serial <id>` (from `adb devices`):

```powershell
.\build.cmd -Install -Serial 192.168.1.50:41234
```

Wireless debugging on Android 11+ can expose an mDNS serial containing spaces,
such as `adb-... (2)._adb-tls-connect._tcp`. The build helper accepts that serial
as well.

The build helper checks `JAVA_HOME`, fails on a Gradle or `adb` error, and prints
the APK path and size after a successful build. `build.cmd` is a wrapper around
`build.ps1`, so it works from `cmd.exe`, PowerShell, or by double-clicking.

`adb install -r` preserves the Room database. To exercise the true first-run
experience (zero balance and the setup panel), clear app storage first:

```powershell
adb shell pm clear com.psyche.app
```

`adb install -r` is preferred over Gradle's `installDebug` task over wireless ADB:
ddmlib sometimes times out waiting for the device even though the APK installs fine.

## First run

1. Open the default Cash account and set its opening balance to the real cash
   you hold.
2. Optionally add a credit account and enter its current outstanding balance.
3. Add categories only if you want labels on expenses.
4. Add goals or more accounts later from the Accounts tab.

No amounts are prefilled. The default Cash account starts at ₱0 and cannot be
archived. After setup, create one-tap actions in Settings (or the Home strip),
then add the Psyche widget from Settings or by long-pressing the home screen.
Widget taps log immediately; the FAB remains the fallback capture path.

## Data and privacy

- Database: `psyche-finance.db` in the app's private storage.
- No `INTERNET` permission.
- `android:allowBackup="false"` so the ledger is not sent to cloud backup.
- Uninstalling or clearing app data removes the ledger permanently.
- The widget is a launcher surface only. It reads the local Room ledger and logs
  through a manifest broadcast receiver; it does not add network access.
- The version 1.1 database migration drops the retired budget and allocation
  tables. Accounts, entries, goals, one-tap actions, and settings are preserved.

## Project structure

```
app/src/main/java/com/psyche/app/
├── MainActivity.kt
├── PsycheApplication.kt
├── data/
│   ├── FinanceDatabase.kt
│   ├── FinanceDao.kt
│   ├── FinanceEntities.kt
│   ├── FinanceModels.kt
│   └── FinanceRepository.kt
├── widget/
│   ├── QuickActionReceiver.kt
│   ├── QuickTransactionWidget.kt
│   └── QuickWidgetRenderer.kt
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
    ├── Formats.kt
    ├── LedgerMath.kt
    ├── Money.kt
    ├── MonthMath.kt
    └── PositionMath.kt
```

The visual system is documented in [DESIGN.md](DESIGN.md); the product truth is in
[PRODUCT.md](PRODUCT.md) and the build brief in
[FINANCE-TRACKER-BRIEF.md](FINANCE-TRACKER-BRIEF.md).

## Unit tests

The project has a JUnit 4 / Truth / Turbine / `kotlinx-coroutines-test` test
classpath.

- `PositionMathTest` covers net position, debt coverage, card payments, goal
  contributions, negative net, and archived-account exclusion.
- `LedgerMathTest` covers the running net in the activity board.
- `FinanceRepositoryQuickTransactionTest` covers one-tap logging, the four-slot
  limit, and the account-archive guard.

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

Once `adb devices -l` lists the phone, build and install with:

```powershell
.\build.cmd -Install
```

Run unit tests with `.\gradlew.bat testDebugUnitTest`; follow logs with
`adb logcat`.

## Notes

- The application ID is `com.psyche.app`. Change it before publishing if needed.
- The debug keystore is generated automatically on the first build at
  `%USERPROFILE%\.android\debug.keystore`.
- `gradle.properties` is tuned for a machine with roughly 6 GB of usable RAM:
  1.5 GB Gradle heap, 1 GB Kotlin daemon, two workers, no parallel builds.
