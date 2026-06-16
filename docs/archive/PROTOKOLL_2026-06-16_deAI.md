# PROTOKOLL — 16.06.2026 — FORMIX: Feature-Bereinigung (Voice + Cloud Backup entfernt)

## Zusammenfassung

**Ziel:** Voice-Steuerung, KI-Coach und Cloud Backup aus FORMIX-App entfernen,
da diese Features nicht genutzt werden und die App unnötig aufblähen.

**Ergebnis:** Alle drei Features sauber entfernt, Build läuft durch.
Backups der entfernten Module liegen unter `backup/`.

---

## 1. Voice-Steuerung entfernt

### Gelöschte Dateien (→ backup/voice/)

| Datei | Backup-Pfad |
|-------|-------------|
| `voice/VoiceCommands.kt` | `backup/voice/VoiceCommands.kt` |
| `voice/VoiceAvatar.kt` | `backup/voice/VoiceAvatar.kt` |
| `voice/VoiceButton.kt` | `backup/voice/VoiceButton.kt` |
| `voice/VoiceCoachPlugin.kt` | `backup/voice/VoiceCoachPlugin.kt` |
| `voice/VoiceCoachService.kt` | `backup/voice/VoiceCoachService.kt` |
| `voice/VoiceCoachUi.kt` | `backup/voice/VoiceCoachUi.kt` |
| `voice/CoachSuggestionEngine.kt` | `backup/voice/CoachSuggestionEngine.kt` |
| `voice/VoiceCoachSettings.kt` | `backup/voice/VoiceCoachSettings.kt` |
| `voice/` (gesamter Ordner) | `backup/voice/` |

### Geänderte Dateien

**`logic/SettingsManager.kt`:**
- `voiceEnabled` + Getter/Setter entfernt
- `voiceCoachEnabled` + Getter/Setter entfernt

**`features/Profile.kt`:**
- Voice-Einstellungen Sektion entfernt (KI-COACH)
- Verweis auf `VoiceCoachSettingsSheet` entfernt

**`features/DayMenu.kt`:**
- Coach-FAB entfernt

**`features/WorkoutStep.kt`:**
- Coach-Icon in AppBar entfernt

---

## 2. CoachChatSheet entfernt (→ backup/voice_coach_plugin/)

**`features/CoachChatSheet.kt`:**
→ `voice_coach_plugin/` als Standalone-Plugin archiviert

Alle Imports und Verweise auf CoachChatSheet aus allen Dateien entfernt.

---

## 3. Google Drive Cloud Backup entfernt (→ backup/DriveBackupManager/)

**`logic/DriveBackupManager.kt`:**
→ `backup/DriveBackupManager/DriveBackupManager.kt`

**`features/Profile.kt`:**
- Cloud Backup UI (CLOUD BACKUP & SYNC) vollständig entfernt
- Google Sign-In State + Launcher entfernt
- Import `DriveBackupManager` entfernt
- Import `Log` entfernt (nicht mehr benötigt)

---

## 4. Fixes während der Bereinigung

### `features/MainActivity.kt` — isStepComplete Compile-Fehler
**Problem:** `StepData.isStepComplete` existierte nicht als Methode.
**Fix:** Auf `StepData.Companion.isStepComplete(data, ...)` umgestellt.

### `features/Profile.kt` — Brache-Struktur nach Edit
**Problem:** Nach Entfernen der Cloud-Backup-Sektion blieb ein
extra `}` (FantasySurface-Close) übrig, der den Build brach.
**Fix:** Stray `}` und falsche `@Composable`-Einrückung korrigiert.

### `features/Profile.kt` — Google Sign-In Fehlermeldung
**Verbesserung:** Fehlermeldung zeigt jetzt `e.message` statt
festem String "Anmeldung fehlgeschlagen" — besser für Debugging.

---

## 5. Build-Status

```powershell
BUILD SUCCESSFUL in 8s
40 actionable tasks: 6 executed, 34 up-to-date
```

APK: `app/build/outputs/apk/debug/app-debug.apk`

---

## 6. Offene Punkte

| Prio | Aufgabe | Status |
|------|---------|--------|
| NIEDRIG | `SubscriptionManager.hasCloudBackup()` ggf. aufräumen | Nicht nötig — Methode wird noch für PREMIUM-Prüfung genutzt |
