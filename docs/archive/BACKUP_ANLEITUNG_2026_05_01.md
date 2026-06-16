# 📦 BACKUP-ANLEITUNG - Manuelles Backup

**Datum:** 01. Mai 2026  
**Grund:** Lottie Animation Analyse Session abgeschlossen  
**Backup-Name:** `FORMIX_Backup_2026_05_01_LOTTIE_ANALYSE`

---

## ⚠️ WICHTIG: Manuelles Backup durchführen!

Das automatische Backup-Script hatte Probleme mit Windows-Pfaden.  
Bitte **manuelles Backup** per Windows Explorer durchführen:

---

## 📋 SCHRITT-FÜR-SCHRITT ANLEITUNG

### 1. Windows Explorer öffnen
Drücke `Windows + E`

### 2. Zum Projekt-Ordner navigieren
```
D:\Entwicklung\Android\FORMIX
```

### 3. Ganzen Ordner kopieren
- Rechtsklick auf `FORMIX` Ordner
- Wähle **"Kopieren"** (oder `Strg + C`)

### 4. Zum Backup-Verzeichnis navigieren
```
D:\Backups\
```

Falls der Ordner nicht existiert:
- Rechtsklick im Explorer
- "Neu" → "Ordner"
- Name: `Backups`

### 5. Einfügen und umbenennen
- Im `D:\Backups\` Ordner: Rechtsklick → **"Einfügen"** (oder `Strg + V`)
- Warte bis Kopiervorgang abgeschlossen
- Rechtsklick auf den kopierten Ordner → **"Umbenennen"**
- Neuer Name: `FORMIX_Backup_2026_05_01_LOTTIE_ANALYSE`

### 6. Überprüfung
Stelle sicher dass folgende Dateien im Backup vorhanden sind:
- ✅ `LOTTIE_ANALYSE.md`
- ✅ `LOTTIE_BENUTZTE_UEBUNGEN.md`
- ✅ `SITZUNGSPROTOKOLL_2026_05_01_LOTTIE_ANALYSE.md`
- ✅ `app/` Verzeichnis
- ✅ `build.gradle.kts`
- ✅ `settings.gradle.kts`

---

## 📊 BACKUP-INHALT

### Was wird gesichert:
- ✅ Gesamtes FORMIX Projekt (ca. 500+ MB)
- ✅ Alle Quellcode-Dateien (`app/src/`)
- ✅ Alle Assets (Lottie-Animationen)
- ✅ Alle Protokolle und Dokumentationen
- ✅ Gradle Build-Dateien
- ✅ Git-Historie (`.git/`)
- ✅ IDE-Einstellungen (`.idea/`)

### Was NICHT gesichert werden muss:
- ❌ `.gradle/` (Build-Cache - kann regeneriert werden)
- ❌ `app/build/` (Build-Output - kann regeneriert werden)
- ❌ `hs_err_pid*.log` (Crash-Logs)

---

## 🎯 BACKUP-ZWECK

Dieses Backup sichert den Stand **VOR** den Quick Wins:

### Aktueller Stand:
- Version: 3.1.0 / Build 17
- Build-Status: ✅ BUILD SUCCESSFUL
- Letzter Code-Change: Keine Änderungen in dieser Session
- Neue Dateien: 3 Dokumentations-Dateien (siehe oben)

### Geplante Änderungen (nach Backup):
- Quick Win #1: Crunches/Sit-ups Animation mappen
- Quick Win #2: Beinheben Animation mappen
- Quick Win #3: Thruster Animation mappen

**Betroffene Datei:** `app/src/main/java/com/fantasyfoodplanner/features/fitness/LottieAnimationProvider.kt`  
**Änderungsumfang:** ~5 Zeilen (Synonym-Map erweitern)

---

## 🔄 ALTERNATIVE: ZIP-Backup

Falls Windows Explorer zu langsam ist:

### Option A: Mit 7-Zip (falls installiert)
1. Rechtsklick auf `FORMIX` Ordner
2. "7-Zip" → "Zu Archiv hinzufügen..."
3. Archivname: `FORMIX_Backup_2026_05_01_LOTTIE_ANALYSE.7z`
4. Speicherort: `D:\Backups\`

### Option B: Mit Windows ZIP
1. Rechtsklick auf `FORMIX` Ordner
2. "Senden an" → "ZIP-komprimierter Ordner"
3. Umbenennen zu: `FORMIX_Backup_2026_05_01_LOTTIE_ANALYSE.zip`
4. Verschieben nach `D:\Backups\`

---

## ✅ BACKUP BESTÄTIGUNG

Nach erfolgreichem Backup:

1. Überprüfe Backup-Größe (sollte ~500 MB sein)
2. Überprüfe Dateianzahl (sollte ~1000+ Dateien sein)
3. Öffne `SITZUNGSPROTOKOLL_2026_05_01_LOTTIE_ANALYSE.md` im Backup
4. Wenn alles OK → Backup ist abgeschlossen ✅

---

## 📞 BEI PROBLEMEN

Falls das Backup fehlschlägt:
- Stelle sicher dass genug Speicherplatz auf `D:\` vorhanden ist (mindestens 1 GB)
- Prüfe ob Antivirus das Kopieren blockiert
- Verwende ZIP-Backup als Alternative

---

**Erstellt am:** 01.05.2026 09:10  
**Status:** ⏸️ WARTET AUF MANUELLES BACKUP
