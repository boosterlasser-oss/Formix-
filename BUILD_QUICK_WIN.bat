@echo off
echo ========================================
echo FORMIX Quick Win Build
echo ========================================
echo.
echo Starte Gradle Build...
echo.

cd /d "D:\Entwicklung\Android\FORMIX"
call gradlew.bat assembleDebug --console=plain

echo.
echo ========================================
if %ERRORLEVEL% EQU 0 (
    echo BUILD SUCCESSFUL!
    echo APK Location: app\build\outputs\apk\debug\app-debug.apk
) else (
    echo BUILD FAILED!
    echo Error Code: %ERRORLEVEL%
)
echo ========================================
pause
