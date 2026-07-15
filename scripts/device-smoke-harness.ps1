# Drive SdkDiagnosticsScreen harness buttons via adb taps.
$ErrorActionPreference = "Stop"
$sdk = if ($env:ANDROID_HOME) { $env:ANDROID_HOME } else { Join-Path $env:LOCALAPPDATA "Android\Sdk" }
$adb = Join-Path $sdk "platform-tools\adb.exe"

function Invoke-Tap([int]$x, [int]$y) {
    Write-Host "tap $x $y"
    & $adb shell input tap $x $y
    Start-Sleep -Seconds 2
}

function Get-TapForText([string]$needle) {
    $dump = Join-Path $env:TEMP "kycis-ui-smoke.xml"
    & $adb shell uiautomator dump /sdcard/ui.xml | Out-Null
    & $adb pull /sdcard/ui.xml $dump | Out-Null
    $py = @"
import re, xml.etree.ElementTree as ET, sys
root = ET.parse(r'$dump').getroot()
needle = '$needle'.lower()
for node in root.iter('node'):
    t = (node.attrib.get('text') or node.attrib.get('content-desc') or '')
    if needle in t.lower():
        b = node.attrib.get('bounds','')
        m = re.match(r'\[(\d+),(\d+)\]\[(\d+),(\d+)\]', b)
        if m:
            x1,y1,x2,y2 = map(int, m.groups())
            print((x1+x2)//2, (y1+y2)//2)
            sys.exit(0)
sys.exit(1)
"@
    $coords = python -c $py 2>$null
    if (-not $coords) { throw "UI element not found: $needle" }
    $parts = $coords.Trim().Split(' ')
    return [int]$parts[0], [int]$parts[1]
}

Write-Host "Clearing logcat..."
& $adb logcat -c

Write-Host "Launching app..."
& $adb shell am force-stop com.kycis.demo
& $adb shell am start -n com.kycis.demo/.SplashActivity | Out-Null
Start-Sleep -Seconds 5

Write-Host "Open SDK diagnostics harness..."
Invoke-Tap 640 2304

Write-Host "Harness: validation failure"
$x, $y = Get-TapForText "Send validation failure"
Invoke-Tap $x $y

Write-Host "Harness: dynamic popup"
$x, $y = Get-TapForText "Check dynamic popup"
Invoke-Tap $x $y
Start-Sleep -Seconds 3

Write-Host "Harness: generic error"
$x, $y = Get-TapForText "Track generic error"
Invoke-Tap $x $y
Start-Sleep -Seconds 3

Write-Host "Done. Logcat tail:"
& $adb logcat -d -s KYCIS:* KYCIS/net:* KYCIS/ws:* | Select-Object -Last 40
