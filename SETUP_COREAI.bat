@echo off
REM ═══════════════════════════════════════════════════════════
REM CoreAI Setup – llama.cpp + NDK + CMake einrichten
REM ═══════════════════════════════════════════════════════════
REM
REM Dieses Skript:
REM   1) Installiert NDK und CMake ueber sdkmanager
REM   2) Klont llama.cpp
REM   3) Passt build.gradle.kts an
REM   4) Erklaert wie die GGUF-Datei aufs Geraet kommt
REM
REM VORAUSSETZUNG: Android SDK installiert (D:\Android\SDK)
REM ═══════════════════════════════════════════════════════════

set SDK_DIR=D:\Android\SDK
set SDKMANAGER=%SDK_DIR%\cmdline-tools\latest\bin\sdkmanager.bat

echo.
echo ══════════════════════════════════════════
echo  CoreAI Setup - Schritt 1: NDK + CMake
echo ══════════════════════════════════════════
echo.

REM Pruefe ob sdkmanager existiert
if not exist "%SDKMANAGER%" (
    echo [INFO] sdkmanager nicht unter cmdline-tools\latest gefunden.
    echo [INFO] Versuche alternativen Pfad...
    for /d %%d in (%SDK_DIR%\cmdline-tools\*) do (
        if exist "%%d\bin\sdkmanager.bat" set SDKMANAGER=%%d\bin\sdkmanager.bat
    )
)

if exist "%SDKMANAGER%" (
    echo [CoreAI] Installiere NDK 26.1.10909125...
    call "%SDKMANAGER%" "ndk;26.1.10909125" --sdk_root="%SDK_DIR%"
    echo.
    echo [CoreAI] Installiere CMake 3.22.1...
    call "%SDKMANAGER%" "cmake;3.22.1" --sdk_root="%SDK_DIR%"
    echo.
) else (
    echo [WARNUNG] sdkmanager nicht gefunden!
    echo [WARNUNG] Bitte installiere NDK und CMake manuell ueber Android Studio:
    echo           SDK Manager ^> SDK Tools ^> NDK (Side by side) + CMake
    echo.
)

echo.
echo ══════════════════════════════════════════
echo  CoreAI Setup - Schritt 2: llama.cpp
echo ══════════════════════════════════════════
echo.

cd /d "%~dp0app\src\main\cpp"

REM Pruefe ob Git verfuegbar ist
where git >nul 2>&1
if %ERRORLEVEL% neq 0 (
    echo [FEHLER] Git ist nicht installiert oder nicht im PATH!
    echo [FEHLER] Bitte installiere Git: https://git-scm.com/download/win
    echo.
    echo ALTERNATIVE: Lade llama.cpp manuell herunter:
    echo   1) https://github.com/ggerganov/llama.cpp/archive/refs/heads/master.zip
    echo   2) Entpacke nach: app\src\main\cpp\llama.cpp\
    echo.
    pause
    exit /b 1
)

if exist "llama.cpp" (
    echo [CoreAI] llama.cpp existiert bereits. Update...
    cd llama.cpp
    git pull
    cd ..
) else (
    echo [CoreAI] Klone llama.cpp von GitHub (shallow)...
    git clone --depth 1 https://github.com/ggerganov/llama.cpp.git
)

echo.
echo [CoreAI] Pruefe Dateien...
if exist "llama.cpp\ggml\src\ggml.c" (
    echo [OK] ggml.c gefunden
) else (
    echo [FEHLER] ggml.c NICHT gefunden - Clone fehlgeschlagen?
    pause
    exit /b 1
)

if exist "llama.cpp\llama.cpp" (
    echo [OK] llama.cpp gefunden
) else (
    echo [FEHLER] llama.cpp NICHT gefunden
    pause
    exit /b 1
)

echo.
echo ══════════════════════════════════════════
echo  CoreAI Setup - Schritt 3: Gradle Config
echo ══════════════════════════════════════════
echo.
echo WICHTIG: Fuege folgendes in app/build.gradle.kts ein:
echo.
echo Im android { } Block NACH buildTypes:
echo.
echo     externalNativeBuild {
echo         cmake {
echo             path = file("src/main/cpp/CMakeLists.txt")
echo             version = "3.22.1"
echo         }
echo     }
echo.
echo Im defaultConfig { } Block:
echo.
echo     ndk { abiFilters += listOf("arm64-v8a", "armeabi-v7a") }
echo.

echo.
echo ══════════════════════════════════════════
echo  CoreAI Setup - Schritt 4: Modell-Datei
echo ══════════════════════════════════════════
echo.
echo Die GGUF-Datei (gemma-2-2b-it-Q4_K_M.gguf) muss aufs Geraet:
echo.
echo   Per ADB:
echo   adb push gemma-2-2b-it-Q4_K_M.gguf /sdcard/Android/data/com.fantasyfoodplanner.fix.v4.debug/files/models/
echo.
echo   ODER in den App-internen Speicher:
echo   adb push gemma-2-2b-it-Q4_K_M.gguf /data/data/com.fantasyfoodplanner.fix.v4.debug/files/models/
echo.
echo ══════════════════════════════════════════
echo  FERTIG! Jetzt in Android Studio bauen.
echo ══════════════════════════════════════════
pause
