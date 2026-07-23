<#
.SYNOPSIS
  Start an Android emulator (if needed), build KYCISDemo, install, and launch.

.PARAMETER Avd
  AVD name. Default: Pixel_9_Pro

.PARAMETER DemoRoot
  Path to KYCISDemo. Default: parent of this scripts/ folder.

.EXAMPLE
  .\scripts\run-on-emulator.ps1
  .\scripts\run-on-emulator.ps1 -Avd Pixel_9_Pro
#>
param(
    [string]$Avd = "Pixel_9_Pro",
    [string]$DemoRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
)

$ErrorActionPreference = "Stop"

$sdk = if ($env:ANDROID_HOME) { $env:ANDROID_HOME } else { Join-Path $env:LOCALAPPDATA "Android\Sdk" }
$adb = Join-Path $sdk "platform-tools\adb.exe"
$emu = Join-Path $sdk "emulator\emulator.exe"

if (-not (Test-Path $adb)) { throw "adb not found at $adb (set ANDROID_HOME or install Android SDK)" }
if (-not (Test-Path $emu)) { throw "emulator not found at $emu" }

$env:Path = "$sdk\platform-tools;$sdk\emulator;$env:Path"

function Test-EmulatorReady {
    # adb may print "* daemon not running..." on stderr when starting; don't treat as failure.
    $prev = $ErrorActionPreference
    $ErrorActionPreference = "Continue"
    $lines = & $adb devices 2>&1 | ForEach-Object { "$_" }
    $ErrorActionPreference = $prev
    return [bool]($lines | Select-String -Pattern "emulator-\d+\s+device")
}

if (-not (Test-EmulatorReady)) {
    Write-Host "Starting AVD '$Avd'..."
    $avds = & $emu -list-avds 2>$null
    if (-not ($avds | Where-Object { $_.Trim() -eq $Avd })) {
        Write-Host "Available AVDs:"
        $avds | ForEach-Object { Write-Host "  $_" }
        throw "AVD '$Avd' not found. Pass -Avd <name>."
    }
    Start-Process -FilePath $emu -ArgumentList @(
        "-avd", $Avd,
        "-netdelay", "none",
        "-netspeed", "full"
    ) -WindowStyle Normal

    Write-Host "Waiting for adb device..."
    & $adb wait-for-device

    $ready = $false
    for ($i = 0; $i -lt 90; $i++) {
        $boot = (& $adb shell getprop sys.boot_completed 2>$null | Out-String).Trim()
        if ($boot -eq "1") { $ready = $true; break }
        Start-Sleep -Seconds 2
    }
    if (-not $ready) { throw "Emulator did not finish booting (sys.boot_completed)" }
    Write-Host "Emulator ready."
} else {
    Write-Host "Emulator already running."
}

Write-Host "Building + installing debug APK from $DemoRoot ..."
Set-Location $DemoRoot
& .\gradlew.bat :app:installDebug
if ($LASTEXITCODE -ne 0) { throw "gradlew :app:installDebug failed ($LASTEXITCODE)" }

Write-Host "Launching com.kycis.demo ..."
& $adb shell am force-stop com.kycis.demo
& $adb shell am start -n com.kycis.demo/.SplashActivity
Write-Host "Done. Default backend is https://api.kycis.zynnex.in; switch in Backend Settings for local (10.0.2.2:8000)."
