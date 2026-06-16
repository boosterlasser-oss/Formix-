# FORMIX Quick Win Build Script
Write-Host "========================================"
Write-Host "FORMIX Quick Win Build"
Write-Host "========================================"
Write-Host ""

Set-Location "D:\Entwicklung\Android\FORMIX"

Write-Host "Starte Gradle Build..." -ForegroundColor Yellow
Write-Host ""

# Gradle Build ausführen
& ".\gradlew.bat" assembleDebug --stacktrace --info 2>&1 | Tee-Object -FilePath "build_output_ps.txt"

$exitCode = $LASTEXITCODE

Write-Host ""
Write-Host "========================================"
if ($exitCode -eq 0) {
    Write-Host "BUILD SUCCESSFUL!" -ForegroundColor Green
    Write-Host ""
    
    $apkPath = "app\build\outputs\apk\debug\app-debug.apk"
    if (Test-Path $apkPath) {
        $apk = Get-Item $apkPath
        Write-Host "APK erstellt:" -ForegroundColor Green
        Write-Host "  Pfad: $apkPath"
        Write-Host "  Größe: $([math]::Round($apk.Length / 1MB, 2)) MB"
        Write-Host "  Letzte Änderung: $($apk.LastWriteTime)"
    }
} else {
    Write-Host "BUILD FAILED!" -ForegroundColor Red
    Write-Host "Error Code: $exitCode"
    Write-Host ""
    Write-Host "Siehe build_output_ps.txt für Details"
}
Write-Host "========================================"
Write-Host ""
Write-Host "Build abgeschlossen."
