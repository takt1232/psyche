param(
    [Parameter(Position = 0)]
    [ValidateSet("connect", "devices", "deploy", "test", "logcat")]
    [string]$Command = "deploy",

    [Parameter(Position = 1)]
    [string]$Target
)

$ErrorActionPreference = "Stop"
$gradlew = Join-Path $PSScriptRoot "gradlew.bat"

function Get-Serial {
    if ($Target) { return $Target }

    $device = adb devices |
        Select-String -Pattern "^(\S+)\s+device$" |
        Select-Object -First 1

    if (-not $device) {
        throw "No device connected. Pair first, then run: .\dev.ps1 connect <ip:port>"
    }

    return $device.Matches[0].Groups[1].Value
}

switch ($Command) {
    "connect" {
        if (-not $Target) { throw "Usage: .\dev.ps1 connect <ip:port>" }
        adb connect $Target
    }

    "devices" {
        adb devices -l
    }

    "deploy" {
        & $gradlew installDebug
        if ($LASTEXITCODE -ne 0) { throw "Gradle build failed." }

        $serial = Get-Serial
        adb -s $serial shell am start -n "com.psyche.app/.MainActivity"
    }

    "test" {
        & $gradlew testDebugUnitTest
    }

    "logcat" {
        $serial = Get-Serial
        adb -s $serial logcat
    }
}
