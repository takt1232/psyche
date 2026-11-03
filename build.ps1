param(
    [Alias("b")]
    [switch]$Build,

    [Alias("i")]
    [switch]$Install,

    [string]$Serial
)

$ErrorActionPreference = "Stop"

$gradlew = Join-Path $PSScriptRoot "gradlew.bat"
$apk = Join-Path $PSScriptRoot "app\build\outputs\apk\debug\app-debug.apk"
$applicationId = "com.psyche.app"

function Show-Usage {
    Write-Host "Usage:"
    Write-Host "  .\build.cmd -Build      Build the debug APK."
    Write-Host "  .\build.cmd -Install    Build and install the debug APK on the connected phone."
    Write-Host ""
    Write-Host "Options:"
    Write-Host "  -Serial <id>            Target a specific device (default: first connected)."
    Write-Host ""
    Write-Host "Short flags: -b (build), -i (install)."
}

function Invoke-Build {
    Write-Host "Building debug APK..." -ForegroundColor Cyan
    & $gradlew ":app:assembleDebug"

    if ($LASTEXITCODE -ne 0) {
        throw "Gradle build failed (exit code $LASTEXITCODE)."
    }

    if (-not (Test-Path $apk)) {
        throw "Build finished but the APK was not found at $apk."
    }

    $sizeMb = [math]::Round((Get-Item $apk).Length / 1MB, 1)
    Write-Host "APK: $apk ($sizeMb MB)" -ForegroundColor Green
}

function Get-Serial {
    if ($Serial) { return $Serial }

    if (-not (Get-Command adb -ErrorAction SilentlyContinue)) {
        throw "adb was not found on PATH. Install Android platform-tools or add them to PATH."
    }

    $output = adb devices

    if ($output -match "unauthorized") {
        throw "A device is unauthorized. Accept the USB debugging prompt on the phone, then retry."
    }

    $online = @(
        $output |
            Select-String -Pattern "^(\S+)\s+device$" |
            ForEach-Object { $_.Matches[0].Groups[1].Value }
    )

    if ($online.Count -eq 0) {
        throw "No device connected. Pair and connect over ADB first, then run 'adb devices' to confirm."
    }

    if ($online.Count -gt 1) {
        Write-Warning "Multiple devices detected. Using $($online[0]). Pass -Serial to choose another."
    }

    return $online[0]
}

function Invoke-Install {
    $serial = Get-Serial
    Write-Host "Installing on $serial..." -ForegroundColor Cyan

    adb -s $serial install -r $apk
    if ($LASTEXITCODE -ne 0) {
        throw "adb install failed (exit code $LASTEXITCODE)."
    }

    Write-Host "Installed $applicationId on $serial." -ForegroundColor Green
}

if (-not $Build -and -not $Install) {
    Show-Usage
    exit 1
}

if ($Build -and $Install) {
    Write-Host "error: choose either -Build or -Install, not both." -ForegroundColor Red
    exit 1
}

if (-not $env:JAVA_HOME) {
    Write-Warning "JAVA_HOME is not set. Set it to a JDK 17 install or Gradle may use the wrong Java version."
}

if ($Install) {
    Invoke-Build
    Invoke-Install
} else {
    Invoke-Build
}

exit 0
