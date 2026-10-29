# Psyche

A minimal Jetpack Compose Android app scaffold, set up for command-line development
without Android Studio.

## Toolchain

| Tool | Version |
|---|---|
| JDK | 17 (Temurin 17.0.20.1) |
| Gradle | 9.6.0 (wrapper) |
| Android Gradle Plugin | 9.4.1 |
| Kotlin | 2.4.21 |
| Compose BOM | 2026.09.00 |
| compileSdk | 37.0 |
| targetSdk | 36 |
| minSdk | 26 |

`compileSdkMinor = 0` is required because Android 17 ships as the `android-37.0`
platform package rather than a plain `android-37`.

## Build and install

```powershell
.\gradlew.bat assembleDebug
adb install -r app\build\outputs\apk\debug\app-debug.apk
adb shell am start -n com.psyche.app/.MainActivity
```

`adb install -r` is preferred over Gradle's `installDebug` task over wireless ADB:
ddmlib sometimes times out waiting for the device even though the APK installs fine.

## Unit tests

```powershell
.\gradlew.bat testDebugUnitTest
```

The HTML report is written to:

```
app/build/reports/tests/testDebugUnitTest/index.html
```

Tests use JUnit 4, Truth, Turbine, and `kotlinx-coroutines-test`. MockK is on the
test classpath for collaborators that need it.

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

## Project structure

```
app/src/main/java/com/psyche/app/
├── MainActivity.kt
├── PsycheApplication.kt
├── data/
│   ├── Entry.kt
│   ├── EntryRepository.kt
│   └── InMemoryEntryRepository.kt
└── ui/
    ├── PsycheApp.kt
    ├── home/
    │   ├── HomeScreen.kt
    │   ├── HomeUiState.kt
    │   └── HomeViewModel.kt
    └── theme/
        ├── Color.kt
        ├── Theme.kt
        └── Type.kt
```

The repository is in-memory for now. Replace `InMemoryEntryRepository` with a
Room-backed implementation later; `HomeViewModel` only depends on the
`EntryRepository` interface.

## Notes

- The application ID is `com.psyche.app`. Change it before publishing if needed.
- The debug keystore is generated automatically on the first build at
  `%USERPROFILE%\.android\debug.keystore`.
- `gradle.properties` is tuned for a machine with roughly 6 GB of usable RAM:
  1.5 GB Gradle heap, 1 GB Kotlin daemon, two workers, no parallel builds.
