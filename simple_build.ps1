Write-Host "FORMIX Quick Win Build Starting..." -ForegroundColor Cyan
Set-Location "D:\Entwicklung\Android\FORMIX"
& ".\gradlew.bat" assembleDebug --console=plain
$exitCode = $LASTEXITCODE
if ($exitCode -eq 0) {
    Write-Host "BUILD SUCCESSFUL" -ForegroundColor Green
    $apkPath = "app\build\outputs\apk\debug\app-debug.apk"
    if (Test-Path $apkPath) {
        $apk = Get-Item $apkPath
        Write-Host "APK Size: $([math]::Round($apk.Length / 1MB, 2)) MB"
    }
} else {
    Write-Host "BUILD FAILED - Exit Code: $exitCode" -ForegroundColor Red
}
